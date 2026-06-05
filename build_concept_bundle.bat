@echo off
setlocal

set "ROOT=%~dp0"
set "OUTPUT=%ROOT%chatly_code_full_concept.txt"

if exist "%OUTPUT%" del "%OUTPUT%"

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference = 'Stop';" ^
  "$root = (Resolve-Path '%ROOT%').Path;" ^
  "$output = Join-Path $root 'chatly_code_full_concept.txt';" ^
  "$folders = @('rules', 'concept');" ^
  "$files = foreach ($folder in $folders) { Get-ChildItem -LiteralPath (Join-Path $root $folder) -File | Sort-Object Name };" ^
  "$utf8 = New-Object System.Text.UTF8Encoding($false);" ^
  "$writer = New-Object System.IO.StreamWriter($output, $false, $utf8);" ^
  "try {" ^
  "  foreach ($file in $files) {" ^
  "    $relative = $file.FullName.Substring($root.TrimEnd('\').Length + 1);" ^
  "    $writer.WriteLine('================================================================================');" ^
  "    $writer.WriteLine('FILE: ' + $relative);" ^
  "    $writer.WriteLine('PATH: ' + $file.FullName);" ^
  "    $writer.WriteLine('================================================================================');" ^
  "    $writer.WriteLine();" ^
  "    $content = [System.IO.File]::ReadAllText($file.FullName, [System.Text.Encoding]::UTF8);" ^
  "    $writer.Write($content);" ^
  "    if (-not $content.EndsWith([Environment]::NewLine)) { $writer.WriteLine(); }" ^
  "    $writer.WriteLine();" ^
  "    $writer.WriteLine();" ^
  "  }" ^
  "} finally { $writer.Dispose(); }" ^
  "Write-Host ('Created: ' + $output)"

if errorlevel 1 (
  echo Failed to build concept bundle.
  exit /b 1
)

echo Done.
echo Output: %OUTPUT%
