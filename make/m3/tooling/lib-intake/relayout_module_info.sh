#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Replays the current S6 comment block (module_info_header.sh) onto the generated module-info.java of every committed
# relocated layer, without re-running the intake: everything before the "module" line is replaced, the declaration and
# its body stay byte-identical (proved by comparing the files from the "module" line on). Comments cannot change the
# compiled module descriptor, so the JMODs are unaffected.
# Usage (repository root): bash m3/tooling/lib-intake/relayout_module_info.sh
set -euo pipefail
D=m3/tooling/lib-intake; LAYERS=m3/vendor/third_party-m3/relocated
find "$LAYERS" -path '*/src/main/java/module-info.java' | LC_ALL=C sort | while IFS= read -r f; do
    lib=${f#"$LAYERS"/}; lib=${lib%%/*}
    body=$(awk '/^module / { on = 1 } on' "$f")
    [ -n "$body" ] || { echo "RELAYOUT_FAIL no module line in $f"; exit 2; }
    { bash "$D/module_info_header.sh" "$lib"; printf '%s\n' "$body"; } > "$f.new"
    cmp -s <(awk '/^module / { on = 1 } on' "$f.new") <(printf '%s\n' "$body") || { echo "RELAYOUT_FAIL body $f"; exit 3; }
    mv "$f.new" "$f"; echo "RELAYOUT $lib"
done
