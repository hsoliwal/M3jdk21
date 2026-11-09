#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0
"""Run existing current-source witnesses on stock Java21 with an explicit java.base patch.

This is not a built M3JDK21 image and does not replace the original verifier.
"""
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import shutil
import subprocess

ROOT = Path('/workspace/scratch/53bcd62c244c/M3JDK21')
WORK = Path(__file__).resolve().parent
JDK = Path('/tmp/m3jdk21-toolchain-20261020/jdk-21.0.12.1+1')
PREFIX = Path('m3/runtime-integration/progress-job-20261018')
ORIGINAL = ROOT / PREFIX / 'verify_progress_job.py'
spec = importlib.util.spec_from_file_location('original_progress_verifier', ORIGINAL)
original = importlib.util.module_from_spec(spec)
spec.loader.exec_module(original)
OUTPUT = WORK / 'stock-patch-current-source'
OUTPUT.mkdir(exist_ok=False)
receipt = {'schema': 'M3_CURRENT_PROGRESS_STOCK_PATCH_PROOF_V1', 'result': 'RUNNING',
           'runtime': 'STOCK_TEMURIN21_WITH_EXPLICIT_CURRENT_SOURCE_JAVA_BASE_PATCH',
           'actual_m3jdk21_image': 'UNAVAILABLE_NOT_TESTED', 'phase': 'STRING_UNCHANGED',
           'full_image_api_admission': 'PENDING', 'source_writes': 'ISOLATED_COPY_ONLY',
           'original_verifier_sha256': original.sha(ORIGINAL), 'commands': []}

def run(label, command, success=True, quiet=False):
    result = subprocess.run([str(x) for x in command], capture_output=True, text=True, timeout=120)
    stdout = OUTPUT / (label + '.stdout.log')
    stderr = OUTPUT / (label + '.stderr.log')
    stdout.write_text(result.stdout)
    stderr.write_text(result.stderr)
    receipt['commands'].append({'label': label, 'command': [str(x) for x in command],
        'exit': result.returncode, 'expected_success': success,
        'stdout_sha256': original.sha(stdout), 'stderr_sha256': original.sha(stderr)})
    if (result.returncode == 0) != success:
        raise RuntimeError(label + '\n' + result.stdout + result.stderr)
    if quiet and (result.stdout.strip() or result.stderr.strip()):
        raise RuntimeError('strict compilation emitted diagnostics: ' + label + '\n' + result.stdout + result.stderr)
    return result.stdout, result.stderr

try:
    review = json.loads((ROOT / PREFIX / 'DONOR-REVIEW.json').read_text())
    dependencies = list(review['current_owner_dependencies'])
    dependencies.append({'path': review['product_owner'], 'sha256': review['product_source_sha256']})
    extra = 'src/java.base/share/classes/java/util/concurrent/MonitoredRunnable.java'
    dependencies.append({'path': extra, 'sha256': original.sha(ROOT / extra),
                         'reason': 'existing M3MonitoredTemplateTest dependency'})
    source_root = OUTPUT / 'source'
    sources = []
    for row in dependencies:
        assert original.sha(ROOT / row['path']) == row['sha256'], row['path']
        destination = source_root / row['path']
        original.copy(ROOT / row['path'], destination)
        sources.append(destination)
    receipt['source_dependencies'] = dependencies
    for row in review['eclipse_release_inputs']['files']:
        assert original.sha(WORK / 'eclipse' / row['file']) == row['sha256'], row['file']
    receipt['eclipse_inputs'] = review['eclipse_release_inputs']['files']
    run('stock-javac-version', [JDK / 'bin/javac', '-version'])
    run('stock-java-version', [JDK / 'bin/java', '-version'])
    receipt['toolchain_sha256'] = {str(JDK / ('bin/' + tool)): original.sha(JDK / ('bin/' + tool))
                                   for tool in ['java', 'javac']}
    patch = OUTPUT / 'patch-classes'
    patch.mkdir()
    run('strict-current-source-api', [JDK / 'bin/javac', *original.JDK_FLAGS,
        '--patch-module', 'java.base=' + str(source_root / 'src/java.base/share/classes'),
        '-d', patch, *sources], quiet=True)
    clients = OUTPUT / 'client-classes'
    clients.mkdir()
    jars = [WORK / 'eclipse/org.eclipse.equinox.common-3.21.0.jar',
            WORK / 'eclipse/org.eclipse.osgi-3.24.300.jar']
    eclipse_cp = ':'.join(str(x) for x in jars)
    core = ROOT / original.JDK_TEST
    status = ROOT / PREFIX / 'src/test/java/EclipseStatusIdentity.java'
    monitored = ROOT / 'test/jdk/java/lang/Object/M3MonitoredTemplateTest.java'
    run('strict-existing-client-tests', [JDK / 'bin/javac', *original.CLIENT_FLAGS,
        '--patch-module', 'java.base=' + str(patch), '-classpath', eclipse_cp,
        '-d', clients, core, status, monitored], quiet=True)
    receipt['tests'] = [{'path': str(x.relative_to(ROOT)), 'sha256': original.sha(x)}
                        for x in [core, status, monitored]]
    old_source = ROOT / PREFIX / 'src/test/java/OldCallableClient.java'
    old = OUTPUT / 'frozen-old-client'
    recompiled = OUTPUT / 'recompiled-old-client'
    old.mkdir()
    recompiled.mkdir()
    run('stock-old-client-compile', [JDK / 'bin/javac', *original.CLIENT_FLAGS, '-d', old, old_source], quiet=True)
    frozen = original.class_tree(old)
    assert len(frozen) == 3
    run('patched-old-client-compile', [JDK / 'bin/javac', *original.CLIENT_FLAGS,
        '--patch-module', 'java.base=' + str(patch), '-d', recompiled, old_source], quiet=True)
    assert original.class_tree(recompiled) == frozen
    negative = OUTPUT / 'negative/ProgressJobRunnableConflict.java'
    original.copy(ROOT / PREFIX / 'src/test/resources/ProgressJobRunnableConflict.java.txt', negative)
    stdout, stderr = run('expected-runnable-source-conflict', [JDK / 'bin/javac', *original.CLIENT_FLAGS,
        '-XDrawDiagnostics', '--patch-module', 'java.base=' + str(patch), '-d', OUTPUT / 'negative-classes',
        negative], success=False)
    keys = re.findall(r'compiler\.err\.[a-zA-Z0-9_.]+', stderr)
    assert {'compiler.err.override.incompatible.ret', 'compiler.err.override.weaker.access'}.issubset(keys)
    assert not any('cant.resolve' in key for key in keys)
    receipt['negative_diagnostics'] = keys
    stdout, _ = run('stock-old-client', [JDK / 'bin/java', '-ea', '-classpath', old, 'OldCallableClient'])
    assert 'PASS old Callable' in stdout
    for mode, flags in [('mixed', []), ('interpreter', ['-Xint'])]:
        runtime = [JDK / 'bin/java', '-Xmx256m', '-ea', '-Xcheck:jni', *flags,
                   '--patch-module', 'java.base=' + str(patch)]
        for main, cp, marker in [('M3ProgressJob', str(clients), 'PASS ProgressJob checks='),
                                ('EclipseStatusIdentity', str(clients) + ':' + eclipse_cp,
                                 'PASS real Eclipse IStatus/Status/MultiStatus checks='),
                                ('OldCallableClient', str(old), 'PASS old Callable'),
                                ('M3MonitoredTemplateTest', str(clients), None)]:
            stdout, _ = run(mode + '-' + main, [*runtime, '-classpath', cp, main])
            if marker:
                assert marker in stdout, (mode, main)
    assert original.class_tree(old) == frozen
    receipt['patch_classes'] = original.class_tree(patch)
    receipt['client_classes'] = original.class_tree(clients)
    receipt['frozen_old_classes'] = frozen
    receipt['result'] = 'PASS_BOUNDED_STOCK_PATCH_ONLY'
except Exception as error:
    receipt['result'] = 'FAIL'
    receipt['failure'] = str(error)
    raise
finally:
    (OUTPUT / 'receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
print(receipt['result'])
