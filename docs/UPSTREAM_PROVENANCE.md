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
| C: Track & Graph migrated | Sampling interfaces listed below |
| D: Daily Flow new code | Planning/provenance documents, test fixtures and provenance report script |
| E: New dependency | None |

## Track & Graph Migration Ledger

The following files have been migrated or approved for later evaluation:

| Planned task | Upstream path or area | Daily Flow destination | Status |
|---|---|---|---|
| DF-102 | `app/data/src/main/java/com/samco/trackandgraph/data/database/dto/IDataPoint.kt` | `tracking/.../analytics/sampling/IDataPoint.kt` | Migrated 2026-06-13; package and timestamp adapted |
| DF-102 | `app/data/src/main/java/com/samco/trackandgraph/data/database/dto/DataPoint.kt` | `tracking/.../analytics/sampling/RawDataPoint.kt` | Adapted 2026-06-13; UUID tracker ID and kotlinx datetime |
| DF-102 | `app/data/src/main/java/com/samco/trackandgraph/data/sampling/DataSampleProperties.kt` | `tracking/.../analytics/sampling/DataSampleProperties.kt` | Migrated 2026-06-13; datetime period adapted |
| DF-102 | `app/data/src/main/java/com/samco/trackandgraph/data/sampling/DataSample.kt` | `tracking/.../analytics/sampling/DataSample.kt` | Migrated 2026-06-13; raw point type adapted |
| DF-102 | `app/data/src/main/java/com/samco/trackandgraph/data/sampling/RawDataSample.kt` | `tracking/.../analytics/sampling/RawDataSample.kt` | Migrated 2026-06-13; raw point type adapted |
| DF-104 | `app/data/src/main/java/com/samco/trackandgraph/data/database/entity/DataPoint.kt` | `tracking/.../data/database/entity/DataPointEntity.kt` | Adapted 2026-06-13; generated ID, session/option references and nullable values |
| DF-104 | `app/data/src/main/java/com/samco/trackandgraph/data/database/dto/DataPoint.kt` | `tracking/.../data/factory/TrackingEntityFactory.kt` | Adapted 2026-06-13; timestamp fields and generated identity |
| DF-201 | `app/app/src/main/java/com/samco/trackandgraph/ui/compose/ui/DurationInput.kt` | - | Reference only |
| DF-201 | `app/app/src/main/java/com/samco/trackandgraph/ui/compose/ui/DateTimeSelectorButtons.kt` | - | Reference only |
| DF-205 | `app/app/src/main/java/com/samco/trackandgraph/adddatapoint/SuggestedValueHelper.kt` | - | Reference only |
| DF-302 | `app/app/src/main/java/com/samco/trackandgraph/graphstatview/functions/aggregation/` | - | Reference only |
| DF-306 | `app/data/src/main/java/com/samco/trackandgraph/data/csvreadwriter/CSVReadWriterImpl.kt` | - | Reference only |
| DF-402 | `app/app/src/main/java/com/samco/trackandgraph/reminders/` | - | Reference only |

When a file is migrated, add its destination, original path, original
copyright holder, modification date and Daily Flow modification summary here.
