#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0

import importlib.util
import pathlib
import shutil
import sys
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[2]
MODULE_PATH = pathlib.Path(__file__).with_name("adapted_jbs_proof.py")
SPEC = importlib.util.spec_from_file_location("m3_adapted_jbs_proof_tested", MODULE_PATH)
MODULE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


class AdaptedJbsProofTest(unittest.TestCase):
    def test_current_tree_wave_is_exact_and_authority_free(self):
        work, targets = MODULE.compile_root(ROOT)
        self.assertEqual(7, len(work))
        self.assertGreaterEqual(len(targets), 20)
        self.assertEqual(list(range(7)), [row.order for row in work])
        self.assertEqual(
            {
                "JDK-8347112",
                "JDK-8357439",
                "JDK-8359706",
                "JDK-8364182",
                "JDK-8367584",
                "JDK-8368692",
                "JDK-8374808",
            },
            {row.work_id for row in work},
        )
        self.assertRegex(MODULE.root_hash(work, targets), r"^[0-9a-f]{64}$")
        self.assertTrue(all(row.linux_build for row in work))
        self.assertEqual(
            ["JDK-8359706"],
            [row.work_id for row in work if row.macos_build],
        )

    def test_vm_symbols_successor_is_explicit_not_generic_drift_tolerance(self):
        _, targets = MODULE.compile_root(ROOT)
        vm = [
            row
            for row in targets
            if row.work_id == "JDK-8364182"
            and row.path == "src/hotspot/share/classfile/vmSymbols.hpp"
        ]
        self.assertEqual(1, len(vm))
        self.assertIn(vm[0].state, {"TARGET_MATCH", "SUPERSEDED_APPROVED"})
        if vm[0].state == "SUPERSEDED_APPROVED":
            self.assertEqual(
                "4f752ab79abf854d144e53118a2cc57a4b3cc2432140e9297079cca86dd2abab",
                vm[0].actual,
            )

    def test_drift_fails_closed(self):
        with tempfile.TemporaryDirectory() as temp:
            target = pathlib.Path(temp) / "repo"
            shutil.copytree(
                ROOT / "m3",
                target / "m3",
                dirs_exist_ok=True,
            )
            # Only the verifier authorities are copied above; provide one required target with
            # deliberately wrong bytes so the first adapted packet fails before later reads.
            victim = (
                target
                / "src/jdk.javadoc/share/classes/jdk/javadoc/internal/doclets/"
                / "formats/html/DocFilesHandlerImpl.java"
            )
            victim.parent.mkdir(parents=True, exist_ok=True)
            victim.write_text("drift\n", encoding="utf-8")
            with self.assertRaises(ValueError):
                MODULE.compile_root(target)


if __name__ == "__main__":
    unittest.main()
