#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 of the relocation: the donor plugin (compiled from the donor git tree at the lineage commit, with the donor's
# own service descriptor) and the relocated plugin (target/classes of this module) run as -Xplugin:M3NullnessFacts
# over the same corpus, the Sample fixture plus the generated sources of recipe/generate_fixtures.py, in one javac
# invocation each. The two stderr fact streams must be byte-identical; the relocated plugin under javac -J-Xint must
# repeat its own stream; and the class files of a plugin-free baseline compilation must equal the class files of both
# instrumented compilations byte for byte (the plugin never changes bytecode). Prints DIFFERENTIAL PASS|FAIL with the
# sha256 digests, the fact count and the corpus size.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir>   (JAVA_HOME = JDK 21; run after mvn package)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) winpath() { (cd "$1" && pwd -W); } ;; *) winpath() { (cd "$1" && pwd); } ;; esac
MODULE=$(winpath "$(dirname "$0")/..")
DONOR_GIT=$1
REV=$2
rm -rf "$3/src" "$3/classes" "$3/corpus" "$3/baseline" "$3/donor-out" "$3/relocated-out" "$3/xint-out"
mkdir -p "$3/src" "$3/classes/META-INF/services" "$3/corpus" "$3/baseline" "$3/donor-out" "$3/relocated-out" "$3/xint-out"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
PY=${PYTHON:-python}
COUNT=${CORPUS_COUNT:-200}
SEED=${CORPUS_SEED:-20261106}
MAP="$MODULE/RELOCATION_MAP.tsv"
PLUGIN=M3NullnessFacts
RELOCATED="$MODULE/target/classes"
# the donor sources are the FILE rows whose target is a main source: the plugin, the lattice and the bridge
mapfile -t donor_sources < <(awk -F'\t' '$1=="FILE" && $3 ~ /^src\/main\/java\// {print $2}' "$MAP")
service_path=$(awk -F'\t' '$1=="SERVICE" {print $2; exit}' "$MAP")
fixture_target=$(awk -F'\t' '$1=="FIXTURE" && $3 ~ /Sample\.java$/ {print $3; exit}' "$MAP")
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
for path in "${donor_sources[@]}"; do
    git -C "$DONOR_GIT" show "$REV:$path" > "$SCRATCH/src/$(basename "$path")"
done
# An explicit, initially empty classpath: javac must not discover a service descriptor before its provider exists.
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -proc:none -cp "$SCRATCH/classes" -d "$SCRATCH/classes" "$SCRATCH"/src/*.java
git -C "$DONOR_GIT" show "$REV:$service_path" > "$SCRATCH/classes/META-INF/services/com.sun.source.util.Plugin"
"$PY" -I "$MODULE/recipe/generate_fixtures.py" --out "$SCRATCH/corpus" --count "$COUNT" --seed "$SEED"
cp "$MODULE/$fixture_target" "$SCRATCH/corpus/Sample.java"
mapfile -t corpus < <(find "$SCRATCH/corpus" -name '*.java' | sort)
"$JAVA_BIN/javac" --release 21 -proc:none -d "$SCRATCH/baseline" "${corpus[@]}"
"$JAVA_BIN/javac" --release 21 -proc:none -processorpath "$SCRATCH/classes" -Xplugin:$PLUGIN \
    -d "$SCRATCH/donor-out" "${corpus[@]}" 2> "$SCRATCH/donor.txt"
"$JAVA_BIN/javac" --release 21 -proc:none -processorpath "$RELOCATED" -Xplugin:$PLUGIN \
    -d "$SCRATCH/relocated-out" "${corpus[@]}" 2> "$SCRATCH/relocated.txt"
"$JAVA_BIN/javac" -J-Xint --release 21 -proc:none -processorpath "$RELOCATED" -Xplugin:$PLUGIN \
    -d "$SCRATCH/xint-out" "${corpus[@]}" 2> "$SCRATCH/xint.txt"
donor_sha=$(sha256sum "$SCRATCH/donor.txt" | cut -d' ' -f1)
relocated_sha=$(sha256sum "$SCRATCH/relocated.txt" | cut -d' ' -f1)
xint_sha=$(sha256sum "$SCRATCH/xint.txt" | cut -d' ' -f1)
facts=$(grep -c '^M3_NULLNESS_FACT' "$SCRATCH/relocated.txt" || true)
classfiles=$(find "$SCRATCH/baseline" -name '*.class' | wc -l | tr -d ' ')
echo "DONOR $donor_commit ${donor_sources[*]}"
echo "CORPUS files=${#corpus[@]} classfiles=$classfiles facts=${facts:-0} lines=$(wc -l < "$SCRATCH/relocated.txt")"
status=PASS
cmp -s "$SCRATCH/donor.txt" "$SCRATCH/relocated.txt" || { echo "FACTS donor != relocated"; status=FAIL; }
cmp -s "$SCRATCH/relocated.txt" "$SCRATCH/xint.txt" || { echo "FACTS relocated != relocated -J-Xint"; status=FAIL; }
diff -rq "$SCRATCH/baseline" "$SCRATCH/donor-out" > /dev/null || { echo "CLASSFILES baseline != donor"; status=FAIL; }
diff -rq "$SCRATCH/baseline" "$SCRATCH/relocated-out" > /dev/null || { echo "CLASSFILES baseline != relocated"; status=FAIL; }
diff -rq "$SCRATCH/baseline" "$SCRATCH/xint-out" > /dev/null || { echo "CLASSFILES baseline != relocated -J-Xint"; status=FAIL; }
[[ "${facts:-0}" -gt 0 ]] || { echo "FACTS none emitted"; status=FAIL; }
echo "DIFFERENTIAL $status donor_sha256=$donor_sha relocated_sha256=$relocated_sha xint_sha256=$xint_sha facts=${facts:-0} corpus_files=${#corpus[@]} classfiles=$classfiles"
[[ "$status" == PASS ]]
