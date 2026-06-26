# =============================================================================
# TechMart Online — Deployment Script (Windows / PowerShell)
# =============================================================================

param(
    [string]$WildFlyHome = "C:\wildfly-34.0.0.Final",
    [string]$DbName = "techmart",
    [string]$DbUser = "techmart_app",
    [string]$DbPass = "techmart_pass"
)

Write-Host "=== TechMart Online Deployment ===" -ForegroundColor Cyan

# 1. Build the project
Write-Host "[1/5] Building project with Maven..." -ForegroundColor Yellow
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "Maven build failed!" -ForegroundColor Red
    exit 1
}

# 2. Stop WildFly if running
Write-Host "[2/5] Stopping WildFly..." -ForegroundColor Yellow
$wfProcess = Get-Process -Name "standalone" -ErrorAction SilentlyContinue
if ($wfProcess) {
    Stop-Process -Name "standalone" -Force
    Start-Sleep -Seconds 5
}

# 3. Deploy PostgreSQL JDBC driver module
Write-Host "[3/5] Deploying PostgreSQL driver..." -ForegroundColor Yellow
$modulesDir = "$WildFlyHome\modules\org\postgresql\main"
if (-not (Test-Path $modulesDir)) {
    New-Item -ItemType Directory -Path $modulesDir -Force | Out-Null
}
Copy-Item ".\target\dependency\postgresql-*.jar" "$modulesDir\postgresql.jar" -Force
@"
<?xml version="1.0" encoding="UTF-8"?>
<module xmlns="urn:jboss:module:1.9" name="org.postgresql">
    <resources>
        <resource-root path="postgresql.jar"/>
    </resources>
    <dependencies>
        <module name="javax.api"/>
        <module name="javax.transaction.api"/>
    </dependencies>
</module>
"@ | Out-File -FilePath "$modulesDir\module.xml" -Encoding utf8

# 4. Start WildFly and run CLI setup
Write-Host "[4/5] Starting WildFly and configuring resources..." -ForegroundColor Yellow
Start-Process -FilePath "$WildFlyHome\bin\standalone.bat" -WindowStyle Hidden
Start-Sleep -Seconds 15

# Wait for WildFly to be ready
$maxRetries = 30
$retry = 0
do {
    Start-Sleep -Seconds 2
    $retry++
    $wc = try { (Invoke-WebRequest -Uri "http://localhost:9990/health" -UseBasicParsing -ErrorAction SilentlyContinue).StatusCode } catch { 0 }
} while ($wc -ne 200 -and $retry -lt $maxRetries)

if ($retry -ge $maxRetries) {
    Write-Host "WildFly did not start in time. Proceeding anyway..." -ForegroundColor Yellow
}

# Run CLI setup
& "$WildFlyHome\bin\jboss-cli.bat" --connect --file=scripts\setup-wildfly.cli
if ($LASTEXITCODE -ne 0) {
    Write-Host "CLI setup had errors — continuing..." -ForegroundColor Yellow
}

# 5. Deploy the application
Write-Host "[5/5] Deploying techmart-online.war..." -ForegroundColor Yellow
Copy-Item ".\target\techmart-online.war" "$WildFlyHome\standalone\deployments\" -Force

Write-Host "=== Deployment Complete ===" -ForegroundColor Green
Write-Host "Application: http://localhost:8080/techmart-online"
Write-Host "API Base:    http://localhost:8080/techmart-online/api"
