# Fork « thomas » du launcher Clean Your TV

Branche `thomas` de [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv) (launcher 3.0 amont `b06ab21`,
licence MIT), pour la TCL 65Q6C de Thomas. Seul le dossier `launcher/` change ; le reste du dépôt est tel quel.
Version `3.0-thomas.N`, `versionCode` toujours au-dessus de l'amont.

## Ce que le fork ajoute (tout dans `launcher/`)

| Fonction | Fichiers | Clés (`config.xml`, ou `config.json` d'un thème) |
|---|---|---|
| Météo à droite : ville, température, conditions, « Min · Max · Neige dès ». Open-Meteo, modèles MétéoSuisse ICON-CH d'abord. Rafraîchie toutes les 30 min quand l'accueil est à l'écran, relancée quand le réseau revient, dernière lecture gardée hors ligne. Ville choisissable dans Launcher settings. | `Weather.java`, `MainActivity.java`, `activity_main.xml`, `LauncherSettingsActivity.java`, `strings.xml` (+ `values-fr`) | `show_weather` (true), `weather_city` (Saint-Gervais-les-Bains), `weather_lat` / `weather_lon` (vides : géocodage une fois), `weather_refresh_minutes` (30) |
| Mode nuit : après N minutes sans touche sur l'accueil, dans la plage horaire, écran noir, heure tamisée, météo du lendemain. | `ClockActivity.java`, `ClockScene.java`, `dream_clock.xml` | `night_mode` (true), `night_idle_minutes` (10), `night_from` (21), `night_to` (7) |
| Pastilles RAM % et Disque % avec anneau, toutes les 5 s. | `MainActivity.java`, `RingDrawable.java`, `styles.xml` | `show_stats` (true ici, false en amont) |
| Boutons de la télécommande appris sur la télé (Launcher settings › boutons), par keycode ou scancode. | `HomeKeyService.java`, `LauncherSettingsActivity.java`, `Config.java` | préférence `app_keys_learnt`, fusionnée dans `app_keys` |
| Dock collé en bas, la rangée suivante entièrement hors écran (l'amont laisse dépasser 30 dp) ; les applis restent en grille sous le dock, atteintes par Bas. | `MainActivity.java`, `Config.java` | `dock_bottom_room` (0 ; amont : 30 fixe) |
| 5 tuiles par rangée par défaut. | `config.xml` | `columns` (5) |

Autres écarts avec l'amont : permission `INTERNET` (interdite en amont, d'où le fork), variantes de mise en page
`layout-iw` et `layout-zh` retirées, signature par `keystore.properties` (jamais commité), dépendances de test
`org.json` et Robolectric (`WeatherTest`, `StartupTest`, `RenderTest`).

## Comparé à l'amont et gardé

| Notre ajout | Option amont proche | Pourquoi on garde le nôtre |
|---|---|---|
| `dock_bottom_room` | `single_row` (« Every app in one row », appli 0.4.5) | Essayé le 27/10/2026 (3.0-thomas.6) : `single_row` met toutes les applis sur la ligne du dock, à droite, au lieu de les garder en dessous, et grandit le dock (29 dp de noms sous les tuiles, bandeau plus large). Thomas veut la grille sous le dock : retour au nôtre en 3.0-thomas.7. |
| Pastilles RAM / Disque | ligne texte `show_stats` (CPU, mémoire, stockage) | Rendu différent (pastilles à anneau). |

## À chaque reprise de l'amont : vérifier les recouvrements

Avant de reprendre nos ajouts sur une nouvelle version amont, et à chaque fois :

1. Lire le CHANGELOG de l'appli amont et le diff de `launcher/app/src/main/res/values/config.xml` (nouvelles clés).
2. Pour chaque ligne du tableau « Ce que le fork ajoute » (ici et dans `FORK-THOMAS.md` de l'appli), chercher si
   l'amont fait maintenant la même chose (clé, option du Look, écran de l'appli).
3. Si c'est le même résultat : proposer à Thomas de retirer notre version au profit de la sienne, et lui dire quel
   réglage activer. Ne retirer qu'après son essai sur la télé.
4. Si c'est seulement proche : garder la nôtre, l'inscrire dans « Comparé à l'amont et gardé », et le signaler.

## Compiler

JDK 17 ou 21, SDK Android 34. Dans `launcher/` : `gradle --no-daemon testReleaseUnitTest lintRelease assembleRelease`
(lint sans erreur, APK sous 589 824 o). L'APK sort dans `launcher/app/build/outputs/apk/release/`. Il est embarqué tel
quel dans l'appli téléphone forkée (`Eliotm2/cleanyourtv-app`,
`android/platform/src/main/assets/bundled/cleanyourtv-launcher.apk`), dont les épingles (`core/trust/.../Pins.kt`,
`LauncherPinTest`) doivent suivre : taille, SHA-256, commit, versionCode, versionName.

## Non vérifié faute de télé

La disponibilité de la limite pluie-neige selon la saison dans le modèle MétéoSuisse, et la lecture de `/proc/stat`.
