[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$ProjectName,
    [switch]$ConfirmReset
)

$ErrorActionPreference = "Stop"
if ($ProjectName -notmatch '^els-demo-[a-z0-9][a-z0-9-]*$') {
    throw "ProjectName must begin with els-demo- so this command cannot target the normal developer or test database."
}
if (-not $ConfirmReset) {
    throw "Pass -ConfirmReset only after confirming this is a disposable demo project."
}

docker compose -p $ProjectName --env-file .env -f compose.yaml down -v
Remove-Item -LiteralPath "target/els45-demo-state.json" -Force -ErrorAction SilentlyContinue
Write-Output "Removed only the $ProjectName Compose project, its volumes, and this workspace's demo state file."
