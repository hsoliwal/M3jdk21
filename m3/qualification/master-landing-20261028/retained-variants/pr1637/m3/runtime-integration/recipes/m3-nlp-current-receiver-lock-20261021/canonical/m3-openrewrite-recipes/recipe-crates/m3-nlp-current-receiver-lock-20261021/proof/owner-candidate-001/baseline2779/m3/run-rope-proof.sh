#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Compile and test the separately built M3 text module without altering java.base.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
jdk="$(printenv M3_JDK 2>/dev/null || printenv JAVA_HOME 2>/dev/null || true)"
if [[ -z "$jdk" || ! -x "$jdk/bin/javac" || ! -x "$jdk/bin/java" ]]; then
  echo "Set M3_JDK (or JAVA_HOME) to a real Java 21 JDK" >&2
  exit 2
fi
if [[ "$("$jdk/bin/javac" -version 2>&1)" != *" 21."* ]]; then
  echo "This standalone proof requires javac 21" >&2
  exit 2
fi

scratch="$(mktemp -d)"
trap 'rm -rf "$scratch"' EXIT
mkdir -p "$scratch/module" "$scratch/tests"
mapfile -t sources < <(find "$root/m3/core/src" -type f -name '*.java' | sort)
"$jdk/bin/javac" --release 21 -Xlint:all,-module -Werror \
  -d "$scratch/module" "${sources[@]}"
"$jdk/bin/javac" --release 21 --module-path "$scratch/module" \
  --add-modules com.m3.text -d "$scratch/tests" \
  "$root/m3/core/test/M3RopeTextIndexTest.java"

for profile in jit int noncompact c2; do
  flags=()
  case "$profile" in
    int) flags=(-Xint) ;;
    noncompact) flags=(-XX:-CompactStrings) ;;
    c2) flags=(-Xbatch -XX:-TieredCompilation) ;;
  esac
  echo "Running M3 rope-index Java21 proof: $profile"
  "$jdk/bin/java" -ea -esa "${flags[@]}" --module-path "$scratch/module" \
    --add-modules com.m3.text -cp "$scratch/tests" M3RopeTextIndexTest
done
