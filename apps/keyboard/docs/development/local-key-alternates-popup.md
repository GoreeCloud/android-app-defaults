# Local key alternates popup

Status: Development

The Android keyboard surface now consumes the deterministic `KeyAlternates` catalog through a native long-press interaction.

Eligible text keys schedule the platform long-press timeout. The resulting popup uses the Glaze interaction floor for alternate cells, supports pointer movement selection and cancellation, provides long-press haptic feedback, and announces the interaction through accessibility services. Selected alternates are committed through the existing `KeyboardView.Listener.onText` path, preserving the IME service as the text-commit authority.

## Privacy boundary

The popup performs no network lookup, learning, personalization, clipboard read, surrounding-text inspection, persistence, language inference, or sensitive-field classification. It uses only the key label and the static local alternates catalog.

Emoji search mode and emoji keys do not expose this alternates interaction.

This is Development evidence only; device gesture and accessibility acceptance remain required before Stable claims.
