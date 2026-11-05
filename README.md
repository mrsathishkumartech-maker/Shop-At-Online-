# App-AndroidGames

Several Android games in one repository. Each game is its own app with its own package
name, its own APK and its own release, and all of them share common code from `core/`.

| Game | Folder | Package | Status |
| --- | --- | --- | --- |
| Arrow Escape | `games/arrowescape` | `com.sathishkumar.games.arrowescape` | Playable, levels 1–∞ |
| Kirana Sort | `games/kiranasort` | `com.sathishkumar.games.kiranasort` | Playable, levels 1–∞ |
| Chai Stall Tycoon | `games/chaistall` | `com.sathishkumar.games.chaistall` | Playable idle game |
| Super Over Cricket | `games/superover` | `com.sathishkumar.games.superover` | Playable, 4 tournaments |
| Kingdom Guard | `games/kingdomguard` | `com.sathishkumar.games.kingdomguard` | Playable, endless waves |
| Brain Games | `games/braingames` | `com.sathishkumar.games.braingames` | Playable, 4 mini-games |
| Puzzle Games | `games/puzzlegames` | `com.sathishkumar.games.puzzlegames` | Playable, 4 puzzles, endless levels |
| Word Games | `games/wordgames` | `com.sathishkumar.games.wordgames` | Playable, 4 word games |

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
                          saves in games/<game-id>/screenshot-states/, and games with
                          several screens script their own steps in screenshot-steps.sh
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

## Super Over Cricket

One-tap batting. Each ball comes in at its own speed and type: straight, outswinger, slower
ball, yorker or bouncer. Tap when it reaches the bat. Your timing decides the shot: perfect
for a six, good for a four (sometimes a six, with a strong bat), OK for 1–2 runs, an edge
(which might be caught), or a miss. Miss a ball on the stumps and you're bowled. A timing bar
shows how early or late each swing was.

- **Matches:** chase a target in one over with 2 wickets.
- **Tournaments:** Gully Cup → City League → National Trophy → World Super Over Cup. Bowlers get
  faster and add yorkers and bouncers as you go. Winning the World Cup starts it again with
  higher targets.
- **Bats:** runs earn coins (+20 for a win, plus a prize for each tournament). Better bats widen
  the timing window and turn more fours into sixes.
- **Difficulty**, from a simulation of 400 runs per match: a steady player (taps within about
  ±60 ms) wins about 65% of World Cup matches with the starting bat and 91% with the English
  willow. A more casual player (±100 ms) wins 78% in the Gully Cup but only 21% at the World Cup.
  A unit test checks that each tournament is harder than the last and that better bats help.
- **Ads:** a rewarded **free hit**. If you're out, the umpire calls a no-ball: the wicket is
  cancelled, you get 1 run, and the ball is bowled again as a free hit (once per match). There's
  also an **interstitial between matches**, starting from the 3rd match you finish.

## Kingdom Guard

Merge defense. Buy recruits onto stone pads beside the road, and drag a soldier onto an
identical one to merge them into the next rank: Recruit → Militia → Spearman → Archer →
Knight → Captain → Paladin → Champion. Each rank has its own damage, range and attack speed.
Goblins, orcs and trolls march from the cave to your castle, with a Warlord every 10th wave.
Soldiers fight on their own.

It's built to practise what a bigger strategy game needs:

- **Pathfinding:** maps are drawn as text (`#` rock, `.` road, `P` pad, `S` spawn, `C` castle),
  and a breadth-first search works out the enemies' route. Change the map and the route changes
  with it. Green Valley (waves 1–10) and Hill Fort (11–20) alternate.
- **Target finding:** each soldier attacks the enemy in range that's furthest along the road.
- **Troop stats and waves:** see `Rank`, `EnemyKind` and `Waves` in `logic/Units.kt`. Enemies get
  13% tougher each wave.
- **Upgrades:** the forge raises every soldier's damage (+15% a level) and attack speed (−8%
  cooldown a level).
- **Tests:**
  - every map has a route, and every pad is within a recruit's reach of it
  - the BFS returns shortest paths
  - merging and targeting rules hold
  - a simulated player who buys, merges and forges greedily survives wave 12 or later (it
    currently falls on wave 20) but not forever

**Ads** (your plan didn't list any for this game, so these are a suggestion): a rewarded
**Rebuild walls** (revive the castle with half its lives, once per run) and an interstitial
after a run ends, from the 3rd run on.

## Brain Games

Four quick brain-training games, each scored out of 250, which add up to a daily **Brain Score**
out of 1000. Playing on consecutive days builds a 🔥 streak.

- **Quick Math** (speed, 60 s): pick the right answer out of four. Each right answer raises the
  level: bigger numbers, then ×, then ÷. A wrong answer drops it a step.
- **Memory Grid** (memory): tiles light up, then go dark; tap the ones that were lit. Each round
  adds a tile, and the grid grows from 3×3 to 6×6. You have 3 lives.
- **Color Clash** (focus, 45 s): a colour word printed in a different ink. Tap the ink colour,
  not the word. 75% of cards clash, and streaks score extra.
- **Number Rush** (attention): tap 1–25 in order on a shuffled 5×5 grid. The clock starts on your
  first correct tap, and each wrong tap adds 2 s.

**Ads:** a rewarded **continue** in Memory Grid (one more life, once per game), and an
interstitial after every 3rd finished game.

Tests check that math answers are right (division is always whole) and options are distinct,
that harder operations unlock with level, and the memory difficulty curve and lives. They also
cover the colour-clash rate, Number Rush order and penalties, the Brain Score scale, and the streak rules.

## Puzzle Games

Four classic puzzle types in one app, each with endless numbered levels and up to 3 stars a level.

- **Slide Puzzle:** slide the tiles back into order (3×3 for levels 1–8, then 4×4). Tap any tile in
  the gap's row or column to slide it. Levels are scrambled from the solved board, so they're always
  solvable. On 3×3, par is the true minimum (found by A*). On 4×4 it's the scramble length.
- **Lights Out:** tapping a light toggles it and its neighbours, and you turn them all off (3×3 → 4×4
  → 5×5). Par is the exact fewest taps, worked out by solving the board as equations over GF(2).
- **Pipe Flow:** rotate pipe pieces until water from the tank reaches every tile with no open ends
  (3×3 → 6×6). Levels are a random tree of pipes with each piece turned at random. Par is the fewest
  turns.
- **Mini Sudoku:** 6×6 with 2×3 boxes, and fewer clues as levels rise (21 down to about 12). Every
  puzzle is checked to have exactly one solution. Wrong numbers aren't placed and count as mistakes.

Stars: at or under par = 3, up to 50% over = 2, otherwise 1 (Sudoku: 0 mistakes = 3, 1–2 = 2).

**Ads:** a rewarded **Hint** in every puzzle, and an interstitial after every 3rd solved level.
Each hint makes one helpful move:
- **Slide:** follows a planned solution.
- **Lights Out:** presses a light in the shortest solution.
- **Pipe Flow:** fixes one wrongly turned piece.
- **Sudoku:** fills the selected cell.

Tests check that levels of every type can be solved, that repeated hints always finish a puzzle,
that the Lights Out par is optimal, that Sudoku solutions are unique, and that levels are
deterministic.

## Word Games

Four word games on a real English dictionary.

- **Anagram Rush:** 60 seconds to unscramble as many words as you can. Any real word that uses all
  the letters counts, and words get longer as you go.
- **Word Builder:** make words of 3+ letters from a 6- or 7-letter word. Find about 40% of the
  possible words (5–20) to clear the level.
- **Word Ladder:** change one letter at a time to get from one word to another, with every step a
  real word. Par is the shortest route, found by breadth-first search over the dictionary,
  e.g. `woods → foods → foots → boots → blots → blows → glows`.
- **Word Search:** swipe to find hidden words. Levels add diagonals, then backwards words.

**Word lists:** `games/wordgames/tools/build_wordlists.py` builds them from the `wordlist-english`
npm package (MIT), which comes from **SCOWL**. Players may enter any word from SCOWL sizes 10–35
(about 24,000 words of 3–8 letters). Answers and puzzles only use the commonest words, sizes 10–20
(about 7,500), minus a blocklist of offensive words. SCOWL's copyright and permission notice is in
`games/wordgames/THIRD_PARTY_NOTICES-SCOWL.txt`, and inside the app under **Word list credits**,
as its license requires.

**Ads:** a rewarded **Hint** in every game (reveal a letter, give a word, take the next ladder step,
or point at a hidden word), and an interstitial after every 3rd level or round.
