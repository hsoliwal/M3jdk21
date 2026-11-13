# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Executable red/green controls for full JSR/JEP/JBS commit denominator admission."""
import copy
import hashlib
import csv
import io
import tempfile
import subprocess
from unittest.mock import patch
import unittest
from pathlib import Path

import compatibility_queue
import inventory as upstream_inventory
import jsr_jep_jbs_coverage as gate


class CoverageGateTest(unittest.TestCase):
    SMALL = tuple((release, jsr, major, 1) for release, jsr, major, _ in gate.PLATFORM)

    def fixture(self, platform=None):
        platform = self.SMALL if platform is None else platform
        authority = {r: (600 + r,) for r, _j, _m, _c in platform}
        catalogue = [dict(release=str(r), jep=str(600 + r), disposition="candidate")
                     for r in authority]
        queue = []
        for release, _jsr, _major, count in platform:
            for _ in range(count):
                i = len(queue)
                queue.append(dict(order=str(i), release=str(release), commit=f"{i+1:040x}",
                                  jbs_ids=f"JDK-{8100000+i}" if i == 0 else "",
                                  subject="candidate", domain="core-libs",
                                  inventory_disposition="review", risk="MEDIUM",
                                  scope_floor="FILE", proof_lane="CORE_LIBRARY",
                                  recipe_strategy="OPENREWRITE_OR_HASH_PINNED_JAVA",
                                  priority="35", compatibility_state=gate.PENDING,
                                  next_action="PROVE", paths="src/java.base/A.java"))
        seeds = [dict(release="22", jbs="JDK-8100000", upstream_commit="")]
        return authority, catalogue, seeds, queue

    def test_release_jsr_jep_commit_join_never_claims_implementation(self):
        inputs = self.fixture()
        rows, result = gate.join(*inputs, platform=self.SMALL)
        self.assertEqual(6, result["platform_jsr_count"])
        self.assertEqual(6, result["released_jep_count"])
        self.assertEqual(6, result["upstream_commit_count"])
        self.assertEqual(1, result["subject_candidate_distinct_jbs_ids"])
        self.assertEqual(1, result["jbs_subject_issue_index_rows"])
        self.assertEqual(64, len(result["jbs_subject_issue_index_sha256"]))
        self.assertEqual(5, result["unlinked_upstream_commits"])
        self.assertEqual(18, len(rows))
        self.assertEqual({"JSR", "JEP", "JBS_COMMIT"}, {r["kind"] for r in rows})
        self.assertTrue(all(not result[k] for k in (
            "jsr_platforms_implemented", "all_jeps_implemented",
            "all_jbs_fixes_implemented", "java21_compatibility_proven",
            "promotion_authorized")))
        self.assertEqual(gate.render_tsv(rows), gate.render_tsv(rows))
        self.assertEqual(64, len(result["coverage_tsv_sha256"]))

    def test_missing_or_extra_release_is_refused(self):
        auth, catalogue, seeds, queue = self.fixture()
        with self.assertRaisesRegex(ValueError, "JSR_RELEASE_DENOMINATOR_MISMATCH"):
            gate.join({22: auth[22]}, catalogue, seeds, queue, platform=self.SMALL)

    def test_truncated_queue_is_not_full_jsr_jep_jbs_coverage(self):
        a, c, s, q = self.fixture()
        with self.assertRaisesRegex(ValueError, "INCOMPLETE_UPSTREAM_COMMIT_DENOMINATOR"):
            gate.join(a, c, s, q[:-1], platform=self.SMALL)

    def test_jep_missing_duplicate_or_cross_release_reuse_is_refused(self):
        a, c, s, q = self.fixture()
        with self.assertRaisesRegex(ValueError, "JEP_AUTHORITY_CATALOGUE_DRIFT"):
            gate.join(a, c[:-1], s, q, platform=self.SMALL)
        with self.assertRaisesRegex(ValueError, "JEP_AUTHORITY_CATALOGUE_DRIFT"):
            gate.join(a, c + c[-1:], s, q, platform=self.SMALL)
        reused = copy.deepcopy(a)
        reused[23] = reused[22]
        duped = [dict(release=str(r), jep=str(reused[r][0]), disposition="candidate") for r in reused]
        with self.assertRaisesRegex(ValueError, "JEP_ID_REUSED_ACROSS_RELEASES"):
            gate.join(reused, duped, s, q, platform=self.SMALL)

    def test_jbs_duplicate_commit_order_bad_id_and_forged_promotion_refuse(self):
        a, c, s, queue = self.fixture()
        modifications = (
            ("commit", queue[0]["commit"], "DUPLICATE_OR_INVALID_JBS_COMMIT_ORDER"),
            ("order", "42", "DUPLICATE_OR_INVALID_JBS_COMMIT_ORDER"),
            ("jbs_ids", "JDK-8100001,JDK-8100001", "INVALID_JBS_SUBJECT_CANDIDATE"),
            ("jbs_ids", "8100001", "INVALID_JBS_SUBJECT_CANDIDATE"),
            ("compatibility_state", "PROMOTED", "UNPROVEN_JBS_PROMOTED"),
        )
        for key, value, signature in modifications:
            with self.subTest(key=key, value=value):
                q = copy.deepcopy(queue)
                q[1][key] = value
                with self.assertRaisesRegex(ValueError, signature):
                    gate.join(a, c, s, q, platform=self.SMALL)

    def test_candidate_seed_is_not_a_verified_jbs_issue(self):
        a, c, s, q = self.fixture()
        s[0]["jbs"] = "JDK-9999999"
        _rows, report = gate.join(a, c, s, q, platform=self.SMALL)
        self.assertEqual(["JDK-9999999"], report["seed_ids_not_in_commit_subjects"])
        self.assertFalse(report["all_jbs_fixes_implemented"])
        self.assertFalse(report["queue_recomputed_from_inventory"])
        self.assertFalse(report["upstream_git_history_authenticated"])
        s.append(s[0])
        with self.assertRaisesRegex(ValueError, "DUPLICATE_JBS_SEED"):
            gate.join(a, c, s, q, platform=self.SMALL)

    def test_tsv_reader_schema_and_global_csv_limit_refusal(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "queue.tsv"
            path.write_text("bad\tcolumns\n1\t2\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "unexpected TSV schema"):
                gate.load_tsv(path, gate.QUEUE_COLUMNS, large=True)
            old = csv.field_size_limit()
            path.write_text("\t".join(gate.QUEUE_COLUMNS) + "\n" + "x" * (4*1024*1024+1), encoding="utf-8")
            with self.assertRaises(csv.Error):
                gate.load_tsv(path, gate.QUEUE_COLUMNS, large=True)
            self.assertEqual(old, csv.field_size_limit())

    def test_full_frozen_denominator_is_tested_as_14948_distinct_pending_commits(self):
        a, c, s, q = self.fixture(platform=gate.PLATFORM)
        rows, report = gate.join(a, c, s, q)
        self.assertEqual(14948, report["upstream_commit_count"])
        self.assertEqual(14948+6+6, len(rows))
        self.assertEqual({str(r): count for r, _jsr, _major, count in gate.PLATFORM},
                         report["upstream_commit_counts_by_release"])
        self.assertEqual(14947, report["unlinked_upstream_commits"])
        self.assertTrue(all(r["proof_state"] == gate.PENDING
                            for r in rows if r["kind"] == "JBS_COMMIT"))

    def inventory_fixture(self, queue_rows):
        """Use the original M3JDK21 upstream inventory schema in a local fixture."""
        intervals = {
            str(release): (base, head)
            for release, base, head in upstream_inventory.INTERVALS
        }
        return [
            dict(
                release=row["release"], base_ref=intervals[row["release"]][0],
                head_ref=intervals[row["release"]][1],
                commit=row["commit"], jbs_ids=row["jbs_ids"],
                subject=(row["jbs_ids"] + ": " + row["subject"]
                         if row["jbs_ids"] else row["subject"]),
                domain=row["domain"],
                javac_touch="false", grammar_touch="false",
                hotspot_compiler_touch="false", compatibility_signal="false",
                disposition=row["inventory_disposition"], paths=row["paths"],
            )
            for row in queue_rows
        ]

    def test_existing_queue_classifier_binds_every_field_to_original_inventory(self):
        _a, _c, _s, queue = self.fixture()
        inventory = self.inventory_fixture(queue)
        replay = compatibility_queue.queue(inventory)
        canonical = [
            {field: str(getattr(item, field)) for field in gate.QUEUE_COLUMNS}
            for item in replay
        ]
        self.assertEqual(len(inventory), gate.verify_inventory_queue(inventory, canonical))
        for field in gate.QUEUE_COLUMNS:
            with self.subTest(field=field):
                mutated = copy.deepcopy(canonical)
                mutated[0][field] = "forged"
                with self.assertRaisesRegex(
                    ValueError, "INVENTORY_QUEUE_SOURCE_DRIFT.*field=" + field
                ):
                    gate.verify_inventory_queue(inventory, mutated)
        with self.assertRaisesRegex(ValueError, "INVENTORY_QUEUE_CARDINALITY_DRIFT"):
            gate.verify_inventory_queue(inventory, canonical[:-1])
        altered = copy.deepcopy(inventory)
        altered[1]["subject"] = "source-subject-moved"
        with self.assertRaisesRegex(
            ValueError, "INVENTORY_QUEUE_SOURCE_DRIFT.*field=subject"
        ):
            gate.verify_inventory_queue(altered, canonical)

    def test_full_denominator_requires_canonical_reclassification(self):
        _a, _c, _s, queue = self.fixture(platform=gate.PLATFORM)
        inventory = self.inventory_fixture(queue)
        emitted = compatibility_queue.queue(inventory)
        canonical = [
            {field: str(getattr(item, field)) for field in gate.QUEUE_COLUMNS}
            for item in emitted
        ]
        self.assertEqual(14948, gate.verify_inventory_queue(inventory, canonical))
        self.assertEqual("PENDING_COMPATIBILITY_PROOF", canonical[0]["compatibility_state"])
        tampered = copy.deepcopy(canonical)
        tampered[-1]["jbs_ids"] = "JDK-9999999"
        with self.assertRaisesRegex(ValueError, "INVENTORY_QUEUE_SOURCE_DRIFT"):
            gate.verify_inventory_queue(inventory, tampered)


    def test_original_release_tags_and_subject_issues_are_not_forged(self):
        _a, _c, _s, queue = self.fixture()
        original = self.inventory_fixture(queue)
        def replay(rows):
            return [
                {field: str(getattr(item, field)) for field in gate.QUEUE_COLUMNS}
                for item in compatibility_queue.queue(rows)
            ]

        self.assertEqual(len(original), gate.verify_inventory_queue(original, replay(original)))
        wrong_tag = copy.deepcopy(original)
        wrong_tag[1]["head_ref"] = "jdk-99+1"
        with self.assertRaisesRegex(ValueError, "INVENTORY_GA_TAG_INTERVAL_DRIFT"):
            gate.verify_inventory_queue(wrong_tag, replay(wrong_tag))
        fake_issue = copy.deepcopy(original)
        fake_issue[1]["jbs_ids"] = "JDK-9999999"
        with self.assertRaisesRegex(ValueError, "INVENTORY_SUBJECT_JBS_ID_DRIFT"):
            gate.verify_inventory_queue(fake_issue, replay(fake_issue))
        wrong_subject = copy.deepcopy(original)
        wrong_subject[0]["subject"] = "JDK-8100000 and JDK-8100001: invalid duplicate claim"
        with self.assertRaisesRegex(ValueError, "INVENTORY_SUBJECT_JBS_ID_DRIFT"):
            gate.verify_inventory_queue(wrong_subject, replay(wrong_subject))

    def upstream_git_fixture(self):
        """Tiny *real Git* fixture; never masquerade it as official GA history."""
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        root = Path(directory.name)

        def git(*args):
            result = subprocess.run(
                ["git", "-C", str(root), *args], text=True, capture_output=True,
                check=False,
            )
            self.assertEqual(0, result.returncode, result.stderr)
            return result.stdout.strip()

        git("init", "-q")
        git("config", "user.email", "m3-inventory@example.invalid")
        git("config", "user.name", "M3 Inventory Test")
        cases = (
            ("jdk-21+35", "JDK 21 baseline"),
            ("jdk-22+36", "JDK-8123456: G1 source correction"),
            ("jdk-23+37", "JDK-8123457: javac source correction"),
        )
        for ordinal, (tag, subject) in enumerate(cases):
            (root / f"file{ordinal}.txt").write_text(subject + "\n", encoding="utf-8")
            git("add", ".")
            git("commit", "-qm", subject)
            git("tag", tag)
        pins = tuple((
            tag, git("rev-parse", f"refs/tags/{tag}^{{commit}}")
        ) for tag, _ in cases)
        intervals = (
            (22, "jdk-21+35", "jdk-22+36"),
            (23, "jdk-22+36", "jdk-23+37"),
        )
        with patch.object(upstream_inventory, "INTERVALS", intervals):
            changes = upstream_inventory.inventory(root, (22, 23))
        rows = []
        for change in changes:
            row = {}
            for field in gate.INVENTORY_COLUMNS:
                value = getattr(change, field)
                if field in ("jbs_ids", "paths"):
                    row[field] = ",".join(value)
                elif isinstance(value, bool):
                    row[field] = str(value).lower()
                else:
                    row[field] = str(value)
            rows.append(row)
        return root, pins, intervals, rows, git

    def test_actual_git_replay_matches_every_original_inventory_atom(self):
        root, pins, intervals, rows, _git = self.upstream_git_fixture()
        with patch.object(upstream_inventory, "INTERVALS", intervals):
            self.assertEqual(
                2, gate.verify_actual_git_history(root, rows, tag_pins=pins))

    def test_actual_git_replay_refuses_every_forged_source_atom(self):
        root, pins, intervals, rows, _git = self.upstream_git_fixture()
        with patch.object(upstream_inventory, "INTERVALS", intervals):
            for field in gate.INVENTORY_COLUMNS:
                with self.subTest(field=field):
                    mutated = copy.deepcopy(rows)
                    mutated[0][field] = "FORGED"
                    with self.assertRaisesRegex(
                            ValueError, "UPSTREAM_INVENTORY_GIT_DRIFT"):
                        gate.verify_actual_git_history(root, mutated, tag_pins=pins)
            with self.assertRaisesRegex(
                    ValueError, "UPSTREAM_INVENTORY_CARDINALITY_DRIFT"):
                gate.verify_actual_git_history(root, rows[:-1], tag_pins=pins)

    def test_actual_git_replay_refuses_changed_or_missing_ga_tags(self):
        root, pins, intervals, rows, git = self.upstream_git_fixture()
        with patch.object(upstream_inventory, "INTERVALS", intervals):
            git("tag", "-f", "jdk-22+36", pins[-1][1])
            with self.assertRaisesRegex(
                    ValueError, "UPSTREAM_GA_TAG_COMMIT_DRIFT"):
                gate.verify_actual_git_history(root, rows, tag_pins=pins)
            with self.assertRaisesRegex(
                    ValueError, "UPSTREAM_GA_TAG_SET_DRIFT"):
                gate.verify_actual_git_history(root, rows, tag_pins=pins[:-1])
            git("tag", "-f", "jdk-22+36", pins[1][1])
            git("tag", "-d", "jdk-23+37")
            with self.assertRaises(RuntimeError):
                gate.verify_actual_git_history(root, rows, tag_pins=pins)

    def test_issue_candidate_groups_multiple_commit_sources_and_secondary_issue(self):
        queue = [
            dict(release="22", commit=f"{1:040x}", jbs_ids="JDK-8334060,JDK-8320175",
                 compatibility_state=gate.PENDING),
            dict(release="24", commit=f"{2:040x}", jbs_ids="JDK-8334060",
                 compatibility_state=gate.PENDING),
            dict(release="27", commit=f"{3:040x}", jbs_ids="JDK-8320175",
                 compatibility_state=gate.PENDING),
            dict(release="25", commit=f"{4:040x}", jbs_ids="",
                 compatibility_state=gate.PENDING),
        ]
        rows = gate.jbs_issue_index(queue)
        self.assertEqual(["JDK-8320175", "JDK-8334060"],
                         [row["jbs_id"] for row in rows])
        self.assertEqual(["22,27", "22,24"], [row["release_list"] for row in rows])
        self.assertEqual(["2", "2"], [row["source_commit_count"] for row in rows])
        self.assertEqual(rows, gate.jbs_issue_index(list(reversed(queue))))
        self.assertTrue(all(row["proof_state"] == gate.PENDING for row in rows))
        self.assertEqual(gate.render_jbs_index_tsv(rows), gate.render_jbs_index_tsv(rows))

    def test_issue_index_rejects_duplicate_forged_or_promoted_commit(self):
        def source(number, issue, status=gate.PENDING, release="22"):
            return dict(release=release, commit=f"{number:040x}",
                        jbs_ids=issue, compatibility_state=status)
        original = source(1, "JDK-8371656")
        rejected = (
            ([original, original], "INVALID_ISSUE_SOURCE_COMMIT"),
            ([original, source(2, "JDK-8371656,JDK-8371656")],
             "INVALID_ISSUE_SOURCE_IDS"),
            ([original, source(2, "8371656")], "INVALID_ISSUE_SOURCE_IDS"),
            ([original, source(2, "JDK-8371656", "PROMOTED")],
             "UNVERIFIED_ISSUE_SOURCE_PROMOTED"),
            ([original, source(2, "JDK-8371656", release="28")],
             "INVALID_ISSUE_SOURCE_COMMIT"),
        )
        for rows, signature in rejected:
            with self.subTest(reason=signature):
                with self.assertRaisesRegex(ValueError, signature):
                    gate.jbs_issue_index(rows)

    def test_full_commit_queue_index_retains_every_subject_candidate(self):
        rows = [
            dict(release=str(22 + i % 6), commit=f"{i+1:040x}",
                 jbs_ids=f"JDK-{8300000+i%71}" if i % 13 else "",
                 compatibility_state=gate.PENDING)
            for i in range(14948)
        ]
        issues = gate.jbs_issue_index(rows)
        self.assertEqual(71, len(issues))
        self.assertTrue(all(row["proof_state"] == gate.PENDING for row in issues))
        self.assertEqual(
            sum(bool(row["jbs_ids"]) for row in rows),
            sum(int(row["source_commit_count"]) for row in issues),
        )
        self.assertEqual(64, len(hashlib.sha256(
            gate.render_jbs_index_tsv(issues).encode("utf-8")
        ).hexdigest()))

    def test_full_issue_index_requires_canonical_numeric_parser(self):
        with patch.object(upstream_inventory, "_subject_jbs_ids", None, create=True):
            with self.assertRaisesRegex(
                    ValueError, "JBS_ISSUE_INDEX_NUMERIC_PARSER_MISSING"):
                gate.require_complete_jbs_issue_parser()
        with patch.object(upstream_inventory, "_subject_jbs_ids",
                          lambda _value: ("JDK-9999999",), create=True):
            with self.assertRaisesRegex(
                    ValueError, "JBS_ISSUE_INDEX_NUMERIC_PARSER_INCOMPATIBLE"):
                gate.require_complete_jbs_issue_parser()
        answers = {
            "8345678: fix": ("JDK-8345678",),
            "8345678, 8345679: two fixes": ("JDK-8345678", "JDK-8345679"),
            "8320175: [BACKOUT] 8316533: regression": ("JDK-8320175", "JDK-8316533"),
            "Merge 8345678 fix": (),
        }
        with patch.object(upstream_inventory, "_subject_jbs_ids",
                          lambda subject: answers[subject], create=True):
            self.assertIsNone(gate.require_complete_jbs_issue_parser())

    def test_legacy_parser_cannot_emit_claimed_complete_issue_index(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            output = root / "m3/backports/target/all-issues.tsv"
            with patch.object(upstream_inventory, "_subject_jbs_ids", None, create=True):
                with self.assertRaisesRegex(
                        ValueError, "JBS_ISSUE_INDEX_NUMERIC_PARSER_MISSING"):
                    gate.main([
                        "--root", str(root),
                        "--queue", str(output),
                        "--inventory", str(output),
                        "--upstream-repo", str(root),
                        "--tsv-out", str(output),
                        "--json-out", str(root / "report.json"),
                        "--jbs-index-out", str(output),
                    ])
            self.assertFalse(output.exists(), "partial index must not be written")


    def test_coverage_outputs_cannot_overwrite_queue_or_upstream_inventory(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            queue = root / "COMPATIBILITY_QUEUE.tsv"
            inventory = root / "UPSTREAM_CHANGES.tsv"
            queue.write_text("QUEUE ORIGINAL\n", encoding="utf-8")
            inventory.write_text("INVENTORY ORIGINAL\n", encoding="utf-8")
            coverage = root / "coverage.tsv"
            report = root / "report.json"
            for outputs in ((queue, report),
                            (coverage, inventory),
                            (queue, report, root / "issues.tsv"),
                            (coverage, report, inventory)):
                with self.subTest(outputs=outputs):
                    with self.assertRaisesRegex(
                            ValueError, "INPUT_OUTPUT_PATH_COLLISION"):
                        gate.ensure_distinct_output_paths((queue, inventory), outputs)
            self.assertEqual("QUEUE ORIGINAL\n", queue.read_text(encoding="utf-8"))
            self.assertEqual("INVENTORY ORIGINAL\n", inventory.read_text(encoding="utf-8"))

    def test_coverage_outputs_require_unique_artifact_paths(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            queue = root / "queue.tsv"
            inventory = root / "inventory.tsv"
            main = root / "coverage.tsv"
            report = root / "report.json"
            issues = root / "issues.tsv"
            with self.assertRaisesRegex(ValueError, "OUTPUT_PATH_COLLISION"):
                gate.ensure_distinct_output_paths((queue, inventory),
                                                  (main, report, main))
            with self.assertRaisesRegex(ValueError, "OUTPUT_PATH_COLLISION"):
                gate.ensure_distinct_output_paths((queue, inventory),
                                                  (report, report))
            self.assertIsNone(
                gate.ensure_distinct_output_paths(
                    (queue, inventory), (main, report, issues)
                )
            )


if __name__ == "__main__":
    unittest.main()
