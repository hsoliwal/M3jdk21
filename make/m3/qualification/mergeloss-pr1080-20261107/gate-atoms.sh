#!/bin/bash
# Master's M3RegexDpPlan atom tests (JUnit) on a stock JDK 21 with java.base patched by master or merged classes.
cd "$(dirname "$0")"
J="C:/Program Files/Java/jdk-21/bin"; JU=S:/m3-scratch/m3jdk-release-audit/junit-platform-console-standalone-1.12.2.jar
for side in master merged; do
  P=../gate/p_$side; rm -rf cls_$side; mkdir -p cls_$side
  "$J/javac" --patch-module java.base=$P --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -cp "$JU" -d cls_$side *.java > javac_$side.log 2>&1 || { echo "$side javac FAIL"; head javac_$side.log; continue; }
  "$J/java" --patch-module java.base=$P --add-opens java.base/jdk.internal.mindex=ALL-UNNAMED --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED \
    -jar "$JU" execute -cp cls_$side --select-class M3RegexDpPlanAtomTest --details=summary --disable-banner > run_$side.log 2>&1
  echo "$side rc=$? $(grep -E 'tests (successful|failed)' run_$side.log | tr -s ' ' | tr '\n' ' ')"
done
