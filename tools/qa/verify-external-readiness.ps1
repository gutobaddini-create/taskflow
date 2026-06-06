param(
    [switch]$Strict
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "../..")
Set-Location $root

$checks = New-Object System.Collections.Generic.List[object]

function Add-Check {
    param(
        [string]$Name,
        [string]$Status,
        [string]$Evidence,
        [string]$NextStep
    )

    $checks.Add([ordered]@{
        name = $Name
        status = $Status
        evidence = $Evidence
        nextStep = $NextStep
    }) | Out-Null
}

function Invoke-Capture {
    param([scriptblock]$Command)

    try {
        $output = & $Command 2>&1
        return @{
            exitCode = if ($null -eq $global:LASTEXITCODE) { 0 } else { $global:LASTEXITCODE }
            output = ($output | Out-String).Trim()
        }
    } catch {
        return @{
            exitCode = 1
            output = $_.Exception.Message
        }
    }
}

$remote = Invoke-Capture { git remote -v }
if ($remote.output) {
    Add-Check "GitHub remote" "ready" $remote.output "Push the current branch and open/publish the repository."
} else {
    Add-Check "GitHub remote" "missing" "git remote -v returned no remote." "Run: git remote add origin <repo-url>"
}

$gh = Invoke-Capture { gh auth status }
if ($gh.exitCode -eq 0) {
    Add-Check "GitHub CLI auth" "ready" $gh.output "Use gh or the GitHub connector to push/open a PR."
} else {
    Add-Check "GitHub CLI auth" "missing" $gh.output "Run: gh auth login"
}

$googleServices = Get-ChildItem -Path $root -Recurse -File -Filter "google-services.json" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($googleServices) {
    Add-Check "Firebase config" "ready" $googleServices.FullName "Enable real Firebase integration and run real-project validation."
} else {
    Add-Check "Firebase config" "missing" "No google-services.json found under the project." "Add app/google-services.json from the Firebase Android app configuration."
}

$adbCandidates = @(
    "C:\TaskFlowAndroidSdk\platform-tools\adb.exe",
    "adb"
)
$adb = $null
foreach ($candidate in $adbCandidates) {
    if ($candidate -eq "adb") {
        $cmd = Get-Command adb -ErrorAction SilentlyContinue
        if ($cmd) {
            $adb = $cmd.Source
            break
        }
    } elseif (Test-Path $candidate) {
        $adb = $candidate
        break
    }
}

if ($adb) {
    $adbDevices = Invoke-Capture { & $adb devices -l }
    $physical = ($adbDevices.output -split "`r?`n") | Where-Object {
        $_ -match "\sdevice\s" -and $_ -notmatch "emulator-" -and $_ -notmatch "^List of devices"
    }
    if ($physical) {
        Add-Check "Physical Android device" "ready" ($physical -join [Environment]::NewLine) "Install release build and capture physical-device QA evidence."
    } else {
        Add-Check "Physical Android device" "missing" $adbDevices.output "Connect an unlocked phone with USB debugging authorized."
    }
} else {
    Add-Check "Physical Android device" "missing" "adb not found." "Install Android platform-tools or keep C:\TaskFlowAndroidSdk available."
}

$keystorePath = $env:TASKFLOW_RELEASE_STORE_FILE
$signingVars = @(
    "TASKFLOW_RELEASE_STORE_FILE",
    "TASKFLOW_RELEASE_STORE_PASSWORD",
    "TASKFLOW_RELEASE_KEY_ALIAS",
    "TASKFLOW_RELEASE_KEY_PASSWORD"
)
$missingSigningVars = $signingVars | Where-Object { [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_)) }
if ($keystorePath -and (Test-Path $keystorePath) -and $missingSigningVars.Count -eq 0) {
    Add-Check "Production signing keystore" "ready" $keystorePath "Rebuild release artifacts and regenerate the release manifest."
} else {
    $evidence = if ($keystorePath -and -not (Test-Path $keystorePath)) {
        "TASKFLOW_RELEASE_STORE_FILE points to a missing file: $keystorePath"
    } elseif ($missingSigningVars.Count -gt 0) {
        "Missing signing variable(s): $($missingSigningVars -join ', ')"
    } else {
        "Production signing variables are incomplete."
    }
    Add-Check "Production signing keystore" "missing" $evidence "Set TASKFLOW_RELEASE_STORE_FILE, TASKFLOW_RELEASE_STORE_PASSWORD, TASKFLOW_RELEASE_KEY_ALIAS, and TASKFLOW_RELEASE_KEY_PASSWORD."
}

Write-Host ""
Write-Host "TaskFlow external readiness"
Write-Host ""

foreach ($check in $checks) {
    Write-Host "[$($check.status)] $($check.name)"
    Write-Host "  Evidence: $($check.evidence)"
    Write-Host "  Next: $($check.nextStep)"
    Write-Host ""
}

$missing = $checks | Where-Object { $_.status -ne "ready" }
if ($Strict -and $missing.Count -gt 0) {
    throw "External readiness failed: $($missing.Count) missing input(s)."
}

if ($missing.Count -gt 0) {
    Write-Host "External readiness incomplete: $($missing.Count) missing input(s)."
} else {
    Write-Host "External readiness complete."
}
