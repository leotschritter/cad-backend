"""
Multi-Service Periodic Workload Test

Simulates normal daily user traffic across all microservices:
- Itinerary Service: Browse, search, create itineraries
- Comments & Likes Service: Social interactions
- Recommendation Service: Personalized feed, graph updates
- Weather Forecast Service: Weather lookups
- Travel Warnings Service: Travel advisory checks

This test represents typical user behavior with:
- Heavy read operations (browsing, searching)
- Moderate social interactions (likes, comments)
- Light content creation (new itineraries)
- Coordinated service calls (e.g., like + graph update)

Transaction Mix (weighted by @task decorators):
- 45% Browse/Search operations (itineraries, weather, warnings)
- 25% Social interactions (likes, comments)
- 15% Recommendation service (feed, graph)
- 10% Content creation (itineraries, locations)
- 5% User profile operations
"""

from locust import FastHttpUser, task, between, events, TaskSet
from urllib.parse import urlparse
from pathlib import Path
from dotenv import load_dotenv
import os
import random
import time
from datetime import datetime, timedelta
from auth_helper import get_bearer_token
from typing import Optional, List

BASE_DIR = Path(__file__).resolve().parent
load_dotenv(BASE_DIR / ".env.periodic")

# Service URLs
ITINERARY_SERVICE_URL = os.getenv("ITINERARY_SERVICE_URL", "http://localhost:8080").rstrip("/")
COMMENTS_LIKES_SERVICE_URL = os.getenv("COMMENTS_LIKES_SERVICE_URL", "http://localhost:8084").rstrip("/")
RECOMMENDATION_SERVICE_URL = os.getenv("RECOMMENDATION_SERVICE_URL", "http://localhost:8081").rstrip("/")
WEATHER_SERVICE_URL = os.getenv("WEATHER_SERVICE_URL", "http://localhost:8082").rstrip("/")
TRAVEL_WARNINGS_SERVICE_URL = os.getenv("TRAVEL_WARNINGS_SERVICE_URL", "http://localhost:8083").rstrip("/")

# Sample data pools
DESTINATIONS = [
    "Paris", "Tokyo", "New York", "London", "Barcelona", "Rome", "Dubai", "Singapore",
    "Sydney", "Amsterdam", "Berlin", "Prague", "Vienna", "Budapest", "Lisbon",
    "Copenhagen", "Stockholm", "Munich", "Venice", "Madrid", "Porto", "Athens"
]

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
}

USER_NAMES = [
    "Emma Smith", "Liam Johnson", "Olivia Williams", "Noah Brown", "Ava Jones",
    "Emma", "John", "Jane", "Michael", "Sarah", "David", "Lisa", "Robert", "Maria"
]

COMMENTS = [
    "This looks amazing!",
    "I've been there! Such a wonderful place.",
    "Adding this to my bucket list!",
    "Great itinerary, very well planned.",
    "Thanks for sharing this experience.",
    "Beautiful destination!",
    "Very helpful, thank you!",
    "Can't wait to try this!",
]

# Shared state
created_itinerary_ids: List[int] = []


class PeriodicWorkloadUser(FastHttpUser):
    """
    Simulates a typical user with periodic (normal daily) usage patterns.
    Tests all microservices with realistic transaction mix.
    """
    
    # Main host is itinerary service
    host = ITINERARY_SERVICE_URL
    
    # Realistic wait time between actions
    wait_time = between(1, 5)
    
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        
        # Get bearer token if authentication is enabled
        self.bearer_token = get_bearer_token()
        
        # Common headers
        self.default_headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
        }
        
        if self.bearer_token:
            self.default_headers["Authorization"] = f"Bearer {self.bearer_token}"
            print(f"🔐 Using authentication for user requests")
    
    def on_start(self):
        """Initialize user session."""
        # 70% use existing test user, 30% create new user
        if random.random() < 0.7:
            num_users = int(os.getenv("NUM_USERS", "1000"))
            user_id = random.randint(0, num_users - 1)
            self.user_email = f"loadtest.{user_id}@example.com"
            self.user_name = f"Test User {user_id}"
        else:
            timestamp = int(time.time())
            user_id = random.randint(10000, 99999)
            self.user_name = random.choice(USER_NAMES)
            self.user_email = f"{self.user_name.replace(' ', '.').lower()}.{user_id}@loadtest.example.com"
            
            # Register new user
            self.client.post(
                "/user/register",
                json={"name": self.user_name, "email": self.user_email},
                name="POST /user/register",
                headers=self.default_headers
            )
    
    # ========== BROWSE/SEARCH OPERATIONS (45% weight) ==========
    
    @task(15)
    def search_itineraries_by_destination(self):
        """Search for itineraries by destination."""
        destination = random.choice(DESTINATIONS)
        
        with self.client.post(
            "/itinerary/search",
            json={"destination": destination},
            catch_response=True,
            name="POST /itinerary/search [destination]",
            headers=self.default_headers
        ) as response:
            if response.status_code == 200:
                try:
                    results = response.json()
                    if isinstance(results, list) and len(results) > 0:
                        for item in results[:3]:
                            if 'id' in item and item['id'] not in created_itinerary_ids:
                                created_itinerary_ids.append(item['id'])
                    response.success()
                except:
                    response.success()
    
    @task(10)
    def browse_all_itineraries(self):
        """Browse all itineraries."""
        with self.client.post(
            "/itinerary/search",
            json={},
            catch_response=True,
            name="POST /itinerary/search [all]",
            headers=self.default_headers
        ) as response:
            if response.status_code == 200:
                try:
                    results = response.json()
                    if isinstance(results, list) and len(results) > 0:
                        for item in results[:5]:
                            if 'id' in item and item['id'] not in created_itinerary_ids:
                                created_itinerary_ids.append(item['id'])
                    response.success()
                except:
                    response.success()
    
    @task(8)
    def get_weather_for_destination(self):
        """Get weather forecast for a destination."""
        destination = random.choice(list(DESTINATION_COORDS.keys()))
        coords = DESTINATION_COORDS[destination]
        
        # Use a separate client for weather service
        import requests
        try:
            response = requests.get(
                f"{WEATHER_SERVICE_URL}/api/weather/forecast/coordinates",
                params={"lat": coords[0], "lon": coords[1]},
                timeout=5
            )
            
            # Log to Locust
            self.environment.events.request.fire(
                request_type="GET",
                name=f"[Weather] GET /api/weather/forecast/coordinates",
                response_time=(response.elapsed.total_seconds() * 1000),
                response_length=len(response.content),
                exception=None if response.status_code == 200 else Exception(f"Status {response.status_code}")
            )
        except Exception as e:
            self.environment.events.request.fire(
                request_type="GET",
                name=f"[Weather] GET /api/weather/forecast/coordinates",
                response_time=0,
                response_length=0,
                exception=e
            )
    
    @task(7)
    def get_travel_warnings(self):
        """Get travel warnings/advisories."""
        import requests
        try:
            response = requests.get(
                f"{TRAVEL_WARNINGS_SERVICE_URL}/travelwarning",
                timeout=5
            )
            
            self.environment.events.request.fire(
                request_type="GET",
                name=f"[TravelWarnings] GET /travelwarning",
                response_time=(response.elapsed.total_seconds() * 1000),
                response_length=len(response.content),
                exception=None if response.status_code == 200 else Exception(f"Status {response.status_code}")
            )
        except Exception as e:
            self.environment.events.request.fire(
                request_type="GET",
                name=f"[TravelWarnings] GET /travelwarning",
                response_time=0,
                response_length=0,
                exception=e
            )
    
    @task(5)
    def search_itineraries_by_date_range(self):
        """Search for itineraries by date range."""
        start_date = (datetime.now() + timedelta(days=random.randint(0, 90))).strftime("%Y-%m-%d")
        end_date = (datetime.now() + timedelta(days=random.randint(91, 180))).strftime("%Y-%m-%d")
        
        self.client.post(
            "/itinerary/search",
            json={"startDateFrom": start_date, "startDateTo": end_date},
            name="POST /itinerary/search [date range]",
            headers=self.default_headers
        )
    
    # ========== SOCIAL INTERACTIONS (25% weight) ==========
    
    @task(10)
    def view_itinerary_locations(self):
        """View locations for an itinerary."""
        if created_itinerary_ids:
            itinerary_id = random.choice(created_itinerary_ids)
            self.client.get(
                f"/location/itinerary/{itinerary_id}",
                name="GET /location/itinerary/:id",
                headers=self.default_headers
            )
    
    @task(6)
    def like_itinerary_with_graph_update(self):
        """Like an itinerary and update recommendation graph (coordinated operation)."""
        if not created_itinerary_ids:
            return
        
        itinerary_id = random.choice(created_itinerary_ids)
        
        # Step 1: Like in comments-likes service (UPDATED: now separate microservice)
        import requests
        headers = self.default_headers.copy()
        headers['Content-Length'] = '0'
        
        try:
            like_response = requests.post(
                f"{COMMENTS_LIKES_SERVICE_URL}/like/itinerary/{itinerary_id}",
                headers=headers,
                timeout=5
            )

            # Log to Locust
            self.environment.events.request.fire(
                request_type="POST",
                name=f"[CommentsLikes] POST /like/itinerary/:id",
                response_time=(like_response.elapsed.total_seconds() * 1000),
                response_length=len(like_response.content),
                exception=None if like_response.status_code in [200, 400] else Exception(f"Status {like_response.status_code}")
            )

            # Step 2: Update graph in recommendation service (coordinated)
            if like_response.status_code in [200, 400]:  # 400 = already liked, which is OK
                try:
                    graph_headers = {"Content-Type": "application/json"}
                    if self.bearer_token:
                        graph_headers["Authorization"] = f"Bearer {self.bearer_token}"
                    
                    graph_response = requests.post(
                        f"{RECOMMENDATION_SERVICE_URL}/graph/likes",
                        json={"itineraryId": itinerary_id},
                        headers=graph_headers,
                        timeout=5
                    )
                    
                    self.environment.events.request.fire(
                        request_type="POST",
                        name=f"[Recommendation] POST /graph/likes",
                        response_time=(graph_response.elapsed.total_seconds() * 1000),
                        response_length=len(graph_response.content),
                        exception=None if graph_response.status_code in [200, 201] else Exception(f"Status {graph_response.status_code}")
                    )
                except Exception as e:
                    self.environment.events.request.fire(
                        request_type="POST",
                        name=f"[Recommendation] POST /graph/likes",
                        response_time=0,
                        response_length=0,
                        exception=e
                    )
        except Exception as e:
            self.environment.events.request.fire(
                request_type="POST",
                name=f"[CommentsLikes] POST /like/itinerary/:id",
                response_time=0,
                response_length=0,
                exception=e
            )

    @task(5)
    def view_itinerary_comments(self):
        """View comments for an itinerary."""
        if created_itinerary_ids:
            itinerary_id = random.choice(created_itinerary_ids)
            self.client.get(
                f"/comment/itinerary/{itinerary_id}",
                name="GET /comment/itinerary/:id",
                headers=self.default_headers
            )
    
    @task(4)
    def add_comment_to_itinerary(self):
        """Add a comment to an itinerary."""
        if created_itinerary_ids:
            itinerary_id = random.choice(created_itinerary_ids)
            comment_data = {
                "comment": random.choice(COMMENTS)
            }
            # Use comments-likes service (UPDATED: now separate microservice)
            import requests
            try:
                response = requests.post(
                    f"{COMMENTS_LIKES_SERVICE_URL}/comment/itinerary/{itinerary_id}",
                    json=comment_data,
                    headers=self.default_headers,
                    timeout=5
                )

                self.environment.events.request.fire(
                    request_type="POST",
                    name=f"[CommentsLikes] POST /comment/itinerary/:id",
                    response_time=(response.elapsed.total_seconds() * 1000),
                    response_length=len(response.content),
                    exception=None if response.status_code in [200, 201] else Exception(f"Status {response.status_code}")
                )
            except Exception as e:
                self.environment.events.request.fire(
                    request_type="POST",
                    name=f"[CommentsLikes] POST /comment/itinerary/:id",
                    response_time=0,
                    response_length=0,
                    exception=e
                )

    # ========== RECOMMENDATION SERVICE (15% weight) ==========
    
    @task(10)
    def get_personalized_feed(self):
        """Get personalized recommendation feed."""
        import requests
        try:
            feed_headers = {"Accept": "application/json"}
            if self.bearer_token:
                feed_headers["Authorization"] = f"Bearer {self.bearer_token}"
            
            response = requests.get(
                f"{RECOMMENDATION_SERVICE_URL}/feed",
                headers=feed_headers,
                timeout=5
            )
            
            self.environment.events.request.fire(
                request_type="GET",
                name=f"[Recommendation] GET /feed",
                response_time=(response.elapsed.total_seconds() * 1000),
                response_length=len(response.content),
                exception=None if response.status_code == 200 else Exception(f"Status {response.status_code}")
            )
        except Exception as e:
            self.environment.events.request.fire(
                request_type="GET",
                name=f"[Recommendation] GET /feed",
                response_time=0,
                response_length=0,
                exception=e
            )
    
    @task(5)
    def get_popular_feed(self):
        """Get popular itineraries feed."""
        import requests
        try:
            feed_headers = {"Accept": "application/json"}
            if self.bearer_token:
                feed_headers["Authorization"] = f"Bearer {self.bearer_token}"
            
            response = requests.get(
                f"{RECOMMENDATION_SERVICE_URL}/feed/popular",
                headers=feed_headers,
                timeout=5
            )
            
            self.environment.events.request.fire(
                request_type="GET",
                name=f"[Recommendation] GET /feed/popular",
                response_time=(response.elapsed.total_seconds() * 1000),
                response_length=len(response.content),
                exception=None if response.status_code == 200 else Exception(f"Status {response.status_code}")
            )
        except Exception as e:
            self.environment.events.request.fire(
                request_type="GET",
                name=f"[Recommendation] GET /feed/popular",
                response_time=0,
                response_length=0,
                exception=e
            )
    
    # ========== CONTENT CREATION (10% weight) ==========
    
    @task(7)
    def create_itinerary_with_graph_record(self):
        """Create a new itinerary and record in recommendation graph."""
        destination = random.choice(DESTINATIONS)
        start_date = (datetime.now() + timedelta(days=random.randint(1, 180))).strftime("%Y-%m-%d")
        
        itinerary_data = {
            "title": f"{'Trip to' if random.random() > 0.5 else 'Visit to'} {destination}",
            "destination": destination,
            "startDate": start_date,
            "shortDescription": f"An amazing journey to {destination}",
            "detailedDescription": f"A comprehensive trip exploring the highlights of {destination}."
        }
        
        with self.client.post(
            "/itinerary/create",
            json=itinerary_data,
            catch_response=True,
            name="POST /itinerary/create",
            headers=self.default_headers
        ) as response:
            if response.status_code in [200, 201]:
                try:
                    result = response.json()
                    itinerary_id = result.get('id')
                    
                    if itinerary_id:
                        created_itinerary_ids.append(itinerary_id)
                        
                        # Record in recommendation graph (coordinated operation)
                        import requests
                        try:
                            graph_headers = {"Content-Type": "application/json"}
                            if self.bearer_token:
                                graph_headers["Authorization"] = f"Bearer {self.bearer_token}"
                            
                            graph_data = {
                                "itineraryId": itinerary_id,
                                "title": itinerary_data["title"],
                                "description": itinerary_data["shortDescription"],
                                "locationNames": [],
                                "likesCount": 0,
                                "eventType": "CREATED"
                            }
                            
                            graph_response = requests.post(
                                f"{RECOMMENDATION_SERVICE_URL}/graph/itineraries",
                                json=graph_data,
                                headers=graph_headers,
                                timeout=5
                            )
                            
                            self.environment.events.request.fire(
                                request_type="POST",
                                name=f"[Recommendation] POST /graph/itineraries",
                                response_time=(graph_response.elapsed.total_seconds() * 1000),
                                response_length=len(graph_response.content),
                                exception=None if graph_response.status_code in [200, 201] else Exception(f"Status {graph_response.status_code}")
                            )
                        except Exception as e:
                            self.environment.events.request.fire(
                                request_type="POST",
                                name=f"[Recommendation] POST /graph/itineraries",
                                response_time=0,
                                response_length=0,
                                exception=e
                            )
                    
                    response.success()
                except:
                    response.success()
    
    @task(3)
    def create_location_for_itinerary(self):
        """Add a location to an existing itinerary."""
        if not created_itinerary_ids:
            return
        
        itinerary_id = random.choice(created_itinerary_ids)
        destination = random.choice(DESTINATIONS)
        coords = DESTINATION_COORDS.get(destination, (0.0, 0.0))
        
        from_date = (datetime.now() + timedelta(days=random.randint(1, 30))).strftime("%Y-%m-%d")
        to_date = (datetime.now() + timedelta(days=random.randint(31, 37))).strftime("%Y-%m-%d")
        
        location_data = {
            "name": f"Location in {destination}",
            "description": "A wonderful place to visit",
            "latitude": coords[0] + random.uniform(-0.1, 0.1),
            "longitude": coords[1] + random.uniform(-0.1, 0.1),
            "fromDate": from_date,
            "toDate": to_date
        }
        
        self.client.post(
            f"/location/itinerary/{itinerary_id}",
            json=location_data,
            name="POST /location/itinerary/:id",
            headers=self.default_headers
        )

