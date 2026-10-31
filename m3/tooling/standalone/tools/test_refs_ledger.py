#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""Tests for REFS_LEDGER v2 and for the LINEAGE_LEDGER_REF v1 consumers it introduced.

Run from the repository root: python3 -m unittest -v m3/tooling/standalone/tools/test_refs_ledger.py
"""
from __future__ import annotations

import csv
import importlib.util
import re
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parent
REPO = TOOLS.parents[3]
LEDGER = REPO / "m3" / "tooling" / "standalone" / "LINEAGE_SOURCES.tsv"
LEDGER_KEY = "synexia"
WORKFLOWS = REPO / ".github" / "workflows"
PORT = REPO / "m3" / "ports" / "collections"
PORT_VERIFIER = PORT / "verify_collection_port_wave1.py"
PASS_MARKER = "M3_COLLECTION_PORT_WAVE1_PASS"
DRIFTED = "example/drifted-repository"
INLINE = re.compile(r"python3 - <<'PY'\n(.*?)\n\s*PY\n", re.S)
STEP_TIMEOUT = 60

spec = importlib.util.spec_from_file_location("refs_ledger", TOOLS / "refs_ledger.py")
refs = importlib.util.module_from_spec(spec)
spec.loader.exec_module(refs)


def donor_repository() -> str:
    with LEDGER.open(encoding="utf-8", newline="") as handle:
        return {row["key"]: row for row in csv.DictReader(handle, delimiter="\t")}[LEDGER_KEY]["repository"]


def write_tsv(path: Path, header: tuple[str, ...], rows: list[tuple[str, ...]]) -> None:
    path.write_text("\n".join("\t".join(r) for r in [header, *rows]) + "\n", encoding="utf-8", newline="\n")


def rewrite_ledger(root: Path, repository: str | None) -> None:
    rows = [] if repository is None else [(LEDGER_KEY, repository, "test", "test")]
    target = root / LEDGER.relative_to(REPO)
    target.parent.mkdir(parents=True, exist_ok=True)
    write_tsv(target, ("key", "repository", "role", "evidence"), rows)


def inline_script(workflow: str) -> str:
    text = (WORKFLOWS / workflow).read_text(encoding="utf-8")
    script = next(m.group(1) for m in INLINE.finditer(text) if "LINEAGE_SOURCES.tsv" in m.group(1))
    indent = min(len(line) - len(line.lstrip()) for line in script.splitlines() if line.strip())
    return "\n".join(line[indent:] for line in script.splitlines()) + "\n"


def run(script_or_file, cwd: Path) -> subprocess.CompletedProcess:
    argv = [sys.executable, "-I", "-B"] + (["-c", script_or_file] if isinstance(script_or_file, str) else [str(script_or_file)])
    return subprocess.run(argv, cwd=cwd, capture_output=True, text=True, timeout=STEP_TIMEOUT)


class RuleEngineTest(unittest.TestCase):
    HEADER = ("kind", "path", "line", "text")

    def rules(self, td: Path, *extra: tuple[str, ...]) -> Path:
        path = td / "rules.tsv"
        write_tsv(path, refs.RULE_FIELDS, [
            ("10", "1", "^D_REPO_REF$", "^a\\.yml$", ".", "A", "ABSORB", "act-a", "ev-a"),
            ("20", "0", "^D_", ".", "lineage", "B", "NEXT", "act-b", "ev-b"),
            *extra,
        ])
        return path

    def test_first_match_wins_and_state_follows_the_after_scan(self):
        with tempfile.TemporaryDirectory() as name:
            td = Path(name)
            write_tsv(td / "before.tsv", self.HEADER, [("D_REPO_REF", "a.yml", "3", "x lineage"),
                                                       ("D_REPO_REF", "b.py", "9", "y lineage"),
                                                       ("N_NAME", "c.py", "1", "ignored")])
            write_tsv(td / "after.tsv", self.HEADER, [("D_REPO_REF", "b.py", "12", "y lineage")])
            rows = refs.build(refs.dependency_findings(td / "before.tsv"), refs.dependency_findings(td / "after.tsv"),
                              refs.load_rules(self.rules(td)))
            body = {r[0]: r for r in rows if r[2] != refs.COUNT_DECISION}
            self.assertEqual(("A", "RESOLVED"), (body["a.yml"][6], body["a.yml"][7]))
            self.assertEqual(("B", "REMAINS"), (body["b.py"][6], body["b.py"][7]))
            counts = {(r[0], r[1]): r[3] for r in rows if r[2] == refs.COUNT_DECISION}
            self.assertEqual("before=2 after=1", counts[(refs.FULL_SCOPE, "D_REPO_REF")])
            self.assertEqual("before=1 after=0", counts[(refs.LEAF_SCOPE, "D_REPO_REF")])

    def test_after_only_findings_new_kinds_and_moved_paths_are_counted_and_listed(self):
        # review of #2236: v1 took "after" from the REMAINS rows and dropped every after-only finding
        with tempfile.TemporaryDirectory() as name:
            td = Path(name)
            write_tsv(td / "before.tsv", self.HEADER, [("D_REPO_REF", "a.yml", "3", "x lineage"),
                                                       ("D_REPO_REF", "old/m.py", "4", "m lineage")])
            write_tsv(td / "after.tsv", self.HEADER, [("D_REPO_REF", "a.yml", "9", "x lineage"),
                                                      ("D_REPO_REF", "a.yml", "3", "x lineage"),
                                                      ("D_REPO_REF", "moved/m.py", "4", "m lineage"),
                                                      ("D_REPO_REF", "new.yml", "1", "git clone https://example.invalid/d"),
                                                      ("D_SECRET", "new.yml", "2", "secrets.DONOR_TOKEN")])
            rules = self.rules(td, ("30", "0", "^D_", ".", ".", "C", "NEXT", "act-c", "ev-c"))
            rows = refs.build(refs.dependency_findings(td / "before.tsv"), refs.dependency_findings(td / "after.tsv"),
                              refs.load_rules(rules))
            body = [(r[0], r[5], r[1], r[6], r[7]) for r in rows if r[2] != refs.COUNT_DECISION]
            self.assertEqual([("a.yml", "3", "D_REPO_REF", "A", "REMAINS"),
                              ("a.yml", "9", "D_REPO_REF", "A", "NEW"),
                              ("moved/m.py", "4", "D_REPO_REF", "B", "NEW"),
                              ("new.yml", "1", "D_REPO_REF", "C", "NEW"),
                              ("new.yml", "2", "D_SECRET", "C", "NEW"),
                              ("old/m.py", "4", "D_REPO_REF", "B", "RESOLVED")], body)
            counts = [(r[0], r[1], r[3]) for r in rows if r[2] == refs.COUNT_DECISION]
            self.assertEqual([(refs.FULL_SCOPE, "D_REPO_REF", "before=2 after=4"),
                              (refs.FULL_SCOPE, "D_SECRET", "before=0 after=1"),
                              (refs.LEAF_SCOPE, "D_REPO_REF", "before=1 after=2"),
                              (refs.LEAF_SCOPE, "D_SECRET", "before=0 after=0")], counts)

    def test_unmatched_finding_and_header_drift_are_refused(self):
        with tempfile.TemporaryDirectory() as name:
            td = Path(name)
            write_tsv(td / "f.tsv", self.HEADER, [("D_SECRET", "z.yml", "1", "no rule text")])
            rules = td / "narrow.tsv"
            write_tsv(rules, refs.RULE_FIELDS, [("1", "0", "^D_REPO_REF$", ".", ".", "A", "HOLD", "a", "e")])
            with self.assertRaises(ValueError):
                refs.build(refs.dependency_findings(td / "f.tsv"), [], refs.load_rules(rules))
            with self.assertRaises(ValueError):  # v2: after-only findings are classified as well
                refs.build([], refs.dependency_findings(td / "f.tsv"), refs.load_rules(rules))
            write_tsv(rules, ("order", "kind"), [("1", "^D_")])
            with self.assertRaises(ValueError):
                refs.load_rules(rules)

    def test_ledger_is_a_fixed_point(self):
        with tempfile.TemporaryDirectory() as name:
            td = Path(name)
            write_tsv(td / "b.tsv", self.HEADER, [("D_REPO_REF", "a.yml", "3", "x lineage")])
            argv = ["--before", str(td / "b.tsv"), "--after", str(td / "b.tsv"), "--rules", str(self.rules(td)),
                    "--out", str(td / "ledger.tsv")]
            self.assertEqual(refs.EXIT_OK, refs.main(argv))
            first = (td / "ledger.tsv").read_bytes()
            self.assertEqual(refs.EXIT_OK, refs.main(argv + ["--check"]))
            self.assertEqual(refs.EXIT_OK, refs.main(argv))
            self.assertEqual(first, (td / "ledger.tsv").read_bytes())
            (td / "ledger.tsv").write_text("drift\n", encoding="utf-8")
            self.assertEqual(refs.EXIT_DIFFERS, refs.main(argv + ["--check"]))


class LineageLedgerTest(unittest.TestCase):
    def test_ledger_names_the_donor_once(self):
        with LEDGER.open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))
        self.assertEqual(["key", "repository", "role", "evidence"], list(rows[0]))
        self.assertEqual([LEDGER_KEY], [r["key"] for r in rows])
        self.assertRegex(rows[0]["repository"], r"^[\w.-]+/[\w.-]+$")

    def test_live_workflows_fetch_no_donor_and_read_no_donor_secret(self):
        donor = re.escape(donor_repository())
        fetch = re.compile(rf"repository:\s*['\"]?{donor}\b|codeload\.github\.com/{donor}\b|github\.com/{donor}\.git"
                           rf"|gh repo clone {donor}\b|secrets\.(M3_)?SYNEXIA_\w+")
        hits = [f"{p.name}:{n}" for p in sorted(WORKFLOWS.glob("*.y*ml"))
                for n, line in enumerate(p.read_text(encoding="utf-8").splitlines(), 1) if fetch.search(line)]
        self.assertEqual([], hits)

    def port_tree(self, td: Path) -> Path:
        root = td / "repo"
        shutil.copytree(PORT, root / PORT.relative_to(REPO))
        rewrite_ledger(root, donor_repository())
        return root

    def test_collection_port_verifier_reads_the_ledger(self):
        with tempfile.TemporaryDirectory() as name:
            root = self.port_tree(Path(name))
            verifier = root / PORT_VERIFIER.relative_to(REPO)
            self.assertIn(PASS_MARKER, run(verifier, root).stdout)
            rewrite_ledger(root, DRIFTED)
            self.assertIn("unexpected source repository", run(verifier, root).stderr)
            rewrite_ledger(root, None)
            self.assertIn("missing lineage source", run(verifier, root).stderr)

    def test_collection_port_verifier_refuses_drifted_provenance(self):
        with tempfile.TemporaryDirectory() as name:
            root = self.port_tree(Path(name))
            provenance = root / PORT.relative_to(REPO) / "PROVENANCE.tsv"
            lines = provenance.read_text(encoding="utf-8").splitlines()
            lines[1] = lines[1].replace(donor_repository(), DRIFTED, 1)
            provenance.write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")
            result = run(root / PORT_VERIFIER.relative_to(REPO), root)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("unexpected source repository", result.stderr)

    def receiver_tree(self, td: Path, receiver: str, with_header: bool) -> Path:
        root = td / "repo"
        source = REPO / receiver
        target = root / receiver
        target.parent.mkdir(parents=True)
        body = source.read_text(encoding="utf-8")
        if with_header and not body.startswith("field\tvalue\n"):
            body = "field\tvalue\n" + body
        target.write_text(body, encoding="utf-8", newline="\n")
        rewrite_ledger(root, donor_repository())
        return root

    def check_receiver_step(self, workflow: str, receiver: str):
        script = inline_script(workflow)
        with tempfile.TemporaryDirectory() as name:
            root = self.receiver_tree(Path(name), receiver, with_header=True)
            self.assertEqual(0, run(script, root).returncode, run(script, root).stderr)
            rewrite_ledger(root, DRIFTED)
            self.assertIn("receiver drift", run(script, root).stderr)

    def test_jep493_receiver_step_reads_the_ledger(self):
        self.check_receiver_step("m3-jep493-runtime-image-materialize.yml",
                                 "m3/backports/recipes/jep-493-runtime-image/WORKFLOW_VALIDATOR_RECEIVER.tsv")

    def test_jep485_receiver_step_reads_the_ledger(self):
        # the j485 receiver lacks its field/value header (pre-existing); the step logic is tested on a headed copy
        self.check_receiver_step("m3-jep485-stream-gatherers.yml",
                                 "m3/backports/recipes/j485/WORKFLOW_VALIDATOR_RECEIVER.tsv")


if __name__ == "__main__":
    unittest.main()
