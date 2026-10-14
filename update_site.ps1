<#
  Build random_island, sync the latest jar + version.json into site/,
  then optionally deploy to Cloudflare Pages.
  Usage:
    .\update_site.ps1              # build and update jar + version.json into site/
    .\update_site.ps1 -Git         # update site/ then git add/commit/push
    .\update_site.ps1 -Cloudflare  # update site/ then deploy via wrangler (needs CLOUDFLARE_API_TOKEN / CLOUDFLARE_ACCOUNT_ID, or use `wrangler login`)
#>
param([switch]$Git, [switch]$Cloudflare)

$ErrorActionPreference = 'Stop'

$root      = Split-Path $MyInvocation.MyCommand.Path -Parent
$gradleLib = 'E:\gradle-home\wrapper\dists\gradle-8.8-bin\82ab1joe9hopwwifh6l2d8ovj\gradle-8.8\lib/*'
$java      = 'C:\jdk17\bin\java.exe'
$siteDir   = Join-Path $root 'site'
$dlDir     = Join-Path $siteDir 'download'

# ---------- 1) build ----------
Write-Host '=== 1/3 build mod ==='
Push-Location $root
& $java -Xmx4G -XX:MaxMetaspaceSize=1G '-Duser.home=E:\gradle-home' -cp $gradleLib org.gradle.launcher.GradleMain build --console=plain
if ($LASTEXITCODE -ne 0) { Write-Host 'build failed, aborted.' -ForegroundColor Red; exit $LASTEXITCODE }
Pop-Location

# ---------- 2) pick the produced jar ----------
$jars = Get-ChildItem -Path (Join-Path $root 'build\libs') -Filter 'random_island-*.jar' |
        Where-Object { $_.Name -notmatch '-(sources|dev)\.jar$' }
if ($jars.Count -eq 0) { Write-Host 'no built jar found.' -ForegroundColor Red; exit 1 }
$sourceJar = ($jars | Sort-Object LastWriteTime -Descending | Select-Object -First 1).FullName
$jarName   = Split-Path $sourceJar -Leaf

# ---------- 3) read version info ----------
function Get-Prop($key) {
  $line = Get-Content (Join-Path $root 'gradle.properties') | Where-Object { $_ -match "^$key=" }
  if ($line) { ($line -split '=', 2)[1].Trim() } else { '' }
}
$modVersion = Get-Prop 'mod_version'
$mcVersion  = Get-Prop 'minecraft_version'
if (-not $modVersion) { $modVersion = '1.0.0' }
if (-not $mcVersion)  { $mcVersion  = '1.20.1' }

$sizeKB  = [math]::Round((Get-Item $sourceJar).Length / 1KB, 1)
$updated = (Get-Date).ToString('yyyy-MM-dd')

# ---------- 4) sync into site/ ----------
New-Item -ItemType Directory -Force -Path $dlDir | Out-Null
Copy-Item -Force -Path $sourceJar -Destination (Join-Path $dlDir $jarName)

$versionJson = [ordered]@{
  name      = 'Random Island'
  version   = $modVersion
  fileName  = $jarName
  size      = "$sizeKB KB"
  updated   = $updated
  mcVersion = $mcVersion
  loader    = 'Fabric'
  changelog = 'NEW: "Mining" enchantment (挖掘) - only on pickaxes, breaks ANY block in a fixed 5s ignoring hardness (even bedrock), dropped in most reward/starter chests (miningBookChance). Also: "uniform" toggle (islandUniform); all-blocks mode (useAllBlocks) and all-items mode (useAllItems) include other mods; configurable starter chest; better default loot. Commands: interval / transform / weight / allblocks / allitems / uniform / reset / help.'
} | ConvertTo-Json -Compress

Set-Content -Path (Join-Path $siteDir 'version.json') -Value $versionJson -Encoding UTF8
Write-Host "updated site/download/$jarName and site/version.json (v$modVersion, $sizeKB KB)"

# ---------- 5) deploy ----------
if ($Cloudflare) {
  Write-Host '=== deploy to Cloudflare Pages (wrangler) ==='
  $cfEnv = Join-Path $root '.cf.env'
  if (Test-Path $cfEnv) {
    Get-Content $cfEnv | ForEach-Object {
      if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$') {
        Set-Item -Path "env:$($matches[1])" -Value $matches[2]
      }
    }
    Write-Host 'loaded credentials from .cf.env'
  }
  if (-not $env:CLOUDFLARE_API_TOKEN -or -not $env:CLOUDFLARE_ACCOUNT_ID) {
    Write-Host 'CLOUDFLARE_API_TOKEN / CLOUDFLARE_ACCOUNT_ID not set; will try `wrangler login` default identity if logged in.' -ForegroundColor Yellow
  }
  Push-Location $root
  $prevEA = $ErrorActionPreference
  $ErrorActionPreference = 'Continue'
  # try to create the project; if it already exists (409) or has a non-fatal issue, keep going
  wrangler pages project create random-island-site --production-branch main 2>&1 |
    ForEach-Object { Write-Host "[create] $_" }
  wrangler pages deploy $siteDir --project-name random-island-site
  $code = $LASTEXITCODE
  $ErrorActionPreference = $prevEA
  Pop-Location
  if ($code -eq 0) { Write-Host 'deploy finished.' -ForegroundColor Green }
  else { Write-Host 'deploy failed, see wrangler output above (likely an API permission issue -> use an API Token with Pages:Edit, or `wrangler login` may lack Pages scope).' -ForegroundColor Red; exit $code }
} elseif ($Git) {
  Write-Host '=== git commit and push site/ ==='
  Push-Location $root
  git add site
  git commit -m "site: update mod v$modVersion"
  git push
  Pop-Location
  Write-Host 'pushed. Cloudflare Pages (Git integration) will redeploy automatically.' -ForegroundColor Green
} else {
  Write-Host ''
  Write-Host 'no deploy method specified. Pick one next time:' -ForegroundColor Yellow
  Write-Host '  .\update_site.ps1 -Git'
  Write-Host '  .\update_site.ps1 -Cloudflare'
}
