#!/usr/bin/env bash
# Runs inside the Android emulator job (.github/workflows/screenshots.yml).
# Installs every game's debug APK, opens it fresh and in a later state, and saves screenshots.
set -euo pipefail

mkdir -p screenshots

shot() {
  local pkg="$1" name="$2"
  adb shell am force-stop "$pkg"
  adb shell monkey -p "$pkg" -c android.intent.category.LAUNCHER 1 > /dev/null
  sleep 10
  adb exec-out screencap -p > "screenshots/$name.png"
  echo "saved screenshots/$name.png"
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
  if [[ -d "$states" ]]; then
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
