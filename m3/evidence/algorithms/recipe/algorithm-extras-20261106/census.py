#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Census gate of recipe/algorithm-extras-20261106: checks METHOD_CENSUS.tsv against the class files it describes.

1. javap -public of the whole donor catalogue compile (overlap.sh leaves it in <scratch>/overlap/donor): every public
   static method of the 21 donor files is exactly one STATIC row (same name, simplified erased parameters), and every
   STATIC row is such a method.
2. Every owner named by an OVERLAP_LANDED or OVERLAP_OTHER_OWNER row exists: a jdk.m3.algorithm owner method in the
   module's target/classes (same name and simplified erased parameters, type variables erased to Object); other owners
   (jdk.m3.collections, jdk.m3.tools.cpu, java.base jdk.internal.m3) by class and, when the row names one, method
   name and parameters on --owner-cp.
3. RELOCATION_MAP.tsv agrees: a non-ported member of a relocated file has exactly one DROP_METHOD or DROP_TYPE row
   naming the same owner; a ported member has none; every ABSORB row is ported.
Prints CENSUS PASS|FAIL with the counts per verdict. usage:
python -I census.py --donor-classes DIR --landed-classes DIR --owner-cp CP [--javap PATH]
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

RECIPE_DIR = Path(__file__).resolve().parent


def map_packages() -> list:
    """The donor packages of the PACKAGE rows of RELOCATION_MAP.tsv (no donor package name is spelled in this file,
    so the jdk-naming recipe leaves it at its fixed point)."""
    return [line.split("\t")[1] for line in (RECIPE_DIR / "RELOCATION_MAP.tsv").read_text(encoding="utf-8")
            .splitlines() if line.startswith("PACKAGE\t")]


DONOR_PACKAGE = next(package for package in map_packages() if package.endswith("algorithms"))
VERDICTS = ("ABSORB", "OVERLAP_LANDED", "OVERLAP_OTHER_OWNER")
STRIP = re.compile(r"\b(?:" + "|".join(re.escape(package) for package in map_packages())
                   + r"|jdk\.m3\.algorithm|java\.(?:lang|util|math))\.")


def simplify(params: str) -> str:
    while re.search(r"<[^<>]*>", params):
        params = re.sub(r"<[^<>]*>", "", params)
    types = [STRIP.sub("", part.strip()) for part in params.split(",") if part.strip()]
    return ",".join("Object" if re.fullmatch(r"[A-Z]", t) else t for t in types)


def javap_methods(javap: str, classpath: str, classes: list[str]) -> dict:
    """{class simple name (with $): set of (static?, name, simplified params)}"""
    out = subprocess.run([javap, "-public", "-cp", classpath, *classes], check=True, capture_output=True,
                         text=True).stdout
    found: dict = {}
    current = None
    for line in out.splitlines():
        head = re.match(r"^(?:public )?(?:final |abstract )*(?:class|interface|enum) ([\w.$]+)", line.strip())
        if head and not line.startswith(" "):
            current = head.group(1).rsplit(".", 1)[-1]
            found.setdefault(current, set())
            continue
        member = re.match(r"^\s+public (static )?.*?(\w+)\((.*)\);$", line)
        if member and current:
            found[current].add((bool(member.group(1)), member.group(2), simplify(member.group(3))))
    return found


def class_names(root: Path, package: str) -> list[str]:
    folder = root / package.replace(".", "/")
    return sorted(package + "." + p.stem for p in folder.glob("*.class"))


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--donor-classes", required=True)
    parser.add_argument("--landed-classes", required=True)
    parser.add_argument("--owner-cp", required=True)
    parser.add_argument("--javap", default="javap")
    args = parser.parse_args(argv)
    problems = []
    rows = [line.split("\t") for line in (RECIPE_DIR / "METHOD_CENSUS.tsv").read_text(encoding="utf-8").splitlines()
            if line and not line.startswith("#") and not line.startswith("donor_class\t")]
    donor = javap_methods(args.javap, args.donor_classes, class_names(Path(args.donor_classes), DONOR_PACKAGE))
    statics = {(cls, name + "(" + params + ")") for cls, members in donor.items()
               for is_static, name, params in members if is_static}
    census_statics = {(r[0], r[1]) for r in rows if r[2] == "STATIC"}
    for key in sorted(statics - census_statics):
        problems.append("donor public static without a census row: %s.%s" % key)
    for key in sorted(census_statics - statics):
        problems.append("census row without a donor public static: %s.%s" % key)
    landed = javap_methods(args.javap, args.landed_classes, class_names(Path(args.landed_classes),
                                                                         "jdk.m3.algorithm"))
    owners_checked = 0
    for r in rows:
        verdict, owner, ported = r[5], r[7], r[8]
        if verdict not in VERDICTS:
            problems.append("unknown verdict %s on %s.%s" % (verdict, r[0], r[1]))
        if verdict == "ABSORB":
            if ported != "yes":
                problems.append("ABSORB row not ported: %s.%s" % (r[0], r[1]))
            continue
        name_part, paren, params = owner.partition("(")
        owner_class, _dot, method = name_part.rpartition(".") if paren else (name_part, "", "")
        simple = owner_class.rsplit(".", 1)[-1]
        if owner_class.startswith("jdk.m3.algorithm."):
            members = landed.get(simple, set())
        else:
            try:
                members = javap_methods(args.javap, args.owner_cp, [owner_class]).get(simple, set())
            except subprocess.CalledProcessError:
                members = None
        if members is None:
            problems.append("owner class not found: %s (%s.%s)" % (owner_class, r[0], r[1]))
            continue
        if paren and not any(n == method and p == simplify(params.rstrip(")")) for _s, n, p in members):
            problems.append("owner method not found: %s (%s.%s)" % (owner, r[0], r[1]))
        owners_checked += 1
    problems += map_problems(rows)
    counts = {}
    for r in rows:
        key = ("STATIC" if r[2] == "STATIC" else "MEMBER") + ":" + r[5] + ":ported=" + r[8]
        counts[key] = counts.get(key, 0) + 1
    summary = " ".join("%s=%d" % item for item in sorted(counts.items()))
    for problem in problems:
        print("PROBLEM " + problem)
    print("CENSUS %s statics=%d rows=%d owners_checked=%d %s" % ("PASS" if not problems else "FAIL",
                                                                len(census_statics), len(rows), owners_checked,
                                                                summary))
    return 1 if problems else 0


def map_problems(rows: list) -> list:
    problems = []
    drops = {}
    relocated = set()
    for line in (RECIPE_DIR / "RELOCATION_MAP.tsv").read_text(encoding="utf-8").splitlines():
        f = line.split("\t")
        if f[0] == "FILE":
            relocated.add(Path(f[1]).stem)
        elif f[0] in ("DROP_METHOD", "DROP_TYPE"):
            name = f[1].split("(")[0]
            drops.setdefault(name, []).append((f[1], f[2]))
    for r in rows:
        file_type = r[0].split("$")[0]
        if file_type not in relocated:
            continue
        if r[2] == "TYPE":
            key = file_type + "." + r[0].split("$", 1)[1] if "$" in r[0] else None
        else:
            key = file_type + "." + r[1].split("(")[0] if "$" not in r[0] else None
        hits = drops.get(key, []) if key else []
        if r[8] == "no":
            if not hits:
                problems.append("dropped member without a DROP row: %s.%s" % (r[0], r[1]))
            elif not any(owner == r[7] for _k, owner in hits):
                problems.append("DROP row owner differs from the census owner: %s.%s" % (r[0], r[1]))
        elif hits and r[2] == "TYPE":
            problems.append("ported type has a DROP_TYPE row: %s" % r[0])
        elif hits and not any("(" in k for k, _o in hits):
            problems.append("ported member has a DROP row: %s.%s" % (r[0], r[1]))
    return problems


if __name__ == "__main__":
    sys.exit(main())
