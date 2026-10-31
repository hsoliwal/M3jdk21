#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
set -euo pipefail
ROOT=$(cd "$(dirname "$0")/../../../.." && pwd)
RECIPE="$ROOT/m3/collections/recipe/state-mask-histogram-20261017"
OUT=$(mktemp -d)
trap 'rm -rf "$OUT"' EXIT
python3 - "$ROOT" "$RECIPE" <<'PY'
import hashlib, pathlib, sys
root = pathlib.Path(sys.argv[1]); recipe = pathlib.Path(sys.argv[2])
for manifest in ('manifest.tsv', 'native-manifest.tsv'):
    for row in (recipe / manifest).read_text().splitlines():
        if not row.strip() or row.startswith('#'): continue
        path, before, digest, template = row.split('\t')
        if before != 'ABSENT': raise SystemExit('unexpected preimage claim: ' + path)
        output = root / path
        mirror = recipe / 'templates' / template
        if not output.is_file() or not mirror.is_file():
            raise SystemExit('missing generated output / sealed mirror: ' + path)
        actual = hashlib.sha256(output.read_bytes()).hexdigest()
        mirror_hash = hashlib.sha256(mirror.read_bytes()).hexdigest()
        if digest != actual or digest != mirror_hash:
            raise SystemExit('generated output/source mirror SHA-256 drift: ' + path)
print('source-sealed Java21/C17 receiver: PASS')
PY
mkdir -p "$OUT/classes" "$OUT/native" "$OUT/headers"
JH=${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}
SRC="$ROOT/m3/collections/src/main/java/com/m3/collections"
TEST="$ROOT/m3/collections/src/test/java/com/m3/collections"
C="$ROOT/m3/collections/src/main/native/collections/m3_state_mask_histogram.c"
javac --release 21 -proc:none -Xlint:all -Werror -h "$OUT/headers" \
  -d "$OUT/classes" "$SRC/M3StateMaskHistogram.java" "$TEST/M3StateMaskHistogramSelfTest.java"
java -Djava.library.path="$OUT/native" -cp "$OUT/classes" \
  com.m3.collections.M3StateMaskHistogramSelfTest
"${CC:-gcc}" -std=c17 -Wall -Wextra -Wpedantic -Werror -Wconversion -Wsign-conversion \
  -I"$JH/include" -I"$JH/include/linux" -I"$OUT/headers" \
  -fPIC -shared -fsanitize=undefined -fno-sanitize-recover=all "$C" \
  -o "$OUT/native/libm3_collections_statehist.so"
java -Xcheck:jni -Djava.library.path="$OUT/native" -cp "$OUT/classes" \
  com.m3.collections.M3StateMaskHistogramSelfTest
printf '%s\n' 'BOUNDED_JAVA21_C17_JNI_PASS; owning module/JDK/Maven qualification separate'
