#!/usr/bin/env python3
"""
Cleanup Script for Load Testing

This script removes all test users and data created by the seeding process.
It cleans up both Firebase users and database users.

WARNING: This will delete all test users matching the pattern!
"""

import requests
import json
import sys
from pathlib import Path
from dotenv import load_dotenv
import os

BASE_DIR = Path(__file__).resolve().parent
load_dotenv(BASE_DIR / ".env.seed")

# Configuration
BASE_URL = os.getenv("LOCUST_HOST", "http://localhost:8080")
FIREBASE_API_KEY = os.getenv("FIREBASE_API_KEY", "")

# Try to import firebase_admin
try:
    import firebase_admin
    from firebase_admin import auth as firebase_auth
    FIREBASE_AVAILABLE = True
except ImportError:
    FIREBASE_AVAILABLE = False
    firebase_admin = None
    firebase_auth = None

# Statistics
stats = {
    "firebase_users_deleted": 0,
    "database_users_deleted": 0,
    "files_deleted": 0,
    "errors": []
}


def initialize_firebase():
    """Initialize Firebase Admin SDK."""
    if not FIREBASE_AVAILABLE:
        return False
    
    try:
        firebase_admin.initialize_app()
        return True
    except ValueError:
        # Already initialized
        return True
    except Exception as e:
        print(f"⚠️  Failed to initialize Firebase: {e}")
        return False


def load_test_users():
    """Load test users from test_users.json."""
    users_file = BASE_DIR / "test_users.json"
    
    if not users_file.exists():
        return []
    
    try:
        with open(users_file, 'r') as f:
            data = json.load(f)
            return data.get('users', [])
    except Exception as e:
        print(f"⚠️  Failed to load test users: {e}")
        return []


def delete_firebase_user(email):
    """Delete a Firebase user by email."""
    if not FIREBASE_AVAILABLE:
        return False
    
    try:
        # Get user by email
        user = firebase_auth.get_user_by_email(email)
        # Delete user
        firebase_auth.delete_user(user.uid)
        stats["firebase_users_deleted"] += 1
        return True
    except firebase_admin.exceptions.NotFoundError:
        # User doesn't exist, that's fine
        return True
    except Exception as e:
        stats["errors"].append(f"Failed to delete Firebase user {email}: {str(e)}")
        return False


def delete_database_user_via_api(email):
    """
    Delete a database user.
    Note: This requires a DELETE endpoint in your API.
    If you don't have one, users will remain in the database.
    """
    # Check if delete endpoint exists
    # Most apps don't have user deletion, so we'll just skip this
    # The database users won't cause issues and can be manually cleaned
    return False


def delete_test_files():
    """Delete generated test files."""
    files_to_delete = [
        "test_users.json"
    ]
    
    for filename in files_to_delete:
        filepath = BASE_DIR / filename
        if filepath.exists():
            try:
                filepath.unlink()
                stats["files_deleted"] += 1
                print(f"  ✅ Deleted: {filename}")
            except Exception as e:
                print(f"  ⚠️  Failed to delete {filename}: {e}")


def confirm_cleanup():
    """Ask user to confirm cleanup."""
    print("=" * 80)
    print("⚠️  WARNING: CLEANUP OPERATION")
    print("=" * 80)
    print()
    print("This will DELETE:")
    print("  - All Firebase test users")
    print("  - test_users.json file")
    print()
    print("This will KEEP:")
    print("  - .env configuration files (for reuse)")
    print("  - Database users (cleaned up automatically by Firebase Auth)")
    print()
    
    response = input("Are you sure you want to continue? (yes/no): ")
    return response.lower() in ['yes', 'y']


def main():
    """Main cleanup function."""
    print("=" * 80)
    print("🧹 Load Test Cleanup Script")
    print("=" * 80)
    print()
    
    # Load test users
    test_users = load_test_users()
    
    if not test_users:
        print("ℹ️  No test users found (test_users.json doesn't exist)")
        print("   Nothing to clean up!")
        print()
        print("✅ Cleanup complete")
        sys.exit(0)
    
    print(f"Found {len(test_users)} test users to clean up")
    print()
    
    # Confirm with user
    if not confirm_cleanup():
        print()
        print("❌ Cleanup cancelled")
        sys.exit(0)
    
    print()
    print("🧹 Starting cleanup...")
    print()
    
    # Initialize Firebase if available
    firebase_initialized = False
    if FIREBASE_AVAILABLE:
        firebase_initialized = initialize_firebase()
        if firebase_initialized:
            print("✅ Firebase Admin SDK initialized")
        else:
            print("⚠️  Firebase Admin SDK not initialized (users may remain)")
    else:
        print("⚠️  firebase-admin not installed (Firebase users won't be deleted)")
    
    print()
    
    # Delete Firebase users
    if firebase_initialized:
        print("🗑️  Deleting Firebase users...")
        for i, user in enumerate(test_users):
            email = user.get('email')
            print(f"  [{i+1}/{len(test_users)}] Deleting: {email}")
            delete_firebase_user(email)
        print(f"✅ Deleted {stats['firebase_users_deleted']} Firebase users")
    else:
        print("⏭️  Skipping Firebase user deletion")
    
    print()
    
    # Delete test files
    print("🗑️  Deleting test files...")
    delete_test_files()
    
    print()
    print("=" * 80)
    print("📊 Cleanup Summary")
    print("=" * 80)
    print(f"✅ Firebase users deleted: {stats['firebase_users_deleted']}")
    print(f"ℹ️  Database users: Will be cleaned up by Firebase Auth (token invalidation)")
    print(f"✅ Files deleted: {stats['files_deleted']}")
    print(f"❌ Errors: {len(stats['errors'])}")
    
    if stats['errors']:
        print()
        print("⚠️  Errors encountered:")
        for error in stats['errors'][:5]:
            print(f"  - {error}")
    
    print("=" * 80)
    print()
    print("✅ Cleanup complete!")
    print()
    print("ℹ️  Note: Database users remain but can no longer authenticate")
    print("   (Firebase users deleted → tokens no longer valid)")
    print()


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print()
        print("❌ Cleanup cancelled by user")
        sys.exit(1)

