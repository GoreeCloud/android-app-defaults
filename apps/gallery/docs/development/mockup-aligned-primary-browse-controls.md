# Mockup-aligned primary browse controls — Development

## Purpose

This Development slice continues the October 1, 2026 GoreeCloud Gallery interface refresh without changing Android media authority.

Photos and Videos expose their presentation controls through the header view/sort action so the primary media canvas follows the supplied mockups without a persistent Group/View row.

## Implemented behavior

- The header view/sort action opens a compact presentation menu.
- Photos and Videos expose **Sort**, **Group**, and **View** from that menu; Albums exposes **Sort**.
- Group retains Day / Month / Year / None and View retains Dense / Comfortable / Spacious through the existing Glaze dialogs.
- The header action retains the 48dp-or-larger target floor and disappears with other media-only header actions when the destination has no readable media.
- Changing a presentation preference re-renders only the already-authorized in-memory media presentation.

## Authority boundary

The controls are presentation-only. They do not:

- issue a new MediaStore listing query;
- broaden Android photo/video permission;
- mutate photo or video files;
- create filesystem authority;
- contact GoreeCloud Photos or another network service; or
- alter Trash, Restore, Delete, Move, or Share authority.

Android MediaStore and the existing Gallery permission/mutation adapters remain authoritative.

## Validation boundary

Rendered acceptance verifies that media-only Search and view/sort header actions stay hidden when no readable media is available. Existing preference-policy tests cover valid Sort, Group, and View values; representative-device interaction with the header menu remains a separate acceptance gate.

Representative-device visual review, large-text/reflow behavior, TalkBack and switch-access review, compact-width polish, and complete Gallery Glaze application acceptance remain separate gates.
