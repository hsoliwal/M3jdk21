#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gates of recipe PRECOMPUTE_JOINED_LANES v1 in GATES.tsv order: recipe fixed point and chain replay, m3/fs verify
# (native build + ctest + JUnit), the stage differentials, the native memchr gate (once stage 02 is in EDITS.tsv) and
# the JDK naming fixed point over m3/fs. Linux (or WSL): the content store needs directory fsync, the module builds a
# Linux JNI library. MVN_ARGS passes extra Maven flags (for example -Dm3.fs.native.prefix=..., -Dcmake.executable=...,
# -Dmaven.repo.local=...; -Dmaven.test.failure.ignore=true when the baseline has known red tests, see GATES.tsv).
# usage: verify.sh <scratch-dir>     env: JAVA_HOME (a JDK 21), PYTHON (default python3), MVN_ARGS
set -euo pipefail
R=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$R/../../../.." && pwd)
W=$1
PY=${PYTHON:-python3}
mkdir -p "$W"
"$PY" -I "$R/apply.py" --root "$ROOT" --check
"$PY" -I "$R/apply.py" --root "$ROOT" --replay
# shellcheck disable=SC2086
mvn -B -ntp ${MVN_ARGS:-} -f "$ROOT/m3/fs/pom.xml" verify | tee "$W/mvn-verify.log" | grep -E "Tests run:|BUILD|ERROR" | tail -n 5
bash "$R/qualification/differential.sh" "$ROOT/m3/fs/target/classes" "$W/differential"
if grep -q "^m3/fs/native/src/m3_fs_jni.c" "$R/EDITS.tsv"; then bash "$R/qualification/native-gate.sh" "$W/native"; fi
"$PY" -I "$ROOT/make/m3/tooling/jdk-naming/jdk_naming.py" plan --root "$ROOT" --only m3/fs/
echo PRECOMPUTE_JOINED_LANES_GATES_DONE
