#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# LIB_INTAKE_TO_JMOD combined image: every library whose run passed S8 (runs/<lib>/STAGES.tsv) contributes its JMOD from
# <work root>/<lib>/jmods; one jlink of java.base + all those modules, --validate-modules, and a presence check per module
# (COMBINED_IMAGE.tsv). Re-runnable on its own after any single-library re-run.
# Usage (repository root): bash m3/tooling/lib-intake/lib_intake_combine.sh <work root>
set -uo pipefail
WR="$1"; D=m3/tooling/lib-intake; BIN="$JAVA_HOME/bin"; SEP=":"; case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";";; esac
mods=(); mp="$JAVA_HOME/jmods"
for st in "$D"/runs/*/STAGES.tsv; do
	lib=$(basename "$(dirname "$st")")
	grep -q $'^S8_LAYER\tPASS' "$st" || continue
	m=$(awk -F'\t' '$1 == "module" { print $2 }' "$D/runs/$lib/RUN_CONTEXT.tsv")
	[ -f "$WR/$lib/jmods/$m.jmod" ] || { echo "COMBINE skip $lib: no $WR/$lib/jmods/$m.jmod"; continue; }
	mods+=("$m"); mp="$mp$SEP$WR/$lib/jmods"
done
img="$WR/combined-image"; rm -rf "$img"
{ printf 'step\tresult\n'
  if "$BIN/jlink" --module-path "$mp" --add-modules "java.base$(printf ',%s' "${mods[@]}")" --strip-debug --no-header-files --no-man-pages --output "$img" > "$D/combined-jlink.log" 2>&1; then
	  printf 'jlink\tPASS modules=%s\n' "${#mods[@]}"
	  "$img/bin/java" --validate-modules > "$D/combined-validate.log" 2>&1 && printf 'validate-modules\tPASS\n' || printf 'validate-modules\tFAIL\n'
	  "$img/bin/java" --list-modules | tr -d '\r' > "$D/combined-modules.log"
	  for m in "${mods[@]}"; do grep -q "^$m@" "$D/combined-modules.log" && printf 'module %s\tPRESENT\n' "$m" || printf 'module %s\tMISSING\n' "$m"; done
  else printf 'jlink\tFAIL (combined-jlink.log)\n'; fi; } > "$D/COMBINED_IMAGE.tsv"
cat "$D/COMBINED_IMAGE.tsv"
