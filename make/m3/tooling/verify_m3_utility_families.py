#!/usr/bin/env python3
"""Validate M3JDK utility name census without treating proposed types as code."""

from __future__ import annotations

import argparse
import csv
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[2]
CATALOGUE = ROOT / "m3/docs/m3-utility-families.tsv"
HEADER = ["family", "module", "upstream_api", "current_owner_path", "status", "required_gate"]
STATES = {"QUALIFIED_ISOLATED", "IMPLEMENTED_INTERNAL", "EXISTING_INTERNAL_KERNEL", "MAPPED_PENDING"}
SEEDS = {"M3Objects", "M3Strings", "M3Streams", "M3Reflection", "M3Arrays",
         "M3Collections", "M3IO", "M3Files"}


def verify() -> tuple[int, list[str]]:
    with CATALOGUE.open(newline="", encoding="utf-8") as stream:
        reader = csv.DictReader(stream, delimiter="\t")
        if reader.fieldnames != HEADER:
            raise ValueError(f"schema mismatch: {reader.fieldnames}")
        records = list(reader)
    if not records:
        raise ValueError("empty family catalogue")
    previous = ""
    module_names = set()
    for row in records:
        family = row["family"]
        if not re.fullmatch(r"M3[A-Z][A-Za-z0-9]*", family) or family <= previous:
            raise ValueError(f"duplicate, unsorted or malformed family: {family}")
        previous = family
        if row["status"] not in STATES or not row["upstream_api"] or not row["required_gate"]:
            raise ValueError(f"bad status/API/gate for {family}")
        module = row["module"]
        if not (ROOT / "src" / module).is_dir():
            raise ValueError(f"unknown target module: {module}")
        module_names.add(module)
        path = row["current_owner_path"]
        if row["status"] == "MAPPED_PENDING":
            if path != "-":
                raise ValueError(f"pending name incorrectly claims implementation: {family}")
        else:
            if path == "-" or not (ROOT / path).is_file():
                raise ValueError(f"implemented name missing exact owner: {family} -> {path}")
            if not Path(path).name == family + ".java":
                raise ValueError(f"implemented name points at unrelated type: {family}")
            source = (ROOT / path).read_text(encoding="utf-8")
            if not re.search(r"\b(?:public\s+)?final\s+class\s+" + re.escape(family) + r"\b", source):
                raise ValueError(f"missing exact class declaration: {family}")
            if row["status"] in ("IMPLEMENTED_INTERNAL", "EXISTING_INTERNAL_KERNEL") and not path.startswith(
                    f"src/{module}/share/classes/jdk/internal/"):
                raise ValueError(f"internal owner not in target JDK module: {family}")
            if row["status"] == "QUALIFIED_ISOLATED" and not path.startswith("m3/"):
                raise ValueError(f"isolated owner not in its source module: {family}")
    missing = SEEDS.difference(row["family"] for row in records)
    if missing:
        raise ValueError(f"seed API families missing: {sorted(missing)}")
    all_modules = {p.name for p in (ROOT / "src").iterdir()
                   if p.is_dir() and (p / "share/classes/module-info.java").is_file()}
    return len(records), sorted(all_modules - module_names)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--missing-modules", action="store_true",
                        help="show module-level inventory gaps; not a claim of one utility per module")
    args = parser.parse_args()
    try:
        count, missing = verify()
        print(f"M3 utility-family catalogue valid: {count} families; "
              "implemented classes checked against source")
        if args.missing_modules:
            print(f"Not-yet-covered modules ({len(missing)}): " + ", ".join(missing))
        return 0
    except (OSError, ValueError) as exc:
        print(f"M3 utility-family catalogue INVALID: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
