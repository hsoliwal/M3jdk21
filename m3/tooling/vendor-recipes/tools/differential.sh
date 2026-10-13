#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Differential lanes: recipe (M3VendoredPinApply) vs oracle copy (oracle/switch_consumers_outdir.py --out-dir).
# One JVM at a time; each lane: oracle -> recipe -> compare. Results: runs/DIFFERENTIAL.tsv + runs/<lane>/.
set -u
H="$(cd "$(dirname "$0")/.." && pwd)"  # this module; build it first (mvn package + dependency:build-classpath -> cp.txt)
C="${APEX_RUN:?set APEX_RUN to the apex run directory holding the modules tables}"
JAVA="/c/Program Files/Java/jdk-21/bin/java"
CP="$(cygpath -m "$H")/target/classes;$(cat "$H/cp.txt")"
cd "$H" || exit 2
: > runs/DIFFERENTIAL.tsv

lane() { # name repo base modules suffix targets(-|path) defaults
  local name=$1 repo=$2 base=$3 mods=$4 sfx=$5 tg=$6 dp=$7 sha
  sha=$(sha256sum "$mods" | cut -c1-64)
  rm -rf "runs/$name" && mkdir -p "runs/$name"
  local oargs=(--repo "$repo" --base "$base" --modules "$mods" "--suffix=$sfx" --out "runs/$name/ORACLE_ROWS.tsv" --out-dir "runs/$name/oracle-poms")
  local rargs=(--repo "$repo" --base "$base" --modules "$mods" --modules-sha256 "$sha" --suffix "$sfx" --out-dir "runs/$name/recipe-poms"
               --rows "runs/$name/RECIPE_ROWS.tsv" --receipt "runs/$name/RECIPE_RECEIPT.tsv")
  [ "$tg" != "-" ] && oargs+=(--targets "$tg") && rargs+=(--targets "$tg")
  [ -n "$dp" ] && oargs+=(--default-props "$dp") && rargs+=(--default-props "$dp")
  python -I oracle/switch_consumers_outdir.py "${oargs[@]}" > "runs/$name/oracle.log" 2>&1; echo "oracle_exit=$?" >> "runs/$name/oracle.log"
  "$JAVA" -cp "$CP" com.m3.rewrite.vendor.M3VendoredPinApply "${rargs[@]}" > "runs/$name/recipe.log" 2>&1; echo "recipe_exit=$?" >> "runs/$name/recipe.log"
  python -I tools/compare.py "$name" "runs/$name/oracle-poms" "runs/$name/ORACLE_ROWS.tsv" "runs/$name/recipe-poms" "runs/$name/RECIPE_ROWS.tsv" > "runs/$name/COMPARE.txt" 2>&1
  echo "compare_exit=$?" >> "runs/$name/COMPARE.txt"
  head -1 "runs/$name/COMPARE.txt" >> runs/DIFFERENTIAL.tsv
  echo "LANE_DONE $name $(tail -1 runs/$name/oracle.log) $(tail -1 runs/$name/recipe.log) $(tail -1 runs/$name/COMPARE.txt)"
}

M3PRE=cc785460f870a081f0907a2823daeca5dab0c838
SYNPRE=5bb1089c1cb04b27274316e163e410abfe61f81b
lane m3 "${M3_REPO:?set M3_REPO}" origin/master "$C/M3_THIRD_PARTY_MODULES.tsv" -m3-source-1 "$C/m3_consumer_targets.tsv" rewrite.version=8.17.1,openrewrite.version=8.17.1
lane synexia "${SYN_REPO:?set SYN_REPO}" origin/develop "$C/THIRD_PARTY_MODULES.tsv" -synexia-source-1 - ""
lane m3-preswitch "${M3_REPO:?set M3_REPO}" $M3PRE "$C/M3_THIRD_PARTY_MODULES.tsv" -m3-source-1 "$C/m3_consumer_targets.tsv" rewrite.version=8.17.1,openrewrite.version=8.17.1
lane synexia-preswitch "${SYN_REPO:?set SYN_REPO}" $SYNPRE "$C/THIRD_PARTY_MODULES.tsv" -synexia-source-1 - ""
echo ALL_LANES_DONE
