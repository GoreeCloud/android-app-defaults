# GoreeCloud Gallery Changelogs

## October 3, 2026 — icon-first action chrome

### Changed
- Replaced text-heavy full-screen viewer controls with compact icon/glyph actions for view mode, slideshow/repeat, Share, Favorite, Edit, Delete, More, and video Play/Pause while retaining accessible names, tooltips, selected state, and 48dp minimum targets.
- Rebuilt multi-selection actions around icon-first Share, Favorite, Move, Copy, Delete, and More controls; destructive actions keep distinct semantic treatment rather than relying on icon shape alone.
- Replaced Trash Refresh, Trash selection actions, and Trash viewer actions with matching icon-first controls while preserving Android-managed Trash authority and confirmation behavior.
- Reworked the first-party photo editor so Cancel/Save, Rotate left/right, Flip, and Reset are icon controls; crop aspect presets remain short textual ratios because the value itself is the useful affordance.
- Replaced text pills for Settings actions such as Clear, Import/Export, reset, and setup replay with compact glyph affordances while keeping the whole setting row labeled and accessible.
- The current-main candidate now consolidates the unified navigation-glyph and icon-first action passes in one directly based branch while retaining the `0.8.10-dev` Development identity and monotonic CI version-code contract.
- Replaced remaining font/Unicode pseudo-glyph controls with first-party vector artwork for previous/next navigation, video play overlays, media-selection checks, card overflow, and directional affordances.
- Added bounded press/ripple feedback to first-party viewer, selection, header, Settings, Trash, and editor controls.
- Extended bounded feedback to inline search close, media-access, album quick-access, and album-card surfaces; header title/subtitle now stay single-line with ellipsis so long album names cannot crowd out primary actions.
- Photo-editor crop presets now expose an explicit selected visual/accessibility state, beginning with Original and clearing when the crop becomes custom.
- The first-use setup wizard now uses a compact progress rail plus icon-only Back/Return/Continue/Finish controls, preserving its existing accessibility names and persisted step behavior.
- Strengthened Android state semantics for stateful icon controls: selected media tiles, selected dialog choices, selection actions, and active viewer toggles now expose explicit state descriptions where supported.
- Media-access actions now pair concise labels with contextual first-party glyphs (choose media, change access, refresh/retry) so high-consequence permission/recovery actions remain explicit without reverting to text-only chrome.
- Preserved Glaze control refinement for dynamically labeled media-access actions and explicitly exposed destructive-action state semantics on Trash permanent-delete controls.
- Settings value rows now pair concise values with chevron glyphs and expose the current value through Android state descriptions; boolean settings use vector on/off switch glyphs rather than text-only On/Off pills.
- Added specific save-copy and clear-cache glyphs so those actions no longer reuse generic download/reset imagery.
- Photos/Albums/Videos/search empty states now render as semantic Glaze icon cards, while Trash adds a matching semantic empty-state card and compact-width-safe ellipsized header text.
- Refined Videos responsiveness: sub-360dp layouts use one compact follow-on card column, phones use two, wider layouts use three, and very wide screens use four; the featured card is capped and centered so it does not become excessively large on tablets/desktops.
- Every Albums quick-access chip now carries a first-party glyph, using the generic Albums icon when a recognized collection-specific icon is unavailable.
- Video identity now relies on the play glyph; duration badges render only when MediaStore supplies a duration, avoiding the old text-only “VIDEO” fallback. Trash video tiles receive the same centered play affordance and selection-aware cleanup.

### Verification
- Extended Android runtime coverage for icon-only photo-editor chrome, safe-area placement, and selected crop-preset state.
- Extended rendered Trash acceptance to require an icon-only Refresh control while preserving minimum target size, Glaze refinement, and the unified navigation ripple checks.

### Boundary
This is a presentation and interaction-density pass. It does not add storage, filesystem, network, account, cloud, Protected Photos, or new media-mutation authority, and it does not establish Production Acceptance, Stable, Seal, or Anchor status.

## October 3, 2026 — unified navigation glyph system

### Changed
- Replaced the five mixed-style bottom-navigation symbols with a consistent 24×24 outline glyph family using one stroke language across Photos, Albums, Videos, Trash, and Settings.
- Preserved the 22dp rendered optical size established by the preceding alignment pass.
- Added bounded per-destination ripple feedback while keeping selected Glaze material, accessibility state, and icon-only tooltips intact.
- Advanced the Development identity to `0.8.10-dev`; delivered CI version codes must exceed `2000741`.

### Verification
- Rendered Android acceptance now checks the shared glyph bounds and ripple feedback on the primary Gallery navigation.
- Trash rendered acceptance verifies the same ripple treatment on the Android-managed recovery destination.

### Boundary
This is a presentation/interaction refinement only. It does not change destination authority, media permissions, local-storage scope, network/account access, destructive-operation authority, or release status.

## October 3, 2026 — bottom-navigation optical alignment

### Changed
- Reworked icons-only navigation so glyphs use a truly centered compound-drawable slot instead of a top drawable paired with an invisible text line.
- Removed per-destination start margins so all five navigation targets have equal-width, evenly spaced centers.
- Increased navigation glyphs to a consistent 22dp optical size and tightened the selected Glaze pill with mode-aware insets.
- Centralized bottom-navigation presentation so Gallery and Android-managed Trash use the same geometry from the first frame.
- Added destination tooltips for icon-only discoverability while preserving accessibility content/state descriptions.
- Simplified setup-wizard copy and reduced dialog density without changing permission or privacy boundaries.
- Advanced the Development identity to `0.8.9-dev`; delivered CI version codes must exceed `2000730`.

### Verification
- Added unit coverage for icon placement and selected-state geometry policy.
- Added rendered Android acceptance coverage for icons-only centered glyph placement, equal slot widths, even distribution, inset selected material, and tooltips.

### Boundary
This pass changes Gallery presentation and onboarding wording only. It does not expand Android media permission scope, filesystem access, network/account authority, mutation authority, or Stable/release status.

## October 2, 2026 — live library refresh and navigation polish

### Added
- Added a foreground MediaStore observer with bounded debounce so newly created, edited, moved, copied, restored, or removed local media can refresh the authorized Gallery snapshot without requiring an app relaunch.
- Added a silent refresh path that avoids replacing the current grid with the full loading state, retains still-valid multi-selection, and preserves the current scroll offset when the user is already browsing below the top.
- Added **Settings > Appearance > Bottom navigation** with **Icons only** (default), **Text only**, and **Icons & text** modes. Accessibility destination names remain available in every visual mode, and the preference participates in settings export/import.
- Added rounded Glaze popup surfaces for three-dot overflow/context menus in light and dark themes.

### Changed
- Tapping the already-selected primary destination now returns its current view to the top.
- Compact video cards now ellipsize long filenames on one line instead of wrapping awkwardly.
- Advanced the Development identity to `0.8.8-dev` and require delivered CI version codes above the previously staged `2000709` build.

### Boundary
Live refresh observes only Android MediaStore URIs and re-reads only the current Android-authorized media scope. No network, account, filesystem, broader storage, background sync, or additional media permission authority was added. Gallery remains Development/non-Stable.

## October 2, 2026 — isolate the Development install channel

Gallery Development debug builds now use the approved variant identity `com.goreecloud.gallery.dev` while the canonical product identity remains `com.goreecloud.gallery`. This prevents a differently signed Development APK from colliding with an existing canonical Gallery installation, allows both variants to coexist during representative-device testing, and establishes the package boundary required for future persistent-signer in-place Development updates. Versioning remains `0.8.7-dev` with a monotonically increasing CI-derived Android versionCode for delivered Development artifacts. Gallery remains Development/non-Stable.

## October 2, 2026 — integrate slideshow pause/resume

Protected PR #185 integrated session-local Pause / Resume controls for the bounded photo slideshow from exact candidate `69a65412f24271ecb627b2eb5aaf96b55de9e570` as monorepo main `7902e7bd58a510591e6c8f8778a3872df4c77642`. Gallery build/lint/unit and Android 16 runtime passed before integration. Lifecycle exit and manual viewer navigation continue to stop slideshow state, and no media/storage/network authority changed. Representative-device and release acceptance remain open.

## October 1, 2026 — bounded viewer zoom and pan

### Added
- Added photo-only pinch zoom from 1× through a bounded 4× maximum over the existing full-screen viewer bitmap.
- Added single-finger panning while zoomed with translation bounded to the scaled viewer viewport.
- Suppressed horizontal item-navigation swipes while zoomed or after a multi-pointer zoom gesture.
- Reworked the viewer scale control into explicit Fit, Fill, 2× Zoom, and conditional Reset zoom options with accessibility announcements.
- Reset zoom/pan when navigating to a different media item and keep zoom unavailable for native video playback.
- Added pure zoom-policy coverage for scale limits, pan bounds, invalid gesture input, zoom-state detection, and the 2× preset.

### Boundary
This capability changes presentation only. It does not increase MediaStore scope, decode authority, storage access, mutation authority, or network/cloud access. The image decoder remains bounded to the current viewport and a 2048px long-edge ceiling, so this does not claim original-resolution/full-resolution zoom.

## October 1, 2026 — manual app-local Album ordering

### Added
- Added **Move earlier / Move later** to ordinary Album card overflow menus when movement is available.
- Persisted an ordered album-identifier list as Gallery-local presentation state and added it to settings export/import.
- Kept Pin/Unpin as the top-level grouping rule: manual moves never cross the pinned/unpinned boundary.
- Added **Reset album order** under Appearance after a manual order exists; reset preserves Pin/Unpin choices.
- Added pure policy and overflow coverage for deterministic ordering, stale-ID handling, new-album fallback, group boundaries, and movement availability.

### Boundary
Manual Album ordering changes only Gallery's Collections presentation. It does not reorder provider-owned MediaStore folders, mutate media, create album authority, or add storage/filesystem/network/account permissions.

## October 1, 2026 — opt-in animated GIF thumbnails

### Added
- Activated **Animate GIFs in thumbnails** under Settings > Playback.
- Added bounded Android animated decoding for authorized `image/gif` grid and album-cover thumbnails on Android 9/API 28+.
- Preserved source aspect ratio, disabled upscaling, and capped animated thumbnail decode requests at a 512 px long edge.
- Added static MediaStore thumbnail fallback when animated decode is unavailable or fails.
- Start/stop animated drawables with ImageView attachment so off-screen cards do not intentionally continue animating.
- Added pure policy coverage and rendered Settings acceptance for the active control.

### Boundary
The capability is presentation-only, remains off by default, and adds no media permission, mutation, filesystem, account, network, cloud, or synchronization authority. Representative-device/OEM/profile, accessibility, complex-GIF resource use, performance/power, and release acceptance remain open.

## October 2, 2026 — preserve-original MediaStore Copy

### Added
- Added **Copy** to bounded Gallery multi-select alongside the existing Move path.
- Added existing-folder Copy destinations derived only from the current Android-authorized album/path snapshot while excluding every selected source path.
- Added **Create & copy** with Pictures / Movies / DCIM rooting based on selected media type, including mixed-source folders when all selected media remains on one concrete MediaStore volume.
- Added deterministic bounded destination-name collision handling using `(copy)`, `(copy 2)`, and later suffixes, with generated names trimmed as needed to remain within the supported MediaStore display-name bound.
- Added an Android 11+ MediaStore Copy executor that preflights one concrete provider-owned `VOLUME_NAME` across the full selection, rejects synthetic aggregate-volume insertion, supplies `QUERY_ARG_RELATED_URI`, creates `IS_PENDING` destination rows, streams exact selected source bytes, publishes only successful outputs, and cleans failed partial rows best-effort.
- Projected provider-owned `VOLUME_NAME` separately from Gallery's established aggregate-external content-URI identity so volume-sensitive organization can be correct without invalidating existing URI-keyed Favorites/settings state.
- Added core and adapter policy coverage for foreign selections, metadata conflicts, new-folder authority, naming, canonical URIs, duplicate sources, and MIME/collection mismatch.

### Boundary
Copy preserves originals and does not use Move's Android write-authorization request. It adds no arbitrary filesystem, network, cloud, account, synchronization, cross-profile, source-overwrite, source-delete, or source-move authority. Representative-device/OEM/profile, large-file/failure, metadata-fidelity, accessibility, performance, and release acceptance remain open.

## October 1, 2026 — pinned Albums and configurable slideshow pace

### Added
- Added **Pin to top / Unpin from top** to ordinary Albums card overflow menus.
- Persisted pinned album identifiers locally and used them only to order the existing authorized Collections grid; stale pins are ignored rather than fabricating albums.
- Added a **Slideshow speed** setting with 3-second, 5-second, and 10-second choices; five seconds remains the default.
- Added pinned-album and slideshow-speed fields to the existing additive non-secret settings portability envelope.
- Added pure policy coverage plus rendered Settings acceptance for the new presentation controls.

### Boundary
Both capabilities are app-private presentation behavior. Album pins do not rename, move, copy, create, or mutate MediaStore content, and slideshow pace does not expand media, storage, filesystem, account, network, cloud, or synchronization authority.

## October 1, 2026 — functional-only Settings cleanup

### Changed
- Removed inactive Settings rows for GIF thumbnail animation, automatic empty-folder cleanup, and password-protected photos rather than presenting unfinished behavior as active controls.
- Retained compatible settings import/export fields for the unfinished GIF and empty-folder preferences so existing Development settings files continue to parse safely.
- Renamed the visible deletion preference to **Move deleted items to Trash** to match the dedicated Trash destination and Android MediaStore terminology.
- Removed the obsolete password-protection explanation helper after the inactive row was removed.
- Added no new media, storage, authentication, filesystem, account, network, cloud, or synchronization authority.

### Development boundary
Protected Photos, GIF thumbnail animation, and automatic empty-folder cleanup remain unfinished capabilities. They must return to Settings only when real behavior and their required security/acceptance boundaries exist.

## October 1, 2026 — Albums cover-badge mockup alignment

### Changed
- Added the mockup's circular lower-left cover badge treatment to Albums cards.
- Favorites, Camera, Screenshots, Downloads, and Screen recordings reuse their existing first-party Gallery icon semantics; other video-dominant collections can use the Videos icon and remaining albums use the generic Albums icon.
- Refactored the Albums quick-access policy to expose semantic collection kinds instead of coupling UI icon selection to numeric priority values.
- Cover badges are decorative only and do not add media permission, collection inference, mutation, filesystem, account, network, cloud, or synchronization authority.

## October 1, 2026 — primary-surface chrome cleanup

### Changed
- Removed the persistent Group/View row from Photos and Videos.
- Kept Search and Sort as the primary header actions to match the supplied mockups more closely.
- Preserved grouping and view-density in Settings > Appearance.
- Kept Videos category filters unchanged.

## October 1, 2026 — mockup-aligned smart-filter iconography

### Changed
- Added compact semantic icons to Videos filter chips for All, Screen recordings, Camera, and Favorites.
- Added matching semantic icons to Albums smart-access pills for recognized Favorites, Camera, Screenshots, Downloads, Screen recordings, and Videos shortcuts.
- Icons inherit the same foreground tint as their labels, preserving light/dark Glaze contrast and keeping selected state communicated through both surface treatment and accessibility state.

### Boundary
This is presentation-only work. No new album inference, media permission, mutation, filesystem, account, network, cloud, or synchronization authority is introduced.

## October 1, 2026 — contextual card overflow actions

### Added
- Added trailing vertical overflow controls to Albums and Videos cards to match the supplied Gallery mockups.
- Video card menus provide Share, Add/Remove Favorite, and Details over the already-authorized local item.
- Album card menus provide Open and Details while reusing the existing album navigation path.
- Overflow controls retain a 48dp target and independent accessibility focus.

### Boundary
The compact card menus intentionally exclude Delete, Move, permanent deletion, and other destructive mutation. Existing Android-owned confirmation and selection/action surfaces remain authoritative for those operations.

## October 1, 2026 — Albums card and Videos subtitle mockup alignment

### Changed
- Corrected the Albums collection grid to the supplied mockup's rounded Raised-card treatment with landscape media covers and compact title/count footers.
- Replaced thumbnail-style Albums Quick access mini-cards with compact real-data smart-access pills; authorized Videos can appear as a direct pill without fabricating People, Places, Documents, or other unsupported smart collections.
- Counts Gallery-local Favorites as a displayed Albums collection when present, so the Albums subtitle better reflects the visible collection surface.
- Uses **Recently added** for the newest Videos presentation order while retaining **Oldest first** when the user reverses sort order.
- Preserved Android MediaStore authority and added no new media permission, mutation, filesystem, account, network, cloud, or synchronization authority.

## October 1, 2026 — Albums mockup-alignment refinement

### Changed
- Refined the Albums surface toward the supplied Gallery mockup with media-first flat collection tiles, square rounded covers, and album labels/counts aligned directly beneath each cover.
- Tightened the real-data Quick access lane into compact cover shortcuts and aligned the Albums subtitle to the plural **Smart collections** wording when recognized local smart-access collections exist.
- Preserved the complete Collections grid and existing album navigation while keeping Quick access limited to collections actually present in the current Android-authorized snapshot or Gallery-local Favorites state.
- Added no new MediaStore permission, mutation, filesystem, account, network, or synchronization authority.

## October 1, 2026 — real-data Albums Quick access

### Added
- Added a horizontal **Quick access** lane above the full Albums collection grid.
- Quick access is derived only from currently visible authorized collections: Gallery-local Favorites and existing Camera, Screenshots, Downloads, or Screen recordings albums.
- Familiar collections are ordered deterministically and capped at four shortcuts; ordinary albums remain in the complete Collections grid.
- Quick-access cards reuse the existing local thumbnail loader and album navigation path, adding no MediaStore permission, mutation, filesystem, account, or network authority.

### Development boundary
Representative-device visual/accessibility/form-factor acceptance remains open. Gallery does not fabricate People, Places, Documents, or other smart collections that are not actually implemented.

## October 1, 2026 — mockup-aligned five-tab Gallery navigation

### Changed
- Began the new Gallery UI implementation with a five-destination bottom navigation model: **Photos / Albums / Videos / Trash / Settings**.
- Promoted Android MediaStore Trash to a dedicated primary **Trash** destination and removed the Recovery/Recycle Bin entry from Albums.
- Reworked the Trash surface as a root destination with the same Glaze bottom-navigation capsule, selected-state treatment, and direct return paths to Photos, Albums, Videos, and Settings.
- Preserved the existing Android-owned restore and permanent-delete confirmation boundary; this navigation change adds no new storage, filesystem, network, account, or mutation authority.
- Added a dedicated Trash navigation icon, shared destination handoff contract, and rendered acceptance coverage for the five-tab shell.

### Development boundary
This is the first implementation slice of the supplied Gallery mockup direction. Broader mockup-aligned Photos, Albums, Videos, Trash, and Settings composition/polish, representative-device visual review, accessibility acceptance, and release qualification remain open.

## September 29, 2026 — bounded authorized local search reconciliation

- Reconciled still-required behavior from legacy Gallery PRs #42 and #43 into the monorepo Development line.
- Restored one bounded core search contract over the already-authorized visible media snapshot, with tokenized case-insensitive matching across display name, album name, MIME type, and image/video kind.
- Routed Photos, Videos, Favorites, opened albums, and album collection search through that shared core contract instead of maintaining a separate activity-local predicate.
- Search does not issue a new MediaStore listing query, request broader Android media permission, contact a network/cloud service, or create query-history persistence.
- This reconciliation is Development source work only and does not establish representative-device, accessibility, Glaze UI, platform-system, release, or Stable acceptance.


## September 29, 2026 — persistent local sort order candidate
- Persisted **Newest first / Oldest first** instead of resetting the local browsing order on process launch.
- The existing header sort control now writes the app-private preference, and Settings exposes the same choice directly.
- Added `sortPreference` to Gallery settings export/import while preserving the additive schema-v1 envelope.
- Newest first remains the migration-safe default and maps to the existing core `MediaSortOrder` contract.
- Added pure policy coverage for defaulting, stored-value round trips, and core-sort mapping.
- Sorting remains presentation-only over the same Android-authorized media and does not add permission, mutation, storage, account, network, or synchronization authority.
- Fresh exact-head validation and representative-device/accessibility acceptance remain required.

## September 29, 2026 — local timeline grouping candidate

- Added a persisted **Day / Month / Year / None** media-grouping preference with Day as the migration-safe default.
- Day preserves Today/Yesterday/calendar-day sections; Month and Year provide broader local timeline sections; None renders one continuous grid.
- Grouping is presentation-only over the same Android-authorized and already-sorted media collection.
- Added grouping to Gallery settings export/import and corrected the existing one-way `viewDensity` portability path so imported settings restore density too.
- Added pure policy coverage for defaulting, stable portable identities, option order, and round trips.
- Added no media permission, mutation, storage, account, network, or synchronization authority.
- This remains Development candidate evidence pending fresh exact-head validation and representative-device/accessibility acceptance.


## September 29, 2026 — viewer image-decode authority reconciliation

- Corrected repository feature authority to reflect live PR #100 source: authorized full-screen image viewing already uses the bounded `GalleryViewerBitmapLoader` / `GalleryViewerImageDecoder` path when the viewport is measured.
- Recorded Android `ImageDecoder` orientation handling, viewport-aware sizing, no-upscale behavior, and the existing 2048px long-edge memory bound.
- Kept true unrestricted full-resolution/zoom behavior and representative-device quality/memory/accessibility acceptance explicitly open.
- No runtime source, media authorization, mutation, permission, storage, network, or release authority changed in this documentation reconciliation.


## September 28, 2026 — bounded local photo slideshow

The authorized full-screen Gallery viewer now provides a bounded **Slide / Stop** photo slideshow. It advances every five seconds to the next later image in the current authorized viewer collection, skips non-photo entries, stops at the end, and cancels on Activity pause/destroy, viewer replacement/close, manual Previous/Next, or swipe navigation. It adds no new MediaStore, storage, mutation, account, or network authority.

Pure policy coverage locks forward-only photo progression and fail-closed invalid/end states. Exact candidate head `c30ba640e86f9e4b28f78f9ad7e8b80644eec00f` passed Platform Contract, Native Core, Native Android Adapter, Native Android App, Native Android Rendered Acceptance, and GoreeCloud Gallery acceptance validation. Representative physical-device presentation, large-text top-bar fit, TalkBack/Switch Access, orientation/form-factor behavior, video slideshow behavior, and production/Stable acceptance remain open.


## September 28, 2026 — viewer Fit / Fill presentation control

The authorized full-screen media viewer now exposes an explicit **Fit / Fill** presentation control. **Fit** keeps the complete media visible with `FIT_CENTER`; **Fill** uses `CENTER_CROP` to occupy the viewer canvas. The control carries state-specific accessibility wording and announces mode changes. This is session-local presentation only and does not change MediaStore scope, mutation authority, metadata, favorites, editing, or playback permissions.

Focused policy coverage locks deterministic Fit ↔ Fill toggling. Representative-device image/video behavior, large-text/top-bar fit, TalkBack/Switch Access, orientation changes, and broader viewer acceptance remain open.


This file is the authoritative repository change-history record under Standard — Repository Feature Tracking and Changelog Governance v1.0. It supersedes the retired singular `CHANGELOG.md` filename while preserving the existing Gallery history below.

## 2026-09-28 — dismissible Gallery guidance candidate

### Added
- Per-destination dismissal for the optional Photos, Videos, and Albums contextual hints, persisted in Gallery's local setup/preferences store.
- A **Reset dismissed hints** action under Settings → Guidance that restores previously dismissed tips without resetting onboarding or unrelated preferences.
- Runtime preference coverage for dismissal persistence and reset behavior.

### Boundary
The global Contextual hints switch remains separate and safety/privacy/error/system messages remain unaffected. This is Development source; fresh exact-head validation and representative-device accessibility/form-factor acceptance remain required.

---

## 2026-09-25 — Current-main physical-device source reconciliation

### Added

- Current-main app-layer Move integration over the bounded Android-authorized MediaStore foundation: eligible selection action, current-scope existing-folder choices, bounded new-folder `Create & move`, exact write-authorization handoff, Activity-state restoration of pending authorization, cancellation handling, confirmed background execution, refresh, and moved/failed result reporting.
- A non-exported first-party photo editor for bounded MediaStore image sources with left/right 90° rotation, horizontal flip, custom/full and 1:1/4:3/16:9 crop controls, reset, Activity-state restoration, and save-as-new-copy behavior that preserves the original.
- Exact-head rendered evidence for main Gallery and Recycle Bin surfaces in light and dark modes on the deterministic Android 36 emulator profile.

### Changed

- Reconciled the physical-device stabilization work onto current authoritative `main` ancestry rather than integrating the long-lived historical feature stack.
- Updated the Gallery presentation source mapping to GLAZE UI V1.6 / 1.6.0 while treating retained Gallery-local optical/spatial values as adapter invariants rather than canonical V1.6 token claims.
- Preserved `RecycleBinActivity` as non-exported while moving CI screenshot capture for that private surface into an instrumentation-only harness instead of weakening the production Activity boundary.
- Retained safe-area/system-bar, navigation, launcher-icon, selection/drag, media-mutation, and photo-editor source/test corrections in the reconciled Development candidate.

### Validated

- Source revision `06804cb768714c57dd9656e6597b5265b2f541e7` passed Platform Contract #73, Native Core #191, Native Android Adapter #359, Native Android App #355, and Native Android Rendered Acceptance #146.
- Rendered artifact `10854541717` is bound to that source revision with GitHub digest `sha256:68ccf0ae3bee1913bb20292deae6979d4b63df02dafb91c92887df0dce2a0e42`; the reviewed evidence contains main and Recycle Bin light/dark screenshots and the rendered instrumentation suite reports no failures.
- The broader GoreeCloud Gallery acceptance workflow remains a separate exact-head gate and must be recorded from its final result before integration.

### Lifecycle boundary

This is Development evidence only. Representative physical-device/OEM/profile Move and editor acceptance, complete GLAZE UI V1.6 conformance, accessibility/form-factor/performance/Human Visual Excellence, recovery/rollback, platform-system runtime acceptance, branch protection/review, production signing/provenance, Release Candidate, production, and Anchor qualification remain open.

---

## 2026-09-24 — Android-authorized Move foundation

### Added

- Provider-owned MediaStore `RELATIVE_PATH` projection and normalization without creating filesystem authority.
- Existing-folder and new-folder move destination policies constrained to the current authorized media scope.
- Android 11+ exact-item write authorization through `MediaStore.createWriteRequest(...)`, canonical pending-state restoration, and bounded `RELATIVE_PATH` update execution.
- JVM/adapter regression coverage for destination authority, unsafe paths/names, foreign selections, provider metadata conflicts, and canonical pending state.

### Lifecycle boundary

The user-facing Gallery Move action remains disabled in this foundation. App-layer lifecycle/UI integration, post-move refresh behavior, representative physical-device/OEM validation, GLAZE UI acceptance, Release Candidate, production, and Stable qualification remain open.

---

## 2026-09-22 — Repository feature/changelog governance migration

### Changed

- Established `IMPLEMENTED-FEATURES.md`, `PLANNED-FEATURES.md`, and `CHANGELOGS.md` as the repository-native feature/change authority.
- Retired `FEATURE-ROADMAP.md` and the singular `CHANGELOG.md` filename from the active control model.
- Removed the obsolete requirement to synchronize roadmap/changelog authority with Google Drive.

### Lifecycle boundary

This documentation/control-plane migration does not alter Gallery runtime behavior and does not establish Release Candidate, Production Acceptance, or Stable qualification.

---

## Preserved historical changelog

This changelog records material source, build, validation, release-engineering, and product-readiness changes in the dedicated `GoreeCloud/goreecloud-gallery` repository. Historical development that occurred in the temporary website-repository build carrier remains preserved in Git and in the GoreeCloud patch provenance records.

## 1.0.0 — Stable candidate

### Added

- gc.9 final package-identity patch with `VERSION_NAME=1.0.0` and Android `VERSION_CODE=10009`;
- non-secret Stable signing execution runbook;
- representative-device acceptance runbook;
- authoritative `docs/STABLE-CANDIDATE-1.0.0.md` promotion contract;
- gc.10 Settings cleanup and Android-native Privacy & permissions access;
- gc.11 native Glaze UI 1.0 semantic resources, adaptive Settings composition, practical target sizing, and fail-closed conformance checks;
- gc.12 Glaze UI browsing-surface integration for the folders screen and opened-folder media grid;
- gc.13 Glaze UI search-surface integration with shared Canvas, branded search chrome, adaptive browsing gutters, and a rounded muted empty-state surface;
- gc.14 Glaze UI media-viewer overlay treatment with muted semantic chrome and comfortable viewer action targets;
- gc.15 representative-device refinement for sorting/grouping/filter dialogs, destructive confirmations, overflow menus, Settings density/header treatment, and folder label/count hierarchy;
- gc.16 representative-device dialog-geometry refinement for content-sized sorting/grouping/filter surfaces.

### Changed

- ordinary acceptance CI builds and validates `GoreeCloud-Gallery-1.0.0.apk` while retaining `acceptance-candidate` classification;
- the protected manual signing workflow defaults to `1.0.0` while retaining `signed-release-candidate` classification;
- gc.10 removes misleading fixed-thumbnail controls and upstream purchase UI while preserving meaningful Gallery settings;
- gc.11 increments Android `VERSION_CODE` to `10011` while preserving semantic version `1.0.0`;
- gc.12 increments Android `VERSION_CODE` to `10012` while preserving semantic version `1.0.0`;
- gc.13 increments Android `VERSION_CODE` to `10013` while preserving semantic version `1.0.0`;
- gc.14 increments Android `VERSION_CODE` to `10014` while preserving semantic version `1.0.0`;
- gc.15 increments Android `VERSION_CODE` to `10015` while preserving semantic version `1.0.0`;
- gc.16 increments Android `VERSION_CODE` to `10016` while preserving semantic version `1.0.0`;
- Settings uses Glaze Canvas, gradient navigation emphasis, rounded Raised rows, Android ripple feedback, 48dp comfortable interactive targets, dedicated light/dark semantic palettes, and wider native insets at `sw600dp` and `sw840dp`;
- gc.15 reduces redundant Settings divider rules and excessive card spacing while reinforcing the Glaze app-bar treatment;
- primary browsing surfaces use the Glaze Canvas, branded menu chrome, adaptive 8/16/24dp media-aware gutters, Raised empty-state actions, comfortable action targets, and semantic loading accent without cardifying every thumbnail;
- folder names use the semantic primary text role and stronger weight while item counts use the semantic muted role and smaller secondary typography;
- search reuses the same browsing composition and adds a restrained muted Raised empty state so search visually belongs to the same Glaze family as folders and media;
- the full-screen media viewer uses restrained muted Glaze chrome and a rounded bottom action overlay while keeping the media itself visually dominant;
- viewer actions use 48dp comfortable target sizing without altering the behavior of delete or other destructive operations;
- sorting, grouping, and media-filter dialogs use rounded Glaze surfaces, semantic accent controls, 48dp comfortable targets, and reduced redundant divider rules;
- gc.16 changes those dialog root ScrollViews from full-height to content-driven `wrap_content`, disables forced viewport filling, and retains overflow scrolling only when content exceeds the available viewport;
- destructive folder-deletion confirmation retains its existing confirmation semantics while using the semantic danger role for warning text;
- toolbar overflow menus use coordinated Glaze light/dark surfaces with rounded geometry and restrained accent outlines;
- Gallery records Glaze UI `1.0.0` and canonical reference revision `d6e446fd8ef251259d16368d50aad90d9287a774` as its native conformance target;
- repository/source validation requires the current Glaze patch line and fails closed if the semantic resource, Settings, browsing-surface, search, media-viewer, transient-surface, or dialog-geometry integration contract disappears;
- Stable promotion must reuse the exact accepted signed binary rather than rebuilding merely to alter release labeling.

### Validated

- gc.9 exact-head validation reconstructed pinned Fossify Gallery and Commons source, passed repository/security/source checks, executed `GoreeCloudGalleryPolicyTest`, passed Android lint, and assembled/validated the `1.0.0` FOSS acceptance APK;
- gc.10 exact-head and post-merge validation passed after Settings simplification and produced the updated acceptance APK from `main`;
- gc.11 exact-head run 31973570278 and post-merge main run 31977767072 completed successfully through repository/security/source validation, behavioral tests, Android lint, APK assembly, APK/evidence validation, and artifact upload;
- gc.12 exact-head run 31978012920 and post-merge main run 31978605284 completed successfully through every acceptance step; PR #16 was squash merged as `1e1099246fb771508be16a8423771a66f6b9055d`;
- gc.13 exact-head run 31980042523 and post-merge run 31982557178 completed successfully through every acceptance step; PR #17 was squash merged as `87f87ecdca9317d4acf3cd9a8d74a766eb5dd060`;
- gc.14 exact-head run 31983755647 completed successfully through every acceptance step, retained the `1.0.0` acceptance APK/evidence artifact, and PR #18 was squash merged as `9767bccfd5f43805f81f796c27b4168b6649f782`;
- representative-device screenshots from the gc.14 acceptance build informed the gc.15 refinement scope for transient surfaces, Settings density/header consistency, and folder typography hierarchy;
- gc.15 exact-head run 31988324362 and post-merge main run 31988927104 completed successfully through every acceptance step; PR #19 was squash merged as `418bb064c9abf72b91087966bc88cedec0bcda53`;
- representative-device gc.15 screenshots identified excessive empty vertical space in the Sort by dialog and motivated the narrow gc.16 geometry refinement;
- gc.16 requires its own exact-head acceptance run before merge because it changes dialog root geometry, versionCode, patch provenance, conformance validation, and documentation.

### Release boundary

`1.0.0` is **not yet Stable**. Long-lived signing and recovery, protected `stable-release` administration, repository-level `main` protection, representative-device permissions/file-operation/user-profile/accessibility/Glaze UI acceptance, same-signer upgrade/recovery testing, and final release evidence remain blocking.

## Repository readiness hardening — gc.8

### Added

- root GNU GPL v3 license text;
- `SECURITY.md` vulnerability and security-boundary guidance;
- `CONTRIBUTING.md` contribution, patch-provenance, Glaze UI, and release guidance;
- `NOTICE.md` modified-work and exact upstream provenance record;
- `.github/CODEOWNERS` review ownership;
- `docs/ARCHITECTURE.md` runtime, source, privacy, user/profile, Glaze UI, build, and release architecture;
- `docs/GLAZE-UI.md` Gallery-specific Glaze UI implementation and release-review contract;
- `docs/REPOSITORY-READINESS.md` readiness classification and open stable-release blockers;
- `docs/RELEASE-EVIDENCE-TEMPLATE.md` comprehensive candidate/stable evidence record;
- `scripts/validate-repository-structure.sh` fail-closed repository layout/governance validation;
- `scripts/write-build-evidence.sh` machine-readable non-secret build identity and patch-line provenance;
- gc.8 GoreeCloud-owned behavioral-test foundation for enforced thumbnail presentation policy.

### Changed

- pull-request CI checks out and verifies the exact pull-request head SHA rather than relying on GitHub's synthetic merge checkout;
- push/manual acceptance validates the exact `github.sha`;
- signed-candidate CI verifies the exact dispatched revision;
- repository security guardrails reject unapproved write permissions, `pull_request_target`, automatic signed-candidate triggers, missing checkout credential hardening, mutable third-party Action refs, and common committed key/certificate container files;
- ordinary and signed-candidate CI fail closed unless JUnit XML proves the required GoreeCloud behavioral test class executed with at least three tests and zero failures/errors;
- CI retains Gradle test/lint console evidence, JUnit XML/results, packaged APK validator output, and machine-readable build evidence;
- build evidence schema v2 records the exact validated checkout commit and maintained GoreeCloud patch line separately from the packaged application version;
- build/release and stable-checklist documentation includes Android OS user/profile isolation as Gallery's application-appropriate multi-user boundary;
- the acceptance workflow uses a version-independent name so its GitHub check identity does not become stale as the maintained patch line evolves.

### Validated

- exact-head validation reconstructed pinned Fossify Gallery and Commons source plus gc.1-gc.8, passed repository/security/source checks, executed `GoreeCloudGalleryPolicyTest` with 3 tests, 0 failures, and 0 errors, completed Android lint, built and validated the `1.0.0-gc.7` FOSS acceptance APK, and retained checksum, licensing, test, lint, APK, and build-identity evidence.

## 1.0.0-gc.7 — Acceptance candidate

### Changed

- corrected the remaining toolbar overflow popup defect at its real owner path in Fossify Commons `MySearchMenu` and embedded `MaterialToolbar`;
- ensured the overflow popup receives the intended GoreeCloud light/dark presentation during construction and search-bar color refresh;
- preserved rounded Gallery media presentation and previously accepted Glaze UI refinements.

### Validated

- real-device screenshots confirmed the previously unreadable top-bar overflow menus are readable on both the main folders view and an opened media folder;
- repository CI reconstructed the exact pinned Fossify Gallery and Commons source, applied the accepted GoreeCloud patch chain, ran source invariants and Android validation, built the FOSS acceptance APK, verified package identity/version/offline boundary/signature/notice, scanned packaged DEX for removed warning text, generated SHA-256, and retained acceptance evidence.

### Known release limitations

- the acceptance APK is not the long-lived stable signing baseline;
- full stable-release real-device permission, destructive-operation, Android user/profile, accessibility, upgrade/recovery, and signing evidence is incomplete;
- repository-level `main` protection remains a separate administrative gate.

## Historical gc.1 through gc.7 transformation line

The gc.1-gc.7 development increments established the dedicated GoreeCloud package identity, branding, offline boundary, Glaze UI palette and surfaces, launcher behavior, removal of inappropriate upstream counterfeit-build messaging from the GoreeCloud build, rounded settings/dialog/media presentation, no-square-thumbnail product behavior, navigation-resource corrections, and the accepted toolbar overflow correction.
