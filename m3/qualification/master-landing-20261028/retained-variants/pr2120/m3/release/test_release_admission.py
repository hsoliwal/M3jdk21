# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import importlib.util
import hashlib
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

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

    def test_sha256_is_stable(self):
        with tempfile.TemporaryDirectory() as td:
            path = Path(td) / "x"
            path.write_bytes(b"m3\n")
            self.assertEqual(
                "3831561589b9de83145d35aebf9d2b55a9843a09e8b7754bedd56173e25b3aa6",
                release.sha256(path),
            )


class ReleaseEvidenceUnitTest(unittest.TestCase):
    """Synthetic proof packs test admission mechanics, never platform qualification."""

    HEAD = "a" * 40
    TREE = "b" * 40
    PLATFORM = "linux-x64"

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name) / "repo"
        self.root.mkdir()
        for relative in release.REQUIRED_FILES:
            path = self.root / relative
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("synthetic test fixture\n", encoding="utf-8")
        self.contract_path = self.root / "m3/release/release-contract.json"
        (self.root / "m3/release/verify_release_admission.py").write_bytes(MODULE_PATH.read_bytes())
        self.contract = json.loads(MODULE_PATH.with_name("release-contract.json").read_text())
        self.save_contract()
        (self.root / "M3-SYNEXIA-NOTICE.md").write_text(
            "This notice does not relicense\nOpenJDK or JDK-derived source code as Apache-2.0.\n")
        (self.root / "M3-SYNEXIA-RIGHTS.md").write_text(
            "All ownership and all rights not expressly granted by the applicable license\n")
        (self.root / "m3/README.md").write_text(
            "does **not** modify `java.lang.String`\n"
            "No speedup, complete compatibility, or full jtreg pass is claimed.\n")
        mapping = self.root / "m3/docs/name-mapping.json"
        mapping.parent.mkdir()
        mapping.write_text(json.dumps({"porting_invariant": {"receiving_sequence": {
            "order": release.EXPECTED_RECEIVING_ORDER, "active_phase": "STRING"}}}))
        self.pack = Path(self.temp.name) / "synthetic-proof"
        self.pack.mkdir()
        artifact = self.pack / "synthetic-image.txt"
        artifact.write_text("SYNTHETIC TEST IMAGE, NOT A JDK BUILD\n")
        self.data = {
            "schema": "M3_RELEASE_EVIDENCE_V1", "source_commit": self.HEAD,
            "source_tree": self.TREE, "platform": self.PLATFORM,
            "artifacts": [{"path": artifact.name, "sha256": release.sha256(artifact)}],
            "gates": [],
        }
        for name in release.EXPECTED_RELEASE_GATES:
            proof = self.pack / (name + ".log")
            proof.write_text("SYNTHETIC RECEIPT FIXTURE: " + name + "\n")
            self.data["gates"].append({
                "name": name, "status": "PASS", "exit_code": 0, "tests_run": 1,
                "command": ["fixture-only", name], "environment": {"runner": "synthetic"},
                "proofs": [{"path": proof.name, "sha256": release.sha256(proof)}],
            })
        self.manifest = self.pack / "evidence.json"

    def save_contract(self):
        self.contract_path.write_text(json.dumps(self.contract), encoding="utf-8")

    def fake_git(self, root, *args):
        if args == ("rev-parse", "HEAD"):
            return self.HEAD
        if args == ("rev-parse", "HEAD^{tree}"):
            return self.TREE
        if args == ("merge-base", "HEAD", self.contract["release"]["upstream_baseline"]):
            return self.contract["release"]["upstream_baseline"]
        if args == ("status", "--porcelain=v1", "--untracked-files=no"):
            return ""
        if args == ("ls-files", "--others", "-z"):
            return ""
        if len(args) == 4 and args[:3] == ("ls-tree", "HEAD", "--"):
            raw = (self.root / args[3]).read_bytes()
            blob = hashlib.sha1(b"blob " + str(len(raw)).encode() + b"\0" + raw).hexdigest()
            return f"100644 blob {blob}\t{args[3]}"
        raise AssertionError("unexpected git command: " + repr(args))

    def inspect(self, *, evidence=True, release_mode=True, platform=PLATFORM):
        self.manifest.write_text(json.dumps(self.data), encoding="utf-8")
        with patch.object(release, "git", side_effect=self.fake_git):
            return release.validate(self.root, release_mode=release_mode,
                                    evidence=self.manifest if evidence else None,
                                    platform=platform)

    def assertBlocked(self, result, expected=None):
        self.assertFalse(result["release_ready"])
        self.assertTrue(result["blockers"], result)
        if expected:
            self.assertTrue(any(expected in row["name"] for row in result["blockers"]), result)

    def test_source_only_development_never_claims_release_ready(self):
        result = self.inspect(evidence=False, release_mode=False)
        self.assertTrue(result["source_admissible"], result)
        self.assertFalse(result["release_ready"])
        self.assertFalse(result["blockers"])

    def test_release_requires_gate_evidence(self):
        self.assertBlocked(self.inspect(evidence=False), "evidence.required")

    def test_complete_synthetic_pack_is_admitted_only_for_its_exact_scope(self):
        result = self.inspect()
        self.assertTrue(result["release_ready"], result)
        self.assertEqual(self.PLATFORM, result["platform"])
        self.assertEqual("LOCAL_PROOF_PACK_INTEGRITY_NOT_REMOTE_ATTESTATION", result["admission_scope"])
        self.assertFalse(self.inspect(release_mode=False)["release_ready"])

    def test_receipt_commands_remain_opaque_data(self):
        marker = self.pack / "must-not-execute"
        self.data["gates"][0]["command"] = [sys.executable, "-c", f"open({str(marker)!r}, 'w').close()"]
        self.assertTrue(self.inspect()["release_ready"])
        self.assertFalse(marker.exists())

    def test_every_missing_gate_blocks(self):
        original = list(self.data["gates"])
        for index in range(len(original)):
            with self.subTest(gate=original[index]["name"]):
                self.data["gates"] = original[:index] + original[index + 1:]
                self.assertBlocked(self.inspect())

    def test_every_failed_skipped_pending_or_unknown_gate_blocks(self):
        for gate in self.data["gates"]:
            for status in ("FAIL", "SKIP", "PENDING", "UNKNOWN", "pass", None):
                with self.subTest(gate=gate["name"], status=status):
                    gate["status"] = status
                    self.assertBlocked(self.inspect(), "evidence.gate:")
            gate["status"] = "PASS"

    def test_duplicate_unknown_and_nonobject_gates_block(self):
        original = self.data["gates"][0]
        for replacement in (self.data["gates"][1], {"name": "UNKNOWN"}, None, []):
            with self.subTest(replacement=replacement):
                self.data["gates"][0] = replacement
                self.assertBlocked(self.inspect())
        self.data["gates"][0] = original

    def test_zero_or_noninteger_test_counts_block_every_test_gate(self):
        for gate in self.data["gates"]:
            if gate["name"] not in release.TEST_GATES:
                continue
            for count in (0, -1, True, "1", None):
                with self.subTest(gate=gate["name"], count=count):
                    gate["tests_run"] = count
                    self.assertBlocked(self.inspect())
            gate["tests_run"] = 1

    def test_nonzero_or_boolean_exit_code_blocks(self):
        for code in (1, -1, True, False, "0", None):
            with self.subTest(code=code):
                self.data["gates"][0]["exit_code"] = code
                self.assertBlocked(self.inspect())

    def test_empty_command_environment_or_proofs_block(self):
        gate = self.data["gates"][0]
        for key, values in {"command": [[], "cmd", [""]],
                            "environment": [{}, {"runner": ""}, []],
                            "proofs": [[], [None], "log"]}.items():
            original = gate[key]
            for value in values:
                with self.subTest(key=key, value=value):
                    gate[key] = value
                    self.assertBlocked(self.inspect())
            gate[key] = original

    def test_stale_commit_tree_or_platform_blocks(self):
        for key in ("source_commit", "source_tree", "platform"):
            original = self.data[key]
            self.data[key] = "different"
            self.assertBlocked(self.inspect())
            self.data[key] = original
        for platform in (None, "../linux", "linux-x64/extra", "linux--x64", "macos-arm64"):
            self.assertBlocked(self.inspect(platform=platform))

    def test_each_proof_hash_is_checked(self):
        for gate in self.data["gates"]:
            original = gate["proofs"][0]["sha256"]
            gate["proofs"][0]["sha256"] = "0" * 64
            self.assertBlocked(self.inspect())
            gate["proofs"][0]["sha256"] = original

    def test_missing_empty_changed_or_duplicate_product_artifact_blocks(self):
        original = list(self.data["artifacts"])
        for artifacts in ([], original * 2, [{"path": "missing", "sha256": "0" * 64}]):
            self.data["artifacts"] = artifacts
            self.assertBlocked(self.inspect())
        self.data["artifacts"] = original
        (self.pack / original[0]["path"]).write_bytes(b"")
        self.assertBlocked(self.inspect())
        (self.pack / original[0]["path"]).write_bytes(b"changed image")
        self.assertBlocked(self.inspect())

    def test_unsafe_paths_and_symlinks_block(self):
        proof = self.data["gates"][0]["proofs"][0]
        original = proof["path"]
        for path in ("../outside.log", "/absolute.log", "a/../b", "./log", "a//b", "a\\b", ""):
            with self.subTest(path=path):
                proof["path"] = path
                self.assertBlocked(self.inspect())
        proof["path"] = "linked.log"
        (self.pack / "linked.log").symlink_to(self.pack / original)
        self.assertBlocked(self.inspect())

    def test_manifest_schema_unknown_fields_and_duplicates_block(self):
        self.data["extra"] = "unrecognized"
        self.assertBlocked(self.inspect())
        del self.data["extra"]
        self.data["schema"] = "other"
        self.assertBlocked(self.inspect())
        self.manifest.write_text('{"schema":"a","schema":"b"}')
        with patch.object(release, "git", side_effect=self.fake_git):
            self.assertBlocked(release.validate(self.root, True, self.manifest, self.PLATFORM))

    def test_oversized_manifest_and_proof_block(self):
        self.manifest.write_bytes(b" " * (release.MAX_EVIDENCE_BYTES + 1))
        with patch.object(release, "git", side_effect=self.fake_git):
            self.assertBlocked(release.validate(self.root, True, self.manifest, self.PLATFORM))
        proof = self.pack / self.data["gates"][0]["proofs"][0]["path"]
        with proof.open("wb") as stream:
            stream.truncate(release.MAX_PROOF_BYTES + 1)
        self.assertBlocked(self.inspect())

    def test_contract_cannot_remove_or_duplicate_mandatory_gates(self):
        original = self.contract["mandatory_release_gates"]
        for gates in ([], original[:-1], original + [original[0]]):
            self.contract["mandatory_release_gates"] = gates
            self.save_contract()
            self.assertBlocked(self.inspect(), "contract.mandatory_release_gates")

    def test_malformed_contract_and_missing_boundary_file_fail_closed(self):
        self.contract_path.write_text("[]")
        self.assertBlocked(self.inspect(), "contract.read")
        self.contract_path.write_text("{")
        self.assertBlocked(self.inspect(), "contract.read")
        self.save_contract()
        (self.root / "M3-SYNEXIA-NOTICE.md").unlink()
        self.assertBlocked(self.inspect(), "file:")

    def test_dirty_tracked_source_blocks(self):
        def dirty(root, *args):
            return " M changed.java" if args[0] == "status" else self.fake_git(root, *args)
        self.manifest.write_text(json.dumps(self.data))
        with patch.object(release, "git", side_effect=dirty):
            self.assertBlocked(release.validate(self.root, True, self.manifest, self.PLATFORM), "git.worktree")

    def initialize_source_checkout(self):
        def command(*args):
            return subprocess.check_output(["git", "-C", str(self.root), *args], text=True,
                                           stderr=subprocess.STDOUT).strip()
        command("init", "--quiet")
        command("config", "user.name", "Synthetic Test")
        command("config", "user.email", "synthetic@example.invalid")
        command("config", "commit.gpgsign", "false")
        command("config", "core.autocrlf", "false")
        command("add", ".")
        command("commit", "--quiet", "-m", "synthetic baseline")
        self.contract["release"]["upstream_baseline"] = command("rev-parse", "HEAD")
        self.save_contract()
        command("add", ".")
        command("commit", "--quiet", "-m", "append release contract")
        (self.root / "follow-up.txt").write_text("append, never squash\n")
        command("add", ".")
        command("commit", "--quiet", "-m", "append follow-up")
        self.data["source_commit"] = command("rev-parse", "HEAD")
        self.data["source_tree"] = command("rev-parse", "HEAD^{tree}")
        self.manifest.write_text(json.dumps(self.data))
        return command

    def test_append_only_history_with_multiple_commits_is_accepted(self):
        self.initialize_source_checkout()
        result = release.validate(self.root, True, self.manifest, self.PLATFORM)
        self.assertTrue(result["release_ready"], result)
        cli = [sys.executable, "-B", str(MODULE_PATH), "--root", str(self.root)]
        admitted = subprocess.run(cli + ["--release", "--evidence", str(self.manifest),
                                        "--platform", self.PLATFORM], capture_output=True, text=True)
        self.assertEqual(0, admitted.returncode, admitted.stdout + admitted.stderr)
        refused = subprocess.run(cli + ["--release"], capture_output=True, text=True)
        self.assertEqual(1, refused.returncode, refused.stdout + refused.stderr)
        development = subprocess.run(cli, capture_output=True, text=True)
        self.assertEqual(0, development.returncode)
        self.assertFalse(json.loads(development.stdout)["release_ready"])
        (self.root / "M3-SYNEXIA-NOTICE.md").write_text("invalid notice")
        invalid_source = subprocess.run(cli, capture_output=True, text=True)
        self.assertEqual(1, invalid_source.returncode, invalid_source.stdout + invalid_source.stderr)

    def test_cli_outputs_cannot_overwrite_inputs_or_alias_each_other(self):
        self.initialize_source_checkout()
        image = self.pack / self.data["artifacts"][0]["path"]
        proof = self.pack / self.data["gates"][0]["proofs"][0]["path"]
        alias = self.pack / "image-hardlink.txt"
        alias.hardlink_to(image)
        symlink = self.pack / "image-symlink.txt"
        symlink.symlink_to(image)
        cli = [sys.executable, "-B", str(MODULE_PATH), "--root", str(self.root),
               "--release", "--evidence", str(self.manifest), "--platform", self.PLATFORM]
        for flag in ("--json-out", "--sha256-out"):
            for path in (image, proof, self.manifest, alias, symlink, self.root / "M3-SYNEXIA-NOTICE.md"):
                with self.subTest(flag=flag, path=path.name):
                    before = path.read_bytes()
                    result = subprocess.run(cli + [flag, str(path)], capture_output=True, text=True)
                    self.assertEqual(1, result.returncode, result.stdout + result.stderr)
                    self.assertFalse(json.loads(result.stdout)["release_ready"])
                    self.assertEqual(before, path.read_bytes())
        same = self.pack / "same-output.json"
        result = subprocess.run(cli + ["--json-out", str(same), "--sha256-out", str(same)],
                                capture_output=True, text=True)
        self.assertEqual(1, result.returncode)
        self.assertFalse(same.exists())
        inside = self.root / "new-admission.json"
        result = subprocess.run(cli + ["--json-out", str(inside)], capture_output=True, text=True)
        self.assertEqual(1, result.returncode)
        self.assertFalse(inside.exists())
        result_file, checksum_file = self.pack / "admission.json", self.pack / "SHA256SUMS"
        result = subprocess.run(cli + ["--json-out", str(result_file), "--sha256-out", str(checksum_file)],
                                capture_output=True, text=True)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertTrue(json.loads(result_file.read_text())["release_ready"])
        self.assertTrue(checksum_file.read_text())

    def test_untracked_and_ignored_source_files_block_release(self):
        command = self.initialize_source_checkout()
        source = self.root / "src/Injected.java"
        source.parent.mkdir()
        source.write_text("class Injected {}")
        self.assertBlocked(release.validate(self.root, True, self.manifest, self.PLATFORM), "git.untracked_source")
        source.unlink()
        (self.root / ".gitignore").write_text("src/Injected.java\n")
        command("add", ".gitignore")
        command("commit", "--quiet", "-m", "synthetic ignored source witness")
        source.write_text("class Injected {}")
        self.data["source_commit"] = command("rev-parse", "HEAD")
        self.data["source_tree"] = command("rev-parse", "HEAD^{tree}")
        self.manifest.write_text(json.dumps(self.data))
        self.assertBlocked(release.validate(self.root, True, self.manifest, self.PLATFORM), "git.untracked_source")

    def test_tracked_symlink_boundary_inputs_block_release(self):
        command = self.initialize_source_checkout()
        original = self.contract_path.read_bytes()
        external = self.pack / "external-contract.json"
        external.write_bytes(original)
        self.contract_path.unlink()
        self.contract_path.symlink_to(external)
        command("add", ".")
        command("commit", "--quiet", "-m", "synthetic symlink witness")
        self.data["source_commit"] = command("rev-parse", "HEAD")
        self.data["source_tree"] = command("rev-parse", "HEAD^{tree}")
        self.manifest.write_text(json.dumps(self.data))
        self.assertBlocked(release.validate(self.root, True, self.manifest, self.PLATFORM), "git.input:m3/release/release-contract.json")

    def test_boundary_blob_drift_and_verifier_identity_block(self):
        original = self.fake_git
        def stale_blob(root, *args):
            value = original(root, *args)
            if args[:3] == ("ls-tree", "HEAD", "--"):
                return "100644 blob " + "0" * 40 + "\t" + args[3]
            return value
        self.manifest.write_text(json.dumps(self.data))
        with patch.object(release, "git", side_effect=stale_blob):
            self.assertBlocked(release.validate(self.root, True, self.manifest, self.PLATFORM), "git.input:")
        (self.root / "m3/release/verify_release_admission.py").write_text("different verifier\n")
        self.assertBlocked(self.inspect(), "git.verifier_identity")


if __name__ == "__main__":
    unittest.main()
