#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gates 2-5 of the hash-family relocation, after mvn package of m3/indexdb (target/classes, target/test-classes):
# (a) donor build: every main source of the crate (FILE and SKIP rows) and the three relocated contracts' donor files,
#     javac --release 21 -Xlint:all -Werror, from the donor git tree into the scratch directory.
# (b) crate figures: the donor contracts and the relocated contracts over the crate's oracle corpus (corpus.py output:
#     403 files, LHASH_ORACLE.tsv), default VM and -Xint; the INFO lines must be identical and equal the crate figures.
# (c) dumps: M3HashFamilyDump (test tree) compiled against the relocated classes as is and against the donor crate with
#     the TYPE rows reversed (donor package, M3HashFamilyContractSupport -> the donor CLI); full scale and the -Xint
#     scale; donor and relocated dumps must be byte-identical (sha256 printed). The C lane stability counts per
#     content kind are printed (a finding, not a gate: identical on both sides).
# (d) Python oracle: lhash_stream.py with the crate's cpu_lhash.py (from the donor git) over the dump's 100,000 L-lane
#     inputs in four bounded shards (bytes mode) must equal the relocated dump's L lines byte for byte (per-column
#     agreement printed); the text-mode run (universal newlines, as oracle_lhash.py reads files) is counted.
#     M3_SPECGREP_LHASH=<path to the Python specgrep's cpu_lhash.py> adds that reference as a second oracle.
# (e) fixtures: the crate's oracle_lhash.py over the fixture subset regenerates src/test/resources/jdk/m3/indexdb/
#     hash-family/LHASH_ORACLE.tsv and LHASH_FIXTURES.tsv byte for byte.
# (f) NEC probes (M3HashFamilyNecProbe): the jdk.m3.search SimHash64/FNV owners (#2550) and the cpu-tools
#     TextSignatures compiled from this repository (PROBE rows), counts printed, no equivalence decided.
# (g) javap -p of every relocated main class and its nested records vs the donor's (names mapped, CR stripped).
# Donor package and type names come from RELOCATION_MAP.tsv, so no file in the naming scope names them literally.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> <corpus-dir>   (JAVA_HOME = JDK 21)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
REPO=$(winpath "$(dirname "$0")/../../../..")
DONOR_GIT=$1
REV=$2
mkdir -p "$3"
SCRATCH=$(winpath "$3")
CORPUS=$(winpath "$4")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
BOUND=120
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
MAIN="$MODULE/target/classes"
TEST="$MODULE/target/test-classes"
TPKG=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
DPKG=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
status=PASS
fail() { echo "FAIL $*"; status=FAIL; }
rm -rf "$SCRATCH/src" "$SCRATCH/donor-classes" "$SCRATCH/driver-src" "$SCRATCH/driver-classes" "$SCRATCH/probe-src" \
    "$SCRATCH/probe-classes" "$SCRATCH/oracle" "$SCRATCH/fixtures" "$SCRATCH/fixture-out"
mkdir -p "$SCRATCH/src" "$SCRATCH/donor-classes" "$SCRATCH/driver-src" "$SCRATCH/driver-classes" "$SCRATCH/probe-src" \
    "$SCRATCH/probe-classes" "$SCRATCH/oracle" "$SCRATCH/fixture-out"
commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")

# (a) donor build
awk -F'\t' '($1=="FILE" || $1=="SKIP") && $2 ~ /\/src\/main\/java\// {print $2} $1=="FILE" && $2 ~ /\/src\/test\/java\// {print $2}' "$MAP" \
    | while read -r path; do git -C "$DONOR_GIT" show "$REV:$path" > "$SCRATCH/src/$(basename "$path")"; done
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -d "$SCRATCH/donor-classes" "$SCRATCH"/src/*.java
echo "DONOR $commit sources=$(ls "$SCRATCH"/src/*.java | wc -l) javac=PASS"
DCP="$SCRATCH/donor-classes"
RCP="$MAIN$SEP$TEST"
run() { timeout "$BOUND" "$JAVA_BIN/java" "$@"; }

# (b) crate figures over the oracle corpus
oracle_tsv="$SCRATCH/oracle/LHASH_ORACLE.tsv"
git -C "$DONOR_GIT" show "$REV:$(awk -F'\t' '$1=="ORACLE" && $2 ~ /LHASH_ORACLE\.tsv$/ {print $2; exit}' "$MAP")" > "$oracle_tsv"
figures() {
    local cp=$1 pkg=$2 vm=$3
    run $vm -cp "$cp" "$pkg.M3LogicHashContract" "--root=$CORPUS" "--oracle=$oracle_tsv" --min=403
    run $vm -cp "$cp" "$pkg.M3MinHashContract"
    run $vm -cp "$cp" "$pkg.M3ChunkCdcContract" "--root=$CORPUS" "--list=$CORPUS/corpus.txt"
}
for vm in "" "-Xint"; do
    tag=${vm:-default}
    figures "$DCP" "$DPKG" "$vm" > "$SCRATCH/figures-donor-$tag.txt" 2>&1 || fail "donor contracts $tag"
    figures "$RCP" "$TPKG" "$vm" > "$SCRATCH/figures-relocated-$tag.txt" 2>&1 || fail "relocated contracts $tag"
    if cmp -s "$SCRATCH/figures-donor-$tag.txt" "$SCRATCH/figures-relocated-$tag.txt"; then
        echo "figures $tag IDENTICAL sha256=$(sha256sum < "$SCRATCH/figures-relocated-$tag.txt" | cut -c1-64)"
    else
        fail "figures $tag DIFFERENT"
    fi
done
grep -h '^INFO' "$SCRATCH/figures-relocated-default.txt" | sed 's/^/  /'
for figure in 'rows=403 matched=403 mismatches=0 drift=0' 'meanAbsError=0.0269 maxAbsError=0.1607 within0.05=851/1000' \
    'recalled=200 candidatePairs=200 recall=1.0000' 'bytes=2950996 min=2048;avg=8192;max=65536;window=64 chunks=148' \
    'chunks=1601 edit=1644224 insert=188 farChunks=1598 survived=1598' 'chunks=826 edit=513556 insert=38 farChunks=821 survived=821'; do
    grep -qF "$figure" "$SCRATCH/figures-relocated-default.txt" || fail "crate figure not reproduced: $figure"
done

# (c) dumps
reverse=$(awk -F'\t' '$1=="TYPE" && $2!=$3 {printf "-e s/\\<%s\\>/%s/g ", $3, $2}' "$MAP" | sort -u)
sed -e "s/^package $TPKG;/package $DPKG;/" $reverse "$MODULE/src/test/java/${TPKG//.//}/M3HashFamilyDump.java" \
    > "$SCRATCH/driver-src/M3HashFamilyDump.java"
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -cp "$DCP" -d "$SCRATCH/driver-classes" "$SCRATCH/driver-src/M3HashFamilyDump.java"
DDCP="$DCP$SEP$SCRATCH/driver-classes"
run -cp "$DDCP" "$DPKG.M3HashFamilyDump" "--out=$SCRATCH/donor-dump.txt"
run -cp "$RCP" "$TPKG.M3HashFamilyDump" "--out=$SCRATCH/relocated-dump.txt" "--inputs=$SCRATCH/lhash-inputs.tsv" "--fixtures=$SCRATCH/fixtures"
run -Xint -cp "$DDCP" "$DPKG.M3HashFamilyDump" "--out=$SCRATCH/donor-xint.txt" --scale=xint
run -Xint -cp "$RCP" "$TPKG.M3HashFamilyDump" "--out=$SCRATCH/relocated-xint.txt" --scale=xint
run -cp "$RCP" "$TPKG.M3HashFamilyDump" "--out=$SCRATCH/relocated-xint-default-vm.txt" --scale=xint
compare() {
    local donor_sha relocated_sha
    donor_sha=$(sha256sum < "$1" | cut -c1-64)
    relocated_sha=$(sha256sum < "$2" | cut -c1-64)
    if cmp -s "$1" "$2"; then
        echo "$3 IDENTICAL donor_sha256=$donor_sha relocated_sha256=$relocated_sha bytes=$(wc -c < "$1") lines=$(wc -l < "$1")"
    else
        fail "$3 DIFFERENT donor_sha256=$donor_sha relocated_sha256=$relocated_sha"
    fi
}
compare "$SCRATCH/donor-dump.txt" "$SCRATCH/relocated-dump.txt" dump
compare "$SCRATCH/donor-xint.txt" "$SCRATCH/relocated-xint.txt" dump-xint
compare "$SCRATCH/relocated-xint-default-vm.txt" "$SCRATCH/relocated-xint.txt" dump-xint-vs-default-vm
grep -E '^(MSUM|GSUM|CSUM|Mall)' "$SCRATCH/relocated-dump.txt" | sed 's/^/  /'
awk -F'\t' '/^L/ {n++} /^F/ {f++} /^P/ {p++} /^C/ {c++} END {printf "  lanes L=%d F=%d P=%d C=%d\n", n, f, p, c}' "$SCRATCH/relocated-dump.txt"
awk -F'\t' '/^S/ {i = substr($1, 2) + 0; kind = (i < 25) ? "boundary" : "kind" (i % 5); split($2, q, ";"); key = kind " " q[1];
    n[key]++; if ($NF == "BEYOND_2MAX") b[key]++; split($8, r, "="); if (r[2] + 0 > m[key]) m[key] = r[2] + 0}
    END {for (k in n) printf "  stability %s insertions=%d beyond2max=%d maxLostReach=%d\n", k, n[k], b[k] + 0, m[k] + 0}' \
    "$SCRATCH/relocated-dump.txt" | sort
mpairs=$(awk -F'\t' '/^MSUM/ {split($3, a, "="); split($4, b, "="); split($5, c, "="); print (a[2] <= 0.05 && b[2] <= 0.20 && c[2] >= 8000) ? "PASS" : "FAIL"}' "$SCRATCH/relocated-dump.txt")
echo "minhash_bound $mpairs (mean <= 0.05, per-pair max <= 0.20, >= 80% within 0.05 over 10,000 pairs; crate contract bounds)"
[ "$mpairs" = PASS ] || fail "minhash bound"

# (d) Python oracle
git -C "$DONOR_GIT" show "$REV:$(awk -F'\t' '$1=="ORACLE" && $2 ~ /cpu_lhash\.py$/ {print $2; exit}' "$MAP")" > "$SCRATCH/oracle/cpu_lhash.py"
git -C "$DONOR_GIT" show "$REV:$(awk -F'\t' '$1=="ORACLE" && $2 ~ /oracle_lhash\.py$/ {print $2; exit}' "$MAP")" > "$SCRATCH/oracle/oracle_lhash.py"
grep '^L' "$SCRATCH/relocated-dump.txt" > "$SCRATCH/relocated-L.tsv"
inputs=$(wc -l < "$SCRATCH/lhash-inputs.tsv")
python_oracle() {
    local reference=$1 out=$2 mode=$3 shard pids=()
    for shard in 0 1 2 3; do
        timeout "$BOUND" python -I -B "$RECIPE_DIR/lhash_stream.py" "$reference" "$SCRATCH/lhash-inputs.tsv" "$out.$shard" $mode \
            "--range=$((shard * inputs / 4)):$(((shard + 1) * inputs / 4))" > "$out.$shard.log" &
        pids+=($!)
    done
    for pid in "${pids[@]}"; do wait "$pid" || return 1; done
    cat "$out.0" "$out.1" "$out.2" "$out.3" > "$out"
    head -1 "$out.0.log"
}
agreement() {
    python -I - "$SCRATCH/relocated-L.tsv" "$1" "$2" <<'PY'
import sys
java, python = [open(path, encoding="ascii").read().splitlines() for path in sys.argv[1:3]]
names = ["index", "bytes", "nTokens", "h1", "h2", "h3", "h4", "h5Count", "h5Fold", "similarity"]
equal = [0] * len(names)
rows = sum(1 for a, b in zip(java, python) if a == b)
for a, b in zip(java, python):
    for column, (x, y) in enumerate(zip(a.split("\t"), b.split("\t"))):
        equal[column] += x == y
print("python_oracle %s inputs=%d python_rows=%d identical_rows=%d %s" % (sys.argv[3], len(java), len(python), rows,
      " ".join("%s=%d" % (n, e) for n, e in zip(names[2:], equal[2:]))))
PY
}
python_oracle "$SCRATCH/oracle/cpu_lhash.py" "$SCRATCH/python-bytes.tsv" "" || fail "python oracle (bytes) did not finish within ${BOUND}s per shard"
agreement "$SCRATCH/python-bytes.tsv" crate-cpu_lhash.py-bytes
cmp -s "$SCRATCH/relocated-L.tsv" "$SCRATCH/python-bytes.tsv" && echo "python_oracle bytes IDENTICAL sha256=$(sha256sum < "$SCRATCH/python-bytes.tsv" | cut -c1-64)" \
    || fail "python oracle (bytes) differs from the relocated L lane"
python_oracle "$SCRATCH/oracle/cpu_lhash.py" "$SCRATCH/python-text.tsv" --text-mode || fail "python oracle (text) did not finish"
agreement "$SCRATCH/python-text.tsv" crate-cpu_lhash.py-text-mode
python -I - "$SCRATCH/lhash-inputs.tsv" "$SCRATCH/relocated-L.tsv" "$SCRATCH/python-text.tsv" <<'PY'
import base64, re, sys
lone = []
for row in open(sys.argv[1], encoding="ascii"):
    cells = row.rstrip("\n").split("\t")
    lone.append(re.search(rb"\r(?!\n)", base64.b64decode(cells[1]) if len(cells) > 1 else b"") is not None)
java, text = [open(path, encoding="ascii").read().splitlines() for path in sys.argv[2:4]]
hashes = [i for i, (a, b) in enumerate(zip(java, text)) if a.split("\t")[:9] != b.split("\t")[:9]]
similarity_only = [i for i, (a, b) in enumerate(zip(java, text)) if a != b and i not in set(hashes)]
print("  text-mode attribution: lone_cr_inputs=%d hash_columns_differ=%d (lone CR: %d, other: %d) similarity_only=%d"
      " (previous input has a lone CR: %d)" % (sum(lone), len(hashes), sum(lone[i] for i in hashes),
      sum(not lone[i] for i in hashes), len(similarity_only), sum(1 for i in similarity_only if i and lone[i - 1])))
PY
if [ -n "${M3_SPECGREP_LHASH:-}" ]; then
    cp "$M3_SPECGREP_LHASH" "$SCRATCH/oracle/specgrep_cpu_lhash.py"
    python_oracle "$SCRATCH/oracle/specgrep_cpu_lhash.py" "$SCRATCH/python-specgrep.tsv" "" || fail "specgrep oracle did not finish"
    agreement "$SCRATCH/python-specgrep.tsv" specgrep-cpu_lhash.py-bytes
fi

# (e) fixtures
fixtures_dir="$MODULE/src/test/resources/${TPKG//.//}/hash-family"
(cd "$SCRATCH/fixtures" && timeout "$BOUND" python -I -B "$SCRATCH/oracle/oracle_lhash.py" "$SCRATCH/oracle/cpu_lhash.py" . listing.txt "$SCRATCH/fixture-out/LHASH_ORACLE.tsv")
python -I - "$SCRATCH/fixtures" "$SCRATCH/fixture-out/LHASH_FIXTURES.tsv" <<'PY'
import base64, sys
from pathlib import Path
root = Path(sys.argv[1])
names = root.joinpath("listing.txt").read_text(encoding="ascii").split()
with open(sys.argv[2], "w", encoding="ascii", newline="\n") as out:
    out.write("path\tbytes\tbase64\n")
    for name in names:
        data = root.joinpath(name).read_bytes()
        out.write("%s\t%d\t%s\n" % (name, len(data), base64.b64encode(data).decode("ascii") or "-"))
PY
for name in LHASH_ORACLE.tsv LHASH_FIXTURES.tsv; do
    if cmp -s "$SCRATCH/fixture-out/$name" "$fixtures_dir/$name"; then
        echo "fixture $name IDENTICAL rows=$(($(wc -l < "$fixtures_dir/$name") - 1)) sha256=$(sha256sum < "$fixtures_dir/$name" | cut -c1-64)"
    else
        fail "fixture $name differs from the regenerated one ($SCRATCH/fixture-out/$name)"
    fi
done

# (f) NEC probes
awk -F'\t' '$1=="PROBE" && $4=="m3" {print $2}' "$MAP" | while read -r path; do
    git -C "$REPO" show "HEAD:$path" > "$SCRATCH/probe-src/$(basename "$path")"
    echo "PROBE $path blob=$(git -C "$REPO" rev-parse "HEAD:$path" | cut -c1-12)"
done
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -d "$SCRATCH/probe-classes" "$SCRATCH"/probe-src/*.java
probe_class() { awk -F'\t' -v n="$1" '$1=="PROBE" && $4=="m3" && $2 ~ ("/" n "\\.java$") {p=$2; sub(/^.*\/java\//, "", p); sub(/^.*src\/[^\/]*\//, "", p); sub(/\.java$/, "", p); gsub(/\//, ".", p); print p; exit}' "$MAP"; }
simhash_skip=$(awk -F'\t' '$1=="SKIP" && $2 ~ /\/src\/main\/java\/.*SimHash64\.java$/ {n=$2; sub(/^.*\//, "", n); sub(/\.java$/, "", n); print n; exit}' "$MAP")
run -cp "$RCP$SEP$DCP$SEP$SCRATCH/probe-classes" "$TPKG.M3HashFamilyNecProbe" "--simhash=$DPKG.$simhash_skip" \
    "--search-simhash=$(probe_class M3SimHash64)" "--search-fnv=$(probe_class M3Utf8Fnv1a64)" \
    "--textsignatures=$(probe_class TextSignatures)" "--corpus-root=$CORPUS" "--corpus-list=$CORPUS/corpus.txt" \
    > "$SCRATCH/nec-probes.txt" 2>&1 || fail "NEC probes"
sed 's/^/  /' "$SCRATCH/nec-probes.txt"

# (g) javap -p parity modulo names
forward=$(awk -F'\t' '$1=="TYPE" && $2!=$3 && $4=="" {printf "-e s/\\<%s\\>/%s/g ", $2, $3}' "$MAP")
: > "$SCRATCH/donor-javap.txt"
: > "$SCRATCH/relocated-javap.txt"
for target in $(awk -F'\t' '$1=="FILE" && $3 ~ /^src\/main\/java\// {n=$3; sub(/^.*\//, "", n); sub(/\.java$/, "", n); print n}' "$MAP"); do
    donor=$(awk -F'\t' -v t="$target" '$1=="TYPE" && $3==t && $4=="" {print $2; exit}' "$MAP")
    for class in $(cd "$MAIN/${TPKG//.//}" && ls "$target.class" "$target\$"*.class 2>/dev/null); do
        class=${class%.class}
        "$JAVA_BIN/javap" -p -cp "$DCP" "$DPKG.${class/#$target/$donor}" | tr -d '\r' | sed -e "s/$DPKG/$TPKG/g" $forward >> "$SCRATCH/donor-javap.txt"
        "$JAVA_BIN/javap" -p -cp "$MAIN" "$TPKG.$class" | tr -d '\r' >> "$SCRATCH/relocated-javap.txt"
    done
done
compare "$SCRATCH/donor-javap.txt" "$SCRATCH/relocated-javap.txt" javap-p
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
