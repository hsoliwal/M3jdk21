#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Source-backed file workflow and receipt-consumer regression tests.

Receipt fixtures qualify only the consumer. Actual Java contracts and JNI gates
must execute separately; these tests never simulate Java implementation types.
"""
import ast
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import textwrap
import unittest

ROOT = Path(__file__).resolve().parents[2]
FILE = ROOT / 'm3/file'
WORKFLOWS = ROOT / '.github/workflows'
MOVE = 'm3-file-move-candidates-20261021.yml'
SHADOW = 'm3-file-shadow-stream-20261020.yml'
LAYERED = 'm3-native-shadow-stream-progress-20261020.yml'
DELTA = 'm3-file-delta-20261020.yml'
IMPACT = 'm3-file-build-impact-20261020.yml'


def workflow(name):
    return (WORKFLOWS / name).read_text(encoding='utf-8')


def receipt_guard():
    lines = workflow(SHADOW).splitlines()
    start = next(i for i, line in enumerate(lines) if line.endswith("<<'PY'")) + 1
    end = next(i for i in range(start, len(lines)) if lines[i].strip() == 'PY')
    return textwrap.dedent('\n'.join(lines[start:end]))


def declared_roles():
    """Read the real verifier's literal role declarations and complete test tree."""
    names = {'contracts', 'image_delta_contracts', 'host_compiler_contracts',
             'bounded_probe_contracts', 'maven_junit_sources'}
    roles = {}
    tree = ast.parse((FILE / 'verify.py').read_text(encoding='utf-8'))
    for node in sorted(ast.walk(tree), key=lambda item: getattr(item, 'lineno', 0)):
        if (isinstance(node, ast.Assign) and len(node.targets) == 1
                and isinstance(node.targets[0], ast.Name) and node.targets[0].id in names):
            roles[node.targets[0].id] = ast.literal_eval(node.value)
        elif (isinstance(node, ast.AugAssign) and isinstance(node.target, ast.Name)
              and node.target.id == 'maven_junit_sources' and isinstance(node.op, ast.BitOr)):
            roles[node.target.id] |= ast.literal_eval(node.value)
    if set(roles) != names:
        raise AssertionError('review changed verifier role declarations')
    sources = sorted((FILE / 'src/test/java').rglob('*.java'))
    roles['fixtures'] = {p.stem for p in sources if p.parent.name == 'donor'}
    roles['sources'] = sources
    return roles


def source_receipt():
    roles = declared_roles()
    shared = {**roles['contracts'], **roles['image_delta_contracts']}
    probes = {name + ':' + argument: marker
              for name, cases in roles['bounded_probe_contracts'].items()
              for argument, marker in cases}
    return {
        'state': 'PASSED',
        'host_module_contracts': {**shared, **roles['host_compiler_contracts'], **probes},
        'image_module_contracts': {**shared, **probes},
        'contract_inventory': {
            'source_count': len(roles['sources']),
            'host_and_image': sorted(shared),
            'host_javac_only': sorted(roles['host_compiler_contracts']),
            'bounded_probe_host_and_image': sorted(roles['bounded_probe_contracts']),
            'fixtures_no_entrypoint': sorted(roles['fixtures']),
            'maven_junit_separate_gate': sorted(roles['maven_junit_sources']),
        },
        'full_m3jdk21_bootstrap': 'NOT_RUN',
        'maven_fork_execution': 'SEPARATE_GATE',
    }


def check_receipt(receipt, *, duplicate_state=False):
    with tempfile.TemporaryDirectory(prefix='m3-file-receipt-test-') as directory:
        path = Path(directory) / 'receipt.json'
        text = json.dumps(receipt)
        if duplicate_state:
            text = '{"state":"PASSED",' + text[1:]
        path.write_text(text, encoding='utf-8')
        return subprocess.run([sys.executable, '-c', receipt_guard(), str(path)],
                              text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                              check=False, timeout=30)


class FileWorkflowWiringTest(unittest.TestCase):
    def test_current_source_roles_are_disjoint_and_complete(self):
        roles = declared_roles()
        partitions = [set(roles[name]) for name in
                      ('contracts', 'image_delta_contracts', 'host_compiler_contracts',
                       'bounded_probe_contracts', 'maven_junit_sources', 'fixtures')]
        classified = set().union(*partitions)
        self.assertEqual(sum(map(len, partitions)), len(classified))
        self.assertEqual(classified, {path.stem for path in roles['sources']})
        self.assertEqual(len(classified), len(roles['sources']))

    def test_source_derived_receipt_is_admitted(self):
        result = check_receipt(source_receipt())
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertIn('M3_FILE_SHADOW_HOST_LINKED_IMAGE_PASS', result.stdout)

    def test_obsolete_source_census_is_refused(self):
        receipt = source_receipt()
        receipt['contract_inventory']['source_count'] = 19
        result = check_receipt(receipt)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('CONTRACT_ROLE_INVENTORY_MISMATCH', result.stdout)

    def test_missing_bounded_run_is_refused(self):
        for lane in ('host_module_contracts', 'image_module_contracts'):
            with self.subTest(lane=lane):
                receipt = source_receipt()
                del receipt[lane]['M3ChunkDeltaWorkspaceProbe:distance']
                result = check_receipt(receipt)
                self.assertNotEqual(result.returncode, 0)
                self.assertIn('EXACT_HOST_IMAGE_CENSUS_MISMATCH', result.stdout)

    def test_missing_current_change_map_proof_is_refused(self):
        receipt = source_receipt()
        del receipt['image_module_contracts']['M3FileChangeMapBinarySearchProof']
        self.assertNotEqual(check_receipt(receipt).returncode, 0)

    def test_false_success_and_duplicate_json_keys_are_refused(self):
        receipt = source_receipt()
        receipt['state'] = 'STARTED'
        self.assertNotEqual(check_receipt(receipt).returncode, 0)
        result = check_receipt(source_receipt(), duplicate_state=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('DUPLICATE_JSON_KEY', result.stdout)

    def test_maven_gate_cannot_be_silently_promoted(self):
        receipt = source_receipt()
        receipt['maven_fork_execution'] = 'PASSED'
        result = check_receipt(receipt)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('MAVEN_GATE_CHANGED', result.stdout)

    def test_all_contract_compilation_precedes_host_execution(self):
        tree = ast.parse((FILE / 'verify.py').read_text(encoding='utf-8'))
        calls = {node.args[0].value: node.lineno for node in ast.walk(tree)
                 if isinstance(node, ast.Call) and isinstance(node.func, ast.Name)
                 and node.func.id == 'run' and node.args
                 and isinstance(node.args[0], ast.Constant)
                 and isinstance(node.args[0].value, str)}
        for label in ('compile-module', 'compile-contract', 'compile-module-contracts'):
            self.assertLess(calls[label], calls['host-contract'])

    def test_complete_dependencies_follow_actual_module_edges(self):
        file_descriptor = (FILE / 'src/main/java/module-info.java').read_text(encoding='utf-8')
        collection_descriptor = (ROOT / 'src/jdk.m3.collections/share/classes/module-info.java').read_text(
            encoding='utf-8')
        self.assertIn('requires transitive jdk.m3.collections;', file_descriptor)
        self.assertIn('module jdk.m3.collections {', collection_descriptor)
        self.assertIn('exports jdk.m3.arrays;', collection_descriptor)
        self.assertIn('exports jdk.m3.arrays.packed;', collection_descriptor)
        builder = (FILE / 'compile-dependencies.sh').read_text(encoding='utf-8')
        self.assertIn('for module in jdk.m3.collections; do', builder)
        self.assertIn('find "$repo/src/$module/share/classes"', builder)
        self.assertNotIn('m3/$family/src/main/java', builder)
        self.assertIn('-print0', builder)
        self.assertIn('LC_ALL=C sort -z', builder)
        self.assertIn('mapfile -d', builder)
        self.assertIn('--release 21 -proc:none -Xlint:all,-module -Werror', builder)
        self.assertIn('--module-path "$out"', builder)

    def test_workflows_propagate_and_trigger_on_dependencies(self):
        for name in (MOVE, SHADOW, LAYERED):
            with self.subTest(workflow=name):
                text = workflow(name)
                self.assertEqual(text.count("'src/jdk.m3.collections/**'"), text.count("'m3/file/**'"))
                self.assertIn('bash m3/file/compile-dependencies.sh "$JAVA_HOME" "$deps"', text)
                self.assertIn('github.event.pull_request.head.sha || github.sha', text)
                self.assertIn('--module-path', text)
                self.assertIn('python3 -m py_compile m3/file/verify.py m3/file/test_workflow_wiring.py', text)
                self.assertIn('python3 -m unittest discover -s m3/file -p test_workflow_wiring.py -v', text)
        for name in (MOVE, SHADOW):
            self.assertIn('--module-path "$deps"', workflow(name))

    def test_verify_only_workflows_build_and_pass_dependencies(self):
        # org.m3jdk.file requires transitive jdk.m3.collections, so verify.py needs the built dependency module path.
        for name in (DELTA, IMPACT):
            with self.subTest(workflow=name):
                text = workflow(name)
                self.assertEqual(text.count("'src/jdk.m3.collections/**'"), text.count("'m3/file/**'"))
                self.assertIn('github.event.pull_request.head.sha || github.sha', text)
                self.assertIn('bash m3/file/compile-dependencies.sh "$JAVA_HOME" "$deps"', text)
                self.assertIn('python3 m3/file/verify.py --java-home "$JAVA_HOME" --module-path "$deps"', text)

    def test_layered_workflow_keeps_complete_host_and_image_contracts(self):
        text = workflow(LAYERED)
        self.assertIn("find m3/file/src/main/java -type f -name '*.java' -print0", text)
        self.assertIn('LC_ALL=C sort -z', text)
        self.assertIn('"${sources[@]}"', text)
        self.assertIn('"$out/classes:$deps"', text)
        self.assertIn('"$JAVA_HOME/jmods:$deps:$out/jmods"', text)
        self.assertEqual(text.count('-m org.m3jdk.file/jdk.m3.file.M3ShadowStreamsContract'), 4)
        self.assertEqual(text.count('-Xint'), 2)
        self.assertIn('--patch-module org.m3jdk.file="$out/module-tests"', workflow(SHADOW))
        self.assertIn('java -Xint', workflow(SHADOW))

    def test_dependency_builder_refuses_existing_output_before_tools(self):
        with tempfile.TemporaryDirectory(prefix='m3-file-dependency-test-') as directory:
            sentinel = Path(directory) / 'retain.txt'
            sentinel.write_text('unchanged', encoding='utf-8')
            result = subprocess.run(['bash', str(FILE / 'compile-dependencies.sh'),
                                     '/missing-jdk-for-refusal-test', directory],
                                    text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                    check=False, timeout=30)
            self.assertEqual(result.returncode, 2, result.stdout)
            self.assertIn('dependency output already exists', result.stdout)
            self.assertEqual(sentinel.read_text(encoding='utf-8'), 'unchanged')


if __name__ == '__main__':
    unittest.main()
