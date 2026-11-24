#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
set -euo pipefail
if [[ $# -ne 3 ]]; then
  echo 'usage: test-provider-junit-loader-repro.sh /absolute/JDK21 /absolute/junit-console.jar /fresh/out' >&2
  exit 64
fi
jdk="$1"; junit="$2"; out="$3"
if [[ "$jdk" != /* || "$junit" != /* || "$out" != /* ||
      ! -x "$jdk/bin/java" || ! -x "$jdk/bin/javac" ||
      ! -s "$junit" || -L "$junit" ]]; then
  echo 'required source-selected JDK21 and JUnit jar must be absolute, real paths' >&2
  exit 65
fi
if [[ -e "$out" || -L "$out" ]]; then
  echo 'refusing existing fixture output directory' >&2
  exit 66
fi
mkdir -p "$out/src/javax/xml/parsers" "$out/classes" "$out/reports"
fixture="$out/src/javax/xml/parsers/JunitModuleDiscoveryFixture.java"
{
  printf 'package javax.xml.parsers;\n'
  printf 'import org.junit.jupiter.api.Test;\n'
  printf 'import static org.junit.jupiter.api.Assertions.assertEquals;\n'
  for n in $(seq -w 1 15); do
    printf 'class SyntheticProvider%sTest { @Test void test() { assertEquals(42, 6 * 7); } }\n' "$n"
  done
} > "$fixture"
"$jdk/bin/javac" -source 21 -target 21 -proc:none -implicit:none -Xlint:all,-module -Werror \
  --patch-module "java.xml=$out/src" --add-reads java.xml=ALL-UNNAMED \
  -cp "$junit" -d "$out/classes" "$fixture"
sha256sum "$junit" "$fixture" > "$out/inputs-sha256.txt"
selectors=()
for n in $(seq -w 1 15); do
  selectors+=(--select-class "javax.xml.parsers.SyntheticProvider${n}Test")
done
runtime=(--add-modules java.xml --patch-module "java.xml=$out/classes" \
  --add-reads java.xml=ALL-UNNAMED \
  --add-opens java.xml/javax.xml.parsers=ALL-UNNAMED)
set +e
"$jdk/bin/java" -ea -Xmx256m "${runtime[@]}" -cp "$junit" \
  org.junit.platform.console.ConsoleLauncher execute "${selectors[@]}" \
  --include-engine junit-jupiter --disable-banner --details=summary --fail-if-no-tests \
  > "$out/original.log" 2>&1
original_rc=$?
set -e
if [[ "$original_rc" -ne 2 ]] ||
    ! grep -Eq '^\[[[:space:]]*0 tests found' "$out/original.log"; then
  echo 'original application-classpath-only reproduction did not refuse zero discovery' >&2
  cat "$out/original.log" >&2
  exit 67
fi
"$jdk/bin/java" "-Xbootclasspath/a:$junit" -ea -Xmx256m "${runtime[@]}" -cp "$junit" \
  org.junit.platform.console.ConsoleLauncher execute "${selectors[@]}" \
  --include-engine junit-jupiter --disable-banner --details=summary \
  --reports-dir "$out/reports" --fail-if-no-tests \
  | tee "$out/candidate.log"
if ! grep -Eq '^\[[[:space:]]*15 tests successful' "$out/candidate.log" ||
    ! grep -Eq '^\[[[:space:]]*0 tests failed' "$out/candidate.log"; then
  echo 'bootstrap-visible JUnit did not execute all 15 fixture classes' >&2
  exit 68
fi
report="$out/reports/TEST-junit-jupiter.xml"
if [[ ! -s "$report" || -L "$report" ]] ||
    ! grep -Fq '<testsuite name="JUnit Jupiter" tests="15" skipped="0" failures="0" errors="0"' "$report"; then
  echo 'exact Jupiter report missing or not 15/15 successful' >&2
  exit 69
fi
for n in $(seq -w 1 15); do
  grep -Fq "classname=\"javax.xml.parsers.SyntheticProvider${n}Test\"" "$report" || {
    echo "source-visible Jupiter class ${n} was not executed" >&2
    exit 70
  }
done
printf 'M3_PROVIDER_JUNIT_LOADER_REPRO_PASS originalTests=0 candidateTests=15\n' \
  | tee "$out/status.txt"
