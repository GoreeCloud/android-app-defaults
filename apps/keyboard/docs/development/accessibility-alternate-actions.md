# Accessible Alternate Character Actions — Development

Status: **Development**

This continuation extends GoreeCloud Keyboard's merged native virtual-accessibility foundation so deterministic device-local key alternates can be discovered and activated without requiring a touch long-press gesture.

The Android accessibility contract is deliberately direct:

- every rendered key keeps its ordinary accessibility click semantics through the existing virtual node;
- an eligible key derives alternate characters only from the existing first-party `KeyAlternates` catalog;
- each alternate is exposed as a labeled custom Android accessibility action such as `Insert á` on that same virtual key node;
- the bounded `KeyAlternates` catalog exposes at most seven alternates, and PR #51 reserves seven stable Android resource IDs for those custom actions;
- eligible nodes expose `ACTION_LONG_CLICK` plus an accessibility hint announcing that alternate characters are available; invoking long-click announces availability but does not commit text;
- invoking a custom alternate action commits the selected complete String through the same `KeyboardView.Listener.onText` boundary used by the native touch alternate popup;
- uppercase alternates continue to derive deterministically from the rendered shifted key label;
- the transient local emoji-search keyboard never exposes editor alternate actions, because its Latin keys are query controls rather than editor text controls;
- unsupported keys expose no alternate actions rather than inventing context-dependent choices.

This design does not create an alternate text-commit authority. The accessibility delegate may select a value only from the deterministic local catalog and routes that value into the existing Keyboard listener boundary. It consumes already-rendered control labels and virtual-node state only; it does not inspect editor text, query the clipboard, learn from input, persist alternate usage, request network data, or gain GoreeCloud Identity, Mesh, Privacy Shield, Wardveil Security, or other platform authority.

The existing visual/touch long-press popup remains unchanged and continues to use its viewport-bounded geometry policy. Source/runtime tests and the governance validator cover action discovery, long-click semantics, exact alternate activation, stable resource IDs, and emoji-search isolation. Representative TalkBack, Switch Access, Voice Access, physical-device/OEM, large-text, Touch Assistance, localization/RTL, rendered GLAZE UI V1.2, release-signing, RC, and Stable acceptance remain separate gates.
