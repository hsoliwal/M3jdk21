#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
#
# ENCODE_FAMILY_JUNIT v1 (2026-11-17). usage: run_junit.sh <leaf-dir> <work-dir>
# Runs after run_proof.sh (which leaves <work-dir>/classes: java.base patched with the eleven method texts and the M3
# stub). Compiles <leaf-dir>/StringEncodeFamilyTest.java and StringEncodeFamilyDonorTest.java (Guava 33.7.2 tests)
# against it and runs them with the JUnit 5 console launcher in the three proof modes (flat; stub M3 storage with
# facts; without facts), each with compact strings on and off; prints the summary of each run. Exit 0 = no failure in
# any run.
set -u
leaf=$1; work=$2
jh=${PROOF_JAVA_HOME:-C:/Program Files/Java/jdk-21}
home=$(cygpath -m "$HOME" 2>/dev/null || echo "$HOME")
junit=${JUNIT_JAR:-$home/.m2/repository/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar}
export MSYS_NO_PATHCONV=1
classes="StringEncodeFamilyTest StringEncodeFamilyDonorTest"
rm -rf "$work/junit"; mkdir -p "$work/junit"
"$jh/bin/javac" -nowarn -d "$work/junit" --patch-module "java.base=$work/classes" -cp "$junit" \
  $(for c in $classes; do echo "$leaf/$c.java"; done) > "$work/javac-junit.log" 2>&1 \
  || { echo "COMPILE_FAIL tests"; head -20 "$work/javac-junit.log"; exit 3; }
echo "ENCODE family JUnit (junit-platform-console-standalone 1.11.4), stock JDK 21 + head method text (proof/run_junit.sh)"
echo "classes: $classes"
status=0
for mode in flat m3-facts m3-scan; do
  case $mode in
    flat) props= ;;
    m3-facts) props="-Dm3.stub.force=true -Dm3.stub.facts=true" ;;
    m3-scan) props="-Dm3.stub.force=true" ;;
  esac
  for compact in -XX:+CompactStrings -XX:-CompactStrings; do
    log="$work/junit-$mode$compact.out"
    # shellcheck disable=SC2086
    "$jh/bin/java" $props "$compact" --patch-module "java.base=$work/classes" -jar "$junit" execute \
      --class-path "$work/junit" $(for c in $classes; do echo "--select-class $c"; done) \
      --details=summary --disable-banner > "$log" 2>&1 || status=1
    echo "== JUnit $mode $compact"
    grep -E 'Test run finished|containers|tests ' "$log"
    grep -E 'MethodSource|=> |JUnit Jupiter:' "$log" | head -10
  done
done
exit $status
