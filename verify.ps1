param(
  [ValidateSet('all', 'frontend', 'backend')]
  [string]$Target = 'all',

  [ValidateSet('fast', 'full')]
  [string]$Mode = 'fast'
)

$ErrorActionPreference = 'Stop'
$script:Failures = 0

function Add-Failure {
  param([string]$Message)
  $script:Failures += 1
  Write-Host "[fail] $Message"
}

function Test-CommandAvailable {
  param([string]$Name)
  return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

function Invoke-BackendVerify {
  Write-Host ""
  Write-Host "== Backend verify ($Mode) =="

  if (-not (Test-Path -LiteralPath 'backend\pom.xml')) {
    Add-Failure "backend not initialized yet: backend\pom.xml is missing. Next step: complete B1-001 scaffold."
    return
  }

  if (-not (Test-CommandAvailable 'mvn')) {
    Add-Failure "Maven command 'mvn' was not found. Install Maven or add it to PATH before backend verification."
    return
  }

  Push-Location 'backend'
  try {
    Write-Host "[run] mvn test"
    & mvn test
    if ($LASTEXITCODE -ne 0) {
      Add-Failure "mvn test failed with exit code $LASTEXITCODE."
    } else {
      Write-Host "[ok] mvn test passed."
    }
  } finally {
    Pop-Location
  }

  if ($Mode -eq 'full') {
    Write-Host "[info] full backend mode is reserved for API chain, SSE, and golden case verification after scaffold."
  }
}

function Invoke-FrontendVerify {
  Write-Host ""
  Write-Host "== Frontend verify ($Mode) =="

  if (-not (Test-Path -LiteralPath 'frontend\package.json')) {
    Add-Failure "frontend not initialized yet: frontend\package.json is missing. Next step: complete F1-001 scaffold."
    return
  }

  if (-not (Test-CommandAvailable 'pnpm')) {
    Add-Failure "pnpm was not found. Install pnpm or enable it for this shell before frontend verification."
    return
  }

  $pkg = Get-Content -Raw -Encoding UTF8 -LiteralPath 'frontend\package.json' | ConvertFrom-Json
  $scriptNames = @()
  if ($null -ne $pkg.scripts) {
    $scriptNames = $pkg.scripts.PSObject.Properties.Name
  }

  Push-Location 'frontend'
  try {
    if ($scriptNames -contains 'typecheck') {
      Write-Host "[run] pnpm typecheck"
      & pnpm typecheck
      if ($LASTEXITCODE -ne 0) {
        Add-Failure "pnpm typecheck failed with exit code $LASTEXITCODE."
      } else {
        Write-Host "[ok] pnpm typecheck passed."
      }
    } elseif ($scriptNames -contains 'build') {
      Write-Host "[run] pnpm build"
      & pnpm build
      if ($LASTEXITCODE -ne 0) {
        Add-Failure "pnpm build failed with exit code $LASTEXITCODE."
      } else {
        Write-Host "[ok] pnpm build passed."
      }
    } else {
      Add-Failure "frontend package.json has neither typecheck nor build script."
    }
  } finally {
    Pop-Location
  }

  if ($Mode -eq 'full') {
    Write-Host "[info] full frontend mode is reserved for Playwright, fixture rendering, and UI state verification after scaffold."
  }
}

Write-Host "WeekendTravel verify"
Write-Host "Target: $Target"
Write-Host "Mode: $Mode"

if ($Target -eq 'all' -or $Target -eq 'backend') {
  Invoke-BackendVerify
}

if ($Target -eq 'all' -or $Target -eq 'frontend') {
  Invoke-FrontendVerify
}

Write-Host ""
if ($script:Failures -gt 0) {
  Write-Host "Verify finished with $script:Failures failure(s). See messages above; missing scaffold is a real incomplete state, not a pass."
  exit 1
}

Write-Host "Verify passed."
exit 0
