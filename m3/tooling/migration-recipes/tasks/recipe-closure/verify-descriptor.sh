#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Reproducible layout-independent source proof, NOT a full modified JDK build.
set -euo pipefail
if [[ $# -ne 3 ]]; then
  echo "usage: bash verify.sh JDK21_HOME REPOSITORY FRESH_OUTPUT" >&2
  exit 64
fi
jdk=$(realpath "$1")
repo=$(realpath "$2")
out=$(realpath -m "$3")
for tool in java javac javap; do test -x "$jdk/bin/$tool"; done
version=$("$jdk/bin/javac" -version 2>&1)
grep -Eq '^javac 21([.[:space:]]|$)' <<< "$version" || { echo "JDK21 required: $version" >&2; exit 65; }
[[ ! -e "$out" && ! -L "$out" ]] || { echo "output must be absent" >&2; exit 65; }
mkdir -p "$out"
exec > >(tee "$out/verify.log") 2>&1
owner=src/java.base/share/classes/jdk/internal/mindex/M3Descriptor.java
test_source=test/jdk/jdk/internal/mindex/descriptor/com/m3/descriptor/DescriptorTest.java
crate=m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-descriptor
"$jdk/bin/java" -version
printf 'SOURCE_SCOPE=layout-independent-two-source-export\n'
printf 'JDK_HOME=%s\nREPOSITORY=%s\n' "$jdk" "$repo"

diff -U0 "$repo/$crate/before-00-Descriptor.java.txt" "$repo/$owner" > "$out/product.diff" || [[ $? == 1 ]]

# Exact-source admission plus scoped static whitespace check precedes compilation.
python3 - "$repo" "$crate" "$owner" "$test_source" <<'PY'
import hashlib
from pathlib import Path
import sys
root,crate,owner,test=sys.argv[1:]
root=Path(root)
base=root/crate
rows=[line.split('\t') for line in (base/'manifest.tsv').read_text().splitlines()]
assert len(rows)==2 and {row[0] for row in rows}=={owner,test}, 'target set drift'
sha=lambda b:hashlib.sha256(b).hexdigest()
for path,before,after,template in rows:
    target=root/path
    assert not any(p.is_symlink() for p in [target,*target.parents]), 'symlink target'
    data=target.read_bytes()
    assert sha(data)==after, f'product drift: {path}'
    assert (base/template).read_bytes()==data, f'template drift: {path}'
    text=data.decode('utf-8',errors='strict')
    assert text.endswith('\n'), f'missing final LF: {path}'
    assert all(line==line.rstrip() for line in text.splitlines()), f'whitespace: {path}'
    if before!='ABSENT':
        assert sha((base/('before-'+template)).read_bytes())==before, 'preimage drift'
print('exact source / template seals / static whitespace PASS')
PY

mkdir -p "$out"/{baseline-src,baseline,product,tests}
cp "$repo/$crate/before-00-Descriptor.java.txt" "$out/baseline-src/M3Descriptor.java"
"$jdk/bin/javac" --release 21 -proc:none -Xlint:all -Werror \
  -d "$out/baseline" "$out/baseline-src/M3Descriptor.java"
"$jdk/bin/javac" --release 21 -proc:none -Xlint:all -Werror \
  -d "$out/product" "$repo/$owner"
"$jdk/bin/javac" -source 21 -target 21 -proc:none -Xlint:all -Werror \
  --patch-module java.base="$out/product" \
  --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED \
  -d "$out/tests" "$repo/$test_source"
printf 'strict Java21 source compile PASS\n'

"$jdk/bin/java" -Xmx256m --patch-module java.base="$out/baseline" \
  --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED \
  -cp "$out/tests" com.m3.descriptor.DescriptorTest nominal | tee "$out/baseline-nominal.log"
set +e
"$jdk/bin/java" -Xmx256m --patch-module java.base="$out/baseline" \
  --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED \
  -cp "$out/tests" com.m3.descriptor.DescriptorTest > "$out/baseline-red.log" 2>&1
status=$?
set -e
printf '%s\n' "$status" > "$out/baseline-red.exit"
[[ $status -ne 0 ]]
grep -q 'hidden descriptor expected=' "$out/baseline-red.log"
printf 'hidden-descriptor defect reproduced on exact baseline\n'

for mode in normal interpreter noncompact c2; do
  case "$mode" in
    normal) flags=();;
    interpreter) flags=(-Xint);;
    noncompact) flags=(-XX:-CompactStrings);;
    c2) flags=(-Xcomp -XX:-TieredCompilation '-XX:CompileCommand=compileonly,jdk.internal.mindex.M3Descriptor::*');;
  esac
  "$jdk/bin/java" -Xmx256m "${flags[@]}" --patch-module java.base="$out/product" \
    --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED \
    -cp "$out/tests" com.m3.descriptor.DescriptorTest | tee "$out/$mode.log"
done
"$jdk/bin/javap" -protected -s "$out/baseline/jdk/internal/mindex/M3Descriptor.class" > "$out/api-before.txt"
"$jdk/bin/javap" -protected -s "$out/product/jdk/internal/mindex/M3Descriptor.class" > "$out/api-after.txt"
diff -u "$out/api-before.txt" "$out/api-after.txt" > "$out/api.diff"
printf 'public/protected descriptor surface PASS\n'
printf 'RESULT=LOCAL_SOURCE_PROOF_PASS\nFULL_JDK=UNEXECUTED\nOPENREWRITE_JUNIT=SEPARATE_REQUIRED_REPORT\n'
