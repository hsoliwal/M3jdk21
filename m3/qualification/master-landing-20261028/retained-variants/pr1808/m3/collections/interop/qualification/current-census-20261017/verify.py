#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed M3 interop installed-source successor; never relax the historical recipe.

Historical five-file postimages remain immutable. This receipt admits exactly the
current seventy-six first-party source/test owners and their SHA-256 and Git blobs.
"""
import hashlib
from pathlib import Path
import subprocess

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[4] if len(HERE.parents) > 4 else Path.cwd()
CENSUS = HERE / 'current-source-census.tsv'
LEGACY = ROOT / 'm3/collections/interop/recipe/src/main/resources/jdk/internal/m3/rewrite/hash-pinned-java/collections-library-interop/manifest.tsv'
RECEIPT_SHA256 = '8d1dd8c409d6977e25f360455416d98ef89c73a4e4a011b3fee83c90fda2b936'
LEGACY_GIT_BLOB = 'caf83a853a4bd6d80120140ff59a3e527ee233fa'
BASE = 'm3/collections/interop/src/'
MAIN = BASE + 'main/java/jdk/m3/collections/interop/'
TEST = BASE + 'test/java/jdk/m3/collections/interop/'
ALLOWED_PATHS = tuple(sorted([
    MAIN + name for name in (
        'M3ApacheBag.java',
        'M3ApacheBidiMap.java',
        'M3ApacheCollections.java',
        'M3ApacheListValuedMap.java',
        'M3ApacheMultiSet.java',
        'M3ApacheSetValuedMap.java',
        'M3BiMap.java',
        'M3BiMaps.java',
        'M3EclipseBag.java',
        'M3EclipseBagMultimap.java',
        'M3EclipseListMultimap.java',
        'M3EclipseSetMultimap.java',
        'M3FiveFamilyPreparedSelection.java',
        'M3FiveLibraryIndexViews.java',
        'M3FiveLibraryKeyRanges.java',
        'M3FiveLibraryPreparedRows.java',
        'M3FiveListFamilies.java',
        'M3FiveProviderFlagSelection.java',
        'M3FiveProviderKeyGroups.java',
        'M3GuavaBiMap.java',
        'M3GuavaClassToInstanceMaps.java',
        'M3GuavaListMultimap.java',
        'M3GuavaMultiset.java',
        'M3GuavaRowSortedTable.java',
        'M3GuavaSetMultimap.java',
        'M3LibraryBinding.java',
        'M3LibraryBindings.java',
        'M3LibraryCollections.java',
        'M3LibraryCollectors.java',
        'M3SpringArrayMultiValueMap.java',
        'M3SpringCaseInsensitiveMap.java',
        'M3SpringMultiValueMap.java',
    )
] + [
    TEST + name for name in (
        'M3ApacheBagContractTest.java',
        'M3ApacheBidiMapContractTest.java',
        'M3ApacheCollectionsTest.java',
        'M3BiMapTest.java',
        'M3CountedProviderContractTest.java',
        'M3EclipseBagMultimapSelfTest.java',
        'M3EclipseBagMultimapTest.java',
        'M3EclipseListMultimapSelfTest.java',
        'M3EclipseMultimapTest.java',
        'M3EclipseSetMultimapSelfTest.java',
        'M3FiveFamilyPreparedSelectionSelfTest.java',
        'M3FiveFamilyPreparedSelectionTest.java',
        'M3FiveLibraryArrayificationTest.java',
        'M3FiveLibraryExpandedContractsTest.java',
        'M3FiveLibraryIndexViewsTest.java',
        'M3FiveLibraryInteropContractTest.java',
        'M3FiveLibraryKeyRangesTest.java',
        'M3FiveLibraryPreparedRowsTest.java',
        'M3FiveLibraryState32Test.java',
        'M3FiveListFamiliesContract.java',
        'M3FiveListFamiliesTest.java',
        'M3FiveProviderBagContractTest.java',
        'M3FiveProviderCountSharingRegressionTest.java',
        'M3FiveProviderFlagSelectionTest.java',
        'M3FiveProviderKeyGroupClosureTest.java',
        'M3FiveProviderKeyGroupsTest.java',
        'M3GuavaBiMapContractTest.java',
        'M3GuavaBiMapSelfTest.java',
        'M3GuavaClassToInstanceMapsSelfTest.java',
        'M3GuavaClassToInstanceMapsTest.java',
        'M3GuavaListMultimapContract.java',
        'M3GuavaListMultimapRealTest.java',
        'M3GuavaRowSortedTableContract.java',
        'M3GuavaRowSortedTableTest.java',
        'M3GuavaSetMultimapTest.java',
        'M3LibraryBindingsSelfTest.java',
        'M3LibraryClosureTest.java',
        'M3LibraryCollectionsTest.java',
        'M3ObjectStreamProviderContractTest.java',
        'M3SdkFamiliesTest.java',
        'M3SpringArrayContractTest.java',
        'M3SpringCaseInsensitiveContract.java',
        'M3SpringCaseInsensitiveSdkFactoryTest.java',
        'M3SpringCaseInsensitiveTest.java',
    )
]))

# Historical five-file manifest stays immutable. Only this exact independently
# recorded test successor may depart from its historical postimage.
HISTORICAL_SUCCESSORS = {
    TEST + 'M3LibraryClosureTest.java': (
        '5ece9a369bcf5a223cb163ed0857534600fcd53bcc881f239956be15a4200f40', '29691e36f212ae79c31a06a597a58e76c7e51280787c46db6d7264b30fd567e0'),
    TEST + 'M3LibraryCollectionsTest.java': (
        'db377aa16cf2de5a8f49ef273f9b5fc7fb0f1a35ac247339f0556cea086d2d04', 'f3d64d981272b45b0866d307be3ce66bdf88bc7ade581a3497f28c50e19febe5'),
}
PREVIOUS_CENSUS = HERE / 'previous-source-census-20261021.tsv'
PREVIOUS_SHA256 = 'b0f9ec966623afa228a9122b112745acbffe2391142a64b8fae130a6810d60a2'
SUCCESSOR_LEDGER = HERE / 'source-successor-ledger-20261021.tsv'
SUCCESSOR_SHA256 = '108a05334a1fdb5e10dd07bd635400cadb5a27e94c90cb380b9f4c02bd14c3ef'
EPOCH_76 = HERE / 'previous-source-census-76-20261021.tsv'
EPOCH_76_SHA256 = '4301e88f6e9016fb61bb2ad6392ec79bb8c02d9f0d0d7e29f8e9776dc0b616a5'
EPOCH_76_DELTA = HERE / 'source-epoch-76-delta-20261021.tsv'
EPOCH_76_DELTA_SHA256 = '747edf8eaad5a3470b269fda6e30c6dedf155673fc87b4a0b0a0c5295281ea07'
EPOCH_76_2 = HERE / 'previous-source-census-76-epoch2-20261021.tsv'
EPOCH_76_2_SHA256 = 'cee770deadfc05337c3004e02c9fa015d64947f71a9ef94906e413f415e38b54'
EPOCH_76_3_DELTA = HERE / 'source-epoch-76-epoch3-delta-20261021.tsv'
EPOCH_76_3_DELTA_SHA256 = 'bc19dd11e3a26efd46dd000158d9ae486505d0029608e29341335f2d60736a4a'
EPOCH_76_3 = HERE / 'previous-source-census-76-epoch3-20261021.tsv'
EPOCH_76_3_SHA256 = '4a3eb76fa892999da4e435a6aaddd4eff13e48fc6f50d2c39008272e4afb6c22'
EPOCH_76_4_DELTA = HERE / 'source-epoch-76-epoch4-delta-20261021.tsv'
EPOCH_76_4_DELTA_SHA256 = 'd94389c12a2f089981dbcb797340541869a8c2b40a7459f16d86f5be4bf5641b'



def sha256(data):
    return hashlib.sha256(data).hexdigest()

def git_blob(data):
    return hashlib.sha1(b'blob ' + str(len(data)).encode('ascii') + b'\x00' + data).hexdigest()

def rows(path, width):
    if path.is_symlink() or not path.is_file():
        raise ValueError('source seal missing or aliased: ' + str(path))
    result = []
    for line in path.read_text(encoding='utf-8').splitlines():
        if not line or line.startswith('#'):
            continue
        parts = line.split('\t')
        if len(parts) != width:
            raise ValueError('malformed sealed source row: ' + line[:160])
        result.append(parts)
    if len({r[0] for r in result}) != len(result):
        raise ValueError('duplicate source path in ' + str(path))
    return result


def verify_successor_lineage(current, previous, successors, expected_added=40, expected_changed=7):
    """Preserve the previous complete receipt and require exact source successors."""
    prior = {row[0]: row for row in previous}
    present = {row[0]: row for row in current}
    if len(present) != len(prior) + expected_added or not set(prior).issubset(present):
        raise ValueError('current census does not preserve the predecessor source set')
    changed = {path for path, row in prior.items()
               if (row[2], row[3]) != (present[path][2], present[path][3])}
    ledger = {row[0]: row for row in successors}
    if len(changed) != expected_changed or len(ledger) != len(successors) or set(ledger) != changed:
        raise ValueError('unadmitted or missing source successor ledger')
    for path, prior_row in prior.items():
        current_row = present[path]
        if current_row[1] != prior_row[1]:
            raise ValueError('historical predecessor cell mutated: ' + path)
        if path in changed:
            expected = [path, prior_row[2], current_row[2], current_row[3], prior_row[3]]
            if list(ledger[path]) != expected:
                raise ValueError('changed source predecessor or successor not admitted: ' + path)
    for path, row in present.items():
        if path not in prior and row[1] != 'ABSENT':
            raise ValueError('unadmitted new source predecessor: ' + path)


def verify_current_epoch(current, predecessor, changes, expected_changes=1):
    """Admit only the exact changed Git blobs between two full sealed source epochs."""
    old = {row[0]: row for row in predecessor}
    now = {row[0]: row for row in current}
    delta = {row[0]: row for row in changes}
    if (len(old) != len(predecessor) or len(now) != len(current)
            or len(delta) != len(changes)):
        raise ValueError('duplicate row in exact source epoch')
    if set(old) != set(now):
        raise ValueError('source epoch added or lost an original file')
    touched = {path for path in old if tuple(old[path][2:4]) != tuple(now[path][2:4])}
    if len(touched) != expected_changes or touched != set(delta):
        raise ValueError('unadmitted source epoch change')
    for path, previous in old.items():
        current_row = now[path]
        if previous[1] != current_row[1]:
            raise ValueError('changed original predecessor field: ' + path)
        if path in touched:
            expected = [path, previous[2], current_row[2], current_row[3], previous[3]]
            if list(delta[path]) != expected:
                raise ValueError('source epoch successor hash or blob mismatch: ' + path)

def verify(root=ROOT, census=CENSUS, legacy=LEGACY,
           receipt_sha=RECEIPT_SHA256, legacy_blob=LEGACY_GIT_BLOB,
           check_tracked=True):
    root = Path(root)
    census = Path(census)
    legacy = Path(legacy)
    if census.is_symlink() or not census.is_file() or sha256(census.read_bytes()) != receipt_sha:
        raise ValueError('drifted, missing or aliased current-source receipt')
    if legacy.is_symlink() or not legacy.is_file() or git_blob(legacy.read_bytes()) != legacy_blob:
        raise ValueError('historical five-file receipt changed')
    previous = rows(legacy, 4)
    if len(previous) != 5:
        raise ValueError('original recipe is not the exact five-file historical owner')
    old = {path: after for path, before, after, template in previous}
    for path, before, after, template in previous:
        if before != 'ABSENT' or not template.endswith('.java.txt'):
            raise ValueError('historical receipt record corrupted: ' + path)
    current = rows(census, 4)
    if len(current) != len(ALLOWED_PATHS) or tuple(r[0] for r in current) != ALLOWED_PATHS:
        raise ValueError('current interop file census not exact/reordered')

    if check_tracked:
        for path, expected in ((PREVIOUS_CENSUS, PREVIOUS_SHA256),
                               (SUCCESSOR_LEDGER, SUCCESSOR_SHA256),
                               (EPOCH_76, EPOCH_76_SHA256),
                               (EPOCH_76_DELTA, EPOCH_76_DELTA_SHA256),
                               (EPOCH_76_2, EPOCH_76_2_SHA256),
                               (EPOCH_76_3_DELTA, EPOCH_76_3_DELTA_SHA256),
                               (EPOCH_76_3, EPOCH_76_3_SHA256),
                               (EPOCH_76_4_DELTA, EPOCH_76_4_DELTA_SHA256)):
            if path.is_symlink() or not path.is_file() or sha256(path.read_bytes()) != expected:
                raise ValueError('missing or drifted source-epoch receipt: ' + str(path))
        epoch1 = rows(EPOCH_76, 4)
        epoch2 = rows(EPOCH_76_2, 4)
        verify_successor_lineage(epoch1, rows(PREVIOUS_CENSUS, 4),
                                 rows(SUCCESSOR_LEDGER, 5))
        verify_current_epoch(epoch2, epoch1, rows(EPOCH_76_DELTA, 5),
                             expected_changes=1)
        epoch3 = rows(EPOCH_76_3, 4)
        verify_current_epoch(epoch3, epoch2, rows(EPOCH_76_3_DELTA, 5),
                             expected_changes=3)
        verify_current_epoch(current, epoch3, rows(EPOCH_76_4_DELTA, 5),
                             expected_changes=7)
    if check_tracked:
        tracked = set(subprocess.check_output(
            ['git', 'ls-files', '--', BASE + 'main', BASE + 'test'],
            cwd=root, text=True).splitlines())
        if tracked != set(ALLOWED_PATHS):
            raise ValueError('installed interop files missing or not admitted: ' +
                             repr(sorted(tracked ^ set(ALLOWED_PATHS))))
    report = []
    for path, before, after, blob in current:
        if ((before != old[path] if path in old else before != 'ABSENT')
                or len(after) != 64 or any(v not in '0123456789abcdef' for v in after)
                or len(blob) != 40 or any(v not in '0123456789abcdef' for v in blob)):
            raise ValueError('invalid exact predecessor/current identity: ' + path)
        if path in old and path != MAIN + 'M3LibraryCollections.java' and after != old[path]:
            if HISTORICAL_SUCCESSORS.get(path) != (old[path], after):
                raise ValueError('changed unapproved historical implementation: ' + path)
        source = root / path
        if source.is_symlink() or not source.is_file():
            raise ValueError('missing or aliased current source: ' + path)
        data = source.read_bytes()
        if sha256(data) != after or git_blob(data) != blob:
            raise ValueError('current interop source drift: ' + path)
        report.append((path, after))
    return report

if __name__ == '__main__':
    matched = verify()
    for path, sha in matched:
        print('SEALED', path, sha)
    print('PASS exact current interop successor', len(matched))
