#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Differential gates of recipe durable-extent-store-20261107 (run after mvn package of m3/file, which fills
# target/classes and target/test-classes). Donor package, paths and type names come from RELOCATION_MAP.tsv next to
# this script, so no file in the naming scope names the donor package literally.
#  (a) dump: differential/M3ExtentDump (reflection only) over the donor classes (FILE + DONOR_DEP rows compiled from the
#      donor git tree with javac --release 21 -Xlint:all -Werror), over the donor classes with the SCHEMA_ID SUBST rows
#      applied (the jdk_naming schema-id rename, the only identity-domain change), and over target/classes; JIT and
#      -Xint. Gate: the schema-renamed donor and the relocated dumps are byte-identical on both JIT and -Xint, and the
#      raw donor dump differs from the relocated one only on ID lines (values that hash a schema id).
#  (b) crash: differential/M3ExtentCrash over the schema-renamed donor and the relocated classes; both print CRASH PASS
#      (no torn index after a kill before every step) and the two outputs are byte-identical.
#  (c) proof: the donor ExtentStandaloneProof and the relocated M3ExtentStandaloneProof, run on both sides; the outputs
#      must agree (PASS on Linux; on Windows both stop where the proof truncates a blob it mapped).
#  (d) javap -p of the donor and relocated classes, package and type names masked by the TYPE rows: identical except
#      the HELD overload open(Path, StorageOptions) and the HELD field readOptions (the DROP / DROP_BLOCK rows).
#  (e) mutants: every MUTANTS.tsv row applied to a copy of the relocated store, compiled, and run against the
#      relocated JUnit classes (console launcher), the dump and the crash gate; each mutant must be killed.
# Prints DIFFERENTIAL PASS|FAIL.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> <junit-platform-console-standalone jar>
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
mkdir -p "$3"
SCRATCH=$(winpath "$3")
CONSOLE=$4
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
TMP="-Djava.io.tmpdir=$SCRATCH/tmp"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
rm -rf "$SCRATCH"/{donor-src,donor-m3-src,donor-classes,donor-m3-classes,harness,proof-d,proof-r,work,mut,tmp}
mkdir -p "$SCRATCH"/{donor-classes,donor-m3-classes,harness,proof-d,proof-r,work,mut,tmp}
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
donor_dir=$SCRATCH/donor-src/${donor_package//.//}
mkdir -p "$donor_dir"
mapfile -t main_paths < <(awk -F'\t' '($1=="FILE" && $3 ~ /^src\/main\//) || $1=="DONOR_DEP" {print $2}' "$MAP")
proof_path=$(awk -F'\t' '$1=="FILE" && $3 ~ /StandaloneProof\.java$/ {print $2; exit}' "$MAP")
mapfile -t donor_tests < <(awk -F'\t' '$1=="FILE" && $3 ~ /^src\/test\// {print $2}' "$MAP")
for path in "${main_paths[@]}" "$proof_path" "${donor_tests[@]}"; do
    git -C "$DONOR_GIT" show "$REV:$path" > "$donor_dir/$(basename "$path")"
done
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
type_of() { awk -F'\t' -v t="$1" '$1=="TYPE" && $3==t {print $2; exit}' "$MAP"; }
d_store=$(type_of M3DurableExtentStore); d_extent=$(type_of M3ContentExtent); d_proof=$(type_of M3ExtentStandaloneProof)
# The schema-renamed donor: the SCHEMA_ID SUBST rows of the map applied to a copy of the donor extent class.
cp -r "$SCRATCH/donor-src" "$SCRATCH/donor-m3-src"
python -I - "$MAP" "$SCRATCH/donor-m3-src/${donor_package//.//}/$d_extent.java" <<'PY'
import sys
rows = [line.split("\t") for line in open(sys.argv[1], encoding="utf-8").read().splitlines()
        if line.startswith("SUBST\t") and "\tSCHEMA_ID:" in line]
path = sys.argv[2]
text = open(path, encoding="utf-8", newline="").read()
for row in rows:
    if text.count(row[1]) != 1:
        sys.exit("SCHEMA_ID row not found exactly once: " + row[1])
    text = text.replace(row[1], row[2])
open(path, "w", encoding="utf-8", newline="").write(text)
print("SCHEMA_ID rows applied to the donor copy: %d" % len(rows))
PY
mains=(); for path in "${main_paths[@]}"; do mains+=("$(basename "$path")"); done
(cd "$donor_dir" && "$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/donor-classes" "${mains[@]}")
(cd "$SCRATCH/donor-m3-src/${donor_package//.//}" && "$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/donor-m3-classes" "${mains[@]}")
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/harness" "$RECIPE_DIR/differential/M3ExtentDump.java" "$RECIPE_DIR/differential/M3ExtentCrash.java"
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -cp "$SCRATCH/donor-classes" -d "$SCRATCH/proof-d" "$donor_dir/$d_proof.java"
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -cp "$MODULE/target/classes" -d "$SCRATCH/proof-r" "$RECIPE_DIR/differential/M3ExtentStandaloneProof.java"
echo "DONOR $donor_commit package=$donor_package files=${#main_paths[@]} store=$d_store extent=$d_extent"
MAIN="$MODULE/target/classes"
TESTS="$MODULE/target/test-classes"
status=PASS
digest() { sha256sum "$1" | cut -d' ' -f1; }
same() {
    if cmp -s "$1" "$2"; then echo "$3 IDENTICAL sha256=$(digest "$1") lines=$(wc -l < "$1")"
    else echo "$3 DIFFERENT left=$(digest "$1") right=$(digest "$2")"; status=FAIL; fi
}
dump() {  # dump <label> <classes> <store> <extent> [jvm flags]
    local label=$1 classes=$2 st=$3 ex=$4; shift 4
    "$JAVA_BIN/java" "$TMP" "$@" -cp "$classes$SEP$SCRATCH/harness" extent.differential.M3ExtentDump "$st" "$ex" "$SCRATCH/work/$label" > "$SCRATCH/dump-$label.txt"
}
# (a) dumps
dump donor "$SCRATCH/donor-classes" "$donor_package.$d_store" "$donor_package.$d_extent"
dump donor-m3 "$SCRATCH/donor-m3-classes" "$donor_package.$d_store" "$donor_package.$d_extent"
dump donor-m3-xint "$SCRATCH/donor-m3-classes" "$donor_package.$d_store" "$donor_package.$d_extent" -Xint
dump relocated "$MAIN" "$target_package.M3DurableExtentStore" "$target_package.M3ContentExtent"
dump relocated-xint "$MAIN" "$target_package.M3DurableExtentStore" "$target_package.M3ContentExtent" -Xint
same "$SCRATCH/dump-donor-m3.txt" "$SCRATCH/dump-relocated.txt" "dump donor-m3/relocated"
same "$SCRATCH/dump-relocated.txt" "$SCRATCH/dump-relocated-xint.txt" "dump relocated/relocated-xint"
same "$SCRATCH/dump-donor-m3.txt" "$SCRATCH/dump-donor-m3-xint.txt" "dump donor-m3/donor-m3-xint"
grep -v '^ID ' "$SCRATCH/dump-donor.txt" > "$SCRATCH/dump-donor.op"; grep -v '^ID ' "$SCRATCH/dump-relocated.txt" > "$SCRATCH/dump-relocated.op"
same "$SCRATCH/dump-donor.op" "$SCRATCH/dump-relocated.op" "dump raw-donor/relocated OP lines"
id_diff=$(diff <(grep '^ID ' "$SCRATCH/dump-donor.txt") <(grep '^ID ' "$SCRATCH/dump-relocated.txt") | grep -c '^>' || true)
echo "dump raw-donor/relocated ID lines=$(grep -c '^ID ' "$SCRATCH/dump-relocated.txt") differing=${id_diff:-0} (schema-id domain) OP lines=$(wc -l < "$SCRATCH/dump-relocated.op")"
# donor JUnit on the donor classes (reference counts for the relocated surefire run)
mkdir -p "$SCRATCH/donor-tests"
tests=(); for path in "${donor_tests[@]}"; do tests+=("$donor_dir/$(basename "$path")"); done
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -cp "$SCRATCH/donor-classes$SEP$CONSOLE" -d "$SCRATCH/donor-tests" "${tests[@]}"
"$JAVA_BIN/java" "$TMP" -jar "$CONSOLE" execute --disable-banner --details=summary --disable-ansi-colors \
    -cp "$SCRATCH/donor-classes$SEP$SCRATCH/donor-tests" --select-package "$donor_package" > "$SCRATCH/donor-junit.txt" 2>&1 || status=FAIL
echo "donor JUnit on donor classes: $(grep -E 'tests successful' "$SCRATCH/donor-junit.txt" | tr -s ' ') $(grep -E 'tests failed' "$SCRATCH/donor-junit.txt" | tr -s ' ')"
# (b) crash
"$JAVA_BIN/java" "$TMP" -cp "$SCRATCH/donor-m3-classes$SEP$SCRATCH/harness" extent.differential.M3ExtentCrash parent "$donor_package.$d_store" "$donor_package.$d_extent" "$SCRATCH/work/crash-donor" > "$SCRATCH/crash-donor.txt"
"$JAVA_BIN/java" "$TMP" -cp "$MAIN$SEP$SCRATCH/harness" extent.differential.M3ExtentCrash parent "$target_package.M3DurableExtentStore" "$target_package.M3ContentExtent" "$SCRATCH/work/crash-relocated" > "$SCRATCH/crash-relocated.txt"
same "$SCRATCH/crash-donor.txt" "$SCRATCH/crash-relocated.txt" "crash donor-m3/relocated"
tail -1 "$SCRATCH/crash-relocated.txt"
grep -q '^CRASH PASS' "$SCRATCH/crash-relocated.txt" || status=FAIL
# (c) standalone proof
proof() { "$JAVA_BIN/java" "$TMP" -cp "$1" "$2" 2>&1 | sed -E -e 's#[A-Za-z]:[\\/][^ ]*m3-extent-proof-[0-9]+#<tmp>#g' -e 's#/tmp/m3-extent-proof-[0-9]+#<tmp>#g' -e "s#$donor_package\\.##g" -e "s#$target_package\\.##g" -e 's#M3(DurableExtentStore|ContentExtent|ExtentStandaloneProof)#\1#g' -e 's#\(([A-Za-z]+)\.java:[0-9]+\)#(\1.java)#g' | grep -vE '^\s+at (java|jdk)\.' || true; }
proof "$SCRATCH/donor-classes$SEP$SCRATCH/proof-d" "$donor_package.$d_proof" > "$SCRATCH/proof-donor.txt"
proof "$MAIN$SEP$SCRATCH/proof-r" "$target_package.M3ExtentStandaloneProof" > "$SCRATCH/proof-relocated.txt"
same "$SCRATCH/proof-donor.txt" "$SCRATCH/proof-relocated.txt" "proof donor/relocated"
echo "proof relocated: $(head -1 "$SCRATCH/proof-relocated.txt")"
# (d) javap -p
mask() { sed -E -e $'s/\r$//' -e "s#$donor_package\\.##g" -e "s#$target_package\\.##g" -e 's#M3(DurableExtentStore|ContentExtent)#\1#g' -e 's#^Compiled from .*##'; }
: > "$SCRATCH/javap-donor.txt"; : > "$SCRATCH/javap-relocated.txt"
for cls in "$d_store" "$d_store\$BeforePublish" "$d_extent" "$d_extent\$ContentExtentManifest" "$d_extent\$ContentTamperedException"; do
    "$JAVA_BIN/javap" -p -cp "$SCRATCH/donor-classes" "$donor_package.$cls" | mask >> "$SCRATCH/javap-donor.txt"
    "$JAVA_BIN/javap" -p -cp "$MAIN" "$target_package.M3$cls" | mask >> "$SCRATCH/javap-relocated.txt"
done
javap_diff=$(diff "$SCRATCH/javap-donor.txt" "$SCRATCH/javap-relocated.txt" | grep -E '^[<>]' || true)
# The declared differences: the HELD field and overload, the constructor without the HELD parameter, and the
# synthetic lambda of the HELD overload's no-op failpoint.
expected=$'<   private final StorageOptions readOptions;\n<   public static DurableExtentStore open(java.nio.file.Path, StorageOptions) throws java.io.IOException;\n<   private DurableExtentStore(java.nio.file.Path, StorageOptions, DurableExtentStore$BeforePublish) throws java.io.IOException;\n>   private DurableExtentStore(java.nio.file.Path, DurableExtentStore$BeforePublish) throws java.io.IOException;\n<   private static void lambda$open$1() throws java.io.IOException;'
if [ "$javap_diff" == "$expected" ]; then echo "javap -p IDENTICAL except the declared HELD members (5 diff lines; relocated lines=$(wc -l < "$SCRATCH/javap-relocated.txt"))"
else echo "javap -p DIFFERENT:"; echo "$javap_diff"; status=FAIL; fi
# (e) mutants
reloc_src="$MODULE/src/main/java/${target_package//.//}"
mapfile -t mutant_ids < <(awk -F'\t' 'NR>1 && $1 ~ /^M[0-9]+$/ {print $1}' "$RECIPE_DIR/MUTANTS.tsv")
for id in "${mutant_ids[@]}"; do
    m="$SCRATCH/mut/$id"; mkdir -p "$m/src" "$m/classes"
    cp "$reloc_src/M3DurableExtentStore.java" "$reloc_src/M3ContentExtent.java" "$m/src/"
    python -I - "$RECIPE_DIR/MUTANTS.tsv" "$id" "$m/src/M3DurableExtentStore.java" <<'PY'
import sys
row = next(line.split("\t") for line in open(sys.argv[1], encoding="utf-8").read().splitlines()
           if line.startswith(sys.argv[2] + "\t"))
before, after = row[1].replace("\\n", "\n"), row[2].replace("\\n", "\n")
path = sys.argv[3]
text = open(path, encoding="utf-8", newline="").read().replace("\r\n", "\n")
if text.count(before) != 1:
    sys.exit("mutant %s: text found %d times" % (sys.argv[2], text.count(before)))
open(path, "w", encoding="utf-8", newline="\n").write(text.replace(before, after))
PY
    "$JAVA_BIN/javac" --release 21 -nowarn -d "$m/classes" "$m/src/M3DurableExtentStore.java" "$m/src/M3ContentExtent.java"
    junit=SURVIVED
    "$JAVA_BIN/java" "$TMP" -jar "$CONSOLE" execute --disable-banner --details=summary --disable-ansi-colors -cp "$m/classes$SEP$TESTS" \
        --select-class "$target_package.M3DurableExtentStoreTest" --select-class "$target_package.M3DurableExtentStoreOracleTest" \
        --select-class "$target_package.M3ContentExtentTest" > "$m/junit.txt" 2>&1 || junit=KILLED
    failed=$(grep -E 'tests failed' "$m/junit.txt" | grep -oE '[0-9]+' | head -1)
    dump "mut-$id" "$m/classes" "$target_package.M3DurableExtentStore" "$target_package.M3ContentExtent" || true
    if cmp -s "$SCRATCH/dump-mut-$id.txt" "$SCRATCH/dump-relocated.txt"; then dumpv=SURVIVED; else dumpv=KILLED; fi
    "$JAVA_BIN/java" "$TMP" -cp "$m/classes$SEP$SCRATCH/harness" extent.differential.M3ExtentCrash parent "$target_package.M3DurableExtentStore" "$target_package.M3ContentExtent" "$SCRATCH/work/crash-$id" > "$m/crash.txt" 2>&1 || true
    if grep -q '^CRASH PASS' "$m/crash.txt"; then crashv=SURVIVED; else crashv="KILLED($(tail -1 "$m/crash.txt"))"; fi
    if [ "$junit$dumpv$crashv" == "SURVIVEDSURVIVEDSURVIVED" ]; then verdict=SURVIVED; status=FAIL; else verdict=KILLED; fi
    echo "mutant $id $verdict junit=$junit(failed=${failed:-0}) dump=$dumpv crash=$crashv"
done
echo "DIFFERENTIAL $status donor=$donor_commit"
[ "$status" == PASS ]
