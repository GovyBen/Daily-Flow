# Third-Party Notices

Daily Flow is distributed under the GNU General Public License, version 3.

## My Brain

- Project: My Brain
- Source: https://github.com/mhss1/MyBrain
- Fixed baseline: `a49727cbec0e686edb82f447b2984ecb674d3edf`
- Release tag: `v3.1.0`
- License: GNU GPLv3
- Use in Daily Flow: primary application and Git history baseline

Original copyright and license notices are retained in upstream files.

## Track & Graph

- Project: Track & Graph
- Source: https://github.com/SamAmco/track-and-graph
- Fixed reference: `4bb925a731e0537f6330971853770e9aafb51983`
- Local reference tag: `upstream-trackgraph-20260613`
- License: GNU GPLv3 or later
- Use in Daily Flow: read-only reference for selective future migration

No Track & Graph production file is copied into the Daily Flow worktree during
P0. Each future migrated file must retain its original copyright header and be
listed in `docs/UPSTREAM_PROVENANCE.md` with its source path and modification
summary.

## Build Dependencies

Gradle resolves Android and Kotlin dependencies from Google Maven, Maven
Central, the Gradle Plugin Portal and JitPack as declared by the project.
Dependency and license reports will be generated during DF-801 before release.
