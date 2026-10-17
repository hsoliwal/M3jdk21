#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Prepare sealed recipe resources; install ONLY verified actual Maven SDK outputs.

This is authoring/receiving glue, not a replacement OpenRewrite engine. Existing
M3HashPinnedJavaSnapshotRecipe owns Java LST generation. Files are processed
serially, all preconditions are checked before any product output is installed.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

CAPSULE = Path(__file__).resolve().parent
PROOF = 'm3/collections/wrappers/src/test/java/com/m3/collections/M3WrapperBoundaryProof.java'
RECIPE = Path('m3/collections/wrappers/recipe')

def sha(data):
    return hashlib.sha256(data).hexdigest()

def git_blob(data):
    return hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()

def safe(root, path):
    lexical = root / path
    for parent in [lexical, *lexical.parents]:
        if parent == root:
            break
        if parent.is_symlink():
            raise ValueError('symlink in canonical path: ' + str(parent))
    result = lexical.resolve()
    if not result.is_relative_to(root):
        raise ValueError('path escapes root: ' + path)
    return result

def prepare(root, baseline_dir=None):
    config = json.loads((CAPSULE / 'changes.json').read_text())
    images = []
    for rule in config['files']:
        original = (safe(baseline_dir, rule['path']).read_bytes() if baseline_dir else
                    subprocess.check_output(['git', 'show', config['baseline'] + ':' + rule['path']], cwd=root))
        if sha(original) != rule['before_sha256'] or git_blob(original) != rule['before_git_blob']:
            raise ValueError('unqualified preimage: ' + rule['path'])
        post = original.decode('utf-8')
        for atom in rule['replacements']:
            if post.count(atom['before']) != 1:
                raise ValueError('behavioral atom is missing or ambiguous: ' + rule['path'])
            post = post.replace(atom['before'], atom['after'])
        candidate = post.encode('utf-8')
        if sha(candidate) != rule['after_sha256'] or git_blob(candidate) != rule['after_git_blob']:
            raise ValueError('postimage changed; repair and reseal recipe: ' + rule['path'])
        images.append((rule['path'], original, candidate))
    images.append((PROOF, None, (CAPSULE / 'M3WrapperBoundaryProof.java').read_bytes()))
    crate = safe(root, str(RECIPE / 'src/main/resources/com/synexia/rewrite/hash-pinned-java' / config['crate']))
    manifest = []
    for path, original, candidate in images:
        template = Path(path).name + '.txt'
        crate.mkdir(parents=True, exist_ok=True)
        (crate / template).write_bytes(candidate)
        if original is not None:
            (crate / 'pre').mkdir(exist_ok=True)
            (crate / 'pre' / template).write_bytes(original)
        manifest.append('\t'.join((path, 'ABSENT' if original is None else sha(original), sha(candidate), template)))
    (crate / 'manifest.tsv').write_text('\n'.join(manifest) + '\n')
    test = safe(root, str(RECIPE / 'src/test/java/com/synexia/rewrite/M3WrapperBoundaryRecipeTest.java'))
    test.parent.mkdir(parents=True, exist_ok=True)
    test.write_bytes((CAPSULE / 'M3WrapperBoundaryRecipeTest.java').read_bytes())
    definition = safe(root, str(RECIPE / 'src/main/resources/META-INF/rewrite/m3-wrapper-boundaries-20261017.yml'))
    definition.parent.mkdir(parents=True, exist_ok=True)
    definition.write_text('''# SPDX-License-Identifier: Apache-2.0
type: specs.openrewrite.org/v1beta/recipe
name: com.synexia.m3.WrapperBoundaries20261017
displayName: Refine existing mapped and filtered/sorted collection composition
description: Source-sealed sequential cursor and lazy flat-map boundaries; unchanged public interfaces.
recipeList:
  - com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe:
      crateName: m3-wrapper-boundaries-20261017
''')
    return images

def install(root, sdk_output, images):
    checked = []
    for path, original, candidate in images:
        emitted = safe(sdk_output, path).read_bytes()
        if emitted != candidate:
            raise ValueError('actual SDK output disagrees with reviewed template: ' + path)
        target = safe(root, path)
        existing = target.read_bytes() if target.exists() else None
        if existing not in (original, candidate):
            raise ValueError('target drift or occupied new target: ' + path)
        if original is not None and existing is None:
            raise ValueError('missing original target: ' + path)
        temporary = target.with_name(target.name + '.m3-tmp')
        if temporary.exists() or temporary.is_symlink():
            raise ValueError('temporary path occupied: ' + str(temporary))
        checked.append((target, emitted))
    for target, emitted in checked:
        target.parent.mkdir(parents=True, exist_ok=True)
        temporary = target.with_name(target.name + '.m3-tmp')
        if temporary.exists():
            raise ValueError('temporary path occupied: ' + str(temporary))
        temporary.write_bytes(emitted)
        temporary.replace(target)
        print('M3_BOUNDARY_INSTALLED', target.relative_to(root), sha(emitted), git_blob(emitted))

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
        print('M3_BOUNDARY_TEMPLATES_READY files=' + str(len(images)))

if __name__ == '__main__':
    main()
