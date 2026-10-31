# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Refuse missing, drifted and foreign generated Java source postimages."""
from pathlib import Path
import hashlib
import sys

CRATE = 'm3jdk21-five-provider-key-groups-20261018'
HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[4]
PACK = HERE/'src/main/resources/jdk/internal/m3/rewrite/hash-pinned-java'/CRATE
REVIEWED_SUCCESSOR = HERE / 'REVIEWED_SUCCESSOR_20261029.tsv'
HISTORICAL_MANIFEST_BLOB = '1344d07379d7d67fbe6f54f90da0f8140b86f460'
GROUP_OWNER = 'm3/collections/interop/src/main/java/jdk/m3/collections/interop/M3FiveProviderKeyGroups.java'
GROUP_TEST = 'm3/collections/interop/src/test/java/jdk/m3/collections/interop/M3FiveProviderKeyGroupsTest.java'
BEFORE_SHA = 'a7c4dd4eb8de5d12b08d80e81fcb0c0e94830fd1b242dc6f7adedc50d1fd059a'
CURRENT_SHA = '0e6314a8f73d66738956278d7b70022f4990b7e49fb4b469dea2fa003ca7e1e0'
CURRENT_BLOB = '749e8c105cc5d28ec658500af510be1dfcfbf332'
EXPECTED_SUCCESSOR = (
    GROUP_OWNER + '\t' + BEFORE_SHA + '\t' + CURRENT_SHA + '\t' + CURRENT_BLOB
    + '\thsoliwal/M3jdk21#1273\thsoliwal/com.synexia#12263'
    + '\tSOURCE_SUCCESSOR_ACCEPTED_CI_PENDING\n'
)

def sha(data): return hashlib.sha256(data).hexdigest()

def git_blob(data):
    return hashlib.sha1(b'blob ' + str(len(data)).encode('ascii') + b'\0' + data).hexdigest()

def verify_approved_successor():
    if REVIEWED_SUCCESSOR.is_symlink() or not REVIEWED_SUCCESSOR.is_file():
        raise ValueError('missing or aliased reviewed group-owner successor')
    if REVIEWED_SUCCESSOR.read_bytes() != EXPECTED_SUCCESSOR.encode('utf-8'):
        raise ValueError('unreviewed or malformed group-owner source transition')

def verify():
    historical = PACK/'manifest.tsv'
    if (historical.is_symlink() or not historical.is_file()
            or git_blob(historical.read_bytes()) != HISTORICAL_MANIFEST_BLOB):
        raise ValueError('immutable historical five-provider source manifest changed')
    entries = [line.split('\t') for line in historical.read_text().splitlines()
               if line and not line.startswith('#')]
    if len(entries) != 2 or any(len(row) != 4 for row in entries):
        raise ValueError('unexpected generated-source manifest shape')
    if {row[0] for row in entries} != {GROUP_OWNER, GROUP_TEST}:
        raise ValueError('unexpected first-party five-provider source identities')
    verify_approved_successor()
    seen=set()
    for name, before, after, template in entries:
        if before != 'ABSENT' or not name.startswith('m3/collections/interop/src/') or name in seen:
            raise ValueError('bad path, preimage or duplicate: '+name)
        seen.add(name)
        source=PACK/template
        if not source.is_file() or source.is_symlink() or sha(source.read_bytes())!=after:
            raise ValueError('canonical source template changed: '+name)
        target=ROOT/name
        if not target.is_file() or target.is_symlink():
            raise ValueError('missing or aliased generated Java: '+name)
        current=target.read_bytes()
        if name == GROUP_OWNER:
            if after != BEFORE_SHA or sha(current) != CURRENT_SHA or git_blob(current) != CURRENT_BLOB:
                raise ValueError('unreviewed or drifting evolved group-owner Java: '+name)
        elif current != source.read_bytes():
            raise ValueError('missing or drifting original generated Java: '+name)
    print('M3_FIVE_PROVIDER_GROUPS_SEALED_OUTPUTS_PASS',len(seen))

if __name__ == '__main__':
    try: verify()
    except Exception as exc:
        print('M3_FIVE_PROVIDER_GROUPS_SEAL_FAIL',repr(exc),file=sys.stderr)
        raise
