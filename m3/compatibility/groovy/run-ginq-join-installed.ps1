# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
param(
    [Parameter(Mandatory=$true)][string]$JdkHome,
    [Parameter(Mandatory=$true)][string]$OutputDirectory
)
$ErrorActionPreference = 'Stop'
$java = Join-Path $JdkHome 'bin/java.exe'
$javac = Join-Path $JdkHome 'bin/javac.exe'
$javap = Join-Path $JdkHome 'bin/javap.exe'
$moduleImage = Join-Path $JdkHome 'lib/modules'
$collectionsJmod = Join-Path $JdkHome 'jmods/jdk.m3.collections.jmod'
if (!(Test-Path $java -PathType Leaf) -or !(Test-Path $javac -PathType Leaf) -or !(Test-Path $javap -PathType Leaf)) { throw 'Installed JDK executables missing' }
if (Test-Path $OutputDirectory) { throw 'Output directory must not exist' }
$modules = & $java --list-modules
if ($LASTEXITCODE -ne 0 -or !($modules | Select-String '^jdk\.m3\.collections(@|$)')) { throw 'Candidate does not contain installed jdk.m3.collections module' }
if (!(Test-Path $moduleImage -PathType Leaf) -or !(Test-Path $collectionsJmod -PathType Leaf) -or (Get-Item -LiteralPath $moduleImage).Length -le 0 -or (Get-Item -LiteralPath $collectionsJmod).Length -le 0) { throw 'Missing packaged M3 runtime image or collections jmod' }
$stringApi = & $javap --module java.base java.lang.String
if ($LASTEXITCODE -ne 0) { throw 'Could not inspect installed java.lang.String' }
foreach ($required in @('public java.lang.String capitalize();', 'public java.lang.String uncapitalize();', 'public java.lang.String center(int, java.lang.String);')) {
    if (!(@($stringApi) | Where-Object { $_.Contains($required) })) { throw "Missing candidate M3 String member: $required" }
}
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$test = Join-Path $repo 'test/jdk/jdk/m3/collections/GinqHashJoinTest.java'
if (!(Test-Path $test -PathType Leaf)) { throw 'Missing owning test source' }
New-Item -ItemType Directory -Path $OutputDirectory | Out-Null
$modules | Out-File (Join-Path $OutputDirectory 'installed-modules.txt') -Encoding UTF8
$stringApi | Out-File (Join-Path $OutputDirectory 'm3-string-api.txt') -Encoding UTF8
$runtimePropsPath = Join-Path $OutputDirectory 'runtime-properties.txt'
$runtimeStdoutPath = Join-Path $OutputDirectory 'runtime-stdout.txt'
$identityRun = Start-Process -FilePath $java -ArgumentList '-XshowSettings:properties', '-version' -Wait -PassThru -RedirectStandardError $runtimePropsPath -RedirectStandardOutput $runtimeStdoutPath
if ($identityRun.ExitCode -ne 0) { throw 'Candidate runtime property probe failed' }
$runtimeProperties = Get-Content -LiteralPath $runtimePropsPath
$homeLine = @($runtimeProperties | Where-Object { $_ -match '^\s*java[.]home\s*=' }) | Select-Object -First 1
if (!$homeLine) { throw 'Missing java.home in actual runtime settings' }
$observedHome = ($homeLine -replace '^\s*java[.]home\s*=\s*', '').Trim()
if (!(Test-Path -LiteralPath $observedHome -PathType Container)) { throw 'Runtime java.home does not exist' }
$expectedHome = [System.IO.Path]::GetFullPath($JdkHome).TrimEnd('\', '/')
$actualHome = [System.IO.Path]::GetFullPath($observedHome).TrimEnd('\', '/')
if (![string]::Equals($expectedHome, $actualHome, [StringComparison]::OrdinalIgnoreCase)) { throw 'Candidate java.home differs from selected JDK' }
$native = $env:M3_ARRAYS_NATIVE_PATH
if ([string]::IsNullOrWhiteSpace($native) -or !(Test-Path -LiteralPath $native -PathType Leaf)) { throw 'Missing explicit M3 arrays JNI library path' }
$native = [System.IO.Path]::GetFullPath($native)
$nativeFile = Get-Item -LiteralPath $native
if ($nativeFile.Length -le 0 -or ($nativeFile.Attributes -band [System.IO.FileAttributes]::ReparsePoint)) { throw 'M3 arrays JNI library empty or symlinked' }
$hashes = foreach ($inputPath in @($test, $java, $javac, $javap, $moduleImage, $collectionsJmod, $native, $PSCommandPath)) {
    $digest = Get-FileHash -Algorithm SHA256 -LiteralPath $inputPath
    '{0}  {1}' -f $digest.Hash.ToLowerInvariant(), $digest.Path
}
$hashes | Out-File (Join-Path $OutputDirectory 'input-sha256.txt') -Encoding UTF8
Copy-Item -LiteralPath $runtimePropsPath -Destination (Join-Path $OutputDirectory 'java-version.txt')
& $javac -source 21 -target 21 -Xlint:all -Werror --add-modules jdk.m3.collections -d $OutputDirectory $test 2>&1 | Tee-Object -FilePath (Join-Path $OutputDirectory 'compile.txt')
if ($LASTEXITCODE -ne 0) { throw 'Strict compilation failed' }
& $java "-Dm3.arrays.native.path=$native" --add-modules jdk.m3.collections -cp $OutputDirectory GinqHashJoinTest | Tee-Object (Join-Path $OutputDirectory 'mixed.txt')
if ($LASTEXITCODE -ne 0) { throw 'Mixed-mode join contract failed' }
& $java -Xint "-Dm3.arrays.native.path=$native" --add-modules jdk.m3.collections -cp $OutputDirectory GinqHashJoinTest | Tee-Object (Join-Path $OutputDirectory 'interpreted.txt')
if ($LASTEXITCODE -ne 0) { throw 'Interpreted join contract failed' }
$mixed = Get-Content -Raw -LiteralPath (Join-Path $OutputDirectory 'mixed.txt')
$interpreted = Get-Content -Raw -LiteralPath (Join-Path $OutputDirectory 'interpreted.txt')
if ($mixed -cne $interpreted) { throw 'Mixed and interpreted stdout differ' }
'M3_GINQ_INSTALLED_SOURCE_BOUND_TESTS_PASS' | Tee-Object -FilePath (Join-Path $OutputDirectory 'status.txt')
