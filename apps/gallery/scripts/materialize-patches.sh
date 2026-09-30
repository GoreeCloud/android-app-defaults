#!/usr/bin/env bash
set -euo pipefail

OUT_DIR="${1:-.build/patches}"
mkdir -p "$OUT_DIR"

materialize() {
  local gc="$1"
  local output="$2"
  local expected_blob="$3"
  local exact_path="patches/$gc/$output"

  if [ -f "$exact_path" ]; then
    cp "$exact_path" "$OUT_DIR/$output"
  else
    mapfile -t fragments < <(find "patches/$gc" -maxdepth 1 -type f -name '*.pyfrag' | sort)
    if [ "${#fragments[@]}" -eq 0 ]; then
      echo "No patch source found for $gc" >&2
      exit 1
    fi

    case "$gc" in
      gc5|gc6)
        test "${#fragments[@]}" -eq 2
        cat "${fragments[0]}" > "$OUT_DIR/$output"
        printf '\n' >> "$OUT_DIR/$output"
        cat "${fragments[1]}" >> "$OUT_DIR/$output"
        ;;
      *)
        cat "${fragments[@]}" > "$OUT_DIR/$output"
        ;;
    esac

    printf '\n' >> "$OUT_DIR/$output"
  fi

  local actual_blob
  actual_blob="$(git hash-object "$OUT_DIR/$output")"
  if [ "$actual_blob" != "$expected_blob" ]; then
    echo "Patch provenance verification failed for $gc/$output" >&2
    echo "expected Git blob: $expected_blob" >&2
    echo "actual Git blob:   $actual_blob" >&2
    exit 1
  fi

  chmod +x "$OUT_DIR/$output"
  printf 'Verified %-4s %s (%s)\n' "$gc" "$output" "$actual_blob"
}

materialize gc1 build_goreecloud_gallery.py      100b2080cf5e82275dce7a1a1f35d8869ab8af38
materialize gc2 build_goreecloud_gallery_gc2.py  5a9d84b0eaa49107cba52f2b1f02131fa5d03f3e
materialize gc3 build_goreecloud_gallery_gc3.py  67a4e4acfebb2c6e3d271d5387ededa60bc0ee87
materialize gc4 build_goreecloud_gallery_gc4.py  8f3a48d424ead1c253e0e9eb91f27706e6162757
materialize gc5 build_goreecloud_gallery_gc5.py  e8b1362e87d0e47997fa7f2ee36f851b4123fab5
materialize gc6 build_goreecloud_gallery_gc6.py  4c9094e7b4139e0472f1c17ab2ff4a2186244c78
materialize gc7 build_goreecloud_gallery_gc7.py  516339487492806932ec14b669c183e4919b1187
materialize gc8 build_goreecloud_gallery_gc8.py  af2d061814c1066b42b91c64247db7bfce74ec61
materialize gc9 build_goreecloud_gallery_gc9.py  99d6d251aac2a18fd597be660127084f525ea6b3
materialize gc10 build_goreecloud_gallery_gc10.py 9fcc93cadeaba57e2191a6b90cb7f982d38a29a2
materialize gc11 build_goreecloud_gallery_gc11.py 7b9800db1af1b677ecaa0f98b393f8bdbd791928
materialize gc12 build_goreecloud_gallery_gc12.py 68347d0592d0db7c2c32d618aac714c12eadeb4e
materialize gc13 build_goreecloud_gallery_gc13.py a2b8db074e439c7d33ae29fe919d6f6c78170f4a
materialize gc14 build_goreecloud_gallery_gc14.py 2c4560546980539b240e2ad0f2b2959a5a41c6f4
materialize gc15 build_goreecloud_gallery_gc15.py 7bb075526d4c35997f01683ccaec3a55c9d55f5f
materialize gc16 build_goreecloud_gallery_gc16.py a182eb4b7e28a04d507251bb820ba9f48ada6ec5
materialize gc17 build_goreecloud_gallery_gc17.py 5a2f02f6f343da3e7e2849ae54d20b4e6a060bce
