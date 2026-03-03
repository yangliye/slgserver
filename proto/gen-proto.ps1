<#
.SYNOPSIS
    Generate Java source files from .proto definitions.

.DESCRIPTION
    Runs protoc via Gradle protobuf plugin, then copies generated Java files
    to src/main/java/ for IDE indexing and git tracking.

.EXAMPLE
    .\gen-proto.ps1
#>

$ErrorActionPreference = "Stop"

$rootDir = Split-Path $PSScriptRoot -Parent

Push-Location $rootDir
try {
    Write-Host "Generating proto Java sources..." -ForegroundColor Cyan
    & .\gradlew :proto:genProto
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Proto generation failed!" -ForegroundColor Red
        exit 1
    }
    Write-Host "Done. Generated files are in proto/src/main/java/" -ForegroundColor Green
} finally {
    Pop-Location
}
