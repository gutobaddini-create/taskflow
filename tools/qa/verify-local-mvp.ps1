param(
    [switch]$SkipFirebaseRules
)

$ErrorActionPreference = "Stop"

function Invoke-Step {
    param(
        [string]$Name,
        [scriptblock]$Command
    )

    Write-Host ""
    Write-Host "==> $Name"
    & $Command
    if ($LASTEXITCODE -ne 0) {
        throw "Step failed: $Name"
    }
}

function Assert-File {
    param([string]$Path)

    if (-not (Test-Path $Path)) {
        throw "Missing expected artifact: $Path"
    }
    $item = Get-Item $Path
    if ($item.Length -le 0) {
        throw "Artifact is empty: $Path"
    }
    Write-Host "ok - $Path ($($item.Length) bytes)"
}

$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $root

Invoke-Step "Gradle unit tests and debug Kotlin compile" {
    .\gradlew.bat --no-daemon --max-workers=1 :app:testDebugUnitTest :app:compileDebugKotlin --console=plain
}

Invoke-Step "Debug APK and Android test compile" {
    .\gradlew.bat --no-daemon --max-workers=1 :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain
}

Invoke-Step "Release APK and AAB" {
    .\gradlew.bat --no-daemon --max-workers=1 :app:assembleRelease :app:bundleRelease --console=plain
}

if (-not $SkipFirebaseRules) {
    Invoke-Step "Firebase emulator security rules" {
        npm run test:firebase-rules
    }
}

Write-Host ""
Write-Host "==> Artifact check"
Assert-File "app\build\outputs\apk\debug\app-debug.apk"
Assert-File "app\build\outputs\apk\release\app-release.apk"
Assert-File "app\build\outputs\bundle\release\app-release.aab"

Write-Host ""
Write-Host "TaskFlow local MVP verification completed."
