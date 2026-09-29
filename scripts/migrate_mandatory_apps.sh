#!/usr/bin/env bash
set -euo pipefail

TARGET_REPOSITORY="GoreeCloud/android-app-defaults"
MIGRATION_DATE="2026-09-29"

declare -A SOURCE_REPOSITORY=(
  [camera]="GoreeCloud/camera"
  [launcher]="GoreeCloud/launcher"
  [keyboard]="GoreeCloud/keyboard"
  [gallery]="GoreeCloud/gallery"
)

declare -A SOURCE_SHA=(
  [camera]="9b02ca8c19d56868ed30b3d457631e582ef529cb"
  [launcher]="1eb8dd6d8178f6d730b100559e8f8d149501d18c"
  [keyboard]="041b5e0457f2b4d68dc96a416d5abc1f020646d7"
  [gallery]="3f7263c6e31a0af0f1a06b99491f608b96cc69ba"
)

declare -A SOURCE_PR=(
  [camera]="19"
  [launcher]="248"
  [keyboard]="97"
  [gallery]="103"
)

declare -A SOURCE_STATUS=(
  [camera]="Android Foundation was in progress at the refreshed imported head at migration cutover."
  [launcher]="Android CI was in progress at the refreshed imported head at migration cutover; the immediately preceding head had a failing Android 16 Room/runtime test and was therefore not used as the final cutover revision."
  [keyboard]="Android CI and Platform Contract passed at the imported head."
  [gallery]="Native Android App, Native Android Adapter, and Native Android Rendered Acceptance passed at the imported head."
)

workspace="$(mktemp -d)"
trap 'rm -rf "$workspace"' EXIT

for app in camera launcher keyboard gallery; do
  source_repo="${SOURCE_REPOSITORY[$app]}"
  source_sha="${SOURCE_SHA[$app]}"
  source_pr="${SOURCE_PR[$app]}"
  source_status="${SOURCE_STATUS[$app]}"
  source_dir="$workspace/$app"

  git clone --quiet --filter=blob:none --no-checkout "https://github.com/$source_repo.git" "$source_dir"
  git -C "$source_dir" fetch --quiet --depth=1 origin "$source_sha"
  git -C "$source_dir" checkout --quiet --detach "$source_sha"

  rm -rf "apps/$app"
  mkdir -p "apps/$app"
  rsync -a --delete \
    --exclude='.git/' \
    --exclude='.github/' \
    --exclude='artifacts/' \
    "$source_dir/" "apps/$app/"

  cat > "apps/$app/MIGRATION-ORIGIN.md" <<EOF
# ${app^} migration origin

This application is mandatorily migrated into `$TARGET_REPOSITORY` by the GoreeCloud owner directive dated $MIGRATION_DATE.

- Source repository: `$source_repo`
- Imported Development revision: `$source_sha`
- Source pull request at migration start: #$source_pr
- Destination: `apps/$app/`
- Import exclusions: repository-scoped `.github/` metadata and generated `artifacts/` output.
- Source validation at migration start: $source_status
- Lifecycle boundary: this is a Development source migration. It does not establish Release Candidate, Stable, production, representative-device, or other unverified acceptance.
- History boundary: the standalone source repository remains the historical Git provenance until repository-retirement reconciliation is complete.

Repository-scoped workflows are not copied into a nested application directory because GitHub would not execute them there. The destination repository owns monorepo-level validation and migration-integrity checks.
EOF
done

mkdir -p docs/migrations
cat > docs/migrations/2026-09-29-mandatory-app-consolidation.md <<'EOF'
# Mandatory Android app consolidation — 2026-09-29

GoreeCloud Camera, GoreeCloud Launcher, GoreeCloud Keyboard, and GoreeCloud Gallery are required to use `GoreeCloud/android-app-defaults` as their source-development repository.

## Imported Development revisions

| Application | Legacy repository | Exact imported revision | Active source PR at migration start |
| --- | --- | --- | --- |
| Camera | `GoreeCloud/camera` | `9b02ca8c19d56868ed30b3d457631e582ef529cb` | #19 |
| Launcher | `GoreeCloud/launcher` | `1eb8dd6d8178f6d730b100559e8f8d149501d18c` | #248 |
| Keyboard | `GoreeCloud/keyboard` | `041b5e0457f2b4d68dc96a416d5abc1f020646d7` | #97 |
| Gallery | `GoreeCloud/gallery` | `3f7263c6e31a0af0f1a06b99491f608b96cc69ba` | #103 |

The migration intentionally preserves the newest active Development integration heads rather than copying only legacy default-branch state. Repository-scoped `.github/` content and generated `artifacts/` are excluded from nested application imports; destination-root workflows own CI and source-integrity validation.

## Validation boundary

Keyboard and Gallery had successful relevant exact-head CI at migration start. Camera's source validation job was successful while runtime-emulator jobs were still running. Launcher's validate and transition-performance jobs were successful, but its Android 16 runtime job failed in `secondaryHomeRendersMovedBuiltInWidget` with a Compose `performMeasureAndLayout called during measure layout` exception. The migration preserves that Development state and does not convert it into an acceptance claim.

## Legacy repositories

The four legacy repositories must remain available as Git-history and migration provenance until open pull requests, issues, release references, external links, and any other repository-native dependencies have been reconciled. New product source development belongs in this monorepo after the migration cutover is merged.
EOF

python3 - <<'PY'
from pathlib import Path
p = Path("README.md")
text = p.read_text(encoding="utf-8")
marker = "## Current Development state\n"
block = """## Mandatory app repository consolidation

Camera, Launcher, Keyboard, and Gallery are being consolidated into this monorepo under `apps/camera/`, `apps/launcher/`, `apps/keyboard/`, and `apps/gallery/`. The imported revisions are Development snapshots with exact legacy-repository provenance; migration does not imply release or Stable acceptance. See [the migration record](docs/migrations/2026-09-29-mandatory-app-consolidation.md).

"""
if "## Mandatory app repository consolidation" not in text:
    if marker not in text:
        raise SystemExit("README development-state marker not found")
    text = text.replace(marker, block + marker, 1)
    p.write_text(text, encoding="utf-8")
PY

git add apps/camera apps/launcher apps/keyboard apps/gallery docs/migrations/2026-09-29-mandatory-app-consolidation.md README.md

if git diff --cached --quiet; then
  echo "Migration content already matches the exact pinned sources."
  exit 0
fi

git config user.name "GoreeCloud Migration"
git config user.email "admin@goreecloud.com"
git commit -m "migration: consolidate Camera Launcher Keyboard and Gallery"
git push origin "HEAD:${GITHUB_REF_NAME}"
