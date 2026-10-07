# SPDX-License-Identifier: Apache-2.0
"""Fixture-only input/refusal tests; no synthetic source identity is publishable."""
import base64
import copy
import csv
import io
import json
from pathlib import Path
import unittest

import bind_mapping as binder


class MappingBindingTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.audit = Path(__file__).resolve().parents[2]
        cls.current = {path: (cls.audit / "candidate/mapping-v04/input-epoch/current" / path).read_bytes() for path in binder.PATHS}
        cls.prior = {path: (cls.audit / "files/prior" / path).read_bytes() for path in binder.PATHS}
        prior_map = json.loads(cls.prior[binder.PATHS[0]])
        def image(body):
            return {"content_base64": base64.b64encode(body).decode(), "bytes": len(body),
                    "sha256": binder.digest(body), "git_blob_sha1": binder.git_object("blob", body)}
        pom = image(b'<project><modules><module>synexia-openrewrite-recipes</module><module>synexia-algo/nested</module></modules>'
                    b'<profiles><profile><id>fixture</id><modules><module>m3-fast-search</module></modules></profile></profiles></project>\n')
        entries = [{"path": "pom.xml", "type": "blob", "mode": "100644", "sha": pom["git_blob_sha1"]}]
        for path in ["synexia-openrewrite-recipes", "synexia-algo", "synexia-donor-inventory", "m3-fast-search"]:
            entries.append({"path": path, "type": "tree", "mode": "040000", "sha": binder.git_object("tree", b"")})
        tree = binder.tree_hash(entries)
        commit = "c" * 40
        ref = "refs/heads/unpublishable-synthetic-fixture"
        artifacts = []
        for record in prior_map["migration"]["records"]:
            if record["id"] not in binder.IDS:
                continue
            sources = list(record["sources"])
            if record["id"] == binder.LAB:
                sources += [{"path": binder.IOP, "module": "synexia-openrewrite-recipes", "symbol": "com.synexia.rewrite.M3IopPatternMechanicalPasses"}]
            for source in sources:
                body = ("// SYNTHETIC FIXTURE ONLY: " + source["path"] + "\n").encode()
                artifacts.append(dict(image(body), path=source["path"], module=source["module"], symbol=source["symbol"],
                    capability_id=record["id"], commit=commit, repository=binder.SOURCE_REPO,
                    readback_verified=True, readback_sha256=binder.digest(b"synthetic readback marker")))
        build_body = (cls.audit.parent / "runtime-resolution/chrome-pom-repair-v01/materialized/synexia-openrewrite-recipes/pom.xml").read_bytes()
        build_component = dict(image(build_body), kind="module-pom-compile-edge", repository=binder.SOURCE_REPO, commit=commit,
            path="synexia-openrewrite-recipes/pom.xml", module="synexia-openrewrite-recipes",
            readback_verified=True, readback_sha256=binder.digest(b"unpublishable fixture build-component marker"))
        cls.packet = {"schema": "m3-jcc-source-publication-input/1", "fixture": True,
            "repository": binder.SOURCE_REPO, "commit": commit, "root_tree": tree, "ref": ref,
            "scope": "current-five-owner-repair-publication", "publication_state": "SYNTHETIC_FIXTURE",
            "publication_receipt_sha256": binder.digest(b"synthetic publication marker"),
            "commit_readback_sha256": binder.digest(b"synthetic commit marker"),
            "root_readback_sha256": binder.digest(b"synthetic root marker"),
            "ref_readback_sha256": binder.digest(b"synthetic ref marker"),
            "commit_readback": {"sha": commit, "tree": {"sha": tree}},
            "root_readback": {"sha": tree, "truncated": False, "tree": entries},
            "ref_readback": {"ref": ref, "object": {"sha": commit}}, "root_pom": pom,
            "artifacts": artifacts, "source_components": [build_component], "source_export_admitted": False, "destination_gates_passed": False,
            "execution_reports": [{"path": "fixture/current-execution.json", "sha256": binder.digest(b"synthetic execution marker"),
                                   "scope": "unpublishable fixture, no tests or runtime acceptance asserted"}],
            "execution_summary": {"fixture": True}}

    def compose(self, packet=None):
        return binder.compose(self.current, self.prior, packet or self.packet, allow_fixture=True)

    def refused(self, mutate, message=None):
        packet = copy.deepcopy(self.packet)
        mutate(packet)
        with self.assertRaises(binder.Refusal) as failure:
            self.compose(packet)
        if message:
            self.assertIn(message, str(failure.exception))

    def test_fixture_is_refused_by_publication_mode(self):
        with self.assertRaisesRegex(binder.Refusal, "fixture inputs are unpublishable"):
            binder.compose(self.current, self.prior, self.packet)

    def test_only20_designated_owners_are_bound_and_all_other_mapping_fields_are_preserved(self):
        after = json.loads(self.compose()[binder.PATHS[0]])
        before = json.loads(self.current[binder.PATHS[0]])
        self.assertEqual([row["id"] for row in before["migration"]["records"]],
                         [row["id"] for row in after["migration"]["records"]])
        self.assertEqual(55, len(after["migration"]["records"]))
        self.assertEqual([row for row in before["migration"]["records"] if row["id"] not in binder.IDS],
                         [row for row in after["migration"]["records"] if row["id"] not in binder.IDS])
        for key in before:
            if key != "migration":
                self.assertEqual(before[key], after[key], key)
        for key in before["migration"]:
            if key != "records":
                self.assertEqual(before["migration"][key], after["migration"][key], key)
        self.assertEqual(20, len(after["migration"]["gates"]))
        self.assertEqual(34, len(after["mappings"]))
        records = {row["id"]: row for row in after["migration"]["records"] if row["id"] in binder.IDS}
        self.assertEqual(14, len(records[binder.LAB]["sources"]))
        self.assertEqual(6, len(records[binder.JNI]["sources"]))
        self.assertEqual(binder.IOP, records[binder.LAB]["sources"][-1]["path"])
        for row in records.values():
            self.assertEqual("blocked", row["status"])
            self.assertEqual([], row["tests"])
            self.assertTrue(all(source["commit"] == self.packet["commit"] for source in row["sources"]))

    def test_every_prior_source_and_target_object_survives_in_lineage(self):
        after = {row["id"]: row for row in json.loads(self.compose()[binder.PATHS[0]])["migration"]["records"]}
        for source in [self.current, self.prior]:
            for row in json.loads(source[binder.PATHS[0]])["migration"]["records"]:
                if row["id"] not in binder.IDS:
                    continue
                for key, previous in [("sources", "previous_sources"), ("targets", "previous_targets")]:
                    preserved = after[row["id"]]["lineage"][previous]
                    for item in row[key] + row["lineage"][previous]:
                        self.assertIn(item, preserved)
        bindings = json.loads(self.compose()[binder.PATHS[2]])
        epochs = bindings["merge_recovery"]["historical_epochs"]
        for source, epoch in [(self.current, epochs[0]), (self.prior, epochs[1])]:
            self.assertEqual([row for row in json.loads(source[binder.PATHS[0]])["migration"]["records"] if row["id"] in binder.IDS], epoch["records"])
        old = json.loads(self.prior[binder.PATHS[2]])
        self.assertEqual(old["source_execution"], epochs[1]["source_execution"])
        self.assertEqual(old["source_publication"], epochs[1]["source_publication"])

    def test_root_obligations_cover_all_exact_root_entries_without_gate_promotion(self):
        outputs = self.compose()
        rows = list(csv.DictReader(io.StringIO(outputs[binder.PATHS[3]].decode()), delimiter="\t"))
        self.assertEqual(len(self.packet["root_readback"]["tree"]), len(rows))
        self.assertEqual({row["path"] for row in self.packet["root_readback"]["tree"]}, {row["root_path"] for row in rows})
        self.assertEqual({self.packet["commit"]}, {row["source_commit"] for row in rows})
        for row in rows:
            self.assertEqual("false", row["export_admitted"])
            self.assertEqual("false", row["destination_materialized"])
            self.assertEqual("false", row["read_back_delivered"])
        bindings = json.loads(outputs[binder.PATHS[2]])
        accounting = bindings["source_root_accounting"]
        self.assertEqual(3, accounting["root_pom_module_declarations"])
        self.assertEqual(2, accounting["direct_root_modules"])
        self.assertEqual(1, accounting["nested_module_paths"])
        self.assertEqual(binder.digest(outputs[binder.PATHS[3]]), accounting["coverage_receipt_sha256"])
        self.assertTrue(all(value is False for value in bindings["acceptance"].values()))
        self.assertEqual(json.loads(self.current[binder.PATHS[2]])["destination_gate_records_preserved"], bindings["destination_gate_records_preserved"])

    def test_repeated_authoring_is_byte_deterministic_and_earlier_prose_is_retained(self):
        first, second = self.compose(), self.compose()
        self.assertEqual(first, second)
        self.assertEqual(set(binder.PATHS), set(first))
        self.assertTrue(first[binder.PATHS[1]].startswith(b"**UNPUBLISHABLE SYNTHETIC FIXTURE OUTPUT.**"))
        self.assertTrue(first[binder.PATHS[1]].endswith(self.current[binder.PATHS[1]]))

    def test_history_only_source_and_missing_publication_evidence_refuse(self):
        self.refused(lambda packet: packet.update(commit=binder.SOURCE_HISTORY), "history-only")
        self.refused(lambda packet: packet.update(scope="history-custody-only"), "publication scope")
        self.refused(lambda packet: packet.pop("publication_receipt_sha256"), "readback receipt")
        self.refused(lambda packet: packet.update(publication_state="PENDING"), "not verified")
        self.refused(lambda packet: packet.update(execution_reports=[]), "execution reports")

    def test_missing_duplicate_or_wrong_capability_owner_refuses(self):
        self.refused(lambda packet: packet["artifacts"].pop(), "20-owner")
        self.refused(lambda packet: packet["artifacts"].__setitem__(1, copy.deepcopy(packet["artifacts"][0])), "duplicate")
        self.refused(lambda packet: packet["artifacts"][0].update(capability_id=binder.JNI), "capability mismatch")
        self.refused(lambda packet: packet["artifacts"][0].update(symbol="different.Owner"), "owner/capability mismatch")
        self.refused(lambda packet: packet["artifacts"][0].update(path="../escape.java"), "unsafe repository path")

    def test_mixed_source_revision_and_body_or_readback_drift_refuse(self):
        self.refused(lambda packet: packet["artifacts"][0].update(commit="d" * 40), "mixed source revisions")
        self.refused(lambda packet: packet["artifacts"][0].update(content_base64=base64.b64encode(b"drift").decode()), "body size drift")
        self.refused(lambda packet: packet["artifacts"][0].update(sha256="0" * 64), "body identity drift")
        self.refused(lambda packet: packet["artifacts"][0].update(git_blob_sha1="0" * 40), "body identity drift")
        self.refused(lambda packet: packet["artifacts"][0].update(readback_verified=False), "body readback")
        self.refused(lambda packet: packet["artifacts"][0].pop("readback_sha256"), "readback receipt")

    def test_truncated_or_changed_root_pom_commit_and_ref_refuse(self):
        self.refused(lambda packet: packet["root_readback"].update(truncated=True), "incomplete source root")
        self.refused(lambda packet: packet["root_readback"]["tree"].pop(), "root tree hash mismatch")
        self.refused(lambda packet: packet["root_pom"].update(sha256="0" * 64), "source body identity drift")
        self.refused(lambda packet: packet["commit_readback"].update(sha="d" * 40), "commit readback mismatch")
        self.refused(lambda packet: packet["ref_readback"]["object"].update(sha="d" * 40), "ref object mismatch")

    def test_unbound_or_promotion_inputs_refuse(self):
        self.refused(lambda packet: packet.update(commit="0" * 40), "unbound source")
        self.refused(lambda packet: packet.update(source_export_admitted=True), "cannot promote")
        self.refused(lambda packet: packet.update(destination_gates_passed=True), "cannot promote")
        self.refused(lambda packet: packet.pop("source_components"), "module-POM source component")
        self.refused(lambda packet: packet["source_components"][0].update(commit="d" * 40), "mixed source build revision")
        self.refused(lambda packet: packet["source_components"][0].update(readback_verified=False), "component readback")

    def test_previous47_record_epoch_is_refused_without_relabelling_it(self):
        earlier = {path: (self.audit / "files/current" / path).read_bytes() for path in binder.PATHS}
        self.assertEqual(47, len(json.loads(earlier[binder.PATHS[0]])["migration"]["records"]))
        with self.assertRaisesRegex(binder.Refusal, "frozen current mapping input drift"):
            binder.compose(earlier, self.prior, self.packet, allow_fixture=True)

    def test_all_prior_and_concurrent_mapping_nodes_survive_in_full(self):
        epoch = json.loads((self.audit / "candidate/mapping-v04/input-epoch/INPUT_EPOCH.json").read_bytes())
        self.assertEqual(binder.INPUT_EPOCH_SHA256,
                         binder.digest((self.audit / "candidate/mapping-v04/input-epoch/INPUT_EPOCH.json").read_bytes()))
        after = json.loads(self.compose()[binder.PATHS[0]])
        before = json.loads(self.current[binder.PATHS[0]])
        self.assertFalse(epoch["all_47_prior_records_unchanged_and_ordered"])
        self.assertTrue(epoch["original_47_ordered_ids_preserved"])
        self.assertEqual(46, epoch["original_47_complete_records_unchanged"])
        self.assertEqual(5, len(epoch["added_records"]))
        added_ids = {row["id"] for row in epoch["added_records"]}
        self.assertEqual(epoch["added_records"],
                         [row for row in after["migration"]["records"] if row["id"] in added_ids])
        self.assertEqual(2, len(epoch["top_level_mapping_updates"]))
        for update in epoch["top_level_mapping_updates"]:
            self.assertNotEqual(update["before"], update["after"])
            self.assertEqual(update["after"], after["mappings"][update["index"]])
        self.assertEqual(8, len(epoch["top_level_mapping_additions"]))
        self.assertEqual(epoch["top_level_mapping_additions"], after["mappings"][17:25])
        self.assertTrue(epoch["prior_52_complete_records_unchanged_and_ordered"])
        self.assertTrue(epoch["prior_25_complete_mappings_unchanged_and_ordered"])
        self.assertEqual(epoch["concurrent_additions"]["records"], after["migration"]["records"][52:])
        self.assertEqual(epoch["concurrent_additions"]["mappings"], after["mappings"][25:])
        self.assertEqual({"donor_artifact_policy", "porting_invariant", "family_name_mapping"}, set(epoch["concurrent_additions"]["top_level_nodes"]))
        for key, value in epoch["concurrent_additions"]["top_level_nodes"].items():
            self.assertEqual(value, after[key])
        self.assertEqual(before["mappings"], after["mappings"])
        self.assertEqual(epoch["prior_record_update"]["after"], after["migration"]["records"][46])
        self.assertEqual(self.packet["source_components"][0]["sha256"], json.loads(self.compose()[binder.PATHS[2]])["source_build_components"][0]["sha256"])
        self.assertEqual(53, len([row for row in after["migration"]["records"] if row["id"] not in binder.IDS]))

    def test_each_frozen_current_body_drift_refuses_before_authoring(self):
        for path in binder.PATHS:
            with self.subTest(path=path):
                changed = dict(self.current)
                changed[path] += b"\n"
                with self.assertRaisesRegex(binder.Refusal, "frozen current mapping input drift"):
                    binder.compose(changed, self.prior, self.packet, allow_fixture=True)


if __name__ == "__main__":
    unittest.main(verbosity=2)
