# P0 Progress Report

Date: 2026-06-13

## Completed Tasks

- DF-001: imported My Brain `v3.1.0` as the exact Git baseline
- DF-002: added Track & Graph as a fixed, read-only Git reference
- DF-003: added license and provenance ledgers
- DF-004: established a reproducible local build, test and lint baseline
- DF-007: added deterministic test fixtures and a provenance report script

## In Progress

- DF-005: branding and application ID changes are complete; device coexistence
  and provider checks are deferred because no emulator/device is to be used
  yet.
- DF-006: CI configuration is complete and mirrors local verification; the
  first remote GitHub Actions run requires a Daily Flow repository remote.

## Reuse

- My Brain: entire project baseline at
  `a49727cbec0e686edb82f447b2984ecb674d3edf`
- Track & Graph: no production source copied; fixed commit is available through
  `upstream-trackgraph-20260613`
- Other open-source dependencies: unchanged from My Brain

## New Code and Documents

- Provenance and third-party notices required for GPL source tracking
- Deterministic test clock, zone and ID fixtures
- Lightweight PowerShell provenance report
- Optional external Gradle build root for synchronized-folder reliability

## Verification

- `testDebugUnitTest`: pass
- `lintDebug`: pass
- `:app:assembleDebug`: pass
- APK package and version inspected with Android Build-Tools
- No emulator, adb connection or device operation performed

## Next Entry Condition

Before DF-101 begins, either:

1. run the configured workflow on a GitHub remote and complete DF-006; or
2. explicitly accept local CI-equivalent verification for the engineering
   preview.

Device checks for DF-005 can remain deferred until the separate test-environment
work is approved.
