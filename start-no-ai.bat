@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "ROOT_DIR=%~dp0"
set "BACK_DIR=%ROOT_DIR%back"
set "FRONT_DIR=%ROOT_DIR%front"

if not exist "%BACK_DIR%\pom.xml" (
    echo Missing file: back\pom.xml
    exit /b 1
)

if not exist "%FRONT_DIR%\package.json" (
    echo Missing file: front\package.json
    exit /b 1
)

if not exist "%FRONT_DIR%\node_modules" (
    echo Installing frontend dependencies...
    pushd "%FRONT_DIR%"
    call npm install
    set "NPM_EXIT=!ERRORLEVEL!"
    popd
    if not "!NPM_EXIT!"=="0" (
        echo npm install failed.
        exit /b !NPM_EXIT!
    )
)

start "WMS Backend" cmd /k "cd /d ""%BACK_DIR%"" && call mvnw.cmd spring-boot:run"
start "WMS Frontend" cmd /k "cd /d ""%FRONT_DIR%"" && npm run dev"

echo Backend and frontend launch commands have been started in new windows.
echo Backend: http://localhost:8080
echo Frontend: http://localhost:5173
exit /b 0