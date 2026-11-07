#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Doclint gate of the module: javac --release 21 -Xdoclint:all/protected over every main source of the module (the
# pom compiles with -Xlint:all -Werror only, so no other gate reads the documentation). Prints the error and warning
# counts with the error kinds and DOCLINT PASS|FAIL; the gate fails on any error (a dangling {@link} or a malformed
# tag); missing-documentation warnings are counted, not gated (relocated javadoc keeps the donor's text).
# usage: doclint.sh <scratch-dir>   (JAVA_HOME = JDK 21)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) winpath() { (cd "$1" && pwd -W); } ;; *) winpath() { (cd "$1" && pwd); } ;; esac
MODULE=$(winpath "$(dirname "$0")/../..")
mkdir -p "$1/doclint-classes"
SCRATCH=$(winpath "$1")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
(cd "$MODULE" && find src/main/java -name '*.java' | sort) > "$SCRATCH/doclint-sources.txt"
(cd "$MODULE" && "$JAVA_BIN/javac" -J-Djava.io.tmpdir="$SCRATCH" --release 21 -encoding UTF-8 -proc:none \
    -Xdoclint:all/protected -Xmaxerrs 100000 -Xmaxwarns 100000 -d "$SCRATCH/doclint-classes" \
    @"$SCRATCH/doclint-sources.txt") > "$SCRATCH/doclint.log" 2>&1 || true
errors=$(grep -cE ': error: ' "$SCRATCH/doclint.log" || true)
warnings=$(grep -cE ': warning: ' "$SCRATCH/doclint.log" || true)
echo "DOCLINT sources=$(wc -l < "$SCRATCH/doclint-sources.txt") errors=${errors:-0} warnings=${warnings:-0}"
grep -E ': error: ' "$SCRATCH/doclint.log" | sed -E 's/.*error: //' | sort | uniq -c || true
if [ "${errors:-0}" -eq 0 ]; then echo "DOCLINT PASS"; else echo "DOCLINT FAIL"; exit 1; fi
