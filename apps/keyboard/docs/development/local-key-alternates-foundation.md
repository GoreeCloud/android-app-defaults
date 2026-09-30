# Local key alternates foundation

Status: Development

This slice adds a deterministic device-local catalog for common long-press key alternates on the native keyboard surface.

The initial catalog covers common Latin diacritics and selected punctuation alternates, with uppercase variants derived deterministically for uppercase letter keys.

## Privacy boundary

Alternate lookup is based only on the visible key label supplied by the keyboard layout. It performs no network request, learning, personalization, clipboard read, surrounding-text inspection, persistence, language-model inference, or sensitive-field classification.

Unsupported keys return no alternates rather than inventing context-dependent choices.

## Next composition step

A later native UI slice can add accessible long-press popup rendering, touch-target movement, selection feedback, and cancellation behavior using this catalog while keeping actual input commit behavior inside the existing IME authority path.
