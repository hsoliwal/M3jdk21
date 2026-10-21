#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
task=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
root=$(cd -- "$task/../../.." && pwd)
mode="${1:---check}"
case "$mode" in --plan|--apply|--check) ;; *) exit 2 ;; esac
out="$task/target/replay-classes"
mkdir -p -- "$out"
javac --release 21 -Xlint:all -Werror -d "$out" \
  "$root/synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3GpuToolJoinManifest.java" \
  "$root/synexia-openrewrite-recipes/tasks/recipe-qualification/src/main/java/com/synexia/rewrite/Replay.java"
java -cp "$out" com.synexia.rewrite.Replay "$mode" "$root" "$task/templates"
