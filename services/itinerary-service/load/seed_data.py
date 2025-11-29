#!/usr/bin/env python3
"""
Initial data seeding script for load testing.

This script populates the application with test data including:
- Users
- Itineraries
- Locations with transport and accommodation
- Likes and comments

The data is seeded with random variations to avoid caching issues.
"""

import requests
import random
import time
from datetime import datetime, timedelta
from pathlib import Path
from dotenv import load_dotenv
import os
import sys

# Load environment variables
BASE_DIR = Path(__file__).resolve().parent
load_dotenv(BASE_DIR / ".env.seed")

# Configuration
BASE_URL = os.getenv("LOCUST_HOST", "http://localhost:8080")
NUM_USERS = int(os.getenv("NUM_USERS", "50"))
NUM_ITINERARIES_PER_USER = int(os.getenv("NUM_ITINERARIES_PER_USER", "3"))
NUM_LOCATIONS_PER_ITINERARY = int(os.getenv("NUM_LOCATIONS_PER_ITINERARY", "2"))
SEED = int(os.getenv("RANDOM_SEED", str(int(time.time()))))

# Set random seed for reproducibility
random.seed(SEED)

# Sample data for generating varied content
FIRST_NAMES = [
    "Emma", "Liam", "Olivia", "Noah", "Ava", "Ethan", "Sophia", "Mason", "Isabella", "William",
    "Mia", "James", "Charlotte", "Benjamin", "Amelia", "Lucas", "Harper", "Henry", "Evelyn", "Alexander",
    "Abigail", "Michael", "Emily", "Daniel", "Elizabeth", "Matthew", "Sofia", "Jackson", "Avery", "David"
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
    "Mexico City", "Toronto", "Vancouver", "Montreal", "San Francisco", "Los Angeles", "Miami", "Chicago", "Boston"
]

TRIP_TYPES = [
    "Family Trip to", "Business Trip to", "Romantic Getaway in", "Solo Adventure in", "Weekend in",
    "Summer Vacation in", "Winter Holiday in", "Backpacking through", "Luxury Tour of", "Cultural Exploration of"
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
    "Wandering through charming streets"
]

DESCRIPTIONS_DETAILED = [
    "An amazing journey that combines culture, history, and modern attractions. We'll visit famous landmarks, try local cuisine, and immerse ourselves in the vibrant atmosphere of this incredible destination.",
    "This trip promises to be unforgettable with carefully planned visits to must-see attractions, hidden local gems, and authentic experiences that showcase the true spirit of the place.",
    "A comprehensive itinerary that balances sightseeing with leisure time, allowing us to explore at our own pace while ensuring we don't miss the highlights that make this destination special.",
    "From historic monuments to contemporary art scenes, this adventure will take us through the diverse facets of the destination, offering insights into both its past and present.",
    "An carefully curated experience featuring the best accommodations, dining options, and activities that will make this trip truly memorable and worth every moment."
]

LOCATION_NAMES = [
    "City Center", "Historic District", "Waterfront Area", "Old Town", "Modern Quarter",
    "Museum District", "Shopping Area", "Beach Resort", "Mountain Lodge", "Cultural Center",
    "Financial District", "Arts Quarter", "Riverside Walk", "Park Area", "Entertainment Zone"
]

LOCATION_DESCRIPTIONS = [
    "A vibrant area filled with local shops, restaurants, and attractions.",
    "Historic buildings and charming streets full of character.",
    "Beautiful views and plenty of activities for everyone.",
    "A must-visit spot known for its unique atmosphere.",
    "The perfect place to relax and enjoy the surroundings.",
    "Rich in history and cultural significance.",
    "Modern architecture and world-class facilities.",
    "Breathtaking scenery and natural beauty."
]

TRANSPORT_TYPES = ["Flight", "Train", "Bus", "Car", "Ferry", "Subway"]

ACCOMMODATION_NAMES = [
    "Grand Hotel", "Boutique Inn", "Luxury Resort", "Cozy B&B", "Modern Apartment",
    "Historic Manor", "Seaside Villa", "Mountain Chalet", "Downtown Loft", "Garden Suite"
]

COMMENTS = [
    "This looks amazing! Can't wait to visit someday.",
    "I've been there! Such a wonderful place.",
    "Adding this to my bucket list!",
    "Great itinerary, very well planned.",
    "The photos are stunning!",
    "Thanks for sharing this experience.",
    "How long did you stay there?",
    "Would love to hear more about your trip!",
    "This is exactly the kind of trip I've been dreaming about.",
    "Incredible destination! Hope to visit soon."
]

# Statistics
stats = {
    "users_created": 0,
    "itineraries_created": 0,
    "locations_created": 0,
    "likes_created": 0,
    "comments_created": 0,
    "errors": []
}


def generate_email(first_name, last_name, index):
    """Generate a unique email address."""
    timestamp = int(time.time())
    return f"{first_name.lower()}.{last_name.lower()}.{index}.{timestamp}@loadtest.example.com"


def generate_user_data(index):
    """Generate user registration data."""
    first_name = random.choice(FIRST_NAMES)
    last_name = random.choice(LAST_NAMES)
    email = generate_email(first_name, last_name, index)
    name = f"{first_name} {last_name}"
    
    return {
        "name": name,
        "email": email
    }


def generate_itinerary_data():
    """Generate itinerary data."""
    destination = random.choice(DESTINATIONS)
    trip_type = random.choice(TRIP_TYPES)
    
    # Generate random start date between 30 days ago and 180 days in the future
    days_offset = random.randint(-30, 180)
    start_date = (datetime.now() + timedelta(days=days_offset)).strftime("%Y-%m-%d")
    
    return {
        "title": f"{trip_type} {destination}",
        "destination": destination,
        "startDate": start_date,
        "shortDescription": f"{random.choice(DESCRIPTIONS_SHORT)} in {destination}",
        "detailedDescription": random.choice(DESCRIPTIONS_DETAILED)
    }


def generate_location_data():
    """Generate location data for multipart form."""
    # Random dates
    days_from = random.randint(0, 10)
    days_to = days_from + random.randint(1, 5)
    
    from_date = (datetime.now() + timedelta(days=days_from)).strftime("%Y-%m-%d")
    to_date = (datetime.now() + timedelta(days=days_to)).strftime("%Y-%m-%d")
    
    location_data = {
        "name": random.choice(LOCATION_NAMES),
        "description": random.choice(LOCATION_DESCRIPTIONS),
        "fromDate": from_date,
        "toDate": to_date,
    }
    
    # Add transport (50% chance)
    if random.random() > 0.5:
        location_data.update({
            "transportType": random.choice(TRANSPORT_TYPES),
            "transportDuration": str(random.randint(30, 360)),
            "transportDistance": str(random.randint(10, 1000))
        })
    
    # Add accommodation (70% chance)
    if random.random() > 0.3:
        location_data.update({
            "accommodationName": f"{random.choice(ACCOMMODATION_NAMES)} - {random.choice(LOCATION_NAMES)}",
            "accommodationPricePerNight": str(round(random.uniform(50, 500), 2)),
            "accommodationRating": str(round(random.uniform(3.0, 5.0), 1)),
            "accommodationNotes": "Comfortable stay with great amenities",
            "accommodationImageUrl": f"https://example.com/images/accommodation-{random.randint(1, 100)}.jpg",
            "bookingPageUrl": f"https://booking.example.com/hotel-{random.randint(1000, 9999)}"
        })
    
    return location_data


def register_user(user_data):
    """Register a new user."""
    try:
        response = requests.post(
            f"{BASE_URL}/user/register",
            json=user_data,
            timeout=10
        )
        if response.status_code in [200, 201]:
            stats["users_created"] += 1
            return user_data
        else:
            error_msg = f"Failed to register user {user_data['email']}: {response.status_code} - {response.text[:100]}"
            stats["errors"].append(error_msg)
            print(f"  ❌ {error_msg}")
            return None
    except Exception as e:
        error_msg = f"Exception registering user {user_data['email']}: {str(e)}"
        stats["errors"].append(error_msg)
        print(f"  ❌ {error_msg}")
        return None


def create_itinerary(user_email, itinerary_data):
    """Create an itinerary for a user."""
    try:
        # Note: The API uses authenticated user's email from the token
        # For testing without auth, we might need to use a different endpoint
        response = requests.post(
            f"{BASE_URL}/itinerary/create",
            json=itinerary_data,
            timeout=10
        )
        
        if response.status_code in [200, 201]:
            stats["itineraries_created"] += 1
            # Try to extract itinerary ID from response
            try:
                return response.json().get("id")
            except:
                # If we can't get ID from response, we'll need to fetch itineraries
                return None
        else:
            error_msg = f"Failed to create itinerary for {user_email}: {response.status_code} - {response.text[:100]}"
            stats["errors"].append(error_msg)
            print(f"    ❌ {error_msg}")
            return None
    except Exception as e:
        error_msg = f"Exception creating itinerary for {user_email}: {str(e)}"
        stats["errors"].append(error_msg)
        print(f"    ❌ {error_msg}")
        return None


def add_location_to_itinerary(itinerary_id, location_data):
    """Add a location to an itinerary."""
    try:
        response = requests.post(
            f"{BASE_URL}/location/itinerary/{itinerary_id}",
            data=location_data,
            timeout=10
        )
        
        if response.status_code in [200, 201]:
            stats["locations_created"] += 1
            return True
        else:
            error_msg = f"Failed to add location to itinerary {itinerary_id}: {response.status_code}"
            stats["errors"].append(error_msg)
            print(f"      ❌ {error_msg}")
            return False
    except Exception as e:
        error_msg = f"Exception adding location to itinerary {itinerary_id}: {str(e)}"
        stats["errors"].append(error_msg)
        print(f"      ❌ {error_msg}")
        return False


def add_like_to_itinerary(itinerary_id):
    """Add a like to an itinerary."""
    try:
        response = requests.post(
            f"{BASE_URL}/like/itinerary/{itinerary_id}",
            timeout=10
        )
        
        if response.status_code in [200, 201]:
            stats["likes_created"] += 1
            return True
        else:
            return False
    except Exception as e:
        return False


def add_comment_to_itinerary(itinerary_id, user_email):
    """Add a comment to an itinerary."""
    try:
        comment_data = {
            "userEmail": user_email,
            "comment": random.choice(COMMENTS)
        }
        
        response = requests.post(
            f"{BASE_URL}/comment/itinerary/{itinerary_id}",
            json=comment_data,
            timeout=10
        )
        
        if response.status_code in [200, 201]:
            stats["comments_created"] += 1
            return True
        else:
            return False
    except Exception as e:
        return False


def main():
    """Main execution function."""
    print("=" * 80)
    print("🌱 Starting Data Seeding Process")
    print("=" * 80)
    print(f"Base URL: {BASE_URL}")
    print(f"Number of Users: {NUM_USERS}")
    print(f"Itineraries per User: {NUM_ITINERARIES_PER_USER}")
    print(f"Locations per Itinerary: {NUM_LOCATIONS_PER_ITINERARY}")
    print(f"Random Seed: {SEED}")
    print("=" * 80)
    print()
    
    all_users = []
    all_itinerary_ids = []
    
    # Step 1: Create users and their itineraries
    print("📝 Step 1: Creating users and itineraries...")
    for i in range(NUM_USERS):
        user_data = generate_user_data(i)
        print(f"  Creating user {i+1}/{NUM_USERS}: {user_data['name']} ({user_data['email']})")
        
        user = register_user(user_data)
        if user:
            all_users.append(user)
            
            # Create itineraries for this user
            for j in range(NUM_ITINERARIES_PER_USER):
                itinerary_data = generate_itinerary_data()
                print(f"    Creating itinerary {j+1}/{NUM_ITINERARIES_PER_USER}: {itinerary_data['title']}")
                
                itinerary_id = create_itinerary(user['email'], itinerary_data)
                if itinerary_id:
                    all_itinerary_ids.append(itinerary_id)
                    
                    # Add locations to this itinerary
                    for k in range(NUM_LOCATIONS_PER_ITINERARY):
                        location_data = generate_location_data()
                        print(f"      Adding location {k+1}/{NUM_LOCATIONS_PER_ITINERARY}: {location_data['name']}")
                        add_location_to_itinerary(itinerary_id, location_data)
        
        # Small delay to avoid overwhelming the server
        time.sleep(0.1)
    
    print()
    
    # Step 2: Add social interactions (likes and comments)
    if all_itinerary_ids and all_users:
        print("💬 Step 2: Adding social interactions (likes and comments)...")
        
        # Each user likes 20-40% of random itineraries
        for user in all_users:
            num_likes = random.randint(int(len(all_itinerary_ids) * 0.2), int(len(all_itinerary_ids) * 0.4))
            liked_itineraries = random.sample(all_itinerary_ids, min(num_likes, len(all_itinerary_ids)))
            
            for itinerary_id in liked_itineraries:
                add_like_to_itinerary(itinerary_id)
            
            # Add comments to 10-20% of random itineraries
            num_comments = random.randint(int(len(all_itinerary_ids) * 0.1), int(len(all_itinerary_ids) * 0.2))
            commented_itineraries = random.sample(all_itinerary_ids, min(num_comments, len(all_itinerary_ids)))
            
            for itinerary_id in commented_itineraries:
                add_comment_to_itinerary(itinerary_id, user['email'])
            
            time.sleep(0.05)
        
        print(f"  ✅ Social interactions added")
    
    print()
    print("=" * 80)
    print("📊 Seeding Complete - Summary")
    print("=" * 80)
    print(f"✅ Users created: {stats['users_created']}")
    print(f"✅ Itineraries created: {stats['itineraries_created']}")
    print(f"✅ Locations created: {stats['locations_created']}")
    print(f"✅ Likes created: {stats['likes_created']}")
    print(f"✅ Comments created: {stats['comments_created']}")
    print(f"❌ Errors: {len(stats['errors'])}")
    
    if stats['errors']:
        print("\n⚠️  First 5 errors:")
        for error in stats['errors'][:5]:
            print(f"  - {error}")
    
    print("=" * 80)
    
    # Return exit code based on success
    if stats['users_created'] == 0:
        print("\n❌ Critical: No users were created. Check your server connection and configuration.")
        sys.exit(1)
    elif len(stats['errors']) > stats['users_created']:
        print("\n⚠️  Warning: More errors than successes. Please review the logs.")
        sys.exit(1)
    else:
        print("\n✅ Seeding completed successfully!")
        sys.exit(0)


if __name__ == "__main__":
    main()

