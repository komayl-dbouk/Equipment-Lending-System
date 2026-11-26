[CmdletBinding()]
param(
    [string]$BaseUrl = $(if ($env:ELS_API_BASE_URL) { $env:ELS_API_BASE_URL } else { 'http://localhost:8080' })
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Net.Http
$BaseUrl = $BaseUrl.TrimEnd('/')
$http = [System.Net.Http.HttpClient]::new()

function Assert-Equal {
    param([object]$Actual, [object]$Expected, [string]$Label)
    if ($Actual -ne $Expected) {
        throw "${Label}: expected '$Expected', got '$Actual'"
    }
}

function Assert-True {
    param([bool]$Condition, [string]$Label)
    if (-not $Condition) { throw "Assertion failed: $Label" }
}

function Invoke-Api {
    param(
        [string]$Method,
        [string]$Path,
        [int]$ExpectedStatus,
        [object]$Body,
        [string]$ExpectedCode
    )

    $request = [System.Net.Http.HttpRequestMessage]::new(
        [System.Net.Http.HttpMethod]::new($Method), "$BaseUrl$Path")
    if ($null -ne $Body) {
        $json = $Body | ConvertTo-Json -Compress -Depth 10
        $request.Content = [System.Net.Http.StringContent]::new(
            $json, [System.Text.Encoding]::UTF8, 'application/json')
    }

    $response = $http.SendAsync($request).GetAwaiter().GetResult()
    $text = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    $status = [int]$response.StatusCode
    Assert-Equal $status $ExpectedStatus "$Method $Path status"

    $value = if ([string]::IsNullOrWhiteSpace($text)) { $null } else { $text | ConvertFrom-Json }
    if ($ExpectedCode) {
        Assert-Equal $value.code $ExpectedCode "$Method $Path business code"
        Assert-Equal $value.status $ExpectedStatus "$Method $Path error status field"
        Assert-Equal $value.path ($Path -split '\?')[0] "$Method $Path error path"
    }

    [pscustomobject]@{
        Status = $status
        Value = $value
        Headers = $response.Headers
    }
}

function Contains-Id {
    param([object[]]$Values, [long]$Id)
    return $null -ne ($Values | Where-Object { $_.id -eq $Id } | Select-Object -First 1)
}

function Assert-AscendingIds {
    param([object[]]$Values, [string]$Label)
    $ids = @($Values | ForEach-Object { [long]$_.id })
    $sorted = @($ids | Sort-Object)
    Assert-Equal ($ids -join ',') ($sorted -join ',') "$Label ascending IDs"
}

function Get-BeirutDate {
    $zone = $null
    foreach ($name in @('Asia/Beirut', 'Middle East Standard Time')) {
        try {
            $zone = [TimeZoneInfo]::FindSystemTimeZoneById($name)
            break
        } catch { }
    }
    if ($null -eq $zone) { throw 'Asia/Beirut time zone data is unavailable' }
    return [TimeZoneInfo]::ConvertTime([DateTimeOffset]::UtcNow, $zone).Date
}

try {
    $suffix = [Guid]::NewGuid().ToString('N').Substring(0, 12)
    $today = Get-BeirutDate
    $borrowed = $today.AddDays(-3).ToString('yyyy-MM-dd')
    $pastDue = $today.AddDays(-2).ToString('yyyy-MM-dd')
    $returned = $today.AddDays(-1).ToString('yyyy-MM-dd')
    $todayText = $today.ToString('yyyy-MM-dd')
    $futureDue = $today.AddDays(1).ToString('yyyy-MM-dd')

    Invoke-Api GET '/actuator/health' 200 | Out-Null

    $studentResponse = Invoke-Api POST '/api/students' 201 @{
        name = ' ELS-37 Synthetic Student '
        email = " ELS37.$suffix@Example.test "
    }
    $student = $studentResponse.Value
    $studentId = [long]$student.id
    Assert-True ($studentId -gt 0) 'student ID is generated and positive'
    Assert-Equal $student.name 'ELS-37 Synthetic Student' 'normalized student name'
    Assert-Equal $student.email "els37.$suffix@example.test" 'normalized student email'
    Assert-Equal ($studentResponse.Headers.Location.ToString()) "/api/students/$studentId" 'student Location'

    $stored = (Invoke-Api GET "/api/students/$studentId" 200).Value
    Assert-Equal $stored.id $studentId 'retrieved student ID'

    $updated = (Invoke-Api PUT "/api/students/$studentId" 200 @{
        name = ' ELS-37 Updated Student '
        email = " ELS37.$suffix@Example.test "
    }).Value
    Assert-Equal $updated.name 'ELS-37 Updated Student' 'updated student name'

    $equipment = (Invoke-Api POST '/api/equipment' 201 @{
        inventoryCode = " els37-$suffix "
        name = ' Synthetic Camera '
        category = ' Camera '
        description = ' ELS-37 reproducible workflow '
    }).Value
    $equipmentId = [long]$equipment.id
    Assert-True ($equipmentId -gt 0) 'equipment ID is generated and positive'
    Assert-Equal $equipment.inventoryCode "ELS37-$($suffix.ToUpperInvariant())" 'normalized inventory code'
    Assert-Equal $equipment.available $true 'new equipment is available'

    $available = @((Invoke-Api GET '/api/equipment?available=true' 200).Value)
    Assert-AscendingIds $available 'available equipment'
    Assert-True (Contains-Id $available $equipmentId) 'new equipment appears in available=true'

    $firstLoan = (Invoke-Api POST '/api/loans' 201 @{
        studentId = $studentId
        equipmentId = $equipmentId
        borrowingDate = $borrowed
        dueDate = $pastDue
    }).Value
    $firstLoanId = [long]$firstLoan.id
    Assert-True ($firstLoanId -gt 0) 'first loan ID is generated and positive'
    Assert-Equal $firstLoan.returnDate $null 'new loan returnDate'

    Invoke-Api POST '/api/loans' 409 @{
        studentId = $studentId
        equipmentId = $equipmentId
        borrowingDate = $todayText
        dueDate = $futureDue
    } 'EQUIPMENT_UNAVAILABLE' | Out-Null

    $unavailable = @((Invoke-Api GET '/api/equipment?available=false' 200).Value)
    Assert-AscendingIds $unavailable 'unavailable equipment'
    Assert-True (Contains-Id $unavailable $equipmentId) 'active item appears in available=false'

    $filterPath = "/api/loans?studentId=$studentId&equipmentId=$equipmentId"
    $history = @((Invoke-Api GET $filterPath 200).Value)
    Assert-AscendingIds $history 'combined-filter history'
    Assert-True (Contains-Id $history $firstLoanId) 'combined filters include the first loan'

    $overdue = @((Invoke-Api GET '/api/loans/overdue' 200).Value)
    Assert-AscendingIds $overdue 'overdue loans'
    Assert-True (Contains-Id $overdue $firstLoanId) 'past-due active loan is overdue'

    $returnedLoan = (Invoke-Api POST "/api/loans/$firstLoanId/return" 200 @{
        returnDate = $returned
    }).Value
    Assert-Equal $returnedLoan.returnDate $returned 'recorded return date'
    Invoke-Api POST "/api/loans/$firstLoanId/return" 409 @{
        returnDate = $todayText
    } 'LOAN_ALREADY_RETURNED' | Out-Null

    $availableAgain = @((Invoke-Api GET '/api/equipment?available=true' 200).Value)
    Assert-True (Contains-Id $availableAgain $equipmentId) 'returned item is available again'

    $secondLoan = (Invoke-Api POST '/api/loans' 201 @{
        studentId = $studentId
        equipmentId = $equipmentId
        borrowingDate = $todayText
        dueDate = $futureDue
    }).Value
    $secondLoanId = [long]$secondLoan.id
    Assert-True ($secondLoanId -ne $firstLoanId) 'reborrow creates a distinct loan'

    $finalHistory = @((Invoke-Api GET $filterPath 200).Value)
    Assert-AscendingIds $finalHistory 'retained loan history'
    Assert-True (Contains-Id $finalHistory $firstLoanId) 'returned loan remains in history'
    Assert-True (Contains-Id $finalHistory $secondLoanId) 'new active loan appears in history'
    $preserved = $finalHistory | Where-Object { $_.id -eq $firstLoanId } | Select-Object -First 1
    Assert-Equal $preserved.returnDate $returned 'first return date is immutable'

    $finalOverdue = @((Invoke-Api GET '/api/loans/overdue' 200).Value)
    Assert-True (-not (Contains-Id $finalOverdue $firstLoanId)) 'returned loan is not overdue'
    Assert-True (-not (Contains-Id $finalOverdue $secondLoanId)) 'future-due active loan is not overdue'

    Invoke-Api DELETE "/api/equipment/$equipmentId" 409 $null 'EQUIPMENT_HAS_HISTORY' | Out-Null

    Write-Output "PASS: ELS-37 workflow executed with student=$studentId equipment=$equipmentId loans=$firstLoanId,$secondLoanId"
} finally {
    $http.Dispose()
}
