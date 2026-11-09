#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Freeze actual qualified parser/provider classes and JARs for recipe attribution."""
from pathlib import Path
import hashlib
import json
import os

ROOT = Path(__file__).resolve().parents[2]
OUT = Path(__file__).resolve().parent / 'parser-dependencies-001'
SOURCE_CLASSES = ROOT / 'maven-work/candidate-classes'
ARTIFACTS = ROOT / 'maven-work/evidence/PROVIDER_ARTIFACTS.json'
CLASSPATH = ROOT / 'maven-work/evidence/PROVIDER_CLASSPATH.txt'
PROOF = ROOT / 'maven-work/evidence/REAL_PROVIDER_003_COMPARISON.json'
PRODUCTION = ROOT / 'maven-work/candidates/m3/tooling/module-packs/parser/src/main/java/jdk/m3/parser/M3ASTAuthoritativeModels.java'


def digest(data):
    return hashlib.sha256(data).hexdigest()


proof = json.loads(PROOF.read_text())
assert proof['status'] == 'PASS_CANDIDATE_TEMPLATES'
assert proof['baseline_checks'] == 13 and proof['candidate_checks'] == 102
assert proof['incumbent_shapes_equal']
assert digest(PRODUCTION.read_bytes()) == '2814f6f292978b421cec2914cc48a96384cc3e926377b138559e287a005854c3'
artifacts = json.loads(ARTIFACTS.read_text())
ordered = CLASSPATH.read_text().strip().split(os.pathsep)
assert ordered == [entry['path'] for entry in artifacts]
assert len(ordered) == len(set(ordered)) == 90
assert sum(entry['origin'] == 'source-built pinned Maven4 fork' for entry in artifacts) == 16
class_files = sorted(SOURCE_CLASSES.rglob('*'))
class_files = [path for path in class_files if path.is_file()]
assert any(path.name == 'M3ASTAuthoritativeModels.class' for path in class_files)
prepared = []
for source in class_files:
    assert not source.is_symlink()
    relative = Path('classes') / source.relative_to(SOURCE_CLASSES)
    data = source.read_bytes()
    prepared.append((source, relative, data, 'qualified parser candidate classes'))
for index, entry in enumerate(artifacts):
    source = Path(entry['path'])
    assert source.is_file() and not source.is_symlink()
    data = source.read_bytes()
    assert len(data) == entry['bytes'] and digest(data) == entry['sha256'], source
    prepared.append((source, Path('jars') / (f'{index:03d}-' + source.name), data, entry['origin']))
OUT.mkdir(exist_ok=False)
records = []
for source, relative, data, origin in prepared:
    destination = OUT / relative
    destination.parent.mkdir(parents=True, exist_ok=True)
    with destination.open('xb') as stream:
        stream.write(data)
    assert destination.read_bytes() == data
    assert source.read_bytes() == data, 'source changed during snapshot: ' + str(source)
    records.append({'source_path': str(source), 'frozen_path': str(destination),
        'sha256': digest(data), 'bytes': len(data), 'origin': origin})
classpath = [OUT / 'classes'] + [OUT / 'jars' / (f'{index:03d}-' + Path(entry['path']).name)
                                for index, entry in enumerate(artifacts)]
inventory = OUT / 'PARSER_CLASSPATH.txt'
inventory.write_text(''.join(str(path) + '\n' for path in classpath))
receipt = {'schema': 'M3_MAVEN4_RECIPE_PARSER_DEPENDENCIES_V1',
    'scope': 'Actual qualified candidate classpath; compiler-attribution inputs only, no source-vendored reactor claim',
    'candidate_source_sha256': digest(PRODUCTION.read_bytes()),
    'provider_comparison_receipt': str(PROOF),
    'provider_comparison_sha256': digest(PROOF.read_bytes()),
    'provider_artifacts_sha256': digest(ARTIFACTS.read_bytes()),
    'original_colon_joined_classpath_sha256': digest(CLASSPATH.read_bytes()),
    'format': 'One existing compiled directory or JAR per line; qualifier joins with platform path separator',
    'parser_classpath_sha256': digest(inventory.read_bytes()),
    'classpath_members': len(classpath), 'jar_members': len(artifacts),
    'source_built_maven4_jars': 16, 'compiled_class_files': len(class_files),
    'files': records, 'all_source_and_copy_readbacks_match': True}
(OUT / 'RESULT.json').write_text(json.dumps(receipt, indent=2) + '\n')
print(json.dumps({key: receipt[key] for key in ('classpath_members', 'jar_members',
    'source_built_maven4_jars', 'compiled_class_files', 'all_source_and_copy_readbacks_match')}))
