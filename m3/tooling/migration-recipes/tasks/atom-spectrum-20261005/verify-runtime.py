#!/usr/bin/env python3
"""Audit the completed focused proof receipts; this does not run or replace the gates."""
import argparse
from collections import Counter, defaultdict
import csv
import hashlib
import itertools
import json
from pathlib import Path
import xml.etree.ElementTree as ET


def rows(path):
    with path.open(encoding='utf-8', newline='') as stream:
        return list(csv.DictReader((line for line in stream if not line.startswith('#')), delimiter='\t'))


def require(condition, message):
    if not condition:
        raise SystemExit(message)


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--static-receipt', type=Path, required=True)
    parser.add_argument('--corpus-root', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    task = Path(__file__).resolve().parent
    owner = task.parent.parent
    repo = owner.parent.parent.parent
    target = task / 'target'
    for row in rows(args.static_receipt):
        require(sha(repo / row['path']) == row['sha256'], 'SOURCE_CHANGED_AFTER_STATIC:' + row['path'])
    for row in rows(task / 'SOURCE_PINS.tsv'):
        require(sha(repo / row['path']) == row['after_sha256'], 'SOURCE_PIN_DRIFT:' + row['path'])

    expected_suites = {
        'M3AtomCoverageClosureTest': 7,
        'M3AtomDefensiveBranchTest': 4,
        'M3PatternMarkerTest': 2,
        'M3InventoryPureIntAtomCandidatesTest': 3,
        'M3AtomCommentRecipeTest': 2,
        'M3PureIntConvergenceRecipeTest': 4,
        'M3IntRecipeProjectTest': 6,
        'M3RepositoryCorpusTest': 3,
        'M3IntSpectrumTest': 4,
        'M3AtomizePureIntReturnRecipeTest': 4,
    }
    suites = {}
    for path in sorted((target / 'surefire-reports').glob('TEST-*.xml')):
        suite = ET.parse(path).getroot()
        name = suite.attrib['name'].rsplit('.', 1)[-1]
        require(name not in suites, 'DUPLICATE_SUITE:' + name)
        suites[name] = int(suite.attrib['tests'])
        require(all(int(suite.attrib[field]) == 0 for field in ('failures', 'errors', 'skipped')),
                'JUNIT_NOT_GREEN:' + name)
        require(len(suite.findall('testcase')) == suites[name], 'JUNIT_COUNT_DRIFT:' + name)
    require(suites == expected_suites, 'JUNIT_SUITE_DENOMINATOR_DRIFT')

    count = rows(target / 'm3-spectrum/COUNTS.tsv')
    require(len(count) == 1, 'SPECTRUM_COUNT_ROW')
    count = count[0]
    require(tuple(int(count[name]) for name in ('expressions', 'input_pairs', 'orders', 'intermediate_checks'))
            == (324, 225, 6, 45), 'SPECTRUM_DENOMINATOR_DRIFT')
    require(count['original_sha256'] != count['canonical_sha256'], 'ALL_NOOP_POSITIVE_CORPUS')
    permutations = rows(target / 'm3-spectrum/PERMUTATIONS.tsv')
    require(len(permutations) == 45, 'INTERMEDIATE_DENOMINATOR_DRIFT')
    by_order = defaultdict(list)
    for row in permutations:
        require(row['proof'] == 'compiler_runtime_surface', 'MISSING_INTERMEDIATE_PROOF')
        by_order[tuple(json.loads(row['order']))].append(row)
    require(set(by_order) == set(itertools.permutations(range(3))), 'RECIPE_ORDER_COVERAGE')
    sweeps = {}
    for order, sequence in by_order.items():
        require(len(sequence) % 3 == 0 and 2 <= len(sequence) // 3 <= 4, 'PASS_BOUND')
        previous = count['original_sha256']
        for index, row in enumerate(sequence):
            require(int(row['sweep']) == index // 3 and int(row['leaf']) == order[index % 3],
                    'RECIPE_ORDER_SEQUENCE')
            require(row['before_sha256'] == previous, 'CANDIDATE_CHAIN_DRIFT')
            previous = row['after_sha256']
        require(previous == count['canonical_sha256'], 'NORMAL_FORM_DIVERGENCE')
        require(all(row['before_sha256'] == row['after_sha256'] for row in sequence[-3:]),
                'ZERO_CHANGE_SWEEP_REQUIRED')
        sweeps[str(list(order))] = len(sequence) // 3

    manifest = owner / 'src/test/resources/com/m3/rewrite/atom/spectrum/CORPUS.tsv'
    pins = {row['path']: row for row in rows(manifest)}
    require(len(pins) == 100, 'REAL_CORPUS_DENOMINATOR')
    for name, pin in pins.items():
        data = (args.corpus_root / name).read_bytes()
        require(len(data) == int(pin['bytes']) and hashlib.sha256(data).hexdigest() == pin['sha256'],
                'CORPUS_CHANGED_AFTER_TEST:' + name)
        blob = hashlib.sha1(b'blob ' + str(len(data)).encode('ascii') + b'\0' + data).hexdigest()
        require(blob == pin['git_blob_sha1'], 'CORPUS_GIT_BLOB_DRIFT:' + name)
    corpus = rows(target / 'm3-spectrum/CORPUS.tsv')
    require(len(corpus) == 100 and {row['path'] for row in corpus} == set(pins), 'CORPUS_REPORT_COVERAGE')
    statuses = Counter()
    for row in corpus:
        require(row['category'] == pins[row['path']]['category'], 'CORPUS_CATEGORY_DRIFT')
        require(row['before_sha256'] == pins[row['path']]['sha256'], 'CORPUS_RECEIPT_PIN_DRIFT')
        require(row['status'] in {'NOOP_FIXED_POINT', 'PARSE_REFUSED', 'TYPE_REFUSED'},
                'NEW_REAL_CANDIDATE_REQUIRES_REVIEW')
        require(row['after_sha256'] == row['before_sha256'], 'UNEXPECTED_REAL_SOURCE_CHANGE')
        statuses[row['status']] += 1
    require(dict(statuses) == {row['status']: int(row['files'])
                              for row in rows(target / 'm3-spectrum/CORPUS_COUNTS.tsv')},
            'CORPUS_SUMMARY_DRIFT')
    group = rows(target / 'm3-spectrum/M3_GROUP.tsv')
    require(len(group) == 1 and tuple(int(group[0][name]) for name in
            ('source_units', 'strict_type_admitted', 'compiled', 'utf16_cases')) == (25, 25, 25, 515),
            'M3_GROUP_DENOMINATOR_DRIFT')

    materialized = target / 'materialized/atom-pattern-spectrum'
    materialization = rows(materialized / 'MATERIALIZATION.tsv')
    require(len(materialization) == 2, 'RECIPE_OWNER_DENOMINATOR')
    crate = owner / 'src/main/resources/com/synexia/rewrite/hash-pinned-java/atom-pattern-spectrum'
    targets = {}
    for line in (crate / 'manifest.tsv').read_text(encoding='utf-8').splitlines():
        path, before, after, filename = line.split('\t')
        require(path not in targets, 'DUPLICATE_RECIPE_OWNER')
        require(sha(crate / filename) == after and sha(owner / path) == after, 'RECIPE_OWNER_RESOURCE_DRIFT')
        targets[path] = (before, after)
    require({row['path'] for row in materialization} == set(targets), 'MATERIALIZATION_OWNER_SET_DRIFT')
    for row in materialization:
        require((row['before_sha256'], row['after_sha256']) == targets[row['path']], 'MATERIALIZATION_PIN_DRIFT')
        require(sha(owner / row['path']) == row['after_sha256'], 'MATERIALIZED_OWNER_DRIFT')
        require(sha(materialized / Path(row['path']).name) == row['after_sha256'],
                'MATERIALIZED_OUTPUT_DRIFT')

    result = {
        'status': 'FOCUSED_RECEIPTS_AUDITED',
        'junit_tests': sum(suites.values()),
        'junit_failures_errors_skips': 0,
        'generated_expressions': 324,
        'input_pairs': 225,
        'recipe_orders': 6,
        'intermediate_checks': 45,
        'arithmetic_result_comparisons_per_oracle_across_intermediates': 324 * 225 * 45,
        'zero_change_sweeps': sweeps,
        'real_corpus_files': 100,
        'real_corpus_bytes': sum(int(pin['bytes']) for pin in pins.values()),
        'single_file_statuses': dict(sorted(statuses.items())),
        'real_m3_source_group': {'files': 25, 'strict_type_admitted': 25, 'compiled': 25, 'utf16_cases': 515},
        'source_snapshot_owners': 2,
        'full_default_module_gate': 'NOT_EXECUTED',
        'jdk_configure_make_jtreg': 'NOT_EXECUTED',
        'jni_native_runtime': 'NOT_EXECUTED',
    }
    args.out.mkdir(parents=True, exist_ok=True)
    (args.out / 'RUNTIME_AUDIT.json').write_text(json.dumps(result, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, sort_keys=True))


if __name__ == '__main__':
    main()
