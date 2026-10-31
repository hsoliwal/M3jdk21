# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# perf_ab_high.ps1 -JavaBin <dir> -TestClasses <dir> -Out <tsv> -Rounds <n> -Variants label=classes,label=classes
#                  [-Affinity <hex mask>]
# PERF_AB v1, Windows variant for a loaded host: same interleaving and output as perf_ab.sh, but every measured JVM runs
# at High priority so other processes on the host disturb the timing less; -Affinity pins it to a logical-CPU mask
# (on a hybrid CPU: the performance cores, so every run measures the same core type). Variant order alternates per
# round (ABBA) so a run-order bias (the second JVM of a round on a warmer CPU) cancels. One row per atom per variant
# per round.
param([string]$JavaBin, [string]$TestClasses, [string]$Out, [int]$Rounds, [string]$Variants, [string]$Affinity = '')
$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("round`tvariant`tatom`tns_per_op_atom`tns_per_op_reference`tspeedup")
for ($r = 1; $r -le $Rounds; $r++) {
    $order = $Variants.Split(',')
    if ($r % 2 -eq 0) { [array]::Reverse($order) }  # ABBA: even rounds run the variants in reverse order
    foreach ($spec in $order) {
        $label, $classes = $spec.Split('=', 2)
        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName = Join-Path $JavaBin 'java.exe'
        $psi.Arguments = "-Xmx1g --patch-module java.base=$classes --add-opens java.base/jdk.internal.math=ALL-UNNAMED -cp $TestClasses M3NumberParseAtomsPerf"
        $psi.UseShellExecute = $false
        $psi.RedirectStandardOutput = $true
        $psi.RedirectStandardError = $true
        $p = [System.Diagnostics.Process]::Start($psi)
        $p.PriorityClass = [System.Diagnostics.ProcessPriorityClass]::High
        if ($Affinity) { $p.ProcessorAffinity = [IntPtr][Convert]::ToInt64($Affinity, 16) }
        $stdout = $p.StandardOutput.ReadToEnd()
        $null = $p.StandardError.ReadToEnd()
        $p.WaitForExit()
        $rows = $stdout -split "`r?`n" | Where-Object { ($_ -split "`t").Count -eq 4 -and -not $_.StartsWith('atom') }
        if (-not $rows) { $lines.Add("$r`t$label`tRUN_FAILED`t`t`t") }
        foreach ($row in $rows) { $lines.Add("$r`t$label`t$row") }
        Write-Output "round $r $label done"
    }
}
[System.IO.File]::WriteAllText($Out, ($lines -join "`n") + "`n")
