#!/usr/bin/env bash
set -euo pipefail

GALLERY_DIR="${1:-upstream-gallery}"
COMMONS_DIR="${2:-upstream-commons}"

readonly GALLERY_REPOSITORY="https://github.com/FossifyOrg/Gallery.git"
readonly COMMONS_REPOSITORY="https://github.com/FossifyOrg/commons.git"
readonly GALLERY_COMMIT="b28299dc33821eee8d108a9880ce87876cf31443"
readonly COMMONS_COMMIT="acfd352df1a1852d17a5f77def8b7ad6e522a5b6"

fail() {
  printf 'GoreeCloud Gallery reconstruction failed: %s\n' "$*" >&2
  exit 1
}

for path in "$GALLERY_DIR" "$COMMONS_DIR"; do
  if [ -e "$path" ]; then
    fail "refusing to reuse existing path: $path"
  fi
done

bash scripts/materialize-patches.sh

git clone --filter=blob:none --no-checkout "$GALLERY_REPOSITORY" "$GALLERY_DIR"
git -C "$GALLERY_DIR" checkout --detach "$GALLERY_COMMIT"
test "$(git -C "$GALLERY_DIR" rev-parse HEAD)" = "$GALLERY_COMMIT" \
  || fail "Gallery checkout does not match the pinned upstream commit"

git clone --filter=blob:none --no-checkout "$COMMONS_REPOSITORY" "$COMMONS_DIR"
git -C "$COMMONS_DIR" checkout --detach "$COMMONS_COMMIT"
test "$(git -C "$COMMONS_DIR" rev-parse HEAD)" = "$COMMONS_COMMIT" \
  || fail "Commons checkout does not match the pinned upstream commit"

python3 .build/patches/build_goreecloud_gallery.py "$GALLERY_DIR"
python3 .build/patches/build_goreecloud_gallery_gc2.py "$GALLERY_DIR" "$COMMONS_DIR"
python3 .build/patches/build_goreecloud_gallery_gc3.py "$GALLERY_DIR" "$COMMONS_DIR"
python3 .build/patches/build_goreecloud_gallery_gc4.py "$GALLERY_DIR" "$COMMONS_DIR"
python3 .build/patches/build_goreecloud_gallery_gc5.py "$GALLERY_DIR" "$COMMONS_DIR"
python3 .build/patches/build_goreecloud_gallery_gc6.py "$GALLERY_DIR" "$COMMONS_DIR"
python3 .build/patches/build_goreecloud_gallery_gc7.py "$GALLERY_DIR" "$COMMONS_DIR"
python3 .build/patches/build_goreecloud_gallery_gc8.py "$GALLERY_DIR" "$COMMONS_DIR"
python3 .build/patches/build_goreecloud_gallery_gc9.py "$GALLERY_DIR" "$COMMONS_DIR"
python3 .build/patches/build_goreecloud_gallery_gc10.py "$GALLERY_DIR" "$COMMONS_DIR"
python3 .build/patches/build_goreecloud_gallery_gc11.py "$GALLERY_DIR" "$COMMONS_DIR"
python3 .build/patches/build_goreecloud_gallery_gc12.py "$GALLERY_DIR" "$COMMONS_DIR"
python3 .build/patches/build_goreecloud_gallery_gc13.py "$GALLERY_DIR" "$COMMONS_DIR"
python3 .build/patches/build_goreecloud_gallery_gc14.py "$GALLERY_DIR" "$COMMONS_DIR"
python3 .build/patches/build_goreecloud_gallery_gc15.py "$GALLERY_DIR" "$COMMONS_DIR"
python3 .build/patches/build_goreecloud_gallery_gc16.py "$GALLERY_DIR" "$COMMONS_DIR"
python3 .build/patches/build_goreecloud_gallery_gc17.py "$GALLERY_DIR" "$COMMONS_DIR"

python3 - "$GALLERY_DIR/settings.gradle.kts" <<'PY'
from pathlib import Path
import sys

path = Path(sys.argv[1])
path.write_text(path.read_text(encoding="utf-8").rstrip() + "\n", encoding="utf-8")
PY

git -C "$GALLERY_DIR" diff --check
git -C "$COMMONS_DIR" diff --check

printf 'Reconstructed GoreeCloud Gallery 1.0.0 acceptance-candidate source through gc.17.\n'
