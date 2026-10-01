# Mockup-aligned primary browse options — Development

## Purpose

This Development slice continues the October 1, 2026 GoreeCloud Gallery interface refresh without changing Android media authority.

Photos and Videos now use the mockup's compact Search + overflow header so presentation controls no longer occupy a permanent row beneath the title.

## Implemented behavior

- **Group** cycles through Day → Month → Year → None.
- **View** cycles through Dense → Comfortable → Spacious.
- Both controls persist through the same app-private preferences already used by Settings.
- Both controls use 48dp-or-larger touch targets.
- The controls are hidden while search is open and while multi-select is active so contextual work keeps priority.
- Changing either control re-renders only the already-authorized in-memory media presentation.

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

Pure unit coverage locks deterministic control cycling and wraparound. Rendered acceptance verifies that media-only Search, Sort, Group, and View controls stay hidden when no readable media is available; interactive sizing and state behavior are covered when those controls are rendered over an authorized library.

Representative-device visual review, large-text/reflow behavior, TalkBack and switch-access review, compact-width polish, and complete Gallery Glaze application acceptance remain separate gates.
