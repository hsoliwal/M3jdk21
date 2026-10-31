#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Gate of the annotations convergence (run from the repository root; JAVA_HOME = JDK 21):
#   1. LEDGER: tools/AnnotationLedger.java over the vendored donors -> DONORS.tsv, ANNOTATIONS.tsv (every MAP_RULES row
#      must name a donor annotation).
#   2. COMPILE: src/jdk.m3.annotations with -Xlint:all (module name warning excepted, as in make/modules), -Xdoclint
#      all/protected and -Werror; with M3JDK_STANDARD_PLUGIN=<classes dir> also -Xplugin:M3JdkStandard.
#   3. API: every CANONICAL row's owner has the donor's targets, retention, elements and meta-annotations, read back by
#      the same ledger code from the compiled module sources.
#   4. JUNIT (with JUNIT_CONSOLE = a junit-platform-console-standalone jar): tests/JdkM3AnnotationsTest, one test per
#      MAP_RULES atom (CANONICAL: equal to the compiled donor; MAP: a type-use or type owner exists).
# Exit 0 = all three PASS. DONORS=<dir> overrides the donors root (default m3/vendor/third_party).
set -euo pipefail
T=m3/tooling/annotations-absorb; M=src/jdk.m3.annotations/share/classes; D="${DONORS:-m3/vendor/third_party}"
BIN="$JAVA_HOME/bin"; W=$(mktemp -d); trap 'rm -rf "$W"' EXIT
SEP=":"; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";"; W=$(cygpath -m "$W");; esac
mkdir -p "$W/donors" "$W/m3/donors/m3/src/main/java" "$W/mod"
for d in jspecify-1.0.1 checker-qual-3.55.1 annotations error_prone_annotations jsr305 j2objc-annotations; do
    cp -r "$D/$d" "$W/donors/$d"
done
"$BIN/java" "$T/tools/AnnotationLedger.java" "$W/donors" "$T/MAP_RULES.tsv" "$T"
echo "ANNOTATIONS_GATE ledger PASS"
plugin=(); [ -z "${M3JDK_STANDARD_PLUGIN:-}" ] || plugin=(-processorpath "$M3JDK_STANDARD_PLUGIN" -Xplugin:M3JdkStandard)
"$BIN/javac" --release 21 -Xlint:all,-module -Xdoclint:all/protected -Werror "${plugin[@]}" \
    --module-source-path "jdk.m3.annotations=$M" -m jdk.m3.annotations -d "$W/mod"
echo "ANNOTATIONS_GATE compile PASS"
(cd "$M" && find jdk -name '*.java' | tar -cf - -T -) | (cd "$W/m3/donors/m3/src/main/java" && tar -xf -)
printf 'donor_fqn\tdecision\trelation\tm3_owner\tnote\n' > "$W/m3/rules.tsv"
"$BIN/java" "$T/tools/AnnotationLedger.java" "$W/m3/donors" "$W/m3/rules.tsv" "$W/m3" 2> /dev/null
# compare targets, retention, elements, meta (columns 3-6) of each CANONICAL donor row with its owner's row
awk -F'\t' 'FNR == 1 { next } NR == FNR { if ($8 == "CANONICAL") want[$10] = $3 FS $4 FS $5 FS $6; next }
    { have[$2] = $3 FS $4 FS $5 FS $6 }
    END { for (o in want) { n++; if (have[o] != want[o]) { bad++; print "API MISMATCH " o ": " want[o] " vs " have[o] } }
          print "ANNOTATIONS_GATE api " (bad ? "FAIL" : "PASS") " canonical=" n " mismatches=" bad + 0; exit bad > 0 }' \
    "$T/ANNOTATIONS.tsv" "$W/m3/ANNOTATIONS.tsv"
if [ -n "${JUNIT_CONSOLE:-}" ]; then
    mkdir -p "$W/donor" "$W/test"
    "$BIN/javac" --release 21 -proc:none -nowarn -d "$W/donor" $(find "$W/donors/jspecify-1.0.1/src/main/java" \
        -name '*.java' ! -name module-info.java)
    CP="$W/mod/jdk.m3.annotations$SEP$W/donor"
    "$BIN/javac" --release 21 -proc:none -Xlint:all -Werror -cp "$CP$SEP$JUNIT_CONSOLE" -d "$W/test" \
        "$T/tests/JdkM3AnnotationsTest.java"
    "$BIN/java" -Dm3.annotations.rules="$T/MAP_RULES.tsv" -cp "$JUNIT_CONSOLE$SEP$CP$SEP$W/test" \
        org.junit.platform.console.ConsoleLauncher execute --disable-banner --details=summary \
        --select-class JdkM3AnnotationsTest > "$W/junit.log" 2>&1 || { cat "$W/junit.log"; exit 1; }
    echo "ANNOTATIONS_GATE junit PASS $(grep -oE '[0-9]+ tests successful' "$W/junit.log")"
fi
