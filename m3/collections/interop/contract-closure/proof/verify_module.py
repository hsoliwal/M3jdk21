#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Test the actual SDK-emitted optional POM using real locally compiled first-party jars, not stubs."""
import argparse,hashlib,json,pathlib,shutil,subprocess,sys,xml.etree.ElementTree as E
if sys.flags.optimize:raise SystemExit('Assertions required')
p=argparse.ArgumentParser()
for name in ['build','receiver','cache','maven']:p.add_argument('--'+name,required=True)
a=p.parse_args();P=pathlib.Path;build=P(a.build).resolve();receiver=P(a.receiver).resolve();cache=P(a.cache).resolve()
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
assert (build/'sdk-output/qualified-recipe-receipt.tsv').is_file(),'prior gated delivery required'
parent=build/'parent/runtime-proof';records=[]
# This is an isolated qualification cache, not deployment or a published artifact claim.
for artifact,jar,pom in [('m3-collections',parent/'m3-collections.jar',receiver/'m3/collections/pom.xml'),('m3-collections-interop',parent/'m3-collections-interop-preview.jar',receiver/'m3/collections/interop/pom.xml')]:
    dest=cache/'com/m3'/artifact/'0.1.0-SNAPSHOT';dest.mkdir(parents=True,exist_ok=True)
    ownership=dest/'m3-qualification-only.json'
    prior=json.loads(ownership.read_text()) if ownership.exists() else []
    for source,suffix in [(jar,'.jar'),(pom,'.pom')]:
        target=dest/(artifact+'-0.1.0-SNAPSHOT'+suffix)
        # A dedicated cache may contain a prior exact build. Refuse unrelated installed artifacts.
        if target.exists():
            assert any(v['target']==str(target) and v['sha256']==sha(target) for v in prior),'unowned first-party artifact'
            if target.suffix=='.pom':assert sha(target)==sha(source),'foreign first-party POM'
        shutil.copyfile(source,target);records.append(dict(artifact=artifact,source=str(source),sha256=sha(source),target=str(target)))
    (dest/'m3-qualification-only.json').write_text(json.dumps(records[-2:],indent=2)+'\n')
work=build/'optional-module';shutil.copytree(build/'sdk-output/m3/collections/interop/apache',work)
before={str(p.relative_to(work)):sha(p) for p in work.rglob('*') if p.is_file()}
cmd=[a.maven,'-o','-B','-ntp','-Dmaven.repo.local='+str(cache),'-f',str(work/'pom.xml'),'test']
r=subprocess.run(cmd,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=90)
(build/'module-maven.log').write_text(r.stdout)
if r.returncode:raise RuntimeError(r.stdout[-14000:])
root=E.parse(work/'target/surefire-reports/TEST-com.m3.collections.interop.M3ApacheContractTest.xml').getroot()
assert {k:int(root.get(k,'-1')) for k in ['tests','failures','errors','skipped']}==dict(tests=23,failures=0,errors=0,skipped=0)
assert all(sha(work/path)==h for path,h in before.items()),'SDK source changed during Maven test'
receipt=dict(status='PASS_ACTUAL_OPTIONAL_MODULE_MAVEN_TEST',test_count=23,command=cmd,first_party_inputs=records,source_hashes=before,limitations=['first-party jar inputs actually compiled by the source-sealed proof, not installed/released by a reactor','package/install/deploy, complete reactor, static and installed-JDK admission are separate gates'])
(build/'module-qualification.json').write_text(json.dumps(receipt,indent=2)+'\n');print(r.stdout[-4500:]);print(json.dumps(receipt,indent=2))
