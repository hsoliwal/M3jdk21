#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import csv
import importlib.util
import tempfile
import unittest
from pathlib import Path


def load_module():
    path = Path(__file__).with_name("j491_materialize.py")
    spec = importlib.util.spec_from_file_location("m3_j491_materialize", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


class J491MaterializeTest(unittest.TestCase):
    def setUp(self) -> None:
        self.mod = load_module()

    def test_representative_domains_and_architectures(self) -> None:
        cases = {
            "src/hotspot/cpu/x86/sharedRuntime_x86_64.cpp": ("HOTSPOT_CPU", "x86"),
            "src/hotspot/cpu/aarch64/aarch64.ad": ("HOTSPOT_CPU", "aarch64"),
            "src/hotspot/share/runtime/objectMonitor.cpp": ("HOTSPOT_SHARED", "shared"),
            "src/hotspot/share/prims/jvmtiEnv.cpp": ("JVMTI", "shared"),
            "src/hotspot/share/jfr/metadata/metadata.xml": ("JFR", "shared"),
            "src/jdk.hotspot.agent/share/classes/sun/jvm/hotspot/runtime/JavaThread.java": ("SA", "shared"),
            "src/java.base/share/native/libjava/VirtualThread.c": ("JAVA_BASE_NATIVE", "shared"),
            "src/java.base/share/classes/java/lang/VirtualThread.java": ("JAVA_BASE", "shared"),
            "test/hotspot/jtreg/runtime/vthread/JNIMonitor/JNIMonitor.java": ("HOTSPOT_TEST", "test"),
            "test/jdk/java/lang/Thread/virtual/MonitorEnterExit.java": ("JDK_TEST", "test"),
        }
        for path, expected in cases.items():
            node = self.mod.classify(path)
            self.assertEqual(expected, (node.domain, node.architecture), path)

    def test_invalid_or_unknown_paths_fail_closed(self) -> None:
        for path in ("", "/abs/file", "../escape", "docs/unrelated.md"):
            with self.assertRaises(ValueError, msg=path):
                self.mod.classify(path)

    def test_exact_denominator_materializes_sorted_dag_and_counts(self) -> None:
        root = Path(__file__).parents[0]
        paths = root / "recipes" / "j491" / "PATHS.txt"
        # Unit tests may run from m3/backports; use the checked-in packet path directly.
        if not paths.is_file():
            paths = Path(__file__).with_name("recipes") / "j491" / "PATHS.txt"
        rows = self.mod.load_paths(paths)
        self.assertEqual(246, len(rows))

        with tempfile.TemporaryDirectory() as temp:
            out = Path(temp)
            nodes = self.mod.materialize(rows, out)
            self.assertEqual(246, len(nodes))
            with (out / "ARCH_DAG.tsv").open(encoding="utf-8", newline="") as handle:
                dag = list(csv.DictReader(handle, delimiter="\t"))
            self.assertEqual(246, len(dag))
            self.assertEqual(sorted(row["path"] for row in dag), [row["path"] for row in dag])
            self.assertTrue((out / "DOMAIN_COUNTS.tsv").is_file())


if __name__ == "__main__":
    unittest.main()
