#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Synthetic 81-blob Git cohort and negative tamper tests; no repository credentials."""
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile
import importlib.util
spec = importlib.util.spec_from_file_location(
    "historical_cohort_audit", Path(__file__).with_name("verify_historical_cohort.py"))
assert spec is not None and spec.loader is not None
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
verify = module.verify


def git(repo, *args):
    return subprocess.check_output(['git','-C',str(repo),*args], text=True).strip()


def refusal(name, fn):
    try:
        fn()
    except (ValueError, AssertionError):
        print('M3_HISTORICAL_REFUSAL_PASS|'+name)
        return
    raise AssertionError('accepted invalid provenance: '+name)

with tempfile.TemporaryDirectory(prefix='m3-original-core-unit-') as temp:
    base=Path(temp)
    repo=base/'fixture'; repo.mkdir()
    subprocess.run(['git','init','-q',str(repo)],check=True)
    git(repo,'config','user.email','test@example.invalid')
    git(repo,'config','user.name','Synthetic Test')
    data={}
    for i in range(75):
        data[f'm3/core/src/jdk/m3/text/HistoricalAtom{i:03d}.java']=(f'final class HistoricalAtom{i:03d} {{}}\n').encode()
    data['m3/build.sh']=b'#!/bin/sh\necho historical build\n'
    for i in range(5):
        data[f'm3/lexicon/lexicon-{i}.tsv']=f'entry-{i}\tword\n'.encode()
    for path, bytes_ in data.items():
        p=repo/path; p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(bytes_)
    git(repo,'add','m3')
    git(repo,'commit','-qm','Synthetic historical source corpus')
    commit=git(repo,'rev-parse','HEAD')
    manifest={'schema':'M3_CURRENT_CORE_CONTRACT_INPUTS_V1','repository':'hsoliwal/M3jdk21',
              'sourceCommit':commit,'programs':[f'StressTest{i}' for i in range(18)],
              'modes':['jit','int','nocompact','c2'],'files':[]}
    for path, bytes_ in data.items():
        manifest['files'].append({'path':path,'bytes':len(bytes_),
                 'gitBlob':hashlib.sha1(b'blob '+str(len(bytes_)).encode()+b'\0'+bytes_).hexdigest(),
                 'sha256':hashlib.sha256(bytes_).hexdigest()})
    control=base/'control';control.mkdir()
    mf=control/'manifest.json';mf.write_text(json.dumps(manifest)+'\n')
    result=verify(repo,mf,commit)
    assert result['fileCount']==81 and result['javaSourceCount']==75
    assert result['historicalCommit']==commit and not result['currentRuntimeQualified']
    print('M3_HISTORICAL_SNAPSHOT_SYNTHETIC_PASS|files=81|java=75')

    refusal('wrong-head',lambda:verify(repo,mf,'0'*40))
    old=mf.read_text();mf.write_text(old.replace(commit,'1'*40,1))
    refusal('manifest-commit-drift',lambda:verify(repo,mf,commit));mf.write_text(old)
    doc=json.loads(mf.read_text());doc['files'][0]['sha256']='0'*64
    mf.write_text(json.dumps(doc))
    refusal('digest-drift',lambda:verify(repo,mf,commit));mf.write_text(old)
    target=repo/'m3/build.sh';original=target.read_bytes();target.write_bytes(original+b'changed')
    refusal('modified-checkout-source',lambda:verify(repo,mf,commit));target.write_bytes(original)
    target.unlink()
    refusal('missing-source',lambda:verify(repo,mf,commit));target.write_bytes(original)
    target.unlink();target.symlink_to(base/'control/manifest.json')
    refusal('redirected-source',lambda:verify(repo,mf,commit));target.unlink();target.write_bytes(original)

    extra=repo/'m3/core/test/UninventoriedSource.java';extra.parent.mkdir(parents=True,exist_ok=True)
    extra.write_text('final class UninventoriedSource {}\n')
    git(repo,'add','m3')
    git(repo,'commit','-qm','Add unaccounted Java atom')
    commit2=git(repo,'rev-parse','HEAD')
    doc=json.loads(mf.read_text());doc['sourceCommit']=commit2
    mf.write_text(json.dumps(doc))
    refusal('unlisted-java-atom',lambda:verify(repo,mf,commit2))
print('M3_HISTORICAL_COHORT_SELFTEST_COMPLETE|refusals=7')
