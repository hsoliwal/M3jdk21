#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Oracle gates of recipe census2-a5-20261106 (RELOCATE_WAVE9_FLOW_CONTRACT v1), after mvn -f m3/algorithms/pom.xml package:
#   1 recipe fixed point (relocate.py --check)
#   2 donor figure: the donor contract with the donor wave9 solver and spatial index, compiled from the donor tree
#     into the scratch directory (never committed), JIT and -Xint = the crate README/evidence figure 138,244
#   3 mapped figure: MinCostFlowWave9Contract over the landed owner (target classes), JIT and -Xint = 8,069
#   4 mutants: four one-line mutants of the landed owner (scratch copies) must each fail the mapped contract;
#     the module's donor oracle MinCostFlowOracle is run on the same mutants for comparison (informational)
# usage: bash verify.sh <com.synexia clone> <donor rev> <scratch dir>
set -euo pipefail
donor_git=$1; donor_rev=$2; scratch=$3
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
module="$(cd "$here/../.." && pwd -W)"  # Windows form: java.exe takes the ;-separated class path
jdk="${JDK21_HOME:-C:/Program Files/Java/jdk-21}/bin"
# Donor paths live only in RELOCATION_MAP.tsv: jdk_naming.py rewrites a donor path spelled in a script (KEBAB, PKG_PATH).
crate=$(awk -F'\t' '$1 == "FILE" {print $2}' "$here/RELOCATION_MAP.tsv")
crate=${crate%/*}
rm -rf "$scratch"; mkdir -p "$scratch/donor/src" "$scratch/donor/out"

python -I "$here/relocate.py" --donor-git "$donor_git" --donor-rev "$donor_rev" --check | tail -1

for f in MinCostFlow M3PackedSpatialIndex M3Wave9FlowSpatialContract; do
  git -C "$donor_git" show "$donor_rev:$crate/$f.java.txt" > "$scratch/donor/src/$f.java"
done
"$jdk/javac" --release 21 -d "$scratch/donor/out" "$scratch"/donor/src/*.java
echo "DONOR_JIT  $("$jdk/java" -cp "$scratch/donor/out" M3Wave9FlowSpatialContract)"
echo "DONOR_XINT $("$jdk/java" -Xint -cp "$scratch/donor/out" M3Wave9FlowSpatialContract)"

cp="$module/target/classes;$module/target/test-classes"
echo "MAPPED_JIT  $("$jdk/java" -cp "$cp" jdk.m3.algorithm.MinCostFlowWave9Contract)"
echo "MAPPED_XINT $("$jdk/java" -Xint -cp "$cp" jdk.m3.algorithm.MinCostFlowWave9Contract)"

owner="$module/src/main/java/jdk/m3/algorithm/MinCostFlow.java"
mutate() {  # name, old, new: one exact occurrence, else refuse
  python -I -c 'import sys; s=open(sys.argv[1],encoding="utf-8").read(); n=s.count(sys.argv[2]); assert n==1, n; open(sys.argv[3],"w",encoding="utf-8",newline="").write(s.replace(sys.argv[2],sys.argv[4]))' \
    "$owner" "$2" "$scratch/$1/MinCostFlow.java" "$3"
}
first_outcome() {  # the first PASS figure or failure of a contract run, one line
  grep -oE 'PASS checks=[0-9]+|AssertionError[^[:cntrl:]]{0,60}|[A-Za-z]+Exception[^[:cntrl:]]{0,60}' | head -n 1 || true
}
run_mutant() {  # name, old, new
  mkdir -p "$scratch/$1/out"
  mutate "$1" "$2" "$3"
  "$jdk/javac" --release 21 -d "$scratch/$1/out" "$scratch/$1/MinCostFlow.java" \
    "$module/src/test/java/jdk/m3/algorithm/MinCostFlowWave9Contract.java" \
    "$module/src/test/java/jdk/m3/algorithm/MinCostFlowOracle.java"
  local wave9 oracle
  wave9=$("$jdk/java" -cp "$scratch/$1/out" jdk.m3.algorithm.MinCostFlowWave9Contract 2>&1 | first_outcome || true)
  oracle=$("$jdk/java" -cp "$scratch/$1/out" jdk.m3.algorithm.MinCostFlowOracle 2>&1 | first_outcome || true)
  case "$wave9" in *PASS*) echo "MUTANT $1 SURVIVED wave9=[$wave9] oracle=[$oracle]"; return 1;; esac
  echo "MUTANT $1 KILLED wave9=[$wave9] oracle=[$oracle]"
}
run_mutant M1_reverse_cost "arcCost[forward + 1] = -cost;" "arcCost[forward + 1] = cost;"
run_mutant M2_one_pass_fewer "for (int pass = 1; pass < vertices; pass++)" "for (int pass = 2; pass < vertices; pass++)"
run_mutant M3_demand_unbounded "int augmentation = demand - flow;" "int augmentation = Integer.MAX_VALUE;"
run_mutant M4_self_loop_admitted " || u == v" ""
