#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
here=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
build=$(mktemp -d)
trap 'rm -rf "$build"' EXIT
javac --release 21 -Xlint:all -Werror -d "$build/classes" "$here/GroovyExtensionCollisionAudit.java"
java -cp "$build/classes" GroovyExtensionCollisionAudit selftest
java -cp "$build/classes" GroovyExtensionCollisionAudit defaults "$build/defaults"
java -cp "$build/classes" GroovyExtensionCollisionAudit defaults "$build/defaults"
test "$(grep -cv '^#' "$build/defaults/owners.txt")" = 35
test "$(grep -cv '^#' "$build/defaults/providers.txt")" = 5
printf '
modified
' >> "$build/defaults/owners.txt"
if java -cp "$build/classes" GroovyExtensionCollisionAudit defaults "$build/defaults" >/dev/null 2>&1; then
  echo "FAIL: changed owner inventory was accepted" >&2; exit 1
fi
# Orchestrator smoke: only synthetic providers; absolutely not real Groovy evidence.
printf 'GroovyExtensionCollisionAudit$SyntheticReceiver\n' > "$build/owners.txt"
printf 'GroovyExtensionCollisionAudit$SyntheticExtension\n' > "$build/providers.txt"
mkdir -p "$build/empty"
jar --create --file "$build/mock-groovy.jar" -C "$build/empty" .
printf 'mock-groovy.jar\t%s\n' "$(sha256sum "$build/mock-groovy.jar" | cut -d' ' -f1)" > "$build/lock.tsv"
java -cp "$build/classes" GroovyExtensionCollisionAudit compare-synthetic-locked \
  "$(readlink -f "$(command -v java)")" "$(readlink -f "$(command -v java)")" "$build/mock-groovy.jar" \
  "$build/owners.txt" "$build/providers.txt" "$build/lock.tsv" "$build/report"
grep -qx $'status\tMOCK_PASS' "$build/report/report.tsv"
# Stock Java must not be able to claim an M3 product probe even with synthetic providers.
if java -cp "$build/classes" GroovyExtensionCollisionAudit probe-m3 \
  "$build/owners.txt" "$build/providers.txt" >/dev/null 2>&1; then
  echo 'FAIL: stock Java accepted a candidate M3 image probe' >&2; exit 1
fi
# Mutation must fail before launching either JVM.
printf 'mock-groovy.jar\t%s\n' "$(printf '0%.0s' {1..64})" > "$build/lock.tsv"
if java -cp "$build/classes" GroovyExtensionCollisionAudit compare-synthetic-locked \
  "$(readlink -f "$(command -v java)")" "$(readlink -f "$(command -v java)")" "$build/mock-groovy.jar" \
  "$build/owners.txt" "$build/providers.txt" "$build/lock.tsv" "$build/negative" >/dev/null 2>&1; then
  echo "FAIL: tampered jar lock accepted" >&2; exit 1
fi
# Prove the full two-launcher BLOCK path (the shell launchers are protocol mocks).
cat > "$build/base-java" <<'MOCK'
#!/bin/sh
printf 'M3X\tV\tSYNTHETIC_ONLY\n'
printf 'M3X\tM\tjava.lang.String\tlength\t0\tjava.lang.String#length():int\n'
printf 'M3X\tE\tjava.lang.String\tcapitalize\t0\torg.codehaus.groovy.runtime.StringGroovyMethods#capitalize(java.lang.CharSequence):java.lang.String\n'
MOCK
cat > "$build/added-java" <<'MOCK'
#!/bin/sh
printf 'M3X\tV\tSYNTHETIC_ONLY\n'
printf 'M3X\tM\tjava.lang.String\tlength\t0\tjava.lang.String#length():int\n'
printf 'M3X\tM\tjava.lang.String\tcapitalize\t0\tjava.lang.String#capitalize():java.lang.String\n'
printf 'M3X\tE\tjava.lang.String\tcapitalize\t0\torg.codehaus.groovy.runtime.StringGroovyMethods#capitalize(java.lang.CharSequence):java.lang.String\n'
MOCK
chmod +x "$build/base-java" "$build/added-java"
printf 'mock-groovy.jar\t%s\n' "$(sha256sum "$build/mock-groovy.jar" | cut -d' ' -f1)" > "$build/lock.tsv"
if java -cp "$build/classes" GroovyExtensionCollisionAudit compare-synthetic-locked \
  "$build/base-java" "$build/added-java" "$build/mock-groovy.jar" \
  "$build/owners.txt" "$build/providers.txt" "$build/lock.tsv" "$build/block-report" \
  > "$build/block-log" 2>&1; then
  echo "FAIL: new String.capitalize extension shadow was not blocked" >&2; exit 1
fi
grep -q 'BLOCK.*NEW_EXTENSION_SHADOW.*java.lang.String.*capitalize' "$build/block-report/report.tsv"
grep -qx $'status\tBLOCK' <(head -n 1 "$build/block-report/report.tsv")
echo 'M3_GROOVY_EXTENSION_ORCHESTRATOR_PASS mockOnly=true newStringMemberBlocked=true'

# A successful synthetic run cannot be promoted by selecting the real mode.
if java -cp "$build/classes" GroovyExtensionCollisionAudit compare-locked \
  "$(readlink -f "$(command -v java)")" "$(readlink -f "$(command -v java)")" "$build/mock-groovy.jar" \
  "$build/owners.txt" "$build/providers.txt" "$build/lock.tsv" "$build/fake-real" 6.0.0 \
  >/dev/null 2>&1; then
  echo 'FAIL: identical stock and M3 executables accepted as real' >&2; exit 1
fi
if java -cp "$build/classes" GroovyExtensionCollisionAudit compare-locked \
  "$build/base-java" "$build/added-java" "$build/mock-groovy.jar" \
  "$build/owners.txt" "$build/providers.txt" "$build/lock.tsv" "$build/unapproved" 6.1.0 \
  >/dev/null 2>&1; then
  echo 'FAIL: unapproved Groovy version accepted' >&2; exit 1
fi
if java -cp "$build/classes" GroovyExtensionCollisionAudit compare-synthetic-locked \
  "$build/base-java" "$build/added-java" "$build/mock-groovy.jar" \
  "$build/owners.txt" "$build/providers.txt" "$build/lock.tsv" "$build/block-report" \
  >/dev/null 2>&1; then
  echo 'FAIL: existing report directory silently overwritten' >&2; exit 1
fi
echo 'M3_GROOVY_EXTENSION_HARDENED_REFUSALS_PASS sameJvm=true versionPin=true staleOutput=true'
