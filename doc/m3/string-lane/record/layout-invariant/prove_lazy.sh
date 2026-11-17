#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
#
# PROVE_LAYOUT_LAZY v1 (2026-11-17): applies STRING_LAYOUT_LAZY v1 and VM_STRING_LAYOUT_LAZY v1 to CURRENT
# origin/master, javac-checks String.java in its M3 closure, and runs StringLayoutInvariantTest on the stock VM:
# first on master's java.base (eager fill, the discriminator: expected to FAIL the lazy checks), then with the recipe.
# usage: prove_lazy.sh <lane-dir>   (writes <lane-dir>/LAZY_PROOF.txt, edit-lazy/, vm-lazy-out/)
set -u
L=$1
J="C:/Program Files/Java/jdk-21/bin"
repo=/c/m3-superset/conv/m3jdk21
exec > "$L/LAZY_PROOF.txt" 2>&1
cd "$repo" && git fetch -q origin master
base=$(git rev-parse origin/master)
echo "PROVE_LAYOUT_LAZY v1 on origin/master $base"
for f in String M3String; do git show "$base:src/java.base/share/classes/java/lang/$f.java" > "$L/master/src/java/lang/$f.java"; done
mkdir -p "$L/vm-lazy-in" "$L/vm-lazy-out" "$L/edit-lazy/java/lang"
git show "$base:src/hotspot/share/classfile/javaClasses.cpp" > "$L/vm-lazy-in/javaClasses.cpp"
git show "$base:src/hotspot/share/opto/c2_globals.hpp" > "$L/vm-lazy-in/c2_globals.hpp"
git show "$base:src/java.base/share/man/java.1" > "$L/vm-lazy-in/java.1"
cd "$L"
cp master/src/java/lang/String.java String.lazy-base.java
python -I string_layout_lazy.py String.lazy-base.java edit-lazy/java/lang/String.java || exit 1
python -I vm_layout_lazy.py vm-lazy-in vm-lazy-out || exit 1
python -I ascii_escape.py test/StringLayoutInvariantTest.java
rm -rf tout; mkdir -p tout
MSYS_NO_PATHCONV=1 "$J/javac" -d tout test/StringLayoutInvariantTest.java || exit 1
run() {
  echo "== $1: $2"
  MSYS_NO_PATHCONV=1 timeout 1200 "$J/java" --patch-module "java.base=$(cygpath -m "$L/master/out")" \
    --add-opens java.base/java.lang=ALL-UNNAMED $2 -cp "$(cygpath -m "$L/tout")" StringLayoutInvariantTest 2>&1 | tail -5
}
echo "== javac closure, master (eager): $(bash compile_closure.sh "$L/master" "$L/seed.txt" | tail -1)"
run "DISCRIMINATOR master java.base (eager fill)" ""
echo "== javac closure, lazy recipe: $(bash compile_closure.sh "$L/master" "$L/seed.txt" "$L/edit-lazy" | tail -1)"
for flags in "" "-XX:-CompactStrings" "-Xcomp -XX:-TieredCompilation" \
             "-Xcomp -XX:-TieredCompilation -XX:+OptimizeStringConcat"; do
  run "lazy java.base" "$flags"
done
cp String.lazy-base.java master/src/java/lang/String.java
rm -rf master/out tout
echo "PROVE_LAYOUT_LAZY done"
