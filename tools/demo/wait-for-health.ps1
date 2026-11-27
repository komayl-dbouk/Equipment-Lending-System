[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8080",
    [ValidateRange(1, 300)]
    [int]$TimeoutSeconds = 60
)

$ErrorActionPreference = "Stop"
$healthUrl = "$($BaseUrl.TrimEnd('/'))/actuator/health"
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$lastFailure = $null

do {
    try {
        $health = Invoke-RestMethod -Method Get -Uri $healthUrl
        if ($health.status -eq "UP") {
            Write-Output "Application readiness confirmed: $healthUrl"
            exit 0
        }
        $lastFailure = "Health endpoint reported status '$($health.status)'."
    }
    catch {
        $lastFailure = $_.Exception.Message
    }
    Start-Sleep -Seconds 1
} while ((Get-Date) -lt $deadline)

throw "Application did not become ready within $TimeoutSeconds seconds at $healthUrl. Last result: $lastFailure"
