# Regional-indicator backspace parity — Development

GoreeCloud Keyboard's bounded Unicode text-unit deletion model now follows the Unicode regional-indicator pair-from-the-start rule for consecutive flag-letter sequences.

Behavior and privacy boundary:

- A normal two-indicator flag continues to delete in one backspace action.
- In a run of three regional indicators, the trailing unmatched indicator deletes by itself rather than being incorrectly merged with the second indicator.
- In a run of four, the final pair deletes together; runs of five again leave the final unmatched indicator as a single deletion unit.
- Non-regional text resets the pairing run.
- Existing bounded handling for combining marks, variation selectors, emoji modifiers, keycaps, tag sequences, common ZWJ emoji, and CRLF remains unchanged.
- The correction operates only on the existing local look-behind text already used for non-sensitive editor backspace behavior. It adds no network access, telemetry, persistence, dictionary lookup, account state, or remote Unicode service.
- Sensitive-editor behavior remains unchanged and continues to use the existing privacy-gated single-code-point deletion path rather than reading extended context.

This remains a deliberately bounded deletion model. It does not claim complete Unicode UAX #29 extended-grapheme segmentation for every writing system.

Status: **Development**. Broader grapheme-segmentation coverage, representative physical-device IME acceptance, accessibility acceptance, complete current Stable Glaze UI application acceptance, signing, release, and Stable qualification remain separate gates.
