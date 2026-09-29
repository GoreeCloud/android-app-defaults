# Portable Preference Import Preview

## Status

Development candidate only. This is a user-control refinement for the existing one-field `goreecloud-keyboard-preferences/1` portability workflow.

## Behavior

Selecting an import document through Android's Storage Access Framework no longer writes the local category preference immediately. The selected bytes are first bounded to 4096 bytes, checked for canonical UTF-8, and fully decoded through the strict existing snapshot codec.

A successful decode produces only a typed `EmojiCategory` preview. No preference writer is passed to the preview step and no SharedPreferences mutation occurs while the user reviews the proposed category.

The native Activity then presents the decoded category in an explicit confirmation dialog. **Apply** performs the single category write. **Cancel** dismisses the preview and leaves the current preference unchanged.

## Privacy boundary

The preview contains exactly one low-sensitivity explicit preference: the selected emoji category. It has no surface for typed text, composing or surrounding editor content, suggestions, learned input, emoji recents or frequency history, search queries, clipboard data, key history, sensitive-editor values, telemetry, Identity data, credentials, or cryptographic secrets.

No InputConnection, clipboard, network, background URI grant, synchronization path, or broader storage permission is introduced.

## Recovery boundary

This remains one-field portability. The preview/confirmation flow is not product-wide Keyboard restore, backup, Everkeep recovery, cross-device synchronization, conflict resolution, provenance verification, or production Privacy Shield/Wardveil acceptance.

## Acceptance boundary

Source/build/test validation does not establish rendered accessibility, representative-device ergonomics, complete GLAZE UI V1.0 acceptance, signed distribution, release approval, production, or Stable qualification.
