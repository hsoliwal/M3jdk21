# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Real-Git regression fixtures for fail-closed product release admission.

The positive evidence below is deliberately synthetic. It proves receipt
validation only; it is never a release, JDK/JNI, benchmark or recipe receipt.
"""
from __future__ import annotations

import contextlib
import hashlib
import importlib.util
import io
import json
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

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
            artifact = self.evidence_dir / (ident + ".log")
            artifact.write_text("SYNTHETIC VALIDATOR FIXTURE ONLY " + ident + "\n")
            gate = {
                "id": ident, "status": "PASS", "exit_code": 0,
                "owner_repository": "hsoliwal/com.synexia" if ident == "M3_MAVEN_CONTROL_REACTOR" else "hsoliwal/M3jdk21",
                "command": ["synthetic-fixture", ident],
                "environment": {"os": "fixture-os", "architecture": "fixture-arch", "toolchain": "fixture-toolchain"},
                "artifacts": [{"path": artifact.name, "sha256": release.sha256(artifact)}],
            }
            if ident == "M3_MAVEN_CONTROL_REACTOR":
                gate["canonical_recipe"] = {
                    "repository": "hsoliwal/com.synexia", "commit": "1" * 40,
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

    def test_artifact_traversal_absolute_and_alias_paths_are_refused(self):
        entry = self.evidence["gates"][0]
        for path in ("../outside.log", "/tmp/outside.log", "./log", "logs//x", "logs/../x", "a\\b", "a\nx"):
            entry["artifacts"][0]["path"] = path
            with self.subTest(path=path):
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

    def test_synexia_recipe_owner_and_identity_are_required(self):
        entry = next(g for g in self.evidence["gates"] if g["id"] == "M3_MAVEN_CONTROL_REACTOR")
        entry["owner_repository"] = "hsoliwal/M3jdk21"
        self.assert_blocked(self.result(), "gate:M3_MAVEN_CONTROL_REACTOR")
        entry["owner_repository"] = "hsoliwal/com.synexia"
        del entry["canonical_recipe"]
        self.assert_blocked(self.result(), "gate:M3_MAVEN_CONTROL_REACTOR")

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

    def test_existing_single_fork_commit_requirement_is_preserved(self):
        self.write("next.txt", "another commit\n")
        self.commit("synthetic successor")
        self.evidence["source_commit"] = self.git("rev-parse", "HEAD")
        self.evidence["source_tree"] = self.git("rev-parse", "HEAD^{tree}")
        for mode in (False, True):
            with self.subTest(release=mode):
                self.assert_blocked(self.result(mode), "git.single_release_commit")

    def test_standalone_rule_refuses_release_while_the_contract_needs_another_repository(self):
        # the contract still names another repository as the M3_MAVEN_CONTROL_REACTOR owner: not standalone
        self.contract["standalone"]["required_for_release"] = True
        self.write("m3/release/release-contract.json", json.dumps(self.contract))
        self.repin_queue()
        self.commit("enforce the standalone rule")
        self.evidence["source_commit"] = self.git("rev-parse", "HEAD")
        self.evidence["source_tree"] = self.git("rev-parse", "HEAD^{tree}")
        result = self.result(True)
        self.assert_blocked(result, "standalone.no_external_repo_dependency")
        detail = next(b["detail"] for b in result["blockers"] if b["name"] == "standalone.no_external_repo_dependency")
        self.assertIn("D_REPO_REF=", detail)

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


if __name__ == "__main__":
    unittest.main()
