# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Target release-admission regressions using real temporary Git histories.

Synthetic receipts exercise this verifier, never qualify a product release.
"""
from __future__ import annotations

import copy
import importlib.util
import json
import os
import subprocess
import tempfile
import unittest
from unittest import mock
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
        contract = release.load_json(Path(__file__).with_name("release-contract.json"))
        self.assertEqual(release.EXPECTED_PRIORITY, contract["priority_order"])

    def test_unqualified_claims_remain_not_claimed(self):
        contract = release.load_json(Path(__file__).with_name("release-contract.json"))
        for key in release.UNQUALIFIED_CLAIMS:
            self.assertEqual("NOT_CLAIMED", contract["current_claims"][key])

    def test_sha256_is_stable(self):
        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / "x"
            path.write_bytes(b"m3\n")
            self.assertEqual("3831561589b9de83145d35aebf9d2b55a9843a09e8b7754bedd56173e25b3aa6", release.sha256(path))


class ExecutedReceiptAdmissionTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)
        self.root = self.base / "source"
        self.proof = self.base / "proof"
        self.root.mkdir()
        self.proof.mkdir()
        self.run_git("init", "-q")
        self.run_git("config", "user.name", "Release verifier fixture")
        self.run_git("config", "user.email", "release-verifier@example.invalid")
        self.put(self.root, "upstream.txt", "fixture upstream\n")
        self.commit("upstream fixture")
        self.upstream = self.run_git("rev-parse", "HEAD")
        self.contract = release.load_json(Path(__file__).with_name("release-contract.json"))
        self.contract["release"]["upstream_baseline"] = self.upstream
        for relative in release.REQUIRED_FILES:
            self.put(self.root, relative, "fixture\n")
        self.put(self.root, "m3/release/release-contract.json", self.contract)
        self.put(self.root, "M3-SYNEXIA-NOTICE.md", "This notice does not relicense\nOpenJDK or JDK-derived source code as Apache-2.0.\n")
        self.put(self.root, "M3-SYNEXIA-RIGHTS.md", "All ownership and all rights not expressly granted by the applicable license\n")
        self.put(self.root, "m3/README.md", "does **not** modify `java.lang.String`\nNo speedup, complete compatibility, or full jtreg pass is claimed.\n")
        self.put(self.root, "m3/docs/name-mapping.json", {"porting_invariant": {"receiving_sequence": {
            "order": release.EXPECTED_RECEIVING_ORDER, "active_phase": "STRING"}}})
        self.commit("candidate fixture")
        self.source = {"repository": release.REPOSITORY,
                       "commit": self.run_git("rev-parse", "HEAD"),
                       "tree": self.run_git("rev-parse", "HEAD^{tree}")}
        self.entries = []
        self.receipts = {}
        for gate in self.contract["mandatory_release_gates"]:
            log = "logs/" + gate + ".txt"
            self.put(self.proof, log, "synthetic verifier fixture: one successful check\n")
            receipt = {"schema": "M3_RELEASE_GATE_RECEIPT_V1", "gate": gate,
                       "source": dict(self.source), "status": "PASS", "exit_code": 0,
                       "command": ["fixture", "--check", gate],
                       "environment": {"os": "fixture-os", "arch": "fixture-arch", "toolchain": "fixture-only"},
                       "checks": {"executed": 1, "failed": 0},
                       "execution": {"kind": "LOCAL", "status": "completed"},
                       "artifacts": [self.descriptor(log)]}
            self.receipts[gate] = receipt
            self.put(self.proof, "receipts/" + gate + ".json", receipt)
            self.entries.append({"id": gate, "receipt": self.descriptor("receipts/" + gate + ".json")})
        self.pack = {"schema": "M3_RELEASE_PROOF_PACK_V1", "source": dict(self.source), "gates": self.entries}
        self.pack_path = self.put(self.proof, "proof-pack.json", self.pack)
        self.gate = "PRECOMPUTE_JNI_NATIVE_PARITY"

    def run_git(self, *args):
        return subprocess.check_output(["git", "-C", str(self.root), *args], text=True, stderr=subprocess.PIPE).strip()

    def commit(self, message):
        self.run_git("add", ".")
        self.run_git("commit", "-qm", message)

    def put(self, directory, relative, value):
        path = directory / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(value, indent=2) + "\n" if isinstance(value, (dict, list)) else value, encoding="utf-8")
        return path

    def descriptor(self, relative):
        return {"path": relative, "sha256": release.sha256(self.proof / relative)}

    def reseal(self, gate=None):
        gate = gate or self.gate
        relative = "receipts/" + gate + ".json"
        self.put(self.proof, relative, self.receipts[gate])
        for entry in self.entries:
            if entry["id"] == gate:
                entry["receipt"] = self.descriptor(relative)
        self.put(self.proof, "proof-pack.json", self.pack)

    def result(self, evidence=True, mode=True):
        return release.validate(self.root, release_mode=mode, evidence_path=self.pack_path if evidence else None)

    def assert_held(self, result, name=None):
        self.assertFalse(result["release_ready"])
        self.assertTrue(result["blockers"])
        if name:
            self.assertIn(name, [b["name"] for b in result["blockers"]])

    def github_receipt(self):
        run = {"id": 17, "head_sha": self.source["commit"], "repository": {"full_name": release.REPOSITORY},
               "status": "completed", "conclusion": "success"}
        jobs = {"total_count": 1, "jobs": [{"id": 19, "run_id": 17, "head_sha": self.source["commit"],
                "status": "completed", "conclusion": "success", "steps": [
                    {"name": "Run target JNI parity", "status": "completed", "conclusion": "success"}]}]}
        self.put(self.proof, "github/run.json", run)
        self.put(self.proof, "github/jobs.json", jobs)
        self.receipts[self.gate]["execution"] = {
            "kind": "GITHUB_ACTIONS", "status": "completed", "run": self.descriptor("github/run.json"),
            "jobs": self.descriptor("github/jobs.json"), "job_ids": [19], "required_steps": ["Run target JNI parity"]}
        self.reseal()
        return run, jobs

    def reseal_github(self, run, jobs):
        self.put(self.proof, "github/run.json", run)
        self.put(self.proof, "github/jobs.json", jobs)
        execution = self.receipts[self.gate]["execution"]
        execution["run"] = self.descriptor("github/run.json")
        execution["jobs"] = self.descriptor("github/jobs.json")
        self.reseal()

    def test_metadata_alone_never_reports_release_ready(self):
        result = self.result(evidence=False)
        self.assertTrue(result["boundary_ready"])
        self.assert_held(result, "proof.pack")
        self.assertEqual(10, len([r for r in result["queue"] if r["id"].startswith("gate:") and r["status"] == "NOT_RUN"]))

    def test_exact_completed_receipts_can_admit_append_only_history(self):
        # There are two commits above upstream; an append-only history is admitted.
        self.put(self.root, "second-change.txt", "independent additive change\n")
        self.commit("second additive fixture")
        self.source.update(commit=self.run_git("rev-parse", "HEAD"), tree=self.run_git("rev-parse", "HEAD^{tree}"))
        self.pack["source"] = dict(self.source)
        for gate, receipt in self.receipts.items():
            receipt["source"] = dict(self.source)
            self.reseal(gate)
        result = self.result()
        self.assertTrue(result["evidence_integrity_ready"], result["blockers"])
        self.assert_held(result, "producer.verification")
        self.assertEqual("2", self.run_git("rev-list", "--count", self.upstream + "..HEAD"))

    def test_development_mode_is_never_release_admission(self):
        result = self.result(mode=False)
        self.assertTrue(result["boundary_ready"])
        self.assertFalse(result["release_ready"])
        self.assertEqual([], result["blockers"])

    def test_missing_gate_blocks(self):
        self.entries.pop()
        self.put(self.proof, "proof-pack.json", self.pack)
        self.assert_held(self.result())

    def test_duplicate_or_unknown_gate_blocks(self):
        for item in [copy.deepcopy(self.entries[0]), {"id": "UNKNOWN", "receipt": self.entries[0]["receipt"]}]:
            with self.subTest(item=item["id"]):
                self.pack["gates"] = self.entries + [item]
                self.put(self.proof, "proof-pack.json", self.pack)
                self.assert_held(self.result(), "proof.pack")

    def test_stale_pack_commit_or_tree_blocks(self):
        for field in ["commit", "tree"]:
            with self.subTest(field=field):
                self.pack["source"] = dict(self.source, **{field: "0" * 40})
                self.put(self.proof, "proof-pack.json", self.pack)
                self.assert_held(self.result(), "proof.pack")

    def test_stale_individual_gate_blocks(self):
        self.receipts[self.gate]["source"]["commit"] = "0" * 40
        self.reseal()
        self.assert_held(self.result(), "gate:" + self.gate)

    def test_dirty_tracked_source_blocks_even_with_old_matching_receipts(self):
        self.put(self.root, "M3-SYNEXIA-AUTHORS.md", "uncommitted change\n")
        self.assert_held(self.result(), "git.source_and_ancestry")

    def test_untracked_source_blocks_but_visible_report_outputs_are_permitted(self):
        source = self.put(self.root, "src/Untracked.java", "class Untracked {}\n")
        self.assert_held(self.result(), "git.source_and_ancestry")
        source.unlink()
        self.put(self.root, "m3/release/queue/runs/prior/admission.json", {"release_ready": False})
        result = self.result(mode=False)
        self.assertTrue(result["evidence_integrity_ready"], result["blockers"])

    def test_artifact_drift_and_empty_result_block(self):
        log = self.receipts[self.gate]["artifacts"][0]["path"]
        for content in ["tampered\n", ""]:
            with self.subTest(content=content):
                self.put(self.proof, log, content)
                self.assert_held(self.result(), "gate:" + self.gate)

    def test_receipt_digest_drift_blocks(self):
        self.put(self.proof, "receipts/" + self.gate + ".json", self.receipts[self.gate] | {"extra": "drift"})
        self.assert_held(self.result(), "gate:" + self.gate)

    def test_no_executed_checks_failed_checks_and_boolean_exit_block(self):
        original = copy.deepcopy(self.receipts[self.gate])
        for changed in [{"checks": {"executed": 0, "failed": 0}},
                        {"checks": {"executed": 1, "failed": 1}}, {"exit_code": False},
                        {"status": "NOT_RUN"}, {"command": []}, {"artifacts": []}]:
            with self.subTest(changed=changed):
                self.receipts[self.gate] = original | changed
                self.reseal()
                self.assert_held(self.result(), "gate:" + self.gate)

    def test_complete_github_run_proves_integrity_but_not_independent_execution(self):
        self.github_receipt()
        result = self.result()
        self.assertTrue(result["evidence_integrity_ready"], result["blockers"])
        self.assert_held(result, "producer.verification")

    def test_zero_jobs_and_incomplete_pagination_block(self):
        run, original = self.github_receipt()
        for jobs in [{"total_count": 0, "jobs": []}, dict(original, total_count=2)]:
            with self.subTest(jobs=jobs):
                self.reseal_github(run, jobs)
                self.assert_held(self.result(), "gate:" + self.gate)

    def test_queued_failed_stale_and_cross_repository_runs_block(self):
        original, jobs = self.github_receipt()
        for changed in [{"status": "queued", "conclusion": None}, {"conclusion": "failure"},
                        {"head_sha": "0" * 40}, {"repository": {"full_name": "someone/else"}}]:
            with self.subTest(changed=changed):
                self.reseal_github(original | changed, jobs)
                self.assert_held(self.result(), "gate:" + self.gate)

    def test_zero_steps_skipped_gate_steps_and_stale_jobs_block(self):
        run, original = self.github_receipt()
        for changed in [{"steps": []}, {"steps": [{"name": "Run target JNI parity", "status": "completed", "conclusion": "skipped"}]},
                        {"head_sha": "0" * 40}, {"run_id": 18}, {"conclusion": "failure"}]:
            with self.subTest(changed=changed):
                jobs = copy.deepcopy(original)
                jobs["jobs"][0].update(changed)
                self.reseal_github(run, jobs)
                self.assert_held(self.result(), "gate:" + self.gate)

    def test_parent_path_and_symlink_artifacts_block(self):
        entry = next(e for e in self.entries if e["id"] == self.gate)
        original = entry["receipt"]
        entry["receipt"] = dict(original, path="../escape.json")
        self.put(self.proof, "proof-pack.json", self.pack)
        self.assert_held(self.result(), "gate:" + self.gate)
        link = self.proof / "linked.json"
        link.symlink_to(self.proof / original["path"])
        entry["receipt"] = dict(original, path="linked.json")
        self.put(self.proof, "proof-pack.json", self.pack)
        self.assert_held(self.result(), "gate:" + self.gate)

    def test_weakened_gate_set_and_squash_policy_block(self):
        self.contract["mandatory_release_gates"] = []
        self.contract["release"]["single_fork_commit_required"] = True
        self.put(self.root, "m3/release/release-contract.json", self.contract)
        self.commit("invalid policy fixture")
        result = self.result(evidence=False)
        self.assert_held(result, "contract.mandatory_release_gates")
        self.assert_held(result, "contract.append_only_history")

    def test_missing_malformed_and_duplicate_key_contract_fail_closed(self):
        path = self.root / "m3/release/release-contract.json"
        for value in [None, "{", '{"schema":1,"schema":2}', "[]"]:
            with self.subTest(value=value):
                if value is None:
                    path.unlink()
                else:
                    path.write_text(value)
                self.assert_held(self.result(evidence=False), "contract.read")

    def test_queue_is_deterministic_visible_and_source_bound(self):
        result = self.result(evidence=False)
        output = release.queue_tsv(result)
        self.assertEqual(output, release.queue_tsv(result))
        self.assertIn("NOT_RUN", output)
        self.assertIn(self.source["commit"], output)
        self.assertIn("evidence\tnext_action", output.splitlines()[0])


    def qualify_fixture_producers(self):
        workflow = ".github/workflows/qualified-fixture.yml"
        self.put(self.root, workflow, "# synthetic test producer, never a product gate\n")
        blob = self.run_git("hash-object", workflow)
        policy = {"status": "QUALIFIED", "workflow": workflow, "workflow_blob": blob,
                  "jobs": [{"name": "Fixture target job", "required_steps": ["Run target JNI parity"]}]}
        self.contract["trusted_gate_producers"] = {gate: copy.deepcopy(policy) for gate in self.receipts}
        self.put(self.root, "m3/release/release-contract.json", self.contract)
        self.commit("synthetic producer policy fixture")
        self.source.update(commit=self.run_git("rev-parse", "HEAD"), tree=self.run_git("rev-parse", "HEAD^{tree}"))
        self.pack["source"] = dict(self.source)
        run, jobs = self.github_receipt()
        run.update(path=workflow, run_attempt=1)
        jobs["jobs"][0]["name"] = "Fixture target job"
        self.reseal_github(run, jobs)
        execution = copy.deepcopy(self.receipts[self.gate]["execution"])
        for gate, receipt in self.receipts.items():
            receipt["source"] = dict(self.source)
            receipt["execution"] = copy.deepcopy(execution)
            self.reseal(gate)
        return run, jobs

    def test_release_requires_fresh_qualified_producer_verification(self):
        run, jobs = self.qualify_fixture_producers()
        # Only this API seam is simulated; local source history/artifacts are real.
        with mock.patch.object(release, "github_api", side_effect=[run, [jobs]]) as api:
            result = release.validate(self.root, True, self.pack_path, verify_github=True)
        self.assertTrue(result["release_ready"], result["blockers"])
        self.assertEqual(2, api.call_count)  # one run, paginated jobs; shared-run cache

    def test_fake_success_json_cannot_override_live_failed_or_unrelated_workflow(self):
        original, jobs = self.qualify_fixture_producers()
        for changed in [{"conclusion": "failure"}, {"path": ".github/workflows/unapproved.yml"},
                        {"run_attempt": 2}]:
            with self.subTest(changed=changed):
                with mock.patch.object(release, "github_api", side_effect=[original | changed, [jobs]]):
                    result = release.validate(self.root, True, self.pack_path, verify_github=True)
                self.assert_held(result)

    def test_unqualified_producer_policy_refuses_without_network(self):
        with mock.patch.object(release, "github_api") as api:
            result = release.validate(self.root, True, self.pack_path, verify_github=True)
        api.assert_not_called()
        self.assert_held(result)

    def test_producer_source_drift_refuses_without_network(self):
        self.qualify_fixture_producers()
        self.put(self.root, ".github/workflows/qualified-fixture.yml", "# altered producer\n")
        self.commit("producer drift")
        with mock.patch.object(release, "github_api") as api:
            result = release.validate(self.root, True, self.pack_path, verify_github=True)
        api.assert_not_called()
        self.assert_held(result, "proof.pack")

    def verified_during_mutation(self, mutation):
        run, jobs = self.qualify_fixture_producers()
        calls = 0

        def api(endpoint, paginated=False):
            nonlocal calls
            calls += 1
            if calls == 1:
                mutation()
                return run
            return [jobs]

        with mock.patch.object(release, "github_api", side_effect=api):
            return release.validate(self.root, True, self.pack_path, verify_github=True)

    def test_concurrent_commit_cannot_reuse_the_initial_source_lease(self):
        def mutate():
            self.put(self.root, "src/AfterSnapshot.java", "class AfterSnapshot {}\n")
            self.commit("concurrent source fixture")
        result = self.verified_during_mutation(mutate)
        self.assert_held(result, "git.final_source_lease")
        self.assertFalse(result["source_lease_verified"])
        self.assertFalse(result["boundary_ready"])
        self.assertNotEqual(result["source"]["commit"], self.run_git("rev-parse", "HEAD"))

    def test_concurrent_dirty_source_is_refused_after_live_producer_verification(self):
        result = self.verified_during_mutation(
            lambda: self.put(self.root, "M3-SYNEXIA-AUTHORS.md", "concurrent dirty source\n"))
        self.assert_held(result, "git.final_source_lease")

    def test_ignored_source_and_arbitrary_target_subtrees_are_not_exempt(self):
        exclude = self.root / ".git/info/exclude"
        exclude.write_text("src/Hidden.java\n**/target/\n", encoding="utf-8")
        for relative in ["src/Hidden.java", "some-module/target/Hidden.java"]:
            with self.subTest(relative=relative):
                path = self.put(self.root, relative, "class Hidden {}\n")
                self.assertEqual("", self.run_git("ls-files", "--others", "--exclude-standard"))
                self.assert_held(self.result(), "git.source_and_ancestry")
                path.unlink()

    def test_ignored_source_added_during_live_verification_is_refused(self):
        def mutate():
            (self.root / ".git/info/exclude").write_text("src/LateIgnored.java\n", encoding="utf-8")
            self.put(self.root, "src/LateIgnored.java", "class LateIgnored {}\n")
        self.assert_held(self.verified_during_mutation(mutate), "git.final_source_lease")

    def test_assume_unchanged_source_cannot_hide_dirty_input(self):
        self.run_git("update-index", "--assume-unchanged", "M3-SYNEXIA-AUTHORS.md")
        self.put(self.root, "M3-SYNEXIA-AUTHORS.md", "hidden tracked mutation\n")
        self.assert_held(self.result(), "git.source_and_ancestry")

    def test_only_exact_report_root_is_exempt_and_filename_whitespace_is_preserved(self):
        self.put(self.root, " m3/release/queue/runs/impostor.json", {"fake": True})
        self.assert_held(self.result(), "git.source_and_ancestry")

    def test_proof_artifact_drift_during_live_verification_is_refused(self):
        log = "logs/" + self.gate + ".txt"
        result = self.verified_during_mutation(lambda: self.put(self.proof, log, "changed after initial hash\n"))
        self.assert_held(result, "proof.final_integrity")
        self.assertFalse(result["evidence_integrity_ready"])

    def test_proof_pack_drift_during_live_verification_is_refused(self):
        def mutate():
            pack = copy.deepcopy(self.pack)
            pack["unexpected-change"] = True
            self.put(self.proof, "proof-pack.json", pack)
        self.assert_held(self.verified_during_mutation(mutate), "proof.final_integrity")

    def run_output_cli(self, *options):
        return subprocess.run(["python3", "-B", str(MODULE_PATH), "--root", str(self.root),
                               "--release", "--evidence", str(self.pack_path), "--verify-github", *options],
                              capture_output=True, text=True)

    def assert_output_refused(self, completed):
        self.assertEqual(2, completed.returncode, completed.stderr)
        result = json.loads(completed.stdout)
        self.assertFalse(result["release_ready"])
        self.assertEqual("output.preflight", result["blockers"][0]["name"])

    def test_cli_cannot_overwrite_a_tracked_verified_input(self):
        source = self.root / "M3-SYNEXIA-AUTHORS.md"
        original = source.read_bytes()
        self.assert_output_refused(self.run_output_cli("--json-out", str(source)))
        self.assertEqual(original, source.read_bytes())
        self.assertEqual("", self.run_git("status", "--porcelain=v1", "--untracked-files=no"))

    def test_cli_cannot_overwrite_proof_pack_receipts_or_raw_artifacts(self):
        for relative in ["proof-pack.json", "receipts/" + self.gate + ".json", "logs/" + self.gate + ".txt"]:
            with self.subTest(relative=relative):
                target = self.proof / relative
                original = target.read_bytes()
                self.assert_output_refused(self.run_output_cli("--queue-out", str(target)))
                self.assertEqual(original, target.read_bytes())

    def test_cli_output_destinations_cannot_overlap_each_other(self):
        output = self.base / "outputs" / "admission.json"
        for other in [output, output / "nested.tsv"]:
            with self.subTest(other=other):
                self.assert_output_refused(self.run_output_cli("--json-out", str(output), "--queue-out", str(other)))
                self.assertFalse(output.exists())

    def test_cli_output_symlink_and_hardlink_aliases_cannot_modify_source(self):
        source = self.root / "M3-SYNEXIA-AUTHORS.md"
        original = source.read_bytes()
        alias = self.base / "source-alias"
        alias.symlink_to(source)
        self.assert_output_refused(self.run_output_cli("--sha256-out", str(alias)))
        alias.unlink()
        os.link(source, alias)
        self.assert_output_refused(self.run_output_cli("--sha256-out", str(alias)))
        self.assertEqual(original, source.read_bytes())

    def test_cli_output_cannot_modify_git_metadata(self):
        metadata = self.root / ".git/config"
        original = metadata.read_bytes()
        self.assert_output_refused(self.run_output_cli("--json-out", str(metadata)))
        self.assertEqual(original, metadata.read_bytes())

    def test_cli_report_root_does_not_exempt_previously_tracked_outputs(self):
        target = self.put(self.root, "m3/release/queue/runs/historical/report.json", {"historical": True})
        self.commit("tracked historical output fixture")
        original = target.read_bytes()
        self.assert_output_refused(self.run_output_cli("--json-out", str(target)))
        self.assertEqual(original, target.read_bytes())

    def test_custody_preflight_blocks_actual_recipe_residue_and_retains_it(self):
        recipe = self.put(self.root, "m3/runtime-integration/recipe/apply.py", "raise RuntimeError('must never execute')\n")
        self.commit("legacy residue fixture")
        result = release.custody_report(self.root, "foundation")
        self.assert_held(result)
        self.assertIn("m3/runtime-integration/recipe/apply.py", result["residue"])
        self.assertTrue(recipe.is_file())
        self.assertIn("Synexia", result["queue"][0]["next_action"])

    def test_custody_preflight_detects_actual_reactor_wiring(self):
        self.put(self.root, "m3/pom.xml", '<project xmlns="http://maven.apache.org/POM/4.0.0"><modules><module>tooling/migration-recipes</module></modules></project>')
        self.commit("legacy wiring fixture")
        result = release.custody_report(self.root, "control-reactor")
        self.assert_held(result)
        self.assertEqual(["m3/pom.xml -> tooling/migration-recipes"], result["residue"])

    def test_release_cli_writes_reports_before_failing_missing_evidence(self):
        output = self.base / "visible-queue" / "admission.json"
        queue = output.with_name("queue.tsv")
        completed = subprocess.run(["python3", str(MODULE_PATH), "--root", str(self.root), "--release",
                                    "--json-out", str(output), "--queue-out", str(queue)], capture_output=True, text=True)
        self.assertEqual(1, completed.returncode, completed.stderr)
        self.assertFalse(release.load_json(output)["release_ready"])
        self.assertTrue(queue.is_file())


if __name__ == "__main__":
    unittest.main()
