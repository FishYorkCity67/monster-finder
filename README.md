# Monster Finder 🟢

PLZ eingeben → die App zeigt, wo in deiner Nähe Monster Energy gerade am günstigsten ist.

```
Android-App  ──WLAN──▶  Raspberry Pi (FastAPI, Port 8000)
                           ├─ zippopotam.us / Nominatim → PLZ → Koordinaten
                           ├─ marktguru                → aktuelle Prospektangebote
                           └─ OpenStreetMap Overpass   → nächste Filiale je Kette
```

Der Pi rechnet alles auf einen **Preis pro 0,5-l-Dose** um (auch Multipacks), prüft, ob die Kette im
gewählten Umkreis eine Filiale hat, und sortiert. Märkte ohne aktuelles Angebot erscheinen mit einem
grauen „Richtpreis“ (Schätzwert, anpassbar in `server/app/config.py` oder per `INCLUDE_REFERENCE_PRICES=0` abschaltbar).
Die Ergebnisse werden 30 Minuten gecacht.

## 1. Server auf dem Raspberry Pi

```bash
scp -r server pi@raspberrypi.local:~/monster-finder
ssh pi@raspberrypi.local
cd ~/monster-finder && chmod +x install.sh && ./install.sh
```

Das Skript legt eine venv an, installiert einen systemd-Dienst (startet automatisch beim Booten) und
zeigt dir am Ende die IP-Adresse. Test:

```bash
curl "http://<PI-IP>:8000/api/search?plz=10115&radius=5"
```

Logs: `journalctl -u monster-finder -f` · Einstellungen: `~/monster-finder/.env`

## 2. Android-App

1. Ordner `android/` in **Android Studio** öffnen (Gradle-Wrapper wird automatisch ergänzt).
2. Handy per USB anschließen → ▶ Run. Oder *Build → Build APK(s)* und die APK installieren.
3. In der App oben rechts ⚙️ → Adresse des Pi eintragen (z. B. `http://192.168.178.42:8000`) → „Verbindung testen“.

Handy und Pi müssen im selben WLAN sein.

## Hinweise

- **marktguru** hat keine offizielle öffentliche API. Der Server nutzt dieselbe Schnittstelle wie die
  marktguru-Website und liest deren öffentliche Web-Keys automatisch aus. Das ist für private Nutzung
  gedacht und kann jederzeit kaputtgehen. Eigene Keys kannst du in `.env` setzen.
- Prospektpreise sind ohne Pfand (0,25 €). Die App zeigt beides.
- Tests: `cd server && python -m pytest`

## Lizenz

MIT – siehe [LICENSE](LICENSE). Inoffizielles Projekt, nicht mit Monster Energy oder marktguru verbunden.
