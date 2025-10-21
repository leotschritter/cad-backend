from locust import task, run_single_user, between
from locust import FastHttpUser
import os, time
from itertools import count

# ---- unique email generator (process-safe, low collision risk in distributed runs) ----
_RUN_ID = os.getenv("RUN_ID", time.strftime("%Y%m%d%H%M%S"))
_PID = os.getpid()
_SEQ = count(1)

def unique_email(local_base=None, domain=None):
    """
    john.doe+<RUN_ID>-<PID>-<N>@example.com
    Override parts with EMAIL_LOCAL / EMAIL_DOMAIN env vars if you like.
    """
    local_base = local_base or os.getenv("EMAIL_LOCAL", "john.doe")
    domain = domain or os.getenv("EMAIL_DOMAIN", "example.com")
    n = next(_SEQ)
    return f"{local_base}+{_RUN_ID}-{_PID}-{n}@{domain}"


class all(FastHttpUser):
    host = "https://api.tripico.fun"
    wait_time = between(1, 3)

    default_headers = {
        "Accept": "*/*",
        "Accept-Encoding": "gzip, deflate, br, zstd",
        "Accept-Language": "en-GB,en;q=0.5",
        "Connection": "keep-alive",
        "Host": "api.tripico.fun",
        "Origin": "http://localhost:5173",
        "Referer": "http://localhost:5173/",
        "Sec-Fetch-Dest": "empty",
        "Sec-Fetch-Mode": "cors",
        "Sec-Fetch-Site": "same-site",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
    }

    @task
    def t(self):
        # keep your existing calls
        with self.rest("GET", "/user/get?email=leotschritter%40web.de", headers={"Priority": "u=0"}) as resp:
            pass

        with self.rest("GET", "/itinerary/get/leotschritter%40web.de", headers={"Priority": "u=4"}) as resp:
            pass

        with self.client.request(
            "POST",
            "/itinerary/create/leotschritter%40web.de",
            headers={"Content-Type": "application/json", "Priority": "u=0"},
            data='{"title":"ukjjkhkj","destination":"loejoelknlk","startDate":"2025-10-23","shortDescription":"jlkhjk","detailedDescription":"jkzjthgfrdsycxvbn"}',
            catch_response=True,
        ) as resp:
            if resp.status_code not in (200, 201):
                resp.failure(f"Create itinerary failed: {resp.status_code}")

        with self.rest("POST", "/itinerary/search", headers={"Priority": "u=0"}, json={}) as resp:
            pass

    # ---- NEW: load test for /user/register with unique emails ----
    @task(2)  # weight it higher/lower as you prefer
    def register_unique_user(self):
        name = os.getenv("NAME_PREFIX", "John Doe")
        email = unique_email()
        payload = {"name": name, "email": email}

        # Use json= so Locust sets Content-Type properly; no need to send Content-Length
        with self.client.post(
            "/user/register",
            json=payload,
            name="POST /user/register",
            catch_response=True,
            headers={"Priority": "u=0"},
        ) as resp:
            if resp.status_code not in (200, 201):
                # if backend returns 409 on duplicate, mark it as failure so you notice
                resp.failure(f"Register failed ({resp.status_code}): {resp.text[:200]}")
