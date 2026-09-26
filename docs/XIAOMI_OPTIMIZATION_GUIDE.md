# Poradnik optymalizacji dla Xiaomi 11T Pro (MIUI / HyperOS)

Xiaomi 11T Pro to potężne urządzenie z procesorem **Snapdragon 888 (8 rdzeni)** i szybką pamięcią LPDDR5, co czyni go świetną platformą dla wielowątkowego silnika **Minecraft Folia**. Jednak systemy MIUI oraz HyperOS posiadają agresywne mechanizmy oszczędzania energii, które mogą uśpić serwer po wygaszeniu ekranu.

Wykonaj poniższe kroki na telefonie, aby serwer działał stabilnie 24/7 w tle.

---

## 1. Wyłączenie oszczędzania baterii dla aplikacji MinecraftMobile
1. Otwórz **Ustawienia** w telefonie.
2. Przejdź do: **Aplikacje** ➔ **Zarządzaj aplikacjami** ➔ wyszukaj **MinecraftMobile**.
3. Wejdź w sekcję **Oszczędzanie energii** (Battery Saver).
4. Zmień domyślne ustawienie na **Bez ograniczeń** (No restrictions).
   *(Dzięki temu system nie zamknie procesu Javy ani tunelu przy wygaszonym ekranie).*

## 2. Zezwolenie na Autostart
1. W tej samej karcie aplikacji **MinecraftMobile** włącz opcję **Autostart**.
2. W oknie potwierdzenia zezwól na działanie.

## 3. Zablokowanie aplikacji kłódką w pamięci RAM
1. Otwórz aplikację **MinecraftMobile**.
2. Wejdź do widoku ostatnich aplikacji (Recent Apps / gest przesunięcia od dołu i przytrzymania).
3. Przytrzymaj palec na karcie MinecraftMobile i kliknij ikonę **kłódki** (zablokuj w pamięci).
   *(Zapobiegnie to przypadkowemu usunięciu serwera podczas czyszczenia pamięci podręcznej).*

## 4. Zarządzanie temperaturą Snapdragona 888
* Snapdragon 888 cechuje się wysoką wydajnością, lecz przy ciągłym obciążeniu generuje ciepło.
* **Rekomendacja**: Trzymaj telefon podłączony do ładowarki (najlepiej bez grubego etui lub na podstawce umożliwiającej cyrkulację powietrza), jeśli planujesz dłuższą sesję dla wielu graczy.
* **Pamięć RAM**: Ustaw suwak na **4 GB** lub **6 GB**. 4 GB to złoty środek dla Folii z kilkoma graczami na telefonie z 8/12 GB RAM-u.
