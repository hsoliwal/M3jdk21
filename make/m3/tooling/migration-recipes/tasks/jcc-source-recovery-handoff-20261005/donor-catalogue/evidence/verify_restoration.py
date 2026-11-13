#!/usr/bin/env python3
"""Verify an exact five-document Git subtree restoration; no code execution."""
import base64
import hashlib
import json
import sys
from pathlib import Path

ROOT = Path("/workspace/scratch/1c68df1bae79/javac-convergence-20261005")
OUT = ROOT / 'work/publication-current/donor-catalogue-recovery-publish'
INPUT = ROOT / 'work/donor-catalogue-recovery-20261005/INPUT_MANIFEST.json'
EXPECTED = ROOT / 'work/donor-catalogue-recovery-20261005/EXPECTED_SOURCE_TREES.json'
BASE = '0c53b4f1dfe66c7eff05c8047b8fc681d26fecb8'
BASE_TREE = '0361e4a07b77a01df170523033fef01c4052ddc5'
FINAL_TREE = '1bd86f643f0e08ecb1b5a43d020bf0110e797d68'
DONORS = '54c7989b23bb4fea69b74352aa144c6ba3c3adf0'
BRANCH = 'aix/jcc-donor-catalogue-recovery-20261005'
BASE_BRANCH = 'aix/jcc-source-merge-recovery-20261005'
PREFIX = 'synexia-openrewrite-recipes/recipes/atom-pattern-mastery-20261005/jcc/donors'
REPO = 'hsoliwal/com.synexia'

def sha(raw):
    return hashlib.sha256(raw).hexdigest()

def blob(raw):
    return hashlib.sha1(b'blob ' + str(len(raw)).encode() + b'\0' + raw).hexdigest()

def norm(entry):
    keys = ['path', 'mode', 'type', 'sha'] + (['size'] if entry['type'] == 'blob' else [])
    return {k:entry[k] for k in keys}

def entries(rows):
    return sorted([norm(e) for e in rows], key=lambda e:e['path'])

def tree(rows):
    body = b''
    for e in sorted(rows, key=lambda e:(e['path'] + ('/' if e['type']=='tree' else '')).encode()):
        mode = '40000' if e['type']=='tree' else e['mode']
        body += mode.encode()+b' '+e['path'].encode()+b'\0'+bytes.fromhex(e['sha'])
    return hashlib.sha1(b'tree '+str(len(body)).encode()+b'\0'+body).hexdigest()

def read(path):
    assert not path.is_symlink() and path.is_file()
    return path.read_bytes()

def write(path, value):
    raw = (json.dumps(value, indent=2, sort_keys=True)+'\n').encode()
    if path.exists():
        assert read(path)==raw
    else:
        with path.open('xb') as f:
            f.write(raw)
    return sha(raw)

def preflight():
    raw = read(INPUT)
    assert sha(raw)=='f7949116d828c55aa5cc77c19764cf0484958d408b86d0c91999c45a3fedd8f3'
    m = json.loads(raw)
    assert (m['repository'],m['base_commit'],m['base_tree'])==(REPO,BASE,BASE_TREE)
    assert (m['files'],m['bytes'],m['existing_updates'],m['deletions'])==(5,78467,0,0)
    assert {r['path'].removeprefix(PREFIX+'/') for r in m['rows']}=={
        'AUTHORING_RECEIPT.json','DONOR_CATALOGUE.md','EXECUTED_TEST_MAPPING.md','INTEGRATION.md','donor_catalogue.json'}
    donor_entries=[]
    for row in m['rows']:
        p=Path(row['local_path'])
        assert p.is_relative_to(ROOT/'work/donor-catalogue-recovery-20261005/payload')
        raw=read(p)
        raw.decode('utf-8',errors='strict')
        assert b'\0' not in raw
        assert (len(raw),sha(raw),blob(raw))==(row['bytes'],row['sha256'],row['git_blob'])
        assert row['mode']=='100644' and (p.stat().st_mode & 0o777)==0o644
        assert raw==read(Path(row['local_original']['local_path']))
        assert row['path'].startswith(PREFIX+'/') and Path(row['path']).parent.as_posix()==PREFIX
        donor_entries.append({'path':Path(row['path']).name,'mode':'100644','type':'blob','sha':row['git_blob'],'size':len(raw)})
    assert tree(donor_entries)==DONORS
    raw=read(OUT/'BASE_CHAIN.json')
    assert sha(raw)=='bea94459b67cc6b7c0d98a2cfe3f946c6f558c803725ee76f89e0821dae3ff4a'
    base=json.loads(raw)
    assert (base['commit'],base['root'])==(BASE,BASE_TREE)
    chain=base['ancestors']
    assert len(chain)==5 and chain[0]['path']=='' and chain[-1]['path']==str(Path(PREFIX).parent)
    for i,node in enumerate(chain):
        body=node['body']
        assert body['truncated'] is False and tree(body['tree'])==body['sha']
        if i==0:
            assert body['sha']==BASE_TREE
        else:
            parent=chain[i-1]
            assert str(Path(node['path']).parent).replace('.','')==parent['path']
            old=next(e for e in parent['body']['tree'] if e['path']==Path(node['path']).name)
            assert old['type']=='tree' and old['sha']==body['sha']
    assert not any(e['path']=='donors' for e in chain[-1]['body']['tree'])
    expected=[{'path':PREFIX,'preimage_tree':None,'postimage_tree':DONORS,'entries':entries(donor_entries)}]
    child_path,child_sha=PREFIX,DONORS
    kept=0
    for node in reversed(chain):
        current=entries(node['body']['tree'])
        child_name=Path(child_path).name
        retained=[e for e in current if e['path']!=child_name]
        kept+=len(retained)
        result=retained+[{'path':child_name,'mode':'040000','type':'tree','sha':child_sha}]
        child_sha=tree(result)
        child_path=node['path']
        expected.append({'path':child_path,'preimage_tree':node['body']['sha'],'postimage_tree':child_sha,'entries':entries(result)})
    assert child_sha==FINAL_TREE
    peer_raw=read(EXPECTED)
    assert sha(peer_raw)=='b8c31ba4c3c6c3cfafc426852655c15994193dc1a9c62edaefce05c7840131e0'
    peer=json.loads(peer_raw)
    for a,b in zip(expected,peer['changed_or_reused_trees'],strict=True):
        assert a['path']==b['path'] and a['preimage_tree']==b['preimage_tree'] and a['postimage_tree']==b['postimage_tree']
        assert a['entries']==entries(b['entries'])
    return {'schema':'jcc-donor-document-restoration-preflight/1','status':'PASS','repository':REPO,
        'base_commit':BASE,'base_tree':BASE_TREE,'expected_tree':FINAL_TREE,'branch':BRANCH,'base_branch':BASE_BRANCH,
        'input_manifest_path':str(INPUT),'input_manifest_sha256':sha(read(INPUT)),
        'files':m['rows'],'file_count':5,'bytes':78467,'existing_updates':0,'deletions':0,
        'trees':expected,'unchanged_sibling_entries':kept,
        'operation':{'path':PREFIX,'mode':'040000','type':'tree','sha':DONORS},
        'source_code_epoch_unchanged':True,'tests_rerun':False,'qualification_promoted':False}

def verify(readback_path, output, published):
    p=preflight()
    readback_raw=read(readback_path)
    r=json.loads(readback_raw)
    assert r['repository']==REPO
    expected={e['postimage_tree']:e for e in p['trees']}
    assert set(r['trees'])==set(expected)
    for s,body in r['trees'].items():
        assert body['sha']==s and body['truncated'] is False and tree(body['tree'])==s
        assert entries(body['tree'])==expected[s]['entries']
    commit=r['commit']
    assert commit['tree']['sha']==FINAL_TREE and [x['sha'] for x in commit['parents']]==[BASE]
    commit_sha=commit['sha']
    assert len(commit_sha)==40
    rows={x['path']:x for x in p['files']}
    assert len(r['files'])==5 and {x['request']['path'] for x in r['files']}==set(rows)
    for call in r['files']:
        req=call['request']; response=call['response']
        assert call['tool']=='mcp__codex_apps__github_fetch_file'
        assert req=={'repository_full_name':REPO,'path':req['path'],'ref':commit_sha,'encoding':'base64'}
        assert not response.get('isError',False)
        body=response['structuredContent']
        assert body['encoding']=='base64'
        raw=base64.b64decode(''.join(body['content'].split()),validate=True)
        row=rows[req['path']]
        assert body['sha']==row['git_blob'] and raw==read(Path(row['local_path']))
        assert (len(raw),sha(raw),blob(raw))==(row['bytes'],row['sha256'],row['git_blob'])
    result={'schema':'jcc-donor-document-restoration-publication/1',
        'status':'COMMIT_VERIFIED_BEFORE_BRANCH','repository':REPO,'commit':commit_sha,
        'tree':FINAL_TREE,'parent':BASE,'branch':BRANCH,'base_branch':BASE_BRANCH,
        'file_count':5,'bytes':78467,'files':p['files'],'changed_or_reused_trees_verified':6,
        'direct_bodies_compared':5,'unchanged_sibling_entries':p['unchanged_sibling_entries'],
        'readback_path':str(readback_path),'readback_sha256':sha(readback_raw),
        'input_manifest_sha256':p['input_manifest_sha256'],'verifier_sha256':sha(read(Path(__file__))),
        'tests_rerun':False,'source_code_epoch_unchanged':True,'qualification_promoted':False}
    if published:
        ref=r['ref']; pr=r['pull_request']
        assert ref['ref']=='refs/heads/'+BRANCH and ref['object']['sha']==commit_sha
        assert pr['state']=='open' and pr['draft'] is True and pr['merged'] is False
        assert pr['head']['sha']==commit_sha and pr['head']['ref']==BRANCH and pr['base']['ref']==BASE_BRANCH
        assert pr['head']['repo']['full_name']==REPO and pr['base']['repo']['full_name']==REPO
        assert pr['base']['sha']==BASE and r['ref_update_forced'] is False
        result.update(status='PUBLISHED_READBACK_VERIFIED',pull_request=pr['html_url'],
                      pull_request_number=pr['number'],branch_commit_pr_verified=True)
    else:
        assert 'ref' not in r and 'pull_request' not in r
    seal=write(output,result)
    print(json.dumps({'status':result['status'],'receipt':str(output),'sha256':seal,'commit':commit_sha,'tree':FINAL_TREE}))

if __name__=='__main__':
    if not __debug__:
        raise RuntimeError('Optimized Python is not supported')
    if sys.argv[1:] == ['prepare']:
        p=preflight()
        seal=write(OUT/'PREFLIGHT.json',p)
        print(json.dumps({'status':'PASS','files':5,'bytes':78467,'expected_tree':FINAL_TREE,
            'unchanged_siblings':p['unchanged_sibling_entries'],'sha256':seal}))
    else:
        assert len(sys.argv)==5 and sys.argv[1]=='verify' and sys.argv[4] in ('before','published')
        verify(Path(sys.argv[2]),Path(sys.argv[3]),sys.argv[4]=='published')

