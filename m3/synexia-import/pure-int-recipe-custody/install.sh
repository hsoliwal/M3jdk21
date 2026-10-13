#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail

ROOT="${1:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)}"
MODULE="$ROOT/m3/synexia-import/pure-int-recipe-custody"
VENDOR="$ROOT/m3/vendor/third_party/pom.xml"

[[ -s "$VENDOR" && -s "$MODULE/pom.xml" ]] || {
  echo "M3 source-built custody inputs are missing" >&2
  exit 1
}

# Install the complete pinned source-built test/tooling closure before invoking
# Surefire. -am includes dependency modules; there is no Maven Central substitute
# for private *-m3-source-1 coordinates. The launcher is required by Surefire.
mvn -B -ntp -f "$VENDOR" \
  -pl rewrite-java-21,rewrite-test,junit-jupiter-5.10.2,junit-platform-launcher-1.10.2 \
  -am -DskipTests install

LAUNCHER="$HOME/.m2/repository/org/junit/platform/junit-platform-launcher/1.10.2-m3-source-1/junit-platform-launcher-1.10.2-m3-source-1.jar"
[[ -s "$LAUNCHER" ]] || {
  echo "pinned source-built JUnit platform launcher was not installed" >&2
  exit 1
}

mvn -B -ntp -f "$MODULE/pom.xml" clean verify install

JAR="$HOME/.m2/repository/com/synexia/synexia-openrewrite-recipes/1.0.0-SNAPSHOT/synexia-openrewrite-recipes-1.0.0-SNAPSHOT.jar"
[[ -s "$JAR" ]] || {
  echo "public Synexia pure-int custody jar not installed" >&2
  exit 1
}

printf 'M3JDK21_SYNEXIA_PURE_INT_CUSTODY_INSTALLED\t%s\n' "$JAR"
