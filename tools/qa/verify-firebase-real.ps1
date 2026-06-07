param(
    [string]$ProjectId = "gen-lang-client-0780081219",
    [switch]$Strict
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "../..")
Set-Location $root

function Add-Result {
    param(
        [System.Collections.Generic.List[object]]$Results,
        [string]$Name,
        [string]$Status,
        [string]$Evidence,
        [string]$NextStep = ""
    )

    $Results.Add([ordered]@{
        name = $Name
        status = $Status
        evidence = $Evidence
        nextStep = $NextStep
    }) | Out-Null
}

function Invoke-Capture {
    param([scriptblock]$Command)

    try {
        $script:LASTEXITCODE = 0
        $output = & $Command 2>&1
        return @{
            exitCode = if ($null -eq $LASTEXITCODE) { 0 } else { $LASTEXITCODE }
            output = ($output | Out-String).Trim()
        }
    } catch {
        return @{
            exitCode = 1
            output = $_.Exception.Message
        }
    }
}

$results = New-Object System.Collections.Generic.List[object]

$login = Invoke-Capture { npx firebase login:list }
if ($login.exitCode -eq 0 -and $login.output -match "Logged in as") {
    Add-Result $results "Firebase CLI auth" "ready" $login.output
} else {
    Add-Result $results "Firebase CLI auth" "missing" $login.output "Run: npx firebase login"
}

$googleServicesPath = Join-Path $root "app/google-services.json"
if (Test-Path $googleServicesPath) {
    $config = Get-Content $googleServicesPath -Raw | ConvertFrom-Json
    $apiKey = $config.client[0].api_key[0].current_key
    $appId = $config.client[0].client_info.mobilesdk_app_id
    Add-Result $results "Android Firebase config" "ready" "Project: $($config.project_info.project_id); App: $appId"
} else {
    Add-Result $results "Android Firebase config" "missing" "app/google-services.json not found." "Download the Android app config from Firebase."
}

if ($apiKey) {
    $email = "taskflow-smoke-delete-$(Get-Date -Format yyyyMMddHHmmss)@example.com"
    $password = "TaskFlow!$(Get-Random -Minimum 100000 -Maximum 999999)"
    $body = @{ email = $email; password = $password; returnSecureToken = $true } | ConvertTo-Json
    try {
        $created = Invoke-RestMethod -Method Post -Uri "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey" -ContentType "application/json" -Body $body
        $deleteBody = @{ idToken = $created.idToken } | ConvertTo-Json
        Invoke-RestMethod -Method Post -Uri "https://identitytoolkit.googleapis.com/v1/accounts:delete?key=$apiKey" -ContentType "application/json" -Body $deleteBody | Out-Null
        Add-Result $results "Firebase Auth email/password" "ready" "Created and deleted smoke user $email."
    } catch {
        $message = $_.ErrorDetails.Message
        if (-not $message) { $message = $_.Exception.Message }
        Add-Result $results "Firebase Auth email/password" "missing" $message "Enable Email/Password sign-in provider in Firebase Authentication."
    }
}

$firestore = Invoke-Capture { npx firebase deploy --only firestore:rules --dry-run --project $ProjectId --json }
if ($firestore.exitCode -eq 0) {
    Add-Result $results "Firestore rules dry-run" "ready" $firestore.output
} elseif ($firestore.output -match "requires billing to be enabled") {
    Add-Result $results "Firestore rules dry-run" "blocked" $firestore.output "Enable billing for project $ProjectId, then rerun this script."
} else {
    Add-Result $results "Firestore rules dry-run" "missing" $firestore.output "Create the Firestore database and rerun this script."
}

$storage = Invoke-Capture { npx firebase deploy --only storage --dry-run --project $ProjectId --json }
if ($storage.exitCode -eq 0) {
    Add-Result $results "Storage rules dry-run" "ready" $storage.output
} elseif ($storage.output -match "Storage has not been set up") {
    Add-Result $results "Storage rules dry-run" "blocked" $storage.output "Open Firebase Storage for project $ProjectId and click Get Started."
} else {
    Add-Result $results "Storage rules dry-run" "missing" $storage.output "Initialize Firebase Storage and rerun this script."
}

Write-Host ""
Write-Host "TaskFlow real Firebase verification"
Write-Host ""

foreach ($result in $results) {
    Write-Host "[$($result.status)] $($result.name)"
    Write-Host "  Evidence: $($result.evidence)"
    if ($result.nextStep) {
        Write-Host "  Next: $($result.nextStep)"
    }
    Write-Host ""
}

$notReady = $results | Where-Object { $_.status -ne "ready" }
if ($Strict -and $notReady.Count -gt 0) {
    throw "Real Firebase verification failed: $($notReady.Count) item(s) are not ready."
}

if ($notReady.Count -gt 0) {
    Write-Host "Real Firebase verification incomplete: $($notReady.Count) item(s) are not ready."
} else {
    Write-Host "Real Firebase verification complete."
}
