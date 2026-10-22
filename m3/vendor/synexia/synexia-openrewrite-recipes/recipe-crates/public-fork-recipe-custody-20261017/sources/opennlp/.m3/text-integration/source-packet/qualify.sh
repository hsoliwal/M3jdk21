#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
root="$(cd "${1:?repository root}" && pwd)"
mode="${2:-verify}"
packet="${3:-$root/.m3/text-integration}"
module="$root/.m3/text-integration"
out="$module/target/qualification"
mkdir -p "$out/classes" "$out/native-classes" "$out/headers" "$out/logs"
exec > >(tee "$out/logs/qualification.log") 2>&1
if [[ -n "${JAVA_HOME:-}" ]]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
javac_bin="$(readlink -f "$(command -v javac)")"
java_home="$(dirname "$(dirname "$javac_bin")")"
manifest="$packet/manifest-owner/M3GpuToolJoinManifest.java"
if [[ ! -f "$manifest" ]]; then manifest="$root/synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3GpuToolJoinManifest.java"; fi
javac --release 21 -d "$out/classes" "$manifest" \
  "$packet/src/main/java/com/synexia/rewrite/M3TextRecipe.java" \
  "$packet/src/test/java/com/synexia/rewrite/Qualification.java"
case "$mode" in
 apply) java -cp "$out/classes" com.synexia.rewrite.M3TextRecipe apply "$root" ;;
 verify) ;;
 *) echo "unknown mode: $mode" >&2; exit 2 ;;
esac
java -cp "$out/classes" com.synexia.rewrite.Qualification "$root"
# Product builds remain authoritative. This bounded gate compiles only the admitted native atom.
if [[ -f "$root/synexia-indexstring/src/main/java/com/synexia/indexstring/IndexTextRangeHashes.java" ]]; then
  javac --release 21 -h "$out/headers" -d "$out/native-classes" \
    "$root/synexia-common/src/main/java/com/synexia/common/progress/ProgressMonitor.java" \
    "$root/synexia-common/src/main/java/com/synexia/common/progress/ProgressState.java" \
    "$root/synexia-common/src/main/java/com/synexia/common/progress/ChildProgressMonitor.java" \
    "$root/synexia-indexstring/src/main/java/com/synexia/indexstring/IndexTextRangeHashes.java" \
    "$packet/src/test/java/com/synexia/indexstring/RangeHashesQualification.java"
  cc -std=c17 -Wall -Wextra -Werror -fPIC -shared \
    -I"$java_home/include" -I"$java_home/include/linux" \
    -include "$out/headers/com_synexia_indexstring_IndexTextRangeHashes_Native.h" \
    "$root/synexia-indexstring/src/main/native/text_metrics_jni.c" -o "$out/libm3_text_qualification.so"
  java -Xcheck:jni -cp "$out/native-classes" com.synexia.indexstring.RangeHashesQualification
  java -Xcheck:jni -cp "$out/native-classes" com.synexia.indexstring.RangeHashesQualification "$out/libm3_text_qualification.so"
fi
java -version
printf '%s\n' 'PASS bounded text packet; full reactors/OpenRewrite scheduler/M3JDK product gates are separate.'
