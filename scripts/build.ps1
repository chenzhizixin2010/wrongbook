param(
    [string]$Tasks = "assembleDebug"
)

# 本机（中文路径）构建入口：等价于 gradle <tasks>，但会自动补齐 JAVA_HOME。
# 用法：
#   .\scripts\build.ps1                 # 编译 debug APK
#   .\scripts\build.ps1 testDebugUnitTest
#   .\scripts\build.ps1 "assembleDebug testDebugUnitTest"

$ErrorActionPreference = 'Stop'

if (-not $env:JAVA_HOME) {
    $candidate = 'D:\dev-tools\jdk\jdk-17.0.2'
    if (Test-Path (Join-Path $candidate 'bin\java.exe')) {
        $env:JAVA_HOME = $candidate
    } else {
        throw 'JAVA_HOME 未设置，且默认路径 D:\dev-tools\jdk\jdk-17.0.2 不存在。'
    }
}

$gradleHome = 'D:\dev-tools\gradle-9.9.2\gradle-9.9.2\bin'
$gradleExe = $null
if (Test-Path (Join-Path $gradleHome 'gradle.bat')) {
    $gradleExe = Join-Path $gradleHome 'gradle.bat'
} else {
    $cmd = Get-Command gradle -ErrorAction SilentlyContinue
    if ($cmd) { $gradleExe = $cmd.Source }
}

if (-not $gradleExe) { throw '找不到 gradle：既没有本机安装，也不在 PATH 中。' }

$projectRoot = Split-Path $PSScriptRoot -Parent
$argsList = @('--no-daemon', '--console=plain')
# 本机依赖镜像（腾讯云），存在才加，避免影响其他机器
$mirror = 'D:\dev-tools\maven-mirrors.gradle'
if (Test-Path $mirror) { $argsList += @('--init-script', $mirror) }
foreach ($t in $Tasks.Split(' ', [System.StringSplitOptions]::RemoveEmptyEntries)) {
    # 允许传 "assembleDebug" 或 ":app:assembleDebug" 两种写法
    if ($t.StartsWith(':')) { $argsList += $t } else { $argsList += ":app:$t" }
}

Push-Location $projectRoot
try {
    Write-Host "JAVA_HOME=$env:JAVA_HOME"
    Write-Host "gradle=$gradleExe"
    Write-Host "tasks=$($argsList -join ' ')"
    & $gradleExe @argsList
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
