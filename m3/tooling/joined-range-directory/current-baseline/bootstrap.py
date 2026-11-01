# Copyright 2026 Hitesh Soliwal and contributors.
# SPDX-License-Identifier: Apache-2.0
"""Prepare only pinned qualification inputs on an approved hosted scratch root."""
from pathlib import Path
import argparse
import base64
import json
import os
import re
import runpy
import shutil
import urllib.request

def main():
    ap=argparse.ArgumentParser()
    for name in ['qualifier','profile','original','receiver','root']:ap.add_argument('--'+name,type=Path,required=True)
    ap.add_argument('--declared-transition',type=Path)
    ap.add_argument('--sdk-owner',type=Path);ap.add_argument('--engine-pins',type=Path);a=ap.parse_args()
    atoms=runpy.run_path(str(a.qualifier),run_name='qualified_core_input_atoms')
    ident,relative,admit=atoms['ident'],atoms['relative'],atoms['admit']
    current=json.loads(a.profile.read_bytes());old=json.loads(a.original.read_bytes())
    assert len(current['files'])==59 and len(old['files'])==58
    assert ident(a.original.read_bytes())['gitBlob']==current['historicalBaseline']['manifestGit']
    transition=None;receiver_transitions=[]
    if a.declared_transition is not None:
        declared=a.receiver/'m3/tooling/joined-range-directory/current-module/COMPOSITION_BINDING.json'
        assert a.declared_transition.resolve()==declared.resolve()
        assert all(not p.is_symlink() and not p.is_junction() for p in [declared,*declared.parents])
        raw=declared.read_bytes()
        assert ident(raw)['sha256']=='a468d927182781321b67c0a9f75f670ccf2d2215983cb7d38e1c59f0666ce432'
        transition=json.loads(raw)
        assert transition['schema']=='M3_JOINED_CURRENT_MODULE_COMPOSITION_V1'
        assert transition['targetPath']=='m3/core/src/com/m3/text/JoinedM3StringPiece.java'
        assert transition['before']['gitBlob']=='cc3c84f5ea0230e2f57e3d3ae0f5d29b9241e597'
        assert transition['after']['gitBlob']=='4f88f794266375ad8174fa56e952c30d32689877'
        indexed={r['path']:r for r in current['files']}
        assert len(indexed)==59
        assert all(indexed[p]['sha256']==digest for p,digest in transition['custodySha256'].items())
    assert not a.root.exists() and shutil.disk_usage(a.root.parent).free-256*1024**2>8*1024**3
    a.root.mkdir();inputs=a.root/'inputs'
    def write(root,name,raw):
        out=root/relative(name);out.parent.mkdir(parents=True,exist_ok=True)
        assert out.resolve().is_relative_to(root.resolve()) and not out.exists()
        out.write_bytes(raw)
    def blob(repo,row):
        assert repo in ('hsoliwal/M3jdk21','hsoliwal/com.synexia') and re.fullmatch('[0-9a-f]{40}',row['gitBlob'])
        headers={'Accept':'application/vnd.github+json','User-Agent':'M3-core-baseline-qualification'}
        token=os.environ.get('GH_TOKEN')
        if token:headers['Authorization']='Bearer '+token
        url='https://api.github.com/repos/'+repo+'/git/blobs/'+row['gitBlob']
        with urllib.request.urlopen(urllib.request.Request(url,headers=headers),timeout=60) as response:
            payload=response.read(2*1024**2+1)
        assert len(payload)<=2*1024**2
        record=json.loads(payload);assert record['sha']==row['gitBlob'] and record['encoding']=='base64'
        raw=base64.b64decode(''.join(record['content'].splitlines()),validate=True)
        assert all(ident(raw)[key]==row[key] for key in ('bytes','gitBlob','sha256'))
        return raw
    values={}
    for row in current['files']:
        raw=(a.receiver/relative(row['path'])).read_bytes()
        observed=ident(raw);expected={key:row[key] for key in ('bytes','gitBlob','sha256')}
        if observed!=expected:
            assert transition is not None
            assert row['path']==transition['targetPath'] and expected==transition['before']
            assert observed==transition['after']
            raw=blob(current['repository'],row)
            receiver_transitions.append(dict(path=row['path'],observed=observed,frozen=ident(raw),recovery='verified-git-blob'))
        assert ident(raw)==expected
        values[row['path']]=raw;write(inputs/'current',row['path'],raw)
    admit(current['files'],values)
    overlays=0
    for row in old['files']:
        raw=values.get(row['path'])
        if raw is None or ident(raw)['gitBlob']!=row['gitBlob']:
            raw=blob(old['repository'],row);write(inputs/'historical-overlays',row['path'],raw);overlays+=1
        assert all(ident(raw)[key]==row[key] for key in ('bytes','gitBlob','sha256'))
    assert overlays==7
    for row in current['notices']:
        raw=(a.receiver/relative(row['path'])).read_bytes()
        assert all(ident(raw)[key]==row[key] for key in ('bytes','gitBlob','sha256'))
        write(inputs/'notices',row['path'],raw)
    engines=0
    if a.sdk_owner is not None:
        assert a.engine_pins is not None and a.sdk_owner.resolve().is_relative_to(a.root.resolve())
        pins=json.loads(a.engine_pins.read_bytes());assert len(pins['files'])==5
        for row in pins['files']:write(a.sdk_owner,row['path'],blob(pins['repository'],row));engines+=1
    for name in ['temp','home','repo-head']:(a.root/name).mkdir()
    result=dict(status='PASS_PINNED_HOSTED_INPUT_PREPARATION',currentFiles=59,historicalOverlays=overlays,notices=len(current['notices']),canonicalSdkOwners=engines,currentCommit=current['commit'],historicalCommit=old['commit'],authority=False)
    if a.declared_transition is not None:result.update(receiverTransitions=receiver_transitions,receiverSourceWrites=0,receiverRole='DECLARED_SOURCE_OBSERVED_FROZEN_BASELINE_RECOVERED')
    (a.root/'BOOTSTRAP.json').write_bytes((json.dumps(result,indent=2)+'\n').encode())
    print(json.dumps(result))

if __name__=='__main__':main()
