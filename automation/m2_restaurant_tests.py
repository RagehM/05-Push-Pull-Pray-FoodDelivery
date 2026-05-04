"""
M2 restaurant-service test harness.

Covers the published M2 scenarios that target the restaurant service:
  S2-F1 search             TC233-TC236
  S2-F2 details merge      TC237-TC239
  S2-F3 revenue            TC240-TC243
  S2-F4 status transitions TC244-TC247
  S2-F5 details search     TC248-TC251
  S2-F6 top-rated          TC252-TC254
  S2-F7 rating             TC255-TC259
  S2-F8 toggle menu item   TC260-TC264
  S2-F9 unavailable items  TC265-TC267
  S2-F12 dashboard         TC48-TC53

Skipped per user request: S2-F10 (TC35-42) and S2-F11 (TC43-47).

Pre-reqs:
  docker compose up -d postgres mongo redis order-service restaurant-service
  pip install pyjwt requests psycopg2-binary pymongo

Run:
  python automation/m2_restaurant_tests.py
"""

from __future__ import annotations

import base64
import datetime as dt
import json
import os
import sys
import time
import traceback
import uuid
from typing import Optional

import jwt
import psycopg2
import pymongo
import requests

# -------- config --------

BASE_URL = "http://localhost:8082"

# read JWT secret from .env (same secret as restaurant-service uses)
ENV_PATH = os.path.join(os.path.dirname(__file__), "..", ".env")
with open(ENV_PATH, encoding="utf-8") as f:
    env = dict(line.strip().split("=", 1) for line in f if "=" in line and not line.strip().startswith("#"))
JWT_SECRET_B64 = env["JWT_SECRET"]
# Spring's Decoders.BASE64.decode -> raw key bytes used by HMAC
JWT_KEY = base64.b64decode(JWT_SECRET_B64 + "=" * (-len(JWT_SECRET_B64) % 4))

PG_DSN = dict(host="localhost", port=5432, dbname="fooddeliverydb", user="postgres", password="postgres")
MONGO_URI = "mongodb://root:rootpass@localhost:27017/fooddeliverymongo?authSource=admin"

# -------- helpers --------


def mint_jwt(uid: int, email: str, role: str) -> str:
    now = int(time.time())
    payload = {"sub": email, "uid": uid, "role": role, "iat": now, "exp": now + 3600}
    return jwt.encode(payload, JWT_KEY, algorithm="HS256")


def pg():
    return psycopg2.connect(**PG_DSN)


def mongo():
    return pymongo.MongoClient(MONGO_URI)


def nonce() -> str:
    return uuid.uuid4().hex[:10]


def ensure_admin_user() -> tuple[int, str]:
    """Ensure an ADMIN user exists in the users table; returns (id, email).
    The S2-F8 toggle endpoint uses countAdminById on a real users.id."""
    email = "grader_admin@grader.testgen.io"
    with pg() as c, c.cursor() as cur:
        cur.execute("SELECT id FROM users WHERE email=%s", (email,))
        row = cur.fetchone()
        if row:
            return row[0], email
        cur.execute(
            """INSERT INTO users (email, name, password, phone, role, status, created_at)
               VALUES (%s, %s, %s, %s, 'ADMIN', 'ACTIVE', now()) RETURNING id""",
            (email, "Grader Admin", "$2a$10$placeholder", f"+9999{uuid.uuid4().int % 10000000:07d}"),
        )
        return cur.fetchone()[0], email


def admin_token() -> str:
    uid, email = ensure_admin_user()
    return mint_jwt(uid, email, "ADMIN")


def auth(token: str) -> dict:
    return {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}


# create restaurant via API (ensures the JSONB defaults the service applies are present)
def create_restaurant(token: str, *, cuisine="EGYPTIAN", status="OPEN", details: Optional[dict] = None,
                      name: Optional[str] = None) -> dict:
    body = {
        "name": name or f"R_{nonce()}",
        "email": f"{nonce()}@grader.io",
        "phone": f"+1{uuid.uuid4().int % 10**10:010d}",
        "cuisineType": cuisine,
        "status": status,
        "details": details or {},
    }
    r = requests.post(f"{BASE_URL}/api/restaurants", json=body, headers=auth(token))
    r.raise_for_status()
    return r.json()


def jdbc_set_rating(rid: int, rating: float, total_ratings: int = 1):
    with pg() as c, c.cursor() as cur:
        cur.execute("UPDATE restaurants SET rating=%s, total_ratings=%s WHERE id=%s", (rating, total_ratings, rid))


def jdbc_insert_order(restaurant_id: int, user_id: int, status: str, total: float,
                      created_at: Optional[dt.datetime] = None) -> int:
    created_at = created_at or dt.datetime.now()
    with pg() as c, c.cursor() as cur:
        cur.execute(
            """INSERT INTO orders (created_at, delivery_address, order_date, restaurant_id, status, total_amount, user_id)
               VALUES (%s, 'addr', %s, %s, %s, %s, %s) RETURNING id""",
            (created_at, created_at, restaurant_id, status, total, user_id),
        )
        return cur.fetchone()[0]


def jdbc_insert_menu_item(restaurant_id: int, *, available: bool = True, name: Optional[str] = None) -> int:
    with pg() as c, c.cursor() as cur:
        cur.execute(
            """INSERT INTO menu_items (available, category, created_at, description, name, price, restaurant_id, metadata)
               VALUES (%s, 'MAIN', now(), 'desc', %s, 10.0, %s, '{}') RETURNING id""",
            (available, name or f"M_{nonce()}", restaurant_id),
        )
        return cur.fetchone()[0]


def jdbc_insert_order_item(order_id: int, menu_item_id: int, status: str = "PENDING") -> int:
    with pg() as c, c.cursor() as cur:
        cur.execute(
            """INSERT INTO order_items (item_name, line_number, menu_item_id, metadata, quantity, status, unit_price, order_id)
               VALUES ('item', 1, %s, '{}', 1, %s, 10.0, %s) RETURNING id""",
            (menu_item_id, status, order_id),
        )
        return cur.fetchone()[0]


def flush_cache(name: str):
    """FLUSH a single Spring cache by name in Redis (used to defeat stale state when needed)."""
    import subprocess
    subprocess.run(
        ["docker", "exec", "fooddelivery-redis", "redis-cli", "-a", "redispass", "--no-auth-warning",
         "EVAL", "local k=redis.call('keys', ARGV[1]); for i=1,#k do redis.call('del', k[i]) end; return #k", "0",
         f"{name}*"],
        capture_output=True,
    )


# -------- test runner --------

results: list[tuple[str, str, str]] = []  # (tc_id, status, msg)


def run(tc_id: str, fn):
    try:
        fn()
        print(f"  PASS  {tc_id}")
        results.append((tc_id, "PASS", ""))
    except AssertionError as e:
        print(f"  FAIL  {tc_id}  {e}")
        results.append((tc_id, "FAIL", str(e)))
    except Exception as e:
        print(f"  ERR   {tc_id}  {type(e).__name__}: {e}")
        traceback.print_exc()
        results.append((tc_id, "ERROR", f"{type(e).__name__}: {e}"))


# ============================================================
# S2-F12 — dashboard (TC48–TC53)
# ============================================================

def tc48_dashboard_happy(tok):
    rest = create_restaurant(tok)
    r = requests.get(f"{BASE_URL}/api/restaurants/{rest['id']}/dashboard", headers=auth(tok))
    assert 200 <= r.status_code < 300, f"status={r.status_code} body={r.text[:200]}"
    body = r.json()
    has_orders = "totalOrders" in body or "total_orders" in body
    has_rev = "totalRevenue" in body or "total_revenue" in body
    assert has_orders, f"missing totalOrders, body={body}"
    assert has_rev, f"missing totalRevenue, body={body}"


def tc49_dashboard_aggregates_match(tok):
    rest = create_restaurant(tok)
    rid = rest["id"]
    user_id, _ = ensure_admin_user()
    # 3 DELIVERED orders summing 600
    for amt in (100, 200, 300):
        jdbc_insert_order(rid, user_id, "DELIVERED", amt)
    r = requests.get(f"{BASE_URL}/api/restaurants/{rid}/dashboard", headers=auth(tok))
    assert r.status_code == 200, f"status={r.status_code} body={r.text[:200]}"
    body = r.json()
    total_orders = body.get("totalOrders", body.get("total_orders"))
    total_rev = body.get("totalRevenue", body.get("total_revenue"))
    # Per spec: dashboard aggregates over orders for this restaurant.
    # The actual service filters in repo; check that count==3 and sum==600 (within tolerance).
    assert total_orders == 3, f"expected 3 orders, got {total_orders} (body={body})"
    assert abs(float(total_rev) - 600.0) < 0.01, f"expected revenue~600, got {total_rev}"


def tc50_dashboard_event_written(tok):
    rest = create_restaurant(tok)
    rid = rest["id"]
    with mongo() as m:
        coll = m.get_database("fooddeliverymongo")["restaurant_events"]
        # try several possible field names — spec says action but service may use eventType
        before = coll.count_documents({"$or": [{"action": "DASHBOARD_VIEWED"}, {"eventType": "DASHBOARD_VIEWED"}]})
        r = requests.get(f"{BASE_URL}/api/restaurants/{rid}/dashboard", headers=auth(tok))
        assert r.status_code == 200, f"GET dashboard status={r.status_code}"
        time.sleep(0.5)  # async write
        after = coll.count_documents({"$or": [{"action": "DASHBOARD_VIEWED"}, {"eventType": "DASHBOARD_VIEWED"}]})
        assert after > before, f"mongo DASHBOARD_VIEWED count did not increase (before={before}, after={after})"


def tc51_dashboard_404_on_missing(tok):
    r = requests.get(f"{BASE_URL}/api/restaurants/9223372036854775807/dashboard", headers=auth(tok))
    assert r.status_code == 404, f"expected 404, got {r.status_code} body={r.text[:200]}"


def tc52_dashboard_zero_orders(tok):
    rest = create_restaurant(tok)
    rid = rest["id"]
    with pg() as c, c.cursor() as cur:
        cur.execute("DELETE FROM orders WHERE restaurant_id=%s", (rid,))
    r = requests.get(f"{BASE_URL}/api/restaurants/{rid}/dashboard", headers=auth(tok))
    assert r.status_code == 200, f"status={r.status_code}"
    body = r.json()
    total_orders = body.get("totalOrders", body.get("total_orders"))
    total_rev = body.get("totalRevenue", body.get("total_revenue"))
    assert total_orders == 0, f"expected 0 orders, got {total_orders}"
    assert float(total_rev) == 0.0, f"expected 0 revenue, got {total_rev}"


def tc53_dashboard_no_token(tok):
    rest = create_restaurant(tok)
    r = requests.get(f"{BASE_URL}/api/restaurants/{rest['id']}/dashboard")
    assert r.status_code == 401, f"expected 401, got {r.status_code}"


# ============================================================
# S2-F1 — search by cuisine + rating range (TC233–TC236)
# ============================================================

def tc233_search_cuisine_and_range(tok):
    # 3 restaurants: EGYPTIAN/4.5, ITALIAN/3.8, EGYPTIAN/4.9
    a = create_restaurant(tok, cuisine="EGYPTIAN")
    b = create_restaurant(tok, cuisine="ITALIAN")
    c = create_restaurant(tok, cuisine="EGYPTIAN")
    jdbc_set_rating(a["id"], 4.5)
    jdbc_set_rating(b["id"], 3.8)
    jdbc_set_rating(c["id"], 4.9)
    r = requests.get(
        f"{BASE_URL}/api/restaurants/search",
        params={"cuisineType": "EGYPTIAN", "minRating": 4.0, "maxRating": 5.0},
        headers=auth(tok),
    )
    assert r.status_code == 200, f"status={r.status_code} body={r.text[:200]}"
    items = r.json()
    ids = {x["id"] for x in items}
    # we created the only EGYPTIAN restaurants in [4.0,5.0] just now? not guaranteed across runs,
    # but at minimum a and c must appear and b must not.
    assert a["id"] in ids and c["id"] in ids, f"missing {a['id']} or {c['id']} in {ids}"
    assert b["id"] not in ids, f"ITALIAN {b['id']} leaked into EGYPTIAN-only results"
    # first item in OUR pair should be the higher-rated (c=4.9 > a=4.5)
    pair = [x for x in items if x["id"] in (a["id"], c["id"])]
    assert pair and pair[0]["id"] == c["id"], f"expected highest rating first; got {[(x['id'],x['rating']) for x in pair]}"


def tc234_range_without_cuisine(tok):
    a = create_restaurant(tok, cuisine="EGYPTIAN")
    b = create_restaurant(tok, cuisine="ITALIAN")
    jdbc_set_rating(a["id"], 4.5)
    jdbc_set_rating(b["id"], 3.8)
    r = requests.get(
        f"{BASE_URL}/api/restaurants/search",
        params={"minRating": 3.0, "maxRating": 4.0},
        headers=auth(tok),
    )
    assert r.status_code == 200, f"status={r.status_code}"
    ids = {x["id"] for x in r.json()}
    assert b["id"] in ids, f"ITALIAN(3.8) should be in [3.0,4.0] result; got {ids}"
    assert a["id"] not in ids, f"EGYPTIAN(4.5) should NOT be in [3.0,4.0]; got {ids}"


def tc235_sort_desc(tok):
    a = create_restaurant(tok, cuisine="EGYPTIAN")
    b = create_restaurant(tok, cuisine="EGYPTIAN")
    c = create_restaurant(tok, cuisine="EGYPTIAN")
    jdbc_set_rating(a["id"], 4.0)
    jdbc_set_rating(b["id"], 5.0)
    jdbc_set_rating(c["id"], 4.5)
    r = requests.get(
        f"{BASE_URL}/api/restaurants/search",
        params={"cuisineType": "EGYPTIAN", "minRating": 3.0, "maxRating": 5.0},
        headers=auth(tok),
    )
    assert r.status_code == 200
    items = [x for x in r.json() if x["id"] in (a["id"], b["id"], c["id"])]
    ratings = [x["rating"] for x in items]
    assert ratings == sorted(ratings, reverse=True), f"not DESC sorted: {ratings}"


def tc236_invalid_range(tok):
    r = requests.get(
        f"{BASE_URL}/api/restaurants/search",
        params={"minRating": 5.0, "maxRating": 3.0},
        headers=auth(tok),
    )
    assert r.status_code == 400, f"expected 400, got {r.status_code}"


# ============================================================
# S2-F2 — details merge (TC237–TC239)
# ============================================================

def tc237_details_merge(tok):
    rest = create_restaurant(tok, details={"address": "X", "deliveryRadius": 5, "minOrder": 50})
    r = requests.put(
        f"{BASE_URL}/api/restaurants/{rest['id']}/details",
        json={"deliveryRadius": 15, "openingHours": "9-23"},
        headers=auth(tok),
    )
    assert r.status_code == 200, f"status={r.status_code}"
    body = r.json()
    d = body["details"]
    assert d.get("address") == "X", f"address lost: {d}"
    assert d.get("deliveryRadius") == 15, f"deliveryRadius not updated: {d}"
    assert d.get("openingHours") == "9-23", f"openingHours not added: {d}"
    assert d.get("minOrder") == 50, f"minOrder lost: {d}"


def tc238_same_key_overwrites(tok):
    rest = create_restaurant(tok, details={"deliveryRadius": 5})
    r = requests.put(
        f"{BASE_URL}/api/restaurants/{rest['id']}/details",
        json={"deliveryRadius": 15},
        headers=auth(tok),
    )
    assert r.status_code == 200
    assert r.json()["details"]["deliveryRadius"] == 15


def tc239_details_404(tok):
    r = requests.put(
        f"{BASE_URL}/api/restaurants/999999/details",
        json={"x": 1},
        headers=auth(tok),
    )
    assert r.status_code == 404, f"expected 404, got {r.status_code}"


# ============================================================
# S2-F3 — revenue (TC240–TC243)
# ============================================================

def _rev_avg(body):
    return (body.get("averageOrderAmount")
            or body.get("averageOrder")
            or body.get("averageOrderValue")
            or body.get("average_order_amount"))


def tc240_revenue_happy(tok):
    rest = create_restaurant(tok)
    rid = rest["id"]
    user_id, _ = ensure_admin_user()
    for amt in (100, 150, 200, 250, 300):
        jdbc_insert_order(rid, user_id, "DELIVERED", amt)
    today = dt.date.today()
    start = (today - dt.timedelta(days=1)).isoformat()
    end = (today + dt.timedelta(days=1)).isoformat()
    r = requests.get(
        f"{BASE_URL}/api/restaurants/{rid}/revenue",
        params={"startDate": start, "endDate": end},
        headers=auth(tok),
    )
    assert r.status_code == 200, f"status={r.status_code} body={r.text[:200]}"
    body = r.json()
    total_orders = body.get("totalOrders") or body.get("total_orders")
    total_rev = body.get("totalRevenue") or body.get("total_revenue")
    avg = _rev_avg(body)
    assert total_orders == 5, f"expected 5 orders, got {total_orders} (body={body})"
    assert abs(float(total_rev) - 1000.0) < 0.01, f"expected 1000 rev, got {total_rev}"
    assert avg is not None and abs(float(avg) - 200.0) < 0.01, f"expected avg 200, got {avg} (body={body})"


def tc241_revenue_zero_far_future(tok):
    rest = create_restaurant(tok)
    r = requests.get(
        f"{BASE_URL}/api/restaurants/{rest['id']}/revenue",
        params={"startDate": "2030-01-01", "endDate": "2030-12-31"},
        headers=auth(tok),
    )
    assert r.status_code == 200, f"status={r.status_code} body={r.text[:200]}"
    body = r.json()
    total_orders = body.get("totalOrders") or body.get("total_orders") or 0
    total_rev = body.get("totalRevenue") or body.get("total_revenue") or 0
    assert total_orders == 0, f"expected 0 orders, got {total_orders} (body={body})"
    assert float(total_rev) == 0.0, f"expected 0 rev, got {total_rev}"


def tc242_revenue_only_delivered(tok):
    rest = create_restaurant(tok)
    rid = rest["id"]
    user_id, _ = ensure_admin_user()
    jdbc_insert_order(rid, user_id, "DELIVERED", 100)
    jdbc_insert_order(rid, user_id, "CANCELLED", 999)
    jdbc_insert_order(rid, user_id, "PLACED", 999)
    today = dt.date.today()
    r = requests.get(
        f"{BASE_URL}/api/restaurants/{rid}/revenue",
        params={"startDate": (today - dt.timedelta(days=1)).isoformat(),
                "endDate": (today + dt.timedelta(days=1)).isoformat()},
        headers=auth(tok),
    )
    assert r.status_code == 200
    body = r.json()
    total_rev = body.get("totalRevenue") or body.get("total_revenue")
    assert abs(float(total_rev) - 100.0) < 0.01, f"expected 100 (DELIVERED only), got {total_rev}"


def tc243_revenue_404(tok):
    r = requests.get(
        f"{BASE_URL}/api/restaurants/999999/revenue",
        params={"startDate": "2024-01-01", "endDate": "2024-12-31"},
        headers=auth(tok),
    )
    assert r.status_code == 404, f"expected 404, got {r.status_code}"


# ============================================================
# S2-F4 — status transitions (TC244–TC247)
# Note: spec uses PUT /api/restaurants/{id} with status in body.
# Actual implementation uses PUT /api/restaurants/{id}/status.
# We test the actual endpoint.
# ============================================================

def _put_status(tok, rid, status):
    return requests.put(
        f"{BASE_URL}/api/restaurants/{rid}/status",
        json={"status": status},
        headers=auth(tok),
    )


def tc244_suspend_with_active_order(tok):
    rest = create_restaurant(tok)
    user_id, _ = ensure_admin_user()
    jdbc_insert_order(rest["id"], user_id, "CONFIRMED", 50)
    r = _put_status(tok, rest["id"], "SUSPENDED")
    assert r.status_code == 400, f"expected 400, got {r.status_code} body={r.text[:200]}"


def tc245_suspend_after_delivery(tok):
    rest = create_restaurant(tok)
    user_id, _ = ensure_admin_user()
    jdbc_insert_order(rest["id"], user_id, "DELIVERED", 50)
    r = _put_status(tok, rest["id"], "SUSPENDED")
    assert r.status_code == 200, f"expected 200, got {r.status_code} body={r.text[:200]}"
    with pg() as c, c.cursor() as cur:
        cur.execute("SELECT status FROM restaurants WHERE id=%s", (rest["id"],))
        assert cur.fetchone()[0] == "SUSPENDED"


def tc246_close_with_active_order(tok):
    rest = create_restaurant(tok)
    user_id, _ = ensure_admin_user()
    jdbc_insert_order(rest["id"], user_id, "CONFIRMED", 50)
    r = _put_status(tok, rest["id"], "CLOSED")
    assert r.status_code == 400, f"expected 400 (active orders block CLOSED too), got {r.status_code}"


def tc247_status_404(tok):
    r = _put_status(tok, 999999, "SUSPENDED")
    assert r.status_code == 404, f"expected 404, got {r.status_code}"


# ============================================================
# S2-F5 — details search (TC248–TC251)
# ============================================================

def tc248_details_search_with_status(tok):
    n = nonce()
    a = create_restaurant(tok, status="OPEN", details={"key_" + n: 10})
    b = create_restaurant(tok, status="CLOSED", details={"key_" + n: 10})
    c = create_restaurant(tok, status="OPEN", details={"key_" + n: 999})
    r = requests.get(
        f"{BASE_URL}/api/restaurants/details/search",
        params={"key": "key_" + n, "value": "10", "status": "OPEN"},
        headers=auth(tok),
    )
    assert r.status_code == 200, f"status={r.status_code} body={r.text[:200]}"
    ids = {x["id"] for x in r.json()}
    assert a["id"] in ids
    assert b["id"] not in ids, f"closed leaked in: {ids}"
    assert c["id"] not in ids, f"value-mismatch leaked in: {ids}"


def tc249_details_search_without_status(tok):
    n = nonce()
    a = create_restaurant(tok, status="OPEN", details={"key_" + n: 10})
    b = create_restaurant(tok, status="CLOSED", details={"key_" + n: 10})
    r = requests.get(
        f"{BASE_URL}/api/restaurants/details/search",
        params={"key": "key_" + n, "value": "10"},
        headers=auth(tok),
    )
    assert r.status_code == 200, f"status={r.status_code} body={r.text[:200]}"
    ids = {x["id"] for x in r.json()}
    assert a["id"] in ids and b["id"] in ids, f"expected both, got {ids}"


def tc250_details_search_no_match(tok):
    n = nonce()
    create_restaurant(tok, details={"key_" + n: 10})
    r = requests.get(
        f"{BASE_URL}/api/restaurants/details/search",
        params={"key": "key_" + n, "value": "999"},
        headers=auth(tok),
    )
    assert r.status_code == 200, f"status={r.status_code}"
    assert r.json() == [], f"expected empty, got {r.json()}"


def tc251_details_search_blank_key(tok):
    r = requests.get(
        f"{BASE_URL}/api/restaurants/details/search",
        params={"key": "", "value": "10"},
        headers=auth(tok),
    )
    assert not (500 <= r.status_code < 600), f"expected non-5xx, got {r.status_code}"


# ============================================================
# S2-F6 — top-rated (TC252–TC254)
# Note: spec says /api/restaurants/top-rated, actual is /api/restaurants/reports/top-rated.
# ============================================================

TOP_RATED_PATH = "/api/restaurants/reports/top-rated"


def tc252_top_rated_ranking(tok):
    a = create_restaurant(tok)
    b = create_restaurant(tok)
    c = create_restaurant(tok)
    jdbc_set_rating(a["id"], 4.9)
    jdbc_set_rating(b["id"], 4.5)
    jdbc_set_rating(c["id"], 4.2)
    r = requests.get(f"{BASE_URL}{TOP_RATED_PATH}", params={"limit": 2}, headers=auth(tok))
    assert r.status_code == 200, f"status={r.status_code} body={r.text[:200]}"
    items = r.json()
    assert len(items) == 2, f"expected size==2, got {len(items)}"
    # spec says first item rating==4.9. since other restaurants exist, assert ordering on the top 2 strictly DESC.
    ratings = [it.get("rating") for it in items]
    assert ratings == sorted(ratings, reverse=True), f"not DESC: {ratings}"


def tc253_top_rated_overflow(tok):
    a = create_restaurant(tok); b = create_restaurant(tok); c = create_restaurant(tok)
    jdbc_set_rating(a["id"], 4.9)
    jdbc_set_rating(b["id"], 4.5)
    jdbc_set_rating(c["id"], 4.2)
    r = requests.get(f"{BASE_URL}{TOP_RATED_PATH}", params={"limit": 10}, headers=auth(tok))
    assert r.status_code == 200, f"status={r.status_code}"
    assert len(r.json()) >= 3, f"expected >=3, got {len(r.json())}"


def tc254_top_rated_dto_fields(tok):
    rest = create_restaurant(tok)
    jdbc_set_rating(rest["id"], 4.9)
    user_id, _ = ensure_admin_user()
    jdbc_insert_order(rest["id"], user_id, "DELIVERED", 50)
    r = requests.get(f"{BASE_URL}{TOP_RATED_PATH}", params={"limit": 100}, headers=auth(tok))
    assert r.status_code == 200
    items = r.json()
    target = next((x for x in items if x.get("restaurantId") == rest["id"] or x.get("id") == rest["id"]), None)
    assert target is not None, f"created restaurant not in top-rated; ids={[x.get('restaurantId') or x.get('id') for x in items]}"
    keys = set(target.keys())
    has_rid = "restaurantId" in keys or "id" in keys
    has_total = "totalOrders" in keys or "total_orders" in keys or "orderCount" in keys
    assert has_rid, f"missing restaurantId/id in DTO: {keys}"
    assert "name" in keys, f"missing name in DTO: {keys}"
    assert "rating" in keys, f"missing rating in DTO: {keys}"
    assert has_total, f"missing totalOrders in DTO: {keys}"


# ============================================================
# S2-F7 — rate (TC255–TC259)
# ============================================================

def tc255_first_rating(tok):
    rest = create_restaurant(tok)
    rid = rest["id"]
    user_id, _ = ensure_admin_user()
    oid = jdbc_insert_order(rid, user_id, "DELIVERED", 50)
    r = requests.post(
        f"{BASE_URL}/api/restaurants/{rid}/rate",
        json={"orderId": oid, "rating": 5},
        headers=auth(tok),
    )
    assert r.status_code == 200, f"status={r.status_code} body={r.text[:200]}"
    with pg() as c, c.cursor() as cur:
        cur.execute("SELECT rating FROM restaurants WHERE id=%s", (rid,))
        rating = cur.fetchone()[0]
    assert abs(float(rating) - 5.0) < 0.01, f"expected rating=5.0, got {rating}"


def tc256_running_average(tok):
    rest = create_restaurant(tok)
    rid = rest["id"]
    user_id, _ = ensure_admin_user()
    o1 = jdbc_insert_order(rid, user_id, "DELIVERED", 50)
    o2 = jdbc_insert_order(rid, user_id, "DELIVERED", 50)
    r1 = requests.post(f"{BASE_URL}/api/restaurants/{rid}/rate", json={"orderId": o1, "rating": 5}, headers=auth(tok))
    r2 = requests.post(f"{BASE_URL}/api/restaurants/{rid}/rate", json={"orderId": o2, "rating": 3}, headers=auth(tok))
    assert r1.status_code == 200 and r2.status_code == 200, f"r1={r1.status_code} r2={r2.status_code}"
    with pg() as c, c.cursor() as cur:
        cur.execute("SELECT rating FROM restaurants WHERE id=%s", (rid,))
        rating = cur.fetchone()[0]
    assert abs(float(rating) - 4.0) < 0.01, f"expected avg=4.0, got {rating}"


def tc257_rating_above_5(tok):
    rest = create_restaurant(tok)
    rid = rest["id"]
    user_id, _ = ensure_admin_user()
    oid = jdbc_insert_order(rid, user_id, "DELIVERED", 50)
    r = requests.post(f"{BASE_URL}/api/restaurants/{rid}/rate", json={"orderId": oid, "rating": 6}, headers=auth(tok))
    assert r.status_code == 400, f"expected 400, got {r.status_code}"


def tc258_rate_non_delivered(tok):
    rest = create_restaurant(tok)
    rid = rest["id"]
    user_id, _ = ensure_admin_user()
    oid = jdbc_insert_order(rid, user_id, "PLACED", 50)
    r = requests.post(f"{BASE_URL}/api/restaurants/{rid}/rate", json={"orderId": oid, "rating": 5}, headers=auth(tok))
    assert r.status_code == 400, f"expected 400, got {r.status_code}"


def tc259_rate_cross_restaurant(tok):
    a = create_restaurant(tok)
    b = create_restaurant(tok)
    user_id, _ = ensure_admin_user()
    oid_b = jdbc_insert_order(b["id"], user_id, "DELIVERED", 50)
    r = requests.post(f"{BASE_URL}/api/restaurants/{a['id']}/rate",
                      json={"orderId": oid_b, "rating": 5}, headers=auth(tok))
    assert r.status_code == 400, f"expected 400, got {r.status_code}"


# ============================================================
# S2-F8 — toggle menu item (TC260–TC264)
# ============================================================

def _toggle(tok, rid, mid, by):
    return requests.put(
        f"{BASE_URL}/api/restaurants/{rid}/menu-items/{mid}/toggle",
        json={"toggledBy": by},
        headers=auth(tok),
    )


def tc260_toggle_happy_admin(tok):
    rest = create_restaurant(tok)
    mid = jdbc_insert_menu_item(rest["id"], available=True)
    admin_uid, _ = ensure_admin_user()
    r = _toggle(tok, rest["id"], mid, admin_uid)
    assert r.status_code == 200, f"status={r.status_code} body={r.text[:200]}"
    with pg() as c, c.cursor() as cur:
        cur.execute("SELECT available, metadata FROM menu_items WHERE id=%s", (mid,))
        avail, meta = cur.fetchone()
    assert avail is False, f"expected available=false, got {avail}"
    assert meta and "toggledAt" in meta and "toggledBy" in meta, f"missing audit fields: {meta}"


def tc261_toggle_blocked_pending_orderitem(tok):
    rest = create_restaurant(tok)
    mid = jdbc_insert_menu_item(rest["id"], available=True)
    admin_uid, _ = ensure_admin_user()
    # need a PENDING order_item against this menu_item
    oid = jdbc_insert_order(rest["id"], admin_uid, "CONFIRMED", 10)
    jdbc_insert_order_item(oid, mid, "PENDING")
    r = _toggle(tok, rest["id"], mid, admin_uid)
    assert r.status_code == 400, f"expected 400, got {r.status_code} body={r.text[:200]}"


def tc262_toggle_non_admin_403(tok):
    rest = create_restaurant(tok)
    mid = jdbc_insert_menu_item(rest["id"], available=True)
    # create a non-admin user in the users table
    with pg() as c, c.cursor() as cur:
        cur.execute(
            """INSERT INTO users (email, name, password, phone, role, status, created_at)
               VALUES (%s, %s, %s, %s, 'CUSTOMER', 'ACTIVE', now()) RETURNING id""",
            (f"customer_{nonce()}@grader.io", "Cust", "x", f"+1{uuid.uuid4().int % 10**10:010d}"),
        )
        cust_id = cur.fetchone()[0]
    r = _toggle(tok, rest["id"], mid, cust_id)
    assert r.status_code == 403, f"expected 403, got {r.status_code} body={r.text[:200]}"


def tc263_toggle_cross_restaurant(tok):
    a = create_restaurant(tok)
    b = create_restaurant(tok)
    mid_b = jdbc_insert_menu_item(b["id"], available=True)
    admin_uid, _ = ensure_admin_user()
    r = _toggle(tok, a["id"], mid_b, admin_uid)
    assert r.status_code == 400, f"expected 400 (item belongs to b), got {r.status_code}"


def tc264_toggle_404(tok):
    rest = create_restaurant(tok)
    admin_uid, _ = ensure_admin_user()
    r = _toggle(tok, rest["id"], 999999, admin_uid)
    assert r.status_code == 404, f"expected 404, got {r.status_code}"


# ============================================================
# S2-F9 — unavailable items report (TC265–TC267)
# Note: spec says /api/restaurants/reports/unavailable-items.
# Actual: /api/restaurants/menu-items/unavailable.
# ============================================================

UNAVAIL_PATH = "/api/restaurants/menu-items/unavailable"


def tc265_unavailable_happy(tok):
    flush_cache("restaurant-service::S2-F9")
    a = create_restaurant(tok); b = create_restaurant(tok); c = create_restaurant(tok)
    jdbc_insert_menu_item(a["id"], available=False)
    jdbc_insert_menu_item(b["id"], available=True)
    jdbc_insert_menu_item(c["id"], available=False)
    jdbc_insert_menu_item(c["id"], available=False)
    jdbc_insert_menu_item(c["id"], available=False)
    r = requests.get(f"{BASE_URL}{UNAVAIL_PATH}", headers=auth(tok))
    assert r.status_code == 200, f"status={r.status_code} body={r.text[:200]}"
    items = r.json()
    rids = {x.get("restaurantId") or x.get("id") for x in items}
    assert a["id"] in rids and c["id"] in rids, f"missing a/c; got {rids}"
    assert b["id"] not in rids, f"all-available b leaked in: {rids}"


def tc266_unavailable_all_available(tok):
    flush_cache("restaurant-service::S2-F9")
    rest = create_restaurant(tok)
    jdbc_insert_menu_item(rest["id"], available=True)
    r = requests.get(f"{BASE_URL}{UNAVAIL_PATH}", headers=auth(tok))
    assert r.status_code == 200, f"status={r.status_code} body={r.text[:200]}"
    rids = {x.get("restaurantId") or x.get("id") for x in r.json()}
    assert rest["id"] not in rids, f"all-available restaurant leaked into report: {rids}"


def tc267_unavailable_count(tok):
    flush_cache("restaurant-service::S2-F9")
    rest = create_restaurant(tok)
    jdbc_insert_menu_item(rest["id"], available=False)
    jdbc_insert_menu_item(rest["id"], available=False)
    jdbc_insert_menu_item(rest["id"], available=True)
    r = requests.get(f"{BASE_URL}{UNAVAIL_PATH}", headers=auth(tok))
    assert r.status_code == 200, f"status={r.status_code} body={r.text[:200]}"
    target = next((x for x in r.json() if (x.get("restaurantId") or x.get("id")) == rest["id"]), None)
    assert target is not None, "created restaurant missing from report"
    assert target.get("unavailableCount") == 2, f"expected 2, got {target.get('unavailableCount')}"


# ============================================================
# main
# ============================================================

def main():
    tok = admin_token()
    print(f"== restaurant-service M2 tests ==  base={BASE_URL}\n")

    print("-- S2-F12 dashboard --")
    run("TC48", lambda: tc48_dashboard_happy(tok))
    run("TC49", lambda: tc49_dashboard_aggregates_match(tok))
    run("TC50", lambda: tc50_dashboard_event_written(tok))
    run("TC51", lambda: tc51_dashboard_404_on_missing(tok))
    run("TC52", lambda: tc52_dashboard_zero_orders(tok))
    run("TC53", lambda: tc53_dashboard_no_token(tok))

    print("\n-- S2-F1 search --")
    run("TC233", lambda: tc233_search_cuisine_and_range(tok))
    run("TC234", lambda: tc234_range_without_cuisine(tok))
    run("TC235", lambda: tc235_sort_desc(tok))
    run("TC236", lambda: tc236_invalid_range(tok))

    print("\n-- S2-F2 details merge --")
    run("TC237", lambda: tc237_details_merge(tok))
    run("TC238", lambda: tc238_same_key_overwrites(tok))
    run("TC239", lambda: tc239_details_404(tok))

    print("\n-- S2-F3 revenue --")
    run("TC240", lambda: tc240_revenue_happy(tok))
    run("TC241", lambda: tc241_revenue_zero_far_future(tok))
    run("TC242", lambda: tc242_revenue_only_delivered(tok))
    run("TC243", lambda: tc243_revenue_404(tok))

    print("\n-- S2-F4 status --")
    run("TC244", lambda: tc244_suspend_with_active_order(tok))
    run("TC245", lambda: tc245_suspend_after_delivery(tok))
    run("TC246", lambda: tc246_close_with_active_order(tok))
    run("TC247", lambda: tc247_status_404(tok))

    print("\n-- S2-F5 details search --")
    run("TC248", lambda: tc248_details_search_with_status(tok))
    run("TC249", lambda: tc249_details_search_without_status(tok))
    run("TC250", lambda: tc250_details_search_no_match(tok))
    run("TC251", lambda: tc251_details_search_blank_key(tok))

    print("\n-- S2-F6 top-rated --")
    run("TC252", lambda: tc252_top_rated_ranking(tok))
    run("TC253", lambda: tc253_top_rated_overflow(tok))
    run("TC254", lambda: tc254_top_rated_dto_fields(tok))

    print("\n-- S2-F7 rate --")
    run("TC255", lambda: tc255_first_rating(tok))
    run("TC256", lambda: tc256_running_average(tok))
    run("TC257", lambda: tc257_rating_above_5(tok))
    run("TC258", lambda: tc258_rate_non_delivered(tok))
    run("TC259", lambda: tc259_rate_cross_restaurant(tok))

    print("\n-- S2-F8 toggle --")
    run("TC260", lambda: tc260_toggle_happy_admin(tok))
    run("TC261", lambda: tc261_toggle_blocked_pending_orderitem(tok))
    run("TC262", lambda: tc262_toggle_non_admin_403(tok))
    run("TC263", lambda: tc263_toggle_cross_restaurant(tok))
    run("TC264", lambda: tc264_toggle_404(tok))

    print("\n-- S2-F9 unavailable --")
    run("TC265", lambda: tc265_unavailable_happy(tok))
    run("TC266", lambda: tc266_unavailable_all_available(tok))
    run("TC267", lambda: tc267_unavailable_count(tok))

    # summary
    p = sum(1 for _, s, _ in results if s == "PASS")
    f = sum(1 for _, s, _ in results if s == "FAIL")
    e = sum(1 for _, s, _ in results if s == "ERROR")
    print(f"\n========== summary ==========")
    print(f"passed: {p}   failed: {f}   errors: {e}   total: {len(results)}")
    if f or e:
        print("\nFailures/errors:")
        for tc, st, msg in results:
            if st != "PASS":
                print(f"  {st:5s} {tc}  {msg[:300]}")
    return 0 if (f == 0 and e == 0) else 1


if __name__ == "__main__":
    sys.exit(main())
