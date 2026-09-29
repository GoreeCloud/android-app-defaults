# Offline Emoji Search Session — Development

This slice adds the transient interaction-state contract needed to render GoreeCloud Keyboard's existing fully offline emoji search model safely inside the native IME.

The search session is explicit: opening enables query collection; closing clears the query and results. Query state is process-memory-only, capped to 48 Unicode code points, supports complete-code-point backspace, and delegates result discovery exclusively to `OfflineEmojiSearch`. Text arriving while search is closed is ignored by this session boundary.

No query persistence, telemetry, account lookup, network request, remote catalog, cloud personalization, or synchronization path is introduced. The existing Android manifest no-network boundary remains unchanged.

The native KeyboardView still needs to render the search affordance and route only search-mode input into this session before this becomes a user-facing picker capability. Physical-device IME, accessibility, signed release, production, and Stable acceptance remain separate gates.
