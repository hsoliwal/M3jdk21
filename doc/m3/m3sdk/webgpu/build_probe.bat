@echo off
call H:\BuildTools\VS2022\VC\Auxiliary\Build\vcvars64.bat >nul 2>nul
set DAWN=K:\m3-work\donors\dawn\v20261009.190412\x\Dawn-c943ea32b614ff53e4fe410fc8f4968b6b8fe46d-windows-latest-Release
cd /d K:\m3-work\tmp\m3sdk-design\webgpu\build
cl /nologo /std:c17 /O2 /W4 /WX /MD /I"%DAWN%\include" ..\dawn_alu_probe.c ..\msvc_stl_vectorized_shim.c /Fe:dawn_alu_probe.exe /link "%DAWN%\lib\webgpu_dawn.lib" user32.lib onecore_apiset.lib dxguid.lib bcrypt.lib > build.log 2>&1
echo cl-rc=%ERRORLEVEL%
