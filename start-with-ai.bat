@echo off
setlocal EnableExtensions EnableDelayedExpansion

set "ROOT_DIR=%~dp0"
set "BACK_DIR=%ROOT_DIR%back"
set "FRONT_DIR=%ROOT_DIR%front"
set "ENV_FILE=%BACK_DIR%\assistant-llm.env"

if not exist "%BACK_DIR%\pom.xml" (
    echo Missing file: back\pom.xml
    exit /b 1
)

if not exist "%FRONT_DIR%\package.json" (
    echo Missing file: front\package.json
    exit /b 1
)

if exist "%ENV_FILE%" (
    echo Found existing file: back\assistant-llm.env
    choice /C YN /N /M "Reuse it? [Y/N]: "
    if errorlevel 2 (
        echo.
        call :writeEnvFile
    )
) else (
    call :writeEnvFile
)

call :loadEnvFile

if "%QWEN_API_KEY%%GLM_API_KEY%%KIMI_API_KEY%%DEEPSEEK_API_KEY%"=="" (
    echo No model API key found in back\assistant-llm.env
    echo Configure at least one provider key before starting.
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

:writeEnvFile
echo.
echo ========================================
echo AI assistant local env setup
echo This file is local-only and should stay out of git.
echo Leave a value blank if you do not use that model.
echo ========================================
echo.

call :prompt QWEN_API_KEY "Qwen API key"
call :prompt GLM_API_KEY "GLM API key"
call :prompt KIMI_API_KEY "Kimi API key"
call :promptDefault KIMI_ENDPOINT "Kimi endpoint" "https://api.moonshot.cn/v1/chat/completions"
call :promptDefault KIMI_MODEL_CODE "Kimi model code" "moonshot-v1-8k"
call :prompt DEEPSEEK_API_KEY "DeepSeek API key"

(
    echo # Local AI assistant keys for Spring Boot
    echo QWEN_API_KEY=!QWEN_API_KEY!
    echo GLM_API_KEY=!GLM_API_KEY!
    echo KIMI_API_KEY=!KIMI_API_KEY!
    echo KIMI_ENDPOINT=!KIMI_ENDPOINT!
    echo KIMI_MODEL_CODE=!KIMI_MODEL_CODE!
    echo DEEPSEEK_API_KEY=!DEEPSEEK_API_KEY!
) > "%ENV_FILE%"

echo.
echo Saved: back\assistant-llm.env
echo.
goto :EOF

:loadEnvFile
for /f "usebackq tokens=1,* delims==" %%A in ("%ENV_FILE%") do (
    set "KEY=%%A"
    set "VALUE=%%B"
    if defined KEY if not "!KEY:~0,1!"=="#" if not "!KEY!"=="" set "!KEY!=!VALUE!"
)
goto :EOF

:prompt
set "%~1="
set /p "%~1=%~2: "
goto :EOF

:promptDefault
set "%~1="
set /p "%~1=%~2 [default: %~3]: "
if not defined %~1 set "%~1=%~3"
goto :EOF