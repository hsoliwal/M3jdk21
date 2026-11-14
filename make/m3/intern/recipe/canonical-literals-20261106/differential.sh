#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gates 2-4 of recipe canonical-literals-20261106 against the donor tree (run after mvn package of m3/intern).
#   donor test  the donor pool with its own dependency closure (the CLOSURE commit of RELOCATION_MAP.tsv: the CLOSURE_PATH
#               SOURCE roots on javac -sourcepath, the JAR / PROCESSOR paths of the local Maven repository) and the donor
#               M3CanonicalLiteralPoolTest compiled from that tree; the JUnit Platform console (LAUNCHER) runs it: 10/10
#   dump        M3CanonicalLiteralPoolDump (test scope of m3/intern) compiled a second time for the donor with its package
#               line set to the donor package and the SIDE lines below replaced (each must occur exactly once); donor and
#               relocated dumps byte-identical under the default JIT and under -Xint (four runs, RUN_LIMIT seconds each),
#               every run TOTAL failures=0 (handle relation = reference-key relation, payload read-back = references)
#   api         javap -public of the donor pool equals the relocated pool after the PACKAGE and TYPE maps
# Prints DIFFERENTIAL PASS|FAIL with the sha256 of every compared output. The donor package, source roots and jars come
# from RELOCATION_MAP.tsv next to this script, so no file in the naming scope names them literally.
# usage: differential.sh <donor-git-dir> <scratch-dir> <maven-repo-local> [random-operations]   (JAVA_HOME = JDK 21)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
OPERATIONS=${4:-100000}
RUN_LIMIT=120
rm -rf "$2/src" "$2/classes" "$2/test-classes" "$2/dump-src" "$2/dump-classes" "$2/out" "$2/javap"
mkdir -p "$2/src" "$2/classes" "$2/test-classes" "$2/dump-src" "$2/dump-classes" "$2/out" "$2/javap"
SCRATCH=$(winpath "$2")
M2=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
row() { awk -F'\t' -v k="$1" '$1==k {print $'"$2"'}' "$MAP"; }
donor_package=$(row PACKAGE 2 | head -1)
target_package=$(row PACKAGE 3 | head -1)
repo_commit=$(git -C "$DONOR_GIT" rev-parse "$(row REPO 3)^{commit}")
closure=$(git -C "$DONOR_GIT" rev-parse "$(row CLOSURE 3)^{commit}")
mapfile -t files < <(row FILE 2)
mapfile -t roots < <(awk -F'\t' '$1=="CLOSURE_PATH" && $3=="SOURCE" {print $2}' "$MAP")
jar_list() { awk -F'\t' -v k="$1" '$1=="CLOSURE_PATH" && $3==k {print $2}' "$MAP" | while read -r p; do printf '%s%s/%s' "$SEP" "$M2" "$p"; done | cut -c2-; }
JARS="$(jar_list JAR)$SEP$(jar_list PROCESSOR)"
PROCESSOR=$(jar_list PROCESSOR)
LAUNCHER=$(jar_list LAUNCHER)
for path in "${files[@]}"; do
    [ "$(git -C "$DONOR_GIT" rev-parse "$closure:$path")" = "$(git -C "$DONOR_GIT" rev-parse "$repo_commit:$path")" ] \
        || { echo "DIFFERENTIAL FAIL: $path differs between CLOSURE and REPO"; exit 1; }
done
pool_path=$(printf '%s\n' "${files[@]}" | grep '/src/main/')
test_path=$(printf '%s\n' "${files[@]}" | grep '/src/test/')
git -C "$DONOR_GIT" archive "$closure" "${roots[@]}" "$test_path" | tar -x -C "$SCRATCH/src"
sourcepath=$(for r in "${roots[@]}"; do printf '%s%s/src/%s' "$SEP" "$SCRATCH" "$r"; done | cut -c2-)
echo "DONOR repo=$repo_commit closure=$closure roots=${#roots[@]} operations=$OPERATIONS"
"$JAVA_BIN/javac" --release 21 -nowarn -sourcepath "$sourcepath" -cp "$JARS" -processorpath "$PROCESSOR" \
    -d "$SCRATCH/classes" "$SCRATCH/src/$pool_path" > "$SCRATCH/out/javac-donor.log" 2>&1 \
    || { tail -20 "$SCRATCH/out/javac-donor.log"; echo "DIFFERENTIAL FAIL: donor closure"; exit 1; }
echo "donor closure classes=$(find "$SCRATCH/classes" -name '*.class' | wc -l)"
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -cp "$SCRATCH/classes$SEP$LAUNCHER" -d "$SCRATCH/test-classes" "$SCRATCH/src/$test_path"
test_class="$donor_package.$(basename "$test_path" .java)"
timeout "$RUN_LIMIT" "$JAVA_BIN/java" -jar "$LAUNCHER" execute --disable-banner --details=summary --disable-ansi-colors \
    --class-path "$SCRATCH/classes$SEP$SCRATCH/test-classes" --select-class "$test_class" > "$SCRATCH/out/donor-test.txt" 2>&1 || true
donor_ok=$(grep -oE '[0-9]+ tests successful' "$SCRATCH/out/donor-test.txt" | grep -oE '^[0-9]+')
donor_failed=$(grep -oE '[0-9]+ tests failed' "$SCRATCH/out/donor-test.txt" | grep -oE '^[0-9]+')
status=PASS
[ "$donor_ok" = 10 ] && [ "$donor_failed" = 0 ] || status=FAIL
echo "donor-test $test_class successful=$donor_ok failed=$donor_failed"
# SIDE lines: relocated line <TAB> donor line (the donor names the space M3InternableSpace and its frozen store
# MIndexAtomStore; TYPE / PATH / int-sequence entries are its type, path and sequenceInts domains)
python -I - "$MODULE/src/test/java/${target_package//.//}/M3CanonicalLiteralPoolDump.java" \
    "$SCRATCH/dump-src/${donor_package//.//}/M3CanonicalLiteralPoolDump.java" "$target_package" "$donor_package" <<'EOF'
import sys
from pathlib import Path
SIDE = """\
package {target};\tpackage {donor};
        M3InternSpace space = new M3InternSpace();\t        M3InternableSpace space = M3InternableSpace.create();
        space.freeze();\t        MIndexAtomStore store = space.freeze();
                byte[] payload = space.blobs().copy(space.shapes().members(handle)[1]);\t                byte[] payload = store.bytes((int) handle);
    private static long admit(M3InternSpace space, M3CanonicalLiteralPool pool, Op op) {\t    private static long admit(M3InternableSpace space, M3CanonicalLiteralPool pool, Op op) {
            case TYPE -> space.internType(op.scope(), op.text());\t            case TYPE -> space.type(op.scope(), op.text());
            case PATH -> space.internPath(op.scope(), op.text());\t            case PATH -> space.path(op.scope(), op.text());
            case INTS -> space.internFrozenInts(new int[] {(int) op.bits()});\t            case INTS -> space.sequenceInts(0, new int[] {(int) op.bits()});
"""
source, target_file, target, donor = sys.argv[1:5]
lines = Path(source).read_bytes().decode("utf-8").split("\n")
for row in SIDE.replace("{target}", target).replace("{donor}", donor).splitlines():
    old, new = row.split("\t")
    hits = [i for i, line in enumerate(lines) if line == old]
    if len(hits) != 1:
        sys.exit("SIDE line matched %d times: %s" % (len(hits), old.strip()))
    lines[hits[0]] = new
Path(target_file).parent.mkdir(parents=True, exist_ok=True)
Path(target_file).write_bytes("\n".join(lines).encode("utf-8"))
print("SIDE lines=%d" % len(SIDE.splitlines()))
EOF
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -cp "$SCRATCH/classes" -d "$SCRATCH/dump-classes" \
    "$SCRATCH/dump-src/${donor_package//.//}/M3CanonicalLiteralPoolDump.java"
DONOR_CP="$SCRATCH/classes$SEP$SCRATCH/dump-classes"
RELOCATED_CP="$MODULE/target/classes$SEP$MODULE/target/test-classes"
compare() { # label
    local label=$1 verdict donor_sha relocated_sha
    donor_sha=$(sha256sum "$SCRATCH/out/donor-$label.txt" | cut -d' ' -f1)
    relocated_sha=$(sha256sum "$SCRATCH/out/relocated-$label.txt" | cut -d' ' -f1)
    if cmp -s "$SCRATCH/out/donor-$label.txt" "$SCRATCH/out/relocated-$label.txt"; then verdict=IDENTICAL; else verdict=DIFFERENT; status=FAIL; fi
    echo "$label $verdict sha256=$relocated_sha donor_sha256=$donor_sha bytes=$(wc -c < "$SCRATCH/out/relocated-$label.txt") lines=$(wc -l < "$SCRATCH/out/relocated-$label.txt")"
}
timed() { # side label classpath java-args...
    local side=$1 label=$2 classpath=$3 started elapsed; shift 3
    started=$(date +%s)
    timeout "$RUN_LIMIT" "$JAVA_BIN/java" -cp "$classpath" "$@" > "$SCRATCH/out/$side-$label.txt" || status=FAIL
    elapsed=$(( $(date +%s) - started ))
    echo "RUN $side $label ${elapsed}s $(grep '^TOTAL ' "$SCRATCH/out/$side-$label.txt")"
    grep -q '^TOTAL .* failures=0$' "$SCRATCH/out/$side-$label.txt" || status=FAIL
}
for mode in default xint; do
    flags=(); [ "$mode" = xint ] && flags=(-Xint)
    timed donor "dump-$mode" "$DONOR_CP" "${flags[@]}" "$donor_package.M3CanonicalLiteralPoolDump" "$OPERATIONS"
    timed relocated "dump-$mode" "$RELOCATED_CP" "${flags[@]}" "$target_package.M3CanonicalLiteralPoolDump" "$OPERATIONS"
    compare "dump-$mode"
done
if cmp -s "$SCRATCH/out/relocated-dump-default.txt" "$SCRATCH/out/relocated-dump-xint.txt"; then echo "dump default-vs-xint IDENTICAL"; else echo "dump default-vs-xint DIFFERENT"; status=FAIL; fi
grep -vE '^E ' "$SCRATCH/out/relocated-dump-default.txt"
echo "classes E=$(grep -c '^E ' "$SCRATCH/out/relocated-dump-default.txt") sha256(E lines)=$(grep '^E ' "$SCRATCH/out/relocated-dump-default.txt" | sha256sum | cut -d' ' -f1)"
retarget() { # the PACKAGE and TYPE maps as literal substitutions on every line
    local line
    while IFS= read -r line; do
        line=${line//"$donor_package"/$target_package}
        while IFS=$'\t' read -r from to; do line=${line//"$from"/$to}; done < <(awk -F'\t' '$1=="TYPE" && $2!=$3 {print $2"\t"$3}' "$MAP")
        printf '%s\n' "$line"
    done
}
pool=$(basename "$pool_path" .java)
"$JAVA_BIN/javap" -public -cp "$SCRATCH/classes" "$donor_package.$pool" | retarget > "$SCRATCH/javap/donor-$pool.txt"
"$JAVA_BIN/javap" -public -cp "$MODULE/target/classes" "$target_package.$pool" > "$SCRATCH/javap/relocated-$pool.txt"
if diff --strip-trailing-cr -q "$SCRATCH/javap/donor-$pool.txt" "$SCRATCH/javap/relocated-$pool.txt" > /dev/null; then
    echo "api javap -public $pool IDENTICAL lines=$(wc -l < "$SCRATCH/javap/relocated-$pool.txt")"
else
    echo "api javap -public $pool DIFFERENT"; diff --strip-trailing-cr "$SCRATCH/javap/donor-$pool.txt" "$SCRATCH/javap/relocated-$pool.txt" || true; status=FAIL
fi
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
