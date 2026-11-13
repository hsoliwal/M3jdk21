# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
#!/usr/bin/env python3
"""Isolated actual-source Java21/C17/UBSan/checked-JNI collection verification.
No Maven, recipe, full-repository, JDK, benchmark, or arbitrary-predicate JNI claim.
"""
from pathlib import Path
import hashlib, json, os, re, shutil, subprocess, time

WORK=Path(__file__).resolve().parent
SOURCE=WORK.parents[1]/'M3jdk21/m3/collections'
JDK=Path('/tmp/m3-toolchain/jdk-21.0.12.1+1')
PIN='2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49'
COPIED=WORK/'source'
LOG=WORK/'logs'; LOG.mkdir(exist_ok=True)
CLASSES=WORK/'classes'; CLASSES.mkdir(exist_ok=True)
TEST_CLASSES=WORK/'test-classes'; TEST_CLASSES.mkdir(exist_ok=True)
HEADERS=WORK/'headers'; HEADERS.mkdir(exist_ok=True)
LIB=WORK/'lib'; LIB.mkdir(exist_ok=True)
TESTS=[
 ('M3IndexMaskCompactorSelfTest',['--native']),
 ('M3IndexMaskWindowSelfTest',['--native']),
 ('M3IndexMaskWindowRecoverySelfTest',['--native']),
 ('M3MaskNativeParityProof',[]),
 ('M3NativeIndexRangeSelfTest',['range','sharing']),
 ('M3FilteredListsMaskBridgeProof',['--native']),
 ('M3IndexPathMasksSelfTest',['--native']),
 ('M3LongIndexPathMasksSelfTest',['--native']),
 ('M3IndexPathMaskWindowSelfTest',[]),
 ('M3IndexPathRankNativeSelfTest',[]),
 ('M3LongAddressKernelSelfTest',[]),
 ('M3LongAddressBoundedSelfTest',[]),
 ('M3LongRankIndexSelfTest',[]),
 ('M3SignalOrderContract',['native']),
 ('M3SignalDimensionsNativeContract',[]),
 ('M3SpanIndexSelfTest',[]),
 ('M3StateMaskHistogramSelfTest',[]),
 ('M3ResidencyStateMaskProof',['native']),
 ('M3LongRangeSetSelfTest',[]),
 ('M3LongSortedSnapshotSelfTest',[]),
 ('M3LongBitmapFilterPlanSelfTest',[]),
 ('M3LongBitmapFilteredSnapshotSelfTest',[]),
 ('M3StateMaskArraysSelfTest',[]),
 ('M3SdkRecoverySelfTest',['--native']),
]
SOURCES={'m3_collections_mask':['m3_index_mask_compactor.c','m3_index_mask_window.c'],
 'm3_collections_pathmask':['m3_index_path_masks.c'],
 'm3_collections_address':['m3_long_address_kernel.c'],
 'm3rankbatch':['m3_long_rank_batch_jni.c'],
 'm3signalorder':['m3_signal_order_jni.c'],
 'm3_collections_span':['m3_span_index_jni.c'],
 'm3_collections_statehist':['m3_state_mask_histogram.c'],
 'm3_residency_state_mask':['m3_residency_state_mask.c'],
 'segmented_bits':['segmented_bit_lane.c']}
COMMANDS=[]
def run(label,args):
 args=list(map(str,args)); start=time.monotonic()
 r=subprocess.run(args,cwd=WORK,capture_output=True,text=True,timeout=180)
 output=r.stdout+r.stderr
 (LOG/(label+'.log')).write_text('$ '+json.dumps(args)+'\n'+output+'\nEXIT='+str(r.returncode)+'\n')
 COMMANDS.append({'label':label,'command':args,'exit':r.returncode,'seconds':round(time.monotonic()-start,3),'output':output})
 (WORK/'commands.json').write_text(json.dumps(COMMANDS,indent=2)+'\n')
 print(label+': '+('PASS' if not r.returncode else 'FAIL'),flush=True)
 if output: print(output[-1800:],end='',flush=True)
 if r.returncode: raise SystemExit(r.returncode)
 return output

def copy(rel):
 to=COPIED/rel;to.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(SOURCE/rel,to)
 return to

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()

def main():
 mains=[copy(p.relative_to(SOURCE)) for p in sorted((SOURCE/'src/main/java').rglob('*.java'))]
 tests=[copy(Path('src/test/java/com/m3/collections')/(name+'.java')) for name,_ in TESTS]
 # This source dependency is used by the rank/mask window oracle.
 tests.append(copy(Path('src/test/java/com/m3/collections/M3IndexPathRankSelfTest.java')))
 probe=copy(Path('verification/SegmentedLaneNativeProbe.java'))
 natives=[copy(p.relative_to(SOURCE)) for p in sorted((SOURCE/'src/main/native/collections').glob('*.c'))]
 files=sorted(p for p in COPIED.rglob('*') if p.is_file())
 rows=[{'path':str(p.relative_to(COPIED)),'sha256':sha(p),'git_blob':hashlib.sha1(b'blob '+str(p.stat().st_size).encode()+b'\0'+p.read_bytes()).hexdigest()} for p in files]
 (WORK/'source-hashes.json').write_text(json.dumps({'repository':'https://github.com/hsoliwal/M3jdk21','hydration_commit':PIN,'snapshot_note':'Files copied from actual receiving module; compare source hashes with root change receipts for any edits after hydration. Native files are unchanged by this verification.','files':rows},indent=2)+'\n')
 run('01-java-version',[JDK/'bin/java','-version'])
 run('02-gcc-version',['gcc','--version'])
 common=[JDK/'bin/javac','--release','21','-proc:none','-Xlint:all,-module','-Werror']
 run('03-java-main-compile',common+['-h',HEADERS,'-d',CLASSES]+mains)
 run('04-java-selftest-compile',common+['-cp',CLASSES,'-d',TEST_CLASSES]+tests+[probe])
 cflags=['gcc','-std=c17','-O2','-Wall','-Wextra','-Werror','-Wpedantic','-fsanitize=undefined','-fno-sanitize-recover=all','-fPIC','-I'+str(HEADERS),'-I'+str(JDK/'include'),'-I'+str(JDK/'include/linux')]
 for name,files in SOURCES.items():
  run('05-build-'+name,cflags+['-shared']+[COPIED/'src/main/native/collections'/file for file in files]+['-o',LIB/('lib'+name+'.so')])
 expected=set()
 for h in HEADERS.glob('*.h'):
  expected.update(re.findall(r'JNIEXPORT\s+\w+\s+JNICALL\s+(Java_\w+)',h.read_text()))
 actual=set()
 for lib in sorted(LIB.glob('*.so')):
  out=run('06-exports-'+lib.stem,['nm','-D','--defined-only',lib])
  actual.update(re.findall(r'\b(Java_\w+)\b',out))
 if actual != expected: raise AssertionError({'missing':sorted(expected-actual),'unexpected':sorted(actual-expected)})
 (WORK/'jni-abi.json').write_text(json.dumps({'schema':'M3_EXISTING_COLLECTION_JNI_SYMBOL_VERIFICATION_V1','java21_generated_header_symbols':sorted(expected),'native_exported_symbols':sorted(actual),'missing':[],'unexpected':[],'capability_versions':{'M3IndexMaskCompactor.rangeAbiNative':1,'M3IndexMaskCompactor.positionsAbiNative':1},'native_libraries':[{'path':p.name,'sha256':sha(p)} for p in sorted(LIB.glob('*.so'))]},indent=2)+'\n')
 java=[JDK/'bin/java','-ea','-Xcheck:jni','-Dm3.collections.signal.native=true','-Dm3.collections.rank.native=true','-Dm3.address.required=true','-Djava.library.path='+str(LIB),'-cp',str(CLASSES)+os.pathsep+str(TEST_CLASSES)]
 for index,(name,args) in enumerate(TESTS):
  run(f'07-{index+1:02d}-'+name,java+['com.m3.collections.'+name]+args)
 run('08-segmented-bit-lane-jni',java+['SegmentedLaneNativeProbe',LIB/'libsegmented_bits.so'])
 (WORK/'result.json').write_text(json.dumps({'status':'PASS','schema':'M3_EXISTING_COLLECTION_NATIVE_VERIFICATION_V1','main_sources':len(mains),'standalone_test_sources':len(tests)+1,'native_c_sources':len(natives),'shared_libraries':len(SOURCES),'jni_symbols':len(actual),'standalone_processes_under_checked_jni':len(TESTS)+1,'java_only_additional_regressions':['M3LongIndexPathMasksSelfTest','M3LongSortedSnapshotSelfTest'],'first_path_window_run':'JAVA; second supplemental run NATIVE_REQUIRED','compiler':'Java21 --release 21 -proc:none -Xlint:all,-module -Werror','native':'GCC C17 -O2 -Wall -Wextra -Werror -Wpedantic -fsanitize=undefined -fno-sanitize-recover=all -fPIC','runtime':'-ea -Xcheck:jni, existing opt-in library settings','limitations':['Standalone current-source proof, not historical source-admission verifier','No Maven/OpenRewrite SDK result inferred','No full JDK/image, jtreg, JCK, cross-platform JNI, GC matrix, memory/performance or whole-collection lock-free claim','Counts overlap across oracles and are not added','Native-required oracles explicitly reject absent libraries; auto-detect oracles report native present where implemented']},indent=2)+'\n')
 print('PASS: isolated actual-source existing-native collection verification',flush=True)
if __name__=='__main__':main()
