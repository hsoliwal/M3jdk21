# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Real-Git regression fixtures for fail-closed product release admission.

The positive evidence below is deliberately synthetic. It proves receipt
validation only; it is never a release, JDK/JNI, benchmark or recipe receipt.
"""
from __future__ import annotations

import contextlib
import copy
import csv
import hashlib
import importlib.util
import io
import json
import os
import runpy
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

MODULE_PATH = Path(__file__).with_name("verify_release_admission.py")
spec = importlib.util.spec_from_file_location("m3_release_admission", MODULE_PATH)
release = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(release)


class ReleaseAdmissionUnitTest(unittest.TestCase):
    def test_unique_json_rejects_duplicates(self):
        with self.assertRaises(ValueError):
            json.loads('{"a":1,"a":2}', object_pairs_hook=release.unique_object)

    def test_contract_priority_is_exact(self):
        contract = json.loads((Path(__file__).with_name("release-contract.json")).read_text())
        self.assertEqual(release.EXPECTED_PRIORITY, contract["priority_order"])

    def test_unqualified_claims_remain_not_claimed(self):
        contract = json.loads((Path(__file__).with_name("release-contract.json")).read_text())
        for key in release.UNQUALIFIED_CLAIMS:
            self.assertEqual("NOT_CLAIMED", contract["current_claims"][key])

    def test_default_evidence_location_matches_visible_queue_contract(self):
        contract = json.loads((Path(__file__).with_name("release-contract.json")).read_text())
        self.assertEqual(release.DEFAULT_EVIDENCE, contract["execution_evidence"]["default_manifest"])
        self.assertEqual(release.DEFAULT_EVIDENCE, "m3/release/queue/current/evidence.json")

    def test_sha256_is_stable(self):
        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / "x"
            path.write_bytes(b"m3\n")
            self.assertEqual(
                "3831561589b9de83145d35aebf9d2b55a9843a09e8b7754bedd56173e25b3aa6",
                release.sha256(path),
            )

    def test_preview_name_matches_the_configured_jdk_version(self):
        root = Path(__file__).resolve().parents[2]
        contract = json.loads(Path(__file__).with_name("release-contract.json").read_text())
        text = (root / "make/conf/version-numbers.conf").read_text()
        release.release_identity(contract["release"], text)
        self.assertEqual(contract["release"]["tag"], "m3jdk-21.0.14-m3.1-preview")

    def test_preview_identity_drift_is_refused(self):
        contract = json.loads(Path(__file__).with_name("release-contract.json").read_text())
        text = "DEFAULT_VERSION_FEATURE=21\nDEFAULT_VERSION_INTERIM=0\n" \
               "DEFAULT_VERSION_UPDATE=14\nDEFAULT_VERSION_PATCH=0\n"
        for field, value in (("version", "22.0.14"), ("version", "21.0.14\n"),
                             ("release_branch", "release/first-clean-boundary-20261007"),
                             ("tag", "m3jdk-21.0.15-m3.1-preview"),
                             ("display_name", "M3 + JDK")):
            candidate = dict(contract["release"], **{field: value})
            with self.subTest(field=field, value=value):
                with self.assertRaisesRegex(ValueError, "disagree"):
                    release.release_identity(candidate, text)

    def test_changed_duplicate_or_missing_build_version_is_refused(self):
        contract = json.loads(Path(__file__).with_name("release-contract.json").read_text())
        text = "DEFAULT_VERSION_FEATURE=21\nDEFAULT_VERSION_INTERIM=0\n" \
               "DEFAULT_VERSION_UPDATE=14\nDEFAULT_VERSION_PATCH=0\n"
        for candidate in (text.replace("UPDATE=14", "UPDATE=15"),
                          text + "DEFAULT_VERSION_FEATURE=21\n",
                          text.replace("DEFAULT_VERSION_PATCH=0\n", "")):
            with self.subTest(text=candidate):
                with self.assertRaises(ValueError):
                    release.release_identity(contract["release"], candidate)

    def test_nonzero_patch_version_requires_matching_four_component_identity(self):
        contract = json.loads(Path(__file__).with_name("release-contract.json").read_text())
        candidate = dict(contract["release"], version="21.0.14.3",
                         tag="m3jdk-21.0.14.3-m3.1-preview",
                         release_branch="release/m3jdk-21.0.14.3-m3.1-preview")
        text = "DEFAULT_VERSION_FEATURE=21\nDEFAULT_VERSION_INTERIM=0\n" \
               "DEFAULT_VERSION_UPDATE=14\nDEFAULT_VERSION_PATCH=3\n"
        release.release_identity(candidate, text)
        for changed in (text.replace("PATCH=3", "PATCH=0"),
                        text.replace("PATCH=3", "PATCH=4")):
            with self.subTest(configuration=changed):
                with self.assertRaisesRegex(ValueError, "differs from make/conf"):
                    release.release_identity(candidate, changed)

    def test_artifact_directory_swap_during_validation_is_refused(self):
        with tempfile.TemporaryDirectory(prefix="m3-artifact-swap-") as td:
            root = Path(td)
            base, outside = root / "evidence", root / "outside"
            directory = base / "reports"
            directory.mkdir(parents=True)
            outside.mkdir()
            artifact = directory / "run.log"
            artifact.write_bytes(b"declared evidence\n")
            (outside / "run.log").write_bytes(b"different external evidence\n")
            original_is_symlink = Path.is_symlink
            swapped = False

            def swap_after_component_check(path):
                nonlocal swapped
                result = original_is_symlink(path)
                if path == artifact and not swapped:
                    # Model an external directory replacement after the component
                    # checks. The final containment check must still refuse it.
                    directory.rename(base / "retained-reports")
                    directory.symlink_to(outside, target_is_directory=True)
                    swapped = True
                return result

            with mock.patch.object(Path, "is_symlink", autospec=True,
                                   side_effect=swap_after_component_check):
                with self.assertRaisesRegex(ValueError, "escapes manifest directory"):
                    release.safe_artifact(base, "reports/run.log")
            self.assertTrue(swapped)
            self.assertEqual((outside / "run.log").read_bytes(), b"different external evidence\n")


class ReleaseEvidenceGitTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(prefix="m3-release-test-")
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name) / "repository"
        self.root.mkdir()
        self.git("init", "-q")
        self.write("upstream.txt", "synthetic upstream\n")
        self.commit("synthetic upstream")
        upstream = self.git("rev-parse", "HEAD")
        self.contract = json.loads(Path(__file__).with_name("release-contract.json").read_text())
        self.contract["release"]["upstream_baseline"] = upstream
        # these fixtures test receipt integrity; the standalone rule has its own fixture below
        self.contract.setdefault("standalone", {})["required_for_release"] = False
        self.write("m3/release/release-contract.json", json.dumps(self.contract))
        self.write(".gitignore", "/target/\n")
        self.write("m3/release/queue/.gitignore", "/current/\n")
        self.write("M3-SYNEXIA-NOTICE.md", "This notice does not relicense\n"
                   "OpenJDK or JDK-derived source code as Apache-2.0.")
        self.write("M3-SYNEXIA-RIGHTS.md", "All ownership and all rights not expressly granted by the applicable license")
        self.write("M3-SYNEXIA-AUTHORS.md", "Synthetic rights fixture; not a product notice")
        self.write("LICENSE-M3-APACHE-2.0.txt", "Synthetic license fixture")
        self.write("m3/README.md", "does **not** modify `java.lang.String`\n"
                   "No speedup, complete compatibility, or full jtreg pass is claimed.")
        self.write("m3/release/README.md", "Synthetic release fixture")
        self.write("m3/release/BENCHMARK_AND_COMPATIBILITY_SPEC.md", "No benchmark result")
        self.write("make/conf/version-numbers.conf",
                   "DEFAULT_VERSION_FEATURE=21\nDEFAULT_VERSION_INTERIM=0\n"
                   "DEFAULT_VERSION_UPDATE=14\nDEFAULT_VERSION_PATCH=0\n")
        self.write("src/example.java", "class Example {}\n")
        mapping = {
            "schema": 1,
            "porting_invariant": {"receiving_sequence": {
                "order": release.EXPECTED_RECEIVING_ORDER, "active_phase": "STRING"}},
            "migration": {"records": [{"id": "fixture.owner", "targets": []}]},
        }
        self.write(release.NAME_MAP, json.dumps(mapping))
        self.repin_queue()
        self.commit("synthetic release tree")
        self.evidence_dir = Path(self.tmp.name) / "evidence"
        self.evidence_dir.mkdir()
        self.manifest = self.evidence_dir / "evidence.json"
        self.evidence = {
            "schema": release.EVIDENCE_SCHEMA,
            "source_commit": self.git("rev-parse", "HEAD"),
            "source_tree": self.git("rev-parse", "HEAD^{tree}"),
            "gates": [],
        }
        for ident in self.contract["mandatory_release_gates"]:
            artifact = self.evidence_dir / (
                ident + (".xml" if ident == "M3_CHANGED_ATOM_COVERAGE" else ".log"))
            if ident == "M3_CHANGED_ATOM_COVERAGE":
                artifact.write_text(
                    '<report name="synthetic"><class name="fixture/Example">'
                    '<method name="run" desc="()V">'
                    '<counter type="LINE" covered="99" missed="1"/>'
                    '<counter type="BRANCH" covered="99" missed="1"/>'
                    '</method></class></report>')
            else:
                artifact.write_text("SYNTHETIC VALIDATOR FIXTURE ONLY " + ident + "\n")
            gate = {
                "id": ident, "status": "PASS", "exit_code": 0,
                "owner_repository": release.gate_owner(ident),
                "command": ["synthetic-fixture", ident],
                "environment": {"os": "fixture-os", "architecture": "fixture-arch", "toolchain": "fixture-toolchain"},
                "artifacts": [{"path": artifact.name, "sha256": release.sha256(artifact)}],
            }
            if ident == "M3_CHANGED_ATOM_COVERAGE":
                inventory_file = self.evidence_dir / (ident + ".tsv")
                inventory_file.write_text(
                    "id\tclass\tmethod\tdescriptor\tsource_path\tbranchless_explanation\n"
                    "fixture.Example#run\tfixture/Example\trun\t()V\tsrc/example.java\t\n")
                gate["artifacts"].append({
                    "path": inventory_file.name, "sha256": release.sha256(inventory_file)})
                gate["coverage"] = {
                    "schema": release.COVERAGE_SCHEMA, "scope": release.COVERAGE_SCOPE,
                    "format": "JACOCO_XML", "instrumented": True, "inventory_complete": True,
                    "unmeasured_count": 0, "excluded_atoms": [],
                    "tool": "synthetic instrumentation fixture", "report_artifact": artifact.name,
                    "inventory_artifact": inventory_file.name,
                    "inventory_sha256": release.sha256(inventory_file),
                    "measured_count": 1, "atoms": [{
                        "id": "fixture.Example#run",
                        "source_path": "src/example.java",
                        "source_sha256": release.sha256(self.root / "src/example.java"),
                        "jacoco_class": "fixture/Example",
                        "jacoco_method": "run", "jacoco_descriptor": "()V",
                        "line_covered": 99, "line_missed": 1,
                        "branch_covered": 99, "branch_missed": 1,
                    }],
                }
            if ident == "M3_MAVEN_CONTROL_REACTOR":
                gate["canonical_recipe"] = {
                    "repository": release.CANONICAL_RECIPE_REPOSITORY, "commit": "1" * 40,
                    "recipe_id": "synthetic.fixture.only",
                }
            self.evidence["gates"].append(gate)

    def write(self, path, text):
        file = self.root / path
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(text, encoding="utf-8")

    def git(self, *args):
        return subprocess.check_output(
            ["git", "-C", str(self.root), "-c", "user.name=Release Fixture",
             "-c", "user.email=release-fixture@example.invalid", *args],
            text=True, stderr=subprocess.PIPE).strip()

    def commit(self, message):
        self.git("add", ".")
        self.git("commit", "-qm", message)

    def repin_queue(self):
        raw = (self.root / release.NAME_MAP).read_bytes()
        blob = hashlib.sha1(b"blob " + str(len(raw)).encode("ascii") + b"\0" + raw).hexdigest()
        self.write(release.NAME_PLAN, json.dumps({
            "schema": "M3JDK21_NAME_CONTRACT_PLAN_V1", "authority": release.NAME_MAP,
            "authority_blob": blob, "operations": [{
                "mapping_id": "fixture.owner", "state": "QUEUED", "kind": "FORMAT_ONLY",
                "before_type": "com.m3.Fixture", "after_type": "com.m3.Fixture",
                "reason": "Excluded synthetic candidate; no product naming change is admitted.",
            }],
        }))

    def save_evidence(self):
        self.manifest.write_text(json.dumps(self.evidence), encoding="utf-8")

    def result(self, release_mode=True):
        self.save_evidence()
        return release.validate(self.root, release_mode, self.manifest)

    def assert_blocked(self, result, name=None):
        self.assertFalse(result["release_ready"], result)
        if name:
            self.assertIn(name, {b["name"] for b in result["blockers"]}, result)

    def coverage_gate(self):
        return next(g for g in self.evidence["gates"] if g["id"] == release.COVERAGE_GATE)

    def write_coverage_artifact(self, field, content):
        gate = self.coverage_gate()
        path = self.evidence_dir / gate["coverage"][field]
        path.write_text(content, encoding="utf-8")
        digest = release.sha256(path)
        next(item for item in gate["artifacts"] if item["path"] == path.name)["sha256"] = digest
        if field == "inventory_artifact":
            gate["coverage"]["inventory_sha256"] = digest

    def change_atom_binding(self, field, value):
        gate = self.coverage_gate()
        gate["coverage"]["atoms"][0][field] = value
        path = self.evidence_dir / gate["coverage"]["inventory_artifact"]
        reader = csv.DictReader(io.StringIO(path.read_text()), delimiter="\t")
        rows = list(reader)
        column = {"jacoco_class": "class", "jacoco_method": "method",
                  "jacoco_descriptor": "descriptor"}.get(field, field)
        rows[0][column] = value
        text = io.StringIO()
        writer = csv.DictWriter(text, fieldnames=reader.fieldnames, delimiter="\t")
        writer.writeheader()
        writer.writerows(rows)
        self.write_coverage_artifact("inventory_artifact", text.getvalue())

    def assert_coverage_refused(self, detail=None):
        result = self.result()
        self.assert_blocked(result, "gate:" + release.COVERAGE_GATE)
        if detail:
            blocker = next(row for row in result["blockers"]
                           if row["name"] == "gate:" + release.COVERAGE_GATE)
            self.assertIn(detail, blocker["detail"])

    def test_missing_all_execution_evidence_never_reports_ready(self):
        for mode in (False, True):
            with self.subTest(release=mode):
                result = release.validate(self.root, mode, self.manifest)
                self.assertTrue(result["policy_valid"], result)
                self.assert_blocked(result, "evidence.manifest")
                pending = [q for q in result["queue"] if q["id"].startswith("gate:")]
                self.assertEqual(len(pending), len(self.contract["mandatory_release_gates"]))

    def test_complete_current_synthetic_receipts_validate(self):
        result = self.result()
        self.assertTrue(result["policy_valid"], result)
        self.assertTrue(result["release_ready"], result)
        queued = [q for q in result["queue"] if q["id"].startswith("naming:")]
        self.assertEqual(len(queued), 1)
        self.assertEqual(queued[0]["state"], "QUEUED")
        self.assertFalse(queued[0]["blocking"])
        self.assertEqual(result["source_commit"], self.git("rev-parse", "HEAD"))

    def test_coverage_receipt_is_source_pinned_and_fail_closed(self):
        item = next(g for g in self.evidence["gates"]
                    if g["id"] == release.COVERAGE_GATE)
        receipt = item["coverage"]
        scenarios = [
            ("missing receipt", lambda: item.pop("coverage"), "coverage receipt"),
            ("missing instrumented proof", lambda: receipt.__setitem__("instrumented", False),
             "instrumentation"),
            ("incomplete inventory", lambda: receipt.__setitem__("inventory_complete", False),
             "complete atom inventory"),
            ("unmeasured", lambda: receipt.__setitem__("unmeasured_count", 1),
             "unmeasured changed atoms"),
            ("excluded", lambda: receipt.__setitem__("excluded_atoms", ["fixture"]),
             "excluded atoms"),
            ("wrong file digest", lambda: receipt["atoms"][0].__setitem__("source_sha256", "f" * 64),
             "source hash mismatch"),
            ("below branch threshold", lambda: receipt["atoms"][0].__setitem__("branch_covered", 8),
             "branch coverage below 90%"),
            ("below line threshold", lambda: receipt["atoms"][0].__setitem__("line_covered", 8),
             "line coverage below 90%"),
            ("missing report", lambda: receipt.__setitem__("report_artifact", "not-an-artifact.xml"),
             "raw report artifact"),
            ("inventory checksum mismatch", lambda: receipt.__setitem__("inventory_sha256", "f" * 64),
             "coverage inventory artifact"),
            ("invented numbers", lambda: receipt["atoms"][0].__setitem__("line_covered", 100),
             "reported atom counts disagree with raw JaCoCo"),
        ]
        import copy
        original = copy.deepcopy(item)
        for name, change, detail in scenarios:
            item.clear()
            item.update(copy.deepcopy(original))
            receipt = item["coverage"]
            with self.subTest(name=name):
                change()
                result = self.result()
                self.assert_blocked(result, "gate:" + release.COVERAGE_GATE)
                blocker = next(b["detail"] for b in result["blockers"]
                               if b["name"] == "gate:" + release.COVERAGE_GATE)
                self.assertIn(detail, blocker)
        item.clear()
        item.update(original)

    def test_coverage_policy_is_not_downgradable(self):
        self.contract["coverage_policy"]["minimum_line_ratio"] = 0.0
        self.write("m3/release/release-contract.json", json.dumps(self.contract))
        self.assert_blocked(self.result(), "contract.coverage_policy")

    def test_branchless_atom_requires_an_explicit_reason(self):
        item = next(g for g in self.evidence["gates"]
                    if g["id"] == release.COVERAGE_GATE)
        atom = item["coverage"]["atoms"][0]
        artifact_path = self.evidence_dir / item["coverage"]["report_artifact"]
        artifact_path.write_text(artifact_path.read_text().replace(
            '<counter type="BRANCH" covered="99" missed="1"/>',
            '<counter type="BRANCH" covered="0" missed="0"/>'))
        report_row = next(a for a in item["artifacts"] if a["path"] == artifact_path.name)
        report_row["sha256"] = release.sha256(artifact_path)
        atom["branch_covered"] = 0
        atom["branch_missed"] = 0
        self.assert_blocked(self.result(), "gate:" + release.COVERAGE_GATE)
        atom["branchless_explanation"] = "No control flow in immutable field accessor fixture"
        result = self.result()
        self.assertTrue(result["release_ready"], result)

    def test_malformed_coverage_container_fields_are_reported_without_crash(self):
        item = next(g for g in self.evidence["gates"] if g["id"] == release.COVERAGE_GATE)
        original = copy.deepcopy(item)
        scenarios = [
            ("null artifacts", lambda gate: gate.update(artifacts=None)),
            ("unhashable artifact path", lambda gate: gate["artifacts"][0].update(path=[])),
            ("unhashable atom ID", lambda gate: gate["coverage"]["atoms"][0].update(id=[])),
            ("unhashable source path", lambda gate: gate["coverage"]["atoms"][0].update(source_path={})),
            ("unhashable method key", lambda gate: gate["coverage"]["atoms"][0].update(jacoco_method=[])),
            ("non-object atom", lambda gate: gate["coverage"].update(atoms=[None])),
        ]
        for name, mutate in scenarios:
            with self.subTest(name=name):
                item.clear()
                item.update(copy.deepcopy(original))
                mutate(item)
                self.assert_blocked(self.result(), "gate:" + release.COVERAGE_GATE)

    def test_coverage_envelope_requires_exact_report_inventory_and_counts(self):
        gate = self.coverage_gate()
        original = copy.deepcopy(gate)
        for field, value in (
                ("schema", "OTHER"), ("scope", "ALL_COPIED_CODE"), ("tool", "  "),
                ("format", "SUMMARY_ONLY"), ("unmeasured_count", False),
                ("measured_count", True), ("measured_count", 2),
                ("atoms", None), ("atoms", []), ("report_artifact", None),
                ("inventory_artifact", original["coverage"]["report_artifact"]),
                ("inventory_artifact", "unlisted.tsv"), ("inventory_sha256", "not-a-digest")):
            with self.subTest(field=field, value=value):
                gate.clear()
                gate.update(copy.deepcopy(original))
                gate["coverage"][field] = value
                self.assert_coverage_refused()

    def test_inventory_requires_columns_unique_rows_and_exact_atom_bindings(self):
        gate = self.coverage_gate()
        original = (self.evidence_dir / gate["coverage"]["inventory_artifact"]).read_text()
        header, row = original.splitlines()
        for content in ("id\nfixture\n", original + row + "\n", header + "\n",
                        original.replace("()V", "(I)V")):
            with self.subTest(content=content):
                self.write_coverage_artifact("inventory_artifact", content)
                self.assert_coverage_refused()

    def test_raw_report_requires_unambiguous_complete_method_counters(self):
        gate = self.coverage_gate()
        original = (self.evidence_dir / gate["coverage"]["report_artifact"]).read_text()
        method = original[original.index("<method"):original.index("</method>") + len("</method>")]
        line = '<counter type="LINE" covered="99" missed="1"/>'
        branch = '<counter type="BRANCH" covered="99" missed="1"/>'
        reports = [
            "<coverage/>", "<report", original.replace(method, ""),
            original.replace("</class>", method + "</class>"),
            original.replace('class name="fixture/Example"', 'class name=""'),
            original.replace('method name="run"', 'method'),
            original.replace(branch, ""), original.replace(line, line + line),
            original.replace('covered="99"', 'covered="-1"', 1),
            original.replace('covered="99"', 'covered="unmeasured"', 1),
        ]
        for report in reports:
            with self.subTest(report=report):
                self.write_coverage_artifact("report_artifact", report)
                self.assert_coverage_refused()
        self.write_coverage_artifact("report_artifact", original.replace(
            "</method>", '<counter type="INSTRUCTION" covered="99" missed="1"/></method>'))
        self.assertTrue(self.result()["release_ready"])

    def test_atom_identity_and_source_seals_cannot_be_replaced_by_summary_labels(self):
        gate = self.coverage_gate()
        original = copy.deepcopy(gate)
        inventory = (self.evidence_dir / gate["coverage"]["inventory_artifact"]).read_text()
        for field, value in (("id", " "), ("source_path", "README.md"),
                             ("source_path", "src/example.txt"),
                             ("source_path", "src/../example.java"),
                             ("source_path", "src/missing.java")):
            with self.subTest(field=field, value=value):
                gate.clear()
                gate.update(copy.deepcopy(original))
                self.write_coverage_artifact("inventory_artifact", inventory)
                self.change_atom_binding(field, value)
                self.assert_coverage_refused()
        for digest in (None, "not-a-digest", "F" * 64):
            with self.subTest(digest=digest):
                gate.clear()
                gate.update(copy.deepcopy(original))
                self.write_coverage_artifact("inventory_artifact", inventory)
                gate["coverage"]["atoms"][0]["source_sha256"] = digest
                self.assert_coverage_refused("invalid atom source SHA-256")

    def test_measured_counts_require_nonnegative_integers_and_observed_lines(self):
        gate = self.coverage_gate()
        original = copy.deepcopy(gate)
        for field in ("line_covered", "line_missed", "branch_covered", "branch_missed"):
            for value in (False, "99", 99.0, -1, None):
                with self.subTest(field=field, value=value):
                    gate.clear()
                    gate.update(copy.deepcopy(original))
                    gate["coverage"]["atoms"][0][field] = value
                    self.assert_coverage_refused("invalid " + field.split("_")[0] + " coverage counts")
        gate.clear()
        gate.update(original)
        gate["coverage"]["atoms"][0].update(line_covered=0, line_missed=0)
        self.assert_coverage_refused("unmeasured line coverage")

    def test_exact_ninety_percent_threshold_uses_matching_raw_counts(self):
        gate = self.coverage_gate()
        original = copy.deepcopy(gate)
        report = (self.evidence_dir / gate["coverage"]["report_artifact"]).read_text()
        for family in ("line", "branch"):
            for covered in (90, 89):
                with self.subTest(family=family, covered=covered):
                    gate.clear()
                    gate.update(copy.deepcopy(original))
                    atom = gate["coverage"]["atoms"][0]
                    atom[family + "_covered"], atom[family + "_missed"] = covered, 100 - covered
                    before = f'<counter type="{family.upper()}" covered="99" missed="1"/>'
                    after = f'<counter type="{family.upper()}" covered="{covered}" missed="{100 - covered}"/>'
                    self.write_coverage_artifact("report_artifact", report.replace(before, after))
                    if covered == 90:
                        self.assertTrue(self.result()["release_ready"])
                    else:
                        self.assert_coverage_refused(family + " coverage below 90%")

    def test_distinct_methods_share_a_source_seal_but_not_an_atom_id(self):
        gate = self.coverage_gate()
        first = gate["coverage"]["atoms"][0]
        second = dict(first, id="fixture.Example#other", jacoco_method="other")
        gate["coverage"]["atoms"].append(second)
        gate["coverage"]["measured_count"] = 2
        report = (self.evidence_dir / gate["coverage"]["report_artifact"]).read_text()
        method = report[report.index("<method"):report.index("</method>") + len("</method>")]
        self.write_coverage_artifact("report_artifact", report.replace(
            "</class>", method.replace('name="run"', 'name="other"') + "</class>"))
        inventory = (self.evidence_dir / gate["coverage"]["inventory_artifact"]).read_text()
        second_row = "fixture.Example#other\tfixture/Example\tother\t()V\tsrc/example.java\t\n"
        self.write_coverage_artifact("inventory_artifact", inventory + second_row)
        self.assertTrue(self.result()["release_ready"])
        second["id"] = first["id"]
        self.write_coverage_artifact("inventory_artifact", inventory + second_row.replace("#other", "#run"))
        self.assert_coverage_refused("duplicate measured atom ID")

    def test_missing_one_mandatory_gate_is_not_a_pass(self):
        removed = self.evidence["gates"].pop()
        self.assert_blocked(self.result(), "gate:" + removed["id"])

    def test_stale_commit_and_tree_are_refused(self):
        for key in ("source_commit", "source_tree"):
            old = self.evidence[key]
            self.evidence[key] = "a" * 40
            with self.subTest(field=key):
                self.assert_blocked(self.result(), "evidence.manifest")
            self.evidence[key] = old

    def test_dirty_tracked_source_is_refused(self):
        self.write("src/example.java", "class Changed {}\n")
        self.assert_blocked(self.result(), "git.clean_source")

    def test_untracked_source_is_refused(self):
        # Git must return NUL-delimited filenames: newline quoting cannot hide a source.
        self.write("src/extra\nquoted.java", "class Extra {}\n")
        self.assert_blocked(self.result(), "git.clean_source")

    def test_untracked_generated_reports_do_not_dirty_source(self):
        self.write("target/m3-release-proof/generated.json", "{}\n")
        self.write("m3/release/queue/current/admission.json", "{}\n")
        self.assertTrue(self.result()["release_ready"])

    def test_default_manifest_in_visible_queue_is_consumed(self):
        self.save_evidence()
        destination = self.root / release.DEFAULT_EVIDENCE
        shutil.copytree(self.evidence_dir, destination.parent)
        result = release.validate(self.root, True)
        self.assertTrue(result["release_ready"], result)
        self.assertEqual(result["evidence_manifest"], str(destination))

    def test_failed_or_noninteger_exit_is_refused(self):
        entry = self.evidence["gates"][0]
        for status, code in (("FAIL", 1), ("PENDING", 0), ("PASS", 2), ("PASS", False), ("PASS", "0")):
            entry["status"], entry["exit_code"] = status, code
            with self.subTest(status=status, exit=code):
                self.assert_blocked(self.result(), "gate:" + entry["id"])

    def test_command_and_environment_are_required(self):
        entry = self.evidence["gates"][0]
        for field, value in (("command", []), ("environment", {})):
            old = entry[field]
            entry[field] = value
            with self.subTest(field=field):
                self.assert_blocked(self.result(), "gate:" + entry["id"])
            entry[field] = old

    def test_missing_artifact_and_digest_mismatch_are_refused(self):
        entry = self.evidence["gates"][0]
        artifact = entry["artifacts"][0]
        file = self.evidence_dir / artifact["path"]
        file.write_text("changed bytes")
        self.assert_blocked(self.result(), "gate:" + entry["id"])
        file.unlink()
        self.assert_blocked(self.result(), "gate:" + entry["id"])

    def test_artifact_records_require_digests_and_cannot_refer_to_the_manifest(self):
        entry = self.evidence["gates"][0]
        original = copy.deepcopy(entry["artifacts"])
        for artifacts in ([None], [dict(original[0], sha256=None)],
                          [dict(original[0], sha256="A" * 64)],
                          [dict(original[0], path=self.manifest.name)], original * 2):
            with self.subTest(artifacts=artifacts):
                entry["artifacts"] = artifacts
                self.assert_blocked(self.result(), "gate:" + entry["id"])

    def test_raw_coverage_bytes_are_verified_before_report_parsing(self):
        gate = self.coverage_gate()
        report = self.evidence_dir / gate["coverage"]["report_artifact"]
        report.write_text("<report")  # Deliberately retain the previously sealed digest.
        self.assert_coverage_refused("execution artifact SHA-256 mismatch")

    def test_source_change_during_artifact_validation_invalidates_snapshot(self):
        observed_artifact = self.evidence_dir / self.evidence["gates"][0]["artifacts"][0]["path"]
        original_hash = release.sha256

        def publish_source_while_hashing(path):
            digest = original_hash(path)
            if path == observed_artifact:
                self.write("src/arrived-during-validation.java", "class Arrived {}\n")
            return digest

        with mock.patch.object(release, "sha256", side_effect=publish_source_while_hashing):
            result = self.result()
        self.assert_blocked(result, "git.stable_snapshot")
        initial = next(check for check in result["checks"] if check["name"] == "git.clean_source")
        self.assertEqual(initial["status"], "PASS")
        self.assertEqual({"git.stable_snapshot"},
                         {row["name"] for row in result["blockers"]})
        passed_gates = {row["name"].removeprefix("gate:")
                        for row in result["checks"]
                        if row["name"].startswith("gate:") and row["status"] == "PASS"}
        self.assertEqual(set(self.contract["mandatory_release_gates"]), passed_gates)

    def test_artifact_traversal_absolute_and_alias_paths_are_refused(self):
        entry = self.evidence["gates"][0]
        for path in ("../outside.log", "/tmp/outside.log", "./log", "logs//x", "logs/../x", "a\\b", "a\nx"):
            entry["artifacts"][0]["path"] = path
            with self.subTest(path=path):
                self.assert_blocked(self.result(), "gate:" + entry["id"])

    def test_windows_drive_paths_are_refused_on_every_host(self):
        entry = self.evidence["gates"][0]
        for relative in ("C:/logs/proof.log", "C:proof.log"):
            if os.name != "nt":
                file = self.evidence_dir / relative
                file.parent.mkdir(parents=True, exist_ok=True)
                file.write_text("a real file must not turn a drive path into portable evidence")
                entry["artifacts"][0]["sha256"] = release.sha256(file)
            entry["artifacts"][0]["path"] = relative
            with self.subTest(path=relative):
                self.assert_blocked(self.result(), "gate:" + entry["id"])

    def test_artifact_and_manifest_symlinks_are_refused(self):
        entry = self.evidence["gates"][0]
        artifact = self.evidence_dir / entry["artifacts"][0]["path"]
        alias = self.evidence_dir / "alias.log"
        alias.symlink_to(artifact.name)
        old = entry["artifacts"][0]["path"]
        entry["artifacts"][0]["path"] = alias.name
        self.assert_blocked(self.result(), "gate:" + entry["id"])
        entry["artifacts"][0]["path"] = old
        self.save_evidence()
        alias_manifest = self.evidence_dir / "alias.json"
        alias_manifest.symlink_to(self.manifest.name)
        self.assert_blocked(release.validate(self.root, True, alias_manifest), "evidence.manifest")

    def test_duplicate_and_unknown_gate_records_are_refused(self):
        self.evidence["gates"].append(dict(self.evidence["gates"][0]))
        self.assert_blocked(self.result(), "evidence.manifest")
        self.evidence["gates"][-1]["id"] = "NOT_A_REQUIRED_GATE"
        self.assert_blocked(self.result(), "evidence.manifest")

    def test_malformed_duplicate_nonfinite_and_wrong_json_shapes_are_refused(self):
        for content in ("{", '{"schema":1,"schema":2}', '{"schema":NaN}', "[]", "null"):
            self.manifest.write_text(content)
            with self.subTest(content=content):
                self.assert_blocked(release.validate(self.root, True, self.manifest), "evidence.manifest")

    def test_missing_required_boundary_file_is_reported_without_crash(self):
        (self.root / "m3/README.md").unlink()
        self.assert_blocked(self.result(), "file:m3/README.md")

    def test_malformed_contract_is_reported_without_crash(self):
        self.write("m3/release/release-contract.json", "[]")
        self.assert_blocked(self.result(), "contract.read")

    def test_gate_removal_from_contract_cannot_manufacture_readiness(self):
        self.contract["mandatory_release_gates"] = []
        self.write("m3/release/release-contract.json", json.dumps(self.contract))
        self.assert_blocked(self.result(), "contract.mandatory_gates")

    def test_maven_gate_owner_and_recipe_identity_are_required(self):
        # Only the exact canonical recipe authority can supply this receipt.
        other = "example/another-repository"
        entry = next(g for g in self.evidence["gates"] if g["id"] == release.MAVEN_GATE)
        entry["owner_repository"] = other
        self.assert_blocked(self.result(), "gate:" + release.MAVEN_GATE)
        entry["owner_repository"] = release.CANONICAL_RECIPE_REPOSITORY
        entry["canonical_recipe"]["repository"] = other
        self.assert_blocked(self.result(), "gate:" + release.MAVEN_GATE)
        del entry["canonical_recipe"]
        self.assert_blocked(self.result(), "gate:" + release.MAVEN_GATE)

    def test_product_cannot_self_attest_the_canonical_recipe_execution(self):
        entry = next(g for g in self.evidence["gates"] if g["id"] == release.MAVEN_GATE)
        entry["owner_repository"] = release.PRODUCT_REPOSITORY
        entry["canonical_recipe"]["repository"] = release.PRODUCT_REPOSITORY
        self.assert_blocked(self.result(), "gate:" + release.MAVEN_GATE)

    def test_canonical_recipe_receipt_cannot_attest_a_product_gate(self):
        entry = next(g for g in self.evidence["gates"] if g["id"] == "STRING_UTF16_CONTRACT")
        entry["owner_repository"] = release.CANONICAL_RECIPE_REPOSITORY
        self.assert_blocked(self.result(), "gate:" + entry["id"])

    def test_contract_naming_another_maven_gate_owner_is_refused(self):
        self.contract["execution_evidence"]["gate_owners"][release.MAVEN_GATE] = "example/another-repository"
        self.write("m3/release/release-contract.json", json.dumps(self.contract))
        self.assert_blocked(self.result(), "contract.recipe_owner")

    def test_contract_delegating_any_other_gate_to_another_repository_is_refused(self):
        # review of #2236: the owner rule covers every gate, not only the Maven gate; the delegating contract is
        # amended into the single release commit and the evidence re-pinned, so the delegation is the only change
        other = "example/another-repository"
        ident = next(g for g in self.contract["mandatory_release_gates"] if g != release.MAVEN_GATE)
        self.contract["execution_evidence"]["gate_owners"][ident] = other
        self.write("m3/release/release-contract.json", json.dumps(self.contract))
        self.git("add", ".")
        self.git("commit", "--amend", "-qm", "synthetic release tree delegating " + ident)
        self.evidence["source_commit"] = self.git("rev-parse", "HEAD")
        self.evidence["source_tree"] = self.git("rev-parse", "HEAD^{tree}")
        entry = next(g for g in self.evidence["gates"] if g["id"] == ident)
        for owner, expected in ((other, {"contract.recipe_owner", "gate:" + ident}),
                                (release.PRODUCT_REPOSITORY, {"contract.recipe_owner"})):
            entry["owner_repository"] = owner
            with self.subTest(evidence_owner=owner):
                result = self.result()
                self.assertFalse(result["policy_valid"], result)
                self.assertFalse(result["release_ready"], result)
                self.assertEqual(expected, {b["name"] for b in result["blockers"]}, result)

    def test_retained_legacy_recipe_sources_remain_visible_holds(self):
        self.write("m3/recipes/apply.py", "# synthetic legacy recipe fixture\n")
        self.git("add", ".")
        self.git("commit", "--amend", "-qm", "synthetic legacy tree")
        self.evidence["source_commit"] = self.git("rev-parse", "HEAD")
        self.evidence["source_tree"] = self.git("rev-parse", "HEAD^{tree}")
        result = self.result()
        self.assertTrue(result["policy_valid"], result)
        self.assert_blocked(result, "legacy_recipe_custody")
        row = next(q for q in result["queue"] if q["id"] == "legacy_recipe_custody")
        self.assertEqual(row["state"], "HOLD")
        self.assertEqual(row["paths"], ["m3/recipes/apply.py"])

    def test_name_queue_map_drift_and_self_promotion_are_refused(self):
        plan = json.loads((self.root / release.NAME_PLAN).read_text())
        plan["authority_blob"] = "a" * 40
        self.write(release.NAME_PLAN, json.dumps(plan))
        self.assert_blocked(self.result(), "name_queue")
        self.repin_queue()
        plan = json.loads((self.root / release.NAME_PLAN).read_text())
        plan["operations"][0]["state"] = "QUALIFIED"
        self.write(release.NAME_PLAN, json.dumps(plan))
        self.assert_blocked(self.result(), "name_queue")

    def test_rights_boundaries_cannot_be_replaced_by_passing_receipts(self):
        cases = (("M3-SYNEXIA-NOTICE.md", "rights.openjdk_boundary"),
                 ("M3-SYNEXIA-RIGHTS.md", "rights.reservation"))
        for path, check in cases:
            original = (self.root / path).read_bytes()
            with self.subTest(path=path):
                self.write(path, "Synthetic altered rights statement\n")
                result = self.result()
                self.assert_blocked(result, check)
                self.assertFalse(result["policy_valid"])
            (self.root / path).write_bytes(original)

    def test_receiving_order_and_active_phase_cannot_be_self_promoted(self):
        original = json.loads((self.root / release.NAME_MAP).read_text())
        for sequence in (None, [], {"order": list(reversed(release.EXPECTED_RECEIVING_ORDER)),
                                    "active_phase": "STRING"},
                         {"order": release.EXPECTED_RECEIVING_ORDER, "active_phase": "ARRAYS"}):
            with self.subTest(sequence=sequence):
                mapping = copy.deepcopy(original)
                mapping["porting_invariant"]["receiving_sequence"] = sequence
                self.write(release.NAME_MAP, json.dumps(mapping))
                self.repin_queue()
                result = self.result()
                self.assert_blocked(result, "receiving_sequence")
                self.assertFalse(result["policy_valid"])

    def test_malformed_mapping_records_are_reported_as_naming_blockers(self):
        original = json.loads((self.root / release.NAME_MAP).read_text())
        for records in (None, {}, "fixture.owner", [None], ["fixture.owner"]):
            with self.subTest(records=records):
                mapping = copy.deepcopy(original)
                mapping["migration"]["records"] = records
                self.write(release.NAME_MAP, json.dumps(mapping))
                self.repin_queue()
                result = self.result()
                self.assert_blocked(result, "name_queue")
                self.assertIn("canonical mapping records missing",
                              next(b["detail"] for b in result["blockers"] if b["name"] == "name_queue"))

    def test_mapping_ids_must_be_nonempty_strings_and_unique(self):
        original = json.loads((self.root / release.NAME_MAP).read_text())
        for ids in ([None], [""], [[]], [{}], ["fixture.owner", "fixture.owner"]):
            with self.subTest(ids=ids):
                mapping = copy.deepcopy(original)
                mapping["migration"]["records"] = [{"id": value, "targets": []} for value in ids]
                self.write(release.NAME_MAP, json.dumps(mapping))
                self.repin_queue()
                result = self.result()
                self.assert_blocked(result, "name_queue")
                self.assertIn("invalid or duplicate canonical mapping IDs",
                              next(b["detail"] for b in result["blockers"] if b["name"] == "name_queue"))

    def test_naming_operations_must_be_an_explicit_list(self):
        original = json.loads((self.root / release.NAME_PLAN).read_text())
        for operations in (None, {}, "QUEUED", True):
            with self.subTest(operations=operations):
                self.write(release.NAME_PLAN, json.dumps(dict(original, operations=operations)))
                result = self.result()
                self.assert_blocked(result, "name_queue")
                self.assertIn("naming operations missing",
                              next(b["detail"] for b in result["blockers"] if b["name"] == "name_queue"))

    def test_release_identity_container_cannot_replace_the_contract(self):
        for value in (None, [], "21.0.14", True):
            with self.subTest(release=value):
                candidate = copy.deepcopy(self.contract)
                candidate["release"] = value
                self.write("m3/release/release-contract.json", json.dumps(candidate))
                result = self.result()
                self.assert_blocked(result, "contract.release_identity")
                self.assert_blocked(result, "git.upstream_baseline")

    def test_divergent_upstream_cannot_be_admitted_as_the_release_parent(self):
        source = self.git("rev-parse", "HEAD")
        self.git("checkout", "-qb", "fixture-divergent-upstream",
                 self.contract["release"]["upstream_baseline"])
        self.write("independent.txt", "synthetic divergent upstream\n")
        self.commit("synthetic independent upstream")
        divergent = self.git("rev-parse", "HEAD")
        self.git("checkout", "-q", source)
        self.contract["release"]["upstream_baseline"] = divergent
        self.write("m3/release/release-contract.json", json.dumps(self.contract))
        self.commit("synthetic mismatched upstream declaration")
        before = self.git("rev-list", "HEAD")
        self.evidence["source_commit"] = self.git("rev-parse", "HEAD")
        self.evidence["source_tree"] = self.git("rev-parse", "HEAD^{tree}")
        result = self.result()
        self.assert_blocked(result, "git.merge_base")
        self.assertEqual(before, self.git("rev-list", "HEAD"))

    def test_unavailable_git_metadata_blocks_initial_and_final_inspection(self):
        (self.root / ".git").rename(Path(self.tmp.name) / "saved-git-metadata")
        result = self.result()
        self.assert_blocked(result, "git.inspect")
        self.assert_blocked(result, "git.stable_snapshot")
        self.assertIsNone(result["source_commit"])
        self.assertIsNone(result["source_tree"])

    def test_standalone_baseline_does_not_waive_the_release_naming_gate(self):
        self.contract["standalone"]["required_for_release"] = True
        self.write("m3/release/release-contract.json", json.dumps(self.contract))
        spec = importlib.util.spec_from_file_location(
            "standalone_clean_fixture", Path(__file__).with_name("check_standalone.py"))
        standalone = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(standalone)
        self.git("add", ".")
        self.git("commit", "--amend", "-qm", "synthetic standalone boundary")
        scanned = standalone.scan(self.root)
        self.assertEqual(sum(scanned["counts"][kind] for kind in standalone.DEPENDENCY_KINDS), 0)
        self.assertEqual(scanned["unscanned"], 0)
        self.assertGreater(sum(scanned["counts"][kind] for kind in standalone.NAMING_KINDS), 0)
        standalone.write_baseline(self.root, scanned)
        self.git("add", ".")
        self.git("commit", "--amend", "-qm", "synthetic clean standalone baseline")
        self.evidence["source_commit"] = self.git("rev-parse", "HEAD")
        self.evidence["source_tree"] = self.git("rev-parse", "HEAD^{tree}")
        # Preserve the canonical recipe declaration in the fixture. A matching
        # development baseline does not authorize its release naming boundary.
        result = self.result(False)
        self.assertTrue(result["policy_valid"], result)
        check = next(c for c in result["checks"] if c["name"] == "standalone.no_external_repo_dependency")
        self.assertEqual(check["status"], "PASS")
        result = self.result(True)
        self.assert_blocked(result, "standalone.no_external_repo_dependency")
        check = next(c for c in result["checks"] if c["name"] == "standalone.no_external_repo_dependency")
        self.assertEqual(check["status"], "HOLD")
        self.assertIn("Synexia names remain", check["detail"])

    def test_invalid_standalone_baseline_is_a_controlled_blocker(self):
        self.write("m3/release/STANDALONE_BASELINE.tsv", "kind\tcount\nD_CHECKOUT\tnot-an-integer\n")
        result = self.result()
        self.assert_blocked(result, "standalone.no_external_repo_dependency")
        self.assertFalse(result["policy_valid"])

    def test_relative_evidence_path_is_bound_to_the_candidate_checkout(self):
        self.save_evidence()
        destination = self.root / "target" / "evidence"
        shutil.copytree(self.evidence_dir, destination)
        result = release.validate(self.root, True, Path("target/evidence/evidence.json"))
        self.assertTrue(result["release_ready"], result)
        self.assertEqual(Path(result["evidence_manifest"]), destination / "evidence.json")

    def test_evidence_gate_container_requires_an_array(self):
        for gates in (None, {}, "PASS", True):
            with self.subTest(gates=gates):
                self.evidence["gates"] = gates
                result = self.result()
                self.assert_blocked(result, "evidence.manifest")
                self.assertIn("evidence gates must be a list",
                              next(b["detail"] for b in result["blockers"] if b["name"] == "evidence.manifest"))

    def test_malformed_gate_entry_cannot_partially_admit_a_valid_prefix(self):
        valid = copy.deepcopy(self.evidence["gates"][0])
        for entry in (None, [], "PASS", False):
            with self.subTest(entry=entry):
                self.evidence["gates"] = [valid, entry]
                result = self.result()
                self.assert_blocked(result, "evidence.manifest")
                checks = [c for c in result["checks"] if c["name"].startswith("gate:")]
                self.assertEqual(len(checks), len(self.contract["mandatory_release_gates"]))
                self.assertEqual({c["status"] for c in checks}, {"PENDING"})

    def test_empty_naming_queue_renders_the_verified_synthetic_receipt_view(self):
        plan = json.loads((self.root / release.NAME_PLAN).read_text())
        plan["operations"] = []
        self.write(release.NAME_PLAN, json.dumps(plan))
        self.git("add", ".")
        self.git("commit", "--amend", "-qm", "synthetic tree with no naming candidates")
        self.evidence["source_commit"] = self.git("rev-parse", "HEAD")
        self.evidence["source_tree"] = self.git("rev-parse", "HEAD^{tree}")
        result = self.result()
        self.assertTrue(result["release_ready"], result)
        self.assertEqual(result["queue"], [])
        rendered = release.render_queue(result)
        self.assertIn("No pending items | VERIFIED_RECEIPTS", rendered)
        self.assertIn("Review the exact proof pack before publication", rendered)

    def test_script_entry_point_preserves_the_receipt_validation_exit_contract(self):
        self.save_evidence()
        output = io.StringIO()
        argv = [str(MODULE_PATH), "--root", str(self.root), "--release",
                "--evidence", str(self.manifest)]
        with mock.patch.object(sys, "argv", argv), contextlib.redirect_stdout(output):
            with self.assertRaises(SystemExit) as exited:
                runpy.run_path(str(MODULE_PATH), run_name="__main__")
        self.assertEqual(exited.exception.code, 0)
        result = json.loads(output.getvalue())
        self.assertTrue(result["release_ready"], result)
        self.assertEqual(result["source_commit"], self.evidence["source_commit"])

    def test_existing_single_fork_commit_requirement_is_preserved(self):
        self.write("next.txt", "another commit\n")
        self.commit("synthetic successor")
        history = self.git("rev-list", "HEAD")
        self.evidence["source_commit"] = self.git("rev-parse", "HEAD")
        self.evidence["source_tree"] = self.git("rev-parse", "HEAD^{tree}")
        for mode in (False, True):
            with self.subTest(release=mode):
                self.assert_blocked(self.result(mode), "git.single_release_commit")
        self.assertEqual(self.git("rev-list", "HEAD"), history,
                         "a failed history gate must preserve append-only ancestry")

    def test_standalone_rule_refuses_release_while_the_contract_needs_another_repository(self):
        # A real checkout is an executable dependency; a passive recipe receipt is not.
        self.contract["standalone"]["required_for_release"] = True
        self.write("m3/release/release-contract.json", json.dumps(self.contract))
        self.write(".github/workflows/external-runtime.yml",
                   "jobs:\n  build:\n    steps:\n      - uses: actions/checkout@v4\n"
                   "        with:\n          repository: hsoliwal/com.synexia\n")
        self.repin_queue()
        self.git("add", ".")
        self.git("commit", "--amend", "-qm", "synthetic tree requiring external runtime")
        spec = importlib.util.spec_from_file_location(
            "standalone_release_fixture", Path(__file__).with_name("check_standalone.py"))
        standalone = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(standalone)
        standalone.write_baseline(self.root, standalone.scan(self.root))
        self.git("add", ".")
        self.git("commit", "--amend", "-qm", "synthetic external-runtime dependency baseline")
        self.evidence["source_commit"] = self.git("rev-parse", "HEAD")
        self.evidence["source_tree"] = self.git("rev-parse", "HEAD^{tree}")
        result = self.result(True)
        self.assert_blocked(result, "standalone.no_external_repo_dependency")
        detail = next(b["detail"] for b in result["blockers"] if b["name"] == "standalone.no_external_repo_dependency")
        self.assertIn("D_CHECKOUT=1", detail)
        self.assertIn("dependencies on another repository remain", detail)

    def test_cli_distinguishes_policy_pass_from_release_acceptance_and_writes_queue(self):
        report = Path(self.tmp.name) / "reports" / "admission.json"
        queue = report.with_name("QUEUE.md")
        args = ["--root", str(self.root), "--evidence", str(self.manifest),
                "--json-out", str(report), "--queue-out", str(queue)]
        with contextlib.redirect_stdout(io.StringIO()):
            self.assertEqual(release.main(args), 0)
            self.assertEqual(release.main(args + ["--release"]), 1)
        self.assertFalse(json.loads(report.read_text())["release_ready"])
        text = queue.read_text()
        self.assertIn("M3JDK21 release queue", text)
        self.assertIn("PENDING", text)
        self.assertIn("QUEUED", text)
        self.assertIn(self.git("rev-parse", "HEAD"), text)

    def test_cli_hash_manifest_seals_each_available_boundary_file(self):
        self.save_evidence()
        hashes = Path(self.tmp.name) / "reports" / "boundary.sha256"
        output = io.StringIO()
        with contextlib.redirect_stdout(output):
            code = release.main(["--root", str(self.root), "--release",
                                 "--evidence", str(self.manifest), "--sha256-out", str(hashes)])
        self.assertEqual(code, 0)
        self.assertTrue(json.loads(output.getvalue())["release_ready"])
        entries = dict(line.split("  ", 1)[::-1] for line in hashes.read_text().splitlines())
        self.assertEqual(set(entries), set(release.REQUIRED_FILES))
        for path, digest in entries.items():
            self.assertEqual(digest, hashlib.sha256((self.root / path).read_bytes()).hexdigest())


    def test_omitted_branch_counter_requires_an_explicit_reason(self):
        item = next(g for g in self.evidence["gates"]
                    if g["id"] == release.COVERAGE_GATE)
        atom = item["coverage"]["atoms"][0]
        artifact_path = self.evidence_dir / item["coverage"]["report_artifact"]
        artifact_path.write_text(artifact_path.read_text().replace(
            '<counter type="BRANCH" covered="99" missed="1"/>', ''))
        report_row = next(a for a in item["artifacts"] if a["path"] == artifact_path.name)
        report_row["sha256"] = release.sha256(artifact_path)
        atom["branch_covered"] = 0
        atom["branch_missed"] = 0
        for explanation in (None, "   "):
            if explanation is None:
                atom.pop("branchless_explanation", None)
            else:
                atom["branchless_explanation"] = explanation
            with self.subTest(explanation=explanation):
                result = self.result()
                self.assert_blocked(result, "gate:" + release.COVERAGE_GATE)
                detail = next(b["detail"] for b in result["blockers"]
                              if b["name"] == "gate:" + release.COVERAGE_GATE)
                self.assertIn("zero-branch atom needs an explicit explanation", detail)
        atom["branchless_explanation"] = "No control flow in branchless fixture"
        result = self.result()
        self.assertTrue(result["release_ready"], result)
        atom["branch_covered"] = 1
        result = self.result()
        self.assert_blocked(result, "gate:" + release.COVERAGE_GATE)
        detail = next(b["detail"] for b in result["blockers"]
                      if b["name"] == "gate:" + release.COVERAGE_GATE)
        self.assertIn("reported atom counts disagree with raw JaCoCo XML", detail)

    def test_unselected_method_may_omit_its_branch_counter(self):
        item = next(g for g in self.evidence["gates"]
                    if g["id"] == release.COVERAGE_GATE)
        artifact_path = self.evidence_dir / item["coverage"]["report_artifact"]
        artifact_path.write_text(artifact_path.read_text().replace(
            '</class>', '<method name="unused" desc="()V">'
            '<counter type="LINE" covered="3" missed="0"/>'
            '</method></class>'))
        report_row = next(a for a in item["artifacts"] if a["path"] == artifact_path.name)
        report_row["sha256"] = release.sha256(artifact_path)
        result = self.result()
        self.assertTrue(result["release_ready"], result)
        self.assertEqual(1, item["coverage"]["measured_count"])
        self.assertEqual(["fixture.Example#run"],
                         [atom["id"] for atom in item["coverage"]["atoms"]])

    def test_raw_line_proof_and_counter_validity_are_preserved(self):
        item = next(g for g in self.evidence["gates"]
                    if g["id"] == release.COVERAGE_GATE)
        atom = item["coverage"]["atoms"][0]
        artifact_path = self.evidence_dir / item["coverage"]["report_artifact"]
        original = artifact_path.read_text()
        report_row = next(a for a in item["artifacts"] if a["path"] == artifact_path.name)
        line = '<counter type="LINE" covered="99" missed="1"/>'
        cases = (
            (original.replace(line, ''), (99, 1), "missing JaCoCo line method counter"),
            (original.replace(line, '<counter type="LINE" covered="0" missed="0"/>'),
             (0, 0), "unmeasured line coverage"),
            (original.replace('</method>', line + '</method>'),
             (99, 1), "duplicate JaCoCo line/branch counter"),
            (original.replace('</method>', '<counter type="BRANCH" covered="1" missed="0"/></method>'),
             (99, 1), "duplicate JaCoCo line/branch counter"),
            (original.replace('type="LINE" covered="99"', 'type="LINE" covered="-1"'),
             (99, 1), "invalid JaCoCo method counter"),
            (original.replace('type="BRANCH" covered="99"', 'type="BRANCH" covered="-1"'),
             (99, 1), "invalid JaCoCo method counter"),
        )
        for xml, line_counts, reason in cases:
            artifact_path.write_text(xml)
            report_row["sha256"] = release.sha256(artifact_path)
            atom["line_covered"], atom["line_missed"] = line_counts
            with self.subTest(reason=reason):
                result = self.result()
                self.assert_blocked(result, "gate:" + release.COVERAGE_GATE)
                detail = next(b["detail"] for b in result["blockers"]
                              if b["name"] == "gate:" + release.COVERAGE_GATE)
                self.assertIn(reason, detail)


if __name__ == "__main__":
    unittest.main()
