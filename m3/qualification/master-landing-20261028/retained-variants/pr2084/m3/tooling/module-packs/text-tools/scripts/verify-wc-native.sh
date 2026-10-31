#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
#
# Leaf-1 proof for the native wc kernel (libm3textio) and jdk.m3.text.Wc on a stock
# JDK 21 plus gcc, without an M3 JDK image (build.py stays the image path).
# Prints one PASS/FAIL/NOT_RUN/HOLD line per gate with parsed counts (BLLSE) and
# keeps every raw log under OUT. Exit status 1 when any gate FAILs.
#
#   OUT=/fresh/dir \
#   UNIX4J_BASE_JAR=.../unix4j-base-0.6.jar UNIX4J_COMMAND_JAR=.../unix4j-command-0.6.jar \
#   [JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64] [CC=gcc] \
#   [MINGW_CC=/mnt/c/.../gcc.exe WIN_JAVA_HOME='/mnt/c/Program Files/Java/jdk-21'] \
#   bash m3/tooling/module-packs/text-tools/scripts/verify-wc-native.sh
set -u
PACK=$(cd "$(dirname "$0")/.." && pwd)
INCLUDE=$(cd "$PACK/../../../native/include" && pwd)
JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-21-openjdk-amd64}
CC=${CC:-gcc}
OUT=${OUT:?set OUT to a fresh output directory}
BASE_SHA=74e8870a54f7a0b343233e997129f283dbaf048b9debf1f568e6a5c07dcb6d8c
COMMAND_SHA=52e9915fb0f3111bccb5da31bea557fcc795ef11f56ece8aac8e5027310b5639
FLIP_FROM='UINT64_C(0x00000001F0003E00)'
FLIP_TO='UINT64_C(0x0000000170003E00)'
EXPECTED_SYMBOLS='Java_com_synexia_unix4jm3_io_FileTools_mkfifo0 Java_jdk_m3_io_FileTools_mkfifo0 Java_jdk_m3_text_WcNative_count0 Java_jdk_m3_text_WcNative_countLines0'
CFLAGS='-std=c11 -Wall -Wextra -Werror -Wconversion -shared'
pass=0; fail=0; not_run=0; hold=0

gate() { # status name detail
    echo "$1 $2 $3"
    case $1 in PASS) pass=$((pass + 1));; FAIL) fail=$((fail + 1));; NOT_RUN) not_run=$((not_run + 1));; HOLD) hold=$((hold + 1));; esac
}
count() { local n; n=$(grep -cE "$1" "$2" 2>/dev/null); echo "${n:-0}"; }
javac_diag() { # log -> "errors=N warnings=N categories=..."
    local e w c
    e=$(count '^[^ ].*\.java:[0-9]+: error: |^error: ' "$1")
    w=$(count '^[^ ].*\.java:[0-9]+: warning: ' "$1")
    c=$(sed -n 's/^[^ ].*\.java:[0-9]*: warning: \[\([a-z-]*\)\].*/\1/p' "$1" | sort | uniq -c | awk '{printf "%s%s:%s", (NR > 1 ? "," : ""), $2, $1}')
    echo "errors=$e warnings=$w categories=${c:-none}"
}
gcc_diag() { # log -> "errors=N warnings=N first=..."
    local e w f
    e=$(count ':[0-9]+:[0-9]+: (fatal )?error: ' "$1")
    w=$(count ':[0-9]+:[0-9]+: warning: ' "$1")
    f=$(grep -m1 -E ':[0-9]+:[0-9]+: ((fatal )?error|warning): ' "$1" | sed 's/^.*\/\([^/]*:[0-9]*:[0-9]*:\)/\1/')
    echo "errors=$e warnings=$w${f:+ first=$f}"
}

[ -e "$OUT" ] && [ -n "$(ls -A "$OUT" 2>/dev/null)" ] && { echo "FAIL setup OUT must be fresh: $OUT"; exit 1; }
mkdir -p "$OUT"/{gen,mods,test,lib,empty,flip,win} || exit 1
echo "INFO pack=$PACK java=$("$JAVA_HOME/bin/java" -version 2>&1 | head -1) cc=$($CC --version | head -1)"

# 0. Oracle jars: Maven Central unix4j 0.6, pinned by SHA-256.
base_ok=$(sha256sum "${UNIX4J_BASE_JAR:-/missing}" 2>/dev/null | cut -c1-64)
command_ok=$(sha256sum "${UNIX4J_COMMAND_JAR:-/missing}" 2>/dev/null | cut -c1-64)
if [ "$base_ok" = "$BASE_SHA" ] && [ "$command_ok" = "$COMMAND_SHA" ]; then
    gate PASS oracle-jars "unix4j-base-0.6=$BASE_SHA unix4j-command-0.6=$COMMAND_SHA"
else
    gate FAIL oracle-jars "base=${base_ok:-missing} command=${command_ok:-missing}"
fi
CP="$UNIX4J_BASE_JAR:$UNIX4J_COMMAND_JAR"

# 1. Template-first table: regenerate and require a byte-identical fixed point.
"$JAVA_HOME/bin/java" "$PACK/native/generator/M3TextioWhitespaceTableGenerator.java" "$OUT/gen/m3textio_whitespace.h" > "$OUT/gen/generator.log" 2>&1
if cmp -s "$OUT/gen/m3textio_whitespace.h" "$PACK/native/m3textio_whitespace.h"; then
    gate PASS generator-fixed-point "header_sha256=$(sha256sum "$PACK/native/m3textio_whitespace.h" | cut -c1-64) $(grep -o 'Whitespace units: [0-9]*' "$PACK/native/m3textio_whitespace.h" | sed 's/.*: /whitespace_units=/') $(grep -o 'SHA-256 ([^)]*): [0-9a-f]*' "$PACK/native/m3textio_whitespace.h" | sed 's/.*: /table_sha256=/')"
else
    gate FAIL generator-fixed-point "regenerated header differs: $(diff "$OUT/gen/m3textio_whitespace.h" "$PACK/native/m3textio_whitespace.h" | grep -c '^[<>]') lines"
fi

# 1b. Atoms: every ATOMS.tsv range still hashes to its recorded SHA-256 and starts at its
#     //FLLDCIM: anchor; ledgers keep their declared column counts.
atoms=0; drift=""; anchors=0
while IFS=$'\t' read -r atom kind _ _ _ _ src sha; do
    [ "$atom" = atom ] && continue
    file=${src%%#*}; range=${src#*#L}; first=${range%%-L*}; last=${range#*-L}
    atoms=$((atoms + 1))
    [ "$(sed -n "${first},${last}p" "$PACK/$file" | sha256sum | cut -c1-64)" = "$sha" ] || drift="$drift $atom"
    sed -n "${first}p" "$PACK/$file" | grep -q "//FLLDCIM: .* - atom $atom: " && anchors=$((anchors + 1))
done < "$PACK/ATOMS.tsv"
[ -z "$drift" ] && [ "$anchors" = "$atoms" ] && gate PASS atoms-hash-anchors "atoms=$atoms hash_mismatches=0 anchored=$anchors" \
    || gate FAIL atoms-hash-anchors "atoms=$atoms drift=[${drift# }] anchored=$anchors"
shape=$(for spec in FOSS_REUSE_DECISION.tsv:10 PATTERNS.tsv:5 RECIPE_MEMORY.tsv:8 ATOMS.tsv:8; do
    awk -F'\t' -v n="${spec#*:}" -v f="${spec%%:*}" 'NF != n {bad++} END {printf "%s:%d:%d ", f, NR, bad + 0}' "$PACK/${spec%%:*}"; done)
echo "$shape" | grep -qE ':[1-9][0-9]*:0 .*:[1-9][0-9]*:0 .*:[1-9][0-9]*:0 .*:[1-9][0-9]*:0 $' \
    && gate PASS ledger-columns "file:rows:bad_rows ${shape% }" || gate FAIL ledger-columns "file:rows:bad_rows ${shape% }"

# 2. javac on the pack modules that a stock JDK 21 can compile.
"$JAVA_HOME/bin/javac" --release 21 -Xlint:all -Werror --module-source-path "$PACK/src" -d "$OUT/strict" \
    --module jdk.m3.text,jdk.m3.structured > "$OUT/javac-xlint-all.log" 2>&1
rc=$?; d=$(javac_diag "$OUT/javac-xlint-all.log")
if [ $rc -eq 0 ]; then gate PASS javac-xlint-all "exit=0 $d"
elif [ "$(count '^[^ ].*\.java:[0-9]+: (error|warning): ' "$OUT/javac-xlint-all.log")" = "$(count '^[^ ].*module-info\.java:[0-9]+: warning: \[module\] module name component m3 should avoid terminal digits' "$OUT/javac-xlint-all.log")" ]; then
    gate HOLD javac-xlint-all "exit=$rc $d only pre-existing module-info [module] terminal-digit warnings; build.py uses -Xlint:all,-module"
else gate FAIL javac-xlint-all "exit=$rc $d"; fi
"$JAVA_HOME/bin/javac" --release 21 -Xlint:all,-module -Werror --module-source-path "$PACK/src" -d "$OUT/mods" \
    --module jdk.m3.text,jdk.m3.structured > "$OUT/javac-modules.log" 2>&1
rc=$?; d=$(javac_diag "$OUT/javac-modules.log")
[ $rc -eq 0 ] && [ "${d%% *}" = "errors=0" ] && gate PASS javac-xlint-all-minus-module "exit=0 $d modules=jdk.m3.text,jdk.m3.structured" \
    || gate FAIL javac-xlint-all-minus-module "exit=$rc $d"
"$JAVA_HOME/bin/javac" --release 21 -Xlint:all,-module -Werror --module-source-path "$PACK/src" -d "$OUT/io" \
    --module jdk.m3.io > "$OUT/javac-io.log" 2>&1
rc=$?; m3file=$(count 'M3File\.java:[0-9]+: error: cannot find symbol' "$OUT/javac-io.log")
if [ $rc -eq 0 ]; then gate PASS javac-jdk.m3.io "exit=0"
elif [ "$(count ': error: ' "$OUT/javac-io.log")" = "$m3file" ]; then
    gate NOT_RUN javac-jdk.m3.io "exit=$rc M3File.java cannot-find-symbol=$m3file; needs M3 java.base ProgressMonitor (M3 image only)"
else gate FAIL javac-jdk.m3.io "exit=$rc $(javac_diag "$OUT/javac-io.log")"; fi
"$JAVA_HOME/bin/javac" --release 21 -Xlint:all -Werror --module-path "$OUT/mods" --add-modules jdk.m3.text -cp "$CP" \
    -d "$OUT/test" "$PACK/fixtures/src/test/java/M3WcNativeSelfTest.java" > "$OUT/javac-test.log" 2>&1
rc=$?; d=$(javac_diag "$OUT/javac-test.log")
[ $rc -eq 0 ] && gate PASS javac-selftest "exit=0 $d" || gate FAIL javac-selftest "exit=$rc $d"

# 3. Native: strict gcc, exact exported JNI symbol set.
build_so() { # src_dir out_lib log
    $CC $CFLAGS -fPIC -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" -I"$INCLUDE" -o "$2" "$1/m3textio.c" > "$3" 2>&1
}
build_so "$PACK/native" "$OUT/lib/libm3textio.so" "$OUT/gcc-linux.log"
rc=$?; d=$(gcc_diag "$OUT/gcc-linux.log")
[ $rc -eq 0 ] && gate PASS gcc-linux "exit=0 $d flags=$CFLAGS -fPIC" || gate FAIL gcc-linux "exit=$rc $d"
nm -D --defined-only "$OUT/lib/libm3textio.so" > "$OUT/nm.log" 2>&1
symbols=$(awk '$2 == "T" && $3 ~ /^Java_/ {print $3}' "$OUT/nm.log" | sort | tr '\n' ' ' | sed 's/ $//')
expected=$(echo "$EXPECTED_SYMBOLS" | tr ' ' '\n' | sort | tr '\n' ' ' | sed 's/ $//')
[ "$symbols" = "$expected" ] && gate PASS nm-jni-symbols "count=$(echo "$symbols" | wc -w) $symbols" \
    || gate FAIL nm-jni-symbols "got=[$symbols] expected=[$expected]"

# 4. Differential self-test under -Xcheck:jni.
run_test() { # library_dir log [args]
    local dir=$1 log=$2; shift 2
    "$JAVA_HOME/bin/java" -Xcheck:jni --module-path "$OUT/mods" --add-modules jdk.m3.text -cp "$OUT/test:$CP" \
        -Djava.library.path="$dir" M3WcNativeSelfTest "$@" > "$log" 2>&1
}
run_test "$OUT/lib" "$OUT/selftest.log"
rc=$?; warn=$(count 'WARNING in native method|WARNING: JNI' "$OUT/selftest.log")
sed -n 's/^\(PASS\|FAIL\) /  \1 /p' "$OUT/selftest.log"
summary=$(grep '^M3_WC_NATIVE_SELFTEST' "$OUT/selftest.log")
[ $rc -eq 0 ] && [ "$warn" = 0 ] && [ "$(count '^FAIL ' "$OUT/selftest.log")" = 0 ] && [ -n "$summary" ] \
    && gate PASS selftest-xcheck-jni "exit=0 xcheck_warnings=0 gates=$(count '^PASS ' "$OUT/selftest.log") ${summary#M3_WC_NATIVE_SELFTEST PASS }" \
    || gate FAIL selftest-xcheck-jni "exit=$rc xcheck_warnings=$warn ${summary:-no summary}"

# 5. Discriminators: library absent, then one wrong table entry (U+001F).
run_test "$OUT/empty" "$OUT/discriminator-link.log" unsatisfied-link
rc=$?; line=$(grep -m1 'discriminator-unsatisfied-link' "$OUT/discriminator-link.log")
[ $rc -eq 0 ] && [ "${line%% *}" = PASS ] && gate PASS discriminator-unsatisfied-link "${line#PASS discriminator-unsatisfied-link }" \
    || gate FAIL discriminator-unsatisfied-link "exit=$rc ${line:-$(tail -1 "$OUT/discriminator-link.log")}"
cp "$PACK/native/m3textio.c" "$PACK/native/m3textio_whitespace.h" "$OUT/flip/"
before=$(grep -cF "$FLIP_FROM" "$OUT/flip/m3textio_whitespace.h")
sed -i "s/$FLIP_FROM/$FLIP_TO/" "$OUT/flip/m3textio_whitespace.h"
after=$(grep -cF "$FLIP_TO" "$OUT/flip/m3textio_whitespace.h")
mkdir -p "$OUT/flip/lib"
build_so "$OUT/flip" "$OUT/flip/lib/libm3textio.so" "$OUT/flip/gcc.log"
run_test "$OUT/flip/lib" "$OUT/discriminator-flip.log"
rc=$?; table=$(grep -m1 'table-exhaustive' "$OUT/discriminator-flip.log"); modes=$(grep -m1 'mode-switch' "$OUT/discriminator-flip.log")
if [ "$before" = 1 ] && [ "$after" = 1 ] && [ $rc -ne 0 ] && echo "$table" | grep -q '^FAIL .*mismatches=3 first=U+001F$' \
        && echo "$modes" | grep -q '^FAIL .*disagreement=M3 JNI wc disagrees'; then
    gate PASS discriminator-table-flip "edited=1 exit=$rc failing_gates=$(count '^FAIL ' "$OUT/discriminator-flip.log") table=[${table#FAIL table-exhaustive }]"
else
    gate FAIL discriminator-table-flip "before=$before after=$after exit=$rc table=[$table] modes=[$modes]"
fi

# 6. Optional Windows mingw build (and run) when invoked from WSL with interop.
if [ -n "${MINGW_CC:-}" ] && [ -n "${WIN_JAVA_HOME:-}" ] && command -v wslpath > /dev/null; then
    w() { wslpath -w "$1"; }
    "$MINGW_CC" $CFLAGS -I"$(w "$WIN_JAVA_HOME/include")" -I"$(w "$WIN_JAVA_HOME/include/win32")" -I"$(w "$INCLUDE")" \
        -o "$(w "$OUT/win")\\m3textio.dll" "$(w "$PACK/native/m3textio.c")" > "$OUT/gcc-mingw.log" 2>&1
    rc=$?; d=$(gcc_diag "$OUT/gcc-mingw.log")
    [ $rc -eq 0 ] && gate PASS gcc-mingw-windows "exit=0 $d $("$MINGW_CC" --version | head -1 | tr -d '\r')" \
        || gate FAIL gcc-mingw-windows "exit=$rc $d"
    if [ $rc -eq 0 ]; then
        "$WIN_JAVA_HOME/bin/java.exe" -Xcheck:jni --module-path "$(w "$OUT/mods")" --add-modules jdk.m3.text \
            -cp "$(w "$OUT/test");$(w "$UNIX4J_BASE_JAR");$(w "$UNIX4J_COMMAND_JAR")" \
            -Djava.library.path="$(w "$OUT/win")" M3WcNativeSelfTest > "$OUT/selftest-windows.log" 2>&1
        rc=$?; summary=$(grep '^M3_WC_NATIVE_SELFTEST' "$OUT/selftest-windows.log" | tr -d '\r')
        warn=$(count 'WARNING in native method|WARNING: JNI' "$OUT/selftest-windows.log")
        [ $rc -eq 0 ] && [ "$warn" = 0 ] && [ -n "$summary" ] && gate PASS selftest-windows-xcheck-jni "exit=0 xcheck_warnings=0 ${summary#M3_WC_NATIVE_SELFTEST PASS }" \
            || gate FAIL selftest-windows-xcheck-jni "exit=$rc xcheck_warnings=$warn ${summary:-no summary}"
    fi
else
    gate NOT_RUN gcc-mingw-windows "set MINGW_CC and WIN_JAVA_HOME and run under WSL"
fi

echo "M3_WC_VERIFY $([ $fail -eq 0 ] && echo PASS || echo FAIL) pass=$pass fail=$fail not_run=$not_run hold=$hold out=$OUT"
[ $fail -eq 0 ]
