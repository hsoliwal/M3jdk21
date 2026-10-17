#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Source-sealed Java21 and actual checked JNI receiver proof; not jtreg/full JDK."""
from __future__ import annotations
import hashlib
import os
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[3]
PACKET = ROOT / 'm3/collections/recipe/mask-window-jni-20261017'
SOURCE = ROOT / 'm3/collections/src/main/java/com/m3/collections'
NATIVE = ROOT / 'm3/collections/src/main/native/collections/m3_index_mask_compactor.c'
TEST = ROOT / 'm3/collections/src/test/java/com/m3/collections/M3IndexMaskWindowLongContract.java'
COUNTER = 0

def run(command: list[str], **kw: object) -> None:
    global COUNTER
    subprocess.run(command, check=True, **kw)
    COUNTER += 1

def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main() -> None:
    manifest = PACKET / 'manifest.tsv'
    if not manifest.is_file():
        raise SystemExit('missing exact M3 receiver manifest')
    checked = set()
    rows = [line.split('\t') for line in manifest.read_text().splitlines()
            if line.strip() and not line.startswith('#')]
    if len(rows) != 2:
        raise SystemExit('receiver needs exactly Java and C image rows')
    for row in rows:
        if len(row) != 4:
            raise SystemExit('invalid receiver manifest row')
        path, expected_before, expected_after, donor_blob = row
        target = ROOT / path
        if target.resolve() not in {(SOURCE / 'M3IndexMaskWindow.java').resolve(), NATIVE.resolve()}:
            raise SystemExit('unapproved receiver target '+path)
        if path in checked or not target.is_file():
            raise SystemExit('duplicate or missing exact target '+path)
        checked.add(path)
        if sha(target) != expected_after:
            raise SystemExit('source drift from reviewed postimage '+path)
        if not expected_before or not donor_blob:
            raise SystemExit('missing before/donor custody '+path)
        print('SEALED', path, expected_after)
    jdk = Path(os.environ.get('JAVA_HOME', '/usr/lib/jvm/java-21-openjdk-amd64'))
    javac = jdk / 'bin/javac'
    java = jdk / 'bin/java'
    if not javac.is_file() or not java.is_file():
        raise SystemExit('requires real JDK21')
    includes = [jdk / 'include', jdk / 'include/linux']
    with tempfile.TemporaryDirectory(prefix='m3-mask-window-') as tmp:
        out = Path(tmp)
        classes = out / 'classes'; classes.mkdir()
        headers = out / 'headers'; headers.mkdir()
        run([str(javac), '--release','21','-proc:none','-Xlint:all','-Werror',
             '-h',str(headers),'-d',str(classes),
             str(SOURCE/'M3IndexMaskCompactor.java'),
             str(SOURCE/'M3IndexMaskWindow.java'), str(TEST)])
        cflags = ['-std=c17','-Wall','-Wextra','-Werror','-fPIC','-shared']
        for folder in includes:
            if not folder.is_dir(): raise SystemExit('missing JNI header directory')
            cflags.extend(['-I',str(folder)])
        binary = out / 'libm3_collections_mask.so'
        run([os.environ.get('CC','cc'),*cflags,str(NATIVE),'-o',str(binary)])
        run([str(java),'-Xmx128m','-cp',str(classes),
             'com.m3.collections.M3IndexMaskWindowLongContract','--java'])
        run([str(java),'-Xmx128m','-Xcheck:jni','-Djava.library.path='+str(out),
             '-cp',str(classes),'com.m3.collections.M3IndexMaskWindowLongContract'])
    print('PASS M3 mask-window Java21 checked JNI commands=',COUNTER,
          ' selected-source only; full JDK/jtreg/HotSpot gates pending')

if __name__=='__main__': main()
