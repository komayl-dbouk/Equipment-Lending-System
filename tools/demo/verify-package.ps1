[CmdletBinding()]
param([string]$ProjectName = 'els-demo-smoke', [ValidateRange(1024,65535)][int]$Port = 18045)

. "$PSScriptRoot/common.ps1"
Assert-ElsDemoProject $ProjectName
$existing = @(Invoke-ElsDocker -Arguments @('ps', '-a', '--filter', "label=com.docker.compose.project=$ProjectName", '-q'))
$volumes = @(Invoke-ElsDocker -Arguments @('volume', 'ls', '--filter', "label=com.docker.compose.project=$ProjectName", '-q'))
$checkpoint = Get-ElsStatePath $ProjectName ''
if ($existing.Count -or $volumes.Count -or (Test-Path -LiteralPath $checkpoint)) { throw 'Smoke check requires an unused project and checkpoint.' }
$privateFile = Join-Path $script:ElsRepoRoot ("target/smoke-" + [Guid]::NewGuid().ToString('N') + '.env')
New-Item -ItemType Directory -Force (Split-Path -Parent $privateFile) | Out-Null
$settings = @{
    ELS_DB_USERNAME = 'equipment_demo'; ELS_DB_PASSWORD = ([Guid]::NewGuid().ToString('N') + ' spaced # literal$')
    ELS_DB_NAME = 'equipment_lending_demo'; ELS_APP_PORT = [string]$Port
}
$old = @{}
foreach ($name in $settings.Keys) { $old[$name] = [Environment]::GetEnvironmentVariable($name, 'Process') }
try {
    [IO.File]::WriteAllLines($privateFile, @($settings.Keys | ForEach-Object { "$_='$($settings[$_])'" }))
    foreach ($name in $settings.Keys) { [Environment]::SetEnvironmentVariable($name, $settings[$name], 'Process') }
    $compose = Get-ElsComposeConfig $ProjectName $privateFile
    Invoke-ElsDocker -Arguments ($compose.Arguments + @('up', '--build', '-d', '--wait')) | Out-Null
    & "$PSScriptRoot/wait-for-health.ps1" -BaseUrl "http://127.0.0.1:$Port" -TimeoutSeconds 60
    & "$PSScriptRoot/seed-demo.ps1" -ProjectName $ProjectName -EnvFile $privateFile
    $context = Get-ElsDemoContext $ProjectName $privateFile ''
    $countSql = 'SELECT (SELECT count(*) FROM student),(SELECT count(*) FROM equipment),(SELECT count(*) FROM loan);'
    $psql = @('exec', $context.DatabaseContainer, 'psql', '-v', 'ON_ERROR_STOP=1', '-U', $context.DatabaseUser, '-d', $context.DatabaseName, '-At', '-c')
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
    Write-Output 'PASS: real container build, non-root/loopback/identity checks, seed/rerun, drift rejection and reset/reseed.'
} finally {
    try {
        if ($compose) { Invoke-ElsDocker -Arguments ($compose.Arguments + @('down', '-v')) | Out-Null }
        if (Test-Path -LiteralPath $checkpoint) { Remove-Item -LiteralPath $checkpoint -Force }
    } finally {
        if (Test-Path -LiteralPath $privateFile) { Remove-Item -LiteralPath $privateFile -Force }
        foreach ($name in $old.Keys) { [Environment]::SetEnvironmentVariable($name, $old[$name], 'Process') }
    }
}
