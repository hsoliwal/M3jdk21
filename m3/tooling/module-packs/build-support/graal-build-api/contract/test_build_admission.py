# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Check the builder's public source-admission refusal before any compiler runs."""
import argparse
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

ROOT = Path(__file__).resolve().parents[1]


def main(output):
    output = output.absolute()
    if output.exists():
        raise ValueError('output already exists')
    output.mkdir(parents=True)
    results = []
    mutations = [
        ('source-body', 'upstream/openjson/src/main/java/com/github/openjson/JSON.java',
         lambda value: value + b'\n// changed source\n', 'source checksum mismatch'),
        ('generated-version', 'generated-sources/src/main/java/org/graalvm/buildtools/VersionInfo.java',
         lambda value: value.replace(b'1.1.13', b'1.1.14'), 'generated VersionInfo differs'),
        ('source-lock', 'source-lock.json', lambda value: value + b' ', 'source lock checksum mismatch'),
        ('unregistered-source', 'upstream/openjson/src/main/java/Unregistered.java',
         lambda value: b'class Unregistered {}\n', 'source inventory mismatch'),
    ]
    for name, relative, mutate, diagnostic in mutations:
        with tempfile.TemporaryDirectory(prefix='graal-admission-') as temporary:
            root = Path(temporary) / 'input'
            shutil.copytree(ROOT, root, ignore=shutil.ignore_patterns('__pycache__'))
            target = root / relative
            target.write_bytes(mutate(target.read_bytes() if target.exists() else b''))
            destination = Path(temporary) / 'build-output'
            command = [sys.executable, str(root / 'build.py'), '--java-home', str(Path(temporary) / 'no-toolchain'),
                       '--output', str(destination)]
            completed = subprocess.run(command, capture_output=True, text=True)
            if completed.returncode == 0 or diagnostic not in completed.stderr or destination.exists():
                raise AssertionError(name + ': expected pre-toolchain refusal: ' + completed.stderr)
            (output / (name + '.log')).write_text(completed.stdout + completed.stderr)
            results.append({'case': name, 'exit': completed.returncode, 'diagnostic': diagnostic,
                            'output_created': False, 'compiler_available': False})
    (output / 'ADMISSION.json').write_text(json.dumps({'status': 'PASS', 'cases': results}, indent=2) + '\n')
    print(json.dumps({'status': 'PASS', 'cases': len(results)}))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    main(parser.parse_args().output)
