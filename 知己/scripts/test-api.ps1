<#
  接口自测脚本：验证注册 / 修改密码 / 登录的全部业务规则。
  用法：powershell -ExecutionPolicy Bypass -File scripts\test-api.ps1

  注意：脚本存为 UTF-8 with BOM，Windows PowerShell 5.1 才能正确读取中文。
#>
param(
    [string]$BaseUrl = 'http://127.0.0.1:8080'
)

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$script:passed = 0
$script:failed = 0

# 后端返回的 Content-Type 不带 charset，Windows PowerShell 5.1 会按 Latin-1 解码，
# 这里手动按 UTF-8 解析响应体，保证中文消息比对准确。
function ConvertFrom-Utf8Json {
    param($Response)
    $text = [System.Text.Encoding]::UTF8.GetString($Response.RawContentStream.ToArray())
    return $text | ConvertFrom-Json
}

function Invoke-Api {
    param([string]$Path, [hashtable]$Body)
    $json = $Body | ConvertTo-Json -Compress
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    $resp = Invoke-WebRequest -Uri "$BaseUrl$Path" -Method Post `
        -ContentType 'application/json; charset=utf-8' -Body $bytes -UseBasicParsing
    return ConvertFrom-Utf8Json $resp
}

function Get-Api {
    param([string]$Path)
    $resp = Invoke-WebRequest -Uri "$BaseUrl$Path" -Method Get -UseBasicParsing
    return ConvertFrom-Utf8Json $resp
}

function Test-Case {
    param([string]$Name, $Response, [int]$ExpectCode, [string]$ExpectMsg)
    $ok = ($Response.code -eq $ExpectCode)
    if ($ok -and $ExpectMsg) { $ok = ($Response.msg -eq $ExpectMsg) }
    if ($ok) {
        $script:passed++
        Write-Host ("  [PASS] {0,-24} -> {1} {2}" -f $Name, $Response.code, $Response.msg)
    } else {
        $script:failed++
        Write-Host ("  [FAIL] {0,-24} -> 实际 {1} {2}，期望 {3} {4}" -f $Name, $Response.code, $Response.msg, $ExpectCode, $ExpectMsg)
    }
}

$suffix = (Get-Random -Minimum 1000 -Maximum 9999)
$user = "u$suffix"                    # 短用户名
$userMax = "m$suffix" + "abcde"       # 1+4+5 = 正好 10 个字符
$userTooLong = "t$suffix" + "abcdef"  # 1+4+6 = 11 个字符

Write-Host "`n== 健康检查 =="
$health = Get-Api '/api/health'
Write-Host ("  /api/health -> code={0}, data={1}" -f $health.code, $health.data)

Write-Host "`n== 注册 =="
Test-Case '用户名不能为空'     (Invoke-Api '/api/register' @{ username = ''; password = '123' })          400 '用户名不能为空'
Test-Case '用户名全空格'       (Invoke-Api '/api/register' @{ username = '   '; password = '123' })       400 '用户名不能为空'
Test-Case '密码不能为空'       (Invoke-Api '/api/register' @{ username = $user; password = '' })          400 '密码不能为空'
Test-Case '用户名超过10个字符' (Invoke-Api '/api/register' @{ username = $userTooLong; password = '1' })  400 '用户名不能超过10个字符'
Test-Case '正常注册'           (Invoke-Api '/api/register' @{ username = $user; password = '1' })         200 '注册成功'
Test-Case '用户名不可重复'     (Invoke-Api '/api/register' @{ username = $user; password = '2' })         400 '用户名已存在'
Test-Case '10个字符可注册'     (Invoke-Api '/api/register' @{ username = $userMax; password = 'p' })      200 '注册成功'
Test-Case '首尾空格被忽略'     (Invoke-Api '/api/register' @{ username = "  $user  "; password = '3' })   400 '用户名已存在'
Test-Case '密码简易无要求'     (Invoke-Api '/api/register' @{ username = "s$suffix"; password = 'a' })    200 '注册成功'
Test-Case '中文用户名可注册'   (Invoke-Api '/api/register' @{ username = "知己$suffix"; password = '中文密码' }) 200 '注册成功'
Test-Case '中文用户名超10字符' (Invoke-Api '/api/register' @{ username = '一二三四五六七八九十十一'; password = 'x' }) 400 '用户名不能超过10个字符'

Write-Host "`n== 登录 =="
Test-Case '用户不存在'   (Invoke-Api '/api/login' @{ username = "nobody$suffix"; password = 'x' }) 400 '用户不存在'
Test-Case '密码错误'     (Invoke-Api '/api/login' @{ username = $user; password = 'wrong' })       400 '密码错误'
Test-Case '登录成功'     (Invoke-Api '/api/login' @{ username = $user; password = '1' })           200 '登录成功'

Write-Host "`n== 修改密码 =="
Test-Case '用户不存在'       (Invoke-Api '/api/resetPwd' @{ username = "nobody$suffix"; oldPassword = '1'; newPassword = '2' }) 400 '用户不存在'
Test-Case '原密码错误'       (Invoke-Api '/api/resetPwd' @{ username = $user; oldPassword = 'bad'; newPassword = '2' })         400 '原密码错误'
Test-Case '新密码不能为空'   (Invoke-Api '/api/resetPwd' @{ username = $user; oldPassword = '1'; newPassword = '' })            400 '请输入新密码'
Test-Case '修改成功'         (Invoke-Api '/api/resetPwd' @{ username = $user; oldPassword = '1'; newPassword = 'newpass' })     200 '密码修改成功'
Test-Case '旧密码已失效'     (Invoke-Api '/api/login' @{ username = $user; password = '1' })                                    400 '密码错误'
Test-Case '新密码可登录'     (Invoke-Api '/api/login' @{ username = $user; password = 'newpass' })                              200 '登录成功'

Write-Host "`n== 照片墙（图片存入 chou 库）=="

# 上传走 multipart，Windows PowerShell 5.1 没有 -Form，用系统自带的 curl.exe
$curl = (Get-Command curl.exe -ErrorAction SilentlyContinue).Source
$image = Join-Path (Split-Path -Parent $PSScriptRoot) 'yang.jpg'

if (-not $curl) {
    Write-Host '  [SKIP] 找不到 curl.exe，跳过上传相关用例'
} elseif (-not (Test-Path $image)) {
    Write-Host "  [SKIP] 找不到测试图片 $image"
} else {
    $uploaded = (curl.exe -s -X POST "$BaseUrl/api/photos" -F "file=@$image" -F "username=$user") | ConvertFrom-Json
    Test-Case '上传图片' $uploaded 200 '上传成功'

    $photoId = $uploaded.data.id
    Test-Case '只能上传图片' (curl.exe -s -X POST "$BaseUrl/api/photos" -F "file=@$PSScriptRoot\test-api.ps1" -F "username=$user" | ConvertFrom-Json) 400 '只能上传图片文件'
    Test-Case '上传者必须已注册' (curl.exe -s -X POST "$BaseUrl/api/photos" -F "file=@$image" -F "username=nobody$suffix" | ConvertFrom-Json) 400 '用户不存在'

    $list = curl.exe -s "$BaseUrl/api/photos?username=$user" | ConvertFrom-Json
    Test-Case '图片列表' $list 200 '操作成功'
    # 注意：PowerShell 5.1 里筛选出的单个对象没有 .Count，必须用 @() 包成数组
    $inList = @($list.data | Where-Object { $_.id -eq $photoId }).Count -ge 1
    if ($inList) { $script:passed++; Write-Host "  [PASS] 列表包含刚上传的图片                  -> id=$photoId" }
    else { $script:failed++; Write-Host "  [FAIL] 列表包含刚上传的图片                  -> 未找到 id=$photoId" }

    $rawCode = curl.exe -s -o "$env:TEMP\zhiji_photo_check.bin" -w "%{http_code}" "$BaseUrl/api/photos/$photoId/raw"
    $rawSize = (Get-Item "$env:TEMP\zhiji_photo_check.bin" -ErrorAction SilentlyContinue).Length
    if ($rawCode -eq '200' -and $rawSize -gt 0) { $script:passed++; Write-Host "  [PASS] 取图片原图                            -> HTTP 200，$rawSize 字节" }
    else { $script:failed++; Write-Host "  [FAIL] 取图片原图                            -> HTTP $rawCode，$rawSize 字节" }
    Remove-Item "$env:TEMP\zhiji_photo_check.bin" -ErrorAction SilentlyContinue

    $missingCode = curl.exe -s -o NUL -w "%{http_code}" "$BaseUrl/api/photos/99999999/raw"
    if ($missingCode -eq '404') { $script:passed++; Write-Host '  [PASS] 不存在的图片返回 404                  -> HTTP 404' }
    else { $script:failed++; Write-Host "  [FAIL] 不存在的图片返回 404                  -> HTTP $missingCode" }
}

Write-Host "`n== 反馈 / 投诉作者 =="
Test-Case '内容不能为空' (Invoke-Api '/api/feedback' @{ content = ''; username = $user })      400 '请填写反馈内容'
Test-Case '提交反馈'     (Invoke-Api '/api/feedback' @{ content = '来自自测脚本'; username = $user }) 200 '感谢你的反馈'

Write-Host "`n== 结果：$script:passed 项通过，$script:failed 项失败 =="
if ($script:failed -gt 0) { exit 1 }
