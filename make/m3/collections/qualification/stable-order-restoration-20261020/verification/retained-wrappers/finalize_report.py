#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Derive the final narrative from existing logs; no compilation or test execution."""
from pathlib import Path
import datetime
import hashlib
import json

root = Path(__file__).resolve().parent
path = root / "RESULTS.json"
before = path.read_bytes()
result = json.loads(before)
if "report_finalization" in result:
    raise SystemExit("Refusing to overwrite the finalized report")
for command in json.loads((root / "COMMANDS.json").read_text()):
    assert hashlib.sha256((root / command["log"]).read_bytes()).hexdigest() == command["log_sha256"]
outcomes = {item["mode"]: item for item in result["original_standalone_contract"]}
assert outcomes["interior-null"]["outcome"] == "PASS"
assert outcomes["interior-null"]["checks"] == 6
result["known_findings"] = [
    "Comparator-owned IllegalArgumentException is swallowed in the bounded read-only map witness. Both comparator-failure and the original default readonly mode stop at that first failure; their later mutable-set assertion is not reached.",
    "The ordinary read-only iterator retains the indexed callback path and exceeds the original linear-work oracle: 787456 predicate calls for a 1024-element backing list. There is no traversal speed claim.",
    "The ordinary read-only iterator does not detect the backing mutation in the original fail-fast witness. Later assertions in that mode are not reached.",
]
result["freshly_passing_historical_oracles"] = {
    "readonly-policy": 25, "readonly-effects": 8, "bounded-map": 64,
    "bounded-set": 64, "interior-null": 6,
}
result["historical_vs_current"] = (
    "The fixtures were published historically and are byte-identical here. Every listed outcome is a new execution against the pinned current tree. "
    "The historical pending interior-null label is not a current reproduced failure: all six unchanged checks pass. "
    "Pass counts across modes overlap and must not be added as unique assertions."
)
result["compiler_diagnostics_empty"] = (root / "strict-javac.log").stat().st_size == 0
result["report_finalization"] = {
    "timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    "operation": "Read existing logs and correct the initial driver's static known-findings narrative to the actual passing interior-null outcome; no execution result changed.",
    "pre_finalization_report_sha256": hashlib.sha256(before).hexdigest(),
    "finalizer_sha256": hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
    "tests_rerun": False,
}
path.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n")
print("RESULTS.json SHA256 " + hashlib.sha256(path.read_bytes()).hexdigest())
