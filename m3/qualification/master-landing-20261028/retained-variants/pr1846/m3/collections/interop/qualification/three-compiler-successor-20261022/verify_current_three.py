#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Prove exactly three compiler-test source revisions on top of original M3JDK21.

No original production file, 36+7+40 source identity, or 158-method JUnit
receipt may change. Git is only the publication provenance boundary, not an
incremental build tree or semantic DAG. Synexia owns the transformation recipe.
"""
import argparse
import hashlib
import importlib.util
from pathlib import Path
import re
import subprocess

BASELINE_COMMIT = '31c8621d5fcad43708edada2ceb418af1f5e9e6c'
HISTORICAL_COMMIT = 'a720780b1a5ab41ea27182b9b7ad873efdf533b4'
TEST_PREFIX = 'm3/collections/interop/src/test/java/com/m3/collections/interop/'
SOURCE_ROOT = 'm3/collections/interop/src/'
CENSUS_ROOT = 'm3/collections/interop/qualification/'
SOURCE_REVISION_RECIPE = 'com.synexia.m3.M3Jdk21ThreeInteropCompilerRepairs20261022'
EXPECTED = (
    ('M3ApacheCollectionsTest.java',
     'ede37d37ca4373ac7888da6542f7447730be822829f789ed82caaaf99dc2ec53',
     '9edf50007e098846f0d677204a445b6bc52b7f0d',
     '754ba2038ec8b65e1cd1687c775b2f05b1e01b188b34bff3fe3c97098b3a4d7d',
     'e68a8d0e2cb37cf18afb05b10423ffb7f8b253a1', 'HISTORICAL_36'),
    ('M3FiveLibraryKeyRangesTest.java',
     'f2f8adcccfee3366007330f84a630ddfb1f093e99cdac060952dddb59dda486d',
     '9698cccbf6ca77c7e6a48eec23cee7781838b19e',
     'ef93d9a53e0b9935c6e796b55ce2c47e4d94a732c7ac9f07b5a635eb436cc467',
     'bda93a103282d5b8a14a9ad600b054a6484d0d19', 'ADDED_40'),
    ('M3FiveProviderFlagSelectionTest.java',
     '511c42a8f8cf2119770931307ef4c29f35e0fdbbc1ba438e170aa151905ca76e',
     '86939063fd00e472850add045086c07521ba29c3',
     '14e80741aade85814e34f06a5831337e82bbb3ea0b87c872ec8374c514893e39',
     '67cfca437535a37d4d2c32e10b01d6ca236cc325', 'ADDED_40'),
)
HEXSHA = re.compile(r'[0-9a-f]{64}\Z')
HEXGIT = re.compile(r'[0-9a-f]{40}\Z')


def git_blob(data: bytes) -> str:
    return hashlib.sha1(b'blob ' + str(len(data)).encode('ascii') + b'\0' + data).hexdigest()


def checked_file(root: Path, relative: str) -> bytes:
    name = Path(relative)
    if (name.is_absolute() or '..' in name.parts or '.' in name.parts
            or root.is_symlink()):
        raise ValueError('unsafe source path')
    path = root / name
    if path.is_symlink() or not path.is_file() or not path.resolve().is_relative_to(root):
        raise ValueError('missing/aliased original or current source: ' + relative)
    return path.read_bytes()


def exact_receipt(receipts: Path) -> dict[str, tuple[str, ...]]:
    source = receipts / 'changed-current-03.tsv'
    if source.is_symlink() or not source.is_file():
        raise ValueError('missing or aliased three-source revision receipt')
    lines = [line for line in source.read_text(encoding='utf-8').splitlines()
             if line and not line.startswith('#')]
    if len(lines) != 3:
        raise ValueError('exactly three source-revision rows required')
    found = {}
    for index, line in enumerate(lines):
        fields = line.split('\t')
        if len(fields) != 8:
            raise ValueError('unadmitted revision row shape')
        relative, old_sha, old_blob, new_sha, new_blob, origin, recipe, status = fields
        expected = EXPECTED[index]
        if (relative != TEST_PREFIX + expected[0]
                or tuple(fields[1:6]) != expected[1:]
                or recipe != SOURCE_REVISION_RECIPE
                or status != 'PENDING_JAVA21_INTEROP_FULL_SUITE'
                or not HEXSHA.fullmatch(old_sha) or not HEXSHA.fullmatch(new_sha)
                or not HEXGIT.fullmatch(old_blob) or not HEXGIT.fullmatch(new_blob)
                or relative in found):
            raise ValueError('source lineage does not match exact canonical revision')
        found[relative] = (old_sha, old_blob, new_sha, new_blob, origin)
    return found


def tracked(root: Path, relative: str) -> set[str]:
    return set(subprocess.check_output(
        ['git', 'ls-files', '--', relative], cwd=root, text=True).splitlines())


def pin_commit(root: Path, commit: str) -> None:
    actual = subprocess.check_output(['git', 'rev-parse', 'HEAD'],
                                     cwd=root, text=True).strip()
    if actual != commit:
        raise ValueError('wrong pinned M3JDK21 source epoch: ' + actual)


def imported(path: Path, name: str):
    spec = importlib.util.spec_from_file_location(name, path)
    if spec is None or spec.loader is None:
        raise ValueError('missing original source verifier: ' + str(path))
    result = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(result)
    return result


def verify_source(product: Path, baseline: Path, historical: Path,
                  receipts: Path) -> tuple[set[str], int]:
    product = product.resolve(strict=True)
    baseline = baseline.resolve(strict=True)
    historical = historical.resolve(strict=True)
    receipts = receipts.resolve(strict=True)
    pin_commit(baseline, BASELINE_COMMIT)
    pin_commit(historical, HISTORICAL_COMMIT)
    revisions = exact_receipt(receipts)

    old_verifier_path = (baseline / CENSUS_ROOT /
                         'current-census-superset-20261022/verify_superset.py')
    legacy = imported(old_verifier_path, 'm3_prior_76_source_census')
    original, old_revisions, additions = legacy.verify_product_checkout(
        baseline, baseline / CENSUS_ROOT / 'current-census-superset-20261022/receipts',
        historical)
    if (original, old_revisions, additions) != (36, 7, 40):
        raise ValueError('previous immutable 36+7+40 source proof failed')

    previous = tracked(baseline, SOURCE_ROOT)
    current = tracked(product, SOURCE_ROOT)
    if len(previous) != 76 or current != previous:
        raise ValueError('M3 interop tracked physical source set changed')

    # Previous 36, seven revisions, 40-addition and 158-JUnit receipts are
    # immutable. New source transitions live in their own additive receipt.
    for dirname in ('current-census-20261017',
                    'current-census-superset-20261022',
                    'runtime-junit-census-20261022'):
        relative = CENSUS_ROOT + dirname
        older_receipts = tracked(baseline, relative)
        now_receipts = tracked(product, relative)
        if older_receipts != now_receipts:
            raise ValueError('earlier M3 source/JUnit admission receipts changed: ' + dirname)
        for path in sorted(older_receipts):
            if checked_file(product, path) != checked_file(baseline, path):
                raise ValueError('historical M3 source/JUnit receipt bytes drifted: ' + path)

    junit = imported(baseline / CENSUS_ROOT /
                     'runtime-junit-census-20261022/verify_surefire.py',
                     'm3_previous_full_junit_census')
    expected_methods = junit.expected_methods(
        baseline, baseline / CENSUS_ROOT / 'runtime-junit-census-20261022/receipts')
    if len(expected_methods) != 158:
        raise ValueError('original 158 JUnit method IDs are no longer admitted')

    actual_revisions = 0
    for path in sorted(previous):
        prior = checked_file(baseline, path)
        current_bytes = checked_file(product, path)
        if path not in revisions:
            if current_bytes != prior:
                raise ValueError('unreviewed M3 source atom changed: ' + path)
            continue
        old_sha, old_blob, new_sha, new_blob, _origin = revisions[path]
        if (hashlib.sha256(prior).hexdigest() != old_sha or git_blob(prior) != old_blob
                or hashlib.sha256(current_bytes).hexdigest() != new_sha
                or git_blob(current_bytes) != new_blob):
            raise ValueError('source transition bytes/provenance mismatch: ' + path)
        if b'SPDX-License-Identifier: Apache-2.0' not in current_bytes[:1800]:
            raise ValueError('changed M3 source license attribution lost: ' + path)
        old_text = prior.decode('utf-8')
        new_text = current_bytes.decode('utf-8')
        if (junit.TEST.findall(old_text) != junit.TEST.findall(new_text)
                or len(re.findall(r'@Test\b', old_text))
                != len(re.findall(r'@Test\b', new_text))):
            raise ValueError('original JUnit method names/count changed: ' + path)
        actual_revisions += 1
    if actual_revisions != 3:
        raise ValueError('exact three source corrections not found')
    return expected_methods, actual_revisions


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--product', type=Path, required=True)
    parser.add_argument('--baseline', type=Path, required=True)
    parser.add_argument('--historical', type=Path, required=True)
    parser.add_argument('--receipts', type=Path, default=Path(__file__).parent / 'receipts')
    parser.add_argument('--reports', type=Path,
                        help='The complete original Java21 Maven Surefire XML directory')
    args = parser.parse_args()
    expected_methods, revised = verify_source(
        args.product, args.baseline, args.historical, args.receipts)
    if args.reports is not None:
        baseline_junit = imported(
            args.baseline.resolve() / CENSUS_ROOT /
            'runtime-junit-census-20261022/verify_surefire.py',
            'm3_original_surefire_contracts')
        suites = baseline_junit.verify_reports(args.reports, expected_methods)
        print(f'M3_INTEROP_THREE_REVISIONS_JUNIT_PASS sources=76 revisions={revised} '
              f'junit=158 suites={suites}')
    else:
        print(f'M3_INTEROP_THREE_REVISIONS_SOURCE_PASS sources=76 '
              f'revisions={revised} unchanged=73 junit=158')


if __name__ == '__main__':
    main()
