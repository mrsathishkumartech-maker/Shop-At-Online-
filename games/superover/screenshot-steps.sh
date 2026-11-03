# Sourced by scripts/screenshots.sh with $pkg and $game set.
# Mid-career home screen, then a live ball, then the result of not swinging.
set_prefs "$pkg" "$game" "games/$game/screenshot-career.xml"
shot "$pkg" "$game-home"
tap_text "Play match" || true
sleep 1.9
capture "$game-ball"
sleep 1.3
capture "$game-result"
