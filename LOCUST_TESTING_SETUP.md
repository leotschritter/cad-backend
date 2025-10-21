# Locust Testing Setup Guide
This guide provides instructions on how to set up and run Locust for load testing the application.
## Prerequisites
- Having Python 3.10.x or higher installed on your machine
  - Having installed all required packages from `load/requirements.txt`. You can install it using pip:
    ```bash
    pip install -r load/requirements.txt
    ```
  - recommended: using a virtual environment to avoid package conflicts and keep your global Python environment clean.
    ```bash
    python -m venv .venv
    source .venv/bin/activate  # On Windows use `.venv\Scripts\activate`
    pip install -r load/requirements.txt
    ```
## Running Locust
1. Ensure your application is running on the machine you are using for testing.
2. Navigate to the `load` directory:
3. Run Locust with the following commands for each architecture:
   ```bash
   # IaaS Stress Test: 1000 users, spawn rate of 8.33 users/sec, duration of 2 minutes
   locust -f locustfile.py --host https://tripico.fun/api -u 1000 -r 8.33 -t 2m --profile IaaS
   # PaaS Stress Test: 1000 users, spawn rate of 8.33 users/sec, duration of 2 minutes
   locust -f locustfile.py --host https://api.tripico.fun -u 1000 -r 8.33 -t 2m --profile PaaS
    ```
   `1000 users / 8.33 users/s ≈ 120s` to reach 1000 users in 2 minutes.
