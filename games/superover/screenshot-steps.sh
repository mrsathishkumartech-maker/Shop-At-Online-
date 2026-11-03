# Sourced by scripts/screenshots.sh with $pkg and $game set.
# Mid-career home screen, then a burst of captures through the first ball.
set_prefs "$pkg" "$game" "games/$game/screenshot-career.xml"
shot "$pkg" "$game-home"
tap_text "Play match" || true
# The emulator's launch time varies, so grab several frames; one lands mid-delivery.
for i in 1 2 3 4 5 6 7 8; do
  capture "$game-ball-$i"
  sleep 0.2
done
