[CmdletBinding()]
param(
    [Parameter(Position = 0)]
    [ValidateSet("status", "start", "connect", "stop")]
    [string]$Action = "status",
    [int]$Index = 1,
    [string]$InstanceName = "Daily Flow Test",
    [string]$Serial = "emulator-5556",
    [string]$Endpoint = "",
    [string]$LdPlayerHome = "C:\leidian\LDPlayer9",
    [int]$TimeoutSeconds = 240
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. (Join-Path $PSScriptRoot "env.ps1") -Quiet

$ldConsole = Join-Path $LdPlayerHome "ldconsole.exe"
$adb = Join-Path $env:ANDROID_SDK_ROOT "platform-tools\adb.exe"

if (-not (Test-Path -LiteralPath $ldConsole)) {
    throw "LDPlayer console was not found at '$ldConsole'."
}

function Get-LdInstance {
    $lines = @(& $ldConsole list2 2>&1)
    if ($LASTEXITCODE -ne 0) {
        throw "LDPlayer list2 failed: $($lines -join [Environment]::NewLine)"
    }

    foreach ($line in $lines) {
        $fields = $line -split ",", 10
        if ($fields.Count -lt 10) {
            continue
        }
        if ([int]$fields[0] -ne $Index) {
            continue
        }

        return [PSCustomObject]@{
            Index = [int]$fields[0]
            Name = $fields[1]
            AndroidStarted = $fields[4] -eq "1"
            PlayerProcessId = [int]$fields[5]
            VirtualMachineProcessId = [int]$fields[6]
            Width = [int]$fields[7]
            Height = [int]$fields[8]
            Dpi = [int]$fields[9]
        }
    }

    throw "LDPlayer instance index $Index was not found."
}

function Assert-ExpectedInstance {
    param([object]$Instance)

    if ($Instance.Name -ne $InstanceName) {
        throw "Instance index $Index is '$($Instance.Name)', not '$InstanceName'."
    }
}

function Get-AdbDeviceState {
    $lines = @(& $adb devices)
    if ($LASTEXITCODE -ne 0) {
        throw "ADB devices failed."
    }

    $serialPattern = [regex]::Escape($Serial)
    foreach ($line in $lines) {
        if ($line -match "^${serialPattern}\s+(\S+)") {
            return $Matches[1]
        }
    }
    return "missing"
}

function Wait-ForAndroid {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $instance = Get-LdInstance
        Assert-ExpectedInstance $instance
        if ($instance.AndroidStarted) {
            return $instance
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    throw "LDPlayer instance '$InstanceName' did not finish booting within $TimeoutSeconds seconds."
}

function Wait-ForAdb {
    & $adb start-server *> $null
    if ($LASTEXITCODE -ne 0) {
        throw "Unable to start the official ADB server."
    }

    if ($Endpoint) {
        $connectOutput = @(& $adb connect $Endpoint 2>&1)
        if ($LASTEXITCODE -ne 0) {
            throw "ADB connect to '$Endpoint' failed: $($connectOutput -join [Environment]::NewLine)"
        }
    }

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        if ((Get-AdbDeviceState) -eq "device") {
            $bootCompleted = (& $adb -s $Serial shell getprop sys.boot_completed 2>$null).Trim()
            if ($LASTEXITCODE -eq 0 -and $bootCompleted -eq "1") {
                return
            }
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)

    throw "ADB device '$Serial' was not ready within $TimeoutSeconds seconds."
}

function Get-Endpoint {
    param([object]$Instance)

    if ($Endpoint) {
        return $Endpoint
    }
    if ($Instance.VirtualMachineProcessId -le 0) {
        return ""
    }

    $connection = Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue |
        Where-Object {
            $_.OwningProcess -eq $Instance.VirtualMachineProcessId -and
            $_.LocalPort -ge 5555 -and
            $_.LocalPort -le 5680
        } |
        Sort-Object LocalPort |
        Select-Object -First 1

    if ($null -eq $connection) {
        return ""
    }
    return "127.0.0.1:$($connection.LocalPort)"
}

function Write-Status {
    $instance = Get-LdInstance
    Assert-ExpectedInstance $instance
    [PSCustomObject]@{
        Index = $instance.Index
        Name = $instance.Name
        AndroidStarted = $instance.AndroidStarted
        Resolution = "$($instance.Width)x$($instance.Height)@$($instance.Dpi)"
        PlayerProcessId = $instance.PlayerProcessId
        VirtualMachineProcessId = $instance.VirtualMachineProcessId
        AdbPath = $adb
        AdbSerial = $Serial
        AdbState = Get-AdbDeviceState
        AdbEndpoint = Get-Endpoint $instance
    }
}

switch ($Action) {
    "status" {
        Write-Status
    }
    "start" {
        $instance = Get-LdInstance
        Assert-ExpectedInstance $instance
        if (-not $instance.AndroidStarted) {
            & $ldConsole launch --index $Index
            if ($LASTEXITCODE -ne 0) {
                throw "Unable to launch LDPlayer instance '$InstanceName'."
            }
        }
        Wait-ForAndroid *> $null
        Wait-ForAdb
        Write-Status
    }
    "connect" {
        $instance = Wait-ForAndroid
        Assert-ExpectedInstance $instance
        Wait-ForAdb
        Write-Status
    }
    "stop" {
        $instance = Get-LdInstance
        Assert-ExpectedInstance $instance
        if ($instance.PlayerProcessId -gt 0) {
            & $ldConsole quit --index $Index
            if ($LASTEXITCODE -ne 0) {
                throw "Unable to stop LDPlayer instance '$InstanceName'."
            }

            $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
            do {
                Start-Sleep -Seconds 1
                $instance = Get-LdInstance
                Assert-ExpectedInstance $instance
                if ($instance.PlayerProcessId -le 0) {
                    break
                }
            } while ((Get-Date) -lt $deadline)

            if ($instance.PlayerProcessId -gt 0) {
                throw "LDPlayer instance '$InstanceName' did not stop within $TimeoutSeconds seconds."
            }
        }
        Write-Status
    }
}
