@echo off
cd /d "%~dp0"
echo === KmerFret Backend ===

echo Compilation...
call mvnw.cmd compile -q
if errorlevel 1 ( echo Erreur de compilation & exit /b 1 )

if not exist "target\libs" (
    echo Copie des dependances...
    call mvnw.cmd dependency:copy-dependencies -DoutputDirectory=target\libs -Dmdep.useRepositoryLayout=false -DincludeScope=runtime -q
)

echo Demarrage sur http://localhost:8080 ...
java -cp "target\classes;target\libs\*" cm.kmerfret.backend.KmerFretBackendApplication
