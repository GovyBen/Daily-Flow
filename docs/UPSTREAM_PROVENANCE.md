# Upstream Provenance

Last updated: 2026-06-13

## Fixed Sources

| Role | Repository | Commit | License | Local reference |
|---|---|---|---|---|
| Primary baseline | https://github.com/mhss1/MyBrain | `a49727cbec0e686edb82f447b2984ecb674d3edf` | GPLv3 | `v3.1.0` |
| Selective migration reference | https://github.com/SamAmco/track-and-graph | `4bb925a731e0537f6330971853770e9aafb51983` | GPLv3-or-later | `upstream-trackgraph-20260613` |

Configured remotes:

- `upstream-mybrain`
- `upstream-trackgraph`

Both fixed commits are available in the local Git object database. Track &
Graph is not a submodule, composite build or runtime dependency.

## P0 Source Classification

| Category | Current use |
|---|---|
| A: My Brain unchanged | Most imported production files |
| B: My Brain modified | Branding, deep links, CI, Gradle download URL, widget lint correction |
| C: Track & Graph migrated | None in P0 |
| D: Daily Flow new code | Planning/provenance documents, test fixtures and provenance report script |
| E: New dependency | None |

## Track & Graph Migration Ledger

No files have been migrated yet. The following fixed-commit paths are approved
for later evaluation:

| Planned task | Upstream path or area | Status |
|---|---|---|
| DF-102 | `app/data/src/main/java/com/samco/trackandgraph/data/database/dto/IDataPoint.kt` | Reference only |
| DF-102 | `app/data/src/main/java/com/samco/trackandgraph/data/sampling/DataSample.kt` | Reference only |
| DF-102 | `app/data/src/main/java/com/samco/trackandgraph/data/sampling/RawDataSample.kt` | Reference only |
| DF-201 | `app/app/src/main/java/com/samco/trackandgraph/ui/compose/ui/DurationInput.kt` | Reference only |
| DF-201 | `app/app/src/main/java/com/samco/trackandgraph/ui/compose/ui/DateTimeSelectorButtons.kt` | Reference only |
| DF-205 | `app/app/src/main/java/com/samco/trackandgraph/adddatapoint/SuggestedValueHelper.kt` | Reference only |
| DF-302 | `app/app/src/main/java/com/samco/trackandgraph/graphstatview/functions/aggregation/` | Reference only |
| DF-306 | `app/data/src/main/java/com/samco/trackandgraph/data/csvreadwriter/CSVReadWriterImpl.kt` | Reference only |
| DF-402 | `app/app/src/main/java/com/samco/trackandgraph/reminders/` | Reference only |

When a file is migrated, add its destination, original path, original
copyright holder, modification date and Daily Flow modification summary here.
