#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${M3_JDK:?Set M3_JDK to an isolated JDK 21 directory}"
mkdir -p build/classes build/tests build/logs
mapfile -t sources < <(find core/src -name '*.java' | sort)
"$M3_JDK/bin/javac" --release 21 -Xlint:all,-module -d build/classes "${sources[@]}" > build/logs/compile.log 2>&1
"$M3_JDK/bin/jar" --create --file build/com.m3.text.jar -C build/classes .
"$M3_JDK/bin/javac" --release 21 --module-path build/com.m3.text.jar --add-modules com.m3.text -d build/tests \
  core/test/FoundationTest.java core/test/M3LexiconPrecomputeTest.java \
  core/test/SynexiaPrecomputePayloadTest.java core/test/SynexiaPrecomputeReceiverTest.java \
  core/test/SharedLexiconFamilySidecarCatalogTest.java core/test/SharedLexiconFamilyContractTest.java core/test/SynexiaSiUnitDecoderTest.java \
  core/test/M3NumberSpaceTest.java core/test/M3NumberPrecomputeTest.java core/test/M3SiUnitPrecomputeCatalogTest.java \
  core/test/M3TypedPrecomputeReceiverTest.java core/test/M3LanguageGrammarSupportTest.java \
  core/test/M3FrequencyCountContractTest.java \
  core/test/SharedLexiconCatalogFamilyAdmissionTest.java
for mode in jit int nocompact c2; do
 flags=();case "$mode" in int)flags=(-Xint);; nocompact)flags=(-XX:-CompactStrings);; c2)flags=(-Xbatch -XX:-TieredCompilation);; esac
 "$M3_JDK/bin/java" -ea -esa "${flags[@]}" --module-path build/com.m3.text.jar --add-modules com.m3.text -cp build/tests FoundationTest | tee "build/logs/test-$mode.log"
 "$M3_JDK/bin/java" -ea -esa "${flags[@]}" --module-path build/com.m3.text.jar --add-modules com.m3.text -cp build/tests M3LexiconPrecomputeTest | tee "build/logs/typed-precompute-$mode.log"
 "$M3_JDK/bin/java" -ea -esa "${flags[@]}" --module-path build/com.m3.text.jar --add-modules com.m3.text -cp build/tests SynexiaPrecomputePayloadTest | tee "build/logs/synexia-payload-$mode.log"
 "$M3_JDK/bin/java" -ea -esa "${flags[@]}" --module-path build/com.m3.text.jar --add-modules com.m3.text -cp build/tests SynexiaPrecomputeReceiverTest | tee "build/logs/synexia-receiver-$mode.log"
 "$M3_JDK/bin/java" -ea -esa "${flags[@]}" --module-path build/com.m3.text.jar --add-modules com.m3.text -cp build/tests SharedLexiconFamilySidecarCatalogTest | tee "build/logs/synexia-family-sidecar-$mode.log"
 "$M3_JDK/bin/java" -ea -esa "${flags[@]}" --module-path build/com.m3.text.jar --add-modules com.m3.text -cp build/tests SharedLexiconFamilyContractTest | tee "build/logs/synexia-family-contract-$mode.log"
 "$M3_JDK/bin/java" -ea -esa "${flags[@]}" --module-path build/com.m3.text.jar --add-modules com.m3.text -cp build/tests SynexiaSiUnitDecoderTest | tee "build/logs/synexia-si-unit-$mode.log"
 "$M3_JDK/bin/java" -ea -esa "${flags[@]}" --module-path build/com.m3.text.jar --add-modules com.m3.text -cp build/tests M3NumberSpaceTest | tee "build/logs/synexia-number-space-$mode.log"
 "$M3_JDK/bin/java" -ea -esa "${flags[@]}" --module-path build/com.m3.text.jar --add-modules com.m3.text -cp build/tests M3NumberPrecomputeTest | tee "build/logs/synexia-number-precompute-$mode.log"
 "$M3_JDK/bin/java" -ea -esa "${flags[@]}" --module-path build/com.m3.text.jar --add-modules com.m3.text -cp build/tests M3SiUnitPrecomputeCatalogTest | tee "build/logs/synexia-si-unit-catalog-$mode.log"
 "$M3_JDK/bin/java" -ea -esa "${flags[@]}" --module-path build/com.m3.text.jar --add-modules com.m3.text -cp build/tests M3TypedPrecomputeReceiverTest | tee "build/logs/synexia-typed-receiver-matrix-$mode.log"
 "$M3_JDK/bin/java" -ea -esa "${flags[@]}" --module-path build/com.m3.text.jar --add-modules com.m3.text -cp build/tests M3LanguageGrammarSupportTest | tee "build/logs/synexia-grammar-support-$mode.log"
 "$M3_JDK/bin/java" -ea -esa "${flags[@]}" --module-path build/com.m3.text.jar --add-modules com.m3.text -cp build/tests M3FrequencyCountContractTest | tee "build/logs/synexia-frequency-count-contract-$mode.log"
 "$M3_JDK/bin/java" -ea -esa "${flags[@]}" --module-path build/com.m3.text.jar --add-modules com.m3.text -cp build/tests SharedLexiconCatalogFamilyAdmissionTest | tee "build/logs/synexia-catalog-family-admission-$mode.log"
done
# Build in a private temporary directory, then publish a complete content-addressed file.
lexicon_tmp=$(mktemp -d build/lexicon.XXXXXX)
trap 'rm -rf "$lexicon_tmp"' EXIT
"$M3_JDK/bin/java" --module-path build/com.m3.text.jar -m com.m3.text/com.m3.text.LexiconTool build lexicon/english-demo.txt "$lexicon_tmp/image"
image="build/english-demo-$(sha256sum "$lexicon_tmp/image" | cut -d' ' -f1).m3lex"
chmod a-w "$lexicon_tmp/image"
mv -n "$lexicon_tmp/image" "$image"
if [[ -f "$lexicon_tmp/image" ]]; then
 cmp "$lexicon_tmp/image" "$image"
 rm "$lexicon_tmp/image"
fi
rmdir "$lexicon_tmp"
trap - EXIT
printf '%s\n' "$image" > build/english-image-path.txt
sha256sum build/com.m3.text.jar "$image" | tee build/logs/artifacts.sha256
