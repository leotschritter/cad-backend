#!/usr/bin/env python3
"""
Multi-Service Data Seeding Script

Seeds test data across all microservices:
- Itinerary Service: Users, itineraries, locations
- Comments & Likes Service: (Data created via itinerary service endpoints)
- Recommendation Service: Graph nodes and relationships
- Weather Forecast Service: Pre-cache weather data for test locations
- Travel Warnings Service: Read-only, no seeding needed

This ensures realistic test conditions with proper data distribution.
"""

import requests
import random
import time
import json
from datetime import datetime, timedelta
from pathlib import Path
from dotenv import load_dotenv
import os
import sys
from typing import List, Dict, Optional

# Load environment variables
BASE_DIR = Path(__file__).resolve().parent
load_dotenv(BASE_DIR / ".env.seed")

# Service URLs
ITINERARY_SERVICE_URL = os.getenv("ITINERARY_SERVICE_URL", "http://localhost:8080").rstrip("/")
COMMENTS_LIKES_SERVICE_URL = os.getenv("COMMENTS_LIKES_SERVICE_URL", "http://localhost:8084").rstrip("/")
RECOMMENDATION_SERVICE_URL = os.getenv("RECOMMENDATION_SERVICE_URL", "http://localhost:8081").rstrip("/")
WEATHER_SERVICE_URL = os.getenv("WEATHER_SERVICE_URL", "http://localhost:8082").rstrip("/")
TRAVEL_WARNINGS_SERVICE_URL = os.getenv("TRAVEL_WARNINGS_SERVICE_URL", "http://localhost:8083").rstrip("/")

# Configuration
NUM_USERS = int(os.getenv("NUM_USERS", "1000"))
NUM_ITINERARIES_PER_USER = int(os.getenv("NUM_ITINERARIES_PER_USER", "3"))
NUM_LOCATIONS_PER_ITINERARY = int(os.getenv("NUM_LOCATIONS_PER_ITINERARY", "2"))
CREATE_FIREBASE_USERS = os.getenv("CREATE_FIREBASE_USERS", "false").lower() == "true"
FIREBASE_PASSWORD = os.getenv("FIREBASE_PASSWORD", "LoadTest123!")
FIREBASE_API_KEY = os.getenv("FIREBASE_API_KEY", "")
SEED = int(os.getenv("RANDOM_SEED", str(int(time.time()))))

# Set random seed for reproducibility
random.seed(SEED)

# Sample data for generating varied content
FIRST_NAMES = [
    "Emma", "Liam", "Olivia", "Noah", "Ava", "Ethan", "Sophia", "Mason", "Isabella", "William",
    "Mia", "James", "Charlotte", "Benjamin", "Amelia", "Lucas", "Harper", "Henry", "Evelyn", "Alexander",
    "Abigail", "Michael", "Emily", "Daniel", "Elizabeth", "Matthew", "Sofia", "Jackson", "Avery", "David",
    "Ella", "Joseph", "Scarlett", "Samuel", "Grace", "Sebastian", "Chloe", "Jack", "Victoria", "Aiden"
]

LAST_NAMES = [
    "Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez",
    "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin",
    "Lee", "Thompson", "White", "Harris", "Clark", "Lewis", "Robinson", "Walker", "Young", "King"
]

DESTINATIONS = [
    "Paris", "Tokyo", "New York", "London", "Barcelona", "Rome", "Dubai", "Singapore", "Sydney", "Amsterdam",
    "Berlin", "Prague", "Vienna", "Budapest", "Lisbon", "Copenhagen", "Stockholm", "Helsinki", "Oslo", "Zurich",
    "Munich", "Venice", "Florence", "Madrid", "Porto", "Athens", "Istanbul", "Bangkok", "Hong Kong", "Seoul",
    "Beijing", "Shanghai", "Mumbai", "Delhi", "Cairo", "Marrakech", "Cape Town", "Rio de Janeiro", "Buenos Aires",
    "Mexico City", "Toronto", "Vancouver", "Montreal", "San Francisco", "Los Angeles", "Miami", "Chicago", "Boston",
    "Seattle", "Austin", "Dublin", "Edinburgh", "Brussels", "Luxembourg", "Reykjavik", "Warsaw", "Krakow", "Tallinn"
]

# Map destinations to approximate coordinates for weather data
DESTINATION_COORDS = {
    "Paris": (48.8566, 2.3522),
    "Tokyo": (35.6762, 139.6503),
    "New York": (40.7128, -74.0060),
    "London": (51.5074, -0.1278),
    "Barcelona": (41.3851, 2.1734),
    "Rome": (41.9028, 12.4964),
    "Dubai": (25.2048, 55.2708),
    "Singapore": (1.3521, 103.8198),
    "Sydney": (-33.8688, 151.2093),
    "Amsterdam": (52.3676, 4.9041),
    "Berlin": (52.5200, 13.4050),
    "Prague": (50.0755, 14.4378),
    "Vienna": (48.2082, 16.3738),
    "Budapest": (47.4979, 19.0402),
    "Lisbon": (38.7223, -9.1393),
}

TRIP_TYPES = [
    "Family Trip to", "Business Trip to", "Romantic Getaway in", "Solo Adventure in", "Weekend in",
    "Summer Vacation in", "Winter Holiday in", "Backpacking through", "Luxury Tour of", "Cultural Exploration of",
    "Food Tour in", "Historical Journey through", "Beach Vacation in", "Mountain Retreat to", "City Break in"
]

DESCRIPTIONS_SHORT = [
    "Exploring the beautiful sights and sounds",
    "A wonderful journey through historic landmarks",
    "Discovering hidden gems and local culture",
    "Relaxing and enjoying the local cuisine",
    "Adventure and excitement await",
    "A perfect blend of relaxation and exploration",
    "Immersing in the local traditions",
    "Experiencing world-class attractions",
    "Creating unforgettable memories",
    "Wandering through charming streets",
    "Authentic cultural experiences",
    "Breathtaking views and scenery"
]

DESCRIPTIONS_DETAILED = [
    "An amazing journey that combines culture, history, and modern attractions. We'll visit famous landmarks, try local cuisine, and immerse ourselves in the vibrant atmosphere.",
    "This trip promises to be unforgettable with carefully planned visits to must-see attractions, hidden local gems, and authentic experiences that showcase the true spirit of the place.",
    "A comprehensive itinerary that balances sightseeing with leisure time, allowing us to explore at our own pace while ensuring we don't miss the highlights.",
    "From historic monuments to contemporary art scenes, this adventure will take us through the diverse facets of the destination, offering insights into both its past and present.",
    "An carefully curated experience featuring the best accommodations, dining options, and activities that will make this trip truly memorable.",
    "Discover the heart and soul of this destination through authentic local experiences, hidden treasures, and unforgettable moments.",
    "A perfect itinerary mixing relaxation, adventure, culture, and cuisine for an enriching travel experience."
]

LOCATION_NAMES = [
    "City Center", "Historic District", "Waterfront Area", "Old Town", "Modern Quarter",
    "Museum District", "Shopping Area", "Cultural Center", "Park Area", "Entertainment Zone",
    "Business District", "Arts Quarter", "Riverside Walk", "Market Square", "Cathedral Quarter"
]

LOCATION_DESCRIPTIONS = [
    "A vibrant area filled with local shops, restaurants, and attractions.",
    "Historic buildings and charming streets full of character.",
    "Beautiful views and plenty of activities for everyone.",
    "A must-visit spot known for its unique atmosphere.",
    "The perfect place to relax and enjoy the surroundings.",
    "Rich in history and cultural significance.",
    "Modern architecture and world-class facilities.",
    "Breathtaking scenery and natural beauty.",
    "Bustling with energy and local life.",
    "Peaceful and scenic, ideal for exploration."
]

COMMENTS = [
    "This looks amazing!",
    "I've been there! Such a wonderful place.",
    "Adding this to my bucket list!",
    "Great itinerary, very well planned.",
    "Thanks for sharing this experience.",
    "Beautiful destination!",
    "Can't wait to visit!",
    "This is exactly what I was looking for.",
    "Wonderful suggestions!",
    "Very helpful itinerary!"
]

# Statistics
stats = {
    "users_created": 0,
    "firebase_users_created": 0,
    "itineraries_created": 0,
    "locations_created": 0,
    "likes_created": 0,
    "comments_created": 0,
    "graph_nodes_created": 0,
    "weather_cached": 0,
    "errors": []
}

# Storage for created entities
created_users = []
created_itineraries = []


def create_firebase_user(email: str, password: str) -> Optional[str]:
    """Create a Firebase user and return the UID."""
    if not FIREBASE_API_KEY:
        return None
    
    url = f"https://identitytoolkit.googleapis.com/v1/accounts:signUp?key={FIREBASE_API_KEY}"
    
    try:
        response = requests.post(
            url,
            json={
                "email": email,
                "password": password,
                "returnSecureToken": True
            },
            timeout=10
        )
        
        if response.status_code == 200:
            data = response.json()
            return data.get("localId")
        else:
            # User might already exist
            return None
            
    except Exception as e:
        stats["errors"].append(f"Firebase user creation error for {email}: {e}")
        return None


def get_firebase_token(email: str, password: str) -> Optional[str]:
    """Get Firebase ID token for authentication."""
    if not FIREBASE_API_KEY:
        return None
    
    url = f"https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key={FIREBASE_API_KEY}"
    
    try:
        response = requests.post(
            url,
            json={
                "email": email,
                "password": password,
                "returnSecureToken": True
            },
            timeout=10
        )
        
        if response.status_code == 200:
            data = response.json()
            return data.get("idToken")
        return None
    except:
        return None


def create_user(user_id: int, name: str, email: str) -> bool:
    """Create a user in the itinerary service."""
    try:
        response = requests.post(
            f"{ITINERARY_SERVICE_URL}/user/register",
            json={"name": name, "email": email},
            timeout=10
        )
        
        if response.status_code in [200, 201]:
            stats["users_created"] += 1
            return True
        else:
            stats["errors"].append(f"User creation failed for {email}: {response.status_code}")
            return False
            
    except Exception as e:
        stats["errors"].append(f"User creation error for {email}: {e}")
        return False


def create_itinerary(user_email: str, bearer_token: Optional[str]) -> Optional[Dict]:
    """Create an itinerary and return its data."""
    destination = random.choice(DESTINATIONS)
    start_date = (datetime.now() + timedelta(days=random.randint(1, 365))).strftime("%Y-%m-%d")
    
    itinerary_data = {
        "title": f"{random.choice(TRIP_TYPES)} {destination}",
        "destination": destination,
        "startDate": start_date,
        "shortDescription": random.choice(DESCRIPTIONS_SHORT),
        "detailedDescription": random.choice(DESCRIPTIONS_DETAILED)
    }
    
    headers = {"Content-Type": "application/json"}
    if bearer_token:
        headers["Authorization"] = f"Bearer {bearer_token}"
    
    try:
        response = requests.post(
            f"{ITINERARY_SERVICE_URL}/itinerary/create",
            json=itinerary_data,
            headers=headers,
            timeout=10
        )
        
        if response.status_code in [200, 201]:
            result = response.json()
            stats["itineraries_created"] += 1
            return result
        else:
            stats["errors"].append(f"Itinerary creation failed: {response.status_code}")
            return None
            
    except Exception as e:
        stats["errors"].append(f"Itinerary creation error: {e}")
        return None


def create_location(itinerary_id: int, destination: str, bearer_token: Optional[str]) -> bool:
    """Create a location for an itinerary."""
    from_date = (datetime.now() + timedelta(days=random.randint(1, 30))).strftime("%Y-%m-%d")
    to_date = (datetime.now() + timedelta(days=random.randint(31, 37))).strftime("%Y-%m-%d")
    
    # Get coordinates for weather lookup
    coords = DESTINATION_COORDS.get(destination, (0.0, 0.0))
    
    location_data = {
        "name": f"{random.choice(LOCATION_NAMES)}, {destination}",
        "description": random.choice(LOCATION_DESCRIPTIONS),
        "latitude": coords[0] + random.uniform(-0.1, 0.1),
        "longitude": coords[1] + random.uniform(-0.1, 0.1),
        "fromDate": from_date,
        "toDate": to_date
    }
    
    headers = {"Content-Type": "application/json"}
    if bearer_token:
        headers["Authorization"] = f"Bearer {bearer_token}"
    
    try:
        response = requests.post(
            f"{ITINERARY_SERVICE_URL}/location/itinerary/{itinerary_id}",
            json=location_data,
            headers=headers,
            timeout=10
        )
        
        if response.status_code in [200, 201]:
            stats["locations_created"] += 1
            return True
        else:
            stats["errors"].append(f"Location creation failed: {response.status_code}")
            return False
            
    except Exception as e:
        stats["errors"].append(f"Location creation error: {e}")
        return False


def record_itinerary_in_graph(itinerary: Dict, bearer_token: Optional[str]) -> bool:
    """Record an itinerary in the recommendation service graph."""
    if not RECOMMENDATION_SERVICE_URL:
        return True  # Skip if recommendation service not configured
    
    graph_data = {
        "itineraryId": itinerary.get("id"),
        "title": itinerary.get("title"),
        "description": itinerary.get("shortDescription", ""),
        "locationNames": [],  # Will be populated as locations are added
        "likesCount": 0,
        "eventType": "CREATED"
    }
    
    headers = {"Content-Type": "application/json"}
    if bearer_token:
        headers["Authorization"] = f"Bearer {bearer_token}"
    
    try:
        response = requests.post(
            f"{RECOMMENDATION_SERVICE_URL}/graph/itineraries",
            json=graph_data,
            headers=headers,
            timeout=10
        )
        
        if response.status_code in [200, 201]:
            stats["graph_nodes_created"] += 1
            return True
        else:
            return False
            
    except Exception as e:
        stats["errors"].append(f"Graph recording error: {e}")
        return False


def add_random_likes(itinerary_id: int, num_likes: int, bearer_token: Optional[str]):
    """Add random likes to an itinerary from other users."""
    if num_likes == 0 or not created_users:
        return
    
    # Select random users to like this itinerary
    likers = random.sample(created_users, min(num_likes, len(created_users)))
    
    headers = {"Content-Type": "application/json", "Content-Length": "0"}
    if bearer_token:
        headers["Authorization"] = f"Bearer {bearer_token}"
    
    for liker in likers:
        try:
            # Like in comments-likes service (UPDATED: now separate microservice)
            response = requests.post(
                f"{COMMENTS_LIKES_SERVICE_URL}/like/itinerary/{itinerary_id}",
                headers=headers,
                timeout=5
            )
            
            if response.status_code in [200, 201]:
                stats["likes_created"] += 1
                
                # Record like in recommendation service
                if RECOMMENDATION_SERVICE_URL and bearer_token:
                    try:
                        requests.post(
                            f"{RECOMMENDATION_SERVICE_URL}/graph/likes",
                            json={"itineraryId": itinerary_id},
                            headers={"Content-Type": "application/json", "Authorization": f"Bearer {bearer_token}"},
                            timeout=5
                        )
                    except:
                        pass
        except:
            pass


def add_random_comments(itinerary_id: int, num_comments: int, bearer_token: Optional[str]):
    """Add random comments to an itinerary."""
    if num_comments == 0 or not created_users:
        return
    
    commenters = random.sample(created_users, min(num_comments, len(created_users)))
    
    headers = {"Content-Type": "application/json"}
    if bearer_token:
        headers["Authorization"] = f"Bearer {bearer_token}"
    
    for commenter in commenters:
        comment_data = {
            "userEmail": commenter["email"],
            "comment": random.choice(COMMENTS)
        }
        
        try:
            response = requests.post(
                f"{COMMENTS_LIKES_SERVICE_URL}/comment/itinerary/{itinerary_id}",
                json=comment_data,
                headers=headers,
                timeout=5
            )
            
            if response.status_code in [200, 201]:
                stats["comments_created"] += 1
        except:
            pass


def cache_weather_data():
    """Pre-cache weather data for common destinations."""
    if not WEATHER_SERVICE_URL:
        return
    
    print("\n📊 Caching weather data for test destinations...")
    
    for destination, coords in list(DESTINATION_COORDS.items())[:10]:  # Cache top 10
        try:
            response = requests.get(
                f"{WEATHER_SERVICE_URL}/api/weather/forecast/coordinates",
                params={"lat": coords[0], "lon": coords[1]},
                timeout=10
            )
            
            if response.status_code == 200:
                stats["weather_cached"] += 1
                print(f"  ✓ Cached weather for {destination}")
        except Exception as e:
            print(f"  ✗ Failed to cache weather for {destination}: {e}")
        
        time.sleep(0.2)  # Rate limiting


def print_progress(current: int, total: int, prefix: str = "Progress"):
    """Print a progress bar."""
    percentage = (current / total) * 100
    filled = int(percentage / 2)
    bar = "█" * filled + "░" * (50 - filled)
    print(f"\r{prefix}: |{bar}| {percentage:.1f}% ({current}/{total})", end="", flush=True)


def main():
    """Main seeding function."""
    print("=" * 70)
    print("🌱 MULTI-SERVICE DATA SEEDING")
    print("=" * 70)
    print(f"\nConfiguration:")
    print(f"  • Users: {NUM_USERS}")
    print(f"  • Itineraries per user: {NUM_ITINERARIES_PER_USER}")
    print(f"  • Locations per itinerary: {NUM_LOCATIONS_PER_ITINERARY}")
    print(f"  • Create Firebase users: {CREATE_FIREBASE_USERS}")
    print(f"  • Random seed: {SEED}")
    print(f"\nServices:")
    print(f"  • Itinerary Service: {ITINERARY_SERVICE_URL}")
    print(f"  • Comments & Likes Service: {COMMENTS_LIKES_SERVICE_URL}")
    print(f"  • Recommendation Service: {RECOMMENDATION_SERVICE_URL}")
    print(f"  • Weather Service: {WEATHER_SERVICE_URL}")
    print(f"  • Travel Warnings Service: {TRAVEL_WARNINGS_SERVICE_URL}")
    print("\n" + "=" * 70)
    
    start_time = time.time()
    
    # Step 1: Create users
    print("\n👥 Creating users...")
    for i in range(NUM_USERS):
        first_name = random.choice(FIRST_NAMES)
        last_name = random.choice(LAST_NAMES)
        name = f"{first_name} {last_name}"
        email = f"loadtest.{i}@example.com"
        
        # Create Firebase user if enabled
        if CREATE_FIREBASE_USERS:
            uid = create_firebase_user(email, FIREBASE_PASSWORD)
            if uid:
                stats["firebase_users_created"] += 1
        
        # Create user in itinerary service
        if create_user(i, name, email):
            created_users.append({"id": i, "name": name, "email": email})
        
        print_progress(i + 1, NUM_USERS, "Creating users")
        
        # Rate limiting
        if (i + 1) % 50 == 0:
            time.sleep(0.5)
    
    print("\n")
    
    # Save test users to file
    users_file = BASE_DIR / "test_users.json"
    with open(users_file, 'w') as f:
        json.dump(created_users, f, indent=2)
    print(f"✓ Saved {len(created_users)} test users to {users_file}")
    
    # Step 2: Create itineraries, locations, and social interactions
    print("\n📝 Creating itineraries and locations...")
    total_operations = NUM_USERS * NUM_ITINERARIES_PER_USER
    current_operation = 0
    
    for user in created_users:
        # Get bearer token for this user
        bearer_token = None
        if FIREBASE_API_KEY:
            bearer_token = get_firebase_token(user["email"], FIREBASE_PASSWORD)
        
        for _ in range(NUM_ITINERARIES_PER_USER):
            current_operation += 1
            
            # Create itinerary
            itinerary = create_itinerary(user["email"], bearer_token)
            if not itinerary:
                continue
            
            created_itineraries.append(itinerary)
            itinerary_id = itinerary.get("id")
            destination = itinerary.get("destination")
            
            # Record in graph database
            record_itinerary_in_graph(itinerary, bearer_token)
            
            # Create locations
            if itinerary_id and destination:
                for _ in range(NUM_LOCATIONS_PER_ITINERARY):
                    create_location(itinerary_id, destination, bearer_token)
            
            # Add random social interactions (20% of itineraries get likes/comments)
            if random.random() < 0.2 and itinerary_id:
                num_likes = random.randint(1, 10)
                num_comments = random.randint(0, 5)
                add_random_likes(itinerary_id, num_likes, bearer_token)
                add_random_comments(itinerary_id, num_comments, bearer_token)
            
            print_progress(current_operation, total_operations, "Creating content")
            
            # Rate limiting
            if current_operation % 20 == 0:
                time.sleep(0.3)
    
    print("\n")
    
    # Step 3: Cache weather data
    cache_weather_data()
    
    # Final statistics
    duration = time.time() - start_time
    
    print("\n" + "=" * 70)
    print("✅ SEEDING COMPLETE")
    print("=" * 70)
    print(f"\nStatistics:")
    print(f"  • Users created: {stats['users_created']}")
    if CREATE_FIREBASE_USERS:
        print(f"  • Firebase users created: {stats['firebase_users_created']}")
    print(f"  • Itineraries created: {stats['itineraries_created']}")
    print(f"  • Locations created: {stats['locations_created']}")
    print(f"  • Likes created: {stats['likes_created']}")
    print(f"  • Comments created: {stats['comments_created']}")
    print(f"  • Graph nodes created: {stats['graph_nodes_created']}")
    print(f"  • Weather data cached: {stats['weather_cached']}")
    print(f"  • Errors: {len(stats['errors'])}")
    print(f"\nDuration: {duration:.1f} seconds")
    
    if stats["errors"]:
        print(f"\n⚠️  Encountered {len(stats['errors'])} errors (first 10):")
        for error in stats["errors"][:10]:
            print(f"  • {error}")
    
    print("\n" + "=" * 70)
    print("Ready for load testing! 🚀")
    print("=" * 70)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n⚠️  Seeding interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n\n❌ Fatal error: {e}")
        sys.exit(1)

