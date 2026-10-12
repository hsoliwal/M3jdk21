#!/usr/bin/env bash
# DEPENDENCY-SOURCE-INLINE: materialize a repository branch from the runner receipts, prove it, and wire it.
#   gen   : module + image generation (resource completion, packaging rules, delombok notices)
#   build : standalone Maven build of every module (forced; dependency order)
#   gate  : API-surface + resource equivalence of the Maven-built jars against the published jars
#   gen2  : regenerate with the gate verdicts (non-admissible PASS donors become images)
#   switch: consumer pins -> source-built coordinates (only admitted modules)
#   insert: root reactor <module> lines (byte-preserving)
#   crate / verify / prbody
# usage: finalize.sh <synexia|m3> <step...>   (all = gen build gate gen2 switch insert crate verify prbody)
set -u
SIDE="$1"; shift
C=/k/m3-work/runs/20261007-m3jdk21-apex/c6
K=K:/m3-work/runs/20261007-m3jdk21-apex/c6
INV="$C/DEPENDENCY_INVENTORY.tsv $C/DEPENDENCY_INVENTORY_T2.tsv $C/DEPENDENCY_INVENTORY_T3.tsv $C/DEPENDENCY_INVENTORY_T4.tsv $C/DEPENDENCY_INVENTORY_T5.tsv $C/DEPENDENCY_INVENTORY_T6.tsv $C/DEPENDENCY_INVENTORY_T7.tsv $C/DEPENDENCY_INVENTORY_T8.tsv $C/DEPENDENCY_INVENTORY_T9.tsv"
RUNS="$C/run/out-1 $C/run/out-1b $C/run/out-2 $C/run/out-1c $C/run/out-1d $C/run/out-1e $C/run/out-3 $C/run/out-4b $C/run/out-4d $C/run/out-5"
STATUS=""; for r in $RUNS; do [ -f "$r/status.tsv" ] && STATUS="$STATUS $r/status.tsv"; done
RECEIPTS=""; for r in $RUNS; do [ -d "$r" ] && RECEIPTS="$RECEIPTS $r"; done
export JAVA_TOOL_OPTIONS="-Xmx1536m -Djava.io.tmpdir=K:/m3-work/tmp" TMP=K:/m3-work/tmp TEMP=K:/m3-work/tmp JAVA_HOME="C:\\Program Files\\Java\\jdk-21"
ASM=$(cygpath -w ~/.m2/repository/org/ow2/asm/asm/9.6/asm-9.6.jar)
if [ "$SIDE" = "synexia" ]; then
  W=/k/m3-work/wt/synexia-c6; WK=K:/m3-work/wt/synexia-c6; BASE=origin/develop
  P=""; GENOPT=""; SWOPT=""; ONLY=""; BOPT=""
else
  W=/k/m3-work/wt/m3jdk21-c7; WK=K:/m3-work/wt/m3jdk21-c7; BASE=origin/master
  P="M3_"; GENOPT="--module-root m3/vendor/third_party --image-root m3/vendor/third_party-images --suffix=-m3-source-1 --owner M3jdk21"
  SWOPT="--targets $K/m3_consumer_targets.tsv --suffix=-m3-source-1 --default-props rewrite.version=8.17.1,openrewrite.version=8.17.1"
  ONLY="--only $(cat $C/m3_only.txt)"
  BOPT="--suffix=-m3-source-1"
fi
gen() {  # $1 = extra args
  if [ -f "$C/${P}THIRD_PARTY_MODULES.tsv" ]; then
    cut -f3 "$C/${P}THIRD_PARTY_MODULES.tsv" | tail -n +2 | grep -v "^third_party/re2j$" | while read -r d; do [ -n "$d" ] && rm -rf "$W/$d"; done
  fi
  python -I "$C/run_traced.py" "$C/gen_third_party.py" --inventory $INV --plan "$C/LOCK_PLAN.tsv" --status $STATUS --receipts $RECEIPTS --build-inputs "$C/BUILD_INPUTS.tsv" \
    --store K:/m3-work/donors/maven-sources/store --repo "$WK" --out "$K/${P}THIRD_PARTY_MODULES.tsv" --template K:/m3-work/wt/synexia-c6/third_party/re2j --pom-classpath $K/POM_CLASSPATH.tsv $GENOPT $ONLY $1 2> "$C/${P}gen.log" | tail -1
  rc=${PIPESTATUS[0]}
  if [ "$rc" != 0 ]; then echo "GEN_FAILED rc=$rc (tree is partial; stopping the lane)"; tail -3 "$C/${P}gen.log"; exit 3; fi
  awk -F'\t' 'NR>1 {c[$2" "$7]++} END {for (k in c) printf "%s=%d; ", k, c[k]; print ""}' "$C/${P}THIRD_PARTY_MODULES.tsv"
}
steps="$*"; [ "$steps" = "all" ] && steps="gen build gate gen2 switch insert crate verify prbody"
for step in $steps; do
  echo "##### $SIDE $step $(date -u +%FT%TZ)"
  case $step in
    gen) gen "" ;;
    build)
      python -I "$C/build_modules.py" --repo "$WK" --modules "$K/${P}THIRD_PARTY_MODULES.tsv" --out "$K/${P}BUILD_GATE.tsv" --logs "$K/${P}build-logs" --force $BOPT --repo-local K:/m3-work/m2-build > "$C/${P}ADMITTED_MODULES.txt" 2> "$C/${P}build_gate.log"
      tail -1 "$C/${P}build_gate.log"; awk -F'\t' 'NR>1 && $2!="PASS" {print "  FAIL "$1": "$5}' "$C/${P}BUILD_GATE.tsv" | cut -c1-200 ;;
    gate)
      rm -rf "$C/${P}api-diffs"
      python -I "$C/api_gate.py" --inventory $INV --modules "$K/${P}THIRD_PARTY_MODULES.tsv" --repo "$WK" --store K:/m3-work/donors/maven-sources/store --asm "$ASM" --built-m2 K:/m3-work/m2-build --tool "$K/ApiSurface.java" --diffs "$K/${P}api-diffs" --out "$K/${P}API_SURFACE.tsv" 2>&1 | tail -1
      python -I "$C/gate_retry.py" --api "$K/${P}API_SURFACE.tsv" --modules "$K/${P}THIRD_PARTY_MODULES.tsv" -- --inventory $INV --repo "$WK" --store K:/m3-work/donors/maven-sources/store --asm "$ASM" --built-m2 K:/m3-work/m2-build --tool "$K/ApiSurface.java" --diffs "$K/${P}api-diffs" 2>&1 | grep 'gate-retry'
      awk -F'\t' 'NR>1 && $9!="IDENTICAL" && $9!="EQUIVALENT_MODULO_COMPILER" && $9!="IDENTICAL_EXCEPT_SHADED" {print "  "$1"\t"$9"\tmissing="$5"\treal="$7"/"$8"\tres_missing="$16"\tres_differ="$17}' "$C/${P}API_SURFACE.tsv" ;;
    gen2) gen "--api-surface $K/${P}API_SURFACE.tsv" ;;
    switch)
      if [ "$SIDE" = "synexia" ]; then git -C "$W" show "$BASE:pom.xml" > "$W/pom.xml"; fi
      python -I "$C/switch_consumers.py" --repo "$WK" --base "$BASE" --modules "$K/${P}THIRD_PARTY_MODULES.tsv" --out "$K/${P}CONSUMER_SWITCH.tsv" $SWOPT --apply 2>&1 | tail -1
      awk -F'\t' 'NR>1 {c[$6]++} END {for (k in c) printf "%s=%d; ", k, c[k]; print ""}' "$C/${P}CONSUMER_SWITCH.tsv" ;;
    insert)
      if [ "$SIDE" = "synexia" ]; then
        awk -F'\t' 'NR>1 && $2=="PASS" {print $1}' "$C/${P}BUILD_GATE.tsv" > "$C/${P}ADMITTED_MODULES.txt"
        python -I "$C/insert_modules.py" --repo "$WK" --modules "$K/${P}THIRD_PARTY_MODULES.tsv" --admit "$K/${P}ADMITTED_MODULES.txt" --apply
      else
        echo "m3: aggregator pom written by m3_crate.py"
      fi ;;
    crate)
      if [ "$SIDE" = "synexia" ]; then
        python -I "$C/make_crate.py" --repo "$WK" --c6 "$K" --baseline "$(git -C "$W" rev-parse $BASE)" --runs $RECEIPTS --atoms "$K/atoms" | tail -1
      else
        python -I "$C/m3_crate.py" --repo "$WK" --c6 "$K" --synexia-crate K:/m3-work/wt/synexia-c6/.m3/task-crates/dependency-source-inline-20261010 --synexia-pr "hsoliwal/com.synexia#10339" --baseline "$(git -C "$W" rev-parse $BASE)" --consumers "$K/m3_consumer_poms.txt" | tail -1
      fi ;;
    verify)
      (cd "$W" && python -I .m3/task-crates/dependency-source-inline-20261010/verify_crate.py | tail -1) ;;
    prbody)
      if [ "$SIDE" = "synexia" ]; then python -I "$C/pr_body.py" --crate "$WK/.m3/task-crates/dependency-source-inline-20261010" --build-gate "$K/BUILD_GATE.tsv" --out "$K/PR_BODY.md"; fi ;;
    *) echo "unknown step $step"; exit 2 ;;
  esac
done
echo "##### done $(date -u +%FT%TZ)"
