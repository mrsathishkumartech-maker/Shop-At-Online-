# Sourced by scripts/screenshots.sh with $pkg and $game set.
# A player with a 5-day streak and three games played today, then each game in turn.
today=$(( $(adb shell date +%s | tr -d '\r') / 86400 ))
prefs="screenshots/.braingames-prefs.xml"
cat > "$prefs" <<XML
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <int name="int_best_QUICK_MATH" value="610" />
    <int name="int_today_QUICK_MATH" value="540" />
    <int name="int_day_QUICK_MATH" value="$today" />
    <int name="int_best_MEMORY_GRID" value="33" />
    <int name="int_today_MEMORY_GRID" value="25" />
    <int name="int_day_MEMORY_GRID" value="$today" />
    <int name="int_best_COLOR_CLASH" value="720" />
    <int name="int_best_NUMBER_RUSH" value="24300" />
    <int name="int_today_NUMBER_RUSH" value="27800" />
    <int name="int_day_NUMBER_RUSH" value="$today" />
    <int name="int_streak" value="5" />
    <int name="int_last_day" value="$today" />
    <int name="int_games_played" value="1" />
</map>
XML
set_prefs "$pkg" "$game" "$prefs"
rm -f "$prefs"
shot "$pkg" "$game-home"

for title in "Quick Math" "Color Clash" "Number Rush" "Memory Grid"; do
  tap_text "$title" || continue
  wait_for_text "$title" || true
  sleep 0.8
  capture "$game-$(echo "$title" | tr 'A-Z ' 'a-z-')"
  adb shell input keyevent KEYCODE_BACK
  sleep 1.5
done
