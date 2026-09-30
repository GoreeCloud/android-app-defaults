# Offline Emoji Search — Development

GoreeCloud Keyboard now has a bounded, fully local emoji discovery model over the emoji already packaged with the Android IME.

## Implemented in this slice

- Plain-language keyword search covers representative smileys, people, nature, food, travel, and symbol emoji.
- Category names and exact emoji characters are searchable.
- Search scans only `KeyboardLayout`'s built-in local emoji rows; it cannot return a remote or unapproved catalog item.
- Multi-token queries require every token to match the local category/keyword/emoji projection.
- Duplicate emoji are removed deterministically and results are capped at 24.
- Search does not add network access, telemetry, account state, cloud personalization, or synchronization.
- Automated tests cover representative terms, exact-emoji queries, category discovery, empty input, and result bounds.

## Boundary

This is the offline search model foundation. The Android keyboard surface does not yet expose a search field or search-result presentation, and no Stable UX acceptance is claimed.
