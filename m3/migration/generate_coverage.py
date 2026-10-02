#!/usr/bin/env python3
from __future__ import annotations
import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).parent
data = json.loads((ROOT / "manifest.json").read_text(encoding="utf-8"))
mappings = data["mappings"]
counts = Counter(item["status"] for item in mappings)
lines = [
    "# MIndex -> M3 migration coverage","",
    "Generated from `manifest.json`; do not edit status rows by hand.","",
    f"Source baseline: `{data['baselines']['source']['commit']}`",
    f"Target master baseline: `{data['baselines']['targetMaster']['commit']}`",
    f"Route-C candidate: `{data['baselines']['targetRuntimeCandidate']['commit']}`","",
    "## Status summary","",
    "| Status | Count |","| --- | ---: |"
]
for status in ("implemented-tested","implemented-unverified","partial","proposed","blocked","excluded"):
    lines.append(f"| {status} | {counts[status]} |")
lines += ["","## Capability mappings","",
          "| ID | Capability | Kind | Status | Source -> target |",
          "| --- | --- | --- | --- | --- |"]
for item in mappings:
    sources = ", ".join(endpoint["symbol"] for endpoint in item["sources"])
    destinations = ", ".join(endpoint["symbol"] for endpoint in item["destinations"]) or "—"
    lines.append(f"| `{item['id']}` | {item['capability']} | {item['mappingKind']} | **{item['status']}** | `{sources}` -> `{destinations}` |")
lines += ["","## Visible gaps",""]
for item in mappings:
    if item["status"] != "implemented-tested":
        detail = "; ".join(item["conflicts"]) or "verification, inventory closure, or promotion still pending"
        lines.append(f"- `{item['id']}` — **{item['status']}**: {detail}")
lines += ["","A destination file is not proof of migration completion. Exact contracts and exact-candidate evidence remain mandatory.",""]
(ROOT / "COVERAGE.md").write_text("\n".join(lines), encoding="utf-8")
print(f"M3_MIGRATION_COVERAGE_WRITTEN mappings={len(mappings)}")
