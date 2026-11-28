[CmdletBinding()]
param([string]$EnvFile = '.env', [string]$ProjectName = 'els-tests')

. "$PSScriptRoot/common.ps1"
$compose = Get-ElsComposeConfig $ProjectName $EnvFile 'compose.test.yaml'
$db = $compose.Config.services.postgres
$ports = @($db.ports)
if ($ports.Count -ne 1 -or $ports[0].host_ip -ne '127.0.0.1' -or [int]$ports[0].target -ne 5432 -or
    $db.environment.POSTGRES_DB -notmatch '_test$') { throw 'Expected a separate loopback PostgreSQL test database.' }
$env:ELS_TEST_DB_USERNAME = [string]$db.environment.POSTGRES_USER
$env:ELS_TEST_DB_PASSWORD = [string]$db.environment.POSTGRES_PASSWORD
$env:ELS_TEST_DB_URL = "jdbc:postgresql://127.0.0.1:$($ports[0].published)/$($db.environment.POSTGRES_DB)"
Write-Output 'Test environment loaded from resolved Compose values; credentials were not printed.'
