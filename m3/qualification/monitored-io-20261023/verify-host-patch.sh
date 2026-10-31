#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# This host-JDK patch proof does not qualify the full native M3JDK21.
set -euo pipefail
root="$(cd "$(dirname "$0")/../../.." && pwd)"
scratch="$(mktemp -d)"
trap 'rm -rf "$scratch"' EXIT
mkdir -p "$scratch/patch/java/io" "$scratch/patch/java/util/concurrent" "$scratch/classes" "$scratch/tests"
for name in InputStream OutputStream Reader Writer; do
  cp "$root/src/java.base/share/classes/java/io/${name}.java" "$scratch/patch/java/io/${name}.java"
done
cat > "$scratch/patch/java/util/concurrent/ProgressMonitor.java" <<'JAVA'
// Synthetic test fixture. M3JDK21 retains the real original progress API.
package java.util.concurrent;
public interface ProgressMonitor {
    default void worked(long units) { }
    default boolean isCancelled() { return false; }
    default void checkCancelled() {
        if (isCancelled() || Thread.currentThread().isInterrupted())
            throw new CancellationException("Operation cancelled");
    }
}
JAVA
cp "$root/test/jdk/java/io/MonitoredIoOperations.java" "$scratch/MonitoredIoOperations.java"
javac --source 21 --target 21 --patch-module java.base="$scratch/patch" \
  -Xlint:all,-module -Werror -proc:none -d "$scratch/classes" \
  "$scratch/patch/java/util/concurrent/ProgressMonitor.java" \
  "$scratch/patch/java/io/InputStream.java" "$scratch/patch/java/io/OutputStream.java" \
  "$scratch/patch/java/io/Reader.java" "$scratch/patch/java/io/Writer.java"
javac --source 21 --target 21 --patch-module java.base="$scratch/classes" \
  -Xlint:all,-module -Werror -proc:none -d "$scratch/tests" "$scratch/MonitoredIoOperations.java"
java --patch-module java.base="$scratch/classes" -cp "$scratch/tests" MonitoredIoOperations
java -Xint --patch-module java.base="$scratch/classes" -cp "$scratch/tests" MonitoredIoOperations
echo "HOST_PATCH_PROOF_PASS (not source-built M3JDK21)"
