# Copyright (c) 2026 Hitesh Soliwal and contributors to the Synexia Project.
# SPDX-License-Identifier: Apache-2.0
"""Execute the actual release workflow shell with bounded artifact fixtures.

These tests cover platform selection and package completeness. Fixture archives
are not JDK images and do not establish native build or installed-image coverage.
"""

from pathlib import Path
import io
import os
import subprocess
import tarfile
import tempfile
import unittest
import zipfile


ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / '.github/workflows/m3-release.yml'
PLATFORMS = ('linux-x64', 'macos-x64', 'macos-aarch64', 'windows-x64')
RIGHTS = (
    'LICENSE', 'ADDITIONAL_LICENSE_INFO', 'ASSEMBLY_EXCEPTION',
    'LICENSE-M3-APACHE-2.0.txt', 'M3-SYNEXIA-NOTICE.md',
    'M3-SYNEXIA-RIGHTS.md', 'M3-SYNEXIA-AUTHORS.md',
    'm3/release/release-contract.json',
)


def workflow_script(step_name):
    """Read one literal run block; do not maintain a duplicate implementation."""
    lines = WORKFLOW.read_text(encoding='utf-8').splitlines()
    marker = "      - name: '" + step_name + "'"
    index = lines.index(marker) + 1
    while index < len(lines) and lines[index] != '        run: |':
        if lines[index].startswith('      - '):
            raise AssertionError('step has no literal run block: ' + step_name)
        index += 1
    if index == len(lines):
        raise AssertionError('missing run block: ' + step_name)
    script = []
    for line in lines[index + 1:]:
        if line and not line.startswith('          '):
            break
        script.append(line[10:] if line else '')
    return '\n'.join(script) + '\n'


class ReleaseWorkflowTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='m3-release-workflow-')
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.env = dict(os.environ)
        self.env.update({
            'GITHUB_OUTPUT': str(self.root / 'github-output'),
            'GITHUB_SHA': 'a' * 40,
            'GITHUB_REF_TYPE': 'branch',
            'GITHUB_REF_NAME': 'release-preparation-fixture',
            'TAG': 'm3jdk-21.0.14-m3.1-preview',
            'RUNNER_TEMP': str(self.root / 'runner'),
            'ART': str(self.root / 'artifacts'),
            'OUT': str(self.root / 'out'),
        })
        for name in ('RUNNER_TEMP', 'ART', 'OUT'):
            Path(self.env[name]).mkdir(parents=True)
        for path in RIGHTS:
            destination = self.root / path
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_text('fixture\n', encoding='utf-8')
        version = self.root / 'make/conf/version-numbers.conf'
        version.parent.mkdir(parents=True)
        version.write_text(
            'DEFAULT_VERSION_FEATURE=21\nDEFAULT_VERSION_INTERIM=0\n'
            'DEFAULT_VERSION_UPDATE=14\nDEFAULT_VERSION_PATCH=0\n', encoding='utf-8')

    def run_step(self, name, **environment):
        return subprocess.run(
            ['bash', '-euo', 'pipefail'],
            input=workflow_script(name), text=True, capture_output=True,
            cwd=self.root, env={**self.env, **environment}, timeout=20,
        )

    def select(self, platforms):
        output = Path(self.env['GITHUB_OUTPUT'])
        output.unlink(missing_ok=True)
        result = self.run_step('Check what jobs to run', INPUT_PLATFORMS=platforms)
        values = {}
        if output.exists():
            values = {line.partition('=')[0]: line.partition('=')[2]
                      for line in output.read_text().splitlines()}
        return result, values

    def bundle(self, platform, with_jmods=True, zipped=False):
        directory = Path(self.env['ART']) / ('bundles-' + platform)
        directory.mkdir(parents=True)
        member = 'fixture-jdk/jmods/java.base.jmod' if with_jmods else 'fixture-jdk/bin/java'
        data = b'non-executable package completeness fixture\n'
        if zipped:
            with zipfile.ZipFile(directory / ('jdk-' + platform + '.zip'), 'w') as archive:
                archive.writestr(member, data)
        else:
            with tarfile.open(directory / ('jdk-' + platform + '.tar.gz'), 'w:gz') as archive:
                entry = tarfile.TarInfo(member)
                entry.size = len(data)
                archive.addfile(entry, io.BytesIO(data))

    def package(self, platforms):
        return self.run_step(
            'Name the bundles, extract jmods, copy the rights files',
            EXPECTED_PLATFORMS=platforms,
        )

    def tag(self, configure='', **environment):
        output = Path(self.env['GITHUB_OUTPUT'])
        output.unlink(missing_ok=True)
        result = self.run_step(
            'Validate the tag against make/conf/version-numbers.conf',
            INPUT_CONFIGURE_ARGUMENTS=configure, **environment)
        values = {}
        if output.exists():
            values = {line.partition('=')[0]: line.partition('=')[2]
                      for line in output.read_text().splitlines()}
        return result, values

    def test_branch_run_cannot_be_promoted_through_configure_input(self):
        for value in ('--with-debug-level=release\nrelease=true\ntag=m3jdk-21.0.14-m3.1-preview',
                      '\n', '--with-vendor-name=fixture\r', '--with-vendor-name=fixture\n'):
            with self.subTest(value=value):
                result, values = self.tag(value)
                self.assertNotEqual(0, result.returncode)
                self.assertEqual({}, values)

    def test_single_line_branch_configuration_is_preserved(self):
        result, values = self.tag('--with-debug-level=release --with-vendor-name=fixture')
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertEqual('false', values['release'])
        self.assertEqual('', values['tag'])
        self.assertEqual('--with-debug-level=release --with-vendor-name=fixture', values['configure-arguments'])

    def test_tag_must_match_declared_java_version(self):
        result, values = self.tag(
            GITHUB_REF_TYPE='tag', GITHUB_REF_NAME='m3jdk-21.0.99-m3.1-preview')
        self.assertNotEqual(0, result.returncode)
        self.assertEqual({}, values)

    def test_valid_preview_tag_retains_draft_build_identity(self):
        result, values = self.tag(
            GITHUB_REF_TYPE='tag', GITHUB_REF_NAME='m3jdk-21.0.14-m3.1-preview')
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertEqual('m3jdk-21.0.14-m3.1-preview', values['tag'])
        self.assertEqual('true', values['release'])
        self.assertEqual('--with-version-opt=m3.1-preview', values['configure-arguments'])

    def test_default_selection_requires_all_platforms(self):
        result, values = self.select('')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(' '.join(PLATFORMS), values['platforms'])
        self.assertTrue(all(values[platform] == 'true' for platform in PLATFORMS))

    def test_explicit_selection_does_not_expand_scope(self):
        result, values = self.select('linux-x64,windows-x64')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual('linux-x64 windows-x64', values['platforms'])
        self.assertEqual('false', values['macos-x64'])
        self.assertEqual('false', values['macos-aarch64'])

    def test_aliases_and_case_keep_existing_selection_contract(self):
        result, values = self.select(' MacOS , LINUX ')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual('linux-x64 macos-x64 macos-aarch64', values['platforms'])

    def test_duplicate_tokens_produce_one_stable_platform(self):
        result, values = self.select('linux,linux-x64,linux')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual('linux-x64', values['platforms'])

    def test_unknown_and_malformed_selection_is_refused_before_outputs(self):
        for value in ('linux-x64,linuz-x64', 'linux-arm', ',linux-x64',
                      'linux-x64,', 'linux-x64,,windows-x64',
                      'linux-x64\nwindows-x64', 'linux-x64;true',
                      'linux-x64\n', '\n', 'linux-x64\r', '\r'):
            with self.subTest(value=value):
                result, values = self.select(value)
                self.assertNotEqual(0, result.returncode)
                self.assertEqual({}, values)
                self.assertIn('::error::', result.stdout)

    def test_missing_selected_platform_is_not_silently_omitted(self):
        self.bundle('linux-x64')
        result = self.package('linux-x64 windows-x64')
        self.assertNotEqual(0, result.returncode)
        self.assertIn('windows-x64: required bundle artifact is missing', result.stdout)

    def test_unselected_platform_artifact_is_refused(self):
        self.bundle('linux-x64')
        self.bundle('windows-x64', zipped=True)
        result = self.package('linux-x64')
        self.assertNotEqual(0, result.returncode)
        self.assertIn('unexpected artifact for a deselected platform', result.stdout)

    def test_empty_selection_is_refused(self):
        result = self.package('')
        self.assertNotEqual(0, result.returncode)
        self.assertIn('did not select any platforms', result.stdout)

    def test_selected_directory_without_bundle_is_refused(self):
        (Path(self.env['ART']) / 'bundles-linux-x64').mkdir()
        result = self.package('linux-x64')
        self.assertNotEqual(0, result.returncode)
        self.assertIn('no jdk bundle', result.stdout)

    def test_bundle_without_jmods_is_refused(self):
        self.bundle('linux-x64', with_jmods=False)
        result = self.package('linux-x64')
        self.assertNotEqual(0, result.returncode)
        self.assertIn('no jmods directory', result.stdout)

    def test_linux_tar_bundle_and_required_rights_are_retained(self):
        self.bundle('linux-x64')
        result = self.package('linux-x64')
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        out = Path(self.env['OUT'])
        self.assertTrue((out / (self.env['TAG'] + '_linux-x64_bin.tar.gz')).is_file())
        with zipfile.ZipFile(out / (self.env['TAG'] + '_linux-x64_jmods.zip')) as archive:
            self.assertIn('jmods/java.base.jmod', archive.namelist())
        self.assertTrue(all((out / Path(path).name).is_file() for path in RIGHTS))

    def test_windows_zip_bundle_is_supported(self):
        self.bundle('windows-x64', zipped=True)
        result = self.package('windows-x64')
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        self.assertTrue((Path(self.env['OUT']) / (self.env['TAG'] + '_windows-x64_bin.zip')).is_file())

    def test_missing_rights_file_is_refused(self):
        self.bundle('linux-x64')
        (self.root / 'M3-SYNEXIA-NOTICE.md').unlink()
        result = self.package('linux-x64')
        self.assertNotEqual(0, result.returncode)
        self.assertIn('required rights or contract file missing', result.stdout)


if __name__ == '__main__':
    unittest.main()
