#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# SEARCH_FAMILY_PROOF v1 (2026-11-17). usage: run_proof.sh <method-text-file> <head-String.java> <work-dir> <classes-dir> <commons-lang3.jar>
# Checks that <method-text-file> occurs verbatim in the head String.java, inserts the same text into the STOCK
# java/lang/String.java of the JDK's src.zip (before the javadoc of contains(CharSequence)), adds a stub
# java.lang.M3String and a null-returning m3() shim, compiles that String into java.base with --patch-module and runs
# SearchFamilyProof on the stock JVM with the patched String. The flat paths and the indexOf-hop kernel then run
# inside the real java.lang.String. Exit 0 = PASS.
set -u
piece=$1; head=$2; work=$3; classes=$4; donor=$5
here=$(cd "$(dirname "$0")" && { pwd -W 2>/dev/null || pwd; })
java_home=${PROOF_JAVA_HOME:-C:/Program Files/Java/jdk-21}
gen=$work/gen; rm -rf "$gen"; mkdir -p "$gen/java/lang" "$classes/jdk" "$classes/harness"
python -I - "$piece" "$head" "$gen" "$java_home/lib/src.zip" <<'EOF' || exit 2
import sys, zipfile
piece, head, gen, src = sys.argv[1:5]
text = open(piece, encoding="utf-8", newline="").read().replace("\r", "")
assert text in open(head, encoding="utf-8", newline="").read().replace("\r", ""), "method text is not in the head source"
stock = zipfile.ZipFile(src).read("java.base/java/lang/String.java").decode("utf-8").replace("\r", "")
lines = stock.split("\n")
anchor = [i for i, l in enumerate(lines) if l == "    public boolean contains(CharSequence s) {"]
assert len(anchor) == 1
start = max(i for i in range(anchor[0]) if lines[i] == "    /**")
lines[start:start] = text.rstrip("\n").split("\n") + ["", "    M3String m3() { return null; }", ""]
open(gen + "/java/lang/String.java", "w", encoding="utf-8", newline="").write("\n".join(lines))
open(gen + "/java/lang/M3String.java", "w", encoding="utf-8", newline="").write("package java.lang;\nfinal class M3String {}\n")
print("method_text_verbatim_in_head\t1")
EOF
MSYS_NO_PATHCONV=1 "$java_home/bin/javac" -nowarn -XDignore.symbol.file -implicit:none -d "$classes/jdk" \
  --patch-module "java.base=$gen" "$gen/java/lang/String.java" "$gen/java/lang/M3String.java" > "$work/javac-string.log" 2>&1 \
  || { echo "COMPILE_FAIL patched String"; head -5 "$work/javac-string.log"; exit 3; }
MSYS_NO_PATHCONV=1 "$java_home/bin/javac" -nowarn -d "$classes/harness" --patch-module "java.base=$classes/jdk" \
  -cp "$donor" "$here/SearchFamilyProof.java" > "$work/javac-harness.log" 2>&1 \
  || { echo "COMPILE_FAIL harness"; head -5 "$work/javac-harness.log"; exit 3; }
MSYS_NO_PATHCONV=1 "$java_home/bin/java" --patch-module "java.base=$classes/jdk" --add-opens java.base/java.lang=ALL-UNNAMED \
  -cp "$classes/harness;$donor" SearchFamilyProof
