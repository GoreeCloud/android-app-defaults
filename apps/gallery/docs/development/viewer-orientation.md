# Orientation-aware full-screen viewer

Status: Development

The native full-screen Gallery viewer now routes already-authorized image media through the bounded `GalleryViewerBitmapLoader` after the viewer `ImageView` has been posted for layout. The loader prefers Android `ImageDecoder` for image media with a positive measured viewport, allowing the platform decoder to interpret encoded orientation while bounding the decoded bitmap to the viewer viewport and a 2048 px long-edge ceiling without upscaling.

`GalleryActivity` dispatches only the full-screen viewer namespace through this path. Grid thumbnails, album covers, and their existing thumbnail cache remain on the established `ContentResolver.loadThumbnail(...)` presentation path. Video, unknown media, decode failure, and pre-layout/invalid viewport states retain the bounded thumbnail fallback defined by `GalleryViewerBitmapLoader` and `GalleryViewerLoadPolicy`.

## Authority boundary

The viewer loader accepts only content URIs supplied by the current authorized Gallery presentation scope. This change grants no new MediaStore, file, network, cloud, profile, sharing, Favorites, editing, Delete/Trash, Protected Photos, or other mutation authority.

## Acceptance boundary

Source and automated validation can establish routing, bounded sizing, compile/test correctness, and authority boundaries, but they cannot prove the original physical rendering defect is resolved on representative hardware. The landscape JPEG that exposed issue #47 must still be re-tested on the representative Moto G 2026 before that issue can be closed or the viewer behavior can be treated as Stable.
