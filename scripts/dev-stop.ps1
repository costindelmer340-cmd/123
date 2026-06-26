param(
  [string[]]$Ports = @("8080", "9000", "5173", "5175")
)

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

foreach ($portValue in $Ports) {
  foreach ($portText in ($portValue -split ",")) {
    $portText = $portText.Trim()
    if (-not $portText) {
      continue
    }
    $port = [int]$portText
    $processIds = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess -Unique

    if (-not $processIds) {
      Write-Host "[SKIP] No process listens on $port" -ForegroundColor Yellow
      continue
    }

    foreach ($processId in $processIds) {
      try {
        $process = Get-Process -Id $processId -ErrorAction Stop
        Stop-Process -Id $processId -Force
        Write-Host "[STOP] Port $port -> $($process.ProcessName) ($processId)" -ForegroundColor Green
      } catch {
        Write-Host "[FAIL] Port $port -> $($_.Exception.Message)" -ForegroundColor Yellow
      }
    }
  }
}
