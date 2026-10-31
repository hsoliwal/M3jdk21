#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


MODULE_PATH = Path(__file__).with_name("path_map.py")
SPEC = importlib.util.spec_from_file_location("m3_jep484_path_map", MODULE_PATH)
assert SPEC is not None and SPEC.loader is not None
mod = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = mod
SPEC.loader.exec_module(mod)


def git(repo: Path, *args: str) -> str:
    env = os.environ.copy()
    env.update(
        {
            "GIT_AUTHOR_NAME": "M3 Test",
            "GIT_AUTHOR_EMAIL": "m3@example.invalid",
            "GIT_COMMITTER_NAME": "M3 Test",
            "GIT_COMMITTER_EMAIL": "m3@example.invalid",
        }
    )
    proc = subprocess.run(
        ("git", "-C", str(repo), *args),
        check=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding="utf-8",
        env=env,
    )
    return proc.stdout.strip()


class Jep484PathMapTest(unittest.TestCase):
    def repository(self) -> Path:
        root = Path(tempfile.mkdtemp(prefix="m3-jep484-map-"))
        git(root, "init", "-q")
        git(root, "checkout", "-q", "-b", "j21")
        prefix = root / mod.J21
        prefix.mkdir(parents=True)
        (prefix / "ClassfileFoo.java").write_text(
            "package jdk.internal.classfile; class ClassfileFoo {}\n",
            encoding="utf-8",
        )
        (prefix / "Keep.java").write_text(
            "package jdk.internal.classfile; class Keep {}\n",
            encoding="utf-8",
        )
        (prefix / "Gone.java").write_text(
            "package jdk.internal.classfile; class Gone {}\n",
            encoding="utf-8",
        )
        git(root, "add", ".")
        git(root, "commit", "-q", "-m", "jdk21")
        git(root, "tag", "jdk-21+35")

        git(root, "checkout", "-q", "-b", "j24")
        git(root, "rm", "-q", "-r", "src/java.base/share/classes/jdk/internal/classfile")
        public = root / mod.J24_PUBLIC
        internal = root / mod.J24_INTERNAL
        public.mkdir(parents=True)
        internal.mkdir(parents=True)
        (public / "ClassFileFoo.java").write_text(
            "package java.lang.classfile; class ClassFileFoo {}\n",
            encoding="utf-8",
        )
        (internal / "Keep.java").write_text(
            "package jdk.internal.classfile; class Keep {}\n",
            encoding="utf-8",
        )
        (public / "Added.java").write_text(
            "package java.lang.classfile; "
            "class Added { int v = JAVA_24_VERSION + 68; Object r = RELEASE_24; }\n",
            encoding="utf-8",
        )
        git(root, "add", ".")
        git(root, "commit", "-q", "-m", "jdk24")
        git(root, "tag", "jdk-24+36")
        return root

    def test_exact_git_mapping_hashes_and_version_signals_are_deterministic(self) -> None:
        repo = self.repository()
        with tempfile.TemporaryDirectory() as temp:
            out = Path(temp)
            summary = mod.run(repo, "jdk-21+35", "jdk-24+36", out)

            self.assertEqual(3, summary["jdk21_internal_files"])
            self.assertEqual(3, summary["jdk24_combined_files"])
            self.assertEqual(2, summary["direct_descendants"])
            self.assertEqual(1, summary["unmapped_jdk21"])
            self.assertEqual(0, summary["ambiguous"])
            self.assertEqual(1, summary["added_jdk24"])
            self.assertFalse(summary["mutation_authority"])
            self.assertFalse(summary["promotion_authority"])

            mapped = (out / "JDK21_INTERNAL_TO_JDK24.tsv").read_text(encoding="utf-8")
            self.assertIn("ClassfileFoo.java", mapped)
            self.assertIn("ClassFileFoo.java", mapped)
            self.assertIn("DIRECT_DESCENDANT", mapped)

            removed = (out / "UNMAPPED_JDK21.tsv").read_text(encoding="utf-8")
            self.assertIn("Gone.java", removed)
            added = (out / "ADDED_JDK24.tsv").read_text(encoding="utf-8")
            self.assertIn("Added.java", added)

            signals = (out / "VERSION_SIGNALS.tsv").read_text(encoding="utf-8")
            self.assertIn("JAVA_24_VERSION", signals)
            self.assertIn("RELEASE_24", signals)
            self.assertIn("CLASSFILE_MAJOR_LITERAL", signals)
            self.assertIn("\t68\t", signals)

            first = hashlib.sha256(
                b"".join(path.read_bytes() for path in sorted(out.iterdir()))
            ).hexdigest()
            second = Path(temp) / "again"
            mod.run(repo, "jdk-21+35", "jdk-24+36", second)
            compare = hashlib.sha256(
                b"".join(path.read_bytes() for path in sorted(second.iterdir()))
            ).hexdigest()
            self.assertEqual(first, compare)

    def test_ambiguous_mapping_remains_explicit(self) -> None:
        source = [
            mod.FileRow("s/A.java", "A.java", "A.java", "1" * 64, "JDK21_INTERNAL")
        ]
        donor = [
            mod.FileRow("public/A.java", "A.java", "A.java", "2" * 64, "JDK24_PUBLIC"),
            mod.FileRow("internal/A.java", "A.java", "A.java", "3" * 64, "JDK24_INTERNAL"),
        ]
        mapped, unmapped, ambiguous, added = mod.map_rows(source, donor)
        self.assertEqual([], mapped)
        self.assertEqual([], unmapped)
        self.assertEqual(1, len(ambiguous))
        self.assertEqual([], added)
        self.assertEqual(
            ["internal/A.java", "public/A.java"],
            [row.path for row in ambiguous[0][1]],
        )

    def test_normalization_is_deliberately_narrow(self) -> None:
        self.assertEqual("ClassFileFoo.java", mod.normalize("ClassfileFoo.java"))
        self.assertEqual("ClassFile/Foo.java", mod.normalize("Classfile/Foo.java"))
        self.assertEqual("classfile/Foo.java", mod.normalize("classfile/Foo.java"))

    def test_missing_ref_fails_closed(self) -> None:
        repo = self.repository()
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaises(ValueError):
                mod.run(repo, "missing-ref", "jdk-24+36", Path(temp))


if __name__ == "__main__":
    unittest.main()
