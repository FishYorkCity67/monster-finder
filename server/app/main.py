"""Monster Finder – Backend für den Raspberry Pi.

Kombiniert:
  * zippopotam.us / Nominatim  -> PLZ in Koordinaten
  * marktguru                  -> aktuelle Prospektangebote für Monster Energy
  * OpenStreetMap Overpass     -> nächstgelegene Filiale jeder Kette
"""
import asyncio
import re
from contextlib import asynccontextmanager
from datetime import datetime, timezone

import httpx
from fastapi import FastAPI, HTTPException, Query

from . import config
from .cache import TTLCache
from .chains import display_name
from .providers.geocode import GeocodeError, locate
from .providers.offers import Offer, search_offers
from .providers.stores import Store, nearby_stores

cache = TTLCache(config.CACHE_TTL_SECONDS)
http: httpx.AsyncClient


@asynccontextmanager
async def lifespan(_: FastAPI):
    global http
    http = httpx.AsyncClient(headers=config.HTTP_HEADERS, follow_redirects=True)
    yield
    await http.aclose()


app = FastAPI(title="Monster Finder", version="1.0", lifespan=lifespan)


def _store_json(s: Store | None) -> dict | None:
    if not s:
        return None
    return {"name": s.name, "address": s.address, "lat": s.lat, "lon": s.lon, "distance_km": round(s.distance_km, 2)}


def _row(kind: str, chain: str, retailer: str, title: str, description: str, price: float,
         liters: float, cans: int, per_can: float, store: Store | None,
         valid_from: str | None = None, valid_to: str | None = None) -> dict:
    return {
        "kind": kind,
        "chain": chain,
        "chain_name": display_name(chain, retailer),
        "title": title,
        "description": description,
        "price": round(price, 2),
        "liters": round(liters, 3),
        "cans": cans,
        "price_per_can": round(per_can, 2),
        "price_per_can_with_pfand": round(per_can + config.PFAND_EUR, 2),
        "price_per_liter": round(per_can / config.CAN_LITERS, 2),
        "valid_from": valid_from,
        "valid_to": valid_to,
        "store": _store_json(store),
    }


def build_results(offers: list[Offer], stores: dict[str, list[Store]], stores_ok: bool,
                  only_nearby: bool) -> list[dict]:
    rows: list[dict] = []
    seen: set[tuple] = set()
    for o in offers:
        key = (o.chain, round(o.price, 2), round(o.liters, 2))
        if key in seen:
            continue
        seen.add(key)
        store = (stores.get(o.chain) or [None])[0]
        if only_nearby and stores_ok and store is None:
            continue
        rows.append(_row("offer", o.chain, o.retailer, o.title, o.description, o.price, o.liters,
                         o.cans, o.price_per_can, store, o.valid_from, o.valid_to))

    if config.INCLUDE_REFERENCE_PRICES:
        chains_with_offer = {r["chain"] for r in rows}
        for chain, ref in config.REFERENCE_PRICES.items():
            if chain in chains_with_offer or not stores.get(chain):
                continue
            rows.append(_row("reference", chain, chain, "Monster Energy 0,5 l",
                             "Richtpreis – kein aktuelles Angebot gefunden", ref, config.CAN_LITERS, 1, ref,
                             stores[chain][0]))

    # Günstigster Dosenpreis zuerst; bei Gleichstand das nähere Geschäft.
    rows.sort(key=lambda r: (r["price_per_can"], r["kind"] != "offer",
                             (r["store"] or {}).get("distance_km", 999)))
    return rows


@app.get("/health")
async def health():
    return {"ok": True}


@app.get("/api/search")
async def search(
    plz: str = Query(..., description="Deutsche Postleitzahl"),
    radius: float = Query(config.DEFAULT_RADIUS_KM, ge=1, le=25),
    only_nearby: bool = True,
):
    if not re.fullmatch(r"\d{5}", plz):
        raise HTTPException(400, "Bitte eine gültige 5-stellige Postleitzahl eingeben")

    cache_key = f"{plz}:{radius}:{only_nearby}"
    if (cached := cache.get(cache_key)) is not None:
        return {**cached, "cached": True}

    try:
        loc = await locate(http, plz)
    except GeocodeError as e:
        raise HTTPException(404, str(e))

    offers_res, stores_res = await asyncio.gather(
        search_offers(http, plz), nearby_stores(http, loc.lat, loc.lon, radius), return_exceptions=True
    )
    sources = {}
    if isinstance(offers_res, BaseException):
        sources["marktguru"] = f"Fehler: {offers_res}"
        offers_res = []
    else:
        sources["marktguru"] = f"{len(offers_res)} Angebote"
    if isinstance(stores_res, BaseException) or not stores_res:
        sources["openstreetmap"] = "Keine Filialdaten" if not isinstance(stores_res, BaseException) else f"Fehler: {stores_res}"
        stores_ok, stores_res = False, {}
    else:
        stores_ok = True
        sources["openstreetmap"] = f"{sum(len(v) for v in stores_res.values())} Filialen"

    payload = {
        "location": {"plz": loc.plz, "city": loc.city, "lat": loc.lat, "lon": loc.lon},
        "radius_km": radius,
        "pfand": config.PFAND_EUR,
        "updated_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "sources": sources,
        "results": build_results(offers_res, stores_res, stores_ok, only_nearby),
    }
    cache.set(cache_key, payload)
    return {**payload, "cached": False}
