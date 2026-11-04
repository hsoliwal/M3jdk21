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
        v1='m3/tooling/joined-range-directory/current-module/COMPOSITION_BINDING.json'
        v2='m3/qualification/lexicon/receiver-transitions/RECEIVER_TRANSITIONS_V2.json'
        v3='m3/qualification/lexicon/receiver-transitions/RECEIVER_TRANSITIONS_V3.json'
        v4='m3/qualification/lexicon/receiver-transitions/RECEIVER_TRANSITIONS_V4.json'
        v5='m3/qualification/lexicon/receiver-transitions/RECEIVER_TRANSITIONS_V5.json'
        v6='m3/qualification/lexicon/receiver-transitions/RECEIVER_TRANSITIONS_V6.json'
        v7='m3/qualification/lexicon/receiver-transitions/RECEIVER_TRANSITIONS_V7.json'
        v8='m3/qualification/lexicon/receiver-transitions/RECEIVER_TRANSITIONS_V8.json'
        v9='m3/qualification/lexicon/receiver-transitions/RECEIVER_TRANSITIONS_V9.json'
        v10='m3/qualification/lexicon/receiver-transitions/RECEIVER_TRANSITIONS_V10.json'
        v11='m3/qualification/lexicon/receiver-transitions/RECEIVER_TRANSITIONS_V11.json'
        v12='m3/qualification/lexicon/receiver-transitions/RECEIVER_TRANSITIONS_V12.json'
        v13='m3/qualification/lexicon/receiver-transitions/RECEIVER_TRANSITIONS_V13.json'
        allowed={v1:'a468d927182781321b67c0a9f75f670ccf2d2215983cb7d38e1c59f0666ce432',
                 v2:'a7a41800bfd69e40eb59cc89cb74bb795a59971c5f5f4157edd78e326e7adb1d',
                 v3:'0e47dec693c1db21dff237352ec6b4c3dec40286944ef9de85934239db3580f9',
                 v4:'b0d72c9e4606e06998ca3514999aa8655990f865ede325c5b7eb22f7d434170b',
                 v5:'4e00d68a96907e23b10670c32ca76bdae5b9bc7f1b6d27cdf323f17b6a1fd178',
                 v6:'25c475dc211870634a0a156ddd02ce3db78a492997019ecdf01c599db9b296b4',
                 v7:'e3f7f6c569d701f2c663c1ca34dcf2d8d5330ecbe8e2e8ddf61764975f636b5e',
                 v8:'5abff0ff5462a74439ceb13610913417ca5a1b2c4c8ddf0ba9d26a2428f1237d',
                 v9:'8b9033adaea873f6253558db142e6823762913da1899b714f019eaf6816e99d3',
                 v10:'178b7c3a0e120b0d5ea00eaf5b1724227e5261f73fe3677e4686b1d2d2b2ac6a',
                 v11:'d937901cb78b253a969f25d74f98cb715cc4064241115e0bbf1704aeb1e88f20',
                 v12:'2a596a61ecac808df5f5e1ca935ca7dd58e5a5c1cef9836537998ef21b094f6f',
                 v13:'c21507209fbb8cd65aab16ec1e1a2276d50abd36238ecb0f51bb6a4cf2dd1696'}
        matches=[name for name in allowed if a.declared_transition.resolve()==(a.receiver/name).resolve()]
        assert len(matches)==1
        name=matches[0];declared=a.receiver/name
        assert all(not p.is_symlink() and not p.is_junction() for p in [declared,*declared.parents])
        raw=declared.read_bytes();assert ident(raw)['sha256']==allowed[name]
        document=json.loads(raw)
        indexed={r['path']:r for r in current['files']};assert len(indexed)==59
        if name==v1:
            assert document['schema']=='M3_JOINED_CURRENT_MODULE_COMPOSITION_V1'
            assert document['targetPath']=='m3/core/src/com/m3/text/JoinedM3StringPiece.java'
            assert document['before']['gitBlob']=='cc3c84f5ea0230e2f57e3d3ae0f5d29b9241e597'
            assert document['after']['gitBlob']=='4f88f794266375ad8174fa56e952c30d32689877'
            rows=[dict(path=document['targetPath'],before=document['before'],after=document['after'])]
        else:
            assert document['schema']=='M3_FROZEN_RECEIVER_TRANSITIONS_V2'
            assert ident(a.profile.read_bytes())['sha256']==document['frozenProfileSha256']
            rows=document['files']
            expected_paths={
                'm3/core/src/com/m3/text/JoinedM3StringPiece.java',
                'm3/core/test/java/lang/M3CodeTextSignalBatchTest.java'}
            if name in (v3,v4,v5,v6,v7,v8,v9,v10,v11,v12,v13):expected_paths.update({
                'm3/core/src/com/m3/text/SharedLexiconPrecomputeCatalog.java',
                'm3/core/test/M3LexiconPrecomputeTest.java'})
            if name in (v4,v5,v6,v7,v8,v9,v10,v11,v12,v13):expected_paths.update({
                'm3/core/src/com/m3/text/SharedRelatedLexemeCatalog.java',
                'm3/core/test/SharedLexiconCatalogFamilyAdmissionTest.java'})
            if name in (v5,v6,v7,v8,v9,v10,v11,v12,v13):expected_paths.update({
                'm3/core/src/com/m3/text/SharedLexiconCatalog.java',
                'm3/core/test/FoundationTest.java'})
            if name in (v8,v9,v10,v11,v12,v13):expected_paths.add('m3/core/src/com/m3/text/SynexiaPrecomputePayload.java')
            assert len(rows)==len(expected_paths) and {r['path'] for r in rows}==expected_paths
        assert all(indexed[p]['sha256']==digest for p,digest in document['custodySha256'].items())
        transition={r['path']:r for r in rows};assert len(transition)==len(rows)
        assert all(r['before']=={k:indexed[p][k] for k in ('bytes','gitBlob','sha256')}
                   and r['after']!=r['before'] for p,r in transition.items())
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
            assert row['path'] in transition
            known=transition[row['path']]
            assert expected==known['before'] and observed==known['after']
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
