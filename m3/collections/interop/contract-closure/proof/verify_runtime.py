#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Verify actual SDK outputs. Copies for isolated proof are exact; no source transformation occurs here."""
import argparse,hashlib,json,os,pathlib,platform,shutil,subprocess,sys,time
if sys.flags.optimize: raise SystemExit('Assertions must be enabled')
P=pathlib.Path
parser=argparse.ArgumentParser()
for name in ['stage','receiver','providers','cache']:parser.add_argument('--'+name,required=True)
a=parser.parse_args();stage=P(a.stage).resolve();receiver=P(a.receiver).resolve();providers=P(a.providers).resolve();cache=P(a.cache).resolve();task=P(__file__).resolve().parents[1]
proof=stage.parent/'closure-proof';proof.mkdir(exist_ok=False);logs=proof/'logs';logs.mkdir();records=[]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def run(tag,cmd,empty_stderr=False):
    started=time.monotonic();result=subprocess.run(list(map(str,cmd)),stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,timeout=120)
    out=logs/(tag+'.stdout');err=logs/(tag+'.stderr');out.write_text(result.stdout);err.write_text(result.stderr)
    records.append(dict(tag=tag,command=list(map(str,cmd)),returncode=result.returncode,seconds=round(time.monotonic()-started,3),stdout_sha256=sha(out),stderr_sha256=sha(err)))
    (proof/'commands.json').write_text(json.dumps(records,indent=2)+'\n')
    if result.returncode:raise RuntimeError(tag+' failed\n'+result.stdout[-14000:]+'\n'+result.stderr[-14000:])
    if empty_stderr:assert not result.stderr,tag+' diagnostic stderr'
    print('PASS',tag,flush=True);return result.stdout
for manifest,root in [('provider-pins.tsv',providers),('dependency-pins.tsv',receiver)]:
    for row in (task/manifest).read_text().splitlines():
        if row and not row.startswith('#'):
            path,h=row.split('\t');assert sha(root/path)==h,path
before={str(p.relative_to(stage)):sha(p) for p in stage.rglob('*') if p.is_file()}
# Requalify actual SDK predecessor results, without altering its exact source inventory or guards.
parent=stage.parent/'parent';parent.mkdir();parentstage=parent/'sdk-output';parentstage.mkdir()
old=receiver/'m3/collections/interop/multiset-recipe'
for row in (receiver/'m3/collections/interop/qualification/multiset-recipe-receipt.tsv').read_text().splitlines():
    if row and not row.startswith('#'):
        path,h=row.split('\t');assert sha(stage/path)==h,path;dest=parentstage/path;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(stage/path,dest)
run('unchanged-parent-java-jni',[sys.executable,old/'proof/verify_runtime.py','--stage',parentstage,'--receiver',receiver,'--providers',providers,'--cache',cache])
parentproof=parent/'runtime-proof';core=parentproof/'core';parentclasses=parentproof/'classes'
classes=proof/'classes';classes.mkdir()
providerjars=[providers/row.split('\t')[0] for row in (task/'provider-pins.tsv').read_text().splitlines() if row and not row.startswith('#')]
junit=[]
for p in cache.rglob('*.jar'):
    if ('sources' not in p.name and ((p.name.startswith('junit-jupiter-') and p.name.endswith('-5.10.2.jar')) or (p.name.startswith('junit-platform-') and p.name.endswith('-1.10.2.jar')) or p.name.startswith(('opentest4j-','apiguardian-api-')))):junit.append(p)
cp=os.pathsep.join(map(str,[classes,parentclasses,core,*providerjars,*junit]));base=['java','-Xmx64m','-XX:ActiveProcessorCount=4','-cp',cp]
source=stage/'m3/collections/interop/apache/src';tests=sorted((source/'test/java').rglob('*.java'));main=source/'main/java/com/m3/collections/interop'
assert len(tests)==3
compilebase=['javac','--release','21','-proc:none','-Xlint:all','-Werror','-cp',cp,'-d',classes]
run('immutable-contract-tests-compile',compilebase+tests+[task/'proof/ClosureJUnit.java'])
# Compilation/behavior after each dependent production leaf, not a whole-output-only success claim.
for name,tag,n in [('M3ApacheMultiSet','counts',7),('M3ApacheCollections','maps',12),('M3ApacheCollectors','collectors',4)]:
    run('compile-'+name,compilebase+[main/(name+'.java')])
    run('contracts-'+tag,base+['ClosureJUnit',tag,str(n)])
run('all-apache-contracts',base+['ClosureJUnit','all','23'])
run('range-low-heap',['java','-Xmx32m','-XX:ActiveProcessorCount=4','-cp',cp,'com.m3.collections.interop.M3RangeClosureContract'])
run('range-repeat',['java','-Xmx32m','-XX:ActiveProcessorCount=4','-cp',cp,'com.m3.collections.interop.M3RangeClosureContract'])
assert (logs/'range-low-heap.stdout').read_bytes()==(logs/'range-repeat.stdout').read_bytes(),'range trace nondeterministic'
run('blocking-deque-concurrency',base+['com.m3.collections.interop.M3ConcurrentClosureContract'])
for mode in ['release','ubsan']:
    run('native-concurrent-'+mode,['java','-Xmx32m','-XX:ActiveProcessorCount=4','-Xcheck:jni','-Djava.library.path='+str(parentproof/mode),'-cp',cp,'com.m3.collections.interop.M3ConcurrentClosureContract','native'],True)
repeat=proof/'repeat';repeat.mkdir();alljava=sorted(source.rglob('*.java'))
run('repeat-compile',['javac','--release','21','-proc:none','-Xlint:all','-Werror','-cp',cp,'-d',repeat,*alljava,task/'proof/ClosureJUnit.java'])
original={str(p.relative_to(classes)):sha(p) for p in classes.rglob('*.class')};again={str(p.relative_to(repeat)):sha(p) for p in repeat.rglob('*.class')};assert original==again,'class reproducibility'
main_names={p.stem for p in main.glob('*.java')};main_classes=sorted(p.relative_to(classes) for p in classes.rglob('*.class') if p.stem.split('$')[0] in main_names)
jar=proof/'m3-collections-apache-preview.jar';cmd=['jar','--create','--file',jar]
for p in main_classes:cmd+=['-C',classes,p]
run('main-only-preview',cmd);listing=run('preview-listing',['jar','--list','--file',jar]);assert not any(x in listing for x in ['Contract','ClosureJUnit','Test.class'])
run('public-api',['javap','-classpath',cp,'-public','-s',*['com.m3.collections.interop.'+n for n in sorted(main_names)]])
assert before=={str(p.relative_to(stage)):sha(p) for p in stage.rglob('*') if p.is_file()},'source mutation'
receipt=dict(status='PASS_BOUNDED_APACHE_CLOSURE',platform=platform.platform(),source_hashes=before,apache_junit_tests=23,predecessor_runtime_tests=43,repeat_class_files=len(original),range_trace=(logs/'range-low-heap.stdout').read_text().strip(),native_concurrent_calls_per_execution=896,limitations=['finite contract corpora, not exhaustive API/JMM proof','no whole-reactor or installed-JDK admission','no automatic pressure/spill integration claimed','dependency binary compatibility pin, production mirror admission separate','no performance improvement claimed'])
(proof/'qualification.json').write_text(json.dumps(receipt,indent=2)+'\n');print(json.dumps(receipt,indent=2))
