#!/usr/bin/env python3
"""Verify pinned inputs and bounded task syntax without compiling or executing recipes."""
import argparse
import hashlib
from pathlib import Path
import xml.etree.ElementTree as ET


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--corpus-root', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    task = Path(__file__).resolve().parent
    owner = task.parent.parent
    ET.parse(task / 'pom.xml')
    ET.parse(task / 'checkstyle.xml')
    crate = owner / 'src/main/resources/com/synexia/rewrite/hash-pinned-java/atom-comment-custody'
    target, before, after, filename = (crate / 'manifest.tsv').read_text().strip().split('\t')
    if target != 'src/main/java/com/m3/rewrite/atom/M3AtomizePureIntReturnRecipe.java':
        raise SystemExit('RECIPE_TARGET_SCOPE')
    if hashlib.sha256((crate / 'before.java.txt').read_bytes()).hexdigest() != before:
        raise SystemExit('RECIPE_PREIMAGE_DRIFT')
    if hashlib.sha256((crate / filename).read_bytes()).hexdigest() != after:
        raise SystemExit('RECIPE_POSTIMAGE_DRIFT')
    final_crate = owner / 'src/main/resources/com/synexia/rewrite/hash-pinned-java/atom-pattern-spectrum'
    for line in (final_crate / 'manifest.tsv').read_text().splitlines():
        target, before, after, filename = line.split('\t')
        if target not in ('src/main/java/com/m3/rewrite/atom/M3AtomizePureIntReturnRecipe.java',
                          'src/main/java/com/m3/rewrite/atom/M3PatternizePureIntAtomRecipe.java'):
            raise SystemExit('FINAL_RECIPE_SCOPE')
        if hashlib.sha256((final_crate / filename.replace('after-', 'before-')).read_bytes()).hexdigest() != before:
            raise SystemExit('FINAL_RECIPE_PREIMAGE_DRIFT')
        if hashlib.sha256((final_crate / filename).read_bytes()).hexdigest() != after:
            raise SystemExit('FINAL_RECIPE_POSTIMAGE_DRIFT')
    source_pins = [line.split('\t') for line in (task / 'SOURCE_PINS.tsv').read_text().splitlines()[1:]]
    repo = owner.parent.parent.parent
    for name, _before, expected in source_pins:
        actual = hashlib.sha256((repo / name).read_bytes()).hexdigest()
        if actual != expected:
            raise SystemExit('SOURCE_PIN_DRIFT:' + name)
    manifest = owner / 'src/test/resources/com/m3/rewrite/atom/spectrum/CORPUS.tsv'
    rows = [line.split('\t') for line in manifest.read_text().splitlines() if not line.startswith(('#', 'path\t'))]
    if len(rows) != 100 or len({row[0] for row in rows}) != 100:
        raise SystemExit('CORPUS_DENOMINATOR')
    total = 0
    for name, _category, size, blob, sha in rows:
        data = (args.corpus_root / name).read_bytes()
        if len(data) != int(size) or hashlib.sha256(data).hexdigest() != sha:
            raise SystemExit('CORPUS_SHA256_DRIFT:' + name)
        if hashlib.sha1(b'blob ' + str(len(data)).encode('ascii') + b'\0' + data).hexdigest() != blob:
            raise SystemExit('CORPUS_GIT_BLOB_DRIFT:' + name)
        total += len(data)
    paths = sorted(p for root in (owner / 'src/main/java/com/m3/rewrite/atom',
                                 owner / 'src/test/java/com/m3/rewrite/atom') for p in root.glob('*.java'))
    for path in paths:
        data = path.read_bytes()
        if not data.endswith(b'\n') or b'\r' in data or b'\t' in data:
            raise SystemExit('SOURCE_WHITESPACE:' + str(path))
        if b'package com.m3.rewrite.atom;' not in data:
            raise SystemExit('SOURCE_NAMESPACE:' + str(path))
    args.out.mkdir(parents=True, exist_ok=True)
    receipt = ['path\tsha256\n']
    for path in paths:
        receipt.append(str(path.relative_to(repo)) + '\t' + hashlib.sha256(path.read_bytes()).hexdigest() + '\n')
    (args.out / 'SOURCE_SHA256.tsv').write_text(''.join(receipt))
    print('STATIC_PIN_CHECK_PASS', len(paths), 'Java files;', len(rows), 'real corpus files;', total, 'bytes')
    print('XML_PARSE_PASS; source syntax still requires Checkstyle and javac')


if __name__ == '__main__':
    main()
