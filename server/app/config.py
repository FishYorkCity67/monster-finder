import os

MARKTGURU_API_KEY = os.getenv("MARKTGURU_API_KEY", "")
MARKTGURU_CLIENT_KEY = os.getenv("MARKTGURU_CLIENT_KEY", "")
# Wenn keine Keys gesetzt sind, liest der Server die öffentlichen Web-Keys von marktguru.de aus.
MARKTGURU_AUTO_KEYS = os.getenv("MARKTGURU_AUTO_KEYS", "1") == "1"

SEARCH_TERM = os.getenv("SEARCH_TERM", "monster energy")
DEFAULT_RADIUS_KM = float(os.getenv("DEFAULT_RADIUS_KM", "5"))
CACHE_TTL_SECONDS = int(os.getenv("CACHE_TTL_SECONDS", "1800"))
PFAND_EUR = 0.25
CAN_LITERS = 0.5

# Richtpreise (ohne Pfand, pro 0,5-l-Dose) für Märkte ohne aktuelles Prospektangebot.
# Grobe Schätzwerte – passe sie gern an deine Gegend an. Leer lassen = abschalten.
INCLUDE_REFERENCE_PRICES = os.getenv("INCLUDE_REFERENCE_PRICES", "1") == "1"
REFERENCE_PRICES = {
    "lidl": 1.29,
    "aldi": 1.29,
    "kaufland": 1.29,
    "netto": 1.39,
    "penny": 1.39,
    "rewe": 1.49,
    "edeka": 1.49,
    "real": 1.39,
    "globus": 1.35,
    "rossmann": 1.39,
    "dm": 1.45,
}

HTTP_HEADERS = {"User-Agent": "MonsterFinder/1.0 (private Raspberry Pi project)"}
