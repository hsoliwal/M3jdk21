#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Derive the changed contract-atom inventory TSV read by build_changed_atom_coverage.py.

Scope rule: every method JaCoCo measures in every class compiled from one source
root whose files are *all* absent from the upstream baseline (so every method in
it is changed). The caller proves that absence with git and records it beside the
inventory; this tool only enumerates methods and never decides coverage or scope.

Rows are sorted by id and rendered with LF line endings so a regenerated inventory
is byte-identical. A method without a JaCoCo BRANCH counter gets the explicit
branchless explanation the importer and release admission require. With
--classfiles every compiled class (except module-info) must appear in the report,
so a class that never loaded cannot silently drop out of the inventory.
"""
from __future__ import annotations

import argparse
from pathlib import Path, PurePosixPath
import sys
import xml.etree.ElementTree as ET

FIELDS = ("id", "class", "method", "descriptor", "source_path", "branchless_explanation")
BRANCHLESS = ("no conditional bytecode branch: JaCoCo reports no BRANCH counter for this "
              "method, so LINE coverage is its complete measure")


def derive(report: Path, root: Path, source_root: str) -> list[dict]:
    xml = ET.parse(report).getroot()
    if xml.tag != "report":
        raise ValueError("JaCoCo XML report expected")
    prefix = PurePosixPath(source_root)
    if prefix.is_absolute() or ".." in prefix.parts or not str(prefix).startswith(("m3/", "src/")):
        raise ValueError("source root must be repository-relative under m3/ or src/")
    rows = {}
    for package in xml.findall("package"):
        for klass in package.findall("class"):
            owner, source = klass.get("name"), klass.get("sourcefilename")
            if not owner or not source:
                raise ValueError("class without name or source file in report")
            source_path = str(prefix / package.get("name", "") / source)
            if not (root / source_path).is_file():
                raise ValueError("reported class has no source under the root: " + owner)
            for method in klass.findall("method"):
                name, desc = method.get("name"), method.get("desc")
                if not name or not desc:
                    raise ValueError("method without name or descriptor: " + owner)
                ident = owner.replace("/", ".") + "#" + name + desc
                if ident in rows:
                    raise ValueError("duplicate method key: " + ident)
                kinds = {counter.get("type") for counter in method.findall("counter")}
                if "LINE" not in kinds:
                    raise ValueError("method without LINE counter: " + ident)
                rows[ident] = {"id": ident, "class": owner, "method": name, "descriptor": desc,
                               "source_path": source_path,
                               "branchless_explanation": "" if "BRANCH" in kinds else BRANCHLESS}
    if not rows:
        raise ValueError("report has no measured methods")
    return [rows[key] for key in sorted(rows)]


def unreported_classes(rows: list[dict], classfiles: Path) -> list[str]:
    reported = {row["class"] for row in rows}
    compiled = {path.relative_to(classfiles).with_suffix("").as_posix()
                for path in classfiles.rglob("*.class") if path.name != "module-info.class"}
    return sorted(compiled - reported)


def render(rows: list[dict]) -> str:
    lines = ["\t".join(FIELDS)]
    for row in rows:
        values = [row[field] for field in FIELDS]
        if any("\t" in value or "\n" in value for value in values):
            raise ValueError("TSV control character in " + row["id"])
        lines.append("\t".join(values))
    return "\n".join(lines) + "\n"


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--report", type=Path, required=True, help="JaCoCo XML report")
    parser.add_argument("--root", type=Path, default=Path("."), help="repository root")
    parser.add_argument("--source-root", default="m3/core/src")
    parser.add_argument("--classfiles", type=Path, help="compiled classes that must all be reported")
    target = parser.add_mutually_exclusive_group(required=True)
    target.add_argument("--out", type=Path, help="write the inventory TSV")
    target.add_argument("--check", type=Path, help="fail unless this TSV is byte-identical")
    args = parser.parse_args(argv)
    try:
        rows = derive(args.report, args.root, args.source_root)
        if args.classfiles is not None:
            missing = unreported_classes(rows, args.classfiles)
            if missing:
                raise ValueError("compiled classes absent from the report: " + ", ".join(missing))
        text = render(rows)
    except (ET.ParseError, ValueError, OSError) as exc:
        parser.error(str(exc))
    branchless = sum(1 for row in rows if row["branchless_explanation"])
    if args.check is not None:
        same = args.check.is_file() and args.check.read_bytes() == text.encode("utf-8")
        print(f"{'MATCH' if same else 'DIFFERS'}: {len(rows)} atoms ({branchless} branchless) vs {args.check}")
        return 0 if same else 1
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_bytes(text.encode("utf-8"))
    print(f"Derived {len(rows)} atoms ({branchless} branchless) from {args.report}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
