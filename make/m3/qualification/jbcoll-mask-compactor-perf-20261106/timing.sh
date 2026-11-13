#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# Timing runs of leaf jbcoll-mask-compactor-perf-20261106 (CompactorTiming, MASK_COMPACTOR_TIMING v1; not JMH).
# Writes the variants (variants.py), compiles them and the probe, then runs the probe RUNS times per CPU set, one JVM
# per run, each pinned to its CPU set from the start (Windows: cmd start /affinity HEX; elsewhere taskset -c LIST is
# expected in place of HEX), C2 only and synchronous (-XX:-TieredCompilation -Xbatch: the same compile decisions in
# every fork; tiered/background compilation made compactRuns bimodal, 2.5x, between forks), -Xms1g -Xmx1g (no GC
# during a run). CPU sets of the qualification host (i9-14900HX, 8 P-cores with HT = CPUs 0-15, 16 E-cores = 16-31):
#   P = 5500 (CPUs 8,10,12,14: one thread of four P-cores)   E = F0000 (CPUs 16-19: four E-cores)
# The two core types rank compactRuns, compactWords and compactScalar differently (TIMING.tsv), so a dispatch decision
# needs both. Each run stays well under 2 minutes; one progress line per run.
# usage: JAVA_HOME=<JDK 21> timing.sh <scratch-dir> [runs] [cpu-sets...]   (default: 3 runs, P and E)
set -uo pipefail
Q=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$Q/../../.." && pwd)
mkdir -p "$1"; S=$(cd "$1" && pwd); RUNS=${2:-3}; SETS=("${@:3}"); [ ${#SETS[@]} -gt 0 ] || SETS=(P:5500 E:F0000)
J="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
SEED=0x4d334a424d534b31
SAMPLES=21
CLASSES=dense,sparse,fragmented,every8th,block,prefix,tail,random50,bern16,cluster12
LENGTHS=64,256,512,1024,4096
LABELS=(master postimage statonly threshold fixed threshold_prefix threshold_words1024 fixed_prefix)
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) WIN=1; SEP=';' ;; *) WIN=0; SEP=':' ;; esac
rm -rf "$S/var" "$S/tdrv"
python -I "$Q/variants.py" --root "$ROOT" --out "$S/var/src" "${LABELS[@]}" > "$S/variants.log" || exit 2
"$J/javac" -Xlint:all -Werror -d "$S/var/cls" "$S"/var/src/jbcoll/*.java > "$S/var.javac.log" 2>&1 || exit 2
"$J/javac" -Xlint:all -Werror -d "$S/tdrv" "$Q/CompactorTiming.java" > "$S/tdrv.javac.log" 2>&1 || exit 2
OPTS="-XX:-TieredCompilation -Xbatch -Xms1g -Xmx1g -Dtiming.classes=$CLASSES -Dtiming.lengths=$LENGTHS"
for set in "${SETS[@]}"; do
    name=${set%%:*}; mask=${set#*:}
    for r in $(seq 1 "$RUNS"); do
        log="$S/timing-$name-$r.log"; start=$(date +%s)
        if [ $WIN = 1 ]; then
            cmdfile="$S/timing-$name-$r.cmd"
            printf '@echo off\r\nstart "" /affinity %s /b /wait "%s\\java.exe" %s -cp "%s%s%s" CompactorTiming %s %s-%s %s %s\r\n' \
                "$mask" "$(cygpath -w "$J")" "$OPTS" "$(cygpath -w "$S/tdrv")" "$SEP" "$(cygpath -w "$S/var/cls")" \
                "$SEED" "$name" "$r" "$SAMPLES" "${LABELS[*]}" > "$cmdfile"
            timeout 115 cmd //c "$(cygpath -w "$cmdfile")" > "$log" 2>&1
        else
            timeout 115 taskset -c "$mask" "$J/java" $OPTS -cp "$S/tdrv$SEP$S/var/cls" CompactorTiming $SEED "$name-$r" \
                $SAMPLES "${LABELS[@]}" > "$log" 2>&1
        fi
        echo "TIMING_RUN set=$name affinity=$mask run=$r rc=$? cells=$(grep -c '^TIMING run' "$log") seconds=$(( $(date +%s) - start ))"
    done
done
