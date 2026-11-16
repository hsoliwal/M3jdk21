#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# LINE_TERMINATOR_PROOF v1 (2026-11-16). usage: run_proof.sh <head-java-lang-dir> <work-dir>
# Copies the two kernel methods of M3StringFacts and the M3LinesSpliterator class of String out of the head sources
# (awk by declaration line; the copied text is not edited except the four constructor lines that read M3 state),
# compiles them with LineTerminatorProof.java on a stock JDK 21 and runs the differential. Exit 0 = PASS.
set -u
here=$(cd "$(dirname "$0")" && { pwd -W 2>/dev/null || pwd; })
head=$1; work=$2
java_home=${PROOF_JAVA_HOME:-C:/Program Files/Java/jdk-21}
mkdir -p "$work/gen" "$work/classes"
method() { awk -v sig="$1" 'index($0, sig) { on = 1 } on { print } on && /^    }$/ { exit }' "$head/M3StringFacts.java"; }
{
  echo 'final class LineTerminatorKernel {'
  method 'static int lineTerminatorStep('
  method 'static int lineTerminatorSeam('
  echo '}'
} > "$work/gen/LineTerminatorKernel.java"
awk 'index($0, "private static final class M3LinesSpliterator") { on = 1 } on { print } on && /^    }$/ { exit }' \
  "$head/String.java" > "$work/gen/spliterator.txt"
edits=$(grep -cE 'M3LinesSpliterator\(String source\) \{|M3String storage = source\.m3\(\);|M3StringFacts facts = storage == null|this\.terminatorFree = facts != null' "$work/gen/spliterator.txt")
[ "$edits" -eq 4 ] || { echo "CONSTRUCTOR_LINES expected 4 found $edits"; exit 2; }
{
  echo 'import java.util.Objects;'
  echo 'import java.util.Spliterator;'
  echo 'import java.util.Spliterators;'
  echo 'import java.util.function.Consumer;'
  echo 'final class LinesHolder {'
  sed -e 's/M3LinesSpliterator(String source) {/M3LinesSpliterator(String source, boolean terminatorFreeArg) {/' \
      -e '/M3String storage = source\.m3();/d' \
      -e '/M3StringFacts facts = storage == null/d' \
      -e 's/this\.terminatorFree = facts != null.*/this.terminatorFree = terminatorFreeArg;/' \
      -e 's/private static final class M3LinesSpliterator/static final class M3LinesSpliterator/' \
      "$work/gen/spliterator.txt"
  echo '}'
} > "$work/gen/LinesHolder.java"
grep -c 'terminatorFree ? length : start' "$work/gen/LinesHolder.java" | sed 's/^/consumer_line_present\t/'
"$java_home/bin/javac" -Xlint:all -Werror -d "$work/classes" "$work/gen/LineTerminatorKernel.java" \
  "$work/gen/LinesHolder.java" "$here/LineTerminatorProof.java" || { echo "COMPILE_FAIL"; exit 3; }
"$java_home/bin/java" -cp "$work/classes" LineTerminatorProof
