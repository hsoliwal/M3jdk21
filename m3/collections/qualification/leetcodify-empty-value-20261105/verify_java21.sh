#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Full-owner Java21 proof for the reviewed empty-value search receiver.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
cd "$ROOT"
SRC="m3/collections/src/main/java/com/m3/collections"
TEST="m3/collections/src/test/java/com/m3/collections"
TARGET="m3/collections/target/m3-empty-value-20261105"
PRE="134508fdd9857110ca21d0042353805586db3ea0"
POST="6cd936c4d0d0971b9834049785f1852107eb6523"
PRE_SHA="2f228e876a5b57700ecb513bc19775735774858017b6c15fcad7b5ea44469e27"
POST_SHA="844924463c460f6b379424cbc4864d738d69229b84d149db3d5066c57375fc7c"
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
mkdir -p "$TARGET/baseline/src/com/m3/collections" \
         "$TARGET/baseline/classes" "$TARGET/candidate/classes" "$TARGET/logs"
git cat-file blob "$PRE" > "$TARGET/baseline/src/com/m3/collections/M3LongLongHashMap.java"
[[ "$(git hash-object "$TARGET/baseline/src/com/m3/collections/M3LongLongHashMap.java")" == "$PRE" ]] || {
    echo "REFUSE historical Java source identity" >&2
    exit 6
}
printf '%s  %s\n' "$PRE_SHA" "$TARGET/baseline/src/com/m3/collections/M3LongLongHashMap.java" |
    sha256sum --check -

# Stage ordering: source seal -> strict lint/compile -> descriptor diff -> test/runtime.
for IMAGE in baseline candidate; do
    if [[ "$IMAGE" == baseline ]]; then
        FILE="$TARGET/baseline/src/com/m3/collections/M3LongLongHashMap.java"
    else
        FILE="$SRC/M3LongLongHashMap.java"
    fi
    javac --release 21 -Xlint:all -Werror -d "$TARGET/$IMAGE/classes" \
        "$FILE" "$SRC/M3LongIterator.java" "$SRC/M3LongLongConsumer.java" \
        "$TEST/M3LongLongHashMapEmptyValueProof.java" \
        > "$TARGET/logs/01-$IMAGE-javac.log" 2>&1
    javap -classpath "$TARGET/$IMAGE/classes" -public -s \
        com.m3.collections.M3LongLongHashMap > "$TARGET/logs/02-$IMAGE-public-api.log"
done
cmp "$TARGET/logs/02-baseline-public-api.log" "$TARGET/logs/02-candidate-public-api.log"

for MODE in default interpreted; do
    ARGS=()
    if [[ "$MODE" == interpreted ]]; then ARGS=(-Xint); fi
    for IMAGE in baseline candidate; do
        java -ea "${ARGS[@]}" -cp "$TARGET/$IMAGE/classes" \
             com.m3.collections.M3LongLongHashMapEmptyValueProof \
             > "$TARGET/logs/03-$IMAGE-$MODE.log" 2>&1
    done
    cmp "$TARGET/logs/03-baseline-$MODE.log" "$TARGET/logs/03-candidate-$MODE.log"
    cat "$TARGET/logs/03-candidate-$MODE.log"
done
sha256sum "$TARGET/logs/"*.log > "$TARGET/logs/SHA256SUMS.tsv"
echo "M3_EMPTY_VALUE_SOURCE_SEALED_JAVA21_PASS|descriptor=IDENTICAL|differential=PASS"
