#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# e2_verify.sh <tag>: verify CURATE_E pass E2 applied in place in bcw (HEAD = base, working tree = curated).
#   1. reactor trees base_e2_<tag> (git archive HEAD) and cur_e2_<tag> (working tree) of m3/arrays (bridge),
#      src/jdk.m3.collections, m3/collections, m3/tooling/jdk-naming + aggregator pom;
#   2. mvn -o test in both (sequential); 3. javac main classes of both + curate_e_bytecode.py (CURATE_CORE=jdk.m3.collections);
#   4. bc_loose_parity.sh for every changed .java outside m3/collections and the arrays area (file vs base/cur classes).
# Writes E2_<tag>.txt.
set -uo pipefail
H=$(cd "$(dirname "$0")" && pwd); cd "$H"; T="$1"; R="E2_$T.txt"; : > "$R"
export JAVA_HOME="C:/Program Files/Java/jdk-21" PATH="/c/Program Files/Java/jdk-21/bin:$PATH"
say() { echo "$*" | tee -a "$R"; }
P="m3/arrays src/jdk.m3.collections m3/collections m3/tooling/jdk-naming"
say "base $(git -C bcw rev-parse --short=12 HEAD)"
for side in base cur; do d="${side}_e2_$T"; rm -rf "$d"; mkdir -p "$d"; cp cur2/pom.xml "$d/"
    if [ $side = base ]; then git -C bcw archive --format=tar HEAD $P | tar -x -C "$d"
    else (cd bcw && tar -cf - --exclude=target $P) | tar -x -C "$d"; fi
done
for side in base cur; do d="${side}_e2_$T"
    (cd "$d" && timeout 3300 mvn -o -B test > "../test_${side}_e2_$T.log" 2>&1)
    say "tests $side: $(grep -E 'Tests run: [0-9]+, Failures' "test_${side}_e2_$T.log" | tail -1 | sed -E 's/\[[A-Z]+\] //') $(grep -oE 'BUILD (SUCCESS|FAILURE)' "test_${side}_e2_$T.log" | tail -1)"
done
for side in base cur; do d="${side}_e2_$T"; rm -rf "cls_${side}_e2_$T"; mkdir -p "cls_${side}_e2_$T"
    find "$d/src/jdk.m3.collections" "$d/m3/arrays/src/main/java" "$d/m3/collections/src/main/java" -name "*.java" ! -name module-info.java ! -name package-info.java 2>/dev/null > "cls_${side}_e2_$T.files"
    javac --release 21 -nowarn -encoding UTF-8 -d "cls_${side}_e2_$T" @"cls_${side}_e2_$T.files" > "cls_${side}_e2_$T.log" 2>&1 || say "javac $side FAILED: $(grep -c error "cls_${side}_e2_$T.log") errors"
done
: > "stay_e2_$T.txt"
CURATE_CORE=jdk.m3.collections python -I curate_e_bytecode.py "cls_base_e2_$T" "cls_cur_e2_$T" RENAME_MAP_E2.tsv "stay_e2_$T.txt" "proof_e2_$T" > "proof_e2_$T.log" 2>&1
say "bytecode: $(tail -1 "proof_e2_$T.log")"
git -C bcw status --porcelain -uall -- m3 | grep -v '/target/' | awk '$1=="M"{print $2}' | grep '\.java$' | grep -v -E '^m3/(collections|arrays)/' > "loose_e2_$T.txt"
bash bc_loose_parity.sh bcw "$(cygpath -m "$H/cls_base_e2_$T")" "$(cygpath -m "$H/cls_cur_e2_$T")" "loose_e2_$T" $(cat "loose_e2_$T.txt") > "LOOSE_E2_$T.tsv" 2>&1
say "loose parity: $(cut -f4 "LOOSE_E2_$T.tsv" | sort | uniq -c | tr '\n' ' ')"
for d in "cur_e2_$T" "base_e2_$T"; do (cd "$d" && git init -q && git config core.autocrlf false && git config core.longpaths true && git add -A > /dev/null 2>&1 && git -c user.name=v -c user.email=v@v commit -q -m t > /dev/null 2>&1); done
(cd "cur_e2_$T" && python -I m3/tooling/jdk-naming/jdk_naming.py plan --root . --only m3/collections 2>&1 | tail -1) | sed 's/^/naming cur: /' | tee -a "$R"
(cd "base_e2_$T" && python -I m3/tooling/jdk-naming/jdk_naming.py plan --root . --only m3/collections 2>&1 | tail -1) | sed 's/^/naming base: /' | tee -a "$R"
say "DONE"
