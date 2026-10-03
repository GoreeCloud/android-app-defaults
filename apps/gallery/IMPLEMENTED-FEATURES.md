# GoreeCloud Gallery — Implemented Features

## October 3, 2026 — unified bottom-navigation glyph family

The five primary Gallery destinations now share one custom 24×24 outline glyph language rather than mixing filled and outlined symbols with different optical weights. Gallery continues to render them at the shared 22dp optical size, preserving the centered icons-only geometry and equal destination slots from the preceding navigation pass.

Each destination also receives bounded Android ripple feedback without replacing the selected Glaze background, tooltip label, content description, or selected-state semantics. The same assets and shared presentation path cover the main Gallery surface and Android-managed Trash.

Rendered Android acceptance verifies shared glyph bounds and ripple presence, including the Trash navigation path. The change is presentation-only and does not expand permissions, media scope, mutation authority, filesystem access, account/network access, or release qualification.

## October 3, 2026 — optically centered bottom navigation

Icons-only bottom navigation now centers each 22dp glyph directly inside its equal-width destination slot instead of using a top compound drawable with an empty text line. This removes the vertical lift visible on-device and eliminates the accumulated horizontal skew caused by per-item start margins.

Selected destinations now use a mode-aware inset Glaze control surface: icons-only keeps a compact focused pill while text-only and icons-with-text retain enough room for labels. The same shared presentation code is used by the main Gallery Activity, Trash, and the physical-device refinement layer, preventing the three paths from drifting apart.

Every icon-only destination keeps its accessibility content description and selected-state semantics and now also exposes a tooltip label. The setup wizard was shortened and slightly densified so the navigation/default explanation is easier to scan without adding new setup friction.

Pure policy tests and Android rendered acceptance tests lock centered icon placement, equal slot widths, even distribution, compact selected material, and tooltip availability. These are presentation changes only and add no media, storage, network, account, cloud, or mutation authority.

## October 2, 2026 — foreground live library refresh and navigation presentation

The Gallery Activity now observes Android MediaStore while it is visible and debounces provider change notifications before silently re-reading the current authorized media snapshot. A live refresh does not replace the grid with the full loading panel, preserves still-valid selection, and keeps the current scroll offset when the user is already below the top. Refresh is deferred while the full-screen viewer or an Android-confirmed Gallery media mutation is active, then reconciled after that conflicting surface clears.

The primary bottom navigation now has a persisted presentation preference with **Icons only** as the default plus **Text only** and **Icons & text** alternatives under Settings > Appearance. Visual labels are independent from accessibility identity, so icons-only mode retains destination names and selected-state semantics for assistive technology. The preference is included in Gallery's additive settings portability envelope.

All Android PopupMenu-based overflow/context surfaces now inherit a rounded Glaze popup background in both light and dark themes. Compact video cards also keep long titles to a single ellipsized line, and re-selecting the current bottom destination scrolls its view to the top.

These changes alter local observation and presentation only; they do not expand MediaStore permission scope, mutation authority, filesystem access, account/network authority, or release status.

## October 2, 2026 — slideshow pause and resume

Protected PR #185 integrated explicit **Pause / Resume** controls into the existing local photo-only slideshow. Pause cancels the pending advance without discarding the current viewer position; Resume continues from that photo using the already-persisted 3/5/10-second interval. Manual Previous/Next, swipe navigation, scale/zoom interaction, Activity pause/destroy, viewer replacement, and viewer close continue to terminate slideshow state so playback never progresses in the background.

The accepted candidate `69a65412f24271ecb627b2eb5aaf96b55de9e570` passed Gallery source/unit/lint/build and Android 16 runtime inside the complete protected matrix before squash merge as monorepo main `7902e7bd58a510591e6c8f8778a3872df4c77642`. No MediaStore, storage, mutation, account, network, or video authority was expanded. Representative-device presentation, accessibility, orientation/form-factor, performance/power, signing/recovery, and release qualification remain open.

## October 1, 2026 — bounded viewer zoom and pan

Authorized photos in the full-screen viewer can now use bounded **1×–4×** session-local zoom over the existing decoded viewer bitmap. Pinch gestures adjust zoom, single-finger dragging pans only while zoomed, and horizontal item-navigation swipes are suppressed during zoom/pan so panning cannot accidentally change media.

The compact viewer scale control now opens explicit **Fit entire photo**, **Fill viewer**, **Zoom 2×**, and conditional **Reset zoom** actions. This provides a non-pinch path for the common 2× state and reset while retaining state-specific accessibility announcements. Zoom resets on item changes and does not apply to native video playback.

Pure `GalleryViewerZoomPolicyTest` coverage locks the 1×–4× scale bound, baseline zero translation, viewport-bounded panning, non-finite gesture handling, zoom-state detection, and the accessible 2× preset. This remains presentation over the existing orientation-aware viewer bitmap; the decoder's current viewport/2048px long-edge bound is unchanged, so this is not an unrestricted original-resolution zoom claim.

## October 1, 2026 — manual app-local Album ordering

Ordinary Albums collections can now be moved **earlier** or **later** from each card's More menu. The preference stores only album identifiers already used by Gallery's authorized collection model and changes Gallery presentation only; it does not rename, move, copy, create, or reorder MediaStore folders.

Pin/Unpin remains the higher-order grouping rule: pinned albums stay in the top group, and manual moves operate only within the album's current pinned or unpinned group. Newly discovered albums fall back to the normal date-sort order, stale stored identifiers do not fabricate collections, and a Settings action can reset manual ordering while preserving Pin/Unpin state.

The ordered identifier list participates in the additive non-secret settings portability envelope. Pure policy coverage locks pinned-group boundaries, stale-ID handling, default fallback for newly visible albums, move availability, and deterministic earlier/later swaps.

## October 1, 2026 — opt-in animated GIF thumbnails

The current Gallery Development line can animate authorized `image/gif` grid and album-cover thumbnails when **Settings > Playback > Animate GIFs in thumbnails** is enabled. The preference remains off by default and changes presentation only.

Animated decoding is isolated behind Android 9/API 28 framework support, preserves source aspect ratio, never upscales, and caps the requested decode edge at 512 px. Decode/security/provider failures fall back to the established static MediaStore thumbnail path. Animated drawables start only after binding to the current generation/tag and stop when their ImageView detaches.

Pure policy coverage locks MIME routing, aspect-ratio sizing, no-upscale behavior, and the 512 px bound. Rendered Settings acceptance now requires the GIF toggle to remain visible. Representative-device/OEM/profile behavior, accessibility, large/complex GIF resource use, performance/power, and release acceptance remain open.

## October 2, 2026 — preserve-original local media Copy

The current Gallery Development line now exposes **Copy** from bounded multi-select. Existing destinations come only from authoritative album/path metadata already present in the current Android-authorized snapshot; every selected source folder is excluded from the existing-destination list to avoid accidental same-folder duplication. **Create & copy** can create a bounded Pictures, Movies, or DCIM destination based on the selected media types and can combine sources from multiple current folders when every selected item belongs to the same concrete MediaStore volume.

On Android 11+, Copy reads only exact canonical selected MediaStore image/video item URIs, requires one concrete provider-owned `VOLUME_NAME` across the whole selection before any output is created, rejects synthetic aggregate-volume insertion, and supplies `QUERY_ARG_RELATED_URI` when inserting each new destination row. Each output starts with `IS_PENDING=1`, receives the selected source bytes, and is published only after the write succeeds. Failed partial outputs are deleted best-effort before the operation continues. Originals are never moved or overwritten. Known destination filenames are collision-avoided with deterministic bounded `(copy)`, `(copy 2)`, and later suffixes that stay within the supported display-name limit. A single Copy operation is bounded to 100 unique source items.

This is Development implementation evidence. Representative-device/OEM/profile Copy behavior, large files, provider failures, storage exhaustion, cancellation/lifecycle behavior, metadata fidelity beyond capture time/MIME/display-name handling, accessibility, performance/power, and release acceptance remain open.

## October 1, 2026 — app-local album pins and configurable slideshow pace

The current Gallery Development line can persistently **Pin to top / Unpin from top** ordinary Albums collections. Pin state stores only authoritative album identifiers already visible in the current authorized snapshot, changes only Gallery's local Collections ordering, ignores stale identifiers when rendering, and does not rename, move, copy, mutate, or create media.

The bounded photo slideshow now uses a persisted **Every 3 seconds / Every 5 seconds / Every 10 seconds** presentation preference, with five seconds as the migration-safe default. The setting participates in the existing additive non-secret settings export/import envelope. Slideshow progression remains bounded to later photos in the current authorized viewer collection and retains the existing lifecycle/manual-navigation cancellation behavior.

Pure unit coverage locks pin ordering/toggling and slideshow interval parsing/defaults. Rendered Settings acceptance requires the active Slideshow speed control to remain present. Representative-device visual/accessibility/form-factor acceptance remains open.

### Local browsing presentation candidate

The current Gallery candidate exposes three persisted media-grid densities: **Dense**, **Comfortable**, and **Spacious**. Spacious reduces the adaptive baseline by two columns while preserving a minimum two-column grid, so the setting remains usable on narrow screens. The setting stays presentation-only and participates in the existing non-secret settings portability path.

## September 29, 2026 — persistent local sort-order candidate

The active Gallery browsing candidate now persists **Newest first / Oldest first** as an app-private presentation preference instead of resetting sort order on every launch. The existing header sort action writes the preference, Settings exposes the same choice explicitly, and Gallery settings export/import carries the portable `sortPreference` value. The preference maps only to the existing core `MediaSortOrder` and composes with Day / Month / Year / None grouping over the same Android-authorized media snapshot.

Newest remains the migration-safe default. This adds no MediaStore mutation, permission, account, network, synchronization, or storage authority. Pure policy coverage locks defaulting, stored-value round trips, and mapping to the existing core sort contract. Representative-device/adaptive/accessibility acceptance remains open.

## September 29, 2026 — local timeline grouping candidate

The current Gallery candidate adds a persisted local **Group media by** presentation preference with **Day**, **Month**, **Year**, and **None** modes. Day remains the migration-safe default so existing installs keep Today/Yesterday/calendar-day sections. Month and Year group the same already-authorized media into broader calendar sections, while None shows the same ordered collection as one continuous grid.

The preference participates in Gallery settings export/import without changing the export schema version because the existing settings envelope already tolerates additive fields. This continuation also corrects the existing density portability path so exported `viewDensity` is validated and restored on import rather than silently ignored. No MediaStore authority, sort order, mutation, permission, storage, account, or network boundary changes. Pure unit coverage locks grouping defaults and portable identities; representative-device/accessibility acceptance remains open.


## September 29, 2026 — bounded orientation-aware viewer decode record reconciliation

Live PR #100 source already routes authorized full-screen image viewing through `GalleryViewerBitmapLoader`. For image MIME types and a measured viewer viewport, that loader prefers `GalleryViewerImageDecoder`, which uses Android `ImageDecoder` for encoded-orientation handling and bounds decoded output to the current viewport with a 2048px long-edge ceiling. Thumbnail loading remains a fail-safe fallback for decode failure, non-image media posters, or pre-layout states.

Pure `GalleryViewerDecodePolicyTest` coverage verifies image routing, portrait/landscape viewport sizing, no upscaling, the 2048px bound, and fail-closed invalid dimensions. This is useful bounded full-screen image viewing, not an unrestricted original-resolution zoom claim. Representative-device quality/memory/accessibility acceptance and any separately justified full-resolution zoom/pan behavior remain open.


## September 28, 2026 — bounded local photo slideshow

The authorized full-screen Gallery viewer now provides a bounded **Slide / Stop** photo slideshow. It advances every five seconds to the next later image in the current authorized viewer collection, skips non-photo entries, stops at the end, and cancels on Activity pause/destroy, viewer replacement/close, manual Previous/Next, or swipe navigation. It adds no new MediaStore, storage, mutation, account, or network authority.

Pure policy coverage locks forward-only photo progression and fail-closed invalid/end states. Exact candidate head `c30ba640e86f9e4b28f78f9ad7e8b80644eec00f` passed Platform Contract, Native Core, Native Android Adapter, Native Android App, Native Android Rendered Acceptance, and GoreeCloud Gallery acceptance validation. Representative physical-device presentation, large-text top-bar fit, TalkBack/Switch Access, orientation/form-factor behavior, video slideshow behavior, and production/Stable acceptance remain open.


## September 28, 2026 — viewer Fit / Fill presentation control

The authorized full-screen media viewer now exposes an explicit **Fit / Fill** presentation control. **Fit** keeps the complete media visible with `FIT_CENTER`; **Fill** uses `CENTER_CROP` to occupy the viewer canvas. The control carries state-specific accessibility wording and announces mode changes. This is session-local presentation only and does not change MediaStore scope, mutation authority, metadata, favorites, editing, or playback permissions.

Focused policy coverage locks deterministic Fit ↔ Fill toggling. Representative-device image/video behavior, large-text/top-bar fit, TalkBack/Switch Access, orientation changes, and broader viewer acceptance remain open.


**Record type:** Repository implemented-feature inventory  
**Repository:** `GoreeCloud/android-app-defaults` (`apps/gallery/`)  
**Lifecycle:** Development / non-Stable  
**Authority:** Current `main` source and accepted repository evidence  
**Governing standard:** Standard — Repository Feature Tracking and Changelog Governance v1.0, effective September 22, 2026.

## Interpretation

This record describes capabilities present in the current first-party GoreeCloud Gallery Development line. It does **not** establish Production Acceptance, Release Candidate, or Stable qualification. Physical-device, accessibility, performance, recovery, signing, deployment, and complete Integral Platform System acceptance remain separate gates.

`FEATURES.md` remains the product-facing feature description. This file is the lifecycle authority for what is implemented.

## Implemented Development capabilities

### Local media access and browsing

- Android-authorized local image/video access with fail-closed permission handling, including selected-media/partial-access behavior.
- Bounded MediaStore image/video reads through the compiled Android adapter.
- Validated media-item and MediaStore-row domain models.
- Local thumbnail loading with bounded in-memory caching and no cloud dependency.
- Direct Photos, Albums, Videos, Trash, and Settings navigation.
- Adaptive Photos timeline grids grouped by Today, Yesterday, and calendar date, with sparse dense-mode groups using a larger presentation lane.
- Videos browsing uses a featured first card followed by responsive cards with local play/duration affordances, title/date metadata, newest-order **Recently added** wording, and icon-bearing category filters derived only from the current authorized snapshot and Gallery-local Favorites.
- Mockup-aligned overflow controls on video and album cards with 48dp targets and bounded non-destructive contextual actions; ordinary album cards include app-local Pin/Unpin-to-top grouping plus Move earlier / Move later ordering.
- Newest/Oldest ordering over the current authorized snapshot.
- Bounded device-local token search over the already-authorized visible media snapshot, matching display name, authoritative album name, MIME type, and image/video kind without a MediaStore re-query, network access, or expanded permission authority.
- Album browsing with rounded Raised landscape-cover cards, mockup-aligned circular cover badges, names, counts, adaptive layout, bounded album-detail browsing, persistent app-local pinned ordering for ordinary collections, and compact smart-access pills populated only from existing local collections plus the authorized Videos domain.

### Favorites, viewer, selection, and sharing

- Device-local Favorites stored only in Gallery application state.
- Full-screen bounded media viewer shell with Previous/Next navigation and contextual actions.
- Bounded native Android playback for canonical authorized MediaStore video item URIs, including Play/Pause, host lifecycle pause/resume, progress/seek state, and persisted autoplay/loop behavior.
- Read-only Android Share handoff for authorized media content URIs.
- Viewer details for type, album, date, dimensions, duration, and size when available.
- Long-press selection and multi-select with selected-count presentation and contextual actions.
- Bulk Share using `ACTION_SEND` or `ACTION_SEND_MULTIPLE` with bounded read-only URI grants.
- Bulk Favorite/Unfavorite behavior over the current authorized selection.
- Selection Details for a single selected item.
- Framework-independent selection and non-destructive bulk-action policy that resolves only against the current authorized/presented scope.

### Delete, Trash, and Recycle Bin Development boundary

- Android 11+ Delete/Trash candidate using Android-owned confirmation through `MediaStore.createTrashRequest(...)` or `MediaStore.createDeleteRequest(...)`.
- First-party Trash destination backed by Android MediaStore Trash, separated from Albums while preserving Android-owned restore and permanent-delete confirmation.
- Recycle Bin browsing, bounded viewer navigation, Restore, permanent Delete, and multi-select Restore/Purge actions.
- Maximum 100 unique authorized MediaStore image/video item URIs per Trash/Restore/Delete request.
- Rejection of non-MediaStore, file, network, blank, collection-only, malformed, stale, or foreign mutation targets.
- Android 10 and earlier remain fail-closed for this mutation path rather than using an unapproved legacy direct-delete workaround.

### Android-authorized Move Development implementation

- Provider-owned Android MediaStore `RELATIVE_PATH` is represented as bounded metadata in the core model and read through the Android adapter; it is not treated as raw filesystem authority.
- Existing-folder destination policy derives only from authoritative relative paths already visible in the current authorized Gallery scope.
- New-folder destination policy requires one authorized source path, validates bounded folder names, and roots photo/video/mixed destinations in `Pictures/`, `Movies/`, or `DCIM/` respectively.
- Android 11+ move authorization uses `MediaStore.createWriteRequest(...)` over canonical bounded MediaStore item URIs; the adapter independently validates the final destination before updating `MediaStore.MediaColumns.RELATIVE_PATH`.
- Selection mode exposes Move only when the current authorized selection has a safe existing-folder or new-folder destination; new folders use the bounded `Create & move` path.
- Pending move authorization state saves and restores only exact canonical URI lists and an exact canonical destination path across Activity state recreation.
- Android remains write-authorization authority. Cancellation leaves the move unapplied; approved requests execute the bounded MediaStore update off the UI thread, clear selection and thumbnail cache state, refresh Gallery state, and report moved/failed counts.
- This Development implementation is not representative physical-device/OEM/profile Move acceptance and does not establish production-safe media mutation.

### Preserve-original Copy Development implementation

- Multi-select exposes Copy when the exact current selection can establish a safe existing or new-folder destination.
- Existing Copy destinations derive only from consistent authoritative album IDs, names, and provider-owned relative paths in the current authorized scope; all selected source paths are excluded.
- New-folder Copy supports mixed source folders and selects Pictures for image-only, Movies for video-only, or DCIM for mixed image/video selections.
- Exact canonical MediaStore source URIs and image/video MIME alignment are revalidated before byte transfer.
- Output names avoid currently known destination collisions with bounded deterministic copy suffixes.
- New rows remain MediaStore `IS_PENDING` until bytes are successfully written; failures are cleaned up best-effort and originals remain untouched.
- Copy is bounded to 100 unique items per operation, requires Android 11+ in this Development slice, inserts only on the source row's concrete MediaStore volume using Android's related-copy hint, and performs no network, cloud, arbitrary filesystem, cross-profile, or source-mutation work.

### First-party photo editing Development implementation

- Viewer Edit accepts only bounded Android MediaStore `content://media` image sources with supported image MIME types and launches a non-exported Gallery editor Activity.
- The editor provides 90° left/right rotation, horizontal flip, full/custom crop plus centered 1:1, 4:3, and 16:9 crop presets, and reset.
- Rotation, flip, and normalized crop state survive ordinary Activity state recreation.
- Save copy renders the current edit plan and publishes a new MediaStore image while preserving the original source item; PNG remains PNG and other supported output is encoded as JPEG, with source relative path/date-taken metadata retained when Android exposes it.
- Failed save publication is cleaned up rather than knowingly leaving an unpublished partial MediaStore item.
- This bounded editor is a Development foundation; representative-device, accessibility, large-image/memory, metadata, recovery, and broader editing acceptance remain open.

### Settings and portability

- Slow/Fast local thumbnail loading priority.
- Included-folder and excluded-folder presentation controls over already authorized MediaStore scope.
- Show-hidden-items preference without bypassing Android authorization or profile isolation.
- Clear-cache behavior limited to the in-memory thumbnail cache.
- Favorites export/import using versioned local JSON through Android's document provider.
- Settings export/import for non-secret Gallery preferences.
- Rounded-square thumbnail preference.
- Move-deleted-items-to-Trash preference controlling Android-confirmed Trash versus permanent delete behavior on Android 11+.
- Active persisted autoplay and loop preferences for the native video viewer.
- Persisted 3/5/10-second slideshow-speed preference for the bounded local photo slideshow, defaulting to five seconds.
- App-local pinned-album identifiers plus an ordered album-identifier list used only to organize ordinary Collections presentation; stale identifiers do not create album authority.
- GIF thumbnail animation is now an active persisted Playback preference backed by bounded Android animated decoding. The empty-folder-cleanup field remains compatibility-only and hidden until that behavior exists.

### Presentation and repository controls

- GLAZE UI V1.6 source mapping for the current Development line, without claiming Gallery-specific acceptance.
- Repository validation, security/source checks, Android build/lint/test evidence, and release-engineering documentation retained in the repository.

## Implemented-but-not-accepted boundaries

The following foundations exist but remain acceptance-gated and therefore also appear in `PLANNED-FEATURES.md`:

- Android-authorized Delete/Trash and Recycle Bin behavior pending representative physical-device/OEM/profile validation.
- Android-authorized Move UI/authorization/execution pending representative physical-device/OEM/profile, cancellation/approval, provider-failure, permission-revocation, and post-move acceptance.
- First-party photo editing pending representative-device, accessibility, large-image/memory, save/recovery, and broader editing acceptance.
- Multi-select and bulk actions pending broader device/accessibility acceptance.
- GLAZE UI V1.6 source adoption pending full Gallery rendered/accessibility/adaptive-layout/performance/HVE acceptance.
- Release-engineering foundations without completed production signing, recovery, Release Candidate, or Stable qualification.

## Maintenance rule

When an obligation in `PLANNED-FEATURES.md` becomes implemented and is verified on the authoritative integration line, reconcile it here and record the material change in `CHANGELOGS.md`. Draft or unmerged pull requests are not implementation authority.