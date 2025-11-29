#!/usr/bin/env python3
"""
Firebase Token Generator for Load Testing

This script generates Firebase custom tokens for test users.
These tokens can be used for load testing with authentication enabled.

Usage:
    python generate_tokens.py --output tokens.json --count 50
"""

import json
import sys
import argparse
from pathlib import Path

try:
    import firebase_admin
    from firebase_admin import credentials, auth
except ImportError:
    print("❌ Error: firebase-admin not installed")
    print("Install it with: pip install firebase-admin")
    sys.exit(1)


def initialize_firebase():
    """Initialize Firebase Admin SDK."""
    try:
        # Try to use Application Default Credentials (ADC)
        firebase_admin.initialize_app()
        print("✅ Using Application Default Credentials")
        return True
    except Exception as e:
        print(f"⚠️  Failed to use ADC: {e}")
        
        # Try to use service account file
        service_account_path = Path("service-account.json")
        if service_account_path.exists():
            try:
                cred = credentials.Certificate(str(service_account_path))
                firebase_admin.initialize_app(cred)
                print("✅ Using service account from service-account.json")
                return True
            except Exception as e:
                print(f"❌ Failed to initialize with service account: {e}")
                return False
        else:
            print("❌ No service-account.json file found")
            print("Please provide a service account JSON file or set up ADC")
            return False


def generate_custom_token(uid, email, name):
    """Generate a Firebase custom token for a user."""
    try:
        # Create custom token with additional claims
        custom_token = auth.create_custom_token(
            uid,
            {
                'email': email,
                'name': name,
                'email_verified': True
            }
        )
        return custom_token.decode('utf-8')
    except Exception as e:
        print(f"❌ Error generating token for {email}: {e}")
        return None


def generate_tokens_for_test_users(count, output_file):
    """Generate tokens for test users."""
    print(f"🔑 Generating {count} custom tokens...")
    print()
    
    tokens = {}
    
    for i in range(1, count + 1):
        # Generate consistent UIDs and emails for test users
        uid = f"test-user-{i}"
        email = f"test.user.{i}@loadtest.example.com"
        name = f"Test User {i}"
        
        token = generate_custom_token(uid, email, name)
        
        if token:
            tokens[email] = {
                "uid": uid,
                "email": email,
                "name": name,
                "token": token
            }
            if i % 10 == 0:
                print(f"  Generated {i}/{count} tokens...")
    
    print(f"✅ Generated {len(tokens)} tokens")
    print()
    
    # Save to file
    output_path = Path(output_file)
    with open(output_path, 'w') as f:
        json.dump(tokens, f, indent=2)
    
    print(f"💾 Tokens saved to: {output_path}")
    print()
    print("📝 Note: These custom tokens need to be exchanged for ID tokens.")
    print("   You can do this by calling the Firebase Auth REST API:")
    print("   https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken")
    print()
    print("   Or use the simpler approach: Set AUTH_ENABLED=false in your app config.")


def main():
    parser = argparse.ArgumentParser(description="Generate Firebase tokens for load testing")
    parser.add_argument('--output', '-o', default='tokens.json', help='Output file for tokens')
    parser.add_argument('--count', '-c', type=int, default=50, help='Number of tokens to generate')
    
    args = parser.parse_args()
    
    print("=" * 80)
    print("🔥 Firebase Token Generator for Load Testing")
    print("=" * 80)
    print()
    
    # Initialize Firebase
    if not initialize_firebase():
        print()
        print("=" * 80)
        print("❌ Setup Instructions")
        print("=" * 80)
        print()
        print("Option 1: Use Application Default Credentials (ADC)")
        print("  gcloud auth application-default login")
        print()
        print("Option 2: Use a service account")
        print("  1. Download service account JSON from Firebase Console")
        print("  2. Save it as 'service-account.json' in this directory")
        print()
        sys.exit(1)
    
    print()
    
    # Generate tokens
    generate_tokens_for_test_users(args.count, args.output)
    
    print("=" * 80)
    print("✅ Token Generation Complete!")
    print("=" * 80)


if __name__ == "__main__":
    main()



