# SPDX-License-Identifier: Apache-2.0
"""Actual-Git tests for the closed ClassVer evidence policy.

The 60 original evidence bodies are protected fixture inputs. Tests copy them to
temporary repositories; no original receipt is edited or promoted by these tests.
"""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import py_compile
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest import mock

SOURCE_ROOT = Path(__file__).resolve().parents[3]
ADAPTER = "m3/migration/classver.py"
LEDGER = "m3/migration/classver-evidence.json"
HELPER = "m3/migration/recipe.py"


def git(root, *arguments, check=True):
    result = subprocess.run(
        ["git", "-C", str(root), *arguments], stdout=subprocess.PIPE,
        stderr=subprocess.PIPE, check=False)
    if check and result.returncode:
        raise AssertionError(result.stderr.decode("utf-8", "replace"))
    return result


class ClassVerEvidenceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.workspace = tempfile.TemporaryDirectory(prefix="m3-classver-fixture-")
        cls.template = Path(cls.workspace.name) / "template"
        cls.template.mkdir()
        cls.ledger = json.loads((SOURCE_ROOT / LEDGER).read_bytes())
        cls.rows = cls.ledger["files"]
        if len(cls.rows) != 60:
            raise AssertionError("the fixture must contain the complete 60-row authority")
        git(cls.template, "init", "--quiet")
        git(cls.template, "config", "user.name", "M3 ClassVer Fixture")
        git(cls.template, "config", "user.email", "classver-fixture@example.invalid")
        git(cls.template, "config", "core.autocrlf", "false")
        git(cls.template, "config", "core.filemode", "true")
        for relative in (ADAPTER, LEDGER, HELPER):
            destination = cls.template / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(SOURCE_ROOT / relative, destination)
        git(cls.template, "add", "--all")
        git(cls.template, "commit", "--quiet", "-m", "Protected fixture controls")
        for row in cls.rows:
            content = (SOURCE_ROOT / row["path"]).read_bytes()
            actual_blob = hashlib.sha1(
                b"blob " + str(len(content)).encode("ascii") + b"\0" + content).hexdigest()
            if (len(content) != row["bytes"]
                    or hashlib.sha256(content).hexdigest() != row["sha256"]
                    or actual_blob != row["gitBlob"]):
                raise AssertionError("protected evidence fixture drift: " + row["path"])
            destination = cls.template / row["path"]
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(content)
        git(cls.template, "add", "--all")
        git(cls.template, "commit", "--quiet", "-m", "Exact retained evidence fixture")

    @classmethod
    def tearDownClass(cls):
        cls.workspace.cleanup()

    def setUp(self):
        self.root = Path(self.workspace.name) / "case"
        self.reset()

    def tearDown(self):
        shutil.rmtree(self.root)

    def reset(self):
        if self.root.exists():
            shutil.rmtree(self.root)
        shutil.copytree(self.template, self.root)

    def run_candidate(self, *arguments):
        return subprocess.run(
            [sys.executable, "-S", str(self.root / ADAPTER), *arguments],
            cwd=self.root, stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)

    def commit(self, message="Fixture perturbation"):
        git(self.root, "add", "--all")
        git(self.root, "commit", "--quiet", "-m", message)

    def refuse(self, expected):
        result = self.run_candidate()
        self.assertNotEqual(0, result.returncode)
        self.assertEqual(b"", result.stdout, "a refusal must not print a pass receipt")
        self.assertIn(expected, result.stderr.decode("utf-8", "replace"))
        return result

    def tracked_bytes(self):
        names = git(self.root, "ls-files", "-z").stdout.split(b"\0")
        return {name: (self.root / os.fsdecode(name)).read_bytes()
                for name in names if name}

    def test_exact_evidence_keeps_bytes_and_native_whitespace_has_real_findings(self):
        native = git(self.root, "diff", "--check", "HEAD^", "HEAD", check=False)
        self.assertNotEqual(0, native.returncode)
        diagnostics = native.stdout + native.stderr
        for row in self.rows:
            self.assertIn(row["path"].encode("utf-8"), diagnostics)
        before = self.tracked_bytes()
        first = self.run_candidate()
        self.assertEqual(0, first.returncode, first.stderr.decode("utf-8", "replace"))
        receipt = json.loads(first.stdout)
        self.assertEqual("PASS_CLASSVER_EVIDENCE_DIFF", receipt["status"])
        self.assertEqual(60, receipt["admittedEvidence"])
        self.assertEqual(sum(row["bytes"] for row in self.rows), receipt["admittedEvidenceBytes"])
        self.assertEqual(0, receipt["gitDiffCheckExit"])
        self.assertEqual(0, receipt["evidenceWrites"])
        again = self.run_candidate()
        self.assertEqual(0, again.returncode, again.stderr.decode("utf-8", "replace"))
        self.assertEqual(first.stdout, again.stdout)
        self.assertEqual(before, self.tracked_bytes())
        self.assertEqual(b"", git(self.root, "status", "--porcelain", "--untracked-files=all").stdout)

    def test_active_code_and_unlisted_same_directory_files_still_fail(self):
        paths = (
            "src/Whitespace.java",
            "m3/tooling/migration-recipes/unlisted-control.xml",
            "m3/migration/intake-20261005/current-752-ci/unlisted-proof.log",
            "m3/migration/intake-20261005/current-752-ci/unlisted[1]*.txt",
        )
        for relative in paths:
            with self.subTest(path=relative):
                self.reset()
                path = self.root / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(b"new unlisted content with trailing whitespace  \n")
                self.commit()
                self.refuse("native git diff --check failed")

    def test_mixed_code_and_exact_evidence_diff_keeps_code_failure(self):
        path = self.root / "Mixed.java"
        path.write_bytes(b"class Mixed {}  \n")
        self.commit()
        git(self.root, "reset", "--soft", "HEAD~2")
        self.commit("Mixed code plus exact evidence")
        self.refuse("native git diff --check failed")

    def test_every_admitted_worktree_byte_drift_refuses(self):
        for row in self.rows:
            with self.subTest(path=row["path"]):
                self.reset()
                path = self.root / row["path"]
                content = path.read_bytes()
                self.assertTrue(content)
                changed = (b"X" if content[:1] != b"X" else b"Y") + content[1:]
                path.write_bytes(changed)
                self.refuse("immutable checkout bytes drift")

    def test_clean_looking_committed_drift_and_restored_checkout_refuse(self):
        row = self.rows[0]
        for restore in (False, True):
            with self.subTest(restore=restore):
                self.reset()
                path = self.root / row["path"]
                original = path.read_bytes()
                path.write_bytes(b"Clean replacement without whitespace defects.\n")
                self.commit()
                if restore:
                    path.write_bytes(original)
                self.refuse("immutable Git identity/mode drift")

    def test_body_and_ledger_resealing_cannot_add_an_admission(self):
        row = self.rows[0]
        path = self.root / row["path"]
        replacement = b"Replacement data.\n"
        path.write_bytes(replacement)
        changed = json.loads((self.root / LEDGER).read_bytes())
        changed["files"][0]["sha256"] = hashlib.sha256(replacement).hexdigest()
        changed["files"][0]["bytes"] = len(replacement)
        changed["files"][0]["gitBlob"] = hashlib.sha1(
            b"blob " + str(len(replacement)).encode("ascii") + b"\0" + replacement).hexdigest()
        (self.root / LEDGER).write_text(json.dumps(changed) + "\n")
        self.commit()
        self.refuse("immutable evidence ledger drift")

    def test_ledger_mutations_and_unsafe_aliases_refuse(self):
        mutations = (
            ("authority", lambda d: d["authority"].update(commit="0" * 40)),
            ("duplicate", lambda d: d["files"].append(dict(d["files"][0]))),
            ("unordered", lambda d: d["files"].reverse()),
            ("traversal", lambda d: d["files"][0].update(path="../outside")),
            ("absolute", lambda d: d["files"][0].update(path="/outside")),
            ("alias", lambda d: d["files"][0].update(path="m3/./alias")),
            ("pathspec", lambda d: d["files"][0].update(path=":(exclude)*")),
            ("role", lambda d: d["files"][0].update(role="active-code")),
            ("new-row", lambda d: d["files"][0].update(path="src/New.java")),
            ("null", lambda d: d.update(files=None)),
        )
        for name, mutate in mutations:
            with self.subTest(mutation=name):
                self.reset()
                document = json.loads((self.root / LEDGER).read_bytes())
                mutate(document)
                (self.root / LEDGER).write_text(json.dumps(document) + "\n")
                self.refuse("immutable evidence ledger drift")

    def test_committed_ledger_drift_cannot_hide_behind_correct_checkout(self):
        path = self.root / LEDGER
        original = path.read_bytes()
        path.write_bytes(original + b"\n")
        self.commit()
        path.write_bytes(original)
        self.refuse("immutable Git identity/mode drift")

    def test_missing_renamed_and_copied_admissions_do_not_expand_scope(self):
        row = self.rows[0]
        for mutation in ("delete", "rename", "copy"):
            with self.subTest(mutation=mutation):
                self.reset()
                path = self.root / row["path"]
                destination = path.with_name("unlisted-copy.log")
                if mutation == "delete":
                    path.unlink()
                elif mutation == "rename":
                    path.rename(destination)
                else:
                    shutil.copyfile(path, destination)
                    destination.write_bytes(destination.read_bytes() + b"unlisted trailing spaces  \n")
                self.commit()
                self.refuse("native git diff --check failed" if mutation == "copy"
                            else "immutable path missing, renamed or aliased")

    def test_file_parent_symlinks_and_git_modes_refuse(self):
        row = self.rows[0]
        for mutation in ("file-link", "parent-link", "worktree-mode", "committed-mode"):
            with self.subTest(mutation=mutation):
                self.reset()
                path = self.root / row["path"]
                if mutation == "file-link":
                    target = self.root / "outside-copy"
                    target.write_bytes(path.read_bytes())
                    path.unlink()
                    path.symlink_to(target)
                    expected = "symlink"
                elif mutation == "parent-link":
                    parent = path.parent
                    target = parent.with_name(parent.name + "-moved")
                    parent.rename(target)
                    parent.symlink_to(target, target_is_directory=True)
                    expected = "symlink"
                else:
                    path.chmod(0o755)
                    if mutation == "committed-mode":
                        self.commit()
                        expected = "immutable Git identity/mode drift"
                    else:
                        expected = "executable evidence/helper refused"
                self.refuse(expected)

    def test_pinned_helper_drift_refuses_before_execution(self):
        path = self.root / HELPER
        path.write_bytes(b"raise RuntimeError('untrusted helper was executed')\n")
        result = self.refuse("pinned recipe helper drift")
        self.assertNotIn(b"untrusted helper was executed", result.stderr)

    def test_verified_helper_bytes_ignore_poisoned_matching_bytecode_cache(self):
        path = self.root / HELPER
        original = path.read_bytes()
        metadata = path.stat()
        prefix = b"raise RuntimeError('cached helper executed')\n#"
        payload = prefix + b"x" * (len(original) - len(prefix) - 1) + b"\n"
        self.assertEqual(len(original), len(payload))
        path.write_bytes(payload)
        os.utime(path, ns=(metadata.st_atime_ns, metadata.st_mtime_ns))
        cache = Path(importlib.util.cache_from_source(str(path)))
        py_compile.compile(str(path), cfile=str(cache), doraise=True,
                           invalidation_mode=py_compile.PycInvalidationMode.TIMESTAMP)
        self.assertTrue(cache.is_file())
        path.write_bytes(original)
        os.utime(path, ns=(metadata.st_atime_ns, metadata.st_mtime_ns))
        result = self.run_candidate()
        self.assertEqual(0, result.returncode, result.stderr.decode("utf-8", "replace"))
        self.assertEqual("PASS_CLASSVER_EVIDENCE_DIFF", json.loads(result.stdout)["status"])

    def test_policy_override_missing_parent_and_nonrepository_refuse(self):
        override = self.run_candidate("--ledger", "anything.json")
        self.assertNotEqual(0, override.returncode)
        self.assertEqual(b"", override.stdout)
        self.assertIn(b"takes no override arguments", override.stderr)
        shutil.rmtree(self.root / ".git")
        self.refuse("Git command refused")
        git(self.root, "init", "--quiet")
        git(self.root, "config", "user.name", "M3 ClassVer Fixture")
        git(self.root, "config", "user.email", "classver-fixture@example.invalid")
        self.commit("Only one commit")
        self.refuse("Git command refused")

    def test_actual_endpoint_movement_during_git_check_refuses(self):
        spec = importlib.util.spec_from_file_location("_classver_race_candidate", self.root / ADAPTER)
        candidate = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(candidate)
        original_run = subprocess.run
        moved = False

        def run_with_one_real_commit(arguments, *args, **kwargs):
            nonlocal moved
            result = original_run(arguments, *args, **kwargs)
            if not moved and "diff" in arguments and "--check" in arguments:
                moved = True
                original_run(
                    ["git", "-C", str(self.root), "commit", "--allow-empty", "--quiet",
                     "-m", "Actual concurrent fixture commit"],
                    stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True)
            return result

        with mock.patch.object(candidate.subprocess, "run", side_effect=run_with_one_real_commit):
            with self.assertRaisesRegex(ValueError, "Git endpoints changed during admission"):
                candidate.check(self.root)
        self.assertTrue(moved)


if __name__ == "__main__":
    unittest.main()
