# Sourced by scripts/screenshots.sh with $pkg and $game set.
# Buy four recruits, start wave 1, and capture the fight in progress.
for _ in 1 2 3 4; do tap_id recruit_button || true; sleep 0.5; done
capture "$game-recruits"
tap_id wave_button || true
sleep 7
capture "$game-wave1"
