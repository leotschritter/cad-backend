"""
Authentication Helper for Multi-Service Load Testing

Provides utilities to handle Firebase authentication during load tests.
Supports both static tokens and dynamic token generation via Firebase Auth API.
"""

import os
import json
import time
import requests
from pathlib import Path
from typing import Optional, Dict, List
import random


class AuthHelper:
    """Helper class to manage authentication tokens for load testing."""
    
    def __init__(self, firebase_api_key: Optional[str] = None):
        """
        Initialize the auth helper.
        
        Args:
            firebase_api_key: Firebase Web API key (optional, can use env var)
        """
        self.api_key = firebase_api_key or os.getenv("FIREBASE_API_KEY", "")
        self.token_cache: Dict[str, Dict] = {}
        self.token_file = Path(__file__).parent / "tokens.json"
        self.users_file = Path(__file__).parent / "test_users.json"
        self.test_users: Optional[List[Dict]] = None
        
    def exchange_custom_token_for_id_token(self, custom_token: str) -> Optional[str]:
        """
        Exchange a Firebase custom token for an ID token.
        
        Args:
            custom_token: The custom token to exchange
            
        Returns:
            ID token string or None if exchange failed
        """
        if not self.api_key:
            print("⚠️  FIREBASE_API_KEY not set, cannot exchange tokens")
            return None
        
        url = f"https://identitytoolkit.googleapis.com/v1/accounts:signInWithCustomToken?key={self.api_key}"
        
        try:
            response = requests.post(
                url,
                json={"token": custom_token, "returnSecureToken": True},
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                return data.get("idToken")
            else:
                print(f"⚠️  Token exchange failed: {response.status_code} - {response.text}")
                return None
                
        except Exception as e:
            print(f"⚠️  Token exchange error: {e}")
            return None
    
    def sign_in_with_email_password(self, email: str, password: str) -> Optional[str]:
        """
        Sign in with email and password to get an ID token.
        
        Args:
            email: User email
            password: User password
            
        Returns:
            ID token string or None if sign-in failed
        """
        if not self.api_key:
            return None
        
        # Check cache first
        cache_key = f"{email}:{password}"
        if cache_key in self.token_cache:
            cached = self.token_cache[cache_key]
            # Check if token is still valid (expires in 1 hour, refresh after 50 min)
            if time.time() - cached["timestamp"] < 3000:
                return cached["token"]
        
        url = f"https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key={self.api_key}"
        
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
                token = data.get("idToken")
                
                # Cache the token
                self.token_cache[cache_key] = {
                    "token": token,
                    "timestamp": time.time()
                }
                
                return token
            else:
                print(f"⚠️  Sign-in failed for {email}: {response.status_code}")
                return None
                
        except Exception as e:
            print(f"⚠️  Sign-in error for {email}: {e}")
            return None
    
    def load_test_users(self) -> List[Dict]:
        """
        Load test users from file.
        
        Returns:
            List of user dictionaries with email and other info
        """
        if self.test_users is not None:
            return self.test_users
        
        if self.users_file.exists():
            try:
                with open(self.users_file, 'r') as f:
                    self.test_users = json.load(f)
                return self.test_users
            except Exception as e:
                print(f"⚠️  Error loading test users: {e}")
        
        return []
    
    def get_random_test_user_token(self, password: str = "LoadTest123!") -> Optional[str]:
        """
        Get a token for a random test user.
        
        Args:
            password: Password for test users
            
        Returns:
            ID token or None
        """
        users = self.load_test_users()
        if not users:
            return None
        
        user = random.choice(users)
        email = user.get("email")
        
        if email:
            return self.sign_in_with_email_password(email, password)
        
        return None
    
    def save_tokens_to_file(self, tokens: List[Dict]):
        """
        Save tokens to file for reuse.
        
        Args:
            tokens: List of token dictionaries
        """
        try:
            with open(self.token_file, 'w') as f:
                json.dump({
                    "tokens": tokens,
                    "timestamp": time.time()
                }, f, indent=2)
        except Exception as e:
            print(f"⚠️  Error saving tokens: {e}")
    
    def load_tokens_from_file(self) -> List[Dict]:
        """
        Load tokens from file.
        
        Returns:
            List of token dictionaries
        """
        if not self.token_file.exists():
            return []
        
        try:
            with open(self.token_file, 'r') as f:
                data = json.load(f)
                # Check if tokens are still fresh (less than 50 minutes old)
                if time.time() - data.get("timestamp", 0) < 3000:
                    return data.get("tokens", [])
        except Exception as e:
            print(f"⚠️  Error loading tokens: {e}")
        
        return []


# Global auth helper instance
_auth_helper = None


def get_auth_helper() -> AuthHelper:
    """Get or create the global auth helper instance."""
    global _auth_helper
    if _auth_helper is None:
        _auth_helper = AuthHelper()
    return _auth_helper


def get_bearer_token(email: Optional[str] = None, password: str = "LoadTest123!") -> Optional[str]:
    """
    Convenience function to get a bearer token.
    
    Args:
        email: Specific user email, or None for random test user
        password: User password
        
    Returns:
        Bearer token string (without "Bearer " prefix) or None
    """
    if not os.getenv("FIREBASE_API_KEY"):
        # No authentication configured
        return None
    
    helper = get_auth_helper()
    
    if email:
        return helper.sign_in_with_email_password(email, password)
    else:
        return helper.get_random_test_user_token(password)

