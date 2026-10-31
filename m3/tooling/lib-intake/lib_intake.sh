#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Pattern LIB_INTAKE_TO_JMOD v2: one new library (or app) into M3JDK21 as an optional JMOD, by existing owners only.
#   S0 VIEW       release-21 source view: src/main/java, then src/main/versions/<N> overlays for N <= 21 (the jmod_pack.py
#                 release view, at source level); resources; the upstream module-info (src/main/java or
#                 src/main/descriptor/<N>) is set aside, never relocated, and only its uses/provides are carried over
#   S1 RELOCATE   packages + imports: OpenRewrite ChangePackage per package, recursive=false, longest first (boundary-checked
#                 form of com.synexia M3BoundaryCheckedPackageRenameRecipe; prefix neighbours are never matched), offline
#                 rewrite-maven-plugin 5.23.1 on rewrite 8.17.1; then the same name map, dotted and slash forms, completes
#                 string literals, comments, resource paths and META-INF/services + proguard text (what ChangePackage does
#                 not own: com.synexia #14520 GAP_NO_OWNER kinds); package resources move with their packages, bytes unchanged
#   S1b BCP       every relocated Java file == name-map(original); every package resource byte-equal at its moved path
#   S2 RESIDUE    m3work:relocation-scan over sources and resources + slash-form grep: residue of every kind must be 0
#   S3 ATOMIZE    M3Mop atomize + plan
#   S4 CONSOLIDATE absorb_plan.py against the M3 owner atoms: OWNER_CONTRACT atoms = consolidation queue (proposal)
#   S5 REFACTOR   METHOD_15 / LOOP / CONST_HOIST ratchet baseline for MOP refactor passes
#   S6 MODULE     requires from jdeps --print-module-deps over a class-path compile; module-info exports every package and
#                 keeps the upstream uses/provides (name-mapped); javac --release 21 -Xlint:all as a module
#   S7 JMOD       jmod create (legal notices only), jmod describe, jlink java.base + jdk.compiler + requires + the module,
#                 --list-modules, --validate-modules; smoke = runs/<lib>/SMOKE.java (// expect:) or LoadAll (every class
#                 of the module loads in the image, 0 failures)
#   S8 LAYER      relocated sources, resources, descriptor and notices into the M3 refactor layer; upstream untouched
# INTAKE.tsv columns: lib source_dir mappings module version layer_dir mop_classes owner_atoms; mappings, module, version and
# layer_dir may be "auto": common package root R -> com.m3.lib.<last segment of R, lower case>, module jdk.m3.<same>,
# version from the source pom, layer m3/vendor/third_party-m3/relocated/<lib>.
# Usage (repository root, JDK 21 at JAVA_HOME, m3-work-maven-plugin 0.1.1 installed, MSYS path conversion on):
#   bash m3/tooling/lib-intake/lib_intake.sh <lib> <work dir> [<intake.tsv>]
set -uo pipefail
LIB="$1"; W="$2"; INTAKE="${3:-m3/tooling/lib-intake/INTAKE.tsv}"
REPO=$(pwd); BIN="$JAVA_HOME/bin"; SEP=":"; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";";; esac
row=$(awk -F'\t' -v l="$LIB" '$1 == l' "$INTAKE"); [ -n "$row" ] || { echo "LIB_INTAKE_FAIL no row for $LIB in $INTAKE"; exit 2; }
IFS=$'\t' read -r _ SRC MAPS MODULE VERSION LAYER MOP OWNERS CDEPS MAIN <<< "$row"; CDEPS="${CDEPS:--}"; MAIN="${MAIN:--}"
RUN="m3/tooling/lib-intake/runs/$LIB"; rm -rf "$RUN/relocation"; mkdir -p "$RUN"; rm -rf "$W"
mkdir -p "$W/orig/java" "$W/orig/resources" "$W/proj/src/main/java" "$W/proj/src/main/resources"
ST="$RUN/STAGES.tsv"; printf 'stage\tstatus\tevidence\n' > "$ST"
stage() { printf '%s\t%s\t%s\n' "$1" "$2" "$3" >> "$ST"; echo "LIB_INTAKE $LIB $1 $2 $3"; [ "$2" = PASS ] || { echo "LIB_INTAKE_FAIL $LIB $1"; exit 3; }; }
mvnw() { (unset MSYS_NO_PATHCONV; cd "$1" && shift && mvn -o -B -Dstyle.color=never "$@"); }
javafiles() { (cd "$1" && find . -name '*.java' ! -name module-info.java | sed 's#^\./##' | LC_ALL=C sort); }

# S0 VIEW -------------------------------------------------------------------------------------------------------------
cp -r "$SRC/src/main/java/." "$W/orig/java/"
over=""; if [ -d "$SRC/src/main/versions" ]; then
	for v in $(ls "$SRC/src/main/versions" | grep -E '^[0-9]+$' | sort -n); do [ "$v" -le 21 ] && cp -r "$SRC/src/main/versions/$v/." "$W/orig/java/" && over="$over$v,"; done
fi
[ -d "$SRC/src/main/resources" ] && cp -r "$SRC/src/main/resources/." "$W/orig/resources/"
UPMI=""; for c in "$W/orig/java/module-info.java" $(ls -d "$SRC"/src/main/descriptor/*/module-info.java 2>/dev/null | sort -t/ -k1 | tail -1); do
	[ -f "$c" ] && { UPMI="$W/upstream-module-info.java"; cp "$c" "$UPMI"; break; }; done
rm -f "$W/orig/java/module-info.java"
javafiles "$W/orig/java" | sed 's#/[^/]*$##; s#/#.#g' | grep -v '\.java$' | LC_ALL=C sort -u > "$W/packages.txt"
stage S0_VIEW PASS "java=$(javafiles "$W/orig/java" | wc -l) packages=$(wc -l < "$W/packages.txt") resources=$(find "$W/orig/resources" -type f | wc -l) overlays=${over:-none} upstream_descriptor=$([ -n "$UPMI" ] && echo yes || echo no)"

# auto values ---------------------------------------------------------------------------------------------------------
if [ "$MAPS" = auto ]; then
	root=$(awk 'NR == 1 { l = $0 } { while (l != "" && $0 != l && index($0, l ".") != 1) { if (l !~ /\./) l = ""; else sub(/\.[^.]*$/, "", l) } } END { print l }' "$W/packages.txt")
	[ "$(echo "$root" | awk -F. '{ print NF }')" -ge 2 ] || stage S0_AUTO FAIL "no common package root of 2+ segments ('$root'); give explicit mappings"
	seg=$(echo "${root##*.}" | tr 'A-Z' 'a-z'); MAPS="$root=com.m3.lib.$seg"; [ "$MODULE" = auto ] && MODULE="jdk.m3.$seg"
fi
[ "$VERSION" = auto ] && VERSION=$(grep -m1 -oE '<version>[^<]+' "$SRC/pom.xml" | sed 's/<version>//')
[ "$LAYER" = auto ] && LAYER="m3/vendor/third_party-m3/relocated/$LIB"
printf 'key\tvalue\nlib\t%s\nsource\t%s\nmappings\t%s\nmodule\t%s\nversion\t%s\nlayer\t%s\n' "$LIB" "$SRC" "$MAPS" "$MODULE" "$VERSION" "$LAYER" > "$RUN/RUN_CONTEXT.tsv"

# S1 RELOCATE ----------------------------------------------------------------------------------------------------------
cp -r "$W/orig/java/." "$W/proj/src/main/java/"
printf 'old\tnew\tstatus\n' > "$RUN/RELOCATION_MAP.tsv"
awk -v maps="$MAPS" 'BEGIN { n = split(maps, m, ";"); for (i = 1; i <= n; i++) { split(m[i], kv, "="); o[i] = kv[1]; t[i] = kv[2] } }
	{ p = $0; best = 0; for (i = 1; i <= n; i++) if ((p == o[i] || index(p, o[i] ".") == 1) && length(o[i]) > length(o[best])) best = i
	  if (best) printf "%s\t%s\tMOVE\n", p, t[best] substr(p, length(o[best]) + 1); else printf "%s\t-\tKEEP\n", p }' "$W/packages.txt" |
	awk -F'\t' '{ print length($1) "\t" $0 }' | sort -t$'\t' -k1,1nr | cut -f2- >> "$RUN/RELOCATION_MAP.tsv"
{ printf 'type: specs.openrewrite.org/v1beta/recipe\nname: com.synexia.m3.intake.Relocate\ndisplayName: LIB_INTAKE relocation of %s\n' "$LIB"
  printf 'description: Boundary-checked package relocation, one non-recursive ChangePackage per package, longest first.\nrecipeList:\n'
  awk -F'\t' 'NR > 1 && $3 == "MOVE" { printf "  - org.openrewrite.java.ChangePackage:\n      oldPackageName: %s\n      newPackageName: %s\n      recursive: false\n", $1, $2 }' "$RUN/RELOCATION_MAP.tsv"; } > "$W/proj/rewrite.yml"
cat > "$W/proj/pom.xml" <<EOF
<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>
  <groupId>m3.intake</groupId><artifactId>$LIB</artifactId><version>$VERSION</version>
  <properties><maven.compiler.release>21</maven.compiler.release><project.build.sourceEncoding>UTF-8</project.build.sourceEncoding></properties>
  <build><plugins><plugin><groupId>org.openrewrite.maven</groupId><artifactId>rewrite-maven-plugin</artifactId><version>5.23.1</version>
    <configuration><activeRecipes><recipe>com.synexia.m3.intake.Relocate</recipe></activeRecipes><failOnInvalidActiveRecipes>true</failOnInvalidActiveRecipes></configuration>
  </plugin></plugins></build></project>
EOF
mvnw "$W/proj" rewrite:runNoFork > "$RUN/s1-rewrite.log" 2>&1; rc=$?
# the name map: mapping roots (longest first), dotted and slash forms, old name followed by a non-identifier character
printf '%s\n' "${MAPS//;/$'\n'}" | awk -F= '{ print length($1) "\t" $1 "\t" $2 }' | sort -t$'\t' -k1,1nr |
	awk -F'\t' '{ o = $2; gsub(/\./, "\\.", o); s = $2; gsub(/\./, "/", s); ns = $3; gsub(/\./, "/", ns)
		printf "s/%s([^A-Za-z0-9_$]|$)/%s\\1/g;s#%s([^A-Za-z0-9_$]|$)#%s\\1#g;", o, $3, s, ns }' > "$W/namemap.sed"
residual=0; while IFS= read -r f; do
	sed -E -f "$W/namemap.sed" "$W/proj/src/main/java/$f" > "$W/tmp.java"
	cmp -s "$W/tmp.java" "$W/proj/src/main/java/$f" || { cp "$W/tmp.java" "$W/proj/src/main/java/$f"; residual=$((residual + 1)); }
done < <(javafiles "$W/proj/src/main/java")
# resources: package resources move with their package (bytes unchanged); META-INF services names/entries and proguard text mapped
moved=0; mapped=0; while IFS= read -r r; do
	if [[ "$r" == META-INF/* ]]; then
		t=$(echo "$r" | sed -E -f "$W/namemap.sed"); mkdir -p "$(dirname "$W/proj/src/main/resources/$t")"
		case "$r" in META-INF/services/*|*.pro) sed -E -f "$W/namemap.sed" "$W/orig/resources/$r" > "$W/proj/src/main/resources/$t"; mapped=$((mapped + 1)) ;;
			*) cp "$W/orig/resources/$r" "$W/proj/src/main/resources/$t" ;; esac
	else
		t=$(echo "$r" | sed -E -f "$W/namemap.sed"); [ "$t" != "$r" ] && moved=$((moved + 1))
		mkdir -p "$(dirname "$W/proj/src/main/resources/$t")"; cp "$W/orig/resources/$r" "$W/proj/src/main/resources/$t"
	fi
done < <(cd "$W/orig/resources" && find . -type f | sed 's#^\./##' | LC_ALL=C sort)
# files OpenRewrite could not parse stay under their old package: they move by the same name map (S1b proves the result
# equals name-map(original), exactly what ChangePackage would have produced) and are listed, never silent
printf 'file\treason\n' > "$RUN/TEXT_FALLBACK.tsv"; fallback=0
while IFS= read -r f; do
	pkg=$(dirname "$f" | tr '/' '.'); awk -F'\t' -v p="$pkg" 'NR > 1 && $1 == p && $3 == "MOVE" { f = 1 } END { exit !f }' "$RUN/RELOCATION_MAP.tsv" || continue
	t=$(echo "$f" | sed -E -f "$W/namemap.sed"); mkdir -p "$(dirname "$W/proj/src/main/java/$t")"
	sed -E -f "$W/namemap.sed" "$W/proj/src/main/java/$f" > "$W/proj/src/main/java/$t" && rm "$W/proj/src/main/java/$f"
	printf '%s\tOpenRewrite 8.17.1 parse failure (s1-rewrite.log); moved by the name map\n' "$f" >> "$RUN/TEXT_FALLBACK.tsv"; fallback=$((fallback + 1))
done < <(javafiles "$W/proj/src/main/java")
movedpk=$(awk -F'\t' 'NR > 1 && $3 == "MOVE"' "$RUN/RELOCATION_MAP.tsv" | wc -l)
left=$(javafiles "$W/proj/src/main/java" | sed 's#/[^/]*$##; s#/#.#g' | LC_ALL=C sort -u | grep -cxFf <(awk -F'\t' 'NR > 1 && $3 == "MOVE" { print $1 }' "$RUN/RELOCATION_MAP.tsv") || true)
[ "$rc" -eq 0 ] && [ "${left:-0}" -eq 0 ] && stage S1_RELOCATE PASS "map=$MAPS packages_moved=$movedpk old_dirs_left=0 text_completed_files=$residual parse_fallback_files=$fallback resources_moved=$moved meta_mapped=$mapped" ||
	stage S1_RELOCATE FAIL "rewrite rc=$rc old_dirs_left=${left:-0}"

# S1b BCP ---------------------------------------------------------------------------------------------------------------
same=0; differ=0; : > "$RUN/s1b-bcp.log"
while IFS= read -r f; do
	t=$(echo "$f" | sed -E -f "$W/namemap.sed")
	if sed -E -f "$W/namemap.sed" "$W/orig/java/$f" | cmp -s - "$W/proj/src/main/java/$t"; then same=$((same + 1)); else differ=$((differ + 1)); echo "BCP_DIFF $f" >> "$RUN/s1b-bcp.log"; fi
done < <(javafiles "$W/orig/java")
rsame=0; while IFS= read -r r; do
	[[ "$r" == META-INF/services/* || "$r" == *.pro ]] && continue
	t=$(echo "$r" | sed -E -f "$W/namemap.sed"); cmp -s "$W/orig/resources/$r" "$W/proj/src/main/resources/$t" && rsame=$((rsame + 1)) || { differ=$((differ + 1)); echo "BCP_RES_DIFF $r" >> "$RUN/s1b-bcp.log"; }
done < <(cd "$W/orig/resources" && find . -type f | sed 's#^\./##' | LC_ALL=C sort)
[ "$differ" -eq 0 ] && stage S1b_BCP PASS "java_equal_after_name_map=$same resources_byte_equal=$rsame differing=0" || stage S1b_BCP FAIL "differing=$differ (s1b-bcp.log)"

# S2 RESIDUE -------------------------------------------------------------------------------------------------------------
mvnw "$W/proj/src" com.synexia.m3:m3-work-maven-plugin:0.1.1:relocation-scan "-Dm3.relocation.mappings=${MAPS//;/,}" \
	"-Dm3.relocation.out=$(cygpath -m "$REPO/$RUN/relocation" 2>/dev/null || echo "$REPO/$RUN/relocation")" > "$RUN/s2-scan.log" 2>&1
hits=$(($(wc -l < "$RUN/relocation/RELOCATION_SCAN.tsv") - 1))
slash=$(printf '%s\n' "${MAPS//;/$'\n'}" | cut -d= -f1 | tr '.' '/' | while read -r s; do grep -rlE "$s([^A-Za-z0-9_\$]|\$)" "$W/proj/src/main" 2>/dev/null; done | wc -l)
[ "$hits" -eq 0 ] && [ "$slash" -eq 0 ] && stage S2_RESIDUE PASS "residue=0 (every kind) slash_form_files=0" || stage S2_RESIDUE FAIL "residue=$hits slash_form_files=$slash"

# S3 ATOMIZE / PATTERNIZE ---------------------------------------------------------------------------------------------------
(cd "$W" && "$BIN/java" -cp "$MOP" com.m3.mop.M3Mop atomize --root=proj/src/main/java --out=atoms > "$REPO/$RUN/s3-atomize.log" 2>&1 &&
	"$BIN/java" -cp "$MOP" com.m3.mop.M3Mop plan --atomize=atoms --out=plan >> "$REPO/$RUN/s3-atomize.log" 2>&1); rc=$?
tr -d '\r' < "$W/atoms/SUMMARY.txt" > "$RUN/ATOMS_SUMMARY.txt" 2>/dev/null
awk -F'\t' 'NR > 1 { c[$5 "\t" $9]++ } END { print "state\tnext_action\tpatterns"; for (k in c) print k "\t" c[k] }' "$W/plan/PATTERN_PLAN.tsv" > "$RUN/PATTERN_PLAN_SUMMARY.tsv" 2>/dev/null
[ "$rc" -eq 0 ] && stage S3_ATOMIZE PASS "$(grep -o 'atoms=[0-9]*.*registered=[0-9]*' "$RUN/ATOMS_SUMMARY.txt" | cut -d' ' -f1-4)" || stage S3_ATOMIZE FAIL "rc=$rc"

# S4 CONSOLIDATE --------------------------------------------------------------------------------------------------------------
head -1 "$W/atoms/ATOMS.tsv" > "$W/empty-ATOMS.tsv"; mkdir -p "$W/absorb"
python -I "$REPO/m3/collections/qualification/converge-commons-20261030/absorb_plan.py" "$W/atoms/ATOMS.tsv" "$W/empty-ATOMS.tsv" "$OWNERS" "$W/absorb" > "$RUN/s4-absorb.log" 2>&1; rc=$?
cp "$W/absorb/SUMMARY.tsv" "$RUN/ABSORB_SUMMARY.tsv" 2>/dev/null
awk -F'\t' 'NR == 1 || $6 == "OWNER_CONTRACT"' "$W/absorb/ABSORPTION_PLAN.tsv" | cut -f1-6,9 > "$RUN/CONSOLIDATION_QUEUE.tsv" 2>/dev/null
[ "$rc" -eq 0 ] && stage S4_CONSOLIDATE PASS "owner_contract_atoms=$(($(wc -l < "$RUN/CONSOLIDATION_QUEUE.tsv") - 1)) (proposal queue)" || stage S4_CONSOLIDATE FAIL "rc=$rc"

# S5 REFACTOR baseline -----------------------------------------------------------------------------------------------------------
base=$(grep -oE '(METHOD_15|LOOP|CONST_HOIST)=[0-9]+' "$RUN/ATOMS_SUMMARY.txt" | tr '\n' ' ')
printf 'metric\tbaseline\n' > "$RUN/REFACTOR_BASELINE.tsv"; for kv in $base; do printf '%s\t%s\n' "${kv%%=*}" "${kv#*=}" >> "$RUN/REFACTOR_BASELINE.tsv"; done
stage S5_REFACTOR_BASELINE PASS "$base(ratchet: MOP passes may only lower these)"

# S6 MODULE ----------------------------------------------------------------------------------------------------------------------
rm -rf "$W/cp-classes" "$W/classes"
srcroot=$(cygpath -m "$W/proj/src/main/java" 2>/dev/null || echo "$W/proj/src/main/java")
javafiles "$W/proj/src/main/java" | sed "s#^#$srcroot/#" > "$W/cp-sources.txt"
# compile-only dependencies (annotations): class path for the class-path compile, module path + requires static for the module
CP=""; STATIC=""; if [ "$CDEPS" != - ]; then
	for j in ${CDEPS//;/ }; do CP="$CP${CP:+$SEP}$j"
		STATIC="$STATIC $("$BIN/jar" --describe-module --file "$j" 2>/dev/null | tr -d '\r' | grep -m1 -oE '^[A-Za-z0-9_.]+@' | tr -d '@')"; done
fi
"$BIN/javac" --release 21 -nowarn -encoding UTF-8 ${CP:+-cp "$CP"} -d "$W/cp-classes" "@$(cygpath -m "$W/cp-sources.txt" 2>/dev/null || echo "$W/cp-sources.txt")" > "$RUN/s6-cp-javac.log" 2>&1 || stage S6_MODULE FAIL "class-path compile failed (s6-cp-javac.log)"
deps=$("$BIN/jdeps" --print-module-deps --ignore-missing-deps --multi-release 21 "$W/cp-classes" 2>"$RUN/s6-jdeps.log" | tr -d '\r')
{ printf '/* GENERATED by m3/tooling/lib-intake/lib_intake.sh (pattern LIB_INTAKE_TO_JMOD v2): requires from jdeps, every package exported,\n   upstream uses/provides kept and name-mapped. */\nmodule %s {\n' "$MODULE"
  echo "$deps" | tr ',' '\n' | grep -vxE 'java.base|' | sed 's/.*/    requires &;/'
  for s in $STATIC; do echo "    requires static $s;"; done
  javafiles "$W/proj/src/main/java" | sed 's#/[^/]*$##; s#/#.#g' | grep -v '\.java$' | LC_ALL=C sort -u | sed 's/.*/    exports &;/'
  [ -n "$UPMI" ] && sed -E 's#//.*##' "$UPMI" | tr '\n' ' ' | grep -oE '(uses|provides)[^;]+;' | sed -E -f "$W/namemap.sed" | sed 's/^/    /'
  printf '}\n'; } > "$W/proj/src/main/java/module-info.java"
{ cat "$W/cp-sources.txt"; echo "$srcroot/module-info.java"; } > "$W/mod-sources.txt"
"$BIN/javac" --release 21 -Xlint:all -encoding UTF-8 ${CP:+--module-path "$CP"} -d "$W/classes" "@$(cygpath -m "$W/mod-sources.txt" 2>/dev/null || echo "$W/mod-sources.txt")" > "$RUN/s6-javac.log" 2>&1; rc=$?
cp -r "$W/proj/src/main/resources/." "$W/classes/"
[ "$rc" -eq 0 ] && stage S6_MODULE PASS "module=$MODULE requires=${deps:-java.base} exports=$(grep -c '^    exports' "$W/proj/src/main/java/module-info.java") javac_warnings=$(grep -c 'warning:' "$RUN/s6-javac.log")" ||
	stage S6_MODULE FAIL "module compile rc=$rc (s6-javac.log)"

# S7 JMOD + IMAGE ------------------------------------------------------------------------------------------------------------------
mkdir -p "$W/jmods" "$W/legal"; rm -rf "$W/image"
for f in LICENSE LICENSE.txt UPSTREAM_LICENSE.txt THIRD_PARTY_NOTICES.md NOTICE NOTICE.txt; do [ -f "$SRC/$f" ] && cp "$SRC/$f" "$W/legal/"; done
rm -f "$W/jmods/$MODULE.jmod"; add="java.base,jdk.compiler,$MODULE"; [ -n "$deps" ] && add="$add,$deps"
# an app (main_class set): the name-mapped main class goes into the module descriptor and jlink makes image/bin/<lib>
APPMAIN=""; LAUNCH=(); if [ "$MAIN" != - ]; then APPMAIN=$(echo "$MAIN" | sed -E -f "$W/namemap.sed"); LAUNCH=(--launcher "$LIB=$MODULE"); fi
"$BIN/jmod" create --class-path "$W/classes" --module-version "$VERSION" ${APPMAIN:+--main-class "$APPMAIN"} --legal-notices "$W/legal" "$W/jmods/$MODULE.jmod" > "$RUN/s7-jmod.log" 2>&1 &&
"$BIN/jmod" describe "$W/jmods/$MODULE.jmod" > "$RUN/s7-jmod-describe.log" 2>&1 &&
"$BIN/jlink" --module-path "$JAVA_HOME/jmods$SEP$W/jmods" --add-modules "$add" "${LAUNCH[@]}" --strip-debug --no-header-files --no-man-pages --output "$W/image" > "$RUN/s7-jlink.log" 2>&1 &&
"$W/image/bin/java" --list-modules > "$RUN/s7-image-modules.log" 2>&1 && "$W/image/bin/java" --validate-modules > "$RUN/s7-validate-modules.log" 2>&1; rc=$?
if [ -f "$RUN/SMOKE.java" ]; then
	smoke=$("$W/image/bin/java" --add-modules "$MODULE" "$RUN/SMOKE.java" 2>&1 | tr -d '\r' | tail -1); want=$(sed -n 's#^// expect: ##p' "$RUN/SMOKE.java")
else
	"$BIN/jmod" list "$W/jmods/$MODULE.jmod" | tr -d '\r' | sed -n 's#^classes/\(.*\)\.class$#\1#p' | grep -v '^module-info$' | tr '/' '.' > "$W/classlist.txt"
	cp "$REPO/m3/tooling/lib-intake/LoadAll.java" "$W/LoadAll.java"
	smoke=$("$W/image/bin/java" --add-modules "$MODULE" "$W/LoadAll.java" "$W/classlist.txt" 2>&1 | tr -d '\r' | tail -1); want="loaded=$(wc -l < "$W/classlist.txt") failed=0"
fi
echo "$smoke" > "$RUN/s7-smoke.log"
appok=1; app=""; if [ -n "$APPMAIN" ]; then
	grep -q "main-class $APPMAIN" "$RUN/s7-jmod-describe.log" && { [ -f "$W/image/bin/$LIB" ] || [ -f "$W/image/bin/$LIB.bat" ]; } || appok=0
	app=" app_main=$APPMAIN launcher=bin/$LIB"
fi
if [ "$rc" -eq 0 ] && [ "$appok" -eq 1 ] && grep -q "^$MODULE@" "$RUN/s7-image-modules.log" && [ "$smoke" = "$want" ]; then
	stage S7_JMOD_IMAGE PASS "jmod=$(stat -c %s "$W/jmods/$MODULE.jmod")B image_modules=$(wc -l < "$RUN/s7-image-modules.log") smoke='$smoke'$app"
else stage S7_JMOD_IMAGE FAIL "rc=$rc smoke='$smoke' want='$want'"; fi

# S8 LAYER ---------------------------------------------------------------------------------------------------------------------------
rm -rf "$LAYER"; mkdir -p "$LAYER/src/main"; cp -r "$W/proj/src/main/java" "$LAYER/src/main/"
[ -n "$(ls -A "$W/proj/src/main/resources")" ] && cp -r "$W/proj/src/main/resources" "$LAYER/src/main/"
cp "$W/legal/"* "$LAYER/" 2>/dev/null; [ -f "$SRC/PROVENANCE.json" ] && cp "$SRC/PROVENANCE.json" "$LAYER/"
cp "$W/proj/rewrite.yml" "$RUN/rewrite.yml"; cp "$W/namemap.sed" "$RUN/namemap.sed"
printf 'java\t%s\nrewrite\trewrite-maven-plugin 5.23.1 / rewrite 8.17.1\n' "$("$BIN/java" -version 2>&1 | head -1 | tr -d '\r"')" >> "$RUN/RUN_CONTEXT.tsv"
stage S8_LAYER PASS "files=$(find "$LAYER" -type f | wc -l) layer=$LAYER"
echo "LIB_INTAKE_DONE $LIB $MODULE $W/jmods/$MODULE.jmod"
