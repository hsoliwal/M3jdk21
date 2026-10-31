# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# perf_jit_ab.ps1 -JavaBin <dir> -TestClasses <dir> -Out <tsv> -Rounds <n> -Variants label=classes,label=classes,...
#                 [-MainClass M3NumberParseJitPerf]
# PERF_JIT_AB v1: like perf_ab_high.ps1 (PERF_AB v1), for any number of variants, one of them may be the stock JDK
# (label=stock with an empty classes part: no --patch-module). Every measured JVM runs at High priority; the variant
# order is reversed on even rounds (ABBA), so run-order bias cancels in paired ratios. The harness prints rows
# atom ns_per_op_atom ns_per_op_reference speedup system_cpu_load (M3NumberParseJitPerf); each row is written with
# its round and variant. A run that prints no row is recorded as RUN_FAILED.
param([string]$JavaBin, [string]$TestClasses, [string]$Out, [int]$Rounds, [string]$Variants,
      [string]$MainClass = 'M3NumberParseJitPerf')
$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("round`tvariant`tatom`tns_per_op_atom`tns_per_op_reference`tspeedup`tsystem_cpu_load")
for ($r = 1; $r -le $Rounds; $r++) {
    $order = $Variants.Split(',')
    if ($r % 2 -eq 0) { [array]::Reverse($order) }
    foreach ($spec in $order) {
        $label, $classes = $spec.Split('=', 2)
        $patch = if ($classes) { "--patch-module java.base=$classes " } else { '' }
        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName = Join-Path $JavaBin 'java.exe'
        $psi.Arguments = "-Xmx1g $patch--add-opens java.base/jdk.internal.math=ALL-UNNAMED -cp $TestClasses $MainClass"
        $psi.UseShellExecute = $false
        $psi.RedirectStandardOutput = $true
        $psi.RedirectStandardError = $true
        $p = [System.Diagnostics.Process]::Start($psi)
        $p.PriorityClass = [System.Diagnostics.ProcessPriorityClass]::High
        $stdout = $p.StandardOutput.ReadToEnd()
        $null = $p.StandardError.ReadToEnd()
        $p.WaitForExit()
        $rows = $stdout -split "`r?`n" | Where-Object { ($_ -split "`t").Count -eq 5 -and -not $_.StartsWith('atom') }
        if (-not $rows) { $lines.Add("$r`t$label`tRUN_FAILED`t`t`t`t") }
        foreach ($row in $rows) { $lines.Add("$r`t$label`t$row") }
        Write-Output "round $r $label done"
    }
}
[System.IO.File]::WriteAllText($Out, ($lines -join "`n") + "`n")
