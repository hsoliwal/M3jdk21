#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
#
# PROVE_DONOR_UPSTREAM_FIXES v1 (2026-11-17): applies donor_upstream_fixes.py to CURRENT origin/master String.java,
# javac-checks it with the M3 closure (layout-invariant/compile_closure.sh) and runs StringDonorUpstreamFixesTest
# in its three modes on the stock VM, first on unfixed master (discriminator, expected to fail) then fixed.
# usage: prove_donor_fixes.sh <lane-dir> <layout-lane-dir>   (appends to <lane-dir>/PROOF.txt)
set -u
U=$1; L=$2
J="C:/Program Files/Java/jdk-21/bin"
repo=/c/m3-superset/conv/m3jdk21
cd "$repo" && git fetch -q origin master
base=$(git rev-parse origin/master)
for f in String M3String; do
  git show "$base:src/java.base/share/classes/java/lang/$f.java" > "$U/closure/src/java/lang/$f.java"
done
cp "$U/closure/src/java/lang/String.java" "$U/String.master.java"
cd "$U"
python -I donor_upstream_fixes.py String.master.java edit/java/lang/String.java || exit 1
{
  echo; echo "PROVE_DONOR_UPSTREAM_FIXES v1 on origin/master $base"
  for side in unfixed fixed; do
    cp String.master.java closure/src/java/lang/String.java
    if [ $side = fixed ]; then over="$U/edit"; else over=""; fi
    echo "== javac closure ($side): $(bash "$L/compile_closure.sh" "$U/closure" "$L/seed.txt" $over | tail -1)"
    P="--patch-module java.base=$(cygpath -m "$U/closure/out")"
    rm -rf tout; mkdir tout
    MSYS_NO_PATHCONV=1 "$J/javac" $P -d tout StringDonorUpstreamFixesTest.java || exit 1
    for c in "small -Xmx256m" "unwrap -Xmx3800m" "turkish -Duser.language=tr -Duser.country=TR"; do
      set -- $c; mode=$1; shift
      echo "== $side $mode $*"
      MSYS_NO_PATHCONV=1 timeout 900 "$J/java" $P "$@" -cp "$(cygpath -m "$U/tout")" \
        StringDonorUpstreamFixesTest $mode 2>&1 | grep -E 'checks|RESULT|expected|instead|Exception' | head -6
    done
  done
} >> PROOF.txt 2>&1
cp String.master.java closure/src/java/lang/String.java
