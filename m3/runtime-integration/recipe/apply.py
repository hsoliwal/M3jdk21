#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Apply a complete reviewed runtime patch; refuse mixed or drifted states."""
import argparse,hashlib,json,subprocess
from pathlib import Path,PurePosixPath
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
def digest(data): return hashlib.sha256(data).hexdigest()
def apply(target,reverse=False,check=False):
    target=target.resolve()
    manifest=json.loads((HERE/'manifest.json').read_text())
    patch=HERE/'runtime.patch'
    if digest(patch.read_bytes())!=manifest['patch_sha256']:raise ValueError('patch drift')
    states=set()
    for name,hashes in manifest['files'].items():
        safe=PurePosixPath(name)
        if safe.is_absolute() or '..' in safe.parts or safe.parts[0]!='src':raise ValueError('unsafe path')
        p=target/name
        if p.is_symlink() or target not in p.resolve().parents:raise ValueError('symlink path')
        actual=digest(p.read_bytes()) if p.exists() else None
        if actual==hashes['before']:states.add('before')
        elif actual==hashes['after']:states.add('after')
        else:raise ValueError('source drift: '+name)
    if len(states)!=1:raise ValueError('mixed runtime patch state')
    current=states.pop();desired='before' if reverse else 'after'
    if current==desired or check:return current
    cmd=['git','apply']+(['--reverse'] if reverse else [])
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
