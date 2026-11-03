@echo off
setlocal
cd /d "%~dp0.."
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tag_entries.ps1"
set EXITCODE=%ERRORLEVEL%
echo.
echo Press any key to close this window.
pause >nul
exit /b %EXITCODE%
