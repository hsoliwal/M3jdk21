#!/usr/bin/env bash
# Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
# GPLv2 with the Classpath exception.
#
# Leaf 1b proof driver: JavaLangAccess.stringM3LeafRanges over M3String leaves.
# Compile-only gates against the sparse java.base sources (baseline twin for every gate), a
# component-level differential oracle on a stock JDK 21 (--patch-module java.base; only classes
# absent from the stock image are patched in), three source mutants that must FAIL with an
# AssertionError, and the qualified-export lint probe.
#
# usage: verify.sh <jdk-21-home> <M3jdk21 checkout root> <fresh output dir>
# Not a substitute for `make images` or jtreg on an M3JDK21 image (both NOT_RUN here).
set -u
export MSYS_NO_PATHCONV=1
JDK=$(cygpath -m "$1" 2>/dev/null || echo "$1")
ROOT=$(cygpath -m "$2" 2>/dev/null || echo "$2")
OUT=$(cygpath -m "$3" 2>/dev/null || echo "$3")
SRC="$ROOT/src/java.base/share/classes"
TST="$ROOT/test/jdk/java/lang/String/M3StringLeafRanges"
ORACLE="$TST/java.base/java/lang/M3StringLeafRangesOracle.java"
LEAF="$SRC/java/lang/M3StringLeafRanges.java"
CHANGED=("$LEAF" "$SRC/java/lang/System.java" "$SRC/jdk/internal/access/JavaLangAccess.java")
SEP=';'; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) ;; *) SEP=':';; esac
# make/modules/java.base/Java.gmk: DISABLED_WARNINGS_java += this-escape (JavaCompilation.gmk
# adds -g -Xlint:all ... -Werror); the gates below use the same lint set for java.base sources.
LINT=-Xlint:all,-this-escape
mkdir "$OUT" || { echo "output dir must be fresh: $OUT"; exit 2; }
GATES="$OUT/GATES.tsv"
printf 'gate\texpect\tpattern\trc\tverdict\tlog\n' > "$GATES"
hb() { echo "HEARTBEAT $(date -u +%FT%TZ) $*"; }

# gate <name> <0|nonzero> <ERE that the log must contain, or -> <cmd...>
gate() {
    local name=$1 expect=$2 pattern=$3; shift 3
    hb "gate $name"
    printf '%q ' "$@" > "$OUT/$name.cmd"; echo >> "$OUT/$name.cmd"
    "$@" > "$OUT/$name.log" 2>&1; local rc=$? verdict=FAIL ok=0
    if [ "$expect" = 0 ] && [ $rc -eq 0 ]; then ok=1; fi
    if [ "$expect" = nonzero ] && [ $rc -ne 0 ]; then ok=1; fi
    if [ $ok = 1 ] && { [ "$pattern" = - ] || grep -q -E "$pattern" "$OUT/$name.log"; }; then
        verdict=PASS
    fi
    printf '%s\t%s\t%s\t%s\t%s\t%s\n' "$name" "$expect" "$pattern" "$rc" "$verdict" "$name.log" >> "$GATES"
    echo "$name rc=$rc expect=$expect verdict=$verdict"
    [ $verdict = PASS ]
}
not_run() { printf '%s\t-\t-\t-\tNOT_RUN\t%s\n' "$1" "$2" >> "$GATES"; echo "$1 NOT_RUN $2"; }
diags() { grep -E ': (warning|error):' "$1" | sed -E 's#^.*[\\/]##; s/:[0-9]+:/:/' | sort; }

# Baseline overlay: HEAD versions of the two changed owners, ahead of the sparse sources.
mkdir -p "$OUT/base/java/lang" "$OUT/base/jdk/internal/access"
git -C "$ROOT" show HEAD:src/java.base/share/classes/java/lang/System.java > "$OUT/base/java/lang/System.java"
git -C "$ROOT" show HEAD:src/java.base/share/classes/jdk/internal/access/JavaLangAccess.java \
    > "$OUT/base/jdk/internal/access/JavaLangAccess.java"
BASE=("$OUT/base/java/lang/System.java" "$OUT/base/jdk/internal/access/JavaLangAccess.java")
BASEPATH="java.base=$OUT/base$SEP$SRC"
JAVAC="$JDK/bin/javac"

# G1: the exact requested command. ct.sym of --release 21 lacks jdk.internal.le/vm.ci/jvmstat
# and jdk.random, so java.base's own module-info fails module lint at HEAD as well.
gate G1_base_release21_werror nonzero 'module not found: jdk.internal.le' "$JAVAC" --release 21 \
    -Xlint:all -Werror --patch-module "$BASEPATH" -d "$OUT/c-g1b" "${BASE[@]}"
gate G1_cand_release21_werror nonzero 'module not found: jdk.internal.le' "$JAVAC" --release 21 \
    -Xlint:all -Werror --patch-module "java.base=$SRC" -d "$OUT/c-g1" "${CHANGED[@]}"
diags "$OUT/G1_base_release21_werror.log" > "$OUT/G1.base.diags"
diags "$OUT/G1_cand_release21_werror.log" > "$OUT/G1.cand.diags"
gate G1_delta_no_new_diagnostics 0 - diff "$OUT/G1.base.diags" "$OUT/G1.cand.diags"
# G2: --release 21 with make's lint set, module lint off (module-pack build.py: -Xlint:all,-module).
gate G2_base_release21_make_lint 0 - "$JAVAC" --release 21 "$LINT,-module" -Werror \
    --patch-module "$BASEPATH" -d "$OUT/c-g2b" "${BASE[@]}"
gate G2_cand_release21_make_lint 0 - "$JAVAC" --release 21 "$LINT,-module" -Werror \
    --patch-module "java.base=$SRC" -d "$OUT/c-g2" "${CHANGED[@]}"
# G3: system image (jdk.internal.* modules observable): make's lint set with module lint ON.
gate G3_base_system_make_lint 0 - "$JAVAC" "$LINT" -Werror --patch-module "$BASEPATH" \
    -d "$OUT/c-g3b" "${BASE[@]}"
gate G3_cand_system_make_lint 0 - "$JAVAC" "$LINT" -Werror --patch-module "java.base=$SRC" \
    -d "$OUT/c-g3" "${CHANGED[@]}"
# G3d: make's doclint options; the candidate may add no doclint diagnostic to the baseline set.
DOCLINT=(-Xdoclint:all/protected '-Xdoclint/package:java.*,javax.*')
"$JAVAC" "$LINT" "${DOCLINT[@]}" --patch-module "$BASEPATH" -d "$OUT/c-g3db" "${BASE[@]}" \
    > "$OUT/G3d.base.log" 2>&1
"$JAVAC" "$LINT" "${DOCLINT[@]}" --patch-module "java.base=$SRC" -d "$OUT/c-g3d" "${CHANGED[@]}" \
    > "$OUT/G3d.cand.log" 2>&1
diags "$OUT/G3d.base.log" > "$OUT/G3d.base.diags"
diags "$OUT/G3d.cand.log" > "$OUT/G3d.cand.diags"
gate G3d_doclint_no_new_diagnostics 0 - diff "$OUT/G3d.base.diags" "$OUT/G3d.cand.diags"
# G4: qualified-export probe. java.base's module-info (picked up from the patch path) exports
# jdk.internal.access to a module absent at compile time; module lint must reject it under
# make's -Werror, and only module lint (G4c control).
mkdir -p "$OUT/export"
sed '/^    exports jdk\.internal\.access to$/,/;$/ s/^        jdk\.crypto\.cryptoki;$/        jdk.crypto.cryptoki,\n        jdk.m3.text;/' \
    "$SRC/module-info.java" > "$OUT/export/module-info.java"
[ "$(grep -c '^        jdk.m3.text;$' "$OUT/export/module-info.java")" = 1 ] \
    || echo "export probe edit did not apply exactly once"
gate G4_export_to_absent_module nonzero 'module not found: jdk\.m3\.text' "$JAVAC" "$LINT" -Werror \
    --patch-module "java.base=$OUT/export$SEP$SRC" -d "$OUT/c-g4" "${CHANGED[@]}"
gate G4c_export_module_lint_off 0 - "$JAVAC" "$LINT,-module" -Werror \
    --patch-module "java.base=$OUT/export$SEP$SRC" -d "$OUT/c-g4c" "${CHANGED[@]}"

# Oracle: compile, then patch in only classes absent from the stock java.base image.
"$JDK/bin/jimage" list "$JDK/lib/modules" | tr -d '\r' \
    | awk '/^Module: /{m=$2; next} m=="java.base" && /\.class$/{gsub(/^ +/,""); print}' \
    | sort -u > "$OUT/stock-java-base.txt"
oracle() { # oracle <name> <0|nonzero> <run pattern> <overlay dir or ->
    local name=$1 expect=$2 pattern=$3 overlay=$4 path="$TST/java.base$SEP$SRC" leaf="$LEAF"
    if [ "$overlay" != - ]; then path="$overlay$SEP$path"; leaf="$overlay/java/lang/M3StringLeafRanges.java"; fi
    if ! gate "${name}_javac" 0 - "$JAVAC" --release 21 "$LINT,-module" -Werror \
            --patch-module "java.base=$path" -d "$OUT/$name-classes" "$ORACLE" "$leaf"; then
        not_run "${name}_run" "javac failed"; return
    fi
    local croot="$OUT/$name-classes"
    [ -d "$croot/java.base" ] && croot="$croot/java.base"
    (cd "$croot" && find . -name '*.class' | sed 's#^\./##' | sort -u) > "$OUT/$name-compiled.txt"
    # New top-level classes and their members only: never a nested class of a stock outer class.
    grep -v -x -F -f "$OUT/stock-java-base.txt" "$OUT/$name-compiled.txt" \
        | while read -r c; do
            outer="${c%%\$*}"; outer="${outer%.class}.class"
            grep -q -x -F "$outer" "$OUT/stock-java-base.txt" || echo "$c"
        done > "$OUT/$name-new.txt"
    while read -r c; do
        mkdir -p "$OUT/$name-patch/$(dirname "$c")"; cp "$croot/$c" "$OUT/$name-patch/$c"
    done < "$OUT/$name-new.txt"
    gate "${name}_run" "$expect" "$pattern" "$JDK/bin/java" -Xcheck:jni -Xshare:auto \
        --patch-module "java.base=$OUT/$name-patch" java.lang.M3StringLeafRangesOracle component
}
oracle O1_component 0 'M3_LEAF_RANGES_PASS mode=component stringLayout=stock' -
mutant() { # mutant <name> <sed expression>
    mkdir -p "$OUT/$1-overlay/java/lang"
    sed "$2" "$LEAF" > "$OUT/$1-overlay/java/lang/M3StringLeafRanges.java"
    if cmp -s "$LEAF" "$OUT/$1-overlay/java/lang/M3StringLeafRanges.java"; then
        not_run "$1_run" "mutant edit did not apply"; return
    fi
    oracle "$1" nonzero 'java\.lang\.AssertionError' "$OUT/$1-overlay"
}
mutant M1_start_plus_one 's/((long) start << (width - LATIN1_WIDTH))/((long) (start + 1) << (width - LATIN1_WIDTH))/'
mutant M2_byte_order_swapped 's/if (width == UTF16_WIDTH \&\& atom.bigEndian)/if (width == UTF16_WIDTH \&\& !atom.bigEndian)/'
mutant M3_inline_off_by_one 's/head |= (long) unit << (index \* BYTE_BITS);/head |= (long) unit << ((index + 1) * BYTE_BITS);/'
hb done
cat "$GATES"
