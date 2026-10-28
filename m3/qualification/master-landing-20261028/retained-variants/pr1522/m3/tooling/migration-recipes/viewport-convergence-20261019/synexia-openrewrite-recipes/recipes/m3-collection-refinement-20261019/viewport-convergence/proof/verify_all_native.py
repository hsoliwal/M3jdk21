#!/usr/bin/env python3
"""Run the existing collection JUnit suite with its declared native gates enabled.

SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
SPDX-License-Identifier: Apache-2.0

Reuses the receiving verifier's command/receipt owner unchanged. This is a
separate runtime lane after that verifier has compiled the complete default-POM
module. No source, POM, test, library, or previous receipt is overwritten.
"""
import argparse
from datetime import datetime, timezone
import importlib.util
import json
import os
from pathlib import Path
import re
import sys


EXTRA_LIBRARIES = (
    ('libm3_collections_address.so', (('m3_long_address_kernel.c', 'M3LongAddressKernel'),)),
    ('libm3_collections_long_path_masks.so', (('m3_long_index_path_masks_jni.c', 'M3LongIndexPathMasksJNI'),)),
    ('libm3_collections_span.so', (('m3_span_index_jni.c', 'M3SpanIndex'),)),
    ('libm3_collections_statehist.so', (('m3_state_mask_histogram.c', 'M3StateMaskHistogram'),)),
    ('libm3rankbatch.so', (('m3_long_rank_batch_jni.c', 'M3LongRankBatchNative'),)),
    ('libm3signalorder.so', (('m3_signal_order_jni.c', 'M3SignalOrderNative'),)),
    ('libm3_residency_state_mask.so', (('m3_residency_state_mask.c', 'M3ResidencyStateMaskIndex'),)),
    ('libm3bitlane.so', (('segmented_bit_lane.c', 'M3BitLane28'),)),
)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--verifier', type=Path, required=True)
    parser.add_argument('--compiled-root', type=Path, required=True)
    parser.add_argument('--java-home', type=Path, required=True)
    parser.add_argument('--junit-console', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--timeout-seconds', type=float, default=900,
        help='Same default as the existing complete-module verifier; each command timeout')
    args = parser.parse_args()
    if args.timeout_seconds <= 0:
        parser.error('--timeout-seconds must be positive')
    verifier = args.verifier.resolve(strict=True)
    sys.dont_write_bytecode = True
    spec = importlib.util.spec_from_file_location('m3_existing_verifier', verifier)
    runner = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(runner)
    compiled = args.compiled_root.resolve(strict=True)
    prior = json.loads((compiled / 'receipt.json').read_text())
    runner.require(prior['status'] == 'PASS', 'prior complete module verification must pass')
    output = args.output.resolve(strict=False)
    runner.reject_symlink_ancestors(output)
    runner.require(not output.is_relative_to(compiled), 'new lane must be disjoint from earlier evidence')
    output.mkdir(parents=True, exist_ok=False)
    jdk = args.java_home.resolve(strict=True)
    console = args.junit_console.resolve(strict=True)
    env = dict(os.environ, JAVA_HOME=str(jdk), LC_ALL='C', TZ='UTC')
    env['PATH'] = str(jdk / 'bin') + os.pathsep + env.get('PATH', '')
    check = runner.Verification(output, env, args.timeout_seconds)
    receipt = check.receipt
    receipt.update(scope='Existing JUnit all-native availability and mask-span gates; '
        'complete default-POM JUnit suite on previously compiled classes; Linux C17 fatal UBSan',
        not_run_by_this_tool=['source transformation', 'Maven recompilation', 'performance',
            'installed M3JDK21 image', 'jtreg/JCK', 'Android/iOS',
            'M3MaskViewportNative host implementation'])
    try:
        for variable in ('JAVA_TOOL_OPTIONS', 'JDK_JAVA_OPTIONS', '_JAVA_OPTIONS', 'CLASSPATH'):
            runner.require(not env.get(variable), 'implicit Java injection: ' + variable)
        runner.require(runner.identity(console)['sha256'] == runner.JUNIT_SHA256, 'JUnit console pin')
        roots = {name: compiled / name for name in ('classes', 'test-classes', 'headers')}
        native_sources = compiled / 'module/src/main/native/collections'
        roots['native_sources'] = native_sources
        frozen = {name: runner.inventory(path) for name, path in roots.items()}
        receipt['inputs'] = frozen
        receipt['prior_verification'] = runner.identity(compiled / 'receipt.json')
        receipt['tools'] = {str(p): runner.identity(p) for p in
            (Path(__file__).resolve(), verifier, jdk / 'bin/java', jdk / 'release', console,
             runner.resolve_tool('gcc'), runner.resolve_tool('nm'))}
        native = output / 'native'; native.mkdir()
        headers = roots['headers']
        flags = [runner.resolve_tool('gcc'), '-std=c17', '-O2', '-Wall', '-Wextra', '-Werror',
            '-fPIC', '-I', jdk / 'include', '-I', jdk / 'include/linux', '-I', headers,
            '-fsanitize=undefined', '-fno-sanitize-recover=all']
        receipt['native_libraries'] = {}
        for name, owners in (*runner.NATIVE_LIBRARIES, *EXTRA_LIBRARIES):
            sources, declarations = [], []
            for filename, owner in owners:
                source = native_sources / filename
                header = headers / ('com_m3_collections_' + owner + '.h')
                check.run('syntax-' + owner, flags + ['-include', header, '-fsyntax-only', source], output)
                sources.append(source); declarations.append(header)
            library = native / name
            includes = [arg for header in declarations for arg in ('-include', header)]
            check.run('build-' + name, flags + includes + ['-shared', *sources, '-o', library], output)
            symbols = check.run('exports-' + name, ['nm', '-D', '--defined-only', library], output)
            actual = sorted(line.split()[-1] for line in symbols.splitlines()
                if line.split() and line.split()[-1].startswith('Java_'))
            expected = sorted({symbol for header in declarations for symbol in
                re.findall(r'\b(Java_[A-Za-z0-9_]+)\s*\(', header.read_text())})
            runner.require(bool(expected) and actual == expected, 'JNI export mismatch: ' + name)
            receipt['native_libraries'][name] = {'sources': {p.name: runner.identity(p) for p in sources},
                'headers': {p.name: runner.identity(p) for p in declarations},
                'library': runner.identity(library), 'exports': actual}
        reports = output / 'junit-reports'
        cp = os.pathsep.join(str(roots[x]) for x in ('classes', 'test-classes'))
        observed = check.run('junit-all-native', [jdk / 'bin/java', '-Xcheck:jni',
            '-Djava.library.path=' + str(native), '-Dm3.mask.spans.native.required=true',
            '-Dm3.collections.all.native.required=true',
            '-Dm3.collections.rank.native=true', '-Dm3.collections.signal.native=true',
            '-jar', console, 'execute',
            '--class-path', cp, '--scan-class-path', '--fail-if-no-tests', '--disable-banner',
            '--disable-ansi-colors', '--details=summary', '--reports-dir', reports], output)
        receipt['junit_tests'] = runner.test_reports(reports)
        runner.require('WARNING in native method' not in observed and 'FATAL ERROR' not in observed,
            'checked JNI reported a native warning or fatal error')
        runner.require(re.search(r'\[\s*0 tests aborted\s*\]', observed) is not None,
            'all-native run retained assumption aborts')
        runner.require({name: runner.inventory(path) for name, path in roots.items()} == frozen,
            'input source, header, or compiled-class drift')
        receipt['source_class_header_freshness'] = 'PASS'
        receipt['status'] = 'PASS'
    except BaseException as failure:
        receipt['status'] = 'FAIL'
        receipt['error'] = type(failure).__name__ + ': ' + str(failure)
        raise
    finally:
        receipt['finished_utc'] = datetime.now(timezone.utc).isoformat()
        check.save()


if __name__ == '__main__':
    main()
