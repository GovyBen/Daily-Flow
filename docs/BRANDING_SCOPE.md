# Branding and Application ID Scope

Decision date: 2026-06-13

## P0 Decisions

- Product name: `Daily Flow`
- Release application ID: `com.dailyflow.app`
- Debug application ID: `com.dailyflow.app.debug`
- Initial Daily Flow version: `0.1.0` (`versionCode` 1)
- Kotlin namespace retained: `com.mhss.app.mybrain`

Retaining the Kotlin namespace avoids a high-risk full-project package rename
and keeps upstream synchronization practical. The namespace is an
implementation detail and does not prevent My Brain and Daily Flow from being
installed together.

## Changed in P0

- App labels and Gradle root project name
- Release/debug application IDs
- Notification completion action
- Internal app deep-link hosts
- README, contribution and privacy placeholders

## Intentionally Retained

- Kotlin package names and source directory layout
- `MyBrainApplication`, `MyBrainDatabase` and theme class names
- Existing Room schema identity
- Upstream launcher artwork as temporary placeholder assets
- Existing export folder names where changing them could affect compatibility

Provider authorities already use `${applicationId}` where applicable. Any
future provider, backup, widget or migration change must be checked against
both release and debug IDs.
