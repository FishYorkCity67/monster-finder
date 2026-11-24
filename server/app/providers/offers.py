"""Aktuelle Prospektangebote über die (inoffizielle) marktguru-API."""
import json
import re
from dataclasses import dataclass

import httpx

from .. import config
from ..chains import chain_id

SEARCH_URL = "https://api.marktguru.de/api/v1/offers/search"
EXCLUDE = re.compile(r"backe|munch|monster high|truck|puzzle|spiel|figur|plüsch|plusch|tasse|buch", re.I)


@dataclass
class Offer:
    chain: str
    retailer: str
    title: str
    description: str
    price: float          # Angebotspreis wie im Prospekt (ohne Pfand)
    liters: float         # Gesamtvolumen des Angebots
    cans: int             # Anzahl Dosen (geschätzt)
    valid_from: str | None
    valid_to: str | None
    source: str = "marktguru"

    @property
    def price_per_can(self) -> float:
        return self.price / max(self.liters / config.CAN_LITERS, 1e-9)

    @property
    def price_per_liter(self) -> float:
        return self.price / self.liters


class OfferError(Exception):
    pass


_keys: dict[str, str] = {}


async def _get_keys(client: httpx.AsyncClient, refresh: bool = False) -> dict[str, str]:
    if config.MARKTGURU_API_KEY and config.MARKTGURU_CLIENT_KEY:
        return {"x-apikey": config.MARKTGURU_API_KEY, "x-clientkey": config.MARKTGURU_CLIENT_KEY}
    if _keys and not refresh:
        return _keys
    if not config.MARKTGURU_AUTO_KEYS:
        raise OfferError("Keine marktguru-Keys konfiguriert (siehe .env)")
    html = (await client.get("https://www.marktguru.de/", timeout=15)).text
    api = re.search(r'"apiKey"\s*:\s*"([^"]+)"', html)
    cli = re.search(r'"clientKey"\s*:\s*"([^"]+)"', html)
    if not (api and cli):
        raise OfferError("marktguru-Keys konnten nicht ermittelt werden")
    _keys.update({"x-apikey": api.group(1), "x-clientkey": cli.group(1)})
    return _keys


def _num(s: str) -> float:
    return float(s.replace(",", "."))


def extract_liters(item: dict, text: str) -> float:
    """Ermittelt das Gesamtvolumen in Litern – aus strukturierten Feldern oder dem Text."""
    unit = ((item.get("unit") or {}).get("shortName") or "").lower()
    qty = item.get("quantity") or item.get("volume")
    if isinstance(qty, (int, float)) and qty > 0:
        if unit == "l":
            return float(qty)
        if unit == "ml":
            return qty / 1000
        if unit == "cl":
            return qty / 100

    t = text.lower()
    multi = re.search(r"(\d+)\s*[x×]\s*(\d+(?:[.,]\d+)?)\s*-?\s*(ml|cl|l)\b", t)
    if multi:
        n, v, u = int(multi.group(1)), _num(multi.group(2)), multi.group(3)
        return n * v / {"l": 1, "cl": 100, "ml": 1000}[u]
    single = re.search(r"(\d+(?:[.,]\d+)?)\s*-?\s*(ml|cl|l)\b", t)
    if single:
        v, u = _num(single.group(1)), single.group(2)
        return v / {"l": 1, "cl": 100, "ml": 1000}[u]

    price, ref = item.get("price"), item.get("referencePrice")
    if price and ref and unit in ("l", "") and ref > 0:
        return price / ref
    return config.CAN_LITERS


def parse_offers(data: dict) -> list[Offer]:
    offers: list[Offer] = []
    for item in data.get("results", []):
        product = item.get("product") or {}
        brand = ((product.get("brand") or item.get("brand") or {}).get("name") or "")
        title = " ".join(x for x in (brand, product.get("name", "")) if x).strip()
        desc = item.get("description") or ""
        text = f"{title} {desc}"
        if "monster" not in text.lower() or EXCLUDE.search(text):
            continue
        price = item.get("price")
        if not isinstance(price, (int, float)) or price <= 0:
            continue
        liters = extract_liters(item, text)
        if not 0.2 <= liters <= 12:
            continue
        for adv in item.get("advertisers") or [{}]:
            name = adv.get("name") or adv.get("uniqueName") or ""
            cid = chain_id(name) or (name.lower() or "unbekannt")
            dates = (item.get("validityDates") or [{}])[0]
            offers.append(Offer(
                chain=cid, retailer=name, title=title or "Monster Energy", description=desc.strip(),
                price=float(price), liters=liters, cans=max(1, round(liters / config.CAN_LITERS)),
                valid_from=(dates.get("from") or "")[:10] or None, valid_to=(dates.get("to") or "")[:10] or None,
            ))
    return offers


async def search_offers(client: httpx.AsyncClient, plz: str) -> list[Offer]:
    params = {"as": "web", "q": config.SEARCH_TERM, "limit": 100, "offset": 0, "zipCode": plz}
    for attempt in range(2):
        headers = await _get_keys(client, refresh=attempt > 0)
        r = await client.get(SEARCH_URL, params=params, headers=headers, timeout=15)
        if r.status_code in (401, 403) and attempt == 0 and not config.MARKTGURU_API_KEY:
            continue
        r.raise_for_status()
        try:
            return parse_offers(r.json())
        except json.JSONDecodeError as e:
            raise OfferError("Ungültige Antwort von marktguru") from e
    raise OfferError("marktguru hat die Anfrage abgelehnt")
