#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Compare human release commands with the machine release contract.

Development mode is an inventory. --release is fail-closed and will reject
stale branch/tag instructions; this tool creates no branch, tag or artifact.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path

NOTES_BRANCH = re.compile(r"\bBranch:\s*\x60([^\x60]+)\x60")
CHECKLIST_BRANCH = re.compile(r"(?m)^\|\s*Release branch\s*\|\s*\x60([^\x60]+)\x60")


def audit(root: Path) -> dict:
    root = root.resolve()
    release = root / "m3/release"
    contract = json.loads((release / "release-contract.json").read_text(encoding="utf-8"))
    notes = (release / "RELEASE_NOTES.md").read_text(encoding="utf-8")
    checklist = (release / "RELEASE_CHECKLIST.md").read_text(encoding="utf-8")
    configured = contract.get("release", {}).get("release_branch")
    note_match = NOTES_BRANCH.search(notes)
    checklist_match = CHECKLIST_BRANCH.search(checklist)
    documented = {
        "machine_contract": configured,
        "release_notes": note_match.group(1) if note_match else None,
        "release_checklist": checklist_match.group(1) if checklist_match else None,
    }
    complete = all(isinstance(value, str) and value.startswith("release/")
                   for value in documented.values())
    consistent = complete and len(set(documented.values())) == 1
    return {
        "schema": "M3_RELEASE_IDENTITY_AUDIT_V1",
        "product": contract.get("release", {}).get("product"),
        "branches": documented,
        "identity_consistent": consistent,
        "release_documentation_admissible": (
            consistent and contract.get("release", {}).get("product") == "M3JDK21"),
        "disclaimer": "checks only branch identity; product build, receipts and legality still required",
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--json-out", type=Path)
    parser.add_argument("--release", action="store_true",
                        help="fail unless all human and machine release branches agree")
    args = parser.parse_args()
    result = audit(args.root)
    output = json.dumps(result, indent=2, sort_keys=True) + "\n"
    if args.json_out:
        args.json_out.parent.mkdir(parents=True, exist_ok=True)
        args.json_out.write_text(output, encoding="utf-8")
    print(output, end="")
    return 0 if not args.release or result["release_documentation_admissible"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
