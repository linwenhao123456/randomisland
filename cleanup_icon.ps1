foreach ($f in @('check_png.ps1','verify_icon.ps1')) {
  $p = Join-Path 'e:\PY Project\随机空岛' $f
  if (Test-Path $p) { Remove-Item $p -Force; Write-Host ('removed ' + $f) }
}
