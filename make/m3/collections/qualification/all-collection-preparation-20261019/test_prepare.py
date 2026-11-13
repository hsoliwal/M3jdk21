# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
import prepare

class PreparationTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix='fixture-', dir=Path(__file__).parent)
        self.root = Path(self.temporary.name)
        self.repo = self.root / 'fixture-repo'; self.repo.mkdir()
        self.git('init', '-q')
        self.git('config', 'user.name', 'Preparation fixture')
        self.git('config', 'user.email', 'fixture@example.invalid')
        p = self.repo / 'm3/collections/src/main/java/Fixture.java'; p.parent.mkdir(parents=True)
        p.write_text('final class Fixture { private static int f(int x) { return x + 1; } }\n')
        (self.repo / 'm3/collections/pom.xml').write_text('<project/>\n')
        (self.repo / 'unrelated.txt').write_text('must remain outside selected scope\n')
        self.git('add', '.'); self.git('commit', '-qm', 'fixture baseline')
        self.item = prepare.inspect_checkout('hsoliwal/M3jdk21', self.repo)
    def tearDown(self): self.temporary.cleanup()
    def git(self, *args):
        return subprocess.run(['git','-C',str(self.repo),*args],check=True,capture_output=True)
    def packet(self, name='packet'):
        out=self.root/name; prepare.prepare([self.item],out); return out
    def test_exact_capture_and_source_unchanged(self):
        out=self.packet(); result=prepare.verify_packet(out)
        self.assertEqual(2,result['files']); self.assertFalse(result['product_ready'])
        self.assertEqual(b'',self.git('status','--porcelain').stdout)
    def test_repeat_is_deterministic(self):
        a=self.packet('first'); b=self.packet('second')
        self.assertEqual((a/'SOURCE_MANIFEST.json').read_bytes(),(b/'SOURCE_MANIFEST.json').read_bytes())
    def test_dirty_checkout_refused(self):
        (self.repo/'untracked').write_text('x')
        with self.assertRaises(ValueError): prepare.inspect_checkout('hsoliwal/M3jdk21',self.repo)
    def test_pin_mismatch_refused(self):
        with self.assertRaises(ValueError): prepare.inspect_checkout('hsoliwal/M3jdk21',self.repo,'0'*40)
    def test_existing_output_refused(self):
        out=self.packet()
        with self.assertRaises(FileExistsError): prepare.prepare([self.item],out)
    def test_duplicate_repository_refused(self):
        with self.assertRaises(ValueError): prepare.prepare([self.item,self.item],self.root/'duplicate')
    def test_output_inside_input_refused(self):
        with self.assertRaises(ValueError): prepare.prepare([self.item],self.repo/'output')
    def test_empty_input_is_not_estate_success(self):
        out=self.root/'empty'; packet=prepare.prepare([],out)
        self.assertFalse(packet['preparation_complete_for_supplied_checkouts'])
        self.assertEqual(set(prepare.REQUIRED),set(packet['missing_families']))
        self.assertFalse(prepare.verify_packet(out)['product_ready'])
    def test_missing_file_refused(self):
        out=self.packet(); p=next((out/'sources').rglob('Fixture.java')); p.unlink()
        with self.assertRaises(ValueError): prepare.verify_packet(out)
    def test_source_drift_refused(self):
        out=self.packet(); p=next((out/'sources').rglob('Fixture.java')); p.write_text('changed\n')
        with self.assertRaises(ValueError): prepare.verify_packet(out)
    def test_foreign_file_refused(self):
        out=self.packet(); (out/'sources/foreign').write_text('x')
        with self.assertRaises(ValueError): prepare.verify_packet(out)
    def test_forged_ready_refused_even_with_recomputed_manifest(self):
        out=self.packet(); p=out/'SOURCE_MANIFEST.json'; value=json.loads(p.read_text()); value['merge_ready']=True
        data=prepare.stable_json(value); p.write_bytes(data)
        (out/'SOURCE_MANIFEST.sha256').write_text(prepare.digest(data)+'  SOURCE_MANIFEST.json\n')
        with self.assertRaises(ValueError): prepare.verify_packet(out)
    def test_explicit_upstream_reference_is_read_only_input(self):
        reference = prepare.inspect_checkout('hrldcpr/pcollections', self.repo)
        self.assertEqual('PCOLLECTIONS', prepare.family(reference['repository']))
        self.assertEqual(b'', self.git('status', '--porcelain').stdout)
    def test_unapproved_upstream_refused(self):
        with self.assertRaises(ValueError):
            prepare.inspect_checkout('unapproved/pcollections', self.repo)
    def test_path_refusals(self):
        for value in ('../x','/x','a//b','a/./b','a/../b','a\\b','C:x','a\n'):
            with self.subTest(value=value),self.assertRaises(ValueError): prepare.canonical_path(value)

if __name__=='__main__': unittest.main()
