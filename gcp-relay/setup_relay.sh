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
sudo tee /var/www/pearium/index.html > /dev/null << 'HTMLEOF'
<!DOCTYPE html>
<html lang="pl">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>pearium.com | Serwer Minecraft Folia</title>
    <meta name="description" content="Oficjalny serwer Minecraft Folia działający na telefonie Xiaomi 11T Pro. Dołącz przez pearium.com:25565!">
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;600;800&family=JetBrains+Mono:wght@400;700&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg-color: #080C10;
            --card-bg: rgba(22, 27, 34, 0.75);
            --border-color: rgba(48, 54, 61, 0.8);
            --primary-emerald: #2ECC71;
            --accent-mint: #00E676;
            --text-primary: #F0F6FC;
            --text-secondary: #8B949E;
            --glow: 0 0 25px rgba(46, 204, 113, 0.25);
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: 'Outfit', sans-serif;
            background-color: var(--bg-color);
            color: var(--text-primary);
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 20px;
            background-image: 
                radial-gradient(circle at 50% 10%, rgba(46, 204, 113, 0.12) 0%, transparent 60%),
                radial-gradient(circle at 80% 90%, rgba(0, 230, 118, 0.08) 0%, transparent 50%);
            background-attachment: fixed;
        }
        .container {
            max-width: 720px;
            width: 100%;
            background: var(--card-bg);
            border: 1px solid var(--border-color);
            backdrop-filter: blur(16px);
            border-radius: 24px;
            padding: 40px;
            box-shadow: 0 20px 40px rgba(0,0,0,0.6);
            text-align: center;
        }
        .badge-status {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            background: rgba(46, 204, 113, 0.15);
            border: 1px solid rgba(46, 204, 113, 0.4);
            color: var(--primary-emerald);
            padding: 6px 16px;
            border-radius: 50px;
            font-size: 13px;
            font-weight: 600;
            margin-bottom: 24px;
        }
        .pulse-dot {
            width: 8px;
            height: 8px;
            background: var(--primary-emerald);
            border-radius: 50%;
            box-shadow: 0 0 10px var(--primary-emerald);
            animation: pulse 2s infinite;
        }
        @keyframes pulse {
            0% { transform: scale(0.95); opacity: 0.8; }
            50% { transform: scale(1.4); opacity: 1; }
            100% { transform: scale(0.95); opacity: 0.8; }
        }
        h1 {
            font-size: 38px;
            font-weight: 800;
            letter-spacing: -0.5px;
            margin-bottom: 12px;
            background: linear-gradient(135deg, #FFFFFF 40%, var(--primary-emerald) 100%);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }
        p.subtitle {
            color: var(--text-secondary);
            font-size: 16px;
            margin-bottom: 30px;
            line-height: 1.6;
        }
        .server-ip-box {
            display: flex;
            align-items: center;
            justify-content: space-between;
            background: rgba(13, 17, 23, 0.9);
            border: 1px solid var(--border-color);
            padding: 14px 20px;
            border-radius: 14px;
            margin-bottom: 32px;
            cursor: pointer;
            transition: all 0.2s ease;
        }
        .server-ip-box:hover {
            border-color: var(--primary-emerald);
            box-shadow: var(--glow);
        }
        .ip-text {
            font-family: 'JetBrains Mono', monospace;
            font-size: 18px;
            font-weight: 700;
            color: var(--accent-mint);
        }
        .copy-btn {
            background: var(--primary-emerald);
            color: var(--bg-color);
            border: none;
            padding: 8px 16px;
            border-radius: 8px;
            font-weight: 700;
            font-size: 13px;
            cursor: pointer;
            transition: background 0.2s;
        }
        .copy-btn:hover { background: var(--accent-mint); }
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(3, 1fr);
            gap: 16px;
            margin-bottom: 32px;
        }
        .stat-card {
            background: rgba(13, 17, 23, 0.6);
            border: 1px solid var(--border-color);
            padding: 16px;
            border-radius: 14px;
        }
        .stat-number { font-size: 22px; font-weight: 800; color: var(--text-primary); margin-bottom: 4px; }
        .stat-label { font-size: 12px; color: var(--text-secondary); text-transform: uppercase; letter-spacing: 0.5px; }
        .specs-section {
            background: rgba(13, 17, 23, 0.4);
            border: 1px dashed var(--border-color);
            border-radius: 14px;
            padding: 16px;
            text-align: left;
            font-size: 13px;
            color: var(--text-secondary);
            line-height: 1.7;
        }
        .specs-section strong { color: var(--text-primary); }
        footer { margin-top: 30px; font-size: 12px; color: var(--text-secondary); }
        @media (max-width: 600px) {
            .container { padding: 24px; }
            h1 { font-size: 28px; }
            .stats-grid { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="badge-status">
            <span class="pulse-dot"></span>
            <span id="status-text">Sprawdzanie stanu serwera...</span>
        </div>
        <h1>pearium.com</h1>
        <p class="subtitle">Wielowątkowy serwer Minecraft Folia hostowany bezpośrednio na telefonie Xiaomi 11T Pro z tunelem przekaźnikowym w chmurze Google (Niemcy).</p>
        <div class="server-ip-box" onclick="copyIp()" id="ipBox">
            <div>
                <span style="font-size: 11px; text-transform: uppercase; color: var(--text-secondary); display: block; text-align: left;">Adres serwera w grze:</span>
                <span class="ip-text">pearium.com:25565</span>
            </div>
            <button class="copy-btn" id="copyBtn">Kopiuj IP</button>
        </div>
        <div class="stats-grid">
            <div class="stat-card">
                <div class="stat-number" id="players-val">- / -</div>
                <div class="stat-label">Gracze online</div>
            </div>
            <div class="stat-card">
                <div class="stat-number" id="version-val">Folia 1.20+</div>
                <div class="stat-label">Wersja silnika</div>
            </div>
            <div class="stat-card">
                <div class="stat-number" id="ping-val">~18 ms</div>
                <div class="stat-label">Ping (GCP Niemcy)</div>
            </div>
        </div>
        <div class="specs-section">
            <p>📱 <strong>Host:</strong> Xiaomi 11T Pro (Snapdragon 888, 8 rdzeni)</p>
            <p>⚡ <strong>Silnik:</strong> PaperMC Folia (wielowątkowe tickowanie regionów)</p>
            <p>🌐 <strong>Infrastruktura:</strong> Google Cloud Platform Spot (europe-west3, Frankfurt)</p>
        </div>
        <footer>&copy; 2026 pearium.com • Projekt MinecraftMobile</footer>
    </div>
    <script>
        function copyIp() {
            navigator.clipboard.writeText("pearium.com:25565").then(() => {
                const btn = document.getElementById("copyBtn");
                btn.innerText = "Skopiowano!";
                btn.style.background = "#00E676";
                setTimeout(() => {
                    btn.innerText = "Kopiuj IP";
                    btn.style.background = "#2ECC71";
                }, 2000);
            });
        }
        async function fetchServerStatus() {
            try {
                const res = await fetch("https://api.mcsrvstat.us/3/pearium.com");
                const data = await res.json();
                if (data.online) {
                    document.getElementById("status-text").innerText = "Serwer Online";
                    document.getElementById("players-val").innerText = `${data.players.online} / ${data.players.max}`;
                    if (data.version) document.getElementById("version-val").innerText = data.version;
                } else {
                    document.getElementById("status-text").innerText = "Serwer Oczekuje (Offline)";
                    document.getElementById("players-val").innerText = "0 / 20";
                }
            } catch (e) {
                document.getElementById("status-text").innerText = "Serwer Aktywny (Folia)";
            }
        }
        fetchServerStatus();
        setInterval(fetchServerStatus, 30000);
    </script>
</body>
</html>
HTMLEOF

echo "=== [5/5] Konfiguracja Nginx ==="
sudo tee /etc/nginx/sites-available/default > /dev/null << 'NGINXEOF'
server {
    listen 80 default_server;
    listen [::]:80 default_server;

    server_name pearium.com www.pearium.com _;

    root /var/www/pearium;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
NGINXEOF

sudo systemctl restart nginx

echo "=========================================================="
echo "Instalacja zakończona pomyślnie!"
echo "Status FRPS: $(systemctl is-active frps)"
echo "Status Nginx: $(systemctl is-active nginx)"
echo "=========================================================="
