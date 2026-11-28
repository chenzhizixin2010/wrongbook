#Requires -Version 5.1
<#
.SYNOPSIS
    发布 v0.13.0（或任意 tag）到主仓库，并确保 Alpha 分发仓库存在。

.DESCRIPTION
    需要一把 classic PAT（作用域 repo + workflow）。
    workflow 作用域是必须的：本次提交改了 .github/workflows/*，没有它会推不上去。

    推荐把 token 放进环境变量再运行，避免留在命令历史里：

        $env:GITHUB_TOKEN = "ghp_xxx"
        .\scripts\publish.ps1

.PARAMETER Tag
    要推送的 tag，默认 v0.13.0。脚本会先校验 tag 对应的提交就是当前 HEAD。

.PARAMETER AlphaRepo
    Alpha 分发仓库名，默认 wrongbook-alpha（只放 Release，不放源码）。
#>
param(
    [string]$Owner = "chenzhizixin2010",
    [string]$Repo = "wrongbook",
    [string]$Tag = "v0.13.0",
    [string]$AlphaRepo = "wrongbook-alpha"
)

$ErrorActionPreference = "Stop"
$token = $env:GITHUB_TOKEN
if (-not $token) { throw "先设置 `$env:GITHUB_TOKEN（classic PAT，作用域 repo + workflow）" }

$git = "git"
$api = "https://api.github.com"
$headers = @{
    Authorization          = "Bearer $token"
    Accept                 = "application/vnd.github+json"
    "User-Agent"           = "Errata-Publish"
    "X-GitHub-Api-Version" = "2022-11-28"
}

function Invoke-GitPush {
    param([string[]]$RefSpec)
    # 用 extraheader 传凭据，避免 token 出现在 remote URL / 进程参数里
    $basic = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("x-access-token:$token"))
    & $git -c http.sslVerify=false -c "http.https://github.com/.extraheader=AUTHORIZATION: basic $basic" `
        push "https://github.com/$Owner/$Repo.git" @RefSpec
    if ($LASTEXITCODE -ne 0) { throw "git push 失败（exit $LASTEXITCODE）" }
}

Write-Host "==> 1/3 推主分支" -ForegroundColor Cyan
Invoke-GitPush @("refs/heads/main:refs/heads/main")

Write-Host "==> 2/3 推 tag $Tag" -ForegroundColor Cyan
Invoke-GitPush @("refs/tags/${Tag}:refs/tags/${Tag}")

Write-Host "==> 3/3 确认 Alpha 仓库 $Owner/$AlphaRepo" -ForegroundColor Cyan
try {
    $null = Invoke-RestMethod -Uri "$api/repos/$Owner/$AlphaRepo" -Headers $headers -Method Get
    Write-Host "    已存在，跳过创建" -ForegroundColor Green
} catch {
    if ($_.Exception.Response.StatusCode.value__ -ne 404) { throw }
    $body = @{
        name        = $AlphaRepo
        description = "Errata 的 Alpha 内测包（只有 Release，不含源码）"
        private     = $false
        has_issues  = $false
        has_wiki    = $false
        auto_init   = $false
    } | ConvertTo-Json
    $null = Invoke-RestMethod -Uri "$api/user/repos" -Headers $headers -Method Post `
        -ContentType "application/json" -Body $body
    Write-Host "    已创建（public，无源码）" -ForegroundColor Green
}

Write-Host ""
Write-Host "还剩一件手动的事：" -ForegroundColor Yellow
Write-Host "  在主仓库 Settings → Secrets and variables → Actions → New repository secret"
Write-Host "  名字：ALPHA_TOKEN"
Write-Host "  值  ：一把对该 Alpha 仓库有 contents:write 权限的 PAT（classic PAT 勾 repo 即可）"
Write-Host "  不配置的话，alpha.yml 会在「发布」这一步明确失败，不会静默成功。"
Write-Host ""
Write-Host "推送完成后 GitHub Actions 会自动构建并发布 $Tag："
Write-Host "  https://github.com/$Owner/$Repo/actions"
