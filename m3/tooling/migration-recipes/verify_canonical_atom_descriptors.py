# Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0
"""Fail closed when a product A3 descriptor points away from canonical Synexia atom recipes.

This is a source-level admission gate, not an OpenRewrite or JDK runtime qualification.
"""
from __future__ import annotations

import argparse
from pathlib import Path
import re

BASE = "m3/tooling/migration-recipes"
PACKAGE = "com.synexia.rewrite.atom"
DESCRIPTORS = {
    "m3-atom-pattern.yml": ("M3AtomizePureIntReturnRecipe",),
    "m3-file-inventory.yml": ("M3InventoryPureIntAtomCandidates",),
    "m3-java21-convergence.yml": (
        "M3InventoryPureIntAtomCandidates",
        "M3AtomizePureIntReturnRecipe",
        "M3PatternizePureIntAtomRecipe",
        "M3DocumentPureIntAtomRecipe",
    ),
}
OWNER_DIR = f"{BASE}/src/main/java/com/synexia/rewrite/atom"
REGISTRY = f"{BASE}/src/main/java/com/m3/rewrite/scope/M3RecipeScopeRegistry.java"
CONSUMER = f"{BASE}/src/main/java/com/m3/rewrite/M3Java21ConvergenceRecipe.java"
CONSUMER_TEST = f"{BASE}/src/test/java/com/m3/rewrite/M3Java21ConvergenceRecipeTest.java"
DESCRIPTOR_DIR = f"{BASE}/src/main/resources/META-INF/rewrite"
STRING_WORKFLOW = ".github/workflows/mindex-string-backing.yml"
TOOLING_WORKFLOW = ".github/workflows/m3-tooling-recipes.yml"


def required(root: Path, relative: str) -> str:
    path = root / relative
    if not path.is_file():
        raise ValueError("required source missing: " + relative)
    return path.read_text(encoding="utf-8")


def targets(text: str, relative: str) -> tuple[str, ...]:
    """Extract exactly the direct class-name entries of one simple declarative recipeList."""
    if text.count("recipeList:") != 1:
        raise ValueError("ambiguous recipeList: " + relative)
    match = re.search(r"(?m)^recipeList:\s*\n((?:[ \t]+- [^\n]+\n?)+)", text)
    if not match:
        raise ValueError("unreadable recipeList: " + relative)
    entries = []
    for line in match.group(1).splitlines():
        element = re.fullmatch(r"[ \t]{2}- ([a-zA-Z0-9_.]+)", line)
        if element is None:
            raise ValueError("non-class recipe entry: " + relative)
        entries.append(element.group(1))
    return tuple(entries)


def verify(root: Path) -> tuple[int, int, int]:
    root = root.resolve()
    descriptor_dir = root / DESCRIPTOR_DIR
    if not descriptor_dir.is_dir():
        raise ValueError("descriptor directory missing")
    # Inspect all installed YAML descriptors, not only the three positive witnesses.
    for source in sorted(descriptor_dir.glob("*.yml")):
        if "com.m3.rewrite.atom." in source.read_text(encoding="utf-8"):
            raise ValueError("obsolete recipe package in " + source.name)

    refs = []
    for name, classes in DESCRIPTORS.items():
        relative = f"{DESCRIPTOR_DIR}/{name}"
        actual = targets(required(root, relative), relative)
        expected = tuple(f"{PACKAGE}.{owner}" for owner in classes)
        if actual != expected:
            raise ValueError("declarative owner/order drift: " + relative)
        refs.extend(expected)

    unique = set(refs)
    registry = required(root, REGISTRY)
    production = required(root, CONSUMER)
    test = required(root, CONSUMER_TEST)
    for qualified in sorted(unique):
        class_name = qualified.rsplit(".", 1)[-1]
        implementation = required(root, f"{OWNER_DIR}/{class_name}.java")
        if not re.search(r"(?m)^package\s+com\.synexia\.rewrite\.atom\s*;", implementation):
            raise ValueError("canonical implementation package drift: " + class_name)
        if not re.search(r"\bpublic\s+final\s+class\s+" + re.escape(class_name) + r"\b", implementation):
            raise ValueError("canonical implementation type drift: " + class_name)
        scope = r'Map\.entry\(\s*"' + re.escape(qualified) + r'"\s*,\s*fixed\(M3EditScope\.FILE\)\s*\)'
        if not re.search(scope, registry):
            raise ValueError("missing FILE-scope admission: " + qualified)
        if f"import {qualified};" not in production or f"import {qualified};" not in test:
            raise ValueError("Java convergence consumer import drift: " + qualified)
    for name, text in ((CONSUMER, production), (CONSUMER_TEST, test)):
        if "import com.m3.rewrite.atom." in text:
            raise ValueError("legacy Java import: " + name)

    expected_order = DESCRIPTORS["m3-java21-convergence.yml"]
    positions = [production.find("new " + name + "()") for name in expected_order]
    if any(pos < 0 for pos in positions) or positions != sorted(positions) or len(set(positions)) != 4:
        raise ValueError("Java convergence DAG order drift")
    if "new RemoveUnusedImports()" not in production:
        raise ValueError("existing import-cleanup contract removed")

    string_workflow = required(root, STRING_WORKFLOW)
    for name in DESCRIPTORS:
        if f"{DESCRIPTOR_DIR}/{name}" not in string_workflow:
            raise ValueError("String workflow missing A3 descriptor trigger: " + name)
    for owner in (CONSUMER, CONSUMER_TEST):
        if owner not in string_workflow:
            raise ValueError("String workflow missing A3 Java trigger: " + owner)
    if "make images" not in string_workflow or "Run M3 String backing jtreg" not in string_workflow:
        raise ValueError("JDK image or jtreg proof gate removed")
    tooling_workflow = required(root, TOOLING_WORKFLOW)
    if f"python3 {BASE}/test_canonical_atom_descriptors.py" not in tooling_workflow:
        raise ValueError("Maven tooling workflow missing preflight verifier")
    if "clean verify" not in tooling_workflow:
        raise ValueError("Maven verification gate removed")
    return len(DESCRIPTORS), len(refs), len(unique)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[3])
    args = parser.parse_args()
    try:
        files, references, classes = verify(args.root)
    except (OSError, ValueError) as failure:
        raise SystemExit("M3_ATOM_DESCRIPTOR_AUTHORITY_FAIL|" + str(failure)) from failure
    print(f"M3_ATOM_DESCRIPTOR_AUTHORITY_PASS|descriptors={files}|refs={references}|classes={classes}")


if __name__ == "__main__":
    main()
