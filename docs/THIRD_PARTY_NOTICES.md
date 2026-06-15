# Daily Flow Third-Party Notices

Last updated: 2026-06-15

This document records third-party source and dependencies introduced by the
Daily Flow migration work. It is not yet the exhaustive dependency audit
required by DF-801.

## Source-Derived Code

### Track & Graph

- Repository: <https://github.com/SamAmco/track-and-graph>
- Fixed commit: `4bb925a731e0537f6330971853770e9aafb51983`
- License: GPL-3.0-or-later
- Use: selective adaptation of tracking sampling, aggregation, chart and CSV
  behavior. Track & Graph is not included as a runtime dependency.
- CSV source:
  `app/data/src/main/java/com/samco/trackandgraph/data/csvreadwriter/CSVReadWriterImpl.kt`
- Daily Flow CSV destination:
  `tracking/src/main/java/com/mhss/app/tracking/data/csv/TrackingCsvCodec.kt`
- Modification summary: replaced the feature-oriented CSV with a versioned
  six-table snapshot, Kotlin datetime, UUID string IDs, full-file preview
  validation, row-numbered errors, Room transaction import and Android SAF
  file handling.

The complete file-by-file migration ledger is maintained in
`docs/UPSTREAM_PROVENANCE.md`. Adapted source files retain the applicable GPL
notice and identify Daily Flow modifications.

## Apache-2.0 Libraries

The following libraries are obtained from Maven repositories configured by the
Gradle build. Their SPDX license identifier is `Apache-2.0`; the license text is
available from <https://www.apache.org/licenses/LICENSE-2.0>.

| Library | Version | Use |
|---|---:|---|
| `com.androidplot:androidplot-core` | 1.5.11 | Tracking line, bar and pie charts |
| `com.halfhp.fig:figlib` | 1.0.11 | AndroidPlot transitive dependency |
| `org.apache.commons:commons-csv` | 1.14.1 | Versioned tracking CSV parsing and writing |
| `commons-io:commons-io` | 2.20.0 | Commons CSV transitive dependency |
| `commons-codec:commons-codec` | 1.19.0 | Commons CSV transitive dependency |

Official project pages:

- Apache Commons CSV: <https://commons.apache.org/proper/commons-csv/>
- Apache Commons IO: <https://commons.apache.org/proper/commons-io/>
- Apache Commons Codec: <https://commons.apache.org/proper/commons-codec/>

## Release Follow-Up

DF-801 will generate the exhaustive resolved dependency and license audit,
check repository provenance and F-Droid compatibility, and update this notice
before Beta publication.
