# Sourced by scripts/screenshots.sh with $pkg and $game set.
# Each game's first screen. Anagram Rush goes last: its running clock keeps the screen
# changing, which stops UI dumps from working, and its time's-up dialog ignores Back.
for title in "Word Builder" "Word Ladder" "Word Search" "Anagram Rush"; do
  tap_text "$title" || continue
  sleep 4
  capture "$game-$(echo "$title" | tr 'A-Z ' 'a-z-')"
  adb shell input keyevent KEYCODE_BACK
  sleep 1.5
done
