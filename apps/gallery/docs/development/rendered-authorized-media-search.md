# Rendered authorized local media search — Development

The native current accepted Gallery Glaze UI contract Gallery search field now consumes the same bounded `AuthorizedMediaSearch` contract used by the first-party core instead of maintaining a separate activity-local predicate.

Behavior and authority boundary:

- Search operates only on the current Android-authorized `MediaItem` snapshot already held by the native Gallery activity.
- Photos, Videos, Favorites, and opened-album views search their current authorized local subset before applying the existing local presentation sort.
- The Albums surface derives matching collections from locally searched authorized items; the local Favorites collection remains discoverable by its explicit label.
- Matching is tokenized and case-insensitive across filename, album name, MIME type, and image/video kind.
- Results remain bounded by the core search contract to at most 100 items.
- Typing into search does not issue another MediaStore list query, request broader Android permission, contact a network/cloud service, or create a search-history persistence path.
- Preview navigation remains bounded to the rendered authorized search result set.

Status: **Development**. This is a search-contract reconciliation within the ongoing Samsung Gallery-inspired native restoration. Broader Gallery capability completion, representative-device and historical-screenshot review, rendered/accessibility acceptance, production Wardveil Security/Privacy Shield/Everkeep acceptance, signing, release, and Stable qualification remain separate gates.
