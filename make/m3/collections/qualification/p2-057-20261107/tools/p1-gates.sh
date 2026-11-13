#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# P1 gates (Linux, JDK 21): compile master's and the candidate's M3LongIndexPathMasks with M3IndexPathMasks, the JNI
# owner, both self-tests and the crate's behaviour probe (javac --release 21 -Xlint:all,-module -Werror); run
# M3LongIndexPathMasksSelfTest and the probe (tape sha256) on both, default and -Xint; one mutant of the delegation.
# usage: TMPDIR=<tmp> JAVA_HOME=<jdk21> p1-gates.sh <out> <master m3/collections> <candidate m3/collections>
set -uo pipefail
HERE=$(cd "$(dirname "$0")/.." && pwd)
OUT=$1; BASE=$(cd "$2" && pwd); HEAD=$(cd "$3" && pwd)
export PATH=${JAVA_HOME:?}/bin:$PATH
JO="-Djava.io.tmpdir=${TMPDIR:-/tmp} -XX:-UsePerfData"
J=src/main/java/jdk/m3/collections; T=src/test/java/jdk/m3/collections
rm -rf "$OUT"; mkdir -p "$OUT"; cd "$OUT" || exit 9
status=PASS
build() { # <module> <classes> [<owner override>]
    local owner=${3:-$1/$J/M3LongIndexPathMasks.java}
    javac -J-Djava.io.tmpdir=${TMPDIR:-/tmp} --release 21 -proc:none -Xlint:all,-module -Werror -d "$2" \
        "$1/$J/M3IndexPathMasks.java" "$owner" "$1/$J/M3LongIndexPathMasksJNI.java" \
        "$1/$T/M3LongIndexPathMasksSelfTest.java" "$1/$T/M3LongIndexPathMasksJNISelfTest.java" \
        "$HERE/oracle/M3LongPathBridgeBehaviorProbe.java"
}
for side in master candidate; do
    m=$BASE; [ $side = candidate ] && m=$HEAD
    build "$m" "$side" > "$side-javac.log" 2>&1 && echo "$side javac -Werror: OK" || { echo "$side javac FAILED"; status=FAIL; }
    for vm in default xint; do o=(); [ $vm = xint ] && o=(-Xint)
        echo "$side $vm: $(java $JO "${o[@]}" -cp $side jdk.m3.collections.M3LongIndexPathMasksSelfTest 2>&1 | tail -1) | $(java $JO "${o[@]}" -cp $side jdk.m3.collections.M3LongPathBridgeBehaviorProbe 2>&1 | tail -1 | tee "$side-$vm.tape")"
    done
done
for vm in default xint; do cmp -s master-$vm.tape candidate-$vm.tape && echo "tape $vm: master = candidate" || { echo "tape $vm: DIFFERENT"; status=FAIL; }; done
grep -q 'sha256=2b6d2baa61986e2357e8621bdc1d9ed663c4bfe08ae9de00117bd34c2a53bb41' candidate-default.tape && echo "tape = crate record 2b6d2baa6198" || { echo "tape differs from the crate record"; status=FAIL; }
mkdir -p mut; sed '/M3IndexPathMasks.selectLongJava/{n;s/requiredMasks, forbiddenMasks/forbiddenMasks, requiredMasks/}' "$HEAD/$J/M3LongIndexPathMasks.java" > mut/M3LongIndexPathMasks.java
cmp -s "$HEAD/$J/M3LongIndexPathMasks.java" mut/M3LongIndexPathMasks.java && { echo "mutant did not apply"; status=FAIL; }
if build "$HEAD" mut/classes mut/M3LongIndexPathMasks.java > mut/javac.log 2>&1; then
    if java $JO -cp mut/classes jdk.m3.collections.M3LongIndexPathMasksSelfTest > mut/run.out 2>&1; then echo "mutant (required/forbidden swapped): SURVIVED"; status=FAIL
    else echo "mutant (required/forbidden swapped): CAUGHT ($(grep -m1 -E 'Error|Exception' mut/run.out | cut -c1-110))"; fi
else echo "mutant javac failed: $(grep -m1 error mut/javac.log)"; status=FAIL; fi
echo "P1_GATES $status"
[ "$status" = PASS ]
