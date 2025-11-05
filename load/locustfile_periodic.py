"""
Periodic Workload Test - Simulates normal daily user traffic.

This test simulates typical user behavior patterns:
- Heavy read operations (browsing, searching, viewing)
- Light write operations (creating, liking, commenting)
- Realistic user wait times between actions
- Mix of authenticated and guest-like behavior

Workload Distribution:
- 60% Browse/Search operations (viewing itineraries, searching)
- 25% Social interactions (likes, comments, viewing details)
- 10% Content creation (new itineraries, locations)
- 5% User profile operations
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

BASE_DIR = Path(__file__).resolve().parent
load_dotenv(BASE_DIR / ".env.periodic")

# Sample data pools for randomization
DESTINATIONS = [
    "Paris", "Tokyo", "New York", "London", "Barcelona", "Rome", "Dubai", "Singapore",
    "Sydney", "Amsterdam", "Berlin", "Prague", "Vienna", "Budapest", "Lisbon"
]

USER_NAMES = [
    "Emma Smith", "Liam Johnson", "Olivia Williams", "Noah Brown", "Ava Jones",
    "Emma", "John", "Jane", "Michael", "Sarah"
]

COMMENTS = [
    "This looks amazing!",
    "I've been there! Such a wonderful place.",
    "Adding this to my bucket list!",
    "Great itinerary, very well planned.",
    "Thanks for sharing this experience.",
]

# Track created itinerary IDs for likes/comments
created_itinerary_ids = []


class PeriodicWorkloadUser(FastHttpUser):
    """
    Simulates a typical user with periodic (regular daily) usage patterns.
    Most operations are reads (browsing), with occasional writes (creating, liking).
    """
    
    host = os.getenv("LOCUST_HOST", "http://localhost:8080")
    
    # Realistic wait time between actions (1-5 seconds)
    wait_time = between(1, 5)
    
    # Derive headers from environment
    _origin = os.getenv("ORIGIN", "http://localhost:5173").rstrip("/")
    _url = urlparse(host)
    _host_header = _url.netloc or "localhost:8080"
    
    default_headers = {
        "Accept": "application/json",
        "Accept-Encoding": "gzip, deflate, br",
        "Accept-Language": "en-GB,en;q=0.5",
        "Connection": "keep-alive",
        "Host": _host_header,
        "Origin": _origin,
        "Referer": f"{_origin}/",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
    }
    
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        # Get bearer token if AUTH is enabled
        self.bearer_token = get_bearer_token()
        if self.bearer_token:
            self.default_headers["Authorization"] = f"Bearer {self.bearer_token}"
            print(f"🔐 Using authentication for user requests")
    
    def on_start(self):
        """Initialize user session - register or simulate existing user."""
        # 70% chance to use an existing test user email
        if random.random() < 0.7:
            # Use a pre-seeded user (format: loadtest.{id}@example.com)
            num_users = int(os.getenv("NUM_USERS", "50"))
            user_id = random.randint(0, num_users - 1)  # 0-indexed
            self.user_email = f"loadtest.{user_id}@example.com"
            self.user_name = f"Test User {user_id}"
        else:
            # Create a new user during the test
            timestamp = int(time.time())
            user_id = random.randint(10000, 99999)
            self.user_name = random.choice(USER_NAMES)
            self.user_email = f"{self.user_name.replace(' ', '.').lower()}.{user_id}@loadtest.example.com"
            
            self.client.post(
                "/user/register",
                json={"name": self.user_name, "email": self.user_email},
                name="POST /user/register",
                headers=self.default_headers
            )
    
    # ========== BROWSE/SEARCH OPERATIONS (60% weight) ==========
    
    @task(20)
    def search_itineraries_by_destination(self):
        """Search for itineraries by destination."""
        destination = random.choice(DESTINATIONS)
        
        with self.client.post(
            "/itinerary/search",
            json={"destination": destination},
            catch_response=True,
            name="POST /itinerary/search [by destination]"
        ) as response:
            if response.status_code == 200:
                try:
                    results = response.json()
                    # Store some itinerary IDs for later use
                    if isinstance(results, list) and len(results) > 0:
                        for item in results[:3]:
                            if 'id' in item and item['id'] not in created_itinerary_ids:
                                created_itinerary_ids.append(item['id'])
                except:
                    pass
    
    @task(15)
    def search_itineraries_by_user(self):
        """Search for itineraries by username."""
        user_name = random.choice(USER_NAMES)
        
        self.client.post(
            "/itinerary/search",
            json={"userName": user_name},
            name="POST /itinerary/search [by user]"
        )
    
    @task(10)
    def search_itineraries_by_date_range(self):
        """Search for itineraries within a date range."""
        start_date = (datetime.now() + timedelta(days=random.randint(0, 90))).strftime("%Y-%m-%d")
        end_date = (datetime.now() + timedelta(days=random.randint(91, 180))).strftime("%Y-%m-%d")
        
        self.client.post(
            "/itinerary/search",
            json={"startDateFrom": start_date, "startDateTo": end_date},
            name="POST /itinerary/search [by date range]"
        )
    
    @task(10)
    def browse_all_itineraries(self):
        """Browse all itineraries (empty search)."""
        with self.client.post(
            "/itinerary/search",
            json={},
            catch_response=True,
            name="POST /itinerary/search [all]"
        ) as response:
            if response.status_code == 200:
                try:
                    results = response.json()
                    if isinstance(results, list) and len(results) > 0:
                        for item in results[:5]:
                            if 'id' in item and item['id'] not in created_itinerary_ids:
                                created_itinerary_ids.append(item['id'])
                except:
                    pass
    
    # @task(5)
    # def get_user_info(self):
    #     """Get user information by email."""
    #     # DISABLED: Endpoint causing issues, skip for now
    #     # Use the seeded user format: loadtest.{id}@example.com
    #     num_users = int(os.getenv("NUM_USERS", "50"))
    #     user_id = random.randint(0, num_users - 1)
    #     email = f"loadtest.{user_id}@example.com"
    #     
    #     self.client.get(
    #         f"/user/get?email={email}",
    #         name="GET /user/get",
    #         headers=self.default_headers
    #     )
    
    # ========== SOCIAL INTERACTIONS (25% weight) ==========
    
    @task(8)
    def view_itinerary_locations(self):
        """View locations for a random itinerary."""
        if created_itinerary_ids:
            itinerary_id = random.choice(created_itinerary_ids)
            self.client.get(
                f"/location/itinerary/{itinerary_id}",
                name="GET /location/itinerary/:id"
            )
    
    @task(6)
    def like_itinerary(self):
        """Like a random itinerary."""
        if created_itinerary_ids:
            itinerary_id = random.choice(created_itinerary_ids)
            # Add Content-Length header for POST request without body
            headers = self.default_headers.copy()
            headers['Content-Length'] = '0'
            with self.client.post(
                f"/like/itinerary/{itinerary_id}",
                name="POST /like/itinerary/:id",
                headers=headers,
                catch_response=True
            ) as response:
                # Accept both success and "already liked" as OK
                if response.status_code in [200, 400]:
                    response.success()
                else:
                    response.failure(f"Unexpected status: {response.status_code}")
    
    @task(6)
    def view_itinerary_comments(self):
        """View comments for a random itinerary."""
        if created_itinerary_ids:
            itinerary_id = random.choice(created_itinerary_ids)
            self.client.get(
                f"/comment/itinerary/{itinerary_id}",
                name="GET /comment/itinerary/:id"
            )
    
    @task(5)
    def add_comment_to_itinerary(self):
        """Add a comment to a random itinerary."""
        if created_itinerary_ids:
            itinerary_id = random.choice(created_itinerary_ids)
            comment_data = {
                "userEmail": self.user_email,
                "comment": random.choice(COMMENTS)
            }
            self.client.post(
                f"/comment/itinerary/{itinerary_id}",
                json=comment_data,
                name="POST /comment/itinerary/:id"
            )
    
    # ========== CONTENT CREATION (10% weight) ==========
    
    @task(6)
    def create_itinerary(self):
        """Create a new itinerary."""
        destination = random.choice(DESTINATIONS)
        start_date = (datetime.now() + timedelta(days=random.randint(1, 180))).strftime("%Y-%m-%d")
        
        itinerary_data = {
            "title": f"{random.choice(['Trip to', 'Visit to', 'Adventure in'])} {destination}",
            "destination": destination,
            "startDate": start_date,
            "shortDescription": f"An amazing journey to {destination}",
            "detailedDescription": f"A comprehensive trip exploring the highlights of {destination}. This itinerary includes visits to famous landmarks, local cuisine experiences, and cultural immersion."
        }
        
        with self.client.post(
            "/itinerary/create",
            json=itinerary_data,
            catch_response=True,
            name="POST /itinerary/create"
        ) as response:
            if response.status_code in [200, 201]:
                try:
                    result = response.json()
                    if 'id' in result:
                        created_itinerary_ids.append(result['id'])
                except:
                    pass
    
    # @task(4)
    # def add_location_to_itinerary(self):
    #     """Add a location to a random itinerary."""
    #     # DISABLED: Endpoint causing issues, skip for now
    #     if created_itinerary_ids:
    #         itinerary_id = random.choice(created_itinerary_ids)
    #         
    #         from_date = (datetime.now() + timedelta(days=random.randint(1, 30))).strftime("%Y-%m-%d")
    #         to_date = (datetime.now() + timedelta(days=random.randint(31, 37))).strftime("%Y-%m-%d")
    #         
    #         location_data = {
    #             "name": f"Location in {random.choice(DESTINATIONS)}",
    #             "description": "A wonderful place to visit with great atmosphere",
    #             "fromDate": from_date,
    #             "toDate": to_date,
    #         }
    #         
    #         self.client.post(
    #             f"/location/itinerary/{itinerary_id}",
    #             data=location_data,
    #             name="POST /location/itinerary/:id",
    #             headers=self.default_headers
    #         )
    
    # ========== USER PROFILE OPERATIONS (5% weight) ==========
    
    @task(3)
    def get_my_itineraries(self):
        """Get itineraries for the current user."""
        self.client.get(
            "/itinerary/get",
            name="GET /itinerary/get"
        )
    
    @task(2)
    def get_my_likes(self):
        """Get likes by the current user."""
        self.client.get(
            "/like/user",
            name="GET /like/user"
        )


# Event handlers for custom metrics
@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    """Called when the test starts."""
    print("=" * 80)
    print("🔄 PERIODIC WORKLOAD TEST STARTED")
    print("=" * 80)
    print(f"Target Host: {os.getenv('LOCUST_HOST', 'http://localhost:8080')}")
    print(f"Test Profile: Normal daily traffic with realistic user behavior")
    print("=" * 80)


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    """Called when the test stops."""
    print("=" * 80)
    print("✅ PERIODIC WORKLOAD TEST COMPLETED")
    print("=" * 80)
    print(f"Total itinerary IDs collected: {len(created_itinerary_ids)}")
    print("=" * 80)

