#!/bin/sh
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# run_absorb.sh <repo root> <work dir>
# The zero-allocation-hashing absorption into java.base, end to end and re-runnable:
#   1. hash_jdk_form.py (HASH_JDK_FORM v2 rules + CHANGE_NOTICE v1) on a staged copy of the kept donor files
#   2. absorb_rename_generic.py (STRIP_JB v1 + RENAME v1): net.openhft.hashing -> jdk.internal.m3.hash, T -> M3T
#   3. the result replaces <repo>/src/java.base/share/classes/jdk/internal/m3/hash/*.java
# A second run over the same donor bytes must leave the repository unchanged (git diff --exit-code).
set -eu
repo=$1
work=$2
lane=$repo/m3/tooling/jdk-superset/hashing
donor=$repo/m3/vendor/third_party/zero-allocation-hashing/src/main/java/net/openhft/hashing
dst=$repo/src/java.base/share/classes/jdk/internal/m3/hash
python "$lane/tools/hash_jdk_form.py" "$donor" "$lane/TYPE_RENAME.tsv" "$work/stage/net/openhft/hashing" "$work/report"
rm -rf "$work/renamed"
ABSORB_PKGS="net.openhft.hashing=jdk.internal.m3.hash" \
    python "$lane/tools/absorb_rename_generic.py" "$work/stage" "$lane/TYPE_RENAME.tsv" "$work/renamed" "$work/report"
mkdir -p "$dst"
rm -f "$dst"/M3*.java
cp "$work/renamed/jdk/internal/m3/hash/"M3*.java "$dst/"
ls "$dst" | wc -l
