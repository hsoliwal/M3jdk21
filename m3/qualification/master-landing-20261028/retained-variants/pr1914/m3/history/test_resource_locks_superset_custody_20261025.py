# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Adversarial end-to-end test of the product's actual source-handoff Python gate.

Run the embedded GitHub Actions Python, not a separately copied implementation.
No JDK, network, Maven, or third-party Python packages are required.
"""
import hashlib
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ".github/workflows/m3-merged-atom-preservation-20261022.yml"
HISTORICAL = "m3/docs/synexia-m3jdk21-eclipse-recipe-handoff-20261022.tsv"
SUCCESSOR = "m3/docs/synexia-m3jdk21-eclipse-source-superset-20261025.tsv"
LOCKS = "m3/file/src/main/java/com/m3/file/M3ResourceLocks.java"
HISTORICAL_SHA = "d6ad65fcb48f9a80b71acda82baa4cfcfce9438c87eae207d346e71ce2c74bba"
OLD_SHA = "18d05ae54945d4129c46d62e26551cbc4fd200b31c185e020b9ee6240fdc9599"
NEW_SHA = "eb0bf36625effaffa147be1f650590dfa08627a5f6f52d105b8fbb201d6b5718"


def embedded_program() -> str:
    lines = (ROOT / WORKFLOW).read_text(encoding="utf-8").splitlines()
    step = next(i for i, line in enumerate(lines)
                if line.strip() == "- name: Verify product source SHA-256 matches canonical Synexia handoff receipts")
    start = next(i for i in range(step + 1, len(lines))
                 if lines[i] == "          python3 - <<'PY'")
    end = next(i for i in range(start + 1, len(lines)) if lines[i] == "          PY")
    statements = lines[start + 1:end]
    if not statements or not all(s.startswith("          ") for s in statements):
        raise AssertionError("modified or incomplete embedded Python block")
    return "\n".join(s[10:] for s in statements) + "\n"


def historical_locks(current: bytes) -> bytes:
    """Reverse only the two additive changes; fail on unexpected source drift."""
    import_line = b"import java.util.concurrent.TimeUnit;\n"
    begin = b"    /**\n     * Wait at most the specified total budget for a participating scope.\n"
    end = b"    /** Execute an ordinary Java Runnable in a release-safe resource scope. */"
    if current.count(import_line) != 1 or current.count(begin) != 1 or current.count(end) != 1:
        raise AssertionError("merged ResourceLocks source shape changed")
    source = current.replace(import_line, b"")
    left = source.index(begin)
    right = source.index(end, left)
    source = source[:left] + source[right:]
    if hashlib.sha256(source).hexdigest() != OLD_SHA:
        raise AssertionError("source rollback fixture does not match historical Git source")
    return source


class ResourceLocksSupersetCustodyTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        historical_bytes = (ROOT / HISTORICAL).read_bytes()
        self.assertEqual(HISTORICAL_SHA, hashlib.sha256(historical_bytes).hexdigest())
        lines = historical_bytes.decode("utf-8").splitlines()
        targets = [s.split("\t", 1)[0] for s in lines if s and not s.startswith("#")]
        self.assertEqual(5, len(targets))
        for path in [HISTORICAL, SUCCESSOR, *targets]:
            dest = self.root / path
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT / path, dest)
        self.program = embedded_program()

    def run_gate(self):
        return subprocess.run([sys.executable, "-c", self.program], cwd=self.root,
                              text=True, capture_output=True, check=False, timeout=20)

    def test_current_additive_source_passes_with_pending_status(self):
        result = self.run_gate()
        self.assertEqual(0, result.returncode, result.stderr + result.stdout)
        self.assertIn("M3_RESOURCE_LOCKS_ADDITIVE_SUCCESSOR_PENDING", result.stdout)
        self.assertIn("historical_rows=5 approved_additive_successors=1", result.stdout)

    def test_rollback_to_historical_constructor_only_source_is_refused(self):
        path = self.root / LOCKS
        current = path.read_bytes()
        self.assertEqual(NEW_SHA, hashlib.sha256(current).hexdigest())
        path.write_bytes(historical_locks(current))
        result = self.run_gate()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("required M3ResourceLocks additive successor missing", result.stderr)

    def test_unrelated_source_drift_is_refused(self):
        historical = (self.root / HISTORICAL).read_text(encoding="utf-8")
        path = historical.splitlines()[1].split("\t", 1)[0]
        with (self.root / path).open("ab") as output:
            output.write(b"\n// unapproved edit\n")
        result = self.run_gate()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("M3 source drifted from Synexia recipe result", result.stderr)

    def test_forged_historical_qualification_is_refused(self):
        path = self.root / HISTORICAL
        text = path.read_text(encoding="utf-8")
        self.assertIn("PENDING_RECIPE_AND_PRODUCT_CI", text)
        path.write_text(text.replace("PENDING_RECIPE_AND_PRODUCT_CI",
                                     "QUALIFIED", 1), encoding="utf-8")
        result = self.run_gate()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("M3 historical source handoff receipt drifted", result.stderr)

    def test_forged_successor_qualification_is_refused(self):
        path = self.root / SUCCESSOR
        text = path.read_text(encoding="utf-8")
        self.assertIn("PENDING_RECIPE_AND_PRODUCT_CI", text)
        path.write_text(text.replace("PENDING_RECIPE_AND_PRODUCT_CI", "QUALIFIED"),
                        encoding="utf-8")
        result = self.run_gate()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("unapproved M3ResourceLocks additive successor receipt", result.stderr)

    def test_forged_extra_successor_row_is_refused(self):
        path = self.root / SUCCESSOR
        row = next(s for s in path.read_text(encoding="utf-8").splitlines()
                   if s and not s.startswith("#"))
        with path.open("a", encoding="utf-8") as output:
            output.write(row + "\n")
        result = self.run_gate()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("unapproved M3ResourceLocks additive successor receipt", result.stderr)


if __name__ == "__main__":
    unittest.main()
