#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Compile/test actual SDK Results. Does not transform or install source files."""
import argparse,hashlib,json,os,pathlib,platform,shutil,subprocess,sys,time
if sys.flags.optimize: raise SystemExit("proof requires Python assertions enabled")
P=pathlib.Path
parser=argparse.ArgumentParser();parser.add_argument('--stage',required=True);parser.add_argument('--receiver',required=True);parser.add_argument('--providers',required=True);parser.add_argument('--cache',required=True)
a=parser.parse_args();stage=P(a.stage).resolve();receiver=P(a.receiver).resolve();providers=P(a.providers).resolve();cache=P(a.cache).resolve();task=P(__file__).resolve().parents[1]
proof=stage.parent/'runtime-proof';proof.mkdir(exist_ok=False);logs=proof/'logs';logs.mkdir();records=[]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def run(tag,cmd):
    t=time.monotonic();r=subprocess.run([str(x) for x in cmd],stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,timeout=180)
    (logs/(tag+'.stdout')).write_text(r.stdout);(logs/(tag+'.stderr')).write_text(r.stderr)
    records.append({'tag':tag,'command':[str(x) for x in cmd],'returncode':r.returncode,'seconds':round(time.monotonic()-t,3),'stdout_sha256':sha(logs/(tag+'.stdout')),'stderr_sha256':sha(logs/(tag+'.stderr'))})
    (proof/'commands.json').write_text(json.dumps(records,indent=2)+'\n')
    if r.returncode:raise RuntimeError(tag+' failed\n'+r.stdout[-10000:]+'\n'+r.stderr[-10000:])
    print('PASS',tag,flush=True);return r.stdout
for line in (task/'dependency-pins.tsv').read_text().splitlines():
    if not line or line.startswith('#'):continue
    path,h=line.split('\t');assert sha(receiver/path)==h,path
for line in (task/'provider-pins.tsv').read_text().splitlines():
    if not line or line.startswith('#'):continue
    path,h=line.split('\t');assert sha(providers/path)==h,path
sources=list(sorted(stage.rglob('*.java')))+list(sorted(stage.rglob('*.c')));before={str(p.relative_to(stage)):sha(p) for p in sources}
core=proof/'core';core.mkdir();classes=proof/'classes';classes.mkdir();headers=proof/'headers';headers.mkdir()
core_sources=sorted((receiver/'m3/collections/src/main/java').rglob('*.java'));assert len(core_sources)==78
run('core-compile',['javac','--release','21','-proc:none','-Xlint:all,-module','-Werror','-d',core,*core_sources])
run('core-jar',['jar','--create','--file',proof/'m3-collections.jar','-C',core,'.'])
assert run('core-java-base',['jdeps','--print-module-deps',proof/'m3-collections.jar']).strip()=='java.base'
provider_jars=[providers/p for p,h in [l.split('\t') for l in (task/'provider-pins.tsv').read_text().splitlines() if l and not l.startswith('#')]]
junit=[]
for p in cache.rglob('*.jar'):
    if ((p.name.startswith('junit-jupiter-') and p.name.endswith('-5.10.2.jar')) or (p.name.startswith('junit-platform-') and p.name.endswith('-1.10.2.jar')) or p.name.startswith(('opentest4j-','apiguardian-api-'))):junit.append(p)
assert any(p.name=='junit-platform-launcher-1.10.2.jar' for p in junit)
cp=os.pathsep.join(str(p) for p in [classes,core,*provider_jars,*junit])
java_sources=sorted(stage.rglob('*.java'));assert len(java_sources)==10
run('runtime-compile',['javac','--release','21','-proc:none','-Xlint:all','-Werror','-cp',cp,'-h',headers,'-d',classes,*java_sources,task/'proof/RuntimeJUnit.java'])
base=['java','-Xmx256m','-cp',cp]
java_result=run('java-contracts',base+['com.m3.collections.interop.M3MultisetContract'])
run('runtime-junit',base+['RuntimeJUnit'])
run('missing-library',base+['-Djava.library.path='+str(proof/'absent'),'com.m3.collections.interop.M3MultisetContract','missing'])
home=P(shutil.which('javac')).resolve().parents[1];native=stage/'m3/collections/interop/src/main/native/m3_collection_counts.c'
for mode,flags in [('release',['-O2']),('ubsan',['-O1','-fsanitize=undefined','-fno-sanitize-recover=all'])]:
    lib=proof/mode;lib.mkdir();so=lib/'libm3_collection_counts.so'
    run('native-'+mode,['gcc','-std=c17','-Wall','-Wextra','-Werror','-pedantic','-fPIC','-shared',*flags,'-I'+str(home/'include'),'-I'+str(home/'include/linux'),'-I'+str(headers),native,'-o',so])
    symbols=run('symbols-'+mode,['nm','-D','--defined-only',so]);assert 'Java_com_m3_collections_interop_M3CountAlgebra_combine0' in symbols
    run('jni-'+mode,['java','-Xmx256m','-Xcheck:jni','-Djava.library.path='+str(lib),'-cp',cp,'com.m3.collections.interop.M3MultisetContract','native'])
    assert not (logs/('jni-'+mode+'.stderr')).read_text(),'JNI stderr diagnostic'
repeat=proof/'repeat';repeat.mkdir()
run('repeat-compile',['javac','--release','21','-proc:none','-Xlint:all','-Werror','-cp',cp,'-d',repeat,*java_sources,task/'proof/RuntimeJUnit.java'])
original={str(p.relative_to(classes)):sha(p) for p in classes.rglob('*.class')};again={str(p.relative_to(repeat)):sha(p) for p in repeat.rglob('*.class')};assert original==again
main_names={p.stem for p in (stage/'m3/collections/interop/src/main/java').rglob('*.java')}
main_classes=sorted(p.relative_to(classes) for p in classes.rglob('*.class') if p.stem.split('$',1)[0] in main_names)
jar_cmd=['jar','--create','--file',proof/'m3-collections-interop-preview.jar']
for p in main_classes:jar_cmd.extend(['-C',classes,p])
run('runtime-jar',jar_cmd)
jar_listing=run('runtime-jar-contents',['jar','--list','--file',proof/'m3-collections-interop-preview.jar'])
assert not any(name in jar_listing for name in ('Test.class','Contract.class','RuntimeJUnit'))
run('public-api',['javap','-classpath',cp,'-public','-s','com.m3.collections.interop.M3Multiset','com.m3.collections.interop.M3Multisets','com.m3.collections.interop.M3CountAlgebra'])
assert before=={str(p.relative_to(stage)):sha(p) for p in sources},'source mutated during proof'
receipt={'status':'PASS_ACTUAL_SDK_GENERATED_JAVA_JNI','sdk_sources':before,'platform':platform.platform(),'core_main_sources':len(core_sources),'runtime_junit_tests':43,'java_contract_stdout':java_result.strip(),'repeat_class_files':len(original),'limitations':['finite bounded provider corpus','no full repository reactor/static admission','no JMH or performance claim','no production OS interner qualification','qualification applies to the recorded platform; other platforms need independent tests','optional JNI operates only on caller-aligned primitive arrays'],'commands':len(records)}
(proof/'qualification.json').write_text(json.dumps(receipt,indent=2)+'\n');print(json.dumps(receipt,indent=2))
