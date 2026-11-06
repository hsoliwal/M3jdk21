#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# Runs every gate of leaf jbcoll-collectionviews-20261106 (census L001) and prints one GATE line per gate (GATES.tsv):
#   recipe        apply.py --check (pinned owner blob, 5 rules, crate postimage blobs at the donor pin): changed=0
#   patch_compile javac -g -Xlint:all -Werror -Xlint:-this-escape -XDstringConcat=inline --patch-module java.base=<dir>
#                 of the owner, of the census-hunk counterfactual (R1+R2) and of the two crate postimages: 0 warnings
#   format_equal  owner vs owner+R3+R4+R5, javac -g:none: every class file byte-identical
#   differential  CowViewsDifferential (seed, 1,000,000 ops, 27 subjects): stock JDK model vs patched owner,
#                 every differing line classified (classify.py, 0 UNCLASSIFIED); counterfactual and crate postimages
#                 classified against the owner / the model (information, same rule)
#   tests         the 13 jtreg tests of test/jdk/java/util/CollectionViews as plain mains (tests.sh): owner 13/13;
#                 counterfactual must fail FilteredBulkMutationContractTest only (the owner's subclass contract)
#   naming        jdk_naming.py plan over the leaf dir: 0 moves, 0 rewrites, 0 refusals (stage new files first)
#   scope         git diff --name-only <base> + staged + untracked: only the leaf dir
# usage: JAVA_HOME=<JDK 21> verify.sh <donor-git-dir> <donor-rev> <scratch-dir> [ops] [base-rev]
set -uo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) winpath() { (cd "$1" && pwd -W); } ;; *) winpath() { (cd "$1" && pwd); } ;; esac
Q=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$Q/../../.." && pwd)
DONOR=$1; REV=$2; mkdir -p "$3"; S=$(winpath "$3"); OPS=${4:-1000000}; BASE=${5:-origin/master}
J="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
SEED=0x4d334a42434f5731
LEAF=m3/qualification/$(basename "$Q")/
OWNER=src/java.base/share/classes/java/util/CollectionViews.java
FLAGS=(-g -Xlint:all -Werror -Xlint:-this-escape -XDstringConcat=inline)
status=PASS
gate() { local name=$1 verdict=$2; shift 2; echo "GATE $name $verdict $*"; [ "$verdict" = PASS ] || status=FAIL; }

out=$(python -I "$Q/apply.py" --check --donor-git "$DONOR" --donor-rev "$REV" 2>&1 | tail -1)
case "$out" in *"apply=0 superseded=2 format_equivalent=3 donor_blobs=2 changed=0 plan=UNCHANGED"*) gate recipe PASS "$out" ;;
    *) gate recipe FAIL "$out" ;; esac

# sides: owner (the tree), whatif (owner + census R1,R2), fmt (owner + R3,R4,R5), crown / ccow (crate postimage blobs)
rm -rf "$S/side"; mkdir -p "$S/side/owner/src/java/util"
cp "$ROOT/$OWNER" "$S/side/owner/src/java/util/"
python -I "$Q/apply.py" --whatif "$S/side/whatif/src/java/util/CollectionViews.java" --rules R1,R2 > /dev/null
python -I "$Q/apply.py" --whatif "$S/side/fmt/src/java/util/CollectionViews.java" --rules R3,R4,R5 > /dev/null
mkdir -p "$S/side/crown/src/java/util" "$S/side/ccow/src/java/util"
git -C "$DONOR" cat-file blob 540ebf535672a27a90f2a547f2d21aeebebe88aa > "$S/side/crown/src/java/util/CollectionViews.java"
git -C "$DONOR" cat-file blob 3252ddc179bcde807785b13ef24dc6b3b6c3ce4a > "$S/side/ccow/src/java/util/CollectionViews.java"
summary=""; v=PASS
for side in owner whatif crown ccow; do
    d="$S/side/$side"
    "$J/javac" "${FLAGS[@]}" --patch-module "java.base=$d/src" -d "$d/cls" "$d/src/java/util/CollectionViews.java" > "$d/javac.log" 2>&1
    rc=$?; w=$(grep -c 'warning:' "$d/javac.log"); [ $rc = 0 ] && [ "$w" = 0 ] || v=FAIL
    summary="$summary $side:rc=$rc,warnings=$w,classes=$(find "$d/cls" -name '*.class' | wc -l)"
done
gate patch_compile $v "$summary"

for side in owner fmt; do
    d="$S/side/$side"; rm -rf "$d/g0"
    "$J/javac" -g:none -Xlint:all -Werror -Xlint:-this-escape -XDstringConcat=inline --patch-module "java.base=$d/src" \
        -d "$d/g0" "$d/src/java/util/CollectionViews.java" > "$d/g0.log" 2>&1
    (cd "$d/g0" && find . -name '*.class' | sort | xargs sha256sum) > "$d/g0.sha"
done
if cmp -s "$S/side/owner/g0.sha" "$S/side/fmt/g0.sha"; then
    gate format_equal PASS "classes=$(wc -l < "$S/side/owner/g0.sha") manifest_sha256=$(sha256sum < "$S/side/owner/g0.sha" | cut -c1-16)"
else gate format_equal FAIL "$(diff "$S/side/owner/g0.sha" "$S/side/fmt/g0.sha" | head -3 | tr '\n' ' ')"; fi

rm -rf "$S/drv" "$S/runs"; mkdir -p "$S/runs"
"$J/javac" -Xlint:all -Werror --patch-module "java.base=$S/side/owner/cls" -d "$S/drv" "$Q/CowViewsDifferential.java" > "$S/drv.log" 2>&1
drc=$?
timeout 120 "$J/java" -Xmx1g -cp "$S/drv" CowViewsDifferential oracle $SEED "$OPS" "$S/runs/oracle.txt" > "$S/runs/oracle.out" 2>&1
for side in owner whatif crown ccow; do
    timeout 120 "$J/java" -Xmx1g --patch-module "java.base=$S/side/$side/cls" -cp "$S/drv" CowViewsDifferential views $SEED "$OPS" \
        "$S/runs/$side.txt" > "$S/runs/$side.out" 2>&1
done
: > "$S/runs/CLASSES.tsv"
c1=$(python -I "$Q/classify.py" "$S/runs/oracle.txt" "$S/runs/owner.txt" stock_vs_owner --tsv "$S/runs/CLASSES.tsv" | tail -1); r1=$?
c2=$(python -I "$Q/classify.py" "$S/runs/owner.txt" "$S/runs/whatif.txt" owner_vs_census_hunk --tsv "$S/runs/CLASSES.tsv" | tail -1)
c3=$(python -I "$Q/classify.py" "$S/runs/oracle.txt" "$S/runs/crown.txt" stock_vs_crate_current_owner --tsv "$S/runs/CLASSES.tsv" | tail -1)
c4=$(python -I "$Q/classify.py" "$S/runs/oracle.txt" "$S/runs/ccow.txt" stock_vs_crate_onepass_cow --tsv "$S/runs/CLASSES.tsv" | tail -1)
d=$(for f in oracle owner whatif crown ccow; do grep -o 'sha256=[0-9a-f]*' "$S/runs/$f.out" | sed "s/^/$f:/" | cut -c1-30; done | tr '\n' ' ')
v=FAIL; case "$c1$c2$c3$c4" in *"unclassified=0"*"unclassified=0"*"unclassified=0"*"unclassified=0"*) [ $drc = 0 ] && v=PASS ;; esac
gate differential $v "driver_javac rc=$drc; $d; $c1; $c2; $c3; $c4"

T=$(ls "$ROOT"/test/jdk/java/util/CollectionViews/*.java)
a=$(bash "$Q/tests.sh" "$S/side/owner/cls" "$S/tests-owner" $T | tail -1)
b=$(bash "$Q/tests.sh" "$S/side/whatif/cls" "$S/tests-whatif" $T | grep -E '^TEST .* FAIL' | awk '{print $2}' | tr '\n' ' ')
v=FAIL; [ "$a" = "TESTS pass=13 fail=0" ] && [ "$b" = "FilteredBulkMutationContractTest " ] && v=PASS
gate tests $v "owner: $a; census-hunk counterfactual fails: ${b:-none}"

naming=$(cd "$ROOT" && python -I m3/tooling/jdk-naming/jdk_naming.py plan --root . --only "$LEAF" 2>&1 | grep -E "^(JDK_NAMING|REFUSED)" | tail -1)
case "$naming" in *"moves=0 rewrites=0 refusals=0"*) gate naming PASS "$naming" ;; *) gate naming FAIL "$naming" ;; esac

changed=$(cd "$ROOT" && { git diff --name-only "$BASE"; git diff --name-only --cached; git ls-files --others --exclude-standard; } | sort -u)
outside=$(printf '%s\n' "$changed" | grep -v -e "^$LEAF" | grep -c .)
v=FAIL; [ "${outside:-0}" = 0 ] && v=PASS
gate scope $v "paths=$(printf '%s\n' "$changed" | grep -c .) outside=${outside:-0} base=$BASE"
echo "VERIFY $status"
[ "$status" = PASS ]
