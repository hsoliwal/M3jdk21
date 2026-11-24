#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# Prove that platform-loaded module tests cannot see application-only JUnit annotations.
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo 'usage: test-provider-junit-loader.sh /absolute/JDK21' >&2
  exit 64
fi
jdk="$1"
if [[ ! -x "$jdk/bin/java" || ! -x "$jdk/bin/javac" || ! -x "$jdk/bin/jar" ]]; then
  echo 'A complete Java 21 image is required' >&2
  exit 65
fi
root="$(cd "$(dirname "$0")" && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p "$work"/{junit-src/org/junit/jupiter/api,stub-classes,tests-src/javax/sql,module-tests,probe}
cat > "$work/junit-src/org/junit/jupiter/api/Test.java" <<'JAVA'
package org.junit.jupiter.api;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Test { }
JAVA
cat > "$work/tests-src/javax/sql/ProviderAnnotationTest.java" <<'JAVA'
package javax.sql;
import org.junit.jupiter.api.Test;
class ProviderAnnotationTest {
    @Test void moduleAnnotation() { }
}
JAVA
"$jdk/bin/javac" --release 21 -Xlint:all -Werror \
  -d "$work/stub-classes" "$work/junit-src/org/junit/jupiter/api/Test.java"
"$jdk/bin/jar" --create --file "$work/junit.jar" -C "$work/stub-classes" .
"$jdk/bin/javac" -source 21 -target 21 -proc:none -Xlint:all -Werror \
  --patch-module "java.sql=$work/tests-src" \
  --add-reads java.sql=ALL-UNNAMED -cp "$work/junit.jar" \
  -d "$work/module-tests" "$work/tests-src/javax/sql/ProviderAnnotationTest.java"
"$jdk/bin/javac" --release 21 -Xlint:all -Werror \
  -d "$work/probe" "$root/M3ProviderJUnitDiscoveryProbe.java"
opts=(--add-modules java.sql --patch-module "java.sql=$work/module-tests" \
  --add-reads java.sql=ALL-UNNAMED --add-opens java.sql/javax.sql=ALL-UNNAMED \
  -cp "$work/probe:$work/junit.jar" M3ProviderJUnitDiscoveryProbe \
  javax.sql.ProviderAnnotationTest)
if "$jdk/bin/java" "${opts[@]}" >"$work/negative.log" 2>&1; then
  echo 'BUG: application-only @Test was incorrectly visible to a platform loader' >&2
  exit 70
fi
if grep -Fq M3_PROVIDER_JUNIT_DISCOVERY_PASS "$work/negative.log"; then
  echo 'BUG: negative discovery emitted a positive test receipt' >&2
  exit 71
fi
"$jdk/bin/java" -Xbootclasspath/a:"$work/junit.jar" "${opts[@]}" \
  | tee "$work/positive.log"
grep -Fx 'M3_PROVIDER_JUNIT_DISCOVERY_PASS classes=1 methods=1' \
  "$work/positive.log"
echo 'M3_PROVIDER_JUNIT_LOADER_SELFTEST_PASS negative=refused positive=1'
