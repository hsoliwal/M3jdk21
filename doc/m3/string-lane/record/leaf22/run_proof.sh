#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
#
# STRING_FAMILY_SPEED_PROOF v1 (2026-11-17). usage: run_proof.sh <leaf-dir> <work-dir> <commons-lang3.jar>
# <leaf-dir> holds search_family{,.old}.txt, edge_family{,.old}.txt, String.head.java and String.master.java.
# In this record: search_family.old.txt = ../leaf20/search_family.txt, edge_family.old.txt = ../leaf21/edge_family.txt
# (byte-identical to the texts on the base), String.master.java / String.head.java = java/lang/String.java of the base
# and of this commit; apply_speed.py turns the old texts into the new ones.
# For side in new/old: checks that both family texts occur verbatim in that side's String.java (head/master), inserts
# them into the STOCK java/lang/String.java of the JDK's src.zip (before the javadoc of contains(CharSequence)) with a
# stub java.lang.M3String and a null-returning m3() shim, and compiles that String into java.base with --patch-module.
# Then on the new side: SearchFamilyProof, EdgeFamilyProof and SpeedProof with compact strings on and off (the off run
# takes the charAt paths for Latin-1 text); then Bench on both sides. Exit 0 = every proof PASS.
set -u
leaf=$1; work=$2; donor=$3
here=$(cd "$(dirname "$0")" && { pwd -W 2>/dev/null || pwd; })
java_home=${PROOF_JAVA_HOME:-C:/Program Files/Java/jdk-21}
export MSYS_NO_PATHCONV=1
build_side() {  # side head-file search-text edge-text
  local side=$1 gen=$work/gen-$1 out=$work/classes-$1
  rm -rf "$gen" "$out"; mkdir -p "$gen/java/lang" "$out"
  python -I - "$leaf/$2" "$gen" "$java_home/lib/src.zip" "$leaf/$3" "$leaf/$4" <<'EOF' || exit 2
import sys, zipfile
head, gen, src = sys.argv[1:4]
pieces = [open(p, encoding="utf-8", newline="").read().replace("\r", "") for p in sys.argv[4:]]
source = open(head, encoding="utf-8", newline="").read().replace("\r", "")
for p in pieces:
    assert source.count(p) == 1, "method text is not once in the side's source"
stock = zipfile.ZipFile(src).read("java.base/java/lang/String.java").decode("utf-8").replace("\r", "")
lines = stock.split("\n")
anchor = [i for i, l in enumerate(lines) if l == "    public boolean contains(CharSequence s) {"]
assert len(anchor) == 1
start = max(i for i in range(anchor[0]) if lines[i] == "    /**")
body = "\n".join(p.rstrip("\n") for p in pieces).split("\n")
lines[start:start] = body + ["", "    M3String m3() { return null; }", ""]
open(gen + "/java/lang/String.java", "w", encoding="utf-8", newline="").write("\n".join(lines))
open(gen + "/java/lang/M3String.java", "w", encoding="utf-8", newline="").write("package java.lang;\nfinal class M3String {}\n")
print("method_text_verbatim\t" + str(len(pieces)))
EOF
  "$java_home/bin/javac" -Xlint:all -XDignore.symbol.file -implicit:none -d "$out" \
    --patch-module "java.base=$gen" "$gen/java/lang/String.java" "$gen/java/lang/M3String.java" \
    > "$work/javac-$side.log" 2>&1 || { echo "COMPILE_FAIL $side"; head -20 "$work/javac-$side.log"; exit 3; }
  echo "lint_$side	$(grep -c 'warning:' "$work/javac-$side.log")"
}
build_side new String.head.java search_family.txt edge_family.txt
build_side old String.master.java search_family.old.txt edge_family.old.txt
mkdir -p "$work/harness"
"$java_home/bin/javac" -nowarn -d "$work/harness" --patch-module "java.base=$work/classes-new" -cp "$donor" \
  "$here/SearchFamilyProof.java" "$here/EdgeFamilyProof.java" "$here/SpeedProof.java" "$here/Bench.java" \
  > "$work/javac-harness.log" 2>&1 || { echo "COMPILE_FAIL harness"; head -20 "$work/javac-harness.log"; exit 3; }
status=0
run() {  # side main [vm options]
  local side=$1 main=$2; shift 2
  "$java_home/bin/java" "$@" --patch-module "java.base=$work/classes-$side" -cp "$work/harness;$donor" "$main"
}
bench() {  # side kernel-index string|donor
  "$java_home/bin/java" --patch-module "java.base=$work/classes-$1" -cp "$work/harness;$donor" Bench "$2" "$3"
}
[ "${SKIP_PROOFS:-0}" = 1 ] || for compact in -XX:+CompactStrings -XX:-CompactStrings; do
  for main in SearchFamilyProof EdgeFamilyProof SpeedProof; do
    echo "== $main $compact"
    run new "$main" "$compact" > "$work/$main$compact.out" 2>&1 || status=1
    grep -v '	0	-$' "$work/$main$compact.out"
  done
done
[ "${SKIP_BENCH:-0}" = 1 ] && exit $status
kernels=$(run new Bench | wc -l)
: > "$work/bench.tsv"
for fork in 1 2 3; do
  for ((k = 0; k < kernels; k++)); do
    for side in old new; do
      bench "$side" "$k" string | sed "s/^/$side	$fork	/" >> "$work/bench.tsv"
    done
    bench new "$k" donor | sed "s/^/donor	$fork	/" >> "$work/bench.tsv"
  done
done
python -I - "$work/bench.tsv" <<'EOF'
import sys, statistics, collections
cells = collections.defaultdict(list)
for line in open(sys.argv[1], encoding="utf-8"):
    build, fork, label, side, ns, _ = line.rstrip("\n").split("\t")
    cells[(label, build)].append(float(ns))
labels = list(dict.fromkeys(label for label, _ in cells))
print("kernel\told ns\tnew ns\tdonor ns\tnew vs old\tnew vs donor")
for label in labels:
    old, new, donor = (statistics.median(cells[(label, b)]) for b in ("old", "new", "donor"))
    print(f"{label}\t{old:.0f}\t{new:.0f}\t{donor:.0f}\t{old / new:.2f}x\t{donor / new:.2f}x")
EOF
exit $status
