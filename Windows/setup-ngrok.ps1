# TouchBridge — ngrok remote access setup
# Exposes the local control server (port 47831) via a public ngrok URL
# so your phone can connect from any network (mobile data, different Wi‑Fi, etc.)

param(
    [switch]$InstallOnly
)

$ErrorActionPreference = "Stop"

function Test-NgrokInstalled {
    $cmd = Get-Command ngrok -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $paths = @(
        "$env:LOCALAPPDATA\ngrok\ngrok.exe",
        "$env:USERPROFILE\scoop\shims\ngrok.exe",
        "C:\Program Files\ngrok\ngrok.exe"
    )
    foreach ($p in $paths) {
        if (Test-Path $p) { return $p }
    }
    return $null
}

Write-Host "TouchBridge ngrok setup" -ForegroundColor Cyan

$ngrok = Test-NgrokInstalled
if (-not $ngrok) {
    Write-Host "Installing ngrok via winget..." -ForegroundColor Yellow
    winget install --id Ngrok.Ngrok -e --accept-source-agreements --accept-package-agreements
    $ngrok = Test-NgrokInstalled
    if (-not $ngrok) {
        Write-Error "ngrok not found after install. Download from https://ngrok.com/download and add to PATH."
    }
}
Write-Host "ngrok: $ngrok" -ForegroundColor Green

if (-not $env:NGROK_AUTHTOKEN) {
    Write-Host ""
    Write-Host "NGROK_AUTHTOKEN is not set." -ForegroundColor Yellow
    Write-Host "1. Create a free account at https://dashboard.ngrok.com/signup"
    Write-Host "2. Copy your authtoken from https://dashboard.ngrok.com/get-started/your-authtoken"
    Write-Host "3. Run (once):"
    Write-Host '   ngrok config add-authtoken YOUR_TOKEN_HERE' -ForegroundColor White
    Write-Host "   OR set permanently:"
    Write-Host '   [Environment]::SetEnvironmentVariable("NGROK_AUTHTOKEN","YOUR_TOKEN","User")' -ForegroundColor White
    Write-Host ""
    if (-not $InstallOnly) {
        Write-Host "Continuing without authtoken — tunnel will fail until configured." -ForegroundColor Yellow
    }
} else {
    & $ngrok config add-authtoken $env:NGROK_AUTHTOKEN 2>$null
    Write-Host "ngrok authtoken configured." -ForegroundColor Green
}

if ($InstallOnly) { exit 0 }

Write-Host ""
Write-Host "Starting TouchBridge with ngrok tunnel..." -ForegroundColor Cyan
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
dotnet run --project TouchBridge.Desktop/TouchBridge.Desktop.csproj
