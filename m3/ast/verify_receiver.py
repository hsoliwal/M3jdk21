#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Bounded Java21 receiver proof with an exact, qualified source successor."""
from pathlib import Path
import hashlib
import importlib.util
import json
import re
import shutil
import subprocess
import sys
import tempfile

sys.dont_write_bytecode = True
THIS = Path(__file__).resolve().parent
ROOT = THIS.parents[1]
SOURCE = THIS / 'src/main/java/com/m3/ast/M3ASTHierarchy.java'
TEST = THIS / 'src/test/java/com/m3/ast/M3ASTHierarchyContract.java'
ENCODING = THIS / 'src/test/java/com/m3/ast/M3ASTHierarchyEncodingTest.java'
BORROW = THIS / 'SOURCE_BORROW.tsv'
SUCCESSOR = THIS / 'SOURCE_SUCCESSOR.json'
CUSTODY = THIS / 'recipe-custody/hierarchy-encoding-20261018'
HISTORICAL_BORROW_SHA256 = '8f39c3ea44bdbdbf7b88b6b495a0c897724ec2bc79d33fcf28737b91d77d78d7'
EXPECTED_SUCCESSOR = {'schema': 'M3_AST_HIERARCHY_SOURCE_SUCCESSOR_V1',
 'canonical_repository': 'hsoliwal/com.synexia',
 'canonical_baseline_commit': '98368e107f4a205d852ef958cd48c1ee633219b1',
 'canonical_source_commit': '9f500fff0bcdf96807899e96090e245fb530c48b',
 'recipe_owner': 'synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017',
 'recipe_name': 'com.synexia.m3jdk.CollectionsContractRefinement20261017',
 'canonical_dataset': 'ast-hierarchy-20261018',
 'target_dataset': 'ast-hierarchy-target-20261018',
 'previous_source_sha256': 'f9d353aa68b31d4d723d584940cb87a39163958d2e7c0962e2a648c96fcf1434',
 'source_sha256': '984d8498e19fc267d338aef1d0136966918855087f78ca8fdb2fce50b9724721',
 'previous_target_sha256': 'f3fe6810edce185dab20ca73ffb03edaf5a778eb60c41b432e0f902d574a8441',
 'target_sha256': '068cb795d6617d77ca5297adf96419fadd97f1f40e6b0a63b36f43b222c6c36a',
 'legacy_contract_sha256': '5086649c5e87cfa418247a175f232c273db121d56ea1afff646de0f615d932c1',
 'custody_manifest_sha256': '9edd442da2af230f61581130bc94be1999c11e53251cc54f84fdb288dff8df9a',
 'compatibility': 'well-formed UTF-16 retains exact v1 roots; malformed identifiers use a disjoint '
                  'framed extension',
 'scope': 'INDEPENDENT_TARGET_MODULE; CANDIDATE_ONLY'}


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(cmd):
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError(f'FAIL exit={result.returncode} {cmd} stderr={result.stderr[-900:]}')
    return result.stdout.strip()


def admitted_file(root, relative):
    path = root / relative
    if not path.resolve().is_relative_to(root.resolve()):
        raise AssertionError('custody path escapes root: ' + relative)
    cursor = path
    while cursor != root:
        if cursor.is_symlink():
            raise AssertionError('symlink custody path: ' + relative)
        cursor = cursor.parent
    if not path.is_file():
        raise AssertionError('missing custody file: ' + relative)
    return path


def qualify_successor(fields):
    receipt = json.loads(SUCCESSOR.read_text(encoding='utf-8'))
    if receipt != EXPECTED_SUCCESSOR:
        raise AssertionError('source successor differs from the reviewed transition')
    revision = receipt['canonical_source_commit']
    if not re.fullmatch(r'[0-9a-f]{40}', revision) or revision == receipt['canonical_baseline_commit']:
        raise AssertionError('successor requires its published immutable canonical source commit')
    if (receipt['previous_target_sha256'] != fields['target_sha256']
            or receipt['previous_source_sha256'] != fields['source_sha256']):
        raise AssertionError('successor does not extend the historical source seal')
    if digest(TEST) != receipt['legacy_contract_sha256']:
        raise AssertionError('historical behavior contract drift')
    cursor = CUSTODY
    while cursor != THIS:
        if cursor.is_symlink():
            raise AssertionError('symlink custody root')
        cursor = cursor.parent
    if any(p.is_symlink() for p in CUSTODY.rglob('*')):
        raise AssertionError('symlink in custody packet')
    manifest = admitted_file(CUSTODY, 'CUSTODY_SHA256.tsv')
    if digest(manifest) != receipt['custody_manifest_sha256']:
        raise AssertionError('custody manifest drift')
    rows = []
    for line in manifest.read_text(encoding='utf-8').splitlines():
        relative, expected = line.split('\t')
        if (not relative or Path(relative).as_posix() != relative or Path(relative).is_absolute()
                or '..' in Path(relative).parts or '\\' in relative or not re.fullmatch(r'[0-9a-f]{64}', expected)):
            raise AssertionError('invalid custody manifest entry')
        rows.append(relative)
        if digest(admitted_file(CUSTODY, relative)) != expected:
            raise AssertionError('custody file drift: ' + relative)
    if rows != sorted(set(rows)):
        raise AssertionError('custody entries must be sorted and unique')
    actual = sorted(p.relative_to(CUSTODY).as_posix() for p in CUSTODY.rglob('*')
                    if p.is_file() and p != manifest)
    if rows != actual:
        raise AssertionError('unexpected or missing custody files')
    # Retained SDK Results are relocated, never recreated from recipe templates.
    # Verification leaves the checked-in immutable custody packet unchanged.
    with tempfile.TemporaryDirectory(prefix='m3ast-custody-') as directory:
        crate = Path(directory) / 'crate'
        shutil.copytree(CUSTODY, crate)
        shutil.copytree(crate / 'qualified', crate / 'target')
        spec = importlib.util.spec_from_file_location('m3ast_qualified_receiver', crate / 'receive.py')
        receiver = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(receiver)
        canonical = receiver.qualification(crate, receipt['canonical_dataset'], crate / 'sdk-owner')
        target = receiver.qualification(crate, receipt['target_dataset'], crate / 'sdk-owner')
        for qualified in (canonical, target):
            if qualified['metadata']['recipe_name'] != receipt['recipe_name']:
                raise AssertionError('canonical recipe identity drift')
        source_path = fields['source_file']
        if canonical['manifests'][source_path] != (receipt['previous_source_sha256'], receipt['source_sha256']):
            raise AssertionError('canonical source transition drift')
        if target['manifests'][fields['target_file']] != (receipt['previous_target_sha256'], receipt['target_sha256']):
            raise AssertionError('target source transition drift')
        for relative, data in target['outputs'].items():
            local = admitted_file(THIS, str(Path(relative).relative_to('m3/ast')))
            if local.read_bytes() != data:
                raise AssertionError('target differs from actual SDK Result: ' + relative)
    print('PASS source successor', revision, receipt['canonical_dataset'], receipt['target_dataset'])


if digest(BORROW) != HISTORICAL_BORROW_SHA256:
    raise SystemExit('FAIL: historical SOURCE_BORROW.tsv drift')
fields = dict(line.split('\t', 1) for line in BORROW.read_text().splitlines()[1:])
if fields['source_recipe'] != 'com.synexia.rewrite.M3ASTL0L5Target20261017':
    raise SystemExit('FAIL: missing historical canonical recipe identity')
if not TEST.is_file() or not SOURCE.is_file():
    raise SystemExit('FAIL: incomplete Java sources')
successor = SUCCESSOR.exists()
if successor:
    qualify_successor(fields)
elif digest(SOURCE) != fields['target_sha256']:
    raise SystemExit('FAIL: generated receiver source drift without a qualified successor')

classes = []
for iteration in range(2):
    with tempfile.TemporaryDirectory(prefix='m3ast-receiver-') as directory:
        output = Path(directory)
        sources = [str(SOURCE), str(TEST)]
        if successor:
            sources.append(str(ENCODING))
        run(['javac', '--release', '21', '-proc:none', '-Xlint:all', '-Werror', '-d', str(output), *sources])
        observed = run(['java', '-Xmx256m', '-cp', str(output), 'com.m3.ast.M3ASTHierarchyContract', '2000'])
        if 'checks=39550' not in observed or 'atoms=17967' not in observed:
            raise AssertionError('unexpected target corpus result: ' + observed)
        if successor:
            encoding = run(['java', '-Xmx256m', '-cp', str(output), 'com.m3.ast.M3ASTHierarchyEncodingTest'])
            if encoding != 'PASS HierarchyEncoding checks=10311':
                raise AssertionError('unexpected identifier-encoding result: ' + encoding)
            print('PASS encoding run', iteration, encoding)
        classes.append({p.relative_to(output).as_posix(): digest(p) for p in sorted(output.rglob('*.class'))})
        print('PASS run', iteration, observed)
if classes[0] != classes[1]:
    raise AssertionError('classfile repeat drift')
print('PASS receiver source pinned; repeat classfiles', len(classes[0]))
if (ROOT / 'm3/collections/src/main/java/com/m3/collections').is_dir():
    census = run(['python3', str(THIS / 'collection_source_census.py')])
    print('SOURCE_CENSUS_ONLY', len([s for s in census.splitlines() if '\t' in s]) - 1)
else:
    print('COLLECTION_SOURCE_CENSUS=NOT_RUN; full target checkout unavailable')
print('RETAINED_OPENREWRITE_SDK_QUALIFICATION=' + ('VERIFIED' if successor else 'HISTORICAL_ONLY'))
print('MAVEN_OPENREWRITE_RERUN_JTREG_JNI_IMAGE=NOT_RUN')
