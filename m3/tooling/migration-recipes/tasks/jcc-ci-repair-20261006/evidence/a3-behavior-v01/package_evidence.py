#!/usr/bin/env python3
"""Freeze additive evidence for the unchanged A3 behavioral gate, including its failure."""
from pathlib import Path
import datetime,hashlib,json,shutil,sys,xml.etree.ElementTree as ET
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[1]
sys.path.insert(0,str(ROOT/'publication'))
import prepare_m3_restoration as gitproof
def identity(p):
 b=p.read_bytes();return {'path':str(p),'bytes':len(b),'sha256':hashlib.sha256(b).hexdigest(),'git_blob':hashlib.sha1(b'blob '+str(len(b)).encode()+b'\0'+b).hexdigest()}
def write(p,data):
 p.parent.mkdir(parents=True,exist_ok=True)
 with p.open('x') as f:json.dump(data,f,indent=2);f.write('\n')
def main():
 trees={};base=gitproof.admit_capture(trees,ROOT/'publication/M3_FOLLOWUP_CURRENT_ROOT.json');m3=gitproof.admit_capture(trees,ROOT/'publication/M3_FOLLOWUP_CURRENT_M3.json')
 commit=json.loads(json.load(open(ROOT/'publication/M3_FOLLOWUP_CURRENT_COMMIT.json'))['response']['structuredContent']['content'])
 assert commit['sha']=='d1b9162cd568108f4c8d82f6b6a03cccfdb91bd2' and commit['tree']['sha']==base and trees[base]['m3']['sha']==m3
 inputs=json.loads((HERE/'INPUTS.json').read_text());rows=[]
 for row in inputs['rows']:
  p=Path(row['path']);assert identity(p)['sha256']==row['sha256']
  if row['origin']!='FOCUSED_VERIFICATION_POM':
   found=gitproof.proof.resolve(trees,base,row['repository_path'])
   if row['origin']=='EXACT_CURRENT_GIT_BLOB':assert found['sha']==row['git_blob'],(row['repository_path'],found,row['git_blob'])
   else:
    expected={'M3A3RegexMemoryLabDeliveryRecipeTest.java':('63344ac5b73c30def736f560bd9aa9348df3b97f','e349a7c727592731681759ba26df5fdeb775342e'),'M3A3RegexMemoryWorkflowRecipeTest.java':('c1865602253b72c9d5109a6b096146e847fd86aa','8e7d6c748c2d8a9b89eb2a24de929a535a310136')}[p.name]
    assert (found['sha'],row['git_blob'])==expected
   rows.append({'repository_path':row['repository_path'],'current_git_blob':found['sha'],'executed_git_blob':row['git_blob'],'sha256':row['sha256'],'bytes':row['bytes'],'origin':row['origin']})
 actual='m3/tooling/a3/src/main/java/com/m3/a3/A3RegexMatrix.java';prod=gitproof.proof.resolve(trees,base,actual);assert prod['sha']=='bf91afcd627d8e2faa9790ea6592b6bc9c1b0e33'
 write(HERE/'CURRENT_ADMISSION.json',{'schema':'m3-a3-behavior-current-admission/1','commit':commit['sha'],'root':base,'m3_tree':m3,'validated_git_tree_objects':len(trees),'inputs':rows,'production_matrix':{**prod,'repository_path':actual},'unchanged_owner_resource_inputs_match_current_revision':18,'test_inputs_are_actual_verified_ci_outputs_over_exact_current_preimages':2})
 reports=HERE/'workspace/m3/tooling/migration-recipes/tasks/jcc-ci-repair-20261006/a3-behavior-verification/target/surefire-reports'
 cp=ET.parse(next(reports.glob('TEST-*.xml'))).getroot().find("properties/property[@name='surefire.test.class.path']").get('value')
 artifacts=[]
 for item in cp.split(':'):
  p=Path(item)
  if p.is_file():artifacts.append(identity(p))
 copy=json.loads((HERE/'CACHE_COPY.json').read_text());source=Path(copy['source']);unchanged=0
 for row in copy['files']:
  assert identity(source/row['relative_path'])['sha256']==row['sha256'];unchanged+=1
 write(HERE/'RUNTIME_CUSTODY.json',{'schema':'m3-a3-behavior-runtime-custody/1','java_home':str(ROOT/'toolchain/jdk-21+35'),'toolchain':identity(ROOT/'toolchain/TOOLCHAIN.json'),'cache_copy':identity(HERE/'CACHE_COPY.json'),'immutable_source_cache_files_unchanged_after':unchanged,'classpath_artifacts':artifacts,'full_native_or_repository_admission':False})
 overlay=HERE/'publication/overlay';prefix=Path('m3/tooling/migration-recipes/tasks/jcc-ci-repair-20261006');evidence=prefix/'evidence/a3-behavior-v01'
 entries=[]
 def copyfile(src,dest):
  dst=overlay/dest;dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dst);entries.append({**identity(dst),'repository_path':str(dest),'source':str(src),'before':None})
 for name in ['ACQUISITION_PLAN.json','INPUTS.json','MAVEN_REQUEST.json','MAVEN_PROCESS.json','EXECUTION.json','CURRENT_ADMISSION.json','RUNTIME_CUSTODY.json','prepare.py','run.py','package_evidence.py','maven.log','REPORT.md']:
  copyfile(HERE/name,evidence/name)
 for name in ['JAVAC_PROCESS.json','javac.log']:
  copyfile(HERE/'diagnostic'/name,evidence/'diagnostic'/name)
 for p in sorted((HERE/'calls').glob('*.json')):copyfile(p,evidence/'calls'/p.name)
 for p in sorted(reports.glob('TEST-*.xml')):copyfile(p,evidence/'junit'/p.name)
 copyfile(HERE/'pom.xml',prefix/'a3-behavior-verification/pom.xml')
 write(HERE/'publication/PUBLICATION_MANIFEST.json',{'schema':'m3-a3-behavior-publication/1','status':'THREE_ORIGINAL_METHODS_PASS_ONE_REAL_TEMPLATE_ERROR','created_at':datetime.datetime.now(datetime.timezone.utc).isoformat(),'overlay':str(overlay),'rows':entries,'counts':{'additions':len(entries),'modifications':0,'bytes':sum(x['bytes'] for x in entries)},'original_methods_executed':4,'original_methods_passed':3,'original_methods_errored':1,'skipped':0,'whole_module_or_jni_qualification':False})
 print(json.dumps({'manifest':identity(HERE/'publication/PUBLICATION_MANIFEST.json'),'files':len(entries),'bytes':sum(x['bytes'] for x in entries)}))
if __name__=='__main__':main()
