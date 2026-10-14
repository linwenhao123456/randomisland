# 构建 random_island，并把 jar 复制到游戏实例的 mods 目录。
# 用法：powershell -NoProfile -ExecutionPolicy Bypass -File "e:/PY Project/随机空岛/rebuild_and_install.ps1"

$ErrorActionPreference = 'Stop'

$root       = 'e:\PY Project\随机空岛'
$gradleLib  = 'E:\gradle-home\wrapper\dists\gradle-8.8-bin\82ab1joe9hopwwifh6l2d8ovj\gradle-8.8\lib/*'
$java       = 'C:\jdk17\bin\java.exe'

# 目标实例（路径含空格，需引号）
$modsDir    = 'E:\360Downloads\.minecraft\versions\1.20.1-Fabric 0.19.3\mods'
$targetJar  = Join-Path $modsDir 'random_island-1.0.0.jar'
$sourceJar  = Join-Path $root 'build\libs\random_island-1.0.0.jar'

Write-Host '=== 1/2 构建 ==='
Push-Location $root
& $java -Xmx4G -XX:MaxMetaspaceSize=1G '-Duser.home=E:\gradle-home' -cp $gradleLib org.gradle.launcher.GradleMain build --console=plain
$buildCode = $LASTEXITCODE
Pop-Location
if ($buildCode -ne 0) {
    Write-Host "构建失败（EXITCODE=$buildCode），未复制 jar。" -ForegroundColor Red
    exit $buildCode
}

Write-Host '=== 2/2 安装到实例 ==='
if (-not (Test-Path $sourceJar)) {
    Write-Host "未找到产物：$sourceJar" -ForegroundColor Red
    exit 1
}
New-Item -ItemType Directory -Force -Path $modsDir | Out-Null
Copy-Item -Force -Path $sourceJar -Destination $targetJar
Write-Host "已复制 -> $targetJar"
Write-Host "  ($([math]::Round((Get-Item $targetJar).Length / 1KB, 1)) KB)"
Write-Host '完成。' -ForegroundColor Green
