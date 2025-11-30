"""
Multi-Service Once-in-a-Lifetime Workload Test

Simulates traffic spikes/viral scenarios across all microservices:
- Heavy concentration on popular content (hot spots)
- Burst of social interactions (likes, comments)
- High read load on specific itineraries
- Recommendation service under heavy load
- Weather lookups for trending destinations

This test represents unusual load patterns such as:
- Viral content being shared
- Featured article or influencer post
- Major event driving traffic
- Flash sale or promotion

Transaction Mix (weighted by @task decorators):
- 65% Heavy reads on popular content
- 20% Burst social interactions (likes/comments on viral content)
- 10% Discovery/search for trending content
- 5% Content creation (new users joining)

Characteristics:
- Shorter wait times (faster user actions)
- High concentration on "hot" itineraries
- More new users (viral traffic)
- Lower diversity in content accessed
"""

from locust import FastHttpUser, task, between, events
from urllib.parse import urlparse
from pathlib import Path
from dotenv import load_dotenv
import os
import random
import time
from datetime import datetime, timedelta
from auth_helper import get_bearer_token
from typing import List

BASE_DIR = Path(__file__).resolve().parent
load_dotenv(BASE_DIR / ".env.onceinlifetime")

# Service URLs
ITINERARY_SERVICE_URL = os.getenv("ITINERARY_SERVICE_URL", "http://localhost:8080").rstrip("/")
COMMENTS_LIKES_SERVICE_URL = os.getenv("COMMENTS_LIKES_SERVICE_URL", "http://localhost:8084").rstrip("/")
RECOMMENDATION_SERVICE_URL = os.getenv("RECOMMENDATION_SERVICE_URL", "http://localhost:8081").rstrip("/")
WEATHER_SERVICE_URL = os.getenv("WEATHER_SERVICE_URL", "http://localhost:8082").rstrip("/")
TRAVEL_WARNINGS_SERVICE_URL = os.getenv("TRAVEL_WARNINGS_SERVICE_URL", "http://localhost:8083").rstrip("/")

# Popular destinations for viral content
DESTINATIONS_POPULAR = [
    "Paris", "Tokyo", "Bali", "Maldives", "Santorini",
    "Iceland", "Dubai", "New York", "Barcelona", "Rome"
]

DESTINATION_COORDS = {
    "Paris": (48.8566, 2.3522),
    "Tokyo": (35.6762, 139.6503),
    "Bali": (-8.3405, 115.0920),
    "Dubai": (25.2048, 55.2708),
    "New York": (40.7128, -74.0060),
    "Barcelona": (41.3851, 2.1734),
}

QUICK_COMMENTS = [
    "Wow!",
    "Amazing!",
    "😍",
    "This is incredible!",
    "I need to go here!",
    "Unbelievable!",
    "Added to my list!",
    "Goals!",
    "Dream destination!",
]

# Shared state for hot content
hot_itinerary_ids: List[int] = []
all_itinerary_ids: List[int] = []


class OnceInLifetimeUser(FastHttpUser):
    """
    Simulates a user during a traffic spike/viral event.
    
    Characteristics:
    - Faster actions (shorter wait time)
    - Focused on popular content
    - Quick social interactions
    - More likely to be a new user
    """
    
    host = ITINERARY_SERVICE_URL
    
    # Shorter wait time for excited, quick-browsing users
    wait_time = between(0.5, 2)
    
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        
        self.bearer_token = get_bearer_token()
        
        self.default_headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
        }
        
        if self.bearer_token:
            self.default_headers["Authorization"] = f"Bearer {self.bearer_token}"
    
    def on_start(self):
        """Initialize user - most are new users during viral events."""
        # 80% are new users during viral traffic
        if random.random() < 0.8:
            timestamp = int(time.time())
            user_id = random.randint(100000, 999999)
            self.user_name = f"Viral User {user_id}"
            self.user_email = f"viral.user.{user_id}.{timestamp}@example.com"
            
            # Quick registration
            try:
                self.client.post(
                    "/user/register",
                    json={"name": self.user_name, "email": self.user_email},
                    name="POST /user/register",
                    headers=self.default_headers,
                    timeout=5
                )
            except:
                pass
        else:
            # Existing test user
            num_users = int(os.getenv("NUM_USERS", "1000"))
            user_id = random.randint(0, num_users - 1)
            self.user_email = f"loadtest.{user_id}@example.com"
            self.user_name = f"Test User {user_id}"
        
        # Discover hot content
        self._discover_hot_content()
    
    def _discover_hot_content(self):
        """Discover popular/viral content at session start."""
        global hot_itinerary_ids, all_itinerary_ids
        
        destination = random.choice(DESTINATIONS_POPULAR)
        try:
            response = self.client.post(
                "/itinerary/search",
                json={"destination": destination},
                name="POST /itinerary/search [discover hot]",
                headers=self.default_headers,
                timeout=5
            )
            
            if response.status_code == 200:
                results = response.json()
                if isinstance(results, list) and len(results) > 0:
                    for item in results[:2]:
                        if 'id' in item:
                            if item['id'] not in hot_itinerary_ids:
                                hot_itinerary_ids.append(item['id'])
                            if item['id'] not in all_itinerary_ids:
                                all_itinerary_ids.append(item['id'])
        except:
            pass
    
    def _get_target_itinerary_id(self, hot_bias=0.9):
        """Get an itinerary ID with bias towards hot content."""
        if hot_itinerary_ids and random.random() < hot_bias:
            return random.choice(hot_itinerary_ids)
        elif all_itinerary_ids:
            return random.choice(all_itinerary_ids)
        else:
            return random.randint(1, 100)
    
    # ========== HEAVY READ OPERATIONS (65% weight) ==========
    
    @task(25)
    def view_viral_itinerary_locations(self):
        """View locations for viral/popular itinerary (hot content)."""
        itinerary_id = self._get_target_itinerary_id(hot_bias=0.95)
        
        if itinerary_id:
            self.client.get(
                f"/location/itinerary/{itinerary_id}",
                name="GET /location/itinerary/:id [HOT]",
                headers=self.default_headers
            )
    
    @task(15)
    def view_viral_itinerary_comments(self):
        """View comments for popular content."""
        itinerary_id = self._get_target_itinerary_id(hot_bias=0.95)
        
        if itinerary_id:
            # Use comments-likes service (UPDATED: now separate microservice)
            import requests
            try:
                response = requests.get(
                    f"{COMMENTS_LIKES_SERVICE_URL}/comment/itinerary/{itinerary_id}",
                    headers=self.default_headers,
                    timeout=3
                )

                self.environment.events.request.fire(
                    request_type="GET",
                    name=f"[CommentsLikes] GET /comment/itinerary/:id [HOT]",
                    response_time=(response.elapsed.total_seconds() * 1000),
                    response_length=len(response.content),
                    exception=None if response.status_code == 200 else Exception(f"Status {response.status_code}")
                )
            except Exception as e:
                self.environment.events.request.fire(
                    request_type="GET",
                    name=f"[CommentsLikes] GET /comment/itinerary/:id [HOT]",
                    response_time=0,
                    response_length=0,
                    exception=e
                )

    @task(10)
    def view_viral_itinerary_likes(self):
        """View like count for popular content."""
        itinerary_id = self._get_target_itinerary_id(hot_bias=0.95)
        
        if itinerary_id:
            # Use comments-likes service (UPDATED: now separate microservice)
            import requests
            try:
                response = requests.get(
                    f"{COMMENTS_LIKES_SERVICE_URL}/like/itinerary/{itinerary_id}",
                    headers=self.default_headers,
                    timeout=3
                )

                self.environment.events.request.fire(
                    request_type="GET",
                    name=f"[CommentsLikes] GET /like/itinerary/:id [HOT]",
                    response_time=(response.elapsed.total_seconds() * 1000),
                    response_length=len(response.content),
                    exception=None if response.status_code == 200 else Exception(f"Status {response.status_code}")
                )
            except Exception as e:
                self.environment.events.request.fire(
                    request_type="GET",
                    name=f"[CommentsLikes] GET /like/itinerary/:id [HOT]",
                    response_time=0,
                    response_length=0,
                    exception=e
                )

    @task(10)
    def get_weather_for_trending_destination(self):
        """Get weather for trending destination."""
        destination = random.choice(list(DESTINATION_COORDS.keys()))
        coords = DESTINATION_COORDS[destination]
        
        import requests
        try:
            response = requests.get(
                f"{WEATHER_SERVICE_URL}/api/weather/forecast/coordinates",
                params={"lat": coords[0], "lon": coords[1]},
                timeout=3
            )
            
            self.environment.events.request.fire(
                request_type="GET",
                name=f"[Weather] GET /api/weather/forecast/coordinates [VIRAL]",
                response_time=(response.elapsed.total_seconds() * 1000),
                response_length=len(response.content),
                exception=None if response.status_code == 200 else Exception(f"Status {response.status_code}")
            )
        except Exception as e:
            self.environment.events.request.fire(
                request_type="GET",
                name=f"[Weather] GET /api/weather/forecast/coordinates [VIRAL]",
                response_time=0,
                response_length=0,
                exception=e
            )
    
    @task(5)
    def get_popular_feed_viral(self):
        """Get popular feed during viral event."""
        import requests
        try:
            feed_headers = {"Accept": "application/json"}
            if self.bearer_token:
                feed_headers["Authorization"] = f"Bearer {self.bearer_token}"
            
            response = requests.get(
                f"{RECOMMENDATION_SERVICE_URL}/feed/popular",
                headers=feed_headers,
                timeout=3
            )
            
            self.environment.events.request.fire(
                request_type="GET",
                name=f"[Recommendation] GET /feed/popular [VIRAL]",
                response_time=(response.elapsed.total_seconds() * 1000),
                response_length=len(response.content),
                exception=None if response.status_code == 200 else Exception(f"Status {response.status_code}")
            )
        except Exception as e:
            self.environment.events.request.fire(
                request_type="GET",
                name=f"[Recommendation] GET /feed/popular [VIRAL]",
                response_time=0,
                response_length=0,
                exception=e
            )
    
    # ========== BURST SOCIAL INTERACTIONS (20% weight) ==========
    
    @task(12)
    def like_viral_content_burst(self):
        """Like popular content (burst of likes)."""
        itinerary_id = self._get_target_itinerary_id(hot_bias=0.98)
        
        if not itinerary_id:
            return
        
        # Use comments-likes service (UPDATED: now separate microservice)
        import requests
        headers = self.default_headers.copy()
        headers['Content-Length'] = '0'
        
        try:
            like_response = requests.post(
                f"{COMMENTS_LIKES_SERVICE_URL}/like/itinerary/{itinerary_id}",
                headers=headers,
                timeout=3
            )

            # Log to Locust
            self.environment.events.request.fire(
                request_type="POST",
                name=f"[CommentsLikes] POST /like/itinerary/:id [BURST]",
                response_time=(like_response.elapsed.total_seconds() * 1000),
                response_length=len(like_response.content),
                exception=None if like_response.status_code in [200, 400] else Exception(f"Status {like_response.status_code}")
            )

            # Update graph (coordinated operation)
            if like_response.status_code in [200, 400]:  # 400 = already liked, which is OK
                try:
                    graph_headers = {"Content-Type": "application/json"}
                    if self.bearer_token:
                        graph_headers["Authorization"] = f"Bearer {self.bearer_token}"

                    graph_response = requests.post(
                        f"{RECOMMENDATION_SERVICE_URL}/graph/likes",
                        json={"itineraryId": itinerary_id},
                        headers=graph_headers,
                        timeout=3
                    )

                    self.environment.events.request.fire(
                        request_type="POST",
                        name=f"[Recommendation] POST /graph/likes [BURST]",
                        response_time=(graph_response.elapsed.total_seconds() * 1000),
                        response_length=len(graph_response.content),
                        exception=None if graph_response.status_code in [200, 201] else Exception(f"Status {graph_response.status_code}")
                    )
                except Exception as e:
                    self.environment.events.request.fire(
                        request_type="POST",
                        name=f"[Recommendation] POST /graph/likes [BURST]",
                        response_time=0,
                        response_length=0,
                        exception=e
                    )
        except Exception as e:
            self.environment.events.request.fire(
                request_type="POST",
                name=f"[CommentsLikes] POST /like/itinerary/:id [BURST]",
                response_time=0,
                response_length=0,
                exception=e
            )
                            request_type="POST",
                            name=f"[Recommendation] POST /graph/likes [BURST]",
                            response_time=0,
                            response_length=0,
                            exception=e
                        )
        except:
            pass
    
    @task(8)
    def comment_on_viral_content(self):
        """Quick comment on popular content."""
        itinerary_id = self._get_target_itinerary_id(hot_bias=0.98)
        
        if not itinerary_id:
            return
        
        comment_data = {
            "comment": random.choice(QUICK_COMMENTS)
        }
        
        # Use comments-likes service (UPDATED: now separate microservice)
        import requests
        try:
            response = requests.post(
                f"{COMMENTS_LIKES_SERVICE_URL}/comment/itinerary/{itinerary_id}",
                json=comment_data,
                headers=self.default_headers,
                timeout=3
            )

            self.environment.events.request.fire(
                request_type="POST",
                name=f"[CommentsLikes] POST /comment/itinerary/:id [BURST]",
                response_time=(response.elapsed.total_seconds() * 1000),
                response_length=len(response.content),
                exception=None if response.status_code in [200, 201] else Exception(f"Status {response.status_code}")
            )
        except Exception as e:
            self.environment.events.request.fire(
                request_type="POST",
                name=f"[CommentsLikes] POST /comment/itinerary/:id [BURST]",
                response_time=0,
                response_length=0,
                exception=e
            )

    # ========== DISCOVERY/SEARCH (10% weight) ==========
    
    @task(6)
    def search_trending_destination(self):
        """Search for trending destinations."""
        destination = random.choice(DESTINATIONS_POPULAR)
        
        with self.client.post(
            "/itinerary/search",
            json={"destination": destination},
            catch_response=True,
            name="POST /itinerary/search [trending]",
            headers=self.default_headers
        ) as response:
            if response.status_code == 200:
                try:
                    results = response.json()
                    if isinstance(results, list) and len(results) > 0:
                        for item in results[:3]:
                            if 'id' in item:
                                if item['id'] not in hot_itinerary_ids:
                                    hot_itinerary_ids.append(item['id'])
                                if item['id'] not in all_itinerary_ids:
                                    all_itinerary_ids.append(item['id'])
                    response.success()
                except:
                    response.success()
    
    @task(4)
    def browse_discover_more(self):
        """Browse to discover more content."""
        with self.client.post(
            "/itinerary/search",
            json={},
            catch_response=True,
            name="POST /itinerary/search [discover]",
            headers=self.default_headers
        ) as response:
            if response.status_code == 200:
                try:
                    results = response.json()
                    if isinstance(results, list) and len(results) > 0:
                        for item in results[:5]:
                            if 'id' in item and item['id'] not in all_itinerary_ids:
                                all_itinerary_ids.append(item['id'])
                    response.success()
                except:
                    response.success()
    
    # ========== CONTENT CREATION (5% weight) ==========
    
    @task(3)
    def create_itinerary_inspired(self):
        """Create a new itinerary inspired by viral content."""
        destination = random.choice(DESTINATIONS_POPULAR)
        start_date = (datetime.now() + timedelta(days=random.randint(1, 90))).strftime("%Y-%m-%d")
        
        itinerary_data = {
            "title": f"My Trip to {destination}",
            "destination": destination,
            "startDate": start_date,
            "shortDescription": f"Inspired by viral content about {destination}",
            "detailedDescription": f"Planning my own adventure to {destination} after seeing it trending!"
        }
        
        with self.client.post(
            "/itinerary/create",
            json=itinerary_data,
            catch_response=True,
            name="POST /itinerary/create [viral-inspired]",
            headers=self.default_headers
        ) as response:
            if response.status_code in [200, 201]:
                try:
                    result = response.json()
                    itinerary_id = result.get('id')
                    
                    if itinerary_id:
                        all_itinerary_ids.append(itinerary_id)
                        
                        # Record in graph
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
                                timeout=3
                            )
                            
                            self.environment.events.request.fire(
                                request_type="POST",
                                name=f"[Recommendation] POST /graph/itineraries [viral]",
                                response_time=(graph_response.elapsed.total_seconds() * 1000),
                                response_length=len(graph_response.content),
                                exception=None if graph_response.status_code in [200, 201] else Exception(f"Status {graph_response.status_code}")
                            )
                        except Exception as e:
                            self.environment.events.request.fire(
                                request_type="POST",
                                name=f"[Recommendation] POST /graph/itineraries [viral]",
                                response_time=0,
                                response_length=0,
                                exception=e
                            )
                    
                    response.success()
                except:
                    response.success()
    
    @task(2)
    def get_travel_warnings_viral(self):
        """Check travel warnings for trending destination."""
        import requests
        try:
            response = requests.get(
                f"{TRAVEL_WARNINGS_SERVICE_URL}/travelwarning",
                timeout=3
            )
            
            self.environment.events.request.fire(
                request_type="GET",
                name=f"[TravelWarnings] GET /travelwarning [VIRAL]",
                response_time=(response.elapsed.total_seconds() * 1000),
                response_length=len(response.content),
                exception=None if response.status_code == 200 else Exception(f"Status {response.status_code}")
            )
        except Exception as e:
            self.environment.events.request.fire(
                request_type="GET",
                name=f"[TravelWarnings] GET /travelwarning [VIRAL]",
                response_time=0,
                response_length=0,
                exception=e
            )

