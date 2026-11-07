#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Additive release-qualification runner; never edit pinned m3/build.sh.
set -euo pipefail
cd "$(dirname "$0")/.."
: "${M3JDK:?Set M3JDK to an isolated JDK 21 directory}"
test -s build/jdk.m3.text.jar || { echo 'HOLD: run the historical m3/build.sh first' >&2; exit 1; }
mkdir -p build/tests build/logs

# Deliberately compile all additional executable tests against the *built*
# historical foundation artifact, without modifying its sealed source.
"$M3JDK/bin/javac" --release 21 --module-path build/jdk.m3.text.jar \
  --add-modules jdk.m3.text -d build/tests \
  core/test/M3LanguageCorpusManifestTest.java \
  core/test/M3RopeTextIndexTest.java \
  core/test/M3PrecomputePayloadFieldPatternTest.java \
  core/test/M3WordRowsContractTest.java > build/logs/extra-core-compile.log 2>&1

# Repeat the UTF-16, regex and corpus test oracles in all four VM modes.
for mode in jit int nocompact c2; do
  flags=()
  case "$mode" in
    int) flags=(-Xint) ;;
    nocompact) flags=(-XX:-CompactStrings) ;;
    c2) flags=(-Xbatch -XX:-TieredCompilation) ;;
  esac
  "$M3JDK/bin/java" -ea -esa "${flags[@]}" --module-path build/jdk.m3.text.jar --add-modules jdk.m3.text -cp build/tests M3LanguageCorpusManifestTest | tee "build/logs/extra-corpus-manifest-$mode.log"
  "$M3JDK/bin/java" -ea -esa "${flags[@]}" --module-path build/jdk.m3.text.jar --add-modules jdk.m3.text -cp build/tests M3RopeTextIndexTest | tee "build/logs/extra-rope-text-index-$mode.log"
done

# Reflective ownership proof has an explicit test-only module open.
"$M3JDK/bin/java" -ea -esa --module-path build/jdk.m3.text.jar --add-modules jdk.m3.text --add-opens jdk.m3.text/jdk.m3.text=ALL-UNNAMED -cp build/tests M3PrecomputePayloadFieldPatternTest | tee build/logs/extra-payload-field-pattern.log

# Word-row fixture worker copies java.class.path, not --module-path. Keep the
# complete released module JAR on this one test's classpath for child parity.
"$M3JDK/bin/java" -ea -esa -cp build/tests:build/jdk.m3.text.jar M3WordRowsContractTest | tee build/logs/extra-word-rows-contract.log
