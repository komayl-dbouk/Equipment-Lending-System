[CmdletBinding()]
param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$StatePath = "target/els45-demo-state.json"
)

$ErrorActionPreference = "Stop"
$base = $BaseUrl.TrimEnd("/")

function Invoke-DemoRequest {
    param([string]$Method, [string]$Path, [object]$Body)
    $parameters = @{ Method = $Method; Uri = "$base$Path"; ContentType = "application/json" }
    if ($null -ne $Body) {
        $parameters.Body = $Body | ConvertTo-Json -Depth 4
    }
    Invoke-RestMethod @parameters
}

if (Test-Path -LiteralPath $StatePath) {
    $state = Get-Content -Raw -LiteralPath $StatePath | ConvertFrom-Json
    $student = Invoke-DemoRequest -Method Get -Path "/api/students/$($state.studentId)" -Body $null
    $equipment = Invoke-DemoRequest -Method Get -Path "/api/equipment" -Body $null
    $loans = Invoke-DemoRequest -Method Get -Path "/api/loans?studentId=$($state.studentId)" -Body $null
    $missing = @($state.equipmentIds | Where-Object { $_ -notin @($equipment.id) })
    $activeLoan = @($loans | Where-Object { $_.id -eq $state.activeLoanId -and $null -eq $_.returnDate })
    $returnedLoan = @($loans | Where-Object { $_.id -eq $state.returnedLoanId -and $null -ne $_.returnDate })
    if ($student.id -ne $state.studentId -or $missing.Count -ne 0 -or $activeLoan.Count -ne 1 -or $returnedLoan.Count -ne 1) {
        throw "Demo state does not match the running database. Reset only the dedicated demo Compose project, then seed again."
    }
    Write-Output "Synthetic demo data already exists; no write was made. State: $StatePath"
    exit 0
}

$today = (Get-Date).Date
$student = Invoke-DemoRequest -Method Post -Path "/api/students" -Body @{
    name = "ELS Demo Borrower"
    email = "els45.demo.borrower@example.test"
}
$camera = Invoke-DemoRequest -Method Post -Path "/api/equipment" -Body @{
    inventoryCode = "ELS45-CAM-001"
    name = "ELS Demo Camera"
    category = "Camera"
    description = "Synthetic delivery demonstration item"
}
$laptop = Invoke-DemoRequest -Method Post -Path "/api/equipment" -Body @{
    inventoryCode = "ELS45-LAP-001"
    name = "ELS Demo Laptop"
    category = "Laptop"
    description = "Synthetic returned-history item"
}
$activeLoan = Invoke-DemoRequest -Method Post -Path "/api/loans" -Body @{
    studentId = $student.id
    equipmentId = $camera.id
    borrowingDate = $today.ToString("yyyy-MM-dd")
    dueDate = $today.AddDays(7).ToString("yyyy-MM-dd")
}
$returnedLoan = Invoke-DemoRequest -Method Post -Path "/api/loans" -Body @{
    studentId = $student.id
    equipmentId = $laptop.id
    borrowingDate = $today.AddDays(-7).ToString("yyyy-MM-dd")
    dueDate = $today.AddDays(-1).ToString("yyyy-MM-dd")
}
Invoke-DemoRequest -Method Post -Path "/api/loans/$($returnedLoan.id)/return" -Body @{ returnDate = $today.ToString("yyyy-MM-dd") } | Out-Null
$loans = Invoke-DemoRequest -Method Get -Path "/api/loans?studentId=$($student.id)" -Body $null
$equipment = Invoke-DemoRequest -Method Get -Path "/api/equipment" -Body $null
if ($loans.Count -ne 2 -or @($equipment | Where-Object { $_.id -eq $camera.id -and $_.available }).Count -ne 0 `
        -or @($equipment | Where-Object { $_.id -eq $laptop.id -and -not $_.available }).Count -ne 0) {
    throw "The synthetic workflow did not persist the expected active and returned loan state."
}

$parent = Split-Path -Parent $StatePath
if ($parent) { New-Item -ItemType Directory -Force -Path $parent | Out-Null }
@{
    studentId = $student.id
    equipmentIds = @($camera.id, $laptop.id)
    activeLoanId = $activeLoan.id
    returnedLoanId = $returnedLoan.id
} | ConvertTo-Json | Set-Content -NoNewline -LiteralPath $StatePath
Write-Output "Created and verified synthetic demo student, two equipment items, one active loan and one returned loan. State: $StatePath"
