@echo off
setlocal
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0tag_entries.ps1" %*
exit /b %ERRORLEVEL%
