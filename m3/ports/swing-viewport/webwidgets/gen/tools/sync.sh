#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copies the generator, specs and concept tables into both target trees. Usage: sync.sh <gen> <concepts dir> <nebula m3web> <swing-viewport>
set -euo pipefail
gen=$1; cd_=$2; neb=$3; sw=$4
for dst in "$neb/tools/gen" "$sw/webwidgets/gen"; do
  rm -rf "$dst"; mkdir -p "$dst"
  cp -r "$gen/M3WebGen.java" "$gen/README.md" "$gen/RECIPE_MEMORY.tsv" "$gen/spec" "$gen/templates" "$gen/tools" "$dst/"
done
cp "$cd_/CONCEPTS.tsv" "$cd_/COMPONENT_CONCEPTS.tsv" "$gen/spec/FAMILIES.tsv" "$neb/"
cp "$gen/spec/FOSS_REUSE_DECISION.webwidgets.tsv" "$neb/FOSS_REUSE_DECISION.tsv"
cp "$gen/README.md" "$neb/README.md"
cp "$cd_/CONCEPTS.tsv" "$cd_/COMPONENT_CONCEPTS.tsv" "$gen/spec/FAMILIES.tsv" "$gen/README.md" "$sw/webwidgets/"
cp "$gen/spec/FOSS_REUSE_DECISION.webwidgets.tsv" "$sw/webwidgets/FOSS_REUSE_DECISION.tsv"
echo synced
