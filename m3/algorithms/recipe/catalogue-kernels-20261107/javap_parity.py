#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Gate 5 of recipe/catalogue-kernels-20261107: javap -p parity of the kept members, modulo names and declared drops.

Derived from recipe/algorithm-extras-20261106/javap_parity.py (same comparison) with three additions: only FILE rows
under src/main/java are compared (the test rows have no donor compile); a DROP_LINE row whose note starts with
member:<name> removes that member's javap line (a JNI method or a field the plan drops by line); and the declared
javap effect of an EDIT row (JAVAP_EDITS below: E1 makes the progress helper package-private) is applied to the
normalized donor text. For every class file compiled from a FILE row's donor source (differential.sh leaves the donor
compile in <scratch>/classes), javap -p of the donor class, normalized by the PACKAGE and TYPE rows of
RELOCATION_MAP.tsv and with the members the DROP_METHOD and member: DROP_LINE rows remove (their lambda bodies too)
taken out, must equal javap -p of the relocated class (target/classes). A donor class file of a DROP_TYPE nested type
has no relocated counterpart and is counted as dropped. Carriage returns are removed on both sides. Prints one line
per class and JAVAP_PARITY PASS|FAIL. usage: python -I javap_parity.py <scratch-dir> [--javap PATH]
"""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

RECIPE_DIR = Path(__file__).resolve().parent
MODULE = RECIPE_DIR.parent.parent
PARAM_STRIP = re.compile(r"\b(?:[a-z]\w*\.)+")
LAMBDA = re.compile(r"(lambda\$\w+\$)\d+")
# the javap effect of an EDIT row of RELOCATION_MAP.tsv: (old, new) on the normalized donor text
JAVAP_EDITS = {
    "E1_PACKAGE_PRIVATE": ("public final class jdk.m3.algorithm.ProgressChecks {",
                           "final class jdk.m3.algorithm.ProgressChecks {"),
}


def read_map() -> dict:
    rows = [line.split("\t") for line in (RECIPE_DIR / "RELOCATION_MAP.tsv").read_text(encoding="utf-8").splitlines()
            if line and not line.startswith("#") and not line.startswith("kind\t")]
    mapping = {"FILE": [], "PACKAGE": {}, "TYPE": {}, "DROP_METHOD": {}, "DROP_TYPE": {}, "DROP_FIELD": {}}
    for kind, donor, target, *note in rows:
        if kind == "FILE" and target.startswith("src/main/java/"):
            mapping["FILE"].append((donor, target))
        elif kind == "DROP_LINE" and note and note[0].startswith("member:"):
            mapping["DROP_FIELD"].setdefault(donor, []).append(note[0].split()[0][len("member:"):])
        elif kind in ("PACKAGE", "TYPE"):
            mapping[kind][donor] = target
        elif kind in ("DROP_METHOD", "DROP_TYPE"):
            name, paren, params = donor.partition("(")
            owner, _dot, member = name.rpartition(".")
            mapping[kind].setdefault(owner, []).append((member, params.rstrip(")") if paren else None))
    # an overload selector names donor types; javap text is compared after the TYPE renames
    for kind in ("DROP_METHOD", "DROP_TYPE"):
        for owner, members in mapping[kind].items():
            mapping[kind][owner] = [(member, None if selector is None else normalize(selector, mapping))
                                    for member, selector in members]
    return mapping


def normalize(text: str, mapping: dict) -> str:
    for donor, target in mapping["PACKAGE"].items():
        if donor != target:
            text = text.replace(donor + ".", target + ".")
    for donor, target in mapping["TYPE"].items():
        if donor != target:
            text = re.sub(r"\b" + re.escape(donor) + r"\b", target, text)
    return text


def simple_params(params: str) -> str:
    while re.search(r"<[^<>]*>", params):
        params = re.sub(r"<[^<>]*>", "", params)
    parts = [PARAM_STRIP.sub("", part.strip()) for part in params.split(",") if part.strip()]
    return ",".join(part.split("$")[-1] for part in parts)


def dropped(line: str, drops: list) -> bool:
    member = re.match(r"^\s+.*?([\w$]+)\((.*)\)(?: throws [\w.$, ]+)?;$", line)
    if not member:
        return False
    name, params = member.group(1), member.group(2)
    for drop, selector in drops:
        if name.startswith("lambda$" + drop + "$"):
            return True
        if name == drop and (selector is None or simple_params(params) == selector):
            return True
    return False


def javap(tool: str, path: Path) -> str:
    return subprocess.run([tool, "-p", str(path)], check=True, capture_output=True, text=True).stdout.replace("\r",
                                                                                                             "")


def main(argv: list) -> int:
    scratch = Path(argv[0])
    tool = argv[argv.index("--javap") + 1] if "--javap" in argv else "javap"
    mapping = read_map()
    out_dir = scratch / "javap"
    out_dir.mkdir(parents=True, exist_ok=True)
    same = differ = dropped_classes = 0
    for donor_path, target in mapping["FILE"]:
        donor_type = Path(donor_path).stem
        donor_pkg = re.sub(r"^(?:.*/)?src/main/java/", "", str(Path(donor_path).parent).replace("\\", "/"))
        target_type = Path(target).stem
        target_pkg = re.sub(r"^src/main/java/", "", str(Path(target).parent).replace("\\", "/"))
        nested_drops = {member for member, _sel in mapping["DROP_TYPE"].get(donor_type, ())}
        classes = sorted((scratch / "classes" / donor_pkg).glob(donor_type + ".class")) \
            + sorted((scratch / "classes" / donor_pkg).glob(donor_type + "$*.class"))
        for donor_class in classes:
            inner = donor_class.stem[len(donor_type):]
            if inner and inner[1:].split("$")[0] in nested_drops:
                dropped_classes += 1
                print("DROPPED %s%s" % (target_type, inner))
                continue
            donor_text = normalize(javap(tool, donor_class), mapping)
            for old, new in JAVAP_EDITS.values():
                donor_text = donor_text.replace(old, new)
            drops = mapping["DROP_METHOD"].get(donor_type, []) if not inner else []
            fields = mapping["DROP_FIELD"].get(donor_type, []) if not inner else []
            donor_lines = [l for l in donor_text.splitlines() if not dropped(l, drops)
                           and not any(re.search(r"\b%s[;(]" % re.escape(name), l) for name in fields)]
            target_class = MODULE / "target" / "classes" / target_pkg / (target_type + inner + ".class")
            target_lines = javap(tool, target_class).splitlines()
            (out_dir / ("d-%s%s.txt" % (target_type, inner))).write_text("\n".join(donor_lines) + "\n",
                                                                          encoding="utf-8")
            (out_dir / ("r-%s%s.txt" % (target_type, inner))).write_text("\n".join(target_lines) + "\n",
                                                                          encoding="utf-8")
            # javac numbers lambda bodies with one class-wide counter, so dropping a method with a lambda renumbers
            # the later ones (lambda$combinationSum$2 -> $1): the counter is compared as #
            donor_body = [LAMBDA.sub(r"\1#", l) for l in donor_lines if not l.startswith("Compiled from")]
            target_body = [LAMBDA.sub(r"\1#", l) for l in target_lines if not l.startswith("Compiled from")]
            if donor_body == target_body:
                same += 1
                print("SAME %s%s" % (target_type, inner))
            else:
                differ += 1
                print("DIFFERS %s%s" % (target_type, inner))
                for d, t in zip(donor_body, target_body):
                    if d != t:
                        print("  donor   : " + d)
                        print("  relocated: " + t)
                        break
    status = "PASS" if differ == 0 else "FAIL"
    print("JAVAP_PARITY %s classes=%d differ=%d dropped_nested=%d" % (status, same, differ, dropped_classes))
    return 0 if differ == 0 else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
