param(
    [string]$Psql = $env:ELS_PSQL,
    [int]$Port = 18036,
    [string]$ExpectedDatabase = 'equipment_lending_workflow_test',
    [string]$OutputPath = 'target/els36-http-results.json'
)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$originalLocation = Get-Location
$appProcess = $null
try {
    Set-Location -LiteralPath $repo
    if (-not $Psql) { $Psql = 'psql' }
    foreach ($name in @('JAVA_HOME','ELS_DB_URL','ELS_DB_USERNAME','ELS_DB_PASSWORD',
            'ELS_TEST_DB_URL','ELS_TEST_DB_USERNAME','ELS_TEST_DB_PASSWORD')) {
        if (-not [Environment]::GetEnvironmentVariable($name)) { throw "Required environment variable missing: $name" }
    }
    if ($Port -lt 1024 -or $Port -gt 65535) { throw 'Choose an unprivileged validation port' }
    $java = Join-Path $env:JAVA_HOME 'bin/java.exe'
    $appJar = Join-Path $repo 'target/equipment-lending-system-0.0.1-SNAPSHOT.jar'
    $fixtureClass = 'elsvalidation/FixedClockConfiguration.class'
    $fixtureJar = Join-Path $repo 'target/els36-fixed-clock.jar'
    if (-not (Test-Path -LiteralPath $appJar)) { throw 'Run mvnw.cmd verify first' }
    if (-not (Test-Path -LiteralPath (Join-Path $repo "target/test-classes/$fixtureClass"))) {
        throw 'Compile test sources first; the fixed Clock fixture is test-only'
    }
    # Refuse an occupied socket; cleanup must never stop another developer's application.
    $listener = New-Object System.Net.Sockets.TcpListener([Net.IPAddress]::Loopback,$Port)
    try { $listener.Start() } finally { $listener.Stop() }
    & python tools/validation/lending_workflow.py --psql $Psql --expected-database $ExpectedDatabase --preflight-only
    if ($LASTEXITCODE -ne 0) { throw 'Disposable database preflight failed' }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($appJar)
    try {
        if (@($archive.Entries | Where-Object { $_.FullName -match 'elsvalidation|LendingIntegrityIT|DatabaseTestSafetyProcessor' }).Count) {
            throw 'Test-only classes unexpectedly present in delivery JAR'
        }
    } finally { $archive.Dispose() }
    # A single-class fixture JAR needs no manifest. Avoid PowerShell treating harmless
    # JAVA_TOOL_OPTIONS output from jar.exe on stderr as a terminating native error.
    $fixtureStream = [IO.File]::Open($fixtureJar, [IO.FileMode]::Create)
    $fixtureArchive = New-Object IO.Compression.ZipArchive($fixtureStream, [IO.Compression.ZipArchiveMode]::Create)
    try {
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($fixtureArchive,
            (Join-Path $repo "target/test-classes/$fixtureClass"), $fixtureClass) | Out-Null
    } finally { $fixtureArchive.Dispose(); $fixtureStream.Dispose() }
    $appProcess = Start-Process -FilePath $java -WindowStyle Hidden -PassThru -ArgumentList @(
        ('"-Dloader.path=' + $fixtureJar + '"'), '-cp', ('"' + $appJar + '"'),
        'org.springframework.boot.loader.launch.PropertiesLauncher',
        '--spring.main.sources=elsvalidation.FixedClockConfiguration',
        '--spring.profiles.active=validation-workflow', '--server.address=127.0.0.1',
        "--server.port=$Port", '--logging.level.root=ERROR',
        '--spring.datasource.hikari.data-source-properties.ApplicationName=els36-workflow'
    ) -RedirectStandardOutput (Join-Path $repo 'target/els36-app.log') `
      -RedirectStandardError (Join-Path $repo 'target/els36-app-error.log')
    $baseUrl = "http://127.0.0.1:$Port"
    $deadline = [DateTime]::UtcNow.AddSeconds(60)
    $ready = $false
    do {
        $appProcess.Refresh()
        if ($appProcess.HasExited) { throw 'Packaged application exited; inspect ignored target/els36-app*.log' }
        try {
            $health = Invoke-WebRequest -UseBasicParsing -Uri "$baseUrl/actuator/health" -TimeoutSec 2
            $ready = $health.StatusCode -eq 200
        } catch { }
        if (-not $ready) { [Threading.Tasks.Task]::Delay(250).Wait() }
    } while (-not $ready -and [DateTime]::UtcNow -lt $deadline)
    if (-not $ready) { throw 'Packaged application readiness timeout' }
    & python tools/validation/lending_workflow.py --base-url $baseUrl --psql $Psql `
        --expected-database $ExpectedDatabase --output $OutputPath
    if ($LASTEXITCODE -ne 0) { throw 'Packaged HTTP/SQL validation failed; inspect the results JSON' }
} finally {
    if ($appProcess) {
        $appProcess.Refresh()
        if (-not $appProcess.HasExited) { Stop-Process -Id $appProcess.Id -Force }
    }
    Set-Location -LiteralPath $originalLocation
}
