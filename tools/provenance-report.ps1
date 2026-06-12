[CmdletBinding()]
param(
    [string]$BaseRef = "v3.1.0"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

& git rev-parse --verify "$BaseRef^{commit}" *> $null
if ($LASTEXITCODE -ne 0) {
    throw "Base ref '$BaseRef' is not available."
}

$changes = @(& git diff --name-status $BaseRef -- .)
if ($LASTEXITCODE -ne 0) {
    throw "Unable to compare the worktree with '$BaseRef'."
}

$untracked = @(& git ls-files --others --exclude-standard)
if ($LASTEXITCODE -ne 0) {
    throw "Unable to list untracked files."
}

$counts = [ordered]@{
    ModifiedMyBrainFiles = 0
    AddedDailyFlowFiles = 0
    DeletedFiles = 0
    RenamedFiles = 0
    UntrackedFiles = $untracked.Count
}

foreach ($change in $changes) {
    $status = ($change -split "`t", 2)[0]
    switch -Regex ($status) {
        "^M" { $counts.ModifiedMyBrainFiles++ }
        "^A" { $counts.AddedDailyFlowFiles++ }
        "^D" { $counts.DeletedFiles++ }
        "^R" { $counts.RenamedFiles++ }
    }
}

[PSCustomObject]$counts

if ($changes.Count -gt 0) {
    "`nTracked changes relative to ${BaseRef}:"
    $changes
}

if ($untracked.Count -gt 0) {
    "`nUntracked files:"
    $untracked
}
