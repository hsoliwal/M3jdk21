# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""Atom and performance tests for retire_profile.py (RETIRE_PROFILE v1 and v2), each on a throwaway git repository.

Atoms: v1 block removal, v2 byte restore of the installer's parent blob, v1 fallback for a pom changed after or
created by the installer, C14N refusal (HELD), KEEP by exact path and by prefix, --under scope, dry run.
Run: python -m unittest discover -s m3/tooling/standalone -p test_retire_profile.py -v"""
import subprocess
import sys
import tempfile
import time
import unittest
from pathlib import Path

TOOL = Path(__file__).resolve().with_name("retire_profile.py")
PID = "m3-recipe-boot"


def original(artifact, version="1"):
    return ('<?xml version="1.0" encoding="UTF-8"?>\n<project xmlns="http://maven.apache.org/POM/4.0.0">\n'
            "  <!-- kept comment -->\n  <modelVersion>4.0.0</modelVersion>\n"
            f"  <groupId>g</groupId><artifactId>{artifact}</artifactId><version>{version}</version>\n</project>\n")


def installed(artifact, version="1"):
    """The installer's shape: one element per line, and the profile appended."""
    return ('<?xml version="1.0" encoding="UTF-8"?>\n<project xmlns="http://maven.apache.org/POM/4.0.0">\n'
            " <!-- kept comment -->\n <modelVersion>4.0.0</modelVersion>\n <groupId>g</groupId>\n"
            f" <artifactId>{artifact}</artifactId>\n <version>{version}</version>\n <profiles>\n  <profile>\n"
            f"   <id>{PID}</id>\n   <properties>\n    <x>1</x>\n   </properties>\n  </profile>\n </profiles>\n"
            "</project>\n")


def v1_shape(artifact, version="1"):
    return installed(artifact, version).split(" <profiles>")[0] + "</project>\n"


class Repo:
    """A git repository built from commits of {path: text}; `installer` is the commit id of the second commit."""

    def __init__(self, root, *commits):
        self.root = Path(root)
        self.git("init", "-q")
        for key, value in (("user.email", "t@example.invalid"), ("user.name", "t"), ("core.autocrlf", "false")):
            self.git("config", key, value)
        self.installer = None
        for number, files in enumerate(commits):
            self.write(files)
            self.git("add", "-A")
            self.git("commit", "-q", "--allow-empty", "-m", f"c{number}")
            self.installer = self.git("rev-parse", "HEAD") if number == 1 else self.installer

    def git(self, *args):
        return subprocess.run(["git", "-C", str(self.root), *args], check=True, capture_output=True,
                              text=True).stdout.strip()

    def write(self, files):
        for path, text in files.items():
            target = self.root / path
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(text.encode("utf-8"))

    def read(self, path):
        return (self.root / path).read_bytes().decode("utf-8")

    def run(self, *extra):
        out = self.root / ".out"
        subprocess.run([sys.executable, "-I", str(TOOL), str(self.root), PID, str(out), *extra], check=True,
                       capture_output=True)
        rows = (out / "POMS.tsv").read_text(encoding="utf-8").splitlines()[1:]
        return {row.split("\t")[0]: row.split("\t")[2] for row in rows}


class RetireProfileAtomTest(unittest.TestCase):

    def repo(self, *commits):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        return Repo(tmp.name, *commits)

    def test_v1_removes_the_block_exactly_and_is_idempotent(self):
        repo = self.repo({"a/pom.xml": installed("a")})
        self.assertEqual(repo.run("--apply"), {"a/pom.xml": "RETIRED"})
        self.assertEqual(repo.read("a/pom.xml"), v1_shape("a"))
        self.assertEqual(repo.run("--apply"), {})

    def test_v2_restores_the_installer_parent_blob_byte_for_byte(self):
        repo = self.repo({"a/pom.xml": original("a")}, {"a/pom.xml": installed("a")})
        self.assertEqual(repo.run("--apply", "--restore-installer", repo.installer), {"a/pom.xml": "RESTORED"})
        self.assertEqual(repo.read("a/pom.xml"), original("a"))

    def test_v2_keeps_the_v1_edit_for_a_pom_changed_after_the_installer(self):
        repo = self.repo({"a/pom.xml": original("a")}, {"a/pom.xml": installed("a")},
                         {"a/pom.xml": installed("a", "2")})
        self.assertEqual(repo.run("--apply", "--restore-installer", repo.installer), {"a/pom.xml": "RETIRED"})
        self.assertEqual(repo.read("a/pom.xml"), v1_shape("a", "2"))

    def test_v2_keeps_the_v1_edit_for_a_pom_created_by_the_installer(self):
        repo = self.repo({"keep.txt": "x"}, {"a/pom.xml": installed("a")})
        self.assertEqual(repo.run("--apply", "--restore-installer", repo.installer), {"a/pom.xml": "RETIRED"})
        self.assertEqual(repo.read("a/pom.xml"), v1_shape("a"))

    def test_v2_holds_a_pom_whose_installer_changed_more_than_the_profile(self):
        repo = self.repo({"a/pom.xml": original("a")}, {"a/pom.xml": installed("a", "9")})
        self.assertEqual(repo.run("--apply", "--restore-installer", repo.installer), {"a/pom.xml": "HELD"})
        self.assertEqual(repo.read("a/pom.xml"), installed("a", "9"))

    def test_keep_by_exact_path_and_by_prefix_leaves_bytes_untouched(self):
        poms = {"a/pom.xml": installed("a"), "k/b/pom.xml": installed("b"), "c/pom.xml": installed("c")}
        repo = self.repo(poms)
        repo.write({"KEEP.tsv": "path\treason\nk/\tPREFIX\nc/pom.xml\tEXACT\n"})
        self.assertEqual(repo.run("--apply", "--keep", str(repo.root / "KEEP.tsv")),
                         {"a/pom.xml": "RETIRED", "k/b/pom.xml": "KEPT", "c/pom.xml": "KEPT"})
        self.assertEqual((repo.read("k/b/pom.xml"), repo.read("c/pom.xml")), (installed("b"), installed("c")))

    def test_under_fixes_the_scope_by_path(self):
        repo = self.repo({"m3/a/pom.xml": installed("a"), "src/b/pom.xml": installed("b")})
        self.assertEqual(repo.run("--apply", "--under", "m3/"), {"m3/a/pom.xml": "RETIRED"})
        self.assertEqual(repo.read("src/b/pom.xml"), installed("b"))

    def test_dry_run_plans_and_writes_nothing(self):
        repo = self.repo({"a/pom.xml": original("a")}, {"a/pom.xml": installed("a")})
        self.assertEqual(repo.run("--restore-installer", repo.installer), {"a/pom.xml": "PLANNED_RESTORE"})
        self.assertEqual(repo.read("a/pom.xml"), installed("a"))


class RetireProfilePerformanceTest(unittest.TestCase):
    POMS = 200
    BOUND_SECONDS = 120.0

    def test_v2_restore_throughput(self):
        names = [f"m{i:03d}" for i in range(self.POMS)]
        with tempfile.TemporaryDirectory() as tmp:
            repo = Repo(tmp, {f"{n}/pom.xml": original(n) for n in names},
                        {f"{n}/pom.xml": installed(n) for n in names})
            started = time.perf_counter()
            actions = repo.run("--apply", "--restore-installer", repo.installer)
            elapsed = time.perf_counter() - started
            self.assertEqual(set(actions.values()), {"RESTORED"})
            self.assertEqual(len(actions), self.POMS)
            self.assertLess(elapsed, self.BOUND_SECONDS)
            print(f"RETIRE_PROFILE_V2_PERF poms={self.POMS} seconds={elapsed:.3f} "
                  f"poms_per_second={self.POMS / elapsed:.1f}")


if __name__ == "__main__":
    unittest.main()
