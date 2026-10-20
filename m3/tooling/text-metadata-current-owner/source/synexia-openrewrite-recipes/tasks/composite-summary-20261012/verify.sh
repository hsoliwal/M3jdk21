#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
task=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
root=$(cd -- "$task/../../.." && pwd)
out="$task/target/proof"
mkdir -p "$out/classes" "$out/headers"
bash "$task/recipe.sh" --check
python3 "$task/verify-recipe.py"
javac --release 21 -Xlint:all -Werror -h "$out/headers" -d "$out/classes" \
  "$root/synexia-mindex/runtime/src/main/java/com/synexia/mindex/TextMetadata.java" \
  "$task/proof/TextMetadataProof.java"
java -Xcheck:jni -cp "$out/classes" com.synexia.mindex.TextMetadataProof
if [[ ${1:-} == --jni ]]; then
  [[ $(uname -s) == Linux ]] || { echo 'focused JNI proof currently requires Linux' >&2; exit 2; }
  jdk=${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")}
  "${CC:-gcc}" -std=c11 -Wall -Wextra -Wpedantic -Werror -Wconversion -Wsign-conversion \
    -fPIC -shared -fsanitize=undefined -fno-sanitize-recover=all \
    -I"$jdk/include" -I"$jdk/include/linux" -I"$out/headers" \
    -include com_synexia_mindex_TextMetadata.h \
    "$root/synexia-mindex/native-jni/src/mindex_text_metadata_jni.c" \
    -o "$out/libm3_summary_proof.so"
  java -Xcheck:jni -cp "$out/classes" com.synexia.mindex.TextMetadataProof "$out/libm3_summary_proof.so"
elif [[ $# != 0 ]]; then exit 2; fi
