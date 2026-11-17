#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
#
# PROVE_LAYOUT_INVARIANT v1 (2026-11-17): re-applies both layout recipes to CURRENT origin/master and proves them.
# usage: prove.sh <lane-dir>   (writes <lane-dir>/PROOF.txt and the edited files under <lane-dir>/edit, vm-out)
set -u
L=$1
J="C:/Program Files/Java/jdk-21/bin"
repo=/c/m3-superset/conv/m3jdk21
exec > "$L/PROOF.txt" 2>&1
cd "$repo" && git fetch -q origin master
base=$(git rev-parse origin/master)
echo "PROVE_LAYOUT_INVARIANT v1 on origin/master $base"
for f in String M3String; do git show "$base:src/java.base/share/classes/java/lang/$f.java" > "$L/$f.master.java"; done
mkdir -p "$L/vm-in" "$L/vm-out" "$L/edit/java/lang"
git show "$base:src/hotspot/share/classfile/javaClasses.hpp" > "$L/vm-in/javaClasses.hpp"
git show "$base:src/hotspot/share/classfile/javaClasses.cpp" > "$L/vm-in/javaClasses.cpp"
git show "$base:src/hotspot/share/opto/c2_globals.hpp" > "$L/vm-in/c2_globals.hpp"
git show "$base:src/java.base/share/man/java.1" > "$L/vm-in/java.1"
cd "$L"
python -I string_layout_invariant.py String.master.java edit/java/lang/String.java M3String.master.java \
  edit/java/lang/M3String.java || exit 1
python -I vm_layout_invariant.py vm-in vm-out || exit 1
echo "== javac closure (master String.java + M3 closure vs stock java.base, -Xlint:all,-this-escape -Werror)"
rm -rf master/src/java/lang/String.java master/src/java/lang/M3String.java
cp String.master.java master/src/java/lang/String.java; cp M3String.master.java master/src/java/lang/M3String.java
bash compile_closure.sh "$L/master" "$L/seed.txt" | tail -1
echo "-- before the recipe: exit above; after the recipe:"
bash compile_closure.sh "$L/master" "$L/seed.txt" "$L/edit" | tail -1
cp String.master.java master/src/java/lang/String.java; cp M3String.master.java master/src/java/lang/M3String.java
rm -rf tout; mkdir -p tout
MSYS_NO_PATHCONV=1 "$J/javac" -d tout test/StringLayoutInvariantTest.java || exit 1
run() {
  echo "== stock VM + edited java.base: $*"
  MSYS_NO_PATHCONV=1 timeout 1200 "$J/java" --patch-module "java.base=$(cygpath -m "$L/master/out")" \
    --add-opens java.base/java.lang=ALL-UNNAMED "$@" -cp tout StringLayoutInvariantTest 2>&1 | tail -12
}
run -Dlayout.vmSites=false
run -XX:-CompactStrings -Dlayout.vmSites=false
run -Xcomp -XX:-TieredCompilation -Dlayout.vmSites=false
run -Xcomp -XX:-TieredCompilation -XX:-OptimizeStringConcat -Dlayout.vmSites=false
run
echo "PROVE_LAYOUT_INVARIANT done"
