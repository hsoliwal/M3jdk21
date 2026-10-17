#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Qualify only actual named OpenRewrite Results in a fresh serial receiving image.
Uses the repository's canonical recipe; this script is not a source rewrite engine.
Linux selected patched-java.base and real JNI proof, not source-built JDK acceptance.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import xml.etree.ElementTree as ET

BASE='src/java.base/share/classes/java/util/CollectionViews.java'
TEST='test/jdk/java/util/CollectionViews/TraversalContractsTest.java'
JNI='test/jdk/java/util/CollectionViews/run_search_jni.c'
PEER='test/jdk/java/util/CollectionViews/RunSearchJni.java'
FACADE='m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java'
METHODS={'namedResultsFixedPointAndActualEmission','everyPartialStateAndInputOrder',
         'independentWorkerReplay','missingDriftDuplicateAndOccupiedTargetsRefused',
         'templateTamperLateEditAndCancellationRefused'}
def sha(data):return hashlib.sha256(data).hexdigest()
def blob(data):return hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()
def put(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    with path.open('xb') as stream:stream.write(data)
def reports(path):
    root=ET.parse(path).getroot();cases=root.findall('testcase')
    if len(cases)!=5 or {c.get('name') for c in cases}!=METHODS:
        raise RuntimeError('Missing, duplicate or foreign SDK methods')
    if any(c.find(tag) is not None for c in cases for tag in ('failure','error','skipped')):
        raise RuntimeError('SDK methods failed or skipped')
    if any(float(root.get(tag,'0'))!=0 for tag in ('errors','failures','skipped')):
        raise RuntimeError('SDK summary not clean')
    return sorted(METHODS)
def main():
    ap=argparse.ArgumentParser();ap.add_argument('--sdk',type=Path,required=True);ap.add_argument('--output',type=Path,required=True)
    args=ap.parse_args();sdk=args.sdk.resolve();out=args.output.resolve();crate=Path(__file__).resolve().parent
    if out.exists():raise RuntimeError('Fresh runtime output required')
    out.mkdir(parents=True)
    r=crate/'src/main/resources/com/synexia/rewrite/java-util-view-contracts'
    rows=[s.split('\t') for s in (r/'manifest.tsv').read_text().splitlines()]
    changed=[row for row in rows if row[3]!='KEEP'];expected=[BASE,JNI,TEST]
    if [row[0] for row in changed]!=expected:raise RuntimeError('Unexpected serial receiving order')
    methods=reports(sdk.parent/'maven/surefire-reports/TEST-com.synexia.rewrite.M3JavaUtilViewContractsRecipeTest.xml')
    if (sdk/'SDK_RECEIPT.txt').read_text()!='ACTUAL_NAMED_OPENREWRITE_RESULTS=3\nFIXED_POINT=0\n':raise RuntimeError('SDK receipt mismatch')
    actual={str(p.relative_to(sdk)) for p in sdk.rglob('*') if p.is_file()}
    if actual!=set(expected)|{'SDK_RECEIPT.txt'}:raise RuntimeError('SDK result file set mismatch')
    for path,pre,post,template in rows:
        if pre!='ABSENT' and sha((r/'before'/path).read_bytes())!=pre:raise RuntimeError('baseline drift '+path)
        if template!='KEEP':
            if sha((r/template).read_bytes())!=post or (r/template).read_bytes()!=(sdk/path).read_bytes():raise RuntimeError('SDK/template drift '+path)
    engine={'M3GpuToolJoinRecipe.java':'1c1b1b568d369f688f6953161636bf36b484bf5a',
            'M3GpuToolJoinManifest.java':'38df5f97bbeec8a8a8cfc77091d56bda925c07f2'}
    for file,expected_blob in engine.items():
        if blob((crate/'src/main/java/com/synexia/rewrite'/file).read_bytes())!=expected_blob:raise RuntimeError('canonical engine drift')
    records=[];observations={}
    def run(name,cmd,negative=None,timeout=180):
        p=subprocess.run([str(x) for x in cmd],capture_output=True,text=True,timeout=timeout)
        record={'name':name,'command':[str(x) for x in cmd],'exit':p.returncode,'stdout':p.stdout,'stderr':p.stderr,
                'expected_failure':negative};records.append(record)
        (out/'commands.json').write_text(json.dumps(records,indent=2)+'\n')
        print(name,'exit='+str(p.returncode),p.stdout.strip(),flush=True)
        if negative is None and p.returncode!=0:raise RuntimeError(name+' failed\n'+p.stdout+'\n'+p.stderr)
        if negative is not None and (p.returncode==0 or negative not in p.stdout+p.stderr):raise RuntimeError('wrong negative-control failure '+name+'\n'+p.stderr)
        if negative is None and ('WARNING in native method' in p.stderr+p.stdout or 'runtime error:' in p.stderr):raise RuntimeError('JNI/native diagnostic '+name)
        return p.stdout.strip()
    def stage(name):
        root=out/name;root.mkdir()
        for path,pre,post,template in rows:
            if pre!='ABSENT':put(root/path,(r/'before'/path).read_bytes())
        return root
    # The new regression is a sealed test input even before its product test file is received.
    test_input=out/'proof-inputs/TraversalContractsTest.java';put(test_input,(sdk/TEST).read_bytes())
    client=out/'proof-inputs/LegacyClient.java'
    put(client,b'''import java.util.*;
import com.m3.collections.M3CollectionViews;
public final class LegacyClient {
 public static void main(String[] args) {
  NavigableSet<Integer> s=M3CollectionViews.filtered(new TreeSet<>(List.of(0,1,2,3,4)),i->i%2==0);
  if(!s.descendingSet().stream().toList().equals(List.of(4,2,0)))throw new AssertionError();
  if(!M3CollectionViews.filteredSortedSnapshot(List.of(2,1,2),i->true,null).equals(List.of(1,2,2)))throw new AssertionError();
  System.out.println("LEGACY_CLIENT_LINK_PASS");
 }
}\n''')
    def compile_image(root,label):
        b=out/label;b.mkdir();base=b/'base';tests=b/'tests';header=b/'headers'
        base.mkdir();tests.mkdir();header.mkdir()
        run(label+'-java-base',['javac','--release','21','-proc:none','-Xlint:all','-Werror','--patch-module','java.base='+str(root/Path(BASE).parents[2]),'-d',base,root/BASE])
        test=root/TEST if (root/TEST).exists() else test_input
        run(label+'-tests',['javac','--release','21','-proc:none','-Xlint:all','-Werror','--patch-module','java.base='+str(base),'-h',header,'-d',tests,test,root/PEER,root/FACADE,client])
        return b
    def java(b,label,mode='all',options=(),extra=(),negative=None,cp=None):
        cmd=['java','-Xss256k',*options,'--patch-module','java.base='+str(b/'base'),'-cp',cp or b/'tests','TraversalContractsTest',mode,*extra]
        value=run(label,cmd,negative=negative)
        if negative is None:observations[label]=value
        return value
    java_home=Path(shutil.which('javac')).resolve().parent.parent
    include=['-I'+str(java_home/'include'),'-I'+str(java_home/'include/linux')]
    def native(root,b,label,sanitize=True):
        so=out/('lib'+label+'.so')
        flags=['-O1','-g','-fsanitize=undefined','-fno-sanitize-recover=all'] if sanitize else ['-O2']
        run(label+'-gcc',['gcc','-std=c17','-Wall','-Wextra','-Wpedantic','-Werror','-fPIC','-shared',*flags,*include,'-I'+str(b/'headers'),root/JNI,'-o',so])
        symbols=run(label+'-symbols',['nm','-D','--defined-only',so])
        exports=[line.split()[-1] for line in symbols.splitlines() if line.strip()]
        if exports!=['Java_RunSearchJni_first']:raise RuntimeError('JNI export drift')
        return so
    def mock(root,b,label,negative=None):
        exe=out/(label+'-mock')
        run(label+'-mock-compile',['gcc','-std=c17','-Wall','-Wextra','-Wpedantic','-Werror','-O1','-g','-fsanitize=undefined','-fno-sanitize-recover=all',*include,'-I'+str(b/'headers'),'-DNATIVE_SOURCE="'+str(root/JNI)+'"',crate/'proof/BoundedJniMock.c','-o',exe])
        return run(label+'-mock-execute',[exe],negative)
    original=stage('original');before=compile_image(original,'baseline')
    legacy=java(before,'baseline-legacy','legacy')
    java(before,'original-metadata-defect','metadata',negative='MISSING_TYPED_CHARACTERISTICS')
    java(before,'original-size-defect','size',negative='COUNT_SATURATION')
    old_so=native(original,before,'baseline-native');old_native=java(before,'baseline-actual-jni','jni',('-Xcheck:jni',),(old_so,))
    mock(original,before,'baseline',negative='PINNED_OR_UNBOUNDED')
    receiving=stage('received');passes=[]
    for index,(path,pre,post,template) in enumerate(changed):
        target=receiving/path
        if pre!='ABSENT' and sha(target.read_bytes())!=pre:raise RuntimeError('receiver preimage drift')
        target.parent.mkdir(parents=True,exist_ok=True)
        if pre=='ABSENT':put(target,(sdk/path).read_bytes())
        else:target.write_bytes((sdk/path).read_bytes()) # Isolated serial receiver of actual SDK Results, never template authoring.
        b=compile_image(receiving,'pass-'+str(index+1))
        if java(b,'pass-'+str(index+1)+'-legacy','legacy')!=legacy:raise RuntimeError('stable-input behavior changed')
        java(b,'pass-'+str(index+1)+'-contracts')
        if index==0:java(b,'pass-1-saturated-size','size')
        so=native(receiving,b,'pass-'+str(index+1)+'-native')
        if java(b,'pass-'+str(index+1)+'-actual-jni','jni',('-Xcheck:jni',),(so,))!=old_native:raise RuntimeError('JNI behavior changed')
        if index>=1:mock(receiving,b,'pass-'+str(index+1))
        passes.append({'path':path,'sha256':post,'git_blob':blob(target.read_bytes())})
    final=b
    for mode,options in [('interpreter',['-Xint']),('C1',['-XX:TieredStopAtLevel=1']),('C2',['-XX:-TieredCompilation'])]:
        java(final,'final-'+mode,'all',options)
    release=native(receiving,final,'final-release',False)
    java(final,'final-release-jni','jni',('-Xcheck:jni',),(release,))
    # Baseline-compiled client runs unchanged against the candidate bootstrap owner/facade.
    run('binary-client',['java','--patch-module','java.base='+str(final/'base'),'-cp',os.pathsep.join([str(before/'tests'),str(final/'tests')]),'LegacyClient'])
    for name,classpath in [('java.util.CollectionViews','base'),('com.m3.collections.M3CollectionViews','tests')]:
        def api(b):
            cmd=['javap','-public','-s','-constants']
            if classpath=='base':cmd+=['--system','none','-classpath',b/'base',name]
            else:cmd+=['-classpath',b/'tests',name]
            return run(('before-' if b==before else 'after-')+name,cmd)
        if api(before)!=api(final):raise RuntimeError('public JVM descriptor drift '+name)
    if (before/'headers/RunSearchJni.h').read_bytes()!=(final/'headers/RunSearchJni.h').read_bytes():raise RuntimeError('JNI header drift')
    repeated=compile_image(receiving,'repeat')
    for kind in ['base','tests']:
        a={str(p.relative_to(final/kind)):sha(p.read_bytes()) for p in (final/kind).rglob('*.class')}
        z={str(p.relative_to(repeated/kind)):sha(p.read_bytes()) for p in (repeated/kind).rglob('*.class')}
        if a!=z:raise RuntimeError('repeated class byte drift')
    # Permanently retained exact generated wrong candidates are test-only, not production fixes.
    counter=out/'counterexamples';counter.mkdir()
    for label,old,new,mode,fingerprint in [
        ('drop-split','new SelectionSpliterator<>(prefix, predicate, guaranteed, ownerComparator)','new SelectionSpliterator<>(prefix, predicate)','metadata','MISSING_TYPED_CHARACTERISTICS'),
        ('false-sized','return (source.characteristics() & (ORDERED | DISTINCT | SORTED | NONNULL | IMMUTABLE)) | guaranteed;',
         'return (source.characteristics() & (ORDERED | DISTINCT | SORTED | NONNULL | IMMUTABLE)) | guaranteed | SIZED;', 'metadata','false size/concurrency promise')]:
        wrong=stage('mutant-'+label);text=(sdk/BASE).read_text()
        if text.count(old)!=1:raise RuntimeError('negative control anchor missing '+label)
        text=text.replace(old,new);(wrong/BASE).write_text(text);put(counter/(label+'.java.txt'),text.encode())
        wb=compile_image(wrong,'mutant-'+label+'-build');java(wb,'mutant-'+label+'-rejected',mode,negative=fingerprint)
    wrong=stage('mutant-native');code=(sdk/JNI).read_text()
    old='if(answer==runs && end>value)answer=row+i;'
    if code.count(old)!=1:raise RuntimeError('native negative control anchor missing')
    code=code.replace(old,'if(answer==runs && end>value)return row+i;')
    (wrong/JNI).write_text(code);put(counter/'early-return.c.txt',code.encode())
    bad_so=native(wrong,final,'mutant-native');java(final,'native-early-return-rejected','jni',('-Xcheck:jni',),(bad_so,),negative='missing failure')
    receipt={'status':'PASS_SELECTED_ACTUAL_MAVEN_OPENREWRITE_JAVA_JNI','sdk_methods':methods,'sdk_results':3,'sdk_partial_states':8,'sdk_partial_order_schedules':24,
      'target_baseline':'hsoliwal/M3jdk21@67feb628e55d886d4a82fd8bc4e8a3a63fe2c967','serial_receiving':passes,
      'observations':observations,'commands':len(records),'canonical_engine_blobs':engine,
      'native_scope':'Existing verification-only RunSearchJni; not production acceleration or libjava changes',
      'proof_scope':'Real selected sources patched into stock Java21 java.base; not a rebuilt matched M3JDK21',
      'unexecuted':['full source-built M3JDK21','jtreg harness and JCK','owning module/full reactor and all-provider closure','hosted exact-head CI','Windows/macOS','application/JMH performance','java.base source license admission']}
    (out/'summary.json').write_text(json.dumps(receipt,indent=2)+'\n');print(json.dumps(receipt,indent=2))
if __name__=='__main__':main()
