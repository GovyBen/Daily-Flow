# Contributing to Daily Flow

Daily Flow follows the task sequence in `docs/CODEX_IMPLEMENTATION_PLAN.md`.
Keep changes scoped to one `DF-` task whenever practical.

Before submitting a change:

1. Preserve existing My Brain copyright and GPL notices.
2. Record any Track & Graph source path and fixed commit in
   `docs/UPSTREAM_PROVENANCE.md`.
3. Do not introduce Hilt, a second database, a second network stack, closed
   SDKs, analytics, or advertising.
4. Add tests for behavior changes.
5. Run:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug :app:assembleDebug
git diff --check
```

Commit subjects should use `DF-<number> <imperative summary>`. Commit bodies
should list reused upstream files, new code and verification commands.
