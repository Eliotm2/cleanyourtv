# Fork « thomas » du launcher Clean Your TV

Branche `thomas` de [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv) (launcher 3.0, licence MIT),
pour la TCL 65Q6C de Thomas. Seul le dossier `launcher/` change ; le reste du dépôt est tel quel.

## Ce que le fork ajoute (tout dans `launcher/`)

| Fonction | Fichiers | Clés (`config.xml`, ou `config.json` d'un thème) |
|---|---|---|
| Météo sous les pastilles : température, conditions, max/min du jour, limite pluie-neige, lever/coucher. Open-Meteo, modèles MétéoSuisse ICON-CH d'abord. Rafraîchie toutes les 30 min quand l'accueil est à l'écran, relancée quand le réseau revient, dernière lecture gardée hors ligne. | `Weather.java`, `MainActivity.java`, `activity_main.xml`, `strings.xml` (+ `values-fr`) | `show_weather` (true), `weather_city` (Saint-Gervais-les-Bains), `weather_lat` / `weather_lon` (vides : la ville est géocodée une fois), `weather_refresh_minutes` (30) |
| Mode nuit : après N minutes sans touche sur l'accueil, dans la plage horaire, écran noir, heure tamisée, météo du lendemain ; n'importe quelle touche réveille et est absorbée. | `ClockActivity.java`, `ClockScene.java`, `dream_clock.xml` | `night_mode` (true), `night_idle_minutes` (10), `night_from` (21), `night_to` (7) |
| CPU et RAM sous l'horloge, toutes les 5 s (10 s en amont), sans la ligne stockage. | `MainActivity.java` | `show_stats` (true par défaut ici, false en amont) |

Autres écarts avec l'amont : permission `INTERNET` (interdite en amont, d'où le fork), variantes de mise en page
`layout-iw` et `layout-zh` retirées, signature par `keystore.properties` (jamais commité), `versionCode` 10
(amont : 9) et `versionName` `3.0-thomas.1`, dépendance de test `org.json` pour `WeatherTest`.

## Compiler

JDK 17 ou 21, SDK Android 34. Dans `launcher/` : `gradle --no-daemon testReleaseUnitTest lintRelease assembleRelease`
(243 tests JVM, lint sans erreur). L'APK sort dans `launcher/app/build/outputs/apk/release/`. Il est embarqué tel quel
dans l'appli téléphone forkée (`cleanyourtv-app`, `android/platform/src/main/assets/bundled/cleanyourtv-launcher.apk`),
dont les épingles (`core/trust/.../Pins.kt`) doivent suivre : taille, SHA-256, commit.

## Non vérifié faute de télé

Le rendu réel du bloc météo et de la ligne stats sur la TCL, la lisibilité de l'écran nuit, la disponibilité de la
limite pluie-neige dans le modèle MétéoSuisse, et la lecture de `/proc/stat` (le CPU disparaît de la ligne si
la télé le cache).
