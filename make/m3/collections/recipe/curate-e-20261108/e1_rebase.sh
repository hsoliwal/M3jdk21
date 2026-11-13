#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# e1_rebase.sh <tag>: CURATE_E pass E1 on the current M3jdk21 master, end to end.
#   1. bcw -> origin/master; census + LANDED_MAP; draft map; carry the proven placement (carry_map_e1.py)
#   2. base tree base_<tag> (m3/arrays + m3/collections + jdk-naming, aggregator pom)
#   3. placement fixed point (curate_e_fastloop.sh, javac without error cap)
#   4. Maven -Werror test-compile of the final map (curate_e_loop.sh, 0 rounds)
#   5. bytecode proof (curate_e_bytecode.py) and mvn test on base and curated trees
# Writes E1_<tag>.txt with one verdict line per step.
set -uo pipefail
H=$(cd "$(dirname "$0")" && pwd); cd "$H"; T="$1"; R="E1_$T.txt"; : > "$R"
export JAVA_HOME="C:/Program Files/Java/jdk-21" PATH="/c/Program Files/Java/jdk-21/bin:$PATH"
say() { echo "$*" | tee -a "$R"; }
git -C bcw fetch -q origin master && git -C bcw reset -q --hard origin/master; M=$(git -C bcw rev-parse --short=12 HEAD); say "master $M"
git -C bcw ls-files 'm3/collections/src/main/java/*.java' | grep -v -E 'module-info|package-info' | sed -E 's#m3/collections/src/main/java/##' > "files_$T.txt"
python -I landed_map.py bcw "LANDED_$T.tsv" MAP.tsv curation/RENAME_MAP.tsv > /dev/null
python -I curate_e_classify.py "files_$T.txt" "LANDED_$T.tsv" curation/java_names.txt "draft_$T.tsv" > "classify_$T.log"
python -I gen_map_e1.py "draft_$T.tsv" bcw "fresh_$T.tsv" > /dev/null || { say "gen_map_e1 FAILED"; exit 2; }
python -I carry_map_e1.py "fresh_$T.tsv" RENAME_MAP_E1_final.tsv DEQUALIFY.tsv "intended_$T.tsv" | sed 's/^/map: /' | tee -a "$R"
rm -rf "base_$T"; mkdir -p "base_$T/m3"; cp cur2/pom.xml "base_$T/"
for d in m3/arrays m3/collections make/m3/tooling/jdk-naming; do git -C bcw archive --format=tar HEAD "$d" | tar -x -C "base_$T"; done
(cd "base_$T" && git init -q && git config core.autocrlf false && git config core.longpaths true && git add -A && git -c user.name=b -c user.email=b@b commit -q -m "base $M")
rm -rf "fast_$T"; bash curate_e_fastloop.sh "base_$T" "intended_$T.tsv" "fast_$T" 16 > "fast_$T.log" 2>&1
fin=$(grep -oE 'STABLE at round [0-9]+: \S+' "fast_$T.log" | awk '{print $NF}')
[ -n "$fin" ] || { say "placement STUCK: $(grep -E '^STUCK' "fast_$T.log")"; exit 3; }
cp "$fin" "RENAME_MAP_E1_$T.tsv"; say "placement STABLE ($(grep -c '^round' "fast_$T.log") rounds)"
rm -rf "mvn_$T"; CURATE_CORE=jdk.internal.m3.collections TESTS_AS_OWNERS=1 TEST_EDITS="$H/curate-e/E1_TEST_EDITS.json" \
    bash curate_e_loop.sh "base_$T" "RENAME_MAP_E1_$T.tsv" "mvn_$T" 0 > "mvn_$T.log" 2>&1
grep -q "CLEAN" "mvn_$T.log" && say "maven -Werror test-compile CLEAN" || { say "maven NOT clean: $(tail -2 "mvn_$T.log")"; exit 4; }
for side in base cur; do d=$([ $side = base ] && echo "base_$T" || echo "mvn_$T/tree")
    rm -rf "cls_${side}_$T"; mkdir -p "cls_${side}_$T"
    find "$d/m3/arrays/src/main/java" "$d/m3/collections/src/main/java" -name "*.java" ! -name module-info.java ! -name package-info.java > "cls_${side}_$T.files"
    javac --release 21 -nowarn -encoding UTF-8 -d "cls_${side}_$T" @"cls_${side}_$T.files" > "cls_${side}_$T.log" 2>&1 || { say "javac $side FAILED"; exit 5; }
done
: > "stay_$T.txt"
CURATE_CORE=jdk.internal.m3.collections python -I curate_e_bytecode.py "cls_base_$T" "cls_cur_$T" "RENAME_MAP_E1_$T.tsv" "stay_$T.txt" "proof_$T" > "proof_$T.log" 2>&1
say "bytecode: $(tail -1 "proof_$T.log")"
for side in base cur; do d=$([ $side = base ] && echo "base_$T" || echo "mvn_$T/tree")
    (cd "$d" && timeout 3300 mvn -o -B test > "../$( [ $side = cur ] && echo ../ )test_${side}_$T.log" 2>&1)
done
for side in base cur; do say "tests $side: $(grep -E 'Tests run: [0-9]+, Failures' "test_${side}_$T.log" | tail -1 | sed -E 's/\[[A-Z]+\] //') $(grep -oE 'BUILD (SUCCESS|FAILURE)' "test_${side}_$T.log" | tail -1)"; done
(cd "mvn_$T/tree" && python -I make/m3/tooling/jdk-naming/jdk_naming.py plan --root . --only m3/collections 2>&1 | tail -1) | sed 's/^/naming cur: /' | tee -a "$R"
(cd "base_$T" && python -I make/m3/tooling/jdk-naming/jdk_naming.py plan --root . --only m3/collections 2>&1 | tail -1) | sed 's/^/naming base: /' | tee -a "$R"
say "DONE"
