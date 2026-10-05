#!/usr/bin/env python3
"""Stage seven immutable terminal consumer trials for the retained metadata packager."""
from pathlib import Path
import argparse,hashlib,json,os,shutil,subprocess
R=Path(__file__).resolve().parents[1]
PINS={'consumer-v4':'65ad6376f5e88ca2bd1e33e549c262a50bc58952ede7af8c17c9b0beed05ed73','consumer-v3':'0d50bac3af04f4c1b498ca34c3b6c57ad76bf97aa3b51fbba1dc926d4b52d2a7','consumer-v2':'d8591a5652baf9c92902e6b1475b773fec4a1f498c1dbd6fe55dcffc002038d7','consumer-v1':'47a1efd8b72eb173d3c48124e549e7ecb28d332be09e97bc63290989ba34ba92','receiving-python-v1':'be337d009319bab7c528f4ab6e61ada6ef5f407a33d070729e8d5cd68937a63e','receiving-python-v2':'687eed40f8cfd7c93a663dbce1bb9fbf7a80626d798fa2f502e73c1a4bbab0cc'}
PACKAGER='99a140af284c3280d285d039e59cbebee4e961ae530fe4117e1421e485a09f9b'

def sha(b):return hashlib.sha256(b).hexdigest()
def require(c,m):
 if not c:raise ValueError(m)
def write(p,d):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,indent=2,sort_keys=True)+'\n')
def inventory(root):
 rows=[]
 for p in root.rglob('*'):
  require(not p.is_symlink(),'evidence symlink')
  if p.is_file():rows.append({'path':p.relative_to(root).as_posix(),'sha256':sha(p.read_bytes()),'bytes':p.stat().st_size})
  else:require(p.is_dir(),'special evidence entry')
 return sorted(rows,key=lambda r:r['path'])

def main():
 ap=argparse.ArgumentParser();ap.add_argument('--final-lane',required=True);ap.add_argument('--final-result-sha',required=True);ap.add_argument('--output',type=Path,required=True);a=ap.parse_args()
 require(a.final_lane.startswith('consumer-v') and a.final_lane not in PINS and '/' not in a.final_lane,'fresh final consumer lane name')
 pins=dict(PINS);pins[a.final_lane]=a.final_result_sha;out=a.output.absolute();require(not out.exists(),'fresh packaging destination')
 selected={};states={}
 for lane,pin in pins.items():
  p=R/'evidence'/lane;data=(p/'RESULT.json').read_bytes();require(sha(data)==pin,'exact original result seal '+lane);result=json.loads(data)
  state=result['state'];require(state.startswith(('PASS_','STOPPED')),'original terminal state required')
  require(result['productWrites']==0 and result['canonicalProductionApplied'] is False,'conservative scope required')
  for rel,digest in result['evidence'].items():require(sha((p/rel).read_bytes())==digest,'original evidence receipt drift '+lane+'/'+rel)
  selected[lane]=inventory(p);states[lane]={'resultSHA256':pin,'state':state}
 require(states['consumer-v4']['state']=='STOPPED' and states['consumer-v3']['state']=='STOPPED' and states['consumer-v2']['state']=='STOPPED' and states['consumer-v1']['state']=='STOPPED' and states['receiving-python-v1']['state']=='PASS_BOUNDED_PYTHON_RECEIVING_COMPONENTS' and states['receiving-python-v2']['state']=='PASS_BOUNDED_PYTHON_RECEIVING_COMPONENTS','preserve exact prior dispositions')
 package=R/'packaging/package_evidence_state_v1.py';require(sha(package.read_bytes())==PACKAGER,'reviewed metadata-only status reader')
 out.mkdir(parents=True);stage=out/'staged';stage.mkdir()
 for lane in pins:
  shutil.copytree(R/'evidence'/lane,stage/'evidence'/lane,copy_function=os.link)
  require(inventory(stage/'evidence'/lane)==selected[lane],'exact original copied bytes')
 licenses=R/'publication-extra/licenses-752';shutil.copytree(licenses,stage/'openjdk-752-attribution')
 shutil.copyfile(package,stage/'package_evidence_state_v1.py')
 for name in ['state-reader.patch','STATE-READER.json']:shutil.copyfile(R/'packaging'/name,stage/name)
 write(stage/'COPY-CUSTODY.json',{'schema':'m3.consumer-evidence-copy/1','terminalResults':states,'originalInventories':selected,'originalAbsolutePathsPreserved':True,'stagingPurpose':'Fresh directories with immutable same-filesystem hard-linked proof file bodies; no staged proof file is written. Original proof inventories are checked before and after packaging; original inputs keep absolute custody paths. Source and working-view file bodies are preserved under copied lane paths; dependency/tool caches are external identity-only references.','proofGatesExecuted':False,'receiptsRewritten':False})
 plan={'schema':'synexia.evidence.package-plan/1','status':'FINAL_REVIEWED_SELECTION','laneSelection':{'required':list(pins),'additional':[],'includeAllExisting':False,'allowNotExecuted':[]},'includePaths':['openjdk-752-attribution','COPY-CUSTODY.json','package_evidence_state_v1.py','state-reader.patch','STATE-READER.json','package-plan.json'],'packageOwnerSHA256':PACKAGER,'scope':'Seven exact terminal trials: failed full consumer-v1, consumer-v2, consumer-v3 and consumer-v4, successful scoped receiving-python-v1 and receiving-python-v2 components, and declared final current consumer trial. Original state-based RESULT bytes remain untouched; metadata-only status fallback supports the original fields.','terminalResults':states,'compiledBinaries':'hash-only','toolCaches':'external-manifest-only','proofGatesExecutedByPackaging':False}
 write(stage/'package-plan.json',plan)
 command=[str(R.parent.parent/'m3-python/bin/python'),str(package),'pack','--root',str(stage),'--plan',str(stage/'package-plan.json'),'--output',str(out/'evidence-bundle')]
 with (out/'pack.stdout.log').open('xb') as stdout,(out/'pack.stderr.log').open('xb') as stderr:run=subprocess.run(command,stdout=stdout,stderr=stderr,check=False)
 write(out/'PACK-COMMAND.json',{'command':command,'exitCode':run.returncode,'proofGatesExecuted':False,'stdoutSHA256':sha((out/'pack.stdout.log').read_bytes()),'stderrSHA256':sha((out/'pack.stderr.log').read_bytes())})
 require(run.returncode==0,'evidence packaging refused; preserve failure')
 for lane in pins:require(inventory(R/'evidence'/lane)==selected[lane],'original proof changed during packaging')
 print(json.dumps({'state':'PACKAGED_ORIGINAL_TERMINAL_EVIDENCE','bundleSHA256':sha((out/'evidence-bundle/BUNDLE.json').read_bytes()),'originalProofsUnchanged':True,'proofGatesExecuted':False}))

if __name__=='__main__':main()
