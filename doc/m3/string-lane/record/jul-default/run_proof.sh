#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
#
# JUL_DEFAULT_PROOF v1 (2026-11-17). usage: run_proof.sh <repo-root> <work-dir>
# Builds a named module jdk.m3.search from the repository's relocated SLF4J API and JUL provider sources with the SLF4J
# part of its descriptor (module-info.search.java), once with the provides directive and once without it (the state
# before this change), and runs JulDefaultProof from the module m3.jul.proof on a stock JDK 21. Expected: "with" PASS,
# "without" FAIL (NOPLogger, no records, the "No SLF4J providers" warning). Harness files: drop the .txt suffix.
set -u
repo=$1; work=$2
here=$(cd "$(dirname "$0")" && { pwd -W 2>/dev/null || pwd; })
jh=${PROOF_JAVA_HOME:-C:/Program Files/Java/jdk-21}
export MSYS_NO_PATHCONV=1
src=$repo/src/jdk.m3.search/providers/classes
mkdir -p "$work/proof/m3/jul/proof"
cp "$here/JulDefaultProof.java" "$work/proof/m3/jul/proof/"
cp "$here/module-info.java" "$work/proof/"
for variant in with without; do
  gen=$work/gen-$variant; mod=$work/mod-$variant
  rm -rf "$gen" "$mod"; mkdir -p "$gen/jdk/internal/search" "$mod"
  cp -r "$src/jdk/internal/search/slf4j" "$gen/jdk/internal/search/"
  if [ $variant = with ]; then cp "$here/module-info.search.java" "$gen/module-info.java"
  else grep -v 'provides\|with jdk' "$here/module-info.search.java" > "$gen/module-info.java"; fi
  "$jh/bin/javac" -nowarn -d "$mod/jdk.m3.search" $(find "$gen" -name '*.java') || exit 3
  "$jh/bin/javac" -nowarn -d "$mod/m3.jul.proof" --module-path "$mod" $(find "$work/proof" -name '*.java') || exit 3
  echo "== $variant provides"
  "$jh/bin/java" --module-path "$mod" -m m3.jul.proof/m3.jul.proof.JulDefaultProof
  echo "exit $?"
done
