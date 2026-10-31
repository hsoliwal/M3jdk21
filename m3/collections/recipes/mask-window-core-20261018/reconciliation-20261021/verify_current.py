#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Supplement the existing serial proof with exact current-master ABI/consumer fixtures."""
from pathlib import Path
import argparse, hashlib, importlib.util, json, subprocess

HERE = Path(__file__).resolve().parent
TASK = HERE.parent
J = 'm3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java'
C = 'm3/collections/src/main/native/collections/m3_index_mask_compactor.c'

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--proof', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    proof = args.proof.resolve()
    out = args.out.resolve()
    out.mkdir(parents=True, exist_ok=False)
    commands = []
    result = {'status': 'RUNNING'}
    def run(stage, argv, marker=None):
        process = subprocess.run(list(map(str, argv)), text=True, capture_output=True, timeout=120)
        log = out / f'{len(commands):02d}-{stage}.log'
        log.write_text(process.stdout + process.stderr)
        commands.append({'stage': stage, 'argv': list(map(str, argv)), 'exit': process.returncode,
                         'log': log.name, 'sha256': digest(log)})
        (out/'COMMANDS.json').write_text(json.dumps(commands, indent=2)+'\n')
        if process.returncode != 0 or marker and marker not in process.stdout:
            raise AssertionError(stage + ': ' + log.read_text())
        print(stage, process.stdout.strip(), flush=True)
        return process.stdout
    try:
        admitted = json.loads((proof/'RESULT.json').read_text())
        assert admitted['status'] == 'PASS_SELECTED_DIRECT_GATES'
        assert admitted['target_base'] == '55b44776b15b658c14f33ee2c4e01b013d6547a8'
        for row in json.loads((HERE/'FIXTURE_PINS.json').read_text()):
            assert digest(HERE/'fixtures'/Path(row['path']).name) == row['sha256']
        spec = importlib.util.spec_from_file_location('existing_author', TASK/'author.py')
        author = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(author)
        for name in author.EXPECTED:
            assert digest(TASK/'before'/name) == author.EXPECTED[name]
        for path, _, after, _ in author_manifest(TASK):
            assert digest(proof/'candidate'/path) == after
        preservation = []
        def preserved(name, signatures):
            before = (TASK/'before'/name).read_text()
            after = (proof/'candidate'/(J if name.endswith('.java') else C)).read_text()
            for signature in signatures:
                aa, ab = author.body_span(before, signature)
                ba, bb = author.body_span(after, signature)
                assert before[aa:ab] == after[ba:bb], signature
                preservation.append({'file': name, 'signature': signature,
                    'body_sha256': hashlib.sha256(before[aa:ab].encode()).hexdigest(),
                    'status': 'BYTE_IDENTICAL'})
        preserved('M3IndexMaskCompactor.java', [
            'public static long[] selectLong(long[] orderedRows, byte[] rowStates,\n            int requiredMask, int excludedMask)',
            'public static int[] selectPositions(byte[] rowStates, int requiredMask, Backend backend)',
            'public static int[] selectPositions(byte[] rowStates, int requiredMask,\n            int excludedMask, Backend backend)',
            'public static int selectedCount(byte[] rowStates, int requiredMask, int excludedMask)',
            'private static int countSelected(byte[] states, int requiredMask, int excludedMask)',
            'static boolean matches(byte state, int requiredMask)',
            'static boolean matches(byte state, int requiredMask, int excludedMask)',
            'static int[] selectPositionsJava(byte[] rowStates, int requiredMask)',
            'static int[] selectPositionsJava(byte[] rowStates, int requiredMask, int excludedMask)',
            'private static int[] identityPositions(int length)',
            'private static final class NativeLibrary',
            'private static final class NativePositions',
            'private static final class NativePositionMasks',
            'private static final class NativeRange'])
        preserved('m3_index_mask_compactor.c', [
            'static void m3_throw_state_drift(', 'static void m3_bad_window(',
            'static int m3_window_valid(', 'static jint m3_count_window(',
            'Java_com_m3_collections_M3IndexMaskCompactor_positionsAbiNative(',
            'Java_com_m3_collections_M3IndexMaskCompactor_selectPositionsNative(',
            'Java_com_m3_collections_M3IndexMaskCompactor_positionsMasksAbiNative(',
            'Java_com_m3_collections_M3IndexMaskCompactor_selectPositionsMasksNative('])
        headers_before = {p.name: digest(p) for p in (proof/'before/headers').glob('*.h')}
        headers_after = {p.name: digest(p) for p in (proof/'test/headers').glob('*.h')}
        assert headers_before and headers_before == headers_after
        (out/'PRESERVATION.json').write_text(json.dumps(preservation, indent=2)+'\n')
        outputs = {}
        fixtures = sorted((HERE/'fixtures').glob('*.java')) + [HERE/'M3RequiredExcludedLegacyProbe.java',
                    HERE/'M3RequiredExcludedLongProbe.java']
        for version in ['before', 'test']:
            classes = out/version
            classes.mkdir()
            product_classes = proof/version/'classes'
            run(version+'-lint-compile', ['javac', '--release', '21', '-proc:none', '-Xlint:all',
                    '-Werror', '-cp', product_classes, '-d', classes, *fixtures])
            cp = str(classes)+':'+str(product_classes)
            launcher = ['java', '-Xmx64m', '-Xcheck:jni',
                '-Djava.library.path='+str(proof/version/'native'), '-cp', cp]
            outputs[version] = {}
            for clazz in ['M3RequiredExcludedMaskSelectorContract', 'M3RequiredExcludedMaskSelectorNativeContract',
                          'M3RequiredExcludedLongProbe']:
                outputs[version][clazz] = run(version+'-'+clazz, launcher+['com.m3.collections.'+clazz], marker='PASS ')
            outputs[version]['capability-current'] = run(version+'-capability-current',
                launcher+['com.m3.collections.M3RequiredExcludedLegacyProbe', 'current'], marker='PASS ')
            for mode, library in [('legacy', proof/'legacy-native'), ('missing', proof/version/'empty')]:
                outputs[version]['capability-'+mode] = run(version+'-capability-'+mode,
                    ['java', '-Xmx64m', '-Xcheck:jni', '-Djava.library.path='+str(library), '-cp', cp,
                     'com.m3.collections.M3RequiredExcludedLegacyProbe', mode], marker='PASS ')
        assert outputs['before'] == outputs['test']
        result = {'status': 'PASS_CURRENT_SOURCE_SUPPLEMENT', 'outputs': outputs,
                  'preserved_bodies': len(preservation), 'generated_headers': headers_before,
                  'proof_result_sha256': digest(proof/'RESULT.json'), 'commands': len(commands),
                  'full_module_jdk': 'NOT_RUN', 'phase_advanced': False}
    except BaseException as ex:
        result.update(status='FAIL', error=repr(ex))
        raise
    finally:
        (out/'RESULT.json').write_text(json.dumps(result, indent=2)+'\n')

def author_manifest(task):
    return [line.split('\t') for line in (task/'src/main/resources/m3-mask-window/manifest.tsv').read_text().splitlines()]

if __name__ == '__main__':
    main()
