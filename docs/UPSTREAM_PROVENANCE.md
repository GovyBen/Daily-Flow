# Upstream Provenance

Last updated: 2026-06-15

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
| E: New dependency | AndroidPlot 1.5.11, figlib 1.0.11, Commons CSV 1.14.1, Commons IO 2.20.0 and Commons Codec 1.19.0; all Apache-2.0 |

## Track & Graph Migration Ledger

The following files have been migrated or approved for later evaluation:

| Planned task | Upstream path or area | Daily Flow destination | Status |
|---|---|---|---|
| DF-102 | `app/data/src/main/java/com/samco/trackandgraph/data/database/dto/IDataPoint.kt` | `tracking/.../analytics/sampling/IDataPoint.kt` | Migrated 2026-06-13; package and timestamp adapted |
| DF-102 | `app/data/src/main/java/com/samco/trackandgraph/data/database/dto/DataPoint.kt` | `tracking/.../analytics/sampling/RawDataPoint.kt` | Adapted 2026-06-13; UUID tracker ID and kotlinx datetime |
| DF-102 | `app/data/src/main/java/com/samco/trackandgraph/data/sampling/DataSampleProperties.kt` | `tracking/.../analytics/sampling/DataSampleProperties.kt` | Migrated 2026-06-13; datetime period adapted |
| DF-102 | `app/data/src/main/java/com/samco/trackandgraph/data/sampling/DataSample.kt` | `tracking/.../analytics/sampling/DataSample.kt` | Migrated 2026-06-13; raw point type adapted |
| DF-102 | `app/data/src/main/java/com/samco/trackandgraph/data/sampling/RawDataSample.kt` | `tracking/.../analytics/sampling/RawDataSample.kt` | Migrated 2026-06-13; raw point type adapted |
| DF-104 | `app/data/src/main/java/com/samco/trackandgraph/data/database/entity/DataPoint.kt` | `core/database/.../tracking/data/database/entity/DataPointEntity.kt` | Adapted 2026-06-13; generated ID, session/option references and nullable values; persistence contract moved to the main database module in DF-109 |
| DF-201 | `app/app/src/main/java/com/samco/trackandgraph/ui/compose/ui/DurationInput.kt` | `tracking/.../presentation/components/DurationInput.kt` | Adapted 2026-06-13; ViewModel and upstream mini text field replaced with a controlled Material 3 seconds input |
| DF-201 | `app/app/src/main/java/com/samco/trackandgraph/ui/compose/ui/DateTimeSelectorButtons.kt` | `tracking/.../presentation/components/DateTimeSelectorButtons.kt` | Adapted 2026-06-13; reuses Daily Flow core dialogs and epoch-millisecond storage |
| DF-104 | `app/data/src/main/java/com/samco/trackandgraph/data/database/dto/DataPoint.kt` | `tracking/.../data/factory/TrackingEntityFactory.kt` | Adapted 2026-06-13; timestamp fields and generated identity |
| DF-201 | `app/app/src/main/java/com/samco/trackandgraph/ui/compose/ui/DurationInput.kt` | - | Reference only |
| DF-201 | `app/app/src/main/java/com/samco/trackandgraph/ui/compose/ui/DateTimeSelectorButtons.kt` | - | Reference only |
| DF-203 | `app/app/src/main/java/com/samco/trackandgraph/adddatapoint/SuggestedValueHelper.kt` | `tracking/.../domain/suggestion/SuggestedValueHelper.kt` | Adapted 2026-06-13; repository-backed recent/frequent/default groups, stable ties, sealed inputs, counter increment and text privacy limit |
| DF-206 | `app/app/src/main/java/com/samco/trackandgraph/adddatapoint/AddDataPointsView.kt` and `AddDataPointsViewModel.kt` | `tracking/.../presentation/record/` | Reference only 2026-06-13; Daily Flow uses one scrollable template form, explicit single-flight save state and repository session results |
| DF-301 | `app/app/src/main/java/com/samco/trackandgraph/graphstatview/functions/aggregation/FixedBinAggregator.kt` | `tracking/.../analytics/aggregation/FixedBinAggregator.kt` | Adapted 2026-06-14; explicit operations, Kotlin datetime calendar bins and newest-first validation |
| DF-301 | `app/app/src/main/java/com/samco/trackandgraph/graphstatview/functions/aggregation/MovingAggregator.kt` | `tracking/.../analytics/aggregation/MovingAggregator.kt` | Adapted 2026-06-14; Kotlin duration and sum/count/average/min/max operations |
| DF-301 | `app/app/src/main/java/com/samco/trackandgraph/graphstatview/functions/helpers/TimeHelper.kt` | `tracking/.../analytics/aggregation/TimeBinHelper.kt` | Adapted 2026-06-14; scoped to DST-safe day/week/month bins |
| DF-301 | `app/app/src/main/java/com/samco/trackandgraph/graphstatview/functions/aggregation/AggregationCommon.kt` and `AggregationPreferences.kt` | `tracking/.../analytics/aggregation/AggregationModels.kt` | Adapted 2026-06-14; explicit time zone and supported operation factory |
| DF-301 | `app/app/src/main/java/com/samco/trackandgraph/graphstatview/functions/data_sample_functions/` filter, composite and function contracts | `tracking/.../analytics/aggregation/DataSampleFunction.kt` and `FilterFunctions.kt` | Adapted 2026-06-14; retained lazy filtering, composition and disposal semantics |
| DF-301 | upstream moving average, time helper and filter tests | `tracking/.../analytics/aggregation/AggregatorTest.kt` and `TimeBinHelperTest.kt` | Adapted 2026-06-14; added Daily Flow ordering, operation and time-zone boundaries |
| DF-302 | No direct upstream equivalent; Daily Flow `TrackingRecordedPoint` and tracker types | `tracking/.../analytics/sampling/TrackingDataSampleAdapter.kt` | New glue 2026-06-14; isolates Room, preserves snapshots, groups selections, excludes text and defines boolean count/ratio semantics |
| DF-303 | No direct upstream equivalent; composes DF-301 aggregation and DF-302 sampling | `tracking/.../analytics/model/`, `repository/` and `usecase/` | New Daily Flow code 2026-06-14; range-only repository, DST-safe summaries/series, option distribution and streak queries |
| DF-304 | `graphstatview/ui/LineGraphView.kt`, `BarChartView.kt`, `PieChartView.kt` and `GraphStatUICommon.kt` | `tracking/.../presentation/analytics/chart/TrackingCharts.kt` | Adapted 2026-06-14; reduced to reusable Compose `AndroidView` wrappers with Daily Flow models, theme colors, empty state, dynamic font sizing and accessibility summaries |
| DF-304 | Track & Graph AndroidPlot dependency selection | `com.androidplot:androidplot-core:1.5.11` | Selected 2026-06-14 from Maven Central; Apache-2.0, transitive `com.halfhp.fig:figlib:1.0.11` also Apache-2.0; R8 consumer rule retained |
| DF-306 | `app/data/src/main/java/com/samco/trackandgraph/data/csvreadwriter/CSVReadWriterImpl.kt` and CSV tests | `tracking/.../data/csv/TrackingCsvCodec.kt` and `TrackingCsvCodecTest.kt` | Adapted 2026-06-15; versioned six-table snapshot, UTF-8/BOM, Kotlin datetime, full-file preview validation, row-numbered errors and Daily Flow entities |
| DF-306 | Track & Graph Commons CSV dependency selection | `org.apache.commons:commons-csv:1.14.1` | Retained 2026-06-15; Apache-2.0, resolved Commons IO 2.20.0 and Commons Codec 1.19.0 also Apache-2.0; precise R8 annotation warning rule added |
| DF-306 | No direct upstream equivalent; Daily Flow Room aggregate and Android SAF | `tracking/.../data/csv/TrackingCsvSnapshotStore.kt`, `TrackingCsvManager.kt` and `presentation/csv/` | New Daily Flow integration 2026-06-15; consistent snapshot, transactional upsert, preview confirmation, custom CSV MIME picker and bilingual UI |
| DF-402 | `reminders/PlatformScheduler.kt`, `androidplatform/AndroidPlatformScheduler.kt`, `ReminderFallbackWorker.kt`, `EnsureAlarmsWorker.kt` and `RecreateAlarmsBroadcastReceiver.kt` | `core/alarm/.../AlarmSchedulePlan.kt`, `core/notification/.../worker/`, existing receivers and `app/.../AlarmSchedulerImpl.kt` | Adapted 2026-06-15; retained Daily Flow scheduler/notification/boot infrastructure, added exact/inexact AlarmManager selection, unique WorkManager fallback, package/time restoration, Koin workers and rollback-safe scheduling |
| DF-403 | No direct upstream equivalent; extends the existing My Brain alarm ID pattern and main Room database | `core/alarm/.../Reminder.kt`, `ReminderRepository.kt`, `core/database/.../ReminderEntity.kt`, DAO/migration/schema and `app/.../ReminderRepositoryImpl.kt` | New Daily Flow glue 2026-06-15; unified target model, mutually exclusive absolute/relative trigger validation, persisted-ID request codes and non-destructive v7→v8 migration |

When a file is migrated, add its destination, original path, original
copyright holder, modification date and Daily Flow modification summary here.
