#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Runs every gate of recipe compression-20261106 in order and prints one line per gate (see GATES.tsv):
#   recipe   relocate.py --check is a fixed point (changed=0 provenance=UNCHANGED)
#   1        mvn package of m3/compression on JDK 21 (-Xlint:all -Werror on main and test sources), 0 compiler warnings
#   2        the oracles ran in the test phase: donor workspace oracle, candidate-index oracle, format oracle
#            (M3_COMPRESSION_FORMAT_RESULT=PASS), and the chunk-cache oracles when the module carries them; with the
#            decoder leaf also the encoder / adversarial / grammar / four-way oracles, the decoder agreement oracle
#            (M3_DECODER_AGREEMENT_RESULT=PASS) and the four-way encoding probe (M3_LZ_FOURWAY_RESULT=PASS)
#   3        differential.sh: donor closure vs relocated classes byte-identical (oracle outputs, wire and M3EC corpora,
#            format oracle digests, cache traces, decoder agreement and four-way probe reports), mutants refused,
#            allocation bytes/op equal
#   4        native leaf, Linux hosts only (SKIP elsewhere): native-gate.sh over the differential scratch (javac -h headers,
#            gcc -Werror, CMake build + ctest, -Xcheck:jni oracles donor vs relocated, JNI vs Java parity, mutant refused)
#   naming   jdk_naming.py apply --only m3/compression/ reports 0 moves, 0 rewrites, 0 refusals (new files staged first)
# Every JVM gets -Djava.io.tmpdir=<scratch>/tmp (surefire through -DargLine).
# usage: JAVA_HOME=<JDK 21> verify.sh <donor-git-dir> <donor-rev> <scratch-dir> [maven-repo-local [maven-repo-local-tail]]
set -uo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$RECIPE_DIR/../../../.." && pwd)   # recipe dir = <root>/m3/compression/recipe/<name>
DONOR_GIT=$1; REV=$2; SCRATCH=$3; REPO=${4:-}; TAIL=${5:-}
mkdir -p "$SCRATCH/tmp"
TMPDIR_JVM=$(cd "$SCRATCH/tmp" && (pwd -W 2>/dev/null || pwd))
MVN_ARGS=(-B -ntp -f "$ROOT/m3/compression/pom.xml" "-DargLine=-Djava.io.tmpdir=$TMPDIR_JVM")
[ -n "$REPO" ] && MVN_ARGS+=("-Dmaven.repo.local=$REPO")
[ -n "$TAIL" ] && MVN_ARGS+=("-Dmaven.repo.local.tail=$TAIL")
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djava.io.tmpdir=$TMPDIR_JVM"
status=PASS
gate() { local name=$1 verdict=$2; shift 2; echo "GATE $name $verdict $*"; [ "$verdict" = PASS ] || [ "$verdict" = SKIP ] || status=FAIL; }

out=$(python -I "$RECIPE_DIR/relocate.py" --donor-git "$DONOR_GIT" --donor-rev "$REV" --module "$ROOT/m3/compression" --check 2>&1 | tail -1)
case "$out" in *"changed=0 provenance=UNCHANGED"*) gate recipe PASS "$out" ;; *) gate recipe FAIL "$out" ;; esac

(cd "$ROOT" && mvn "${MVN_ARGS[@]}" clean package > "$SCRATCH/mvn-package.log" 2>&1)
warnings=$(grep -cE '^\[WARNING\].*\.java' "$SCRATCH/mvn-package.log"); warnings=${warnings:-0}
if grep -q "BUILD SUCCESS" "$SCRATCH/mvn-package.log" && ! grep -qE "^\[ERROR\]" "$SCRATCH/mvn-package.log" && [ "$warnings" = 0 ]; then
    gate 1_package_lint_werror PASS "BUILD SUCCESS, java warnings=$warnings log=$SCRATCH/mvn-package.log"
else
    gate 1_package_lint_werror FAIL "java warnings=$warnings log=$SCRATCH/mvn-package.log"; grep -E "ERROR|FAIL" "$SCRATCH/mvn-package.log" | head -20
fi
figures=$(grep -E "^(M3 compression reusable-workspace oracle checks|M3 LZ index oracle checks|M3 compressed chunk cache checks|M3 compression (encoder oracle|adversarial|grammar) checks|M3 four-way LZ checks|M3_COMPRESSION_FORMAT_(INPUTS|LZ4_ROUNDTRIP|SNAPPY_ROUNDTRIP|RESULT)|M3_CHUNK_CACHE_(OPS|MODEL_MISMATCHES|RESULT)|M3_DECODER_AGREEMENT_(ENCODER_LZ4_ACCEPTED_BOTH|ENCODER_SNAPPY_ACCEPTED_BOTH|MALFORMED_LZ4_REFUSED_BOTH|MALFORMED_SNAPPY_REFUSED_BOTH|FUZZ_STREAMS|RESULT)|M3_LZ_FOURWAY_(TWIN_DIRECT_IDENTICAL|ROUNDTRIP_DONOR_DECODERS|RESULT))=" "$SCRATCH/mvn-package.log" | tr -d '\r' | tr '\n' ' ')
need="M3_COMPRESSION_FORMAT_RESULT=PASS"   # the decoder leaf's two oracles when the module carries them
[ -f "$ROOT/m3/compression/src/test/java/jdk/m3/compression/M3CompressionDecoderAgreement.java" ] && need="$need M3_DECODER_AGREEMENT_RESULT=PASS M3_LZ_FOURWAY_RESULT=PASS"
oracles=PASS; for marker in $need; do case "$figures" in *"$marker"*) ;; *) oracles=FAIL ;; esac; done
gate 2_oracle_test_phase "$oracles" "$figures"
python -I - "$ROOT/m3/compression/target/surefire-reports" <<'EOF'
import glob, re, sys
t = f = e = s = 0
for p in glob.glob(sys.argv[1] + "/TEST-*.xml"):
    m = re.search(r'tests="(\d+)" errors="(\d+)" skipped="(\d+)" failures="(\d+)"', open(p, encoding="utf-8").read(2000))
    if m: t += int(m[1]); e += int(m[2]); s += int(m[3]); f += int(m[4])
print("SUREFIRE tests=%d failures=%d errors=%d skipped=%d" % (t, f, e, s))
EOF

if bash "$RECIPE_DIR/differential.sh" "$DONOR_GIT" "$REV" "$SCRATCH/differential" > "$SCRATCH/differential.log" 2>&1; then
    gate 3_differential PASS "$(grep -E ' (IDENTICAL|DIFFERENT) |^mutant ' "$SCRATCH/differential.log" | awk '{ if ($1 == "mutant") print $2"="$3; else print $1"="$2 }' | tr '\n' ' ')"
else
    gate 3_differential FAIL "log=$SCRATCH/differential.log"; tail -8 "$SCRATCH/differential.log"
fi

if [ -f "$RECIPE_DIR/native-gate.sh" ] && [ "$(uname -s)" = Linux ]; then   # the native leaf's JNI lanes need a Linux host
    if bash "$RECIPE_DIR/native-gate.sh" "$SCRATCH/differential" "$SCRATCH/native-gate" > "$SCRATCH/native-gate.log" 2>&1; then
        gate 4_native PASS "$(grep -E 'IDENTICAL|MATCH|tests passed|^parity|^mutant' "$SCRATCH/native-gate.log" | cut -c1-90 | tr '\n' ';')"
    else
        gate 4_native FAIL "log=$SCRATCH/native-gate.log"; grep FAIL "$SCRATCH/native-gate.log" | head -8
    fi
elif [ -f "$RECIPE_DIR/native-gate.sh" ]; then
    gate 4_native SKIP "not a Linux host: run native-gate.sh <scratch>/differential <out> there (GATES.tsv native_gate)"
fi

naming=$(cd "$ROOT" && python -I m3/tooling/jdk-naming/jdk_naming.py apply --root . --only m3/compression/ 2>&1 | grep -E "^(JDK_NAMING|REFUSED)" | tail -1)
case "$naming" in *"moves=0 rewrites=0 refusals=0"*) gate naming PASS "$naming" ;; *) gate naming FAIL "$naming" ;; esac
echo "VERIFY $status"
[ "$status" = PASS ]
