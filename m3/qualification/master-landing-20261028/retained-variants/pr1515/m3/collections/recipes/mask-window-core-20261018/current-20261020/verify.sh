#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
TASK="$(cd "$(dirname "$0")" && pwd)"
ROOT="${1:?pass a disposable M3JDK21 checkout or bounded source root}"
ROOT="$(cd "$ROOT" && pwd)"
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
SRC="$ROOT/m3/collections/src/main/java/com/m3/collections"
C="$ROOT/m3/collections/src/main/native/collections/m3_index_mask_compactor.c"
RES="$TASK/src/main/resources/current-mask-window"
JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
mkdir -p "$WORK/engine" "$WORK/classes" "$WORK/headers" "$WORK/native" "$WORK/missing"
javac --release 21 -Xlint:all -Werror -d "$WORK/engine" "$TASK"/src/main/java/com/synexia/rewrite/*.java
java -cp "$WORK/engine" com.synexia.rewrite.SealReplay check "$ROOT" "$RES"
java -cp "$WORK/engine" com.synexia.rewrite.SealReplay apply "$ROOT" "$RES"
java -cp "$WORK/engine" com.synexia.rewrite.SealReplay apply "$ROOT" "$RES"
javac --release 21 -Xlint:all -Werror -h "$WORK/headers" -d "$WORK/classes" \
 "$SRC/M3IndexMaskCompactor.java" "$SRC/M3LongAddressKernel.java" "$SRC/M3LongIndexProjection.java" \
 "$ROOT/m3/collections/src/test/java/com/m3/collections/M3MaskWindowCurrentProof.java"
gcc -std=c17 -O2 -Wall -Wextra -Werror -pedantic -fPIC -shared -fsanitize=undefined \
 -fno-sanitize-recover=all -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" "$C" -o "$WORK/native/libm3_collections_mask.so"
java -Xmx128m -cp "$WORK/classes" com.m3.collections.M3MaskWindowCurrentProof java
java -Xmx128m -Xcheck:jni -Djava.library.path="$WORK/native" -cp "$WORK/classes" com.m3.collections.M3MaskWindowCurrentProof native
java -Xmx128m -Djava.library.path="$WORK/missing" -cp "$WORK/classes" com.m3.collections.M3MaskWindowCurrentProof missing
gcc -std=c17 -O2 -Wall -Wextra -Werror -pedantic -fsanitize=undefined -fno-sanitize-recover=all \
 -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" -DOWNER_SOURCE=\"$C\" "$TASK/probes/native_work_probe.c" -o "$WORK/native-work"
"$WORK/native-work" 1
printf '%s\n' 'SELECTED_OWNERS_PASS; MavenSDK/full module/installed JDK/jtreg/cross-platform=NOT_QUALIFIED'
