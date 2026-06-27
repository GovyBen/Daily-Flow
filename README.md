# Daily Flow

Daily Flow is an open-source, local-first Android app for tasks, calendar
events, journaling, and structured daily tracking.

The project is currently at the `v0.11.0` Daily Items preview stage. The active
scope and implementation sequence are documented in:

- `docs/DEVELOPMENT_PLAN.md`
- `docs/CODEX_IMPLEMENTATION_PLAN.md`

## Upstream

Daily Flow is based on [My Brain](https://github.com/mhss1/MyBrain) `v3.1.0`
at commit `a49727cbec0e686edb82f447b2984ecb674d3edf`.

[Track & Graph](https://github.com/SamAmco/track-and-graph) at commit
`4bb925a731e0537f6330971853770e9aafb51983` is retained as a read-only
reference for selective, attributed GPL-compatible code reuse. Its complete
application is not merged into Daily Flow.

See `THIRD_PARTY_NOTICES.md` and `docs/UPSTREAM_PROVENANCE.md` for provenance
details.

## Build

Requirements:

- JDK 17
- Android SDK Platform 36
- Android SDK Build-Tools 36.0.0

On Windows:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug :app:assembleDebug
```

When the checkout is stored in a synchronized folder that locks Gradle
intermediate files, use an external local build directory:

```powershell
.\gradlew.bat -PdailyFlow.buildRoot="$env:LOCALAPPDATA\DailyFlow\build" `
  testDebugUnitTest lintDebug :app:assembleDebug
```

The debug APK is generated at
`app/build/outputs/apk/debug/app-debug.apk`.

## License

Daily Flow is licensed under GNU GPLv3. Upstream copyright notices remain in
their original files.
