# Daily Flow 提醒现状审计

审计日期：2026-06-15

## 范围

本审计覆盖任务单提醒链路：

- `UpsertTaskUseCase`、`DeleteTaskUseCase`、`UpsertTasksUseCase`
- `core:alarm` 的模型、repository 接口和 use case
- `AlarmSchedulerImpl`
- `AlarmReceiver`、`BootBroadcastReceiver` 和通知 channel
- 任务新增/编辑页面的 exact alarm 权限反馈

当前日历事件没有 Daily Flow 自有提醒，tracking 也没有记录提示。

## 当前数据和调度流程

1. 任务的 `dueDate` 同时是截止时间和唯一提醒时间，`Task.alarmId` 是可空外键式引用。
2. `alarms` 表只有自增 `id` 和 UTC epoch millisecond `time`。
3. 新提醒先插入 `alarms`，Room 返回的自增 ID 同时用于：
   - `Task.alarmId`
   - AlarmManager `PendingIntent` 的 `requestCode`
   - 广播 extra `ALARM_ID_EXTRA`
   - 通知 ID 和“完成”动作的 `requestCode`
4. 修改未来截止时间会用原 `alarmId` upsert，并以相同 `requestCode` 更新
   `PendingIntent`，同时以 `alarm_fallback_<id>` 替换唯一 WorkManager 后备，
   所以单个任务的提醒不会因普通修改而叠加。
5. 移除截止时间、首次完成任务或删除任务会取消 AlarmManager 和 WorkManager，
   再删除 `alarms` 行。取消 intent 使用同一 receiver 和 `requestCode`；extra
   不参与 `PendingIntent` identity，因此能够匹配原提醒。
6. 到点后 `AlarmReceiver` 按 alarm ID 查找任务、发送通知并删除 `alarms` 行。
   WorkManager 后备只在 alarm 行和预期时间仍匹配时复用同一 receiver。
   当前不会清空任务中已经触发的 `alarmId`。

## 时间和时区

- 调度使用 `AlarmManager.RTC_WAKEUP` 和绝对 epoch millisecond，因此普通设备时区
  修改不会改变已经排定的触发瞬间。
- UI 按当前设备时区解释和显示 `dueDate`；时区修改后墙上时间会变化。
- 重复任务完成后，下一截止时间使用完成当时的 `TimeZone.currentSystemDefault()`
  按分钟、小时、日、周、月或年推进，并跳过所有已经过去的周期。
- `TIME_CHANGED`、`TIMEZONE_CHANGED` 和 `DATE_CHANGED` 会触发持久化 alarm 恢复；
  当前绝对 epoch 时间本身不会按新墙上时间重新计算。

## Exact Alarm 和通知权限

- Manifest 声明 `SCHEDULE_EXACT_ALARM`。Android 12 及以上通过
  `AlarmManager.canScheduleExactAlarms()` 检查；更低版本视为可用。
- 可用时调用 `AlarmManagerCompat.setExactAndAllowWhileIdle()`，并安排目标时间
  5 分钟后的 WorkManager 后备。
- 不可用时使用 `AlarmManagerCompat.setAndAllowWhileIdle()` 非精确调度，并安排
  目标时间 15 分钟后的 WorkManager 后备；任务和 alarm ID 仍正常保存。
- 系统调度抛错时会取消部分调度并回滚 alarm 行，任务保存为未关联提醒。
- 任务详情在启用截止时间时仍会提示 exact alarm 授权，但降级调度不依赖用户授权。
- `AndroidPermissionState` 对 `SCHEDULE_ALARMS` 的 `isGranted` 当前恒为 `true`，
  实际调度仍依赖 domain 层检查，返回设置页后的 UI 状态不能准确反映权限。
- Android 13 及以上在主 Activity 启动时请求 `POST_NOTIFICATIONS`。拒绝通知权限
  不会阻止 alarm 被消费和数据库行被删除。
- 应用启动时创建 `Constants.REMINDERS_CHANNEL_ID`，名称和说明来自资源，
  importance 为 `IMPORTANCE_DEFAULT`。

## 重启恢复

- Manifest 声明 `RECEIVE_BOOT_COMPLETED`。唯一的 `BootBroadcastReceiver` 监听
  `BOOT_COMPLETED`、日期/系统时间/时区变化和 `MY_PACKAGE_REPLACED`。
- receiver 只入队唯一 `RestoreAlarmsWorker`；worker 读取 `alarms` 全表并逐条
  重新调用 scheduler，单条失败不阻断其他提醒，并进行有限重试。
- 当前不处理 `LOCKED_BOOT_COMPLETED`。
- 恢复仍不检查提醒是否已过期。
- 没有持久化或核对 AlarmManager 当前状态，无法主动发现丢失的系统 alarm。

## 自动回归基线

`UpsertTaskUseCaseTest` 锁定以下现有行为：

- 新任务使用 Room 生成的 alarm ID 调度并回写任务。
- 修改未来时间复用 alarm ID/request code。
- 移除截止时间和完成任务会取消并删除提醒。
- exact alarm 权限不可用时仍保存并调用降级 scheduler。
- 平台调度抛错时回滚 alarm 行并报告未调度。
- 纯策略测试锁定 exact + 5 分钟后备和 inexact + 15 分钟后备。

运行：

```powershell
.\gradlew.bat :core:alarm:test :tasks:domain:test --no-problems-report
```

## 雷电模拟器复验步骤

1. 安装 debug APK，在任务页创建未来 3 分钟的任务。
2. 用 `adb shell dumpsys alarm` 搜索包名，确认存在 RTC_WAKEUP 广播。
3. 将截止时间改到未来 5 分钟，再次检查仍只有该任务对应的一项。
4. 关闭截止时间，确认 alarm 消失。
5. 再建提醒并重启模拟器，解锁后检查 alarm 被恢复且通知按时出现。
6. 点击通知“完成”，确认任务完成且通知消失。

Android 9 雷电无法覆盖 Android 12 exact alarm 特殊访问和 Android 13 通知权限，
这些场景需要高版本模拟器或真实设备补充。

2026-06-15 实测结果：

- 雷电 Android 9 创建 `DF401_Reminder` 后，`dumpsys alarm` 显示单一
  `RTC_WAKEUP`，tag 指向 `AlarmReceiver`，数据库 alarm ID 和任务 `alarmId`
  均为 1。
- 到点通知通过 `reminders_notification_channel` 发出，通知 ID 为 1，并带“完成”
  动作；alarm 行被删除，任务仍保留 `alarmId=1`，复现了旧引用风险。
- 将同一任务改到未来时间后重新生成 alarm ID 1，且系统复用同一
  `PendingIntentRecord`；没有产生第二个任务提醒。
- 关闭截止时间后，任务 `dueDate=0`、`alarmId=NULL`、alarm 表为空，
  待调度区不再包含 Daily Flow `AlarmReceiver`。
- 全流程日志未发现崩溃、ANR、SQLite 约束或调度权限异常。

DF-402 实测结果：

- 雷电 Android 9 创建 `DF402_Dual` 后，alarm ID 2 同时登记一个精确
  `RTC_WAKEUP` 和唯一 WorkManager 工作 `alarm_fallback_2`；后备延迟为
  目标时间加 5 分钟。
- 带活动提醒覆盖安装 APK 后，`RestoreAlarmsWorker` 成功运行，重新登记同一
  alarm ID/requestCode，并替换后备 work UUID，没有产生重复任务提醒。
- 关闭截止时间后，alarm 表为空、任务 `alarmId=NULL`、WorkManager 后备状态
  为 `CANCELLED`，系统待调度区不再包含 Daily Flow `AlarmReceiver`。
- Koin 启动包含两个 worker 定义；真实流程日志无崩溃、ANR、worker failure
  或调度权限异常。

## 已知风险和后续约束

- Android 12 exact alarm 和 Android 13 notification 权限仍需高版本设备实测。
- `AndroidPermissionState` 仍不能准确反映 exact alarm 特殊访问状态。
- 已触发提醒只删除 alarm 行，不清空 `Task.alarmId`。
- 恢复会重排过期提醒，尚未按目标状态做统一核对或清理。
- WorkManager 后备提供送达安全网，但系统仍可因省电策略延迟执行。
- 单一 `dueDate` 无法表示截止时间与多个绝对/相对提醒，DF-403 起改用统一提醒实体。
