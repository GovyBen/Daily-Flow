[CmdletBinding()]
param(
    [int]$Index = 1,
    [string]$InstanceName = "Daily Flow Test",
    [string]$Serial = "emulator-5556",
    [string]$LdPlayerHome = "C:\leidian\LDPlayer9",
    [string]$BuildRoot = "",
    [string]$ArtifactRoot = "",
    [int]$ObservationSeconds = 10,
    [switch]$SkipBuild
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "env.ps1") -Quiet

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path
$gradle = Join-Path $repoRoot "gradlew.bat"
$adb = Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe"
$packageName = "com.dailyflow.app.debug"
$mainActivity = "com.mhss.app.mybrain.presentation.main.MainActivity"

if (-not $BuildRoot) {
    $BuildRoot = Join-Path $env:LOCALAPPDATA "DailyFlow\build"
}
if (-not $ArtifactRoot) {
    $ArtifactRoot = Join-Path $env:LOCALAPPDATA "DailyFlow\test-artifacts"
}

$buildRootPath = [System.IO.Path]::GetFullPath($BuildRoot)
$artifactRootPath = [System.IO.Path]::GetFullPath($ArtifactRoot)
$apk = Join-Path $buildRootPath "app\outputs\apk\debug\app-debug.apk"
$runDirectory = Join-Path $artifactRootPath (Get-Date -Format "yyyyMMdd-HHmmss")

function Invoke-Native {
    param(
        [string]$Stage,
        [string]$FilePath,
        [string[]]$Arguments
    )

    Write-Host "[$Stage] $FilePath $($Arguments -join ' ')"
    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$Stage failed with exit code $LASTEXITCODE."
    }
}

if (-not $SkipBuild) {
    $projectCacheDir = Join-Path $env:LOCALAPPDATA "DailyFlow\gradle-project-cache"
    Invoke-Native -Stage "build" -FilePath $gradle -Arguments @(
        "-PdailyFlow.buildRoot=$buildRootPath",
        "--project-cache-dir=$projectCacheDir",
        "--no-daemon",
        "--console=plain",
        ":app:assembleDebug"
    )
}

if (-not (Test-Path -LiteralPath $apk)) {
    throw "Debug APK was not found at '$apk'."
}

& (Join-Path $PSScriptRoot "ldplayer.ps1") start `
    -Index $Index `
    -InstanceName $InstanceName `
    -Serial $Serial `
    -LdPlayerHome $LdPlayerHome |
    Format-List |
    Out-Host

Invoke-Native -Stage "install" -FilePath $adb -Arguments @(
    "-s", $Serial, "install", "-r", $apk
)
Invoke-Native -Stage "clear-logcat" -FilePath $adb -Arguments @(
    "-s", $Serial, "logcat", "-c"
)
Invoke-Native -Stage "force-stop" -FilePath $adb -Arguments @(
    "-s", $Serial, "shell", "am", "force-stop", $packageName
)

$startOutput = @(& $adb -s $Serial shell am start -W -n "$packageName/$mainActivity" 2>&1)
if ($LASTEXITCODE -ne 0 -or -not ($startOutput -match "^Status:\s+ok$")) {
    throw "launch failed: $($startOutput -join [Environment]::NewLine)"
}
$startOutput | ForEach-Object { Write-Host $_ }

Start-Sleep -Seconds $ObservationSeconds

New-Item -ItemType Directory -Path $runDirectory -Force *> $null
$logcatPath = Join-Path $runDirectory "logcat.txt"
$packagePath = Join-Path $runDirectory "package.txt"
$activityPath = Join-Path $runDirectory "activity.txt"
$alarmPath = Join-Path $runDirectory "alarm.txt"
$notificationPath = Join-Path $runDirectory "notification.txt"

@(& $adb -s $Serial logcat -d -v threadtime) |
    Set-Content -LiteralPath $logcatPath -Encoding UTF8
@(& $adb -s $Serial shell dumpsys package $packageName) |
    Set-Content -LiteralPath $packagePath -Encoding UTF8
@(& $adb -s $Serial shell dumpsys activity activities) |
    Set-Content -LiteralPath $activityPath -Encoding UTF8
@(& $adb -s $Serial shell dumpsys alarm) |
    Set-Content -LiteralPath $alarmPath -Encoding UTF8
@(& $adb -s $Serial shell dumpsys notification) |
    Set-Content -LiteralPath $notificationPath -Encoding UTF8

$logcat = Get-Content -Raw -LiteralPath $logcatPath
$sensitivePattern = "(?i)authorization\s*:|bearer\s+[a-z0-9._-]+|api[_-]?key\s*[:=]|sk-[a-z0-9]{12,}"
if ($logcat -match $sensitivePattern) {
    Remove-Item -LiteralPath $logcatPath -Force
    throw "Logcat matched a possible secret pattern. The logcat file was removed."
}

$crashPattern = "(?im)FATAL EXCEPTION|ANR in $([regex]::Escape($packageName))|Process:\s*$([regex]::Escape($packageName)).*SecurityException"
$crashes = [regex]::Matches($logcat, $crashPattern)
$appProcessId = (& $adb -s $Serial shell pidof $packageName 2>$null).Trim()
$resumed = Select-String -LiteralPath $activityPath -Pattern $packageName -SimpleMatch |
    Select-Object -First 1

if (-not $appProcessId) {
    throw "The app process is not running after the observation period. Logs: '$runDirectory'."
}
if ($null -eq $resumed) {
    throw "The app is not present in the activity state. Logs: '$runDirectory'."
}
if ($crashes.Count -gt 0) {
    throw "Crash, ANR, or SecurityException markers were found. Logs: '$runDirectory'."
}

[PSCustomObject]@{
    Result = "PASS"
    Package = $packageName
    Activity = $mainActivity
    Device = $Serial
    ProcessId = $appProcessId
    Apk = $apk
    Artifacts = $runDirectory
}
