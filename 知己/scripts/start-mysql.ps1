<#
  启动「知己」开发用的 MySQL 实例。

  为什么不直接用 Windows 服务 MySQL82？
  本机 MySQL82 的数据目录在 C:\ProgramData\MySQL\MySQL Server 8.2\Data，
  启动服务需要管理员权限；这个脚本改用用户目录下的独立数据目录，
  普通权限即可启动，且不影响原有数据和配置。

  用法：
      powershell -ExecutionPolicy Bypass -File scripts\start-mysql.ps1

  换一台电脑时，用 -MySqlHome 指定该机 MySQL 的安装目录，例如：
      powershell -ExecutionPolicy Bypass -File scripts\start-mysql.ps1 -MySqlHome "C:\Program Files\MySQL\MySQL Server 8.0"
#>
param(
    [string]$MySqlHome    = 'D:\shujuku',
    [string]$DataDir      = "$env:USERPROFILE\.mochou\mysql-data",
    [int]$Port            = 3306,
    [string]$RootPassword = '123456',
    [string]$Database     = 'mochou'
)

$ErrorActionPreference = 'Stop'

$mysqld = Join-Path $MySqlHome 'bin\mysqld.exe'
$mysql  = Join-Path $MySqlHome 'bin\mysql.exe'

if (-not (Test-Path $mysqld)) {
    throw "找不到 mysqld.exe：$mysqld（用 -MySqlHome 指定 MySQL 安装目录）"
}

function Test-DbPort {
    param([int]$P)
    return [bool](Get-NetTCPConnection -LocalPort $P -State Listen -ErrorAction SilentlyContinue)
}

# Windows PowerShell 5.1 在 $ErrorActionPreference='Stop' 时，会把原生命令写到 stderr 的内容
# （例如 mysql 的 “Using a password ...” 警告、ERROR 1045）当成终止性错误。
# 这里统一包一层：临时放宽为 Continue，并丢弃 stderr，只取退出码。
function Invoke-Native {
    param(
        [Parameter(Mandatory)][string]$FilePath,
        [string[]]$Arguments = @()
    )
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        & $FilePath @Arguments 2>$null | Out-Null
        return $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previous
    }
}

# 1. 首次运行：初始化数据目录（root 空密码）
if (-not (Test-Path $DataDir)) {
    Write-Host "[1/4] 初始化数据目录 $DataDir"
    New-Item -ItemType Directory -Path $DataDir -Force | Out-Null
    $initCode = Invoke-Native -FilePath $mysqld -Arguments @(
        '--initialize-insecure', "--basedir=$MySqlHome", "--datadir=$DataDir", '--console'
    )
    if ($initCode -ne 0 -and -not (Test-Path (Join-Path $DataDir 'mysql'))) {
        throw "初始化数据目录失败（退出码 $initCode）"
    }
} else {
    Write-Host '[1/4] 数据目录已存在，跳过初始化'
}

# 2. 启动实例
if (Test-DbPort -P $Port) {
    Write-Host "[2/4] MySQL 已在 $Port 端口运行"
} else {
    Write-Host "[2/4] 启动 mysqld（端口 $Port）"
    Start-Process -FilePath $mysqld `
        -ArgumentList "--basedir=$MySqlHome", "--datadir=$DataDir", "--port=$Port", "--console" `
        -WindowStyle Hidden | Out-Null

    $deadline = (Get-Date).AddSeconds(60)
    while (-not (Test-DbPort -P $Port)) {
        if ((Get-Date) -gt $deadline) { throw "等待 MySQL 启动超时（端口 $Port）" }
        Start-Sleep -Milliseconds 500
    }
}

# 3. root 密码：空密码时设置为 $RootPassword
# 注意：所有客户端调用都必须显式带 -h 127.0.0.1 -P $Port，否则会连到默认的 3306 实例上
$connArgs = @('-h', '127.0.0.1', '-P', "$Port", '-u', 'root')

if ((Invoke-Native -FilePath $mysql -Arguments ($connArgs + @('-e', 'SELECT 1'))) -eq 0) {
    Write-Host '[3/4] root 当前为空密码，设置为配置的密码'
    $alterCode = Invoke-Native -FilePath $mysql -Arguments ($connArgs + @(
        '-e', "ALTER USER 'root'@'localhost' IDENTIFIED BY '$RootPassword';"
    ))
    if ($alterCode -ne 0) { throw '设置 root 密码失败' }
} else {
    Write-Host '[3/4] root 密码已存在，跳过'
}

# 4. 建库（表结构由后端启动时的 schema.sql 创建）
Write-Host "[4/4] 确认数据库 $Database 存在"
$createSql = "CREATE DATABASE IF NOT EXISTS ``$Database`` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"
if ((Invoke-Native -FilePath $mysql -Arguments ($connArgs + @("-p$RootPassword", '-e', $createSql))) -ne 0) {
    throw "无法用 root/$RootPassword 连接 127.0.0.1:$Port 上的 MySQL。请确认密码与 backend/src/main/resources/application.yml 里的 spring.datasource.password 一致"
}

Write-Host "完成：MySQL 已就绪（127.0.0.1:$Port，用户 root，库 $Database）"
