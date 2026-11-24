#!/usr/bin/env bash
# Installiert den Monster-Finder-Server auf dem Raspberry Pi als systemd-Dienst (Port 8000).
set -euo pipefail

DIR="$(cd "$(dirname "$0")" && pwd)"
USER_NAME="$(whoami)"

sudo apt-get update
sudo apt-get install -y python3 python3-venv

python3 -m venv "$DIR/.venv"
"$DIR/.venv/bin/pip" install --upgrade pip
"$DIR/.venv/bin/pip" install -r "$DIR/requirements.txt"

[ -f "$DIR/.env" ] || cp "$DIR/.env.example" "$DIR/.env"

sed -e "s|__DIR__|$DIR|g" -e "s|__USER__|$USER_NAME|g" "$DIR/monster-finder.service" \
  | sudo tee /etc/systemd/system/monster-finder.service > /dev/null

sudo systemctl daemon-reload
sudo systemctl enable --now monster-finder

IP="$(hostname -I | awk '{print $1}')"
echo
echo "Fertig! Server läuft auf http://$IP:8000"
echo "Test:  curl \"http://$IP:8000/api/search?plz=10115\""
echo "Diese Adresse trägst du in der App unter Einstellungen ein."
