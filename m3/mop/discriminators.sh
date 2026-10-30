#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
#
# Mutation discriminators for the MOP pillars: each line applies one plausible wrong
# implementation to a copy of src/, recompiles, and requires the named contract to FAIL.
# PASS = mutant killed. FAIL = mutant survived, mutation did not apply, or mutant did not compile.
set -uo pipefail
base="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
jhome="${M3_JAVA_HOME:-${JAVA_HOME:-}}"
javac="${jhome:+$jhome/bin/}javac"
java="${jhome:+$jhome/bin/}java"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
failed=0

# mutant NAME CONTRACT FILE SED_EXPR [FILE SED_EXPR]...
mutant() {
  local name="$1" contract="$2"
  shift 2
  rm -rf "$work/src" "$work/classes" "$work/tmp"
  cp -r "$base/src" "$work/src"
  mkdir -p "$work/tmp"
  while (($# > 1)); do
    local file="$work/src/main/java/com/m3/mop/$1"
    sed -i -e "$2" "$file"
    if cmp -s "$base/src/main/java/com/m3/mop/$1" "$file"; then
      printf 'FAIL discriminator %s: mutation did not apply to %s\n' "$name" "$1"
      failed=1
      return
    fi
    shift 2
  done
  if ! "$javac" --release 21 -proc:none -d "$work/classes" $(find "$work/src" -name '*.java') > "$work/log" 2>&1; then
    printf 'FAIL discriminator %s: mutant does not compile\n' "$name"
    failed=1
    return
  fi
  cp -r "$base/src/main/resources/META-INF" "$work/classes/"
  if "$java" -Djava.io.tmpdir="$work/tmp" -cp "$work/classes" "com.m3.mop.$contract" > "$work/log" 2>&1; then
    printf 'FAIL discriminator %s: %s survived the mutant\n' "$name" "$contract"
    failed=1
  else
    printf 'PASS discriminator %s: %s killed (%s)\n' "$name" "$contract" \
      "$(grep -m1 -E 'Error|Exception' "$work/log" | sed 's/^[^:]*: //' | cut -c1-120)"
  fi
}

mutant analyze-time-projection M3AtomizeContract \
  M3MopPlugin.java 's/case PARSE -> parsed(event);/case PARSE -> { }/' \
  M3MopPlugin.java 's/case ANALYZE -> analyzed.add(uri(event));/case ANALYZE -> { parsed(event); analyzed.add(uri(event)); }/'
mutant donor-13966-api-rule M3RefactorGateContract \
  M3AtomTyping.java 's/"METHOD:" + modifiers(source, method.getModifiers()) + ":typeParams="/"METHOD:" + ":typeParams="/' \
  M3RefactorGate.java 's/String missing = difference(exposedBefore, exposedAfter);/String missing = difference(exposedBefore, after.stream().map(M3Atom::signature).collect(Collectors.toSet()));/'
mutant total-count-ratchet M3RefactorGateContract \
  M3RefactorGate.java 's/filter(kind -> count(after, kind) > count(before, kind))/filter(kind -> LIMITS.stream().mapToInt(k -> count(after, k)).sum() > LIMITS.stream().mapToInt(k -> count(before, k)).sum())/'
mutant hold-admitted M3RefactorGateContract \
  M3RefactorGate.java 's/decide.apply(Verdict.HOLD_FIXED_POINT_NOT_RUN)/decide.apply(Verdict.ADMITTED)/'
mutant candidate-into-proven-memory M3RefactorGateContract \
  M3RecipeMemory.java 's/if (!ledger.getFileName().toString().endsWith(CANDIDATES)) {/if (false) {/'
mutant no-registry-persistence M3PatternContract \
  M3PatternRegistry.java 's/int registered = latest.getOrDefault(id, 0);/int registered = 0;/'
mutant partial-promotion M3MopChainContract \
  M3MopChain.java 's/decisions.stream().allMatch(Decision::admitted);/decisions.stream().anyMatch(Decision::admitted);/'

exit "$failed"
