#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Import explicit, source-pinned JaCoCo method coverage into an M3 release receipt.

This is data-only inspection, not recipe execution or proof of inventory completeness.
A reviewer must bind the fragment to the exact source-bound release manifest,
the raw reports, and the *complete* changed contract-atom inventory.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
from pathlib import Path, PurePosixPath
import xml.etree.ElementTree as ET

SCHEMA = "M3_CHANGED_ATOM_COVERAGE_V1"
SCOPE = "M3_OWNED_CHANGED_CONTRACT_ATOMS"
FIELDS = ("id", "class", "method", "descriptor", "source_path")


def digest(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            h.update(block)
    return h.hexdigest()


def source_file(root: Path, relative: str) -> Path:
    if not isinstance(relative, str) or not relative or "\\" in relative:
        raise ValueError("invalid source path")
    parts = PurePosixPath(relative).parts
    if (PurePosixPath(relative).is_absolute() or ".." in parts or "." in parts
            or not relative.startswith(("m3/", "src/"))
            or not relative.endswith((".java", ".c", ".cc", ".cpp", ".h"))):
        raise ValueError("invalid source path: " + relative)
    node = root.resolve()
    for part in parts:
        node = node / part
        if node.is_symlink():
            raise ValueError("source symlink forbidden: " + relative)
    if not node.is_file() or not node.resolve().is_relative_to(root.resolve()):
        raise ValueError("missing or escaping source path: " + relative)
    return node


def read_jacoco(report: Path) -> dict:
    xml = ET.parse(report).getroot()
    if xml.tag != "report":
        raise ValueError("JaCoCo XML report expected")
    by_method = {}
    for klass in xml.findall(".//class"):
        owner = klass.get("name")
        for method in klass.findall("method"):
            key = (owner, method.get("name"), method.get("desc"))
            if not all(isinstance(x, str) and x for x in key) or key in by_method:
                raise ValueError("missing or duplicate JaCoCo method key")
            counters = {}
            for item in method.findall("counter"):
                kind = item.get("type")
                if kind in counters:
                    raise ValueError("duplicate JaCoCo counter: " + str(key))
                if kind in ("LINE", "BRANCH"):
                    covered = int(item.get("covered", "-1"))
                    missed = int(item.get("missed", "-1"))
                    if min(covered, missed) < 0:
                        raise ValueError("invalid JaCoCo count: " + str(key))
                    counters[kind] = (covered, missed)
            if "LINE" not in counters or "BRANCH" not in counters:
                raise ValueError("line/branch counter absent: " + str(key))
            by_method[key] = counters
    if not by_method:
        raise ValueError("JaCoCo report has no measured methods")
    return by_method


def build(root: Path, report: Path, inventory: Path, report_artifact: str,
          inventory_artifact: str, tool: str, reviewed_complete: bool) -> dict:
    if not isinstance(tool, str) or not tool.strip():
        raise ValueError("coverage tool identity required")
    if (not report_artifact or not inventory_artifact
            or report_artifact == inventory_artifact):
        raise ValueError("distinct report and inventory artifact names required")
    methods = read_jacoco(report)
    with inventory.open("r", encoding="utf-8-sig", newline="") as source:
        reader = csv.DictReader(source, delimiter="\t")
        if reader.fieldnames is None or not set(FIELDS).issubset(reader.fieldnames):
            raise ValueError("required inventory TSV columns missing")
        rows = list(reader)
    if not rows:
        raise ValueError("empty coverage inventory")
    atoms = []
    seen = set()
    for row in rows:
        ident = row["id"]
        key = (row["class"], row["method"], row["descriptor"])
        if not ident or ident in seen or not all(key):
            raise ValueError("missing/duplicate coverage atom or method key")
        seen.add(ident)
        if key not in methods:
            raise ValueError("missing instrumented method: " + repr(key))
        counts = methods[key]
        source = source_file(root, row["source_path"])
        line_covered, line_missed = counts["LINE"]
        branch_covered, branch_missed = counts["BRANCH"]
        if line_covered + line_missed == 0:
            raise ValueError("method has no instrumented lines: " + ident)
        atom = {
            "id": ident, "source_path": row["source_path"],
            "source_sha256": digest(source),
            "line_covered": line_covered, "line_missed": line_missed,
            "branch_covered": branch_covered, "branch_missed": branch_missed,
        }
        if branch_covered + branch_missed == 0:
            reason = (row.get("branchless_explanation") or "").strip()
            if not reason:
                raise ValueError("branchless method requires explanation: " + ident)
            atom["branchless_explanation"] = reason
        atoms.append(atom)
    return {
        "schema": SCHEMA, "scope": SCOPE, "instrumented": True,
        "inventory_complete": bool(reviewed_complete),
        "unmeasured_count": 0, "excluded_atoms": [],
        "tool": tool, "report_artifact": report_artifact,
        "inventory_artifact": inventory_artifact, "inventory_sha256": digest(inventory),
        "measured_count": len(atoms), "atoms": sorted(atoms, key=lambda row: row["id"]),
    }


def main(argv=None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--report", type=Path, required=True)
    parser.add_argument("--inventory", type=Path, required=True)
    parser.add_argument("--report-artifact", required=True)
    parser.add_argument("--inventory-artifact", required=True)
    parser.add_argument("--tool", required=True)
    parser.add_argument("--reviewed-complete-inventory", action="store_true",
                        help="mark completed inventory review, never implied by JaCoCo alone")
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args(argv)
    try:
        payload = build(args.root, args.report, args.inventory, args.report_artifact,
                        args.inventory_artifact, args.tool, args.reviewed_complete_inventory)
    except (ET.ParseError, ValueError, OSError) as exc:
        parser.error(str(exc))
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(f"Imported {payload['measured_count']} measured atoms; "
          f"inventory_complete={payload['inventory_complete']}; not a release PASS")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
