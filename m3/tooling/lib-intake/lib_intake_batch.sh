#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# LIB_INTAKE_TO_JMOD batch: runs lib_intake.sh for every INTAKE.tsv row (or the libs given), records BATCH.tsv (lib, module,
# result, last stage), then links every produced JMOD into ONE image with the JDK's jmods and validates it
# (COMBINED_IMAGE.tsv): the M3JDK21 optional-module set as one runtime.
# Usage (repository root): bash m3/tooling/lib-intake/lib_intake_batch.sh <work root> [lib ...]
set -uo pipefail
WR="$1"; shift; D=m3/tooling/lib-intake; BIN="$JAVA_HOME/bin"; SEP=":"; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";";; esac
libs=("$@"); [ "${#libs[@]}" -gt 0 ] || mapfile -t libs < <(awk -F'\t' 'NR > 1 && $1 !~ /^#/ { print $1 }' "$D/INTAKE.tsv")
printf 'lib\tmodule\tresult\tlast_stage\n' > "$D/BATCH.tsv"; mods=(); paths=()
for lib in "${libs[@]}"; do
	mkdir -p "$D/runs/$lib"
	bash "$D/lib_intake.sh" "$lib" "$WR/$lib" > "$D/runs/$lib/intake.log" 2>&1; rc=$?
	last=$(tail -1 "$D/runs/$lib/STAGES.tsv" | cut -f1,2 | tr '\t' ' ')
	module=$(awk -F'\t' '$1 == "module" { print $2 }' "$D/runs/$lib/RUN_CONTEXT.tsv" 2>/dev/null)
	printf '%s\t%s\t%s\t%s\n' "$lib" "${module:--}" "$([ "$rc" -eq 0 ] && echo PASS || echo FAIL)" "$last" >> "$D/BATCH.tsv"
	echo "BATCH $lib rc=$rc $last"
	[ "$rc" -eq 0 ] && { mods+=("$module"); paths+=("$WR/$lib/jmods"); }
done
img="$WR/combined-image"; rm -rf "$img"; mp="$JAVA_HOME/jmods"; for p in "${paths[@]}"; do mp="$mp$SEP$p"; done
add="java.base$(printf ',%s' "${mods[@]}")"
{ printf 'step\tresult\n'
  if "$BIN/jlink" --module-path "$mp" --add-modules "$add" --strip-debug --no-header-files --no-man-pages --output "$img" > "$D/combined-jlink.log" 2>&1; then
	  printf 'jlink\tPASS modules=%s\n' "${#mods[@]}"
	  "$img/bin/java" --validate-modules > "$D/combined-validate.log" 2>&1 && printf 'validate-modules\tPASS\n' || printf 'validate-modules\tFAIL\n'
	  "$img/bin/java" --list-modules | tr -d '\r' > "$D/combined-modules.log"
	  for m in "${mods[@]}"; do grep -q "^$m@" "$D/combined-modules.log" && printf 'module %s\tPRESENT\n' "$m" || printf 'module %s\tMISSING\n' "$m"; done
  else printf 'jlink\tFAIL (combined-jlink.log)\n'; fi; } > "$D/COMBINED_IMAGE.tsv"
cat "$D/COMBINED_IMAGE.tsv"
echo "BATCH_DONE pass=$(awk -F'\t' 'NR > 1 && $3 == "PASS"' "$D/BATCH.tsv" | wc -l) fail=$(awk -F'\t' 'NR > 1 && $3 == "FAIL"' "$D/BATCH.tsv" | wc -l)"
