#!/usr/bin/env python3
        sys.exit(1)
        print(f"\n\n❌ Fatal error: {e}")
    except Exception as e:
        sys.exit(1)
        print("\n\n⚠️  Cleanup interrupted by user")
    except KeyboardInterrupt:
        main()
    try:
if __name__ == "__main__":


    print("=" * 70)
    print("✅ CLEANUP COMPLETE")
    print("\n" + "=" * 70)
    
        print(f"\n✓ Removed {users_file}")
        users_file.unlink()
    if users_file.exists():
    users_file = BASE_DIR / "test_users.json"
    # Remove test users file
    
    print("   • Use database administration tools")
    print("   • Restore from a pre-test backup")
    print("   • Manually truncate test tables")
    print("   If your services don't support deletion, you may need to:")
    print("\n⚠️  NOTE: Database cleanup requires DELETE endpoints in services.")
    
        print(f"\n⚠️  Encountered {len(stats['errors'])} errors")
    if stats["errors"]:
    
    print(f"\nFirebase users deleted: {stats['firebase_users_deleted']}")
    print("=" * 70)
    print("📝 CLEANUP SUMMARY")
    print("\n" + "=" * 70)
    
    # Since these may not exist, we can only provide guidance
    # Note: Database cleanup would require DELETE endpoints in the services
    
        print(f"\n✓ Deleted {stats['firebase_users_deleted']} Firebase users")
        
            print_progress(i + 1, len(test_users), "Deleting Firebase users")
            
                delete_firebase_user(email, password)
            if email:
            email = user.get("email")
        for i, user in enumerate(test_users):
        
        password = os.getenv("FIREBASE_PASSWORD", "LoadTest123!")
        print("\n🔥 Deleting Firebase users...")
    if FIREBASE_API_KEY:
    # Cleanup Firebase users
    
    print(f"✓ Found {len(test_users)} test users")
    
        sys.exit(1)
        print("❌ No test users found. Nothing to cleanup.")
    if not test_users:
    
    test_users = load_test_users()
    print("\n📂 Loading test users...")
    
        sys.exit(0)
        print("\n❌ Cleanup cancelled")
    if response.lower() != "yes":
    response = input("\nAre you sure you want to continue? (yes/no): ")
    
    print("\n" + "=" * 70)
    print("   • Graph nodes in recommendation service")
    print("   • All locations, likes, and comments")
    print("   • All itineraries created by test users")
    print("   • Test users (loadtest.*@example.com)")
    print("\n⚠️  WARNING: This will delete all test data!")
    print("=" * 70)
    print("🧹 MULTI-SERVICE DATA CLEANUP")
    print("=" * 70)
    """Main cleanup function."""
def main():


    print(f"\r{prefix}: |{bar}| {percentage:.1f}% ({current}/{total})", end="", flush=True)
    bar = "█" * filled + "░" * (50 - filled)
    filled = int(percentage / 2)
    percentage = (current / total) * 100
    """Print a progress bar."""
def print_progress(current: int, total: int, prefix: str = "Progress"):


        return []
        print(f"❌ Error loading test users: {e}")
    except Exception as e:
            return json.load(f)
        with open(users_file, 'r') as f:
    try:
    
        return []
        print("⚠️  test_users.json not found. Cannot cleanup users.")
    if not users_file.exists():
    
    users_file = BASE_DIR / "test_users.json"
    """Load test users from file."""
def load_test_users():


        return False
        stats["errors"].append(f"Firebase deletion error for {email}: {e}")
    except Exception as e:
        
        return False
        
            return True
            stats["firebase_users_deleted"] += 1
        if delete_response.status_code == 200:
        
        )
            timeout=10
            json={"idToken": id_token},
            delete_url,
        delete_response = requests.post(
        
        delete_url = f"https://identitytoolkit.googleapis.com/v1/accounts:delete?key={FIREBASE_API_KEY}"
        # Now delete the account
        
        id_token = signin_response.json().get("idToken")
        
            return False
        if signin_response.status_code != 200:
        
        )
            timeout=10
            },
                "returnSecureToken": True
                "password": password,
                "email": email,
            json={
            signin_url,
        signin_response = requests.post(
    try:
    
    signin_url = f"https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key={FIREBASE_API_KEY}"
    # First, sign in to get the ID token
    
        return False
    if not FIREBASE_API_KEY:
    """Delete a Firebase user."""
def delete_firebase_user(email: str, password: str) -> bool:


}
    "errors": []
    "itineraries_deleted": 0,
    "database_users_deleted": 0,
    "firebase_users_deleted": 0,
stats = {
# Statistics

FIREBASE_API_KEY = os.getenv("FIREBASE_API_KEY", "")
RECOMMENDATION_SERVICE_URL = os.getenv("RECOMMENDATION_SERVICE_URL", "http://localhost:8081").rstrip("/")
ITINERARY_SERVICE_URL = os.getenv("ITINERARY_SERVICE_URL", "http://localhost:8080").rstrip("/")
# Service URLs

load_dotenv(BASE_DIR / ".env.seed")
BASE_DIR = Path(__file__).resolve().parent

import os
from dotenv import load_dotenv
from pathlib import Path
import sys
import json
import requests

"""
WARNING: This will delete all test users matching the pattern loadtest.*@example.com

- Graph nodes in recommendation service
- Locations, likes, and comments
- Itineraries created by test users
- Firebase authentication users
- Test users from itinerary service
Removes all test data created during seeding:

Cleanup Script for Multi-Service Load Testing
"""

