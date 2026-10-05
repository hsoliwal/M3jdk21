#!/usr/bin/env python3
"""Derive this exact registry union and a separately bound review packet.

This task-specific authoring recipe accepts four exact inputs. It never applies
product sources, retargets old qualification, or rewrites historical receipts.
The retained sealed installer owns application, fixed points and rollback.
"""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
from pathlib import Path


INPUTS = {
    'base': '67d40925b97a38c3c1a85f337806524ac590a540bd39647b3e4b0988f2ae3fbb',
    'master': '536af76dd7b7b7217707662c2f1437af4adaa53af898627b0884e0b4f4603ace',
    'head': '8e229715d6783906b1ee249bb2707cdb5e412146219be6037154927abac893ea',
    'packet': '109173cb43fd91e084415284083d0d0ff92a1039a0b21216b7336c0a2c3dcf2e',
}
BASE = 'ccb4ace7f7d79f97ae2d9600de55960504cc3c21'
MASTER = '8131f535300c005afd27443d0281ede11198522f'
MASTER_TREE = '8a87d42532c93117f88f8e49abc728f3e49a8118'
HEAD = 'a73202a02577d3e3242a390b962fd3e08b691b60'
HEAD_TREE = 'a3ddbb9adba0023fea585d46bfc0bb74eb58a199'
SOURCE = '99221b0f25a1b09c32855d76e295b816f632a46b'
DESCRIPTOR = 'synexia.counterpart.MIndexJvmDescriptor'
JCC = {'synexia.jcc-recipe-laboratory', 'synexia.jcc-java-jni-regression'}


def digest(raw):
    return hashlib.sha256(raw).hexdigest()


def render(value):
    return (json.dumps(value, indent=2, allow_nan=False) + '\n').encode('utf-8')


def require(condition, message):
    if not condition:
        raise ValueError(message)


def derive(base_bytes, master_bytes, head_bytes, packet_bytes):
    raw = dict(zip(INPUTS, (base_bytes, master_bytes, head_bytes, packet_bytes)))
    for name, content in raw.items():
        require(type(content) is bytes and digest(content) == INPUTS[name],
                'exact input drift: ' + name)
    documents = {name: json.loads(content.decode('utf-8')) for name, content in raw.items()}
    base, master, head, packet = (documents[name] for name in INPUTS)
    records = {}
    for name, count in (('base', 44), ('master', 46), ('head', 79)):
        rows = documents[name]['migration']['records']
        records[name] = {row['id']: row for row in rows}
        require(len(rows) == len(records[name]) == count, 'exact unique record inventory: ' + name)
        without = copy.deepcopy(documents[name])
        without['migration']['records'] = []
        baseline = copy.deepcopy(base)
        baseline['migration']['records'] = []
        require(without == baseline, 'non-record authority changed: ' + name)
    old, current, intake = (records[name] for name in ('base', 'master', 'head'))
    require(set(current) - set(old) == JCC and set(old) <= set(current), 'master additions differ')
    additions = [row for row in head['migration']['records'] if row['id'] not in old]
    require(len(additions) == 35 and all(row['id'].startswith('synexia.intake.') for row in additions),
            'exact intake additions required')
    require(not set(current).intersection(row['id'] for row in additions), 'ambiguous registry identity')
    require({key for key in old if old[key] != current[key]} == {DESCRIPTOR},
            'unexpected master record change')
    require({key for key in old if old[key] != intake[key]} == {DESCRIPTOR},
            'unexpected original intake record change')
    require(current[DESCRIPTOR]['status'] == intake[DESCRIPTOR]['status'] == 'implemented-unverified',
            'descriptor acceptance changed')
    require(current[DESCRIPTOR]['targets'][0]['sha256'] == intake[DESCRIPTOR]['targets'][0]['sha256']
            and current[DESCRIPTOR]['targets'][0]['git_blob_sha1'] == intake[DESCRIPTOR]['targets'][0]['git_blob_sha1'],
            'descriptor source bytes differ')
    require(all(current[key]['status'] == 'blocked' for key in JCC), 'JCC acceptance changed')
    union = copy.deepcopy(master)
    union['migration']['records'].extend(copy.deepcopy(additions))
    require(len(union['migration']['records']) == 81, 'union record count')
    union_bytes = render(union)
    require(packet['authority'] == {'path': 'm3/docs/name-mapping.json', 'sha256': INPUTS['head']},
            'original packet authority differs')
    require(packet['source']['commit'] == SOURCE and packet['target']['commit'] == BASE,
            'original qualification pins differ')
    mapped = [row['id'] for lane in packet['lanes'] for row in lane['mappings']]
    require(len(mapped) == len(set(mapped)) == 35 and set(mapped) == {row['id'] for row in additions},
            'original packet mapping membership differs')
    derived_packet = copy.deepcopy(packet)
    derived_packet['authority']['sha256'] = digest(union_bytes)
    old_authority = INPUTS['head'].encode('ascii')
    new_authority = digest(union_bytes).encode('ascii')
    require(packet_bytes.count(old_authority) == 1, 'authority digest must have one exact byte occurrence')
    packet_bytes_after = packet_bytes.replace(old_authority, new_authority, 1)
    require(json.loads(packet_bytes_after.decode('utf-8')) == derived_packet,
            'derived packet changed a field beyond its authority digest')
    require(packet_bytes_after.replace(new_authority, old_authority, 1) == packet_bytes,
            'derived packet changed bytes beyond its authority digest')
    report = {
        'schema': 'm3.registry-reconciliation/1',
        'state': 'DERIVED_NOT_APPLIED_OR_QUALIFIED',
        'commonBase': BASE,
        'integrationMaster': {'commit': MASTER, 'tree': MASTER_TREE},
        'publicationHead': {'commit': HEAD, 'tree': HEAD_TREE},
        'inputs': {name: {'sha256': digest(content), 'bytes': len(content)} for name, content in raw.items()},
        'outputs': {'registry': {'sha256': digest(union_bytes), 'bytes': len(union_bytes)},
                    'packet': {'sha256': digest(packet_bytes_after), 'bytes': len(packet_bytes_after)}},
        'masterRecordsPreserved': 46, 'intakeRecordsPreserved': 35, 'uniqueRecords': 81,
        'descriptorPolicy': 'Preserve the complete current master record without acceptance upgrade.',
        'packetDelta': ['authority.sha256'],
        'qualificationSource': packet['source'], 'qualificationTarget': packet['target'],
        'wholeSourceTreeCovered': False, 'dependencyClosureComplete': False,
        'runtimeAccepted': False, 'canonicalProductionApplied': False, 'productWrites': 0,
    }
    return {'union': union_bytes, 'packet': packet_bytes_after, 'report': report}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--inputs', type=Path, default=Path(__file__).resolve().parent / 'inputs')
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    require(not args.output.exists(), 'derivation destination already exists')
    result = derive(*(args.inputs.joinpath(name + '.json').read_bytes() for name in INPUTS))
    args.output.mkdir(parents=False)
    (args.output / 'name-mapping.json').write_bytes(result['union'])
    (args.output / 'packet.json').write_bytes(result['packet'])
    (args.output / 'derivation.json').write_bytes(render(result['report']))
    print(json.dumps(result['report']['outputs'], sort_keys=True))


if __name__ == '__main__':
    main()
