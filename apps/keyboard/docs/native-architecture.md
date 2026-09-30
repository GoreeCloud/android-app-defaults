# Native architecture foundation

## Application boundary

GoreeCloud Keyboard is an original first-party input-method application. The Android implementation uses the platform `InputMethodService` contract and a GoreeCloud-owned rendering/input layer. No third-party keyboard application source is inherited.

## Platform systems

Glaze UI governs keyboard geometry, spacing, adaptive presentation, interaction feedback, accessibility, and visual semantics. The initial surface intentionally keeps those decisions local to a first-party `KeyboardView` so the design can evolve without inheriting another keyboard's widget architecture.

Wardveil Security governs trust boundaries around input, clipboard access, extensions, dictionaries, models, updates, and any future privileged integrations. The keyboard must treat typed content as highly sensitive and must avoid unnecessary persistence or observability.

Privacy Shield governs data minimization and user-visible privacy state. The Android foundation requests no network permission. Future Quill language intelligence is designed behind a local suggestion boundary rather than assuming remote processing.

Everkeep governs recoverability and portability of user-controlled keyboard data that is intentionally persisted, such as dictionaries, preferences, layouts, and explicitly enabled personalization. Typed text itself is not a backup target.

## Initial Android slice

The first source slice contains:

- `KeyboardService`: Android IME lifecycle and text-commit boundary.
- `KeyboardView`: first-party QWERTY rendering and pointer input.
- `SuggestionEngine`: local-only Quill suggestion boundary.
- Android manifest and IME metadata with no network permission.
- Unit coverage for the initial suggestion behavior.
- CI that builds the Android debug APK and runs unit tests.

## Next implementation layers

The planned code architecture separates input events, layout models, language intelligence, personalization, clipboard functions, settings, platform security/privacy evidence, and rendering. This separation is required before swipe decoding, autocorrection, multilingual dictionaries, adaptive layouts, or richer Quill features are added.
