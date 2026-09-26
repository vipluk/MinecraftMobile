# MinecraftMobile 🚀
**Wielowątkowy serwer Minecraft Folia na Twoim telefonie (Xiaomi 11T Pro)**

MinecraftMobile to aplikacja na system Android pozwalająca na uruchomienie pełnego serwera Minecraft **Folia** (PaperMC) bezpośrednio na smartfonie, z dynamiczną alokacją pamięci RAM, instalatorem pluginów z bazy **Modrinth** jednym kliknięciem oraz bezpiecznym tunelem przekaźnikowym w chmurze **Google Cloud Platform (GCP)** podpiętym pod domenę **pearium.com**.

---

## 🎯 Główne Funkcjonalności

1. **Silnik Folia (Wielowątkowy Minecraft):**
   * Wykorzystuje wszystkie 8 rdzeni procesora **Qualcomm Snapdragon 888** w Xiaomi 11T Pro dzięki regionalnemu tickowaniu świata.
   * Dedykowane flagi JVM Aikar's Flags zoptymalizowane pod architekturę ARM64.

2. **Zarządzanie Pamięcią RAM:**
   * Płynny suwak w zakresie **1 GB – 8 GB** oraz sprzężone pole numeryczne (zmiana wartości w polu natychmiast przesuwa suwak i odwrotnie).

3. **Konsola na Żywo i Komendy:**
   * Podgląd logów serwera w czasie rzeczywistym z kolorowaniem ANSI.
   * Pasek szybkich komend (`/op`, `/whitelist`, `/gamemode`, `/tps`, `/stop`) oraz pole wysyłania własnych poleceń.

4. **1-Klikowa Integracja z Modrinth:**
   * Przeglądanie i wyszukiwanie tysięcy pluginów z oficjalnego API Modrinth v2.
   * Automatyczne pobieranie pliku `.jar` do folderu `plugins/`.
   * Włączanie / wyłączanie wtyczek bez ich kasowania oraz bezpieczne usuwanie.

5. **Menedżer Plików i Konfiguracji:**
   * Graficzny konfigurator dla `server.properties` (MOTD, limit graczy, tryb Premium / Non-Premium, PvP, trudność).
   * Wbudowany edytor plików RAW (`server.properties`, `bukkit.yml`, `eula.txt`).

6. **Rozwiązanie Braku Port Forwardingu (pearium.com + Maszyna GCP Spot):**
   * Tani serwer przekaźnikowy w Niemczech (Frankfurt, opóźnienia ~18 ms).
   * **W przeglądarce:** Otwarcie `pearium.com` wyświetla nowoczesną stronę wizytówkową ze statusem serwera na żywo.
   * **W grze Minecraft:** Wpisanie `pearium.com` (port 25565) automatycznie łączy gracza z telefonem przez tunel TCP.

---

## 📁 Struktura Projektu

```text
MinecraftMobile/
├── app/                                # Moduł aplikacji Android (Kotlin + Jetpack Compose)
│   ├── src/main/java/com/pearium/minecraftmobile/
│   │   ├── MainActivity.kt             # Główny interfejs i dolna nawigacja
│   │   ├── core/                       # Silnik, zarządca procesu, RAM, FoliaDownloader, ConfigManager
│   │   ├── modrinth/                   # Klient Modrinth API, instalator i menedżer wtyczek
│   │   ├── tunnel/                     # Menedżer tunelu sieciowego FRP Client
│   │   ├── service/                    # MinecraftServerService (Foreground Service, WakeLock, WifiLock)
│   │   └── ui/                         # Ekrany (Dashboard, Konsola, Pluginy, Pliki, Tunel) oraz Theme
├── gcp-relay/                          # Konfiguracja przekaźnika w chmurze GCP
│   ├── deploy_gcp_spot.ps1             # 1-klikowy skrypt PowerShell tworzący maszynę Spot we Frankfurcie
│   ├── setup_relay.sh                  # Skrypt konfiguracyjny (FRP Server, Nginx, Certbot SSL)
│   ├── frps.toml                       # Plik konfiguracyjny serwera FRP
│   ├── nginx.conf                      # Konfiguracja Nginx (port 80/443 WWW + port 25565 Minecraft TCP)
│   └── web/index.html                  # Nowoczesna strona wizytówki serwera pearium.com
└── docs/
    └── XIAOMI_OPTIMIZATION_GUIDE.md    # Instrukcja konfiguracji baterii i autostartu na Xiaomi
```

---

## 🚀 Jak Zbudować Plik APK?

W katalogu głównym projektu uruchom:
```powershell
.\gradlew.bat assembleDebug
```
Wygenerowany plik APK znajdziesz w:
`app\build\outputs\apk\debug\app-debug.apk`

---

## ☁️ Wdrożenie Maszyny GCP Spot (Niemcy)

1. Uruchom skrypt wdrożeniowy PowerShell:
   ```powershell
   cd gcp-relay
   .\deploy_gcp_spot.ps1
   ```
2. Skrypt automatycznie:
   * Założy reguły Firewall w GCP (porty 80, 443, 7000, 25565).
   * Utworzy maszynę `e2-small` typu **SPOT** w regionie `europe-west3-a` (Frankfurt).
   * Zwróci publiczny adres IP.
3. Skieruj domenę `pearium.com` (rekord A) na otrzymany adres IP.
