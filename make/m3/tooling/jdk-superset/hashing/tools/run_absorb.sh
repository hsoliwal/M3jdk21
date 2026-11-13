#!/bin/sh
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# run_absorb.sh <repo root> <work dir> [<destination dir>]
# The zero-allocation-hashing absorption into java.base, end to end and re-runnable:
#   1. hash_jdk_form.py (HASH_JDK_FORM v2 rules + CHANGE_NOTICE v1) on a staged copy of the kept donor files
#   2. absorb_rename_generic.py (STRIP_JB v1 + RENAME v1): net.openhft.hashing -> jdk.internal.m3.hash, T -> M3T
#   3. literal_constants.py (LITERAL_CONSTANTS v1.1) on a copy: every magic literal -> the named constant of
#      CONSTANT_NAMES.tsv (javac folds them back: method bytecode unchanged); a second pass must make 0 edits
#   4. the result replaces <destination dir>/*.java (default <repo>/src/java.base/share/classes/jdk/internal/m3/hash)
# A second run over the same donor bytes must leave the repository unchanged (git diff --exit-code).
# <work dir>/renamed keeps the step 2 output (the literals before step 3) for the bytecode identity gate.
set -eu
abspath() { (cd "$1" && (pwd -W 2>/dev/null || pwd)); }
repo=$(abspath "$1")
mkdir -p "$2"
work=$(abspath "$2")
lane=$repo/m3/tooling/jdk-superset/hashing
donor=$repo/m3/vendor/third_party/zero-allocation-hashing/src/main/java/net/openhft/hashing
dst=${3:-$repo/src/java.base/share/classes/jdk/internal/m3/hash}
pkg=jdk/internal/m3/hash
python "$lane/tools/hash_jdk_form.py" "$donor" "$lane/TYPE_RENAME.tsv" "$work/stage/net/openhft/hashing" "$work/report"
rm -rf "$work/renamed" "$work/named"
ABSORB_PKGS="net.openhft.hashing=jdk.internal.m3.hash" \
    python "$lane/tools/absorb_rename_generic.py" "$work/stage" "$lane/TYPE_RENAME.tsv" "$work/renamed" "$work/report"
cp -r "$work/renamed" "$work/named"
lc="$lane/tools/literal_constants.py"
(cd "$work/renamed/$pkg" && python "$lc" inventory . "$work/report/LITERALS.tsv" M3*.java)
(cd "$work/named/$pkg" && python "$lc" apply . "$lane/CONSTANT_NAMES.tsv" "$work/report/CONSTANTS.tsv" M3*.java)
(cd "$work/named/$pkg" && python "$lc" check . "$lane/CONSTANT_NAMES.tsv" M3*.java) | grep -q '^EDITS.0.FILES.0$'
mkdir -p "$dst"
rm -f "$dst"/M3*.java
cp "$work/named/$pkg/"M3*.java "$dst/"
ls "$dst" | wc -l
