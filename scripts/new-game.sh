#!/usr/bin/env bash
# Create a new game module from templates/game.
#
#   scripts/new-game.sh <game-id> "<Display Name>"
#   scripts/new-game.sh snakeladder "Snake & Ladder"
#
# game-id: lowercase letters and digits only. It becomes the module (:games:<id>),
# the package (com.sathishkumar.games.<id>) and the release tag prefix (<id>-v1.0.0).
set -euo pipefail

if [[ $# -ne 2 ]]; then
  echo "usage: $0 <game-id> \"<Display Name>\"" >&2
  exit 1
fi

id="$1"
name="$2"
root="$(cd "$(dirname "$0")/.." && pwd)"
dest="$root/games/$id"

if [[ ! "$id" =~ ^[a-z][a-z0-9]*$ ]]; then
  echo "game-id must be lowercase letters/digits and start with a letter (got '$id')" >&2
  exit 1
fi
if [[ -e "$dest" ]]; then
  echo "games/$id already exists" >&2
  exit 1
fi

cp -r "$root/templates/game" "$dest"

# Move sources into the package directory.
pkg_dir="$dest/src/main/kotlin/com/sathishkumar/games/$id"
mkdir -p "$pkg_dir"
mv "$dest/src/main/kotlin/MainActivity.kt" "$pkg_dir/"

# Escape characters that are special in sed replacements and in XML.
escaped_name=$(printf '%s' "$name" | sed -e 's/&/\\&amp;/g' -e "s/'/\\\\\\\\'/g" -e 's/[|]/\\|/g')
find "$dest" -type f \( -name '*.kts' -o -name '*.kt' -o -name '*.xml' \) -print0 |
  xargs -0 sed -i.bak -e "s|__GAME_ID__|$id|g" -e "s|__GAME_NAME__|$escaped_name|g"
find "$dest" -name '*.bak' -delete

echo "Created games/$id"
echo "  Build:   ./gradlew :games:$id:assembleDebug"
echo "  Release: git tag $id-v1.0.0 && git push origin $id-v1.0.0"
