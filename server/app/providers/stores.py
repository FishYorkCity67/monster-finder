"""Filialen in der Nähe über die OpenStreetMap Overpass API."""
import math
from dataclasses import dataclass

import httpx

from ..chains import chain_id

OVERPASS_URLS = [
    "https://overpass-api.de/api/interpreter",
    "https://overpass.kumi.systems/api/interpreter",
]
SHOP_TYPES = "supermarket|convenience|beverages|chemist|discount|variety_store|department_store|kiosk"


@dataclass
class Store:
    chain: str
    name: str
    address: str
    lat: float
    lon: float
    distance_km: float


def haversine_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    r = 6371.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp, dl = p2 - p1, math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


def _address(tags: dict) -> str:
    street = " ".join(x for x in (tags.get("addr:street"), tags.get("addr:housenumber")) if x)
    city = " ".join(x for x in (tags.get("addr:postcode"), tags.get("addr:city")) if x)
    return ", ".join(x for x in (street, city) if x)


def parse_overpass(data: dict, lat: float, lon: float) -> dict[str, list[Store]]:
    """Gruppiert gefundene Läden nach Kette, jeweils nach Entfernung sortiert."""
    by_chain: dict[str, list[Store]] = {}
    for el in data.get("elements", []):
        tags = el.get("tags", {})
        name = tags.get("brand") or tags.get("name") or ""
        cid = chain_id(name) or chain_id(tags.get("name", ""))
        if not cid:
            continue
        elat = el.get("lat") or el.get("center", {}).get("lat")
        elon = el.get("lon") or el.get("center", {}).get("lon")
        if elat is None or elon is None:
            continue
        by_chain.setdefault(cid, []).append(
            Store(cid, tags.get("name", name), _address(tags), elat, elon, haversine_km(lat, lon, elat, elon))
        )
    for stores in by_chain.values():
        stores.sort(key=lambda s: s.distance_km)
    return by_chain


async def nearby_stores(client: httpx.AsyncClient, lat: float, lon: float, radius_km: float) -> dict[str, list[Store]]:
    radius_m = int(radius_km * 1000)
    query = f"""
    [out:json][timeout:25];
    nwr["shop"~"^({SHOP_TYPES})$"](around:{radius_m},{lat},{lon});
    out center tags;
    """
    for url in OVERPASS_URLS:
        try:
            r = await client.post(url, data={"data": query}, timeout=30)
            if r.status_code == 200:
                return parse_overpass(r.json(), lat, lon)
        except (httpx.HTTPError, ValueError):
            continue
    return {}
