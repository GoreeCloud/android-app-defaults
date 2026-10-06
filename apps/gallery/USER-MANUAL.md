# GoreeCloud Gallery User Manual

## Status

This manual describes the **current first-party native Development experience**. It includes the five-destination Photos / Albums / Videos / Trash / Settings interface, Android-authorized local browsing and mutation paths, bounded native viewer playback, and the current mockup-aligned presentation candidate. It does not describe a Stable or production-approved release.

The initial `0.6.0-dev` destructive-operation build is superseded. `0.6.1-dev` corrected the MediaStore item URI path, and representative-device testing subsequently verified single-item Trash plus tested 26-item and 10-item multi-select Trash operations. `0.6.2-dev` also physically corrected the prior select/deselect screen flash.

Use **disposable copied photos and videos** when testing Restore, permanent deletion, or other unfinished destructive workflows. Do not use irreplaceable personal media as test input.

## Opening the local library

1. Launch **GoreeCloud Gallery**.
2. If media access has not been granted, choose the media-access action and use Android's permission surface to select the access scope you want to provide.
3. Gallery reads only the local MediaStore view allowed by the current Android permission scope.
4. If Android denies the read or the provider is unavailable, Gallery reports that failure instead of presenting it as an empty library.

On supported Android versions, the app may operate with selected-media access rather than broad image/video access.

While Gallery remains in the foreground, it observes Android MediaStore and automatically refreshes the current authorized snapshot after local media changes. Newly created photos/videos should therefore appear without closing and reopening the app. Gallery still performs a fresh authoritative read when returning to the Activity after it has been backgrounded.

## Main destinations

The current native Development experience provides direct **Photos**, **Albums**, **Videos**, **Trash**, and **Settings** destinations.

The bottom navigation defaults to **Icons only**. In this mode, each glyph is centered in an equal-width destination slot and the selected destination uses a compact inset Glaze pill. Long-press/hover tooltips expose destination names where Android supports them, and accessibility services retain destination and selected-state semantics in every visual mode. Use **Settings > Appearance > Bottom navigation** to switch to **Text only** or **Icons & text**. Tapping the already-selected destination returns that view to the top.

- Photos uses an adaptive local timeline grid. Dense day groups retain the compact multi-column layout, while sparse one- and two-item groups use a larger three-column presentation lane closer to the current Gallery mockup.
- Videos uses a featured first card followed by two-column phone cards (three columns on wider layouts), with play affordances, duration badges, title/date metadata, and category chips that appear only when the current Android-authorized snapshot actually contains matching Screen recordings, Camera, or Favorites media.
- Albums uses Android-authorized album metadata and includes a device-local Favorites collection when Favorites exist. Use an ordinary album card's More menu to **Pin to top / Unpin from top** and, when available, **Move earlier / Move later**. Pinned albums always remain in the top group; manual moves reorder only within the current pinned or unpinned group. These ordering actions change only Gallery's local Collections presentation. The active Development rename candidate additionally exposes **Rename** only when Gallery has full local image-and-video access and every authorized item in the album agrees on one nested MediaStore folder and one storage volume. Rename preserves the provider-owned parent folder, rejects root-level/ambiguous albums and sibling-name collisions, and routes the exact bounded item URIs through Android's write-confirmation surface before changing `RELATIVE_PATH`. Settings > Appearance exposes **Reset album order** after a manual order exists; resetting keeps Pin/Unpin choices. Recovery is no longer embedded in Albums.
- Trash is a dedicated bottom-navigation destination on Android 11+ and uses Android MediaStore Trash as the authoritative recovery state. Restore and permanent deletion continue to use Android-owned confirmation.
- Photos and Videos keep the main canvas focused on Search, Sort, media, and destination-specific filters. Grouping and view-density remain available in Settings > Appearance.
- Long-press a media tile to enter multi-select mode.

## Viewer

Tap a visible photo or video to open the bounded full-screen viewer.

- Use Previous and Next within the current authorized/presented collection. Horizontal swipe navigation remains available at baseline photo scale.
- For photos, use the viewer's view-options control for **Fit entire photo**, **Fill viewer**, or **Zoom 2×**. You can also pinch from 1× up to 4×; while zoomed, drag the photo to pan. Item-navigation swipes are disabled while zoomed so a pan does not change photos. Choose **Reset zoom** or navigate to another item to return to baseline zoom.
- Share hands the current content URI to Android with a read-only URI grant.
- Favorite/Unfavorite changes Gallery's device-local Favorites state.
- More displays available media details.
- Delete on Android 11+ routes through Android's system-owned Trash or permanent-delete confirmation according to the current setting.
- Edit is available for authorized photos through the bounded first-party rotate/flip/crop/save-copy editor; unsupported media types remain disabled.
- Slide/Stop runs the photo-only slideshow using the locally configured **Slideshow speed** of 3, 5, or 10 seconds per photo. Five seconds is the default.

Image viewing now includes bounded session-local zoom/pan over the existing orientation-aware viewer bitmap, but it is still a Development viewer path rather than unrestricted original-resolution zoom: the underlying decode remains bounded to the current viewport with a 2048px long-edge ceiling. Authorized videos use the native bounded playback surface with Play/Pause, lifecycle-safe pause/resume, and the persisted autoplay/loop preferences; representative-device playback and zoom/pan acceptance remain separate.

## Selection and bulk actions

Long-press a visible media tile to enter selection mode, then tap additional items to add or remove them.

Current actions include Share, Favorite/Unfavorite, Android-authorized Delete on Android 11+, Move when the current authorized selection has a safe existing-folder or new-folder destination, **Copy** to another authorized folder or **Create & copy** destination while preserving originals, and More/Details when exactly one item is selected.

The `0.6.2-dev` in-place selection renderer is physically verified on the representative device: selecting and deselecting no longer causes the previous whole-screen flash.

## Copy media

On Android 11 or newer, select one or more authorized photos/videos and choose **Copy**.

- Choose an existing eligible local folder, or choose **New folder** to use **Create & copy**.
- Existing source folders are not offered as Copy destinations in this Development path.
- One Copy operation must stay on one concrete Android media volume. Mixed source folders are allowed only when they belong to that same volume; selections spanning internal/removable media volumes fail closed.
- Image-only new folders use Pictures, video-only folders use Movies, and mixed selections use DCIM.
- Gallery creates new MediaStore items on each source item's concrete provider-owned MediaStore volume and leaves the originals unchanged. The Development path uses Android 11's related-copy insertion contract rather than enabling a separate Android 10 fallback.
- New filenames use `(copy)`, `(copy 2)`, and later bounded suffixes when needed to avoid names already visible in the destination; long source names are shortened as needed to remain within the supported MediaStore display-name bound.
- A single operation is bounded to 100 items. Partial failures are reported; failed incomplete destination rows are cleaned up best-effort.

Use disposable copied media for representative-device testing while Copy remains Development and acceptance-gated.

## Delete and Android Trash

With **Settings > Deletion & recovery > Move deleted items to Trash** enabled, Delete requests Android MediaStore Trash. Android owns the confirmation surface and final mutation. With that setting disabled, Gallery requests Android-confirmed permanent deletion.

Representative-device testing has verified the current corrected Trash path for a single item and tested 26-item and 10-item multi-select operations. Broader cancellation, permanent-delete, permission-change, OEM/profile, and post-mutation edge-case acceptance remains Development work.

Android 10 remains fail-closed for this path; no legacy direct-delete workaround is enabled.

A single mutation is bounded to at most 100 unique Android MediaStore image/video item URIs. Gallery rejects blank, malformed, file, network, non-MediaStore, generic MediaStore Files, collection-only, and nonnumeric-item targets at the mutation adapter boundary.

## Trash — Development candidate

The current candidate exposes first-party **Trash** as its own bottom-navigation destination while keeping Android MediaStore as the authoritative Trash state.

### Opening Trash

1. Launch **GoreeCloud Gallery**.
2. Choose **Trash** from the bottom navigation.

The temporary second launcher icon used by the first `0.7.0-dev` Restore/Purge test slice has been removed. `RecycleBinActivity` is now internal to the application and normal Gallery is the sole launcher entry.

### What Trash shows

- Android 11+ MediaStore image/video items whose authoritative Trash state is set.
- A bounded local thumbnail grid.
- An explicit notice that **Android controls Trash retention and expiration**. Gallery does not promise indefinite retention.
- Empty, unavailable, media-access-required, and provider-failure states.

Ordinary Photos/Albums/Videos media queries continue to exclude trashed items by default.

### Trash viewer

When no selection is active, tap a visible trashed-media tile to open the Trash viewer.

- Use **Previous** and **Next** across the currently loaded trashed-media collection.
- **Restore** asks Android to restore the current item from Trash.
- **Delete permanently** asks Android to permanently delete the current trashed item.
- **More** shows the available media details and explicitly identifies the item as being in Android Recycle Bin state.

The Trash viewer remains a bounded recovery surface. It does not broaden ordinary library permission or bypass Android-owned restore/permanent-delete confirmation.

### Selecting trashed items

Long-press a visible Trash tile to enter selection mode, then tap additional items to toggle them. Selection updates resident tiles in place. The current action surface provides:

- **Select all** — select the currently loaded trashed items.
- **Restore** — ask Android to restore the selected items from Trash.
- **Delete permanently** — ask Android to permanently delete the selected trashed items.
- **Cancel** — clear the current selection.

### Restore

1. Put a **disposable copied** photo/video into Trash using normal Gallery Delete with Recycle Bin enabled.
2. Open **Trash** from the bottom navigation.
3. Confirm that the trashed item appears.
4. Either open the item and choose **Restore**, or long-press/select it and choose **Restore** from the selection action surface.
5. Android should display its system-owned restore confirmation.
6. Approve only the disposable test item.
7. Gallery should refresh the Recycle Bin after Android reports success.
8. Return to the ordinary Gallery and verify the restored item returns to the authorized library.

Restore deliberately preserves Gallery Favorite URI metadata so a restored favorite can remain a favorite when Android retains the same item identity.

### Permanent purge

1. Open or select only disposable trashed media.
2. Choose **Delete permanently**.
3. Android should display its system-owned permanent-delete confirmation.
4. Approve only when you intend to permanently remove the test media.
5. Gallery should refresh the Recycle Bin and the purged item should no longer appear.

Confirmed purge removes stale Gallery Favorite URI references for those items.

### Cancel behavior

Canceling Android's Restore or permanent-delete confirmation must not be treated as success. The media should remain in the Recycle Bin unless Android or another application changed it independently. If cancellation occurs from the viewer, the viewer remains available for the current item.

### Trash acceptance boundary

The Trash/Recycle Bin implementation originated in rendered `0.7.1-dev` acceptance evidence and remains a Development capability in the current `0.8.6-dev` line. Earlier representative-device testing verified Trash-to-bin visibility, populated Recycle Bin browsing, stable in-place selection, Android-owned Restore and permanent-delete confirmation surfaces, denial/cancellation for both recovery mutations, successful permanent purge of 28 selected photos, the post-purge empty-bin state, and mixed photo/video Trash-to-Recycle-Bin plus Restore through the previous Albums recovery entry. The new dedicated five-tab Trash navigation introduced on October 1, 2026 still requires fresh representative-device and accessibility validation. In the mixed-media test, Gallery recognized the test video in Videos, Android separately confirmed moving the video and a photo to Trash, both appeared in the Recycle Bin, Android presented a `move 2 items out of trash` confirmation for the mixed selection, Gallery reported `Restored 2 items`, and the video returned to Videos while the photo returned to the ordinary library. In the 28-photo purge test, Android presented its system-owned confirmation, Gallery reported `Deleted 28 items permanently`, refreshed the Recycle Bin to 0 items, and rendered the intended `Recycle Bin is empty` state. Remaining required device testing includes mixed photo/video permanent-purge behavior, partial-media permission behavior, permission revocation, provider failure, restart/process recreation, OEM/profile behavior, and retention/expiry refresh. This evidence does not establish Stable or production acceptance.

## Settings

Current active settings include local thumbnail loading priority, included/excluded folder presentation, hidden-item visibility within Android's authorized snapshot, rounded-square thumbnails, slideshow speed, Favorites/settings import/export (including app-local album pins and slideshow pace), cache clearing, and the Trash versus permanent-delete choice on supported Android versions.

Video autoplay and looping are active viewer preferences. **Animate GIFs in thumbnails** is also active under Playback; it is off by default and, when enabled, animates authorized GIF cards/covers while they are visible. Automatic empty-folder cleanup remains a compatibility-only settings field and is not shown as an active control.

Protected Photos/password protection is not simulated with insecure app-local credentials; it remains unavailable until supported authentication, protected storage, Privacy Shield, GoreeCloud Identity where applicable, and Wardveil requirements are implemented.

## Privacy and security

- Local browsing and the Recycle Bin do not require cloud retrieval.
- Gallery does not receive authority to read media that Android has not authorized.
- Android MediaStore remains the authoritative Trash state; Gallery does not maintain a second deleted-item database.
- Trash, Restore, and permanent-delete requests are restricted to media-specific MediaStore item URIs and Android owns the final confirmation surface.
- Selection itself never grants filesystem or media-write authority.
- Recycle Bin integration does not request `MANAGE_MEDIA`, `MANAGE_EXTERNAL_STORAGE`, network media authority, or cross-profile access.
- Optional GoreeCloud Photos integration remains a future user-controlled adapter milestone, not a dependency of the current local library.

## Current design-system authority

The current shared authority is **Glaze V1.7 / 1.7.0** (Anchor / Stable compatibility), established by release integration `1a5756daed2294155be2e9972b24f580f6222b7b` from qualification anchor `7c4ded83d7a8725165bb6a55dfb175667cc9589e`. V1.7.0 intentionally inherits the accepted V1.6.0 runtime. Gallery still implements its bounded V1.6 native semantic mapping, so fresh V1.7 contract adoption plus rendered, accessibility, adaptive-device, Human Visual Excellence, and production acceptance remain separate gates.

## Local browsing controls

Gallery keeps browsing controls local to the device. **Sort** in the media header switches between **Newest first** and **Oldest first** and now persists across ordinary relaunches. Settings → Appearance exposes the same sort choice together with **Group media by** (Day, Month, Year, or None) and **View density**. These controls change presentation only; they do not modify media files or broaden Android media access.

## Major capability backlog

The native restoration still includes more work than the earlier rough "11 features" estimate implied. Distinct remaining capability areas include full physical Recycle Bin acceptance; full-resolution image viewing and physical orientation acceptance; native video playback plus autoplay/loop behavior; representative-device/accessibility/performance acceptance for the implemented animated GIF thumbnail path; approved photo/video editing; approved metadata editing; representative-device acceptance and refinement of the implemented Move and preserve-original Copy paths; broader selection tools where appropriate; album creation beyond the bounded Move/Copy-new-folder paths, representative-device acceptance and refinement of the active bounded album-rename candidate, broader provider-backed organization, and richer album actions; richer grouping/timeline modes; additional view-density/layout controls; slideshow and other established local presentation actions; broader contextual/overflow actions; broader export/share workflows where required; secure Private/Protected Photos; fuller hidden/sensitive-media policy; automatic empty-folder cleanup; and any additional established first-party Gallery capability verified by historical GoreeCloud Gallery evidence.

Separate release gates include GLAZE UI V1.6 application acceptance, accessibility/adaptive/OEM/profile testing, Privacy Shield/Wardveil/Everkeep/Identity/Mesh integration where applicable, long-lived signing, upgrade/recovery validation, production approval, and Stable qualification.

## Troubleshooting

**No media is shown after permission was granted:** use the available refresh/change-access path. If the provider read fails, Gallery states that the provider read failed rather than assuming there are no files.

**Only some media appears:** Android may have granted selected-media or media-type-limited access. Change Android media access if you want Gallery to see a different authorized subset.

**Delete is disabled:** the current Development path requires Android 11 or newer and a currently selected/presented authorized media item.

**The ordinary Gallery no longer shows an item after Trash:** open the **Trash** tab to check Android MediaStore Trash.

**Trash says media access is required:** return to ordinary GoreeCloud Gallery and grant the Android media scope you intend Gallery to use.

**Android confirmation does not open:** Gallery must not claim success. Stop that Restore/Delete test and report the exact feedback.
