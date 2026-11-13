# SPDX-License-Identifier: Apache-2.0
import importlib.machinery
import importlib.util
from pathlib import Path
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET

loader = importlib.machinery.SourceFileLoader('checker', sys.argv.pop(1))
spec = importlib.util.spec_from_loader(loader.name, loader)
checker = importlib.util.module_from_spec(spec)
loader.exec_module(checker)
FIXTURES = Path(__file__).resolve().parents[1] / 'resources/logs'
KERNEL = 'jdk.internal.mindex.M3BitLane28'


class EvidenceTest(unittest.TestCase):
    def changed(self, name, modify, compiler='c1', m3=False):
        tree = ET.parse(FIXTURES / name)
        modify(tree.getroot())
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'log.xml'
            tree.write(path)
            with self.assertRaises(ValueError):
                checker.qualify(path, compiler, KERNEL, m3)

    def test_captured_compilers_and_boundary(self):
        for module, kernel in [('lane28', KERNEL), ('tq', 'jdk.internal.mindex.M3TQ')]:
            for compiler in ['c1', 'c2']:
                with self.subTest(module=module, compiler=compiler):
                    result = checker.qualify(FIXTURES / f'{module}-{compiler}.xml', compiler, kernel)
                    self.assertEqual('compiled', result['mode'])
                    self.assertGreater(result['compiled_kernels'], 0)
            result = checker.qualify(FIXTURES / f'{module}-m3-c1.xml', 'c1', kernel, True)
            self.assertEqual('m3-interpreter', result['mode'])

    def test_captured_interpreter_is_not_compiler_evidence(self):
        with self.assertRaises(ValueError):
            checker.qualify(FIXTURES / 'lane28-m3-c1.xml', 'c1', KERNEL)

    def test_wrong_compiler(self):
        with self.assertRaises(ValueError):
            checker.qualify(FIXTURES / 'lane28-c1.xml', 'c2', KERNEL)

    def test_wrong_kernel_and_prefix_collision(self):
        for kernel in ['java.lang.String', KERNEL + 'Extra']:
            with self.assertRaises(ValueError):
                checker.qualify(FIXTURES / 'lane28-c1.xml', 'c1', kernel)

    def test_no_compilation(self):
        self.changed('lane28-c1.xml', lambda r: r.find('tty').clear())

    def test_truncated_log(self):
        self.changed('lane28-c1.xml', lambda r: r.remove(r.find('hotspot_log_done')))

    def test_m3_flag_is_required(self):
        def remove(root):
            args = root.find('vm_arguments/args')
            args.text = args.text.replace('-XX:+UseM3StringStorage', '')
        self.changed('lane28-m3-c1.xml', remove, m3=True)

    def test_requested_compiler_is_required(self):
        self.changed('lane28-m3-c1.xml', lambda r: None, compiler='c2', m3=True)

    def test_interpreter_property_is_required(self):
        def change(root):
            properties = root.find('vm_arguments/properties')
            properties.text = properties.text.replace('interpreted mode', 'mixed mode')
        self.changed('lane28-m3-c1.xml', change, m3=True)

    def test_compilation_under_m3_is_rejected(self):
        def add(root):
            ET.SubElement(root.find('tty'), 'nmethod', compiler='c1', method=KERNEL + ' get (I)Z')
        self.changed('lane28-m3-c1.xml', add, m3=True)

    def test_attribute_layout_is_not_significant(self):
        tree = ET.parse(FIXTURES / 'lane28-c1.xml')
        for node in tree.iter('nmethod'):
            node.attrib = dict(reversed(list(node.attrib.items())))
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'log.xml'
            tree.write(path)
            self.assertEqual('compiled', checker.qualify(path, 'c1', KERNEL)['mode'])


if __name__ == '__main__':
    unittest.main()
