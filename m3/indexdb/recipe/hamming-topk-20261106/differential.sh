#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 of the relocation: the donor top-k selector, its distance primitive (the DONOR_DEP row) and its oracle,
# compiled from the donor git tree with javac --release 21 -Xlint:all -Werror, against the relocated class
# (target/classes of m3/indexdb) and its oracle (target/test-classes) over the same inputs.
# (a) Both oracle mains print byte-identical output (M3_TOPK_HEAPSORT_DIFFERENTIAL_PASS cases=35032 comparisons=1439283).
# (b) M3HammingTopKDump (test scope) replays the oracle inputs and 100,000 seeded fingerprint sets through both
#     classes; the dumps must be byte-identical (this also proves the MEMBER mapping of the distance primitive).
# (c) The dump of the tie-order mutant (MUTANT row) must differ from the relocated dump, first at the oracle's
#     trial 0 k=2 n=2048, and the oracle source renamed onto the mutant (scratch copy, never committed) must fail
#     with "topk oracle failure k=2 n=2048".
# (d) fingerprint_nec: the donor fingerprint(long[]) and M3IndexDbChunkSimHash.fromFeatures(...).simHash64() over
#     20,000 seeded unit-weight windows: equal where no bit is tied (and on the empty window) and different exactly
#     on the tie mask elsewhere (the donor votes 0 on a tie, M3IndexDbChunkSimHash votes 1); unexplained=0 passes.
# (e) javap -public of the donor owner and its Match record (names mapped by sed, CR stripped on both sides: javap
#     writes CRLF on Windows) equals the relocated class's, and the SimHash owner exports the static
#     hammingDistance(long, long) the MEMBER row maps to.
# Prints DIFFERENTIAL PASS|FAIL with the sha256 digests of both sides. Donor package and type names come from
# RELOCATION_MAP.tsv next to this script, so no file in the naming scope names a donor package literally.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir>   (JAVA_HOME = JDK 21; run after mvn package)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
mkdir -p "$3/src" "$3/classes" "$3/mutant-src" "$3/mutant-classes"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
owner_path=$(awk -F'\t' '$1=="FILE" && $3 ~ /src\/main\/java\// {print $2; exit}' "$MAP")
oracle_path=$(awk -F'\t' '$1=="FILE" && $3 ~ /Contract\.java$/ {print $2; exit}' "$MAP")
dep_path=$(awk -F'\t' '$1=="DONOR_DEP" {print $2; exit}' "$MAP")
mutant_target=$(awk -F'\t' '$1=="MUTANT" {print $3; exit}' "$MAP")
donor_owner=$(basename "$owner_path" .java)
donor_oracle=$(basename "$oracle_path" .java)
donor_dep=$(basename "$dep_path" .java)
target_owner=$(awk -F'\t' -v d="$donor_owner" '$1=="TYPE" && $2==d && $4=="" {print $3; exit}' "$MAP")
target_oracle=$(awk -F'\t' -v d="$donor_oracle" '$1=="TYPE" && $2==d && $4=="" {print $3; exit}' "$MAP")
target_mutant=$(basename "$mutant_target" .java)
member_owner=$(awk -F'\t' '$1=="MEMBER" {split($3, a, "."); print a[1]; exit}' "$MAP")
member_name=$(awk -F'\t' '$1=="MEMBER" {split($3, a, "."); print a[2]; exit}' "$MAP")
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
for path in "$owner_path" "$oracle_path" "$dep_path"; do
    git -C "$DONOR_GIT" show "$REV:$path" > "$SCRATCH/src/$(basename "$path")"
done
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/classes" "$SCRATCH/src/$donor_owner.java" "$SCRATCH/src/$donor_dep.java" "$SCRATCH/src/$donor_oracle.java"
echo "DONOR $donor_commit $owner_path $dep_path $oracle_path"
MAIN="$MODULE/target/classes"
TEST="$MODULE/target/test-classes"
DUMP="$target_package.M3HammingTopKDump"
"$JAVA_BIN/java" -cp "$SCRATCH/classes" "$donor_package.$donor_oracle" > "$SCRATCH/donor-oracle.txt"
"$JAVA_BIN/java" -cp "$MAIN$SEP$TEST" "$target_package.$target_oracle" > "$SCRATCH/relocated-oracle.txt"
"$JAVA_BIN/java" -cp "$SCRATCH/classes$SEP$TEST$SEP$MAIN" "$DUMP" "$donor_package.$donor_owner" > "$SCRATCH/donor-dump.txt"
"$JAVA_BIN/java" -cp "$MAIN$SEP$TEST" "$DUMP" "$target_package.$target_owner" > "$SCRATCH/relocated-dump.txt"
"$JAVA_BIN/java" -cp "$MAIN$SEP$TEST" "$DUMP" "$target_package.$target_mutant" > "$SCRATCH/mutant-dump.txt"
"$JAVA_BIN/java" -cp "$SCRATCH/classes$SEP$TEST$SEP$MAIN" "$DUMP" --fingerprint "$donor_package.$donor_dep" > "$SCRATCH/donor-fingerprints.txt"
"$JAVA_BIN/java" -cp "$MAIN$SEP$TEST" "$DUMP" --fingerprint "$target_package.$member_owner" > "$SCRATCH/relocated-fingerprints.txt"
"$JAVA_BIN/javap" -public -cp "$SCRATCH/classes" "$donor_package.$donor_owner" "$donor_package.$donor_owner\$Match" \
    | tr -d '\r' | sed -e "s/\b$donor_owner\b/$target_owner/g" -e "s/$donor_package/$target_package/g" > "$SCRATCH/donor-javap.txt"
"$JAVA_BIN/javap" -public -cp "$MAIN" "$target_package.$target_owner" "$target_package.$target_owner\$Match" | tr -d '\r' > "$SCRATCH/relocated-javap.txt"
status=PASS
compare() {
    local donor_sha relocated_sha verdict
    donor_sha=$(sha256sum "$SCRATCH/donor-$1.txt" | cut -d' ' -f1)
    relocated_sha=$(sha256sum "$SCRATCH/relocated-$1.txt" | cut -d' ' -f1)
    if cmp -s "$SCRATCH/donor-$1.txt" "$SCRATCH/relocated-$1.txt"; then verdict=IDENTICAL; else verdict=DIFFERENT; status=FAIL; fi
    echo "$1 $verdict donor_sha256=$donor_sha relocated_sha256=$relocated_sha bytes=$(wc -c < "$SCRATCH/donor-$1.txt") lines=$(wc -l < "$SCRATCH/donor-$1.txt")"
}
compare oracle
compare dump
compare javap
mutant_sha=$(sha256sum "$SCRATCH/mutant-dump.txt" | cut -d' ' -f1)
if cmp -s "$SCRATCH/relocated-dump.txt" "$SCRATCH/mutant-dump.txt"; then
    echo "mutant-dump IDENTICAL mutant_sha256=$mutant_sha (unexpected: the mutant must differ)"; status=FAIL
else
    first=$(diff "$SCRATCH/relocated-dump.txt" "$SCRATCH/mutant-dump.txt" | grep -m1 '^<' | cut -d' ' -f2-4 || true)
    differing=$(diff "$SCRATCH/relocated-dump.txt" "$SCRATCH/mutant-dump.txt" | grep -c '^<' || true)
    echo "mutant-dump DIFFERENT first=$first differing_lines=${differing:-0} mutant_sha256=$mutant_sha"
    [ "$first" = "T0 k=2 n=2048" ] || status=FAIL
fi
sed -e "s/\b$target_oracle\b/${target_oracle}OnMutant/g" -e "s/\b$target_owner\b/$target_mutant/g" \
    "$MODULE/src/test/java/${target_package//.//}/$target_oracle.java" > "$SCRATCH/mutant-src/${target_oracle}OnMutant.java"
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -cp "$MAIN$SEP$TEST" -d "$SCRATCH/mutant-classes" "$SCRATCH/mutant-src/${target_oracle}OnMutant.java"
if "$JAVA_BIN/java" -cp "$SCRATCH/mutant-classes$SEP$MAIN$SEP$TEST" "$target_package.${target_oracle}OnMutant" > "$SCRATCH/mutant-oracle.txt" 2>&1; then
    echo "mutant-oracle ACCEPTED (unexpected: the oracle must refuse the tie-order mutant)"; status=FAIL
elif grep -q 'topk oracle failure k=2 n=2048' "$SCRATCH/mutant-oracle.txt"; then
    echo "mutant-oracle REFUSED signature=topk_oracle_failure_k2_n2048"
else
    echo "mutant-oracle REFUSED with another signature: $(head -c 200 "$SCRATCH/mutant-oracle.txt")"; status=FAIL
fi
if grep -q "public static int $member_name(long, long);" "$SCRATCH/relocated-javap.txt" 2>/dev/null || \
   "$JAVA_BIN/javap" -public -cp "$MAIN" "$target_package.$member_owner" | grep -q "public static int $member_name(long, long);"; then
    echo "member-owner $target_package.$member_owner exports static int $member_name(long, long)"
else
    echo "member-owner $target_package.$member_owner lacks static int $member_name(long, long)"; status=FAIL
fi
fingerprint_sha_donor=$(sha256sum "$SCRATCH/donor-fingerprints.txt" | cut -d' ' -f1)
fingerprint_sha_relocated=$(sha256sum "$SCRATCH/relocated-fingerprints.txt" | cut -d' ' -f1)
if python -I - "$SCRATCH/donor-fingerprints.txt" "$SCRATCH/relocated-fingerprints.txt" <<'PY'
import sys
donor, m3 = [open(path, encoding="utf-8").read().splitlines() for path in sys.argv[1:3]]
if len(donor) != len(m3) or not donor or not donor[-1].startswith("TOTAL"):
    raise SystemExit("fingerprint dumps differ in shape")
equal = tied = other = 0
for a, b in zip(donor[:-1], m3[:-1]):
    fa = dict(t.split("=") for t in a.split()[1:])
    fb = dict(t.split("=") for t in b.split()[1:])
    n, mask = int(fa["n"]), int(fa["tiemask"], 16)
    x, y = int(fa["fp"], 16), int(fb["fp"], 16)
    if fa["n"] != fb["n"] or fa["tiemask"] != fb["tiemask"]:
        other += 1
    elif x == y and (mask == 0 or n == 0):
        equal += 1
    elif n > 0 and mask != 0 and (x ^ y) == mask:
        tied += 1
    else:
        other += 1
print("fingerprint_nec %s windows=%d equal_untied_or_empty=%d differ_exactly_on_tiemask=%d unexplained=%d"
      % (donor[-1], len(donor) - 1, equal, tied, other))
sys.exit(1 if other else 0)
PY
then echo "fingerprint_nec EXPLAINED donor_sha256=$fingerprint_sha_donor relocated_sha256=$fingerprint_sha_relocated"
else echo "fingerprint_nec UNEXPLAINED donor_sha256=$fingerprint_sha_donor relocated_sha256=$fingerprint_sha_relocated"; status=FAIL
fi
tr '\n' ' ' < "$SCRATCH/relocated-oracle.txt"; echo
tail -n 1 "$SCRATCH/relocated-dump.txt"
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
