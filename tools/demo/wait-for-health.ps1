[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost:8080',
    [ValidateRange(1, 300)][int]$TimeoutSeconds = 60
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$uri = $null
if (-not [Uri]::TryCreate($BaseUrl, [UriKind]::Absolute, [ref]$uri) -or
    $uri.Scheme -notin @('http', 'https') -or $uri.UserInfo -or $uri.Query -or $uri.Fragment -or $uri.AbsolutePath -ne '/') {
    throw 'Provide an HTTP(S) base URL without credentials, path, query or fragment.'
}
$healthUrl = "$($BaseUrl.TrimEnd('/'))/actuator/health"
$watch = [Diagnostics.Stopwatch]::StartNew()
$http = [System.Net.Http.HttpClient]::new()
$http.Timeout = [Threading.Timeout]::InfiniteTimeSpan
$lastFailure = 'No successful health response.'
try {
    while ($watch.Elapsed.TotalSeconds -lt $TimeoutSeconds) {
        $remaining = [int][Math]::Max(1, ($TimeoutSeconds * 1000 - $watch.Elapsed.TotalMilliseconds))
        $cancel = [Threading.CancellationTokenSource]::new($remaining)
        $response = $null
        try {
            $response = $http.GetAsync($healthUrl, $cancel.Token).GetAwaiter().GetResult()
            if ($response.IsSuccessStatusCode) {
                $health = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
                if ($health.status -eq 'UP') { Write-Output 'Application readiness confirmed.'; return }
            }
            $lastFailure = 'Health response was not a successful UP result.'
        } catch { $lastFailure = 'Health request failed or timed out.' }
        finally { if ($response) { $response.Dispose() }; $cancel.Dispose() }
        $pause = [int][Math]::Min(200, [Math]::Max(0, $TimeoutSeconds * 1000 - $watch.Elapsed.TotalMilliseconds))
        if ($pause -gt 0) { Start-Sleep -Milliseconds $pause }
    }
    throw "Application did not become ready within $TimeoutSeconds seconds. $lastFailure"
} finally { $http.Dispose(); $watch.Stop() }
