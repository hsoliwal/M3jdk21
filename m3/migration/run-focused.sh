#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${M3_JDK:?Set M3_JDK to a JDK 21 directory}"
python3 m3/migration/validate_manifest.py
python3 m3/migration/test_validate_manifest.py
python3 m3/migration/generate_coverage.py
git diff --exit-code -- m3/migration/COVERAGE.md
rm -rf m3/build/migration-route
mkdir -p m3/build/migration-route/classes m3/build/migration-route/tests
mapfile -t sources < <(find m3/core/src -name '*.java' | sort)
"$M3_JDK/bin/javac" --release 21 -Xlint:all,-module -Werror -d m3/build/migration-route/classes "${sources[@]}"
"$M3_JDK/bin/javac" --release 21 -Xlint:all -Werror -cp m3/build/migration-route/classes -d m3/build/migration-route/tests m3/core/test/M3TextRouteTest.java
"$M3_JDK/bin/java" -ea -cp m3/build/migration-route/classes:m3/build/migration-route/tests M3TextRouteTest
