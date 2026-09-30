# GoreeCloud Gallery Modification and Provenance Notice

GoreeCloud Gallery is a modified Android gallery application maintained by GoreeCloud.

## Upstream projects

The current `1.0.0-gc.7` acceptance line is reconstructed from these exact upstream revisions:

- Fossify Gallery 1.13.1 — `b28299dc33821eee8d108a9880ce87876cf31443`
- Fossify Commons 6.1.5 — `acfd352df1a1852d17a5f77def8b7ad6e522a5b6`

Upstream authorship, copyright, and project history remain the property of their respective contributors. GoreeCloud rebranding and modification do not erase or replace upstream attribution.

## GoreeCloud modifications

GoreeCloud maintains an ordered `gc.1` through `gc.7` modification chain that currently includes application identity and branding, offline-boundary enforcement, Glaze UI presentation, launcher behavior, removal of inappropriate upstream counterfeit-build messaging from the GoreeCloud build, rounded media presentation, settings/dialog refinements, and toolbar popup contrast corrections.

The accepted historical transformation programs are preserved under `patches/` and are reconstructed and verified by `scripts/materialize-patches.sh`. The exact modified source can be regenerated with `scripts/reconstruct-source.sh` from the pinned upstream revisions.

## License

GoreeCloud Gallery is distributed under the GNU General Public License version 3 as required by the upstream work. The complete upstream GPLv3 license text is copied into produced release/acceptance evidence as `LICENSE-GPL-3.0.txt`, and the corresponding modified source remains reconstructable from this repository plus the pinned upstream source revisions.

This notice is not a replacement for the GNU GPL v3 license terms.

## Product identity

`GoreeCloud`, `GoreeCloud Gallery`, and `Glaze UI` identify the GoreeCloud-maintained product and design work. They do not imply authorship of the original Fossify projects.

## Current release state

`1.0.0-gc.7` is an acceptance candidate, not a stable production release. Stable promotion remains subject to `docs/STABLE-RELEASE-CHECKLIST.md`.