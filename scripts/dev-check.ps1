param(
  [switch]$StartCompose
)

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

function Write-Step {
  param([string]$Message)
  Write-Host ""
  Write-Host "== $Message ==" -ForegroundColor Cyan
}

function Test-Http {
  param(
    [string]$Name,
    [string]$Url
  )
  try {
    $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 5
    Write-Host "[OK] $Name $Url -> HTTP $($response.StatusCode)" -ForegroundColor Green
  } catch {
    Write-Host "[FAIL] $Name $Url -> $($_.Exception.Message)" -ForegroundColor Yellow
  }
}

function Test-JsonPost {
  param(
    [string]$Name,
    [string]$Url,
    [hashtable]$Body
  )
  try {
    $json = $Body | ConvertTo-Json -Depth 8
    $response = Invoke-RestMethod -Uri $Url -Method Post -ContentType "application/json; charset=utf-8" -Body $json -TimeoutSec 8
    Write-Host "[OK] $Name $Url" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 8
  } catch {
    Write-Host "[FAIL] $Name $Url -> $($_.Exception.Message)" -ForegroundColor Yellow
  }
}

function Test-Login {
  param(
    [string]$Username,
    [string]$Password
  )
  try {
    $body = @{ username = $Username; password = $Password } | ConvertTo-Json
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -ContentType "application/json; charset=utf-8" -Body $body -TimeoutSec 8
    if ($response.code -eq "0" -or $response.code -eq 0 -or $response.data.accessToken) {
      Write-Host "[OK] Backend Login $Username" -ForegroundColor Green
      return $response.data.accessToken
    } else {
      Write-Host "[FAIL] Backend Login $Username -> code=$($response.code), message=$($response.message)" -ForegroundColor Yellow
    }
  } catch {
    Write-Host "[FAIL] Backend Login $Username -> $($_.Exception.Message)" -ForegroundColor Yellow
  }
  return $null
}

function Test-BackendAi {
  param([string]$Token)
  if (-not $Token) {
    Write-Host "[SKIP] Backend AI sample skipped because login token is empty." -ForegroundColor Yellow
    return
  }
  try {
    $body = @{ text = "手机屏幕有划痕，想退货退款，客服回复太慢了" } | ConvertTo-Json
    $response = Invoke-RestMethod `
      -Uri "http://localhost:8080/api/merchant/ai/sentiment" `
      -Method Post `
      -ContentType "application/json; charset=utf-8" `
      -Headers @{ Authorization = "Bearer $Token" } `
      -Body $body `
      -TimeoutSec 8
    if ($response.data.riskLevel) {
      Write-Host "[OK] Backend -> AI sentiment riskLevel=$($response.data.riskLevel)" -ForegroundColor Green
    } else {
      Write-Host "[FAIL] Backend -> AI sentiment returned empty riskLevel" -ForegroundColor Yellow
    }
  } catch {
    Write-Host "[FAIL] Backend -> AI sample -> $($_.Exception.Message)" -ForegroundColor Yellow
  }
}

Write-Step "Docker"
docker version
$dockerReady = $LASTEXITCODE -eq 0
if ($LASTEXITCODE -ne 0) {
  Write-Host "Docker CLI can run, but Docker Engine is not reachable. Open Docker Desktop and try again." -ForegroundColor Yellow
}

Write-Step "Ports"
$ports = 3306, 6379, 8080, 9000, 5173, 5175
$connections = Get-NetTCPConnection -LocalPort $ports -ErrorAction SilentlyContinue |
  Select-Object LocalPort, State, OwningProcess
if ($connections) {
  $connections | Format-Table -AutoSize
} else {
  Write-Host "No configured project ports are currently listening."
}

if ($StartCompose) {
  if ($dockerReady) {
    Write-Step "Start Docker Compose"
    docker compose up -d --build
  } else {
    Write-Host "[SKIP] Docker Compose start skipped because Docker Engine is not reachable." -ForegroundColor Yellow
  }
}

Write-Step "Compose Status"
if ($dockerReady) {
  docker compose ps
} else {
  Write-Host "[SKIP] Docker Compose status skipped because Docker Engine is not reachable." -ForegroundColor Yellow
}

Write-Step "Health Checks"
Test-Http "Backend" "http://localhost:8080/api/health"
Test-Http "Swagger" "http://localhost:8080/swagger-ui.html"
Test-Http "AI Service" "http://localhost:9000/health"
Test-Http "Merchant Web" "http://localhost:5173"
Test-Http "Admin Web" "http://localhost:5175"

Write-Step "AI Sample"
Test-JsonPost "AI Sentiment" "http://localhost:9000/api/ai/sentiment" @{
  text = "手机屏幕有划痕，想退货退款，客服回复太慢了"
}

Write-Step "Backend Business Sample"
$token = Test-Login "merchant_admin_demo" "123456"
Test-BackendAi $token
