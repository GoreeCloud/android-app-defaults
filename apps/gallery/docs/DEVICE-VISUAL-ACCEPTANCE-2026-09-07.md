# Device Visual Acceptance — 2026-09-07

Status: Failed — vertical composition redesign required
Device: OnePlus Nord N200 (`dre`)
ROM: LineageOS 23.2 / Android 16

## Physical-device finding

The current Gallery build launches and presents the expected Photos/Albums/Videos/Settings structure, but the phone layout is vertically unbalanced. Content, title, summary, and actions are concentrated near the top while a large portion of the viewport remains unused. The bottom navigation is visually heavy relative to the content region.

## Required correction

- Respect system-bar insets while avoiding excessive top concentration.
- Reduce top header height and action-button mass.
- Move the media grid/list into the visual center of the usable content region rather than leaving an oversized empty canvas beneath a tiny first row.
- Use adaptive grid density based on width and item count.
- Keep section/date labels close to the media they describe.
- Reduce bottom-navigation height/padding to the Glaze mobile navigation target.
- Preserve clear Photos, Albums, Videos, and Settings destinations without making navigation dominate the screen.
- Add intentional empty-state composition when there are very few items so the screen does not look accidentally unfinished.
- Maintain comfortable one-handed reach and consistent Glaze UI spacing.

## Acceptance criteria

- Two-item and low-item libraries look intentional rather than top-heavy.
- Large libraries use the available vertical viewport efficiently.
- Header, media content, and bottom navigation have a clear hierarchy.
- The bottom navigation remains reachable without visually overwhelming the content.
- Layout is checked on the physical Nord N200 in portrait mode.
- CI success alone is not sufficient for visual acceptance.
