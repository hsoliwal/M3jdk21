"""Independent actual-run evidence inspection. No candidate code is imported or run."""
from pathlib import Path
from datetime import datetime
import ast, hashlib, json, re, xml.etree.ElementTree as ET
B=Path('/workspace/scratch/1c68df1bae79/javac-convergence-20261005/work/m3jdk21-current/successor-e04');R=B/'overlay';V=B/'verification-v3';O=B/'execution-review-v3';S=O/'inputs'
P='m3/tooling/migration-recipes';C=R/P/'src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/jcc-source-recovery-handoff-20261005';SOURCE='0c53b4f1dfe66c7eff05c8047b8fc681d26fecb8';ROOT='0361e4a07b77a01df170523033fef01c4052ddc5';SEAL='0aa1f4a3e080c36d233066f0a6e1dc8e1047e6f0592dba60829b8cca3177df64'
def load(p):return json.loads(Path(p).read_text())
def identity(p):
 p=Path(p);assert p.is_file() and not p.is_symlink(),str(p);d=p.read_bytes();return {'path':str(p),'bytes':len(d),'sha256':hashlib.sha256(d).hexdigest(),'git_blob_sha1':hashlib.sha1(f'blob {len(d)}\0'.encode()+d).hexdigest(),'mode':oct(p.stat().st_mode&0o777)}
def check(p,ref):
 got=identity(p)
 for k in ('bytes','sha256','git_blob_sha1','mode'):
  if k in ref:
   value=('100755' if int(got[k],8)&0o111 else '100644') if k=='mode' and ref[k].startswith('100') else got[k]
   assert value==ref[k],(str(p),k)
 return got
snaps=[]
def snapshot(p,name=None):
 p=Path(p);q=S/(name or p.name);d=p.read_bytes()
 if q.exists():assert q.read_bytes()==d,('Review input drift',str(p))
 else:q.write_bytes(d)
 snaps.append({'source':str(p),'snapshot':str(q.relative_to(O)),**{k:v for k,v in identity(p).items() if k!='path'}})
 return load(p) if p.suffix=='.json' else d
receipt=snapshot(V/'receipt.json');assert identity(V/'receipt.json')['sha256']=='1f4ac9825d975520a04b7dc5ac1b282d96be32601c7abe2b0cfb676b0ff2df09';freeze=snapshot(V/'inputs.json');check(V/'inputs.json',receipt['inputs']);invocation=snapshot(B/'verification-invocation-v3/EXECUTION.json','INVOCATION_EXECUTION.json');snapshot(B/'verification-invocation-v3/STARTED.json','INVOCATION_STARTED.json')
for k in ('runner','bound_input','resource_admission','stdout','stderr'):check(invocation[k]['path'],invocation[k])
assert invocation['runner']['sha256']=='496e1877914e15f4342111c37020a2d58790038516b62872680427e585b21f50' and invocation['resource_admission']['sha256']=='df198e3ccda995d5d40fe9e17aca02507b3cb6b5077314c05b3c06b3eeb2cd28'
assert invocation['exit_code']==0 and invocation['stderr']['bytes']==0 and invocation['command'][1:]==['-B',str(B/'run_verification_v3.py')]
snapshot(invocation['stdout']['path'],'invocation-stdout.log');snapshot(invocation['stderr']['path'],'invocation-stderr.log')
packet=load(B/'bound-inputs-v3/BOUND_RESOURCES_REVIEW_PACKET.json');assert identity(B/'bound-inputs-v3/BOUND_RESOURCES_REVIEW_PACKET.json')['sha256']=='9024a1180d4f5856130d9e3a7dd1e8f18786ca8825c9569397d37003019aac9a'
plan=load(C/'plan.json');assert receipt['plan_sha256']==freeze['plan_sha256']==plan['plan_sha256']==SEAL
assert receipt['source_publication_commit']==SOURCE and receipt['source_publication_root']==ROOT and receipt['destination_preimage_commit']=='0994ecd65e86600f417f4d8702838d8c2663af61'
f=load(B/'FINAL_SOURCE_INPUTS_V3.json');assert receipt['source_publication_custody']==f['source_publication_custody']
for k in ('source_qualification_transfer','source_export_admitted','receiver_behavior_rerun','jdk_acceptance','remote_writes'):assert receipt[k] is False
for k in ('all_fixed_inputs_unchanged','four_outputs_equal_sealed_afterimages','source_context_declared_identities_admitted_before_freeze','source_context_receipt_identities_admitted_before_freeze'):assert receipt[k] is True
fixed=freeze['fixed_inputs'];external=freeze['external_source_and_proof_inputs'];assert len(fixed)==391 and len(external)==617 and len({r['path'] for r in fixed+external})==1008
for r in fixed+external:assert identity(r['path'])==r
outputs={r['path']:r for r in packet['outputs']};frozenpaths={r['path'] for r in fixed+external};fixedpaths={r['path'] for r in fixed}
current_nonbuild={str(p) for p in R.rglob('*') if p.is_file() and 'target' not in p.parts and str(p.relative_to(R)) not in outputs};assert fixedpaths <= current_nonbuild
postrun_paths=sorted(current_nonbuild-fixedpaths)
assert all(p.startswith(str(R/P/'tasks/jcc-source-recovery-handoff-20261005')+'/') for p in postrun_paths)
(O/'POSTRUN_ADDITIONAL_PATHS.json').write_text(json.dumps({'scope':'Observed subsequent task evidence staging; not part of execution freeze and not admitted by this review.','count':len(postrun_paths),'paths':postrun_paths},indent=2)+'\n')
assert len(freeze['operational_preimages'])==4
for before in freeze['operational_preimages']:
 relative=str(Path(before['path']).relative_to(R));admitted=outputs[relative];check(admitted['before']['local_path'],before);after=admitted['after'];got=check(R/relative,after);assert (R/relative).read_bytes()==Path(after['local_path']).read_bytes();assert got['mode']==before['mode']
for r in packet['prior_e3_preserved_payload_files']:check(r['local_path'],r)
assert len(packet['prior_e3_preserved_payload_files'])==89
for r in packet['guards']:check(r['local_path'],r)
for r in packet['entrypoint_test_and_proof_owners']:check(r['local_path'],r)
for key in ('plan','manifest'):check(packet[key]['local_path'],packet[key])
context=load(B/'current-context/CONTEXT_REFERENCES.json');externalpaths={r['path'] for r in external}
for r in context['current_context']:assert r['local_path'] in externalpaths;check(r['local_path'],r)
assert len(context['current_context'])==527
for r in context['receipts'].values():assert str(R/r['path']) in fixedpaths;check(R/r['path'],r)
assert len(context['receipts'])==10
expectations=load(B/'custody-v3-proposal/LOCAL_EXPECTATIONS.json');exceptions=expectations['exact_content_addressed_invalid_utf8_exceptions'];assert len(exceptions)==2
for r in exceptions:assert r['local_path'] in externalpaths;check(r['local_path'],r)
inv=load(f['source_inventory']['local_path'])
for group,count in [('sources',19),('proof_references',55)]:
 assert len(inv[group])==count
 for r in inv[group]:assert r['local_path'] in externalpaths
assert inv['source_execution']['qualification_status']=='BLOCKED'
ids=['01-check-E3-preimages','02-actual-text-recipe-maven','03-actual-packet-contract','04-apply','05-apply-fixed-point','06-rollback','07-replay','08-check-final','09-whole-map-validate','10-completion-remains-blocked'];assert [r['id'] for r in receipt['runs']]==ids
summary=[];previous_end=datetime.fromisoformat(freeze['frozen_at_utc']);assert previous_end>=datetime.fromisoformat(invocation['started_utc'])
for run in receipt['runs']:
 name=run['id'];assert run==load(V/(name+'.json'));snapshot(V/(name+'.json'))
 assert run['cwd']==str(R) and run['exit_code']==run['expected_exit_code']==(2 if name==ids[-1] else 0)
 start=datetime.fromisoformat(run['started_utc']);end=datetime.fromisoformat(run['ended_utc']);assert previous_end<=start<=end;previous_end=end
 for stream in ('stdout','stderr'):check(run[stream]['path'],run[stream]);snapshot(run[stream]['path'])
 summary.append({'id':name,'exit_code':run['exit_code'],'stdout_sha256':run['stdout']['sha256'],'stderr_sha256':run['stderr']['sha256']})
assert previous_end<=datetime.fromisoformat(invocation['finished_utc'])
python=receipt['toolchain']['python'];installer=str(R/'m3/migration/recipe.py');args=['--plan',str(C/'plan.json'),'--root',str(R)]
expected_installer={ids[0]:('check','before',0),ids[3]:('apply','after',4),ids[4]:('apply','after',0),ids[5]:('rollback','before',4),ids[6]:('apply','after',4),ids[7]:('check','after',0)}
installer_observed=[]
for run in receipt['runs']:
 if run['id'] not in expected_installer:continue
 mode,state,writes=expected_installer[run['id']];assert run['command']==[python,installer,mode,*args];out=load(run['stdout']['path']);assert (out['state'],out['files'],out['writes'],out['plan_sha256'])==(state,4,writes,SEAL) and run['stderr']['bytes']==0;installer_observed.append({'id':run['id'],'mode':mode,'state':state,'files':4,'writes':writes})
maven=receipt['runs'][1];pom=R/P/'verification/jcc-source-recovery-handoff/pom.xml';assert maven['command']==[receipt['toolchain']['maven'],'-B','-ntp','-o','-s',str(V/'empty-maven-settings.xml'),'-Dmaven.repo.local='+receipt['toolchain']['offline_repository'],'-f',str(pom),'clean','verify']
snapshot(V/'empty-maven-settings.xml');assert (V/'empty-maven-settings.xml').read_text()=='<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"/>\n'
mavenout=Path(maven['stdout']['path']).read_text();assert 'BUILD SUCCESS' in mavenout and 'Tests run: 12, Failures: 0, Errors: 0, Skipped: 0' in mavenout and mavenout.count('Compiling 1 source file with javac [debug release 21]')==2 and 'Copying 12 resources' in mavenout
mavenerr=Path(maven['stderr']['path']).read_text();assert mavenerr=='SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".\nSLF4J: Defaulting to no-operation (NOP) logger implementation\nSLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.\n'
check(receipt['java_xml']['path'],receipt['java_xml']);xmlbytes=snapshot(receipt['java_xml']['path']);suite=ET.fromstring(xmlbytes);assert {k:suite.attrib[k] for k in receipt['java_suite']}==receipt['java_suite'];assert {k:suite.attrib[k] for k in ('tests','failures','errors','skipped')}=={'tests':'12','failures':'0','errors':'0','skipped':'0'}
assert xmlbytes==(pom.parent/'target/surefire-reports/TEST-com.m3.rewrite.backport.JccSourceRecoveryHandoffTest.xml').read_bytes()
java=(R/P/'src/test/java/com/m3/rewrite/backport/JccSourceRecoveryHandoffTest.java').read_text();expected_methods=re.findall(r'@Test\s+void\s+(\w+)\(',java);cases=suite.findall('testcase');assert len(cases)==12 and {r.attrib['name'] for r in cases}==set(expected_methods)
assert not suite.findall('.//failure') and not suite.findall('.//error') and not suite.findall('.//skipped');assert all(r.attrib['classname']==suite.attrib['name'] for r in cases)
properties={r.attrib['name']:r.attrib['value'] for r in suite.findall('./properties/property')};assert properties['java.version']=='21.0.2' and properties['java.home']==receipt['toolchain']['jdk'] and properties['file.encoding']=='UTF-8'
pythonrun=receipt['runs'][2];testpath=R/'m3/migration/test/test_jcc_source_recovery_handoff_packet.py';assert pythonrun['command']==[python,'-B',str(testpath)] and pythonrun['stdout']['bytes']==0
pyast=ast.parse(testpath.read_text());expected_python=[n.name for c in pyast.body if isinstance(c,ast.ClassDef) for n in c.body if isinstance(n,ast.FunctionDef) and n.name.startswith('test_')];pythonerr=Path(pythonrun['stderr']['path']).read_text();observed_python=re.findall(r'^(test_\w+) \(__main__\..*\) \.\.\. ok$',pythonerr,re.M);assert len(observed_python)==len(expected_python)==6 and set(observed_python)==set(expected_python);assert re.search(r'^Ran 6 tests in [0-9.]+s\n\nOK\n$',pythonerr,re.M)
assert receipt['python_tests']==6 and receipt['python_refusal_checks']==(8+14+14+4+4)*3==132
for run,mode,code in [(receipt['runs'][8],'validate',0),(receipt['runs'][9],'complete',2)]:
 assert run['command']==[python,str(R/'m3/migration/migration.py'),mode,str(R),'--previous',str(C/'before00-name-mapping.json.txt')] and run['exit_code']==code
 assert Path(run['stdout']['path']).read_text()=='MIGRATION_MANIFEST_VALID completion=INCOMPLETE\n'
assert receipt['runs'][8]['stderr']['bytes']==0
reasons=Path(receipt['runs'][9]['stderr']['path']).read_text();assert 'full source tree inventory is incomplete' in reasons and 'synexia.jcc-recipe-laboratory: blocked' in reasons and 'synexia.jcc-java-jni-regression: blocked' in reasons
stream=[json.loads(x) for x in Path(invocation['stdout']['path']).read_text().splitlines()];assert len(stream)==11
for actual,run in zip(stream[:10],receipt['runs']):assert actual=={'id':run['id'],'exit_code':run['exit_code'],'expected':run['expected_exit_code']}
assert stream[-1]=={'result':'PASS_WITH_COMPLETION_BLOCKED','java_tests':12,'python_tests':6,'refusal_checks':132,'receipt_sha256':identity(V/'receipt.json')['sha256']}
for r in plan['outputs']:assert (R/r['path']).read_bytes()==(C/r['after']['resource']).read_bytes()
result={'schema':'jcc-e4-actual-execution-independent-review/1','disposition':'FOCUSED_RECEIVING_EXECUTION_PASSED_COMPLETION_BLOCKED','receipt_sha256':identity(V/'receipt.json')['sha256'],'source_commit':SOURCE,'source_root':ROOT,'plan_seal':SEAL,'actual_stage_count':10,'java_tests':12,'java_failures':0,'java_errors':0,'java_skipped':0,'java_case_names':sorted(expected_methods),'python_tests':6,'python_case_names':sorted(expected_python),'python_refusal_checks':132,'refusal_count_basis':'44 finite cases across3 modes in exact executed six-method test; inferred from admitted loops plus six OK outcomes, not132 individually logged traces.','installer_observations':installer_observed,'whole_map_validation':'VALID completion=INCOMPLETE; exit0','completion_refusal':'VALID completion=INCOMPLETE; expected exit2 with blocked reasons','fixed_inputs_rehashed_unchanged':391,'external_inputs_rehashed_unchanged':617,'total_unique_frozen_inputs_rehashed_unchanged':1008,'declared_current_context_bodies_unchanged':527,'declared_context_receipts_unchanged':10,'invalid_utf8_fixture_bodies_frozen_unchanged':2,'current_sources_frozen':19,'current_proofs_frozen':55,'prior_nonoperational_e3_payload_files_preserved':89,'operational_outputs_equal_admitted_afterimages':4,'operational_modes_preserved':True,'all_frozen_nonbuild_overlay_paths_preserved':True,'later_task_evidence_paths_outside_execution_freeze':len(postrun_paths),'maven_scope':'Offline empty user settings; one retained recipe owner and one test class,12 copied resources; strict release21 from admitted POM.','runtime_java_version':'21.0.2','maven_stderr':'Only retained SLF4J NOP-binding diagnostic; no compilation/test error.','source_qualification_status':'BLOCKED','source_export_admitted':False,'destination_capability_accepted':False,'reference_audit_reviewed_or_executed_by_reviewer':False,'publication_admitted':False,'reviewer_executed_candidate_or_gate':False,'remote_actions':False,'snapshots':snaps,'actual_stage_log_identities':summary}
(O/'EXECUTION_CHECKS.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k not in ('snapshots','actual_stage_log_identities','java_case_names','python_case_names')},indent=2))
