#!/usr/bin/env bash
# Runs inside the Android emulator job (.github/workflows/screenshots.yml).
# Installs every game's debug APK, opens it fresh and in a later state, and saves screenshots.
set -euo pipefail

mkdir -p screenshots

# Saves what's on screen right now.
capture() {
  adb exec-out screencap -p > "screenshots/$1.png"
  echo "saved screenshots/$1.png"
}

# Restarts the app, waits for it to settle, and saves a screenshot.
shot() {
  local pkg="$1" name="$2"
  adb shell am force-stop "$pkg"
  adb shell monkey -p "$pkg" -c android.intent.category.LAUNCHER 1 > /dev/null
  sleep 10
  capture "$name"
}

# Waits (up to ~30s) until text "$1" appears on screen.
wait_for_text() {
  for _ in $(seq 1 15); do
    adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1 || true
    if adb exec-out cat /sdcard/ui.xml | grep -qF "text=\"$1\""; then return 0; fi
    sleep 1
  done
  echo "wait_for_text: '$1' never appeared"
  return 1
}

# Taps the on-screen element whose text is exactly "$1" (found via a UI dump).
tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml > /dev/null
  local xy
  xy=$(adb exec-out cat /sdcard/ui.xml | python3 -c '
import re, sys
xml, want = sys.stdin.read(), sys.argv[1]
for m in re.finditer(r"<node [^>]*>", xml):
    node = m.group(0)
    t = re.search(r"text=\"([^\"]*)\"", node)
    b = re.search(r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", node)
    if t and b and t.group(1) == want:
        x1, y1, x2, y2 = map(int, b.groups())
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
' "$1")
  if [[ -z "$xy" ]]; then echo "tap_text: '$1' not on screen"; return 1; fi
  adb shell input tap $xy
}

# Replaces the game's saved preferences with a prepared file.
set_prefs() {
  local pkg="$1" game="$2" file="$3"
  adb shell am force-stop "$pkg"
  adb shell "run-as $pkg mkdir -p shared_prefs"
  adb shell "run-as $pkg sh -c 'cat > shared_prefs/game_$game.xml'" < "$file"
}

set_level() {
  local pkg="$1" game="$2" level="$3"
  adb shell am force-stop "$pkg"
  adb shell "run-as $pkg mkdir -p shared_prefs"
  printf '<?xml version="1.0" encoding="utf-8" standalone="yes" ?>\n<map>\n    <int name="current_level" value="%s" />\n</map>\n' "$level" |
    adb shell "run-as $pkg sh -c 'cat > shared_prefs/game_$game.xml'"
}

# Keep the status bar tidy and stop animations from blurring captures.
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0

for apk in games/*/build/outputs/apk/debug/*-debug.apk; do
  game=$(basename "$(dirname "$(dirname "$(dirname "$(dirname "$(dirname "$apk")")")")")")
  pkg="com.sathishkumar.games.$game"
  echo "== $game"
  adb install -r "$apk" > /dev/null
  shot "$pkg" "$game-start"
  states="games/$game/screenshot-states"
  if [[ -f "games/$game/screenshot-steps.sh" ]]; then
    # Games with several screens script their own steps, using the helpers above.
    # shellcheck source=/dev/null
    source "games/$game/screenshot-steps.sh"
  elif [[ -d "$states" ]]; then
    # Games without levels ship prepared saves: games/<game>/screenshot-states/<name>.xml
    for file in "$states"/*.xml; do
      set_prefs "$pkg" "$game" "$file"
      shot "$pkg" "$game-$(basename "$file" .xml)"
    done
  else
    set_level "$pkg" "$game" "${LATER_LEVEL:-14}"
    shot "$pkg" "$game-level${LATER_LEVEL:-14}"
  fi
done
