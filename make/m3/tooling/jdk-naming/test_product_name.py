# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fixture tests for product_name.py (PRODUCT_NAME_UNIFY v1): every split spelling of the product name becomes one
token, a flag and its argparse destination are renamed together, pinned ids, donor paths, ledgers and evidence keep
their bytes, and a second run is a fixed point."""
from __future__ import annotations

import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

TOOL = Path(__file__).with_name("product_name.py")
TREE = {
    ".github/workflows/w.yml": "name: M3 JDK naming standard\njobs:\n  a:\n    steps:\n      - name: Build the M3 JDK image\n",
    "m3/x/README.md": "Set `M3_JDK` to a JDK and run it; see M3-JDK-PORT-1 and m3-jdk-handoff/.\n",
    "m3/x/run.py": 'p.add_argument("--m3-jdk-receipt")\np.add_argument("--m3-jdk", type=Path)\n'
                   'x = (a.m3_jdk / "bin"), a.m3_jdk_receipt, M3_JDK21_REFUSE\n',
    "m3/x/pins.tsv": "id\tM3 JDK\n",
    "m3/x/qualification/q/run.sh": "M3_JDK=/jdk bash run.sh\n",
}


def run(root: Path, *extra: str) -> subprocess.CompletedProcess:
    return subprocess.run([sys.executable, "-I", str(TOOL), "--root", str(root), *extra], capture_output=True, text=True)


class ProductNameTest(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        for rel, text in TREE.items():
            (self.root / rel).parent.mkdir(parents=True, exist_ok=True)
            (self.root / rel).write_bytes(text.encode("utf-8"))
        out = run(self.root)
        self.assertEqual(out.returncode, 0, out.stdout + out.stderr)

    def tearDown(self) -> None:
        self.tmp.cleanup()

    def read(self, rel: str) -> str:
        return (self.root / rel).read_text(encoding="utf-8")

    def test_prose_and_workflow_names_become_one_token(self) -> None:
        self.assertEqual(self.read(".github/workflows/w.yml"),
                         "name: M3JDK naming standard\njobs:\n  a:\n    steps:\n      - name: Build the M3JDK image\n")

    def test_environment_name_follows_and_pinned_ids_and_donor_paths_stay(self) -> None:
        self.assertEqual(self.read("m3/x/README.md"),
                         "Set `M3JDK` to a JDK and run it; see M3-JDK-PORT-1 and m3-jdk-handoff/.\n")

    def test_flag_and_destination_are_renamed_together(self) -> None:
        self.assertEqual(self.read("m3/x/run.py"),
                         'p.add_argument("--m3jdk-receipt")\np.add_argument("--m3jdk", type=Path)\n'
                         'x = (a.m3jdk / "bin"), a.m3jdk_receipt, M3_JDK21_REFUSE\n')

    def test_ledgers_and_evidence_keep_their_bytes(self) -> None:
        self.assertEqual(self.read("m3/x/pins.tsv"), TREE["m3/x/pins.tsv"])
        self.assertEqual(self.read("m3/x/qualification/q/run.sh"), TREE["m3/x/qualification/q/run.sh"])

    def test_second_run_is_a_fixed_point(self) -> None:
        self.assertEqual(run(self.root, "--check").returncode, 0)


if __name__ == "__main__":
    unittest.main()
