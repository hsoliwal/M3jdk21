#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Before/after evidence of qualification/hash-reentrant-guard-20261107 (recipe HASH_REENTRANT_GUARD v1): compiles
# M3HashReentrantLookupSelfTest with -Xlint:all -Werror against the base and the head classes, runs it in report mode
# on both (every owner, M3 and its java.util twin, with the m3_arrays JNI library when NATIVE_LIB is set) and then in
# assert mode on head, and writes REFERENCE.tsv: per owner pair, hash mode/action and operation, the verdict counts of
# the java.util twin, of the M3 owner on base and of the M3 owner on head.
# usage: differential.sh <base classes> <head classes> <m3-arrays jar> <out dir>
# env:   JAVA_HOME = JDK 21; JVM_OPTS; NATIVE_LIB = the m3_arrays JNI library (linked owners are skipped without it)
set -euo pipefail
export LC_ALL=C
BASE=$1; HEAD=$2; ARRAYS=$3; OUT=$4
HERE=$(cd "$(dirname "$0")" && (pwd -W 2>/dev/null || pwd))
TEST="$HERE/../../src/test/java/jdk/m3/collections/M3HashReentrantLookupSelfTest.java"
J="${JAVA_HOME:?JAVA_HOME}/bin"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
JAVAC_J=$(for o in ${JVM_OPTS:-}; do printf -- "-J%s " "$o"; done)
NATIVE=${NATIVE_LIB:+-Dm3.arrays.native.path=$NATIVE_LIB}
rm -rf "$OUT"; mkdir -p "$OUT/base" "$OUT/head"
for side in base head; do
  classes=$BASE; [ "$side" = head ] && classes=$HEAD
  "$J/javac" $JAVAC_J --release 21 -Xlint:all -Werror -proc:none -encoding UTF-8 -cp "$classes$SEP$ARRAYS" \
      -d "$OUT/$side" "$TEST"
  "$J/java" ${JVM_OPTS:-} $NATIVE -cp "$OUT/$side$SEP$classes$SEP$ARRAYS" jdk.m3.collections.M3HashReentrantLookupSelfTest \
      report | tr -d '\r' > "$OUT/$side.tsv"
  echo "$side: $(tail -1 "$OUT/$side.tsv")"
done
"$J/java" ${JVM_OPTS:-} $NATIVE -cp "$OUT/head$SEP$HEAD$SEP$ARRAYS" jdk.m3.collections.M3HashReentrantLookupSelfTest \
    | tr -d '\r' | tail -1 | sed 's/^/head assert: /'
python -I - "$OUT/base.tsv" "$OUT/head.tsv" "$HERE/REFERENCE.tsv" <<'EOF'
import sys
from collections import Counter, defaultdict

def load(path):
    counts = defaultdict(Counter)
    for line in open(path, encoding="utf-8"):
        cells = line.rstrip("\n").split("\t")
        if len(cells) > 5:
            counts[(cells[0], cells[1], cells[2])][cells[4]] += 1
    return counts

base, head = load(sys.argv[1]), load(sys.argv[2])
twins = {"M3HashMap": "HashMap", "M3LinkedHashMap": "LinkedHashMap", "M3LinkedHashMap(access)": "LinkedHashMap(access)",
         "M3WeakHashMap": "WeakHashMap", "M3HashSet": "HashSet", "M3LinkedHashSet": "LinkedHashSet"}
order = ["PLAIN", "BEFORE", "AFTER", "FAILFAST", "WRONG", "DAMAGED"]
fmt = lambda c: " ".join("%s=%d" % (k, c[k]) for k in order if c.get(k)) or "-"
keys = sorted({k for k in base if k[0] in twins} | {k for k in head if k[0] in twins})
totals = defaultdict(Counter)
with open(sys.argv[3], "w", encoding="utf-8", newline="\n") as out:
    out.write("# SPDX-License-Identifier: Apache-2.0\n")
    out.write("# Verdict counts of M3HashReentrantLookupSelfTest (report mode) per owner pair, hash mode/action and "
              "operation: the java.util twin (reference column), the M3 owner on base (#2676 head 0b97282f6d1d) and on "
              "head (this leaf). One case per user-callback call N of the operation. Regenerate with differential.sh.\n")
    out.write("m3_owner\tjdk_twin\tmode\toperation\tjdk\tm3_base\tm3_head\n")
    for owner, mode, op in keys:
        twin = twins[owner]
        j, b, h = base.get((twin, mode, op), Counter()), base.get((owner, mode, op), Counter()), \
            head.get((owner, mode, op), Counter())
        totals[(owner, "jdk")].update(j); totals[(owner, "base")].update(b); totals[(owner, "head")].update(h)
        out.write("\t".join([owner, twin, mode, op, fmt(j), fmt(b), fmt(h)]) + "\n")
    out.write("# totals per owner\n")
    for owner in twins:
        out.write("\t".join(["TOTAL", owner, "", "", fmt(totals[(owner, "jdk")]), fmt(totals[(owner, "base")]),
                             fmt(totals[(owner, "head")])]) + "\n")
for owner in twins:
    print("%-24s jdk[%s] base[%s] head[%s]" % (owner, fmt(totals[(owner, "jdk")]), fmt(totals[(owner, "base")]),
                                              fmt(totals[(owner, "head")])))
EOF
