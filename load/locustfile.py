from locust import task, run_single_user, between, FastHttpUser
from itertools import count
from urllib.parse import urlparse
from pathlib import Path
from dotenv import load_dotenv
import os, time

# --- load .env from the same folder as this file ---
BASE_DIR = Path(__file__).resolve().parent
load_dotenv(BASE_DIR / ".env")   # or load_dotenv() if you run from repo root

# ---- unique email generator ----
_RUN_ID = os.getenv("RUN_ID", time.strftime("%Y%m%d%H%M%S"))
_PID = os.getpid()
_SEQ = count(1)

def unique_email(local_base=None, domain=None):
    local_base = local_base or os.getenv("EMAIL_LOCAL", "john.doe")
    domain = domain or os.getenv("EMAIL_DOMAIN", "example.com")
    n = next(_SEQ)
    return f"{local_base}+{_RUN_ID}-{_PID}-{n}@{domain}"

class all(FastHttpUser):
    # IMPORTANT: no trailing comma + include scheme
    host = os.getenv("LOCUST_HOST", "http://localhost:8080")
    wait_time = between(1, 3)

    # derive Host/Origin/Referer from env
    _origin = os.getenv("ORIGIN", "http://localhost:5173").rstrip("/")
    _url = urlparse(host)
    _host_header = _url.netloc or "localhost:8080"

    default_headers = {
        "Accept": "*/*",
        "Accept-Encoding": "gzip, deflate, br, zstd",
        "Accept-Language": "en-GB,en;q=0.5",
        "Connection": "keep-alive",
        "Host": _host_header,                 # just host:port
        "Origin": _origin,                    # e.g. http://localhost:5173
        "Referer": f"{_origin}/",             # trailing slash
        "Sec-Fetch-Dest": "empty",
        "Sec-Fetch-Mode": "cors",
        "Sec-Fetch-Site": "same-site",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0",
    }

    @task
    def t(self):
        with self.rest("GET", "/user/get?email=leotschritter%40web.de", headers={"Priority": "u=0"}):
            pass
        with self.rest("GET", "/itinerary/get/leotschritter%40web.de", headers={"Priority": "u=4"}):
            pass
        with self.client.post(
            "/itinerary/create/leotschritter%40web.de",
            headers={"Content-Type": "application/json", "Priority": "u=0"},
            data='{"title":"ukjjkhkj","destination":"loejoelknlk","startDate":"2025-10-23","shortDescription":"jlkhjk","detailedDescription":"jkzjthgfrdsycxvbn"}',
            catch_response=True,
            name="POST /itinerary/create/:email",
        ) as resp:
            if resp.status_code not in (200, 201):
                resp.failure(f"Create itinerary failed: {resp.status_code}")

    @task
    def register_unique_user(self):
        name = os.getenv("NAME_PREFIX", "John Doe")
        email = unique_email()
        with self.client.post(
            "/user/register",
            json={"name": name, "email": email},
            headers={"Priority": "u=0"},
            name="POST /user/register",
            catch_response=True,
        ) as resp:
            if resp.status_code not in (200, 201):
                # if backend returns 409 on duplicate, mark it as failure so you notice
                resp.failure(f"Register failed ({resp.status_code}): {resp.text[:200]}")
