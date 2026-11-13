#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Generator of the module source successor (2026-11-09) of the collections donation custody chain.

usage: python3 make/m3/collections/qualification/module-source-successor-20261109.py [--check]
1. Replays the sealed chain of verify_donation.py (installed manifest -> successors -> Tools4j -> Superset -> Master)
   and derives the exact predecessor SHA-256 before each pinned stage. The JDK_NAMING_RESEAL of 2026-10-31
   (caf39c93e4c) rewrote the installed-source rows and the packet pin but not the three chain pins, so the chain stopped at
   "Tools4j predecessor receipt chain drift" (bisected by the collections lane); the derived values replace the stale
   pins in verify_donation.py and m3-donation.json.
2. Diffs the Master 20261031 result against the tree: a recorded source whose file is gone (renamed by A1/B+C, E1, U1,
   E2 or retired) becomes REMOVED, a changed file a hash transition, and every Java source of the module that the
   chain does not record an ABSENT admission. Recorded keys stay in the m3/collections/src key space; located() and
   recorded() of verify_donation.py map them to the JDK module layout of #2849.
3. Writes qualification/module-source-successor-20261109.tsv, seals it in verify_donation.py (_MODULE_* constants) and
   in m3-donation.json (module_source_successor_20261109, now source_set_successor_current).
Idempotent: a second run changes nothing; --check writes nothing and exits 1 when a run would change a file.
Tip rule: this successor is the chain's tip (source_set_successor_current) and describes the current tree, so a lane that
adds, renames or removes module sources reruns this generator in the same PR and the tip is re-sealed in place; the
earlier receipts (installed manifest ../../../../m3/collections Master 20261031) are never modified."""
import hashlib
import importlib.util
import json
import re
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parents[1]
CHECK = "--check" in sys.argv
SPEC = importlib.util.spec_from_file_location("verify_donation", HERE / "verify_donation.py")
vd = importlib.util.module_from_spec(SPEC)
sys.path.insert(0, str(HERE))
SPEC.loader.exec_module(vd)
RECEIPT = HERE / "qualification/module-source-successor-20261109.tsv"
changed = []


def seal(rows):
    return hashlib.sha256("".join(p + "\t" + e + "\n" for p, e in rows).encode("utf-8")).hexdigest()


def put(path, text):
    if path.exists() and path.read_text(encoding="utf-8") == text:
        return
    changed.append(path.relative_to(HERE.parents[2]).as_posix())
    if not CHECK:
        path.write_text(text, encoding="utf-8", newline="\n")


packet = vd.read_donation_packet()
manifest = HERE / packet["installed_source_manifest"]
rows = [line.split("\t") for line in manifest.read_text().splitlines() if line and not line.startswith("#")]
rows = vd.include_path_mask_successor(vd.include_stored_representative_successor(vd.include_filtered_view_successor(rows)))
rows = vd.include_object_state_offset_successor(rows)
pins = {}
vd._TOOLS4J_PREDECESSOR_SHA256 = pins["_TOOLS4J_PREDECESSOR_SHA256"] = seal(rows)
rows = vd.include_tools4j_refinement_successor(rows)
vd._SUPERSET_PREDECESSOR_SHA256 = pins["_SUPERSET_PREDECESSOR_SHA256"] = seal(rows)
rows = vd.include_superset_source_successor(rows, vd._SUPERSET_RECEIPT)
master_pred = seal(rows)
pins["_MASTER_PREDECESSOR_SHA256"] = master_pred
rows = vd.admit_source_transitions(rows, "Master 20261031", vd._MASTER_RECEIPT, vd._MASTER_RECEIPT_SHA256, master_pred,
                                   vd._MASTER_TRANSITIONS, vd._MASTER_RESULT_ROWS, vd._MASTER_RESULT_JAVA)
print("CHAIN replayed to Master 20261031:", len(rows), "rows")

# 2. the module successor: removals and transitions in recorded order, admissions sorted
module_pred = seal(rows)
recorded_paths = {path for path, _ in rows}
entries = []
for path, expected in rows:
    file = vd.located(path)
    if not file.is_file():
        entries.append((path, expected, "REMOVED"))
    elif vd.digest(file) != expected:
        entries.append((path, expected, vd.digest(file)))
current_java = {vd.recorded(p) for p in (HERE / "src").rglob("*.java")}
current_java |= {vd.recorded(p) for root in vd.MOVED_JAVA_ROOTS for p in (vd.REPO / root).rglob("*.java")}
for path in sorted(current_java - recorded_paths):
    entries.append((path, "ABSENT", vd.digest(vd.located(path))))
receipt_text = "".join("\t".join(cells) + "\n" for cells in entries)
put(RECEIPT, receipt_text)
receipt_sha = hashlib.sha256(receipt_text.encode("utf-8")).hexdigest()
kept = [(p, e) for p, e in rows if not any(c[0] == p and c[2] == "REMOVED" for c in entries)]
result_rows = len(kept) + sum(1 for c in entries if c[1] == "ABSENT")
result_java = len(current_java)
counts = {"removed": sum(1 for c in entries if c[2] == "REMOVED"), "added": sum(1 for c in entries if c[1] == "ABSENT")}
counts["refined"] = len(entries) - counts["removed"] - counts["added"]
print("MODULE transitions", len(entries), counts, "result rows", result_rows, "java", result_java)

# 3. seals in verify_donation.py and m3-donation.json
source = (HERE / "verify_donation.py").read_text(encoding="utf-8")
for name, value in pins.items():
    source = re.sub(rf"^{name} = '[0-9a-f]{{64}}'$", f"{name} = '{value}'", source, flags=re.M)
for name, value in (("_MODULE_RECEIPT_SHA256", f"'{receipt_sha}'"), ("_MODULE_PREDECESSOR_SHA256", f"'{module_pred}'"),
                    ("_MODULE_TRANSITIONS", str(len(entries))), ("_MODULE_RESULT_ROWS", str(result_rows)),
                    ("_MODULE_RESULT_JAVA", str(result_java))):
    source = re.sub(rf"^{name} = .*$", f"{name} = {value}", source, count=1, flags=re.M)
put(HERE / "verify_donation.py", source)

text = (HERE / "m3-donation.json").read_text(encoding="utf-8")
old = json.loads(text)
for key, pin in (("tools4j_unix_refinement_20261019", "_TOOLS4J_PREDECESSOR_SHA256"),
                 ("superset_source_successor_20261030", "_SUPERSET_PREDECESSOR_SHA256"),
                 ("master_source_successor_20261031", "_MASTER_PREDECESSOR_SHA256")):
    stale = old[key]["inherited_composed_manifest_sha256"]
    if stale != pins[pin]:
        assert text.count(stale) == 1, key
        text = text.replace(stale, pins[pin])
text = text.replace('"source_set_successor_current": "master_source_successor_20261031"',
                    '"source_set_successor_current": "module_source_successor_20261109"')
record = {
    "schema": "M3_COLLECTION_EXACT_SOURCE_REFINEMENT_V2",
    "successor_receipt": "qualification/module-source-successor-20261109.tsv",
    "successor_receipt_sha256": receipt_sha,
    "receipt_columns": ["path", "inherited_sha256", "final_received_sha256_or_REMOVED"],
    "inherited_composed_manifest_sha256": module_pred,
    "inherited_rows": len(rows),
    "successor_rows": len(entries),
    "refined_rows": counts["refined"],
    "added_rows": counts["added"],
    "removed_rows": counts["removed"],
    "resulting_rows": result_rows,
    "exact_java_source_membership": result_java,
    "predecessor_successor": "master_source_successor_20261031",
    "origin": "master after the JDK module move #2849: sources absorbed, renamed and moved since the master seal; "
              "recorded keys stay in the m3/collections/src key space (verify_donation.py located/recorded)",
    "chain_repins": "JDK_NAMING_RESEAL caf39c93e4c left the Tools4j, Superset and Master predecessor pins stale; "
                    "derived by replaying the sealed chain",
    "recipe": "make/m3/collections/qualification/module-source-successor-20261109.py",
    "phase_advance": False,
}
body = json.dumps(record, indent=2, ensure_ascii=False).replace("\n", "\n  ")
existing = re.search(r'^  "module_source_successor_20261109": \{.*?\n  \}', text, flags=re.S | re.M)
if existing:  # the record itself (top level, two spaces), not the source_set_successor_current pointer
    text = text[:existing.start()] + '  "module_source_successor_20261109": ' + body + text[existing.end():]
else:
    top = re.search(r'^  "master_source_successor_20261031": \{', text, flags=re.M)
    text = text[:top.start()] + '  "module_source_successor_20261109": ' + body + ",\n" + text[top.start():]
assert "module_source_successor_20261109" in json.loads(text), "the record must be a top-level key"
put(HERE / "m3-donation.json", text)
for c in changed:
    print("CHANGED", c)
print(f"MODULE_SUCCESSOR changed={len(changed)} check={CHECK}")
sys.exit(1 if CHECK and changed else 0)
