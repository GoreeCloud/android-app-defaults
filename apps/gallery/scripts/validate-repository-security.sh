#!/usr/bin/env bash
set -euo pipefail

fail() {
  printf 'GoreeCloud Gallery repository security validation failed: %s\n' "$*" >&2
  exit 1
}

mapfile -t shell_scripts < <(find scripts -maxdepth 1 -type f -name '*.sh' | sort)
[ "${#shell_scripts[@]}" -ge 1 ] || fail "no GoreeCloud shell scripts found"
for script in "${shell_scripts[@]}"; do
  bash -n "$script" || fail "shell syntax check failed: $script"
done

mapfile -t workflow_files < <(find .github/workflows -maxdepth 1 -type f \( -name '*.yml' -o -name '*.yaml' \) | sort)
[ "${#workflow_files[@]}" -ge 1 ] || fail "no GitHub Actions workflows found"

for workflow in "${workflow_files[@]}"; do
  grep -Eq '^permissions:' "$workflow" \
    || fail "$workflow does not declare an explicit top-level permissions boundary"

  ! grep -Eq '^[[:space:]]*pull_request_target:' "$workflow" \
    || fail "$workflow uses pull_request_target, which is not approved for Gallery validation"

  ! grep -Eq '^[[:space:]]*write-all[[:space:]]*$|^[[:space:]]*[A-Za-z0-9_-]+:[[:space:]]*write([[:space:]]*#.*)?$' "$workflow" \
    || fail "$workflow grants a write permission; Gallery validation workflows must remain read-only"
done

while IFS= read -r use_target; do
  case "$use_target" in
    ./*)
      continue
      ;;
  esac

  ref="${use_target##*@}"
  if [[ ! "$ref" =~ ^[0-9a-f]{40}$ ]]; then
    fail "third-party Action is not pinned to a 40-character commit SHA: $use_target"
  fi
done < <(grep -R -h -E '^[[:space:]]*uses:[[:space:]]*[^[:space:]#]+' .github/workflows \
  | sed -E 's/^[[:space:]]*uses:[[:space:]]*([^[:space:]#]+).*$/\1/')

python3 - <<'PY'
from pathlib import Path
import re
import sys

workflows = sorted(Path('.github/workflows').glob('*.y*ml'))
for workflow in workflows:
    text = workflow.read_text(encoding='utf-8')
    lines = text.splitlines()
    for index, line in enumerate(lines):
        if re.search(r'uses:\s*actions/checkout@[0-9a-f]{40}', line):
            block = []
            for candidate in lines[index + 1:]:
                if re.match(r'^\s{6}-\s+name:', candidate):
                    break
                block.append(candidate)
            joined = '\n'.join(block)
            if not re.search(r'^\s*persist-credentials:\s*false\s*(?:#.*)?$', joined, re.MULTILINE):
                print(f'{workflow}: actions/checkout does not explicitly disable persisted credentials', file=sys.stderr)
                sys.exit(1)
PY

ACCEPTANCE_WORKFLOW='.github/workflows/build-and-validate.yml'
SIGNED_WORKFLOW='.github/workflows/build-signed-release-candidate.yml'

grep -Fq "github.event.pull_request.head.sha" "$ACCEPTANCE_WORKFLOW" \
  || fail "acceptance workflow does not bind pull-request validation to the exact head SHA"
grep -Fq "ref: \${{ github.event_name == 'pull_request' && github.event.pull_request.head.sha || github.sha }}" "$ACCEPTANCE_WORKFLOW" \
  || fail "acceptance workflow exact-revision checkout expression is missing"

grep -Fq 'environment: stable-release' "$SIGNED_WORKFLOW" \
  || fail "signed release-candidate workflow is not bound to the stable-release environment"
grep -Fq 'workflow_dispatch:' "$SIGNED_WORKFLOW" \
  || fail "signed release-candidate workflow is not manual"
! grep -Eq '^[[:space:]]*(push|pull_request|pull_request_target):' "$SIGNED_WORKFLOW" \
  || fail "signed release-candidate workflow must not run automatically on push or pull requests"
grep -Fq 'ref: ${{ github.sha }}' "$SIGNED_WORKFLOW" \
  || fail "signed release-candidate workflow does not check out the exact dispatched SHA"

mapfile -t secret_files < <(find . -path './.git' -prune -o -type f \( \
  -name '*.jks' -o \
  -name '*.keystore' -o \
  -name '*.p12' -o \
  -name '*.pfx' -o \
  -name '*.pem' -o \
  -name '*.key' -o \
  -name '*.der' \
\) -print)

if [ "${#secret_files[@]}" -ne 0 ]; then
  printf 'Unexpected key/certificate container files found:\n' >&2
  printf '  %s\n' "${secret_files[@]}" >&2
  fail "release key material must remain outside source control"
fi

if grep -R -n -E \
  --exclude-dir=.git \
  --exclude='*.md' \
  -- '-----BEGIN ([A-Z0-9 ]+ )?PRIVATE KEY-----' \
  .; then
  fail "private-key PEM material appears to be committed"
fi

printf 'GoreeCloud Gallery repository security guardrails passed.\n'
