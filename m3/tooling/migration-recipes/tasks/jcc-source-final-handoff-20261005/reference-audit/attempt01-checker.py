"""Read-only reference audit of actual E3 postimages; separate from migration.py."""
from __future__ import annotations
import csv
import hashlib
import json
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

HERE = Path(__file__).resolve().parent
BASE = HERE.parent
ROOT = HERE / 'overlay'
OLD = BASE / 'candidate'
PUB = BASE.parent / 'publication-current'
CRATE = ROOT / 'm3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/jcc-source-final-handoff-20261005'
SOURCE = 'd1cf2d81ee74a4a837f78e0cd2e4ccae2b3d4906'
DEST = 'df06cdee5a8526f573b3ad89622824d0b49e2f25'
OWNER = 'da958d00d24154c0db87beca0ec80a7df2b43b73'
IDS = ('synexia.jcc-recipe-laboratory', 'synexia.jcc-java-jni-regression')

def load(p): return json.loads(p.read_text())
def ident(p):
    assert p.is_file() and not p.is_symlink(), p
    data = p.read_bytes()
    return dict(bytes=len(data), sha256=hashlib.sha256(data).hexdigest(), git_blob_sha1=hashlib.sha1(f'blob {len(data)}\0'.encode()+data).hexdigest(), mode=oct(p.stat().st_mode & 0o777))
def check(p, ref):
    got=ident(p)
    for k in ('bytes','sha256','git_blob_sha1'):
        if k in ref: assert got[k] == ref[k], (str(p),k,got[k],ref[k])
    return got

def main():
    out=HERE/'reference-audit'; assert not out.exists(); out.mkdir()
    plan=load(CRATE/'plan.json')
    canonical=(json.dumps({k:v for k,v in plan.items() if k!='plan_sha256'},sort_keys=True,separators=(',',':'),ensure_ascii=True)+'\n').encode()
    assert hashlib.sha256(canonical).hexdigest()==plan['plan_sha256']=='af65912789780e214f09644b38f542db5f7670b83ee1fada228bce08f8f9b30e'
    assert plan['source_commit']==SOURCE and plan['target_commit']==DEST
    previous=load(OLD/'m3/docs/name-mapping.json'); current=load(ROOT/'m3/docs/name-mapping.json')
    before={r['id']:r for r in previous['migration']['records']}; rows={r['id']:r for r in current['migration']['records']}
    assert list(before)==list(rows) and len(rows)==46
    assert {k:v for k,v in previous.items() if k!='migration'}=={k:v for k,v in current.items() if k!='migration'}
    assert {k:v for k,v in previous['migration'].items() if k!='records'}=={k:v for k,v in current['migration'].items() if k!='records'}
    assert len(current['migration']['gates'])==20
    for rid in set(rows)-set(IDS): assert rows[rid]==before[rid],rid
    oldbind=load(OLD/'m3/migration/evidence/jcc-handoff-20261005/source-destination-bindings.json')
    binding=load(ROOT/'m3/migration/evidence/jcc-handoff-20261005/source-destination-bindings.json')
    assert all(v is False for v in binding['acceptance'].values())
    assert binding['source_commit']==SOURCE and binding['destination_commit']==DEST
    assert binding['source_execution']['input_remote_revision']=='0b8dc32b9e8a616b7b7141bbdd88722839dc64bc'
    assert binding['source_execution']['published_output_revision']==SOURCE
    assert binding['source_execution']['source_export_admitted'] is False
    assert binding['source_execution']['driver_exit_code']==1 and len(binding['source_execution']['unrun_driver_gates'])==13
    assert binding['descriptor_reconciliation']==oldbind['descriptor_reconciliation']
    preserved=[]
    e2manifest=load(BASE/'PUBLICATION_MANIFEST.json')
    e2paths={x['path']:x for x in e2manifest['files']}
    outputs={x['path']:x for x in plan['outputs']}
    for path,ref in e2paths.items():
        check(OLD/path,ref)
        if path not in outputs:
            check(ROOT/path,ref); assert (ROOT/path).read_bytes()==(OLD/path).read_bytes()
            preserved.append({'path':path,**ident(ROOT/path)})
    assert len(preserved)==56
    materialized=[]
    for path,row in outputs.items():
        assert row['before'] is not None
        assert (CRATE/row['before']['resource']).read_bytes()==(OLD/path).read_bytes()
        check(OLD/path,row['before'])
        assert (ROOT/path).read_bytes()==(CRATE/row['after']['resource']).read_bytes()
        check(ROOT/path,row['after'])
        materialized.append({'path':path,'preimage':ident(OLD/path),'postimage':ident(ROOT/path)})
    acquired={x['path']:x for x in load(BASE/'ACQUISITION_VALIDATION_MANIFEST.json')['files']}
    refs=[]
    def dest(ref,location):
        path=ref['path']; got=check(ROOT/path,ref)
        if path in outputs:
            assert got['sha256']==outputs[path]['after']['sha256']; basis='E3_SEALED_MATERIALIZED_AFTERIMAGE'
        elif path in e2paths:
            assert got['sha256']==e2paths[path]['sha256']; basis='PUBLISHED_E2_DF06_READBACK'
        elif path in acquired:
            check(ROOT/path,acquired[path]); assert (ROOT/path).read_bytes()==Path(acquired[path]['local_path']).read_bytes(); basis='PINNED_DA958_ACQUISITION'
        else:
            assert path.startswith('m3/tooling/migration-recipes/tasks/jcc-source-final-handoff-20261005/source-final/'); basis='E3_RECEIPT_COPY'
        refs.append({'reference':location,'path':path,**got,'basis':basis})
    def nested(value,loc):
        if isinstance(value,dict):
            if 'path' in value and 'sha256' in value: dest(value,loc)
            for k,v in value.items(): nested(v,loc+'.'+k)
        elif isinstance(value,list):
            for i,v in enumerate(value): nested(v,f'{loc}[{i}]')
    oldplan=load(OLD/'m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/jcc-handoff-20261005/plan.json')
    assert plan['guards']==oldplan['guards'] and len(plan['guards'])==7
    for i,g in enumerate(plan['guards']): dest(g,f'plan.guards[{i}]')
    fields=load(HERE/'source-binding-review/SOURCE_FIELDS_AND_PROOFS.json')
    inventory={x['path']:x for k in ('production_changes','retained_mapped_sources','retained_contract_sources') for x in fields[k]}
    assert len(inventory)==17
    transport=load(BASE.parent/'current-integration-0b8dc/publication/FINAL_MANIFEST.json')
    transport={x['path']:x for x in transport['rows']}
    pub=load(PUB/'SOURCE_IMPLEMENTATION_PUBLICATION_VERIFIED.json')
    remote={x['path']:x for x in pub['tree_entries_verified']}
    package=load(PUB/'SOURCE_FINAL_REWRITE_PACKAGE_TREE.json')
    assert package['truncated'] is False
    package={x['path']:x for x in package['tree']}
    source_refs=[]
    for rid in IDS:
        row=rows[rid]; old=before[rid]
        assert row['status']=='blocked' and row['tests']==[]
        assert row['lineage']['previous_sources']==old['lineage']['previous_sources']+old['sources']
        for i,ref in enumerate(row['sources']):
            inv=inventory[ref['path']]; got=check(Path(inv['local_path']),ref);check(Path(inv['local_path']),inv)
            assert ref['commit']==SOURCE and ref['revision_role']=='pinned'
            assert ref['tracking_ref']=='refs/heads/aix/jcc-canonical-integration-20261005'
            if ref['path'] in transport:
                tr=transport[ref['path']]; rr=remote[ref['path']]
                assert got['sha256']==tr['sha256'] and got['git_blob_sha1']==tr['git_blob']==rr['git_blob']
                assert got['bytes']==tr['bytes']==rr['bytes'] and rr['verified'] is True
                basis='FINAL_TRANSPORT_AND_EXACT_PUBLISHED_TREE_READBACK'
            else:
                name=Path(ref['path']).name; pr=package[name]
                assert got['git_blob_sha1']==pr['sha'] and got['bytes']==pr['size'];basis='FINAL_REWRITE_PACKAGE_TREE_READBACK'
            source_refs.append({'reference':f'{rid}.sources[{i}]','path':ref['path'],**got,'commit':SOURCE,'basis':basis})
        for i,ref in enumerate(row['targets']): dest(ref,f'{rid}.targets[{i}]')
        dest(row['recipe'],f'{rid}.recipe')
    assert len(source_refs)==17
    oldfixture=oldbind['independent_receiving_fixture']['candidate_artifact'];newfixture=binding['independent_receiving_fixture']['candidate_artifact']
    assert rows[IDS[0]]['lineage']['previous_targets']==before[IDS[0]]['lineage']['previous_targets']+[oldfixture]
    assert rows[IDS[1]]['lineage']['previous_targets']==before[IDS[1]]['lineage']['previous_targets']
    assert {k:v for k,v in oldfixture.items() if k not in ('commit','revision_role')}=={k:v for k,v in newfixture.items() if k not in ('commit','revision_role')}
    assert oldfixture['commit'] is None and oldfixture['revision_role']=='candidate'
    assert newfixture['commit']==DEST and newfixture['revision_role']=='pinned'
    for k,v in oldbind['independent_receiving_fixture'].items():
        if k not in ('candidate_artifact','classification','qualification'): assert binding['independent_receiving_fixture'][k]==v,k
    assert binding['source_artifacts']==[s for rid in IDS for s in rows[rid]['sources']]
    for i,ref in enumerate(binding['existing_destination_reuse_artifacts']): dest(ref,f'binding.existing_destination_reuse_artifacts[{i}]')
    n=len(refs);nested(binding['independent_receiving_fixture'],'binding.independent_receiving_fixture');assert len(refs)-n==14
    for k,ref in binding['source_publication']['receipts'].items(): dest(ref,'binding.source_publication.receipts.'+k)
    assert len(binding['source_publication']['receipts'])==8
    coverage=binding['source_root_accounting']
    dest({'path':coverage['coverage_receipt_path'],'sha256':coverage['coverage_receipt_sha256']},'binding.source_root_accounting.coverage')
    for path in ('m3/docs/jcc-source-handoff.md','m3/migration/evidence/jcc-handoff-20261005/source-destination-bindings.json'):
        dest({'path':path,'sha256':outputs[path]['after']['sha256']},'blocked_record.observation')
    proofs=[]; proofinv={x['path']:x for x in fields['proof_references']}
    for i,ref in enumerate(binding['source_execution']['proofs']):
        inv=proofinv[ref['path']];got=check(Path(inv['local_path']),ref);check(Path(inv['local_path']),inv)
        tr=transport[ref['path']];rr=remote[ref['path']]
        assert ref['commit']==SOURCE and ref['repo']=='hsoliwal/com.synexia'
        assert ref['url']==f"https://github.com/hsoliwal/com.synexia/blob/{SOURCE}/{ref['path']}"
        assert got['sha256']==tr['sha256'] and got['git_blob_sha1']==tr['git_blob']==rr['git_blob']
        assert got['bytes']==tr['bytes']==rr['bytes'] and rr['verified'] is True
        proofs.append({'reference':f'binding.source_execution.proofs[{i}]','path':ref['path'],**got,'commit':SOURCE,'basis':'FINAL_TRANSPORT_AND_EXACT_PUBLISHED_TREE_READBACK'})
    assert len(proofs)==46
    roots=load(HERE/'source-binding-review/root-accounting/ROOT_ACCOUNTING.json')
    coverage_rows=list(csv.DictReader((ROOT/coverage['coverage_receipt_path']).open(),delimiter='\t'))
    rootentries={x['path']:x for x in roots['root_entries']}
    assert len(coverage_rows)==len(rootentries)==343
    assert {x['root_path'] for x in coverage_rows}==set(rootentries)
    declcount=0
    for row in coverage_rows:
        ent=rootentries[row['root_path']]
        assert (row['object_type'],row['mode'],row['object_id'])==(ent['type'],ent['mode'],ent['sha'])
        assert row['source_commit']==SOURCE and row['source_tree']==roots['final_root_tree']
        declarations=[x for x in roots['maven_declarations'] if x['path'].split('/')[0]==row['root_path']]
        assert row['root_pom_declarations']=='|'.join(x['path'] for x in declarations)
        assert row['declaration_contexts']=='|'.join(x['context'] for x in declarations)
        for k in ('export_admitted','destination_materialized','read_back_delivered'): assert row[k]=='false'
        declcount+=len(declarations)
    assert declcount==201
    freeze=load(HERE/'verification/inputs.json');stable=[]
    for group in ('fixed_inputs','external_source_and_proof_inputs'):
        for row in freeze[group]:
            got=check(Path(row['path']),row);assert got['mode']==row['mode']
            stable.append({'path':row['path'],**got,'group':group})
    result={'schema':'jcc-e3-explicit-reference-audit/1','observed_at_utc':datetime.now(timezone.utc).isoformat(),'result':'PASS','scope':'Read-only exact body/reference/preimage/lineage consistency; separate from ordinary blocked-row validator. Uses already recorded exact remote readbacks, performs no remote operation or gate rerun.','source_publication_commit':SOURCE,'destination_preimage_commit':DEST,'destination_owner_commit':OWNER,'plan_sha256':plan['plan_sha256'],'e2_preserved_payload_files':preserved,'four_materialized_afterimages':materialized,'source_reference_occurrences':source_refs,'source_proof_reference_occurrences':proofs,'destination_reference_occurrences':refs,'destination_distinct_paths':len({x['path'] for x in refs}),'destination_basis_counts':dict(Counter(x['basis'] for x in refs)),'ordered_records':46,'other_records_unchanged':44,'gates_unchanged':20,'four_old_source_objects_preserved':True,'exact_old_null_candidate_preserved':True,'descriptor_record_and_reconciliation_unchanged':True,'root_objects_checked':343,'paired_maven_declarations_checked':201,'frozen_inputs_rechecked':stable,'acceptance_flags_false':True,'capability_tests_empty':True,'receiver_behavior_rerun':False,'source_export_admitted':False,'full_module_or_jdk_acceptance':False,'unverified_active_hash_references_in_requested_two_rows':[],'limits':['This is an explicit bounded reference audit, not repository-wide semantic closure or source export admission.','Source91core and8parent passes coexist with66/67 upstream passes and one error;13later driver stages including JNI remain unexecuted.','The 4 original receiver behavior tests remain historical E2 evidence, not rerun.','Full owning-module coverage, product/JNI/JDK acceptance and current CI remain unqualified.','Historical source/target lineage is preserved as historical evidence, not repinned or promoted.']}
    p=out/'REFERENCE_AUDIT.json';p.write_text(json.dumps(result,indent=2)+'\n')
    print(json.dumps({'result':'PASS','source_refs':len(source_refs),'source_proofs':len(proofs),'destination_occurrences':len(refs),'destination_distinct_paths':result['destination_distinct_paths'],'frozen_inputs':len(stable),'preserved_e2_files':len(preserved),'sha256':ident(p)['sha256']}))

if __name__=='__main__': main()
