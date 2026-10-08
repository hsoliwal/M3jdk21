#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail

ROOT="${1:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)}"
MODULE="$ROOT/m3/synexia-import/pure-int-recipe-custody"

mvn -B -ntp -f "$MODULE/pom.xml" clean verify install

JAR="$HOME/.m2/repository/com/synexia/synexia-openrewrite-recipes/1.0.0-SNAPSHOT/synexia-openrewrite-recipes-1.0.0-SNAPSHOT.jar"
[[ -s "$JAR" ]] || {
  echo "public Synexia pure-int custody jar not installed" >&2
  exit 1
}

printf 'M3JDK21_SYNEXIA_PURE_INT_CUSTODY_INSTALLED\t%s\n' "$JAR"
