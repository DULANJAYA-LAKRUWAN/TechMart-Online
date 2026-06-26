param(
    [Parameter(Mandatory=$false)]
    [string]$JmeterHome = "",

    [Parameter(Mandatory=$false)]
    [string]$Host = "localhost",

    [Parameter(Mandatory=$false)]
    [int]$Port = 8080,

    [Parameter(Mandatory=$false)]
    [string]$Protocol = "http",

    [Parameter(Mandatory=$false)]
    [string]$ResultsDir = "",

    [Parameter(Mandatory=$false)]
    [switch]$RunBenchmark = $true,

    [Parameter(Mandatory=$false)]
    [switch]$RunE2E = $true
)

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$JmeterDir = $JmeterHome
if (-not $JmeterDir) {
    $defaultPaths = @(
        "C:\tools\apache-jmeter*\bin",
        "C:\Program Files\apache-jmeter*\bin",
        "$env:USERPROFILE\apps\apache-jmeter*\bin"
    )
    foreach ($pattern in $defaultPaths) {
        $matches = Get-ChildItem -Path $pattern -ErrorAction SilentlyContinue
        if ($matches) {
            $JmeterDir = $matches[0].FullName
            break
        }
    }
}

$JmeterBin = Join-Path -Path $JmeterDir -ChildPath "jmeter.bat"
if (-not (Test-Path -LiteralPath $JmeterBin)) {
    Write-Error "JMeter not found. Specify -JmeterHome or install JMeter."
    exit 1
}

if (-not $ResultsDir) {
    $ResultsDir = Join-Path -Path $ProjectRoot -ChildPath "jmeter\results"
}
New-Item -ItemType Directory -Path $ResultsDir -Force | Out-Null

$Timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$JmeterDir = Resolve-Path $JmeterDir

$CommonArgs = @(
    "-n"
    "-Jhost=$Host"
    "-Jport=$Port"
    "-Jprotocol=$Protocol"
    "-JbasePath=/api"
    "-d", $ProjectRoot
)

Write-Host "=== TechMart Online — JMeter Performance Test Suite ===" -ForegroundColor Cyan
Write-Host "Target: $Protocol`://$Host`:$Port/api"
Write-Host "Results: $ResultsDir"
Write-Host ""

if ($RunBenchmark) {
    $benchmarkJmx = Join-Path -Path $ProjectRoot -ChildPath "jmeter\techmart-api-benchmark.jmx"
    $benchmarkLog = Join-Path -Path $ResultsDir -ChildPath "api-benchmark-$Timestamp.log"
    $benchmarkJtl  = Join-Path -Path $ResultsDir -ChildPath "api-benchmark-$Timestamp.jtl"
    $benchmarkReport = Join-Path -Path $ResultsDir -ChildPath "api-benchmark-report-$Timestamp"

    Write-Host "[1/2] Running API Benchmark..." -ForegroundColor Yellow
    $benchmarkArgs = $CommonArgs + @(
        "-t", "`"$benchmarkJmx`""
        "-l", "`"$benchmarkJtl`""
        "-j", "`"$benchmarkLog`""
        "-e", "-o", "`"$benchmarkReport`""
    )

    $process = Start-Process -FilePath $JmeterBin -ArgumentList $benchmarkArgs -NoNewWindow -Wait -PassThru
    if ($process.ExitCode -eq 0) {
        Write-Host "  Done. Report: $benchmarkReport\index.html" -ForegroundColor Green
    } else {
        Write-Host "  Failed (exit code: $($process.ExitCode)). Check log: $benchmarkLog" -ForegroundColor Red
    }
}

if ($RunE2E) {
    $e2eJmx = Join-Path -Path $ProjectRoot -ChildPath "jmeter\techmart-e2e-flow.jmx"
    $e2eLog = Join-Path -Path $ResultsDir -ChildPath "e2e-flow-$Timestamp.log"
    $e2eJtl  = Join-Path -Path $ResultsDir -ChildPath "e2e-flow-$Timestamp.jtl"
    $e2eReport = Join-Path -Path $ResultsDir -ChildPath "e2e-flow-report-$Timestamp"

    Write-Host "[2/2] Running E2E Shopping Flow..." -ForegroundColor Yellow
    $e2eArgs = $CommonArgs + @(
        "-t", "`"$e2eJmx`""
        "-l", "`"$e2eJtl`""
        "-j", "`"$e2eLog`""
        "-e", "-o", "`"$e2eReport`""
    )

    $process = Start-Process -FilePath $JmeterBin -ArgumentList $e2eArgs -NoNewWindow -Wait -PassThru
    if ($process.ExitCode -eq 0) {
        Write-Host "  Done. Report: $e2eReport\index.html" -ForegroundColor Green
    } else {
        Write-Host "  Failed (exit code: $($process.ExitCode)). Check log: $e2eLog" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "=== All done ===" -ForegroundColor Cyan
