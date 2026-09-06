[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidateSet(
        'gateway',
        'user-service',
        'content-service',
        'learning-service',
        'teaching-service',
        'game-service',
        'file-service',
        'notification-service',
        'audit-service'
    )]
    [string]$Service,

    [switch]$SkipBuild,

    [switch]$CheckOnly,

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$ApplicationArguments
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$envFile = Join-Path $repositoryRoot '.env'

if (-not (Test-Path -LiteralPath $envFile -PathType Leaf)) {
    throw "未找到本地环境文件：$envFile。请先根据 .env.example 创建 .env。"
}

$loadedKeys = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::OrdinalIgnoreCase)
foreach ($rawLine in Get-Content -LiteralPath $envFile) {
    $line = $rawLine.Trim()
    if ($line.Length -eq 0 -or $line.StartsWith('#')) {
        continue
    }
    if ($line -notmatch '^([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') {
        throw "无法解析 .env 中的配置行，请检查格式。"
    }

    $key = $Matches[1]
    $value = $Matches[2].Trim()
    if ($value.Length -ge 2) {
        $isDoubleQuoted = $value.StartsWith('"') -and $value.EndsWith('"')
        $isSingleQuoted = $value.StartsWith("'") -and $value.EndsWith("'")
        if ($isDoubleQuoted -or $isSingleQuoted) {
            $value = $value.Substring(1, $value.Length - 2)
        }
    }

    [Environment]::SetEnvironmentVariable($key, $value, 'Process')
    [void]$loadedKeys.Add($key)
}

$nacosKeys = @('NACOS_SERVER_ADDR', 'NACOS_USERNAME', 'NACOS_PASSWORD')
$databaseKeys = @('DB_HOST', 'POSTGRES_PORT', 'DB_USERNAME', 'DB_PASSWORD', 'DB_POOL_MAX_SIZE', 'DB_POOL_MIN_IDLE')
$redisKeys = @('REDIS_HOST', 'REDIS_PORT', 'REDIS_PASSWORD', 'REDIS_DATABASE')
$rabbitKeys = @('RABBITMQ_HOST', 'RABBITMQ_PORT', 'RABBITMQ_USERNAME', 'RABBITMQ_PASSWORD', 'RABBITMQ_VHOST')
$internalApiKeys = @('INTERNAL_API_ENABLED', 'INTERNAL_API_TOKEN')

$services = @{
    'gateway' = @{
        Module = 'gateway'
        Artifact = 'gateway'
        Required = $nacosKeys + $redisKeys
    }
    'user-service' = @{
        Module = 'user/user-service'
        Artifact = 'user-service'
        Required = $nacosKeys + $databaseKeys + $redisKeys + $rabbitKeys + $internalApiKeys
    }
    'content-service' = @{
        Module = 'content/content-service'
        Artifact = 'content-service'
        Required = $nacosKeys + $databaseKeys + $rabbitKeys + $internalApiKeys
    }
    'learning-service' = @{
        Module = 'learning/learning-service'
        Artifact = 'learning-service'
        Required = $nacosKeys + $databaseKeys + $rabbitKeys + $internalApiKeys
    }
    'teaching-service' = @{
        Module = 'teaching/teaching-service'
        Artifact = 'teaching-service'
        Required = $nacosKeys + $databaseKeys + $rabbitKeys + $internalApiKeys
    }
    'game-service' = @{
        Module = 'game/game-service'
        Artifact = 'game-service'
        Required = $nacosKeys + $databaseKeys + $rabbitKeys + $internalApiKeys
    }
    'file-service' = @{
        Module = 'file-service'
        Artifact = 'file-service'
        Required = $nacosKeys + $databaseKeys
    }
    'notification-service' = @{
        Module = 'notification-service'
        Artifact = 'notification-service'
        Required = $nacosKeys + $databaseKeys + $rabbitKeys
    }
    'audit-service' = @{
        Module = 'audit-service'
        Artifact = 'audit-service'
        Required = $nacosKeys + $databaseKeys + $rabbitKeys
    }
}

$serviceConfig = $services[$Service]
$missingKeys = @(
    $serviceConfig.Required |
        Sort-Object -Unique |
        Where-Object { [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_, 'Process')) }
)
if ($missingKeys.Count -gt 0) {
    throw "本地环境缺少必要配置：$($missingKeys -join ', ')"
}
if ($env:SPRING_PROFILES_ACTIVE -ne 'local') {
    throw "本地联调必须设置 SPRING_PROFILES_ACTIVE=local，当前值为 '$($env:SPRING_PROFILES_ACTIVE)'。"
}

Write-Host "已从 .env 加载 $($loadedKeys.Count) 个变量，并通过 $Service 的本地配置检查。"
if ($CheckOnly) {
    exit 0
}

$module = $serviceConfig.Module
if (-not $SkipBuild) {
    & (Join-Path $PSScriptRoot 'mvnw.cmd') -pl $module -am package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw "$Service 构建失败，退出码：$LASTEXITCODE"
    }
}

$targetDirectory = Join-Path (Join-Path $PSScriptRoot $module) 'target'
$jar = Get-ChildItem -LiteralPath $targetDirectory -Filter "$($serviceConfig.Artifact)-*.jar" -File |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if ($null -eq $jar) {
    throw "未找到 $Service 的可执行 JAR；请移除 -SkipBuild 后重试。"
}

Write-Host "正在以 local 环境启动 $Service。"
& java -jar $jar.FullName '--spring.profiles.active=local' @ApplicationArguments
exit $LASTEXITCODE
