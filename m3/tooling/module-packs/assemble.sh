#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
[[ $# == 3 ]] || { echo 'usage: assemble.sh LOCK.tsv ROOTS_CSV NEW_TARGET_DIRECTORY' >&2; exit 2; }
module="$(cd "$(dirname "$0")" && pwd)"
jdk="${M3_JDK:-${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")}}"
mkdir -p "$module/target/pack-classes"
output="$(realpath -m "$3")"
case "$output" in "$module/target/"*) ;; *) echo 'Output must be below this module target/ namespace' >&2; exit 2;; esac
[[ ! -e "$output" && ! -L "$output" ]] || { echo 'Output already exists; refusing overwrite' >&2; exit 2; }
mkdir "$output"
trap 'code=$?; if [[ $code != 0 ]]; then printf "gate\tstatus\nassembly\tFAILED_EXIT_%s\n" "$code" > "$output/STATUS.tsv"; fi' EXIT
"$jdk/bin/javac" --release 21 -Xlint:all -Werror -d "$module/target/pack-classes" "$module"/src/main/java/com/m3/pack/*.java
cli=("$jdk/bin/java" -cp "$module/target/pack-classes" com.m3.pack.M3PackTool)
"${cli[@]}" stage "$1" "$output/inputs" > "$output/staging.log"
"${cli[@]}" verify "$output/inputs/LOCK.tsv" "$2" > "$output/INVENTORY.tsv"
"$jdk/bin/jlink" --module-path "$jdk/jmods:$output/inputs" --add-modules "$2" --output "$output/image" > "$output/jlink.log" 2>&1
"$output/image/bin/java" --list-modules > "$output/image-modules.txt"
"$output/image/bin/java" -version > "$output/image-version.txt" 2>&1
# Verify inputs still have the admitted identities after tool execution.
"${cli[@]}" verify "$output/inputs/LOCK.tsv" "$2" > "$output/INVENTORY.after.tsv"
cmp "$output/INVENTORY.tsv" "$output/INVENTORY.after.tsv"
printf 'key\tvalue\njdk\t%s\nos\t%s\narchitecture\t%s\nroots\t%s\n' "$jdk" "$(uname -s)" "$(uname -m)" "$2" > "$output/RUN_CONTEXT.tsv"
printf 'gate\tstatus\nartifact_lock\tPASS\nstaging\tPASS\nmodule_closure\tPASS\njlink\tPASS\nimage_startup\tPASS\napplication_semantics\tNOT_RUN\ndistribution_approval\tNOT_GRANTED\n' > "$output/STATUS.tsv"
cp "$output/STATUS.tsv" "$output/VERIFY_CONTRACT.tsv"
{ printf 'sha256\tfile\n'; sha256sum "$jdk/release" "$jdk"/jmods/*.jmod | awk '{print $1 "\t" $2}'; } > "$output/PROVENANCE.tsv"
printf 'artifact\tmeaning\nINVENTORY.tsv\tActual locked modules\nimage\tLinked platform-specific runtime\nSTATUS.tsv\tExecuted gates only\n' > "$output/OUTPUT_CONTRACT.tsv"
printf '# Module pack assembly\n\nThe selected runtime linked and started. Application/native smoke and distribution approval are separate gates.\n\nRoots: `%s`\n' "$2" > "$output/FINAL_REPORT.md"
printf 'PASS: linked and started %s (application acceptance is separate)\n' "$output/image"
