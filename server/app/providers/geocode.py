"""PLZ -> Koordinaten. Primär zippopotam.us, Fallback OpenStreetMap Nominatim."""
from dataclasses import dataclass

import httpx


@dataclass
class Location:
    plz: str
    city: str
    lat: float
    lon: float


class GeocodeError(Exception):
    pass


async def locate(client: httpx.AsyncClient, plz: str) -> Location:
    try:
        r = await client.get(f"https://api.zippopotam.us/de/{plz}", timeout=8)
        if r.status_code == 200:
            place = r.json()["places"][0]
            return Location(plz, place["place name"], float(place["latitude"]), float(place["longitude"]))
    except (httpx.HTTPError, KeyError, IndexError, ValueError):
        pass

    try:
        r = await client.get(
            "https://nominatim.openstreetmap.org/search",
            params={"postalcode": plz, "country": "de", "format": "json", "limit": 1},
            timeout=10,
        )
        hits = r.json()
        if hits:
            h = hits[0]
            city = h.get("display_name", "").split(",")[1].strip() if "," in h.get("display_name", "") else plz
            return Location(plz, city, float(h["lat"]), float(h["lon"]))
    except (httpx.HTTPError, KeyError, ValueError):
        pass

    raise GeocodeError(f"Postleitzahl {plz} nicht gefunden")
