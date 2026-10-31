#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# absorb_leaf1.sh run|check <repo> <work_dir> : the Skija absorption recipe end to end, from the donor to the leaf-0
# ledgers and the jdk.m3.graphics sources (CPU only, deterministic, fail-closed). Every output is generated under
# <work_dir>/out and gated there first; "run" then copies the outputs into <repo>, "check" only compares. Both exit 1
# when <repo> differs from the outputs at the end, so a "check" after a "run" proves the fixed point.
#
# Inputs (environment; the values of the leaf-1 runs are in leaf1/GATES.tsv):
#   SKIJA_ROOT     HumbleUI Skija 0.143.18 source tree (shared/java, shared/java9, platform/cc)
#   DLL_EXPORTS    JNI symbols exported by the shipped 0.143.18 skija.dll, one per line
#   TYPES_CLASSES  class list of types-0.2.0.jar (its io.github.humbleui.types.* lines are used)
#   DONOR_DL       directory with lombok-1.18.48.jar, annotations-20.1.0.jar and types-0.2.0-sources.jar
#   DONOR_JARS     the published skija-shared-0.143.18.jar and types-0.2.0.jar, separated by ';'
#   JDK_BIN        JDK 21 bin directory (default /c/Program Files/Java/jdk-21/bin); PYTHON (default python)
#
# Stages                                                        outputs (repository paths)
#   L0  skija_patterns.py, gen_rename.py                        ledgers/*.tsv, recipe/M3SkijaToGraphics.yml
#   S0  mr_select.py (MR_SELECT v1)                             leaf1/MR_SELECT.tsv
#   S1  lombok delombok, delombok_clean.py
#   S2  TYPE_RENAME.tsv plus the SneakyThrow helper row         leaf1/TYPE_RENAME_L1.tsv
#       absorb_rename.py with the string ledger                 leaf1/RENAME_COUNTS.tsv, leaf1/STRING_MENTIONS.tsv
#   S3  normalize_ws.py (NORMALIZE_WS v1)                       src/jdk.m3.graphics/share/classes/jdk/**.java
# Gates
#   G1  javac --release 21 -proc:none of stage 1 and of stage 2: 0 errors
#   G2  api_surface.sh: stage 1 renamed by rename_sed.py == stage 2; no humbleui or jetbrains name in the surface
#   G3  the published jars as Java 21 loads them == stage 1, except the synthetic switch-map classes of the other
#       javac and the added SneakyThrow helper
#   G4  javac --release 21 -Xlint:all of the module: 0 errors; every warning kind is disabled in Java.gmk; jdeps: the
#       module depends on java.base only (JDK_SELF_CALL: no absorbed call can re-enter a JDK API the module implements)
#   G5  M3GraphicsLeaf1Probe on this JDK with the module on the module path (F1, F2, F3, F4)
#   G6  the probe inside a jlink image of java.base + jdk.m3.graphics: M3Library.load takes the JDK-bundled branch
#       (skipped when the JDK has no jmods)
set -euo pipefail

mode="${1:-}"; repo="${2:-}"; work="${3:-}"
case "$mode" in run|check) ;; *) echo "usage: $0 run|check <repo> <work_dir>" >&2; exit 2;; esac
[ -n "$repo" ] && [ -n "$work" ] || { echo "usage: $0 run|check <repo> <work_dir>" >&2; exit 2; }
: "${SKIJA_ROOT:?}" "${DLL_EXPORTS:?}" "${TYPES_CLASSES:?}" "${DONOR_DL:?}" "${DONOR_JARS:?}"
J="${JDK_BIN:-/c/Program Files/Java/jdk-21/bin}"
export JDK_BIN="$J"
PY="${PYTHON:-python}"
RELEASE=21
MAX_WARNINGS=10000
OWNER=jdk.m3.graphics
OWNER_IMPL=jdk.m3.graphics.impl
MODULE=jdk.m3.graphics
HELPER_ROW=$'io.github.humbleui.skija.impl.SneakyThrow\tjdk.m3.graphics.impl.M3SneakyThrow'
LEAF0_LEDGERS="CLASSES JNI_BINDINGS CC_CLASSREFS JAVA_CLASSREFS OWNER_MAP SUMMARY TYPE_RENAME JNI_RENAME"
LEAF0_LEDGERS="$LEAF0_LEDGERS CC_STRING_RENAME"
MODULE_DIR=src/jdk.m3.graphics/share/classes
ABSORB=m3/tooling/skija-absorb
TOOLS="$repo/$ABSORB/tools"
MARKER=.absorb_leaf1_work
case "$(uname -s)" in
  MINGW*|MSYS*|CYGWIN*) PS=';'; JDK_HOME=$(cd "$J/.." && pwd -W);;
  *) PS=':'; JDK_HOME=$(cd "$J/.." && pwd);;
esac
IFS=';' read -r -a JARS <<< "$DONOR_JARS"
step() { printf '%s %s\n' "$(date -u +%H:%M:%S)" "$*"; }
die() { echo "FAIL $*" >&2; exit 1; }

if [ -e "$work" ]; then
  [ -f "$work/$MARKER" ] || die "refusing to clear $work: it is not a work directory of this script"
  rm -rf "$work"
fi
mkdir -p "$work"; touch "$work/$MARKER"
out="$work/out"; L0="$out/$ABSORB/ledgers"; L1="$out/$ABSORB/leaf1"
mkdir -p "$L0" "$out/$ABSORB/recipe" "$L1" "$out/$MODULE_DIR"

step "L0 skija_patterns.py + gen_rename.py"
"$PY" -I "$TOOLS/skija_patterns.py" "$SKIJA_ROOT" "$DLL_EXPORTS" "$work/l0" "$OWNER" "$OWNER_IMPL"
"$PY" -I "$TOOLS/gen_rename.py" "$work/l0" "$TYPES_CLASSES" "$OWNER"
for n in $LEAF0_LEDGERS; do cp "$work/l0/$n.tsv" "$L0/"; done
cp "$work/l0/M3SkijaToGraphics.yml" "$out/$ABSORB/recipe/"

step "S0 mr_select.py (MR_SELECT v1, release $RELEASE)"
jar_args=(); for j in "${JARS[@]}"; do jar_args+=(--jar "$j"); done
"$PY" -I "$TOOLS/mr_select.py" "$SKIJA_ROOT" "$DONOR_DL/types-0.2.0-sources.jar" "$work/src0" "$L1/MR_SELECT.tsv" \
  "${jar_args[@]}" --release "$RELEASE" --jar-view "$work/jarview"

step "S1 delombok + delombok_clean.py"
"$J/java" -jar "$DONOR_DL/lombok-1.18.48.jar" delombok "$work/src0" -d "$work/src1" \
  -c "$DONOR_DL/annotations-20.1.0.jar$PS$DONOR_DL/lombok-1.18.48.jar" > "$work/delombok.log" 2>&1
[ ! -s "$work/delombok.log" ] || { head -20 "$work/delombok.log"; die "delombok reported problems"; }
"$PY" -I "$TOOLS/delombok_clean.py" "$work/src1"

step "S2 TYPE_RENAME_L1 + absorb_rename.py"
{ cat "$L0/TYPE_RENAME.tsv"; printf '%s\n' "$HELPER_ROW"; } > "$L1/TYPE_RENAME_L1.tsv"
"$PY" -I "$TOOLS/absorb_rename.py" "$work/src1" "$L1/TYPE_RENAME_L1.tsv" "$work/src2" "$L1" "$L0/CC_STRING_RENAME.tsv"

step "S3 normalize_ws.py"
"$PY" -I "$TOOLS/normalize_ws.py" "$work/src2"
cp -r "$work/src2/jdk" "$out/$MODULE_DIR/"

step "G1 javac stage 1 and stage 2"
find "$work/src1" -name '*.java' | sort > "$work/src1.list"
"$J/javac" --release "$RELEASE" -encoding UTF-8 -proc:none -nowarn -cp "$DONOR_DL/annotations-20.1.0.jar" \
  -d "$work/c1" @"$work/src1.list" > "$work/c1.log" 2>&1 || { cat "$work/c1.log"; die "stage 1 does not compile"; }
find "$work/src2" -name '*.java' | sort > "$work/src2.list"
"$J/javac" --release "$RELEASE" -encoding UTF-8 -proc:none -nowarn -d "$work/c2" @"$work/src2.list" \
  > "$work/c2.log" 2>&1 || { cat "$work/c2.log"; die "stage 2 does not compile"; }
echo "stage1_classes=$(find "$work/c1" -name '*.class' | wc -l)" \
  "stage2_classes=$(find "$work/c2" -name '*.class' | wc -l)"

step "G2 API surface: stage 1 renamed == stage 2"
"$PY" -I "$TOOLS/rename_sed.py" "$L1/TYPE_RENAME_L1.tsv" "$work/rename.sed" > /dev/null
bash "$TOOLS/api_surface.sh" "$work/c1" "$work/api1.txt" "$work/rename.sed" > /dev/null
bash "$TOOLS/api_surface.sh" "$work/c2" "$work/api2.txt" > /dev/null
diff "$work/api1.txt" "$work/api2.txt" > "$work/api12.diff" \
  || { head -20 "$work/api12.diff"; die "API surface differs"; }
! grep -q -E 'humbleui|jetbrains' "$work/api2.txt" || die "an old name is left in the API surface"
echo "api_lines=$(wc -l < "$work/api2.txt") removed=0 added=0"

step "G3 published jars at release $RELEASE == stage 1"
bash "$TOOLS/api_surface.sh" "$work/jarview" "$work/api0.txt" > /dev/null
bash "$TOOLS/api_surface.sh" "$work/c1" "$work/api1_donor.txt" > /dev/null
diff "$work/api0.txt" "$work/api1_donor.txt" > "$work/api01.diff" || true
grep -E '^[<>]' "$work/api01.diff" | grep -v -E '\$[0-9]+$|\.SneakyThrow( |$)' > "$work/api01.unexpected" || true
[ ! -s "$work/api01.unexpected" ] || { cat "$work/api01.unexpected"; die "published surface differs"; }
echo "published_lines=$(wc -l < "$work/api0.txt") stage1_lines=$(wc -l < "$work/api1_donor.txt")" \
  "expected_diff_lines=$(grep -c -E '^[<>]' "$work/api01.diff" || true)"

step "G4 javac -Xlint:all of module $MODULE"
{ find "$out/$MODULE_DIR" -name '*.java'; echo "$repo/$MODULE_DIR/module-info.java"; } | sort > "$work/module.list"
"$J/javac" --release "$RELEASE" -encoding UTF-8 -Xlint:all -Xmaxwarns "$MAX_WARNINGS" -d "$work/cm" \
  @"$work/module.list" > "$work/cm.log" 2>&1 || { tail -40 "$work/cm.log"; die "module does not compile"; }
disabled=" $(sed -n -E 's/^DISABLED_WARNINGS_java \+= //p' "$repo/make/modules/$MODULE/Java.gmk") "
grep -o -E 'warning: \[[a-z-]+\]' "$work/cm.log" | sed -E 's/.*\[([a-z-]+)\]/\1/' | sort | uniq -c \
  > "$work/cm.kinds" || true
while read -r count kind; do
  case "$disabled" in
    *" $kind "*) echo "warning $kind=$count (disabled in Java.gmk)";;
    *) die "warning kind $kind is not disabled in Java.gmk";;
  esac
done < "$work/cm.kinds"
deps=$("$J/jdeps" --module-path "$work/cm" -s --module "$MODULE" | tr -d '\r')
[ "$deps" = "$MODULE -> java.base" ] || die "module dependencies: $deps"
echo "jdeps: $deps (JDK_SELF_CALL: the module implements no java.base API, so no absorbed call re-enters it)"

step "G5 runtime probe on this JDK"
"$J/javac" --release "$RELEASE" --module-path "$work/cm" --add-modules "$MODULE" -d "$work/probe" \
  "$TOOLS/M3GraphicsLeaf1Probe.java"
"$J/java" --module-path "$work/cm" --add-modules "$MODULE" -cp "$work/probe" M3GraphicsLeaf1Probe

if [ -d "$JDK_HOME/jmods" ]; then
  step "G6 runtime probe in a jlink image"
  "$J/jlink" --module-path "$JDK_HOME/jmods$PS$work/cm" --add-modules "$MODULE" --no-header-files --no-man-pages \
    --output "$work/image"
  mkdir -p "$work/nolib"
  "$work/image/bin/java" -Djava.library.path="$work/nolib" -cp "$work/probe" M3GraphicsLeaf1Probe jrt
  rm -rf "$work/image"
else
  step "G6 skipped: no jmods under $JDK_HOME"
fi

compare() {
  local diffs=0 f rel
  while IFS= read -r f; do
    rel="${f#"$out"/}"
    cmp -s "$f" "$repo/$rel" || { echo "DIFF $rel"; diffs=$((diffs + 1)); }
  done < <(find "$out" -type f | sort)
  while IFS= read -r f; do
    rel="${f#"$repo"/}"
    [ -f "$out/$rel" ] || { echo "STALE $rel"; diffs=$((diffs + 1)); }
  done < <(find "$repo/$MODULE_DIR/jdk" -name '*.java' | sort)
  echo "differences=$diffs"
  [ "$diffs" -eq 0 ]
}

if [ "$mode" = run ]; then
  step "sync outputs into $repo"
  find "$repo/$MODULE_DIR/jdk" -name '*.java' -delete
  cp -r "$out/." "$repo/"
fi
step "compare repository with the recipe output"
compare || die "the repository differs from the recipe output"
step "fixed point: the repository equals the recipe output"
