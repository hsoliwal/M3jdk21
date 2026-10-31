#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
set -euo pipefail
crate=$(cd "$(dirname "$0")" && pwd)
root=$(cd "${1:?exact dependency checkout required}" && pwd)
res="$crate/../../src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-view-order-sdk-20261020"
relative=m3/collections/src/main/java/com/m3/collections
emitted="$crate/target/generated-receiver/$relative/M3LongIndexProjection.java"
evidence="$crate/target/evidence"
mkdir -p "$evidence"
exec > >(tee "$evidence/verification.log") 2>&1
[[ $(git -C "$root" rev-parse HEAD) == 987f743658055252bdaf056610a3a903eba0bfee ]]
git -C "$root" diff --exit-code -- m3/collections/src/main src/java.base/share/native/libjava/m3_jni_util.h
[[ $(git hash-object "$root/$relative/M3LongIndexProjection.java") == 317136e66200db1d8fc72d47ff9f8cf52b4566b5 ]]
[[ $(git hash-object "$root/$relative/M3IndexMaskCompactor.java") == b81b069fb4815f0c78b17119390d7b204f6179d1 ]]
[[ $(git hash-object "$root/m3/collections/src/main/native/collections/m3_index_mask_compactor.c") == 8fb2c3fb8e5642deebcfcd482200854ea131912a ]]
[[ $(git hash-object "$root/m3/collections/src/main/native/collections/m3_long_address_kernel.c") == 821876dfb56d836a0ae23e4bc16a391fd7f97ef2 ]]
echo "a29b0bc589565924611538c6fabe15a70d3629bc4eb3db0977d19f411ef4eb4e  $emitted" | sha256sum -c -
echo "77ef7d6197ef936f5ae9f62c35accf2f5f3ff690dcc55ca3fd4d8f545a007545  $res/M3LongIndexProjection.before.java.txt" | sha256sum -c -
echo "f3c44e00f608aba01171e7ce781a233949686034cb621254ade6af8ebf3bbc54  $crate/proof/M3ProjectionRefinementProbe.java" | sha256sum -c -
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
jdk=$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")
mkdir "$work/native" "$work/absent"
for variant in before generated; do
 dir="$work/$variant"
 mkdir -p "$dir/src" "$dir/classes" "$dir/headers"
 if [[ $variant == before ]]; then cp "$res/M3LongIndexProjection.before.java.txt" "$dir/src/M3LongIndexProjection.java"; else cp "$emitted" "$dir/src/M3LongIndexProjection.java"; fi
 javac --release 21 -proc:none -Xlint:all -Werror -h "$dir/headers" -d "$dir/classes" \
  "$dir/src/M3LongIndexProjection.java" "$root/$relative/M3IndexMaskCompactor.java" \
  "$root/$relative/M3LongAddressKernel.java" "$crate/proof/M3ProjectionRefinementProbe.java"
 if [[ $variant == before ]]; then
  for pair in m3_index_mask_compactor:m3_collections_mask m3_long_address_kernel:m3_collections_address; do
   source=${pair%%:*}; library=${pair#*:}
   gcc -std=c17 -O2 -Wall -Wextra -Werror -Wpedantic -fsanitize=undefined -fno-sanitize-recover=undefined -fPIC -shared \
    -I"$jdk/include" -I"$jdk/include/linux" -I"$dir/headers" \
    "$root/m3/collections/src/main/native/collections/$source.c" -o "$work/native/lib$library.so"
  done
 fi
 java -ea -Xmx96m -Djava.library.path="$work/absent" -cp "$dir/classes" com.m3.collections.M3ProjectionRefinementProbe java | tee "$evidence/$variant-java.log"
 java -ea -Xmx96m -Xcheck:jni -Djava.library.path="$work/native" -cp "$dir/classes" com.m3.collections.M3ProjectionRefinementProbe native | tee "$evidence/$variant-native.log"
 java -ea -Djava.library.path="$work/absent" -cp "$dir/classes" com.m3.collections.M3ProjectionRefinementProbe absent | tee "$evidence/$variant-absent.log"
 javap -p -s -classpath "$dir/classes" com.m3.collections.M3LongIndexProjection 'com.m3.collections.M3LongIndexProjection$Order' > "$evidence/$variant-api.txt"
 for repeat in 1 2 3; do
  java -ea -Xmx256m -cp "$dir/classes" com.m3.collections.M3ProjectionRefinementProbe measure > "$evidence/$variant-measure-$repeat.json"
 done
done
for mode in java native absent; do diff -u "$evidence/before-$mode.log" "$evidence/generated-$mode.log"; done
diff -u "$evidence/before-api.txt" "$evidence/generated-api.txt"
diff -r "$work/before/headers" "$work/generated/headers"
if grep -E 'WARNING in native method|runtime error:' "$evidence/"*-native.log; then exit 1; fi
python3 - "$work/before/headers" "$work/native" <<'PY'
import pathlib,re,subprocess,sys
headers,libraries=map(pathlib.Path,sys.argv[1:])
expected=set()
for p in headers.glob('*.h'): expected.update(re.findall(r'(Java_\w+)\s*\(',p.read_text()))
actual=set()
for p in libraries.glob('*.so'):
 text=subprocess.check_output(['nm','-D','--defined-only',str(p)],text=True)
 actual.update(re.findall(r'\b(Java_\w+)',text))
if expected != actual: raise SystemExit(f'JNI export mismatch: {expected ^ actual}')
print('JNI_EXPORTS_MATCH count='+str(len(actual)))
PY
printf '%s\n' 'CANONICAL_SDK_CURRENT_STACK_JAVA_JNI_PASS'
