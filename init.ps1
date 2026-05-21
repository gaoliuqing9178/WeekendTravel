param(
  [ValidateSet('all', 'frontend', 'backend')]
  [string]$Target = 'all'
)

$ErrorActionPreference = 'Stop'

function Test-CommandAvailable {
  param([string]$Name)

  $cmd = Get-Command $Name -ErrorAction SilentlyContinue
  if ($null -eq $cmd) {
    Write-Host "[missing] $Name"
    return $false
  }

  Write-Host "[ok] $Name -> $($cmd.Source)"
  return $true
}

function Test-Backend {
  Write-Host ""
  Write-Host "== Backend environment =="
  [void](Test-CommandAvailable 'java')
  [void](Test-CommandAvailable 'mvn')

  if (-not (Test-Path -LiteralPath 'backend\pom.xml')) {
    Write-Host "[next] backend not initialized yet: scaffold Java 17 + Spring Boot 3 + Maven project under backend/."
    Write-Host "[next] first backend target: B1-001, GET /health on http://localhost:8000 and CORS for http://localhost:5173."
    return
  }

  Write-Host "[ok] backend\pom.xml found. Install dependencies inside backend/ via Maven commands only."
}

function Test-Frontend {
  Write-Host ""
  Write-Host "== Frontend environment =="
  [void](Test-CommandAvailable 'node')
  [void](Test-CommandAvailable 'pnpm')

  if (-not (Test-Path -LiteralPath 'frontend\package.json')) {
    Write-Host "[next] frontend not initialized yet: scaffold Vue 3 + Vite + Pinia + Naive UI project under frontend/."
    Write-Host "[next] first frontend target: F1-001, pnpm dev on http://localhost:5173."
    return
  }

  Write-Host "[ok] frontend\package.json found. Install dependencies inside frontend/; do not install global project dependencies."
}

Write-Host "WeekendTravel init check"
Write-Host "Target: $Target"

if ($Target -eq 'all' -or $Target -eq 'backend') {
  Test-Backend
}

if ($Target -eq 'all' -or $Target -eq 'frontend') {
  Test-Frontend
}

Write-Host ""
Write-Host "Init check completed. This script checks prerequisites and prints next steps; it does not scaffold or install globally."
