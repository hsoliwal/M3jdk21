#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Native gate of recipe PRECOMPUTE_JOINED_LANES v1 stage 02 (Synexia crate m3fs-memchr-bitmap-20261102), Linux/WSL:
# the real m3_contains of master m3_fs_jni.c (baseline) and of the stage-02 image (candidate), both rebuilt from the
# pinned blobs by apply.py --replay --emit, are compiled into the relocated oracle (500,000 randomized + 66,564
# periodic cases + boundaries) as strict C17 under -fsanitize=undefined; old and new must agree on every case.
# usage: native-gate.sh <scratch-dir>      env: CC (default gcc), PYTHON (default python3)
set -euo pipefail
abs() { (cd "$1" && { pwd -W 2>/dev/null || pwd; }); }  # Windows-form path under Git Bash
Q=$(abs "$(dirname "$0")")
R=$(dirname "$Q")
ROOT=$(abs "$R/../../../..")
W=$1
rm -rf "$W" && mkdir -p "$W"
"${PYTHON:-python3}" -I "$R/apply.py" --root "$ROOT" --replay --through 02-memchr-bitmap --emit "$W/images" > "$W/replay.log"
extract() { # <m3_fs_jni.c> <name>: the static m3_contains definition, renamed
  awk '/^static int m3_contains\(/{f=1} f{print} f&&/^}$/{exit}' "$1" | sed "s/^static int m3_contains(/static int $2(/"
}
extract "$W/images/00-base/m3_fs_jni.c" baseline > "$W/m3_contains_baseline.inc"
extract "$W/images/02-memchr-bitmap/m3_fs_jni.c" candidate > "$W/m3_contains_candidate.inc"
grep -q memchr "$W/m3_contains_candidate.inc"
if grep -q memchr "$W/m3_contains_baseline.inc"; then echo "NATIVE GATE FAIL baseline already uses memchr"; exit 1; fi
${CC:-gcc} -std=c17 -O2 -Wall -Wextra -Werror -pedantic -fsanitize=undefined -fno-sanitize-recover=undefined \
    -I "$W" -o "$W/m3_contains_oracle" "$Q/m3_contains_oracle.c"
timeout 120 "$W/m3_contains_oracle"
echo "NATIVE GATE PASS cc=$(${CC:-gcc} -dumpfullversion)"
