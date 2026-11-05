# Sourced by scripts/screenshots.sh with $pkg and $game set.
# Each game's first screen; in Word Builder type a word first.
for title in "Anagram Rush" "Word Builder" "Word Ladder" "Word Search"; do
  tap_text "$title" || continue
  wait_for_text "Hint ▶" || true
  sleep 1.5
  capture "$game-$(echo "$title" | tr 'A-Z ' 'a-z-')"
  adb shell input keyevent KEYCODE_BACK
  sleep 1.5
done
