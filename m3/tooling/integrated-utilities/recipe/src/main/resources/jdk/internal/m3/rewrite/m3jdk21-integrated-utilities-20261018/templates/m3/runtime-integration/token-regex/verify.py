#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Compile the integrated token regex utilities and execute their contracts.

This is receiver proof tooling, not a runtime dependency or a source rewrite.
The canonical Synexia Maven recipe must materialize receiver sources first.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import tempfile

PRODUCT_SOURCES = (
    'src/java.base/share/classes/java/util/regex/TokenPattern.java',
    'src/java.base/share/classes/java/util/regex/TokenMatcher.java',
)
CONTRACT_TEST = 'test/jdk/java/util/regex/TokenPattern/TokenPatternTest.java'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home', default=os.environ.get('M3_TOKEN_JAVA_HOME'))
    parser.add_argument('--repository', type=Path, default=Path(__file__).resolve().parents[3])
    parser.add_argument('--output', type=Path, help='optional source-bound JSON execution receipt')
    parser.add_argument('--temp-directory', default=os.environ.get('M3_TOKEN_TMPDIR'))
    options = parser.parse_args()
    if not options.java_home:
        parser.error('--java-home or M3_TOKEN_JAVA_HOME must identify Java 21')
    jdk = Path(options.java_home).resolve()
    repo = options.repository.resolve()
    receipt = {
        'schema': 'M3_TOKEN_REGEX_QUALIFICATION_V1',
        'scope': 'ISOLATED_JAVA_21_PATCH_MODULE_CONTRACTS',
        'full_fork_image': 'PENDING',
        'commands': [],
        'source_sha256': {path: hashlib.sha256((repo/path).read_bytes()).hexdigest()
                          for path in (*PRODUCT_SOURCES, CONTRACT_TEST)},
    }

    def run(arguments):
        command = [str(value) for value in arguments]
        completed = subprocess.run(command, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        receipt['commands'].append({'command': command, 'exit_code': completed.returncode,
                                    'output': completed.stdout})
        print(completed.stdout, end='', flush=True)
        if completed.returncode:
            raise RuntimeError('command failed: ' + ' '.join(command))
        return completed.stdout

    try:
        version = run([jdk/'bin/java', '-version'])
        if not ('version "21.' in version or 'version "21"' in version):
            raise RuntimeError('Java 21 is required for this qualification')
        with tempfile.TemporaryDirectory(prefix='m3-token-regex-', dir=options.temp_directory) as temporary:
            work = Path(temporary)
            patch, classes, tests = work/'source', work/'classes', work/'tests'
            classes.mkdir()
            tests.mkdir()
            sources = []
            for relative in PRODUCT_SOURCES:
                destination = patch/Path(relative).relative_to('src/java.base/share/classes')
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(repo/relative, destination)
                sources.append(destination)
            run([jdk/'bin/javac', '-J-Xmx256m', '-Xlint:all', '-Werror',
                 '--patch-module', 'java.base='+str(patch), '-d', classes, *sources])
            run([jdk/'bin/javac', '-J-Xmx256m', '-Xlint:all', '-Werror',
                 '--patch-module', 'java.base='+str(classes), '-d', tests, repo/CONTRACT_TEST])
            run([jdk/'bin/javadoc', '-J-Xmx256m', '-quiet', '-Werror', '-Xdoclint:all,-missing',
                 '-tag', 'implNote:a:Implementation Note:',
                 '--patch-module', 'java.base='+str(patch), '-d', work/'docs', *sources])
            for mode in [[], ['-Xint']]:
                run([jdk/'bin/java', '-Xmx256m', *mode,
                     '--patch-module', 'java.base='+str(classes), '-cp', tests, 'TokenPatternTest'])
        receipt['status'] = 'PASSED'
    except Exception as failure:
        receipt['status'] = 'FAILED'
        receipt['failure'] = str(failure)
        raise
    finally:
        if options.output:
            options.output.parent.mkdir(parents=True, exist_ok=True)
            options.output.write_text(json.dumps(receipt, indent=2)+'\n')
    print('Token regex contracts passed in mixed and interpreter modes.')


if __name__ == '__main__':
    main()
