#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail

# Reproducible local source-fixture qualification. It does not replace a full JDK build.
: "${JAVA_HOME:?JAVA_HOME must name a JDK 21 or later installation}"
batch_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
target_root="$batch_root/templates"
crate_root=""
proof_out="$batch_root/qualification"
while [[ $# -gt 0 ]]; do
    case "$1" in
        --target) target_root=${2:?--target needs a path}; shift 2 ;;
        --crate) crate_root=${2:?--crate needs a path}; shift 2 ;;
        --output) proof_out=${2:?--output needs a path}; shift 2 ;;
        *) echo "usage: qualify.sh [--target GENERATED_TARGET] [--crate CANONICAL_CRATE] [--output PROOF_DIR]" >&2; exit 2 ;;
    esac
done
target_root=$(cd -- "$target_root" && pwd)
if [[ -n "$crate_root" ]]; then crate_root=$(cd -- "$crate_root" && pwd); fi
mkdir -p -- "$proof_out/classes" "$proof_out/headers" "$proof_out/tests" "$proof_out/source/jdk/internal/mindex"
proof_out=$(cd -- "$proof_out" && pwd)
owner_java="$target_root/src/java.base/share/classes/jdk/internal/mindex/M3RegexDpPlan.java"
owner_c="$target_root/src/java.base/share/native/libjava/M3RegexDpPlan.c"
test_java="$target_root/test/jdk/java/lang/String/M3StringRegexPreparedBatchTest.java"
for source in "$owner_java" "$owner_c" "$test_java"; do
    [[ -f "$source" ]] || { echo "missing generated target source: $source" >&2; exit 2; }
done
printf '%s\n' "target=$target_root" "crate=$crate_root" > "$proof_out/invocation.txt"
sha256sum "$owner_java" "$owner_c" "$test_java" > "$proof_out/payload-sha256.before"

# Compile the exact generated owner in isolation. Pointing javac at an entire target
# java.base source tree can pull unrelated JDK classes into this component-level gate.
cp -- "$owner_java" "$proof_out/source/jdk/internal/mindex/M3RegexDpPlan.java"
cmp -s "$owner_java" "$proof_out/source/jdk/internal/mindex/M3RegexDpPlan.java"

"$JAVA_HOME/bin/javac" -h "$proof_out/headers" \
    --patch-module "java.base=$proof_out/source" \
    -d "$proof_out/classes" "$proof_out/source/jdk/internal/mindex/M3RegexDpPlan.java"
"$JAVA_HOME/bin/javac" --patch-module "java.base=$proof_out/classes:$batch_root/test-support" \
    -d "$proof_out/classes" \
    "$batch_root/test-support/jdk/internal/mindex/M3RegexPreparedBatchTestSupport.java"
"$JAVA_HOME/bin/javac" --patch-module "java.base=$proof_out/classes" \
    --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED \
    -d "$proof_out/tests" "$test_java" "$batch_root/test-support/M3RegexPreparedBatchAllocationProbe.java"

cflags=(-std=c17 -Wall -Wextra -Werror -fPIC -I "$JAVA_HOME/include"
    -I "$JAVA_HOME/include/linux" -I "$proof_out/headers")
"${CC:-cc}" "${cflags[@]}" -shared "$owner_c" -o "$proof_out/libm3regexbatch.so"
"${CC:-cc}" "${cflags[@]}" -DM3_REGEX_PORTABLE=1 -shared "$owner_c" \
    -o "$proof_out/libm3regexbatch-portable.so"
"${CC:-cc}" "${cflags[@]}" -fsanitize=undefined -fno-sanitize-recover=all -shared "$owner_c" \
    -o "$proof_out/libm3regexbatch-ubsan.so"
"${CC:-cc}" "${cflags[@]}" "-DM3_REGEX_BATCH_OWNER_C=\"$owner_c\"" \
    -shared "$batch_root/test-support/native-allocation-probe.c" \
    -o "$proof_out/libm3regexbatch-allocation.so"
"${CC:-cc}" "${cflags[@]}" -DM3_REGEX_CUDA_PROVIDER=1 -I "$batch_root/optional-cuda" \
    -c "$owner_c" -o "$proof_out/m3regexcuda-jni.o"

java_args=(-Xcheck:jni --patch-module "java.base=$proof_out/classes"
    --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED
    --add-opens java.base/jdk.internal.mindex=ALL-UNNAMED -cp "$proof_out/tests")
"$JAVA_HOME/bin/java" "${java_args[@]}" M3StringRegexPreparedBatchTest java-only \
    | tee "$proof_out/java-fallback.log"
"$JAVA_HOME/bin/java" "${java_args[@]}" \
    "-Dm3.regex.batch.test.library=$proof_out/libm3regexbatch.so" M3StringRegexPreparedBatchTest \
    | tee "$proof_out/jni.log"
"$JAVA_HOME/bin/java" -Xint "${java_args[@]}" \
    "-Dm3.regex.batch.test.library=$proof_out/libm3regexbatch-portable.so" M3StringRegexPreparedBatchTest \
    | tee "$proof_out/portable-interpreted.log"
"$JAVA_HOME/bin/java" "${java_args[@]}" \
    "-Dm3.regex.batch.test.library=$proof_out/libm3regexbatch-ubsan.so" M3StringRegexPreparedBatchTest \
    | tee "$proof_out/ubsan.log"
"$JAVA_HOME/bin/java" "${java_args[@]}" M3RegexPreparedBatchAllocationProbe \
    "$proof_out/libm3regexbatch-allocation.so" | tee "$proof_out/allocation.log"
sha256sum "$owner_java" "$owner_c" "$test_java" > "$proof_out/payload-sha256.txt"
cmp -s "$proof_out/payload-sha256.before" "$proof_out/payload-sha256.txt" || {
    echo "source changed during prepared regex qualification" >&2
    exit 1
}
echo "PREPARED_REGEX_CPU_QUALIFICATION_PASS; CUDA_KERNEL_AND_DEVICE_GATE_NOT_RUN"
