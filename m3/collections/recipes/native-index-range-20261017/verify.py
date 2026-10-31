#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Run the unchanged Synexia source-seal kernel, then actual serial Java/C/JNI proof.
No Maven/OpenRewrite SDK result is inferred from this dependency-free gate.
"""
import argparse, hashlib, json, os, re, shutil, subprocess, tempfile
from pathlib import Path
TASK=Path(__file__).resolve().parent
ROOT=TASK.parents[3]
RES=TASK/'src/main/resources/com/synexia/rewrite/native-index-range-20261017'
JAVA='m3/collections/src/main/java/com/m3/collections/'
C='m3/collections/src/main/native/collections/m3_index_mask_compactor.c'
SELF='m3/collections/src/test/java/com/m3/collections/M3NativeIndexRangeSelfTest.java'
EXAMPLE='m3/collections/examples/IndexedMaskPipelineExample.java'
rows=[line.split('\t') for line in (RES/'manifest.tsv').read_text().splitlines()]
logs=[]
def run(args, expected=0):
    logs.append('$ '+' '.join(map(str,args)))
    p=subprocess.run(list(map(str,args)),stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=120)
    logs.append(p.stdout)
    if (p.returncode==0)!=(expected==0):
        raise RuntimeError(f'Unexpected exit {p.returncode}: {args}\n{p.stdout}')
    if expected==0: print(p.stdout,end='',flush=True)
    else: print('Negative witness detected:',str(args[-1]),flush=True)
    return p.stdout

def compile_java(src,out,candidate):
    out.mkdir(parents=True,exist_ok=True)
    run(['javac','--release','21','-proc:none','-Xlint:all','-Werror','-h',out,'-d',out,
         src/(JAVA+'M3IndexMaskCompactor.java'),src/(JAVA+'M3LongIndexProjection.java'),
         candidate/SELF,TASK/'proof/RangeAllocationProbe.java',candidate/EXAMPLE])

def compile_c(src,out):
    out.mkdir(parents=True,exist_ok=True)
    java_home=Path(shutil.which('javac')).resolve().parents[1]
    run(['gcc','-std=c17','-Wall','-Wextra','-Werror','-Wpedantic','-fsanitize=undefined',
         '-fno-sanitize-recover=undefined','-fPIC','-shared','-I'+str(java_home/'include'),
         '-I'+str(java_home/'include/linux'),src,'-o',out/'libm3_collections_mask.so'])

def java(classes,lib,mode,sharing=False,heap='64m',expected=0,extra=()):
    return run(['java','-Xmx'+heap,'-Xcheck:jni',*extra,'-Djava.library.path='+str(lib),'-cp',classes,
                'com.m3.collections.M3NativeIndexRangeSelfTest',mode,*(['sharing'] if sharing else [])],expected)

def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--apply',action='store_true');args=parser.parse_args()
    evidence=TASK/'evidence'; evidence.mkdir(exist_ok=True)
    try:
        with tempfile.TemporaryDirectory(prefix='m3-native-range-') as d:
            tmp=Path(d); engine=tmp/'engine';engine.mkdir()
            pkg=TASK/'src/main/java/com/synexia/rewrite'
            run(['javac','--release','21','-Xlint:all','-Werror','-d',engine,
                 pkg/'M3GpuToolJoinManifest.java',pkg/'NativeIndexRangeReplay.java'])
            candidate=tmp/'candidate'
            run(['java','-cp',engine,'com.synexia.rewrite.NativeIndexRangeReplay',ROOT,candidate])
            baseline=tmp/'baseline';shutil.copytree(TASK/'before',baseline)
            base_classes=tmp/'base-classes';base_lib=tmp/'base-lib'
            compile_java(baseline,base_classes,candidate);compile_c(baseline/C,base_lib)
            java(base_classes,base_lib,'legacy')
            java(base_classes,base_lib,'legacy',sharing=True,expected=1)
            allocation_before=run(['java','-Xmx128m','-Xcheck:jni','-Djava.library.path='+str(base_lib),
                 '-cp',base_classes,'com.m3.collections.RangeAllocationProbe'])
            current=tmp/'serial';shutil.copytree(baseline,current)
            for step,path in enumerate([C,JAVA+'M3IndexMaskCompactor.java',JAVA+'M3LongIndexProjection.java'],1):
                shutil.copyfile(candidate/path,current/path)
                classes=tmp/f'pass{step}-classes';lib=tmp/f'pass{step}-lib'
                compile_java(current,classes,candidate);compile_c(current/C,lib)
                java(classes,lib,'legacy' if step==1 else 'range',sharing=step==3)
                print('Serial file pass',step,path,flush=True)
            # Now classes/lib refer to the final actual product images.
            for heap in ['32m','128m']: java(classes,lib,'range',sharing=True,heap=heap)
            java(classes,lib,'range',sharing=True,extra=['-Xint'])
            java(classes,tmp/'missing-library','none',sharing=True)
            java(classes,base_lib,'legacy',sharing=True)
            run(['java','-cp',classes,'IndexedMaskPipelineExample'])
            run(['java','-Xcheck:jni','-Djava.library.path='+str(lib),'-cp',classes,'IndexedMaskPipelineExample','native'])
            # Compare the public class/enum surfaces, not private implementation symbols.
            for name in ['M3IndexMaskCompactor','M3IndexMaskCompactor$Backend','M3LongIndexProjection','M3LongIndexProjection$Order']:
                before=run(['javap','-public','-classpath',base_classes,'com.m3.collections.'+name])
                after=run(['javap','-public','-classpath',classes,'com.m3.collections.'+name])
                if before!=after: raise AssertionError('public ABI changed: '+name)
            # Actual legacy JNI sentinel: a native request must not become a Java fallback.
            sentinel=tmp/'sentinel.c'
            sentinel.write_text('''#include <jni.h>
JNIEXPORT jintArray JNICALL Java_com_m3_collections_M3IndexMaskCompactor_selectNative(
JNIEnv* e,jclass c,jintArray r,jbyteArray s,jint m){(void)c;(void)r;(void)s;(void)m;
jclass x=(*e)->FindClass(e,"java/lang/UnsatisfiedLinkError");
if(x!=NULL){(*e)->ThrowNew(e,x,"legacy-native-witness");(*e)->DeleteLocalRef(e,x);}return NULL;}\n''')
            sentinel_lib=tmp/'sentinel';compile_c(sentinel,sentinel_lib);java(classes,sentinel_lib,'sentinel')
            # Faulty compiled C implementations must fail the same product corpus.
            ctext=(candidate/C).read_text()
            mutants={
             'offset':ctext.replace('rows, first + offset, chunk, row_ids);','rows, offset, chunk, row_ids);').replace('    jbyte marks[M3_CHUNK];','    (void)first;\n    jbyte marks[M3_CHUNK];'),
             'high-bit':ctext.replace('(unsigned int)required_mask)', '(unsigned int)(required_mask & 127))')}
            for name,text in mutants.items():
                if text==ctext: raise AssertionError('ineffective mutant')
                source=tmp/(name+'.c');source.write_text(text);mlib=tmp/(name+'-lib');compile_c(source,mlib)
                java(classes,mlib,'range',expected=1)
            allocation_after=run(['java','-Xmx128m','-Xcheck:jni','-Djava.library.path='+str(lib),
                 '-cp',classes,'com.m3.collections.RangeAllocationProbe'])
            def values(log): return [tuple(map(int,m)) for m in re.findall(r'nativeFilterBytes=(\d+) repeatedSortBytes=(\d+)',log)]
            old,new=values(allocation_before),values(allocation_after)
            if len(old)!=3 or len(new)!=3 or any(b>=a for (a,_),(b,_) in zip(old,new)) or any(b>=a for (_,a),(_,b) in zip(old,new)):
                raise AssertionError('allocation reduction not reproduced')
            (evidence/'ALLOCATION.json').write_text(json.dumps({'scope':'3 finite ThreadMXBean samples; 100 operations; 65500-row slice; not JMH',
                 'before':old,'after':new},indent=2)+'\n')
            # Ensure unchanged tracked inputs remain admitted immediately before optional publication.
            for path,before,after,template in rows:
                file=ROOT/path
                actual=sha(file) if file.exists() else 'ABSENT'
                if actual not in [before,after]:raise AssertionError('late input drift '+path)
            if args.apply:
                for path,before,after,template in rows:
                    if template=='KEEP':continue
                    dst=ROOT/path;dst.parent.mkdir(parents=True,exist_ok=True)
                    shutil.copyfile(candidate/path,dst)
            else:
                for path,before,after,template in rows:
                    if template!='KEEP' and (not (ROOT/path).exists() or sha(ROOT/path)!=after):
                        raise AssertionError('generated output not installed: '+path+'; use --apply after template qualification')
            replay=tmp/'fixed-point'
            run(['java','-cp',engine,'com.synexia.rewrite.NativeIndexRangeReplay',ROOT,replay])
            for path,before,after,template in rows:
                if template!='KEEP' and (candidate/path).read_bytes()!=(replay/path).read_bytes():raise AssertionError('nonrepeatable '+path)
            shutil.copyfile(candidate/'GENERATED_SHA256.tsv',evidence/'GENERATED_SHA256.tsv')
            print('PASS: actual Java/JNI, legacy native compatibility, missing-native refusal, 3 serial file passes, public ABI, 2 compiled mutants, exact recipe replay.',flush=True)
    finally:
        (evidence/'qualification.log').write_text('\n'.join(logs))
if __name__=='__main__':main()
