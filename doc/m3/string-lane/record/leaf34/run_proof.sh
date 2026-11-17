#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
#
# ESCAPE_MARKUP_FAMILY_PROOF v1 (2026-11-17). usage: run_proof.sh <leaf-dir> <work-dir> <donor-jars>
# <leaf-dir> holds difference_base.txt, search_family.txt, edge_family.txt, class_family.txt, compare_family.txt,
# slice_family.txt, build_family.txt, split_family.txt, replace_family.txt, case_family.txt, escape_markup_family.txt
# and String.head.java. Checks that the eleven texts occur once, verbatim, in the head, inserts them into the STOCK
# java/lang/String.java of the JDK's src.zip with an m3() shim over the test stub M3StringStub.java.txt, compiles into
# java.base (java.base lint flags), writes the oracle corpus once (EscapeMarkupFamilyProof corpus), runs the CPython
# 3.12 oracle (python_oracle.py: html.escape, html.unescape) once, and runs EscapeMarkupFamilyProof in three modes
# (flat; stub M3 storage with facts; without facts), each with compact strings on and off. <donor-jars> =
# commons-lang3 3.22.0, commons-text 1.15.0, unbescape 1.1.8. Exit 0 = every run PASS.
set -u
leaf=$1; work=$2; donor=$3
here=$(cd "$(dirname "$0")" && { pwd -W 2>/dev/null || pwd; })
jh=${PROOF_JAVA_HOME:-C:/Program Files/Java/jdk-21}
export MSYS_NO_PATHCONV=1
gen=$work/gen; out=$work/classes; rm -rf "$gen" "$out" "$work/harness"; mkdir -p "$gen/java/lang" "$out" "$work/harness"
python -I - "$leaf" "$gen" "$jh/lib/src.zip" "$here/M3StringStub.java.txt" <<'EOF' || exit 2
import sys, zipfile
leaf, gen, src, stubfile = sys.argv[1:5]
read = lambda p: open(p, encoding="utf-8", newline="").read().replace("\r", "")
head = read(leaf + "/String.head.java")
names = ("difference_base.txt", "search_family.txt", "edge_family.txt", "class_family.txt", "compare_family.txt",
         "slice_family.txt", "build_family.txt", "split_family.txt", "replace_family.txt", "case_family.txt",
         "escape_markup_family.txt")
pieces = [read(leaf + "/" + n) for n in names]
for n, p in zip(names, pieces):
    assert head.count(p) == 1, n + " is not once in the head"
stock = zipfile.ZipFile(src).read("java.base/java/lang/String.java").decode("utf-8").replace("\r", "")
lines = stock.split("\n")
anchor = [i for i, l in enumerate(lines) if l == "    public boolean contains(CharSequence s) {"]
assert len(anchor) == 1
start = max(i for i in range(anchor[0]) if lines[i] == "    /**")
body = "\n".join(p.strip("\n") for p in pieces).split("\n")
lines[start:start] = body + ["", "    M3String m3() { return M3String.FORCE ? new M3String(this) : null; }", ""]
open(gen + "/java/lang/String.java", "w", encoding="utf-8", newline="").write("\n".join(lines))
stub = read(stubfile)
cut = stub.index("/** The fields and class rules of M3StringFacts")
open(gen + "/java/lang/M3String.java", "w", encoding="utf-8", newline="").write(stub[:cut])
open(gen + "/java/lang/M3StringFacts.java", "w", encoding="utf-8", newline="").write("package java.lang;\n\n" + stub[cut:])
print("method_texts_verbatim_in_head\t%d" % len(names))
EOF
"$jh/bin/javac" -Xlint:all,-this-escape -Werror -XDignore.symbol.file -implicit:none -d "$out" \
  --patch-module "java.base=$gen" "$gen/java/lang/String.java" "$gen/java/lang/M3String.java" \
  "$gen/java/lang/M3StringFacts.java" > "$work/javac-string.log" 2>&1 \
  || { echo "COMPILE_FAIL String"; head -30 "$work/javac-string.log"; exit 3; }
echo "lint	javac -Xlint:all,-this-escape -Werror: no warnings"
[ "${PROOF_COMPILE_ONLY:-}" = 1 ] && exit 0
"$jh/bin/javac" -nowarn -d "$work/harness" --patch-module "java.base=$out" -cp "$donor" \
  "$here/EscapeMarkupFamilyProof.java" > "$work/javac-harness.log" 2>&1 \
  || { echo "COMPILE_FAIL harness"; head -20 "$work/javac-harness.log"; exit 3; }
"$jh/bin/java" --patch-module "java.base=$out" -cp "$work/harness;$donor" EscapeMarkupFamilyProof corpus "$work" \
  || exit 4
python -I "$here/python_oracle.py" "$work" || exit 4
status=0
for mode in flat m3-facts m3-scan; do
  case $mode in
    flat) props= ;;
    m3-facts) props="-Dm3.stub.force=true -Dm3.stub.facts=true" ;;
    m3-scan) props="-Dm3.stub.force=true" ;;
  esac
  for compact in -XX:+CompactStrings -XX:-CompactStrings; do
    echo "== EscapeMarkupFamilyProof $mode $compact"
    # shellcheck disable=SC2086
    "$jh/bin/java" $props "$compact" --patch-module "java.base=$out" -cp "$work/harness;$donor" \
      EscapeMarkupFamilyProof check "$work" > "$work/EscapeMarkupFamilyProof-$mode$compact.out" 2>&1 \
      || status=1
    grep -v '	0	-$' "$work/EscapeMarkupFamilyProof-$mode$compact.out"
  done
done
exit $status
