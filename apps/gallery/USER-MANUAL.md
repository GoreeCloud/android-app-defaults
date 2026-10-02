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

## Main destinations

The current native Development experience provides direct **Photos**, **Albums**, **Videos**, **Trash**, and **Settings** destinations.

- Photos uses an adaptive local timeline grid. Dense day groups retain the compact multi-column layout, while sparse one- and two-item groups use a larger three-column presentation lane closer to the current Gallery mockup.
- Videos uses a featured first card followed by two-column phone cards (three columns on wider layouts), with play affordances, duration badges, title/date metadata, and category chips that appear only when the current Android-authorized snapshot actually contains matching Screen recordings, Camera, or Favorites media.
- Albums uses Android-authorized album metadata and includes a device-local Favorites collection when Favorites exist. Use an ordinary album card's More menu to **Pin to top** or **Unpin from top**; this changes only Gallery's local Collections order and does not move or rename media. Recovery is no longer embedded in Albums.
- Trash is a dedicated bottom-navigation destination on Android 11+ and uses Android MediaStore Trash as the authoritative recovery state. Restore and permanent deletion continue to use Android-owned confirmation.
- Photos and Videos keep the main canvas focused on Search, Sort, media, and destination-specific filters. Grouping and view-density remain available in Settings > Appearance.
- Long-press a media tile to enter multi-select mode.

## Viewer

Tap a visible photo or video to open the bounded full-screen viewer.

- Use Previous and Next within the current authorized/presented collection.
- Share hands the current content URI to Android with a read-only URI grant.
- Favorite/Unfavorite changes Gallery's device-local Favorites state.
- More displays available media details.
- Delete on Android 11+ routes through Android's system-owned Trash or permanent-delete confirmation according to the current setting.
- Edit is available for authorized photos through the bounded first-party rotate/flip/crop/save-copy editor; unsupported media types remain disabled.
- Slide/Stop runs the photo-only slideshow using the locally configured **Slideshow speed** of 3, 5, or 10 seconds per photo. Five seconds is the default.

Image viewing is still a Development viewer path rather than unrestricted full-resolution zoom/pan. Authorized videos use the native bounded playback surface with Play/Pause, lifecycle-safe pause/resume, and the persisted autoplay/loop preferences; representative-device playback acceptance remains separate.

## Selection and bulk actions

Long-press a visible media tile to enter selection mode, then tap additional items to add or remove them.

Current actions include Share, Favorite/Unfavorite, Android-authorized Delete on Android 11+, Move when the current authorized selection has a safe existing-folder or new-folder destination, and More/Details when exactly one item is selected.

The `0.6.2-dev` in-place selection renderer is physically verified on the representative device: selecting and deselecting no longer causes the previous whole-screen flash.

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

Video autoplay and looping are active viewer preferences. Compatibility fields for GIF thumbnail animation and automatic empty-folder cleanup remain in the settings import/export model, but the current Settings screen does not show inactive toggles for those unfinished behaviors.

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

The current shared authority identifies **GLAZE UI V1.6 / 1.6.0** as Official Stable, with accepted release source `a7180679ea851389e0f3004515f9a25f420e716d`. Gallery's first-party native source contract pins that authority, while complete rendered, accessibility, adaptive-device, Human Visual Excellence, and production conformance remain separate acceptance gates.

## Local browsing controls

Gallery keeps browsing controls local to the device. **Sort** in the media header switches between **Newest first** and **Oldest first** and now persists across ordinary relaunches. Settings → Appearance exposes the same sort choice together with **Group media by** (Day, Month, Year, or None) and **View density**. These controls change presentation only; they do not modify media files or broaden Android media access.

## Major capability backlog

The native restoration still includes more work than the earlier rough "11 features" estimate implied. Distinct remaining capability areas include full physical Recycle Bin acceptance; full-resolution image viewing and physical orientation acceptance; native video playback plus autoplay/loop behavior; animated GIF behavior; approved photo/video editing; approved metadata editing; Move and Copy; broader selection tools where appropriate; album creation, rename, reorder and richer album actions; richer grouping/timeline modes; additional view-density/layout controls; slideshow and other established local presentation actions; broader contextual/overflow actions; broader export/share workflows where required; secure Private/Protected Photos; fuller hidden/sensitive-media policy; automatic empty-folder cleanup; and any additional established first-party Gallery capability verified by historical GoreeCloud Gallery evidence.

Separate release gates include GLAZE UI V1.6 application acceptance, accessibility/adaptive/OEM/profile testing, Privacy Shield/Wardveil/Everkeep/Identity/Mesh integration where applicable, long-lived signing, upgrade/recovery validation, production approval, and Stable qualification.

## Troubleshooting

**No media is shown after permission was granted:** use the available refresh/change-access path. If the provider read fails, Gallery states that the provider read failed rather than assuming there are no files.

**Only some media appears:** Android may have granted selected-media or media-type-limited access. Change Android media access if you want Gallery to see a different authorized subset.

**Delete is disabled:** the current Development path requires Android 11 or newer and a currently selected/presented authorized media item.

**The ordinary Gallery no longer shows an item after Trash:** open the **Trash** tab to check Android MediaStore Trash.

**Trash says media access is required:** return to ordinary GoreeCloud Gallery and grant the Android media scope you intend Gallery to use.

**Android confirmation does not open:** Gallery must not claim success. Stop that Restore/Delete test and report the exact feedback.
