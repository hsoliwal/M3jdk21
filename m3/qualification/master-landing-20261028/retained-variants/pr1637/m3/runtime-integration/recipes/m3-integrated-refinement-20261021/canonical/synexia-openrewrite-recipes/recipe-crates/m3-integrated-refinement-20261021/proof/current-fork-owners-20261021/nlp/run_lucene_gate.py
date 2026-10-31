#!/usr/bin/env python3
"""Run the existing Java21 current Lucene staging/source/image owner unchanged."""
from pathlib import Path
import hashlib
import json
import os
import subprocess
import sys
import time

work=Path(__file__).resolve().parent
fork=work/'forks/lucene'
revision='873b6167603c69101d626ddffd8d733340300fea'
jdk=Path('/tmp/m3-build-tools/jdk-21.0.12.1+1')
root=work/'receiver-source'
packager=root/'m3/tooling/jmod-recipes/tools/jmod_pack.py'
recipe=root/'m3/vendor/synexia/synexia-openrewrite-recipes'
crate=recipe/'crates/m3-nlp-search/current-forks-20261018'
out=work/'lucene-owner-001'
log=work/'evidence/LUCENE_OWNER_001.log'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
assert subprocess.check_output(['git','-C',str(fork),'rev-parse','HEAD'],text=True).strip()==revision
assert subprocess.check_output(['git','-C',str(fork),'status','--porcelain'],text=True)==''
assert json.loads((work/'evidence/OPENNLP_OWNER_001.json').read_text())['status']=='PASS'
assert json.loads((work/'evidence/CURRENT_RECIPE_001.json').read_text())['status']=='PASS'
if out.exists() or log.exists():
    raise FileExistsError('fresh output and log required')
opennlp=work/'opennlp-owner-001'
modules=json.loads((opennlp/'BUILD.json').read_text())['modules']
module_jars=[opennlp/'jars'/(name+'.jar') for name in modules]
sources=[fork/p for p in subprocess.check_output(['git','-C',str(fork),'ls-files','-z']).decode().split('\0') if p]
sources+=sorted(packager.parent.glob('*.py'))
source_rows=json.loads((work/'evidence/RECEIVER_SOURCE_FILES.json').read_text())
sources+=[root/r['path'] for r in source_rows if r['path'].startswith('m3/vendor/synexia/synexia-openrewrite-recipes/')]
sources+=[p for directory in ('classes','test-classes') for p in (crate/'target'/directory).rglob('*') if p.is_file()]
sources+=[crate/'target/sdk-classpath.txt']
sources+=[Path(p) for p in (crate/'target/sdk-classpath.txt').read_text().strip().split(os.pathsep)]
sources+=[jdk/'release',*sorted((work/'dependencies/lucene').glob('*.jar')),*module_jars]
before={str(p):{'sha256':sha(p),'bytes':p.stat().st_size} for p in sorted(set(sources))}
inputs=work/'evidence/LUCENE_OWNER_001_INPUTS.json';inputs.write_text(json.dumps(before,indent=2)+'\n')
command=[sys.executable,'-B',str(fork/'.m3/current-jmods/build.py'),
    '--source',str(fork),'--jdk-home',str(jdk),'--packager',str(packager),
    '--dependency-dir',str(work/'dependencies/lucene'),'--out',str(out),
    '--recipe-runner',str(crate/'run_recipe.py'),'--recipe-resources',str(recipe/'src/main/resources')]
for path in module_jars:command+=['--opennlp-module',str(path)]
env=dict(os.environ,JAVA_HOME=str(jdk),JAVA_TOOL_OPTIONS='-Xmx512m -XX:ActiveProcessorCount=2 -XX:-UsePerfData',PYTHONDONTWRITEBYTECODE='1')
start=time.monotonic()
with log.open('x') as stream:
    stream.write(json.dumps(command)+'\n');stream.flush()
    result=subprocess.run(command,cwd=fork,stdout=stream,stderr=subprocess.STDOUT,env=env,timeout=600)
after={str(p):{'sha256':sha(p),'bytes':p.stat().st_size} for p in sorted(set(sources))}
readback=work/'evidence/LUCENE_OWNER_001_READBACK.json';readback.write_text(json.dumps(after,indent=2)+'\n')
clean=subprocess.check_output(['git','-C',str(fork),'status','--porcelain'],text=True)==''
passed=result.returncode==0 and after==before and clean and 'PASS 29 Lucene modules;' in log.read_text()
receipt={'status':'PASS' if passed else 'FAIL','scope':'Exact current Java21 staged Lucene donor source, real recipe and module-path/installed-image gates; combined M3 receiver remains separate',
    'fork_commit':revision,'command':command,'environment':{'JAVA_HOME':str(jdk),'JAVA_TOOL_OPTIONS':env['JAVA_TOOL_OPTIONS']},
    'exit_code':result.returncode,'seconds':round(time.monotonic()-start,3),'source_and_dependencies_unchanged':after==before,
    'fork_tracked_clean':clean,'inputs_sha256':sha(inputs),'readback_sha256':sha(readback),'log_sha256':sha(log),
    'external_dependencies':'16 separately SHA-locked artifacts plus17 previously qualified OpenNLP named modules; external archives are not source-built',
    'runtime_dependency_resolution':'--dependency-dir selects existing locked archives; staging and runtime invoke no Maven/download step',
    'combined_receiver':'REFUSED existing source lock drift; see CURRENT_RECEIVER_001.json',
    'limits':['Not the original Gradle25 randomized full suite','Java21 preview is explicitly enabled','Faiss native library and optional external models remain separate','No full M3JDK/String/HotSpot or performance promotion']}
if (out/'BUILD_REPORT.json').exists():
    report=json.loads((out/'BUILD_REPORT.json').read_text())
    receipt['build_report_sha256']=sha(out/'BUILD_REPORT.json')
    receipt['production_modules']=len(report['production_modules'])
    receipt['java_sources']=sum(v['java_sources'] for v in report['production_modules'])
    receipt['named_modules']=report['named_modules']
    receipt['source_recipe_changes']=report['source_recipe_changes']
    receipt['source_recipe_fixed_point_changes']=report['source_recipe_fixed_point_changes']
    receipt['descriptor_recipe_fixed_point_changes']=report['descriptor_recipe_fixed_point_changes']
    receipt['runtime_checks']=report['tests']
path=work/'evidence/LUCENE_OWNER_001.json';path.write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps({k:v for k,v in receipt.items() if k!='command'},indent=2),flush=True)
raise SystemExit(0 if passed else 1)
