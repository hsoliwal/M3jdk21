#!/usr/bin/env python3
"""Bounded receiving-context checks; uses the unchanged installer and intake owner."""
import argparse
import ast
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess

TASK='m3/migration/intake-20261005/current-752-ci'
OLDTASK='m3/migration/intake-20261005/reconcile-8131'
REGISTRY='m3/docs/name-mapping.json'
MIGRATION='m3/migration/migration.py'
INSTALLER='m3/migration/recipe.py'
OLD_PACKET='m3/migration/intake-20261005/portable/packet.json'
OLD_RECEIPT='m3/migration/intake-20261005/portable/review-receipt.json'
ORIGINAL_PACKET_SHA='109173cb43fd91e084415284083d0d0ff92a1039a0b21216b7336c0a2c3dcf2e'
ORIGINAL_RECEIPT_SHA='ad1aa88119d8dbf5ab5a3fc64a43bd67bc67df8aadcde9139c663b15a042f1c2'
CURRENT_PACKET_SHA='3b556b885af4b5afeb0b9f22a4bcceea93b6c836125019bde1534652d77c7639'
CURRENT_RECEIPT_SHA='ae4758723f215fda7db7568df5441d83a773eabc7791f86505ca9dfc7c1416db'
REGISTRY_SHA='245fcd73135ef418a30ee350049719574f86222e08c63219d883119b3dc8321f'
JCC_CRATE='m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/jcc-handoff-20261005'
JCC_POM='m3/tooling/migration-recipes/pom.xml'
OLD_POM_SHA='8544be570c2656c5fa641ae34fc01eaea420bbb420f6d80ce5f95253504e9888'
CURRENT_POM_SHA='a53bdeee8afe28b90cb0e348e718b70610a9487edb1b75b46e09bfb76e0f3dc9'
ADDED_TARGETS={
    'm3/migration/test/test_jcc_handoff_current_context.py',
    JCC_CRATE+'/plan-intake-752-ci.json',
    JCC_CRATE+'/pom-before-752.xml',
}
JCC_PROFILES=[
    ('before','before',OLD_POM_SHA,'plan.json','648f80ae4073400f1ea1210f8cdc26942019d53eb4452d839961c466f395d05a','jcc-handoff-20261005/2','70283e86d30abf8e30a09b10d77be40f99160371a98927dc151d680e3c2e4a87'),
    ('after','after',OLD_POM_SHA,'plan-intake-8131.json','8034d77f1124584888dce10760bd2f5dae55eeb93497f7a0db1d09a3a197ec5f','jcc-handoff-20261005/2-intake-8131','0704f21408ef263d4d0adc9eb5d48e62c7c30949cb1a197fb9b7f8a21aff4152'),
    ('current','after',CURRENT_POM_SHA,'plan-intake-752-ci.json','e4ea964db1a6f9527122ff0dad1c969a43dc50a0f80c771c86b70bcd6da6dcaf','jcc-handoff-20261005/2-intake-752-ci','6e8d48067e814566e0c63db14db405c360c8ff3ab4948d352170b6ca20293594'),
]


def require(ok,message):
    if not ok: raise ValueError(message)


def sha(data): return hashlib.sha256(data).hexdigest()

def load(path): return json.loads(path.read_text())

def write(path,obj):
    path.parent.mkdir(parents=True,exist_ok=True)
    require(not path.exists(),'refuse existing output: '+str(path))
    path.write_text(json.dumps(obj,indent=2,sort_keys=True)+'\n')


def inventory(root):
    result=[]
    for p in root.rglob('*'):
        require(not p.is_symlink(),'unexpected symlink')
        if p.is_file():
            b=p.read_bytes();result.append({'path':p.relative_to(root).as_posix(),'sha256':sha(b),'bytes':len(b),'gitBlob':hashlib.sha1(b'blob '+str(len(b)).encode()+b'\0'+b).hexdigest()})
        else:require(p.is_dir(),'special file')
    return sorted(result,key=lambda x:x['path'])


def main():
    ap=argparse.ArgumentParser();ap.add_argument('--proof',type=Path,required=True);a=ap.parse_args();proof=a.proof.resolve()
    manifest=load(proof/'authority/manifest.json');config=load(proof/'config.json')
    source=proof/'source'; tools=manifest['tools'];python=tools['python']
    require(inventory(source)==manifest['files'],'sealed source mismatch')
    require(sha((source/REGISTRY).read_bytes())==REGISTRY_SHA,'unchanged81 authority required')
    require(not (proof/'runtime-work').exists(),'fresh runtime required')
    area=proof/'runtime-work';area.mkdir();logs=proof/'runtime-commands';logs.mkdir()
    env=os.environ.copy()
    for name in ['PYTHONPATH','PYTHONHOME','PYTHONPYCACHEPREFIX','JAVA_TOOL_OPTIONS','_JAVA_OPTIONS','JDK_JAVA_OPTIONS','MAVEN_OPTS','MAVEN_ARGS','M3_JCC_CANDIDATE_ROOT']:
        env.pop(name,None)
    env.update(PYTHONDONTWRITEBYTECODE='1',JAVA_HOME=tools['javaHome'],PATH=tools['javaHome']+'/bin:'+tools['mavenHome']+'/bin:/usr/local/bin:/usr/bin:/bin')
    command_rows=[]
    def run(name,argv,cwd,expected=0,jcc=None):
        local=env.copy()
        if jcc is not None:local['M3_JCC_CANDIDATE_ROOT']=str(jcc)
        out=logs/(name+'.stdout.log');err=logs/(name+'.stderr.log')
        with out.open('xb') as o,err.open('xb') as e:
            completed=subprocess.run(argv,cwd=cwd,env=local,stdout=o,stderr=e,check=False)
        row={'name':name,'argv':argv,'cwd':str(cwd),'exit':completed.returncode,'expected':expected,'stdoutSHA256':sha(out.read_bytes()),'stderrSHA256':sha(err.read_bytes()),'jccCandidateRoot':str(jcc) if jcc else None}
        write(logs/(name+'.json'),row);command_rows.append(row)
        require(completed.returncode==expected,'runtime command failed: '+name)
        return out.read_bytes(),err.read_bytes()
    plan_path=source/TASK/'crate/plan.json';plan=load(plan_path);rows=plan['outputs']
    target_count=manifest['provenance']['declaredOutputCount']
    require(target_count==24 and len(rows)==target_count and {r['path'] for r in rows}=={r['path'] for r in manifest['producerOutputs']},'twenty-four exact target installer closure')
    require(manifest['provenance']['addedTargetPaths']==sorted(ADDED_TARGETS),'exact three declared additions')
    require({r['path'] for r in rows if r['before'] is None}==ADDED_TARGETS,'installer ABSENT preimages')
    operations={r['path']:('ADD' if r['path'] in ADDED_TARGETS else 'REPLACE') for r in rows}
    require(manifest['provenance']['outputOperations']==operations,'exact target operation mapping')
    producer_by_path={r['path']:r for r in manifest['producerOutputs']}
    expected=inventory(source);installed=area/'installed';shutil.copytree(source,installed)
    for row in rows:
        after=(plan_path.parent/row['after']['resource']).read_bytes()
        require(sha(after)==row['after']['sha256'],'installer postimage drift')
        produced=producer_by_path[row['path']]
        require(sha(after)==produced['sha256'] and len(after)==produced['bytes'],'installer postimage differs from actual recipe output')
        require((installed/row['path']).read_bytes()==after,'installed initial candidate mismatch')
        if row['before'] is None:
            require(row['path'] in ADDED_TARGETS,'undeclared ABSENT preimage')
            (installed/row['path']).unlink()
        else:
            require(row['path'] not in ADDED_TARGETS,'ADD target has a concrete preimage')
            before=(plan_path.parent/row['before']['resource']).read_bytes()
            require(sha(before)==row['before']['sha256'],'installer preimage drift')
            (installed/row['path']).write_bytes(before)
    require(all(not (installed/p).exists() for p in ADDED_TARGETS),'all additions absent before apply')
    before=inventory(installed);installer=source/INSTALLER
    def recipe(name,mode,root=installed,selected_plan=plan_path,expected_code=0):
        return run(name,[python,str(installer),mode,'--plan',str(selected_plan),'--root',str(root)],proof,expected_code)
    require(json.loads(recipe('installer-before','check')[0])['state']=='before','before state')
    refusals=[]
    for case in ['stale','mixed','partial','wrong-seal']:
        root=area/('refusal-'+case);shutil.copytree(installed,root);selected=plan_path
        if case=='stale':(root/rows[0]['path']).write_bytes(b'foreign preimage\n')
        elif case=='mixed':(root/rows[0]['path']).write_bytes((plan_path.parent/rows[0]['after']['resource']).read_bytes())
        elif case=='partial':(root/rows[0]['path']).unlink()
        else:
            cr=area/'wrong-seal-crate';shutil.copytree(plan_path.parent,cr);bad=load(cr/'plan.json');bad['plan_sha256']='0'*64;(cr/'plan.json').write_text(json.dumps(bad));selected=cr/'plan.json'
        frozen=inventory(root);out,err=recipe('installer-refuse-'+case,'apply',root,selected,2)
        require(not out and b'REFUSED:' in err and inventory(root)==frozen,'installer refusal changed destination: '+case)
        refusals.append({'case':case,'exit':2,'files':frozen})
    require(json.loads(recipe('installer-apply','apply')[0])['writes']==target_count and inventory(installed)==expected,'exact declared-target apply')
    require(json.loads(recipe('installer-repeat','apply')[0])['writes']==0 and inventory(installed)==expected,'fixed point')
    require(json.loads(recipe('installer-after','check')[0])['state']=='after','after state')
    require(json.loads(recipe('installer-rollback','rollback')[0])['writes']==target_count and inventory(installed)==before and all(not (installed/p).exists() for p in ADDED_TARGETS),'rollback including three ABSENT additions')
    require(json.loads(recipe('installer-reapply','apply')[0])['writes']==target_count and inventory(installed)==expected,'reapply')
    # The retained invalid source must remain evidence of the actual NUL defect.
    policy=[r for r in rows if r['path']=='m3/backports/compatibility_policy.py'][0]
    invalid=(plan_path.parent/policy['before']['resource']).read_bytes();require(invalid.count(b'\0')==1,'one original source NUL required')
    try:compile(invalid,'exact-before-compatibility-policy.py','exec')
    except SyntaxError as error:require('null bytes' in str(error),'unexpected before failure')
    else:raise ValueError('invalid NUL source unexpectedly compiled')
    validator=installed/MIGRATION
    for mode,code in [('validate',0),('complete',2)]:
        out,_=run('canonical-'+mode,[python,str(validator),mode,str(installed),'--previous',str(installed/OLDTASK/'inputs/master.json')],installed,code)
        require(b'MIGRATION_MANIFEST_VALID completion=INCOMPLETE' in out and inventory(installed)==expected,'bounded canonical validation')
    # Retain all six behavior bodies under all three exact validator/POM pairs.
    old_plan_path=source/'m3/migration/intake-20261005/crate/plan.json';old_plan=load(old_plan_path)
    migration_row=next(r for r in old_plan['outputs'] if r['path']==MIGRATION);profiles=[]
    old_pom=(source/JCC_CRATE/'pom-before-752.xml').read_bytes();current_pom=(source/JCC_POM).read_bytes()
    require(sha(old_pom)==OLD_POM_SHA and sha(current_pom)==CURRENT_POM_SHA,'exact historical and current JCC POM images')
    packet_ast=ast.parse((source/'m3/migration/test/test_jcc_handoff_packet.py').read_text())
    packet_names=sorted(n.name for c in packet_ast.body if isinstance(c,ast.ClassDef) and c.name=='JccHandoffPacketTests' for n in c.body if isinstance(n,ast.FunctionDef) and n.name.startswith('test_'))
    require(len(packet_names)==6,'six retained packet methods')
    for state,migration_state,pom_sha,plan_name,plan_sha,recipe_id,plan_seal in JCC_PROFILES:
        root=area/('jcc-'+state);shutil.copytree(source,root);image=migration_row[migration_state]
        code=(old_plan_path.parent/image['resource']).read_bytes();require(sha(code)==image['sha256'],'JCC historic image drift');(root/MIGRATION).write_bytes(code)
        pom=old_pom if state in ['before','after'] else current_pom
        require(sha(pom)==pom_sha,'JCC tuple POM identity');(root/JCC_POM).write_bytes(pom)
        selected=root/JCC_CRATE/plan_name;selected_plan=load(selected)
        require(sha(selected.read_bytes())==plan_sha and selected_plan['recipe_id']==recipe_id and selected_plan['plan_sha256']==plan_seal,'exact selected JCC plan')
        frozen=inventory(root)
        out,err=run('jcc-'+state,[python,'-m','unittest','discover','-s',str(root/'m3/migration/test'),'-p','test_jcc_handoff_packet.py','-v'],root,jcc=root)
        text=(out+b'\n'+err).decode();observed=re.findall(r'^(test_\w+) \([^)]+\) \.\.\. ok$',text,re.M)
        require(sorted(observed)==packet_names and re.findall(r'^Ran ([0-9]+) tests? in ',text,re.M)==['6'] and inventory(root)==frozen,'six retained JCC tests per profile')
        profiles.append({'profile':state,'tests':6,'methods':packet_names,'validatorSHA256':image['sha256'],'pomSHA256':pom_sha,'plan':plan_name,'planSHA256':plan_sha,'recipeId':recipe_id,'planSeal':plan_seal,'files':frozen})
    require(sha((installed/OLD_PACKET).read_bytes())==ORIGINAL_PACKET_SHA and sha((installed/OLD_RECEIPT).read_bytes())==ORIGINAL_RECEIPT_SHA,'original evidence bytes')
    out,err=run('old-packet-current-authority-refusal',[python,str(validator),'intake',str(installed),'--intake-packet',str(installed/OLD_PACKET),'--expected-sha256',ORIGINAL_PACKET_SHA],installed,2)
    require(not out and b'intake: canonical mapping binding' in err and inventory(installed)==expected,'old authority refusal')
    historical=area/'historical';shutil.copytree(source,historical);(historical/REGISTRY).write_bytes((source/OLDTASK/'inputs/head.json').read_bytes());hist_before=inventory(historical)
    out,_=run('historical-original-replay',[python,str(historical/MIGRATION),'intake',str(historical),'--intake-packet',str(historical/OLD_PACKET),'--expected-sha256',ORIGINAL_PACKET_SHA],historical)
    require(sha(out)==ORIGINAL_RECEIPT_SHA and out==(source/OLD_RECEIPT).read_bytes() and inventory(historical)==hist_before,'original historic receipt replay')
    current_packet=installed/OLDTASK/'packet.json';require(sha(current_packet.read_bytes())==CURRENT_PACKET_SHA,'current81 historical packet identity')
    command=[python,str(validator),'intake',str(installed),'--intake-packet',str(current_packet),'--expected-sha256',CURRENT_PACKET_SHA]
    receipts=[]
    for i in [1,2]:
        out,_=run('current81-replay-'+str(i),command,installed)
        require(sha(out)==CURRENT_RECEIPT_SHA and inventory(installed)==expected,'current81 receipt byte replay')
        receipt=json.loads(out);require(receipt['outputs']==35 and receipt['state']=='RECEIVED_FOR_REVIEW','retained35 receipt state')
        for field in ['wholeSourceTreeCovered','dependencyClosureComplete','originalToolchainCustodyRevalidated','runtimeAccepted']:require(receipt[field] is False,'retained acceptance field')
        require(receipt['productWrites']==0,'retained zero product writes');receipts.append(sha(out))
    out,err=run('current81-wrong-seal',command[:-1]+['0'*64],installed,2)
    require(not out and b'intake packet seal drift' in err and inventory(installed)==expected,'wrong packet seal refusal')
    # The explicit task entry exercises the existing installer from Maven, too.
    maven=[str(Path(tools['mavenHome'])/'bin/mvn'),'-o','-B','-ntp','-s',str(proof/'harness/empty-settings.xml'),'-gs',str(proof/'harness/empty-settings.xml'),'-Dmaven.repo.local='+tools['cache'],'-Dm3.python='+python,'-f',str(installed/TASK/'pom.xml'),'verify']
    run('task-maven-recipe',maven,installed)
    require(inventory(installed)==expected,'Maven task changed product context')
    write(proof/'runtime-report.json',{'schema':'m3.current-752-runtime/1','state':'PASS_BOUNDED_RECEIVER_CHECKS','source':manifest['source'],'receiving':manifest['receiving'],'publicationHead':manifest['publicationHead'],'producerResultSHA256':manifest['provenance']['producerResultSHA256'],'additionalProducerResultSHA256':manifest['provenance']['additionalProducerResultSHA256'],'ciaProducerResultSHA256':manifest['provenance']['ciaProducerResultSHA256'],'cicProducerResultSHA256':manifest['provenance']['cicProducerResultSHA256'],'cihProducerResultSHA256':manifest['provenance']['cihProducerResultSHA256'],'addedTargetPaths':sorted(ADDED_TARGETS),'historical569ActiveExceptions':manifest['provenance']['historical569ActiveExceptions'],'declaredOutputCount':target_count,'installer':{'applyWrites':target_count,'repeatWrites':0,'rollbackWrites':target_count,'reapplyWrites':target_count,'refusals':refusals},'jccProfiles':profiles,'originalReceiptSHA256':ORIGINAL_RECEIPT_SHA,'currentReceiptSHA256':CURRENT_RECEIPT_SHA,'readOnlyReplays':receipts,'registrySHA256':REGISTRY_SHA,'registryRecords':81,'retainedOutputs':35,'installedFiles':expected,'historicalFiles':hist_before,'runtimeCommands':command_rows,'canonicalProductionApplied':False,'wholeSourceTreeCovered':False,'runtimeAccepted':False,'productWrites':0,'nativeReexecuted':False,'nativePublicationManifestSHA256':manifest['provenance']['nativePublicationManifestSHA256']})
    require(inventory(source)==manifest['files'],'frozen context changed')
    print('M3_CURRENT_752_RUNTIME_PASS')

if __name__=='__main__':main()
