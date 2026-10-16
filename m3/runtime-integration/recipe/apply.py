#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Apply a complete reviewed runtime patch; refuse mixed or drifted states."""
import argparse,hashlib,json,subprocess
from pathlib import Path,PurePosixPath
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
def digest(data): return hashlib.sha256(data).hexdigest()

# Exact four-owner JNI modified-UTF8 source receipt: Synexia #11288,
# M3JDK21 commit 56d29d32b128de0d998e933f957e090383e8d2dc.
# Historical runtime.patch bytes are immutable and cannot replace these owners.
MUTF8_FANIN = {
    "src/hotspot/share/classfile/javaClasses.cpp": (
        "10816a3adb62ae3dd5b7fdefb79a43947637fb90924298ace56c8320ea4d60cb",
        (),
        "933764d74cca6a40dc01fef1798c38d71a4ac0b754e5b17d9ed42d2a05bc68f6",
    ),
    "src/hotspot/share/classfile/javaClasses.hpp": (
        "36a6ae73f908a42bd565ea8caca830f7de63e423092c92141f0fd4110dc75df3",
        (),
        "d20fa631a2df68353d7e0645690fbb36dd4a1d38c8ce99379596ded1aa912a59",
    ),
    "src/hotspot/share/classfile/javaClasses.inline.hpp": (
        "18edfbc5b4ae95130b6fcfe19fd9927abd0e4cc5504129d9a20b025b10f0b7a1",
        (),
        "bbe505ccc6b5ab1482f0e0235284d625f0cc75181a7b0d418f2fb3c722d30cdf",
    ),
    "src/java.base/share/classes/java/lang/M3StringFacts.java": (
        "c834e63534db855f06e99bbfdf45caf0ecf36b88f5760bfe7fdaa8bcc355509f",
        ("c834e63534db855f06e99bbfdf45caf0ecf36b88f5760bfe7fdaa8bcc355509f",
         "f1cc0ec9d4fd0bbc15d9c684e80b9907b60321cb15ad4fd7fa39f3cf973fc4cd",),
        "2152c154d3cdece330667ee09675fd21d86f3b483a23a3156b8a3626c3f9f420",
    ),
}

def verify_mutf8_fanin(manifest, observed=None):
    advanced = []
    for name, (old_postimage, legacy, admitted) in MUTF8_FANIN.items():
        entry = manifest["files"].get(name)
        if (entry is None or entry.get("after") != old_postimage
                or entry.get("superseded") != [*legacy, admitted]):
            raise ValueError("modified UTF-8 recipe seal drift: " + name)
        if observed is None:
            continue
        actual = observed.get(name)
        if actual not in {entry.get("before"), old_postimage, *entry["superseded"]}:
            raise ValueError("modified UTF-8 source drift: " + name)
        advanced.append(actual == admitted)
    if advanced and any(advanced) and not all(advanced):
        raise ValueError("partial modified UTF-8 native/Java owner fan-in")

def apply(target,reverse=False,check=False):
    target=target.resolve()
    manifest=json.loads((HERE/'manifest.json').read_text())
    verify_mutf8_fanin(manifest)
    patch=HERE/'runtime.patch'
    if digest(patch.read_bytes())!=manifest['patch_sha256']:raise ValueError('patch drift')
    states=set()
    observed={}
    for name,hashes in manifest['files'].items():
        safe=PurePosixPath(name)
        if safe.is_absolute() or '..' in safe.parts or safe.parts[0]!='src':raise ValueError('unsafe path')
        p=target/name
        if p.is_symlink() or target not in p.resolve().parents:raise ValueError('symlink path')
        actual=digest(p.read_bytes()) if p.exists() else None
        observed[name]=actual
        if actual==hashes['before']:states.add('before')
        elif actual==hashes['after']:states.add('after')
        elif actual in hashes.get('superseded',[]):states.add('superseded')
        else:raise ValueError('source drift: '+name)
    verify_mutf8_fanin(manifest,observed)
    if states=={'before'}:current='before'
    elif states and states.issubset({'after','superseded'}):
        current='superseded' if 'superseded' in states else 'after'
    else:raise ValueError('mixed runtime patch state')
    if current=='superseded':return current
    desired='before' if reverse else 'after'
    if current==desired or check:return current
    cmd=['git','-c','core.autocrlf=false','apply']+(['--reverse'] if reverse else [])
    subprocess.run(cmd+['--check',str(patch)],cwd=target,check=True)
    # git apply validates all hunks before writing and refuses partial patches.
    subprocess.run(cmd+[str(patch)],cwd=target,check=True)
    for name,hashes in manifest['files'].items():
        if (digest((target/name).read_bytes()) if (target/name).exists() else None)!=hashes[desired]:raise ValueError('postimage mismatch: '+name)
    return desired
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--target',type=Path,default=ROOT)
    p.add_argument('--reverse',action='store_true');p.add_argument('--check',action='store_true')
    a=p.parse_args();print('MINDEX_INTEGRATION_RECIPE_PASS state='+apply(a.target,a.reverse,a.check))
