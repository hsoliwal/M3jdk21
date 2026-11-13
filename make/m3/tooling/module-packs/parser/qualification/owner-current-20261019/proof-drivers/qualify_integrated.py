#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Qualify emitted owners with existing source-built M3JDK21 modules and native runtime.

This proof reuses the checked-in descriptor/hash/payload preservation implementation.
It packages compiled owners; it does not transform Java or modify any input build.
"""
import argparse
import csv
import importlib.util
from pathlib import Path
import re
import shutil


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo', type=Path, required=True)
    parser.add_argument('--source-build', type=Path, required=True)
    parser.add_argument('--built-jmods', type=Path, required=True)
    parser.add_argument('--built-java', type=Path, required=True)
    parser.add_argument('--tool-jdk', type=Path, required=True)
    parser.add_argument('--ordered-proof', type=Path, required=True)
    parser.add_argument('--sdk-output', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    repo = args.repo.resolve(strict=True)
    sdk = args.sdk_output.resolve(strict=True)
    stock = args.tool_jdk.resolve(strict=True)
    inputs = args.built_jmods.resolve(strict=True)
    ordered = args.ordered_proof.resolve(strict=True)
    output = args.out.absolute()
    if output.exists():
        raise FileExistsError('fresh proof output required: ' + str(output))
    helper = repo / ('m3/tooling/shared-frontend/recipe-custody/enhancements/'
                     '20261018-source-seals/runtime/qualify_runtime.py')
    spec = importlib.util.spec_from_file_location('existing_m3_runtime_qualification', helper)
    runtime = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(runtime)
    require = runtime.require
    output.mkdir(parents=True)
    runner = runtime.Commands(output)
    receipt = {
        'schema': 'M3_EMITTED_SOURCE_BUILT_RUNTIME_PROOF_V1',
        'classification': ('Qualification image relinked from sealed existing source-built '
                           'M3JDK21 JMODs and native runtime, with four explicitly compiled '
                           'compiler owner families and the emitted parser module. '
                           'This is not a fresh full JDK/HotSpot source build.'),
        'runtime_patch_module': False,
        'source_build_root': str(args.source_build.resolve()),
        'source_built_jmods': str(inputs),
        'compiler_build_tool': str(stock),
        'script': runtime.record(Path(__file__)),
        'reused_descriptor_and_hash_helper': runtime.record(helper),
        'sdk_execution': runtime.record(sdk / 'SDK_EXECUTION.tsv'),
    }
    try:
        state = runtime.read_json(ordered / 'STATE.json')
        require(state['completed'] == ['lint', 'compile', 'test', 'runtime']
                and state['failed'] is None, 'complete ordered emitted parser proof required')
        receipt['ordered_parser_state'] = runtime.record(ordered / 'STATE.json')
        previous = None
        for phase in state['completed']:
            record_path = ordered / (phase + '-receipt.json')
            require(runtime.sha_file(record_path) == state['receipts'][phase], 'ordered receipt drift')
            phase_receipt = runtime.read_json(record_path)
            require(phase_receipt['status'] == 'PASS'
                    and phase_receipt['input_sha256'] == state['input_sha256']
                    and phase_receipt['previous_receipt_sha256'] == previous, 'invalid phase chain')
            for artifact in phase_receipt['artifacts']:
                require(runtime.sha_file(ordered / artifact['path']) == artifact['sha256'],
                        'ordered artifact drift: ' + artifact['path'])
            previous = state['receipts'][phase]
        rows = list(csv.DictReader((sdk / 'SDK_EXECUTION.tsv').open(), delimiter='\t'))
        for row in rows:
            require(runtime.sha_file(repo / row['path']) == row['after_sha256']
                    == runtime.sha_file(sdk / row['path']), 'emitted source drift: ' + row['path'])
        receipt['emitted_sources'] = rows
        # The existing build owns the same compiler/module source preimages.
        source_witness = {}
        for relative in runtime.SOURCES:
            before = args.source_build / relative
            after = repo / relative
            same = runtime.sha_file(before) == runtime.sha_file(after)
            changed_owner = relative.endswith('/M3ASTPC.java')
            require(same or changed_owner, 'source-built compiler preimage drift: ' + relative)
            if changed_owner:
                expected = next(row['before_sha256'] for row in rows if row['path'] == relative)
                require(runtime.sha_file(before) == expected, 'compiler preimage differs from SDK receipt')
            source_witness[relative] = {'source_build_sha256': runtime.sha_file(before),
                                       'candidate_sha256': runtime.sha_file(after)}
        receipt['compiler_source_witness'] = source_witness
        original_files = {p.name: runtime.record(p) for p in sorted(inputs.glob('*.jmod'))}
        require(len(original_files) >= 60, 'complete source-built JMOD estate required')
        require('jdk.m3.parser.jmod' not in original_files, 'parser module must be a new image input')
        receipt['input_jmods'] = original_files
        originals = {m: runtime.jmod_descriptor(inputs / (m + '.jmod'))
                     for m in ('java.base', 'jdk.compiler')}
        entries = {m: runtime.jmod_entries(inputs / (m + '.jmod'))
                   for m in originals}
        old_hashes = originals['java.base']['ModuleHashes']
        require(old_hashes['algorithm'] == 'SHA-256', 'unsupported hash algorithm')
        require(runtime.module_hash(inputs / 'jdk.compiler.jmod')
                == old_hashes['hashes']['jdk.compiler'], 'source-built compiler hash mismatch')
        extracts = output / 'extract'
        for module in originals:
            runner.run('extract-' + module, [stock / 'bin/jmod', 'extract', '--dir',
                       extracts / module, inputs / (module + '.jmod')])
        compiler_tree = extracts / 'jdk.compiler'
        compiled = ordered / 'compiler-patch'
        classes = runtime.tree_records(compiled)
        require('module-info.class' not in classes, 'compiled patch must not replace the descriptor')
        require(all(runtime.owned_class(name) for name in classes), 'unlisted compiled owner')
        require(all(owner + '.class' in classes for owner in runtime.OWNERS), 'missing compiler family')
        removed = {}
        for name in runtime.tree_records(compiler_tree / 'classes'):
            if name != 'module-info.class' and runtime.owned_class(name):
                path = compiler_tree / 'classes' / name
                removed[name] = runtime.record(path)
                path.unlink()
        for name in classes:
            destination = compiler_tree / 'classes' / name
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(compiled / name, destination)
        receipt['class_families'] = {'removed': removed, 'compiled': classes}
        module_path = output / 'jmods'
        module_path.mkdir()
        for name in original_files:
            if name not in ('java.base.jmod', 'jdk.compiler.jmod'):
                (module_path / name).symlink_to(inputs / name)
        parser_jmod = ordered / 'pack/jmods/jdk.m3.parser.jmod'
        shutil.copyfile(parser_jmod, module_path / parser_jmod.name)
        receipt['parser_jmod'] = runtime.record(parser_jmod)
        runtime.create_jmod(runner, stock, compiler_tree, module_path / 'jdk.compiler.jmod',
                            originals['jdk.compiler'], 'create-compiler-jmod')
        pattern = '^(?:' + '|'.join(re.escape(m) for m in sorted(old_hashes['hashes'])) + ')$'
        runtime.create_jmod(runner, stock, extracts / 'java.base', module_path / 'java.base.jmod',
                            originals['java.base'], 'create-base-with-original-hash-coverage',
                            ['--hash-modules', pattern, '--module-path', module_path])
        runner.run('supported-jmod-hash', [stock / 'bin/jmod', 'hash', '--date', runtime.JMOD_DATE,
                   '--hash-modules', pattern, '--module-path', module_path, module_path / 'java.base.jmod'])
        updated = {m: runtime.jmod_descriptor(module_path / (m + '.jmod')) for m in originals}
        require(updated['jdk.compiler'] == originals['jdk.compiler'], 'compiler descriptor changed')
        require({k: v for k, v in updated['java.base'].items() if k != 'ModuleHashes'}
                == {k: v for k, v in originals['java.base'].items() if k != 'ModuleHashes'},
                'base metadata changed outside hashes')
        new_hashes = updated['java.base']['ModuleHashes']
        require(new_hashes['hashes'].keys() == old_hashes['hashes'].keys(), 'hash coverage changed')
        for module, hashed in new_hashes['hashes'].items():
            require(runtime.module_hash(module_path / (module + '.jmod')) == hashed,
                    'recorded/computed module hash mismatch: ' + module)
            if module != 'jdk.compiler':
                require(hashed == old_hashes['hashes'][module], 'unrelated module hash changed')
        receipt['module_hashes'] = new_hashes
        receipt['payload_preservation'] = {
            m: runtime.preservation(entries[m], runtime.jmod_entries(module_path / (m + '.jmod')),
                                    m == 'jdk.compiler') for m in originals}
        image = output / 'image'
        runner.run('link-m3jdk21-image', [stock / 'bin/jlink', '--module-path', module_path,
                   '--add-modules', 'ALL-MODULE-PATH', '--output', image])
        java = image / 'bin/java'
        exports = [option for package in ('api', 'main', 'm3', 'util')
                   for option in ('--add-exports', 'jdk.compiler/com.sun.tools.javac.' + package + '=ALL-UNNAMED')]
        tests = ['M3ASTSharedFrontendTest', 'M3ASTSourceSealTest', 'M3ASTPluginSelectionTest',
                 'M3ASTProjectionStateTest', 'M3ComputeConstantFactsTest', 'M3PrimitiveFoldedImageTest']
        test_sources = [repo / 'test/langtools/tools/javac/m3' / (test + '.java') for test in tests]
        probe = Path(__file__).resolve().parent / 'proof-src/M3ProjectionCacheResourceProbe.java'
        test_sources.append(probe)
        test_classes = output / 'test-classes'
        test_classes.mkdir()
        runner.run('compile-all-integrated-consumers', [stock / 'bin/javac', '--system', image,
                   '-source', '21', '-target', '21', '-proc:none', '-implicit:none', '-Xlint:all', '-Werror',
                   *exports, '-d', test_classes, *test_sources])
        receipt['consumer_sources'] = {str(p): runtime.record(p) for p in test_sources}
        # Every build compiler invocation above precedes integrated runtime acceptance below.
        receipt['build_complete_before_runtime'] = True
        runner.run('image-version', [java, '-version'])
        runner.run('image-compiler-descriptor', [java, '--describe-module', 'jdk.compiler'])
        for test in tests:
            runner.run(test, [java, '-ea', *exports, '--add-opens',
                       'jdk.compiler/com.sun.tools.javac.m3=ALL-UNNAMED', '-cp', test_classes, test])
        parser_tests = [('M3ParserFrameworkContract', 'M3_PARSER_FRAMEWORK_PASS'),
                        ('M3JavacParserContract', 'M3_JAVAC_PARSER_PASS'),
                        ('M3ParserImageSmoke', 'M3_PARSER_IMAGE_PASS')]
        for name, marker in parser_tests:
            work = output / ('work-' + name)
            work.mkdir()
            text = runner.run(name, [java, '-ea', '-Dm3.parser.test.work=' + str(work),
                             '--add-modules', 'jdk.m3.parser', '-cp', ordered / 'test-classes',
                             'jdk.m3.parser.test.' + name])
            require(marker in text, 'missing parser contract completion marker')
        for lane, executable in [('original', args.built_java), ('emitted', java)]:
            runner.run(lane + '-cache-allocation', [executable, '-Xint', '-ea',
                       '-cp', test_classes, 'M3ProjectionCacheResourceProbe'])
        require({p.name: runtime.record(p) for p in sorted(inputs.glob('*.jmod'))}
                == original_files, 'input source-built JMODs changed')
        receipt.update(status='PASS', image=str(image), linked_modules=runtime.record(image / 'lib/modules'),
                       commands=runner.commands,
                       pending=['fresh full M3JDK21 source rebuild and full jtreg matrix',
                                'other operating systems/architectures', 'controlled throughput benchmarking'])
        runtime.write_json(output / 'RESULT.json', receipt)
        print('M3_EMITTED_INTEGRATED_RUNTIME_PASS image=' + str(image), flush=True)
    except BaseException as failure:
        receipt.update(status='FAIL', error=str(failure), commands=runner.commands)
        runtime.write_json(output / 'FAILURE.json', receipt)
        raise


if __name__ == '__main__':
    main()
