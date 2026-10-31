@echo off
rem Copyright 2026 Hitesh Soliwal and contributors
rem SPDX-License-Identifier: Apache-2.0
setlocal
for %%I in ("%~dp0..") do set "JAVA_HOME=%%~fI"
set "ANT_HOME=%JAVA_HOME%\lib\m3-tools\ant"
call "%ANT_HOME%\bin\ant.bat" %*
exit /b %ERRORLEVEL%
