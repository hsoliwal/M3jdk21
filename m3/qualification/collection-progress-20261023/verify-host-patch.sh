#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Java21 host patch verification; NOT a modified M3JDK21 image qualification.
set -euo pipefail
root="$(cd "$(dirname "$0")/../../.." && pwd)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/patch/java/lang" "$tmp/patch/java/util/concurrent" "$tmp/patch/java/util" "$tmp/classes" "$tmp/tests"
for n in Iterable Iterator PrimitiveIterator Spliterator Collection List Map; do
  subdir="java/util"
  if [[ "$n" == "Iterable" ]]; then subdir="java/lang"; fi
  cp "$root/src/java.base/share/classes/$subdir/$n.java" "$tmp/patch/$subdir/$n.java"
done
cat > "$tmp/patch/java/util/concurrent/ProgressMonitor.java" <<'JAVA'
// Test-only facade: real monitor belongs to M3JDK21.
package java.util.concurrent;
public interface ProgressMonitor {
    default void worked(long units) { }
    default boolean isCancelled() { return false; }
    default void done() { }
    default void checkCancelled() {
        if (isCancelled() || Thread.currentThread().isInterrupted())
            throw new CancellationException("Operation cancelled");
    }
}
JAVA
javac --source 21 --target 21 --patch-module java.base="$tmp/patch" \
  -Xlint:all,-module -Werror -proc:none -d "$tmp/classes" \
  "$tmp/patch/java/util/concurrent/ProgressMonitor.java" \
  "$tmp/patch/java/lang/Iterable.java" "$tmp/patch/java/util/Iterator.java" \
  "$tmp/patch/java/util/PrimitiveIterator.java" "$tmp/patch/java/util/Spliterator.java" \
  "$tmp/patch/java/util/Collection.java" "$tmp/patch/java/util/List.java" \
  "$tmp/patch/java/util/Map.java"
javac --source 21 --target 21 --patch-module java.base="$tmp/classes" \
  -Xlint:all,-module -Werror -proc:none -d "$tmp/tests" \
  "$root/test/jdk/java/util/MonitoredCollectionDefaults.java"
java --patch-module java.base="$tmp/classes" -cp "$tmp/tests" MonitoredCollectionDefaults
java -Xint --patch-module java.base="$tmp/classes" -cp "$tmp/tests" MonitoredCollectionDefaults
echo 'HOST_PATCH_PROOF_PASS (full modified JDK still HOLD)'
