#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# self_contained_gate.sh [plan|compile|test]        (default: test)
#
# M3jdk21 self-contained gate: compile the M3 control reactor (m3/pom.xml, default modules and every profile's
# modules) and run its tests with ONLY paths inside this repository plus the JDK. No Maven, no ~/.m2, no network,
# no sibling checkout. Every pom reference that the repository itself does not provide is listed.
#
#   plan     pom model + classification only: REFS.tsv, EXTERNAL_REFERENCES.tsv, NEEDED_PATHS.txt
#   compile  plan + javac of every unit (reactor modules and the in-repo providers of their dependencies)
#   test     compile + JUnit Platform run of every reactor module's tests (launcher built from vendored sources)
#
# Provider rule: a dependency is satisfied only by a pom on disk with the exact groupId:artifactId:version
# (REACTOR or REPO). REPO_DRIFT = the repository holds that groupId:artifactId only under another version (Maven
# would go outside); the gate still compiles against the nearest in-repo version so the drift is measurable, but
# the verdict counts it as external. Evidence copies (qualification, verification, snapshots, META-INF/maven,
# retained variants, ...) never provide.
#
# Env: REPO (default: git toplevel), OUT (default: <repo>/../m3-selfcontained-out; must be outside the repo
#      sources), JDK (default: JAVA_HOME), ROOT_POM (default m3/pom.xml), UNITS_ONLY (space separated reactor
#      pom paths to restrict compile/test), TEST_TIMEOUT (seconds per module, default 1800), KEEP_CLASSES (1 keeps
#      class output; default 0 deletes it at the end), HEARTBEAT (seconds, default 300).
# Output (OUT): REACTOR.tsv REFS.tsv EXTERNAL_REFERENCES.tsv EDGES.tsv UNITS.tsv SOURCES.tsv NEEDED_PATHS.txt
#      COMPILE.tsv COMPILE_ERRORS.tsv TESTS.tsv TEST_FAILURES.tsv CLASSPATH_AUDIT.tsv SUMMARY.txt gate.log
# Exit: 0 = self-contained build: yes; 1 = no (see SUMMARY.txt); 64 = usage; 2 = environment problem.
# CPU only: find, awk, sed, grep, javac, java.
# The body is one brace group: bash parses it whole before running, so editing this file mid-run is safe.
{
set -u
MODE="${1:-test}"
case "$MODE" in plan|compile|test) ;; *) echo "usage: self_contained_gate.sh [plan|compile|test]" >&2; exit 64;; esac
TOOLS="$(cd "$(dirname "$0")" && pwd)"
REPO="${REPO:-$(git -C "$TOOLS" rev-parse --show-toplevel 2>/dev/null)}"
[ -n "$REPO" ] && [ -f "$REPO/${ROOT_POM:-m3/pom.xml}" ] || { echo "no repository root with ${ROOT_POM:-m3/pom.xml}" >&2; exit 2; }
# absolute paths in the form the JDK tools accept (Windows: H:/x, not the MSYS /h/x)
apath() { (cd "$1" && { pwd -W 2>/dev/null || pwd; }); }
REPO="$(apath "$REPO")"
OUT="${OUT:-$REPO/../m3-selfcontained-out}"; mkdir -p "$OUT"; OUT="$(apath "$OUT")"
case "$OUT/" in "$REPO"/*) echo "OUT must be outside the repository" >&2; exit 2;; esac
JDK="${JDK:-${JAVA_HOME:-}}"; ROOT_POM="${ROOT_POM:-m3/pom.xml}"
TEST_TIMEOUT="${TEST_TIMEOUT:-1800}"; KEEP_CLASSES="${KEEP_CLASSES:-0}"; HEARTBEAT="${HEARTBEAT:-300}"
export GIT_NO_LAZY_FETCH=1
LOG="$OUT/gate.log"; : > "$LOG"
log() { printf '%s %s\n' "$(date +%H:%M:%S)" "$*" | tee -a "$LOG"; }
cnt() { local n; n=$(grep -c "$@" 2>/dev/null); printf '%s' "${n:-0}"; }
inside() { case "$1" in "$REPO"/*|"$OUT"/*) return 0;; *) return 1;; esac; }

# heartbeat for long javac / test runs
rm -f "$OUT/.stop" "$OUT/.phase"
( s=0; while [ ! -f "$OUT/.stop" ]; do sleep 5; s=$((s + 5))
    [ "$s" -ge "$HEARTBEAT" ] && { s=0; printf '%s heartbeat: %s\n' "$(date +%H:%M:%S)" "$(tail -n 1 "$OUT/.phase" 2>/dev/null)" | tee -a "$LOG"; }
  done ) &
trap 'touch "$OUT/.stop"; wait' EXIT
phase() { echo "$*" >> "$OUT/.phase"; log "$*"; }

cd "$REPO" || exit 2
phase "gate $MODE repo $REPO head $(git rev-parse --short HEAD 2>/dev/null || echo '?') out $OUT"

# 1. pom model of every pom on disk under the root pom's directory, then effective model + classification
ROOTDIR="$(dirname "$ROOT_POM")"
find "$ROOTDIR" -name pom.xml -type f -not -path '*/target/*' -print0 | sort -z \
  | xargs -0 awk -f "$TOOLS/pom_model.awk" > "$OUT/MODEL.tsv"
rm -f "$OUT"/{REACTOR,REFS,EDGES,UNITS,GAV,SOURCES}.tsv
awk -f "$TOOLS/pom_resolve.awk" -v OUT="$OUT" -v ROOT="$ROOT_POM" -v EXTRA="${EXTRA_UNITS:-}" "$OUT/MODEL.tsv"
touch "$OUT/REFS.tsv" "$OUT/EDGES.tsv" "$OUT/SOURCES.tsv"
phase "model: $(cut -f1 "$OUT/MODEL.tsv" | sort -u | wc -l) poms, reactor $(awk -F'\t' '$3 != "pom" { print $1 }' "$OUT/REACTOR.tsv" | sort -u | wc -l) jar modules, $(wc -l < "$OUT/UNITS.tsv") compile units"

# 2. external references (the work queue): every reference the repository does not provide
awk -F'\t' -v OFS='\t' '
  $9 == "REACTOR" || $9 == "REPO" || $9 == "REPO_PATH" || $9 == "JDK" { next }
  {
    cat = ($4 == "plugin") ? "BUILD_PLUGIN" : ($4 == "implicit-plugin") ? "IMPLICIT_PLUGIN" : ($4 == "plugin-dependency") ? "PLUGIN_DEPENDENCY" \
        : ($4 ~ /^path:|systemPath/) ? "PATH" : ($4 ~ /repository/i) ? "REPOSITORY" : ($4 == "parent") ? "PARENT" \
        : ($4 == "launcher") ? "TEST_RUNTIME" : ($2 != "") ? "PROFILE_DEPENDENCY" : ($5 ~ /test/) ? "TEST_DEPENDENCY" : "DEPENDENCY"
    k = cat OFS $9 OFS $6 ":" $7 ":" $8
    if (!((k SUBSEP $1) in seen)) { seen[k, $1] = 1; n[k]++; mods[k] = mods[k] (mods[k] == "" ? "" : ",") $1 }
    if (!(k in note)) note[k] = $11
  }
  END { print "category", "class", "reference", "modules", "module_poms", "note"
        for (k in n) print k, n[k], mods[k], note[k] }' "$OUT/REFS.tsv" \
  | { IFS= read -r h; printf '%s\n' "$h"; sort -t$'\t' -k1,1 -k4,4nr -k3,3; } > "$OUT/EXTERNAL_REFERENCES.tsv"
EXT_TOTAL=$(($(wc -l < "$OUT/EXTERNAL_REFERENCES.tsv") - 1))
EXT_BUILD=$(awk -F'\t' 'NR > 1 && $1 ~ /^(DEPENDENCY|TEST_DEPENDENCY|TEST_RUNTIME|PARENT|PATH)$/' "$OUT/EXTERNAL_REFERENCES.tsv" | wc -l)
phase "external references: $EXT_TOTAL distinct ($EXT_BUILD on the default compile/test path)"
awk -F'\t' 'NR > 1 { c[$1]++ } END { for (k in c) printf "  %-20s %d\n", k, c[k] }' "$OUT/EXTERNAL_REFERENCES.tsv" | sort | tee -a "$LOG"

# 3. paths the compile needs (for sparse clones: every unit directory)
cut -f1 "$OUT/UNITS.tsv" | sed 's#/pom.xml$##' > "$OUT/NEEDED_PATHS.txt"
awk -F'\t' '{ print $3 }' "$OUT/SOURCES.tsv" | grep -v '^\.\.' >> "$OUT/NEEDED_PATHS.txt"
sort -u -o "$OUT/NEEDED_PATHS.txt" "$OUT/NEEDED_PATHS.txt"

summary() {   # verdict, reasons
  {
    echo "self-contained build: $1"
    echo "mode: $MODE   repo head: $(git rev-parse HEAD 2>/dev/null)"
    echo "external references (distinct): $EXT_TOTAL   on the default compile/test path: $EXT_BUILD"
    awk -F'\t' 'NR > 1 { c[$1 " " $2]++ } END { for (k in c) printf "  %-36s %d\n", k, c[k] }' "$OUT/EXTERNAL_REFERENCES.tsv" | sort
    [ -s "$OUT/COMPILE.tsv" ] && awk -F'\t' 'NR > 1 { c[$7]++ } END { printf "compile units:"; for (k in c) printf " %s=%d", k, c[k]; print "" }' "$OUT/COMPILE.tsv"
    [ -s "$OUT/TESTS.tsv" ] && awk -F'\t' 'NR > 1 { c[$9]++; t += $3; p += $4; f += $5 } END { printf "test modules:"; for (k in c) printf " %s=%d", k, c[k]; printf "   tests found=%d succeeded=%d failed=%d\n", t, p, f }' "$OUT/TESTS.tsv"
    [ -s "$OUT/CLASSPATH_AUDIT.tsv" ] && echo "classpath entries outside repo/OUT: $(awk -F'\t' '$3 == "OUTSIDE"' "$OUT/CLASSPATH_AUDIT.tsv" | wc -l)"
    echo "reasons: ${2:-none}"
  } > "$OUT/SUMMARY.txt"
  tee -a "$LOG" < "$OUT/SUMMARY.txt"
}
if [ "$MODE" = plan ]; then
  V=no; [ "$EXT_BUILD" = 0 ] && V="yes (plan only: not compiled)"
  summary "$V" "$EXT_BUILD external references on the default compile/test path"; [ "$EXT_BUILD" = 0 ]; exit $?
fi

# 4. compile
[ -x "$JDK/bin/javac" ] || [ -x "$JDK/bin/javac.exe" ] || { log "no javac under JDK=$JDK"; exit 2; }
JAVAC="$JDK/bin/javac"; JAVA="$JDK/bin/java"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) PS=';';; *) PS=':';; esac
CL="$OUT/classes"; TC="$OUT/test-classes"; mkdir -p "$CL" "$TC" "$OUT/home" "$OUT/tmp" "$OUT/logs"
printf 'unit\trole\tphase\tfiles\trc\terrors\tstatus\tclasspath_entries\tnote\n' > "$OUT/COMPILE.tsv"
printf 'unit\tphase\tfile\tline\tmessage\n' > "$OUT/COMPILE_ERRORS.tsv"
printf 'unit\tphase\tentry\tlocation\n' > "$OUT/CLASSPATH_AUDIT.tsv"
declare -A STATE
unit_dir() { printf '%s' "${1%/pom.xml}"; }
key() { printf '%s' "${1%/pom.xml}" | tr '/' '_'; }
roots() {  # unit pom, main|test -> source roots on stdout
  local u="$1" ph="$2" d; d=$(unit_dir "$u")
  awk -F'\t' -v u="$u" -v ph="$ph" -v d="$d" '
    $1 != u { next }
    ph == "main" && $2 == "sourceDirectory" { sd = $3 } ph == "main" && $2 == "add-source" { add[++n] = $3 }
    ph == "test" && $2 == "testSourceDirectory" { sd = $3 } ph == "test" && $2 == "add-test-source" { add[++n] = $3 }
    END { print (sd != "" ? sd : d "/src/" ph "/java"); for (i = 1; i <= n; i++) print add[i] }' "$OUT/SOURCES.tsv"
}
cp_of() {  # unit pom, phase(main|test|launcher) -> classpath entries (classes dirs) on stdout
  awk -F'\t' -v u="$1" -v ph="$2" '$1 == u && $2 == ph { print $3 }' "$OUT/EDGES.tsv" \
    | while IFS= read -r p; do printf '%s\n' "$CL/$(key "$p")"; done
}
javac_unit() {  # unit pom, role, phase, outdir, classpath-list-file
  local u="$1" role="$2" ph="$3" od="$4" cpf="$5" d enc rel lst rc e note="" args=() r
  d=$(unit_dir "$u"); lst="$OUT/logs/$(key "$u").$ph.src"
  : > "$lst"
  while IFS= read -r r; do
    case "$r" in /*|[A-Za-z]:*) note="$note outside-root:$r"; continue;; esac
    [ -d "$r" ] && find "$r" -name '*.java' -type f -not -name module-info.java >> "$lst"
  done < <(roots "$u" "$ph")
  local n; n=$(wc -l < "$lst" | tr -d ' ')
  local ncp; ncp=$(grep -c . "$cpf" 2>/dev/null); ncp=${ncp:-0}
  while IFS= read -r r; do
    if inside "$r"; then printf '%s\t%s\t%s\tINSIDE\n' "$u" "$ph" "$r" >> "$OUT/CLASSPATH_AUDIT.tsv"
    else printf '%s\t%s\t%s\tOUTSIDE\n' "$u" "$ph" "$r" >> "$OUT/CLASSPATH_AUDIT.tsv"; fi
  done < "$cpf"
  if [ "$n" = 0 ]; then
    if [ ! -d "$d" ]; then printf '%s\t%s\t%s\t0\t-\t-\tNOT_MATERIALIZED\t%s\t%s\n' "$u" "$role" "$ph" "$ncp" "unit directory not on disk$note" >> "$OUT/COMPILE.tsv"; return 3; fi
    printf '%s\t%s\t%s\t0\t0\t0\tNO_SOURCES\t%s\t%s\n' "$u" "$role" "$ph" "$ncp" "${note# }" >> "$OUT/COMPILE.tsv"; return 0
  fi
  enc=$(awk -F'\t' -v u="$u" '$1 == u { print $6 }' "$OUT/UNITS.tsv"); enc="${enc:-UTF-8}"
  args=(-J-Xmx1500m -encoding "$enc" -proc:none -nowarn -Xlint:none -Xmaxerrs 500 -implicit:none -d "$od")
  local ex; ex=$(grep -o -- '--add-exports[= ][^<[:space:]]*' "$u" 2>/dev/null | sed 's/^--add-exports[= ]//' | sort -u)
  # release: the pom's maven.compiler.release/source (1.x -> x, floor 8); none declared -> 21, then 8 (the
  # maven-compiler-plugin default target) when the donor source only compiles against the Java 8 API
  rel=$(awk -F'\t' -v u="$u" '$1 == u { print $7 }' "$OUT/UNITS.tsv"); rel="${rel#1.}"; case "$rel" in ''|*[!0-9]*) rel="";; esac
  [ -n "$rel" ] && [ "$rel" -lt 8 ] && rel=8
  local rels="${rel:-21 8}" rr
  [ "$ncp" -gt 0 ] && args+=(-cp "$(paste -sd "$PS" "$cpf")")
  mkdir -p "$od"
  for rr in $rels; do
    if [ -n "$ex" ]; then
      local xa=(-source "$rr" -target "$rr"); while IFS= read -r r; do xa+=(--add-exports "$r"); done <<< "$ex"
    else local xa=(--release "$rr"); fi
    "$JAVAC" "${args[@]}" "${xa[@]}" "@$lst" > "$OUT/logs/$(key "$u").$ph.javac.log" 2>&1; rc=$?
    [ "$rc" = 0 ] && break
  done
  note="$note release=$rr${ex:+ add-exports}"; [ -z "$rel" ] && [ "$rr" != 21 ] && note="$note (fallback: no release declared)"
  e=$(cnt -E '^.*\.java:[0-9]+: error: ' "$OUT/logs/$(key "$u").$ph.javac.log")
  sed -n -E 's#^(.*\.java):([0-9]+): error: (.*)$#\1\t\2\t\3#p' "$OUT/logs/$(key "$u").$ph.javac.log" \
    | awk -v u="$u" -v ph="$ph" -v repo="$REPO/" -F'\t' -v OFS='\t' '{ gsub(/\\/, "/", $1); if (index($1, repo) == 1) $1 = substr($1, length(repo) + 1); print u, ph, $1, $2, $3 }' >> "$OUT/COMPILE_ERRORS.tsv"
  local st=PASS; [ "$rc" = 0 ] || st=FAIL
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$u" "$role" "$ph" "$n" "$rc" "$e" "$st" "$ncp" "${note# }" >> "$OUT/COMPILE.tsv"
  # resources (main: src/main/resources, test: src/test/resources) onto the class output
  [ -d "$d/src/$ph/resources" ] && cp -r "$d/src/$ph/resources/." "$od/" 2>/dev/null
  return "$rc"
}
compile_main() {  # memoized main compile of one unit (providers first: UNITS.tsv is a postorder)
  local u="$1" role="$2" cpf
  [ -n "${STATE[$u]:-}" ] && return 0
  cpf="$OUT/logs/$(key "$u").main.cp"; cp_of "$u" main > "$cpf"
  phase "javac main $u ($(wc -l < "$cpf" | tr -d ' ') classpath entries)"
  javac_unit "$u" "$role" main "$CL/$(key "$u")" "$cpf"; STATE[$u]=$?
}
declare -A NEED
if [ -n "${UNITS_ONLY:-}" ]; then   # selected reactor modules + everything their main/test/launcher closures need
  for u in $UNITS_ONLY; do NEED[$u]=1; done
  while IFS= read -r p; do NEED[$p]=1; done < <(awk -F'\t' -v sel=" $UNITS_ONLY " '
    { e[$1] = e[$1] " " $3 } index(sel, " " $1 " ") { q[++n] = $1 }
    END { for (i = 1; i <= n; i++) { split(e[q[i]], a, " "); for (j in a) if (!(a[j] in s)) { s[a[j]] = 1; print a[j]; q[++n] = a[j] } } }' "$OUT/EDGES.tsv")
fi
while IFS=$'\t' read -r u role pk ord prof enc rel; do
  [ "$pk" = pom ] && continue
  [ -n "${UNITS_ONLY:-}" ] && [ -z "${NEED[$u]:-}" ] && continue
  compile_main "$u" "$role"
done < "$OUT/UNITS.tsv"
grep -q $'\tFAIL\t\|\tNOT_MATERIALIZED\t' "$OUT/COMPILE.tsv" && log "compile failures: $(awk -F'\t' '$7 == "FAIL" || $7 == "NOT_MATERIALIZED" { print $1 " " $3 " " $7 " errors=" $6 }' "$OUT/COMPILE.tsv" | tr '\n' ';')"

# 5. tests: compile src/test/java of each reactor module and run it on the JUnit Platform launcher from the repo
printf 'module\tprofile\tfound\tsucceeded\tfailed\taborted\tskipped\trc\tstatus\tnote\n' > "$OUT/TESTS.tsv"
printf 'module\ttest\tmessage\n' > "$OUT/TEST_FAILURES.tsv"
RUNNER="$OUT/runner"
FENCE=(-Duser.home="$OUT/home" -Djava.io.tmpdir="$OUT/tmp" -Dmaven.repo.local="$OUT/home/no-m2"
       -DsocksProxyHost=127.0.0.1 -DsocksProxyPort=9 -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=9
       -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=9 -Dhttp.nonProxyHosts= -Djava.net.useSystemProxies=false)
if [ "$MODE" = test ]; then
  while IFS=$'\t' read -r u role pk ord prof enc rel; do
    [ "$role" = reactor ] && [ "$pk" != pom ] || continue
    if [ -n "${UNITS_ONLY:-}" ]; then case " $UNITS_ONLY " in *" $u "*) ;; *) continue;; esac; fi
    k=$(key "$u"); tcpf="$OUT/logs/$k.test.cp"
    { printf '%s\n' "$CL/$k"; cp_of "$u" test; } | awk '!s[$0]++' > "$tcpf"
    roots "$u" test | while IFS= read -r r; do [ -d "$r" ] && echo y; done | grep -q y \
      || { printf '%s\t%s\t0\t0\t0\t0\t0\t-\tNO_TESTS\t\n' "$u" "$prof" >> "$OUT/TESTS.tsv"; continue; }
    [ "${STATE[$u]:-1}" = 0 ] || { printf '%s\t%s\t0\t0\t0\t0\t0\t-\tNOT_RUN\tmain compile failed\n' "$u" "$prof" >> "$OUT/TESTS.tsv"; continue; }
    phase "javac test $u"
    javac_unit "$u" reactor test "$TC/$k" "$tcpf" || { printf '%s\t%s\t0\t0\t0\t0\t0\t-\tNOT_RUN\ttest compile failed\n' "$u" "$prof" >> "$OUT/TESTS.tsv"; continue; }
    L=$(awk -F'\t' -v u="$u" '$1 == u && $2 == "launcher" { print $3 }' "$OUT/EDGES.tsv")
    [ -n "$L" ] || { printf '%s\t%s\t0\t0\t0\t0\t0\t-\tNOT_RUN\tno JUnit Platform engine in the test closure (no in-repo launcher)\n' "$u" "$prof" >> "$OUT/TESTS.tsv"; continue; }
    [ "${STATE[$L]:-1}" = 0 ] || compile_main "$L" provider
    { cat "$tcpf"; printf '%s\n' "$CL/$(key "$L")"; cp_of "$L" main; } | awk '!s[$0]++' > "$OUT/logs/$k.run.cp"
    rdir="$RUNNER/$(key "$L")"
    if [ ! -f "$rdir/SelfContainedTestMain.class" ]; then
      mkdir -p "$rdir"
      "$JAVAC" --release 21 -nowarn -d "$rdir" -cp "$(paste -sd "$PS" "$OUT/logs/$k.run.cp")" "$TOOLS/SelfContainedTestMain.java" > "$OUT/logs/runner.javac.log" 2>&1 \
        || { printf '%s\t%s\t0\t0\t0\t0\t0\t-\tNOT_RUN\trunner does not compile on the launcher\n' "$u" "$prof" >> "$OUT/TESTS.tsv"; continue; }
    fi
    # surefire argLine of the module: keep JVM module/system-property options, drop ${...} placeholders
    argl=$(awk 'BEGIN { RS = "<" } /^argLine>/ { sub(/^argLine>/, ""); gsub(/[\r\n\t ]+/, " "); print; exit }' "$u" | awk '{
      for (i = 1; i <= NF; i++) { t = $i
        if (t ~ /^--(add-opens|add-exports|add-modules|enable-native-access)$/ && i < NF) { printf "%s=%s ", t, $(i + 1); i++; continue }
        if (t ~ /^(--add-opens=|--add-exports=|--add-modules=|--enable-native-access=|--enable-preview$|-D|-Xss|-Xmx)/ && t !~ /\$/) printf "%s ", t } }')
    phase "test run $u"
    # tests run with the module directory as working directory, like surefire (basedir)
    ( cd "$(unit_dir "$u")" && timeout "$TEST_TIMEOUT" "$JAVA" "${FENCE[@]}" -Dbasedir="$REPO/$(unit_dir "$u")" $argl \
        -cp "$rdir$PS$TC/$k$PS$(paste -sd "$PS" "$OUT/logs/$k.run.cp")" SelfContainedTestMain "$TC/$k" "$OUT/logs/$k.tests.tsv" \
        > "$OUT/logs/$k.test.log" 2>&1 ); rc=$?
    while IFS= read -r r; do inside "$r" && loc=INSIDE || loc=OUTSIDE; printf '%s\ttest-run\t%s\t%s\n' "$u" "$r" "$loc" >> "$OUT/CLASSPATH_AUDIT.tsv"; done < "$OUT/logs/$k.run.cp"
    if [ -s "$OUT/logs/$k.tests.tsv" ]; then
      read -r _ fnd _ _ _ ok _ fl _ ab _ sk _ cf < "$OUT/logs/$k.tests.tsv"
      st=PASS; [ "$rc" = 0 ] && [ "$fl" = 0 ] && [ "${cf:-0}" = 0 ] || st=FAIL; [ "$fnd" = 0 ] && st=NO_TESTS_FOUND
      printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$u" "$prof" "$fnd" "$ok" "$fl" "$ab" "$sk" "$rc" "$st" "containers_failed=${cf:-0}" >> "$OUT/TESTS.tsv"
      grep '^FAIL' "$OUT/logs/$k.tests.tsv" | cut -f2- | sed "s#^#$u\t#" | cut -c1-600 >> "$OUT/TEST_FAILURES.tsv"
    else
      printf '%s\t%s\t0\t0\t0\t0\t0\t%s\tFAIL\tno result file (timeout=124 or JVM failure; see logs/%s.test.log)\n' "$u" "$prof" "$rc" "$k" >> "$OUT/TESTS.tsv"
    fi
  done < "$OUT/UNITS.tsv"
fi

# 6. verdict
R=""
[ "$EXT_BUILD" = 0 ] || R="$R $EXT_BUILD external references on the default compile/test path;"
c=$(awk -F'\t' 'NR > 1 && ($7 == "FAIL" || $7 == "NOT_MATERIALIZED")' "$OUT/COMPILE.tsv" | wc -l); [ "$c" = 0 ] || R="$R $c compile units failed or missing;"
o=$(awk -F'\t' '$4 == "OUTSIDE"' "$OUT/CLASSPATH_AUDIT.tsv" | wc -l); [ "$o" = 0 ] || R="$R $o classpath entries outside the repo;"
if [ "$MODE" = test ]; then
  t=$(awk -F'\t' 'NR > 1 && ($9 == "FAIL" || $9 == "NOT_RUN")' "$OUT/TESTS.tsv" | wc -l); [ "$t" = 0 ] || R="$R $t test modules failed or not run;"
fi
[ "$KEEP_CLASSES" = 1 ] || rm -rf "${CL:?}" "${TC:?}" "${RUNNER:?}" "${OUT:?}/tmp"
if [ -z "$R" ]; then summary yes ""; exit 0; else summary no "${R# }"; exit 1; fi
}
