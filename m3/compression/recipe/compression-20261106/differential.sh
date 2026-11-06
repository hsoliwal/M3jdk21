#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Differential gate of recipe compression-20261106 (after mvn package of m3/compression; JAVA_HOME = JDK 21).
# (a) The donor closure (every Java FILE row of RELOCATION_MAP.tsv except the JUnit wrappers) is compiled from the donor
#     git tree with javac --release 21 -Xlint:all -Werror; each donor oracle main and its relocated twin must print
#     byte-identical output, and the M3WR wire corpus the workspace oracle writes must be byte-identical.
# (b) The format oracle (test scope, M3CompressionFormatOracle) drives the donor encoders and the relocated encoders by
#     class name over the same seeded inputs: its report (round-trip counts through the strict spec decoders and the
#     SHA-256 of all LZ4 and all Snappy wire bytes) must be byte-identical.
# (c) Mutant lane: one-line spec mutants of the relocated encoders (LZ4 LAST_LITERALS 5 -> 4, Snappy
#     MAX_COPY_LENGTH 64 -> 65), compiled into the scratch and shadowing the relocated class, must be refused by the
#     format oracle (it is live, not a tautology).
# (d) Allocation probe: donor and relocated M3CompressionAllocationProbe; the bytes/op figures must be equal (ns/op is
#     printed, never compared).
# The donor package comes from RELOCATION_MAP.tsv next to this script, so no file in the naming scope names it.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> [inputs]
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
INPUTS=${4:-100000}
mkdir -p "$3/src" "$3/classes" "$3/out" "$3/mutant-src" "$3/tmp"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
JAVA=("$JAVA_BIN/java" "-Djava.io.tmpdir=$SCRATCH/tmp")
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
RELOCATED_CP="$MODULE/target/classes$SEP$MODULE/target/test-classes"
mapfile -t donor_files < <(awk -F'\t' '$1=="FILE" && $2 ~ /\.java$/ {print $2}' "$MAP")
compiled=0
for path in "${donor_files[@]}"; do
    body=$(git -C "$DONOR_GIT" show "$REV:$path")
    case "$body" in *"org.junit"*) continue ;; esac
    pkg_dir="$SCRATCH/src/$(dirname "$path" | sed 's#.*/src/\(main\|test\)/java/##')"
    mkdir -p "$pkg_dir"
    printf '%s\n' "$body" > "$pkg_dir/$(basename "$path")"
    compiled=$((compiled + 1))
done
echo "DONOR $donor_commit closure=$compiled (JUnit wrappers skipped)"
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/classes" $(find "$SCRATCH/src" -name '*.java')
status=PASS
verdict() { # label donor-file relocated-file
    local sha_d sha_r v=IDENTICAL
    sha_d=$(sha256sum "$2" | cut -d' ' -f1); sha_r=$(sha256sum "$3" | cut -d' ' -f1)
    cmp -s "$2" "$3" || { v=DIFFERENT; status=FAIL; }
    echo "$1 $v donor_sha256=$sha_d relocated_sha256=$sha_r bytes=$(wc -c < "$2")"
}
# (a) oracle mains and the wire corpus
"${JAVA[@]}" -cp "$SCRATCH/classes" "$donor_package.M3CompressionWorkspaceSelfTest" "$SCRATCH/out/donor-wire.bin" > "$SCRATCH/out/donor-workspace.txt"
"${JAVA[@]}" -cp "$RELOCATED_CP" "$target_package.M3CompressionWorkspaceSelfTest" "$SCRATCH/out/relocated-wire.bin" > "$SCRATCH/out/relocated-workspace.txt"
verdict M3CompressionWorkspaceSelfTest "$SCRATCH/out/donor-workspace.txt" "$SCRATCH/out/relocated-workspace.txt"
verdict M3WR_wire_corpus "$SCRATCH/out/donor-wire.bin" "$SCRATCH/out/relocated-wire.bin"
"${JAVA[@]}" -cp "$SCRATCH/classes" "$donor_package.M3LzMatchCandidateSelfTest" > "$SCRATCH/out/donor-lzindex.txt"
"${JAVA[@]}" -cp "$RELOCATED_CP" "$target_package.M3LzMatchCandidateSelfTest" > "$SCRATCH/out/relocated-lzindex.txt"
verdict M3LzMatchCandidateSelfTest "$SCRATCH/out/donor-lzindex.txt" "$SCRATCH/out/relocated-lzindex.txt"
# (b) format oracle over donor and relocated encoders
"${JAVA[@]}" -cp "$RELOCATED_CP$SEP$SCRATCH/classes" "$target_package.M3CompressionFormatOracle" \
    "$donor_package.M3Lz4RawBlockEncoder" "$donor_package.M3SnappyRawEncoder" "$INPUTS" > "$SCRATCH/out/donor-format.txt"
"${JAVA[@]}" -cp "$RELOCATED_CP" "$target_package.M3CompressionFormatOracle" \
    "$target_package.M3Lz4RawBlockEncoder" "$target_package.M3SnappyRawEncoder" "$INPUTS" > "$SCRATCH/out/relocated-format.txt"
verdict M3CompressionFormatOracle "$SCRATCH/out/donor-format.txt" "$SCRATCH/out/relocated-format.txt"
grep -E "_(INPUTS|ROUNDTRIP|SHA256|RESULT)=" "$SCRATCH/out/relocated-format.txt" | tr '\n' ' '; echo
# (c) mutant lane
mutant() { # label file sed-expression expected-message
    local dir="$SCRATCH/mutant-$1"
    rm -rf "$dir"; mkdir -p "$dir/classes"
    sed "$3" "$MODULE/src/main/java/${target_package//.//}/$2" > "$dir/$2"
    if cmp -s "$dir/$2" "$MODULE/src/main/java/${target_package//.//}/$2"; then echo "mutant $1 NOT_APPLIED"; status=FAIL; return; fi
    "$JAVA_BIN/javac" --release 21 -d "$dir/classes" -cp "$MODULE/target/classes" "$dir/$2"
    if "${JAVA[@]}" -cp "$dir/classes$SEP$RELOCATED_CP" "$target_package.M3CompressionFormatOracle" \
        "$target_package.M3Lz4RawBlockEncoder" "$target_package.M3SnappyRawEncoder" "$INPUTS" > "$dir/out.txt" 2> "$dir/err.txt"; then
        echo "mutant $1 ACCEPTED"; status=FAIL
    elif grep -q "$4" "$dir/err.txt"; then
        echo "mutant $1 REFUSED: $(grep -m1 -oE "(AssertionError|IllegalArgumentException).*" "$dir/err.txt" | tr -d '\r' | cut -c1-140)"
    else
        echo "mutant $1 FAILED_OTHERWISE: $(head -c 300 "$dir/err.txt")"; status=FAIL
    fi
}
mutant lz4_last_literals M3Lz4RawBlockEncoder.java 's/LAST_LITERALS = 5;/LAST_LITERALS = 4;/' "LZ4 match reaches the last 5 bytes"
mutant snappy_copy_length M3SnappyRawEncoder.java 's/MAX_COPY_LENGTH = 64;/MAX_COPY_LENGTH = 65;/' "Snappy"
# (d) allocation probe (bytes/op compared, ns/op printed only)
"${JAVA[@]}" -cp "$SCRATCH/classes" "$donor_package.M3CompressionAllocationProbe" > "$SCRATCH/out/donor-probe.txt"
"${JAVA[@]}" -cp "$RELOCATED_CP" "$target_package.M3CompressionAllocationProbe" > "$SCRATCH/out/relocated-probe.txt"
sed 's/ ns\/op=.*//' "$SCRATCH/out/donor-probe.txt" > "$SCRATCH/out/donor-probe-bytes.txt"
sed 's/ ns\/op=.*//' "$SCRATCH/out/relocated-probe.txt" > "$SCRATCH/out/relocated-probe-bytes.txt"
verdict M3CompressionAllocationProbe_bytes_per_op "$SCRATCH/out/donor-probe-bytes.txt" "$SCRATCH/out/relocated-probe-bytes.txt"
echo "probe donor: $(tr -d '\r' < "$SCRATCH/out/donor-probe.txt" | tr '\n' ';')"
echo "probe relocated: $(tr -d '\r' < "$SCRATCH/out/relocated-probe.txt" | tr '\n' ';')"
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
