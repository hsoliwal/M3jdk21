#!/usr/bin/env python3
"""Record an unchanged, exact-source donor gate with immutable input readbacks."""
from pathlib import Path
import hashlib
import json
import os
import subprocess
import sys
import time

work=Path(__file__).resolve().parent
name=sys.argv[1]
if name != 'opennlp':
    raise ValueError('Only the admitted current OpenNLP owner is selected')
fork=work/'forks/opennlp'
revision='8b647d1d389736952e43fb46865c874747f7acde'
jdk=Path('/tmp/m3-build-tools/jdk-21.0.12.1+1')
packager=work/'receiver-source/m3/tooling/jmod-recipes/tools/jmod_pack.py'
out=work/'opennlp-owner-001'
log=work/'evidence/OPENNLP_OWNER_001.log'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
assert subprocess.check_output(['git','-C',str(fork),'rev-parse','HEAD'],text=True).strip()==revision
assert subprocess.check_output(['git','-C',str(fork),'status','--porcelain'],text=True)==''
if out.exists() or log.exists():
    raise FileExistsError('fresh output and log required')
sources=[fork/p for p in subprocess.check_output(['git','-C',str(fork),'ls-files','-z']).decode().split('\0') if p]
sources+=sorted(packager.parent.glob('*.py'))
sources+=[jdk/'release',*sorted((work/'dependencies/opennlp').glob('*.jar'))]
before={str(p):{'sha256':sha(p),'bytes':p.stat().st_size} for p in sources}
inputs=work/'evidence/OPENNLP_OWNER_001_INPUTS.json'
inputs.write_text(json.dumps(before,indent=2)+'\n')
command=[sys.executable,'-B',str(fork/'dev/m3-jmod/build.py'),
    '--source-root',str(fork),'--jdk-home',str(jdk),'--packager',str(packager),
    '--dependency-dir',str(work/'dependencies/opennlp'),'--out',str(out)]
env=dict(os.environ,JAVA_HOME=str(jdk),JAVA_TOOL_OPTIONS='-Xmx512m -XX:ActiveProcessorCount=2 -XX:-UsePerfData',PYTHONDONTWRITEBYTECODE='1')
start=time.monotonic()
with log.open('x') as stream:
    stream.write(json.dumps(command)+'\n');stream.flush()
    result=subprocess.run(command,cwd=fork,stdout=stream,stderr=subprocess.STDOUT,env=env,timeout=600)
after={str(p):{'sha256':sha(p),'bytes':p.stat().st_size} for p in sources}
readback=work/'evidence/OPENNLP_OWNER_001_READBACK.json'
readback.write_text(json.dumps(after,indent=2)+'\n')
unchanged=after==before
clean=subprocess.check_output(['git','-C',str(fork),'status','--porcelain'],text=True)==''
passed=result.returncode==0 and unchanged and clean and 'OPENNLP_JMOD_PASS ' in log.read_text()
receipt={'status':'PASS' if passed else 'FAIL','scope':'Exact current Java21 staged OpenNLP donor source and installed-image gate; combined M3 receiver remains separate',
    'fork_commit':revision,'command':command,'environment':{'JAVA_HOME':str(jdk),'JAVA_TOOL_OPTIONS':env['JAVA_TOOL_OPTIONS']},
    'exit_code':result.returncode,'seconds':round(time.monotonic()-start,3),'source_and_dependencies_unchanged':unchanged,
    'fork_tracked_clean':clean,'inputs_sha256':sha(inputs),'readback_sha256':sha(readback),'log_sha256':sha(log),
    'external_dependencies':'14 separately SHA-locked artifacts; not claimed source-built',
    'runtime_dependency_resolution':'--dependency-dir selects existing locked archives; source gate invokes no Maven/download step',
    'combined_receiver':'REFUSED existing source lock drift; see CURRENT_RECEIVER_001.json',
    'limits':['Not the original Maven full reactor suite','No original Java25 Lucene lane','No complete M3JDK or String/HotSpot phase promotion','DL/GPU and optional external models remain separate']}
if (out/'BUILD.json').exists():
    build=json.loads((out/'BUILD.json').read_text())
    receipt['build_receipt_sha256']=sha(out/'BUILD.json')
    receipt['core_sources']=len(build['source_files'])
    receipt['built_sources']=len(build['source_files'])+sum(len(v['source_files']) for v in build['extensions'].values() if v['disposition']=='BUILT')
    receipt['named_modules']=len(build['modules'])
path=work/'evidence/OPENNLP_OWNER_001.json';path.write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps(receipt,indent=2),flush=True)
raise SystemExit(0 if passed else 1)
