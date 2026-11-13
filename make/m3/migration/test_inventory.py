# SPDX-License-Identifier: Apache-2.0
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from inventory import scan
from recipe import Refusal
class InventoryTests(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.root=Path(self.temp.name)
        self.git('init','-q');self.git('config','user.email','fixture@example.invalid');self.git('config','user.name','Fixture')
        for path,text in {'m/pom.xml':'<project/>','m/src/MIndexX.java':'class MIndexX {}','m/native/helper.c':'int helper;','m/test/resource.txt':'resource','other/pom.xml':'<project/>','other/A.java':'class A {}','consumer/pom.xml':'<project/>','consumer/B.java':'MIndexX ref;','README.md':'root'}.items():
            p=self.root/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
        self.git('add','.');self.git('commit','-qm','fixture');self.pin=self.git('rev-parse','HEAD').strip()
    def tearDown(self):self.temp.cleanup()
    def git(self,*args):return subprocess.check_output(['git','-C',str(self.root),*args],stderr=subprocess.PIPE,text=True)
    def test_names_behavior_and_resources(self):
        result=scan(self.root,self.pin);self.assertTrue(result['file_accounting_complete']);self.assertFalse(result['inventory_exhaustive'])
        self.assertIn('m/native/helper.c',result['candidate_paths']);self.assertIn('consumer/B.java',result['candidate_paths'])
        self.assertNotIn('other/A.java',result['candidate_paths']);self.assertEqual(result['tracked_entries'],9)
    def test_worktree_changes_do_not_change_pin(self):
        first=scan(self.root,self.pin);(self.root/'m/src/MIndexX.java').write_text('modified')
        self.assertEqual(scan(self.root,self.pin),first)
    def test_exact_pin_required(self):
        with self.assertRaises(Refusal):scan(self.root,'HEAD')
    def test_symlink_not_followed(self):
        os.symlink('/missing-secret',self.root/'m/MIndexLink');self.git('add','.');self.git('commit','-qm','link')
        result=scan(self.root,self.git('rev-parse','HEAD').strip())
        link=next(x for x in result['entries'] if x['path']=='m/MIndexLink');self.assertEqual(link['mode'],'120000');self.assertEqual(link['disposition'],'excluded')
    def test_replay_is_deterministic(self):self.assertEqual(scan(self.root,self.pin),scan(self.root,self.pin))
if __name__=='__main__':unittest.main()
