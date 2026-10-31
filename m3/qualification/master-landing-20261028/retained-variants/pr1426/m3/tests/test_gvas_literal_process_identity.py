#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors.
# SPDX-License-Identifier: Apache-2.0
"""Independent procfs fixtures and a live-child oracle for the GVAS proof."""
from __future__ import annotations

import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch


SOURCE = Path(__file__).resolve().parents[1] / "verify-gvas-literal-precompute.py"
SPEC = importlib.util.spec_from_file_location("m3_gvas_literal_process_identity", SOURCE)
assert SPEC is not None and SPEC.loader is not None
PROOF = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(PROOF)


class ProcfsFixtureTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temporary = tempfile.TemporaryDirectory(prefix="m3-gvas-proc-fixture-")
        self.addCleanup(self.temporary.cleanup)
        self.proc = Path(self.temporary.name)
        self.parent = 41000
        self.parent_local = 30
        self.child = 41011
        self.child_local = 31
        self.descriptor = 73
        self.status(self.parent, 1, (self.parent, self.parent_local))
        (self.proc / "self").symlink_to(str(self.parent), target_is_directory=True)
        self.status(self.child, self.parent, (self.child, self.child_local))
        self.fdinfo(self.child, (self.child, self.child_local))

    def status(self, pid: int, parent: int, namespaces: tuple[int, ...]) -> None:
        path = self.proc / str(pid) / "status"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(f"Name:\tfixture\nPid:\t{pid}\nPPid:\t{parent}\n"
                        f"NSpid:\t{' '.join(str(value) for value in namespaces)}\n")

    def fdinfo(self, pid: int, namespaces: tuple[int, ...]) -> None:
        path = self.proc / f"{self.parent}/fdinfo/{self.descriptor}"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(f"pos:\t0\nflags:\t02000002\nPid:\t{pid}\n"
                        f"NSpid:\t{' '.join(str(value) for value in namespaces)}\n")

    def resolve(self, child: int | None = None, parent: int | None = None,
                open_error: OSError | None = None) -> int:
        wanted = self.child_local if child is None else child
        parent_pid = self.parent_local if parent is None else parent
        with patch.object(PROOF.os, "getpid", return_value=parent_pid), \
                patch.object(PROOF.os, "pidfd_open", return_value=self.descriptor,
                             side_effect=open_error) as opened, \
                patch.object(PROOF.os, "close") as closed:
            try:
                with PROOF.proc_child_identity(wanted, self.proc) as proc_pid:
                    closed.assert_not_called()
                    if opened.called:
                        opened.assert_called_once_with(wanted, 0)
                    result = proc_pid
            finally:
                if opened.called and open_error is None:
                    closed.assert_called_once_with(self.descriptor)
                else:
                    closed.assert_not_called()
        return result

    def test_outer_procfs_namespace(self) -> None:
        self.assertEqual(self.child, self.resolve())

    def test_direct_pid_namespace_compatibility_without_pidfd(self) -> None:
        self.status(self.parent, 1, (self.parent,))
        self.status(self.child, self.parent, (self.child,))
        self.assertEqual(self.child, self.resolve(child=self.child, parent=self.parent,
                                                 open_error=OSError(38, "unsupported pidfd")))

    def test_coincident_parent_numbers_still_require_namespace_association(self) -> None:
        self.status(self.parent, 1, (self.parent, self.parent))
        self.assertEqual(self.child, self.resolve(parent=self.parent))

    def test_existing_unrelated_numeric_path_is_never_selected(self) -> None:
        self.status(self.child_local, 1, (self.child_local,))
        (self.proc / str(self.child_local) / "maps").write_text("unrelated process\n")
        self.assertEqual(self.child, self.resolve())

    def test_descendant_namespace_uses_parent_depth(self) -> None:
        namespaces = (self.child, self.child_local, 1)
        self.status(self.child, self.parent, namespaces)
        self.fdinfo(self.child, namespaces)
        self.assertEqual(self.child, self.resolve())

    def test_three_namespace_levels(self) -> None:
        self.status(self.parent, 1, (self.parent, 600, self.parent_local))
        namespaces = (self.child, 601, self.child_local)
        self.status(self.child, self.parent, namespaces)
        self.fdinfo(self.child, namespaces)
        self.assertEqual(self.child, self.resolve())

    def test_wrong_parent_fails_closed(self) -> None:
        self.status(self.child, 49999, (self.child, self.child_local))
        with self.assertRaisesRegex(AssertionError, "parent-owned child relationship"):
            self.resolve()

    def test_direct_wrong_parent_fails_closed(self) -> None:
        self.status(self.parent, 1, (self.parent,))
        self.status(self.child, 49999, (self.child,))
        with self.assertRaisesRegex(AssertionError, "parent-owned child relationship"):
            self.resolve(child=self.child, parent=self.parent)

    def test_unavailable_pidfd_fails_closed(self) -> None:
        with self.assertRaises(OSError):
            self.resolve(open_error=OSError(38, "unsupported pidfd"))

    def test_missing_child_status_fails_closed(self) -> None:
        (self.proc / str(self.child) / "status").unlink()
        with self.assertRaises(FileNotFoundError):
            self.resolve()

    def test_closed_descriptor_evidence_fails_closed(self) -> None:
        (self.proc / f"{self.parent}/fdinfo/{self.descriptor}").unlink()
        with self.assertRaises(FileNotFoundError):
            self.resolve()

    def test_exited_descriptor_identity_fails_closed(self) -> None:
        self.fdinfo(-1, (-1,))
        with self.assertRaisesRegex(AssertionError, "invalid Pid"):
            self.resolve()

    def test_missing_nspid_fails_closed(self) -> None:
        (self.proc / f"{self.parent}/fdinfo/{self.descriptor}").write_text(f"Pid:\t{self.child}\n")
        with self.assertRaisesRegex(AssertionError, "missing process identity"):
            self.resolve()

    def test_ambiguous_descriptor_pid_fails_closed(self) -> None:
        path = self.proc / f"{self.parent}/fdinfo/{self.descriptor}"
        path.write_text(path.read_text() + "Pid:\t41012\n")
        with self.assertRaisesRegex(AssertionError, "invalid Pid"):
            self.resolve()

    def test_duplicate_status_field_fails_closed(self) -> None:
        path = self.proc / str(self.child) / "status"
        path.write_text(path.read_text() + f"NSpid:\t{self.child} {self.child_local}\n")
        with self.assertRaisesRegex(AssertionError, "invalid NSpid"):
            self.resolve()

    def test_wrong_descriptor_namespace_pid_fails_closed(self) -> None:
        self.fdinfo(self.child, (self.child, 99))
        with self.assertRaisesRegex(AssertionError, "does not match the child namespace PID"):
            self.resolve()

    def test_wrong_descriptor_namespace_depth_fails_closed(self) -> None:
        self.fdinfo(self.child, (self.child,))
        with self.assertRaisesRegex(AssertionError, "does not match the child namespace PID"):
            self.resolve()

    def test_descriptor_status_namespace_disagreement_fails_closed(self) -> None:
        self.status(self.child, self.parent, (self.child, self.child_local, 1))
        with self.assertRaisesRegex(AssertionError, "parent-owned child relationship"):
            self.resolve()

    def test_inconsistent_child_pid_fails_closed(self) -> None:
        path = self.proc / str(self.child) / "status"
        path.write_text(f"Pid:\t41012\nPPid:\t{self.parent}\nNSpid:\t41012 {self.child_local}\n")
        with self.assertRaisesRegex(AssertionError, "parent-owned child relationship"):
            self.resolve()

    def test_inconsistent_nspid_prefix_fails_closed(self) -> None:
        self.status(self.child, self.parent, (41012, self.child_local))
        with self.assertRaisesRegex(AssertionError, "inconsistent process identity"):
            self.resolve()

    def test_parent_namespace_mismatch_fails_closed(self) -> None:
        with self.assertRaisesRegex(AssertionError, "does not match this parent"):
            self.resolve(parent=99)

    def test_invalid_self_link_fails_closed(self) -> None:
        (self.proc / "self").unlink()
        (self.proc / "self").symlink_to("../unrelated")
        with self.assertRaisesRegex(AssertionError, "invalid procfs self link"):
            self.resolve()

    def test_invalid_requested_pid_fails_closed(self) -> None:
        with self.assertRaisesRegex(AssertionError, "invalid child namespace PID"):
            self.resolve(child=0)

    def test_descriptor_stays_open_through_caller_evidence_and_closes_on_failure(self) -> None:
        with patch.object(PROOF.os, "getpid", return_value=self.parent_local), \
                patch.object(PROOF.os, "pidfd_open", return_value=self.descriptor), \
                patch.object(PROOF.os, "close") as closed:
            with self.assertRaisesRegex(RuntimeError, "mapping check fixture"):
                with PROOF.proc_child_identity(self.child_local, self.proc) as proc_pid:
                    self.assertEqual(self.child, proc_pid)
                    closed.assert_not_called()
                    raise RuntimeError("mapping check fixture")
            closed.assert_called_once_with(self.descriptor)


@unittest.skipUnless(sys.platform.startswith("linux"), "Linux procfs proof")
class LiveProcessIdentityTest(unittest.TestCase):
    def test_parent_resolves_independent_live_child_self_witness(self) -> None:
        program = ("import json, os, sys\n"
                   "print(json.dumps({'pid': os.getpid(), 'proc_pid': int(os.readlink('/proc/self'))}), flush=True)\n"
                   "sys.stdin.readline()\n")
        process = subprocess.Popen([sys.executable, "-c", program], stdin=subprocess.PIPE,
                                   stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        original_pid = process.pid
        try:
            assert process.stdout is not None
            witness = json.loads(process.stdout.readline())
            self.assertEqual(original_pid, witness["pid"])
            with PROOF.proc_child_identity(original_pid) as proc_pid:
                self.assertEqual(witness["proc_pid"], proc_pid)
                self.assertEqual(original_pid, process.pid)
                self.assertTrue((Path("/proc") / str(proc_pid) / "maps").read_text().strip())
            stdout, stderr = process.communicate("\n", timeout=15)
            self.assertEqual(0, process.returncode, stdout + stderr)
            print(f"M3_GVAS_PROC_IDENTITY_LIVE_PASS namespace_pid={original_pid} proc_pid={proc_pid}")
        finally:
            if process.poll() is None:
                process.kill()
                process.communicate(timeout=15)


if __name__ == "__main__":
    unittest.main(verbosity=2)
