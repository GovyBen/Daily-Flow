# LDPlayer Test Report - 2026-06-27

## Summary

- Test window: 2026-06-27 22:29-22:52 Asia/Shanghai
- Device: LDPlayer 9, `emulator-5556`, 1080x1920 @ 420dpi, Android 9 / API 28
- Package: `com.dailyflow.app.debug`
- APK: `%LOCALAPPDATA%/DailyFlow/build/app/outputs/apk/debug/app-debug.apk`
- Test artifacts: `C:\Users\10844\AppData\Local\DailyFlow\test-artifacts\manual-20260627-222914`
- Source plan: `doc_for_hermes/BETA_TEST_PLAN.md`

The current worktree is no longer the same UI described by the old v0.9 beta plan. The installed build exposes the P9-style navigation:

- `概览`
- `Items`
- `Records`
- `设置`

The old plan expects:

- `概览`
- `日历`
- `空间`
- `设置`

Therefore, exact Calendar/Space tab cases from the old plan could not be executed as written. I mapped the test to the current equivalent surfaces: `Items` for the unified task/calendar-like item flow, and `Records` for custom tracking.

## Build And Install

Result: PASS

- Stopped Baidu sync processes before building.
- Started LDPlayer through `tools/android/ldplayer.ps1 -Action start`.
- Built debug APK with external build/cache paths:
  - `:app:assembleDebug`
  - `-PdailyFlow.buildRoot=%LOCALAPPDATA%/DailyFlow/build`
  - `--project-cache-dir=%LOCALAPPDATA%/DailyFlow/gradle-project-cache`
  - `--no-problems-report`
- Installed with `adb install -r`.
- Launched with `monkey -p com.dailyflow.app.debug -c android.intent.category.LAUNCHER 1`.
- App process remained alive: PID `4132`.

## Navigation

Result: PASS with plan mismatch

- `概览`: opened successfully.
- `Items`: opened successfully.
- `Records`: opened successfully.
- `设置`: opened successfully.

Evidence:

- `test_01a_overview.png`
- `test_01b_items.png`
- `test_01c_records.png`
- `test_01d_settings.png`

Notes:

- The old plan's `日历` and `空间` tabs are absent in this build.
- The selected bottom navigation item renders with a very large icon/indicator, especially on `Items`, and overlaps usable lower content. This is visible across multiple screenshots, including `test_04b_items_all_filter.png`, `test_12a_overview_after_records_and_items.png`, and `test_13c_sync_after_save_or_prompt.png`.

## Overview

Result: PASS

Verified:

- Overview page loads.
- Dashboard shows Custom Tracking panel.
- Dashboard shows Daily Items panel.
- Dashboard shows Overdue panel.
- After creating a tracking record and items, Overview updates:
  - `今天 1 条记录`
  - `Daily Items` shows `LDDueToday`
  - `Overdue` shows `LDDueToday`

Evidence:

- `test_12a_overview_after_records_and_items.png`
- `test_12a_overview_after_records_and_items.xml`

## Daily Items

Result: PASS with UX findings

### Empty State And Filters

Verified:

- `Daily Items` screen opens.
- Search box is present.
- Filter chips are present:
  - `Today`
  - `+/- 7`
  - `Next 7`
  - `Week`
  - `Month`
  - `Overdue`
  - `No date`
  - `Done`
  - `All`
  - `Custom`
- Empty state `No Daily Items` appears before creating items.

Evidence:

- `test_01b_items.png`
- `test_01b_items.xml`

### Create No-Date Item

Result: PASS

Created item:

- Title: `LDTestItem`
- Description: `CreatedByLDPlayer`
- Kind: `TASK`
- Priority: `Low`
- Completable: enabled
- Date: none
- Calendar sync: disabled

Verified:

- Item is not shown in `Today`.
- Item is shown in `No date`.
- Item is shown in `All` while active.

Evidence:

- `test_03a_items_fab_after_tap.png`
- `test_03b_items_form_filled.png`
- `test_04a_items_no_date_filter.png`
- `test_04b_items_all_filter.png`

### Complete Item

Result: PASS

Verified:

- Tapping the item's `Complete` control marks it completed.
- Completed item disappears from the active `All` view.
- Completed item appears in `Done`.
- Completed title is shown with strikethrough.

Evidence:

- `test_05a_items_after_complete_tap.png`
- `test_05b_items_done_filter.png`

UX note:

- `All` currently appears to mean all active items, not all items including completed. This may be intended, but the label is ambiguous because there is also a separate `Done` filter.

### Create Dated Item And Range Filters

Result: PASS

Created item:

- Title: `LDDueToday`
- Due date: 2026-06-27 22:36 Asia/Shanghai
- Calendar sync: disabled

Verified:

- `Today` shows the item.
- `+/- 7` shows the item.
- `Next 7` shows the item.
- `Week` shows the item.
- `Month` shows the item.
- `Overdue` shows the item after the due minute passed during the test.
- `All` shows the active dated item.

Evidence:

- `test_06b_due_item_due_enabled.png`
- `test_07a_filter_today.png`
- `test_07b_filter_surrounding_7.png`
- `test_07c_filter_next_7.png`
- `test_07d_filter_week.png`
- `test_07e_filter_month.png`
- `test_07f_filter_overdue.png`
- `test_07g_filter_all.png`

### Search

Result: PASS

Verified:

- Query `LDDue` shows `LDDueToday`.
- Query `NoHit` shows empty state.

Evidence:

- `test_08a_items_search_match.png`
- `test_08b_items_search_no_match.png`

### Month View Toggle

Result: PASS

Verified:

- Month view opens from the top calendar icon.
- June 2026 grid is shown.
- June 27 shows count `1`.
- Switching back to list view works.

Evidence:

- `test_09a_items_month_view.png`
- `test_09b_items_back_to_list_view.png`

### App -> System Calendar Sync

Result: PASS for crash safety, FAIL for permission UX

Created item:

- Title: `LDSync`
- Due date: 2026-06-27 22:43 Asia/Shanghai
- Calendar sync: enabled

Observed:

- App did not crash.
- Item saved successfully.
- List row displays sync state suffix: `failed`.
- Detail screen shows:
  - `Calendar sync`: `Enabled`
  - `Sync state`: `FAILED`
  - `Last error`: permission denial requiring `READ_CALENDAR` or `WRITE_CALENDAR`.

Evidence:

- `test_13a_sync_form_scrolled.png`
- `test_13c_sync_after_save_or_prompt.png`
- `test_13d_sync_detail.png`
- `test_13d_sync_detail.xml`

Finding:

- On a fresh install, enabling calendar sync does not request runtime calendar permissions before trying to access Calendar Provider.
- The user-facing list only shows `failed`; the detail screen shows a raw Android permission error. This should become a guided permission flow and a friendly error message.

## Records / Custom Tracking

Result: PASS

Verified:

- `Records` tab opens the template list.
- Default templates are visible:
  - `健身`
  - `心情`
  - `习惯次数`
- Opening `健身` shows the quick record form.
- Required multi-choice field `训练部位 *` is present.
- Selecting `胸` and tapping `保存记录` saves successfully.
- Success screen shows `记录已保存` and a session ID:
  - `a47462c6-d685-4f39-b1c5-479a05a2faff`
- Overview updates to `今天 1 条记录`.

Evidence:

- `test_10a_records_tab_again.png`
- `test_10b_records_first_template_open.png`
- `test_11a_records_chest_selected.png`
- `test_11b_records_after_save.png`
- `test_12a_overview_after_records_and_items.png`

## Settings

Result: PASS

Verified:

- Settings page loads.
- Visible settings include:
  - Theme
  - Start page
  - Font
  - Font Size
  - First day of week
  - Block screenshots
  - Lock app
  - Integrations
  - Export/import data
- Integrations subpage opens and returns without crash.

Evidence:

- `test_01d_settings.png`
- `test_02c_after_back_from_integrations.png`

## Logcat

Result: PASS for app crash scan

Log file:

- `logcat_final.txt`

Observed:

- No `FATAL EXCEPTION` for `com.dailyflow.app.debug`.
- No app ANR found.
- App process remained alive after tests.
- One unrelated shell/system `cmd` SIGSEGV appeared while taking early screenshots:
  - PID `4210`, UID `2000(shell)`, process `cmd`
  - Not the app process.
- Calendar sync produced expected permission-denial lines:
  - `Permission Denial: opening provider com.android.providers.calendar.CalendarProvider2 ... requires android.permission.READ_CALENDAR or android.permission.WRITE_CALENDAR`

## Automation Notes

- `uiautomator dump` intermittently failed with `ERROR: could not get idle state` while text fields or Compose surfaces were focused.
- Screenshots were still captured successfully in those cases.
- For future automation stability, add Compose test tags or prefer instrumentation tests for critical flows.

## Findings To Fix Or Review

1. P1 - Calendar sync permission UX is incomplete.
   - Fresh install sync fails because runtime calendar permission is not requested.
   - Detail page exposes raw Android permission text.
   - Recommended fix: request/read/write calendar permission before enabling sync, then retry sync after grant; show a friendly actionable error if denied.

2. P2 - Bottom navigation selected item renders too large and overlaps content.
   - The selected `Items` icon/indicator is visually huge and consumes lower screen space.
   - This materially affects usability on 1080x1920 LDPlayer.

3. P2 - `BETA_TEST_PLAN.md` is stale for the P9 UI.
   - Calendar and Space tab cases no longer map directly to current navigation.
   - Recommended fix: replace old tab coordinates with current `Overview / Items / Records / Settings` flows and add unified item/calendar sync cases.

4. P3 - Mixed-language UI remains in zh-CN environment.
   - Examples: `Daily Items`, `Records`, `Today`, `No date`, `failed`, `Complete item`.
   - Recommended fix: add strings and translations for the new Daily Items surfaces.

5. P3 - `All` filter label may be ambiguous.
   - Completed items are only shown under `Done`, not under `All`.
   - If intended, consider renaming to `Active` / `未完成`; otherwise include completed items in `All`.

## Overall Result

The current P9-style build is runnable and the main mapped flows pass on LDPlayer:

- Launch
- Current bottom navigation
- Overview dashboard refresh
- Daily Items create/filter/search/month/complete flows
- Custom tracking quick-record save
- Settings and Integrations navigation

The build is not ready to call the old beta plan fully passed because the plan is stale and because two user-visible issues remain: calendar sync permission UX and oversized bottom navigation selection rendering.
