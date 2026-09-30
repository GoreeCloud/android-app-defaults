# Current Authorized View Summary and Reset — Development

Gallery now makes the state of its local authorized presentation explicit and provides one bounded reset action.

## Behavior

- The summary reports how many items are currently presented out of the already-authorized snapshot.
- It names the active album scope, media-type filter, and sort order.
- Reset returns presentation to All albums, All media, Newest first.
- Summary and reset are derived entirely from the existing authorized `MediaItem` snapshot.
- Reset does not perform another MediaStore query, widen Android media permission, or change provider authority.
- Preview Previous/Next continues to use the resulting presented list.

## Boundary

Development only. No album mutation, full-resolution viewer/editing, playback, cloud albums, new provider authority, or Stable qualification is added.
