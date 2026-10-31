#!/usr/bin/env python3
"""Run emitted collection modules and real native selectors on the integrated M3JDK21 image."""
from pathlib import Path
import argparse,hashlib,json,subprocess,time,os
p=argparse.ArgumentParser()
for name in ['runtime','image','out']:p.add_argument('--'+name,type=Path,required=True)
a=p.parse_args();runtime,image,out=[v.resolve() for v in [a.runtime,a.image,a.out]]
out.mkdir(parents=True,exist_ok=False)
def sha(p):
    h=hashlib.sha256()
    with p.open('rb') as f:
        for b in iter(lambda:f.read(1024*1024),b''):h.update(b)
    return h.hexdigest()
classes=runtime/'candidate/main-classes';tests=runtime/'candidate/test-classes';native=runtime/'native'
receipt={'scope':'Existing integrated M3JDK21 image plus exact emitted external com.m3.collections module and test patch, real required JNI libraries; not a claim that collections was linked into the prebuilt image','runtime_inputs_sha256':sha(runtime/'INPUTS.json'),'sdk_source_kind':json.loads((runtime/'INPUTS.json').read_text())['source_kind'],'java_image':str(image),'image_files':[{ 'path':str(p.relative_to(image)),'sha256':sha(p)} for p in [image/'bin/java',image/'lib/modules',image/'lib/server/libjvm.so',image/'release']],'module_classes':[{'path':str(p.relative_to(classes)),'sha256':sha(p)} for p in sorted(classes.rglob('*.class'))],'test_classes':[{'path':str(p.relative_to(tests)),'sha256':sha(p)} for p in sorted(tests.rglob('*.class'))],'native_libraries':[{'path':str(p.relative_to(native)),'sha256':sha(p)} for p in sorted(native.glob('*.so'))],'native_source_seals':json.loads((runtime/'NATIVE_INPUTS.json').read_text())}
(out/'SOURCE_RECEIPT.json').write_text(json.dumps(receipt,indent=2)+'\n')
records=[]
def run(name,cmd):
    start=time.monotonic();r=subprocess.run([str(v) for v in cmd],capture_output=True,text=True,timeout=180)
    (out/(name+'.log')).write_text(r.stdout+r.stderr)
    records.append({'name':name,'command':[str(v) for v in cmd],'exit':r.returncode,'seconds':round(time.monotonic()-start,3)})
    (out/'COMMANDS.json').write_text(json.dumps(records,indent=2)+'\n')
    print(name,'exit='+str(r.returncode),r.stdout[-250:].strip(),r.stderr[-200:].strip(),flush=True)
    if r.returncode:raise AssertionError(name+' failed')
    return r.stdout
run('integrated-java-version',[image/'bin/java','-version'])
base=[image/'bin/java','-Xmx256m','-Xcheck:jni','-Djava.library.path='+str(native),'--module-path',classes,'--patch-module','com.m3.collections='+str(tests)]
observed=run('integrated-module-source',base+['--show-module-resolution','--module','com.m3.collections/com.m3.collections.M3LongIndexProjectionSelfTest'])
assert 'root com.m3.collections '+classes.as_uri()+'/' in observed
for name,args in [('M3IndexOrderWindowContract',['--native']),('M3NativeIndexRangeSelfTest',['native']),('M3FilteredListsMaskBridgeProof',['--native']),('M3IndexedMultiValueOccurrenceSelfTest',[]),('M3StrictFilteredSequencedContract',[]),('M3FilteredMergedSequencedContract',[]),('M3FilteredListMutationProof',[])]:
    run('integrated-'+name,base+['--module','com.m3.collections/com.m3.collections.'+name,*args])
result={'status':'PASS','source_kind':'ACTUAL_SDK_EMITTED_CURRENT_SEVEN','java_image':str(image),'module':'com.m3.collections','module_source':str(classes),'native':'REAL_REQUIRED_CHECKED_JNI_UBSAN','commands':len(records),'source_receipt_sha256':sha(out/'SOURCE_RECEIPT.json')}
(out/'RESULT.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result),flush=True)
