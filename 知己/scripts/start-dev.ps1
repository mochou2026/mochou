<#
  一键启动开发环境：MySQL -> 后端(8080) -> 前端(5173)

  用法：powershell -ExecutionPolicy Bypass -File scripts\start-dev.ps1
#>
$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $root 'backend\target\zhiji-backend-1.0.0.jar'

Write-Host '=== 1/3 启动 MySQL ==='
& (Join-Path $PSScriptRoot 'start-mysql.ps1')

if (-not (Test-Path $jar)) {
    Write-Host '=== 2/3 首次运行，先构建后端 ==='
    Push-Location (Join-Path $root 'backend')
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try { & mvn -B -ntp clean package -DskipTests } finally { $ErrorActionPreference = $previous; Pop-Location }
    if ($LASTEXITCODE -ne 0) { throw '后端构建失败，请检查上面的 Maven 输出' }
}

Write-Host '=== 2/3 启动后端（新窗口）==='
Start-Process powershell -ArgumentList '-NoExit', '-Command', "`$host.UI.RawUI.WindowTitle='知己-后端'; Set-Location '$root\backend'; java -jar '$jar'"

Start-Sleep -Seconds 8

Write-Host '=== 3/3 启动前端（新窗口）==='
Start-Process powershell -ArgumentList '-NoExit', '-Command', "`$host.UI.RawUI.WindowTitle='知己-前端'; Set-Location '$root\frontend'; npm run dev"

Write-Host ''
Write-Host '前端页面： http://127.0.0.1:5173'
Write-Host '后端接口： http://127.0.0.1:8080/api/health'
