#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
# Run from any directory using bash; no external test libraries or replacement owner stubs.
# M3_PROOF_JAVA_HOME=/path/to/jdk-21 bash path/to/run-proof.sh [--baseline]
# --baseline expects original source-digest work and is for the exact preimage checkout.
set -euo pipefail
proof_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
proof_repo=${M3_PROOF_REPO:-$(cd -- "$proof_dir/../../../../.." && pwd)}
if [[ -n ${M3_PROOF_JAVA_HOME:-} ]]; then
    proof_javac="$M3_PROOF_JAVA_HOME/bin/javac"
    proof_java="$M3_PROOF_JAVA_HOME/bin/java"
else
    proof_javac=$(command -v javac)
    proof_java=$(command -v java)
fi
proof_out=$(mktemp -d "${TMPDIR:-/tmp}/m3-source-facts-proof.XXXXXXXX")
trap 'rm -rf -- "$proof_out"' EXIT
proof_sourcepath="$proof_repo/m3/ports/compiletime/src/main/java:$proof_repo/m3/vendor/synexia/synexia-mindex/compiler/src/main/java:$proof_repo/m3/vendor/synexia/synexia-mindex/runtime/src/main/java"
"$proof_javac" --release 21 -Xlint:all -Werror -d "$proof_out" \
    -sourcepath "$proof_sourcepath" \
    "$proof_repo/m3/ports/compiletime/src/test/java/com/m3/compiletime/ast/M3SourceFactsReuseProof.java"
"$proof_java" -ea -cp "$proof_out" com.m3.compiletime.ast.M3SourceFactsReuseProof "$@"
