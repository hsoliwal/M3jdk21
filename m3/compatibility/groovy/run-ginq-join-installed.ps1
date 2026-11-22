# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
param(
    [Parameter(Mandatory=$true)][string]$JdkHome,
    [Parameter(Mandatory=$true)][string]$OutputDirectory
)
$ErrorActionPreference = 'Stop'
$java = Join-Path $JdkHome 'bin/java.exe'
$javac = Join-Path $JdkHome 'bin/javac.exe'
if (!(Test-Path $java -PathType Leaf) -or !(Test-Path $javac -PathType Leaf)) { throw 'Installed JDK executables missing' }
if (Test-Path $OutputDirectory) { throw 'Output directory must not exist' }
$modules = & $java --list-modules
if ($LASTEXITCODE -ne 0 -or !($modules | Select-String '^jdk\.m3\.collections(@|$)')) { throw 'Candidate does not contain installed jdk.m3.collections module' }
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../../..')).Path
$test = Join-Path $repo 'test/jdk/jdk/m3/collections/GinqHashJoinTest.java'
if (!(Test-Path $test -PathType Leaf)) { throw 'Missing owning test source' }
New-Item -ItemType Directory -Path $OutputDirectory | Out-Null
& $java -version 2>&1 | Out-File (Join-Path $OutputDirectory 'java-version.txt')
& $javac -source 21 -target 21 -Xlint:all -Werror --add-modules jdk.m3.collections -d $OutputDirectory $test
if ($LASTEXITCODE -ne 0) { throw 'Strict compilation failed' }
& $java --add-modules jdk.m3.collections -cp $OutputDirectory GinqHashJoinTest | Tee-Object (Join-Path $OutputDirectory 'mixed.txt')
if ($LASTEXITCODE -ne 0) { throw 'Mixed-mode join contract failed' }
& $java -Xint --add-modules jdk.m3.collections -cp $OutputDirectory GinqHashJoinTest | Tee-Object (Join-Path $OutputDirectory 'interpreted.txt')
if ($LASTEXITCODE -ne 0) { throw 'Interpreted join contract failed' }
'M3_GINQ_INSTALLED_SOURCE_BOUND_TESTS_PASS'
