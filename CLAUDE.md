# MapOverlay

Nakładka ekranowa z dużymi przyciskami gestów do sterowania mapą w trasie (np. rower z uchwytem na telefon — ciężko trafić w małe przyciski). Wykonuje gesty dotykowe (swipe, pinch zoom) przez AccessibilityService.

## Architektura

```
MainActivity
└── SetupScreen (Compose) — konfiguracja uprawnień, start/stop nakładki, slidery gestów

OverlayService (ForegroundService)
└── osobne ComposeView w WindowManager
    ├── FAB toggle (tap = zwijanie/rozwijanie, long press = otwórz Setup UI)
    └── EdgeButtons (6 osobnych views na krawędziach ekranu, powiększone)

MapGestureAccessibilityService
├── performSwipe(direction) — jeden ślad, konfigurowalny czas
└── performZoom(zoomIn) — dwa równoległe ślady (pinch)
```

## Koncepcja UI nakładki

Nakładka ma dwa tryby:
- **Zwinięty:** mały FAB w lewym górnym rogu
- **Rozwinięty:** przyciski rozłożone po krawędziach ekranu (góra/dół/lewo/prawo), zoom ＋ pod lewą krawędzią, zoom － pod prawą. Środek ekranu wolny — widać aplikację pod spodem.

FAB: tap = toggle, long press = otwiera MainActivity (Setup UI).

Przyciski krawędziowe półprzezroczyste (~30% opacity), reagują na press (nie tap) — odporne na przesunięcie palca podczas ruchu. Góra/dół są 2× szersze, boki i zoom 2× wyższe dla łatwiejszego celowania w trasie.

## Komunikacja między komponentami

`MapGestureAccessibilityService.instance` — singleton companion. Overlay czyta go w każdym `onPress`, nie cache'uje referencji.

`OverlayService.isRunning` — companion Boolean. MainActivity czyta go w `ON_RESUME` do blokowania przycisków start/stop.

Świadomy kompromis: singleton zamiast Messenger/AIDL. Przy jednym producencie i jednym konsumencie Messenger dodaje złożoność bez korzyści.

## Strojenie gestów

Parametry gestów w `GestureConfig` (`MapGestureAccessibilityService.kt`). Trzy z nich są konfigurowalne przez użytkownika z Setup UI (slidery), reszta hardcoded:

- **`swipeDistanceRatio`** — dystans swipe jako % szerokości ekranu (slider w UI)
- **`zoomInPercent`** — rozstaw palców zoom-in jako % szer. (slider w UI)
- **`zoomOutPercent`** — rozstaw palców zoom-out jako % szer. (slider w UI, min 7%)
- `SWIPE_DURATION_MS` — czas trwania swipe (hardcoded)
- `ZOOM_IN_DURATION_MS` / `ZOOM_OUT_DURATION_MS` — czas trwania pinch (hardcoded)

Wartości użytkownika persystowane w SharedPreferences (`map_overlay_prefs`), ładowane w `MainActivity.onCreate`, zmiana z slidera działa natychmiast (config czytany przy każdym geście).

## WindowManager — osobne views zamiast fullscreen

Kluczowa decyzja: overlay NIE używa jednego fullscreen `ComposeView` — to blokowałoby dotyki w całej aplikacji pod spodem. Zamiast tego każdy przycisk/grupa to osobny mały `ComposeView` dodawany do WindowManager z `WRAP_CONTENT` i dokładnym rozmiarem.

- **FAB** — jeden mały view 84dp w lewym górnym rogu, zawsze widoczny
- **Edge buttons** — 6 osobnych views (4 krawędzie + 2 zoom), dodawane/usuwane dynamicznie przy toggle
- Gdy zwinięty: tylko FAB jest w WindowManager, reszta ekranu w 100% interaktywna
- Gdy rozwinięty: przyciski krawędziowe przechwytują dotyk tylko w swoich obszarach

Flagi WindowManager:
- `FLAG_NOT_FOCUSABLE` — dotyki poza view trafiają do aplikacji pod spodem
- `FLAG_NOT_TOUCH_MODAL` — pozwala na interakcję z aplikacją w wolnych obszarach
- `FLAG_SHOW_WHEN_LOCKED` — overlay widoczny i interaktywny na ekranie blokady

`OverlayLifecycleOwner` implementuje `LifecycleOwner + ViewModelStoreOwner + SavedStateRegistryOwner` — współdzielony między wszystkimi ComposeView, niezbędny bo Compose jest hostowany poza Activity.

## Budowanie

```bash
./gradlew assembleDebug
```

AGP 9.0.1, Gradle 9.4.0, wbudowany Kotlin (bez osobnego pluginu `kotlin-android`).

## Znane ograniczenia

- AccessibilityService jest `null` do momentu ręcznego włączenia w ustawieniach systemowych
- Gesty mogą nie działać na niektórych ekranach systemowych (panel powiadomień)
- `FLAG_NOT_FOCUSABLE` sprawia, że nakładka nie przechwytuje dotyku w obszarach bez przycisków, ale też nie może obsługiwać focus/klawiatury
