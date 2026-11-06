#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Compile gate of the generated Nebula m3web bundle (javac --release 21, -Xlint:all -Werror) + headless JUnit 4
# model tests (model package only; the SWT canvas tests need a Display: coverage_nebula.sh runs them on the hidden
# test desktop). Usage: gate_nebula.sh <widgets/m3web dir> <out dir> <swt jar> <junit4 jar> <hamcrest jar>
set -euo pipefail
src=$1; out=$2; swt=$3; junit=$4; ham=$5
jdk=${M3_JDK21:-"/c/Program Files/Java/jdk-21"}
w() { cygpath -m "$1"; }
rm -rf "$out"; mkdir -p "$out/main" "$out/test" "$out/snip"
find "$src/org.eclipse.nebula.widgets.m3web/src" -name '*.java' > "$out/main.txt"
"$jdk/bin/javac" -J-Xmx512m --release 21 -Xlint:all,-serial,-this-escape -Werror -d "$(w "$out/main")" -cp "$(w "$swt")" @"$out/main.txt"
find "$src/org.eclipse.nebula.widgets.m3web.test/src" -name '*.java' > "$out/test.txt"
"$jdk/bin/javac" -J-Xmx512m --release 21 -Xlint:all,-serial,-this-escape -Werror -d "$(w "$out/test")" -cp "$(w "$out/main");$(w "$swt");$(w "$junit")" @"$out/test.txt"
find "$src/org.eclipse.nebula.widgets.m3web.snippets/src" -name '*.java' > "$out/snip.txt"
"$jdk/bin/javac" -J-Xmx512m --release 21 -Xlint:all,-serial,-this-escape -Werror -d "$(w "$out/snip")" -cp "$(w "$out/main");$(w "$swt")" @"$out/snip.txt"
echo "NEBULA_COMPILED main=$(wc -l < "$out/main.txt") test=$(wc -l < "$out/test.txt") snippets=$(wc -l < "$out/snip.txt")"
classes=$(cd "$out/test" && find ./org/eclipse/nebula/widgets/m3web/model -name '*_Test.class' | sed 's|^\./||;s|\.class$||;s|/|.|g' | sort | tr '\n' ' ')
"$jdk/bin/java" -Xmx512m -Djava.awt.headless=true -cp "$(w "$out/main");$(w "$out/test");$(w "$swt");$(w "$junit");$(w "$ham")" org.junit.runner.JUnitCore $classes | tail -4
