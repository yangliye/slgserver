# ============================================================
# Gradle 项目打包脚本
# 用法: .\build.ps1 [选项]
# ============================================================

param(
    [switch]$Clean,           # 清理后构建
    [switch]$Test,            # 运行测试（默认跳过）
    [switch]$Publish,         # 发布到本地 Maven 仓库
    [string]$Module = ""      # 指定模块 (db/rpc)
)

$ErrorActionPreference = "Stop"
$projectRoot = $PSScriptRoot

Set-Location $projectRoot

# 兼容 .\build.ps1 clean 写法
if ($Module -eq "clean") {
    $Clean = $true
    $Module = ""
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  SLG Server - Gradle Build Script" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# 显示当前配置
Write-Host "[Configuration]" -ForegroundColor Yellow
Write-Host "  Project Root: $projectRoot"
Write-Host "  Clean Build:  $Clean"
Write-Host "  Run Tests:    $Test"
Write-Host "  Publish:      $Publish"
Write-Host "  Module:       $(if ($Module) { $Module } else { 'All' })"
Write-Host ""

# 构建命令参数
$gradleArgs = @()

# 清理
if ($Clean) {
    $gradleArgs += "clean"
}

# 确定构建目标
if ($Module) {
    # 指定模块
    $gradleArgs += ":${Module}:build"
    if ($Publish) {
        $gradleArgs += ":${Module}:publishToMavenLocal"
    }
} else {
    # 所有模块
    $gradleArgs += "build"
    if ($Publish) {
        $gradleArgs += "publishToMavenLocal"
    }
}

# 默认跳过测试，除非指定 -Test
if (-not $Test) {
    $gradleArgs += "-x"
    $gradleArgs += "test"
}

# 执行构建
Write-Host "[Building]" -ForegroundColor Yellow
Write-Host "  Command: .\gradlew $($gradleArgs -join ' ')"
Write-Host ""

$startTime = Get-Date
& .\gradlew @gradleArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "Build FAILED!" -ForegroundColor Red
    exit 1
}

$elapsed = (Get-Date) - $startTime

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host "  Build SUCCESS!" -ForegroundColor Green
Write-Host "  Time: $([math]::Round($elapsed.TotalSeconds, 2)) seconds" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
Write-Host ""

# 显示输出文件
Write-Host "[Output Files]" -ForegroundColor Yellow

if (-not $Module -or $Module -eq "db") {
    $dbJar = Get-ChildItem "$projectRoot\db\build\libs\*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -notmatch "sources|javadoc" }
    if ($dbJar) {
        Write-Host "  [db]  $($dbJar.Name) ($([math]::Round($dbJar.Length/1KB, 1)) KB)" -ForegroundColor White
        Write-Host "        $($dbJar.FullName)" -ForegroundColor DarkGray
    }
}

if (-not $Module -or $Module -eq "rpc") {
    $rpcJar = Get-ChildItem "$projectRoot\rpc\build\libs\*.jar" -ErrorAction SilentlyContinue | Where-Object { $_.Name -notmatch "sources|javadoc" }
    if ($rpcJar) {
        Write-Host "  [rpc] $($rpcJar.Name) ($([math]::Round($rpcJar.Length/1KB, 1)) KB)" -ForegroundColor White
        Write-Host "        $($rpcJar.FullName)" -ForegroundColor DarkGray
    }
}

if ($Publish) {
    Write-Host ""
    Write-Host "[Published to Local Maven Repository]" -ForegroundColor Yellow
    Write-Host "  Location: $env:USERPROFILE\.m2\repository\com\muyi\" -ForegroundColor DarkGray
}

Write-Host ""
