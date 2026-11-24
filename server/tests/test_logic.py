from app.chains import chain_id
from app.main import build_results
from app.providers.offers import parse_offers
from app.providers.stores import parse_overpass

LAT, LON = 52.532, 13.3922

MARKTGURU_SAMPLE = {
    "results": [
        {"price": 0.99, "description": "0,5-l-Dose, versch. Sorten, zzgl. 0,25 Pfand",
         "product": {"name": "Energy Drink", "brand": {"name": "Monster"}},
         "advertisers": [{"name": "Lidl"}], "validityDates": [{"from": "2026-10-06T00:00:00", "to": "2026-10-11T00:00:00"}]},
        {"price": 4.44, "description": "4 x 0,5 l", "product": {"name": "Energy Ultra", "brand": {"name": "Monster"}},
         "advertisers": [{"name": "REWE"}], "validityDates": [{"from": "2026-10-06", "to": "2026-10-11"}]},
        {"price": 1.11, "quantity": 500, "unit": {"shortName": "ml"},
         "product": {"name": "Energy", "brand": {"name": "Monster"}}, "advertisers": [{"name": "Kaufland"}]},
        {"price": 1.49, "description": "Quark 500 g", "product": {"name": "Monster Backe"},
         "advertisers": [{"name": "EDEKA"}]},
    ]
}

OVERPASS_SAMPLE = {
    "elements": [
        {"type": "node", "lat": 52.533, "lon": 13.393, "tags": {"shop": "supermarket", "name": "Lidl", "addr:street": "Chausseestr.", "addr:housenumber": "1"}},
        {"type": "way", "center": {"lat": 52.54, "lon": 13.40}, "tags": {"shop": "supermarket", "brand": "REWE", "name": "REWE City"}},
        {"type": "node", "lat": 52.52, "lon": 13.38, "tags": {"shop": "supermarket", "name": "Netto Marken-Discount"}},
        {"type": "node", "lat": 52.52, "lon": 13.38, "tags": {"shop": "kiosk", "name": "Späti Ali"}},
    ]
}


def test_chain_matching():
    assert chain_id("Netto Marken-Discount") == "netto"
    assert chain_id("ALDI SÜD") == "aldi"
    assert chain_id("REWE City") == "rewe"
    assert chain_id("Getränke Hoffmann") == "getraenke_hoffmann"
    assert chain_id("Späti Ali") is None


def test_parse_offers_filters_and_volumes():
    offers = parse_offers(MARKTGURU_SAMPLE)
    assert [o.chain for o in offers] == ["lidl", "rewe", "kaufland"]  # Monster Backe gefiltert
    lidl, rewe, kaufland = offers
    assert lidl.liters == 0.5 and lidl.price_per_can == 0.99
    assert rewe.liters == 2.0 and rewe.cans == 4 and round(rewe.price_per_can, 2) == 1.11
    assert kaufland.liters == 0.5
    assert lidl.valid_to == "2026-10-11"


def test_build_results_sorted_and_nearby_only():
    stores = parse_overpass(OVERPASS_SAMPLE, LAT, LON)
    assert set(stores) == {"lidl", "rewe", "netto"}
    rows = build_results(parse_offers(MARKTGURU_SAMPLE), stores, stores_ok=True, only_nearby=True)
    chains = [r["chain"] for r in rows]
    assert chains[0] == "lidl"
    assert "kaufland" not in chains          # kein Kaufland im Umkreis
    assert rows[0]["price_per_can_with_pfand"] == 1.24
    netto = next(r for r in rows if r["chain"] == "netto")
    assert netto["kind"] == "reference"
    assert [r["price_per_can"] for r in rows] == sorted(r["price_per_can"] for r in rows)
