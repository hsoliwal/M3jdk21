#!/usr/bin/env python3
"""Prepare only an isolated, exact two-file candidate and its immutable inputs."""
from __future__ import annotations

import difflib
import hashlib
import json
from pathlib import Path
import shutil

from run_text_owner_gate import ROOT, WORK, digest, write_json


def main() -> None:
    base = WORK / "source-lock-correction"
    out = base / "candidate-receiver"
    if out.exists():
        raise AssertionError("Candidate receiver already exists")
    shutil.copytree(ROOT, out, ignore=shutil.ignore_patterns(".git", "target", "__pycache__"))
    owner = "m3/core/src/com/m3/text/SharedLexiconPrecomputeCatalog.java"
    lock_path = "m3/tooling/module-packs/nlp-search/current-source-lock.json"
    before = (ROOT / owner).read_text()
    marker = "        public Builder translation(TranslationIdentity identity, M3LexiconPrecompute.TranslationProjection value) {\n"
    if before.count(marker) != 1 or "public Builder()" in before:
        raise AssertionError("Unexpected Builder constructor preimage")
    after = before.replace(marker, "        public Builder() { }\n\n" + marker)
    (out / owner).write_text(after)
    lock = json.loads((ROOT / lock_path).read_text())
    old_lock = json.loads((ROOT / lock_path).read_text())
    changed = [
        "m3/core/src/com/m3/text/M3GvasLiteralImage.java",
        "m3/core/src/com/m3/text/M3BigString.java",
        "m3/core/src/com/m3/text/M3RopeTextIndex.java",
        owner,
    ]
    for path in changed:
        lock["modules"]["com.m3.text"]["files"][path] = digest((out / path).read_bytes())
    (out / lock_path).write_text(json.dumps(lock, indent=2, sort_keys=True) + "\n")
    scope = []
    old = old_lock["modules"]["com.m3.text"]["files"]
    new = lock["modules"]["com.m3.text"]["files"]
    for path in sorted(set(old) | set(new)):
        if old.get(path) != new.get(path):
            scope.append({"path": path, "before_sha256": old.get(path, "ABSENT"),
                          "after_sha256": new.get(path, "ABSENT")})
    if {row["path"] for row in scope} != set(changed):
        raise AssertionError("Unexpected lock scope")
    restored = json.loads(json.dumps(lock))
    restored["modules"]["com.m3.text"]["files"] = old
    if restored != old_lock:
        raise AssertionError("Non-file guard changed")
    actual = {str(p.relative_to(out)) for p in (out / "m3/core/src").rglob("*.java")}
    if actual != set(new):
        raise AssertionError("Source coverage is not exact")
    handoff = []
    for path, kind in [(owner, "JAVA"), (lock_path, "TEXT")]:
        candidate = base / "candidates" / path
        candidate.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(out / path, candidate)
        handoff.append({"target_path": path, "candidate_path": str(candidate),
                        "before_sha256": digest((ROOT / path).read_bytes()),
                        "after_sha256": digest(candidate.read_bytes()), "kind": kind})
        delta = "".join(difflib.unified_diff((ROOT / path).read_text().splitlines(True),
                                           candidate.read_text().splitlines(True),
                                           fromfile="baseline2779/" + path, tofile="candidate/" + path))
        (base / (Path(path).name + ".diff")).write_text(delta)
    write_json(base / "CANDIDATE_PATHS.json", handoff)
    write_json(base / "CANDIDATE_SCOPE.json", {
        "schema": "M3_NLP_SOURCE_LOCK_CANDIDATE_SCOPE_1",
        "state": "PREPARED_NOT_YET_QUALIFIED",
        "receiver_base": "2779aba3f98f31c324be43f731be7914e1a05935",
        "isolated_candidate": str(out), "target_changes": handoff,
        "lock_file_entry_changes": scope,
        "all_other_json_values_unchanged": True,
        "com_m3_text_exact_source_coverage": len(actual),
        "declared_constructor": "public SharedLexiconPrecomputeCatalog.Builder()",
        "lock_original_receiver_metadata_retained": old_lock["receiver"],
        "historical_receiver_metadata_scope": "Retained original lock provenance; exact current base and two-file successor are recorded by the new canonical recipe, not inferred from this older field."
    })
    print(json.dumps({"candidate_receiver": str(out), "handoff": handoff,
                      "exact_source_coverage": len(actual), "lock_entry_changes": len(scope)}))


if __name__ == "__main__":
    main()
