#!/bin/bash
# ==============================================================================
# Skrypt instalacyjny dla maszyny GCP Spot (Ubuntu 22.04 / 24.04 LTS) w Niemczech
# Konfiguruje FRP Server (frps) + Nginx + Stronę pearium.com + Tunel Minecraft
# ==============================================================================

set -e

echo "=== [1/5] Aktualizacja systemu i instalacja pakietów ==="
sudo apt-get update -y
sudo apt-get install -y nginx curl wget tar certbot python3-certbot-nginx

echo "=== [2/5] Pobieranie i instalacja FRP Server (frps) ==="
FRP_VERSION="0.58.1"
ARCH="amd64"
wget -q "https://github.com/fatedier/frp/releases/download/v${FRP_VERSION}/frp_${FRP_VERSION}_linux_${ARCH}.tar.gz" -O /tmp/frp.tar.gz
tar -xzf /tmp/frp.tar.gz -C /tmp/
sudo cp "/tmp/frp_${FRP_VERSION}_linux_${ARCH}/frps" /usr/local/bin/frps
sudo chmod +x /usr/local/bin/frps
sudo mkdir -p /etc/frp

# Zapis konfiguracji frps.toml
sudo tee /etc/frp/frps.toml > /dev/null << 'EOF'
bindPort = 7000
auth.token = "pearium-mc-secret-2026"

webServer.addr = "0.0.0.0"
webServer.port = 7500
webServer.user = "admin"
webServer.password = "pearium2026admin"

allowPorts = [
  { start = 25565, end = 25565 }
]
EOF

echo "=== [3/5] Konfiguracja usługi systemd dla frps ==="
sudo tee /etc/systemd/system/frps.service > /dev/null << 'EOF'
[Unit]
Description=FRP Server Daemon (Minecraft Relay)
After=network.target

[Service]
Type=simple
User=root
Restart=on-failure
RestartSec=5s
ExecStart=/usr/local/bin/frps -c /etc/frp/frps.toml

[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
sudo systemctl enable --now frps

echo "=== [4/5] Przygotowanie strony WWW pearium.com ==="
sudo mkdir -p /var/www/pearium
sudo cp -r web/* /var/www/pearium/ 2>/dev/null || true

# Podstawowa strona jeśli folder web/ nie został jeszcze skopiowany
if [ ! -f /var/www/pearium/index.html ]; then
sudo tee /var/www/pearium/index.html > /dev/null << 'HTMLEOF'
<!DOCTYPE html>
<html lang="pl">
<head>
    <meta charset="UTF-8">
    <title>pearium.com - Minecraft Mobile Server</title>
</head>
<body style="background:#0d1117;color:#f0f6fc;font-family:sans-serif;text-align:center;padding:50px;">
    <h1>🎮 pearium.com - Serwer Minecraft Folia</h1>
    <p>Serwer działa na telefonie Xiaomi 11T Pro z procesorem Snapdragon 888!</p>
    <p>Adres serwera w grze: <strong>pearium.com:25565</strong></p>
</body>
</html>
HTMLEOF
fi

echo "=== [5/5] Konfiguracja Nginx ==="
sudo cp nginx.conf /etc/nginx/nginx.conf 2>/dev/null || true
sudo systemctl restart nginx

echo "=========================================================="
echo "Instalacja zakończona pomyślnie!"
echo "Status FRPS: $(systemctl is-active frps)"
echo "Status Nginx: $(systemctl is-active nginx)"
echo ""
echo "Aby wygenerować darmowy certyfikat SSL (HTTPS) dla pearium.com, uruchom:"
echo "sudo certbot --nginx -d pearium.com -d www.pearium.com"
echo "=========================================================="
