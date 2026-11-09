#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""relocate.py <donor-checkout> [--check] [--commit=SHA] : COMPILER_REFERENCE_RELOCATE v1 (census2 P2-132 + P2-136).
--commit names the donor commit when the checkout is an exported tree without .git.

Relocates the javac reference-flow family (closure of M3CRefPathCertainty and M3CompilerReferenceCensus: 7 main
classes, 3 donor proofs, 3 JUnit wrappers) from the donor checkout of hsoliwal/com.synexia into this module. Every
donor name lives in RELOCATION_MAP.tsv (a lineage table, outside the JDK-naming rewrite), so this recipe stays a fixed
point of that rewrite. Rules, in order, per FILE row:
  1. the PACKAGE row: the package line, exactly once;
  2. each TYPE row: its import line, exactly the count recorded in the FILE row, then every remaining simple-name use
     of the donor type (not preceded by a dot or identifier character) to the target simple name;
  3. nothing else: all other bytes (SPDX attribution lines included) are kept.
Refuses when a rule site count differs, when a donor package or donor simple type name survives outside comments
and string literals, or (with --check) when the module's sources differ from the recipe output. PROVENANCE.tsv
records donor blob and sha256 of every input and sha256 of every output."""
import hashlib
import re
import subprocess
import sys
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1]
ROWS = [line.split("\t") for line in (MODULE / "recipe" / "RELOCATION_MAP.tsv").read_text(encoding="utf-8").splitlines()]
COL = {name: i for i, name in enumerate(ROWS[0])}
MASK = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\])*"', re.S)


def rows(kind):
    return [r for r in ROWS[1:] if r[COL["kind"]] == kind]


DONOR_MODULE = rows("MODULE")[0][COL["donor"]]
DONOR_PKG, TARGET_PKG = rows("PACKAGE")[0][COL["donor"]], rows("PACKAGE")[0][COL["target"]]
TYPES = [(r[COL["donor"]], r[COL["target"]]) for r in rows("TYPE")]


def sub(text, old, new, name, count):
    n = text.count(old)
    if n != count:
        sys.exit(f"REFUSE {name}: site {old!r} found {n}x, expected {count}")
    return text.replace(old, new)


def relocate(row, text):
    name = row[COL["donor"]]
    text = sub(text, f"package {DONOR_PKG};", f"package {TARGET_PKG};", name, 1)
    for (donor, target), column in zip(TYPES, ("sites_ProgressMonitor_import", "sites_ProgressState_import")):
        text = sub(text, f"import {donor};", f"import {target};", name, int(row[COL[column]]))
        simple_donor, simple_target = donor.rsplit(".", 1)[1], target.rsplit(".", 1)[1]
        text = re.sub(rf"(?<![\w.]){simple_donor}\b", simple_target, text)
    code = MASK.sub(" ", text)
    left = [t for t in [DONOR_PKG.split(".")[0] + "." + DONOR_PKG.split(".")[1]] + [d.rsplit(".", 1)[1] for d, _ in TYPES]
            if re.search(rf"(?<![\w.]){re.escape(t)}\b", code)]
    if left:
        sys.exit(f"REFUSE {name}: donor names survive in code: {left}")
    return text


def main():
    donor = Path(sys.argv[1])
    check = "--check" in sys.argv[2:]
    given = [a.split("=", 1)[1] for a in sys.argv[2:] if a.startswith("--commit=")]
    commit = given[0] if given else subprocess.run(["git", "-C", str(donor), "rev-parse", "HEAD"],
                                                   capture_output=True, text=True).stdout.strip()
    provenance = ["donor_repo\tdonor_commit\tdonor_path\tdonor_blob\tdonor_sha256\ttarget_path\ttarget_sha256"]
    changed = 0
    for row in rows("FILE"):
        side = row[COL["side"]]
        rel = f"{DONOR_MODULE}/src/{side}/java/{DONOR_PKG.replace('.', '/')}/{row[COL['donor']]}.java"
        data = (donor / rel).read_bytes()
        out = relocate(row, data.decode("utf-8")).encode("utf-8")
        target = f"src/{side}/java/{TARGET_PKG.replace('.', '/')}/{row[COL['target']]}.java"
        blob = subprocess.run(["git", "-C", str(donor), "hash-object", rel], capture_output=True, text=True).stdout.strip()
        provenance.append("\t".join(["hsoliwal/com.synexia", commit or "-", rel, blob or "-",
                                     hashlib.sha256(data).hexdigest(), target, hashlib.sha256(out).hexdigest()]))
        path = MODULE / target
        if check:
            if not path.is_file() or path.read_bytes() != out:
                changed += 1
                print(f"DRIFT {target}")
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(out)
    if check:
        print(f"changed={changed}")
        sys.exit(1 if changed else 0)
    (MODULE / "PROVENANCE.tsv").write_bytes(("\n".join(provenance) + "\n").encode("utf-8"))
    print(f"relocated={len(rows('FILE'))}")


main()
