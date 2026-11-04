# Sourced by scripts/screenshots.sh with $pkg and $game set.
# Home, then each puzzle's first level; for Sudoku also select a cell.
for title in "Slide Puzzle" "Lights Out" "Pipe Flow" "Mini Sudoku"; do
  tap_text "$title" || continue
  sleep 2
  capture "$game-$(echo "$title" | tr 'A-Z ' 'a-z-')"
  adb shell input keyevent KEYCODE_BACK
  sleep 1.5
done
