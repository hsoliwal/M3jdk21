#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import importlib.util
import io
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


def load_inventory_module():
    path = Path(__file__).with_name("inventory.py")
    spec = importlib.util.spec_from_file_location("m3_backport_inventory", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class InventoryTest(unittest.TestCase):
    def setUp(self) -> None:
        self.inv = load_inventory_module()

    @staticmethod
    def git(repo: Path, *args: str) -> None:
        subprocess.run(
            ("git", "-C", str(repo), *args),
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
        )

    def test_tool_and_language_classification(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            repo = Path(temp)
            subprocess.run(("git", "init", "-q", str(repo)), check=True)
            self.git(repo, "config", "user.email", "fixture@example.invalid")
            self.git(repo, "config", "user.name", "Fixture")

            (repo / "README").write_text("base\n", encoding="utf-8")
            self.git(repo, "add", "README")
            self.git(repo, "commit", "-q", "-m", "base")
            self.git(repo, "tag", "jdk-21+35")

            tool = repo / "src/jdk.jcmd/share/conf/bash-completion"
            tool.mkdir(parents=True)
            (tool / "jcmd").write_text("complete\n", encoding="utf-8")
            self.git(repo, "add", ".")
            self.git(
                repo,
                "commit",
                "-q",
                "-m",
                "JDK-8357439: Add bash autocompletion for jcmd",
            )
            self.git(repo, "tag", "jdk-22+36")

            parser = (
                repo
                / "src/jdk.compiler/share/classes/com/sun/tools/javac/parser"
            )
            parser.mkdir(parents=True)
            (parser / "JavacParser.java").write_text("parser\n", encoding="utf-8")
            self.git(repo, "add", ".")
            self.git(
                repo,
                "commit",
                "-q",
                "-m",
                "JDK-8000001: Parser language change",
            )
            self.git(repo, "tag", "jdk-23+37")

            for tag in ("jdk-24+36", "jdk-25+36", "jdk-26+35", "jdk-27+35"):
                self.git(repo, "tag", tag)

            self.inv._verify_repo(repo)
            changes = self.inv.inventory(repo, range(22, 28))
            self.assertEqual(2, len(changes))
            self.assertEqual("review-tool", changes[0].disposition)
            self.assertEqual(("JDK-8357439",), changes[0].jbs_ids)
            self.assertEqual("hold-language", changes[1].disposition)
            self.assertTrue(changes[1].grammar_touch)

            output = io.StringIO()
            self.inv.write_tsv(changes, output)
            text = output.getvalue()
            self.assertIn("JDK-8357439", text)
            self.assertIn("hold-language", text)


if __name__ == "__main__":
    unittest.main()
