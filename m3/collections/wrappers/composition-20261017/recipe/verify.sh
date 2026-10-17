#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
# Source-sealed qualification; production installation is a separate receiving step.
set -euo pipefail
crate=$(cd "$(dirname "$0")" && pwd)
out=${1:?new absolute proof output required}
receiver=$(cd "${2:?M3JDK21 checkout required}" && pwd)
emitted=${M3_COMPOSITION_EMITTED:-$crate/target/emitted}
case "$out" in /*) ;; *) echo 'absolute output required' >&2; exit 2;; esac
test ! -e "$out" || { echo 'proof output must be absent' >&2; exit 2; }
test -n "${JAVA_HOME:-}" || { echo 'JAVA_HOME must select Java 21' >&2; exit 2; }
mkdir -p "$out"
export PATH="$JAVA_HOME/bin:$PATH"
java -version 2>&1 | tee "$out/java-version.txt"
javac -version 2>&1 | tee "$out/javac-version.txt"
python3 - "$crate" "$receiver" "$emitted" "$out" <<'PY'
import hashlib,json,sys
from pathlib import Path
crate,receiver,emitted,out=map(Path,sys.argv[1:])
pins=json.loads((crate/'SOURCE_PINS.json').read_text())
h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
for row in pins['inherited_proofs']:
    if h(receiver/row['path'])!=row['sha256']:raise SystemExit('inherited proof drift: '+row['path'])
for relative,key in [
    ('proof/pinned/com/synexia/rewrite/M3HashPinnedJavaSnapshotRecipe.java','recipe_provider_sha256'),
    ('proof/pinned/com/synexia/rewrite/M3HashPinnedTextSnapshotRecipe.java','text_provider_sha256'),
    ('proof/native/RangeJniProof.java','native_java_sha256'),
    ('proof/native/range_iterator_bridge.c','native_c_sha256')]:
    if h(crate/relative)!=pins[key]:raise SystemExit('pinned dependency drift: '+relative)
for state in ('representative','batch'):
    resources=crate/'resources/com/synexia/rewrite/hash-pinned-java'/('wrapper-composition-'+state+'-20261017')
    receipt=(emitted/state/'SDK_RECEIPT.tsv').read_text()
    for row in (resources/'manifest.tsv').read_text().splitlines():
        path,before,after,template=row.split('\t')
        if h(resources/template)!=after:raise SystemExit('resource drift: '+path)
        if (emitted/state/path).read_bytes()!=(resources/template).read_bytes():raise SystemExit('SDK output drift: '+path)
        if path+'\t'+after+'\n' not in receipt:raise SystemExit('SDK receipt mismatch: '+path)
        if before!='ABSENT':
            if h(resources/'pre'/template)!=before:raise SystemExit('preimage drift: '+path)
    directory=out/(state+'-source');directory.mkdir()
    (directory/'M3CollectionViews.java').write_bytes((resources/'pre/M3CollectionViews.java.txt').read_bytes())
print('WRAPPER_COMPOSITION_SOURCE_SEALS_PASS routes=2 inherited_proofs=7')
PY
main=m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java
batch_test=m3/collections/src/test/java/com/m3/collections/FilteredBatchContracts.java
rep_test=m3/collections/wrappers/src/test/java/com/m3/collections/M3StoredRepresentativeProof.java
batch_source=$emitted/representative/$batch_test
rep_source=$emitted/batch/$rep_test
for state in representative batch; do
  javac --release 21 -proc:none -Xlint:all -Werror -d "$out/$state" \
    "$out/$state-source/M3CollectionViews.java" "$batch_source" "$rep_source"
done
# Each exact predecessor retains its existing capability and exposes the missing sibling.
java -ea -Xss256k -Xmx128m -cp "$out/representative" com.m3.collections.M3StoredRepresentativeProof | tee "$out/representative-existing.log"
java -ea -Xss256k -Xmx128m -cp "$out/representative" com.m3.collections.FilteredBatchContracts stable | tee "$out/representative-stable.log"
if java -ea -Xmx128m -cp "$out/representative" com.m3.collections.FilteredBatchContracts > "$out/representative-red.log" 2>&1; then
  echo 'representative predecessor unexpectedly passed batch regression' >&2; exit 1
fi
rg -q 'VALIDATED_REFERENCES_ONLY' "$out/representative-red.log"
java -ea -Xss256k -Xmx128m -cp "$out/batch" com.m3.collections.FilteredBatchContracts | tee "$out/batch-existing.log"
if java -ea -Xmx128m -cp "$out/batch" com.m3.collections.M3StoredRepresentativeProof > "$out/batch-red.log" 2>&1; then
  echo 'batch predecessor unexpectedly passed representative regression' >&2; exit 1
fi
rg -q 'search stored representative not external probe' "$out/batch-red.log"
src=$receiver/m3/collections/src/main/java/com/m3/collections
tests=$receiver/m3/collections/wrappers/src/test/java/com/m3/collections
javac --release 21 -proc:none -Xlint:all -Werror -h "$out/headers" -d "$out/after" \
  "$emitted/representative/$main" "$src/M3FilteredMaps.java" \
  "$batch_source" "$rep_source" "$tests/M3WrapperContractsProof.java" \
  "$tests/M3DeepFilterCompositionProof.java" "$tests/M3DeepFilterNativeProof.java" \
  "$crate/proof/native/RangeJniProof.java"
for mode in tiered interpreter; do
  flags=(); test "$mode" != interpreter || flags=(-Xint)
  for proof in M3StoredRepresentativeProof FilteredBatchContracts M3WrapperContractsProof; do
    java "${flags[@]}" -ea -Xss256k -Xmx256m -cp "$out/after" "com.m3.collections.$proof" | tee "$out/$proof-$mode.log"
  done
  depth=65536; test "$mode" != interpreter || depth=8192
  java "${flags[@]}" -ea -Xss256k -Xmx256m "-Dm3.deep.depth=$depth" \
    -cp "$out/after" com.m3.collections.M3DeepFilterCompositionProof | tee "$out/deep-$mode.log"
done
for state in representative batch after; do
  javap -public -s -cp "$out/$state" com.m3.collections.M3CollectionViews > "$out/$state-api.txt"
done
cmp "$out/representative-api.txt" "$out/after-api.txt"
cmp "$out/batch-api.txt" "$out/after-api.txt"
gcc -std=c17 -O2 -shared -fPIC -Wall -Wextra -Werror -Wpedantic \
  -fsanitize=undefined -fno-sanitize-recover=undefined \
  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" -I"$out/headers" \
  "$crate/proof/native/range_iterator_bridge.c" -o "$out/libwrapper_range.so"
java -Xmx96m -Xcheck:jni -XX:MaxJNILocalCapacity=64 -cp "$out/after" \
  com.synexia.common.collections.RangeJniProof "$out/libwrapper_range.so" | tee "$out/range-jni.log"
gcc -std=c17 -O2 -shared -fPIC -Wall -Wextra -Werror -Wpedantic \
  -fsanitize=undefined -fno-sanitize-recover=undefined \
  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" -I"$out/headers" \
  "$receiver/m3/collections/wrappers/direct-projection-20261017/m3_map_cursor_jni.c" \
  "$receiver/m3/collections/wrappers/deep-filter-20261017/native/m3_deep_filter_jni.c" \
  -o "$out/libwrapper_deep.so"
java -ea -Xss256k -Xmx256m -Xcheck:jni "-Dm3.deep.native.library=$out/libwrapper_deep.so" \
  -cp "$out/after" com.m3.collections.M3DeepFilterNativeProof | tee "$out/deep-jni.log"
nm -D --defined-only "$out/libwrapper_range.so" | rg ' Java_' > "$out/range-symbols.txt"
nm -D --defined-only "$out/libwrapper_deep.so" | rg ' Java_' > "$out/deep-symbols.txt"
javac --release 21 -proc:none -g:none -d "$out/before-stripped" "$out/representative-source/M3CollectionViews.java"
javac --release 21 -proc:none -g:none -d "$out/after-stripped" "$emitted/representative/$main"
python3 - "$out" <<'PY'
from pathlib import Path
import sys
out=Path(sys.argv[1]);before=out/'before-stripped/com/m3/collections';after=out/'after-stripped/com/m3/collections'
count=0
for source in before.glob('M3CollectionViews*.class'):
    if source.name=='M3CollectionViews$FilteredCollection.class':continue
    if source.read_bytes()!=(after/source.name).read_bytes():raise SystemExit('unaffected bytecode drift: '+source.name)
    count+=1
print('UNCHANGED_RUNTIME_CLASSES_PASS count='+str(count))
PY
python3 - "$crate" "$receiver" "$emitted" "$out" <<'PY'
from pathlib import Path
import hashlib,json,subprocess,sys
crate,receiver,emitted,out=map(Path,sys.argv[1:])
pins=json.loads((crate/'SOURCE_PINS.json').read_text())
main='m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java'
sources=[]
for row in pins['module_sources']:
    path=receiver/row['path']
    actual=hashlib.sha256(path.read_bytes()).hexdigest()
    admitted={row['sha256']}
    if row['path']==main:admitted.add(pins['source_states'][0]['main_after_sha256'])
    if actual not in admitted:raise SystemExit('module source drift: '+row['path'])
    sources.append(str(emitted/'representative'/main) if row['path']==main else str(path))
subprocess.run(['javac','--release','21','-proc:none','-Xlint:all,-module','-Werror',
                '-d',str(out/'whole-module'),*sources],check=True)
print('WHOLE_COLLECTIONS_MODULE_JAVAC_PASS sources='+str(len(sources)))
PY
printf '%s\n' 'WRAPPER_COMPOSITION_VERIFIER_PASS scope=ISOLATED_JAVA21_SDK_JNI'
