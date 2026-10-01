# Mockup-aligned primary browse controls — Development

## Purpose

This Development slice continues the October 1, 2026 GoreeCloud Gallery interface refresh without changing Android media authority.

The initial implementation exposed Group/View controls below the search surface. The later mockup-alignment refinement removed that persistent row; grouping and view-density remain available in Settings > Appearance.

## Implemented behavior

- Grouping remains Day → Month → Year → None in Settings.
- View density remains Dense → Comfortable → Spacious in Settings.
- Photos and Videos keep Search and Sort in the primary header without a permanent secondary row.

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

Pure unit coverage continues to lock grouping and density behavior. Rendered acceptance verifies the media-only Search and Sort state.

Representative-device visual review, large-text/reflow behavior, TalkBack and switch-access review, compact-width polish, and complete Gallery Glaze application acceptance remain separate gates.
