# SPDX-License-Identifier: Apache-2.0
# Runs the conservative GDK/JDK API-collision gate; does not waive behavioral Groovy tests.
[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$StockJava,
    [Parameter(Mandatory=$true)][string]$M3Java,
    [Parameter(Mandatory=$true)][string]$GroovyClasspath,
    [Parameter(Mandatory=$true)][string]$VerifiedJarLock,
    [Parameter(Mandatory=$true)][ValidateSet('4.0.33','5.1.3','6.0.0')][string]$GroovyVersion,
    [Parameter(Mandatory=$true)][string]$OutputDirectory,
    [string]$Javac = 'javac'
)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$build = Join-Path $OutputDirectory 'audit-classes'
New-Item -ItemType Directory -Force $build | Out-Null
& $Javac --release 21 -Xlint:all -Werror -d $build (Join-Path $root 'GroovyExtensionCollisionAudit.java')
if ($LASTEXITCODE -ne 0) { throw 'Groovy extension audit compilation failed' }
& $StockJava -cp $build GroovyExtensionCollisionAudit selftest
if ($LASTEXITCODE -ne 0) { throw 'Groovy extension audit self-test failed' }
$profile = Join-Path $OutputDirectory 'profiles'
& $StockJava -cp $build GroovyExtensionCollisionAudit defaults $profile
if ($LASTEXITCODE -ne 0) { throw 'Groovy donor manifests failed exact generation' }
& $StockJava -cp $build GroovyExtensionCollisionAudit compare-locked `
   $StockJava $M3Java $GroovyClasspath (Join-Path $profile 'owners.txt') `
   (Join-Path $profile 'providers.txt') $VerifiedJarLock `
   (Join-Path $OutputDirectory 'report') $GroovyVersion
if ($LASTEXITCODE -ne 0) { throw 'BLOCK: Groovy extension/JDK member compatibility drift; inspect report.tsv' }
Write-Host 'Real GDK/JDK method-surface audit passed; behavioral GroovyInteropGate still required.'
