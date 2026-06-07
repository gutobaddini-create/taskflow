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

function ConvertTo-FirestoreFields {
    param([hashtable]$Values)

    $fields = @{}
    foreach ($key in $Values.Keys) {
        $value = $Values[$key]
        if ($value -is [array]) {
            $fields[$key] = @{
                arrayValue = @{
                    values = @($value | ForEach-Object { @{ stringValue = "$_" } })
                }
            }
        } elseif ($value -is [int] -or $value -is [long]) {
            $fields[$key] = @{ integerValue = "$value" }
        } elseif ($value -is [bool]) {
            $fields[$key] = @{ booleanValue = $value }
        } else {
            $fields[$key] = @{ stringValue = "$value" }
        }
    }

    return @{ fields = $fields }
}

function Invoke-FirestoreWrite {
    param(
        [string]$ProjectId,
        [string]$DocumentPath,
        [string]$IdToken,
        [hashtable]$Values
    )

    $headers = @{ Authorization = "Bearer $IdToken" }
    $body = ConvertTo-FirestoreFields $Values | ConvertTo-Json -Depth 20
    $encodedPath = $DocumentPath -split "/" | ForEach-Object { [System.Uri]::EscapeDataString($_) }
    $uri = "https://firestore.googleapis.com/v1/projects/$ProjectId/databases/(default)/documents/$($encodedPath -join '/')"
    Invoke-RestMethod -Method Patch -Uri $uri -Headers $headers -ContentType "application/json" -Body $body | Out-Null
}

function Invoke-FirestoreDelete {
    param(
        [string]$ProjectId,
        [string]$DocumentPath,
        [string]$IdToken
    )

    $headers = @{ Authorization = "Bearer $IdToken" }
    $encodedPath = $DocumentPath -split "/" | ForEach-Object { [System.Uri]::EscapeDataString($_) }
    $uri = "https://firestore.googleapis.com/v1/projects/$ProjectId/databases/(default)/documents/$($encodedPath -join '/')"
    Invoke-RestMethod -Method Delete -Uri $uri -Headers $headers -ErrorAction SilentlyContinue | Out-Null
}

function Invoke-StorageUpload {
    param(
        [string]$Bucket,
        [string]$ObjectPath,
        [string]$IdToken
    )

    $headers = @{ Authorization = "Bearer $IdToken" }
    $encodedPath = [System.Uri]::EscapeDataString($ObjectPath)
    $uri = "https://firebasestorage.googleapis.com/v0/b/$Bucket/o?uploadType=media&name=$encodedPath"
    Invoke-RestMethod -Method Post -Uri $uri -Headers $headers -ContentType "text/plain" -Body "TaskFlow Firebase Storage smoke $(Get-Date -Format o)" | Out-Null
}

function Invoke-StorageDelete {
    param(
        [string]$Bucket,
        [string]$ObjectPath,
        [string]$IdToken
    )

    $headers = @{ Authorization = "Bearer $IdToken" }
    $encodedPath = [System.Uri]::EscapeDataString($ObjectPath)
    $uri = "https://firebasestorage.googleapis.com/v0/b/$Bucket/o/$encodedPath"
    Invoke-RestMethod -Method Delete -Uri $uri -Headers $headers -ErrorAction SilentlyContinue | Out-Null
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
    $storageBucket = $config.project_info.storage_bucket
    Add-Result $results "Android Firebase config" "ready" "Project: $($config.project_info.project_id); App: $appId"
} else {
    Add-Result $results "Android Firebase config" "missing" "app/google-services.json not found." "Download the Android app config from Firebase."
}

if ($apiKey) {
    $email = "taskflow-smoke-delete-$(Get-Date -Format yyyyMMddHHmmss)@example.com"
    $password = "TaskFlow!$(Get-Random -Minimum 100000 -Maximum 999999)"
    $body = @{ email = $email; password = $password; returnSecureToken = $true } | ConvertTo-Json
    $created = $null
    $signIn = $null
    try {
        $created = Invoke-RestMethod -Method Post -Uri "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$apiKey" -ContentType "application/json" -Body $body
        $signIn = Invoke-RestMethod -Method Post -Uri "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$apiKey" -ContentType "application/json" -Body $body
        Add-Result $results "Firebase Auth email/password" "ready" "Created, signed in, and deleted smoke user $email."
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

if ($signIn -and $storageBucket) {
    $uid = $signIn.localId
    $token = $signIn.idToken
    $suffix = Get-Date -Format yyyyMMddHHmmss
    $spaceId = "smoke-space-$suffix"
    $taskId = "smoke-task-$suffix"
    $operationId = "smoke-operation-$suffix"
    $attachmentId = "smoke-attachment-$suffix"
    $fcmTokenId = "smoke-fcm-token-$suffix"
    $storagePath = "users/$uid/tasks/$taskId/attachments/$attachmentId/smoke.txt"

    try {
        Invoke-FirestoreWrite $ProjectId "users/$uid" $token @{
            id = $uid
            email = $email
            createdAt = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
            notificationPermissionStatus = "unknown"
        }
        Invoke-FirestoreWrite $ProjectId "spaces/$spaceId" $token @{
            id = $spaceId
            ownerId = $uid
            members = @($uid)
            name = "TaskFlow Smoke"
            createdAt = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        }
        Invoke-FirestoreWrite $ProjectId "tasks/$taskId" $token @{
            id = $taskId
            spaceId = $spaceId
            createdBy = $uid
            assignedTo = $uid
            participants = @($uid)
            title = "TaskFlow Firebase smoke"
            status = "todo"
            createdAt = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        }
        Invoke-FirestoreWrite $ProjectId "pendingOperations/$operationId" $token @{
            id = $operationId
            userId = $uid
            entity = "Task"
            entityId = $taskId
            operation = "Create"
            createdAt = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        }
        Invoke-StorageUpload $storageBucket $storagePath $token
        Invoke-FirestoreWrite $ProjectId "attachments/$attachmentId" $token @{
            id = $attachmentId
            taskId = $taskId
            uploadedBy = $uid
            fileName = "smoke.txt"
            originalFileName = "smoke.txt"
            mimeType = "text/plain"
            fileSize = 32
            storagePath = $storagePath
            createdAt = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        }
        Invoke-FirestoreWrite $ProjectId "users/$uid/fcmTokens/$fcmTokenId" $token @{
            token = $fcmTokenId
            platform = "android"
            updatedAt = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        }

        Add-Result $results "Firestore/Storage/FCM smoke flow" "ready" "Wrote user, space, task, pending operation, attachment metadata, FCM token document, and uploaded Storage object for $email."
    } catch {
        $message = $_.ErrorDetails.Message
        if (-not $message) { $message = $_.Exception.Message }
        Add-Result $results "Firestore/Storage/FCM smoke flow" "missing" $message "Check Firestore/Storage rules and Firebase SDK data contracts."
    } finally {
        Invoke-FirestoreDelete $ProjectId "users/$uid/fcmTokens/$fcmTokenId" $token
        Invoke-FirestoreDelete $ProjectId "attachments/$attachmentId" $token
        Invoke-StorageDelete $storageBucket $storagePath $token
        Invoke-FirestoreDelete $ProjectId "pendingOperations/$operationId" $token
        Invoke-FirestoreDelete $ProjectId "tasks/$taskId" $token
        Invoke-FirestoreDelete $ProjectId "spaces/$spaceId" $token
        Invoke-FirestoreDelete $ProjectId "users/$uid" $token
    }
}

if ($signIn) {
    $deleteBody = @{ idToken = $signIn.idToken } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "https://identitytoolkit.googleapis.com/v1/accounts:delete?key=$apiKey" -ContentType "application/json" -Body $deleteBody | Out-Null
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
