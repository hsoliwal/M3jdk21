# SPDX-License-Identifier: Apache-2.0
import hashlib
import tempfile
import unittest
from pathlib import Path

import source_convergence_gate as gate


def sha(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest()


class SourceConvergenceGateTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.source = self.root / "src/java.base/share/classes/example/A.java"
        self.source.parent.mkdir(parents=True)
        self.before = b"package example; final class A {}\n"
        self.after = b"package example; final class A { /* M3 */ }\n"
        self.source.write_bytes(self.before)

        self.output = self.root / "m3/target/m3-jdk-source-convergence"
        candidate = self.output / "candidates/src/java.base/share/classes/example/A.java"
        candidate.parent.mkdir(parents=True)
        candidate.write_bytes(self.after)

        self.manifest = self.output / "SOURCE_CONVERGENCE.tsv"
        self.manifest.write_text(
            "path\tpreSha256\tpostSha256\tatomizationChanged\t"
            "patternizationChanged\tdocumentationChanged\tfixedPoint\t"
            "status\tmessage\tcandidate\n"
            f"src/java.base/share/classes/example/A.java\t{sha(self.before)}\t"
            f"{sha(self.after)}\ttrue\ttrue\ttrue\ttrue\t"
            "CONVERGED_CHANGED\t\t"
            "candidates/src/java.base/share/classes/example/A.java\n",
            encoding="utf-8",
        )
        self.targets = self.root / "targets.txt"
        self.targets.write_text(
            "src/java.base/share/classes/example/A.java\n"
            "make/ignored.gmk\n",
            encoding="utf-8",
        )

    def tearDown(self) -> None:
        self.temp.cleanup()

    def test_matching_fixed_point_target_passes(self) -> None:
        root = gate.verify(self.root, self.manifest, self.targets)
        self.assertRegex(root, r"^[0-9a-f]{64}$")
        self.assertEqual(root, gate.verify(self.root, self.manifest, self.targets))

    def test_preimage_drift_fails(self) -> None:
        self.source.write_text("drift\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "preimage drift"):
            gate.verify(self.root, self.manifest, self.targets)

    def test_hold_fails(self) -> None:
        text = self.manifest.read_text(encoding="utf-8")
        self.manifest.write_text(
            text.replace("true\tCONVERGED_CHANGED", "false\tHOLD"),
            encoding="utf-8",
        )
        with self.assertRaisesRegex(ValueError, "not at FILE fixed point"):
            gate.verify(self.root, self.manifest, self.targets)

    def test_missing_row_fails(self) -> None:
        self.targets.write_text(
            "src/java.base/share/classes/example/Missing.java\n",
            encoding="utf-8",
        )
        with self.assertRaisesRegex(ValueError, "missing SOURCE_CONVERGENCE row"):
            gate.verify(self.root, self.manifest, self.targets)

    def test_candidate_drift_fails(self) -> None:
        candidate = (
            self.output
            / "candidates/src/java.base/share/classes/example/A.java"
        )
        candidate.write_text("drift\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "candidate postimage drift"):
            gate.verify(self.root, self.manifest, self.targets)

    def test_test_tree_java_target_is_admitted(self) -> None:
        path = "test/jdk/example/TestA.java"
        source = self.root / path
        source.parent.mkdir(parents=True)
        before = b"package example; final class TestA {}\n"
        source.write_bytes(before)
        with self.manifest.open("a", encoding="utf-8") as handle:
            handle.write(
                f"{path}\t{sha(before)}\t{sha(before)}\tfalse\tfalse\tfalse\t"
                "true\tCONVERGED_UNCHANGED\t\t\n"
            )
        self.targets.write_text(path + "\n", encoding="utf-8")

        root = gate.verify(self.root, self.manifest, self.targets)

        self.assertRegex(root, r"^[0-9a-f]{64}$")

    def test_duplicate_manifest_row_fails(self) -> None:
        text = self.manifest.read_text(encoding="utf-8")
        line = text.splitlines()[1]
        self.manifest.write_text(text + line + "\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "duplicate convergence row"):
            gate.verify(self.root, self.manifest, self.targets)


if __name__ == "__main__":
    unittest.main()
