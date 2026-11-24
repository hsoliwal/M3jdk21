#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# Source-only Java 21 reproducer for JUnit annotations hidden from bootstrap modules.
set -euo pipefail
if [[ $# -ne 2 ]]; then
  echo 'usage: test-provider-junit-discovery-bootstrap.sh /absolute/java21home /fresh/output' >&2
  exit 64
fi
jdk="$1"
out="$2"
repo="${M3_PROVIDER_PROBE_SOURCE_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)}"
if [[ ! -x "$jdk/bin/java" || ! -x "$jdk/bin/javac" || ! -x "$jdk/bin/jar" ]]; then
  echo 'missing installed Java 21 test tools' >&2
  exit 65
fi
if [[ -e "$out" || -L "$out" ]]; then
  echo 'fresh synthetic proof output required' >&2
  exit 66
fi
mkdir -p "$out/source/org/junit/jupiter/api" "$out/source/java/m3/tests" \
  "$out/api-classes" "$out/patch-classes" "$out/probe-classes"
cat > "$out/source/org/junit/jupiter/api/Test.java" <<'JAVA'
package org.junit.jupiter.api;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Test { }
JAVA
cat > "$out/source/java/m3/tests/PatchedTest.java" <<'JAVA'
package java.m3.tests;
import org.junit.jupiter.api.Test;
public final class PatchedTest {
    @Test public void sample() { }
}
JAVA
"$jdk/bin/javac" --release 21 -Xlint:all -Werror \
  -d "$out/api-classes" "$out/source/org/junit/jupiter/api/Test.java"
"$jdk/bin/jar" --create --file "$out/jupiter-test-stub.jar" -C "$out/api-classes" .
"$jdk/bin/javac" --patch-module "java.base=$out/source/java" \
  --add-reads java.base=ALL-UNNAMED -cp "$out/jupiter-test-stub.jar" \
  -d "$out/patch-classes" "$out/source/java/m3/tests/PatchedTest.java"
"$jdk/bin/javac" --release 21 -Xlint:all -Werror \
  -d "$out/probe-classes" "$repo/make/m3/collections/ProviderJUnitDiscoveryProbe.java"
vm=(--patch-module "java.base=$out/patch-classes"
    --add-reads java.base=ALL-UNNAMED
    --add-opens java.base/java.m3.tests=ALL-UNNAMED)
set +e
"$jdk/bin/java" "${vm[@]}" -cp "$out/jupiter-test-stub.jar:$out/probe-classes" \
  ProviderJUnitDiscoveryProbe java.base 1 java.m3.tests.PatchedTest \
  >"$out/negative.log" 2>&1
negative_status=$?
set -e
if [[ $negative_status -eq 0 ]] || \
    ! grep -Fq 'No runtime-visible Jupiter methods: java.m3.tests.PatchedTest' \
       "$out/negative.log"; then
  echo 'expected classpath-only JUnit annotation visibility failure' >&2
  cat "$out/negative.log" >&2
  exit 67
fi
"$jdk/bin/java" "-Xbootclasspath/a:$out/jupiter-test-stub.jar" "${vm[@]}" \
  -cp "$out/jupiter-test-stub.jar:$out/probe-classes" \
  ProviderJUnitDiscoveryProbe java.base 1 java.m3.tests.PatchedTest \
  | tee "$out/positive.log"
grep -Fx 'M3_PROVIDER_JUNIT_ANNOTATIONS_VISIBLE module=java.base classes=1 methods=1' \
  "$out/positive.log"
printf 'M3_PROVIDER_JUNIT_BOOTSTRAP_DISCOVERY_PASS negative=1 positive=1\n' \
  | tee "$out/status.txt"
