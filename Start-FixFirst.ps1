param([switch]$Demo)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
function Read-PrivateValue([string]$Prompt) {
    $value = Read-Host $Prompt -AsSecureString
    return [System.Net.NetworkCredential]::new('', $value).Password
}
try {
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Java was not found. Install JDK 17 and reopen VS Code.' }
    if (-not $Demo) {
        if (-not $env:DB_USER) { $env:DB_USER = 'fixfirst_app' }
        $env:DB_PASSWORD = Read-PrivateValue 'MySQL fixfirst_app password'
    }
    $env:ADMIN_PASSWORD = Read-PrivateValue 'FixFirst admin password (12+ characters; use same password on later starts)'
    Write-Host 'Admin email: ' -NoNewline
    if ($env:ADMIN_EMAIL) { Write-Host $env:ADMIN_EMAIL } else { Write-Host 'admin@fixfirst.local' }
    Write-Host 'First run downloads dependencies. Wait for Started FixFirstApplication, then visit http://localhost:8080.'
    if ($Demo) {
        Write-Host 'TEMPORARY DEMO: all data is erased when the app stops.'
        & .\mvnw.cmd '-Dspring-boot.run.profiles=demo' spring-boot:run
    } else {
        & .\mvnw.cmd spring-boot:run
    }
    if ($LASTEXITCODE -ne 0) { throw 'Startup failed. Read the error above and consult README.md.' }
} finally {
    Remove-Item Env:DB_PASSWORD,Env:ADMIN_PASSWORD -ErrorAction SilentlyContinue
}
