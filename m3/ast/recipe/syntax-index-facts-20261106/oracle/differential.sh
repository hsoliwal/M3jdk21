#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Before/after differential of jdk.m3.ast.M3SyntaxIndex (recipe m3/ast/recipe/syntax-index-facts-20261106).
# Both sides compile the m3/ast main sources of the working tree; the before side replaces M3SyntaxIndex.java by the
# git blob <before-owner-blob> (a pinned preimage of EDITS.tsv: the master owner, or the image an earlier crate
# produced), so the two sides differ in that one class only. SyntaxIndexDifferentialDump.java is compiled against
# each side separately and run over the Java sources of m3/ast and m3/algorithms at <corpus-rev> (default: the
# recipe's BASE_COMMIT), the same bytes for both sides. Every differing dump line is classified by its record type
# (FILE, KIND, FACTS, F, REFUSE, CONCURRENT, TOTAL) and every differing class file by javap -c -p.
# Exit 0 when both dumps are identical and the brute-force kind scan has no mismatch on either side; 1 otherwise.
# usage: differential.sh <before-owner-blob> <out-dir> [corpus-rev]   (JAVA_HOME = a JDK 21)
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$HERE/../../../../.." && pwd)
BLOB=$1; OUT=$2
CORPUS_REV=${3:-$(sed -n 's/^BASE_COMMIT = "\([0-9a-f]*\)"$/\1/p' "$HERE/../apply.py")}
J="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
MAIN=m3/ast/src/main/java
OWNER=$MAIN/jdk/m3/ast/M3SyntaxIndex.java
rm -rf "$OUT"; mkdir -p "$OUT/before-src" "$OUT/corpus"
cp -r "$ROOT/$MAIN/." "$OUT/before-src/"
git -C "$ROOT" cat-file blob "$BLOB" > "$OUT/before-src/${OWNER#"$MAIN/"}"
git -C "$ROOT" ls-tree -r --name-only "$CORPUS_REV" -- m3/ast m3/algorithms | grep '\.java$' > "$OUT/corpus.txt"
while read -r path; do
    mkdir -p "$OUT/corpus/$(dirname "$path")"
    git -C "$ROOT" show "$CORPUS_REV:$path" > "$OUT/corpus/$path"
done < "$OUT/corpus.txt"
for side in before after; do
    if [ "$side" = before ]; then src="$OUT/before-src"; else src="$ROOT/$MAIN"; fi
    mkdir -p "$OUT/$side-classes" "$OUT/$side-driver"
    "$J/javac" --release 21 -proc:none -Xlint:all -Werror -g -d "$OUT/$side-classes" $(find "$src" -name '*.java' | sort)
    "$J/javac" --release 21 -proc:none -Xlint:all -Werror -cp "$OUT/$side-classes" -d "$OUT/$side-driver" \
        "$HERE/SyntaxIndexDifferentialDump.java"
    start=$(date +%s)
    timeout 120 "$J/java" -Xmx1g -cp "$OUT/$side-classes$SEP$OUT/$side-driver" SyntaxIndexDifferentialDump \
        "$OUT/corpus.txt" "$OUT/corpus" > "$OUT/$side.dump"
    echo "SIDE $side seconds=$(( $(date +%s) - start )) lines=$(wc -l < "$OUT/$side.dump") sha256=$(sha256sum < "$OUT/$side.dump" | cut -c1-64)"
    (cd "$OUT/$side-classes" && find . -name '*.class' | sort | xargs sha256sum) > "$OUT/$side-classes.sha"
    tail -n 1 "$OUT/$side.dump"
done
{ diff "$OUT/before-classes.sha" "$OUT/after-classes.sha" || true; } | sed -n 's/^> [0-9a-f]* [ *]*\.\///p' > "$OUT/classes.diff"
echo "CLASSES same=$(comm -12 <(sort "$OUT/before-classes.sha") <(sort "$OUT/after-classes.sha") | wc -l) differ=$(wc -l < "$OUT/classes.diff")"
while read -r class; do
    # a differing class file: is it more than debug line numbers? (javap -c -p prints no LineNumberTable)
    if [ ! -f "$OUT/before-classes/$class" ]; then
        echo "BYTECODE new $class"
    elif diff -q <("$J/javap" -c -p "$OUT/before-classes/$class" | tail -n +2) \
            <("$J/javap" -c -p "$OUT/after-classes/$class" | tail -n +2) > /dev/null; then
        echo "BYTECODE same $class"
    else
        echo "BYTECODE differs $class"
    fi
done < "$OUT/classes.diff"
diff "$OUT/before.dump" "$OUT/after.dump" > "$OUT/dump.diff" || true
lines=$(grep -c '^[<>]' "$OUT/dump.diff" || true)
echo "DIFF_LINES=${lines:-0}"
{ grep '^[<>]' "$OUT/dump.diff" || true; } | awk '{print $1, $2}' | sort | uniq -c | sed 's/^/CLASS /'
mismatch=$(cat "$OUT/before.dump" "$OUT/after.dump" | awk '/^BRUTE_MISMATCH/ {s++} END {print s+0}')
if [ "${lines:-0}" = 0 ] && [ "$mismatch" = 0 ]; then
    echo "DIFFERENTIAL IDENTICAL before_blob=$BLOB corpus=$CORPUS_REV files=$(wc -l < "$OUT/corpus.txt") sha256=$(sha256sum < "$OUT/after.dump" | cut -c1-64)"
else
    echo "DIFFERENTIAL DIFFERS lines=${lines:-0} brute_mismatch_records=$mismatch (see $OUT/dump.diff)"
    exit 1
fi
