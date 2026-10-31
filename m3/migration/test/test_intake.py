# SPDX-License-Identifier: Apache-2.0
"""Synthetic receipt-parser fixtures; no fixture below is an executed upstream proof.

Reuses the current records-shaped MigrationTest setup without inheriting its tests.
All success documents, logs, class hashes and XML are authored synthetic data. Their
only authority is to exercise review-only packet admission and negative controls.
"""
import contextlib
import copy
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import unittest


_spec = importlib.util.spec_from_file_location(
    '_intake_current_migration_fixture', Path(__file__).with_name('test_migration.py'))
_legacy = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(_legacy)
migration = _legacy.migration

SOURCE_COMMIT = 'd' * 40
# A real commit-shaped identity used only in synthetic parser data below. No
# upstream checkout, compiler, recipe or consumer execution is asserted by it.
FRESH_SOURCE_COMMIT = '99221b0f25a1b09c32855d76e295b816f632a46b'
TARGET_COMMIT = 'e' * 40
SOURCE_PATH = 'source/src/main/java/example/Owner.java'
RECIPE = 'com.synexia.rewrite.Pcr'
MAPPING_ID = 'synexia.intake.pcr.Owner'
SYNTHETIC = 'SYNTHETIC RECEIPT PARSER FIXTURE; NO UPSTREAM TOOL WAS EXECUTED\n'
STAGES = ('lint', 'compile', 'test', 'runtime')


def sha(data):
    return hashlib.sha256(data).hexdigest()


def git_object(kind, data):
    return hashlib.sha1(kind.encode('ascii') + b' ' + str(len(data)).encode('ascii')
                        + b'\0' + data).hexdigest()


def tree_id(rows):
    entries = []
    for row in rows:
        name = row['path'].encode('utf-8')
        sort_name = name + (b'/' if row['type'] == 'tree' else b'')
        value = row['mode'].lstrip('0').encode('ascii') + b' ' + name + b'\0' + bytes.fromhex(row['sha'])
        entries.append((sort_name, value))
    return git_object('tree', b''.join(value for _, value in sorted(entries)))


def replace_at(document, path, value):
    owner = document
    for key in path[:-1]:
        owner = owner[key]
    owner[path[-1]] = copy.deepcopy(value)


class IntakeTest(unittest.TestCase):
    def setUp(self):
        # Exact existing test-fixture owner, rather than the incompatible lineage entries form.
        _legacy.MigrationTest.setUp(self)
        self.source_commit = SOURCE_COMMIT
        self.historical = copy.deepcopy(self.doc)
        self.candidate_bytes = b'package example; public final class Owner { public int value() { return 37; } }\n'
        self.candidate_sha = sha(self.candidate_bytes)
        self.candidate_git = git_object('blob', self.candidate_bytes)
        self.candidate_artifact = 'portable/actual-output/Owner.java'
        self.put(self.candidate_artifact, self.candidate_bytes)
        self.output_seal = sha((SOURCE_PATH + '\t' + self.candidate_sha + '\n').encode())
        current = copy.deepcopy(self.record)
        current.update(id=MAPPING_ID, kind='pending', status='pending',
                       reason='Synthetic review intake only; no product adaptation.',
                       owner='example.Owner', targets=[], tests=[])
        current['sources'] = [{'repo': 'hsoliwal/com.synexia', 'commit': SOURCE_COMMIT,
                              'module': 'source', 'path': SOURCE_PATH, 'symbol': 'example.Owner',
                              'signatures': ['int value()'], 'sha256': self.candidate_sha,
                              'git_blob_sha1': self.candidate_git, 'fingerprint': None,
                              'revision_role': 'candidate'}]
        current['sync'].update(source_revision=SOURCE_COMMIT, target_revision=TARGET_COMMIT,
                               pending=['Downstream module and ABI gates have not run.'])
        current['contract']['differences'].append('Disposition: module-port-candidate')
        current['recipe']['id'] = RECIPE
        current['recipe']['preconditions'].append('Required downstream gates: java21-module,jni-abi-review')
        self.doc['migration']['records'].append(current)
        self.intake_record = copy.deepcopy(current)
        rows = [{'path': 'README.md', 'mode': '100644', 'type': 'blob',
                 'sha': git_object('blob', b'Synthetic source root\n')},
                {'path': 'source', 'mode': '040000', 'type': 'tree', 'sha': '3' * 40}]
        self.root_inventory = {'sha': tree_id(rows), 'tree': rows, 'truncated': False}
        self.recipe_resource = 'com/synexia/rewrite/hash-pinned-java/pcs-reconciled-v1/manifest.tsv'
        self.template_resource = self.recipe_resource.rsplit('/', 1)[0] + '/Owner.java.txt'
        self.recipe_manifest_bytes = (SOURCE_PATH + '\t' + sha(b'synthetic prior source')
                                      + '\t' + self.candidate_sha + '\tOwner.java.txt\n').encode()
        manifest_ref = self.put('portable/recipe/materialization/manifest.tsv', self.recipe_manifest_bytes)
        self.manifest = {'sourceCommit': SOURCE_COMMIT, 'recipe': RECIPE,
                         'files': [{'path': SOURCE_PATH, 'sha256': self.candidate_sha,
                                    'gitBlob': self.candidate_git, 'artifact': self.candidate_artifact}],
                         'materialization': {'manifest': manifest_ref, 'output': None, 'patch': None}}
        self.packet = {
            'schema': 'm3.synexia-intake/1',
            'source': {'repo': 'hsoliwal/com.synexia', 'commit': SOURCE_COMMIT,
                       'tree': self.root_inventory['sha']},
            'target': {'repo': 'hsoliwal/M3jdk21', 'commit': TARGET_COMMIT, 'tree': 'f' * 40},
            'authority': {}, 'sourceRoot': {},
            'owners': [{'path': 'README.md', 'disposition': 'custody-only',
                        'reason': 'Synthetic top-level documentation.'},
                       {'path': 'source', 'disposition': 'module-port-candidate',
                        'reason': 'One synthetic candidate; downstream gates remain open.'}],
            'lanes': [{'id': 'pcr', 'recipe': RECIPE, 'manifest': {},
                       'outputSeal': self.output_seal, 'proofs': [],
                       'mappings': [{'path': SOURCE_PATH, 'id': MAPPING_ID,
                                     'disposition': 'module-port-candidate',
                                     'requiredGates': ['java21-module', 'jni-abi-review']}]}]}
        self.bind_authority()
        self.bind_root()
        self.bind_manifest()
        self.proofs = {role: self.proof_model(role) for role in ('recipe', 'consumer')}
        self.seal_bundle()

    def put(self, relative, content):
        path = self.root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content)
        return {'path': relative, 'sha256': sha(content)}

    def put_json(self, relative, document):
        return self.put(relative, (json.dumps(document, indent=2) + '\n').encode('utf-8'))

    def bind_authority(self):
        self.packet['authority'] = self.put_json('m3/docs/name-mapping.json', self.doc)

    def bind_root(self):
        self.packet['sourceRoot'] = self.put_json('portable/current-root.json', self.root_inventory)

    def bind_manifest(self):
        self.packet['lanes'][0]['manifest'] = self.put_json('portable/pcr-outputs.json', self.manifest)

    def proof_model(self, role):
        historical = '/synthetic/proof/' + role
        runtime = ['/synthetic/jdk/bin/java', '-ea',
                   'com.synexia.rewrite.PcrTest' if role == 'recipe' else 'example.SyntheticConsumer']
        if role == 'recipe':
            runtime.insert(2, '-Dpcr.output=' + historical + '/generated-candidate')
        commands = {'lane': 'synthetic-' + role, 'workspace': '/synthetic/workspace', 'parents': [],
                    'python': '/synthetic/python', 'javaHome': '/synthetic/jdk',
                    'inputs': historical + '/inputs.json', 'target': historical + '/target',
                    'artifacts': historical + '/build-artifacts.json',
                    'requiredClassFiles': ['classes/example/SyntheticIntake.class'],
                    'expectedJUnit': {'example.SyntheticIntake': 1},
                    'runtimePatterns': {'1': ['^PCR_VERIFIED ' if role == 'recipe'
                                               else '^SYNTHETIC_CONSUMER_OK$']},
                    'commands': {stage: [['/synthetic/tool', stage]] for stage in STAGES}}
        commands['commands']['runtime'] = [runtime]
        commands['commands']['compile'][0].append('-Dproof.sources=' + historical + '/sources')
        prefix = 'synexia-openrewrite-recipes/src/main/resources/'
        staged = ([{'path': prefix + resource, 'sha256': sha(content),
                    'gitBlob': git_object('blob', content), 'bytes': len(content)}
                   for resource, content in [(self.recipe_resource, self.recipe_manifest_bytes),
                                             (self.template_resource, self.candidate_bytes)]]
                  if role == 'recipe' else [{'path': SOURCE_PATH, 'sha256': self.candidate_sha,
                                             'gitBlob': self.candidate_git, 'bytes': len(self.candidate_bytes)}])
        staged.sort(key=lambda row: row['path'])
        xml = ('<testsuite name="example.SyntheticIntake" tests="1" failures="0" errors="0" skipped="0">'
               '<testcase name="syntheticReceiptParserFixture" classname="example.SyntheticIntake"/>'
               '</testsuite>\n').encode()
        counts = {'tests': 1, 'failures': 0, 'errors': 0, 'skipped': 0}
        synthetic_class_bytes = b'synthetic class identity, not a compiled class'
        marker = ('PCR_VERIFIED targets=1 changes=1 refusals=1 postScanRefusals=1 candidateFiles=1 outputSeal=' + self.output_seal + '\n'
                  if role == 'recipe' else 'SYNTHETIC_CONSUMER_OK\n')
        return {'commands': commands, 'bindRecipe': role == 'consumer',
                'inputs': [{'path': historical + '/sources/' + SOURCE_PATH,
                            'sha256': self.candidate_sha, 'bytes': len(self.candidate_bytes)}],
                'staging': {'baseline': self.source_commit, 'files': staged},
                'tooling': {'fixture': SYNTHETIC.strip(), 'canonicalOfflineSourceClosure': False},
                'artifacts': [{'path': 'classes/example/SyntheticIntake.class',
                               'sha256': sha(synthetic_class_bytes), 'bytes': len(synthetic_class_bytes)}]
                             + ([{'path': 'classes/' + resource, 'sha256': sha(content), 'bytes': len(content)}
                                 for resource, content in [(self.recipe_resource, self.recipe_manifest_bytes),
                                                           (self.template_resource, self.candidate_bytes)]]
                                if role == 'recipe' else []),
                'xml': xml,
                'junit': {'status': 'ACTUAL_SUREFIRE_REPORTS', 'totals': counts,
                          'suites': {'example.SyntheticIntake': {
                              'counts': copy.deepcopy(counts), 'report': 'TEST-example.SyntheticIntake.xml',
                              'sha256': sha(xml)}}},
                'stdout': SYNTHETIC + marker, 'stderr': '',
                'patchOverrides': {}, 'resultOverrides': {}, 'runtimeOverrides': {},
                'runtimeReportOverrides': {}, 'receiptOverrides': {}, 'gateOverrides': {}}

    def seal_proof(self, role):
        """Reseal synthetic envelopes so semantic negatives cannot fail only on stale hashes."""
        model = self.proofs[role]
        base = 'portable/' + role + '/'
        refs = self._seal_input_documents(role, base, model)
        self.put(base + 'target/surefire-reports/TEST-example.SyntheticIntake.xml', model['xml'])
        patch = {'status': 'SEALED_BEFORE_LINT', 'baseline': self.source_commit,
                 'lane': 'synthetic-' + role, 'specSHA256': sha(b'synthetic specification'),
                 'inputManifestSHA256': refs['inputs.json'], 'commandsSHA256': refs['commands.json'],
                 'stagingSHA256': refs['staging.json'], 'toolingSHA256': refs['tooling.json'],
                 'sourceFiles': len(model['staging']['files']),
                 'javaFiles': sum(row['path'].endswith('.java') for row in model['staging']['files']),
                 'lintJavaFiles': sum(row['path'].endswith('.java') for row in model['staging']['files']),
                 'effectiveOutputAbsent': True, 'strictCanonicalAdmission': False,
                 'canonicalProductionApplied': False}
        patch.update(model['patchOverrides'])
        patch_ref = self.put_json(base + 'PATCH.json', patch)
        gates = []
        prior = []
        for index, stage in enumerate(STAGES, 1):
            stem = f'{index:02d}-{stage}'
            out = self.put(base + 'chain/' + stem + '.stdout.log', (SYNTHETIC + stage + '\n').encode())
            err = self.put(base + 'chain/' + stem + '.stderr.log', b'')
            historical = str(Path(model['commands']['inputs']).parent)
            command = [model['commands']['python'], historical + '/harness/execute.py',
                       '--config', historical + '/commands.json', '--stage', stage]
            receipt = {'stage': stage, 'command': command, 'cwd': '/synthetic/workspace',
                       'JAVA_HOME': '/synthetic/jdk', 'source_manifest_sha256': refs['inputs.json'],
                       'prior_receipts': list(prior), 'exit_code': 0,
                       'stdout_sha256': out['sha256'], 'stderr_sha256': err['sha256']}
            receipt.update(model['receiptOverrides'].get(stage, {}))
            receipt_ref = self.put_json(base + 'chain/' + stem + '.json', receipt)
            minute = 0 if role == 'recipe' else 1
            gate = {'stage': stage, 'exitCode': 0,
                    'startedAt': f'2000-01-01T00:{minute:02d}:{(index - 1) * 2:02d}+00:00',
                    'endedAt': f'2000-01-01T00:{minute:02d}:{(index - 1) * 2 + 1:02d}+00:00',
                    'receipt': 'chain/' + stem + '.json', 'receiptSHA256': receipt_ref['sha256'],
                    'stdoutSHA256': out['sha256'], 'stderrSHA256': err['sha256']}
            gate.update(model['gateOverrides'].get(stage, {}))
            gates.append(gate)
            prior.append(stem + '.json')
        out = self.put(base + 'runtime-1.stdout.log', model['stdout'].encode())
        err = self.put(base + 'runtime-1.stderr.log', model['stderr'].encode())
        command = {'commandIndex': 1, 'command': model['commands']['commands']['runtime'][0],
                   'exitCode': 0, 'startedAt': f'2000-01-01T00:{minute:02d}:06+00:00',
                   'endedAt': f'2000-01-01T00:{minute:02d}:07+00:00',
                   'stdout': 'runtime-1.stdout.log', 'stdoutSHA256': out['sha256'],
                   'stderr': 'runtime-1.stderr.log', 'stderrSHA256': err['sha256']}
        command.update(model['runtimeOverrides'])
        self.put_json(base + 'runtime-1.json', command)
        runtime = {'status': 'ACTUAL_STANDALONE_EXECUTIONS', 'commands': [command],
                   'strictCanonicalAdmission': False}
        runtime.update(model['runtimeReportOverrides'])
        runtime_ref = self.put_json(base + 'runtime-report.json', runtime)
        result = {'status': 'PASS_FOCUSED_TEST_PROJECT', 'chain': 'synthetic-' + role,
                  'baseline': self.source_commit, 'orderedGates': gates,
                  'patchSHA256': patch_ref['sha256'], 'inputManifestSHA256': refs['inputs.json'],
                  'compiledArtifactManifestSHA256': refs['build-artifacts.json'],
                  'junit': model['junit']['totals'], 'junitReportSHA256': refs['junit-report.json'],
                  'runtimeReportSHA256': runtime_ref['sha256'], 'runtimeCommands': 1,
                  'strictCanonicalAdmission': False, 'canonicalProductionApplied': False, 'sourceSlice': True}
        result.update(model['resultOverrides'])
        return self.put_json(base + 'RESULT.json', result)

    def seal_bundle(self):
        recipe = self.seal_proof('recipe')
        consumer = self.proofs['consumer']
        if consumer['bindRecipe']:
            consumer['commands']['parents'] = [{'resultSHA256': recipe['sha256'],
                                                'chain': 'synthetic-recipe'}]
        consumer_ref = self.seal_proof('consumer')
        self.packet['lanes'][0]['proofs'] = [{'role': 'recipe', 'result': recipe},
                                           {'role': 'consumer', 'result': consumer_ref}]

    def snapshot(self):
        return {str(path.relative_to(self.root)): ('link', str(path.readlink())) if path.is_symlink()
                else ('file', sha(path.read_bytes())) for path in self.root.rglob('*')
                if path.is_file() or path.is_symlink()}

    def admit(self, packet=None):
        return migration._admit_intake(self.doc, self.packet if packet is None else packet,
                                       self.root, sha((self.root / 'm3/docs/name-mapping.json').read_bytes()))

    def refuse(self, packet=None):
        before = self.snapshot()
        with self.assertRaises((ValueError, TypeError, KeyError)):
            self.admit(packet)
        self.assertEqual(before, self.snapshot(), 'refusal may not alter authority, proof or product files')

    def cli(self, extra=(), packet_bytes=None, expected=None):
        path = self.root / 'packet.json'
        payload = (json.dumps(self.packet, indent=2) + '\n').encode() if packet_bytes is None else packet_bytes
        path.write_bytes(payload)
        args = ['intake', str(self.root), '--manifest', str(self.root / 'm3/docs/name-mapping.json'),
                '--intake-packet', str(path), '--expected-sha256', sha(payload) if expected is None else expected]
        stdout, stderr = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(stdout), contextlib.redirect_stderr(stderr):
            code = migration.main(args + list(extra))
        return code, stdout.getvalue(), stderr.getvalue()

    def test_synthetic_review_receipt_is_repeatable_and_never_promotes(self):
        self.assertEqual([], migration.validate(self.doc, self.root))
        before = self.snapshot()
        receipt = self.admit()
        self.assertEqual(receipt, self.admit())
        self.assertEqual(before, self.snapshot())
        self.assertEqual('m3.synexia-intake-receipt/1', receipt['schema'])
        self.assertEqual('RECEIVED_FOR_REVIEW', receipt['state'])
        self.assertEqual((2, 1, 0), (receipt['rootOwners'], receipt['outputs'], receipt['productWrites']))
        for flag in ('wholeSourceTreeCovered', 'dependencyClosureComplete',
                     'originalToolchainCustodyRevalidated', 'runtimeAccepted'):
            self.assertIs(receipt[flag], False, flag)
        self.assertEqual([MAPPING_ID], receipt['lanes'][0]['mappingIds'])
        self.assertEqual(self.output_seal, receipt['lanes'][0]['outputSeal'])
        self.assertEqual(self.historical['migration']['records'][0], self.doc['migration']['records'][0])
        for key in ('source', 'target', 'coverage', 'gates'):
            self.assertEqual(self.historical['migration'][key], self.doc['migration'][key])
        self.assertTrue(migration.completion_errors(self.doc))

    def test_cli_sealed_packet_output_and_exact_replay(self):
        code, output, errors = self.cli()
        self.assertEqual((0, ''), (code, errors))
        receipt = json.loads(output)
        self.assertEqual(sha((self.root / 'packet.json').read_bytes()), receipt['packetSHA256'])
        self.assertIs(receipt['runtimeAccepted'], False)
        destination = self.root / 'review/intake.json'
        code, _, errors = self.cli(['--output', str(destination)])
        self.assertEqual((0, ''), (code, errors))
        exact = destination.read_bytes()
        self.assertEqual(receipt, json.loads(exact))
        self.assertEqual(0, self.cli(['--output', str(destination)])[0])
        self.assertEqual(exact, destination.read_bytes())
        self.assertEqual(b'candidate\n', self.output.read_bytes(), 'historical product bytes stay intact')

    def test_cli_refuses_modified_review_output(self):
        destination = self.root / 'review.json'
        destination.write_bytes(b'prior user review\n')
        code, _, errors = self.cli(['--output', str(destination)])
        self.assertEqual(2, code)
        self.assertIn('refusing modified output', errors)
        self.assertEqual(b'prior user review\n', destination.read_bytes())

    def test_cli_refuses_packet_seal_drift_and_duplicate_json_without_output(self):
        destination = self.root / 'must-not-exist.json'
        code, _, errors = self.cli(['--output', str(destination)], expected='0' * 64)
        self.assertEqual(2, code)
        self.assertIn('packet seal drift', errors)
        self.assertFalse(destination.exists())
        duplicate = b'{"schema":"m3.synexia-intake/1","schema":"m3.synexia-intake/1"}'
        code, _, errors = self.cli(['--output', str(destination)], packet_bytes=duplicate)
        self.assertEqual(2, code)
        self.assertIn('duplicate JSON key', errors)
        self.assertFalse(destination.exists())

    def test_packet_identity_and_authority_mismatch(self):
        cases = [(('schema',), 'other/1'), (('source', 'repo'), 'other/source'),
                 (('source', 'commit'), '1' * 40), (('source', 'tree'), '1' * 40),
                 (('target', 'repo'), 'other/target'), (('target', 'commit'), '1' * 40),
                 (('target', 'tree'), 'not-a-pin'),
                 (('authority', 'path'), 'm3/other-authority.json'),
                 (('authority', 'sha256'), '0' * 64)]
        for path, value in cases:
            with self.subTest(path=path):
                packet = copy.deepcopy(self.packet)
                replace_at(packet, path, value)
                self.refuse(packet)

    def test_missing_duplicate_and_unsafe_output_mapping(self):
        original = self.packet['lanes'][0]['mappings']
        variants = [[], original + original]
        for field, value in [('id', 'unknown'), ('path', '../escape'), ('disposition', 'custody-only'),
                             ('disposition', 'blocked'), ('requiredGates', ['other-gate']),
                             ('requiredGates', []), ('requiredGates', ['z', 'a']),
                             ('requiredGates', ['same', 'same'])]:
            rows = copy.deepcopy(original)
            rows[0][field] = value
            variants.append(rows)
        for rows in variants:
            with self.subTest(rows=rows):
                packet = copy.deepcopy(self.packet)
                packet['lanes'][0]['mappings'] = rows
                self.refuse(packet)

    def test_resealed_canonical_record_must_match_every_output_identity(self):
        original = copy.deepcopy(self.doc)
        prefix = ('migration', 'records', 1)
        cases = [(prefix + ('status',), 'implemented-unverified'),
                 (prefix + ('sync', 'source_revision'), '1' * 40),
                 (prefix + ('sync', 'target_revision'), '1' * 40),
                 (prefix + ('recipe', 'id'), 'other.Recipe'),
                 (prefix + ('recipe', 'preconditions'), ['source pin']),
                 (prefix + ('contract', 'differences'), ['new package'])]
        for field, value in [('repo', 'other/source'), ('commit', '1' * 40),
                             ('path', 'source/src/Other.java'), ('sha256', '0' * 64),
                             ('git_blob_sha1', '0' * 40), ('revision_role', 'pinned')]:
            cases.append((prefix + ('sources', 0, field), value))
        for path, value in cases:
            with self.subTest(path=path):
                self.doc = copy.deepcopy(original)
                replace_at(self.doc, path, value)
                self.bind_authority()
                self.refuse()
        self.doc = copy.deepcopy(original)
        self.doc['migration']['records'][1]['sources'] *= 2
        self.bind_authority()
        self.refuse()

    def test_missing_duplicate_and_legacy_authority_records(self):
        original = copy.deepcopy(self.doc)
        for variant in ('missing', 'duplicate', 'legacy-shape'):
            with self.subTest(variant=variant):
                self.doc = copy.deepcopy(original)
                records = self.doc['migration']['records']
                if variant == 'missing':
                    records.pop()
                elif variant == 'duplicate':
                    records.append(copy.deepcopy(records[-1]))
                else:
                    self.doc['migration'] = {'schema': 'm3.migration/1', 'entries': records}
                self.bind_authority()
                self.refuse()

    def test_top_level_owner_coverage_is_exact_and_explicit(self):
        owners = self.packet['owners']
        variants = [owners[:-1], owners + [owners[-1]], list(reversed(owners))]
        for field, value in [('path', 'unknown'), ('disposition', 'accepted'), ('reason', '')]:
            rows = copy.deepcopy(owners)
            rows[1][field] = value
            variants.append(rows)
        for rows in variants:
            with self.subTest(rows=rows):
                packet = copy.deepcopy(self.packet)
                packet['owners'] = rows
                self.refuse(packet)

    def test_resealed_root_tree_is_recomputed_and_incomplete_or_invalid_roots_refuse(self):
        original = copy.deepcopy(self.root_inventory)
        for field, value in [('truncated', True), ('sha', '0' * 40)]:
            with self.subTest(field=field):
                self.root_inventory = copy.deepcopy(original)
                self.root_inventory[field] = value
                self.bind_root()
                self.refuse()
        for field, value in [('sha', '1' * 40), ('mode', '100600'), ('type', 'commit'),
                             ('path', 'source/nested')]:
            with self.subTest(row_field=field):
                self.root_inventory = copy.deepcopy(original)
                self.root_inventory['tree'][1][field] = value
                self.bind_root()
                self.refuse()
        self.root_inventory = copy.deepcopy(original)
        self.root_inventory['tree'].append(copy.deepcopy(original['tree'][1]))
        self.root_inventory['sha'] = tree_id(self.root_inventory['tree'])
        self.packet['source']['tree'] = self.root_inventory['sha']
        self.bind_root()
        self.refuse()

    def test_output_manifest_scope_source_recipe_and_git_blob_identity(self):
        original = copy.deepcopy(self.manifest)
        variants = [(('sourceCommit',), '1' * 40), (('recipe',), 'other.Recipe'),
                    (('files',), []), (('files',), original['files'] * 2),
                    (('files', 0, 'gitBlob'), '0' * 40),
                    (('files', 0, 'path'), 'unclassified/src/Owner.java')]
        for path, value in variants:
            with self.subTest(path=path, value=value):
                self.manifest = copy.deepcopy(original)
                replace_at(self.manifest, path, value)
                self.bind_manifest()
                self.refuse()

    def test_output_content_and_seal_drift(self):
        artifact = self.root / self.candidate_artifact
        artifact.write_bytes(self.candidate_bytes + b'// drift\n')
        self.refuse()
        artifact.write_bytes(self.candidate_bytes)
        packet = copy.deepcopy(self.packet)
        packet['lanes'][0]['outputSeal'] = '0' * 64
        self.refuse(packet)

    def test_all_input_paths_refuse_aliases_and_escape(self):
        for path in ('../outside', '/absolute', 'portable/../actual-output/Owner.java',
                     'portable//actual-output/Owner.java', 'portable/./actual-output/Owner.java',
                     'portable\\actual-output\\Owner.java', 'C:/outside', 'bad\npath'):
            with self.subTest(path=path):
                original = self.manifest['files'][0]['artifact']
                self.manifest['files'][0]['artifact'] = path
                self.bind_manifest()
                self.refuse()
                self.manifest['files'][0]['artifact'] = original

    def test_artifact_symlink_and_parent_symlink_refuse(self):
        artifact = self.root / self.candidate_artifact
        backing = self.root / 'same-bytes.java'
        backing.write_bytes(artifact.read_bytes())
        artifact.unlink()
        artifact.symlink_to(backing)
        self.refuse()
        artifact.unlink()
        artifact.write_bytes(self.candidate_bytes)
        parent = artifact.parent
        moved = parent.with_name('actual-output-moved')
        parent.rename(moved)
        parent.symlink_to(moved, target_is_directory=True)
        self.refuse()

    def test_each_required_proof_artifact_must_exist_and_match_its_seal(self):
        names = ['RESULT.json', 'PATCH.json', 'inputs.json', 'commands.json', 'staging.json',
                 'tooling.json', 'build-artifacts.json', 'junit-report.json', 'runtime-report.json',
                 'chain/01-lint.json', 'chain/04-runtime.stdout.log', 'runtime-1.json',
                 'runtime-1.stdout.log', 'runtime-1.stderr.log',
                 'target/surefire-reports/TEST-example.SyntheticIntake.xml']
        for name in names:
            with self.subTest(name=name):
                path = self.root / 'portable/recipe' / name
                original = path.read_bytes()
                path.unlink()
                self.refuse()
                path.write_bytes(original + b' ')
                self.refuse()
                path.write_bytes(original)

    def test_resealed_failed_or_unexecuted_proofs_are_not_success(self):
        original = copy.deepcopy(self.proofs)
        cases = [('resultOverrides', 'status', 'STOPPED'),
                 ('resultOverrides', 'baseline', '1' * 40),
                 ('resultOverrides', 'strictCanonicalAdmission', True),
                 ('resultOverrides', 'canonicalProductionApplied', True),
                 ('resultOverrides', 'sourceSlice', False),
                 ('patchOverrides', 'status', 'PLANNED'),
                 ('patchOverrides', 'effectiveOutputAbsent', False),
                 ('runtimeOverrides', 'exitCode', 1), ('runtimeOverrides', 'exitCode', True),
                 ('runtimeOverrides', 'commandIndex', True),
                 ('resultOverrides', 'runtimeCommands', True),
                 ('runtimeReportOverrides', 'status', 'PLANNED')]
        for section, field, value in cases:
            with self.subTest(section=section, field=field):
                self.proofs = copy.deepcopy(original)
                self.proofs['recipe'][section][field] = value
                self.seal_bundle()
                self.refuse()
        self.proofs = copy.deepcopy(original)
        self.seal_bundle()
        self.put_json('portable/recipe/STOPPED.json', {'status': 'STOPPED'})
        self.refuse()

    def test_resealed_gate_order_exit_command_and_lineage_contradictions(self):
        original = copy.deepcopy(self.proofs)
        cases = [('gateOverrides', 'test', {'exitCode': 1}),
                 ('gateOverrides', 'test', {'exitCode': True}),
                 ('receiptOverrides', 'test', {'exit_code': 1}),
                 ('receiptOverrides', 'test', {'prior_receipts': []}),
                 ('receiptOverrides', 'compile', {'source_manifest_sha256': '0' * 64}),
                 ('receiptOverrides', 'runtime', {'command': ['/synthetic/unrelated', '--stage', 'runtime']})]
        for section, stage, value in cases:
            with self.subTest(section=section, stage=stage, value=value):
                self.proofs = copy.deepcopy(original)
                self.proofs['recipe'][section][stage] = value
                self.seal_bundle()
                self.refuse()
        for gates in ([], [{'stage': stage} for stage in reversed(STAGES)]):
            self.proofs = copy.deepcopy(original)
            self.proofs['recipe']['resultOverrides']['orderedGates'] = gates
            self.seal_bundle()
            self.refuse()

    def test_resealed_runtime_argv_must_match_the_sealed_command_plan(self):
        self.proofs['recipe']['runtimeOverrides']['command'] = ['/synthetic/jdk/bin/java', 'OtherTest']
        self.seal_bundle()
        self.refuse()

    def test_resealed_empty_or_missing_required_compilation_outputs_refuse(self):
        original = copy.deepcopy(self.proofs)
        for rows in ([], [{'path': 'classes/example/Other.class', 'sha256': '0' * 64, 'bytes': 1}],
                     [{'path': 'classes/example/SyntheticIntake.class', 'sha256': '0' * 64, 'bytes': 0}]):
            with self.subTest(rows=rows):
                self.proofs = copy.deepcopy(original)
                self.proofs['recipe']['artifacts'] = rows
                self.seal_bundle()
                self.refuse()

    def test_resealed_junit_counts_suites_and_xml_must_agree(self):
        original = copy.deepcopy(self.proofs)
        mutations = [(('junit', 'suites'), {}),
                     (('junit', 'totals', 'tests'), 0),
                     (('junit', 'totals', 'tests'), True),
                     (('junit', 'totals', 'failures'), 1),
                     (('junit', 'suites', 'example.SyntheticIntake', 'counts', 'tests'), True),
                     (('junit', 'suites', 'example.SyntheticIntake', 'counts', 'tests'), 2)]
        for path, value in mutations:
            with self.subTest(path=path):
                self.proofs = copy.deepcopy(original)
                replace_at(self.proofs['recipe'], path, value)
                self.seal_bundle()
                self.refuse()
        self.proofs = copy.deepcopy(original)
        model = self.proofs['recipe']
        model['xml'] = model['xml'].replace(b'failures="0"', b'failures="1"')
        model['junit']['suites']['example.SyntheticIntake']['sha256'] = sha(model['xml'])
        self.seal_bundle()
        self.refuse()

    def test_cli_resealed_malformed_junit_xml_refuses_without_receipt(self):
        model = self.proofs['recipe']
        model['xml'] = b'<testsuite name="example.SyntheticIntake"'
        model['junit']['suites']['example.SyntheticIntake']['sha256'] = sha(model['xml'])
        self.seal_bundle()
        destination = self.root / 'must-not-exist.json'
        code, _, errors = self.cli(['--output', str(destination)])
        self.assertEqual(2, code)
        self.assertIn('MIGRATION_REFUSED:', errors)
        self.assertFalse(destination.exists())

    def test_materialization_marker_must_be_unique_unambiguous_and_exact(self):
        correct = next(line for line in self.proofs['recipe']['stdout'].splitlines()
                       if line.startswith('PCR_VERIFIED ')) + '\n'
        for marker in ('', correct.replace('targets=1', 'targets=2'),
                       correct.replace('candidateFiles=1', 'candidateFiles=0'),
                       correct.replace(self.output_seal, '0' * 64), correct + correct,
                       correct.replace('outputSeal=', 'outputSeal=' + '0' * 64 + ' outputSeal=')):
            with self.subTest(marker=marker):
                self.proofs['recipe']['stdout'] = SYNTHETIC + marker
                self.seal_bundle()
                self.refuse()

    def test_recipe_consumer_roles_and_proof_identity_are_required(self):
        proofs = self.packet['lanes'][0]['proofs']
        variants = [proofs[:1], proofs[1:], [proofs[0], proofs[0]],
                    [dict(proofs[0], role='consumer'), proofs[1]],
                    [proofs[0], dict(proofs[1], role='recipe')]]
        for rows in variants:
            with self.subTest(rows=rows):
                packet = copy.deepcopy(self.packet)
                packet['lanes'][0]['proofs'] = rows
                self.refuse(packet)

    def test_resealed_consumer_must_bind_this_recipe_result(self):
        self.proofs['consumer']['bindRecipe'] = False
        for parents in ([], [{'resultSHA256': '0' * 64}]):
            with self.subTest(parents=parents):
                self.proofs['consumer']['commands']['parents'] = parents
                self.seal_bundle()
                self.refuse()

    def test_resealed_consumer_staging_must_cover_every_exact_output(self):
        original = copy.deepcopy(self.proofs)
        current = self.proofs['consumer']['staging']['files'][0]
        variants = [[], [current, current]]
        for field, value in [('path', 'source/src/main/java/example/Stale.java'),
                             ('sha256', '0' * 64), ('gitBlob', '0' * 40)]:
            variants.append([dict(current, **{field: value})])
        for rows in variants:
            with self.subTest(rows=rows):
                self.proofs = copy.deepcopy(original)
                self.proofs['consumer']['staging']['files'] = rows
                self.seal_bundle()
                self.refuse()

    def test_duplicate_lanes_and_reused_output_paths_refuse(self):
        packet = copy.deepcopy(self.packet)
        packet['lanes'].append(copy.deepcopy(packet['lanes'][0]))
        self.refuse(packet)
        packet['lanes'][1]['id'] = 'different-name-same-output'
        self.refuse(packet)

    def test_existing_validate_and_reconcile_commands_keep_review_semantics(self):
        stdout, stderr = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(stdout), contextlib.redirect_stderr(stderr):
            code = migration.main(['validate', str(self.root)])
        self.assertEqual((0, ''), (code, stderr.getvalue()))
        self.assertIn('completion=INCOMPLETE', stdout.getvalue())
        inventory = self.root / 'source-inventory.tsv'
        inventory.write_text('path\tsha256\n' + SOURCE_PATH + '\t' + self.candidate_sha + '\n')
        receipt = self.put_json('source-inventory-receipt.json', {
            'repo': 'hsoliwal/com.synexia', 'commit': SOURCE_COMMIT,
            'inventory_sha256': sha(inventory.read_bytes()), 'scope_complete': False,
            'producer': 'com.synexia.m3.inventory.InventoryWriter'})
        stdout, stderr = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(stdout), contextlib.redirect_stderr(stderr):
            code = migration.main(['reconcile', str(self.root), '--inventory', str(inventory),
                                   '--inventory-receipt', str(self.root / receipt['path'])])
        self.assertEqual((0, ''), (code, stderr.getvalue()))
        report = json.loads(stdout.getvalue())
        self.assertIs(report['semantic_equivalence_proved'], False)
        current = next(row for row in report['records'] if row['id'] == MAPPING_ID)
        self.assertEqual('unchanged', current['decision'])
        self.assertIs(current['automatic_replay_allowed'], False)
        self.assertEqual(self.historical['migration']['records'][0], self.doc['migration']['records'][0])


    def _seal_input_documents(self, role, base, model):
        """Seal metadata first, then its exact own-source/metadata input inventory."""
        historical = str(Path(model['commands']['inputs']).parent)
        refs = {}
        for name, key in [('commands.json', 'commands'), ('staging.json', 'staging'),
                          ('tooling.json', 'tooling'), ('build-artifacts.json', 'artifacts'),
                          ('junit-report.json', 'junit')]:
            refs[name] = self.put_json(base + name, model[key])['sha256']
        inputs = [{'path': historical + '/sources/' + row['path'],
                   'sha256': row['sha256'], 'bytes': row['bytes']}
                  for row in model['staging']['files']]
        for name in ('commands.json', 'staging.json'):
            content = (self.root / (base + name)).read_bytes()
            inputs.append({'path': historical + '/' + name,
                           'sha256': sha(content), 'bytes': len(content)})
        inputs.sort(key=lambda row: Path(row['path']))
        # Malformed/stale inventories are deliberate negative inputs; never repair
        # an override while resealing its outer envelopes.
        if 'inputRowsOverride' in model:
            inputs = copy.deepcopy(model['inputRowsOverride'])
        model['lastSealedInputs'] = copy.deepcopy(inputs)
        refs['inputs.json'] = self.put_json(base + 'inputs.json', inputs)['sha256']
        return refs


    def test_v2_resealed_staging_cannot_bypass_unchanged_input_and_gate_seals(self):
        model = self.proofs['consumer']
        previous_inputs = (self.root / 'portable/consumer/inputs.json').read_bytes()
        previous_gates = {
            stage: (self.root / f'portable/consumer/chain/{i:02d}-{stage}.json').read_bytes()
            for i, stage in enumerate(STAGES, 1)}
        model['inputRowsOverride'] = copy.deepcopy(model['lastSealedInputs'])
        # Coverage tuples are unchanged: the original validator ignored bytes and
        # did not inspect the old staging metadata entry inside inputs.json.
        model['staging']['files'][0]['bytes'] += 1
        self.seal_bundle()
        self.assertEqual(previous_inputs, (self.root / 'portable/consumer/inputs.json').read_bytes())
        for i, stage in enumerate(STAGES, 1):
            self.assertEqual(previous_gates[stage],
                             (self.root / f'portable/consumer/chain/{i:02d}-{stage}.json').read_bytes())
        self.refuse()


    def test_v2_resealed_commands_cannot_bypass_their_input_manifest_entry(self):
        model = self.proofs['consumer']
        previous_inputs = (self.root / 'portable/consumer/inputs.json').read_bytes()
        model['inputRowsOverride'] = copy.deepcopy(model['lastSealedInputs'])
        # Leave compile-root semantics valid; only the metadata binding may reject
        # this otherwise innocuous but resealed command-plan change.
        model['commands']['fixture_note'] = 'changed after the retained input snapshot'
        self.seal_bundle()
        self.assertEqual(previous_inputs, (self.root / 'portable/consumer/inputs.json').read_bytes())
        self.refuse()


    def test_v2_staging_requires_its_exact_own_source_input_record(self):
        original = copy.deepcopy(self.proofs)
        rows = copy.deepcopy(self.proofs['consumer']['lastSealedInputs'])
        source_path = '/synthetic/proof/consumer/sources/' + SOURCE_PATH
        source_index = next(i for i, row in enumerate(rows) if row['path'] == source_path)
        variants = []
        missing = copy.deepcopy(rows)
        del missing[source_index]
        variants.append(missing)
        for field, value in [('sha256', '0' * 64),
                             ('bytes', len(self.candidate_bytes) + 1),
                             ('path', '/synthetic/proof/recipe/sources/' + SOURCE_PATH),
                             ('path', '/synthetic/original-authoring/' + SOURCE_PATH)]:
            changed = copy.deepcopy(rows)
            changed[source_index][field] = value
            changed.sort(key=lambda row: Path(row['path']))
            variants.append(changed)
        for index, inventory in enumerate(variants):
            with self.subTest(variant=index):
                self.proofs = copy.deepcopy(original)
                self.proofs['consumer']['inputRowsOverride'] = inventory
                self.seal_bundle()
                self.refuse()


    def test_v2_input_manifest_requires_sorted_unique_well_formed_records(self):
        original = copy.deepcopy(self.proofs)
        rows = copy.deepcopy(self.proofs['consumer']['lastSealedInputs'])
        duplicate = sorted(rows + [copy.deepcopy(rows[0])], key=lambda row: Path(row['path']))
        boolean_size = copy.deepcopy(rows)
        boolean_size[0]['bytes'] = True
        alias = copy.deepcopy(rows)
        alias[0]['path'] = '/synthetic/proof/consumer/../consumer/commands.json'
        variants = [[], {}, [None], list(reversed(rows)), duplicate, boolean_size, alias]
        for index, inventory in enumerate(variants):
            with self.subTest(variant=index):
                self.proofs = copy.deepcopy(original)
                self.proofs['consumer']['inputRowsOverride'] = inventory
                self.seal_bundle()
                self.refuse()


    def test_v2_compile_source_root_requires_one_exact_selection(self):
        original = copy.deepcopy(self.proofs)
        exact = '-Dproof.sources=/synthetic/proof/consumer/sources'
        variants = [[], ['-Dproof.sources=/synthetic/proof/recipe/sources'],
                    [exact, exact], [exact, '-Dproof.sources=/synthetic/other'],
                    ['-Dproof.sources=/synthetic/proof/consumer/./sources'],
                    [exact, '-Dproof.sources'], [exact, '-D', 'proof.sources=/synthetic/other'],
                    [exact, '--define', 'proof.sources=/synthetic/other'],
                    [exact, '--define=proof.sources=/synthetic/other']]
        for properties in variants:
            with self.subTest(properties=properties):
                self.proofs = copy.deepcopy(original)
                self.proofs['consumer']['commands']['commands']['compile'] = [
                    ['/synthetic/tool', 'compile'] + properties]
                self.seal_bundle()
                self.refuse()


    def test_v2_bound_historical_paths_remain_labels_and_accept_the_synthetic_bundle(self):
        before = self.snapshot()
        receipt = self.admit()
        self.assertEqual('RECEIVED_FOR_REVIEW', receipt['state'])
        self.assertIs(receipt['runtimeAccepted'], False)
        self.assertIs(receipt['originalToolchainCustodyRevalidated'], False)
        self.assertEqual(before, self.snapshot())

    def domain_fixture(self, kind, count, additions=0, crate=None, recipe_name=None, source_commit=SOURCE_COMMIT):
        """Author protocol-shaped data; this is not an executed Java recipe receipt."""
        recipe_name = recipe_name or kind.capitalize()
        recipe_id = 'com.synexia.rewrite.' + recipe_name
        crate = crate or kind + '-v1'
        resource = 'com/synexia/rewrite/hash-pinned-java/' + crate + '/manifest.tsv'
        root_resource = 'synexia-openrewrite-recipes/src/main/resources/'
        outputs, targets, resources = [], [], []
        for index in range(count):
            path = 'source/src/main/java/example/Owner' + str(index) + '.java'
            content = ('package example; final class Owner' + str(index) + ' {}\n').encode()
            artifact = self.put('synthetic-domain/' + kind + '/outputs/' + path, content)
            before = 'ABSENT' if index >= count - additions else sha(b'synthetic baseline ' + str(index).encode())
            template = 'Owner' + str(index) + '.java.txt'
            targets.append(path + '\t' + before + '\t' + artifact['sha256'] + '\t' + template + '\n')
            outputs.append({'path': path, 'sha256': artifact['sha256'],
                            'gitBlob': git_object('blob', content), 'artifact': artifact['path']})
            resources.append((resource.rsplit('/', 1)[0] + '/' + template, content))
        manifest_bytes = ''.join(targets).encode()
        resources.append((resource, manifest_bytes))
        manifest_ref = self.put('synthetic-domain/' + kind + '/manifest.tsv', manifest_bytes)
        patch_bytes = ''.join('diff --git a/' + row['path'] + ' b/' + row['path'] + '\n'
                              for row in outputs).encode()
        # Deliberately synthetic patch bytes: only the envelope parser is exercised.
        patch_ref = self.put('synthetic-domain/' + kind + '/candidate.patch', patch_bytes)
        rows = []
        for index, row in enumerate(outputs):
            content = (self.root / row['artifact']).read_bytes()
            before = targets[index].split('\t')[1]
            rows.append({'path': row['path'], 'beforeSHA256': before, 'afterSHA256': row['sha256'],
                         'sha256': row['sha256'], 'gitBlob': row['gitBlob'], 'bytes': len(content),
                         'change': 'ADD' if before == 'ABSENT' else 'REPLACE'})
        seal_text = (kind.upper() + '-OUTPUT/1\nmanifest\t' + manifest_ref['sha256']
                     + '\npatch\t' + patch_ref['sha256'] + '\t' + str(len(patch_bytes)) + '\n')
        seal_text += ''.join(row['path'] + '\t' + row['sha256'] + '\t' + str(row['bytes']) + '\n'
                             for row in rows)
        seal = sha(seal_text.encode())
        controls = ({'inventoryRefusals': 12, 'postScanRefusals': 8, 'postScanSchedulerRefusals': 3}
                    if kind == 'mre' else {'mixedStates': 1 << count, 'inventoryRefusals': 4 * count + 1})
        output = {'schema': 'synexia.' + kind + '.output/1', 'crate': crate,
                  'baseline': source_commit, 'recipe': recipe_id,
                  'manifest': {'resource': resource, 'sha256': manifest_ref['sha256']},
                  'targetCount': count, 'changes': count, 'replacements': count - additions,
                  'additions': additions, 'envelope': 'generic-rendered-java-snapshot',
                  'patch': {'path': 'candidate.patch', 'sha256': patch_ref['sha256'], 'bytes': len(patch_bytes)},
                  'files': rows, 'outputSealSHA256': seal, **controls}
        output_ref = self.put_json('synthetic-domain/' + kind + '/OUTPUT.json', output)
        manifest = {'sourceCommit': source_commit, 'recipe': recipe_id, 'files': outputs,
                    'materialization': {'manifest': manifest_ref, 'output': output_ref, 'patch': patch_ref}}
        fields = {'crate': crate, 'targets': count, 'changes': count, 'replacements': count - additions,
                  'additions': additions, **controls, 'manifestSHA256': manifest_ref['sha256'], 'outputSeal': seal}
        marker = kind.upper() + '_VERIFIED ' + ' '.join(key + '=' + str(value) for key, value in fields.items()) + '\n'
        historical = '/synthetic/domain/' + kind
        command = ['/synthetic/jdk/bin/java', '-ea', '-D' + kind + '.output=' + historical + '/generated-candidate',
                   'com.synexia.rewrite.' + ('Mrp2Test' if crate == 'mrp-v2' else recipe_name + 'Test')]
        proof = {'commands': {'inputs': historical + '/inputs.json', 'javaHome': '/synthetic/jdk',
                              'commands': {'runtime': [command]}}, 'stdout': [SYNTHETIC + marker],
                 'staging': [{'path': root_resource + name, 'sha256': sha(data),
                              'gitBlob': git_object('blob', data), 'bytes': len(data)}
                             for name, data in resources],
                 'artifacts': [{'path': 'classes/' + name, 'sha256': sha(data), 'bytes': len(data)}
                               for name, data in resources]}
        if crate == 'mrp-v2':
            legacy_command = ['/synthetic/jdk/bin/java', '-ea', '-Dmrp.output=' + historical + '/legacy-candidate',
                              'com.synexia.rewrite.MrpTest']
            proof['commands']['commands']['runtime'].insert(0, legacy_command)
            proof['stdout'].insert(0, SYNTHETIC + 'MRP_VERIFIED crate=mrp-v1 targets=2 compatibility=synthetic\n')
        return {'recipe': recipe_id, 'outputSeal': seal}, manifest, proof

    def test_actual_domain_grammars_reconstruct_original_metadata_and_seals(self):
        for kind, count, additions, crate in (('mre', 3, 1, 'mre-v1'), ('mrc', 7, 0, 'mrc-v1'),
                                               ('mrp', 2, 0, 'mrp-v1'), ('mrp', 4, 0, 'mrp-v2'),
                                               ('mrd', 1, 0, 'mrd-v1')):
            with self.subTest(kind=kind, crate=crate):
                lane, manifest, proof = self.domain_fixture(kind, count, additions, crate)
                self.assertEqual(lane['outputSeal'], migration._intake_materialization(self.root, lane, manifest, proof))
                self.assertNotIn('candidateFiles=', proof['stdout'][0])

    def test_domain_output_rejects_pcr_raw_seal_and_resealed_metadata_drift(self):
        lane, manifest, proof = self.domain_fixture('mre', 3, 1)
        raw_seal = sha(''.join(row['path'] + '\t' + row['sha256'] + '\n' for row in manifest['files']).encode())
        wrong = dict(lane, outputSeal=raw_seal)
        with self.assertRaises(ValueError):
            migration._intake_materialization(self.root, wrong, manifest, proof)
        output_ref = manifest['materialization']['output']
        output = json.loads((self.root / output_ref['path']).read_bytes())
        output['files'][0]['bytes'] += 1
        manifest['materialization']['output'] = self.put_json(output_ref['path'], output)
        with self.assertRaises(ValueError):
            migration._intake_materialization(self.root, lane, manifest, proof)

    def test_domain_patch_drift_is_not_hidden_by_resealing_artifact_reference(self):
        lane, manifest, proof = self.domain_fixture('mrc', 7)
        ref = manifest['materialization']['patch']
        manifest['materialization']['patch'] = self.put(ref['path'], (self.root / ref['path']).read_bytes() + b'\n')
        with self.assertRaises(ValueError):
            migration._intake_materialization(self.root, lane, manifest, proof)

    def test_pcr_same_seal_wrong_recipe_prefix_refuses(self):
        model = self.proofs['recipe']
        model['stdout'] = model['stdout'].replace('PCR_VERIFIED', 'PCS_VERIFIED')
        model['commands']['runtimePatterns'] = {'1': ['^PCS_VERIFIED ']}
        self.seal_bundle()
        self.refuse()

    def test_materializer_class_and_output_property_are_command_bound(self):
        original = copy.deepcopy(self.proofs)
        for field in ('class', 'output', 'duplicate-output', 'assertions'):
            with self.subTest(field=field):
                self.proofs = copy.deepcopy(original)
                command = self.proofs['recipe']['commands']['commands']['runtime'][0]
                if field == 'class':
                    command[-1] = 'example.UnrelatedTest'
                elif field == 'output':
                    command[2] = '-Dpcr.output=/synthetic/elsewhere'
                elif field == 'duplicate-output':
                    command.insert(2, command[2])
                else:
                    command.remove('-ea')
                self.seal_bundle()
                self.refuse()

    def test_template_compiled_inventory_must_match_actual_output(self):
        model = self.proofs['recipe']
        row = next(row for row in model['artifacts'] if row['path'] == 'classes/' + self.template_resource)
        row['sha256'] = '0' * 64
        self.seal_bundle()
        self.refuse()

    def test_resealed_consumer_runtime_completion_pattern_must_hold(self):
        self.proofs['consumer']['stdout'] = SYNTHETIC + 'INCOMPLETE\n'
        self.seal_bundle()
        self.refuse()



    def test_pcr_changes_must_match_the_manifest_before_after_rows(self):
        model = self.proofs['recipe']
        model['stdout'] = model['stdout'].replace('changes=1', 'changes=0')
        self.seal_bundle()
        self.refuse()

    def test_historical_input_order_uses_posix_path_components(self):
        model = self.proofs['consumer']
        rows = copy.deepcopy(model['lastSealedInputs'])
        rows += [{'path': '/synthetic/m2/guava/file.jar', 'sha256': sha(b'x'), 'bytes': 1},
                 {'path': '/synthetic/m2/guava-parent/file.pom', 'sha256': sha(b'y'), 'bytes': 1}]
        rows.sort(key=lambda row: Path(row['path']))
        self.assertNotEqual([row['path'] for row in rows], sorted(row['path'] for row in rows))
        model['inputRowsOverride'] = rows
        self.seal_bundle()
        self.assertEqual('RECEIVED_FOR_REVIEW', self.admit()['state'])

    def alias_fixture(self, kind, source_commit=SOURCE_COMMIT):
        """Exact new protocol shapes, independent of the production lookup table."""
        if kind == 'mre':
            return self.domain_fixture('mre', 3, 1, 'mre-v2', 'Mre2', source_commit)
        if kind == 'mrc':
            return self.domain_fixture('mrc', 6, 0, 'mrc-v2', 'Mrc2', source_commit)
        raise AssertionError('undeclared synthetic alias')

    def fresh_alias_packet(self, kind):
        """Reseal a complete synthetic producer/consumer bundle at one fresh pin.

        This fixture exercises parser consistency only. Its logs, class hashes,
        XML and root tree are authored data, not evidence of upstream execution.
        """
        self.source_commit = FRESH_SOURCE_COMMIT
        partial_lane, self.manifest, partial_proof = self.alias_fixture(kind, self.source_commit)
        self.packet['source']['commit'] = self.source_commit
        self.doc['migration']['records'] = copy.deepcopy(self.historical['migration']['records'])
        mappings = []
        for index, output in enumerate(self.manifest['files']):
            record = copy.deepcopy(self.intake_record)
            record['id'] = 'synexia.intake.' + kind + '2.Owner' + str(index)
            record['owner'] = 'example.Owner' + str(index)
            record['sources'][0].update(commit=self.source_commit, path=output['path'],
                                         symbol=record['owner'], sha256=output['sha256'],
                                         git_blob_sha1=output['gitBlob'])
            record['sync']['source_revision'] = self.source_commit
            record['recipe']['id'] = partial_lane['recipe']
            self.doc['migration']['records'].append(record)
            mappings.append({'path': output['path'], 'id': record['id'],
                             'disposition': 'module-port-candidate',
                             'requiredGates': ['java21-module', 'jni-abi-review']})
        self.packet['lanes'] = [{'id': kind + '2', **partial_lane, 'manifest': {},
                                 'proofs': [], 'mappings': mappings}]
        self.proofs = {role: self.proof_model(role) for role in ('recipe', 'consumer')}
        producer = self.proofs['recipe']
        producer['commands']['commands']['runtime'] = [[
            '/synthetic/jdk/bin/java', '-ea',
            '-D' + kind + '.output=/synthetic/proof/recipe/generated-candidate',
            'com.synexia.rewrite.' + ('Mre2Test' if kind == 'mre' else 'Mrc2Test')]]
        producer['commands']['runtimePatterns'] = {'1': ['^' + kind.upper() + '_VERIFIED ']}
        producer['stdout'] = partial_proof['stdout'][0]
        producer['staging']['files'] = sorted(partial_proof['staging'], key=lambda row: row['path'])
        producer['artifacts'] = [producer['artifacts'][0]] + partial_proof['artifacts']
        self.proofs['consumer']['staging']['files'] = [
            {'path': row['path'], 'sha256': row['sha256'], 'gitBlob': row['gitBlob'],
             'bytes': len((self.root / row['artifact']).read_bytes())}
            for row in self.manifest['files']]
        self.bind_authority()
        self.bind_manifest()
        self.seal_bundle()

    def test_v3_explicit_aliases_preserve_family_schema_domain_and_property(self):
        for kind, count, replacements, additions in (('mre', 3, 2, 1), ('mrc', 6, 6, 0)):
            with self.subTest(kind=kind):
                lane, manifest, proof = self.alias_fixture(kind)
                self.assertEqual(lane['outputSeal'],
                                 migration._intake_materialization(self.root, lane, manifest, proof))
                output = json.loads((self.root / manifest['materialization']['output']['path']).read_bytes())
                self.assertEqual('synexia.' + kind + '.output/1', output['schema'])
                self.assertEqual(kind + '-v2', output['crate'])
                self.assertEqual('com.synexia.rewrite.' + ('Mre2' if kind == 'mre' else 'Mrc2'), output['recipe'])
                self.assertEqual((count, replacements, additions),
                                 (output['targetCount'], output['replacements'], output['additions']))
                command = proof['commands']['commands']['runtime'][0]
                self.assertIn('-D' + kind + '.output=/synthetic/domain/' + kind + '/generated-candidate', command)
                if kind == 'mrc':
                    self.assertEqual((64, 25), (output['mixedStates'], output['inventoryRefusals']))

    def test_v3_aliases_are_explicit_not_inferred_from_recipe_suffix(self):
        for kind in ('mre', 'mrc'):
            for name in (kind.capitalize(), kind.capitalize() + '3', kind.upper() + '2'):
                with self.subTest(kind=kind, name=name):
                    lane, manifest, proof = self.alias_fixture(kind)
                    lane['recipe'] = manifest['recipe'] = 'com.synexia.rewrite.' + name
                    ref = manifest['materialization']['output']
                    output = json.loads((self.root / ref['path']).read_bytes())
                    output['recipe'] = lane['recipe']
                    manifest['materialization']['output'] = self.put_json(ref['path'], output)
                    with self.assertRaises(ValueError):
                        migration._intake_materialization(self.root, lane, manifest, proof)

    def test_v3_resealed_output_requires_exact_alias_recipe_id(self):
        for kind in ('mre', 'mrc'):
            with self.subTest(kind=kind):
                lane, manifest, proof = self.alias_fixture(kind)
                ref = manifest['materialization']['output']
                output = json.loads((self.root / ref['path']).read_bytes())
                output['recipe'] = 'com.synexia.rewrite.' + kind.capitalize()
                manifest['materialization']['output'] = self.put_json(ref['path'], output)
                with self.assertRaisesRegex(ValueError, 'OUTPUT.json differs'):
                    migration._intake_materialization(self.root, lane, manifest, proof)

    def test_v3_aliases_refuse_a_fully_consistent_old_crate(self):
        for kind, count, additions in (('mre', 3, 1), ('mrc', 6, 0)):
            with self.subTest(kind=kind):
                lane, manifest, proof = self.domain_fixture(
                    kind, count, additions, kind + '-v1', kind.capitalize() + '2')
                with self.assertRaisesRegex(ValueError, 'bounded materializer crate identity'):
                    migration._intake_materialization(self.root, lane, manifest, proof)

    def test_v3_alias_runtime_requires_exact_main_and_family_property(self):
        for kind in ('mre', 'mrc'):
            for variant in ('old-main', 'other-main', 'alias-property'):
                with self.subTest(kind=kind, variant=variant):
                    lane, manifest, proof = self.alias_fixture(kind)
                    command = proof['commands']['commands']['runtime'][0]
                    if variant == 'old-main':
                        command[-1] = 'com.synexia.rewrite.' + kind.capitalize() + 'Test'
                    elif variant == 'other-main':
                        command[-1] = 'com.synexia.rewrite.' + ('Mrc2Test' if kind == 'mre' else 'Mre2Test')
                    else:
                        command[2] = command[2].replace('-D' + kind + '.output=', '-D' + kind + '2.output=')
                    with self.assertRaises(ValueError):
                        migration._intake_materialization(self.root, lane, manifest, proof)

    def test_v3_fresh_single_baseline_alias_packet_remains_review_only(self):
        for kind, count in (('mre', 3), ('mrc', 6)):
            with self.subTest(kind=kind):
                self.fresh_alias_packet(kind)
                before = self.snapshot()
                receipt = self.admit()
                self.assertEqual(('RECEIVED_FOR_REVIEW', FRESH_SOURCE_COMMIT, count),
                                 (receipt['state'], receipt['source']['commit'], receipt['outputs']))
                for flag in ('runtimeAccepted', 'originalToolchainCustodyRevalidated',
                             'wholeSourceTreeCovered', 'dependencyClosureComplete'):
                    self.assertIs(receipt[flag], False)
                self.assertEqual(FRESH_SOURCE_COMMIT, self.manifest['sourceCommit'])
                for role in ('recipe', 'consumer'):
                    for filename in ('RESULT.json', 'PATCH.json', 'staging.json'):
                        value = json.loads((self.root / 'portable' / role / filename).read_bytes())
                        self.assertEqual(FRESH_SOURCE_COMMIT, value['baseline'])
                self.assertEqual(before, self.snapshot())

    def test_v3_resealed_old_producer_or_consumer_baselines_refuse(self):
        for role in ('recipe', 'consumer'):
            for variant in ('result', 'patch', 'staging', 'all'):
                with self.subTest(role=role, variant=variant):
                    self.fresh_alias_packet('mrc')
                    self.assertEqual('RECEIVED_FOR_REVIEW', self.admit()['state'])
                    model = self.proofs[role]
                    if variant in ('result', 'all'):
                        model['resultOverrides']['baseline'] = SOURCE_COMMIT
                    if variant in ('patch', 'all'):
                        model['patchOverrides']['baseline'] = SOURCE_COMMIT
                    if variant in ('staging', 'all'):
                        model['staging']['baseline'] = SOURCE_COMMIT
                    self.seal_bundle()
                    self.refuse()

    def test_v3_resealed_old_output_baseline_refuses(self):
        self.fresh_alias_packet('mre')
        self.assertEqual('RECEIVED_FOR_REVIEW', self.admit()['state'])
        ref = self.manifest['materialization']['output']
        output = json.loads((self.root / ref['path']).read_bytes())
        output['baseline'] = SOURCE_COMMIT
        self.manifest['materialization']['output'] = self.put_json(ref['path'], output)
        self.bind_manifest()
        self.refuse()

    def test_v3_packet_and_lanes_cannot_declare_extra_source_baselines(self):
        self.fresh_alias_packet('mre')
        self.assertEqual('RECEIVED_FOR_REVIEW', self.admit()['state'])
        variants = []
        lane_pin = copy.deepcopy(self.packet)
        lane_pin['lanes'][0]['sourceCommit'] = SOURCE_COMMIT
        variants.append(lane_pin)
        multi_pin = copy.deepcopy(self.packet)
        multi_pin['sourceCommits'] = [FRESH_SOURCE_COMMIT, SOURCE_COMMIT]
        variants.append(multi_pin)
        for packet in variants:
            with self.subTest(extra=set(packet) - set(self.packet)):
                self.refuse(packet)


    def fresh_mrc3_packet(self):
        """Reuse the complete synthetic current packet with the explicit third alias."""
        self.fresh_alias_packet('mrc')
        lane, self.manifest, proof = self.domain_fixture(
            'mrc', 6, 0, 'mrc-v3', 'Mrc3', FRESH_SOURCE_COMMIT)
        self.packet['lanes'][0].update(lane)
        for record in self.doc['migration']['records']:
            if record['id'].startswith('synexia.intake.'):
                record['recipe']['id'] = lane['recipe']
        producer = self.proofs['recipe']
        producer['commands']['commands']['runtime'][0][-1] = 'com.synexia.rewrite.Mrc3Test'
        producer['stdout'] = proof['stdout'][0]
        producer['staging']['files'] = sorted(proof['staging'], key=lambda row: row['path'])
        producer['artifacts'] = [producer['artifacts'][0]] + proof['artifacts']
        self.bind_authority()
        self.bind_manifest()
        self.seal_bundle()

    def test_v4_mrc3_current_six_target_packet_is_read_only_and_family_bound(self):
        self.fresh_mrc3_packet()
        before = self.snapshot()
        receipt = self.admit()
        self.assertEqual(('RECEIVED_FOR_REVIEW', FRESH_SOURCE_COMMIT, 6),
                         (receipt['state'], receipt['source']['commit'], receipt['outputs']))
        for flag in ('runtimeAccepted', 'originalToolchainCustodyRevalidated',
                     'wholeSourceTreeCovered', 'dependencyClosureComplete'):
            self.assertIs(receipt[flag], False)
        output = json.loads((self.root / self.manifest['materialization']['output']['path']).read_bytes())
        self.assertEqual(('synexia.mrc.output/1', 'mrc-v3', 'com.synexia.rewrite.Mrc3'),
                         (output['schema'], output['crate'], output['recipe']))
        self.assertEqual((6, 6, 0, 64, 25), (output['targetCount'], output['replacements'],
                         output['additions'], output['mixedStates'], output['inventoryRefusals']))
        command = self.proofs['recipe']['commands']['commands']['runtime'][0]
        self.assertEqual('com.synexia.rewrite.Mrc3Test', command[-1])
        self.assertIn('-Dmrc.output=/synthetic/proof/recipe/generated-candidate', command)
        self.assertEqual(before, self.snapshot())

    def test_v4_mrc3_refuses_other_crates_main_classes_recipes_and_properties(self):
        for variant in ('old-crate', 'old-main', 'old-recipe', 'alias-property'):
            with self.subTest(variant=variant):
                lane, manifest, proof = self.domain_fixture(
                    'mrc', 6, 0, 'mrc-v2' if variant == 'old-crate' else 'mrc-v3',
                    'Mrc3', FRESH_SOURCE_COMMIT)
                command = proof['commands']['commands']['runtime'][0]
                if variant == 'old-main':
                    command[-1] = 'com.synexia.rewrite.Mrc2Test'
                elif variant == 'old-recipe':
                    ref = manifest['materialization']['output']
                    output = json.loads((self.root / ref['path']).read_bytes())
                    output['recipe'] = 'com.synexia.rewrite.Mrc2'
                    manifest['materialization']['output'] = self.put_json(ref['path'], output)
                elif variant == 'alias-property':
                    command[2] = command[2].replace('-Dmrc.output=', '-Dmrc3.output=')
                with self.assertRaises(ValueError):
                    migration._intake_materialization(self.root, lane, manifest, proof)

    def test_v4_mrc3_resealed_old_producer_and_consumer_baselines_refuse(self):
        for role in ('recipe', 'consumer'):
            with self.subTest(role=role):
                self.fresh_mrc3_packet()
                self.assertEqual('RECEIVED_FOR_REVIEW', self.admit()['state'])
                model = self.proofs[role]
                model['resultOverrides']['baseline'] = SOURCE_COMMIT
                model['patchOverrides']['baseline'] = SOURCE_COMMIT
                model['staging']['baseline'] = SOURCE_COMMIT
                self.seal_bundle()
                self.refuse()


    def fresh_mrc4_packet(self):
        """Author a complete seven-output synthetic bundle; no upstream execution.

        Every mapping, canonical record, resource and consumer row is built from
        the seven-output fixture. Historical six-output alias fixtures stay intact.
        """
        self.source_commit = FRESH_SOURCE_COMMIT
        lane, self.manifest, proof = self.domain_fixture(
            'mrc', 7, 0, 'mrc-v4', 'Mrc4', self.source_commit)
        self.packet['source']['commit'] = self.source_commit
        self.doc['migration']['records'] = copy.deepcopy(self.historical['migration']['records'])
        mappings = []
        for index, output in enumerate(self.manifest['files']):
            record = copy.deepcopy(self.intake_record)
            record['id'] = 'synexia.intake.mrc2.Owner' + str(index)
            record['owner'] = 'example.Owner' + str(index)
            record['sources'][0].update(commit=self.source_commit, path=output['path'],
                                       symbol=record['owner'], sha256=output['sha256'],
                                       git_blob_sha1=output['gitBlob'])
            record['sync']['source_revision'] = self.source_commit
            record['recipe']['id'] = lane['recipe']
            self.doc['migration']['records'].append(record)
            mappings.append({'path': output['path'], 'id': record['id'],
                             'disposition': 'module-port-candidate',
                             'requiredGates': ['java21-module', 'jni-abi-review']})
        self.packet['lanes'] = [{'id': 'mrc2', **lane, 'manifest': {},
                                 'proofs': [], 'mappings': mappings}]
        self.proofs = {role: self.proof_model(role) for role in ('recipe', 'consumer')}
        producer = self.proofs['recipe']
        producer['commands']['commands']['runtime'] = [[
            '/synthetic/jdk/bin/java', '-ea',
            '-Dmrc.output=/synthetic/proof/recipe/generated-candidate',
            'com.synexia.rewrite.Mrc4Test']]
        producer['commands']['runtimePatterns'] = {'1': ['^MRC_VERIFIED ']}
        producer['stdout'] = proof['stdout'][0]
        producer['staging']['files'] = sorted(proof['staging'], key=lambda row: row['path'])
        producer['artifacts'] = [producer['artifacts'][0]] + proof['artifacts']
        self.proofs['consumer']['staging']['files'] = [
            {'path': row['path'], 'sha256': row['sha256'], 'gitBlob': row['gitBlob'],
             'bytes': len((self.root / row['artifact']).read_bytes())}
            for row in self.manifest['files']]
        self.bind_authority()
        self.bind_manifest()
        self.seal_bundle()

    def test_v5_mrc4_complete_seven_target_packet_is_read_only_and_family_bound(self):
        self.fresh_mrc4_packet()
        lane = self.packet['lanes'][0]
        self.assertEqual('mrc2', lane['id'])
        self.assertEqual(7, len(lane['mappings']))
        self.assertEqual(7, len([record for record in self.doc['migration']['records']
                                if record['id'].startswith('synexia.intake.')]))
        self.assertEqual([row['path'] for row in self.manifest['files']],
                         [row['path'] for row in self.proofs['consumer']['staging']['files']])
        self.assertEqual(8, len(self.proofs['recipe']['staging']['files']))
        before = self.snapshot()
        receipt = self.admit()
        self.assertEqual(('RECEIVED_FOR_REVIEW', FRESH_SOURCE_COMMIT, 7, 0),
                         (receipt['state'], receipt['source']['commit'], receipt['outputs'], receipt['productWrites']))
        for flag in ('runtimeAccepted', 'originalToolchainCustodyRevalidated',
                     'wholeSourceTreeCovered', 'dependencyClosureComplete'):
            self.assertIs(receipt[flag], False)
        output = json.loads((self.root / self.manifest['materialization']['output']['path']).read_bytes())
        self.assertEqual(('synexia.mrc.output/1', 'mrc-v4', 'com.synexia.rewrite.Mrc4'),
                         (output['schema'], output['crate'], output['recipe']))
        self.assertEqual((7, 7, 0, 128, 29), (output['targetCount'], output['replacements'],
                         output['additions'], output['mixedStates'], output['inventoryRefusals']))
        command = self.proofs['recipe']['commands']['commands']['runtime'][0]
        self.assertEqual('com.synexia.rewrite.Mrc4Test', command[-1])
        self.assertIn('-Dmrc.output=/synthetic/proof/recipe/generated-candidate', command)
        self.assertEqual(receipt, self.admit())
        self.assertEqual(before, self.snapshot())

    def test_v5_mrc4_refuses_other_crates_main_classes_recipes_and_properties(self):
        for variant in ('old-crate', 'old-main', 'old-recipe', 'alias-property'):
            with self.subTest(variant=variant):
                lane, manifest, proof = self.domain_fixture(
                    'mrc', 7, 0, 'mrc-v3' if variant == 'old-crate' else 'mrc-v4',
                    'Mrc4', FRESH_SOURCE_COMMIT)
                command = proof['commands']['commands']['runtime'][0]
                if variant == 'old-main':
                    command[-1] = 'com.synexia.rewrite.Mrc3Test'
                elif variant == 'old-recipe':
                    ref = manifest['materialization']['output']
                    output = json.loads((self.root / ref['path']).read_bytes())
                    output['recipe'] = 'com.synexia.rewrite.Mrc3'
                    manifest['materialization']['output'] = self.put_json(ref['path'], output)
                elif variant == 'alias-property':
                    command[2] = command[2].replace('-Dmrc.output=', '-Dmrc4.output=')
                with self.assertRaises(ValueError):
                    migration._intake_materialization(self.root, lane, manifest, proof)

    def test_v5_mrc4_resealed_old_producer_and_consumer_baselines_refuse(self):
        for role in ('recipe', 'consumer'):
            with self.subTest(role=role):
                self.fresh_mrc4_packet()
                self.assertEqual('RECEIVED_FOR_REVIEW', self.admit()['state'])
                model = self.proofs[role]
                model['resultOverrides']['baseline'] = SOURCE_COMMIT
                model['patchOverrides']['baseline'] = SOURCE_COMMIT
                model['staging']['baseline'] = SOURCE_COMMIT
                self.seal_bundle()
                self.refuse()

    def test_v5_mrc4_missing_seventh_mapping_record_or_consumer_source_refuses(self):
        for variant in ('lane-mapping', 'canonical-record', 'consumer-source'):
            with self.subTest(variant=variant):
                self.fresh_mrc4_packet()
                self.assertEqual('RECEIVED_FOR_REVIEW', self.admit()['state'])
                seventh = self.packet['lanes'][0]['mappings'][-1]
                self.assertTrue(seventh['path'].endswith('/Owner6.java'))
                if variant == 'lane-mapping':
                    self.packet['lanes'][0]['mappings'].pop()
                elif variant == 'canonical-record':
                    self.doc['migration']['records'] = [record for record in self.doc['migration']['records']
                                                        if record['id'] != seventh['id']]
                    self.bind_authority()
                else:
                    self.proofs['consumer']['staging']['files'].pop()
                    self.seal_bundle()
                self.refuse()


if __name__ == '__main__':
    unittest.main(verbosity=2)
