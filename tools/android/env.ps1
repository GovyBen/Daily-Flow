[CmdletBinding()]
param(
    [string]$JdkHome = "",
    [string]$SdkRoot = "",
    [switch]$Quiet
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not $JdkHome) {
    $JdkHome = [Environment]::GetEnvironmentVariable("JAVA_HOME", "User")
}
if (-not $JdkHome) {
    $JdkHome = "C:\Program Files\Java\jdk-17"
}

if (-not $SdkRoot) {
    $SdkRoot = [Environment]::GetEnvironmentVariable("ANDROID_SDK_ROOT", "User")
}
if (-not $SdkRoot) {
    $SdkRoot = [Environment]::GetEnvironmentVariable("ANDROID_HOME", "User")
}
if (-not $SdkRoot) {
    $SdkRoot = Join-Path $env:LOCALAPPDATA "Android\Sdk"
}

$JdkHome = [System.IO.Path]::GetFullPath($JdkHome)
$SdkRoot = [System.IO.Path]::GetFullPath($SdkRoot)
$java = Join-Path $JdkHome "bin\java.exe"
$adb = Join-Path $SdkRoot "platform-tools\adb.exe"
$sdkManager = Join-Path $SdkRoot "cmdline-tools\latest\bin\sdkmanager.bat"
$gradleWrapper = Join-Path (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path "gradlew.bat"

$requiredPaths = [ordered]@{
    "JDK 17" = $JdkHome
    "Java" = $java
    "Android SDK" = $SdkRoot
    "Platform Tools" = (Join-Path $SdkRoot "platform-tools")
    "ADB" = $adb
    "SDK Manager" = $sdkManager
    "Android 36 Platform" = (Join-Path $SdkRoot "platforms\android-36")
    "Build Tools 36.0.0" = (Join-Path $SdkRoot "build-tools\36.0.0")
    "Gradle Wrapper" = $gradleWrapper
}

foreach ($entry in $requiredPaths.GetEnumerator()) {
    if (-not (Test-Path -LiteralPath $entry.Value)) {
        throw "$($entry.Key) was not found at '$($entry.Value)'."
    }
}

$env:JAVA_HOME = $JdkHome
$env:ANDROID_HOME = $SdkRoot
$env:ANDROID_SDK_ROOT = $SdkRoot

$pathEntries = @(
    (Join-Path $JdkHome "bin"),
    (Join-Path $SdkRoot "platform-tools"),
    (Join-Path $SdkRoot "cmdline-tools\latest\bin")
)

$currentPath = @($env:Path -split ";" | Where-Object { $_ })
$reversedPathEntries = @($pathEntries)
[array]::Reverse($reversedPathEntries)
foreach ($entry in $reversedPathEntries) {
    $currentPath = @($currentPath | Where-Object {
        $_.TrimEnd("\") -ine $entry.TrimEnd("\")
    })
    $currentPath = @($entry) + $currentPath
}
$env:Path = $currentPath -join ";"

$previousErrorActionPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
try {
    $javaVersion = @(& $java -version 2>&1)
    $javaExitCode = $LASTEXITCODE
}
finally {
    $ErrorActionPreference = $previousErrorActionPreference
}
if ($javaExitCode -ne 0) {
    throw "Unable to run Java from '$java'."
}
if ($javaVersion[0] -notmatch '"17\.') {
    throw "Daily Flow requires JDK 17, but '$($javaVersion[0])' was found."
}

$adbVersion = @(& $adb version 2>&1)
if ($LASTEXITCODE -ne 0) {
    throw "Unable to run ADB from '$adb'."
}

if (-not $Quiet) {
    [PSCustomObject]@{
        JavaHome = $JdkHome
        JavaVersion = $javaVersion[0]
        AndroidSdkRoot = $SdkRoot
        AdbPath = $adb
        AdbVersion = ($adbVersion | Select-Object -First 2) -join " | "
        SdkManagerPath = $sdkManager
        GradleWrapper = $gradleWrapper
    }
}
