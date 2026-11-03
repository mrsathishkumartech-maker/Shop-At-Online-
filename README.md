# App-AndroidGames

Several Android games in one repository. Each game is its own app with its own package
name, its own APK and its own release, and all of them share common code from `core/`.

| Game | Folder | Package | Status |
| --- | --- | --- | --- |
| Arrow Escape | `games/arrowescape` | `com.sathishkumar.games.arrowescape` | Playable, levels 1–∞ |
| Kirana Sort | `games/kiranasort` | `com.sathishkumar.games.kiranasort` | Playable, levels 1–∞ |
| Chai Stall Tycoon | `games/chaistall` | `com.sathishkumar.games.chaistall` | Playable idle game |

## Layout

```
core/                     shared library: saved progress, haptics, AdMob wrapper, ad timing rules
games/<game-id>/          one Android app per game (auto-discovered by settings.gradle.kts)
templates/game/           starting point for a new game
scripts/new-game.sh       creates games/<game-id> from the template
.github/workflows/
  build.yml               every push/PR: unit tests + debug APK for every game
  release.yml             tag <game-id>-v1.2.3: signed APK + AAB for that one game
  screenshots.yml         runs every game in an Android emulator and saves screenshots
                          (to the `screenshots` branch); games without levels put prepared
                          saves in games/<game-id>/screenshot-states/
```

## Build locally

You need JDK 17 and the Android SDK (Android Studio installs both). Open the folder in
Android Studio, or:

```bash
./gradlew :games:arrowescape:assembleDebug
```

The APK ends up in `games/arrowescape/build/outputs/apk/debug/`. To run the tests:

```bash
./gradlew testDebugUnitTest
```

## Add a new game

```bash
scripts/new-game.sh snakeladder "Snake & Ladder"
```

That creates `games/snakeladder` with its own package (`com.sathishkumar.games.snakeladder`),
a placeholder screen and test ad ids. Gradle picks it up automatically.

## Release a game

Each game is released on its own, by tag:

```bash
git tag arrowescape-v1.0.0
git push origin arrowescape-v1.0.0
```

The **Release game** workflow then tests and builds only that game, and creates a GitHub
Release named `arrowescape 1.0.0` with:

- `arrowescape-1.0.0.apk`, which installs directly on a phone
- `arrowescape-1.0.0.aab`, which you upload to Google Play

The version code is worked out from the version, `major*10000 + minor*100 + patch`
(`1.4.2` becomes `10402`), so minor and patch must stay between 0 and 99. You can also
run the workflow by hand from the **Actions** tab by giving a game id and a version.

### Signing (needed before Google Play)

Without a keystore, release APKs are signed with a throwaway debug key. They install for
testing, but Google Play won't accept them, and later versions can't be installed over them.
Create one keystore once and use it for every game:

```bash
keytool -genkeypair -v -keystore release.jks -alias games -keyalg RSA -keysize 2048 -validity 10000
base64 -w0 release.jks > release.jks.b64
```

Then add these four **repository secrets** (Settings → Secrets and variables → Actions):

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | contents of `release.jks.b64` |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | `games` (or the alias you chose) |
| `ANDROID_KEY_PASSWORD` | key password |

Keep `release.jks` and its passwords backed up somewhere safe and never commit them. If you
lose the key, you can't update your apps on Play (unless you use Play App Signing).

## Ads (AdMob)

`core/` contains the shared ad code. Every game currently uses **Google's public test ad ids**,
which show "Test Ad" banners and never pay. Before publishing, create an app and two ad units
(rewarded and interstitial) in AdMob for each game, then add them to `gradle.properties`:

```properties
arrowescape.admob.appId=ca-app-pub-XXXXXXXX~YYYYYYYY
arrowescape.admob.rewarded=ca-app-pub-XXXXXXXX/ZZZZZZZZ
arrowescape.admob.interstitial=ca-app-pub-XXXXXXXX/WWWWWWWW
```

Arrow Escape's ad moments:

- **Rewarded:** Hint, Undo (also offered when you run out of lives) and Skip level
- **Interstitial:** after every 3rd level once the player is past level 10 (13, 16, 19, …),
  set by `AdPolicy` in `core/`

## Arrow Escape

Tap an arrow and it slides out the way it points, but only if nothing is in its path.
Clear the board.

- **Lives:** 3 per level. Tapping a blocked arrow costs one, and stars depend on how many
  you lose.
- **Coloured gates:** from level 4, coloured gates appear on the border. A coloured arrow can
  only leave through a gate of its own colour, and plain arrows can't use gates.
- **Rangoli:** each cleared cell reveals part of a rangoli. Clearing the whole board shows
  all of it and adds it to the **Rangoli book**. Skipped levels don't add one.
- **Levels** are generated from the level number, so level 12 is the same for everyone. The
  generator places arrows one at a time, each with a clear path to an exit it's allowed to
  use at that moment. Removing them in reverse order always works, so every level can be
  solved. Unit tests check levels 1–100.

## Kirana Sort

Shelves full of mixed shop items: chai, mangoes, laddoos, bangles, samosas, jalebi and more.
Drag an item onto another shelf, and three of the same on one shelf clear. When a shelf's
front row is empty, the row hidden behind it comes forward. Clear every shelf before the
timer runs out.

- **Coins and shop:** each level pays coins (more for time left). Spend them to grow from a
  **Street stall** to a **Kirana shop**, **General store**, **Mini mart** and finally a
  **Supermarket**. Each upgrade raises the coins you earn per level.
- **Rewarded ads:** **+Shelf** (an extra empty shelf), **Shuffle** and **+30s**. They're also
  offered when the shelves are full or time runs out.
- **Levels** are built backwards from an empty shop: un-clear a triple, then un-move items
  around. Playing those steps forwards clears the board, so every level can be solved.
  Unit tests replay the solution for levels 1–150.

## Chai Stall Tycoon

An idle business game. Start with one roadside chai stall, add samosa, vada and bun maska,
hire helpers, then open branches in **Chennai**, **Mumbai** and **Delhi**. Money keeps coming
in on its own, and tapping the stall serves a customer for a quick bonus.

- **Upgrades:** each one costs 15% more than the last, and a product's sales double every 25 levels.
- **Helpers** (up to 3 per location): +50% sales each. Locations with a helper keep earning
  while the app is closed, at half speed for up to 8 hours.
- **Branches:** each city sells at 5×, 25× and 125× roadside prices. Costs grow faster than
  income, so each city is a real goal. A simulated player who never taps reaches Chennai in
  about 20 minutes of earning, Mumbai in about 3 hours and Delhi in about 4 days. A unit test
  keeps that pacing.
- **Money** is shown the Indian way: ₹99,999 → ₹1.5 L → ₹3.4 Cr.
- **Rewarded ads:** **2× profits for 4 hours** (stacks up to 8 hours), **double the money earned
  while away**, and **Free ▶** instant upgrades (once every 5 minutes).
