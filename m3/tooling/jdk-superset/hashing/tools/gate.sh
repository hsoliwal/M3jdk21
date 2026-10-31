#!/bin/sh
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# gate.sh <repo root> <out dir> <junit classes dir> <M3GateJUnitRun classes dir>
# The gates of the zero-allocation-hashing absorption on a stock JDK 21 (M3_GATE_JDK, default the Windows JDK 21
# install; not JAVA_HOME, which may name another release) with java.base patched, no JDK
# build needed. Windows paths (C:/...) are expected, since the class paths below use ';'.
#   oracle      the vendored donor compiled unchanged (+ JetBrains annotations, JSR-305 sources)
#   recipe      run_absorb.sh into <out>/absorb must reproduce the 16 committed sources byte for byte
#   patch       the 16 jdk.internal.m3.hash sources with java.base's build flags (-Xlint:all -Werror)
#   identity    bytecode_identity.py: the recipe output before LITERAL_CONSTANTS vs the committed sources, compiled
#               alike: every method instruction-identical, only private static final constants added
#   junit       M3HashAtomsTest, oracle prebuilt, then compiled at run time from test.src (the jtreg path);
#               M3HashJitAtomsTest (the JIT leaf's atoms, the read atom, the entry checks, the bounds proof) the same
#   surface     api_surface.py: vendored vs absorbed public/protected members
#   mutants     mutation_gate.py: every recipe-slip mutant must fail a test of either class
#   perf        M3HashAtomsPerf rows (evidence; -DM3_PERF_SLACK=<factor> to guard)
set -eu
repo=$1
out=$2
junit=$3
runner=$4
jbin=${M3_GATE_JDK:-"C:/Program Files/Java/jdk-21"}/bin
lane=$repo/m3/tooling/jdk-superset/hashing
vendor=$repo/m3/vendor/third_party
tests=$repo/test/jdk/jdk/internal/m3/hash
src=$repo/src/java.base/share/classes/jdk/internal/m3/hash
mkdir -p "$out/oracle" "$out/psrc/jdk/internal/m3/hash" "$out/patch" "$out/prehoist" "$out/testc" "$out/testj" \
    "$out/perfc" "$out/mutants" "$out/tmp"
"$jbin/javac" -proc:none -nowarn -XDsuppressNotes --add-exports java.base/sun.nio.ch=ALL-UNNAMED \
    -sourcepath "$vendor/annotations/src/main/java;$vendor/jsr305/src/main/java" -d "$out/oracle" \
    $(find "$vendor/zero-allocation-hashing/src/main/java" -name '*.java')
sh "$lane/tools/run_absorb.sh" "$repo" "$out/absorb" "$out/absorb/dst" > /dev/null
diff -r "$out/absorb/dst" "$src" > /dev/null
echo "recipe: reproduces the $(ls "$src" | wc -l) committed sources"
cp "$src"/*.java "$out/psrc/jdk/internal/m3/hash/"
flags="-g -Xlint:all -Werror -Xlint:-this-escape -XDstringConcat=inline"
"$jbin/javac" $flags --patch-module "java.base=$out/psrc" -d "$out/patch" $(find "$out/psrc" -name '*.java')
echo "patch: $(find "$out/patch" -name '*.class' | wc -l) classes"
"$jbin/javac" $flags --patch-module "java.base=$out/absorb/renamed" -d "$out/prehoist" \
    $(find "$out/absorb/renamed" -name '*.java')
python "$lane/tools/bytecode_identity.py" "$jbin/javap" "$out/prehoist" "$out/patch" "$out/BYTECODE_IDENTITY.tsv"
"$jbin/javac" -g -Xlint:all -Xlint:-serial --patch-module "java.base=$out/patch" \
    --add-exports java.base/jdk.internal.m3.hash=ALL-UNNAMED -cp "$junit" -d "$out/testc" \
    "$tests/M3HashOracle.java" "$tests/M3HashAtomsTest.java"
"$jbin/javac" -g -Xlint:all -Xlint:-serial --patch-module "java.base=$out/patch" \
    --add-exports java.base/jdk.internal.m3.hash=ALL-UNNAMED -cp "$junit" -d "$out/testj" \
    "$tests/M3HashOracle.java" "$tests/M3HashJitAtomsTest.java"
"$jbin/javac" -g -Xlint:all -Xlint:-serial --patch-module "java.base=$out/patch" \
    --add-exports java.base/jdk.internal.m3.hash=ALL-UNNAMED -d "$out/perfc" \
    "$tests/M3HashOracle.java" "$tests/M3HashAtomsPerf.java"
run="--patch-module java.base=$out/patch --add-opens java.base/jdk.internal.m3.hash=ALL-UNNAMED --add-exports java.base/sun.nio.ch=ALL-UNNAMED"
"$jbin/java" -Xmx1g $run "-Dm3.hash.oracle=$out/oracle" -cp "$out/testc;$junit;$runner" M3GateJUnitRun "$out/testc"
"$jbin/java" -Xmx1g "-Djava.io.tmpdir=$out/tmp" $run "-Dtest.src=$tests" -cp "$out/testc;$junit;$runner" \
    M3GateJUnitRun "$out/testc"
"$jbin/java" -Xmx1g $run "-Dm3.hash.oracle=$out/oracle" -cp "$out/testj;$junit;$runner" M3GateJUnitRun "$out/testj"
"$jbin/java" -Xmx1g "-Djava.io.tmpdir=$out/tmp" $run "-Dtest.src=$tests" -cp "$out/testj;$junit;$runner" \
    M3GateJUnitRun "$out/testj"
python "$lane/tools/api_surface.py" "$jbin/javap" "$out/oracle" "$out/patch" "$out/API_SURFACE.tsv"
python "$lane/tools/mutation_gate.py" "$jbin" "$src" "$out/testc,$out/testj" "$junit;$runner" "$out/oracle" \
    "$out/mutants"
"$jbin/java" -Xmx512m $run "-Dm3.hash.oracle=$out/oracle" -cp "$out/perfc" M3HashAtomsPerf
