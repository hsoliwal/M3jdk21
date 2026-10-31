#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
# Modified 2026 by Hitesh Soliwal and contributors: select the current exact additive successor inventory named by source_set_successor_current.
# Modified 2026 by Hitesh Soliwal and contributors: accept the sealed transition-receipt successor shape next to the additive-manifest shape.
"""Prove current collections manifest custody and reject ambiguous donation JSON."""
import importlib.util
import ast
import hashlib
import json
import os
import subprocess
import tempfile
import unittest
import zipfile
from contextlib import ExitStack
from pathlib import Path
from unittest import mock

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("verify_donation", HERE / "verify_donation.py")
VERIFIER = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(VERIFIER)


class DonationPacketTest(unittest.TestCase):
    def full_verifier(self):
        path = HERE / 'qualification/full-java-jni-20261018/verify.py'
        spec = importlib.util.spec_from_file_location('full_verifier_contract', path)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)  # The existing __main__ guard is not entered.
        return module

    def prepare_fixture(self, provider, root, calls, jar_mode='module', native=True,
                        jar_source=None, native_source=None):
        """Record real command construction; producer effects are bounded fixture data."""
        out = root / 'out'
        jar = root / 'm3/arrays/target/m3-arrays-0.1.0-SNAPSHOT.jar'

        def run(*args):
            command = list(map(str, args[-1]))
            calls.append(command)
            if command[0] == 'fixture-mvn' and jar_mode:
                jar.parent.mkdir(parents=True, exist_ok=True)
                if jar_source is not None:
                    jar.write_bytes(Path(jar_source).read_bytes())
                else:
                    with zipfile.ZipFile(jar, 'w') as archive:
                        if jar_mode == 'module':
                            archive.writestr('module-info.class', b'fixture descriptor')
            if command[:2] == ['cmake', '--build'] and native:
                library = out / 'arrays-native/libm3_arrays.so'
                library.parent.mkdir(parents=True, exist_ok=True)
                library.write_bytes(Path(native_source).read_bytes()
                                    if native_source is not None else b'fixture library')

        with ExitStack() as stack:
            stack.enter_context(mock.patch.dict(os.environ, {
                'MVN': 'fixture-mvn', 'M3_MAVEN_OFFLINE': '1', 'M2_REPO': str(root / 'repo')}))
            if provider == 'donation':
                stack.enter_context(mock.patch.object(VERIFIER, 'REPO', root))
                return VERIFIER.prepare_arrays(out, run)
            module = self.full_verifier()
            stack.enter_context(mock.patch.object(module, 'ROOT', root))
            stack.enter_context(mock.patch.object(module, 'OUT', out))
            stack.enter_context(mock.patch.object(module, 'run', run))
            return module.prepare_arrays()

    def test_arrays_build_commands_reuse_existing_owners_and_offline_inputs(self):
        for provider in ('donation', 'full'):
            with self.subTest(provider=provider), tempfile.TemporaryDirectory() as folder:
                root = Path(folder)
                calls = []
                jar, library = self.prepare_fixture(provider, root, calls)
                self.assertTrue(jar.is_absolute() and library.is_absolute())
                self.assertEqual(3, len(calls))
                self.assertIn('-o', calls[0])
                self.assertIn('-Dmaven.repo.local=' + str(root / 'repo'), calls[0])
                self.assertEqual(str(root / 'm3/arrays/pom.xml'), calls[0][calls[0].index('-f') + 1])
                self.assertEqual(['-Dmaven.test.skip=true', 'install'], calls[0][-2:])
                self.assertEqual(['cmake', '-S', str(root / 'm3/arrays/src/main/native')], calls[1][:3])
                self.assertEqual(str(root / 'out/arrays-native'), calls[1][calls[1].index('-B') + 1])
                self.assertEqual(['cmake', '--build', str(root / 'out/arrays-native'),
                                  '--config', 'Release', '--parallel', '2'], calls[2])

    def test_missing_arrays_jar_refuses_before_native_build(self):
        for provider in ('donation', 'full'):
            with self.subTest(provider=provider), tempfile.TemporaryDirectory() as folder:
                calls = []
                with self.assertRaisesRegex((SystemExit, RuntimeError), 'module jar is absent'):
                    self.prepare_fixture(provider, Path(folder), calls, jar_mode=False)
                self.assertEqual(1, len(calls))

    def test_arrays_descriptor_is_required_before_native_build(self):
        for provider in ('donation', 'full'):
            with self.subTest(provider=provider), tempfile.TemporaryDirectory() as folder:
                calls = []
                with self.assertRaisesRegex((SystemExit, RuntimeError), 'module descriptor is missing'):
                    self.prepare_fixture(provider, Path(folder), calls, jar_mode='empty')
                self.assertEqual(1, len(calls))

    def test_missing_arrays_native_library_never_becomes_java_fallback(self):
        for provider in ('donation', 'full'):
            with self.subTest(provider=provider), tempfile.TemporaryDirectory() as folder:
                calls = []
                with self.assertRaisesRegex((SystemExit, RuntimeError), 'JNI library is absent'):
                    self.prepare_fixture(provider, Path(folder), calls, native=False)
                self.assertEqual(3, len(calls))

    def test_full_preflight_refuses_other_platform_before_git(self):
        module = self.full_verifier()
        with mock.patch.object(module.sys, 'platform', 'win32'), \
                mock.patch.object(module.subprocess, 'check_output') as git:
            with self.assertRaisesRegex(RuntimeError, 'requires Linux'):
                module.preflight()
            git.assert_not_called()

    def test_full_preflight_requires_full_jdk_before_git(self):
        module = self.full_verifier()
        with tempfile.TemporaryDirectory() as folder, \
                mock.patch.object(module.sys, 'platform', 'linux'), \
                mock.patch.object(module, 'JAVA', Path(folder)), \
                mock.patch.dict(os.environ, {'JAVA_HOME': folder}), \
                mock.patch.object(module.subprocess, 'check_output') as git:
            with self.assertRaisesRegex(RuntimeError, 'selected full JDK'):
                module.preflight()
            git.assert_not_called()

    def test_full_preflight_checks_expected_head_without_changing_evidence(self):
        module = self.full_verifier()
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root / 'bin').mkdir()
            for tool in ('java', 'javac', 'jar', 'jdeps'):
                (root / 'bin' / tool).write_bytes(b'fixture')
            with mock.patch.object(module.sys, 'platform', 'linux'), \
                    mock.patch.object(module, 'JAVA', root), \
                    mock.patch.dict(os.environ, {'JAVA_HOME': folder, 'EXPECTED_HEAD': 'expected'}), \
                    mock.patch.object(module.subprocess, 'check_output', return_value='different\n'):
                with self.assertRaisesRegex(RuntimeError, 'differs from expected head'):
                    module.preflight()
            with mock.patch.object(module.sys, 'platform', 'linux'), \
                    mock.patch.object(module, 'JAVA', root), \
                    mock.patch.dict(os.environ, {'JAVA_HOME': folder, 'EXPECTED_HEAD': 'expected'}), \
                    mock.patch.object(module.subprocess, 'check_output', return_value='expected\n'):
                self.assertEqual('expected', module.preflight())

    def test_source_manifest_escape_remains_refused(self):
        with mock.patch.object(VERIFIER, 'read_donation_packet', return_value={
                'installed_source_manifest': '../outside.tsv'}):
            with self.assertRaisesRegex(SystemExit, 'Invalid installed source manifest path'):
                VERIFIER.verified_source_rows()

    def test_preflight_and_source_guards_precede_output_mutation(self):
        legacy = ast.parse((HERE / 'verify_donation.py').read_text(encoding='utf-8'))
        full = ast.parse((HERE / 'qualification/full-java-jni-20261018/verify.py').read_text(encoding='utf-8'))
        for tree, required in ((legacy, 'verified_source_rows'), (full, 'preflight')):
            main = next(node for node in tree.body if isinstance(node, ast.FunctionDef) and node.name == 'main')
            calls = sorted((node.lineno, ast.unparse(node.func)) for node in ast.walk(main)
                           if isinstance(node, ast.Call))
            check = next(line for line, name in calls if name == required)
            mutations = [line for line, name in calls if name.endswith(('.mkdir', '.unlink', '.rmtree'))]
            self.assertTrue(mutations and check < min(mutations))

    def live_link_commands(self, provider, context):
        """Evaluate only the actual verifier's compiler/runtime argument expressions."""
        path = (HERE / 'verify_donation.py' if provider == 'donation' else
                HERE / 'qualification/full-java-jni-20261018/verify.py')
        tree = ast.parse(path.read_text(encoding='utf-8'))
        main = next(node for node in tree.body if isinstance(node, ast.FunctionDef)
                    and node.name == 'main')
        assignments = {node.targets[0].id: node.value for node in main.body
                       if isinstance(node, ast.Assign) and len(node.targets) == 1
                       and isinstance(node.targets[0], ast.Name)}

        def value(expression):
            return eval(compile(ast.Expression(expression), str(path), 'eval'),
                        {'__builtins__': {}, 'str': str, 'map': map, 'os': os}, context)

        calls = [node for node in ast.walk(main) if isinstance(node, ast.Call)
                 and isinstance(node.func, ast.Name) and node.func.id == 'run']
        if provider == 'donation':
            compiler = next(node.args[1] for node in calls if isinstance(node.args[0], ast.Constant)
                            and node.args[0].value == '02-module-compile')
            context['array_vm'] = value(assignments['array_vm'])
            context['runtime_classpath'] = value(assignments['runtime_classpath'])
            runtime = next(node.args[1] for node in calls if isinstance(node.args[0], ast.BinOp)
                           and isinstance(node.args[0].left, ast.Constant)
                           and node.args[0].left.value == '04-')
            return value(compiler), value(runtime)
        compiler = next(node.args[0] for node in calls if isinstance(node.args[0], ast.List)
                        and any(isinstance(item, ast.Constant) and item.value == '--module-path'
                                for item in node.args[0].elts))
        context['common'] = value(assignments['common'])
        context['cp'] = value(assignments['cp'])
        return value(compiler), value(assignments['java']) + ['jdk.m3.collections.ArrayLinkProbe']

    @unittest.skipUnless(os.environ.get('M3_BUILD_FIX_RUNTIME_INPUTS'),
                         'Bind qualified JDK/arrays inputs for the real modular runtime projection')
    def test_real_modular_jdk_runtime_and_missing_dependency_discriminators(self):
        """A temporary Java main is test data; no Java product/test owner is added."""
        inputs = json.loads(os.environ['M3_BUILD_FIX_RUNTIME_INPUTS'])
        java_home = Path(inputs['java_home'])
        suffix = '.exe' if os.name == 'nt' else ''
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            source = root / 'ArrayLinkProbe.java'
            source.write_text('''package jdk.m3.collections;
public final class ArrayLinkProbe {
  private ArrayLinkProbe() { }
  public static void main(String[] args) {
    Object cache = new M3LFUCache<Object, Object>(4);
    if (cache == null || !jdk.m3.arrays.M3ArrayNative.isLoaded()) throw new AssertionError();
    System.out.println("M3_ARRAYS_RUNTIME_LINK_PASS");
  }
}
''', encoding='utf-8')
            env = dict(os.environ, TEMP=folder, TMP=folder)
            for key in ('JAVA_TOOL_OPTIONS', '_JAVA_OPTIONS', 'JDK_JAVA_OPTIONS'):
                env.pop(key, None)
            env['JAVA_TOOL_OPTIONS'] = '-Djava.io.tmpdir=' + root.as_posix() + ' -XX:-UsePerfData'

            def execute(command):
                argv = list(map(str, command))
                result = subprocess.run(argv, cwd=root, env=env, capture_output=True, timeout=40)
                print(json.dumps({'projectionCommand': argv, 'returncode': result.returncode,
                                  'stdoutSha256': hashlib.sha256(result.stdout).hexdigest(),
                                  'stderrSha256': hashlib.sha256(result.stderr).hexdigest()}))
                return result

            tool_vm = ['-J-Djava.io.tmpdir=' + folder, '-J-XX:-UsePerfData']
            sources = [HERE / 'src/main/java/module-info.java',
                       HERE / 'src/main/java/jdk/m3/collections/M3LFUCache.java', source]
            for provider in ('donation', 'full'):
                provider_root = root / provider
                calls = []
                jar, library = self.prepare_fixture(provider, provider_root, calls,
                    jar_source=inputs['array_jar'], native_source=inputs['array_library'])
                classes = provider_root / 'classes'
                compiler = [java_home / 'bin' / ('javac' + suffix), *tool_vm, '--release', '21',
                            '-encoding', 'UTF-8', '-Xlint:all,-module', '-Werror']
                context = {'java_home': java_home, 'JAVA': java_home, 'common': compiler,
                           'array_jar': jar, 'array_library': library, 'classes': classes,
                           'test_classes': provider_root / 'test-classes', 'mains': sources,
                           'MODULE': provider_root / 'module', 'OUT': provider_root / 'out',
                           'name': 'ArrayLinkProbe'}
                if provider == 'full':
                    classes = context['OUT'] / 'classes'
                    (context['OUT'] / 'main.args').write_text(
                        '\n'.join('"' + path.as_posix() + '"' for path in sources) + '\n',
                        encoding='utf-8')
                actual_compiler, actual_runtime = self.live_link_commands(provider, context)
                result = execute(actual_compiler)
                self.assertEqual(0, result.returncode, result.stderr.decode(errors='replace'))
                missing = execute(compiler + ['-d', provider_root / 'missing-classes', *sources])
                self.assertNotEqual(0, missing.returncode)
                self.assertIn(b'jdk.m3.arrays', missing.stderr)
                projection = provider_root / 'projection.jar'
                packaged = execute([java_home / 'bin' / ('jar' + suffix), *tool_vm,
                                    '--create', '--file', projection, '-C', classes, '.'])
                self.assertEqual(0, packaged.returncode, packaged.stderr.decode(errors='replace'))
                deps = execute([java_home / 'bin' / ('jdeps' + suffix), *tool_vm,
                                '--module-path', jar, '--print-module-deps', projection])
                self.assertEqual(0, deps.returncode, deps.stderr.decode(errors='replace'))
                self.assertEqual('java.base,jdk.m3.arrays', deps.stdout.decode().strip())
                if provider == 'full':
                    # Maven supplies this directory in the real pipeline; copy only the
                    # tiny, freshly compiled fixture projection into that classpath slot.
                    destination = context['MODULE'] / 'target/classes'
                    for path in classes.rglob('*.class'):
                        target = destination / path.relative_to(classes)
                        target.parent.mkdir(parents=True, exist_ok=True)
                        target.write_bytes(path.read_bytes())
                vm = [java_home / 'bin' / ('java' + suffix), '-XX:-UsePerfData',
                      '-Djava.io.tmpdir=' + folder, '-Xcheck:jni']
                entry = ['--module', 'jdk.m3.collections/jdk.m3.collections.ArrayLinkProbe']
                linked = execute(actual_runtime)
                self.assertEqual(0, linked.returncode, linked.stderr.decode(errors='replace'))
                self.assertIn(b'M3_ARRAYS_RUNTIME_LINK_PASS', linked.stdout)
                bad_classpath = list(actual_runtime)
                cp_index = bad_classpath.index('-cp') + 1
                bad_classpath[cp_index] = os.pathsep.join(
                    item for item in str(bad_classpath[cp_index]).split(os.pathsep) if item != str(jar))
                missing_runtime = execute(bad_classpath)
                self.assertNotEqual(0, missing_runtime.returncode)
                self.assertIn(b'NoClassDefFoundError', missing_runtime.stderr)
                missing_jar = execute(vm + ['--module-path', projection, *entry])
                self.assertNotEqual(0, missing_jar.returncode)
                self.assertIn(b'jdk.m3.arrays', missing_jar.stdout + missing_jar.stderr)
                missing_native = execute(actual_runtime[:-1] + [
                    '-Dm3.arrays.native.path=' + str(root / 'missing.so'), actual_runtime[-1]])
                self.assertNotEqual(0, missing_native.returncode)
                self.assertIn(b'UnsatisfiedLinkError', missing_native.stderr)

    def test_current_packet_selects_a_sha256_sealed_manifest(self):
        packet = VERIFIER.read_donation_packet()
        self.assertEqual(
            "qualification/installed-source-enum-set-20261014.tsv",
            packet["installed_source_manifest"])
        self.assertEqual(
            "qualification/installed-source-enum-set-20261008.tsv",
            packet["historical_installed_source_manifest_20261008"])
        self.assertNotEqual(
            packet["installed_source_manifest"],
            packet["historical_installed_source_manifest_20261008"])
        manifest = HERE / packet["installed_source_manifest"]
        self.assertEqual(packet["installed_source_manifest_sha256"],
                         VERIFIER.digest(manifest))

    def test_current_successor_is_exact_additive(self):
        packet = VERIFIER.read_donation_packet()
        record = packet[packet["source_set_successor_current"]]
        if "successor_receipt" in record:
            self.assert_transition_receipt_successor(record)
            return
        self.assertEqual(record["installed_manifest"], packet["installed_source_manifest"])
        old_path = HERE / record["predecessor_manifest"]
        self.assertEqual(record["predecessor_manifest_sha256"], VERIFIER.digest(old_path))
        old = old_path.read_text()
        new = (HERE / packet["installed_source_manifest"]).read_text()
        self.assertTrue(new.startswith(old))
        rows = [line.split("\t") for line in new[len(old):].splitlines()
                if line and not line.startswith("#")]
        self.assertEqual(record["added_sources"], [row[0] for row in rows])
        self.assertEqual(record["installed_rows"], record["predecessor_rows"] + len(rows))
        for path, expected in rows:
            self.assertTrue(path.startswith("m3/collections/src/") and path.endswith(".java"))
            self.assertEqual(64, len(expected))
            self.assertNotIn(path + "\t", old)

    def test_duplicate_selector_is_refused_even_if_values_match(self):
        packet = (HERE / "m3-donation.json").read_text()
        duplicate = packet.replace(
            '"historical_installed_source_manifest_20261008"',
            '"installed_source_manifest"', 1)
        self.assertNotEqual(duplicate, packet)
        with self.assertRaisesRegex(ValueError,
                                    "Duplicate donation metadata key: installed_source_manifest"):
            json.loads(duplicate, object_pairs_hook=VERIFIER.unique_json_object)

    def test_nested_duplicate_is_refused(self):
        with self.assertRaisesRegex(ValueError,
                                    "Duplicate donation metadata key: source"):
            json.loads('{"history": {"source": 1, "source": 2}}',
                       object_pairs_hook=VERIFIER.unique_json_object)

    def test_loader_refuses_drift_without_changing_file(self):
        with tempfile.TemporaryDirectory() as folder:
            source = Path(folder) / "donation.json"
            broken = '{"installed_source_manifest": "new.tsv", "installed_source_manifest": "old.tsv"}'
            source.write_text(broken)
            with self.assertRaisesRegex(ValueError,
                                        "Duplicate donation metadata key"):
                VERIFIER.read_donation_packet(source)
            self.assertEqual(broken, source.read_text())

    def assert_transition_receipt_successor(self, record):
        receipt = HERE / record["successor_receipt"]
        self.assertEqual(record["successor_receipt_sha256"], VERIFIER.digest(receipt))
        self.assertEqual(record["successor_receipt_sha256"], VERIFIER._SUPERSET_RECEIPT_SHA256)
        self.assertEqual(record["inherited_composed_manifest_sha256"],
                         VERIFIER._SUPERSET_PREDECESSOR_SHA256)
        self.assertEqual(record["resulting_rows"], VERIFIER._SUPERSET_RESULT_ROWS)
        self.assertEqual(record["exact_java_source_membership"], VERIFIER._SUPERSET_RESULT_JAVA)
        rows = [line.split(chr(9)) for line in receipt.read_text(encoding="utf-8").splitlines()]
        self.assertEqual(record["successor_rows"], len(rows))
        self.assertEqual(record["successor_rows"], VERIFIER._SUPERSET_TRANSITIONS)
        for path, inherited, after in rows:
            self.assertTrue(path.startswith("m3/collections/"))
            self.assertEqual(64, len(after))
            self.assertTrue(inherited == "ABSENT" or len(inherited) == 64)


if __name__ == "__main__":
    unittest.main()
