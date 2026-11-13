#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""The target workflow keeps its runtime tests and refuses private recipe execution."""
import ast
from pathlib import Path
import re
import subprocess
import tempfile
import unittest

import yaml

ROOT = Path(__file__).resolve().parents[2]
CHECKER = ROOT / 'm3/runtime-integration/check-m3string-invariants.py'
WORKFLOW = ROOT / '.github/workflows/mindex-string-backing.yml'
GUARD_WORKFLOW = ROOT / '.github/workflows/m3-string-replacement-guard-proof.yml'
INPUT_SEALS = ROOT / 'm3/runtime-integration/string-replacement-target-sha256.txt'
REGEX_VERIFIER = ROOT / 'm3/runtime-integration/regex-precompute/verify.py'


def workflow_yaml(text):
    # BaseLoader retains the GitHub event key "on" as text rather than the
    # YAML 1.1 boolean understood by PyYAML's default resolver.
    document = yaml.load(text, Loader=yaml.BaseLoader)
    if not isinstance(document, dict) or not isinstance(document.get('on'), dict):
        raise AssertionError('workflow event map missing')
    return document


def sealed_inputs():
    paths = set()
    for line in INPUT_SEALS.read_text(encoding='utf-8').splitlines():
        match = re.fullmatch(r'[0-9a-f]{64}  (.+)', line)
        if not match or match[1] in paths:
            raise AssertionError('malformed or duplicate target source seal')
        paths.add(match[1])
    if not paths:
        raise AssertionError('target source seals missing')
    return paths


def receipted_regex_inputs():
    tree = ast.parse(REGEX_VERIFIER.read_text(encoding='utf-8'))
    assignments = {target.id: node.value for node in tree.body if isinstance(node, ast.Assign)
                   for target in node.targets if isinstance(target, ast.Name)}
    test = ast.literal_eval(assignments['TEST'])
    source = assignments['SOURCE_INPUTS']
    if not isinstance(source, ast.Tuple):
        raise AssertionError('regex receipt source inputs must remain explicit')
    paths = {test if isinstance(node, ast.Name) and node.id == 'TEST'
             else ast.literal_eval(node) for node in source.elts}
    if not paths or not all(isinstance(path, str) for path in paths):
        raise AssertionError('regex receipt source inputs missing')
    return paths


def watched_paths(patterns, paths):
    """Use Git's rooted wildmatch, including ** and ordered ! exclusions.

    These workflows use path patterns shared by GitHub Actions and gitignore;
    fnmatch's '*' would incorrectly accept paths across directory boundaries.
    Only a temporary empty repository is created, and no source file is edited.
    """
    if not isinstance(patterns, list) or not patterns or not all(
            isinstance(pattern, str) and pattern for pattern in patterns):
        raise AssertionError('workflow path filters missing')
    with tempfile.TemporaryDirectory(prefix='m3-workflow-paths-') as temporary:
        directory = Path(temporary)
        subprocess.run(['git', 'init', '--quiet', '--template=', temporary], check=True,
                       stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        empty = directory / 'empty-global-excludes'
        empty.write_text('', encoding='utf-8')
        rooted = ['!/' + pattern[1:].lstrip('/') if pattern.startswith('!')
                  else '/' + pattern.lstrip('/') for pattern in patterns]
        (directory / '.gitignore').write_text('\n'.join(rooted) + '\n', encoding='utf-8')
        result = subprocess.run(
            ['git', '-C', temporary, '-c', 'core.excludesFile=' + str(empty),
             '-c', 'core.ignoreCase=false', 'check-ignore', '--no-index', '-z', '--stdin'],
            input='\0'.join(sorted(paths)) + '\0', text=True, capture_output=True)
        if result.returncode not in (0, 1):
            raise AssertionError('Git workflow path matching failed: ' + result.stderr)
        return set(result.stdout.rstrip('\0').split('\0')) if result.stdout else set()


def require_watched_inputs(document, event, paths):
    configuration = document['on'].get(event)
    if not isinstance(configuration, dict):
        raise AssertionError('workflow event missing: ' + event)
    missing = sorted(paths - watched_paths(configuration.get('paths'), paths))
    if missing:
        raise AssertionError('unwatched ' + event + ' inputs: ' + ', '.join(missing))


def actual_contract():
    tree = ast.parse(CHECKER.read_text(encoding='utf-8'), filename=str(CHECKER))
    names = {'check_string_jtreg_schedule', 'check_string_workflow_contract'}
    definitions = [node for node in tree.body
                   if isinstance(node, ast.FunctionDef) and node.name in names]
    if len(definitions) != len(names) or {node.name for node in definitions} != names:
        raise AssertionError('actual workflow contract missing or duplicated')
    calls = [node.value for node in tree.body if isinstance(node, ast.Expr)
             and isinstance(node.value, ast.Call) and isinstance(node.value.func, ast.Name)
             and node.value.func.id == 'check_string_workflow_contract']
    if (len(calls) != 1 or calls[0].keywords
            or [ast.dump(arg) for arg in calls[0].args]
            != [ast.dump(ast.Name(id='workflow', ctx=ast.Load()))]):
        raise AssertionError('actual workflow contract invocation changed')
    def fail(message):
        raise AssertionError(message)
    scope = {'re': re, 'fail': fail}
    exec(compile(ast.Module(body=definitions, type_ignores=[]), str(CHECKER), 'exec'), scope)
    return scope['check_string_workflow_contract']


class StringWorkflowContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.workflow = WORKFLOW.read_text(encoding='utf-8')
        cls.guard = GUARD_WORKFLOW.read_text(encoding='utf-8')

    def test_actual_workflow_retains_required_target_gates(self):
        actual_contract()(self.workflow)

    def test_lost_runtime_dag_test_refuses(self):
        path = 'test/jdk/java/lang/String/M3StringCanonicalDagTest.java'
        self.assertIn(path, self.workflow)
        with self.assertRaisesRegex(AssertionError, 'jtreg coverage lost'):
            actual_contract()(self.workflow.replace(path, ''))

    def test_lost_runtime_streams_test_refuses(self):
        path = 'test/jdk/java/lang/String/M3StringStreamsTest.java'
        self.assertIn(path, self.workflow)
        with self.assertRaisesRegex(AssertionError, 'jtreg coverage lost'):
            actual_contract()(self.workflow.replace(path, ''))

    def test_lost_position_owner_test_refuses(self):
        path = 'test/jdk/java/lang/String/M3StringPositionInvariant6Test.java'
        self.assertIn(path, self.workflow)
        with self.assertRaisesRegex(AssertionError, 'jtreg coverage lost'):
            actual_contract()(self.workflow.replace(path, ''))

    def test_private_recipe_execution_refuses(self):
        for recipe in ['M3StringCanonicalDagMasterRepairRecipeTest',
                       'M3StringDirectStreamTraversalRecipeTest',
                       'M3StringHistoryConvergenceRecipeTest']:
            with self.subTest(recipe=recipe):
                with self.assertRaisesRegex(AssertionError, 'private recipe execution'):
                    actual_contract()(self.workflow + '\n      - run: mvn -Dtest=' + recipe + ' test\n')

    def test_no_concatenated_jtreg_path_entries(self):
        bad = self.workflow + "\n      - 'test/jdk/java/lang/String/M3StringPrecomputeSearchTest.java test/jdk/java/lang/String/M3StringStreamsTest.java'\n"
        with self.assertRaisesRegex(AssertionError, 'concatenated path entries'):
            actual_contract()(bad)

    def test_guard_workflow_watches_all_sealed_inputs_for_each_event(self):
        for event in ('push', 'pull_request'):
            with self.subTest(event=event):
                require_watched_inputs(workflow_yaml(self.guard), event, sealed_inputs())

    def test_each_guard_event_refuses_an_unwatched_source(self):
        for event in ('push', 'pull_request'):
            with self.subTest(event=event):
                document = workflow_yaml(self.guard)
                document['on'][event]['paths'].remove(
                    'src/java.base/share/classes/java/lang/M3String*.java')
                other = 'pull_request' if event == 'push' else 'push'
                require_watched_inputs(document, other, sealed_inputs())
                with self.assertRaisesRegex(AssertionError, 'unwatched ' + event):
                    require_watched_inputs(document, event, sealed_inputs())

    def test_workflow_filters_use_rooted_git_wildmatch(self):
        paths = {'src/Owner.java', 'src/deep/Owner.java', 'src/private/Owner.java',
                 'unrelated/src/Owner.java'}
        self.assertEqual({'src/Owner.java'}, watched_paths(['src/*.java'], paths))
        self.assertEqual({'src/Owner.java', 'src/deep/Owner.java'},
                         watched_paths(['src/**/*.java', '!src/private/**'], paths))

    def test_regex_workflow_watches_every_receipted_source(self):
        require_watched_inputs(workflow_yaml(self.workflow), 'pull_request',
                               receipted_regex_inputs())

    def test_native_regex_watch_cannot_be_removed(self):
        document = workflow_yaml(self.workflow)
        native = 'src/java.base/share/native/libjava/M3StringRegexDp.c'
        self.assertIn(native, receipted_regex_inputs())
        document['on']['pull_request']['paths'].remove(native)
        with self.assertRaisesRegex(AssertionError, 'unwatched pull_request.*M3StringRegexDp'):
            require_watched_inputs(document, 'pull_request', receipted_regex_inputs())


if __name__ == '__main__':
    unittest.main(verbosity=2)
