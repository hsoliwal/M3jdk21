# SPDX-License-Identifier: Apache-2.0
# Adapted from the proved Guava source-custody helper.
import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=Path('.'))
    parser.add_argument('--jar', type=Path)
    args = parser.parse_args()
    root = args.root.resolve()
    spec = json.loads((Path(__file__).resolve().parent / 'source-bindings.json').read_text(encoding='utf-8'))
    seen = set()
    for row in spec['sources']:
        path = PurePosixPath(row['path'])
        if path.is_absolute() or '..' in path.parts or row['path'] in seen:
            raise SystemExit('invalid or duplicate selected source path')
        seen.add(row['path'])
        data = (root / row['path']).read_bytes()
        if len(data) != row['bytes'] or hashlib.sha256(data).hexdigest() != row['sha256']:
            raise SystemExit('source custody differs: ' + row['path'])
    if args.jar:
        data = args.jar.read_bytes()
        expected = spec['provider']
        if len(data) != expected['bytes'] or hashlib.sha256(data).hexdigest() != expected['sha256']:
            raise SystemExit('provider custody differs')
    print(json.dumps({'source_custody': 'PASS', 'sources': len(spec['sources']),
        'selectedBaseCommit': spec['baseCommit'], 'providerVerified': bool(args.jar),
        'scope': 'All selected source inputs before compilation/testing; bindings document is externally sealed.'}))


if __name__ == '__main__':
    main()
