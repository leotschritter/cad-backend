"""
Once-in-a-Lifetime Workload Test - Simulates traffic spike/viral scenario.

This test simulates a sudden burst of traffic, such as:
- Viral content being shared on social media
- Featured article or promotion
- Major event driving traffic
- Influencer sharing content

Workload Characteristics:
- 80% Heavy read operations focused on popular content
- 15% Social interactions (burst of likes and comments)
- 4% Search and discovery
- 1% Content creation
- Lower wait times between requests (faster user actions)
- Higher concurrency on specific items (hot spots)
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
load_dotenv(BASE_DIR / ".env.onceinlifetime")

# Hot itinerary IDs (simulating viral/popular content)
# These will be accessed much more frequently
hot_itinerary_ids = []
all_itinerary_ids = []

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
    "How is this real?!"
]

DESTINATIONS_POPULAR = [
    "Paris", "Tokyo", "New York", "Bali", "Maldives", "Santorini",
    "Iceland", "Switzerland", "Dubai", "Singapore"
]


class OnceInLifetimeUser(FastHttpUser):
    """
    Simulates a user during a traffic spike.
    Characteristics:
    - Faster actions (shorter wait time)
    - More focused on popular content
    - Quick social interactions
    """
    
    host = os.getenv("LOCUST_HOST", "http://localhost:8080")
    
    # Shorter wait time to simulate excited, quick-browsing users
    wait_time = between(0.5, 2)
    
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
        """Initialize user - most are new/guest users during viral events."""
        # 80% are new users during viral traffic
        if random.random() < 0.8:
            # New user
            timestamp = int(time.time())
            user_id = random.randint(100000, 999999)
            self.user_name = f"Viral User {user_id}"
            self.user_email = f"viral.user.{user_id}.{timestamp}@example.com"
            
            # Quick registration (fail silently if user exists)
            try:
                self.client.post(
                    "/user/register",
                    json={"name": self.user_name, "email": self.user_email},
                    name="POST /user/register",
                    timeout=5
                )
            except:
                pass
        else:
            # Use existing user (format: loadtest.{id}@example.com)
            num_users = int(os.getenv("NUM_USERS", "50"))
            user_id = random.randint(0, num_users - 1)  # 0-indexed
            self.user_email = f"loadtest.{user_id}@example.com"
            self.user_name = f"Test User {user_id}"
        
        # Initialize hot content list
        self._discover_hot_content()
    
    def _discover_hot_content(self):
        """Discover popular/viral content at the start of the session."""
        global hot_itinerary_ids, all_itinerary_ids
        
        # Search for popular destinations
        destination = random.choice(DESTINATIONS_POPULAR)
        try:
            response = self.client.post(
                "/itinerary/search",
                json={"destination": destination},
                name="POST /itinerary/search [discover hot]",
                timeout=5
            )
            
            if response.status_code == 200:
                results = response.json()
                if isinstance(results, list) and len(results) > 0:
                    # Add to hot content (simulate viral spread)
                    for item in results[:2]:
                        if 'id' in item:
                            if item['id'] not in hot_itinerary_ids:
                                hot_itinerary_ids.append(item['id'])
                            if item['id'] not in all_itinerary_ids:
                                all_itinerary_ids.append(item['id'])
        except:
            pass
    
    def _get_target_itinerary_id(self, hot_content_bias=0.8):
        """
        Get an itinerary ID with bias towards hot content.
        
        Args:
            hot_content_bias: Probability of choosing hot content (default 0.8 = 80%)
        """
        if hot_itinerary_ids and random.random() < hot_content_bias:
            return random.choice(hot_itinerary_ids)
        elif all_itinerary_ids:
            return random.choice(all_itinerary_ids)
        else:
            # Fallback to a random ID
            return random.randint(1, 100)
    
    # ========== HEAVY READ OPERATIONS (80% weight) ==========
    
    @task(30)
    def view_viral_itinerary_locations(self):
        """View locations for a viral/popular itinerary (hot content)."""
        itinerary_id = self._get_target_itinerary_id(hot_content_bias=0.9)
        
        if itinerary_id:
            self.client.get(
                f"/location/itinerary/{itinerary_id}",
                name="GET /location/itinerary/:id [HOT]"
            )
    
    @task(20)
    def view_viral_itinerary_comments(self):
        """View comments for popular content."""
        itinerary_id = self._get_target_itinerary_id(hot_content_bias=0.9)
        
        if itinerary_id:
            self.client.get(
                f"/comment/itinerary/{itinerary_id}",
                name="GET /comment/itinerary/:id [HOT]"
            )
    
    @task(15)
    def view_viral_itinerary_likes(self):
        """View like count for popular content."""
        itinerary_id = self._get_target_itinerary_id(hot_content_bias=0.9)
        
        if itinerary_id:
            self.client.get(
                f"/like/itinerary/{itinerary_id}",
                name="GET /like/itinerary/:id [HOT]"
            )
    
    @task(10)
    def search_trending_destination(self):
        """Search for trending destinations."""
        destination = random.choice(DESTINATIONS_POPULAR)
        
        with self.client.post(
            "/itinerary/search",
            json={"destination": destination},
            catch_response=True,
            name="POST /itinerary/search [trending]"
        ) as response:
            if response.status_code == 200:
                try:
                    results = response.json()
                    if isinstance(results, list) and len(results) > 0:
                        # Feed the hot content pool
                        for item in results[:3]:
                            if 'id' in item:
                                if item['id'] not in hot_itinerary_ids:
                                    hot_itinerary_ids.append(item['id'])
                                if item['id'] not in all_itinerary_ids:
                                    all_itinerary_ids.append(item['id'])
                except:
                    pass
    
    @task(5)
    def browse_discover_more(self):
        """Browse to discover more content (empty search)."""
        with self.client.post(
            "/itinerary/search",
            json={},
            catch_response=True,
            name="POST /itinerary/search [discover]"
        ) as response:
            if response.status_code == 200:
                try:
                    results = response.json()
                    if isinstance(results, list) and len(results) > 0:
                        for item in results[:5]:
                            if 'id' in item and item['id'] not in all_itinerary_ids:
                                all_itinerary_ids.append(item['id'])
                except:
                    pass
    
    # ========== SOCIAL INTERACTIONS - BURST (15% weight) ==========
    
    @task(8)
    def like_viral_content(self):
        """Like popular content (burst of likes)."""
        itinerary_id = self._get_target_itinerary_id(hot_content_bias=0.95)
        
        if not itinerary_id:
            return
        
        # Quick like, accept duplicate likes as success
        try:
            # Add Content-Length header for POST request without body
            headers = self.default_headers.copy()
            headers['Content-Length'] = '0'
            with self.client.post(
                f"/like/itinerary/{itinerary_id}",
                name="POST /like/itinerary/:id [BURST]",
                headers=headers,
                timeout=3,
                catch_response=True
            ) as response:
                # Accept both success and "already liked" as OK
                if response.status_code in [200, 400]:
                    response.success()
        except:
            pass
    
    @task(7)
    def comment_on_viral_content(self):
        """Quick comment on popular content."""
        itinerary_id = self._get_target_itinerary_id(hot_content_bias=0.95)
        
        if not itinerary_id:
            return
        
        comment_data = {
            "userEmail": self.user_email,
            "comment": random.choice(QUICK_COMMENTS)
        }
        
        try:
            self.client.post(
                f"/comment/itinerary/{itinerary_id}",
                json=comment_data,
                name="POST /comment/itinerary/:id [BURST]",
                timeout=3
            )
        except:
            pass
    
    # ========== DISCOVERY/SEARCH (4% weight) ==========
    
    @task(2)
    def search_by_date_range(self):
        """Search by date range during viral event."""
        start_date = (datetime.now() + timedelta(days=random.randint(0, 30))).strftime("%Y-%m-%d")
        end_date = (datetime.now() + timedelta(days=random.randint(31, 90))).strftime("%Y-%m-%d")
        
        self.client.post(
            "/itinerary/search",
            json={"startDateFrom": start_date, "startDateTo": end_date},
            name="POST /itinerary/search [date range]"
        )
    
    # @task(2)
    # def view_random_user_profile(self):
    #     """View a user profile."""
    #     # DISABLED: Endpoint causing issues, skip for now
    #     # Use the seeded user format: loadtest.{id}@example.com
    #     num_users = int(os.getenv("NUM_USERS", "50"))
    #     user_id = random.randint(0, num_users - 1)
    #     email = f"loadtest.{user_id}@example.com"
    #     
    #     try:
    #         self.client.get(
    #             f"/user/get?email={email}",
    #             name="GET /user/get",
    #             headers=self.default_headers,
    #             timeout=3
    #         )
    #     except:
    #         pass
    
    # ========== CONTENT CREATION (1% weight - minimal during spike) ==========
    
    @task(1)
    def create_inspired_itinerary(self):
        """Create itinerary inspired by viral content (rare during viewing spike)."""
        destination = random.choice(DESTINATIONS_POPULAR)
        start_date = (datetime.now() + timedelta(days=random.randint(30, 180))).strftime("%Y-%m-%d")
        
        itinerary_data = {
            "title": f"My trip to {destination}",
            "destination": destination,
            "startDate": start_date,
            "shortDescription": f"Inspired to visit {destination}!",
            "detailedDescription": f"Planning an amazing trip to {destination} after seeing all the incredible photos and stories!"
        }
        
        try:
            with self.client.post(
                "/itinerary/create",
                json=itinerary_data,
                catch_response=True,
                name="POST /itinerary/create [inspired]",
                timeout=5
            ) as response:
                if response.status_code in [200, 201]:
                    try:
                        result = response.json()
                        if 'id' in result:
                            all_itinerary_ids.append(result['id'])
                    except:
                        pass
        except:
            pass


# Event handlers
@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    """Called when the test starts."""
    print("=" * 80)
    print("🔥 ONCE-IN-A-LIFETIME WORKLOAD TEST STARTED")
    print("=" * 80)
    print(f"Target Host: {os.getenv('LOCUST_HOST', 'http://localhost:8080')}")
    print(f"Test Profile: Viral traffic spike - heavy read load on hot content")
    print("=" * 80)


@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    """Called when the test stops."""
    print("=" * 80)
    print("✅ ONCE-IN-A-LIFETIME WORKLOAD TEST COMPLETED")
    print("=" * 80)
    print(f"Hot itinerary IDs tracked: {len(hot_itinerary_ids)}")
    print(f"Total itinerary IDs tracked: {len(all_itinerary_ids)}")
    print("=" * 80)


# Optional: Run standalone for debugging
if __name__ == "__main__":
    import subprocess
    subprocess.run([
        "locust",
        "-f", __file__,
        "--host", os.getenv("LOCUST_HOST", "http://localhost:8080")
    ])

