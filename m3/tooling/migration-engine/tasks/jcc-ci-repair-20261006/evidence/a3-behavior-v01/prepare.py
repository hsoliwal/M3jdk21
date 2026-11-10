#!/usr/bin/env python3
"""Stage exact retained owners/resources and actual CI recipe outputs for A3 tests."""
from pathlib import Path
import hashlib,json,shutil,datetime

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[1]
WORK=HERE/'workspace'
def ident(path):
    b=path.read_bytes()
    return {'path':str(path),'bytes':len(b),'sha256':hashlib.sha256(b).hexdigest(),'git_blob':hashlib.sha1(b'blob '+str(len(b)).encode()+b'\0'+b).hexdigest()}
def save(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    with path.open('xb') as f:f.write(data)
def main():
    plan=json.loads((HERE/'ACQUISITION_PLAN.json').read_text())
    rows=[]
    for row in plan['rows']:
        capture=HERE/'calls'/f"{row['sha']}.json"
        d=json.loads(capture.read_text())['response']
        assert not d.get('isError')
        data=d['structuredContent']['content'].encode()
        dest=WORK/'m3'/row['path']
        save(dest,data)
        i=ident(dest)
        assert i['git_blob']==row['sha'] and i['bytes']==row['size'],row
        rows.append({**i,'repository_path':'m3/'+row['path'],'origin':'EXACT_CURRENT_GIT_BLOB','capture':ident(capture)})
    prior=json.loads((ROOT/'current-ci/repair-candidate-v02/execution/JAVA_EXECUTION.json').read_text())
    for row in prior['outputs']:
        if not row['repository_path'].endswith('.java'):continue
        src=Path(row['path']);i=ident(src)
        assert i['sha256']==row['sha256'] and i['git_blob']==row['git_blob']
        dest=WORK/row['repository_path'];save(dest,src.read_bytes())
        rows.append({**ident(dest),'repository_path':row['repository_path'],'origin':'ACTUAL_CI_OPENREWRITE_RESULT','source':i})
    pom=HERE/'pom.xml'
    dest=WORK/'m3/tooling/migration-recipes/tasks/jcc-ci-repair-20261006/a3-behavior-verification/pom.xml'
    save(dest,pom.read_bytes())
    rows.append({**ident(dest),'repository_path':str(dest.relative_to(WORK)),'origin':'FOCUSED_VERIFICATION_POM'})
    source_cache=ROOT/'current-ci/repair-candidate/execution/cache-v02'
    cache=HERE/'cache'
    shutil.copytree(source_cache,cache)
    cache_rows=[]
    for p in sorted(source_cache.rglob('*')):
        if p.is_file():
            i=ident(p); d=cache/p.relative_to(source_cache);assert ident(d)['sha256']==i['sha256']
            cache_rows.append({'relative_path':str(p.relative_to(source_cache)),'bytes':i['bytes'],'sha256':i['sha256']})
    save(HERE/'CACHE_COPY.json',(json.dumps({'source':str(source_cache),'destination':str(cache),'files':cache_rows},indent=2)+'\n').encode())
    receipt={'schema':'m3-a3-behavior-inputs/1','created_at':datetime.datetime.now(datetime.timezone.utc).isoformat(),'revision':plan['revision'],'m3_tree':plan['m3_tree'],'rows':rows,'cache':ident(HERE/'CACHE_COPY.json'),'prior_java_receipt':ident(ROOT/'current-ci/repair-candidate-v02/execution/JAVA_EXECUTION.json'),'toolchain':ident(ROOT/'toolchain/TOOLCHAIN.json')}
    save(HERE/'INPUTS.json',(json.dumps(receipt,indent=2)+'\n').encode())
    print(json.dumps({'staged':len(rows),'cache_files':len(cache_rows),'receipt':ident(HERE/'INPUTS.json')}))
if __name__=='__main__':main()
