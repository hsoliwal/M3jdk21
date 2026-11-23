# SPDX-License-Identifier: Apache-2.0
# Native Windows PowerShell smoke (uses JAVA_HOME if configured, no WSL).
$ErrorActionPreference = 'Stop'
$dir = Split-Path -Parent $MyInvocation.MyCommand.Path
$temp = Join-Path ([IO.Path]::GetTempPath()) ('m3-groovy-audit-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Force -Path $temp | Out-Null
try {
    $java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\java.exe' } else { (Get-Command java).Source }
    $javac = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\javac.exe' } else { (Get-Command javac).Source }
    $jar = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin\jar.exe' } else { (Get-Command jar).Source }
    $classes = Join-Path $temp 'classes'
    New-Item -ItemType Directory -Path $classes | Out-Null
    & $javac --release 21 -Xlint:all -Werror -d $classes (Join-Path $dir 'GroovyExtensionCollisionAudit.java')
    if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
    & $java -cp $classes GroovyExtensionCollisionAudit selftest
    if ($LASTEXITCODE -ne 0) { throw 'Self-test failed' }
    $defaults = Join-Path $temp 'defaults'
    & $java -cp $classes GroovyExtensionCollisionAudit defaults $defaults
    if ($LASTEXITCODE -ne 0) { throw 'Defaults failed' }
    & $java -cp $classes GroovyExtensionCollisionAudit defaults $defaults
    if ($LASTEXITCODE -ne 0) { throw 'Defaults were not fixed-point' }
    [IO.File]::WriteAllText((Join-Path $temp 'owners.txt'), "GroovyExtensionCollisionAudit`$SyntheticReceiver`n")
    [IO.File]::WriteAllText((Join-Path $temp 'providers.txt'), "GroovyExtensionCollisionAudit`$SyntheticExtension`n")
    $empty = Join-Path $temp 'empty'
    New-Item -ItemType Directory -Path $empty | Out-Null
    & $jar --create --file (Join-Path $temp 'mock-groovy.jar') -C $empty .
    if ($LASTEXITCODE -ne 0) { throw 'Jar creation failed' }
    $hash = (Get-FileHash (Join-Path $temp 'mock-groovy.jar') -Algorithm SHA256).Hash.ToLowerInvariant()
    [IO.File]::WriteAllText((Join-Path $temp 'lock.tsv'), "mock-groovy.jar`t$hash`n")
    & $java -cp $classes GroovyExtensionCollisionAudit compare-synthetic-locked $java $java `
        (Join-Path $temp 'mock-groovy.jar') (Join-Path $temp 'owners.txt') `
        (Join-Path $temp 'providers.txt') (Join-Path $temp 'lock.tsv') (Join-Path $temp 'report')
    if ($LASTEXITCODE -ne 0) { throw 'Mock orchestrator failed' }
    $lines = Get-Content (Join-Path $temp 'report\report.tsv')
    if ($lines[0] -ne "status`tMOCK_PASS") { throw 'Bad audit status' }
    [IO.File]::WriteAllText((Join-Path $temp 'lock.tsv'), "mock-groovy.jar`t$('0'*64)`n")
    & $java -cp $classes GroovyExtensionCollisionAudit compare-synthetic-locked $java $java `
        (Join-Path $temp 'mock-groovy.jar') (Join-Path $temp 'owners.txt') `
        (Join-Path $temp 'providers.txt') (Join-Path $temp 'lock.tsv') (Join-Path $temp 'negative') 2>$null
    if ($LASTEXITCODE -eq 0) { throw 'Tampered jar lock was accepted' }
    Write-Host 'M3_GROOVY_EXTENSION_ORCHESTRATOR_PASS mockOnly=true'
} finally { Remove-Item -LiteralPath $temp -Recurse -Force -ErrorAction SilentlyContinue }
