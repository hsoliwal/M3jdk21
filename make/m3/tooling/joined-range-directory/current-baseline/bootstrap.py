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
        v1='make/m3/tooling/joined-range-directory/current-module/COMPOSITION_BINDING.json'
        v2='make/m3/tooling/joined-range-directory/current-baseline/RECEIVER_TRANSITIONS_V2.json'
        v3='make/m3/tooling/joined-range-directory/current-baseline/RECEIVER_TRANSITIONS_V3.json'
        v4='make/m3/tooling/joined-range-directory/current-baseline/RECEIVER_TRANSITIONS_V4.json'
        v5='make/m3/tooling/joined-range-directory/current-baseline/RECEIVER_TRANSITIONS_V5.json'
        v6='make/m3/tooling/joined-range-directory/current-baseline/RECEIVER_TRANSITIONS_V6.json'
        v7='make/m3/tooling/joined-range-directory/current-baseline/RECEIVER_TRANSITIONS_V7.json'
        v8='make/m3/tooling/joined-range-directory/current-baseline/RECEIVER_TRANSITIONS_V8.json'
        v9='make/m3/tooling/joined-range-directory/current-baseline/RECEIVER_TRANSITIONS_V9.json'
        v10='make/m3/tooling/joined-range-directory/current-baseline/RECEIVER_TRANSITIONS_V10.json'
        v11='make/m3/tooling/joined-range-directory/current-baseline/RECEIVER_TRANSITIONS_V11.json'
        allowed={v1:'1a827ea287629995ee0eb62cd2ffa35d3c0b48c680b67da592cb0a37305d0853',
                 v2:'0f7c12d9f170c0290db4a114dc3f32d2e08af9090b9be14d86b2e46576899afd',
                 v3:'819b65189f71f0776ca85bdf05bd1cc92494a0a661e67e10a30ea529f4839364',
                 v4:'a5f2d41a604fb3f24ec0feed86d2247fb6065355b4285bf81f87f65d5d4ca328',
                 v5:'34369e1ad23948e495a426d76969e208794d4007c2ec24690eda4b61717e6a30',
                 v6:'166ccfe6d6c585e5fc2ae70de91a911c0e5dd3634b9c2f740e28611317670ba9',
                 v7:'4f6aa0fa97dc0e7ba6e9eb89afd95a3f4bc50180fba4c937d3c53efeb4f93936',
                 v8:'6f90d22fb6db68af59668138e3757f3c29826af552592b14322c927b372c3873',
                 v9:'8405960a98d2632bcfa45e9c93c90930fd8f8955ee0720c126421a856efc1c47',
                 v10:'48a2cdabd2093b8e18635b898d7294c3bb1accf14f8db0787ef636ca46935c4c',
                 v11:'86382f8143dea0fb608e1895987feb85f54e2b27f182d2cdc21a68fd4d5affdc'}
        matches=[name for name in allowed if a.declared_transition.resolve()==(a.receiver/name).resolve()]
        assert len(matches)==1
        name=matches[0];declared=a.receiver/name
        assert all(not p.is_symlink() and not p.is_junction() for p in [declared,*declared.parents])
        raw=declared.read_bytes();assert ident(raw)['sha256']==allowed[name]
        document=json.loads(raw)
        indexed={r['path']:r for r in current['files']};assert len(indexed)==59
        if name==v1:
            assert document['schema']=='M3_JOINED_CURRENT_MODULE_COMPOSITION_V1'
            assert document['targetPath']=='m3/core/src/jdk/m3/text/JoinedM3StringPiece.java'
            assert document['before']['gitBlob']=='5022e9811ce9391671c794446c7772569cfd369f'
            assert document['after']['gitBlob']=='765d43267bb22a3650b622c01cb29b550e1deb01'
            rows=[dict(path=document['targetPath'],before=document['before'],after=document['after'])]
        else:
            assert document['schema']=='M3_FROZEN_RECEIVER_TRANSITIONS_V2'
            assert ident(a.profile.read_bytes())['sha256']==document['frozenProfileSha256']
            rows=document['files']
            expected_paths={
                'm3/core/src/jdk/m3/text/JoinedM3StringPiece.java',
                'm3/core/test/java/lang/M3CodeTextSignalBatchTest.java'}
            if name in (v3,v4,v5,v6,v7,v8,v9,v10):expected_paths.update({
                'm3/core/src/jdk/m3/text/SharedLexiconPrecomputeCatalog.java',
                'm3/core/test/M3LexiconPrecomputeTest.java'})
            if name in (v4,v5,v6,v7,v8,v9,v10):expected_paths.update({
                'm3/core/src/jdk/m3/text/SharedRelatedLexemeCatalog.java',
                'm3/core/test/SharedLexiconCatalogFamilyAdmissionTest.java'})
            if name in (v5,v6,v7,v8,v9,v10):expected_paths.update({
                'm3/core/src/jdk/m3/text/SharedLexiconCatalog.java',
                'm3/core/test/FoundationTest.java'})
            if name in (v8,v9,v10):expected_paths.add('m3/core/src/jdk/m3/text/M3PrecomputePayload.java')
            if name==v11:expected_paths={
                'make/m3/build.sh',
                'm3/core/src/jdk/m3/text/JoinedM3StringPiece.java',
                'm3/core/src/jdk/m3/text/M3BigString.java',
                'm3/core/src/jdk/m3/text/M3PrecomputePayload.java',
                'm3/core/src/jdk/m3/text/SharedLexiconCatalog.java',
                'm3/core/src/jdk/m3/text/SharedLexiconImage.java',
                'm3/core/src/jdk/m3/text/SharedLexiconPrecomputeCatalog.java',
                'm3/core/src/jdk/m3/text/SharedRelatedLexemeCatalog.java',
                'm3/core/test/FoundationTest.java',
                'm3/core/test/LangDexProjectionReceiverTest.java',
                'm3/core/test/M3LexiconPrecomputeTest.java',
                'm3/core/test/M3NumberSpaceContractTest.java',
                'm3/core/test/M3TranslationReceiverMapTest.java',
                'm3/core/test/SharedLexiconCatalogFamilyAdmissionTest.java',
                'm3/core/test/java/lang/M3CodeTextSignalBatchTest.java',
                'm3/lexicon/m3-number-target-map.tsv',
                'm3/lexicon/m3-precompute-field-map.tsv'}
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
        assert repo=='hsoliwal/M3jdk21' and re.fullmatch('[0-9a-f]{40}',row['gitBlob'])
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
