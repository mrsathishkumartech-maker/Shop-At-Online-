# Sourced by scripts/screenshots.sh with $pkg and $game set.
# Mid-career home screen, then a live ball, then the result of not swinging.
set_prefs "$pkg" "$game" "games/$game/screenshot-career.xml"
shot "$pkg" "$game-home"
tap_text "Play match" || true
# The first ball is bowled 1.1s after the match screen opens and takes about a second.
wait_for_text "Tap anywhere when the ball reaches the bat" || true
sleep 1.5
capture "$game-ball"
sleep 2
capture "$game-result"
