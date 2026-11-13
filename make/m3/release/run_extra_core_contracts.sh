#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Additive release-qualification runner; never edit pinned make/m3/build.sh.
set -euo pipefail
cd "$(dirname "$0")/.."
: "${M3JDK:?Set M3JDK to an isolated JDK 21 directory}"
test -s build/jdk.m3.text.jar || { echo 'HOLD: run the historical make/m3/build.sh first' >&2; exit 1; }
mkdir -p build/tests build/logs

# Deliberately compile all additional executable tests against the *built*
# historical foundation artifact, without modifying its sealed source.
"$M3JDK/bin/javac" --release 21 --module-path build/jdk.m3.text.jar \
  --add-modules jdk.m3.text -d build/tests \
  core/test/M3LanguageCorpusManifestTest.java \
  core/test/M3RopeTextIndexTest.java \
  core/test/M3PrecomputePayloadFieldPatternTest.java \
  core/test/M3WordRowsContractTest.java \
  core/test/LexiconToolContractTest.java \
  core/test/M3BigStringContractTest.java \
  core/test/M3InstanceIndexPrecomputeEdgeContractTest.java \
  core/test/M3LangDexSourceEnvelopeRefusalContractTest.java \
  core/test/M3LanguageCorpusManifestRefusalContractTest.java \
  core/test/M3LexiconPrecomputeEdgeContractTest.java \
  core/test/M3PrecomputePayloadParserContractTest.java \
  core/test/M3Sha256UnavailableContractTest.java \
  core/test/M3StringPieceContractTest.java \
  core/test/M3WordRowsNativeLoadContractTest.java \
  core/test/SharedLexiconCatalogRefusalContractTest.java \
  core/test/SharedLexiconFamilySidecarRefusalContractTest.java \
  core/test/SharedLexiconImageContractTest.java \
  core/test/SharedLexiconPrecomputeCatalogContractTest.java \
  core/test/SharedRelatedLexemeCatalogContractTest.java > build/logs/extra-core-compile.log 2>&1

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

# Streaming contracts: rope appendTo windows and blockwise catalog shard digests.
"$M3JDK/bin/javac" --release 21 --module-path build/jdk.m3.text.jar --add-modules jdk.m3.text \
  -d build/tests core/test/M3StreamingContractTest.java >> build/logs/extra-core-compile.log 2>&1
"$M3JDK/bin/java" -ea -esa --module-path build/jdk.m3.text.jar --add-modules jdk.m3.text -cp build/tests M3StreamingContractTest | tee build/logs/extra-streaming-contract.log

# Word-row fixture worker copies java.class.path, not --module-path. Keep the
# complete released module JAR on this one test's classpath for child parity.
"$M3JDK/bin/java" -ea -esa -cp build/tests:build/jdk.m3.text.jar M3WordRowsContractTest | tee build/logs/extra-word-rows-contract.log

# Changed-atom contracts (M3_CHANGED_ATOM_COVERAGE): exported API refusals and edges.
"$M3JDK/bin/java" -ea -esa --module-path build/jdk.m3.text.jar --add-modules jdk.m3.text -cp build/tests LexiconToolContractTest | tee build/logs/extra-lexicon-tool-contract.log
"$M3JDK/bin/java" -ea -esa --module-path build/jdk.m3.text.jar --add-modules jdk.m3.text -cp build/tests M3BigStringContractTest | tee build/logs/extra-big-string-contract.log
"$M3JDK/bin/java" -ea -esa --module-path build/jdk.m3.text.jar --add-modules jdk.m3.text -cp build/tests M3InstanceIndexPrecomputeEdgeContractTest | tee build/logs/extra-instance-index-edge-contract.log
"$M3JDK/bin/java" -ea -esa --module-path build/jdk.m3.text.jar --add-modules jdk.m3.text -cp build/tests M3LangDexSourceEnvelopeRefusalContractTest | tee build/logs/extra-langdex-source-refusal-contract.log
"$M3JDK/bin/java" -ea -esa --module-path build/jdk.m3.text.jar --add-modules jdk.m3.text -cp build/tests M3LanguageCorpusManifestRefusalContractTest | tee build/logs/extra-corpus-manifest-refusal-contract.log
"$M3JDK/bin/java" -ea -esa --module-path build/jdk.m3.text.jar --add-modules jdk.m3.text -cp build/tests M3LexiconPrecomputeEdgeContractTest | tee build/logs/extra-lexicon-precompute-edge-contract.log
"$M3JDK/bin/java" -ea -esa --module-path build/jdk.m3.text.jar --add-modules jdk.m3.text -cp build/tests M3PrecomputePayloadParserContractTest | tee build/logs/extra-payload-parser-contract.log
"$M3JDK/bin/java" -ea -esa --module-path build/jdk.m3.text.jar --add-modules jdk.m3.text -cp build/tests M3Sha256UnavailableContractTest | tee build/logs/extra-sha256-unavailable-contract.log
"$M3JDK/bin/java" -ea -esa --module-path build/jdk.m3.text.jar --add-modules jdk.m3.text -cp build/tests M3StringPieceContractTest | tee build/logs/extra-string-piece-contract.log
"$M3JDK/bin/java" -ea -esa --module-path build/jdk.m3.text.jar --add-modules jdk.m3.text -cp build/tests SharedLexiconCatalogRefusalContractTest | tee build/logs/extra-lexicon-catalog-refusal-contract.log
"$M3JDK/bin/java" -ea -esa --module-path build/jdk.m3.text.jar --add-modules jdk.m3.text -cp build/tests SharedLexiconFamilySidecarRefusalContractTest | tee build/logs/extra-family-sidecar-refusal-contract.log
"$M3JDK/bin/java" -ea -esa --module-path build/jdk.m3.text.jar --add-modules jdk.m3.text -cp build/tests SharedLexiconImageContractTest | tee build/logs/extra-lexicon-image-contract.log
"$M3JDK/bin/java" -ea -esa --module-path build/jdk.m3.text.jar --add-modules jdk.m3.text -cp build/tests SharedLexiconPrecomputeCatalogContractTest | tee build/logs/extra-precompute-catalog-contract.log
"$M3JDK/bin/java" -ea -esa --module-path build/jdk.m3.text.jar --add-modules jdk.m3.text -cp build/tests SharedRelatedLexemeCatalogContractTest | tee build/logs/extra-related-lexeme-contract.log

# White-box contracts live in package jdk.m3.text. They are compiled as a patch
# of the built module and run inside it; the released JAR is never modified.
"$M3JDK/bin/javac" --release 21 --module-path build/jdk.m3.text.jar \
  --patch-module jdk.m3.text=core/test -d build/whitebox \
  core/test/jdk/m3/text/M3BigStringSelfTest.java \
  core/test/jdk/m3/text/M3FixedAddressMappingContractTest.java \
  core/test/jdk/m3/text/M3FixedAddressMappingRefusalContractTest.java \
  core/test/jdk/m3/text/M3GvasLiteralImageJniContractTest.java \
  core/test/jdk/m3/text/M3WordRowsImageRefusalContractTest.java > build/logs/extra-whitebox-compile.log 2>&1
"$M3JDK/bin/java" -ea -esa --module-path build/jdk.m3.text.jar --patch-module jdk.m3.text=build/whitebox -m jdk.m3.text/jdk.m3.text.M3BigStringSelfTest | tee build/logs/extra-big-string-self.log
"$M3JDK/bin/java" -ea -esa --module-path build/jdk.m3.text.jar --patch-module jdk.m3.text=build/whitebox -m jdk.m3.text/jdk.m3.text.M3WordRowsImageRefusalContractTest | tee build/logs/extra-word-rows-image-refusal.log

# JNI-backed owners are measured through their shipped native sources, built
# here with the host C compiler (CC, default cc) against the same JDK headers.
case "$(uname -s)" in
  Linux) jni_os=linux; jni_ext=so; jni_pic=(-fPIC) ;;
  Darwin) jni_os=darwin; jni_ext=dylib; jni_pic=(-fPIC) ;;
  MINGW*|MSYS*|CYGWIN*) jni_os=win32; jni_ext=dll; jni_pic=() ;;
  *) echo "HOLD: no JNI platform headers for $(uname -s)" >&2; exit 1 ;;
esac
mkdir -p build/native
for unit in m3_fixed_address_mapping_jni m3_wordrows_jni; do
  "${CC:-cc}" -std=c11 -O2 -Wall -Wextra -Werror "${jni_pic[@]}" -shared \
    -I"$M3JDK/include" -I"$M3JDK/include/$jni_os" \
    "lexicon/native/$unit.c" -o "build/native/$unit.$jni_ext" >> build/logs/extra-native-compile.log 2>&1
done
# Test-only fault double binding the same natives; it never maps a file.
"${CC:-cc}" -std=c11 -O2 -Wall -Wextra -Werror "${jni_pic[@]}" -shared \
  -I"$M3JDK/include" -I"$M3JDK/include/$jni_os" \
  core/test/native/m3_fixed_address_fault_jni.c -o "build/native/m3_fixed_address_fault_jni.$jni_ext" >> build/logs/extra-native-compile.log 2>&1
mapping_lib="build/native/m3_fixed_address_mapping_jni.$jni_ext"
# Fixed-address fixture: zero image with the two canonical payload bytes at 128.
mapping_image=build/native/fixed-address-fixture.bin
{ head -c 128 /dev/zero; printf '\x5a\x36'; head -c 126 /dev/zero; } > "$mapping_image"
mapping_sha=$(sha256sum "$mapping_image" | cut -d' ' -f1)
for mode in plain digest collision; do
  "$M3JDK/bin/java" -ea -esa -Xcheck:jni --module-path build/jdk.m3.text.jar --patch-module jdk.m3.text=build/whitebox -m jdk.m3.text/jdk.m3.text.M3FixedAddressMappingContractTest "$mode" "$mapping_lib" "$mapping_image" "$mapping_sha" | tee "build/logs/extra-fixed-address-$mode.log"
done
"$M3JDK/bin/java" -ea -esa -Xcheck:jni --module-path build/jdk.m3.text.jar --patch-module jdk.m3.text=build/whitebox -m jdk.m3.text/jdk.m3.text.M3FixedAddressMappingRefusalContractTest refusals "$mapping_lib" "build/native/m3_wordrows_jni.$jni_ext" build/native/fixed-address-refusals | tee build/logs/extra-fixed-address-refusals.log
"$M3JDK/bin/java" -ea -esa -Xcheck:jni --module-path build/jdk.m3.text.jar --patch-module jdk.m3.text=build/whitebox -m jdk.m3.text/jdk.m3.text.M3FixedAddressMappingRefusalContractTest fault "build/native/m3_fixed_address_fault_jni.$jni_ext" build/native/fixed-address-faults | tee build/logs/extra-fixed-address-faults.log
# A provider-free java.security file (no security.provider.N entries) removes SHA-256.
printf '# no security.provider.N entries: no MessageDigest SHA-256\n' > build/native/provider-free.security
"$M3JDK/bin/java" -ea -esa -Djava.security.properties==build/native/provider-free.security --module-path build/jdk.m3.text.jar --patch-module jdk.m3.text=build/whitebox -m jdk.m3.text/jdk.m3.text.M3FixedAddressMappingRefusalContractTest nosha "$mapping_lib" build/native/fixed-address-nosha | tee build/logs/extra-fixed-address-nosha.log
"$M3JDK/bin/java" -ea -esa -Xcheck:jni --module-path build/jdk.m3.text.jar --patch-module jdk.m3.text=build/whitebox -m jdk.m3.text/jdk.m3.text.M3GvasLiteralImageJniContractTest "$mapping_lib" build/native/gvas-literal | tee build/logs/extra-gvas-literal-jni.log
"$M3JDK/bin/java" -ea -esa -Xcheck:jni -cp build/tests:build/jdk.m3.text.jar M3WordRowsContractTest "build/native/m3_wordrows_jni.$jni_ext" | tee build/logs/extra-word-rows-jni-contract.log
"$M3JDK/bin/java" -ea -esa -Xcheck:jni --module-path build/jdk.m3.text.jar --add-modules jdk.m3.text -cp build/tests M3WordRowsNativeLoadContractTest "build/native/m3_wordrows_jni.$jni_ext" | tee build/logs/extra-word-rows-native-load.log
