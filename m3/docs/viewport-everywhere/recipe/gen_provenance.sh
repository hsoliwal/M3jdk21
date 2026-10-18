#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
#
# Writes <module>/provenance-viewport-20261018.json for m3/collections and m3/viewport-core from WAVE1_MAP.tsv.
# Usage: gen_provenance.sh <donor-dir> <worktree-m3-dir>
#   <donor-dir> holds swt/<Class>.java and swt-tests/<Class>.java exported from the source commit.
set -eu
DONOR=$1; M3=$2; MAP=$(dirname "$0")/WAVE1_MAP.tsv
SRC=553eefd41d9961d303c41e0e4e181712e64a1e45
BASE="bundles/org.eclipse.swt/Eclipse SWT/common/org/eclipse/swt"
TESTS="tests/org.eclipse.swt.tests/JUnit Tests/org/eclipse/swt/tests/junit"
for module in collections viewport-core; do
  pkg=$([ $module = collections ] && echo collections || echo viewport)
  out="$M3/$module/provenance-viewport-20261018.json"
  {
    printf '{\n  "source_repository": "hsoliwal/eclipse.platform.swt",\n  "source_commit": "%s",\n' $SRC
    printf '  "recipe": "m3/docs/viewport-everywhere/recipe/META-INF/rewrite/m3-viewport-everywhere.yml",\n'
    printf '  "authority": "m3/docs/viewport-everywhere/RELICENSE_RECORD.md",\n  "files": [\n'
    awk -F'\t' -v m=$module 'NR>1 && $3==m' "$MAP" | while IFS=$'\t' read -r cls dir mod target kind; do
      if [ $kind = main ]; then donor="$DONOR/swt/$cls.java"; tree=main; spath="$BASE/$dir/$cls.java"
      else donor="$DONOR/swt-tests/$cls.java"; tree=test; spath="$TESTS/$cls.java"; fi
      tfile="$M3/$module/src/$tree/java/com/m3/$pkg/$target.java"
      printf '    {"source": "%s", "source_blob": "%s", "source_sha256": "%s",\n' "$spath" \
        "$(git hash-object "$donor")" "$(sha256sum "$donor" | cut -d' ' -f1)"
      printf '     "target": "m3/%s/src/%s/java/com/m3/%s/%s.java", "target_sha256": "%s",\n' $module $tree $pkg $target \
        "$(sha256sum "$tfile" | cut -d' ' -f1)"
      printf '     "changes": ["Apache-2.0 header with donor attribution kept", "type rename", "package move", "%s"],\n' \
        "$([ $kind = main ] && echo 'package-private surface made public; SWT.error replaced by the identical IllegalArgumentException' || echo 'reflective class names follow the rename')"
      printf '     "license": "Apache-2.0 (%s at source; same author)"},\n' \
        "$([ $cls = PackedHeapGrowth ] && echo 'EPL-2.0 OR Apache-2.0' || echo 'EPL-2.0')"
    done | sed '$ s/},$/}/'
    printf '  ]\n}\n'
  } > "$out"
done
