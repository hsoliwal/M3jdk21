#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors.
"""Verify SDK-generated files and compile/run a private target receiver. No product writes."""
from pathlib import Path
import hashlib,json,os,re,shutil,subprocess,sys,time
TASK=Path(__file__).resolve().parent
BUILD=Path(sys.argv[1]).resolve(); OUT=BUILD/'runtime-proof';OUT.mkdir(parents=True,exist_ok=False)
RES=TASK/'src/main/resources/com/synexia/rewrite/flow-jdk21'
SDK=BUILD/'sdk-image';CLASSES=BUILD/'classes'
if not (SDK/'SDK.txt').is_file():raise SystemExit('actual named SDK image required')
rows=[line.split('\t') for line in (RES/'manifest.tsv').read_text().splitlines()]
def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
for path,before,after,template in rows:
 if digest(SDK/path)!=after:raise SystemExit('SDK postimage mismatch: '+path)
 if template!='KEEP' and digest(RES/template)!=after:raise SystemExit('template drift: '+path)
commands=[]
def run(name,args):
 begin=time.monotonic();p=subprocess.run([str(a) for a in args],cwd=TASK,text=True,capture_output=True,timeout=180)
 (OUT/(name+'.log')).write_text(p.stdout+p.stderr)
 commands.append(dict(name=name,argv=[str(a) for a in args],exit=p.returncode,seconds=time.monotonic()-begin))
 (OUT/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');print(name,p.returncode,flush=True)
 if p.returncode:raise RuntimeError(name+'\n'+p.stdout[-4000:]+p.stderr[-4000:])
 return p.stdout
receiver=OUT/'receiver';receiver.mkdir()
for path,before,after,template in rows:
 if before!='ABSENT':
  p=receiver/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((RES/'preimages'/Path(path).name).read_bytes())
apply=run('apply',['java','-ea','-cp',CLASSES,'com.synexia.rewrite.CollectionFlowDriver',RES,receiver])
assert apply.strip()=='SOURCE_SEAL_APPLY='+str(sum(r[1]=='ABSENT' for r in rows))
assert run('replay',['java','-ea','-cp',CLASSES,'com.synexia.rewrite.CollectionFlowDriver',RES,receiver]).strip()=='SOURCE_SEAL_APPLY=0'
for path,before,after,template in rows:assert digest(receiver/path)==after and (receiver/path).read_bytes()==(SDK/path).read_bytes(),path
main=receiver/'m3/collections/src/main/java';tests=receiver/'m3/collections/src/test/java'
module=OUT/'module';tc=OUT/'tests';headers=OUT/'headers'
for p in [module,tc,headers]:p.mkdir()
run('module-javac',['javac','--release','21','-proc:none','-Xlint:all,-module','-Werror','-h',headers,'-d',module,*sorted(main.rglob('*.java'))])
run('test-javac',['javac','--release','21','-proc:none','-Xlint:all','-Werror','-cp',module,'-d',tc,*sorted(tests.rglob('*.java'))])
home=Path(os.environ.get('JAVA_HOME',str(Path(shutil.which('javac')).resolve().parents[1])))
lib=OUT/'libm3flowreceiver.so'
run('native-gcc',['gcc','-std=c17','-O2','-Wall','-Wextra','-Werror','-shared','-fPIC','-fsanitize=undefined','-fno-sanitize-recover=all','-I'+str(home/'include'),'-I'+str(home/'include/linux'),'-I'+str(headers),'-include',headers/'com_m3_collections_M3NativeLongs.h',*sorted((receiver/'m3/collections/src/main/native/collections').glob('*.c')),'-o',lib])
symbols=run('native-symbols',['nm','-D','--defined-only',lib]);expected=set()
for header in headers.glob('*.h'):expected.update(re.findall(r'JNICALL\s+(Java_\w+)',header.read_text()))
actual={line.split()[-1] for line in symbols.splitlines() if line.split()[-1].startswith('Java_')};assert expected==actual,(expected,actual)
cp=str(tc)+os.pathsep+str(module)
for proof in ['FlowV2Proof','FlowV3Proof','ClosurePreparedProof','M3FlowReceiverProof']:
 run(proof+'-java',['java','-ea','-cp',cp,'com.m3.collections.'+proof])
 run(proof+'-jni',['java','-ea','-Xcheck:jni','-cp',cp,'com.m3.collections.'+proof,lib])
client=OUT/'client-src';client.mkdir();(client/'module-info.java').write_text('module m3.receiver.client { requires com.m3.collections; }\n')
(client/'example').mkdir();(client/'example/Client.java').write_text('''package example;
import com.m3.collections.*;
public final class Client {
 private Client() { }
 public static void main(String[] args) {
  long v=M3LongFlow.of(new long[]{1,2,3}).windowSum(2,1,M3Windows.Tail.DROP).accumulate(0,Long::sum).findFirst().orElseThrow();
  if(v!=8)throw new AssertionError(v);
  System.out.println("NAMED_MODULE_CLIENT=PASS");
 }
}
''')
run('client-javac',['javac','--release','21','-proc:none','-Xlint:all,-module','-Werror','--module-path',module,'-d',OUT/'client',*sorted(client.rglob('*.java'))])
run('client-java',['java','--module-path',str(module)+os.pathsep+str(OUT/'client'),'-m','m3.receiver.client/example.Client'])
run('diagnostic-javac',['javac','--release','21','-proc:none','-Xlint:all','-Werror','-cp',module,'-d',tc,TASK/'diagnostics/AllocationProbeV2.java'])
for fork in range(3):run('allocation-fork-'+str(fork),['java','-cp',cp,'com.m3.collections.AllocationProbeV2'])
for path,before,after,template in rows:
 if template!='KEEP' and ('/src/main/java/' in path or path.endswith('.c')):
  text=(receiver/path).read_text();assert 'com.synexia.' not in text and 'com/synexia/' not in text and 'com_synexia_' not in text,path
(OUT/'RESULT.json').write_text(json.dumps(dict(status='PASS',source_seals=len(rows),additions=sum(r[1]=='ABSENT' for r in rows),fixed_point=0,generated_native_symbols=sorted(expected),source_sha256={r[0]:r[2] for r in rows},limitations=['selected-source named module compile; not full product module/reactor','not source-built JDK/jtreg','Linux JNI only','no installed target writes']),indent=2)+'\n')
print('TARGET_JAVA_JNI=PASS',flush=True)
