#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
# Thin receiving gate: no source mutation or alternative recipe implementation.
set -euo pipefail
target=$(cd "${1:?target checkout required}" && pwd)
canonical=$(cd "${2:?canonical Synexia checkout required}" && pwd)
out=${3:?new absolute output directory required}
case "$out" in /*) ;; *) echo 'absolute output required' >&2; exit 2;; esac
test ! -e "$out" || { echo 'output must be absent' >&2; exit 2; }
task=synexia-openrewrite-recipes/recipe-crates/collection-view-range-20261016/batch-admission-20261017
pin=b095e3c944d66a70d21c267e78ed0db197526a62
test "$(git -C "$canonical" rev-parse HEAD)" = "$pin"
test "$(git -C "$canonical" rev-parse "HEAD:$task")" = f84ca9c65a60be620b9fb1cc5ae251d1a91264d4
git -C "$canonical" diff --quiet HEAD -- "$task"
git -C "$target" merge-base --is-ancestor cb501d5447c659606f4a5d0910379abaa0c36739 HEAD
mkdir -p "$out"
crate=$canonical/$task
extra=()
if [[ ${M3_OFFLINE:-false} == true ]]; then extra+=(-o); fi
if [[ -n ${M3_MAVEN_REPOSITORY:-} ]]; then extra+=("-Dmaven.repo.local=$M3_MAVEN_REPOSITORY"); fi
"${MAVEN:-mvn}" -B -ntp "${extra[@]}" -f "$crate/pom.xml" -Pcaptured-sdk "-Dm3.batch.output=$out/emitted" test | tee "$out/sdk.log"
M3_BATCH_EMITTED="$out/emitted" bash "$crate/verify.sh" "$out/runtime" | tee "$out/runtime.log"
python3 - "$target" "$crate" "$out/emitted" <<'PY'
import hashlib,sys
from pathlib import Path
target,crate,emitted=map(Path,sys.argv[1:])
resources=crate/'resources/com/synexia/rewrite/hash-pinned-java/filtered-batch-admission-20261017'
count=0
for row in (resources/'manifest.tsv').read_text().splitlines():
    path,before,after,template=row.split('\t')
    installed=(target/path).read_bytes()
    if hashlib.sha256(installed).hexdigest()!=after:
        raise SystemExit('receiving source drift: '+path)
    if installed!=(emitted/path).read_bytes() or installed!=(resources/template).read_bytes():
        raise SystemExit('SDK/resource/receiver mismatch: '+path)
    count+=1
if count!=2:raise SystemExit('unexpected target set')
print('FILTERED_BATCH_SDK_RECEIVER_EQUAL paths=2')
PY
git -C "$canonical" diff --quiet HEAD -- "$task"
printf '%s\n' 'FILTERED_BATCH_RECEIVER_PASS scope=ISOLATED_JAVA21_SDK_JNI'
