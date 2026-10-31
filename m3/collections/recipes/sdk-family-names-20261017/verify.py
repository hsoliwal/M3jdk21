#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Qualify exact recipe candidates. Metadata/JNI execution is not donor-resolved SDK execution."""
import argparse, hashlib, json, shutil, subprocess, tempfile, xml.etree.ElementTree as ET
from pathlib import Path
TASK=Path(__file__).resolve().parent
ROOT=TASK.parents[3]
RES=TASK/'src/main/resources/com/synexia/rewrite/sdk-family-names-20261017'
PRODUCT='m3/collections/interop/'
SOURCE=PRODUCT+'src/main/java/com/m3/collections/interop/'
TEST=PRODUCT+'src/test/java/com/m3/collections/interop/'
CORE='m3/collections/src/main/java/com/m3/collections/'
LOG=[]
def run(args,negative=False):
    args=list(map(str,args)); LOG.append('$ '+' '.join(args))
    result=subprocess.run(args,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=90)
    LOG.append(result.stdout)
    print(result.stdout,end='',flush=True)
    if negative:
        if result.returncode==0 or 'AssertionError' not in result.stdout: raise AssertionError('negative witness not detected')
    elif result.returncode: raise RuntimeError('failed gate '+args[0])
    return result.stdout

def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--apply',action='store_true');args=parser.parse_args()
    evidence=TASK/'evidence';evidence.mkdir(exist_ok=True)
    rows=[line.split('\t') for line in (RES/'manifest.tsv').read_text().splitlines()]
    try:
        with tempfile.TemporaryDirectory(prefix='m3-sdk-family-') as directory:
            tmp=Path(directory);engine=tmp/'engine';engine.mkdir()
            source=TASK/'src/main/java/com/synexia/rewrite'
            run(['javac','--release','21','-proc:none','-Xlint:all','-Werror','-d',engine,
                 source/'M3GpuToolJoinManifest.java',source/'SdkFamilyReplay.java'])
            candidate=tmp/'candidate'
            run(['java','-cp',engine,'com.synexia.rewrite.SdkFamilyReplay',ROOT,candidate])
            for path,before,after,template in rows:
                if template!='KEEP' and sha(candidate/path)!=after:raise AssertionError('emission drift')
            classes=tmp/'classes';classes.mkdir()
            run(['javac','--release','21','-proc:none','-Xlint:all','-Werror','-d',classes,
                 candidate/(SOURCE+'M3LibraryBinding.java'),candidate/(SOURCE+'M3LibraryBindings.java'),
                 candidate/(TEST+'M3LibraryBindingsSelfTest.java'),TASK/'proof/SdkSourceAudit.java'])
            resource=classes/'META-INF/m3';resource.mkdir(parents=True)
            shutil.copyfile(candidate/(PRODUCT+'sdk-family-bindings.tsv'),resource/'sdk-family-bindings.tsv')
            for heap in ['32m','128m']:
                run(['java','-Xmx'+heap,'-cp',classes,'com.m3.collections.interop.M3LibraryBindingsSelfTest',resource/'sdk-family-bindings.tsv'])
            java_sources=[p for p in (candidate/PRODUCT).rglob('*.java')]
            audit=['java','-cp',classes,'SdkSourceAudit',TASK/'before'/(SOURCE+'M3LibraryCollections.java'),
                   candidate/(SOURCE+'M3LibraryCollections.java'),candidate/(PRODUCT+'sdk-family-bindings.tsv')]
            run([*audit,*java_sources])
            bad=tmp/'bad-bindings.tsv'
            bad.write_text((resource/'sdk-family-bindings.tsv').read_text().replace('guavaFilteredCollection','guavaFilteredCollectionWrong'))
            run([*audit[:-1],bad],negative=True)
            ET.parse(candidate/(PRODUCT+'pom.xml'))
            # Actual incumbent native owners, unchanged and hash-pinned by the recipe.
            native_classes=tmp/'native-classes';native_classes.mkdir()
            run(['javac','--release','21','-proc:none','-Xlint:all','-Werror','-d',native_classes,
                 ROOT/(CORE+'M3IndexMaskCompactor.java'),ROOT/(CORE+'M3LongIndexProjection.java'),TASK/'proof/SdkIndexMaskProof.java'])
            home=Path(shutil.which('javac')).resolve().parents[1]
            native=tmp/'native';native.mkdir()
            original=ROOT/'m3/collections/src/main/native/collections/m3_index_mask_compactor.c'
            cargs=['gcc','-std=c17','-Wall','-Wextra','-Werror','-Wpedantic','-fsanitize=undefined','-fno-sanitize-recover=undefined',
                   '-fPIC','-shared','-I'+str(home/'include'),'-I'+str(home/'include/linux')]
            run([*cargs,original,'-o',native/'libm3_collections_mask.so'])
            for heap in ['32m','128m']:
                run(['java','-Xcheck:jni','-Xmx'+heap,'-Djava.library.path='+str(native),'-cp',native_classes,'SdkIndexMaskProof','native'])
            run(['java','-Xcheck:jni','-Djava.library.path='+str(tmp/'no-library'),'-cp',native_classes,'SdkIndexMaskProof'])
            mutant=tmp/'mutant.c';text=original.read_text();broken=text.replace('(unsigned int)(unsigned char)marks[i]','((unsigned int)(unsigned char)marks[i] & 127u)')
            if text==broken:raise AssertionError('ineffective high-bit mutant')
            mutant.write_text(broken);mutant_lib=tmp/'mutant';mutant_lib.mkdir()
            run([*cargs,mutant,'-o',mutant_lib/'libm3_collections_mask.so'])
            run(['java','-Xcheck:jni','-Djava.library.path='+str(mutant_lib),'-cp',native_classes,'SdkIndexMaskProof','native'],negative=True)
            # All current input hashes must still be admitted before serial publication.
            for path,before,after,template in rows:
                target=ROOT/path;actual=sha(target) if target.exists() else 'ABSENT'
                if actual not in (before,after):raise AssertionError('late source drift '+path)
            if args.apply:
                for path,before,after,template in rows:
                    if template=='KEEP':continue
                    target=ROOT/path;target.parent.mkdir(parents=True,exist_ok=True)
                    shutil.copyfile(candidate/path,target)
            for path,before,after,template in rows:
                if template!='KEEP' and (not (ROOT/path).exists() or sha(ROOT/path)!=after):raise AssertionError('unpublished/drifted output '+path)
            replay=tmp/'replay'
            run(['java','-cp',engine,'com.synexia.rewrite.SdkFamilyReplay',ROOT,replay])
            for path,before,after,template in rows:
                if template!='KEEP' and (replay/path).read_bytes()!=(candidate/path).read_bytes():raise AssertionError('fixed point mismatch')
            shutil.copyfile(candidate/'GENERATED_SHA256.tsv',evidence/'GENERATED_SHA256.tsv')
            print('PASS: exact recipe emission; five-family metadata; 18 old method trees; Java/JNI; negative controls. NOT_RUN: actual donor compilation, donor JUnit, Maven SDK.',flush=True)
    finally:
        (evidence/'qualification.log').write_text('\n'.join(LOG))
if __name__=='__main__':main()
