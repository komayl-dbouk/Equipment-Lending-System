[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$ProjectName,
    [string]$EnvFile = '.env',
    [string]$BaseUrl,
    [string]$StatePath
)

. "$PSScriptRoot/common.ps1"
$context = Get-ElsDemoContext $ProjectName $EnvFile $BaseUrl
$stateFile = Get-ElsStatePath $ProjectName $StatePath

function Invoke-DemoRequest {
    param([string]$Method, [string]$Path, [object]$Body)
    $parameters = @{ Method = $Method; Uri = "$($context.BaseUrl)$Path"; ContentType = 'application/json'; TimeoutSec = 15
        Headers = $context.StaffHeaders; MaximumRedirection = 0 }
    if ($null -ne $Body) { $parameters.Body = $Body | ConvertTo-Json -Depth 5 }
    try { $result = Invoke-RestMethod @parameters }
    catch { throw 'Demo API request failed; inspect the owned application without printing credentials.' }
    return $result
}

function Assert-DemoFields {
    param($Actual, $Expected)
    if ($null -eq $Actual) { throw 'A demo fixture is missing.' }
    foreach ($field in $Expected.Keys) {
        if ($Actual.$field -cne $Expected[$field]) { throw "Demo fixture field $field changed; inspect before resetting this disposable project." }
    }
}

function Assert-DemoState {
    param($State)
    $student = Invoke-DemoRequest Get "/api/students/$($State.studentId)" $null
    Assert-DemoFields $student @{ id = $State.studentId; name = 'ELS Demo Borrower'; email = 'els45.demo.borrower@example.test' }
    $equipment = @(Invoke-DemoRequest Get '/api/equipment' $null)
    $camera = @($equipment | Where-Object { $_.id -eq $State.cameraId })
    $laptop = @($equipment | Where-Object { $_.id -eq $State.laptopId })
    if ($camera.Count -ne 1 -or $laptop.Count -ne 1) { throw 'Expected exactly one fixture per equipment ID.' }
    Assert-DemoFields $camera[0] @{ inventoryCode = 'ELS45-CAM-001'; name = 'ELS Demo Camera'; category = 'Camera'; description = 'Synthetic delivery demonstration item'; available = $false }
    Assert-DemoFields $laptop[0] @{ inventoryCode = 'ELS45-LAP-001'; name = 'ELS Demo Laptop'; category = 'Laptop'; description = 'Synthetic returned-history item'; available = $true }
    $loans = @(Invoke-DemoRequest Get "/api/loans?studentId=$($State.studentId)" $null)
    $active = @($loans | Where-Object { $_.id -eq $State.activeLoanId })
    $returned = @($loans | Where-Object { $_.id -eq $State.returnedLoanId })
    if ($loans.Count -ne 2 -or $active.Count -ne 1 -or $returned.Count -ne 1) { throw 'Demo lending history changed.' }
    Assert-DemoFields $active[0] @{ studentId = $State.studentId; equipmentId = $State.cameraId; borrowingDate = $State.today; dueDate = $State.activeDueDate; returnDate = $null }
    Assert-DemoFields $returned[0] @{ studentId = $State.studentId; equipmentId = $State.laptopId; borrowingDate = $State.previousBorrowingDate; dueDate = $State.previousDueDate; returnDate = $State.today }
}

if (Test-Path -LiteralPath $stateFile) {
    $state = Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json
    if ($state.version -ne 2 -or $state.project -ne $ProjectName -or $state.checkout -ne $context.Checkout -or
        $state.baseUrl -ne $context.BaseUrl -or $state.databaseId -ne $context.DatabaseId) { throw 'Checkpoint belongs to another target; no write was attempted.' }
    if ($state.status -ne 'complete') { throw 'A previous seed was interrupted. Inspect and explicitly reset only this disposable project; no retry write was attempted.' }
    Assert-DemoState $state
    Write-Output 'Synthetic fixtures and target identity verified; no write was made.'
    return
}

$sql = 'SELECT (SELECT count(*) FROM student),(SELECT count(*) FROM equipment),(SELECT count(*) FROM loan);'
$counts = @(Invoke-ElsDocker -Arguments @('exec', $context.DatabaseContainer, 'psql', '-v', 'ON_ERROR_STOP=1', '-U', $context.DatabaseUser, '-d', $context.DatabaseName, '-At', '-c', $sql))
if ($counts.Count -ne 1 -or $counts[0] -ne '0|0|0') { throw 'Initial seeding requires an empty verified demo database; no write was attempted.' }
try { $zone = [TimeZoneInfo]::FindSystemTimeZoneById('Asia/Beirut') }
catch { $zone = [TimeZoneInfo]::FindSystemTimeZoneById('Middle East Standard Time') }
$today = [TimeZoneInfo]::ConvertTimeFromUtc([DateTime]::UtcNow, $zone).Date
$culture = [Globalization.CultureInfo]::InvariantCulture
$state = [ordered]@{
    version = 2; project = $ProjectName; checkout = $context.Checkout; baseUrl = $context.BaseUrl; databaseId = $context.DatabaseId; status = 'seeding'
    today = $today.ToString('yyyy-MM-dd', $culture); activeDueDate = $today.AddDays(7).ToString('yyyy-MM-dd', $culture)
    previousBorrowingDate = $today.AddDays(-7).ToString('yyyy-MM-dd', $culture); previousDueDate = $today.AddDays(-1).ToString('yyyy-MM-dd', $culture)
    studentId = $null; cameraId = $null; laptopId = $null; activeLoanId = $null; returnedLoanId = $null
}
Write-ElsDemoState $stateFile $state
$student = Invoke-DemoRequest Post '/api/students' @{ name = 'ELS Demo Borrower'; email = 'els45.demo.borrower@example.test' }
$state.studentId = $student.id; Write-ElsDemoState $stateFile $state
$camera = Invoke-DemoRequest Post '/api/equipment' @{ inventoryCode = 'ELS45-CAM-001'; name = 'ELS Demo Camera'; category = 'Camera'; description = 'Synthetic delivery demonstration item' }
$state.cameraId = $camera.id; Write-ElsDemoState $stateFile $state
$laptop = Invoke-DemoRequest Post '/api/equipment' @{ inventoryCode = 'ELS45-LAP-001'; name = 'ELS Demo Laptop'; category = 'Laptop'; description = 'Synthetic returned-history item' }
$state.laptopId = $laptop.id; Write-ElsDemoState $stateFile $state
$activeLoan = Invoke-DemoRequest Post '/api/loans' @{ studentId = $state.studentId; equipmentId = $state.cameraId; borrowingDate = $state.today; dueDate = $state.activeDueDate }
$state.activeLoanId = $activeLoan.id; Write-ElsDemoState $stateFile $state
$returnedLoan = Invoke-DemoRequest Post '/api/loans' @{ studentId = $state.studentId; equipmentId = $state.laptopId; borrowingDate = $state.previousBorrowingDate; dueDate = $state.previousDueDate }
$state.returnedLoanId = $returnedLoan.id; Write-ElsDemoState $stateFile $state
Invoke-DemoRequest Post "/api/loans/$($state.returnedLoanId)/return" @{ returnDate = $state.today } | Out-Null
Assert-DemoState $state
$state.status = 'complete'; Write-ElsDemoState $stateFile $state
Write-Output 'Created and verified the owned demo student, equipment and active/returned history.'
