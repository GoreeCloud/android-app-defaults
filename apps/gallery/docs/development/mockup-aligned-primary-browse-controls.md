# Mockup-aligned primary browse controls — Development

## Purpose

This Development slice continues the October 1, 2026 GoreeCloud Gallery interface refresh without changing Android media authority.

The supplied Photos and Videos mockups keep persistent browsing chrome intentionally minimal. Grouping and view-density choices now remain under Settings → Appearance instead of appearing as always-visible primary controls.

## Implemented behavior

- Search and Sort remain in the primary header when readable media exists.
- Videos retains destination-specific filter chips.
- **Group media by** remains available in Settings with Day → Month → Year → None choices.
- **View density** remains available in Settings with Dense → Comfortable → Spacious choices.
- Grouping and density keep the same app-private persisted preferences and affect only the already-authorized in-memory media presentation.

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

Unit coverage continues to lock grouping, density, sort, and filter policy. Rendered acceptance covers primary header/navigation visibility and target sizing where those controls are rendered.

Representative-device visual review, large-text/reflow behavior, TalkBack and switch-access review, compact-width polish, and complete Gallery Glaze application acceptance remain separate gates.
