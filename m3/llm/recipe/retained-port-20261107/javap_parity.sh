#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# javap parity of recipe retained-port-20261107 (m3/llm): every donor class (the com.m3 namespace, PR #2108 head
# 58bc0d2d297e) against its restored class (jdk.m3.llm). javap -p (declarations, package names normalized) must be
# identical for all classes; javap -p -c -constants of the one ADAPT class ChromeMlTaskService must differ in exactly
# one ldc_w pair (the capabilities schema id). Line endings are normalized before the diff (javap prints CRLF on
# Windows). The donor package and path are spelled in two shell words so that jdk_naming.py stays a fixed point here.
# usage: bash javap_parity.sh <donor classes dir> <restored classes dir> <out dir> [java home]
set -euo pipefail
DONOR="$1"; RESTORED="$2"; OUT="$3"; JH="${4:-${JAVA_HOME:-C:/Program Files/Java/jdk-21}}"
DONOR_PKG="com.m3"".llm"; DONOR_PATH="com/m3""/llm"
rm -rf "$OUT"; mkdir -p "$OUT/donor" "$OUT/restored"
norm() { sed -e "s/${DONOR_PKG//./[.]}/jdk.m3.llm/g" -e "s|$DONOR_PATH|jdk/m3/llm|g" | tr -d '\r'; }
cd "$DONOR"
for c in $(find com -name '*.class' | sed 's|\.class$||; s|/|.|g'); do
  n=${c#"$DONOR_PKG".}
  "$JH/bin/javap" -p -cp . "$c" | norm > "$OUT/donor/$n.txt"
  "$JH/bin/javap" -p -cp "$RESTORED" "jdk.m3.llm.$n" | tr -d '\r' > "$OUT/restored/$n.txt"
done
COUNT=$(ls "$OUT/donor" | wc -l)
if diff -r "$OUT/donor" "$OUT/restored" > "$OUT/javap-p-diff.log"; then echo "javap -p identical classes=$COUNT"; else echo "javap -p DIFFERS ($OUT/javap-p-diff.log)"; exit 1; fi
"$JH/bin/javap" -p -c -constants -cp . "$DONOR_PKG.chrome.ChromeMlTaskService" | norm > "$OUT/donor-ChromeMlTaskService-code.txt"
"$JH/bin/javap" -p -c -constants -cp "$RESTORED" jdk.m3.llm.chrome.ChromeMlTaskService | tr -d '\r' > "$OUT/restored-ChromeMlTaskService-code.txt"
diff "$OUT/donor-ChromeMlTaskService-code.txt" "$OUT/restored-ChromeMlTaskService-code.txt" > "$OUT/taskservice-code-diff.log" || true
PAIRS=$(grep -c '^[<>] ' "$OUT/taskservice-code-diff.log" || true); PAIRS=${PAIRS:-0}
LDC=$(grep -c '^[<>] .*ldc_w.*CHROMEML-CAPABILITIES/1' "$OUT/taskservice-code-diff.log" || true); LDC=${LDC:-0}
echo "ChromeMlTaskService bytecode changed_lines=$PAIRS schema_ldc_lines=$LDC"
if [ "$PAIRS" = 2 ] && [ "$LDC" = 2 ]; then echo "JAVAP PARITY PASS"; exit 0; fi
echo "JAVAP PARITY FAIL"; exit 1
