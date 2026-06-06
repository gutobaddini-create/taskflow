$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $root

$manifestPath = Join-Path $root "docs\qa\release-manifest.json"
$artifacts = @(
    "app\build\outputs\apk\debug\app-debug.apk",
    "app\build\outputs\apk\release\app-release.apk",
    "app\build\outputs\bundle\release\app-release.aab"
)

function Get-Sha256 {
    param([string]$Path)

    $stream = [System.IO.File]::OpenRead($Path)
    try {
        $hash = [System.Security.Cryptography.SHA256]::Create().ComputeHash($stream)
        return ([System.BitConverter]::ToString($hash) -replace "-", "").ToLowerInvariant()
    } finally {
        $stream.Dispose()
    }
}

$entries = foreach ($path in $artifacts) {
    if (-not (Test-Path $path)) {
        throw "Missing expected artifact: $path"
    }

    $item = Get-Item $path
    if ($item.Length -le 0) {
        throw "Artifact is empty: $path"
    }

    [ordered]@{
        path = $path -replace "\\", "/"
        bytes = $item.Length
        sha256 = Get-Sha256 $item.FullName
        lastModifiedUtc = $item.LastWriteTimeUtc.ToString("yyyy-MM-ddTHH:mm:ssZ")
    }
}

$manifest = [ordered]@{
    generatedAtUtc = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
    appId = "com.taskflow"
    version = "0.1.0"
    mode = "local-first-mvp"
    artifacts = $entries
}

New-Item -ItemType Directory -Force (Split-Path $manifestPath) | Out-Null
$manifest | ConvertTo-Json -Depth 5 | Set-Content -Encoding UTF8 $manifestPath
Write-Host "Release manifest written to $manifestPath"
