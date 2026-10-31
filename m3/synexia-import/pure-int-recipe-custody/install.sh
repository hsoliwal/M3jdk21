#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail

ROOT="${1:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)}"
MODULE="$ROOT/m3/synexia-import/pure-int-recipe-custody"
VENDOR="$ROOT/m3/vendor/third_party/pom.xml"
MODE="${2:-custody}"
case "$MODE" in
  custody|--dependencies-only) ;;
  *)
    echo "usage: install.sh [repository-root] [custody|--dependencies-only]" >&2
    exit 2
    ;;
esac

[[ -s "$VENDOR" && ( "$MODE" == "--dependencies-only" || -s "$MODULE/pom.xml" ) ]] || {
  echo "M3 source-built custody inputs are missing" >&2
  exit 1
}

# Install the complete pinned source-built test/tooling closure before invoking
# Surefire. -am includes dependency modules; there is no Maven Central substitute
# for private *-m3-source-1 coordinates. The launcher is required by Surefire.
PROJECTS="rewrite-java-21,rewrite-test,junit-jupiter-5.10.2,junit-platform-launcher-1.10.2"
if [[ "$MODE" == "--dependencies-only" ]]; then
  # TQ explicitly consumes the source-built SLF4J 2 API in addition to the
  # OpenRewrite closure's retained SLF4J 1.7 dependency.
  PROJECTS+=",slf4j-api-2.0.9"
fi
mvn -B -ntp -f "$VENDOR" -pl "$PROJECTS" -am -DskipTests install

LAUNCHER="$HOME/.m2/repository/org/junit/platform/junit-platform-launcher/1.10.2-m3-source-1/junit-platform-launcher-1.10.2-m3-source-1.jar"
[[ -s "$LAUNCHER" ]] || {
  echo "pinned source-built JUnit platform launcher was not installed" >&2
  exit 1
}

if [[ "$MODE" == "--dependencies-only" ]]; then
  printf 'M3JDK21_SOURCE_DEPENDENCIES_INSTALLED\t%s\n' "$PROJECTS"
  exit 0
fi

mvn -B -ntp -f "$MODULE/pom.xml" clean verify install

JAR="$HOME/.m2/repository/com/synexia/synexia-openrewrite-recipes/1.0.0-SNAPSHOT/synexia-openrewrite-recipes-1.0.0-SNAPSHOT.jar"
[[ -s "$JAR" ]] || {
  echo "public Synexia pure-int custody jar not installed" >&2
  exit 1
}

printf 'M3JDK21_SYNEXIA_PURE_INT_CUSTODY_INSTALLED\t%s\n' "$JAR"
