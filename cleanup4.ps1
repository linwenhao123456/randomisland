$tmp = @(
  'api_probe_dim.ps1','probe_dim2.ps1','probe_dim3.ps1','find_jars.ps1',
  'read_diag.ps1','read_presets.ps1','inspect_jar.ps1'
)
foreach ($f in $tmp) {
  $p = Join-Path 'e:\PY Project\随机空岛' $f
  if (Test-Path $p) { Remove-Item $p -Force; Write-Host ('removed ' + $f) }
}
