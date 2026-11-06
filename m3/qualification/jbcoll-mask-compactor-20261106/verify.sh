#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# Runs every gate of leaf jbcoll-mask-compactor-20261106 (census L003, classification only) and prints one GATE line
# per gate (GATES.tsv):
#   lineage       lineage.py --check: pinned blobs, owner history, 3-way merges; tables unchanged; the only CLEAN
#                 merge is ours=live base=postimage theirs=postimage (result = ours: no edit)
#   patch_compile javac -g -Xlint:all -Werror -Xlint:-this-escape -XDstringConcat=inline --patch-module java.base=<dir>
#                 of the live compactor with master ArrayList and Vector (its callers), and of the postimage: 0 warnings
#   differential  CompactorDifferential diff (seed, 1,000,000 cases): live class (patched java.base) vs renamed copies
#                 of live (control), postimage, pr1754, pr1873, pr1908: 0 mismatches; a scalar-path mutant: > 0
#   tests         the owner's jtreg tests (test/jdk/java/util/ArrayList/M3*: compactor, removeIf, GC barrier and
#                 mutation contracts, mask differential) as plain mains on the live patch: all pass
#   (INFO)        the same tests with the postimage compactor; CompactorDifferential probe (live maskStatistics value,
#                 the dispatch it selects, median ns live vs postimage on fragmented 4096-element masks)
#   naming        jdk_naming.py plan over the leaf dir: 0 moves, 0 rewrites, 0 refusals (stage first)
#   scope         git diff --name-only <base> + staged + untracked: only the leaf dir
# usage: JAVA_HOME=<JDK 21> verify.sh <scratch-dir> [cases] [base-rev]
set -uo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
Q=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$Q/../../.." && pwd)
mkdir -p "$1"; S=$(winpath "$1"); CASES=${2:-1000000}; BASE=${3:-origin/master}
J="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
SEED=0x4d334a424d534b31
LEAF=m3/qualification/$(basename "$Q")/
U=src/java.base/share/classes/java/util
OWNER=src/java.base/share/classes/jdk/internal/mindex/M3CollectionMaskCompactor.java
FLAGS=(-g -Xlint:all -Werror -Xlint:-this-escape -XDstringConcat=inline)
EXPORT=(--add-exports java.base/jdk.internal.mindex=ALL-UNNAMED)
status=PASS
gate() { local name=$1 verdict=$2; shift 2; echo "GATE $name $verdict $*"; [ "$verdict" = PASS ] || status=FAIL; }

out=$(python -I "$Q/lineage.py" --check 2>&1)
v=FAIL; case "$out" in *"MERGE live postimage postimage CLEAN ours"*"clean=1 tables=UNCHANGED"*) v=PASS ;; esac
gate lineage $v "$(printf '%s\n' "$out" | tr '\n' ';')"

blob() { awk -F'\t' -v l="$1" '$1==l {print $2}' "$Q/LINEAGE.tsv"; }
rm -rf "$S/side" "$S/var"; mkdir -p "$S/side/live/src/java/util" "$S/side/live/src/jdk/internal/mindex" "$S/var/src/jbcoll"
cp "$ROOT/$U/ArrayList.java" "$ROOT/$U/Vector.java" "$S/side/live/src/java/util/"
cp "$ROOT/$OWNER" "$S/side/live/src/jdk/internal/mindex/"
mkdir -p "$S/side/post"; cp -r "$S/side/live/src" "$S/side/post/"
git -C "$ROOT" cat-file blob "$(blob postimage)" > "$S/side/post/src/jdk/internal/mindex/M3CollectionMaskCompactor.java"
summary=""; v=PASS
for side in live post; do
    d="$S/side/$side"
    "$J/javac" "${FLAGS[@]}" --patch-module "java.base=$d/src" -d "$d/cls" "$d/src/java/util/ArrayList.java" \
        "$d/src/java/util/Vector.java" "$d/src/jdk/internal/mindex/M3CollectionMaskCompactor.java" > "$d/javac.log" 2>&1
    rc=$?; w=$(grep -c 'warning:' "$d/javac.log"); [ $rc = 0 ] && [ "$w" = 0 ] || v=FAIL
    summary="$summary $side:rc=$rc,warnings=$w,classes=$(find "$d/cls" -name '*.class' | wc -l)"
done
gate patch_compile $v "$summary (ArrayList + Vector + M3CollectionMaskCompactor)"

rename() { sed "s/^package jdk.internal.mindex;/package jbcoll;/; s/\bM3CollectionMaskCompactor\b/MaskCompactor_$1/g"; }
rename livecopy < "$ROOT/$OWNER" > "$S/var/src/jbcoll/MaskCompactor_livecopy.java"
for l in postimage pr1754 pr1873 pr1908; do git -C "$ROOT" cat-file blob "$(blob $l)" | rename $l > "$S/var/src/jbcoll/MaskCompactor_$l.java"; done
rename mutant < "$ROOT/$OWNER" | sed 's/(1L << (read & 63))) == 0) {/(1L << (read \& 62))) == 0) {/' > "$S/var/src/jbcoll/MaskCompactor_mutant.java"
mutated=$(diff "$S/var/src/jbcoll/MaskCompactor_livecopy.java" "$S/var/src/jbcoll/MaskCompactor_mutant.java" | grep -c '^>')
"$J/javac" -Xlint:all -Werror -d "$S/var/cls" "$S"/var/src/jbcoll/*.java > "$S/var/javac.log" 2>&1; vrc=$?
"$J/javac" -Xlint:all -Werror --patch-module "java.base=$S/side/live/cls" "${EXPORT[@]}" -d "$S/drv" "$Q/CompactorDifferential.java" > "$S/drv.log" 2>&1; drc=$?
RUN=(--patch-module "java.base=$S/side/live/cls" "${EXPORT[@]}" -cp "$S/drv$SEP$S/var/cls")
d=$(timeout 120 "$J/java" -Xmx2g "${RUN[@]}" CompactorDifferential diff $SEED "$CASES" livecopy postimage pr1754 pr1873 pr1908 mutant 2>&1)
v=FAIL
case "$d" in *"livecopy mismatches=0 "*"postimage mismatches=0 "*"pr1754 mismatches=0 "*"pr1873 mismatches=0 "*"pr1908 mismatches=0 "*"mutant mismatches=0 "*) ;;
    *"livecopy mismatches=0 "*"postimage mismatches=0 "*"pr1754 mismatches=0 "*"pr1873 mismatches=0 "*"pr1908 mismatches=0 "*"mutant mismatches="[1-9]*) [ $vrc = 0 ] && [ $drc = 0 ] && v=PASS ;; esac
gate differential $v "variants_javac rc=$vrc driver_javac rc=$drc mutant_lines=$mutated; $d"

T=$(ls "$ROOT"/test/jdk/java/util/ArrayList/M3*.java)
bash "$Q/tests.sh" "$S/side/live/cls" "$S/tests-live" $T > "$S/tests-live.txt" 2>&1
bash "$Q/tests.sh" "$S/side/post/cls" "$S/tests-post" $T > "$S/tests-post.txt" 2>&1
a=$(tail -1 "$S/tests-live.txt"); b=$(tail -1 "$S/tests-post.txt")
v=FAIL; case "$a" in "TESTS pass="*" fail=0 skip=0") v=PASS ;; esac
gate tests $v "live: $a ($(grep '^TEST ' "$S/tests-live.txt" | awk '{print $2 "=" $3}' | tr '\n' ' '))"
echo "INFO tests_with_postimage_compactor $b"
p=$(timeout 120 "$J/java" "${RUN[@]}" --add-opens java.base/jdk.internal.mindex=ALL-UNNAMED CompactorDifferential probe postimage 2>&1 | tr '\n' ';')
echo "INFO probe $p"

naming=$(cd "$ROOT" && python -I m3/tooling/jdk-naming/jdk_naming.py plan --root . --only "$LEAF" 2>&1 | grep -E "^(JDK_NAMING|REFUSED)" | tail -1)
case "$naming" in *"moves=0 rewrites=0 refusals=0"*) gate naming PASS "$naming" ;; *) gate naming FAIL "$naming" ;; esac

changed=$(cd "$ROOT" && { git diff --name-only "$BASE"; git diff --name-only --cached; git ls-files --others --exclude-standard; } | sort -u)
outside=$(printf '%s\n' "$changed" | grep -v -e "^$LEAF" | grep -c .)
v=FAIL; [ "${outside:-0}" = 0 ] && v=PASS
gate scope $v "paths=$(printf '%s\n' "$changed" | grep -c .) outside=${outside:-0} base=$BASE"
echo "VERIFY $status"
[ "$status" = PASS ]
