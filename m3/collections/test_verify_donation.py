#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
# Modified 2026 by Hitesh Soliwal and contributors: select the current exact additive successor inventory named by source_set_successor_current.
# Modified 2026 by Hitesh Soliwal and contributors: accept the sealed transition-receipt successor shape next to the additive-manifest shape.
# Modified 2026 by Hitesh Soliwal and contributors: check the seals of whichever transition-receipt successor is current; unit-test the shared transition atom.
"""Prove current collections manifest custody and reject ambiguous donation JSON."""
import ast
import hashlib
import importlib.util
import json
import tempfile
import unittest
from unittest import mock
from pathlib import Path

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("verify_donation", HERE / "verify_donation.py")
VERIFIER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VERIFIER)


class DonationPacketTest(unittest.TestCase):
    def test_current_packet_selects_a_sha256_sealed_manifest(self):
        packet = VERIFIER.read_donation_packet()
        self.assertEqual(
            "qualification/installed-source-enum-set-20261014.tsv",
            packet["installed_source_manifest"])
        self.assertEqual(
            "qualification/installed-source-enum-set-20261008.tsv",
            packet["historical_installed_source_manifest_20261008"])
        self.assertNotEqual(
            packet["installed_source_manifest"],
            packet["historical_installed_source_manifest_20261008"])
        manifest = HERE / packet["installed_source_manifest"]
        self.assertEqual(packet["installed_source_manifest_sha256"],
                         VERIFIER.digest(manifest))

    def test_current_successor_is_exact_additive(self):
        packet = VERIFIER.read_donation_packet()
        record = packet[packet["source_set_successor_current"]]
        if record.get("schema") == "M3_COLLECTION_SOURCE_GUARD_V1":
            self.assertEqual(packet["source_set_successor_current"],
                             "count_totals_test_source_guard_20261124")
            self.assertEqual(record, VERIFIER._COUNT_TOTALS_GUARD_CONFIG)
            predecessor = "longlane_required_native_source_successor_20261113"
            self.assertEqual(record["predecessor_successor"], predecessor)
            self.assert_transition_receipt_successor(predecessor, packet[predecessor])
            return
        if "successor_receipt" in record:
            self.assert_transition_receipt_successor(packet["source_set_successor_current"], record)
            return
        self.assertEqual(record["installed_manifest"], packet["installed_source_manifest"])
        old_path = HERE / record["predecessor_manifest"]
        self.assertEqual(record["predecessor_manifest_sha256"], VERIFIER.digest(old_path))
        old = old_path.read_text()
        new = (HERE / packet["installed_source_manifest"]).read_text()
        self.assertTrue(new.startswith(old))
        rows = [line.split("\t") for line in new[len(old):].splitlines()
                if line and not line.startswith("#")]
        self.assertEqual(record["added_sources"], [row[0] for row in rows])
        self.assertEqual(record["installed_rows"], record["predecessor_rows"] + len(rows))
        for path, expected in rows:
            self.assertTrue(path.startswith("m3/collections/src/") and path.endswith(".java"))
            self.assertEqual(64, len(expected))
            self.assertNotIn(path + "\t", old)

    def test_duplicate_selector_is_refused_even_if_values_match(self):
        packet = (HERE / "m3-donation.json").read_text()
        duplicate = packet.replace(
            '"historical_installed_source_manifest_20261008"',
            '"installed_source_manifest"', 1)
        self.assertNotEqual(duplicate, packet)
        with self.assertRaisesRegex(ValueError,
                                    "Duplicate donation metadata key: installed_source_manifest"):
            json.loads(duplicate, object_pairs_hook=VERIFIER.unique_json_object)

    def test_nested_duplicate_is_refused(self):
        with self.assertRaisesRegex(ValueError,
                                    "Duplicate donation metadata key: source"):
            json.loads('{"history": {"source": 1, "source": 2}}',
                       object_pairs_hook=VERIFIER.unique_json_object)

    def test_loader_refuses_drift_without_changing_file(self):
        with tempfile.TemporaryDirectory() as folder:
            source = Path(folder) / "donation.json"
            broken = '{"installed_source_manifest": "new.tsv", "installed_source_manifest": "old.tsv"}'
            source.write_text(broken)
            with self.assertRaisesRegex(ValueError,
                                        "Duplicate donation metadata key"):
                VERIFIER.read_donation_packet(source)
            self.assertEqual(broken, source.read_text())

    def assert_transition_receipt_successor(self, name, record):
        _, receipt_sha256, predecessor_sha256, transitions, result_rows, result_java = (
            VERIFIER.SOURCE_TRANSITION_SEALS[name])
        receipt = HERE / record["successor_receipt"]
        self.assertEqual(record["successor_receipt_sha256"], VERIFIER.digest(receipt))
        self.assertEqual(record["successor_receipt_sha256"], receipt_sha256)
        self.assertEqual(record["inherited_composed_manifest_sha256"],
                         predecessor_sha256)
        self.assertEqual(record["resulting_rows"], result_rows)
        self.assertEqual(record["exact_java_source_membership"], result_java)
        rows = [line.split(chr(9)) for line in receipt.read_text(encoding="utf-8").splitlines()]
        self.assertEqual(record["successor_rows"], len(rows))
        self.assertEqual(record["successor_rows"], transitions)
        for path, inherited, after in rows:
            self.assertTrue(path.startswith("m3/collections/"))
            self.assertTrue(after == "REMOVED" or len(after) == 64)
            self.assertTrue(inherited == "ABSENT" or len(inherited) == 64)
            self.assertFalse(inherited == "ABSENT" and after == "REMOVED")

    def test_source_transitions_admit_the_exact_receipt_and_refuse_drift(self):
        tab, newline = chr(9), chr(10)
        path = "m3/collections/src/main/java/jdk/m3/collections/A.java"
        added = "m3/collections/src/test/java/jdk/m3/collections/BTest.java"
        old, new, fresh, other = "a" * 64, "b" * 64, "c" * 64, "d" * 64

        def seal(rows):
            return hashlib.sha256("".join(p + tab + e + newline for p, e in rows).encode("utf-8")).hexdigest()

        text = path + tab + old + tab + new + newline + added + tab + "ABSENT" + tab + fresh + newline
        with tempfile.TemporaryDirectory() as directory:
            receipt = Path(directory) / "receipt.tsv"
            receipt.write_bytes(text.encode("utf-8"))
            sealed = VERIFIER.digest(receipt)
            prior = [(path, old)]
            self.assertEqual([(path, new), (added, fresh)], VERIFIER.admit_source_transitions(
                prior, "Unit", receipt, sealed, seal(prior), 2, 2, 2))
            drifted = [(path, other)]
            with self.assertRaisesRegex(SystemExit, "Unit inherited source preimage drift"):
                VERIFIER.admit_source_transitions(drifted, "Unit", receipt, sealed, seal(drifted), 2, 2, 2)
            with self.assertRaisesRegex(SystemExit, "Unit predecessor receipt chain drift"):
                VERIFIER.admit_source_transitions(prior, "Unit", receipt, sealed, seal(drifted), 2, 2, 2)
            receipt.write_bytes(text.replace(new, other).encode("utf-8"))
            with self.assertRaisesRegex(SystemExit, "Unit successor receipt missing, aliased or drifted"):
                VERIFIER.admit_source_transitions(prior, "Unit", receipt, sealed, seal(prior), 2, 2, 2)

    def test_removed_transition_retires_a_recorded_source_and_refuses_drift(self):
        tab, newline = chr(9), chr(10)
        kept = "m3/collections/src/main/java/jdk/m3/collections/A.java"
        gone = "m3/collections/src/main/java/jdk/m3/collections/M3Old.java"
        old, other = "a" * 64, "d" * 64

        def seal(rows):
            return hashlib.sha256("".join(p + tab + e + newline for p, e in rows).encode("utf-8")).hexdigest()

        with tempfile.TemporaryDirectory() as directory:
            receipt = Path(directory) / "receipt.tsv"
            receipt.write_bytes((gone + tab + old + tab + "REMOVED" + newline).encode("utf-8"))
            sealed = VERIFIER.digest(receipt)
            prior = [(kept, old), (gone, old)]
            self.assertEqual([(kept, old)], VERIFIER.admit_source_transitions(
                prior, "Unit", receipt, sealed, seal(prior), 1, 1, 1))
            drifted = [(kept, old), (gone, other)]
            with self.assertRaisesRegex(SystemExit, "Unit inherited source preimage drift"):
                VERIFIER.admit_source_transitions(drifted, "Unit", receipt, sealed, seal(drifted), 1, 1, 1)
            receipt.write_bytes((gone + tab + "ABSENT" + tab + "REMOVED" + newline).encode("utf-8"))
            with self.assertRaisesRegex(SystemExit, "Unit absent source cannot be removed"):
                VERIFIER.admit_source_transitions(prior, "Unit", receipt, VERIFIER.digest(receipt), seal(prior),
                                                  1, 1, 1)



class CountTotalsSourceGuardTest(unittest.TestCase):
    """Exercise the real historical row pipeline and one generated test-source guard."""
    SOURCE_PATH = 'm3/collections/src/test/java/jdk/m3/collections/CountTotalsTest.java'
    OLD_HASH = '660534fa205ff0fccca987107773637b4c3b625895f452439e904ca6f180cd55'
    NEW_HASH = 'e092c7160e4b8f6791c26e95f018c5ee23099a1db181b088073d7b9bdb477cf6'
    BEFORE_ROOT = '10a8b59ab6bf9d48365e89f38c03c244b301ad5e9488d0a8864442b3a2697425'
    AFTER_ROOT = 'cba9276750d4c18c19224fdcb0850b15ea36674832770a50cf7d32ec58ee92ca'
    GUARD_NAME = 'include_count_totals_test_source_guard'
    CALL = '    rows = include_count_totals_test_source_guard(rows, packet)\n'
    LONG_CALL = '    rows = include_longlane_required_native_source_successor(rows)\n'

    @staticmethod
    def row_root(rows):
        raw = ''.join(path + '\t' + expected + '\n' for path, expected in rows).encode('utf-8')
        return hashlib.sha256(raw).hexdigest()

    def packet(self):
        return VERIFIER.read_donation_packet()

    def source(self):
        return Path(VERIFIER.__file__).read_text(encoding='utf-8')

    def main_rows(self, *, before_guard=False, source=None):
        """Execute only main's actual row-composition assignments, never its build."""
        packet = self.packet()
        manifest = HERE / packet['installed_source_manifest']
        self.assertEqual(packet['installed_source_manifest_sha256'], VERIFIER.digest(manifest))
        rows = [line.split('\t') for line in manifest.read_text(encoding='utf-8').splitlines()
                if line and not line.startswith('#')]
        text = self.source() if source is None else source
        tree = ast.parse(text)
        owner = next(node for node in tree.body if isinstance(node, ast.FunctionDef) and node.name == 'main')
        namespace = dict(vars(VERIFIER), rows=rows, packet=packet)
        for node in owner.body:
            if (isinstance(node, ast.Assign) and len(node.targets) == 1
                    and isinstance(node.targets[0], ast.Name) and node.targets[0].id == 'rows'
                    and isinstance(node.value, ast.Call)):
                if (before_guard and isinstance(node.value.func, ast.Name)
                        and node.value.func.id == self.GUARD_NAME):
                    continue
                unit = ast.Module(body=[node], type_ignores=[])
                exec(compile(ast.fix_missing_locations(unit), VERIFIER.__file__, 'exec'), namespace, namespace)
        return namespace['rows']

    def test_main_composition_admits_exact_count_totals_test(self):
        result = self.main_rows()
        # Execute main's unchanged filesystem-admission loop for this one actual
        # received test source. This is a bounded source check, not the full build.
        owner = next(node for node in ast.parse(self.source()).body
                     if isinstance(node, ast.FunctionDef) and node.name == 'main')
        source_loop = next(node for node in owner.body if isinstance(node, ast.For)
                           and isinstance(node.iter, ast.Name) and node.iter.id == 'rows')
        selected = [(path, expected) for path, expected in result if path == self.SOURCE_PATH]
        self.assertEqual(1, len(selected))
        namespace = dict(vars(VERIFIER), rows=selected)
        try:
            unit = ast.Module(body=[source_loop], type_ignores=[])
            exec(compile(ast.fix_missing_locations(unit), VERIFIER.__file__, 'exec'), namespace, namespace)
        except SystemExit as failure:
            self.fail('Actual installed source admission refused CountTotalsTest: ' + str(failure))
        self.assertEqual(self.AFTER_ROOT, self.row_root(result))
        self.assertEqual(self.NEW_HASH, dict(result)[self.SOURCE_PATH])
        self.assertEqual(1957, len(result))
        self.assertEqual(1951, len({path for path, _ in result if path.endswith('.java') and '/src/' in path}))

    def test_guard_preserves_all_other_ordered_rows_and_input(self):
        prior = self.main_rows(before_guard=True)
        snapshot = [tuple(row) for row in prior]
        self.assertEqual(self.BEFORE_ROOT, self.row_root(prior))
        result = VERIFIER.include_count_totals_test_source_guard(prior, self.packet())
        self.assertEqual(snapshot, [tuple(row) for row in prior])
        self.assertEqual([p for p, _ in prior], [p for p, _ in result])
        changes = [(old, new) for old, new in zip(prior, result) if tuple(old) != tuple(new)]
        self.assertEqual([((self.SOURCE_PATH, self.OLD_HASH), (self.SOURCE_PATH, self.NEW_HASH))],
                         [(tuple(old), tuple(new)) for old, new in changes])
        self.assertEqual(1956, sum(old is new for old, new in zip(prior, result)))

    def test_main_calls_guard_once_immediately_after_longlane(self):
        owner = next(node for node in ast.parse(self.source()).body
                     if isinstance(node, ast.FunctionDef) and node.name == 'main')
        calls = [node.value.func.id for node in owner.body if isinstance(node, ast.Assign)
                 and isinstance(node.value, ast.Call) and isinstance(node.value.func, ast.Name)]
        self.assertEqual(1, calls.count(self.GUARD_NAME))
        self.assertEqual('include_longlane_required_native_source_successor',
                         calls[calls.index(self.GUARD_NAME) - 1])

    def test_omitted_guard_does_not_admit_the_current_test(self):
        source = self.source()
        self.assertEqual(1, source.count(self.CALL))
        rows = self.main_rows(source=source.replace(self.CALL, ''))
        self.assertEqual(self.BEFORE_ROOT, self.row_root(rows))
        self.assertEqual(self.OLD_HASH, dict(rows)[self.SOURCE_PATH])
        self.assertNotEqual(self.AFTER_ROOT, self.row_root(rows))

    def test_misordered_guard_refuses_the_wrong_predecessor(self):
        source = self.source()
        sequence = self.LONG_CALL + self.CALL
        self.assertEqual(1, source.count(sequence))
        with self.assertRaisesRegex(SystemExit, 'predecessor chain drift'):
            self.main_rows(source=source.replace(sequence, self.CALL + self.LONG_CALL))

    def test_duplicate_guard_call_refuses(self):
        source = self.source()
        with self.assertRaisesRegex(SystemExit, 'predecessor chain drift'):
            self.main_rows(source=source.replace(self.CALL, self.CALL + self.CALL))

    def test_changed_count_totals_or_unrelated_preimage_refuses(self):
        prior = self.main_rows(before_guard=True)
        positions = [next(i for i, (p, _) in enumerate(prior) if p == self.SOURCE_PATH),
                     next(i for i, (p, _) in enumerate(prior) if p != self.SOURCE_PATH)]
        for position in positions:
            with self.subTest(position=position):
                changed = list(prior)
                changed[position] = (changed[position][0], '0' * 64)
                with self.assertRaisesRegex(SystemExit, 'predecessor chain drift'):
                    VERIFIER.include_count_totals_test_source_guard(changed, self.packet())

    def test_reordered_predecessor_refuses(self):
        prior = self.main_rows(before_guard=True)
        prior[0], prior[1] = prior[1], prior[0]
        with self.assertRaisesRegex(SystemExit, 'predecessor chain drift'):
            VERIFIER.include_count_totals_test_source_guard(prior, self.packet())

    def test_changed_test_path_refuses(self):
        prior = self.main_rows(before_guard=True)
        index = next(i for i, (path, _) in enumerate(prior) if path == self.SOURCE_PATH)
        prior[index] = (self.SOURCE_PATH.replace('CountTotalsTest.java', 'OtherTest.java'), self.OLD_HASH)
        with self.assertRaisesRegex(SystemExit, 'predecessor chain drift'):
            VERIFIER.include_count_totals_test_source_guard(prior, self.packet())

    def test_already_applied_rows_are_not_readmitted(self):
        rows = self.main_rows()
        self.assertEqual(self.AFTER_ROOT, self.row_root(rows))
        with self.assertRaisesRegex(SystemExit, 'predecessor chain drift'):
            VERIFIER.include_count_totals_test_source_guard(rows, self.packet())

    def test_missing_duplicate_or_extra_rows_refuse(self):
        prior = self.main_rows(before_guard=True)
        duplicate = list(prior)
        duplicate[0] = duplicate[1]
        for rows in (prior[:-1], prior + [prior[0]], duplicate):
            with self.subTest(rows=len(rows)):
                with self.assertRaisesRegex(SystemExit, 'predecessor'):
                    VERIFIER.include_count_totals_test_source_guard(rows, self.packet())

    def test_malformed_predecessor_rows_refuse(self):
        prior = self.main_rows(before_guard=True)
        for row in ('not-a-row', ('path',), ('path', 'hash', 'extra'), ('path', None)):
            with self.subTest(row=row):
                rows = list(prior)
                rows[0] = row
                with self.assertRaisesRegex(SystemExit, 'malformed predecessor rows'):
                    VERIFIER.include_count_totals_test_source_guard(rows, self.packet())

    def test_wrong_current_selector_refuses(self):
        packet = self.packet()
        packet['source_set_successor_current'] = 'longlane_required_native_source_successor_20261113'
        with self.assertRaisesRegex(SystemExit, 'configuration drift'):
            VERIFIER.include_count_totals_test_source_guard(self.main_rows(before_guard=True), packet)

    def test_missing_or_changed_build_configuration_refuses(self):
        prior = self.main_rows(before_guard=True)
        key = 'count_totals_test_source_guard_20261124'
        for field in (None, 'schema', 'guard', 'predecessor_successor', 'unrecognized_permission'):
            with self.subTest(field=field):
                packet = self.packet()
                if field is None:
                    del packet[key]
                else:
                    packet[key][field] = 'unrecognized'
                with self.assertRaisesRegex(SystemExit, 'configuration drift'):
                    VERIFIER.include_count_totals_test_source_guard(prior, packet)

    def test_configuration_cannot_supply_an_arbitrary_source_hash(self):
        packet = self.packet()
        packet['count_totals_test_source_guard_20261124']['source_sha256'] = '0' * 64
        with self.assertRaisesRegex(SystemExit, 'configuration drift'):
            VERIFIER.include_count_totals_test_source_guard(self.main_rows(before_guard=True), packet)

    def test_configuration_cannot_expand_source_membership(self):
        prior = self.main_rows(before_guard=True)
        for field in ('resulting_rows', 'exact_java_source_membership'):
            with self.subTest(field=field):
                packet = self.packet()
                packet['count_totals_test_source_guard_20261124'][field] = 9999
                with self.assertRaisesRegex(SystemExit, 'configuration drift'):
                    VERIFIER.include_count_totals_test_source_guard(prior, packet)

    def test_guard_performs_no_filesystem_receipt_access(self):
        prior, packet = self.main_rows(before_guard=True), self.packet()
        with mock.patch.object(Path, 'open', side_effect=AssertionError('unexpected filesystem access')):
            result = VERIFIER.include_count_totals_test_source_guard(prior, packet)
        self.assertEqual(self.AFTER_ROOT, self.row_root(result))

    def test_changed_generated_result_pin_refuses(self):
        prior, packet = self.main_rows(before_guard=True), self.packet()
        with mock.patch.object(VERIFIER, '_COUNT_TOTALS_RESULT_SHA256', '0' * 64):
            with self.assertRaisesRegex(SystemExit, 'result drift'):
                VERIFIER.include_count_totals_test_source_guard(prior, packet)

    def test_selected_test_source_is_the_received_existing_owner(self):
        source = VERIFIER.located(self.SOURCE_PATH)
        self.assertEqual('test/jdk/jdk/m3/collections/CountTotalsTest.java',
                         source.relative_to(VERIFIER.REPO).as_posix())
        self.assertFalse(source.is_symlink())
        self.assertEqual(self.NEW_HASH, VERIFIER.digest(source))


if __name__ == "__main__":
    unittest.main()
