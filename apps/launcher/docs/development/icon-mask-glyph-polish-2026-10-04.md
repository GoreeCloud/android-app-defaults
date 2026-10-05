# Launcher app-icon mask and glyph polish

Status: Development candidate  
Base: authoritative monorepo main 45cf4a5a0e5f73e1675f64e7042cb90800bc0860  
Pull request: #238

## Problem

Representative-device screenshots show app artwork arriving with an Android/OEM mask already baked into transparent pixels, then being clipped into a second Launcher-selected shape. The visible result is undersized circular artwork floating inside rounded-square or other masks. The same screenshots also expose inconsistent Launcher-owned glyph weight and a misleading managed Search cleanup toast when no movable Search widget exists.

## Candidate behavior

- Prefer mask-neutral activity artwork before the already-badged Android fallback.
- Flatten adaptive background and foreground layers into a square presentation bitmap.
- Use bounded foreground overscan and bounded normalization for unusually padded legacy artwork.
- Apply Android profile badging after normalization.
- Let the Launcher shape own the final clip without a generic translucent backing plate.
- Provide a live Look & feel icon preview and a single accessible reset for shape, size, and icon-pack presentation.
- Use a more coherent rounded-line family for Settings categories, popup actions, App Drawer header controls, and local Search-source identity.
- Do not issue a managed Search removal mutation when Swipe down or fixed Search is selected and no Search widget exists.

## Authority boundary

This work changes presentation and local bitmap processing only. It adds no Android permission, network destination, account authority, query retention, package/profile visibility authority, workspace schema, telemetry, analytics, advertising, or system-wide app-blocking authority.

## Validation

Local source guards passed for privacy, manifest/Search authority, Glaze authority, Room cutover, Room schema, and whitespace. Fresh exact-head protected CI is required after every candidate-head change.

## Acceptance boundary

Emulator/source evidence cannot establish representative-device visual acceptance. Physical-device confirmation is still required for adaptive and legacy icon fill, profile badges, icon packs, all supported Launcher masks, glyph legibility, Search-mode transitions, accessibility, large text, RTL/localization, form factors, performance/power, protected Development signing/update continuity, and release lifecycle gates.
