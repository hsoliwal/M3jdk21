#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
#
# Strict Java 21 proof of m3/mop: compile with every lint fatal, run every contract in the default
# VM and with -Xint, dogfood the javac Plugin and the CLI over this module's own sources, check the
# pattern registry, dry-run the chain, and kill the discriminator mutants. One PASS/FAIL line per
# step; exit status 1 when any step fails. M3_JAVA_HOME (or JAVA_HOME) selects a JDK 21.
set -uo pipefail
base="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
jhome="${M3_JAVA_HOME:-${JAVA_HOME:-}}"
javac="${jhome:+$jhome/bin/}javac"
java="${jhome:+$jhome/bin/}java"
build="$(mktemp -d)"
trap 'rm -rf "$build"' EXIT
failed=0
native() { if command -v cygpath > /dev/null 2>&1; then cygpath -m "$1"; else printf '%s' "$1"; fi; }
step() {
  local name="$1"
  shift
  if "$@" > "$build/step.log" 2>&1; then
    printf 'PASS %s\n' "$name"
  else
    printf 'FAIL %s\n' "$name"
    tail -20 "$build/step.log" | sed 's/^/  /'
    failed=1
  fi
}
b="$(native "$build")"
m="$(native "$base")"
classes="$b/classes"
mkdir -p "$build/tmp" "$build/classes/META-INF"
main=("$base"/src/main/java/com/m3/mop/*.java)
tests=("$base"/src/test/java/com/m3/mop/*.java)

step "javac --release 21 -Xlint:all -Werror (main+test)" \
  "$javac" --release 21 -proc:none -Xlint:all -Werror -d "$classes" "${main[@]}" "${tests[@]}"
cp -r "$base/src/main/resources/META-INF/services" "$build/classes/META-INF/"
for contract in M3RecipeMemoryContract M3RecipeMemoryAdmissionContract M3AtomizeContract \
    M3PatternContract M3RefactorGateContract M3MopChainContract; do
  step "$contract (default VM)" "$java" -Djava.io.tmpdir="$b/tmp" -cp "$classes" "com.m3.mop.$contract"
  step "$contract (-Xint)" "$java" -Xint -Djava.io.tmpdir="$b/tmp" -cp "$classes" "com.m3.mop.$contract"
done

dog="$b/dogfood"
step "dogfood javac -Xplugin:M3Mop over m3/mop main sources" \
  "$javac" --release 21 -proc:none -Xlint:all -Werror -cp "$classes" -d "$b/dogfood-classes" \
  "-Xplugin:M3Mop out=$dog/plugin root=$m registry=$m/PATTERN_REGISTRY.tsv" "${main[@]}"
step "dogfood CLI atomize over m3/mop main sources" "$java" -cp "$classes" com.m3.mop.M3Mop atomize \
  "--root=$m" "--out=$dog/cli" "--registry=$m/PATTERN_REGISTRY.tsv" "$m/src/main/java"
same() { local name; for name in FILES.tsv ATOMS.tsv PATTERNS.tsv MANIFEST.sha256; do cmp "$1/$name" "$2/$name" || return 1; done; }
step "dogfood plugin output == CLI output (byte-identical)" same "$build/dogfood/plugin" "$build/dogfood/cli"
[[ -f "$build/dogfood/plugin/SUMMARY.txt" ]] && printf 'INFO plugin %s\n' "$(cat "$build/dogfood/plugin/SUMMARY.txt")"
step "pattern registry schema M3-PATTERN/1" "$java" -cp "$classes" com.m3.mop.M3Mop registry-verify "$m/PATTERN_REGISTRY.tsv"
cp "$base/PATTERN_REGISTRY.tsv" "$build/registry.tsv"
step "pattern registry at fixed point for m3/mop sources (register appends 0)" bash -c \
  "'$java' -cp '$classes' com.m3.mop.M3Mop atomize --root='$m' --out='$dog/register' --registry='$b/registry.tsv' --register '$m/src/main/java' | grep -q ' registered=0\$' && cmp '$build/registry.tsv' '$base/PATTERN_REGISTRY.tsv'"
step "pattern plan over dogfood atomize" "$java" -cp "$classes" com.m3.mop.M3Mop plan "--atomize=$dog/cli" "--out=$dog/plan"
[[ -f "$build/dogfood/plan/PATTERN_PLAN.tsv" ]] && printf 'INFO plan rows=%s\n' "$(($(wc -l < "$build/dogfood/plan/PATTERN_PLAN.tsv") - 1))"
mkdir -p "$build/no-proposal"
step "dogfood chain (dry, no proposals) over m3/mop" "$java" -cp "$classes" com.m3.mop.M3Mop chain \
  "--root=$m/src/main/java" "--proposal=$b/no-proposal" "--out=$dog/chain"
step "discriminator mutants killed" bash "$base/discriminators.sh"
sed 's/^/  /' "$build/step.log"

if ((failed)); then printf 'M3_MOP_VERIFY FAIL\n'; exit 1; fi
printf 'M3_MOP_VERIFY PASS\n'
