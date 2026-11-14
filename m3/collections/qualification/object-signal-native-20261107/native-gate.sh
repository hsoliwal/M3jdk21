#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Native gate of leaf object-signal-native-20261107 for a Linux host (2026-11-07 run: WSL Ubuntu 24.04, gcc 13.3.0,
# OpenJDK 21.0.12.1). Library m3signalorder = m3_signal_order_jni.c (M3SignalOrderNative) + m3_object_signal_jni.c
# (M3ObjectSignalOps), as the LIBRARIES registry of qualification/full-java-jni-20261018/verify.py builds it (one object
# per file through -include of its owner's javac -h header, one shared object per library; no CMake).
#   1. javac -h of both owners (over the module's compiled classes): the javac -h prototypes equal the C definitions.
#   2. gcc, 0 diagnostics: the release library (-std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC), the verify.py
#      flags with UBSan (-fsanitize=undefined -fno-sanitize-recover=undefined), the kernel without -include, the
#      master library (m3_signal_order_jni.c alone) and the donor library libm3objectsignal (donor C verbatim, donor
#      CMake flags -std=c11 -Wall -Wextra -Wpedantic -Werror); information: -Wconversion -Wshadow.
#   3. nm -D --defined-only exports: master library vs this library.
#   4. the dump driver of differential.sh (<queries>, seed 20261107): relocated Java (library absent), relocated native
#      (default JIT and -Xint, -Xcheck:jni), relocated native with the UBSan library, relocated with the library and
#      -Dm3.objectsignal.disableNative=true, donor Java and donor native (libm3objectsignal): every dump byte-identical,
#      and identical to the Windows donor dump of differential.sh. -Xlog:jni+resolve proves the native method is
#      linked; the dump's tables of 16,384 rows or more (>= 256 words) count the queries that take the native path.
#   5. the oracle (relocated and donor, Java and native) and the reference model (Java, native, native -Xint).
#   6. C mutants on copies of m3_object_signal_jni.c: the native relocated dump must differ from the Java dump.
# usage: native-gate.sh <out-dir> <differential-scratch-dir> [queries]
#   <differential-scratch-dir>: the scratch of differential.sh (donor-src, donor-classes, relocated-classes,
#   donor-dump-jit.txt); the module's target/classes and target/test-classes come from mvn package.
#   Env: JAVA_HOME (a Linux JDK 21; default /usr/lib/jvm/java-21-openjdk-amd64). Set HOME and TMPDIR outside the WSL
#   image when it lives on a forbidden disk; every JVM runs with -XX:-UsePerfData and -Djava.io.tmpdir=$TMPDIR.
set -uo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$HERE/../.." && pwd)
OUT=$1
DIFF=$2
QUERIES=${3:-20000}
SEED=20261107
JDK=${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}
export JAVA_HOME=$JDK PATH=$JDK/bin:$PATH
TMP=${TMPDIR:-$OUT/tmp}
rm -rf "$OUT"; mkdir -p "$OUT"/{hdr,hdr-classes,lib,ubsan,master,donor-lib,missing,run,mut,obj} "$TMP"
status=PASS
step() { echo "== $*"; }
fail() { echo "FAIL: $*"; status=FAIL; }
NATIVE="$MODULE/src/main/native/collections"
C="$NATIVE/m3_object_signal_jni.c"
ORDER_C="$NATIVE/m3_signal_order_jni.c"
DONOR_C=$(find "$DIFF/donor-src" -name 'object_signal_jni.c' | head -1)
JFLAGS="-XX:-UsePerfData -Djava.io.tmpdir=$TMP"
CP="$MODULE/target/classes:$MODULE/target/test-classes:$DIFF/relocated-classes"
DCP="$DIFF/donor-classes"
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$HERE/../object-signal-20261107/RELOCATION_MAP.tsv")
RELEASE="-std=c17 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC"
VERIFY="-std=c17 -Wall -Wextra -Werror -fsanitize=undefined -fno-sanitize-recover=undefined -O2 -fPIC"
INC="-I$JDK/include -I$JDK/include/linux -I$OUT/hdr"
blob() { python3 -c 'import hashlib, sys; d = open(sys.argv[1], "rb").read(); print(hashlib.sha1(b"blob %d\0" % len(d) + d).hexdigest())' "$1"; }
java -XX:-UsePerfData -version 2>&1 | head -1; gcc --version | head -1
for f in "$C" "$ORDER_C" "$DONOR_C"; do echo "C $(basename "$f") sha256=$(sha256sum "$f" | cut -c1-16) git_blob=$(blob "$f")"; done

step "1. javac -h of M3ObjectSignalOps and M3SignalOrderNative; prototypes vs C definitions"
javac -J-XX:-UsePerfData --release 21 -proc:none -Xlint:all,-module -Werror -cp "$MODULE/target/classes" -h "$OUT/hdr" \
    -d "$OUT/hdr-classes" "$MODULE/src/main/java/jdk/m3/collections/M3ObjectSignalOps.java" \
    "$MODULE/src/main/java/jdk/m3/collections/M3SignalOrderNative.java" || fail "javac -h"
ls "$OUT/hdr"
python3 - "$OUT/hdr" "$C" "$ORDER_C" > "$OUT/prototypes.txt" <<'PY' || fail "prototype mismatch"
import re, sys, pathlib
heads = ''.join(p.read_text() for p in sorted(pathlib.Path(sys.argv[1]).glob('*.h')))
src = ''.join(pathlib.Path(p).read_text() for p in sys.argv[2:])
norm = lambda s: re.sub(r'\s+', ' ', s).replace(' *', '*').strip()
bad = 0
declared = re.findall(r'JNIEXPORT (\w+) JNICALL (Java_\w+)\s*\(([^)]*)\);', heads)
for ret, name, params in declared:
    c = re.search(r'JNIEXPORT (\w+) JNICALL\s*' + name + r'\(([^)]*)\)\s*\{', src)
    ctypes = [norm(re.sub(r'\w+$', '', p.strip())) for p in c.group(2).split(',')] if c else []
    htypes = [norm(p) for p in params.split(',')]
    same = bool(c and c.group(1) == ret and htypes == ctypes)
    bad += not same
    print(('EQUAL ' if same else 'DIFFERENT ') + ret + ' ' + name + '(' + ', '.join(htypes) + ')'
          + ('' if same else ' | C: ' + (c.group(1) + str(ctypes) if c else 'absent')))
defined = set(re.findall(r'JNIEXPORT \w+ JNICALL\s*(Java_\w+)\(', src))
undeclared = defined - {name for _r, name, _p in declared}
for name in sorted(undeclared):
    print('UNDECLARED ' + name); bad += 1
print('prototypes=%d equal=%d' % (len(declared), len(declared) - bad))
sys.exit(1 if bad or not declared else 0)
PY
cat "$OUT/prototypes.txt"

step "2. gcc builds (0 diagnostics each)"
build_library() { # <dir> <flags> <kernel C> -> <dir>/libm3signalorder.so
    local dir=$1 flags=$2 kernel=$3
    gcc $flags $INC -include jdk_m3_collections_M3SignalOrderNative.h -c "$ORDER_C" -o "$dir/order.o" 2> "$dir/gcc.log" \
        && gcc $flags $INC -include jdk_m3_collections_M3ObjectSignalOps.h -c "$kernel" -o "$dir/signal.o" 2>> "$dir/gcc.log" \
        && gcc $flags -shared "$dir/order.o" "$dir/signal.o" -o "$dir/libm3signalorder.so" 2>> "$dir/gcc.log"
}
diagnostics() { grep -cE 'warning:|error:|note:' "$1" || true; }
build_library "$OUT/lib" "$RELEASE" "$C" || fail "gcc release"
echo "release library (-std=c17 -O2 -Wall -Wextra -Wpedantic -Werror): diagnostics $(diagnostics "$OUT/lib/gcc.log")"
build_library "$OUT/ubsan" "$VERIFY" "$C" || fail "gcc verify.py flags + UBSan"
echo "verify.py flags + UBSan library: diagnostics $(diagnostics "$OUT/ubsan/gcc.log")"
gcc $RELEASE -I"$JDK/include" -I"$JDK/include/linux" -c "$C" -o "$OUT/obj/plain.o" 2> "$OUT/obj/plain.log" || fail "gcc without -include"
echo "kernel without -include: diagnostics $(diagnostics "$OUT/obj/plain.log")"
gcc $RELEASE $INC -include jdk_m3_collections_M3SignalOrderNative.h -shared "$ORDER_C" -o "$OUT/master/libm3signalorder.so" \
    2> "$OUT/master/gcc.log" || fail "gcc master library"
echo "master library (m3_signal_order_jni.c alone): diagnostics $(diagnostics "$OUT/master/gcc.log")"
gcc -std=c11 -O2 -Wall -Wextra -Wpedantic -Werror -fPIC -I"$JDK/include" -I"$JDK/include/linux" -shared "$DONOR_C" \
    -o "$OUT/donor-lib/libm3objectsignal.so" 2> "$OUT/donor-lib/gcc.log" || fail "gcc donor library"
echo "donor library libm3objectsignal (donor C verbatim, donor CMake flags): diagnostics $(diagnostics "$OUT/donor-lib/gcc.log")"
gcc -std=c17 -O2 -Wall -Wextra -Wpedantic -Wconversion -Wshadow -fPIC $INC -c "$C" -o "$OUT/obj/wconv.o" 2> "$OUT/obj/wconv.log"
echo "information: -Wconversion -Wshadow warnings on the kernel: $(grep -c 'warning:' "$OUT/obj/wconv.log" || true)"

step "3. nm -D exports"
nm -D --defined-only "$OUT/master/libm3signalorder.so" | grep ' Java_' > "$OUT/exports-master.txt"
nm -D --defined-only "$OUT/lib/libm3signalorder.so" | grep ' Java_' > "$OUT/exports.txt"
nm -D --defined-only "$OUT/ubsan/libm3signalorder.so" | grep ' Java_' > "$OUT/exports-ubsan.txt"
cat "$OUT/exports.txt"
echo "exports: master $(wc -l < "$OUT/exports-master.txt"), this library $(wc -l < "$OUT/exports.txt"), UBSan library $(wc -l < "$OUT/exports-ubsan.txt")"
grep -q ' Java_jdk_m3_collections_M3ObjectSignalOps_applyFeatureMasksNative$' "$OUT/exports.txt" || fail "kernel export missing"
grep -q ' Java_jdk_m3_collections_M3SignalOrderNative_sort0$' "$OUT/exports.txt" || fail "sort0 export missing"

step "4. dumps: Java, native (JIT, -Xint, -Xcheck:jni), UBSan, disableNative, donor Java, donor native"
dump() { # <name> <classpath> <main package> <library dir> [jvm flags...]
    local name=$1 cp=$2 pkg=$3 lib=$4; shift 4
    java $JFLAGS "$@" -Djava.library.path="$lib" -cp "$cp" "$pkg.M3ObjectSignalDump" "$QUERIES" "$SEED" \
        > "$OUT/run/$name.txt" 2> "$OUT/run/$name.err" || fail "dump $name"
}
dump java-jit "$CP" jdk.m3.collections "$OUT/missing"
dump java-xint "$CP" jdk.m3.collections "$OUT/missing" -Xint
dump native-jit "$CP" jdk.m3.collections "$OUT/lib" -Xcheck:jni
echo "heartbeat native-jit $(date +%H:%M:%S)"
dump native-xint "$CP" jdk.m3.collections "$OUT/lib" -Xcheck:jni -Xint
echo "heartbeat native-xint $(date +%H:%M:%S)"
dump native-ubsan "$CP" jdk.m3.collections "$OUT/ubsan" -Xcheck:jni
dump native-disabled "$CP" jdk.m3.collections "$OUT/lib" -Dm3.objectsignal.disableNative=true
dump donor-java "$DCP" "$donor_package" "$OUT/missing"
dump donor-native "$DCP" "$donor_package" "$OUT/donor-lib" -Xcheck:jni
java $JFLAGS -Xlog:jni+resolve=debug -Djava.library.path="$OUT/lib" -cp "$CP" jdk.m3.collections.M3ObjectSignalDump 2000 "$SEED" \
    > "$OUT/run/resolve.txt" 2>&1 || fail "dump with -Xlog:jni+resolve"
echo "native link proof: $(grep -c 'M3ObjectSignalOps.applyFeatureMasksNative' "$OUT/run/resolve.txt") -Xlog:jni+resolve line(s): $(grep -m1 -o '\[[^]]*M3ObjectSignalOps.applyFeatureMasksNative[^]]*\]' "$OUT/run/resolve.txt")"
reference="$OUT/run/java-jit.txt"
for name in java-xint native-jit native-xint native-ubsan native-disabled donor-java donor-native; do
    if cmp -s "$reference" "$OUT/run/$name.txt"; then verdict=IDENTICAL; else verdict=DIFFERENT; status=FAIL; fi
    echo "dump $name vs java-jit: $verdict"
done
if [ -f "$DIFF/donor-dump-jit.txt" ]; then
    # a Windows JVM prints CRLF line ends (println uses line.separator); the comparison is of the line contents
    tr -d '\r' < "$DIFF/donor-dump-jit.txt" > "$OUT/run/windows-donor-lf.txt"
    cmp -s "$reference" "$OUT/run/windows-donor-lf.txt" \
        && echo "dump java-jit vs the Windows donor dump (CRLF -> LF): IDENTICAL ($(wc -c < "$DIFF/donor-dump-jit.txt") Windows bytes)" \
        || { echo "dump java-jit vs the Windows donor dump: DIFFERENT"; status=FAIL; }
fi
echo "dump sha256=$(sha256sum "$reference" | cut -c1-16) bytes=$(wc -c < "$reference") lines=$(wc -l < "$reference")"
awk '$2 == "table" {split($4, s, "="); big = (s[2] + 0 >= 16384)} $2 == "query" && big {n++} END {print "queries on tables of >= 16,384 rows (>= 256 words, native path): " n+0}' "$reference"
echo "-Xcheck:jni warning lines (native runs): $(cat "$OUT"/run/native-jit.err "$OUT"/run/native-xint.err "$OUT"/run/native-ubsan.err "$OUT"/run/donor-native.err | grep -ci 'warning' || true)"
echo "UBSan runtime errors: $(grep -c 'runtime error' "$OUT/run/native-ubsan.err" || true); stderr bytes of all dump runs: $(cat "$OUT"/run/*.err | wc -c)"

step "5. oracle and reference model"
oracle() { # <name> <classpath> <class> <library dir> [flags]
    local name=$1 cp=$2 cls=$3 lib=$4; shift 4
    java $JFLAGS "$@" -Djava.library.path="$lib" -cp "$cp" "$cls" > "$OUT/run/$name.txt" 2> "$OUT/run/$name.err" || fail "$name"
    echo "$name: $(cat "$OUT/run/$name.txt")"
}
oracle oracle-java "$CP" jdk.m3.collections.M3ObjectSignalSelfTest "$OUT/missing"
oracle oracle-native "$CP" jdk.m3.collections.M3ObjectSignalSelfTest "$OUT/lib" -Xcheck:jni
oracle oracle-donor-java "$DCP" "$donor_package.ObjectSignalSelfTest" "$OUT/missing"
oracle oracle-donor-native "$DCP" "$donor_package.ObjectSignalSelfTest" "$OUT/donor-lib" -Xcheck:jni
cmp -s "$OUT/run/oracle-java.txt" "$OUT/run/oracle-donor-java.txt" && echo "oracle Java relocated = donor: IDENTICAL" || fail "oracle Java differs"
cmp -s "$OUT/run/oracle-native.txt" "$OUT/run/oracle-donor-native.txt" && echo "oracle native relocated = donor: IDENTICAL" || fail "oracle native differs"
grep -q 'JNI=true' "$OUT/run/oracle-native.txt" || fail "oracle did not load the library"
oracle model-java "$CP" jdk.m3.collections.M3ObjectSignalReferenceModel "$OUT/missing"
oracle model-native "$CP" jdk.m3.collections.M3ObjectSignalReferenceModel "$OUT/lib" -Xcheck:jni
oracle model-native-xint "$CP" jdk.m3.collections.M3ObjectSignalReferenceModel "$OUT/lib" -Xcheck:jni -Xint
echo "-Xcheck:jni warning lines (oracle and model): $(cat "$OUT"/run/oracle-native.err "$OUT"/run/oracle-donor-native.err "$OUT"/run/model-native*.err | grep -ci 'warning' || true)"

step "6. C mutants: the native relocated dump must differ from the Java dump"
killed=0; total=0
cmutant() { # <id> <occurrence (1-based)> <exact text> <replacement> <what>
    local id=$1 nth=$2 from=$3 to=$4 what=$5 dir="$OUT/mut/$1"
    total=$((total + 1)); mkdir -p "$dir"
    FROM="$from" TO="$to" NTH="$nth" python3 - "$C" "$dir/m.c" <<'PY' || { fail "mutant $id did not apply"; return; }
import os, sys
src = open(sys.argv[1]).read(); old, new, nth = os.environ['FROM'], os.environ['TO'], int(os.environ['NTH'])
at = -1
for _ in range(nth):
    at = src.find(old, at + 1)
    if at < 0: sys.exit(1)
open(sys.argv[2], 'w').write(src[:at] + new + src[at + len(old):])
PY
    build_library "$dir" "$RELEASE" "$dir/m.c" || { fail "mutant $id gcc"; return; }
    java $JFLAGS -Djava.library.path="$dir" -cp "$CP" jdk.m3.collections.M3ObjectSignalDump "$QUERIES" "$SEED" \
        > "$dir/dump.txt" 2> "$dir/dump.err"
    local rc=$?
    if [ $rc != 0 ] || ! cmp -s "$dir/dump.txt" "$reference"; then
        killed=$((killed + 1))
        echo "$id KILLED ($what): exit $rc, first difference at $( { cmp "$dir/dump.txt" "$reference" 2>&1 || true; } | grep -o 'line [0-9]*' | head -n 1)"
    else
        echo "$id SURVIVED ($what)"; status=FAIL
    fi
}
cmutant CM1 1 '& ~(uint64_t)planes[base + (size_t)word]' '& (uint64_t)planes[base + (size_t)word]' 'forbidden plane applied as a required plane'
cmutant CM2 1 'uint64_t forbidden = (uint64_t)forbidden_bits;' 'uint64_t forbidden = (uint64_t)forbidden_bits & UINT64_C(0);' 'forbidden bits ignored (parameter still read: -Werror=unused-parameter)'
cmutant CM3 1 'const size_t base = (size_t)feature * (size_t)word_count;' 'const size_t base = (size_t)(feature ^ 1u) * (size_t)word_count;' 'required bit reads the neighbouring plane'
cmutant CM4 1 'for (jint word = 0; word < word_count; word++) {' 'for (jint word = 0; word < word_count - 1; word++) {' 'required pass skips the last word'
cmutant CM5 2 'for (jint word = 0; word < word_count; word++) {' 'for (jint word = 1; word < word_count; word++) {' 'forbidden pass skips the first word'
echo "C MUTANTS killed=$killed of $total"
[ "$killed" = "$total" ] || status=FAIL
echo "NATIVE_GATE $status"
[ "$status" = PASS ]
