#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""The dependents closure of a Maven artifact, read from the HEAD tree (not from the checkout).

usage: pom_dependents.py <repo root> <artifactId> [--require-checkout]
Every pom.xml of the HEAD tree outside evidence segments that declares <artifactId> inside a <dependency> block is a
dependent; its module directory is printed as DEPENDENT <dir> <pom path>. With --require-checkout the run exits 2 and
prints MISSING <dir> for every dependent whose pom is not in the working tree (sparse checkout): a rename or reference
rewrite over the checkout would miss that module (RECIPE_MEMORY 2026-11-08, PATTERN:RENAME_COMPLETENESS), so the
caller adds the module to the sparse set and gates it with a consumer build before rewriting anything."""
from __future__ import annotations

import re
import subprocess
import sys
from pathlib import Path

EVIDENCE = re.compile(r"(^|/)(recipe-custody|custody|evidence|qualification|recipes?|retained-variants|proofs?|snapshots?|"
                      r"before|after|postimages?|preimages?|donors?|intakes?|fixtures?|golden|expected|samples?|vendor|target)/")
DEPENDENCY = re.compile(r"<dependency>(.*?)</dependency>", re.S)
ARTIFACT = re.compile(r"<artifactId>\s*([^<\s]+)\s*</artifactId>")


def git(root: Path, *args: str) -> str:
    return subprocess.run(["git", "-C", str(root), *args], capture_output=True, text=True, check=True).stdout


def pom_paths(root: Path) -> list[str]:
    return [p for p in git(root, "ls-tree", "-r", "HEAD", "--name-only").splitlines()
            if p.endswith("pom.xml") and not EVIDENCE.search(p)]


def dependents(root: Path, artifact: str) -> list[tuple[str, str]]:
    """(module dir, pom path) of every HEAD pom that depends on the artifact; the artifact's own pom is not a dependent."""
    marker = f"<artifactId>{artifact}</artifactId>"
    candidates = git(root, "grep", "-l", "--fixed-strings", marker, "HEAD", "--", *pom_paths(root)) if pom_paths(root) else ""
    found = []
    for line in candidates.splitlines():
        path = line.split(":", 1)[1]
        text = git(root, "show", f"HEAD:{path}")
        if any(m.group(1) == artifact for block in DEPENDENCY.findall(text) for m in ARTIFACT.finditer(block)):
            found.append((str(Path(path).parent.as_posix()), path))
    return sorted(found)


def missing_from_checkout(root: Path, found: list[tuple[str, str]]) -> list[str]:
    return [d for d, pom in found if not (root / pom).exists()]


def main(argv: list[str]) -> int:
    if len(argv) < 3:
        print(__doc__)
        return 1
    root, artifact = Path(argv[1]).resolve(), argv[2]
    found = dependents(root, artifact)
    for d, pom in found:
        print(f"DEPENDENT\t{d}\t{pom}")
    missing = missing_from_checkout(root, found) if "--require-checkout" in argv else []
    for d in missing:
        print(f"MISSING\t{d}")
    print(f"POM_DEPENDENTS artifact={artifact} dependents={len(found)} missing_from_checkout={len(missing)}")
    return 2 if missing else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
