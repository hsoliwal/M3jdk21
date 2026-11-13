#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
set -euo pipefail
root=$(cd "$(dirname "$0")/../.." && pwd)
: "${M3_TEST_JDK:?matched M3JDK image required}"
out="$root/m3/build/phase1-string-resources"
rm -rf "$out"
mkdir -p "$out/classes"
"$M3_TEST_JDK/bin/javac" -Xlint:all -Werror -d "$out/classes" \
  "$root/m3/runtime-integration/tests/M3StringPhase1ResourceProbe.java"
for fork in 1 2 3; do
  for enabled in false true; do
    flags=()
    if [[ "$enabled" == true ]]; then
      flags=(-XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage)
    fi
    "$M3_TEST_JDK/bin/java" -Xms256m -Xmx256m -XX:+UseG1GC \
      -XX:-UseStringDeduplication "${flags[@]}" \
      --add-opens java.base/java.lang=ALL-UNNAMED \
      -cp "$out/classes" M3StringPhase1ResourceProbe "$enabled" "$fork" \
      > "$out/fork-$fork-m3-$enabled.json"
  done
done
python3 - "$out" <<'PY'
import json
import pathlib
import statistics
import sys

root = pathlib.Path(sys.argv[1])
rows = [json.loads(path.read_text(encoding="utf-8"))
        for path in sorted(root.glob("fork-*-m3-*.json"))]
if len(rows) != 6:
    raise SystemExit(f"expected six resource rows, got {len(rows)}")
fields = [
    "cold_allocated_bytes", "cold_ns", "warm_allocated_bytes", "warm_ns",
    "native_retained_delta_bytes", "compatibility_shadow_bytes",
]
summary = {"schema": "M3JDK21_STRING_PHASE1_RESOURCES_V1", "samples": len(rows)}
for enabled in (False, True):
    subset = [row for row in rows if row["m3"] is enabled]
    key = "m3_on" if enabled else "m3_off"
    summary[key] = {
        field: int(statistics.median(row[field] for row in subset))
        for field in fields
    }
summary["claims"] = {
    "universal_speedup": False,
    "promotion_authority": False,
    "measurement_only": True,
}
(root / "summary.json").write_text(
    json.dumps(summary, indent=2, sort_keys=True) + "\n", encoding="utf-8")
print(json.dumps(summary, sort_keys=True))
PY
