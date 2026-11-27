$ErrorActionPreference = 'Stop'
$script:ElsRepoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))

function Invoke-ElsDocker {
    param([string[]]$Arguments)
    Get-Command docker -ErrorAction Stop | Out-Null
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $output = & docker @Arguments 2>$null
        $nativeExit = $LASTEXITCODE
    } finally { $ErrorActionPreference = $previousPreference }
    if ($nativeExit -ne 0) { throw 'Docker operation failed; no successful result is assumed.' }
    return $output
}

function Assert-ElsDemoProject {
    param([string]$ProjectName)
    if ($ProjectName -cnotmatch '^els-demo-[a-z0-9][a-z0-9-]*$') { throw 'Use an explicitly disposable lowercase els-demo-* project.' }
}

function Get-ElsStatePath {
    param([string]$ProjectName, [string]$StatePath)
    if (-not $StatePath) { $StatePath = "target/els45-$ProjectName.json" }
    if (-not [IO.Path]::IsPathRooted($StatePath)) { $StatePath = Join-Path $script:ElsRepoRoot $StatePath }
    $full = [IO.Path]::GetFullPath($StatePath)
    $target = [IO.Path]::GetFullPath((Join-Path $script:ElsRepoRoot 'target')) + [IO.Path]::DirectorySeparatorChar
    if (-not $full.StartsWith($target, [StringComparison]::OrdinalIgnoreCase)) { throw 'Demo state must stay inside this checkout target directory.' }
    return $full
}

function Get-ElsComposeConfig {
    param([string]$ProjectName, [string]$EnvFile, [string]$ComposeFile = 'compose.yaml')
    if (-not [IO.Path]::IsPathRooted($EnvFile)) { $EnvFile = Join-Path $script:ElsRepoRoot $EnvFile }
    if (-not (Test-Path -LiteralPath $EnvFile -PathType Leaf)) { throw 'Private environment file not found.' }
    $composeArgs = @('compose', '-p', $ProjectName, '--env-file', $EnvFile, '-f', (Join-Path $script:ElsRepoRoot $ComposeFile))
    $config = (Invoke-ElsDocker -Arguments ($composeArgs + @('config', '--format', 'json'))) | ConvertFrom-Json
    # Compose escapes literal dollars when rendering a reusable configuration model.
    foreach ($service in $config.services.PSObject.Properties) {
        if ($service.Value.environment) {
            foreach ($entry in $service.Value.environment.PSObject.Properties) {
                if ($entry.Value -is [string]) { $entry.Value = $entry.Value.Replace('$$', '$') }
            }
        }
    }
    return [pscustomobject]@{ Config = $config; Arguments = $composeArgs }
}

function Assert-ElsContainerOwner {
    param($Container, [string]$ProjectName, [string]$Service)
    $labels = $Container.Config.Labels
    if ($labels.'com.docker.compose.project' -ne $ProjectName -or $labels.'com.docker.compose.service' -ne $Service -or
        [IO.Path]::GetFullPath($labels.'com.docker.compose.project.working_dir') -ne $script:ElsRepoRoot) { throw 'Container belongs to another project, checkout or service.' }
}

function Get-ElsDemoContext {
    param([string]$ProjectName, [string]$EnvFile, [string]$BaseUrl)
    Assert-ElsDemoProject $ProjectName
    $compose = Get-ElsComposeConfig $ProjectName $EnvFile
    $config = $compose.Config
    $ports = @($config.services.app.ports)
    if ($ports.Count -ne 1 -or $ports[0].host_ip -ne '127.0.0.1' -or [int]$ports[0].target -ne 8080) { throw 'Expected one loopback-only application port.' }
    $port = [int]$ports[0].published
    if ($BaseUrl) {
        $uri = $null
        if (-not [Uri]::TryCreate($BaseUrl, [UriKind]::Absolute, [ref]$uri) -or $uri.Scheme -ne 'http' -or
            $uri.Host -notin @('localhost', '127.0.0.1') -or $uri.Port -ne $port -or $uri.AbsolutePath -ne '/' -or
            $uri.UserInfo -or $uri.Query -or $uri.Fragment) { throw 'BaseUrl must match this demo project loopback port.' }
    }
    if (@($config.services.postgres.ports | Where-Object { $null -ne $_ }).Count -gt 0) { throw 'Demo PostgreSQL must not publish a host port.' }
    $appIds = @(Invoke-ElsDocker -Arguments ($compose.Arguments + @('ps', '-q', 'app')))
    $dbIds = @(Invoke-ElsDocker -Arguments ($compose.Arguments + @('ps', '-q', 'postgres')))
    if ($appIds.Count -ne 1 -or $dbIds.Count -ne 1) { throw 'Start exactly one app and database for this demo first.' }
    $app = @((Invoke-ElsDocker -Arguments @('inspect', $appIds[0])) | ConvertFrom-Json)[0]
    $db = @((Invoke-ElsDocker -Arguments @('inspect', $dbIds[0])) | ConvertFrom-Json)[0]
    Assert-ElsContainerOwner $app $ProjectName 'app'
    Assert-ElsContainerOwner $db $ProjectName 'postgres'
    $binding = @($app.NetworkSettings.Ports.'8080/tcp')
    if ($binding.Count -ne 1 -or $binding[0].HostIp -ne '127.0.0.1' -or [int]$binding[0].HostPort -ne $port) { throw 'Running application binding differs from demo config.' }
    foreach ($property in $db.NetworkSettings.Ports.PSObject.Properties) { if ($null -ne $property.Value) { throw 'Running PostgreSQL publishes a port.' } }
    $dbName = [string]$config.services.postgres.environment.POSTGRES_DB
    $dbUser = [string]$config.services.postgres.environment.POSTGRES_USER
    $appEnv = @{}
    foreach ($entry in $app.Config.Env) { $pair = $entry -split '=', 2; $appEnv[$pair[0]] = $pair[1] }
    if ($appEnv['ELS_DB_URL'] -ne "jdbc:postgresql://postgres:5432/$dbName" -or $appEnv['ELS_DB_USERNAME'] -ne $dbUser) { throw 'Running app uses another database configuration.' }
    $sql = 'SELECT current_database(),current_user,(SELECT oid FROM pg_database WHERE datname=current_database()),system_identifier FROM pg_control_system();'
    $identity = @(Invoke-ElsDocker -Arguments @('exec', $dbIds[0], 'psql', '-v', 'ON_ERROR_STOP=1', '-U', $dbUser, '-d', $dbName, '-At', '-c', $sql))
    $parts = ([string]$identity[0]).Split('|')
    if ($identity.Count -ne 1 -or $parts.Count -ne 4 -or $parts[0] -ne $dbName -or $parts[1] -ne $dbUser) { throw 'Cannot establish the effective demo database identity.' }
    return [pscustomobject]@{ BaseUrl = "http://127.0.0.1:$port"; ProjectName = $ProjectName; Checkout = $script:ElsRepoRoot
        DatabaseId = "$($parts[0])/$($parts[2])/$($parts[3])"; DatabaseName = $dbName; DatabaseUser = $dbUser; DatabaseContainer = $dbIds[0] }
}

function Write-ElsDemoState {
    param([string]$Path, $State)
    $parent = Split-Path -Parent $Path
    New-Item -ItemType Directory -Force -Path $parent | Out-Null
    $temporary = Join-Path $parent ([Guid]::NewGuid().ToString('N') + '.tmp')
    try {
        [IO.File]::WriteAllText($temporary, ($State | ConvertTo-Json -Depth 8), [Text.UTF8Encoding]::new($false))
        if (Test-Path -LiteralPath $Path) { [IO.File]::Replace($temporary, $Path, [NullString]::Value) }
        else { [IO.File]::Move($temporary, $Path) }
    } finally { if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary -Force } }
}
