[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$ProjectName,
    [string]$EnvFile = '.env',
    [string]$StatePath,
    [switch]$ConfirmReset
)

. "$PSScriptRoot/common.ps1"
Assert-ElsDemoProject $ProjectName
if (-not $ConfirmReset) { throw 'Pass -ConfirmReset only for your disposable demo project.' }
$stateFile = Get-ElsStatePath $ProjectName $StatePath
$compose = Get-ElsComposeConfig $ProjectName $EnvFile
foreach ($volume in $compose.Config.volumes.PSObject.Properties) {
    if ($volume.Value.external -or $volume.Value.name -ne "$($ProjectName)_$($volume.Name)") { throw 'Reset refuses external or non-project-owned volumes.' }
}
$containers = @(Invoke-ElsDocker -Arguments ($compose.Arguments + @('ps', '-a', '-q')))
foreach ($id in $containers) {
    $container = @((Invoke-ElsDocker -Arguments @('inspect', $id)) | ConvertFrom-Json)[0]
    Assert-ElsContainerOwner $container $ProjectName $container.Config.Labels.'com.docker.compose.service'
}
if (Test-Path -LiteralPath $stateFile) {
    $state = Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json
    if ($state.project -ne $ProjectName -or $state.checkout -ne $script:ElsRepoRoot) { throw 'State belongs to another project or checkout.' }
}
Invoke-ElsDocker -Arguments ($compose.Arguments + @('down', '-v')) | Out-Null
if (Test-Path -LiteralPath $stateFile) { Remove-Item -LiteralPath $stateFile -Force }
Write-Output "Reset completed for $ProjectName; its checkpoint was removed only after Docker succeeded."
