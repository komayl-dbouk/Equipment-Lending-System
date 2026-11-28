[CmdletBinding()]
param([string]$ProjectName = 'els-demo-smoke', [ValidateRange(1024,65535)][int]$Port = 18045, [string]$EvidencePath)

. "$PSScriptRoot/common.ps1"
Assert-ElsDemoProject $ProjectName
$existing = @(Invoke-ElsDocker -Arguments @('ps', '-a', '--filter', "label=com.docker.compose.project=$ProjectName", '-q'))
$volumes = @(Invoke-ElsDocker -Arguments @('volume', 'ls', '--filter', "label=com.docker.compose.project=$ProjectName", '-q'))
$checkpoint = Get-ElsStatePath $ProjectName ''
$evidenceFile = if ($EvidencePath) { Get-ElsStatePath $ProjectName $EvidencePath } else { $null }
if ($evidenceFile -and ($evidenceFile -eq $checkpoint -or (Test-Path -LiteralPath $evidenceFile))) { throw 'Evidence requires a new target path separate from the checkpoint.' }
if ($existing.Count -or $volumes.Count -or (Test-Path -LiteralPath $checkpoint)) { throw 'Smoke check requires an unused project and checkpoint.' }
$privateFile = Join-Path $script:ElsRepoRoot ("target/smoke-" + [Guid]::NewGuid().ToString('N') + '.env')
New-Item -ItemType Directory -Force (Split-Path -Parent $privateFile) | Out-Null
$settings = @{
    ELS_DB_USERNAME = 'equipment_demo'; ELS_DB_PASSWORD = ([Guid]::NewGuid().ToString('N') + ' spaced # literal$')
    ELS_DB_NAME = 'equipment_lending_demo'; ELS_APP_PORT = [string]$Port
    ELS_STAFF_USERNAME = 'demo-staff'; ELS_STAFF_PASSWORD = ([Guid]::NewGuid().ToString('N') + ' staff # literal$')
}
$old = @{}
foreach ($name in $settings.Keys) { $old[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
try {
    [IO.File]::WriteAllLines($privateFile, @($settings.Keys | ForEach-Object { "$_='$($settings[$_])'" }))
    foreach ($name in $settings.Keys) { [Environment]::SetEnvironmentVariable($name, $settings[$name], 'Process') }
    $compose = Get-ElsComposeConfig $ProjectName $privateFile
    Invoke-ElsDocker -Arguments ($compose.Arguments + @('up', '--build', '-d', '--wait')) | Out-Null
    & "$PSScriptRoot/wait-for-health.ps1" -BaseUrl "http://127.0.0.1:$Port" -TimeoutSeconds 60
    $context = Get-ElsDemoContext $ProjectName $privateFile ''
    $countSql = 'SELECT (SELECT count(*) FROM student),(SELECT count(*) FROM equipment),(SELECT count(*) FROM loan);'
    $psql = @('exec', $context.DatabaseContainer, 'psql', '-v', 'ON_ERROR_STOP=1', '-U', $context.DatabaseUser, '-d', $context.DatabaseName, '-At', '-c')
    $empty = Invoke-ElsDocker -Arguments ($psql + @($countSql))
    if ($empty -ne '0|0|0') { throw 'Access probes require an empty owned database.' }
    Add-Type -AssemblyName System.Net.Http
    $handler = [System.Net.Http.HttpClientHandler]::new()
    $handler.AllowAutoRedirect = $false
    $http = [System.Net.Http.HttpClient]::new($handler)
    $http.Timeout = [TimeSpan]::FromSeconds(10)
    try {
        foreach ($invalid in @($false, $true)) {
            foreach ($path in @('/api/equipment', '/api/students')) {
                $method = if ($path -eq '/api/students') { 'POST' } else { 'GET' }
                $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::new($method), "$($context.BaseUrl)$path")
                $response = $null
                try {
                    if ($invalid) { $request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Basic', [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('invalid-synthetic:invalid-secret'))) }
                    if ($method -eq 'POST') { $request.Content = [System.Net.Http.StringContent]::new('{"name":"Rejected Synthetic","email":"rejected@example.test"}', [Text.Encoding]::UTF8, 'application/json') }
                    $response = $http.SendAsync($request).GetAwaiter().GetResult()
                    $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
                    if ([int]$response.StatusCode -ne 401 -or $body.status -ne 401 -or $body.code -ne 'UNAUTHORIZED' -or
                        $body.message -ne 'Staff credentials required' -or $body.path -ne $path -or
                        @($body.PSObject.Properties).Count -ne 6 -or $null -eq $body.fieldErrors -or @($body.fieldErrors.PSObject.Properties).Count -ne 0 -or
                        [string]$response.Headers.WwwAuthenticate -ne 'Basic realm="equipment-lending"') { throw 'Missing/invalid staff access did not return the sanitized challenge.' }
                } finally { if ($response) { $response.Dispose() }; $request.Dispose() }
            }
        }
        if ((Invoke-ElsDocker -Arguments ($psql + @($countSql))) -ne $empty) { throw 'Rejected access changed committed state.' }
        & "$PSScriptRoot/seed-demo.ps1" -ProjectName $ProjectName -EnvFile $privateFile
        $checkpointText = Get-Content -LiteralPath $checkpoint -Raw
        foreach ($private in @($settings.ELS_STAFF_PASSWORD, $settings.ELS_DB_PASSWORD, $context.StaffHeaders.Authorization)) {
            if ($checkpointText.Contains($private)) { throw 'Checkpoint retained a private credential.' }
        }
        $request = [System.Net.Http.HttpRequestMessage]::new([System.Net.Http.HttpMethod]::Post, "$($context.BaseUrl)/api/students")
        $response = $null
        try {
            $request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::Parse($context.StaffHeaders.Authorization)
            $request.Content = [System.Net.Http.StringContent]::new('{"name":"Duplicate Synthetic","email":"els45.demo.borrower@example.test"}', [Text.Encoding]::UTF8, 'application/json')
            $response = $http.SendAsync($request).GetAwaiter().GetResult()
            $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
            if ([int]$response.StatusCode -ne 409 -or $body.code -ne 'DUPLICATE_EMAIL') { throw 'Duplicate-email privacy probe did not exercise the expected conflict.' }
        } finally { if ($response) { $response.Dispose() }; $request.Dispose() }
        $appLogs = (Invoke-ElsDocker -Arguments ($compose.Arguments + @('logs', '--no-color', 'app'))) -join "`n"
        foreach ($sensitive in @($settings.ELS_STAFF_USERNAME, $settings.ELS_STAFF_PASSWORD, $settings.ELS_DB_PASSWORD, $context.StaffHeaders.Authorization, 'els45.demo.borrower@example.test', 'rejected@example.test', 'Key (email)=')) {
            if ($appLogs.Contains($sensitive)) { throw 'Packaged application logs exposed private or student data.' }
        }
    } finally { $http.Dispose() }
    $before = Invoke-ElsDocker -Arguments ($psql + @($countSql))
    & "$PSScriptRoot/seed-demo.ps1" -ProjectName $ProjectName -EnvFile $privateFile
    $after = Invoke-ElsDocker -Arguments ($psql + @($countSql))
    if ($before -ne '1|2|2' -or $after -ne $before) { throw 'Seed/rerun changed unexpected persisted state.' }
    $user = Invoke-ElsDocker -Arguments @('exec', (Invoke-ElsDocker -Arguments ($compose.Arguments + @('ps', '-q', 'app'))), 'id', '-u')
    if ([string]$user -eq '0') { throw 'Application container runs as root.' }
    Invoke-ElsDocker -Arguments ($psql + @("UPDATE student SET name='Changed synthetic fixture';")) | Out-Null
    $rejected = $false
    try { & "$PSScriptRoot/seed-demo.ps1" -ProjectName $ProjectName -EnvFile $privateFile }
    catch { if ($_.Exception.Message -notmatch 'fixture field') { throw }; $rejected = $true }
    if (-not $rejected) { throw 'Fixture drift was accepted.' }
    & "$PSScriptRoot/reset-demo.ps1" -ProjectName $ProjectName -EnvFile $privateFile -ConfirmReset
    Invoke-ElsDocker -Arguments ($compose.Arguments + @('up', '-d', '--wait')) | Out-Null
    & "$PSScriptRoot/wait-for-health.ps1" -BaseUrl "http://127.0.0.1:$Port" -TimeoutSeconds 60
    & "$PSScriptRoot/seed-demo.ps1" -ProjectName $ProjectName -EnvFile $privateFile
    $context = Get-ElsDemoContext $ProjectName $privateFile ''
    $final = Invoke-ElsDocker -Arguments @('exec', $context.DatabaseContainer, 'psql', '-v', 'ON_ERROR_STOP=1', '-U', $context.DatabaseUser, '-d', $context.DatabaseName, '-At', '-c', $countSql)
    if ($final -ne '1|2|2') { throw 'Reset/reseed state differs from expected fixtures.' }
    if ($evidenceFile) {
        $source = & git -C $script:ElsRepoRoot rev-parse HEAD
        if ($LASTEXITCODE -ne 0) { throw 'Cannot identify the tested source.' }
        $appId = Invoke-ElsDocker -Arguments ($compose.Arguments + @('ps', '-q', 'app'))
        $imageId = Invoke-ElsDocker -Arguments @('inspect', '--format', '{{.Image}}', $appId)
        $packageEvidence = [ordered]@{
            card = 'ELS-47'; sourceCommit = [string]$source; status = 'passed'
            jarSha256 = (Get-FileHash -LiteralPath (Join-Path $script:ElsRepoRoot 'target/equipment-lending-system-0.0.1-SNAPSHOT.jar') -Algorithm SHA256).Hash.ToLowerInvariant()
            runtimeImageId = [string]$imageId; databaseIdentity = $context.DatabaseId
            assertions = @('public health', 'missing/wrong read and write rejected', 'independent unchanged SQL counts', 'authenticated seed and read-only rerun', 'checkpoint secrecy', 'production application log privacy', 'non-root process', 'loopback application/internal PostgreSQL', 'target/fixture drift rejection', 'reset/reseed counts 1|2|2')
        }
    }
    Write-Output 'PASS: real container build, missing/invalid access with no writes, authorized workflow, production log privacy, non-root/loopback/identity, seed/rerun, drift rejection and reset/reseed.'
} finally {
    try {
        if ($compose) { Invoke-ElsDocker -Arguments ($compose.Arguments + @('down', '-v')) | Out-Null }
        if (Test-Path -LiteralPath $checkpoint) { Remove-Item -LiteralPath $checkpoint -Force }
    } finally {
        if (Test-Path -LiteralPath $privateFile) { Remove-Item -LiteralPath $privateFile -Force }
        foreach ($name in $old.Keys) { [Environment]::SetEnvironmentVariable($name, $old[$name], 'Process') }
    }
}
if ($evidenceFile) {
    $packageEvidence.cleanup = 'passed; owned project removed by successful Compose down -v, checkpoint and private configuration removed'
    Write-ElsDemoState $evidenceFile $packageEvidence
}
