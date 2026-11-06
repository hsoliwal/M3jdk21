#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Full-owner Java21 proof for the reviewed empty-value search receiver.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
cd "$ROOT"
SRC="m3/collections/src/main/java/jdk/m3/collections"
TEST="m3/collections/src/test/java/jdk/m3/collections"
TARGET="m3/collections/target/m3-empty-value-20261105"
PRE="9a84487f7a66257ce5e42e2e31698068496f49d9"
POST="4ebceb9bf2f66a15c1ebc931169af8a5913fc6f5"
PRE_SHA="1758a7688eb0d5b47e4b7ab06074a77042519d42a2d4ba976d3d330f108e969b"
POST_SHA="11b444a76a3e024e6a4f4bddbd3be30b440b5e46d29be8b680252f93ef0e041c"
if [[ -e "$TARGET" ]]; then
    echo "REFUSE existing evidence target: $TARGET" >&2
    exit 3
fi
[[ "$(git hash-object "$SRC/M3LongLongHashMap.java")" == "$POST" ]] || {
    echo "REFUSE current Java owner Git identity drift" >&2
    exit 4
}
printf '%s  %s\n' "$POST_SHA" "$SRC/M3LongLongHashMap.java" | sha256sum --check -
git cat-file -e "$PRE^{blob}" || {
    echo "REFUSE missing exact original Git blob" >&2
    exit 5
}
mkdir -p "$TARGET/baseline/src/jdk/m3/collections" \
         "$TARGET/baseline/classes" "$TARGET/candidate/classes" "$TARGET/logs"
git cat-file blob "$PRE" > "$TARGET/baseline/src/jdk/m3/collections/M3LongLongHashMap.java"
[[ "$(git hash-object "$TARGET/baseline/src/jdk/m3/collections/M3LongLongHashMap.java")" == "$PRE" ]] || {
    echo "REFUSE historical Java source identity" >&2
    exit 6
}
printf '%s  %s\n' "$PRE_SHA" "$TARGET/baseline/src/jdk/m3/collections/M3LongLongHashMap.java" |
    sha256sum --check -

# Stage ordering: source seal -> strict lint/compile -> descriptor diff -> test/runtime.
for IMAGE in baseline candidate; do
    if [[ "$IMAGE" == baseline ]]; then
        FILE="$TARGET/baseline/src/jdk/m3/collections/M3LongLongHashMap.java"
    else
        FILE="$SRC/M3LongLongHashMap.java"
    fi
    javac --release 21 -Xlint:all -Werror -d "$TARGET/$IMAGE/classes" \
        "$FILE" "$SRC/M3LongIterator.java" "$SRC/M3LongLongConsumer.java" \
        "$TEST/M3LongLongHashMapEmptyValueProof.java" \
        > "$TARGET/logs/01-$IMAGE-javac.log" 2>&1
    javap -classpath "$TARGET/$IMAGE/classes" -public -s \
        jdk.m3.collections.M3LongLongHashMap > "$TARGET/logs/02-$IMAGE-public-api.log"
done
cmp "$TARGET/logs/02-baseline-public-api.log" "$TARGET/logs/02-candidate-public-api.log"

for MODE in default interpreted; do
    ARGS=()
    if [[ "$MODE" == interpreted ]]; then ARGS=(-Xint); fi
    for IMAGE in baseline candidate; do
        java -ea "${ARGS[@]}" -cp "$TARGET/$IMAGE/classes" \
             jdk.m3.collections.M3LongLongHashMapEmptyValueProof \
             > "$TARGET/logs/03-$IMAGE-$MODE.log" 2>&1
    done
    cmp "$TARGET/logs/03-baseline-$MODE.log" "$TARGET/logs/03-candidate-$MODE.log"
    cat "$TARGET/logs/03-candidate-$MODE.log"
done
sha256sum "$TARGET/logs/"*.log > "$TARGET/logs/SHA256SUMS.tsv"
echo "M3_EMPTY_VALUE_SOURCE_SEALED_JAVA21_PASS|descriptor=IDENTICAL|differential=PASS"
