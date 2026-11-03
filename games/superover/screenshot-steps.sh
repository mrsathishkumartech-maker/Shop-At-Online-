# Sourced by scripts/screenshots.sh with $pkg and $game set.
# Mid-career home screen, then the match screen after the first (unplayed) ball.
set_prefs "$pkg" "$game" "games/$game/screenshot-career.xml"
shot "$pkg" "$game-home"
tap_text "Play match" || true
wait_for_text "Tap anywhere when the ball reaches the bat" || true
# First ball: bowled 1.1s after the screen opens, about a second in the air, then the result.
sleep 2.8
capture "$game-match"
