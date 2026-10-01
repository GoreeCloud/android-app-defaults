# Authorized Local Media Search — Development

This slice restores a bounded first-party local search foundation for GoreeCloud Gallery without widening media authority.

Search receives an already-authorized `List<MediaItem>` and matches normalized query tokens only against metadata already present in that snapshot: display name, authoritative album name when present, MIME type, and image/video kind. All query tokens must match. Existing snapshot ordering is preserved and results are capped at 100.

A blank query returns the bounded current authorized snapshot. Search never performs its own MediaStore query, requests broader Android media permission, inspects media bytes, sends a network request, or contacts GoreeCloud Photos or another cloud service.

The current native Android Pictures/Albums surface still needs to render and compose this search with the existing album/type/sort presentation controls. This is Development core behavior only; rendered current accepted Gallery Glaze UI contract search acceptance, physical-device review, full historical Gallery feature restoration, release signing, production acceptance, and Stable qualification remain separate gates.
