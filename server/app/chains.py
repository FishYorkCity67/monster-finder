"""Ordnet Händlernamen aus verschiedenen APIs einer gemeinsamen Ketten-ID zu."""
import re
import unicodedata

# Ketten-ID -> (Anzeigename, Suchmuster). Reihenfolge zählt: spezifische Muster zuerst.
CHAINS: dict[str, tuple[str, list[str]]] = {
    "netto_city": ("Netto (Dansk)", [r"netto.*(dansk|scottie|hund)"]),
    "netto": ("Netto", [r"\bnetto\b"]),
    "aldi": ("Aldi", [r"\baldi\b"]),
    "lidl": ("Lidl", [r"\blidl\b"]),
    "kaufland": ("Kaufland", [r"kaufland"]),
    "penny": ("Penny", [r"\bpenny\b"]),
    "rewe": ("REWE", [r"\brewe\b"]),
    "edeka": ("EDEKA", [r"edeka", r"\bmarktkauf\b", r"\bnp\b", r"\bdiska\b", r"\be center\b"]),
    "real": ("real", [r"^real\b"]),
    "globus": ("Globus", [r"\bglobus\b"]),
    "rossmann": ("Rossmann", [r"rossmann"]),
    "dm": ("dm", [r"^dm\b", r"dm-drogerie"]),
    "norma": ("Norma", [r"\bnorma\b"]),
    "tegut": ("tegut", [r"\btegut\b"]),
    "famila": ("famila", [r"\bfamila\b"]),
    "hit": ("HIT", [r"^hit\b"]),
    "trinkgut": ("trinkgut", [r"trinkgut"]),
    "getraenke_hoffmann": ("Getränke Hoffmann", [r"getranke hoffmann", r"getraenke hoffmann"]),
    "fristo": ("Fristo", [r"\bfristo\b"]),
    "action": ("Action", [r"^action\b"]),
    "tedi": ("TEDi", [r"\btedi\b"]),
    "thomas_philipps": ("Thomas Philipps", [r"thomas philipps"]),
}


def normalize(text: str) -> str:
    text = unicodedata.normalize("NFKD", text or "").encode("ascii", "ignore").decode()
    return re.sub(r"[^a-z0-9]+", " ", text.lower()).strip()


def chain_id(name: str) -> str | None:
    n = normalize(name)
    if not n:
        return None
    for cid, (_, patterns) in CHAINS.items():
        if any(re.search(p, n) for p in patterns):
            return "netto" if cid == "netto_city" else cid
    return None


def display_name(cid: str, fallback: str = "") -> str:
    return CHAINS.get(cid, (fallback or cid, []))[0]
