#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$here/layout.sh"

fail() {
  printf 'SELF_TEST_FAIL:%s\n' "$1" >&2
  exit 1
}

assert_eq() {
  local expected="$1" actual="$2" label="$3"
  [[ "$actual" == "$expected" ]] || fail "$label expected=$expected actual=$actual"
}

assert_eq "src/main/java" "$(m3_java_root "src/main/java/p/A.java")" "maven-main-root"
assert_eq "module-a/src/test/java" "$(m3_java_root "module-a/src/test/java/p/ATest.java")" "maven-test-root"
assert_eq "src/java.base/share/classes" "$(m3_java_root "src/java.base/share/classes/java/lang/String.java")" "jdk-share-root"
assert_eq "src/java.base/unix/classes" "$(m3_java_root "src/java.base/unix/classes/sun/nio/ch/UnixDispatcher.java")" "jdk-platform-root"
assert_eq "make/jdk/src/classes" "$(m3_java_root "make/jdk/src/classes/build/tools/foo/Tool.java")" "jdk-make-root"
assert_eq "test/jdk" "$(m3_java_root "test/jdk/java/lang/String/Basic.java")" "jdk-test-root"
assert_eq "test/hotspot/jtreg" "$(m3_java_root "test/hotspot/jtreg/runtime/foo/Test.java")" "hotspot-jtreg-root"

tmp="$(mktemp -d)"
trap 'rm -rf -- "$tmp"' EXIT
repo="$tmp/repo"
mkdir -p "$repo/module-a/src/main/java/p" "$repo/src/java.base/share/classes/java/lang" "$repo/test/jdk/java/lang"
: > "$repo/module-a/pom.xml"
roots="$tmp/roots.txt"
external="$tmp/external.txt"
printf '%s\n' \
  "module-a/src/main/java" \
  "src/java.base/share/classes" \
  "test/jdk" > "$roots"

m3_root_has_pom "$repo" "module-a/src/main/java" || fail "maven-root-not-owned"
if m3_root_has_pom "$repo" "src/java.base/share/classes"; then
  fail "openjdk-root-falsely-maven-owned"
fi

m3_collect_external_roots "$repo" "$roots" "$external"
mapfile -t values < "$external"
assert_eq "2" "${#values[@]}" "external-root-count"
assert_eq "src/java.base/share/classes" "${values[0]}" "external-root-0"
assert_eq "test/jdk" "${values[1]}" "external-root-1"

printf 'M3_ATOM_PATTERN_LAYOUT_SELF_TEST_PASS roots=%s\n' "${#values[@]}"
