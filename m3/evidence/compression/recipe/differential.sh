#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Differential gate of recipe compression-20261106 (after mvn package of m3/compression; JAVA_HOME = JDK 21).
# (a) The donor closure (every Java FILE row of RELOCATION_MAP.tsv except the JUnit wrappers) is compiled from the donor
#     git tree with javac --release 21 -Xlint:all -Werror; each donor oracle main and its relocated twin must print
#     byte-identical output (workspace, candidate-index and chunk-cache oracles), and the M3WR wire corpus the
#     workspace oracle writes must be byte-identical.
# (b) The format oracle (test scope, M3CompressionFormatOracle) drives the donor encoders and the relocated encoders by
#     class name over the same seeded inputs: its report (round-trip counts through the strict spec decoders and the
#     SHA-256 of all LZ4 and all Snappy wire bytes) must be byte-identical.
# (c) Mutant lane: one-line mutants of the relocated owners (LZ4 LAST_LITERALS 5 -> 4, Snappy MAX_COPY_LENGTH
#     64 -> 65, chunk cache evicting the most recent slot instead of the least recent), compiled into the scratch and
#     shadowing the relocated class, must be refused by the format oracle / the cache model oracle (they are live).
# (d) Allocation probe: donor and relocated M3CompressionAllocationProbe; the bytes/op figures must be equal (ns/op is
#     printed, never compared).
# (e) The chunk-cache model oracle (test scope, M3CompressedChunkCacheModel) drives the donor cache and the relocated
#     cache by class name through the same seeded workloads; both must agree with the LinkedHashMap LRU model at every
#     operation and their reports (counters and the SHA-256 of the per-operation trace) must be byte-identical.
# Decoder / four-way leaf (third commit of the family):
# (f) The donor oracles of the leaf (encoder round trip, adversarial, grammar, four-way index) and their relocated twins
#     print byte-identical output; the M3EC corpus M3CompressionEncodedCorpusCli writes is byte-identical.
# (g) The decoder agreement oracle (test scope, M3CompressionDecoderAgreement, run under -Xmx256m) drives the donor
#     decoder pair and the relocated pair by class name against the strict reference decoders (100,000 encoder
#     streams, grammar streams, malformed streams of nine defect kinds, 200,000 fuzz streams): reports byte-identical.
# (h) The four-way encoding probe (test scope, M3LzFourWayEncodingProbe) parses the same inputs over the donor four-way
#     index and over the relocated one: reports (wire digests, candidate trace) byte-identical, and the probe itself
#     checks per input that the donor index's wires equal the relocated index's wires.
# (i) Mutant lane of the leaf: LZ4 decoder MIN_MATCH 4 -> 3, Snappy decoder COPY_1 length base 4 -> 3, LZ4 decoder
#     offset-0 check removed (the refusal changes class) are refused by the agreement oracle; a four-way index that
#     evicts even when a way is vacant changes the candidate trace of the probe (refused against the donor trace).
# Native leaf (fourth commit): the donor C files of the map are written byte for byte to <scratch>/native for
# native-gate.sh, and (j) the donor oracles of the JNI owner (M3LzSelfTest, M3LzFourWayNativeInteropSelfTest) print
# byte-identical output on their Java lanes (no library here; the JNI lanes run in native-gate.sh on a Linux host).
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
# native leaf: the donor C files of the map, byte for byte, for native-gate.sh (Linux host)
mkdir -p "$SCRATCH/native"
mapfile -t donor_native < <(awk -F'\t' '$1=="FILE" && $2 ~ /\.(c|h)$/ {print $2}' "$MAP")
for path in "${donor_native[@]}"; do
    git -C "$DONOR_GIT" show "$REV:$path" > "$SCRATCH/native/$(basename "$path")"
done
echo "DONOR native files=${#donor_native[@]} -> $SCRATCH/native"
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
"${JAVA[@]}" -cp "$SCRATCH/classes" "$donor_package.M3CompressedChunkCacheSelfTest" > "$SCRATCH/out/donor-chunkcache.txt"
"${JAVA[@]}" -cp "$RELOCATED_CP" "$target_package.M3CompressedChunkCacheSelfTest" > "$SCRATCH/out/relocated-chunkcache.txt"
verdict M3CompressedChunkCacheSelfTest "$SCRATCH/out/donor-chunkcache.txt" "$SCRATCH/out/relocated-chunkcache.txt"
# (b) format oracle over donor and relocated encoders
"${JAVA[@]}" -cp "$RELOCATED_CP$SEP$SCRATCH/classes" "$target_package.M3CompressionFormatOracle" \
    "$donor_package.M3Lz4RawBlockEncoder" "$donor_package.M3SnappyRawEncoder" "$INPUTS" > "$SCRATCH/out/donor-format.txt"
"${JAVA[@]}" -cp "$RELOCATED_CP" "$target_package.M3CompressionFormatOracle" \
    "$target_package.M3Lz4RawBlockEncoder" "$target_package.M3SnappyRawEncoder" "$INPUTS" > "$SCRATCH/out/relocated-format.txt"
verdict M3CompressionFormatOracle "$SCRATCH/out/donor-format.txt" "$SCRATCH/out/relocated-format.txt"
grep -E "_(INPUTS|ROUNDTRIP|SHA256|RESULT)=" "$SCRATCH/out/relocated-format.txt" | tr '\n' ' '; echo
# (c) mutant lane
mutant() { # label file sed-expression expected-message oracle-simple-name [oracle args]
    local label=$1 dir="$SCRATCH/mutant-$1" file=$2 expression=$3 expected=$4 oracle=$5
    shift 5
    rm -rf "$dir"; mkdir -p "$dir/classes"
    sed "$expression" "$MODULE/src/main/java/${target_package//.//}/$file" > "$dir/$file"
    if cmp -s "$dir/$file" "$MODULE/src/main/java/${target_package//.//}/$file"; then echo "mutant $label NOT_APPLIED"; status=FAIL; return; fi
    "$JAVA_BIN/javac" --release 21 -d "$dir/classes" -cp "$MODULE/target/classes" "$dir/$file"
    if "${JAVA[@]}" -cp "$dir/classes$SEP$RELOCATED_CP" "$target_package.$oracle" "$@" > "$dir/out.txt" 2> "$dir/err.txt"; then
        echo "mutant $label ACCEPTED"; status=FAIL
    elif grep -q "$expected" "$dir/err.txt"; then
        echo "mutant $label REFUSED: $(grep -m1 -oE "(AssertionError|IllegalArgumentException).*" "$dir/err.txt" | tr -d '\r' | cut -c1-140)"
    else
        echo "mutant $label FAILED_OTHERWISE: $(head -c 300 "$dir/err.txt")"; status=FAIL
    fi
}
encoders=("$target_package.M3Lz4RawBlockEncoder" "$target_package.M3SnappyRawEncoder" "$INPUTS")
mutant lz4_last_literals M3Lz4RawBlockEncoder.java 's/LAST_LITERALS = 5;/LAST_LITERALS = 4;/' \
    "LZ4 match reaches the last 5 bytes" M3CompressionFormatOracle "${encoders[@]}"
mutant snappy_copy_length M3SnappyRawEncoder.java 's/MAX_COPY_LENGTH = 64;/MAX_COPY_LENGTH = 65;/' \
    "Snappy" M3CompressionFormatOracle "${encoders[@]}"
mutant cache_evicts_most_recent M3CompressedChunkCache.java 's/access\[i\] < access\[oldest\]/access[i] > access[oldest]/' \
    "model" M3CompressedChunkCacheModel
# (d) allocation probe (bytes/op compared, ns/op printed only)
"${JAVA[@]}" -cp "$SCRATCH/classes" "$donor_package.M3CompressionAllocationProbe" > "$SCRATCH/out/donor-probe.txt"
"${JAVA[@]}" -cp "$RELOCATED_CP" "$target_package.M3CompressionAllocationProbe" > "$SCRATCH/out/relocated-probe.txt"
sed 's/ ns\/op=.*//' "$SCRATCH/out/donor-probe.txt" > "$SCRATCH/out/donor-probe-bytes.txt"
sed 's/ ns\/op=.*//' "$SCRATCH/out/relocated-probe.txt" > "$SCRATCH/out/relocated-probe-bytes.txt"
verdict M3CompressionAllocationProbe_bytes_per_op "$SCRATCH/out/donor-probe-bytes.txt" "$SCRATCH/out/relocated-probe-bytes.txt"
echo "probe donor: $(tr -d '\r' < "$SCRATCH/out/donor-probe.txt" | tr '\n' ';')"
echo "probe relocated: $(tr -d '\r' < "$SCRATCH/out/relocated-probe.txt" | tr '\n' ';')"
# (e) chunk-cache model oracle over donor and relocated caches
"${JAVA[@]}" -cp "$RELOCATED_CP$SEP$SCRATCH/classes" "$target_package.M3CompressedChunkCacheModel" \
    "$donor_package.M3CompressedChunkCache" > "$SCRATCH/out/donor-cachemodel.txt"
"${JAVA[@]}" -cp "$RELOCATED_CP" "$target_package.M3CompressedChunkCacheModel" \
    "$target_package.M3CompressedChunkCache" > "$SCRATCH/out/relocated-cachemodel.txt"
verdict M3CompressedChunkCacheModel "$SCRATCH/out/donor-cachemodel.txt" "$SCRATCH/out/relocated-cachemodel.txt"
grep -E "^M3_CHUNK_CACHE_(OPS|HITS|MISSES|EVICTIONS|TRIMS|MODEL_FIELD_CHECKS|MODEL_MISMATCHES|TRACE_SHA256|RESULT)=" \
    "$SCRATCH/out/relocated-cachemodel.txt" | tr -d '\r' | tr '\n' ' '; echo
# (f) decoder / four-way leaf donor oracles and the M3EC corpus
for oracle in M3CompressionEncoderSelfTest M3CompressionAdversarialSelfTest M3CompressionGrammarSelfTest M3LzFourWayCandidateSelfTest; do
    "${JAVA[@]}" -cp "$SCRATCH/classes" "$donor_package.$oracle" > "$SCRATCH/out/donor-$oracle.txt"
    "${JAVA[@]}" -cp "$RELOCATED_CP" "$target_package.$oracle" > "$SCRATCH/out/relocated-$oracle.txt"
    verdict "$oracle" "$SCRATCH/out/donor-$oracle.txt" "$SCRATCH/out/relocated-$oracle.txt"
done
"${JAVA[@]}" -cp "$SCRATCH/classes" "$donor_package.M3CompressionEncodedCorpusCli" "$SCRATCH/out/donor.m3ec" > /dev/null
"${JAVA[@]}" -cp "$RELOCATED_CP" "$target_package.M3CompressionEncodedCorpusCli" "$SCRATCH/out/relocated.m3ec" > /dev/null
verdict M3EC_encoded_corpus "$SCRATCH/out/donor.m3ec" "$SCRATCH/out/relocated.m3ec"
# (g) decoder agreement oracle over the donor and the relocated decoder pair (bounded heap)
"${JAVA[@]}" -Xmx256m -cp "$RELOCATED_CP$SEP$SCRATCH/classes" "$target_package.M3CompressionDecoderAgreement" \
    "$donor_package.M3Lz4RawBlockDecoder" "$donor_package.M3SnappyRawDecoder" "$INPUTS" > "$SCRATCH/out/donor-agreement.txt"
"${JAVA[@]}" -Xmx256m -cp "$RELOCATED_CP" "$target_package.M3CompressionDecoderAgreement" \
    "$target_package.M3Lz4RawBlockDecoder" "$target_package.M3SnappyRawDecoder" "$INPUTS" > "$SCRATCH/out/relocated-agreement.txt"
verdict M3CompressionDecoderAgreement "$SCRATCH/out/donor-agreement.txt" "$SCRATCH/out/relocated-agreement.txt"
grep -E "_(ENCODER_LZ4_ACCEPTED_BOTH|ENCODER_SNAPPY_ACCEPTED_BOTH|MALFORMED_LZ4_REFUSED_BOTH|MALFORMED_SNAPPY_REFUSED_BOTH|FUZZ_STREAMS|RESULT)=" \
    "$SCRATCH/out/relocated-agreement.txt" | tr -d '\r' | tr '\n' ' '; echo
# (h) four-way encoding probe over the donor and the relocated four-way index
"${JAVA[@]}" -cp "$RELOCATED_CP$SEP$SCRATCH/classes" "$target_package.M3LzFourWayEncodingProbe" \
    "$donor_package.M3LzFourWayCandidateIndex" "$INPUTS" > "$SCRATCH/out/donor-fourway.txt"
"${JAVA[@]}" -cp "$RELOCATED_CP" "$target_package.M3LzFourWayEncodingProbe" \
    "$target_package.M3LzFourWayCandidateIndex" "$INPUTS" > "$SCRATCH/out/relocated-fourway.txt"
verdict M3LzFourWayEncodingProbe "$SCRATCH/out/donor-fourway.txt" "$SCRATCH/out/relocated-fourway.txt"
grep -E "_(TWIN_DIRECT_IDENTICAL|NAMED_FOURWAY_EQUALS_RELOCATED_FOURWAY|LZ4_FOURWAY_OVER_DIRECT|SNAPPY_FOURWAY_OVER_DIRECT|CANDIDATE_TRACE|RESULT)=" \
    "$SCRATCH/out/relocated-fourway.txt" | tr -d '\r' | tr '\n' ' '; echo
# (i) mutant lane of the leaf
mutant lz4_decoder_min_match M3Lz4RawBlockDecoder.java 's/MIN_MATCH = 4;/MIN_MATCH = 3;/' \
    "LZ4 ENCODER round trip" M3CompressionDecoderAgreement
mutant snappy_decoder_copy1_length M3SnappyRawDecoder.java 's/length = 4 + ((tag >>> 2) \& 7);/length = 3 + ((tag >>> 2) \& 7);/' \
    "Snappy GRAMMAR round trip" M3CompressionDecoderAgreement
mutant lz4_decoder_offset_zero_check M3Lz4RawBlockDecoder.java 's/if (offset == 0 || offset > written) {/if (offset > written) {/' \
    "refusal message without a class" M3CompressionDecoderAgreement
trace_mutant() { # label file sed-expression: the probe over the mutant index must change the candidate trace
    local label=$1 dir="$SCRATCH/mutant-$1" file=$2
    rm -rf "$dir"; mkdir -p "$dir/classes"
    sed "$3" "$MODULE/src/main/java/${target_package//.//}/$file" > "$dir/$file"
    if cmp -s "$dir/$file" "$MODULE/src/main/java/${target_package//.//}/$file"; then echo "mutant $label NOT_APPLIED"; status=FAIL; return; fi
    "$JAVA_BIN/javac" --release 21 -d "$dir/classes" -cp "$MODULE/target/classes" "$dir/$file"
    "${JAVA[@]}" -cp "$dir/classes$SEP$RELOCATED_CP" "$target_package.M3LzFourWayEncodingProbe" \
        "$target_package.M3LzFourWayCandidateIndex" "$INPUTS" > "$dir/out.txt" 2> "$dir/err.txt" || true
    local donor_trace mutant_trace
    donor_trace=$(grep -m1 "_CANDIDATE_TRACE=" "$SCRATCH/out/donor-fourway.txt" | tr -d '\r')
    mutant_trace=$(grep -m1 "_CANDIDATE_TRACE=" "$dir/out.txt" | tr -d '\r')
    if [ -n "$mutant_trace" ] && [ "$mutant_trace" = "$donor_trace" ]; then
        echo "mutant $label ACCEPTED"; status=FAIL
    else
        echo "mutant $label REFUSED: candidate trace ${mutant_trace#*=} != donor ${donor_trace#*=}"
    fi
}
trace_mutant fourway_evicts_with_vacant_way M3LzFourWayCandidateIndex.java \
    's/int slot = vacant >= 0 ? vacant : oldest;/int slot = oldest >= 0 ? oldest : vacant;/'
# (j) native leaf, Java lanes (no library on this host; the JNI lanes run in native-gate.sh): the donor oracles of the
#     JNI owner print byte-identical output with the Java kernels as the backend
for oracle in M3LzSelfTest M3LzFourWayNativeInteropSelfTest; do
    "${JAVA[@]}" -cp "$SCRATCH/classes" "$donor_package.$oracle" > "$SCRATCH/out/donor-$oracle.txt"
    "${JAVA[@]}" -cp "$RELOCATED_CP" "$target_package.$oracle" > "$SCRATCH/out/relocated-$oracle.txt"
    verdict "$oracle" "$SCRATCH/out/donor-$oracle.txt" "$SCRATCH/out/relocated-$oracle.txt"
done
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
