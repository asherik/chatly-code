@echo off
setlocal

set "ROOT=%~dp0"
set "DEFAULT_PROJECT=C:\Users\asher\PycharmProjects\chatly\chatly_v2"
set "APP=%ROOT%app-desktop\build\install\app-desktop\bin\app-desktop.bat"

cd /d "%ROOT%" || exit /b 1

call "%ROOT%gradlew.bat" :app-desktop:installDist
if errorlevel 1 exit /b %errorlevel%

if "%~1"=="" (
    call "%APP%" --project="%DEFAULT_PROJECT%"
) else (
    call "%APP%" %*
)

endlocal
