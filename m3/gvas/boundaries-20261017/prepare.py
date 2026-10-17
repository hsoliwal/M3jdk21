#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Prepare a native/Java crate for the unchanged Synexia snapshot recipes.
This script authors source-sealed resources, not OpenRewrite results. Only actual
successful Maven SDK output may be installed into product paths. All preimages
and candidate bytes are validated before serial, resumable installation.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

HERE = Path(__file__).resolve().parent
MODULE = Path('m3/gvas/boundaries-20261017/recipe')


def sha(data):
    return hashlib.sha256(data).hexdigest()


def blob(data):
    return hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()


def safe(root, name):
    path = root / name
    for parent in [path, *path.parents]:
        if parent == root:
            break
        if parent.is_symlink():
            raise ValueError('symlink in candidate path: ' + str(parent))
    result = path.resolve()
    if not result.is_relative_to(root):
        raise ValueError('candidate path escapes root: ' + name)
    return result


def prepare(root, baseline):
    rules = json.loads((HERE / 'changes.json').read_text())
    images = []
    for rule in rules['files']:
        original = (safe(baseline, rule['path']).read_bytes() if baseline else
                    subprocess.check_output(['git', 'show', rules['baseline'] + ':' + rule['path']], cwd=root))
        if sha(original) != rule['before_sha256'] or blob(original) != rule['before_git_blob']:
            raise ValueError('preimage mismatch: ' + rule['path'])
        text = original.decode('utf-8')
        for atom in rule['replacements']:
            if text.count(atom['before']) != 1:
                raise ValueError('ambiguous or missing atom: ' + rule['path'])
            text = text.replace(atom['before'], atom['after'])
        candidate = text.encode('utf-8')
        if sha(candidate) != rule['after_sha256'] or blob(candidate) != rule['after_git_blob']:
            raise ValueError('postimage mismatch: fix and reseal the recipe: ' + rule['path'])
        images.append((rule['kind'], rule['path'], original, candidate))
    for rule in rules['additions']:
        candidate = (HERE / rule['fixture']).read_bytes()
        if sha(candidate) != rule['after_sha256']:
            raise ValueError('new fixture/template drift: ' + rule['path'])
        images.append((rule['kind'], rule['path'], None, candidate))
    for kind in ('java', 'text'):
        crate = safe(root, str(MODULE / ('src/main/resources/com/synexia/rewrite/hash-pinned-' + kind) / rules['crate']))
        crate.mkdir(parents=True, exist_ok=True)
        rows = []
        for _, path, before, after in sorted(image for image in images if image[0] == kind):
            name = Path(path).name + '.txt'
            (crate / name).write_bytes(after)
            if before is not None:
                (crate / 'pre').mkdir(exist_ok=True)
                (crate / 'pre' / name).write_bytes(before)
            rows.append('\t'.join((path, 'ABSENT' if before is None else sha(before), sha(after), name)))
        (crate / 'manifest.tsv').write_bytes(('\n'.join(rows) + '\n').encode())
    test = safe(root, str(MODULE / 'src/test/java/com/synexia/rewrite/M3GvasBoundaryRecipeTest.java'))
    test.parent.mkdir(parents=True, exist_ok=True)
    test.write_bytes((HERE / 'M3GvasBoundaryRecipeTest.java').read_bytes())
    pom = safe(root, str(MODULE / 'pom.xml'))
    pom.write_bytes((HERE / 'pom.xml').read_bytes())
    return images


def install(root, sdk, images):
    ready = []
    for _, path, before, after in sorted(images, key=lambda item: item[1]):
        emitted = safe(sdk, path).read_bytes()
        if emitted != after:
            raise ValueError('actual SDK output differs from sealed candidate: ' + path)
        destination = safe(root, path)
        current = destination.read_bytes() if destination.exists() else None
        if current not in (before, after) or before is not None and current is None:
            raise ValueError('missing/drifting/occupied product path: ' + path)
        temporary = destination.with_name(destination.name + '.m3-tmp')
        if temporary.exists() or temporary.is_symlink():
            raise ValueError('temporary output path occupied: ' + str(temporary))
        ready.append((destination, emitted))
    for destination, emitted in ready:
        destination.parent.mkdir(parents=True, exist_ok=True)
        temporary = destination.with_name(destination.name + '.m3-tmp')
        with temporary.open('xb') as output:
            output.write(emitted)
        temporary.replace(destination)
        print('M3_GVAS_SDK_INSTALLED', destination.relative_to(root), sha(emitted), blob(emitted))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--root', type=Path, default=Path.cwd())
    parser.add_argument('--baseline-dir', type=Path)
    parser.add_argument('--install-sdk-output', type=Path)
    args = parser.parse_args()
    root = args.root.resolve()
    images = prepare(root, args.baseline_dir.resolve() if args.baseline_dir else None)
    if args.install_sdk_output:
        install(root, args.install_sdk_output.resolve(), images)
    else:
        print('M3_GVAS_TEMPLATES_READY files=' + str(len(images)))


if __name__ == '__main__':
    main()
