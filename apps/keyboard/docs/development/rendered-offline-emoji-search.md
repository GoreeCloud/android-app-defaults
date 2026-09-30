# Rendered offline emoji search — Development

The native Android Emoji layer now exposes a first-party local search control using the existing `EmojiSearchSession` and `OfflineEmojiSearch` contracts.

Behavior and privacy boundary:

- A Search entry is visible in the compact emoji category strip.
- Opening search keeps the IME in the Emoji layer and replaces the emoji grid with a temporary letter-key query surface.
- Query text, spaces, and backspace are intercepted inside `KeyboardView` and mutate only the transient `EmojiSearchSession`.
- Search query text is never sent through `InputConnection`, never added to Quill composing state, and never persisted to emoji recents.
- Up to three of the already-bounded local search results are rendered in the top strip for direct insertion.
- Only tapping an emoji search result calls the normal text-commit path; that selected emoji may then be recorded in the existing device-local recents store.
- Clear and Close affect only the transient search session. Leaving Emoji mode also clears the session.
- Search uses only the emoji catalog packaged with GoreeCloud Keyboard. It introduces no network permission, telemetry, cloud catalog, account lookup, or remote personalization.

Status: **Development**. Complete Glaze UI 2.1 rendered/native acceptance, TalkBack/switch-access acceptance, representative physical-device input acceptance, signed release packaging, and Stable qualification remain separate gates.
