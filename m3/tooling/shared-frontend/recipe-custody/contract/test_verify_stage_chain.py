# SPDX-License-Identifier: Apache-2.0
"""Executed Java/JNI multi-pass source-chain refusal, resume and fixed-point fixtures."""
from pathlib import Path
import json
import hashlib
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).parent))
import verify_stage_chain as v


class ChainTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.name = 'stage-chain-fixture'
        self.crate = 'm3-stage-chain-fixture'
        self.data = self.root / 'm3/tooling/shared-frontend/recipe-custody/datasets' / self.name
        self.resources = self.data / 'resources'
        self.data.mkdir(parents=True)
        self.targets = {
            'src/java.base/share/classes/java/lang/M3String.java': (b'old string\n', b'final string\n', 'java'),
            'src/jdk.compiler/share/classes/com/sun/source/util/M3AST.java': (b'ast\n', b'ast\n', 'java'),
            'src/java.base/share/native/libjava/m3_dispatch.c': (b'old JNI\n', b'new JNI\n', 'text'),
            'test/jdk/java/lang/String/M3StringCursorTest.java': (None, b'new test\n', 'java'),
        }
        updates = [
            ('M3String.java', 'old string\n', 'intermediate string\n'),
            ('M3String.java', 'intermediate string\n', 'final string\n'),
            ('m3_dispatch.c', 'old JNI\n', 'new JNI\n'),
            ('M3StringCursorTest.java', None, 'new test\n'),
            ('M3AST.java', 'ast\n', 'ast\n'),
        ]
        self.snapshot_list = []
        cur = {p: old for p, (old, _, _) in self.targets.items()}
        self.snapshot_list.append(dict(cur))
        rows = []
        names = []
        for i, (basename, source, output) in enumerate(updates, 1):
            path = next(p for p in self.targets if p.endswith('/' + basename))
            stage = f'm3-pass-{i}'
            names.append(stage)
            kind = self.targets[path][2]
            stage_root = self.resources / 'com/synexia/rewrite' / ('hash-pinned-' + kind) / stage
            stage_root.mkdir(parents=True)
            template = f'{i:04d}-{basename}.txt'
            new = output.encode()
            (stage_root / template).write_bytes(new)
            if source is not None:
                (stage_root / 'pre').mkdir()
                (stage_root / 'pre' / template).write_bytes(source.encode())
            prehash = 'ABSENT' if source is None else v.digest(source.encode())
            stage_manifest = '\t'.join((path, prehash, v.digest(new), template)) + '\n'
            (stage_root / 'manifest.tsv').write_text(stage_manifest)
            rows.append('\t'.join((str(i), stage, path, template, f'atom-{i}', f'category-{i}')))
            cur[path] = new
            self.snapshot_list.append(dict(cur))
        (self.data / 'PASSES.tsv').write_text('# ordinal\tstage\tpath\ttemplate\tatom\tcategory\n'
                                               + '\n'.join(rows) + '\n')
        yaml='type: specs.openrewrite.org/v1beta/recipe\nrecipeList:\n'
        for stage in names:
            yaml+='  - com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe:\n      crateName: '+stage+'\n'
        dest = self.resources / 'META-INF/rewrite'
        dest.mkdir(parents=True)
        (dest / (self.crate+'.yml')).write_text(yaml)
        catalogue = 'category\tdecision\n' + ''.join(f'category-{n}\tSELECT\n' for n in range(1, 6))
        self.catalogue = self.root / 'm3/algorithm-catalogue.tsv'
        self.catalogue.parent.mkdir(parents=True, exist_ok=True)
        self.catalogue.write_text(catalogue)
        payload = catalogue.encode()
        catalogue_blob = hashlib.sha1(b'blob ' + str(len(payload)).encode() + b'\0' + payload).hexdigest()
        (self.data / 'DATASET.json').write_text(json.dumps({
            'dataset': self.name, 'crate_name': self.crate, 'targets': len(self.targets),
            'java_targets': 3, 'text_targets': 1,
            'category_catalogue': 'm3/algorithm-catalogue.tsv',
            'category_catalogue_blob': catalogue_blob,
        }))
        (self.data / 'MODES.tsv').write_text(''.join(p+'\t0644\n' for p in sorted(self.targets)))
        for kind in ('java','text'):
            stage_root = self.resources / 'com/synexia/rewrite' / ('hash-pinned-' + kind) / self.crate
            stage_root.mkdir(parents=True)
            manifest = []
            for i,(path,(initial,final,t)) in enumerate(sorted(self.targets.items()),1):
                if kind != t: continue
                template=f'aggregate-{i:04d}.txt'
                (stage_root / template).write_bytes(final)
                if initial is not None:
                    (stage_root / 'pre').mkdir(exist_ok=True)
                    (stage_root / 'pre' / template).write_bytes(initial)
                manifest.append('\t'.join((path,'ABSENT' if initial is None else v.digest(initial),
                                           v.digest(final),template)))
            if manifest:(stage_root/'manifest.tsv').write_text('\n'.join(manifest)+'\n')
        self.set_checkpoint(5)

    def tearDown(self):
        self.tmp.cleanup()

    def set_checkpoint(self,k):
        for path,payload in self.snapshot_list[k].items():
            file=self.root/path
            if payload is None:
                file.unlink(missing_ok=True)
            else:
                file.parent.mkdir(parents=True,exist_ok=True)
                file.write_bytes(payload)

    def test_full_chain_repeat_and_equal_last_pass(self):
        one=v.verify(self.root,self.name)
        self.assertEqual(one['stages'],5)
        self.assertEqual(one['targets'],4)
        self.assertEqual(one['remaining_stages'],0)
        self.assertEqual(one['checkpoint'],5)
        self.assertEqual(one,v.verify(self.root,self.name))

    def test_resume_at_every_boundary_without_reparsing(self):
        for i in range(6):
            self.set_checkpoint(i)
            got=v.verify(self.root,self.name,False)
            expected = 5 if i >= 4 else i
            self.assertEqual(got['checkpoint'],expected)
            if expected < 5:
                with self.assertRaisesRegex(ValueError,'not yet at terminal'):
                    v.verify(self.root,self.name,True)

    def test_torn_nonprefix_mix_refused(self):
        self.set_checkpoint(1)
        file=self.root/'src/java.base/share/native/libjava/m3_dispatch.c'
        file.write_bytes(b'new JNI\n')
        with self.assertRaisesRegex(ValueError,'neither sealed original'):
            v.verify(self.root,self.name,False)

    def test_cross_generation_drift_refused(self):
        p=self.root/'src/java.base/share/classes/java/lang/M3String.java'
        p.write_bytes(b'unsealed source\n')
        with self.assertRaisesRegex(ValueError,'neither sealed original'):
            v.verify(self.root,self.name,False)

    def test_stale_intermediate_manifest_refused(self):
        p=self.resources/'com/synexia/rewrite/hash-pinned-java/m3-pass-2/manifest.tsv'
        p.write_text(p.read_text().replace('a', 'f', 1))
        with self.assertRaises(ValueError):v.verify(self.root,self.name)

    def test_stage_output_tamper_refused(self):
        p=self.resources/'com/synexia/rewrite/hash-pinned-text/m3-pass-3/0003-m3_dispatch.c.txt'
        p.write_bytes(b'wrong\n')
        with self.assertRaisesRegex(ValueError,'SHA-256 drift'):
            v.verify(self.root,self.name)

    def test_donor_catalogue_drift_refused(self):
        self.catalogue.write_text(self.catalogue.read_text() + 'foreign\tSELECT\n')
        with self.assertRaisesRegex(ValueError, 'catalogue source changed'):
            v.verify(self.root, self.name)

    def test_uncatalogued_behavioral_atom_refused(self):
        p = self.data / 'PASSES.tsv'
        p.write_text(p.read_text().replace('category-2', 'missing-category'))
        with self.assertRaisesRegex(ValueError, 'uncatalogued'):
            v.verify(self.root, self.name)

    def test_wrong_pass_order_refused(self):
        p=self.data/'PASSES.tsv'
        p.write_text(p.read_text().replace('1\tm3-pass-1','9\tm3-pass-1'))
        with self.assertRaisesRegex(ValueError,'stage order'):
            v.verify(self.root,self.name)

    def test_extra_source_in_pass_refused(self):
        p=self.resources/'com/synexia/rewrite/hash-pinned-text/m3-pass-3/manifest.tsv'
        p.write_text(p.read_text()*2)
        with self.assertRaisesRegex(ValueError,'exactly one'):
            v.verify(self.root,self.name)

    def test_absent_input_mismatch_refused(self):
        p=self.resources/'com/synexia/rewrite/hash-pinned-java/m3-pass-4/manifest.tsv'
        p.write_text(p.read_text().replace('ABSENT', v.digest(b'not absent')))
        with self.assertRaisesRegex(ValueError,'noncontiguous'):
            v.verify(self.root,self.name)

    def test_source_symlink_refused(self):
        p=self.root/'test/jdk/java/lang/String/M3StringCursorTest.java'
        p.unlink()
        p.symlink_to(self.data/'DATASET.json')
        with self.assertRaisesRegex(ValueError,'symlinked'):
            v.verify(self.root,self.name)

    def test_unexpected_kind_refused(self):
        manifest=self.resources/'com/synexia/rewrite/hash-pinned-java'/self.crate/'manifest.tsv'
        p=manifest.read_text().replace('src/java.base/share/classes/java/lang/M3String.java',
                                       'src/java.base/share/classes/java/lang/M3String.c')
        manifest.write_text(p)
        with self.assertRaisesRegex(ValueError,'cross-kind'):
            v.verify(self.root,self.name)


if __name__ == '__main__':
    unittest.main()
