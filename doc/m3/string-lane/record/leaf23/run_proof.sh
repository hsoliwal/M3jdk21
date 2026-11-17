#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
#
# CLASS_FAMILY_PROOF v1 (2026-11-17). usage: run_proof.sh <method-text-file> <head-String.java> <work-dir> <commons-lang3.jar>
# Checks that <method-text-file> occurs once, verbatim, in the head String.java, inserts it into the STOCK
# java/lang/String.java of the JDK's src.zip (before the javadoc of contains(CharSequence)) with a stub java.lang.M3String
# and an m3() shim over the test stub M3StringStub.java.txt (M3 paths: -Dm3.stub.force, -Dm3.stub.facts), compiles that String into java.base with --patch-module (java.base lint flags), runs
# ClassFamilyProof with compact strings on and off (the off run takes the code-point path for Latin-1 text), then
# Bench: one JVM per kernel and side (string, naive, donor), 3 forks, median. Exit 0 = PASS.
set -u
piece=$1; head=$2; work=$3; donor=$4
here=$(cd "$(dirname "$0")" && { pwd -W 2>/dev/null || pwd; })
jh=${PROOF_JAVA_HOME:-C:/Program Files/Java/jdk-21}
export MSYS_NO_PATHCONV=1
gen=$work/gen; out=$work/classes; rm -rf "$gen" "$out"; mkdir -p "$gen/java/lang" "$out" "$work/harness"
python -I - "$piece" "$head" "$gen" "$jh/lib/src.zip" "$here/M3StringStub.java.txt" <<'EOF' || exit 2
import sys, zipfile
piece, head, gen, src = sys.argv[1:5]
text = open(piece, encoding="utf-8", newline="").read().replace("\r", "")
assert open(head, encoding="utf-8", newline="").read().replace("\r", "").count(text) == 1, "method text not once in head"
stock = zipfile.ZipFile(src).read("java.base/java/lang/String.java").decode("utf-8").replace("\r", "")
lines = stock.split("\n")
anchor = [i for i, l in enumerate(lines) if l == "    public boolean contains(CharSequence s) {"]
assert len(anchor) == 1
start = max(i for i in range(anchor[0]) if lines[i] == "    /**")
lines[start:start] = text.strip("\n").split("\n") + [
    "", "    M3String m3() { return M3String.FORCE ? new M3String(this) : null; }", ""]
open(gen + "/java/lang/String.java", "w", encoding="utf-8", newline="").write("\n".join(lines))
stub = open(sys.argv[5], encoding="utf-8").read()
cut = stub.index("/** The fields and class rules of M3StringFacts")
open(gen + "/java/lang/M3String.java", "w", encoding="utf-8", newline="").write(stub[:cut])
open(gen + "/java/lang/M3StringFacts.java", "w", encoding="utf-8", newline="").write("package java.lang;\n\n" + stub[cut:])
print("method_text_verbatim_in_head\t1")
EOF
"$jh/bin/javac" -Xlint:all,-this-escape -Werror -XDignore.symbol.file -implicit:none -d "$out" \
  --patch-module "java.base=$gen" "$gen/java/lang/String.java" "$gen/java/lang/M3String.java" "$gen/java/lang/M3StringFacts.java" \
  > "$work/javac-string.log" 2>&1 || { echo "COMPILE_FAIL String"; head -20 "$work/javac-string.log"; exit 3; }
echo "lint	javac -Xlint:all,-this-escape -Werror: no warnings"
"$jh/bin/javac" -nowarn -d "$work/harness" --patch-module "java.base=$out" -cp "$donor" \
  "$here/ClassFamilyProof.java" "$here/ClassBench.java" > "$work/javac-harness.log" 2>&1 \
  || { echo "COMPILE_FAIL harness"; head -20 "$work/javac-harness.log"; exit 3; }
status=0
for mode in flat m3-facts m3-scan; do
  case $mode in
    flat) props= ;;
    m3-facts) props="-Dm3.stub.force=true -Dm3.stub.facts=true" ;;
    m3-scan) props="-Dm3.stub.force=true" ;;
  esac
  for compact in -XX:+CompactStrings -XX:-CompactStrings; do
    echo "== ClassFamilyProof $mode $compact"
    # shellcheck disable=SC2086
    "$jh/bin/java" $props "$compact" --patch-module "java.base=$out" -cp "$work/harness;$donor" \
      ClassFamilyProof || status=1
  done
done
[ "${SKIP_BENCH:-0}" = 1 ] && exit $status
kernels=$("$jh/bin/java" --patch-module "java.base=$out" -cp "$work/harness;$donor" ClassBench | wc -l)
: > "$work/bench.tsv"
for fork in 1 2 3; do
  for ((k = 0; k < kernels; k++)); do
    for side in string naive donor; do
      "$jh/bin/java" --patch-module "java.base=$out" -cp "$work/harness;$donor" ClassBench "$k" "$side" \
        | sed "s/^/$fork	/" >> "$work/bench.tsv"
    done
  done
done
python -I - "$work/bench.tsv" <<'EOF'
import sys, statistics, collections
cells = collections.defaultdict(list)
for line in open(sys.argv[1], encoding="utf-8"):
    fork, label, side, ns, _ = line.rstrip("\n").split("\t")
    cells[(label, side)].append(float(ns))
print("kernel\tstring ns\tnaive ns\tdonor ns\tvs naive\tvs donor")
for label in dict.fromkeys(label for label, _ in cells):
    s, n, d = (statistics.median(cells[(label, side)]) for side in ("string", "naive", "donor"))
    print(f"{label}\t{s:.0f}\t{n:.0f}\t{d:.0f}\t{n / s:.2f}x\t{d / s:.2f}x")
EOF
exit $status
