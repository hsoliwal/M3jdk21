#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# repoint_chain.sh <donor-old> <donor-new> [consumer-pom ...]
#
# One version-duplicate repoint chain under m3/vendor/third_party (v3, 2026-10-31; v1/v2 lived in the com.synexia
# run docs/M3-SCALE/runs/20261030-m3jdk21-sanitize-atomize and are superseded by this in-repo copy):
#   0. coordinates: groupId:artifactId:version of donor-old and donor-new from their poms (same groupId:artifactId);
#      consumers = every pom on disk that declares a dependency on groupId:artifactId:OLDV (evidence copies excluded),
#      or the consumer poms given on the command line;
#   1. baseline proof: exact pom pins (pom_model.awk DEP records) + every text mention of the donor-old directory;
#   2. gate BEFORE: self_contained_gate.sh compile of each consumer with its full in-repo dependency closure
#      (EXTRA_UNITS), donor-old on the classpath;
#   3. repoint: the version inside each consumer's <dependency> block of that artifact (literal OLDV only; a
#      property-driven pin is HELD for a hand decision) and the PROVENANCE.json dependency pin;
#   4. gate AFTER: the same compile with donor-new; a consumer passes when it compiles with 0 errors (PLAIN), or, when
#      it already failed before because of references outside the repository, when the repoint adds no error
#      (DIFFERENTIAL: after-errors subset of before-errors);
#   5. git rm -r --sparse donor-old + its aggregator <module> line;
#   6. proof after: remaining pom pins and directory mentions outside the ledgers block; ledgers (qualification,
#      verification, evidence, task crates, m3/sanitize records, JMOD_IMAGE OLDER_VERSION rows) are reported only;
#   7. records in OUT: REPOINT_RESULT_<old>.tsv, REPOINT_GATE_<old>.tsv, PROOF_<old>_{before,after,blocking}.txt.
# Nothing is committed. On HELD every path the script touched is restored and the exit status is 2.
# Env: WT (worktree, default .), OUT (scratch dir, outside the worktree), JDK (JDK 21 home),
#      DISK_MOUNT (df path of the clone volume, default /h), DISK_FLOOR_KB (default 1000000).
# CPU only: git, awk, sed, grep, find, javac (through self_contained_gate.sh). No Maven, no network beyond the
# partial-clone blob fetch of the paths the chain needs.
# The body is one brace group: bash parses it whole before running, so editing this file mid-run is safe.
{
set -u
[ $# -ge 2 ] || { echo "usage: repoint_chain.sh <donor-old> <donor-new> [consumer-pom ...]" >&2; exit 64; }
OLD="$1"; NEW="$2"; shift 2
TOOLS="$(cd "$(dirname "$0")" && pwd)"
WT="${WT:-.}"; OUT="${OUT:?OUT must name a scratch directory outside the worktree}"; JDK="${JDK:-${JAVA_HOME:-}}"
DISK_MOUNT="${DISK_MOUNT:-/h}"; DISK_FLOOR_KB="${DISK_FLOOR_KB:-1000000}"
VENDOR=m3/vendor/third_party
LEDGER='^(\./)?(\.m3/task-crates/|m3/sanitize/|.*/(qualification|verification|evidence|receipts|snapshots|history|retained-variants|upstream-excluded|META-INF)/)'
log() { printf '%s %s\n' "$(date +%H:%M:%S)" "$*"; }
TOUCHED=()
restore() { [ ${#TOUCHED[@]} -gt 0 ] && git restore --source=HEAD --staged --worktree -- "${TOUCHED[@]}" 2>/dev/null; }
die() { log "HELD: $*"; printf 'HELD\t%s\n' "$*" > "$OUT/HELD_${OLD}.txt"; restore; exit 2; }
cd "$WT" || { echo "no worktree $WT" >&2; exit 2; }
mkdir -p "$OUT"; OUT="$(cd "$OUT" && pwd)"; rm -f "$OUT/HELD_${OLD}.txt"
log "worktree $(pwd) head $(git rev-parse --short HEAD) branch $(git branch --show-current)"
[ -f "$VENDOR/$OLD/pom.xml" ] && [ -f "$VENDOR/$NEW/pom.xml" ] || die "donor poms missing (the sparse set must hold every pom under m3/)"

# 0. coordinates + consumers from the pom model of every pom on disk
find m3 -name pom.xml -type f -not -path '*/target/*' -print0 | sort -z | xargs -0 awk -f "$TOOLS/pom_model.awk" > "$OUT/MODEL.tsv"
coord() { awk -F'\t' -v p="$1" '$1 == p && $2 == "PARENT" { pg = $4; pv = $6 } $1 == p && $2 == "COORD" { g = $4; a = $5; v = $6 }
  END { printf "%s\t%s\t%s\n", (g != "" ? g : pg), a, (v != "" ? v : pv) }' "$OUT/MODEL.tsv"; }
IFS=$'\t' read -r G A OLDV < <(coord "$VENDOR/$OLD/pom.xml")
IFS=$'\t' read -r G2 A2 NEWV < <(coord "$VENDOR/$NEW/pom.xml")
[ "$G:$A" = "$G2:$A2" ] && [ -n "$OLDV" ] && [ -n "$NEWV" ] || die "coordinates differ or unreadable: $G:$A:$OLDV vs $G2:$A2:$NEWV"
log "repoint $G:$A $OLDV ($OLD) -> $NEWV ($NEW)"
pins() {  # every pom DEP record on G:A:OLDV -> pom<TAB>profile<TAB>section<TAB>version
  awk -F'\t' -v g="$G" -v a="$A" -v v="$OLDV" -v old="$VENDOR/$OLD/" -v OFS='\t' \
    '$2 == "DEP" && $5 == g && $6 == a && ($7 == v || index($7, "${")) && index($1, old) != 1 { print $1, $3, $4, $7 }' "$OUT/MODEL.tsv"
}
pins > "$OUT/PINS_${OLD}_before.tsv"
if [ $# -gt 0 ]; then CONSUMERS=("$@")
else mapfile -t CONSUMERS < <(awk -F'\t' -v L="$LEDGER" '$4 !~ /\$\{/ && $1 !~ L { print $1 }' "$OUT/PINS_${OLD}_before.tsv" | sort -u); fi
log "consumers (${#CONSUMERS[@]}): ${CONSUMERS[*]:-none}"
awk -F'\t' -v L="$LEDGER" '$4 ~ /\$\{/ && $1 !~ L' "$OUT/PINS_${OLD}_before.tsv" > "$OUT/PROPERTY_PINS_${OLD}.tsv"

# 1. baseline proof: pins + directory-name mentions
dirmentions() { grep -rnF --exclude-dir=.git --exclude-dir="$OLD" -- "$OLD" . 2>/dev/null | sed 's#^\./##' | sort -u; }
{ sed 's/^/PIN\t/' "$OUT/PINS_${OLD}_before.tsv"; dirmentions | sed 's/^/DIR\t/'; } > "$OUT/PROOF_${OLD}_before.txt"
log "baseline: $(wc -l < "$OUT/PINS_${OLD}_before.tsv") pins, $(grep -c '^DIR' "$OUT/PROOF_${OLD}_before.txt") directory mentions"

# 2. sparse set: consumers' compile closure (plan of the gate), donor-new, donor-old
PLAN="$OUT/plan"; mkdir -p "$PLAN"
gate() { OUT="$1" JDK="$JDK" REPO="$(pwd)" EXTRA_UNITS="${CONSUMERS[*]}" UNITS_ONLY="${CONSUMERS[*]}" KEEP_CLASSES=0 \
  bash "$TOOLS/self_contained_gate.sh" "$2" > "$1.console" 2>&1; }
[ ${#CONSUMERS[@]} -gt 0 ] && { gate "$PLAN" plan; true; }
SC="$(git rev-parse --git-path info/sparse-checkout)"; need=0
add_sparse() { grep -qxF -- "$1" "$SC" || { printf '%s\n' "$1" >> "$SC"; need=1; }; }
add_sparse "/$VENDOR/$NEW/"; add_sparse "/$VENDOR/$OLD/"
for c in "${CONSUMERS[@]}"; do add_sparse "/$(dirname "$c")/"; done
if [ -s "$PLAN/EDGES.tsv" ]; then
  for c in "${CONSUMERS[@]}"; do
    awk -F'\t' -v c="$c" '$1 == c { print $3 }' "$PLAN/EDGES.tsv" | while IFS= read -r p; do echo "$p"; awk -F'\t' -v p="$p" '$1 == p { print $3 }' "$PLAN/EDGES.tsv"; done
  done | sort -u | while IFS= read -r p; do d=$(dirname "$p"); printf '/%s/src/main/java/\n/%s/src/main/resources/\n' "$d" "$d"; done > "$OUT/closure_sparse.txt"
  while IFS= read -r l; do add_sparse "$l"; done < "$OUT/closure_sparse.txt"
fi
avail=$(df -k "$DISK_MOUNT" | awk 'NR==2{print $4}')
log "disk $DISK_MOUNT avail_kb=$avail floor_kb=$DISK_FLOOR_KB fetch_needed=$need"
if [ "$need" = 1 ]; then
  [ "$avail" -ge "$DISK_FLOOR_KB" ] || die "disk $DISK_MOUNT below floor; blob fetch refused"
  git sparse-checkout reapply 2>/dev/null || die "sparse reapply failed"
fi

# 3. gate BEFORE (donor-old on the classpath)
if [ ${#CONSUMERS[@]} -gt 0 ]; then gate "$OUT/before" compile; log "gate before: $(awk -F'\t' 'NR > 1 && $2 == "extra" { printf "%s %s errors=%s; ", $1, $7, $6 }' "$OUT/before/COMPILE.tsv")"; fi

# 4. repoint (literal version inside the matching <dependency> block)
for c in "${CONSUMERS[@]}"; do
  awk -F'\t' -v c="$c" '$1 == c && $4 ~ /\$\{/' "$OUT/PINS_${OLD}_before.tsv" | grep -q . && die "$c pins $A through a property; hand decision"
  n=$(awk -v a="$A" -v v="$OLDV" 'BEGIN { RS = "<dependency>" } NR > 1 { split($0, b, "</dependency>"); if (index(b[1], "<artifactId>" a "</artifactId>") && index(b[1], "<version>" v "</version>")) k++ } END { print k + 0 }' "$c")
  [ "$n" -ge 1 ] || die "$c: no <dependency> block with $A $OLDV"
  awk -v a="$A" -v v="$OLDV" -v nv="$NEWV" 'BEGIN { RS = "<dependency>"; ORS = "" }
    NR > 1 { i = index($0, "</dependency>"); blk = substr($0, 1, i); rest = substr($0, i + 1)
             if (index(blk, "<artifactId>" a "</artifactId>")) { j = index(blk, "<version>" v "</version>"); if (j) blk = substr(blk, 1, j - 1) "<version>" nv "</version>" substr(blk, j + length("<version>" v "</version>")) }
             $0 = blk rest; printf "%s", "<dependency>" }
    { printf "%s", $0 }' "$c" > "$OUT/pom.tmp" && cat "$OUT/pom.tmp" > "$c"; TOUCHED+=("$c")
  prov="$(dirname "$c")/PROVENANCE.json"
  if [ -f "$prov" ]; then
    p=$(grep -c -F "\"version\": \"$OLDV\"" "$prov"); p=${p:-0}
    if [ "$p" = 1 ]; then sed -b -i "s#\"version\": \"$OLDV\"#\"version\": \"$NEWV\"#" "$prov"; TOUCHED+=("$prov")
    elif [ "$p" -gt 1 ]; then die "$prov pins $OLDV $p times"; fi
  fi
  log "repointed $c ($n block(s))"
done

# 5. gate AFTER (donor-new on the classpath) + verdict per consumer
printf 'consumer\tbefore_status\tbefore_errors\tafter_status\tafter_errors\tadded_errors\tgate\n' > "$OUT/REPOINT_GATE_${OLD}.tsv"
if [ ${#CONSUMERS[@]} -gt 0 ]; then
  gate "$OUT/after" compile
  for c in "${CONSUMERS[@]}"; do
    IFS=$'\t' read -r bs be < <(awk -F'\t' -v c="$c" '$1 == c && $3 == "main" { print $7 "\t" $6 }' "$OUT/before/COMPILE.tsv")
    IFS=$'\t' read -r as ae < <(awk -F'\t' -v c="$c" '$1 == c && $3 == "main" { print $7 "\t" $6 }' "$OUT/after/COMPILE.tsv")
    norm() { awk -F'\t' -v c="$c" '$1 == c && $2 == "main" { print $3 ":" $4 ": " $5 }' "$1/COMPILE_ERRORS.tsv" | sort -u; }
    comm -13 <(norm "$OUT/before") <(norm "$OUT/after") > "$OUT/added_$(basename "$(dirname "$c")").txt"
    add=$(wc -l < "$OUT/added_$(basename "$(dirname "$c")").txt" | tr -d ' ')
    if [ "$as" = PASS ]; then g=PLAIN_PASS; elif [ "$add" = 0 ] && [ "$as" = FAIL ]; then g=DIFFERENTIAL_PASS; else g=FAIL; fi
    printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$c" "${bs:-?}" "${be:-?}" "${as:-?}" "${ae:-?}" "$add" "$g" >> "$OUT/REPOINT_GATE_${OLD}.tsv"
    log "gate $c: before ${bs:-?}/${be:-?} after ${as:-?}/${ae:-?} added=$add -> $g"
    [ "$g" = FAIL ] && { head -20 "$OUT/added_$(basename "$(dirname "$c")").txt"; die "repoint breaks $c (javac: $add added errors; see $OUT/after)"; }
  done
fi

# 6. drop donor-old + aggregator line
FILES=$(git ls-tree -r --name-only HEAD -- "$VENDOR/$OLD" | wc -l | tr -d ' ')
BYTES=$(GIT_NO_LAZY_FETCH=1 git ls-tree -r -l HEAD -- "$VENDOR/$OLD" 2>/dev/null | awk '$4 ~ /^[0-9]+$/ { b += $4 } END { print b + 0 }')
git rm -r -q --sparse -- "$VENDOR/$OLD" || die "git rm $OLD failed"; TOUCHED+=("$VENDOR/$OLD")
AGG="$VENDOR/pom.xml"; a=$(grep -c -F "<module>$OLD</module>" "$AGG"); a=${a:-0}
[ "$a" -le 1 ] || die "aggregator names $OLD $a times"
[ "$a" = 1 ] && { sed -b -i "/<module>$OLD<\/module>/d" "$AGG"; TOUCHED+=("$AGG"); }
log "dropped $VENDOR/$OLD: $FILES files, $BYTES bytes; aggregator lines removed: $a"

# 7. proof after
find m3 -name pom.xml -type f -not -path '*/target/*' -print0 | sort -z | xargs -0 awk -f "$TOOLS/pom_model.awk" > "$OUT/MODEL.tsv"
pins > "$OUT/PINS_${OLD}_after.tsv"
{ sed 's/^/PIN\t/' "$OUT/PINS_${OLD}_after.tsv"; dirmentions | sed 's/^/DIR\t/'; } > "$OUT/PROOF_${OLD}_after.txt"
awk -F'\t' -v L="$LEDGER" '{ p = $2; sub(/:.*/, "", p) } p ~ L { next } $0 ~ /JMOD_IMAGE\.tsv:.*OLDER_VERSION_OF_SAME_MODULE/ { next } { print }' "$OUT/PROOF_${OLD}_after.txt" > "$OUT/PROOF_${OLD}_blocking.txt"
TOTAL=$(wc -l < "$OUT/PROOF_${OLD}_after.txt" | tr -d ' '); BLOCK=$(wc -l < "$OUT/PROOF_${OLD}_blocking.txt" | tr -d ' ')
log "proof: before $(wc -l < "$OUT/PROOF_${OLD}_before.txt" | tr -d ' '), after $TOTAL ($((TOTAL - BLOCK)) ledger rows), blocking $BLOCK"
V=PASS; [ "$BLOCK" = 0 ] || V=HELD
{ printf 'drop\tkeep\tcoordinates\tconsumers\tgate\tfiles_dropped\tbytes_dropped\tproof_before\tproof_after\tproof_blocking\tverdict\n'
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$OLD" "$NEW" "$G:$A $OLDV -> $NEWV" "${CONSUMERS[*]:-none}" \
    "$(awk -F'\t' 'NR > 1 { printf "%s%s", (n++ ? ";" : ""), $7 }' "$OUT/REPOINT_GATE_${OLD}.tsv")" "$FILES" "$BYTES" \
    "$(wc -l < "$OUT/PROOF_${OLD}_before.txt" | tr -d ' ')" "$TOTAL" "$BLOCK" "$V"; } > "$OUT/REPOINT_RESULT_${OLD}.tsv"
cat "$OUT/REPOINT_RESULT_${OLD}.tsv"
[ "$V" = PASS ] || { cat "$OUT/PROOF_${OLD}_blocking.txt"; die "blocking references remain"; }
log "chain complete; review git diff, then commit by explicit paths"
}
