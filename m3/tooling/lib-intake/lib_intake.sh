#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Pattern LIB_INTAKE_TO_JMOD v1: one new library (or app) into M3JDK21 as an optional JMOD, by existing owners only.
#   S1 RELOCATE   packages + imports: OpenRewrite ChangePackage per package, recursive=false, longest first (the
#                 boundary-checked form of com.synexia M3BoundaryCheckedPackageRenameRecipe; prefix neighbours such as
#                 javaewah / javaewah32 are never matched), run offline by rewrite-maven-plugin 5.23.1 on rewrite 8.17.1
#   S2 RESIDUE    m3work:relocation-scan (com.synexia #14520): JAVA_SYNTAX residue must be 0; other kinds -> manifests
#   S3 ATOMIZE    M3Mop atomize + plan: atoms and pattern ids of the relocated library
#   S4 CONSOLIDATE absorb_plan.py against the M3 owner atoms: OWNER_CONTRACT atoms = consolidation queue (proposal)
#   S5 REFACTOR   METHOD_15 / LOOP / CONST_HOIST counts as the ratchet baseline for MOP refactor passes
#   S6 MODULE     module-info.java (module <name>, exports every relocated package), javac --release 21 -Xlint:all
#   S7 JMOD       jmod create, jmod describe, jlink java.base + the module, --list-modules, --validate-modules, smoke run
#   S8 LAYER      relocated sources + descriptor + notices into the M3 refactor layer; ledgers into the run directory
# Usage (repository root, JDK 21 at JAVA_HOME, m3-work-maven-plugin installed):
#   bash m3/tooling/lib-intake/lib_intake.sh <lib> <work dir> [<intake.tsv>]
# Exit 0 = every stage PASS. One row per stage in STAGES.tsv; nothing outside <work dir>, the layer and the run dir is written.
set -uo pipefail
LIB="$1"; W="$2"; INTAKE="${3:-m3/tooling/lib-intake/INTAKE.tsv}"
REPO=$(pwd); BIN="$JAVA_HOME/bin"; SEP=":"; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";";; esac
row=$(awk -F'\t' -v l="$LIB" '$1 == l' "$INTAKE"); [ -n "$row" ] || { echo "LIB_INTAKE_FAIL no row for $LIB in $INTAKE"; exit 2; }
IFS=$'\t' read -r _ SRC MAPS MODULE VERSION LAYER MOP OWNERS <<< "$row"
RUN="m3/tooling/lib-intake/runs/$LIB"; mkdir -p "$RUN"; rm -rf "$W"; mkdir -p "$W/proj/src/main/java"
ST="$RUN/STAGES.tsv"; printf 'stage\tstatus\tevidence\n' > "$ST"
stage() { printf '%s\t%s\t%s\n' "$1" "$2" "$3" >> "$ST"; echo "LIB_INTAKE $1 $2 $3"; [ "$2" = PASS ] || { echo "LIB_INTAKE_FAIL $1"; exit 3; }; }
mvnw() { (unset MSYS_NO_PATHCONV; cd "$1" && shift && mvn -o -B -Dstyle.color=never "$@"); }

# S1 RELOCATE ------------------------------------------------------------------------------------------------------
cp -r "$SRC/src/main/java/." "$W/proj/src/main/java/"
(cd "$W/proj/src/main/java" && find . -name '*.java' -printf '%h\n' | sed 's#^\./##; s#/#.#g' | sort -u) > "$W/packages.txt"
printf 'type: specs.openrewrite.org/v1beta/recipe\nname: com.synexia.m3.intake.Relocate\ndisplayName: LIB_INTAKE relocation of %s\ndescription: Boundary-checked package relocation, one non-recursive ChangePackage per package, longest first.\nrecipeList:\n' "$LIB" > "$W/proj/rewrite.yml"
printf 'old\tnew\tstatus\n' > "$RUN/RELOCATION_MAP.tsv"
awk -v maps="$MAPS" 'BEGIN { n = split(maps, m, ";"); for (i = 1; i <= n; i++) { split(m[i], kv, "="); o[i] = kv[1]; t[i] = kv[2] } }
	{ p = $0; best = 0; for (i = 1; i <= n; i++) if ((p == o[i] || index(p, o[i] ".") == 1) && length(o[i]) > length(o[best])) best = i
	  if (best) printf "%s\t%s\tMOVE\n", p, t[best] substr(p, length(o[best]) + 1); else printf "%s\t-\tKEEP\n", p }' "$W/packages.txt" |
	awk -F'\t' '{ print length($1) "\t" $0 }' | sort -t$'\t' -k1,1nr | cut -f2- >> "$RUN/RELOCATION_MAP.tsv"
awk -F'\t' 'NR > 1 && $3 == "MOVE" { printf "  - org.openrewrite.java.ChangePackage:\n      oldPackageName: %s\n      newPackageName: %s\n      recursive: false\n", $1, $2 }' "$RUN/RELOCATION_MAP.tsv" >> "$W/proj/rewrite.yml"
cat > "$W/proj/pom.xml" <<EOF
<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>
  <groupId>m3.intake</groupId><artifactId>$LIB</artifactId><version>$VERSION</version>
  <properties><maven.compiler.release>21</maven.compiler.release><project.build.sourceEncoding>UTF-8</project.build.sourceEncoding></properties>
  <build><plugins><plugin><groupId>org.openrewrite.maven</groupId><artifactId>rewrite-maven-plugin</artifactId><version>5.23.1</version>
    <configuration><activeRecipes><recipe>com.synexia.m3.intake.Relocate</recipe></activeRecipes><failOnInvalidActiveRecipes>true</failOnInvalidActiveRecipes></configuration>
  </plugin></plugins></build></project>
EOF
mvnw "$W/proj" rewrite:runNoFork > "$RUN/s1-rewrite.log" 2>&1; rc=$?
moved=$(awk -F'\t' 'NR > 1 && $3 == "MOVE"' "$RUN/RELOCATION_MAP.tsv" | wc -l)
left=$(cd "$W/proj/src/main/java" && find . -name '*.java' -printf '%h\n' | sed 's#^\./##; s#/#.#g' | sort -u | grep -cxFf <(awk -F'\t' 'NR > 1 && $3 == "MOVE" { print $1 }' "$RUN/RELOCATION_MAP.tsv") || true)
[ "$rc" -eq 0 ] && [ "${left:-0}" -eq 0 ] && stage S1_RELOCATE PASS "packages_moved=$moved old_package_dirs_left=${left:-0} recipe=rewrite.yml" || stage S1_RELOCATE FAIL "rc=$rc left=${left:-0}"

# S1b BCP: every relocated file must be byte-equal to its original after the textual name map (longest old name first,
# old name followed by a non-identifier character), so the recipe changed names and nothing else
sedmap=$(awk -F'\t' 'NR > 1 && $3 == "MOVE" { print length($1) "\t" $1 "\t" $2 }' "$RUN/RELOCATION_MAP.tsv" | sort -t$'\t' -k1,1nr |
	awk -F'\t' '{ o = $2; gsub(/\./, "\\.", o); printf "s/%s([^A-Za-z0-9_$]|$)/%s\\1/g;", o, $3 }')
same=0; differ=0
while IFS= read -r f; do
	pkg=$(dirname "$f" | sed 's#/#.#g'); np=$(awk -F'\t' -v p="$pkg" '$1 == p && $3 == "MOVE" { print $2 }' "$RUN/RELOCATION_MAP.tsv")
	t="${np:-$pkg}"; t="${t//.//}/$(basename "$f")"
	if sed -E "$sedmap" "$SRC/src/main/java/$f" | cmp -s - "$W/proj/src/main/java/$t"; then same=$((same + 1)); else differ=$((differ + 1)); echo "BCP_DIFF $f" >> "$RUN/s1b-bcp.log"; fi
done < <(cd "$SRC/src/main/java" && find . -name '*.java' | sed 's#^\./##')
[ "$differ" -eq 0 ] && stage S1b_BCP PASS "byte_equal_after_name_map=$same differing=0" || stage S1b_BCP FAIL "differing=$differ (s1b-bcp.log)"

# S2 RESIDUE --------------------------------------------------------------------------------------------------------
mvnw "$W/proj/src" com.synexia.m3:m3-work-maven-plugin:0.1.0:relocation-scan "-Dm3.relocation.mappings=${MAPS//;/,}" \
	"-Dm3.relocation.out=$(cygpath -m "$REPO/$RUN/relocation" 2>/dev/null || echo "$REPO/$RUN/relocation")" > "$RUN/s2-scan.log" 2>&1
syntax=$(awk -F'\t' 'NR > 1 && $4 == "JAVA_SYNTAX"' "$RUN/relocation/RELOCATION_SCAN.tsv" | wc -l)
other=$(awk -F'\t' 'NR > 1 && $4 != "JAVA_SYNTAX"' "$RUN/relocation/RELOCATION_SCAN.tsv" | wc -l)
[ "$syntax" -eq 0 ] && stage S2_RESIDUE PASS "java_syntax_residue=0 other_residue=$other (manifests per owner in relocation/)" || stage S2_RESIDUE FAIL "java_syntax_residue=$syntax"

# S3 ATOMIZE / PATTERNIZE ---------------------------------------------------------------------------------------------
(cd "$W" && "$BIN/java" -cp "$MOP" com.m3.mop.M3Mop atomize --root=proj/src/main/java --out=atoms > "$REPO/$RUN/s3-atomize.log" 2>&1 &&
	"$BIN/java" -cp "$MOP" com.m3.mop.M3Mop plan --atomize=atoms --out=plan >> "$REPO/$RUN/s3-atomize.log" 2>&1); rc=$?
cp "$W/atoms/SUMMARY.txt" "$RUN/ATOMS_SUMMARY.txt" 2>/dev/null
awk -F'\t' 'NR > 1 { c[$5 "\t" $9]++ } END { print "state\tnext_action\tpatterns"; for (k in c) print k "\t" c[k] }' "$W/plan/PATTERN_PLAN.tsv" > "$RUN/PATTERN_PLAN_SUMMARY.tsv" 2>/dev/null
[ "$rc" -eq 0 ] && stage S3_ATOMIZE PASS "$(tr -d '\r' < "$RUN/ATOMS_SUMMARY.txt" | grep -o 'atoms=[0-9]*.*registered=[0-9]*' | cut -d' ' -f1-4)" || stage S3_ATOMIZE FAIL "rc=$rc"

# S4 CONSOLIDATE ----------------------------------------------------------------------------------------------------------
head -1 "$W/atoms/ATOMS.tsv" > "$W/empty-ATOMS.tsv"; mkdir -p "$W/absorb"
python -I "$REPO/m3/collections/qualification/converge-commons-20261030/absorb_plan.py" "$W/atoms/ATOMS.tsv" "$W/empty-ATOMS.tsv" "$OWNERS" "$W/absorb" > "$RUN/s4-absorb.log" 2>&1; rc=$?
cp "$W/absorb/SUMMARY.tsv" "$RUN/ABSORB_SUMMARY.tsv" 2>/dev/null
awk -F'\t' 'NR == 1 || $6 == "OWNER_CONTRACT"' "$W/absorb/ABSORPTION_PLAN.tsv" | cut -f1-6,9 > "$RUN/CONSOLIDATION_QUEUE.tsv" 2>/dev/null
[ "$rc" -eq 0 ] && stage S4_CONSOLIDATE PASS "owner_contract_atoms=$(($(wc -l < "$RUN/CONSOLIDATION_QUEUE.tsv") - 1)) (proposal queue; GAP/OWNER_SHAPE in ABSORB_SUMMARY.tsv)" || stage S4_CONSOLIDATE FAIL "rc=$rc"

# S5 REFACTOR baseline ------------------------------------------------------------------------------------------------------
base=$(tr -d '\r' < "$RUN/ATOMS_SUMMARY.txt" | grep -oE '(METHOD_15|LOOP|CONST_HOIST)=[0-9]+' | tr '\n' ' ')
printf 'metric\tbaseline\n' > "$RUN/REFACTOR_BASELINE.tsv"; for kv in $base; do printf '%s\t%s\n' "${kv%%=*}" "${kv#*=}" >> "$RUN/REFACTOR_BASELINE.tsv"; done
stage S5_REFACTOR_BASELINE PASS "$base(ratchet: MOP passes may only lower these)"

# S6 MODULE --------------------------------------------------------------------------------------------------------------
{ printf '/* GENERATED by m3/tooling/lib-intake/lib_intake.sh (pattern LIB_INTAKE_TO_JMOD v1): every relocated package is exported. */\nmodule %s {\n' "$MODULE"
  awk -F'\t' 'NR > 1 && $3 == "MOVE" { print $2 }' "$RUN/RELOCATION_MAP.tsv" | sort | sed 's/.*/    exports &;/'; printf '}\n'; } > "$W/proj/src/main/java/module-info.java"
rm -rf "$W/classes"; "$BIN/javac" --release 21 -Xlint:all -encoding UTF-8 -d "$W/classes" $(cd "$W/proj/src/main/java" && find "$(pwd)" -name '*.java') > "$RUN/s6-javac.log" 2>&1; rc=$?
warn=$(grep -c 'warning:' "$RUN/s6-javac.log" || true)
[ "$rc" -eq 0 ] && stage S6_MODULE PASS "module=$MODULE exports=$(grep -c exports "$W/proj/src/main/java/module-info.java") javac_warnings=${warn:-0}" || stage S6_MODULE FAIL "javac rc=$rc"

# S7 JMOD + IMAGE ------------------------------------------------------------------------------------------------------------
mkdir -p "$W/jmods" "$W/legal"; rm -rf "$W/image" "$W/jmods/$MODULE.jmod"
for f in LICENSE UPSTREAM_LICENSE.txt THIRD_PARTY_NOTICES.md NOTICE NOTICE.txt; do [ -f "$SRC/$f" ] && cp "$SRC/$f" "$W/legal/"; done
"$BIN/jmod" create --class-path "$W/classes" --module-version "$VERSION" --legal-notices "$W/legal" "$W/jmods/$MODULE.jmod" > "$RUN/s7-jmod.log" 2>&1 &&
"$BIN/jmod" describe "$W/jmods/$MODULE.jmod" > "$RUN/s7-jmod-describe.log" 2>&1 &&
"$BIN/jlink" --module-path "$JAVA_HOME/jmods$SEP$W/jmods" --add-modules "java.base,jdk.compiler,$MODULE" --strip-debug --no-header-files --no-man-pages \
	--output "$W/image" > "$RUN/s7-jlink.log" 2>&1 &&
"$W/image/bin/java" --list-modules > "$RUN/s7-image-modules.log" 2>&1 &&
"$W/image/bin/java" --validate-modules > "$RUN/s7-validate-modules.log" 2>&1; rc=$?
smoke=""; if [ -f "$RUN/SMOKE.java" ]; then smoke=$("$W/image/bin/java" --add-modules "$MODULE" "$RUN/SMOKE.java" 2>&1 | tr -d '\r' | tail -1); echo "$smoke" > "$RUN/s7-smoke.log"; fi
want=$(sed -n 's#^// expect: ##p' "$RUN/SMOKE.java" 2>/dev/null)
if [ "$rc" -eq 0 ] && grep -q "^$MODULE@$VERSION" "$RUN/s7-image-modules.log" && { [ -z "$want" ] || [ "$smoke" = "$want" ]; }; then
	stage S7_JMOD_IMAGE PASS "jmod=$(stat -c %s "$W/jmods/$MODULE.jmod")B image_modules=$(wc -l < "$RUN/s7-image-modules.log") smoke=${smoke:-none}"
else stage S7_JMOD_IMAGE FAIL "rc=$rc smoke='$smoke' want='$want'"; fi

# S8 LAYER -------------------------------------------------------------------------------------------------------------------
rm -rf "$LAYER"; mkdir -p "$LAYER/src/main/java"; cp -r "$W/proj/src/main/java/." "$LAYER/src/main/java/"
for f in LICENSE UPSTREAM_LICENSE.txt THIRD_PARTY_NOTICES.md PROVENANCE.json; do [ -f "$SRC/$f" ] && cp "$SRC/$f" "$LAYER/"; done
cp "$W/proj/rewrite.yml" "$RUN/rewrite.yml"
stage S8_LAYER PASS "files=$(find "$LAYER" -type f | wc -l) layer=$LAYER"
printf 'key\tvalue\nlib\t%s\nsource\t%s\nmodule\t%s\nversion\t%s\njava\t%s\nrewrite\trewrite-maven-plugin 5.23.1 / rewrite 8.17.1\n' "$LIB" "$SRC" "$MODULE" "$VERSION" "$("$BIN/java" -version 2>&1 | head -1 | tr -d '\r"')" > "$RUN/RUN_CONTEXT.tsv"
echo "LIB_INTAKE_DONE $LIB"
