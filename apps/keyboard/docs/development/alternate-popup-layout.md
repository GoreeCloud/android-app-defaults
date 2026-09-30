# Alternate popup layout policy

Status: Development — Glaze UI 2.2 Adoption Candidate line

The native keyboard has one pure viewport-bounded geometry authority for long-press alternate character popups, and `KeyboardView` consumes that same authority for both rendering and pointer-move selection on the active Glaze UI 2.2 adoption branch.

The policy keeps the current 48 dp Glaze-sized cells inside the available keyboard viewport, reduces the column count on narrow surfaces, prefers placement above the source key, uses below-key placement when the top edge is constrained, and fails closed when the available surface cannot fit a valid popup.

`KeyboardView.drawAlternatePopup(...)` delegates placement and exact per-item geometry to `AlternatePopupLayout.calculate(...)` and `itemBounds(...)` instead of reconstructing row/column arithmetic. The resulting `AlternatePopupLayoutResult` is retained only for the active popup interaction. If a valid popup cannot be laid out, the retained layout and selection are explicitly nulled so an invisible or stale alternate cannot be committed on pointer-up.

`KeyboardView` pointer movement delegates directly to `AlternatePopupLayoutResult.hitTest(...)`. Rendering and gesture selection therefore use the same tested cell geometry rather than maintaining a second mutable list of Android `RectF` hit targets. `hitTest(...)` returns an item only for a finite pointer coordinate inside an actual alternate cell; inter-cell gaps, unused cells in the final row, non-finite coordinates, points outside the popup, and a popup that has not produced a valid layout all fail closed to no selection.

This composition means the popup geometry can be reviewed as part of the current 2.2 Adoption Candidate rather than as a parallel historical interaction branch. It does not promote the application to current-Stable conformance: platform Touch Assistance detection and 56 dp hit-area integration, complete component/state mapping, Reduced Motion/Transparency, Increased Contrast, Forced Colors, 200% text/reflow, RTL/localization, TalkBack/Switch Access, and representative physical-device long-press/slide/release acceptance remain separate gates.

The layout and hit-test path consumes only source-key, viewport, and pointer geometry. It performs no surrounding-text inspection, clipboard read, language inference, learning, personalization, persistence, or network request. Existing IME text-commit authority is unchanged.
