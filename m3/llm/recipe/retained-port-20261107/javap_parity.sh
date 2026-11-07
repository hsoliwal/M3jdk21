#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# javap parity of recipe retained-port-20261107 (m3/llm): every donor class (donor package "com.m3" + ".llm", spelled in
# two shell words below so that jdk_naming.py stays a fixed point on this gate; PR #2108 head 58bc0d2d297e)
# against its restored class (jdk.m3.llm). javap -p (declarations, package names normalized) must be identical for all
# classes; javap -p -c -constants of the one ADAPT class ChromeMlTaskService must differ in exactly one ldc_w pair
# (the capabilities schema id). Line endings are normalized before the diff (javap prints CRLF on Windows).
# usage: bash javap_parity.sh <donor classes dir> <restored classes dir> <out dir> [java home]
set -euo pipefail
DONOR="$1"; RESTORED="$2"; OUT="$3"; JH="${4:-${JAVA_HOME:-C:/Program Files/Java/jdk-21}}"
rm -rf "$OUT"; mkdir -p "$OUT/donor" "$OUT/restored"
DP="com.m3"".llm"; DD="com/m3""/llm"
norm() { sed -e 's/com\.m3\.llm/jdk.m3.llm/g' -e "s|$DD|jdk/m3/llm|g" | tr -d '\r'; }
cd "$DONOR"
for c in $(find com -name '*.class' | sed 's|\.class$||; s|/|.|g'); do
  n=${c#$DP.}
  "$JH/bin/javap" -p -cp . "$c" | norm > "$OUT/donor/$n.txt"
  "$JH/bin/javap" -p -cp "$RESTORED" "jdk.m3.llm.$n" | tr -d '\r' > "$OUT/restored/$n.txt"
done
COUNT=$(ls "$OUT/donor" | wc -l)
if diff -r "$OUT/donor" "$OUT/restored" > "$OUT/javap-p-diff.log"; then echo "javap -p identical classes=$COUNT"; else echo "javap -p DIFFERS ($OUT/javap-p-diff.log)"; exit 1; fi
"$JH/bin/javap" -p -c -constants -cp . "$DP.chrome.ChromeMlTaskService" | norm > "$OUT/donor-ChromeMlTaskService-code.txt"
"$JH/bin/javap" -p -c -constants -cp "$RESTORED" jdk.m3.llm.chrome.ChromeMlTaskService | tr -d '\r' > "$OUT/restored-ChromeMlTaskService-code.txt"
diff "$OUT/donor-ChromeMlTaskService-code.txt" "$OUT/restored-ChromeMlTaskService-code.txt" > "$OUT/taskservice-code-diff.log" || true
PAIRS=$(grep -c '^[<>] ' "$OUT/taskservice-code-diff.log" || true); PAIRS=${PAIRS:-0}
LDC=$(grep -c '^[<>] .*ldc_w.*CHROMEML-CAPABILITIES/1' "$OUT/taskservice-code-diff.log" || true); LDC=${LDC:-0}
echo "ChromeMlTaskService bytecode changed_lines=$PAIRS schema_ldc_lines=$LDC"
if [ "$PAIRS" = 2 ] && [ "$LDC" = 2 ]; then echo "JAVAP PARITY PASS"; exit 0; fi
echo "JAVAP PARITY FAIL"; exit 1
