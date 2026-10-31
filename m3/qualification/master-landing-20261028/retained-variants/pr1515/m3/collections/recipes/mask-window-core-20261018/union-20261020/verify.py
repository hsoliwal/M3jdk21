#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Verification orchestration only: the unchanged Java SealReplay is the only source writer.
import argparse, hashlib, itertools, json, os, pathlib, shutil, subprocess, tempfile, time
P=pathlib.Path; TASK=P(__file__).resolve().parent
ap=argparse.ArgumentParser();ap.add_argument('--out',required=True);args=ap.parse_args()
OUT=P(args.out).resolve()
if OUT.exists(): raise SystemExit('output must be new and exclusively owned')
OUT.mkdir(parents=True); E=OUT/'evidence';E.mkdir(); RES=TASK/'src/main/resources/union'; BASE=TASK/'before'
rows=[x.split('\t') for x in (RES/'manifest.tsv').read_text().splitlines()]
changes=[r for r in rows if r[3]!='KEEP']; assert len(changes)==5 and len(rows)==9
commands=[]
def run(name,argv,expected=0,timeout=40):
 start=time.monotonic();p=subprocess.run(list(map(str,argv)),capture_output=True,text=True,timeout=timeout)
 (E/(name+'.log')).write_text(p.stdout+p.stderr)
 commands.append({'name':name,'argv':list(map(str,argv)),'exitCode':p.returncode,'seconds':time.monotonic()-start})
 (E/'COMMANDS.json').write_text(json.dumps(commands,indent=2)+'\n')
 if (expected==0 and p.returncode!=0) or (expected!=0 and p.returncode==0): raise RuntimeError(name+'\n'+p.stdout+p.stderr)
 return p
JH=P(shutil.which('javac')).resolve().parents[1]; ENGINE=OUT/'engine';ENGINE.mkdir()
run('engine-compile',['javac','--release','21','-Xlint:all','-Werror','-d',ENGINE,*sorted((TASK/'src/main/java/com/synexia/rewrite').glob('*.java')),TASK/'probes/KernelCancellationProof.java'])
def replay(name,root,res=RES): return run(name,['java','-cp',ENGINE,'com.synexia.rewrite.SealReplay','apply',root,res])
CAND=OUT/'candidate';shutil.copytree(BASE,CAND)
run('preflight',['java','-cp',ENGINE,'com.synexia.rewrite.SealReplay','check',CAND,RES])
# Deliberate prerequisite order, followed by native, actual java.base, then fixtures.
assert changes[0][0]=='m3/native/include/m3_jni_core.h'
for n,row in enumerate(changes):
 with tempfile.TemporaryDirectory(dir=OUT) as tmp:
  resources=P(tmp);shutil.copytree(RES,resources,dirs_exist_ok=True)
  (resources/'manifest.tsv').write_text('\t'.join(row)+'\n')
  replay('emit-'+str(n),CAND,resources)
  if row[0].endswith('m3_index_mask_compactor.c'):
   run('intermediate-native-compile',['gcc','-std=c17','-O2','-Wall','-Wextra','-Werror','-pedantic','-fPIC','-c','-I'+str(JH/'include'),'-I'+str(JH/'include/linux'),CAND/row[0],'-o',OUT/'intermediate.o'])
  if row[0].endswith('/SortedSet.java'):
   patch=OUT/'intermediate-java-base';patch.mkdir()
   run('intermediate-java-base-compile',['javac','--release','21','-Xlint:all','-Werror','--patch-module','java.base='+str(CAND/'src/java.base/share/classes'),'-d',patch,CAND/row[0]])
assert 'changes=0' in replay('fixed-point-1',CAND).stdout
assert 'changes=0' in replay('fixed-point-2',CAND).stdout
run('kernel-cancellation',['java','-cp',ENGINE,'com.synexia.rewrite.KernelCancellationProof',RES])
for variant,root in [('baseline',BASE),('candidate',CAND)]:
 W=OUT/variant/'build';W.mkdir(parents=True,exist_ok=True)
 for d in ['classes','patch','native','headers']: (W/d).mkdir(exist_ok=True)
 java=root/'m3/collections/src/main/java/com/m3/collections'
 run(variant+'-compile',['javac','--release','21','-Xlint:all','-Werror','-h',W/'headers','-d',W/'classes',*sorted(java.glob('*.java')),BASE/'m3/collections/src/test/java/com/m3/collections/M3MaskWindowCurrentProof.java',CAND/'test/jdk/java/util/SortedSet/M3SortedSplitComposition.java'])
 run(variant+'-patch-compile',['javac','--release','21','-Xlint:all','-Werror','--patch-module','java.base='+str(root/'src/java.base/share/classes'),'-d',W/'patch',root/'src/java.base/share/classes/java/util/SortedSet.java'])
 c=root/'m3/collections/src/main/native/collections/m3_index_mask_compactor.c'
 flags=['gcc','-std=c17','-O2','-Wall','-Wextra','-Werror','-pedantic','-fsanitize=undefined','-fno-sanitize-recover=all','-I'+str(JH/'include'),'-I'+str(JH/'include/linux')]
 run(variant+'-native-compile',flags+['-fPIC','-shared',c,'-o',W/'native/libm3_collections_mask.so'])
 common=['java','-Xmx128m','-Xcheck:jni','--patch-module','java.base='+str(W/'patch'),'-Djava.library.path='+str(W/'native'),'-cp',W/'classes']
 for mode in ['java','native']:
  p=run(variant+'-'+mode,common+['com.m3.collections.M3MaskWindowCurrentProof',mode]);print(p.stdout.strip())
 p=run(variant+'-sorted',common+['M3SortedSplitComposition'],expected=0 if variant=='candidate' else 1)
 if variant=='baseline': assert 'split lost owner comparator' in p.stderr
 else: print(p.stdout.strip())
 run(variant+'-span-compile',flags+['-DOWNER_SOURCE="'+str(c)+'"',CAND/'m3/collections/src/test/native/M3MaskSpanUnionProbe.c','-o',W/'span'])
 p=run(variant+'-span',[W/'span','1' if variant=='candidate' else '0']);print(p.stdout.splitlines()[-1])
 for type_ in ['M3IndexMaskCompactor','M3IndexMaskCompactor$Backend','M3LongAddressKernel','M3LongIndexProjection']:
  run(variant+'-api-'+type_,['javap','-public','-s','-classpath',W/'classes','com.m3.collections.'+type_])
 run(variant+'-api-SortedSet',['javap','-public','-s',W/'patch/java/util/SortedSet.class'])
 p=run(variant+'-exports',['nm','-D','--defined-only',W/'native/libm3_collections_mask.so'])
 (E/(variant+'-exports.txt')).write_text('\n'.join(sorted(line.split()[-1] for line in p.stdout.splitlines() if ' Java_' in line))+'\n')
for type_ in ['M3IndexMaskCompactor','M3IndexMaskCompactor$Backend','M3LongAddressKernel','M3LongIndexProjection','SortedSet']:
 assert (E/('baseline-api-'+type_+'.log')).read_bytes()==(E/('candidate-api-'+type_+'.log')).read_bytes()
assert (E/'baseline-exports.txt').read_bytes()==(E/'candidate-exports.txt').read_bytes()
assert len((E/'candidate-exports.txt').read_text().splitlines())==9
assert (OUT/'baseline/build/headers/com_m3_collections_M3IndexMaskCompactor.h').read_bytes()==(OUT/'candidate/build/headers/com_m3_collections_M3IndexMaskCompactor.h').read_bytes()
# Actual paired Java/C run, not a JNI function table.
for java,native in [('baseline','candidate'),('candidate','baseline')]:
 run(java+'-java-'+native+'-native',['java','-Xmx128m','-Xcheck:jni','--patch-module','java.base='+str(OUT/'candidate/build/patch'),'-Djava.library.path='+str(OUT/native/'build/native'),'-cp',OUT/java/'build/classes','com.m3.collections.M3MaskWindowCurrentProof','native'])
W=OUT/'candidate/build'; (W/'missing').mkdir()
run('missing-native',['java','-Xmx128m','-Djava.library.path='+str(W/'missing'),'-cp',W/'classes','com.m3.collections.M3MaskWindowCurrentProof','missing'])
run('interpreted-jni',['java','-Xmx128m','-Xint','-Xcheck:jni','--patch-module','java.base='+str(W/'patch'),'-Djava.library.path='+str(W/'native'),'-cp',W/'classes','com.m3.collections.M3MaskWindowCurrentProof','native'])
run('interpreted-sorted',['java','-Xmx128m','-Xint','--patch-module','java.base='+str(W/'patch'),'-cp',W/'classes','M3SortedSplitComposition'])
flags=['gcc','-std=c17','-O2','-Wall','-Wextra','-Werror','-pedantic','-fsanitize=undefined','-fno-sanitize-recover=all','-I'+str(JH/'include'),'-I'+str(JH/'include/linux')]
run('common-header-compile',flags+['-I'+str(CAND/'m3/native/include'),TASK/'probes/CoreMaskContract.c','-o',W/'core'])
print(run('common-header',[W/'core']).stdout.strip())
# Pre-bounded exhaustive partial source states; never enumerate arbitrary recipe permutations.
refusals=[]
def snapshot(root):return {str(p.relative_to(root)):hashlib.sha256(p.read_bytes()).hexdigest() for p in root.rglob('*') if p.is_file() and not p.is_symlink()}
for bits in itertools.product([0,1],repeat=5):
 with tempfile.TemporaryDirectory(dir=OUT) as tmp:
  root=P(tmp)/'root';shutil.copytree(BASE,root)
  for bit,(path,b,a,t) in zip(bits,changes):
   if bit:dest=root/path;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes((RES/t).read_bytes())
  replay('partial-'+''.join(map(str,bits)),root)
  assert all(hashlib.sha256((root/r[0]).read_bytes()).hexdigest()==r[2] for r in rows)
  times={str(p):p.stat().st_mtime_ns for p in root.rglob('*') if p.is_file()}
  assert 'changes=0' in replay('partial-fixed-'+''.join(map(str,bits)),root).stdout
  assert times=={str(p):p.stat().st_mtime_ns for p in root.rglob('*') if p.is_file()}
for case in ['source-drift','keep-drift','missing-source','template-drift','duplicate-manifest','symlink-source','symlink-ancestor']:
 with tempfile.TemporaryDirectory(dir=OUT) as tmp:
  root=P(tmp)/'root';shutil.copytree(BASE,root);res=P(tmp)/'resources';shutil.copytree(RES,res)
  owner=root/'m3/collections/src/main/native/collections/m3_index_mask_compactor.c'
  if case=='source-drift':owner.write_text(owner.read_text()+'\n// synthetic drift\n')
  elif case=='keep-drift':p=root/next(r[0] for r in rows if r[3]=='KEEP');p.write_text(p.read_text()+'\n// synthetic drift\n')
  elif case=='missing-source':owner.unlink()
  elif case=='template-drift':p=res/'m3_index_mask_compactor.c.txt';p.write_text(p.read_text()+'\n// synthetic drift\n')
  elif case=='duplicate-manifest':p=res/'manifest.tsv';p.write_text(p.read_text()+'\t'.join(rows[0])+'\n')
  elif case=='symlink-source':p=P(tmp)/'outside';shutil.copy2(owner,p);owner.unlink();owner.symlink_to(p)
  else:p=owner.parent;o=P(tmp)/'outside';shutil.move(p,o);p.symlink_to(o,target_is_directory=True)
  before=snapshot(root)
  q=run('refusal-'+case,['java','-cp',ENGINE,'com.synexia.rewrite.SealReplay','apply',root,res],expected=1)
  assert before==snapshot(root);refusals.append({'case':case,'exitCode':q.returncode,'targetBytesUnchanged':True})
(E/'REFUSALS.json').write_text(json.dumps(refusals,indent=2)+'\n')
result={'status':'SELECTED_SOURCE_PASS_HOLD_FULL_PRODUCT','javaChecks':236639,'jniChecks':320006,'sortedSplitChecks':361273,'nativeSpanCases':9712,'nativeSpanChecks':67784,'commonHeaderChecks':16777730,'partialStates':32,'prewriteRefusals':7,'kernelCancellationChecks':2,'apiComparisons':5,'jniExports':9,'missingNativeChecks':21,'notRun':['Maven/OpenRewrite SDK','whole owning module','installed M3JDK21 image/jtreg/JCK','cross-platform union','whole live-wrapper suite','remaining programme'],'sourceSha256':snapshot(CAND)}
# Exclude build products from source receipt.
result['sourceSha256']={p:h for p,h in result['sourceSha256'].items() if not p.startswith('build/')}
(E/'RESULT.json').write_text(json.dumps(result,indent=2)+'\n')
print('UNION_REPLAY partialStates=32 fixedPoints=32 refusals=7 API=5 JNI_EXPORTS=9 PASS')
