# Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0
"""Hostile synthetic fixtures for the canonical A3 descriptor preflight."""
from __future__ import annotations

from pathlib import Path
import tempfile
import unittest

from verify_canonical_atom_descriptors import (
    BASE, CONSUMER, CONSUMER_TEST, DESCRIPTOR_DIR, DESCRIPTORS,
    OWNER_DIR, PACKAGE, REGISTRY, STRING_WORKFLOW, TOOLING_WORKFLOW, verify,
)


class CanonicalAtomDescriptorTest(unittest.TestCase):
    def setUp(self) -> None:
        self.sandbox = tempfile.TemporaryDirectory()
        self.addCleanup(self.sandbox.cleanup)
        self.root = Path(self.sandbox.name)
        self.owners = sorted({name for names in DESCRIPTORS.values() for name in names})
        for descriptor, classes in DESCRIPTORS.items():
            refs = "\n".join("  - " + PACKAGE + "." + name for name in classes)
            self.write(f"{DESCRIPTOR_DIR}/{descriptor}",
                       "type: specs.openrewrite.org/v1beta/recipe\n"
                       "recipeList:\n" + refs + "\n")
        for name in self.owners:
            self.write(f"{OWNER_DIR}/{name}.java",
                       "package com.synexia.rewrite.atom;\n"
                       f"public final class {name} {{}}\n")
        self.write(REGISTRY,
                   "\n".join('Map.entry("' + PACKAGE + "." + name +
                             '\", fixed(M3EditScope.FILE))' for name in self.owners))
        imports = "\n".join(f"import {PACKAGE}.{name};" for name in self.owners)
        order = DESCRIPTORS["m3-java21-convergence.yml"]
        self.write(CONSUMER, imports + "\n" +
                   "\n".join("new " + name + "()" for name in order) +
                   "\nnew RemoveUnusedImports()\n")
        self.write(CONSUMER_TEST, imports + "\n")
        self.write(STRING_WORKFLOW,
                   "\n".join(f"{DESCRIPTOR_DIR}/{name}" for name in DESCRIPTORS) +
                   "\n" + CONSUMER + "\n" + CONSUMER_TEST +
                   "\nmake images\nRun M3 String backing jtreg\n")
        self.write(TOOLING_WORKFLOW,
                   f"python3 {BASE}/test_canonical_atom_descriptors.py\n"
                   "mvn clean verify\n")

    def write(self, relative: str, text: str) -> None:
        path = self.root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")

    def edit(self, relative: str, before: str, after: str) -> None:
        path = self.root / relative
        original = path.read_text(encoding="utf-8")
        self.assertIn(before, original)
        path.write_text(original.replace(before, after), encoding="utf-8")

    def test_accepted_exact_canonical_repository(self) -> None:
        self.assertEqual((3, 6, 4), verify(self.root))
        self.assertEqual((3, 6, 4), verify(self.root))

    def test_legacy_namespace_refuses_even_in_unrelated_descriptor(self) -> None:
        self.write(DESCRIPTOR_DIR + "/extra.yml", "com.m3.rewrite.atom.M3Old\n")
        with self.assertRaisesRegex(ValueError, "obsolete recipe package"):
            verify(self.root)

    def test_unknown_class_refuses(self) -> None:
        self.edit(f"{DESCRIPTOR_DIR}/m3-atom-pattern.yml",
                  "M3AtomizePureIntReturnRecipe", "M3ImaginaryRecipe")
        with self.assertRaisesRegex(ValueError, "owner/order drift"):
            verify(self.root)

    def test_order_change_refuses(self) -> None:
        original = DESCRIPTORS["m3-java21-convergence.yml"]
        self.edit(f"{DESCRIPTOR_DIR}/m3-java21-convergence.yml",
                  "  - " + PACKAGE + "." + original[0] + "\n"
                  "  - " + PACKAGE + "." + original[1],
                  "  - " + PACKAGE + "." + original[1] + "\n"
                  "  - " + PACKAGE + "." + original[0])
        with self.assertRaisesRegex(ValueError, "owner/order drift"):
            verify(self.root)

    def test_missing_implementation_refuses(self) -> None:
        (self.root / OWNER_DIR / "M3DocumentPureIntAtomRecipe.java").unlink()
        with self.assertRaisesRegex(ValueError, "source missing"):
            verify(self.root)

    def test_registry_scope_drift_refuses(self) -> None:
        self.edit(REGISTRY, "fixed(M3EditScope.FILE)", "fixed(M3EditScope.MODULE)")
        with self.assertRaisesRegex(ValueError, "FILE-scope admission"):
            verify(self.root)

    def test_string_workflow_trigger_removal_refuses(self) -> None:
        self.edit(STRING_WORKFLOW, "m3-file-inventory.yml", "m3-file-inventory-removed.yml")
        with self.assertRaisesRegex(ValueError, "missing A3 descriptor trigger"):
            verify(self.root)

    def test_maven_gate_removal_refuses(self) -> None:
        self.edit(TOOLING_WORKFLOW, "clean verify", "skip")
        with self.assertRaisesRegex(ValueError, "Maven verification gate removed"):
            verify(self.root)


if __name__ == "__main__":
    unittest.main()
