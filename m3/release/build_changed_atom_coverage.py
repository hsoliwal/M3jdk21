#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Import explicit, source-pinned JaCoCo method coverage into an M3 release receipt.

This is data-only inspection, not recipe execution or proof of inventory completeness.
A reviewer must bind the fragment to the exact source-bound release manifest,
the raw reports, and the *complete* changed contract-atom inventory.

Gate rule: an atom passes at >=90% instrumented line and branch coverage (a
branchless atom needs its explanation), or as a reviewed exemption. An exemption
is the inventory's optional `exemption` column: a proof that the uncovered branch
only re-checks an invariant its callers already enforce, citing at least one
repository `path:line` that must exist. A blank cell is no exemption.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import xml.etree.ElementTree as ET

SCHEMA = "M3_CHANGED_ATOM_COVERAGE_V1"
SCOPE = "M3_OWNED_CHANGED_CONTRACT_ATOMS"
FIELDS = ("id", "class", "method", "descriptor", "source_path")
MIN_RATIO = 0.90
EXEMPTION_REF = re.compile(r"(?<![\w/.-])((?:m3|src)/[\w./$-]+\.(?:java|c|cc|cpp|h)):([1-9][0-9]*)")


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


def exemption_proof(root: Path, text: str, ident: str) -> str:
    """Return the stripped proof, refusing one that cites no existing path:line."""
    proof = text.strip()
    refs = EXEMPTION_REF.findall(proof)
    if not refs:
        raise ValueError("reviewed exemption must cite file:line evidence: " + ident)
    for relative, line in refs:
        try:
            cited = source_file(root, relative)
        except ValueError as invalid:
            raise ValueError(f"exemption cites an unusable source ({invalid}): {ident}") from None
        with cited.open("rb") as source:
            lines = sum(1 for _ in source)
        if int(line) > lines:
            raise ValueError(f"exemption cites a line beyond {relative}: {ident}")
    return proof


def atom_passes(atom: dict) -> bool:
    """>=90% line and branch (zero branches count as met), or a reviewed exemption."""
    def met(covered: int, missed: int) -> bool:
        return covered + missed == 0 or covered / (covered + missed) >= MIN_RATIO
    return bool(atom.get("exemption")) or (
        met(atom["line_covered"], atom["line_missed"])
        and met(atom["branch_covered"], atom["branch_missed"]))


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
            if "LINE" not in counters:
                raise ValueError("line counter absent: " + str(key))
            # JaCoCo omits zero-total counters; branchless methods still need
            # an explicit inventory explanation before their receipt is built.
            counters.setdefault("BRANCH", (0, 0))
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
    source_digests = {}  # reuse SHA-256 for atoms sharing one source file
    for row in rows:
        ident = row["id"]
        key = (row["class"], row["method"], row["descriptor"])
        if not ident or ident in seen or not all(key):
            raise ValueError("missing/duplicate coverage atom or method key")
        seen.add(ident)
        if key not in methods:
            raise ValueError("missing instrumented method: " + repr(key))
        counts = methods[key]
        if row["source_path"] not in source_digests:
            source_digests[row["source_path"]] = digest(source_file(root, row["source_path"]))
        line_covered, line_missed = counts["LINE"]
        branch_covered, branch_missed = counts["BRANCH"]
        if line_covered + line_missed == 0:
            raise ValueError("method has no instrumented lines: " + ident)
        atom = {
            "id": ident, "source_path": row["source_path"],
            "source_sha256": source_digests[row["source_path"]],
            "jacoco_class": row["class"], "jacoco_method": row["method"],
            "jacoco_descriptor": row["descriptor"],
            "line_covered": line_covered, "line_missed": line_missed,
            "branch_covered": branch_covered, "branch_missed": branch_missed,
        }
        if branch_covered + branch_missed == 0:
            reason = (row.get("branchless_explanation") or "").strip()
            if not reason:
                raise ValueError("branchless method requires explanation: " + ident)
            atom["branchless_explanation"] = reason
        exemption = (row.get("exemption") or "").strip()
        if exemption:
            atom["exemption"] = exemption_proof(root, exemption, ident)
        atoms.append(atom)
    return {
        "schema": SCHEMA, "scope": SCOPE, "format": "JACOCO_XML",
        "instrumented": True,
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
    parser.add_argument("--require-pass", action="store_true",
                        help="exit 1 unless every atom is >=90%% or a reviewed exemption")
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args(argv)
    try:
        payload = build(args.root, args.report, args.inventory, args.report_artifact,
                        args.inventory_artifact, args.tool, args.reviewed_complete_inventory)
    except (ET.ParseError, ValueError, OSError) as exc:
        parser.error(str(exc))
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    failing = [atom["id"] for atom in payload["atoms"] if not atom_passes(atom)]
    exempt = sum(1 for atom in payload["atoms"] if atom.get("exemption"))
    print(f"Imported {payload['measured_count']} measured atoms; "
          f"{payload['measured_count'] - len(failing)} meet the gate rule ({exempt} by reviewed "
          f"exemption), {len(failing)} below 90%; "
          f"inventory_complete={payload['inventory_complete']}; not a release PASS")
    for ident in failing[:20]:
        print("BELOW_90: " + ident)
    return 1 if args.require_pass and failing else 0


if __name__ == "__main__":
    raise SystemExit(main())
