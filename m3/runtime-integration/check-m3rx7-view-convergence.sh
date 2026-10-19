#!/usr/bin/env bash
# Copyright (c) 2026, Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
# Target proof mirror. Canonical task: Synexia PR 12446 at b8ee85a73166407e0d82a6bdf51cfaad7698f4ed.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd -P)"
: "${M3_JAVA_HOME:?set M3_JAVA_HOME to a Java 21 JDK}"
: "${M3_EXPECTED_COMMIT:?set M3_EXPECTED_COMMIT to the receiver commit published on this PR}"
[[ "$M3_EXPECTED_COMMIT" =~ ^[0-9a-f]{40}$ ]] || { echo 'A full pinned commit is required.' >&2; exit 2; }
[[ "$(git -C "$ROOT" rev-parse HEAD)" == "$M3_EXPECTED_COMMIT" ]] || { echo 'Exact receiver checkout required.' >&2; exit 2; }
[[ "$(uname -s)" == Linux ]] || { echo 'Host script qualified on Linux only.' >&2; exit 2; }
export JAVA_HOME="$M3_JAVA_HOME" PATH="$M3_JAVA_HOME/bin:$PATH"
CC="${CC:-cc}"
WORK="$(mktemp -d "${TMPDIR:-/tmp}/m3-jdk-views.XXXXXX")"
trap 'rm -rf -- "$WORK"' EXIT
COLL_REL=m3/collections/src/main/java/com/m3/collections
PROOF_REL=m3/collections/qualification/jdk-rx7-view-convergence-20261019/proof
INPUTS=(
  src/java.base/share/classes/java/util/SortedSet.java
  src/java.base/share/classes/java/util/ReverseOrderSortedSetView.java
  src/java.base/share/classes/jdk/internal/mindex/M3RX7.java
  src/java.base/share/classes/jdk/internal/mindex/M3RX7Transitions.java
  src/java.base/share/native/libjava/M3RX7.c
  "$COLL_REL/M3FilteredViews.java" "$COLL_REL/M3FilteredNavigableSet.java"
  "$COLL_REL/M3IndexMaskCompactor.java" "$COLL_REL/M3IndexMaskWindow.java"
  m3/collections/src/main/native/collections/m3_index_mask_window.c
  test/jdk/java/util/SortedSet/SortedSetSplitOrder.java
  test/jdk/jdk/internal/mindex/RX7FindConvergence.java
  test/jdk/jdk/internal/mindex/M3RX7ArrayPowerContract.java
  "$PROOF_REL/FilteredOwnerIntegration.java" "$PROOF_REL/MaskWindowConvergence.java"
  "$PROOF_REL/MaskWindowWork.c" "$PROOF_REL/ConvergenceCostProbe.java"
  m3/runtime-integration/check-m3rx7-view-convergence.sh
  m3/runtime-integration/evidence/jdk-rx7-view-convergence-20261019/PRODUCTION.sha256
)
for path in "${INPUTS[@]}"; do
  at="$ROOT/$path"
  while [[ "$at" != "$ROOT" ]]; do
    [[ ! -L "$at" ]] || { echo "Source symlink refused: $at" >&2; exit 2; }
    at="$(dirname -- "$at")"
  done
  git -C "$ROOT" cat-file blob "$M3_EXPECTED_COMMIT:$path" > "$WORK/expected"
  cmp -- "$ROOT/$path" "$WORK/expected"
done
(cd "$ROOT" && sha256sum -c m3/runtime-integration/evidence/jdk-rx7-view-convergence-20261019/PRODUCTION.sha256 && sha256sum "${INPUTS[@]}")
java -version; "$CC" --version
mkdir -p "$WORK/source/java/util" "$WORK/source/jdk/internal/mindex" "$WORK/patch" "$WORK/tests" "$WORK/headers" "$WORK/native" "$WORK/missing"
for name in SortedSet ReverseOrderSortedSetView; do cp "$ROOT/src/java.base/share/classes/java/util/$name.java" "$WORK/source/java/util/"; done
for name in M3RX7 M3RX7Transitions; do cp "$ROOT/src/java.base/share/classes/jdk/internal/mindex/$name.java" "$WORK/source/jdk/internal/mindex/"; done
javac -Xlint:all -Werror --patch-module "java.base=$WORK/source" -h "$WORK/headers" -d "$WORK/patch" "$WORK/source/java/util/"*.java "$WORK/source/jdk/internal/mindex/"*.java
COLL="$ROOT/$COLL_REL"
PROOF="$ROOT/$PROOF_REL"
javac -Xlint:all -Werror -h "$WORK/headers" -d "$WORK/tests" "$COLL/M3FilteredViews.java" "$COLL/M3FilteredNavigableSet.java" "$COLL/M3IndexMaskCompactor.java" "$COLL/M3IndexMaskWindow.java" "$ROOT/test/jdk/java/util/SortedSet/SortedSetSplitOrder.java" "$PROOF/FilteredOwnerIntegration.java" "$PROOF/MaskWindowConvergence.java"
javac -Xlint:all -Werror --patch-module "java.base=$WORK/patch" --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -d "$WORK/tests" "$ROOT/test/jdk/jdk/internal/mindex/RX7FindConvergence.java"
JAVA_ARGS=(-Xmx256m -Xcheck:jni --patch-module "java.base=$WORK/patch" --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -cp "$WORK/tests")
java "${JAVA_ARGS[@]}" SortedSetSplitOrder
java "${JAVA_ARGS[@]}" FilteredOwnerIntegration
java "${JAVA_ARGS[@]}" RX7FindConvergence
java -Xint "${JAVA_ARGS[@]}" RX7FindConvergence
MASK="$ROOT/m3/collections/src/main/native/collections/m3_index_mask_window.c"
"$CC" -std=c17 -O2 -Wall -Wextra -Werror -fsanitize=undefined -fno-sanitize-recover=all -fPIC -shared -I"$M3_JAVA_HOME/include" -I"$M3_JAVA_HOME/include/linux" -include "$WORK/headers/com_m3_collections_M3IndexMaskWindow.h" "$MASK" -o "$WORK/native/libm3_collections_mask.so"
java "${JAVA_ARGS[@]}" -Djava.library.path="$WORK/native" MaskWindowConvergence
java "${JAVA_ARGS[@]}" -Djava.library.path="$WORK/missing" MaskWindowConvergence missing
"$CC" -std=c17 -O2 -Wall -Wextra -Werror -fsanitize=undefined -fno-sanitize-recover=all -I"$M3_JAVA_HOME/include" -I"$M3_JAVA_HOME/include/linux" "-DM3_WINDOW_SOURCE=\"$MASK\"" "$PROOF/MaskWindowWork.c" -o "$WORK/window-work"
"$WORK/window-work" candidate
cp "$ROOT/test/jdk/jdk/internal/mindex/M3RX7ArrayPowerContract.java" "$WORK/source/jdk/internal/mindex/"
javac -Xlint:all -Werror --patch-module "java.base=$WORK/patch:$WORK/source" -d "$WORK/patch" "$WORK/source/jdk/internal/mindex/M3RX7ArrayPowerContract.java"
"$CC" -std=c17 -O2 -Wall -Wextra -Werror -fPIC -shared -I"$M3_JAVA_HOME/include" -I"$M3_JAVA_HOME/include/linux" -I"$WORK/headers" "$ROOT/src/java.base/share/native/libjava/M3RX7.c" -o "$WORK/native/libm3rx7.so"
java "${JAVA_ARGS[@]}" jdk.internal.mindex.M3RX7ArrayPowerContract "$WORK/native/libm3rx7.so"
printf '%s\n' 'JDK_SORTED_RX7_MASK_HOST_PASS; actual SDK/reactor/source-built JDK/jtreg/production linkage/platform/pressure gates NOT_RUN'
