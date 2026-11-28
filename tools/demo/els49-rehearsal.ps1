[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$ProjectName,
    [string]$EnvFile = '.env',
    [string]$BaseUrl
)

. "$PSScriptRoot/common.ps1"
$context = Get-ElsDemoContext $ProjectName $EnvFile $BaseUrl
$stateFile = Get-ElsStatePath $ProjectName ''
$base = $context.BaseUrl
$assertions = 0

function Assert-Els49 {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw "ELS-49 rehearsal failed: $Message" }
    $script:assertions++
}

function Invoke-Els49Http {
    param([string]$Method, [string]$Path, [string]$Authorization, [string]$Json)
    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::new($Method), "$base$Path")
    $response = $null
    try {
        if ($Authorization) {
            $request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::Parse($Authorization)
        }
        if ($Json) {
            $request.Content = [System.Net.Http.StringContent]::new($Json, [Text.Encoding]::UTF8, 'application/json')
        }
        $response = $http.SendAsync($request).GetAwaiter().GetResult()
        $content = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        return [pscustomobject]@{
            Status = [int]$response.StatusCode
            Challenge = [string]$response.Headers.WwwAuthenticate
            Body = if ($content) { ConvertFrom-Json -InputObject $content } else { $null }
        }
    } finally {
        if ($response) { $response.Dispose() }
        $request.Dispose()
    }
}

function Get-Els49Counts {
    $sql = 'SELECT (SELECT count(*) FROM student),(SELECT count(*) FROM equipment),(SELECT count(*) FROM loan);'
    return [string](Invoke-ElsDocker -Arguments @('exec', $context.DatabaseContainer, 'psql', '-v', 'ON_ERROR_STOP=1', '-U', $context.DatabaseUser, '-d', $context.DatabaseName, '-At', '-c', $sql))
}

Add-Type -AssemblyName System.Net.Http
$handler = [System.Net.Http.HttpClientHandler]::new()
$handler.AllowAutoRedirect = $false
$http = [System.Net.Http.HttpClient]::new($handler)
$http.Timeout = [TimeSpan]::FromSeconds(10)
try {
    $health = Invoke-Els49Http Get '/actuator/health' '' ''
    Assert-Els49 ($health.Status -eq 200 -and $health.Body.status -eq 'UP' -and @($health.Body.PSObject.Properties).Count -eq 1) 'public aggregate health'

    $before = Get-Els49Counts
    foreach ($authorization in @('', ('Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('invalid-synthetic:invalid-secret'))))) {
        $rejected = Invoke-Els49Http Post '/api/students' $authorization '{"name":"Rejected Synthetic","email":"rejected@example.test"}'
        Assert-Els49 ($rejected.Status -eq 401 -and $rejected.Body.code -eq 'UNAUTHORIZED' -and $rejected.Body.path -eq '/api/students' -and @($rejected.Body.PSObject.Properties).Count -eq 6 -and $rejected.Challenge -eq 'Basic realm="equipment-lending"') 'staff rejection contract'
    }
    Assert-Els49 ((Get-Els49Counts) -eq $before) 'rejected requests made no committed write'

    & "$PSScriptRoot/seed-demo.ps1" -ProjectName $ProjectName -EnvFile $EnvFile -BaseUrl $base | Out-Null
    $state = Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json
    Assert-Els49 ($state.status -eq 'complete' -and $state.project -eq $ProjectName) 'guarded seed completed for owned project'
    Assert-Els49 ((Get-Els49Counts) -eq '1|2|2') 'student/equipment/loan counts are 1|2|2'

    $auth = $context.StaffHeaders.Authorization
    $student = Invoke-Els49Http Get "/api/students/$($state.studentId)" $auth ''
    Assert-Els49 ($student.Status -eq 200 -and $student.Body.email -eq 'els45.demo.borrower@example.test') 'authorized synthetic student'
    $available = Invoke-Els49Http Get '/api/equipment?available=true' $auth ''
    Assert-Els49 ($available.Status -eq 200 -and @($available.Body).Count -eq 1 -and @($available.Body)[0].id -eq $state.laptopId) 'returned item available; active item unavailable'
    $history = Invoke-Els49Http Get "/api/loans?studentId=$($state.studentId)" $auth ''
    $loans = @($history.Body)
    Assert-Els49 ($history.Status -eq 200 -and $loans.Count -eq 2) 'filtered student history has exactly two loans'
    $active = @($loans | Where-Object { $_.id -eq $state.activeLoanId })
    $returned = @($loans | Where-Object { $_.id -eq $state.returnedLoanId })
    Assert-Els49 ($active.Count -eq 1 -and $null -eq $active[0].returnDate -and $active[0].equipmentId -eq $state.cameraId) 'active camera loan persists'
    Assert-Els49 ($returned.Count -eq 1 -and $returned[0].returnDate -eq $state.today -and $returned[0].equipmentId -eq $state.laptopId) 'returned laptop history persists'

    $duplicate = Invoke-Els49Http Post '/api/students' $auth '{"name":"Duplicate Synthetic","email":"els45.demo.borrower@example.test"}'
    Assert-Els49 ($duplicate.Status -eq 409 -and $duplicate.Body.code -eq 'DUPLICATE_EMAIL') 'duplicate student rejected with public error code'
    $seededCounts = Get-Els49Counts
    Assert-Els49 ($seededCounts -eq '1|2|2') 'duplicate rejection made no committed write'
    & "$PSScriptRoot/seed-demo.ps1" -ProjectName $ProjectName -EnvFile $EnvFile -BaseUrl $base | Out-Null
    Assert-Els49 ((Get-Els49Counts) -eq $seededCounts) 'guarded rerun made no committed write'
    Write-Output "PASS: ELS-49 local HTTP rehearsal, $assertions assertions; synthetic seed, auth, queries, errors and read-only rerun."
} finally {
    $http.Dispose()
    $handler.Dispose()
}
