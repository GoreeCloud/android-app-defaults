# GoreeCloud Gallery Android Layout Redesign Milestone

Status: Historical milestone; current mockup alignment continues on the first-party native Development line

This milestone follows the failed 2026-09-07 physical-device visual acceptance pass.

## Goals

- Correct the top-heavy phone composition seen on the OnePlus Nord N200.
- Historical goal: preserve the then-current Photos / Albums / Videos / Settings information architecture. Current Gallery uses Photos / Albums / Videos / Trash / Settings, with Trash promoted to a dedicated primary destination.
- Use the available vertical viewport intentionally for both sparse and dense libraries.
- Reduce navigation and action chrome so media remains the visual focus.

## Photos screen

- Compact title and summary block with smaller top inset.
- Search and sort controls use compact Glaze control sizing.
- Date/group headings stay close to the corresponding media rows.
- Sparse libraries use an intentional centered/anchored composition rather than leaving a large accidental blank area.
- Dense libraries use adaptive media-grid columns based on width and available space.

## Bottom navigation

- Reduce total visual height and horizontal padding while preserving accessible touch targets.
- Active destination uses restrained accent material; inactive destinations remain quiet.
- Navigation should appear attached to the app shell rather than floating as an oversized pill disconnected from content.

## Acceptance

- Verify two-item, empty, medium, and large library states.
- Verify portrait layout on OnePlus Nord N200.
- Verify system-bar insets and gesture-navigation inset behavior.
- Verify search/sort controls and destination switching after the layout changes.
- Physical-device visual acceptance is required before stable promotion.
