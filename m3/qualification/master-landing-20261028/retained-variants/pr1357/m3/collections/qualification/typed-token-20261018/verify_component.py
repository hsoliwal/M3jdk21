#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Typed-token continuation of the existing token-convergence component runner; host proof only."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--target', type=Path, required=True)
    parser.add_argument('--baseline-source', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--jdk', type=Path, required=True)
    args = parser.parse_args()
    target, baseline, output, jdk = (p.resolve() for p in (args.target, args.baseline_source, args.output, args.jdk))
    output.mkdir(parents=True, exist_ok=False)
    here = Path(__file__).resolve().parent
    classes, old, tests, headers, old_headers = (output / n for n in ('classes', 'baseline', 'tests', 'headers', 'baseline-headers'))
    for folder in (classes, old, tests, headers, old_headers): folder.mkdir()
    source = target / 'src/java.base/share/classes/jdk/internal/mindex'
    native = target / 'src/java.base/share/native/libjava/M3RegexDpPlan.c'
    test = target / 'test/jdk/jdk/internal/mindex'
    inputs = [baseline, source / 'M3RegexDpPlan.java', source / 'M3TokenRegexPlan.java', native,
              test / 'M3TokenRegexPlanContract.java', test / 'M3TokenCompilerLexicalContract.java',
              here / 'src/test/java/M3CharacterPlanPreservation.java', here / 'src/test/java/M3TokenResourceProbe.java',
              here / 'src/test/native/libM3TokenPlanProof.c']
    (output / 'INPUTS.json').write_text(json.dumps({str(p): hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs}, indent=2) + '\n')
    commands = []

    def call(name, *argv):
        argv = list(map(str, argv)); commands.append({'stage': name, 'argv': argv})
        print('QUALIFY', name, flush=True)
        with (output / (name + '.log')).open('w') as log:
            with subprocess.Popen(argv, stdout=log, stderr=subprocess.STDOUT,
                                  env={**os.environ, 'UBSAN_OPTIONS': 'halt_on_error=1:print_stacktrace=1'}) as process:
                _, status, usage = os.wait4(process.pid, 0)
                process.returncode = os.waitstatus_to_exitcode(status)
                code = process.returncode
        commands[-1].update(exit_code=code, max_rss_kib=usage.ru_maxrss, user_seconds=usage.ru_utime, system_seconds=usage.ru_stime)
        (output / 'COMMANDS.json').write_text(json.dumps(commands, indent=2) + '\n')
        if code: raise RuntimeError(name + ' failed; raw log retained')

    javac, java, javap = (jdk / 'bin' / n for n in ('javac', 'java', 'javap'))
    call('01-baseline-compile', javac, '-Xlint:all', '-Werror', '-d', old, '-h', old_headers, baseline)
    call('02-candidate-compile', javac, '-Xlint:all', '-Werror', '-d', classes, '-h', headers, inputs[1], inputs[2])
    patch = ['--patch-module', 'java.base=' + str(classes)]
    exports = ['--add-exports', 'java.base/jdk.internal.mindex=ALL-UNNAMED']
    compiler = ['--add-exports', 'jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED',
                '--add-exports', 'jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED']
    call('03-tests-compile', javac, '-Xlint:all', '-Werror', *patch, *exports, *compiler, '-d', tests, *inputs[4:8])
    flags = ['-std=c17', '-Wall', '-Wextra', '-Werror', '-I', headers, '-I', jdk / 'include', '-I', jdk / 'include/linux']
    call('04-native-lint', 'gcc', *flags, '-fsyntax-only', native, inputs[8])
    library = output / 'libM3TokenPlanProof.so'
    call('05-native-link', 'gcc', *flags, '-O2', '-fPIC', '-shared', '-fsanitize=undefined', '-fno-sanitize-recover=undefined', native, inputs[8], '-o', library)
    call('06-symbols', 'nm', '-D', '--defined-only', library)
    run = [java, *patch, *exports, '-cp', tests]
    call('07-baseline-character', java, '--patch-module', 'java.base=' + str(old), *exports, '-cp', tests, 'M3CharacterPlanPreservation')
    call('08-candidate-character', *run, 'M3CharacterPlanPreservation')
    if (output / '07-baseline-character.log').read_bytes() != (output / '08-candidate-character.log').read_bytes(): raise AssertionError('character observation drift')
    call('09-java-token', *run, 'M3TokenRegexPlanContract')
    call('10-javac-lexical', *run, *compiler, 'M3TokenCompilerLexicalContract')
    call('11-native-token', *run, '-Xcheck:jni', 'M3TokenRegexPlanContract', library)
    call('12-native-interpreter', *run, '-Xcheck:jni', '-Xint', 'M3TokenRegexPlanContract', library)
    for name in ('M3RegexDpPlan', 'M3RegexDpPlan$Effect'):
        short = name.replace('$', '-')
        for variant, root in (('old', old), ('new', classes)):
            call('api-' + variant + '-' + short, javap, '-public', '-s', root / ('jdk/internal/mindex/' + name + '.class'))
        if (output / ('api-old-' + short + '.log')).read_bytes() != (output / ('api-new-' + short + '.log')).read_bytes(): raise AssertionError('API drift')
    header = 'jdk_internal_mindex_M3RegexDpPlan.h'
    if (headers / header).read_bytes() != (old_headers / header).read_bytes(): raise AssertionError('JNI header drift')
    for trial in range(3): call('resource-' + str(trial), *run, 'M3TokenResourceProbe')
    print('TYPED_TOKEN_COMPONENT_PASS; host patch-module qualification, not full product acceptance')


if __name__ == '__main__': main()
