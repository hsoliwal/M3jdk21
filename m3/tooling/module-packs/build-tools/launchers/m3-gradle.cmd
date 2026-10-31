@echo off
rem Copyright 2026 Hitesh Soliwal and contributors
rem SPDX-License-Identifier: Apache-2.0
setlocal
for %%I in ("%~dp0..") do set "JAVA_HOME=%%~fI"
call "%JAVA_HOME%\lib\m3-tools\gradle\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
