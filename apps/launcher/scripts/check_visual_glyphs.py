#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

TARGETS = (
    ROOT / "app/src/main/java/com/goreecloud/launcher/ui/LauncherProviderControlledSearchSurface.kt",
    ROOT / "app/src/main/java/com/goreecloud/launcher/ui/LauncherStartupWizard.kt",
    ROOT / "app/src/main/java/com/goreecloud/launcher/ui/LauncherBetaRoot.kt",
)

# These characters previously appeared as visible pseudo-icons in Search, onboarding,
# Home editor, or App Drawer surfaces. Launcher now owns vector geometry for these roles.
FORBIDDEN = ("✓", "★", "⚙", "◫", "▦", "▤", "⌕", "↗", "☎", "✉", "↑", "↓", "←")


def fail(message: str) -> None:
    raise SystemExit(f"Launcher visual glyph guard failed: {message}")


def main() -> None:
    for path in TARGETS:
        if not path.is_file():
            fail(f"missing source file: {path.relative_to(ROOT)}")
        text = path.read_text(encoding="utf-8")
        offenders = [glyph for glyph in FORBIDDEN if glyph in text]
        if offenders:
            joined = " ".join(offenders)
            fail(
                f"{path.relative_to(ROOT)} contains font-dependent pseudo-icons: {joined}. "
                "Use Launcher-owned vector geometry and accessibility semantics instead."
            )

    print(
        "Launcher visual glyph guard passed: Search, onboarding, Home editor, and Drawer "
        "surfaces contain no guarded font-dependent pseudo-icons."
    )


if __name__ == "__main__":
    main()
