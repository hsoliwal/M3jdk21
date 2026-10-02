# SPDX-License-Identifier: Apache-2.0
import copy
import unittest
from pathlib import Path
import tempfile
import importlib.util
from lineage import check, classify, closure, plan
from recipe import Refusal, digest, canonical

def fixture():
    source={'repository':'r/s','commit':'1'*40,'module':'m','path':'a.java','symbol':'a.A',
            'signature_kind':'javap','signatures':['public A()'],'sha256':'3'*64,'inspection':'content'}
    dest=dict(source,repository='r/t',commit='2'*40,path='b.java')
    entry={'id':'a','kind':'direct-port','status':'implemented-tested','sources':[source],'destinations':[dest],
      'owner':'a.A','identity_rules':'owner local','contract_differences':[], 'compatibility':{},'formats':[],
      'bootstrap':'outside java.base','dependencies':[],'consumers':[], 'materialization':[], 'facts_owner':'a.A',
      'recipe':{},'tests':['test'],'license':'Apache-2.0','publication':'authorized',
      'last_sync':{'target_hashes':['3'*64]},'target_adaptations':[],'conflicts':[],
      'direction':'source-to-target','rationale':'exact closure','lineage':[]}
    return {'schema':1,'mappings':[], 'migration':{'schema':'m3.migration/1','source_baseline':'1'*40,
      'target_baseline':'2'*40,'entries':[entry],'inventory_exhaustive':False,'gates':{'full':'OPEN'},
      'candidate_scope_sha256':'4'*64,'scope_files':{},'evidence':{'test':{'status':'PASS',
        'candidate_scope_sha256':'4'*64,'path':'e.txt','sha256':'5'*64}}}}

class LineageTests(unittest.TestCase):
    def test_partial_is_visible(self): self.assertEqual(check(fixture())['completion'],'NOT_COMPLETE')
    def test_complete_fails_closed(self):
        with self.assertRaises(Refusal): check(fixture(),complete=True)
    def test_pin_drift(self):
        with self.assertRaises(Refusal): check(fixture(),expected_source='6'*40)
    def test_new_unmapped(self):
        with self.assertRaises(Refusal):check(fixture(),inventory={'commit':'1'*40,'candidate_paths':['new.java']})
    def test_old_inventory(self):
        with self.assertRaises(Refusal):check(fixture(),inventory={'commit':'0'*40,'candidate_paths':[]})
    def test_mapping_ids_unique(self):
        m=fixture();m['migration']['entries']*=2
        with self.assertRaises(Refusal):check(m)
    def test_dependency_covered(self):
        m=fixture();m['migration']['entries'][0]['dependencies']=['unknown']
        with self.assertRaises(Refusal):check(m)
    def test_no_missing_fields(self):
        m=fixture();del m['migration']['entries'][0]['identity_rules']
        with self.assertRaises(Refusal):check(m)
    def test_missing_evidence(self):
        m=fixture();m['migration']['evidence']={}
        with self.assertRaises(Refusal):check(m)
    def test_failed_evidence(self):
        m=fixture();m['migration']['evidence']['test']['status']='FAIL'
        with self.assertRaises(Refusal):check(m)
    def test_borrowed_evidence(self):
        m=fixture();m['migration']['evidence']['test']['candidate_scope_sha256']='5'*64
        with self.assertRaises(Refusal):check(m)
    def test_claim_requires_pin(self):
        m=fixture();m['migration']['entries'][0]['destinations'][0]['commit']=None
        with self.assertRaises(Refusal):check(m)
    def test_claim_requires_signature(self):
        m=fixture();m['migration']['entries'][0]['sources'][0]['signatures']=[]
        with self.assertRaises(Refusal):check(m)
    def test_missing_destination(self):
        with tempfile.TemporaryDirectory() as d:
            with self.assertRaises(Refusal):check(fixture(),root=Path(d))
    def test_destination_evidence_and_scope_hashes(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);(root/'b.java').write_text('bytes');(root/'e.txt').write_text('evidence')
            m=fixture()['migration'];m['entries'][0]['destinations'][0]['sha256']=digest(b'bytes')
            m['scope_files']={'b.java':digest(b'bytes')};m['candidate_scope_sha256']=digest(canonical(m['scope_files']))
            m['evidence']['test'].update(sha256=digest(b'evidence'),candidate_scope_sha256=m['candidate_scope_sha256'])
            check({'migration':m},root=root)
            (root/'e.txt').write_text('changed')
            with self.assertRaises(Refusal):check({'migration':m},root=root)
    def test_scope_drift(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d);(root/'b.java').write_text('bytes');m=fixture();e=m['migration']
            e['entries'][0]['destinations'][0]['sha256']=digest(b'bytes');e['scope_files']={'b.java':'0'*64}
            with self.assertRaises(Refusal):check(m,root=root)
    def test_unsafe_path(self):
        m=fixture();m['migration']['entries'][0]['sources'][0]['path']='../secret'
        with self.assertRaises(Refusal):check(m)
    def test_tombstone_needs_lineage(self):
        m=fixture();m['migration']['entries'][0].update(kind='tombstone',status='tombstone',tests=[])
        with self.assertRaises(Refusal):check(m)
    def test_exclusion_needs_reason(self):
        m=fixture();m['migration']['entries'][0].update(kind='excluded',status='excluded',rationale='')
        with self.assertRaises(Refusal):check(m)
    def test_three_way_states(self):
        self.assertEqual(classify('a','a','b','b'),'unchanged')
        self.assertEqual(classify('a','x','b','b'),'source-change-review')
        self.assertEqual(classify('a',None,'b','b'),'source-deletion-review')
        self.assertEqual(classify('a','a','b','x'),'target-only-review')
        self.assertEqual(classify('a','x','b','x'),'converged-content-review')
        self.assertEqual(classify('a','x','b','y'),'CONFLICT-refuse-automatic-overwrite')
    def test_cycles_and_many_to_one_closure(self):
        entries=[{'id':'a','dependencies':['c']},{'id':'b','dependencies':['a']},{'id':'c','dependencies':['b']},{'id':'d','dependencies':[]}]
        self.assertEqual(closure(entries,{'a'}),['a','b','c'])
    def test_planner_never_writes(self):
        m=fixture();result=plan(m,{'a:0':{'source_sha256':'x','target_sha256':'y'}})
        self.assertEqual(result['writes'],0);self.assertEqual(result['dependency_closure'],['a'])
        self.assertFalse(result['automatic_reverse_port'])
    def test_multi_endpoint_is_not_guessed(self):
        m=fixture();m['migration']['entries'][0]['destinations']*=2
        result=plan(m,{'a:0':{'source_sha256':'x','target_sha256':'y'}})
        self.assertEqual(result['decisions'][0]['decision'],'multi-endpoint-review')
    def test_no_automatic_reverse(self):
        m=fixture();m['migration']['entries'][0]['direction']='bidirectional-auto'
        with self.assertRaises(Refusal):check(m)
    def test_schema_version(self):
        m=fixture();m['migration']['schema']='future'
        with self.assertRaises(Refusal):check(m)

class AuthorityLineageTests(unittest.TestCase):
    def setUp(self):
        spec = importlib.util.spec_from_file_location('authority_fixture',
                Path(__file__).parent / 'test/test_migration.py')
        module = importlib.util.module_from_spec(spec); spec.loader.exec_module(module)
        self.fixture = module.MigrationTest(); self.fixture.setUp()
        self.addCleanup(self.fixture.doCleanups)

    def test_current_authority_is_partial_and_artifact_checked(self):
        result = check(self.fixture.doc, self.fixture.root)
        self.assertEqual(result['completion'], 'NOT_COMPLETE')
        self.fixture.output.write_bytes(b'drift')
        with self.assertRaisesRegex(Refusal, 'hash drift'):
            check(self.fixture.doc, self.fixture.root)

    def test_current_completion_and_inventory_refuse(self):
        with self.assertRaisesRegex(Refusal, 'completion refused'):
            check(self.fixture.doc, self.fixture.root, complete=True)
        with self.assertRaisesRegex(Refusal, 'unmapped'):
            check(self.fixture.doc, self.fixture.root,
                  inventory={'commit':'a'*40,'candidate_paths':['unmapped.java']})

    def test_current_planner_uses_historical_target_for_candidate(self):
        row = self.fixture.record
        previous = dict(row['targets'][0], commit='b'*40, revision_role='pinned', sha256='2'*64)
        row['lineage']['previous_targets'] = [previous]
        result = plan(self.fixture.doc, {'m3.prefix-z:0':{
            'source_sha256':row['sources'][0]['sha256'], 'target_sha256':row['targets'][0]['sha256']}})
        self.assertEqual(result['decisions'][0]['decision'], 'target-only-review')
        self.assertEqual(result['writes'], 0)


if __name__=='__main__':unittest.main()
