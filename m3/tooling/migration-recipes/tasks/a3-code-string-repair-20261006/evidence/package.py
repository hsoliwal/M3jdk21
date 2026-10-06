#!/usr/bin/env python3
"""Freeze actual A3 repair outputs and disjoint evidence for root publication."""
from pathlib import Path
import json,hashlib,shutil,datetime,xml.etree.ElementTree as ET
HERE=Path(__file__).resolve().parent;ROOT=HERE.parents[1];PREFIX=Path('m3/tooling/migration-recipes/tasks/a3-code-string-repair-20261006');OVER=HERE/'publication/overlay'
def ident(p):
 b=p.read_bytes();return {'bytes':len(b),'sha256':hashlib.sha256(b).hexdigest(),'git_blob':hashlib.sha1(b'blob '+str(len(b)).encode()+b'\0'+b).hexdigest()}
def put(p,d):
 p.parent.mkdir(parents=True,exist_ok=True)
 with p.open('x') as f:json.dump(d,f,indent=2);f.write('\n')
def main():
 failed=ROOT/'current-ci/a3-behavior-v01/publication/PUBLICATION_MANIFEST.json';f=json.load(open(failed));assert ident(failed)['sha256']=='6cdd2fd2063f3cf9a4c3b396dfb510bdf4eebaff66be3f2a32a760eee6eb3109'
 for r in f['rows']:assert ident(Path(r['path']))['sha256']==r['sha256']
 execution=json.load(open(HERE/'execution/EXECUTION.json'));assert execution['status']=='PASS';entries=[]
 def copy(src,dest,before=None,origin='ADDITIVE_RECIPE_OR_EVIDENCE'):
  dst=OVER/dest;dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst);i=ident(dst);assert i==ident(src)
  entries.append({'repository_path':str(dest),'path':str(dest),'local_path':str(dst),'mode':'100644',**i,'expected_before_git_blob':before,'origin_path':str(src),'origin':origin})
 for p in sorted((HERE/'candidate/overlay').rglob('*')):
  if p.is_file():copy(p,p.relative_to(HERE/'candidate/overlay'))
 for row in execution['outputs']:
  src=Path(row['path']);assert ident(src)['sha256']==row['sha256'];copy(src,Path(row['repository_path']),row['expected_before_git_blob'],'ACTUAL_CANONICAL_OPENREWRITE_RESULT')
 for name in ['REFERENCE_PLAN.json','REFERENCE_AUDIT.json','PROOF_INPUTS.json','author.py','prepare_verification.py','run_gates.py','package.py']:
  copy(HERE/name,PREFIX/'evidence'/name)
 for p in sorted((HERE/'calls').glob('*.json')):copy(p,PREFIX/'evidence/reference-calls'/p.name)
 for name in ['CANDIDATE_DIFF.patch','CANDIDATES.json']:copy(HERE/'candidate'/name,PREFIX/name)
 copy(HERE/'README.md',PREFIX/'README.md')
 for p in sorted((HERE/'execution').glob('*')):
  if p.is_file():copy(p,PREFIX/'evidence/execution'/p.name)
 for key,lane in [('proof_junit','recipe-junit'),('original_behavior_junit','original-a3-junit')]:
  for report in execution[key]:
   p=Path(report['identity']['path']);assert ident(p)['sha256']==report['identity']['sha256'];copy(p,PREFIX/'evidence'/lane/p.name)
 paths=[]
 for key in ['proof_junit','original_behavior_junit']:
  report=Path(execution[key][0]['identity']['path']);e=ET.parse(report).getroot();cp=e.find("properties/property[@name='surefire.test.class.path']").get('value')
  paths.extend(Path(p) for p in cp.split(':') if p and Path(p).is_file())
 artifacts=[{'path':str(p),**ident(p)} for p in sorted(set(paths))]
 cache=json.load(open(ROOT/'current-ci/a3-behavior-v01/CACHE_COPY.json'));source=Path(cache['source'])
 for r in cache['files']:assert ident(source/r['relative_path'])['sha256']==r['sha256']
 custody={'schema':'m3-a3-repair-custody/1','failure_overlay_preserved':{'manifest':str(failed),**ident(failed),'files':len(f['rows'])},'immutable_source_cache_files_unchanged':len(cache['files']),'actual_classpath_artifacts':artifacts,'toolchain':{'path':str(ROOT/'toolchain/TOOLCHAIN.json'),**ident(ROOT/'toolchain/TOOLCHAIN.json')},'native_source_or_broad_admission':False}
 put(HERE/'CUSTODY.json',custody);copy(HERE/'CUSTODY.json',PREFIX/'evidence/CUSTODY.json')
 assert len({r['repository_path'] for r in entries})==len(entries)
 manifest={'schema':'m3-a3-code-string-repair-publication/1','status':'ACTUAL_THREE_OUTPUTS_AND_THIRTEEN_PASSED_METHODS','created_at_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'overlay':str(OVER),'tested_baseline':'d1b9162cd568108f4c8d82f6b6a03cccfdb91bd2','recipe':'com.m3.rewrite.backport.A3CodeStringRepair','rows':entries,'counts':{'files':len(entries),'modifications':3,'additions':len(entries)-3,'bytes':sum(x['bytes'] for x in entries)},'tests':{'recipe_methods':9,'original_a3_methods':4,'failures':0,'errors':0,'skips':0,'actual_outputs':3,'fixed_point_changes':0,'refusal_cases':12,'mixed_states':8,'actual_produced_javac_exit':0},'execution_receipt':{'path':str(HERE/'execution/EXECUTION.json'),**ident(HERE/'execution/EXECUTION.json')},'failure_overlay_unchanged':True,'full_runtime_jni_or_jdk_qualified':False}
 put(HERE/'publication/PUBLICATION_MANIFEST.json',manifest);print(json.dumps({'manifest':str(HERE/'publication/PUBLICATION_MANIFEST.json'),**ident(HERE/'publication/PUBLICATION_MANIFEST.json'),'counts':manifest['counts']}))
if __name__=='__main__':main()
