[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$ProjectName,
    [string]$EnvFile = '.env',
    [string]$BaseUrl,
    [switch]$PauseBetweenSteps
)

. "$PSScriptRoot/common.ps1"
$context = Get-ElsDemoContext $ProjectName $EnvFile $BaseUrl
$stateFile = Get-ElsStatePath $ProjectName ''
# Strictly validate the complete seed and target before marking any mutation.
& "$PSScriptRoot/seed-demo.ps1" -ProjectName $ProjectName -EnvFile $EnvFile -BaseUrl $context.BaseUrl | Out-Null
$state = Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json
try { $zone = [TimeZoneInfo]::FindSystemTimeZoneById('Asia/Beirut') }
catch { $zone = [TimeZoneInfo]::FindSystemTimeZoneById('Middle East Standard Time') }
$today = [TimeZoneInfo]::ConvertTimeFromUtc([DateTime]::UtcNow, $zone).Date
if ($state.today -ne $today.ToString('yyyy-MM-dd')) { throw 'Seed business day changed; use a fresh owned disposable project.' }

function Assert-Workflow([bool]$Condition, [string]$Message) {
    if (-not $Condition) { throw "Live workflow failed: $Message. Preserve the checkpoint; inspect privately." }
}
function Show-Step([string]$Message) {
    Write-Output $Message
    if ($PauseBetweenSteps) { Read-Host 'Explain the oracle, then press Enter' | Out-Null }
}
function Invoke-Workflow([string]$Method, [string]$Path, $Body, [int]$Expected, [string]$Code = '') {
    $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::new($Method), "$($context.BaseUrl)$Path")
    $response = $null
    try {
        $request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::Parse($context.StaffHeaders.Authorization)
        if ($null -ne $Body) { $request.Content = [System.Net.Http.StringContent]::new(($Body | ConvertTo-Json -Compress), [Text.Encoding]::UTF8, 'application/json') }
        $response = $http.SendAsync($request).GetAwaiter().GetResult()
        $value = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
        Assert-Workflow ([int]$response.StatusCode -eq $Expected) 'HTTP status'
        if ($Code) { Assert-Workflow ($value.code -eq $Code -and $value.path -eq $Path -and @($value.PSObject.Properties).Count -eq 6) 'public error contract' }
        return $value
    } finally { if ($response) { $response.Dispose() }; $request.Dispose() }
}
function Get-LoanSnapshot {
    return [string](Invoke-ElsDocker -Arguments @('exec', $context.DatabaseContainer, 'psql', '-v', 'ON_ERROR_STOP=1', '-U', $context.DatabaseUser, '-d', $context.DatabaseName, '-At', '-c', 'SELECT coalesce(json_agg(t ORDER BY id)::text,''[]'') FROM loan t;'))
}

Add-Type -AssemblyName System.Net.Http
$handler = [System.Net.Http.HttpClientHandler]::new()
$handler.AllowAutoRedirect = $false
$http = [System.Net.Http.HttpClient]::new($handler)
$http.Timeout = [TimeSpan]::FromSeconds(10)
try {
    $state.status = 'rehearsal-mutating'
    Write-ElsDemoState $stateFile $state
    $borrow = @{studentId=$state.studentId; equipmentId=$state.cameraId; borrowingDate=$state.previousBorrowingDate; dueDate=$state.previousDueDate}
    $before = Get-LoanSnapshot
    Invoke-Workflow Post '/api/loans' $borrow 409 'EQUIPMENT_UNAVAILABLE' | Out-Null
    Assert-Workflow ((Get-LoanSnapshot) -ceq $before) 'rejected borrow changed no loan values'
    Show-Step 'PASS debugging probe: active camera rejects borrowing; committed loan snapshot unchanged.'

    $returned = Invoke-Workflow Post "/api/loans/$($state.activeLoanId)/return" @{returnDate=$state.today} 200
    Assert-Workflow ($returned.id -eq $state.activeLoanId -and $returned.returnDate -eq $state.today -and $returned.equipmentId -eq $state.cameraId) 'exact return identity/date'
    $available = @(Invoke-Workflow Get '/api/equipment?available=true' $null 200)
    Assert-Workflow (@($available | Where-Object { $_.id -eq $state.cameraId }).Count -eq 1) 'returned camera available'
    $before = Get-LoanSnapshot
    Invoke-Workflow Post "/api/loans/$($state.activeLoanId)/return" @{returnDate=$state.today} 409 'LOAN_ALREADY_RETURNED' | Out-Null
    Assert-Workflow ((Get-LoanSnapshot) -ceq $before) 'repeat return preserves every loan value'
    Show-Step 'PASS return: camera available; second return rejected without overwriting history.'

    $new = Invoke-Workflow Post '/api/loans' $borrow 201
    Assert-Workflow ($new.id -ne $state.activeLoanId -and $new.studentId -eq $state.studentId -and $new.equipmentId -eq $state.cameraId -and $new.borrowingDate -eq $state.previousBorrowingDate -and $new.dueDate -eq $state.previousDueDate -and $null -eq $new.returnDate) 'new immutable loan values'
    $overdue = @(Invoke-Workflow Get '/api/loans/overdue' $null 200)
    Assert-Workflow ($overdue.Count -eq 1 -and $overdue[0].id -eq $new.id) 'only unreturned past-due loan overdue'
    $available = @(Invoke-Workflow Get '/api/equipment?available=true' $null 200)
    Assert-Workflow (@($available | Where-Object { $_.id -eq $state.cameraId }).Count -eq 0) 'reborrowed camera unavailable'
    Show-Step 'PASS reborrow and overdue: new loan ID, previous history retained, camera unavailable.'

    Invoke-Workflow Post "/api/loans/$($new.id)/return" @{returnDate=$state.today} 200 | Out-Null
    $overdue = @(Invoke-Workflow Get '/api/loans/overdue' $null 200)
    Assert-Workflow ($overdue.Count -eq 0) 'returned past-due loan excluded'
    $history = @(Invoke-Workflow Get "/api/loans?studentId=$($state.studentId)&equipmentId=$($state.cameraId)" $null 200)
    Assert-Workflow ($history.Count -eq 2 -and @($history | Where-Object { $_.returnDate -ne $state.today }).Count -eq 0 -and @($history | Where-Object { $_.id -eq $state.activeLoanId }).Count -eq 1 -and @($history | Where-Object { $_.id -eq $new.id }).Count -eq 1) 'both returned camera rows retained'
    $state.status = 'rehearsal-complete'
    Write-ElsDemoState $stateFile $state
    Show-Step 'PASS history: both camera loans retained; overdue list empty. Use guarded reset before seed/rerun.'
} finally { $http.Dispose(); $handler.Dispose() }
