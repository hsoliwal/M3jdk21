#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Compile the integrated utility owners in isolation and execute contract tests.

This is target proof tooling, not a JDK runtime dependency or a transformation
recipe. Source files must first be materialized by the qualified Synexia recipe.
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
    'src/java.base/share/classes/java/io/IOUtils.java',
    'src/java.base/share/classes/java/util/stream/Streams.java',
    'src/java.base/share/classes/java/lang/reflect/Reflection.java',
    'src/java.base/share/classes/java/util/Strings.java',
)
CONTRACT_TESTS = (
    'test/jdk/java/io/IOUtils/IOUtilsTest.java',
    'test/jdk/java/util/stream/Streams/StreamsTest.java',
    'test/jdk/java/lang/reflect/Reflection/ReflectionTest.java',
)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home', default=os.environ.get('M3_UTILITY_JAVA_HOME'))
    parser.add_argument('--repository', type=Path, default=Path(__file__).resolve().parents[3])
    parser.add_argument('--output', type=Path, help='optional JSON execution receipt')
    parser.add_argument('--temp-directory', default=os.environ.get('M3_UTILITY_TMPDIR'))
    options = parser.parse_args()
    if not options.java_home:
        parser.error('--java-home or M3_UTILITY_JAVA_HOME must identify Java 21')
    jdk = Path(options.java_home).resolve()
    repo = options.repository.resolve()
    receipt = {
        'scope': 'ISOLATED_JAVA_21_PATCH_MODULE_CONTRACTS',
        'full_fork_image': 'PENDING',
        'commands': [],
        'source_sha256': {path: hashlib.sha256((repo/path).read_bytes()).hexdigest()
                          for path in PRODUCT_SOURCES + CONTRACT_TESTS},
    }
    def run(arguments):
        command = [str(x) for x in arguments]
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
            raise RuntimeError('Java 21 is required for this receiver qualification')
        with tempfile.TemporaryDirectory(prefix='m3-integrated-utilities-', dir=options.temp_directory) as temporary:
            work = Path(temporary)
            patch = work/'source'
            classes = work/'classes'
            tests = work/'tests'
            classes.mkdir()
            tests.mkdir()
            isolated = []
            for relative in PRODUCT_SOURCES:
                package_path = Path(relative).relative_to('src/java.base/share/classes')
                destination = patch/package_path
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(repo/relative, destination)
                isolated.append(destination)
            run([jdk/'bin/javac', '-J-Xmx256m', '--patch-module', 'java.base='+str(patch),
                 '-d', classes, *isolated])
            run([jdk/'bin/javac', '-J-Xmx256m', '--patch-module', 'java.base='+str(classes),
                 '-d', tests, *[repo/path for path in CONTRACT_TESTS]])
            for mode in [[], ['-Xint']]:
                for source in CONTRACT_TESTS:
                    run([jdk/'bin/java', '-Xmx256m', *mode,
                         '--patch-module', 'java.base='+str(classes), '-cp', tests,
                         Path(source).stem])
        receipt['status'] = 'PASSED'
    except Exception as failure:
        receipt['status'] = 'FAILED'
        receipt['failure'] = str(failure)
        raise
    finally:
        if options.output:
            options.output.parent.mkdir(parents=True, exist_ok=True)
            options.output.write_text(json.dumps(receipt, indent=2)+'\n')
    print('Integrated utility contracts passed in mixed and interpreter modes.')

if __name__ == '__main__':
    main()
