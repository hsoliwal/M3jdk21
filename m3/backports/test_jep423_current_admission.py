# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0

import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path


SCRIPT = (
    Path(__file__).parent
    / "recipes"
    / "jep-423-region-pinning"
    / "validate_current_admission.py"
)
SPEC = importlib.util.spec_from_file_location("jep423_current_admission", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


class JEP423CurrentAdmissionTest(unittest.TestCase):
    def packet(self, root: Path, mechanical: int, held: int) -> Path:
        packet = root / "packet"
        packet.mkdir()
        held_path = "c.java" if held else "NONE"
        (packet / "CURRENT_TREE_PROOF_REQUEST.tsv").write_text(
            "field\tvalue\n"
            "candidate_paths\t2\n"
            f"expected_mechanical_paths\t{mechanical}\n"
            f"expected_held_paths\t{held}\n"
            f"expected_held_path\t{held_path}\n"
            "preserved_java21_deletions\t1\n"
            "cumulative_lineage_paths\t3\n",
            encoding="utf-8",
        )
        (packet / "CUMULATIVE_ADMIT_PATHS.txt").write_text(
            "a.java\nb.java\n", encoding="utf-8"
        )
        (packet / "COMPATIBILITY_SPLIT.tsv").write_text(
            "path\tupstream_status\tplane\tdisposition\treason\n"
            "a.java\tmodified\tPRODUCT\tFILE_ATOM_CANDIDATE\ta\n"
            "b.java\tadded\tTEST\tFILE_ATOM_CANDIDATE\tb\n"
            "keep.java\tremoved\tTEST\tPRESERVE_JDK21_PATH\tkeep\n",
            encoding="utf-8",
        )
        return packet

    def test_admission_accepts_all_mechanical_packet(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            packet = self.packet(root, 2, 0)
            admission = root / "admission.tsv"
            admission.write_text(
                "path\tcurrent_status\tcurrent_sha256\tbaseline_status\t"
                "baseline_sha256\tkind\tadmission\n"
                "a.java\tPRESENT\ta\tPRESENT\ta\tJAVA\tMECHANICAL_FILE_REPLAY\n"
                "b.java\tABSENT\tABSENT\tABSENT\tABSENT\tJAVA\tMECHANICAL_FILE_REPLAY\n",
                encoding="utf-8",
            )
            self.assertEqual(
                "JEP423_CUMULATIVE_ADMISSION candidates=2 mechanical=2 held=0",
                MODULE.admission(packet, admission),
            )

    def test_admission_rejects_stale_hold_expectation(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            packet = self.packet(root, 1, 1)
            admission = root / "admission.tsv"
            admission.write_text(
                "path\tcurrent_status\tcurrent_sha256\tbaseline_status\t"
                "baseline_sha256\tkind\tadmission\n"
                "a.java\tPRESENT\ta\tPRESENT\ta\tJAVA\tMECHANICAL_FILE_REPLAY\n"
                "b.java\tABSENT\tABSENT\tABSENT\tABSENT\tJAVA\tMECHANICAL_FILE_REPLAY\n",
                encoding="utf-8",
            )
            with self.assertRaisesRegex(ValueError, "admission drift"):
                MODULE.admission(packet, admission)


    def test_accounting_requires_exact_path_donor_receipt(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            packet = self.packet(root, 2, 0)
            request = packet / "CURRENT_TREE_PROOF_REQUEST.tsv"
            request.write_text(
                request.read_text(encoding="utf-8")
                + "path_donor_overrides\t1\n"
                + "donor_override_file\tpacket/PATH_DONOR_OVERRIDES.tsv\n",
                encoding="utf-8",
            )
            donor = "a" * 40
            (packet / "PATH_DONOR_OVERRIDES.tsv").write_text(
                "path\tdonor_ref\n"
                f"a.java\t{donor}\n",
                encoding="utf-8",
            )

            runner = root / "runner"
            (runner / "file-atoms").mkdir(parents=True)
            (runner / "MECHANICAL_PATHS.txt").write_text(
                "a.java\nb.java\n", encoding="utf-8"
            )
            (runner / "JDK21_TO_JEP423_FINAL.tsv").write_text(
                "release\tpath\tstatus\n"
                "22\ta.java\tMODIFIED\n"
                "22\tb.java\tSAME\n",
                encoding="utf-8",
            )
            (runner / "file-atoms" / "CRATES.tsv").write_text(
                "crate_name\trelease\tbaseline_ref\tdonor_ref\ttarget_count\t"
                "first_path\tlast_path\tstatus\n"
                f"crate-a\t22\tjdk-21+35\t{donor}\t1\ta.java\ta.java\t"
                "CANDIDATE_UNVERIFIED\n",
                encoding="utf-8",
            )
            (runner / "file-atoms" / "EXCLUSIONS.tsv").write_text(
                "path\treason\n", encoding="utf-8"
            )

            self.assertEqual(
                "JEP423_ACCOUNTED generated=1 same=1 held=0 preserved=1 total=3",
                MODULE.accounting(packet, runner),
            )

            (runner / "file-atoms" / "CRATES.tsv").write_text(
                "crate_name\trelease\tbaseline_ref\tdonor_ref\ttarget_count\t"
                "first_path\tlast_path\tstatus\n"
                "crate-a\t22\tjdk-21+35\t" + ("b" * 40)
                + "\t1\ta.java\ta.java\tCANDIDATE_UNVERIFIED\n",
                encoding="utf-8",
            )
            with self.assertRaisesRegex(ValueError, "donor override receipt drift"):
                MODULE.accounting(packet, runner)

    def test_accounting_uses_packet_and_compatibility_split(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            packet = self.packet(root, 2, 0)
            runner = root / "runner"
            (runner / "file-atoms").mkdir(parents=True)
            (runner / "MECHANICAL_PATHS.txt").write_text(
                "a.java\nb.java\n", encoding="utf-8"
            )
            (runner / "JDK21_TO_JEP423_FINAL.tsv").write_text(
                "release\tpath\tstatus\n"
                "22\ta.java\tMODIFIED\n"
                "22\tb.java\tSAME\n",
                encoding="utf-8",
            )
            (runner / "file-atoms" / "CRATES.tsv").write_text(
                "crate\ttarget_count\ncrate-a\t1\n", encoding="utf-8"
            )
            (runner / "file-atoms" / "EXCLUSIONS.tsv").write_text(
                "path\treason\n", encoding="utf-8"
            )
            self.assertEqual(
                "JEP423_ACCOUNTED generated=1 same=1 held=0 preserved=1 total=3",
                MODULE.accounting(packet, runner),
            )


if __name__ == "__main__":
    unittest.main()
