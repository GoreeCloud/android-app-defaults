# Mockup-aligned primary browse options — Development

## Purpose

This Development slice continues the October 1, 2026 GoreeCloud Gallery interface refresh without changing Android media authority.

Photos and Videos now use the mockup's compact Search + overflow header so presentation controls no longer occupy a permanent row beneath the title.

## Implemented behavior

- **More Gallery options** opens a local overflow menu.
- **Sort** offers Newest first and Oldest first.
- On Photos and Videos, **Group** offers Day, Month, Year, and None.
- On Photos and Videos, **View** offers Dense, Comfortable, and Spacious.
- These options persist through the same app-private preferences already used by Settings.
- The header actions use 48dp-or-larger touch targets and stay hidden when unreadable media or selection state makes them irrelevant.

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

Pure policy coverage retains grouping, density, and sort behavior. Rendered acceptance verifies that media-only Search and overflow actions stay hidden when no readable media is available.

Representative-device visual review, large-text/reflow behavior, TalkBack and switch-access review, compact-width polish, and complete Gallery Glaze application acceptance remain separate gates.
