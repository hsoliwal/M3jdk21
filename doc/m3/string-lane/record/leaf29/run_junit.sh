#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
#
# SPLIT_FAMILY_JUNIT v1 (2026-11-17). usage: run_junit.sh <leaf-dir> <work-dir> <junit-console-standalone.jar>
# Runs after run_proof.sh (which leaves the patched java.base classes in <work-dir>/classes): compiles the family's
# JUnit classes (String<Family>Test.java and the ported String<Family>DonorTest.java of <leaf-dir>) against them and
# runs them with the JUnit console launcher in the three proof modes (flat; stub M3 storage with facts; without
# facts), each with compact strings on and off. Prints the console summary of each run. Exit 0 = no failure.
set -u
leaf=$1; work=$2; junit=$3
jh=${PROOF_JAVA_HOME:-C:/Program Files/Java/jdk-21}
export MSYS_NO_PATHCONV=1
rm -rf "$work/junit"; mkdir -p "$work/junit"
"$jh/bin/javac" -nowarn -d "$work/junit" --patch-module "java.base=$work/classes" -cp "$junit" \
  "$leaf"/String*FamilyTest.java "$leaf"/String*FamilyDonorTest.java > "$work/javac-junit.log" 2>&1 \
  || { echo "COMPILE_FAIL junit"; head -30 "$work/javac-junit.log"; exit 3; }
status=0
for mode in flat m3-facts m3-scan; do
  case $mode in
    flat) props= ;;
    m3-facts) props="-Dm3.stub.force=true -Dm3.stub.facts=true" ;;
    m3-scan) props="-Dm3.stub.force=true" ;;
  esac
  for compact in -XX:+CompactStrings -XX:-CompactStrings; do
    echo "== JUnit $mode $compact"
    # shellcheck disable=SC2086
    "$jh/bin/java" $props "$compact" --patch-module "java.base=$work/classes" -jar "$junit" execute \
      --disable-banner --details=summary --class-path "$work/junit" --scan-class-path \
      > "$work/junit-$mode$compact.out" 2>&1 || status=1
    grep -E "tests (found|successful|failed|aborted|skipped)|containers failed|MethodSource|=> " \
      "$work/junit-$mode$compact.out" | sed 's/^[[:space:]]*//' | head -40
  done
done
exit $status
