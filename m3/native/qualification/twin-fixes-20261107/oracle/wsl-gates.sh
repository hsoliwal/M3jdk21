#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Linux-side gates of recipe qualification/twin-fixes-20261107 (m3/native), run under WSL Ubuntu (gcc 13.3, JDK 21):
#   1. FIXTURE: oracle/native/MissingFullSymbolLibrary.c -> libm3_scoped_leases.so with gcc -std=c11 -Wall -Wextra
#      -Werror -Wconversion -Wshadow (the donor crate's flags); nm must show JNI_OnLoad and no Java_ symbol;
#   2. ORACLE N: the relocated M3NativeFullSymbolFallbackOracle against MASTER classes (must end in UnsatisfiedLinkError)
#      and against HEAD classes (PASS) with the fixture on java.library.path, -Xcheck:jni, JIT / -Xint / C1; HEAD stdout
#      identical across the three modes; and the donor oracle compiled from the donor closure (the donor-package tree
#      that oracles.sh exported from the blobless donor clone at the leaf-N merge commit: the host git reads the
#      worktree, a Linux git cannot follow a Windows worktree's .git link; the donor package name is read from the
#      relocation map, never written here) versus the relocated one;
#   3. TEST N: the relocated JUnit test M3NativeLeaseFullscanFallbackTest (JUnit console) with the fixture on the path
#      against MASTER classes (must fail: UnsatisfiedLinkError) and HEAD classes (4/4 pass);
#   4. NATIVE MATRIX: the module's recipe/scoped-leases-20261106/native-matrix.sh on the HEAD sources (the real C17
#      library m3_scoped_leases.c with -Werror, the three exported symbols, every self-test under -Xcheck:jni in the
#      three modes, the preflight and code-carrier oracles).
# usage: wsl-gates.sh <repo root> <donor-src dir (oracles.sh <out>/donor-src)> <master classes> <head classes>
#        <head test-classes> <heap jar> <junit console jar> <scratch dir>
# env: JAVA_HOME (a JDK 21 with include/linux), CC (default gcc), JTMP (java.io.tmpdir for every JVM; default <scratch>/tmp)
set -uo pipefail
ROOT=$1; DONOR_SRC=$2; MC=$3; HC=$4; HT=$5; HEAP=$6; CONSOLE=$7; S=$8
HERE="$ROOT/m3/native/qualification/twin-fixes-20261107"
J="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
CC=${CC:-gcc}
JTMP=${JTMP:-$S/tmp}
mkdir -p "$S/fixture" "$S/classes" "$S/donor" "$S/tests" "$S/matrix" "$JTMP"
JVM=(-Djava.io.tmpdir="$JTMP" -XX:-UsePerfData -Xcheck:jni "-Djava.library.path=$S/fixture")
status=PASS
same() { if cmp -s "$2" "$3"; then echo "$1 IDENTICAL sha256=$(sha256sum < "$2" | cut -c1-16)"; else echo "$1 DIFFERENT"; status=FAIL; fi; }
echo "== 1 FIXTURE ($("$CC" --version | head -1))"
"$CC" -std=c11 -Wall -Wextra -Werror -Wconversion -Wshadow -fPIC -shared -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
    "$HERE/oracle/native/MissingFullSymbolLibrary.c" -o "$S/fixture/libm3_scoped_leases.so" || { echo "FIXTURE gcc FAIL"; exit 2; }
nm -D --defined-only "$S/fixture/libm3_scoped_leases.so" | awk '{print $NF}' | grep -E '^(JNI_OnLoad|Java_)' | sed 's/^/  export /'
nm -D --defined-only "$S/fixture/libm3_scoped_leases.so" | grep -q ' Java_' && { echo "FIXTURE exports a Java_ symbol"; status=FAIL; }
echo "== 2 ORACLE N"
"$J/javac" -J-Djava.io.tmpdir="$JTMP" --release 21 -proc:none -Xlint:all -Werror -cp "$HC:$HEAP" -d "$S/classes" \
    "$HERE/oracle/jdk/m3/jni/M3NativeFullSymbolFallbackOracle.java" || { echo "ORACLE javac FAIL"; exit 2; }
"$J/java" "${JVM[@]}" -cp "$S/classes:$MC:$HEAP" jdk.m3.jni.M3NativeFullSymbolFallbackOracle > "$S/oracle-master.txt" 2>&1
rc=$?; echo "  MASTER exit=$rc $(grep -m1 -E 'UnsatisfiedLinkError|PASS|Error' "$S/oracle-master.txt" | cut -c1-140)"
[ $rc -ne 0 ] && grep -q UnsatisfiedLinkError "$S/oracle-master.txt" || { echo "  MASTER did not fail with UnsatisfiedLinkError"; status=FAIL; }
for mode in jit xint c1; do
    X=""; [ $mode = xint ] && X="-Xint"; [ $mode = c1 ] && X="-XX:TieredStopAtLevel=1"
    "$J/java" "${JVM[@]}" $X -cp "$S/classes:$HC:$HEAP" jdk.m3.jni.M3NativeFullSymbolFallbackOracle > "$S/oracle-head-$mode.txt" 2>&1 || { echo "  HEAD $mode exit=$?"; status=FAIL; }
done
same "  HEAD jit-vs-xint" "$S/oracle-head-jit.txt" "$S/oracle-head-xint.txt"
same "  HEAD jit-vs-c1" "$S/oracle-head-jit.txt" "$S/oracle-head-c1.txt"
echo "  HEAD: $(tail -1 "$S/oracle-head-jit.txt")"
donor_pkg=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$ROOT/m3/native/recipe/scoped-leases-20261106/RELOCATION_MAP.tsv")
DP="$DONOR_SRC/${donor_pkg//./\/}"
for f in M3RuntimeHeap M3NativeLeaseScan M3ScopedLeaseTable NativeFullSymbolFallbackOracle; do
    [ -f "$DP/$f.java" ] || { echo "donor closure file $DP/$f.java missing (run oracles.sh first)"; exit 2; }
done
echo "  donor closure: $(cd "$DP" && sha256sum M3NativeLeaseScan.java NativeFullSymbolFallbackOracle.java | cut -c1-16 | tr '\n' ' ')"
"$J/javac" -J-Djava.io.tmpdir="$JTMP" --release 21 -proc:none -Xlint:all -Werror -d "$S/donor" "$DP"/M3RuntimeHeap.java "$DP"/M3NativeLeaseScan.java "$DP"/M3ScopedLeaseTable.java "$DP"/NativeFullSymbolFallbackOracle.java || { echo "donor javac FAIL"; exit 2; }
for mode in jit xint c1; do
    X=""; [ $mode = xint ] && X="-Xint"; [ $mode = c1 ] && X="-XX:TieredStopAtLevel=1"
    "$J/java" "${JVM[@]}" $X -cp "$S/donor" "$donor_pkg.NativeFullSymbolFallbackOracle" > "$S/oracle-donor-$mode.txt" 2>&1 || { echo "  donor $mode exit=$?"; status=FAIL; }
    same "  oracle N $mode donor-vs-relocated" "$S/oracle-donor-$mode.txt" "$S/oracle-head-$mode.txt"
done
echo "== 3 TEST N (JUnit console, fixture on the path)"
"$J/javac" -J-Djava.io.tmpdir="$JTMP" --release 21 -proc:none -nowarn -cp "$HC:$HEAP:$CONSOLE" -d "$S/tests" \
    "$ROOT/m3/native/src/test/java/jdk/m3/jni/M3NativeLeaseFullscanFallbackTest.java" || { echo "TEST javac FAIL"; exit 2; }
for side in master head; do
    C=$MC; [ $side = head ] && C=$HC
    "$J/java" "${JVM[@]}" -jar "$CONSOLE" execute --disable-banner --details=summary --class-path "$S/tests:$C:$HEAP" \
        --select-class jdk.m3.jni.M3NativeLeaseFullscanFallbackTest > "$S/test-$side.txt" 2>&1
    echo "  $side: $(grep -E 'tests (successful|failed)' "$S/test-$side.txt" | sed 's/^ *//' | tr '\n' ' ')"
done
grep -q '4 tests failed\|[1-9] tests failed' "$S/test-master.txt" && grep -q 'UnsatisfiedLinkError' "$S/test-master.txt" || { echo "  TEST N did not fail on MASTER"; status=FAIL; }
grep -q '4 tests successful' "$S/test-head.txt" && grep -q '0 tests failed' "$S/test-head.txt" || { echo "  TEST N did not pass on HEAD"; status=FAIL; }
echo "== 4 NATIVE MATRIX (head sources)"
SRC="$ROOT/m3/native/src/main/java/jdk/m3/jni"; TST="$ROOT/m3/native/src/test/java/jdk/m3/jni"
JAVA_HOME="$JAVA_HOME" CC="$CC" bash "$ROOT/m3/native/recipe/scoped-leases-20261106/native-matrix.sh" jdk.m3.jni jdk.m3.jni.requireNative \
    "$ROOT/m3/native/src/main/native/m3_scoped_leases.c" "$S/matrix" \
    "$SRC"/M3ScopedLeaseTable.java "$SRC"/M3NativeLeaseScan.java "$SRC"/M3LeaseScopes.java "$SRC"/M3LeaseWindowScan.java \
    "$SRC"/M3JniArrayReferences.java "$SRC"/M3JniByteConcat.java "$ROOT/m3/collections/src/main/java/jdk/m3/collections/M3RuntimeHeap.java" \
    "$TST"/M3ScopedLeaseTableSelfTest.java "$TST"/M3ScopedLeaseWindowSelfTest.java "$TST"/M3LeaseScopesSelfTest.java \
    "$TST"/M3JniArrayBatchSelfTest.java "$TST"/M3NativeLeasePreflightOracle.java "$TST"/M3NativeLeaseCodeCarrierOracle.java \
    "$TST"/M3NativeConcatParityProbe.java > "$S/matrix.log" 2>&1
rc=$?; grep -E '^(SYMBOL|NATIVE_MATRIX)' "$S/matrix.log" | sed 's/^/  /'; [ $rc -eq 0 ] || { echo "  native-matrix exit=$rc"; tail -5 "$S/matrix.log"; status=FAIL; }
echo "WSL_GATES $status"
[ "$status" = PASS ]
