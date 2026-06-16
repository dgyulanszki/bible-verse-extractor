@echo off
powershell -ExecutionPolicy Bypass -File "%~dp0build-windows-package.ps1" %*

