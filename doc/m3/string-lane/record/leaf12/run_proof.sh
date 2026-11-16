#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# REGEX_MEMO_PROOF v1 (2026-11-16). usage: run_proof.sh <leaf-dir> <classes-root>
# Applies apply_regex_memo.py (the recipe that edited the fork String.java) to the STOCK java/lang/String.java of the
# JDK's src.zip, compiles the result into java.base with --patch-module, and runs RegexMemoProof on the unpatched stock
# JVM and on the JVM with the edited String. Gates: every case digest equal in both runs; the memo checks pass.
# Then a rough timing of both. Exit 0 = PASS, 1 = a difference or a failed memo check, 3 = compile or run failure.
set -u
leaf=$1; classes=$2
here=$(cd "$(dirname "$0")" && { pwd -W 2>/dev/null || pwd; })
java_home=${PROOF_JAVA_HOME:-C:/Program Files/Java/jdk-21}
work=$leaf/proof/work; gen=$classes/gen; rm -rf "$gen"; mkdir -p "$work" "$gen/java/lang" "$classes/jdk" "$classes/harness"
unzip -p "$java_home/lib/src.zip" java.base/java/lang/String.java > "$gen/String.stock.java" || exit 3
python -I "$leaf/apply_regex_memo.py" "$gen/String.stock.java" "$leaf/regex_memo.txt" "$gen/java/lang/String.java" || exit 3
rm -f "$gen/String.stock.java"
MSYS_NO_PATHCONV=1 "$java_home/bin/javac" -nowarn -XDignore.symbol.file -implicit:none -d "$classes/jdk" \
  --patch-module "java.base=$gen" "$gen/java/lang/String.java" > "$work/javac-string.log" 2>&1 \
  || { echo "COMPILE_FAIL edited stock String"; head -5 "$work/javac-string.log"; exit 3; }
"$java_home/bin/javac" -nowarn -d "$classes/harness" "$here/RegexMemoProof.java" || { echo "COMPILE_FAIL harness"; exit 3; }
stock() { "$java_home/bin/java" -cp "$classes/harness" RegexMemoProof "$@"; }
memo() { MSYS_NO_PATHCONV=1 "$java_home/bin/java" --patch-module "java.base=$classes/jdk" --add-opens java.base/java.lang=ALL-UNNAMED -cp "$classes/harness" RegexMemoProof "$@"; }
stock cases "$work/cases-stock.tsv" | sed 's/^/stock\t/' || exit 3
memo cases "$work/cases-memo.tsv" | sed 's/^/memo\t/' || exit 3
differ=$(diff "$work/cases-stock.tsv" "$work/cases-memo.tsv" | grep -c '^>')
printf 'case_digests_differ\t%s\n' "$differ"
memo memo || { echo "RESULT	FAIL"; exit 1; }
stock time | sed 's/^/stock\t/'; memo time | sed 's/^/memo\t/'
[ "$differ" -eq 0 ] && echo "RESULT	PASS" || { echo "RESULT	FAIL"; exit 1; }
