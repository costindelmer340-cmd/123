param(
  [string]$DbUrl = "jdbc:mysql://localhost:3306/ecommerce_after_sale?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8",
  [string]$DbUsername = "root",
  [string]$DbPassword = "123456",
  [switch]$SkipBackend,
  [switch]$SkipFrontend,
  [switch]$SkipAi
)

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$Root = Split-Path -Parent $PSScriptRoot

function Write-Step {
  param([string]$Message)
  Write-Host ""
  Write-Host "== $Message ==" -ForegroundColor Cyan
}

function Test-Port {
  param([int]$Port)
  return [bool](Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

function Start-HiddenProcess {
  param(
    [string]$Name,
    [string]$FilePath,
    [string[]]$ArgumentList,
    [string]$WorkingDirectory,
    [hashtable]$Environment = @{},
    [string]$LogName = ""
  )

  $logDir = Join-Path $Root "logs"
  if (-not (Test-Path $logDir)) {
    New-Item -ItemType Directory -Path $logDir | Out-Null
  }
  if (-not $LogName) {
    $LogName = ($Name -replace "\s+", "-").ToLower()
  }

  $psi = New-Object System.Diagnostics.ProcessStartInfo
  $psi.FileName = $FilePath
  $psi.Arguments = ($ArgumentList | ForEach-Object {
    if ($_ -match "\s") {
      '"' + ($_ -replace '"', '\"') + '"'
    } else {
      $_
    }
  }) -join " "
  $psi.WorkingDirectory = $WorkingDirectory
  $psi.UseShellExecute = $false
  $psi.CreateNoWindow = $true
  $psi.RedirectStandardOutput = $true
  $psi.RedirectStandardError = $true
  foreach ($key in $Environment.Keys) {
    $psi.Environment[$key] = [string]$Environment[$key]
  }
  $process = [System.Diagnostics.Process]::Start($psi)
  $stdout = Join-Path $logDir "$LogName.out.log"
  $stderr = Join-Path $logDir "$LogName.err.log"
  Register-ObjectEvent -InputObject $process -EventName OutputDataReceived -Action {
    if ($EventArgs.Data) { Add-Content -Path $Event.MessageData.Stdout -Value $EventArgs.Data }
  } -MessageData @{ Stdout = $stdout } | Out-Null
  Register-ObjectEvent -InputObject $process -EventName ErrorDataReceived -Action {
    if ($EventArgs.Data) { Add-Content -Path $Event.MessageData.Stderr -Value $EventArgs.Data }
  } -MessageData @{ Stderr = $stderr } | Out-Null
  $process.BeginOutputReadLine()
  $process.BeginErrorReadLine()
  Write-Host "[START] $Name" -ForegroundColor Green
}

if (-not $SkipAi) {
  Write-Step "AI Service"
  if (Test-Port 9000) {
    Write-Host "[SKIP] AI Service already listens on 9000" -ForegroundColor Yellow
  } else {
    Start-HiddenProcess `
      -Name "AI Service" `
      -FilePath "python" `
      -ArgumentList @("-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "9000") `
      -WorkingDirectory (Join-Path $Root "ai-service") `
      -LogName "ai-service"
  }
}

if (-not $SkipBackend) {
  Write-Step "Backend"
  if (Test-Port 8080) {
    Write-Host "[SKIP] Backend already listens on 8080" -ForegroundColor Yellow
  } else {
    $jar = Join-Path $Root "backend\target\mall-backend-0.0.1-SNAPSHOT.jar"
    if (-not (Test-Path $jar)) {
      Write-Host "[BUILD] Backend jar not found, running Maven package" -ForegroundColor Yellow
      Push-Location (Join-Path $Root "backend")
      mvn -DskipTests package
      Pop-Location
    }
    Start-HiddenProcess `
      -Name "Backend" `
      -FilePath "java" `
      -ArgumentList @("-jar", $jar) `
      -WorkingDirectory (Join-Path $Root "backend") `
      -Environment @{
        DB_URL = $DbUrl
        DB_USERNAME = $DbUsername
        DB_PASSWORD = $DbPassword
        REDIS_HOST = "localhost"
        REDIS_PORT = "6379"
        APP_AI_BASE_URL = "http://localhost:9000"
      } `
      -LogName "backend"
  }
}

if (-not $SkipFrontend) {
  Write-Step "Web Frontends"
  $frontends = @(
    @{ Name = "Merchant Web"; Port = 5173; Dir = "frontend\merchant-web" },
    @{ Name = "Admin Web"; Port = 5175; Dir = "frontend\admin-web" }
  )

  foreach ($app in $frontends) {
    if (Test-Port $app.Port) {
      Write-Host "[SKIP] $($app.Name) already listens on $($app.Port)" -ForegroundColor Yellow
      continue
    }
    $appDir = Join-Path $Root $app.Dir
    $viteCmd = Join-Path $appDir "node_modules\.bin\vite.cmd"
    Start-HiddenProcess `
      -Name $app.Name `
      -FilePath "cmd.exe" `
      -ArgumentList @("/c", "`"$viteCmd`" --host 0.0.0.0 --port $($app.Port)") `
      -WorkingDirectory $appDir `
      -LogName ($app.Name -replace "\s+", "-").ToLower()
  }
}

Write-Step "Done"
Write-Host "Wait a few seconds, then run: .\scripts\dev-check.ps1" -ForegroundColor Cyan
