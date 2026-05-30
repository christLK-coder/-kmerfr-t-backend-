Set-Location $PSScriptRoot
Write-Host "=== KmerFret Backend ===" -ForegroundColor Green

# Liberer le port 8081 et tuer tout processus Java KmerFret existant
$javaProcs = Get-Process java -ErrorAction SilentlyContinue
foreach ($p in $javaProcs) {
    try {
        $cmdLine = (Get-WmiObject Win32_Process -Filter "ProcessId=$($p.Id)").CommandLine
        if ($cmdLine -like "*KmerFretBackendApplication*" -or $cmdLine -like "*kmerfret*") {
            Write-Host "Arret processus Java KmerFret (PID $($p.Id))..." -ForegroundColor Yellow
            Stop-Process -Id $p.Id -Force
            Start-Sleep -Seconds 2
        }
    } catch {}
}

# Liberer port 8081 si occupe
$conn = netstat -ano | Select-String "0.0.0.0:8081\s"
if ($conn) {
    $pid8081 = ($conn -split '\s+')[-1] | Select-Object -First 1
    if ($pid8081 -match '^\d+$') {
        Write-Host "Port 8081 occupe (PID $pid8081) - liberation..." -ForegroundColor Yellow
        Stop-Process -Id ([int]$pid8081) -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 1
    }
}

# Compiler sans clean (evite le verrou sur les JARs)
Write-Host "Compilation..." -ForegroundColor Cyan
.\mvnw.cmd compile -q 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Erreur compilation. Tentative avec clean..." -ForegroundColor Yellow
    Start-Sleep -Seconds 2
    .\mvnw.cmd clean compile -q 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Echec compilation." -ForegroundColor Red
        exit 1
    }
}

# Copier les dependances si le dossier est vide ou absent
$libsPath = Join-Path $PSScriptRoot "target\libs"
$jarCount  = if (Test-Path $libsPath) { (Get-ChildItem $libsPath -Filter "*.jar").Count } else { 0 }
if ($jarCount -lt 10) {
    Write-Host "Copie des dependances ($jarCount JARs trouves)..." -ForegroundColor Cyan
    .\mvnw.cmd "dependency:copy-dependencies" `
        "-DoutputDirectory=target\libs" `
        "-Dmdep.useRepositoryLayout=false" `
        "-DincludeScope=runtime" `
        "-q" 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Erreur copie des dependances." -ForegroundColor Red
        exit 1
    }
}

# Construire le classpath
$sep  = [System.IO.Path]::PathSeparator
$cp   = "target\classes" + $sep + "target\libs\*"
$main = "cm.kmerfret.backend.KmerFretBackendApplication"

Write-Host "Demarrage sur http://localhost:8081 ..." -ForegroundColor Green
Write-Host "(Ctrl+C pour arreter)" -ForegroundColor Gray
& java -cp $cp $main
