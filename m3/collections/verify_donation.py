#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
# Modified 2026 by Hitesh Soliwal and contributors: qualify concurrent EnumSet in a new exact inventory; preserve all acceptance guards and historical receipts.
# Modified 2026 by Hitesh Soliwal and contributors: restore exact collection receiving provenance and verification while retaining current owners and String phase authority.
# Modified 2026 by Hitesh Soliwal and contributors: restore the Tools4j predecessor selector and seal the superset source set as an exact additive successor after the Tools4j refinement; historical receipts stay unchanged.
# Modified 2026 by Hitesh Soliwal and contributors: seal the master source set (2026-10-31) as a transition-receipt successor after the superset seal, through one shared transition atom.
"""Verify the installed M3 collection donation with target-native Java/JNI checks.

This host verifies product source. The canonical transformation and SDK replay remain
in Synexia. The earlier verify.py remains the frozen primitive-lane installer.
"""
import argparse
import ast
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
import zipfile
from m3_object_state_offset_receipt import include_successor as include_object_state_offset_successor

HERE = Path(__file__).resolve().parent
REPO = HERE.parents[1]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


# The module move of 2026-11-08 (MOVE_TO_JDK_MODULE v3, m3/tooling/jdk-naming/move_module_area.py) gives each recorded
# custody path a new home; recorded paths stay the keys of the hash-pinned manifests. Specific entries come first.
MOVED_PREFIXES = (
    ('m3/collections/src/test/java/MaskWindowConvergence.java',
     'test/jdk/jdk/m3/collections/MaskWindowConvergence.java'),
    ('m3/collections/src/main/java/jdk/', 'src/jdk.m3.collections/share/classes/jdk/'),
    ('m3/collections/src/test/java/jdk/', 'test/jdk/jdk/'),
    ('m3/collections/src/main/native/', 'src/jdk.m3.collections/share/native/libm3collections/'),
    ('m3/collections/src/main/c/', 'src/jdk.m3.collections/share/native/libm3collections/'),
    ('m3/collections/src/test/native/', 'test/jdk/jdk/m3/collections/native/'),
    ('m3/collections/src/main/resources/', 'src/jdk.m3.collections/share/classes/'),
    ('m3/collections/src/test/resources/', 'test/jdk/jdk/m3/collections/resources/'),
)
MOVED_JAVA_ROOTS = ('src/jdk.m3.collections/share/classes/jdk/m3/collections',
                    'src/jdk.m3.collections/share/classes/jdk/internal/m3/collections',
                    'test/jdk/jdk/m3/collections', 'test/jdk/jdk/internal/m3/collections')


def located(path):
    """The file of a recorded custody path, at its home after the module move."""
    for old, new in MOVED_PREFIXES:
        if path.startswith(old):
            return REPO / (new + path[len(old):])
    return REPO / path


def recorded(path):
    """The recorded custody key of a moved Java file (inverse of located)."""
    relative = path.relative_to(REPO).as_posix()
    for old, new in MOVED_PREFIXES:
        if relative.startswith(new):
            return old + relative[len(new):]
    return relative


def unique_json_object(pairs):
    """Reject duplicate metadata keys before historical fields can shadow live pins."""
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError('Duplicate donation metadata key: ' + key)
        result[key] = value
    return result


def read_donation_packet(path=HERE / 'm3-donation.json'):
    """Read all nested objects without silently accepting duplicate JSON keys."""
    return json.loads(path.read_text(encoding='utf-8'), object_pairs_hook=unique_json_object)



# The next exact successor applies the validated stored-representative atom
# without rewriting the six original filtered/sorted receiver source receipts.
_REPRESENTATIVE_RECEIPT = HERE / 'recipe/stored-representative-20261017/manifest.tsv'
_REPRESENTATIVE_SHA256 = '4558799d12aa2ac5dd8789af591beb8789da632c93ad9020681062d10e664931'
_REPRESENTATIVE_PATHS = (
    'm3/collections/src/main/java/jdk/m3/collections/M3FilteredViews.java',
    'm3/collections/src/main/java/jdk/m3/collections/M3MutableFilteredViews.java',
    'm3/collections/src/test/java/jdk/m3/collections/M3StoredRepresentativeContract.java',
    'm3/collections/src/test/java/jdk/m3/collections/M3StoredRepresentativeTest.java',
)


def include_stored_representative_successor(previous_rows,
        receipt=_REPRESENTATIVE_RECEIPT):
    """Source-sealed additive correction for exactly two owners and two tests.

    Verify the full previous receipt first, then only replace those two
    exact old digests and append the two new Java21 test owners. No historical
    manifest rewriting, unknown source admission or permissive wildcards.
    """
    if not receipt.exists() and not receipt.is_symlink():
        return list(previous_rows)
    if receipt.is_symlink() or not receipt.is_file() or digest(receipt) != _REPRESENTATIVE_SHA256:
        raise SystemExit('Stored-representative successor missing, aliased or drifted')
    entries = [line.split('\t') for line in receipt.read_text(encoding='utf-8').splitlines()]
    if len(entries) != len(_REPRESENTATIVE_PATHS):
        raise SystemExit('Stored-representative successor must declare exactly four paths')
    current = {path: expected for path, expected in previous_rows}
    if len(current) != len(previous_rows):
        raise SystemExit('Duplicate prior stored-representative source path')
    extras = []
    for cells, expected_path in zip(entries, _REPRESENTATIVE_PATHS):
        if len(cells) != 4:
            raise SystemExit('Malformed stored-representative successor')
        path, before, after, template = cells
        if (path != expected_path or len(after) != 64
                or any(c not in '0123456789abcdef' for c in after)
                or template != path.rsplit('/', 1)[1] + '.after'):
            raise SystemExit('Unapproved stored-representative source: ' + path)
        if before == 'ABSENT':
            if path in current:
                raise SystemExit('Occupied new stored-representative source: ' + path)
            extras.append((path, after))
        else:
            if current.get(path) != before:
                raise SystemExit('Stored-representative source preimage drift: ' + path)
            current[path] = after
    return [(path, current[path]) for path, _ in previous_rows] + extras


# The historical installed-source receipt remains immutable. This optional
# additive overlay is authored by the Synexia OpenRewrite collection receiver.
_FILTERED_VIEW_RECEIPT = HERE / 'recipe/filtered-sorted-views-20261017/manifest.tsv'
_FILTERED_VIEW_SHA256 = 'e6e70eb2d1ec267edb8bf6647881d157453350702f32f36828c13e82c178454b'
_FILTERED_VIEW_PATHS = (
    'm3/collections/src/main/java/jdk/m3/collections/M3FilteredViews.java',
    'm3/collections/src/main/java/jdk/m3/collections/M3MutableFilteredViews.java',
    'm3/collections/src/test/java/jdk/m3/collections/M3FilteredViewsContract.java',
    'm3/collections/src/test/java/jdk/m3/collections/M3FilteredViewsTest.java',
    'm3/collections/src/test/java/jdk/m3/collections/M3MutableFilteredViewsContract.java',
    'm3/collections/src/test/java/jdk/m3/collections/M3MutableFilteredViewsTest.java',
)


def include_filtered_view_successor(historical_rows, receipt=_FILTERED_VIEW_RECEIPT):
    """Admit only the exact six source-sealed, additive receiver files.

    Missing overlay preserves historical behavior; unlisted Java files still fail
    below. An invalid/aliased overlay is never treated as silently absent.
    """
    if not receipt.exists() and not receipt.is_symlink():
        return list(historical_rows)
    if receipt.is_symlink() or not receipt.is_file() or digest(receipt) != _FILTERED_VIEW_SHA256:
        raise SystemExit('Filtered-view successor manifest missing, aliased or drifted')
    entries = [line.split('\t') for line in receipt.read_text(encoding='utf-8').splitlines()]
    if len(entries) != len(_FILTERED_VIEW_PATHS):
        raise SystemExit('Filtered-view successor must declare exactly six paths')
    extra = []
    for entry, expected_path in zip(entries, _FILTERED_VIEW_PATHS):
        if len(entry) != 4:
            raise SystemExit('Malformed filtered-view successor row')
        path, before, after, template = entry
        if (path != expected_path or before != 'ABSENT'
                or len(after) != 64 or any(c not in '0123456789abcdef' for c in after)
                or template != path.rsplit('/', 1)[1] + '.after'):
            raise SystemExit('Unapproved filtered-view receiver source: ' + path)
        extra.append((path, after))
    return [*historical_rows, *extra]


# Additive M3IndexPathMasks receiving overlay. The pre-existing source manifest
# and filtered/sorted receiver seal are immutable; this successor is exact and opt-in.
_PATH_MASK_RECEIPT = HERE / 'recipe/index-path-masks-20261017/manifest.tsv'
_PATH_MASK_SHA256 = 'c3af2d443d0c79ee375f6ac0399a874fd5662123e4ef1af52b358c2a6f8ea28f'
_PATH_MASK_PATHS = (
    'm3/collections/src/main/java/jdk/m3/collections/M3IndexPathMasks.java',
    'm3/collections/src/main/native/collections/m3_index_path_masks.c',
    'm3/collections/src/test/java/jdk/m3/collections/M3IndexPathMasksSelfTest.java',
    'm3/collections/src/test/java/jdk/m3/collections/M3IndexPathMasksTest.java',
)


def include_path_mask_successor(historical_rows, receipt=_PATH_MASK_RECEIPT):
    """Admit only the four SHA-256 sealed Java/JNI path-mask receiver sources."""
    if not receipt.exists() and not receipt.is_symlink():
        return list(historical_rows)
    if receipt.is_symlink() or not receipt.is_file() or digest(receipt) != _PATH_MASK_SHA256:
        raise SystemExit('Index-path-mask successor manifest missing, aliased or drifted')
    entries = [line.split('\t') for line in receipt.read_text(encoding='utf-8').splitlines()]
    if len(entries) != len(_PATH_MASK_PATHS):
        raise SystemExit('Index-path-mask successor must declare exactly four paths')
    extra = []
    for entry, expected_path in zip(entries, _PATH_MASK_PATHS):
        if len(entry) != 4:
            raise SystemExit('Malformed index-path-mask successor row')
        path, before, after, template = entry
        if (path != expected_path or before != 'ABSENT'
                or len(after) != 64 or any(c not in '0123456789abcdef' for c in after)
                or template != path.rsplit('/', 1)[1] + '.after'):
            raise SystemExit('Unapproved index-path-mask receiver source: ' + path)
        extra.append((path, after))
    return [*historical_rows, *extra]

# Exact successor of the four unchanged historical receiving receipts.
_TOOLS4J_RECEIPT = HERE / 'qualification/tools4j-unix-source-successor-20261019.tsv'
_TOOLS4J_RECEIPT_SHA256 = 'bf7c68162c99c6066446f2ff97e7fce4700cdd3370aebd8b814730e8ec188fb6'
_TOOLS4J_PREDECESSOR_SHA256 = 'f390043a4b1522e78b734b2aaf845d2c4276087fc1991138a655f29b122ca6a5'


def include_tools4j_refinement_successor(previous_rows, receipt=_TOOLS4J_RECEIPT):
    """Reconcile only the sealed current inventory and qualified source refinements.

    Each row keeps the inherited manifest hash, exact pinned-master preimage,
    and final received hash distinct. Historical receipts remain unchanged.
    """
    prior = list(previous_rows)
    if (len(prior) != 113 or any(len(row) != 2 for row in prior)
            or len({row[0] for row in prior}) != len(prior)):
        raise SystemExit('Tools4j predecessor rows missing, duplicate or malformed')
    predecessor = ''.join(path + '\t' + expected + '\n' for path, expected in prior)
    if hashlib.sha256(predecessor.encode('utf-8')).hexdigest() != _TOOLS4J_PREDECESSOR_SHA256:
        raise SystemExit('Tools4j predecessor receipt chain drift')
    if receipt.is_symlink() or not receipt.is_file() or digest(receipt) != _TOOLS4J_RECEIPT_SHA256:
        raise SystemExit('Tools4j successor receipt missing, aliased or drifted')
    entries = [line.split('\t') for line in receipt.read_text(encoding='utf-8').splitlines()]
    if len(entries) != 418:
        raise SystemExit('Tools4j successor must declare exactly418 source transitions')
    current = dict(prior)
    extra = []
    seen = set()
    for cells in entries:
        if len(cells) != 4:
            raise SystemExit('Tools4j malformed source transition')
        path, inherited, master, after = cells
        relative = Path(path)
        java_source = path.endswith('.java') and path.startswith((
            'm3/collections/src/main/java/', 'm3/collections/src/test/java/'))
        native_source = path == 'm3/collections/src/main/native/collections/m3_long_address_kernel.c'
        if (relative.is_absolute() or '..' in relative.parts or '\\' in path
                or not (java_source or native_source) or path in seen):
            raise SystemExit('Tools4j duplicate or out-of-scope source: ' + path)
        seen.add(path)
        hashes = (master, after) if inherited == 'ABSENT' else (inherited, master, after)
        if any(len(value) != 64 or any(c not in '0123456789abcdef' for c in value) for value in hashes):
            raise SystemExit('Tools4j invalid source hash: ' + path)
        if inherited == after:
            raise SystemExit('Tools4j unchanged source is not a transition: ' + path)
        if inherited == 'ABSENT':
            if path in current:
                raise SystemExit('Tools4j occupied source admission: ' + path)
            extra.append((path, after))
        else:
            if current.get(path) != inherited:
                raise SystemExit('Tools4j inherited source preimage drift: ' + path)
            current[path] = after
    result = [(path, current[path]) for path, _ in prior] + extra
    source_java = {path for path, _ in result if path.endswith('.java') and '/src/' in path}
    if len(result) != 510 or len(source_java) != 503:
        raise SystemExit('Tools4j exact current source membership drift')
    return result


# Exact successor sealing the superset source set after the Tools4j refinement.
_SUPERSET_RECEIPT = HERE / 'qualification/superset-source-successor-20261030.tsv'
_SUPERSET_RECEIPT_SHA256 = '74ef03554983726b1db350f743ae2d6a218d9f5e380d3b523703ff6b4cece3d1'
_SUPERSET_PREDECESSOR_SHA256 = 'e76d7c11d6258711d18e10b1d448400a83c987fa59683ee9fd087bbeaf7387e2'
_SUPERSET_TRANSITIONS = 428
_SUPERSET_RESULT_ROWS = 772
_SUPERSET_RESULT_JAVA = 765


def admit_source_transitions(previous_rows, label, receipt, receipt_sha256, predecessor_sha256,
        transitions, result_rows, result_java):
    """Admit only the sealed (path, inherited, after) transitions of RECEIPT over the exact predecessor rows.

    ABSENT admits a new Java source under the receiving roots; otherwise the inherited digest must
    match before the final digest replaces it. A final REMOVED retires a recorded source whose file was
    renamed or deleted (its inherited digest must match). Historical receipts remain unchanged.
    """
    prior = list(previous_rows)
    predecessor = ''.join(path + '\t' + expected + '\n' for path, expected in prior)
    if hashlib.sha256(predecessor.encode('utf-8')).hexdigest() != predecessor_sha256:
        raise SystemExit(label + ' predecessor receipt chain drift')
    if receipt.is_symlink() or not receipt.is_file() or digest(receipt) != receipt_sha256:
        raise SystemExit(label + ' successor receipt missing, aliased or drifted')
    entries = [line.split('\t') for line in receipt.read_text(encoding='utf-8').splitlines()]
    if len(entries) != transitions:
        raise SystemExit(label + ' successor must declare exactly ' + str(transitions) + ' source transitions')
    current = dict(prior)
    extra = []
    seen = set()
    for cells in entries:
        if len(cells) != 3:
            raise SystemExit(label + ' malformed source transition')
        path, inherited, after = cells
        relative = Path(path)
        java_source = path.endswith('.java') and path.startswith((
            'm3/collections/src/main/java/', 'm3/collections/src/test/java/'))
        if (relative.is_absolute() or '..' in relative.parts or chr(92) in path or path in seen
                or not (java_source or path in current)):
            raise SystemExit(label + ' duplicate or out-of-scope source: ' + path)
        seen.add(path)
        if inherited == 'ABSENT' and after == 'REMOVED':
            raise SystemExit(label + ' absent source cannot be removed: ' + path)
        hashes = (after,) if inherited == 'ABSENT' else (inherited,) if after == 'REMOVED' else (inherited, after)
        if any(len(value) != 64 or any(c not in '0123456789abcdef' for c in value) for value in hashes):
            raise SystemExit(label + ' invalid source hash: ' + path)
        if inherited == after:
            raise SystemExit(label + ' unchanged source is not a transition: ' + path)
        if after == 'REMOVED':
            if current.get(path) != inherited:
                raise SystemExit(label + ' inherited source preimage drift: ' + path)
            del current[path]
        elif inherited == 'ABSENT':
            if path in current:
                raise SystemExit(label + ' occupied source admission: ' + path)
            extra.append((path, after))
        else:
            if current.get(path) != inherited:
                raise SystemExit(label + ' inherited source preimage drift: ' + path)
            current[path] = after
    result = [(path, current[path]) for path, _ in prior if path in current] + extra
    source_java = {path for path, _ in result if path.endswith('.java') and '/src/' in path}
    if len(result) != result_rows or len(source_java) != result_java:
        raise SystemExit(label + ' exact current source membership drift')
    return result


def include_superset_source_successor(previous_rows, receipt=_SUPERSET_RECEIPT):
    """Admit only the sealed (path, inherited, after) transitions over the Tools4j result."""
    return admit_source_transitions(previous_rows, 'Superset', receipt, _SUPERSET_RECEIPT_SHA256,
                                    _SUPERSET_PREDECESSOR_SHA256, _SUPERSET_TRANSITIONS,
                                    _SUPERSET_RESULT_ROWS, _SUPERSET_RESULT_JAVA)


# Exact successor sealing the master source set after the superset successor: sources that merged PRs changed
# or added after superset-source-successor-20261030 (installed-collections job 124012481118 on bb66a8c16642).
_MASTER_RECEIPT = HERE / 'qualification/master-source-successor-20261031.tsv'
_MASTER_RECEIPT_SHA256 = '3435d71706220d7a82e570739c0d6cfa9e57bd4e3ca911270f62510e2c0dff3d'
_MASTER_PREDECESSOR_SHA256 = '0fdc2fdfcc553ac60118bfc541c3c5c0eeffea2d293f15709b0982a4b3d29e2e'
_MASTER_TRANSITIONS = 10
_MASTER_RESULT_ROWS = 774
_MASTER_RESULT_JAVA = 767


def include_master_source_successor(previous_rows, receipt=_MASTER_RECEIPT):
    """Admit only the sealed (path, inherited, after) transitions over the superset result."""
    return admit_source_transitions(previous_rows, 'Master 20261031', receipt, _MASTER_RECEIPT_SHA256,
                                    _MASTER_PREDECESSOR_SHA256, _MASTER_TRANSITIONS,
                                    _MASTER_RESULT_ROWS, _MASTER_RESULT_JAVA)


# Exact successor sealing the module-move tree (2026-11-09): the master seal of 20261031 rebased onto the sources
# absorbed, renamed and moved since (A1/B+C, E1, U1, E2, the v6 batches, MOVE_TO_JDK_MODULE v3 #2849). Recorded
# keys stay in the m3/collections/src key space; located() finds each file. Generator:
# qualification/module-source-successor-20261109.py (replays the sealed chain, diffs it against the tree).
_MODULE_RECEIPT = HERE / 'qualification/module-source-successor-20261109.tsv'
_MODULE_RECEIPT_SHA256 = '3c8cc30a27385503e473d1cef6bf59374d6c5633b1dba1105e3f73a21865b2b9'
_MODULE_PREDECESSOR_SHA256 = '860b9552b6d9e13ec46b6634ce1a6bf759f75f71aee1604533bb3e8c4d770e44'
_MODULE_TRANSITIONS = 2650
_MODULE_RESULT_ROWS = 1932
_MODULE_RESULT_JAVA = 1926


def include_module_source_successor(previous_rows, receipt=_MODULE_RECEIPT):
    """Admit only the sealed (path, inherited, after) transitions over the master result."""
    return admit_source_transitions(previous_rows, 'Module 20261109', receipt, _MODULE_RECEIPT_SHA256,
                                    _MODULE_PREDECESSOR_SHA256, _MODULE_TRANSITIONS,
                                    _MODULE_RESULT_ROWS, _MODULE_RESULT_JAVA)


# Exact current-context refinements; historical receipts and source guards remain unchanged.
_CURRENT_CONTEXT_RECEIPT = HERE / 'qualification/current-context-source-successor-20261113.tsv'
_CURRENT_CONTEXT_RECEIPT_SHA256 = '62db1f5618f037a996528cc789416dd43934acee90e73a4b9356d2a25c24582a'
_CURRENT_CONTEXT_PREDECESSOR_SHA256 = '82bf706fcaa997feb84806b87923f7fb4a275eeb19dfcfcd4d6d3049db0b372a'
_CURRENT_CONTEXT_TRANSITIONS = 796
_CURRENT_CONTEXT_RESULT_ROWS = 1932
_CURRENT_CONTEXT_RESULT_JAVA = 1926


def include_current_context_source_successor(previous_rows, receipt=_CURRENT_CONTEXT_RECEIPT):
    """Admit the qualified current-context refinements over the exact module result."""
    return admit_source_transitions(previous_rows, 'Current context 20261113', receipt,
                                    _CURRENT_CONTEXT_RECEIPT_SHA256, _CURRENT_CONTEXT_PREDECESSOR_SHA256,
                                    _CURRENT_CONTEXT_TRANSITIONS, _CURRENT_CONTEXT_RESULT_ROWS,
                                    _CURRENT_CONTEXT_RESULT_JAVA)



# Additive current-receiver source cohort; immutable historical receipts stay unchanged.
_CURRENT_RECEIVER_RECEIPT = HERE / 'qualification/current-receiver-source-successor-20261113.tsv'
_CURRENT_RECEIVER_RECEIPT_SHA256 = 'c0390d51cc7d9a5fd326d0c2722c61f6985502721547e7ae81e3f985c8cb4a93'
_CURRENT_RECEIVER_PREDECESSOR_SHA256 = '54953383deb9998b7f8f411256c8d1ac2d8a343fa8dd7744ed6a8fe7454f7913'
_CURRENT_RECEIVER_TRANSITIONS = 40
_CURRENT_RECEIVER_RESULT_ROWS = 1957
_CURRENT_RECEIVER_RESULT_JAVA = 1951


def include_current_receiver_source_successor(previous_rows, receipt=_CURRENT_RECEIVER_RECEIPT):
    """Admit the exact current receiver cohort over its immutable current-context ancestor."""
    return admit_source_transitions(previous_rows, 'Current receiver 20261113', receipt,
                                    _CURRENT_RECEIVER_RECEIPT_SHA256, _CURRENT_RECEIVER_PREDECESSOR_SHA256,
                                    _CURRENT_RECEIVER_TRANSITIONS, _CURRENT_RECEIVER_RESULT_ROWS,
                                    _CURRENT_RECEIVER_RESULT_JAVA)


_CURRENT_PROBE_RECEIPT = HERE / 'qualification/current-probe-source-successor-20261113.tsv'
_CURRENT_PROBE_RECEIPT_SHA256 = '6fc5b1a0ca9e4226e3b1aa5634afb5e222ee11c3544594f16f6b90ff7cf39f7e'
_CURRENT_PROBE_PREDECESSOR_SHA256 = 'd7c4cd1d8e1357ba3c91f84309891b7aed504980f5abdb142c16816952e276e4'
_CURRENT_PROBE_TRANSITIONS = 2
_CURRENT_PROBE_RESULT_ROWS = 1957
_CURRENT_PROBE_RESULT_JAVA = 1951


def include_current_probe_source_successor(previous_rows, receipt=_CURRENT_PROBE_RECEIPT):
    """Admit the exact current receiver cohort over its immutable current-context ancestor."""
    return admit_source_transitions(previous_rows, 'Current probe 20261113', receipt,
                                    _CURRENT_PROBE_RECEIPT_SHA256, _CURRENT_PROBE_PREDECESSOR_SHA256,
                                    _CURRENT_PROBE_TRANSITIONS, _CURRENT_PROBE_RESULT_ROWS,
                                    _CURRENT_PROBE_RESULT_JAVA)


_LONG_NATIVE_RECEIPT = HERE / 'qualification/longlane-required-native-source-successor-20261113.tsv'
_LONG_NATIVE_RECEIPT_SHA256 = 'd5ae8c5f706d23cde626460ee6810f1af666ec4a0f80ce2cb1e49189ded87f2d'
_LONG_NATIVE_PREDECESSOR_SHA256 = '58fa6463746028a79b7e398dcc2f5fb7a6159f6a09986e809638b2503506a759'
_LONG_NATIVE_TRANSITIONS = 2
_LONG_NATIVE_RESULT_ROWS = 1957
_LONG_NATIVE_RESULT_JAVA = 1951


def include_longlane_required_native_source_successor(previous_rows, receipt=_LONG_NATIVE_RECEIPT):
    """Admit the exact current receiver cohort over its immutable current-context ancestor."""
    return admit_source_transitions(previous_rows, 'LongLane required native 20261113', receipt,
                                    _LONG_NATIVE_RECEIPT_SHA256, _LONG_NATIVE_PREDECESSOR_SHA256,
                                    _LONG_NATIVE_TRANSITIONS, _LONG_NATIVE_RESULT_ROWS,
                                    _LONG_NATIVE_RESULT_JAVA)


# The transition-receipt successors by their m3-donation.json record: (receipt, receipt SHA-256,
# predecessor SHA-256, transitions, resulting rows, exact Java source membership).
SOURCE_TRANSITION_SEALS = {
    'superset_source_successor_20261030': (_SUPERSET_RECEIPT, _SUPERSET_RECEIPT_SHA256, _SUPERSET_PREDECESSOR_SHA256,
                                           _SUPERSET_TRANSITIONS, _SUPERSET_RESULT_ROWS, _SUPERSET_RESULT_JAVA),
    'master_source_successor_20261031': (_MASTER_RECEIPT, _MASTER_RECEIPT_SHA256, _MASTER_PREDECESSOR_SHA256,
                                         _MASTER_TRANSITIONS, _MASTER_RESULT_ROWS, _MASTER_RESULT_JAVA),
    'module_source_successor_20261109': (_MODULE_RECEIPT, _MODULE_RECEIPT_SHA256, _MODULE_PREDECESSOR_SHA256,
                                         _MODULE_TRANSITIONS, _MODULE_RESULT_ROWS, _MODULE_RESULT_JAVA),
    'current_context_source_successor_20261113': (_CURRENT_CONTEXT_RECEIPT, _CURRENT_CONTEXT_RECEIPT_SHA256, _CURRENT_CONTEXT_PREDECESSOR_SHA256,
                                           _CURRENT_CONTEXT_TRANSITIONS, _CURRENT_CONTEXT_RESULT_ROWS, _CURRENT_CONTEXT_RESULT_JAVA),
    'current_receiver_source_successor_20261113': (_CURRENT_RECEIVER_RECEIPT, _CURRENT_RECEIVER_RECEIPT_SHA256, _CURRENT_RECEIVER_PREDECESSOR_SHA256,
                                            _CURRENT_RECEIVER_TRANSITIONS, _CURRENT_RECEIVER_RESULT_ROWS, _CURRENT_RECEIVER_RESULT_JAVA),
    'current_probe_source_successor_20261113': (_CURRENT_PROBE_RECEIPT, _CURRENT_PROBE_RECEIPT_SHA256, _CURRENT_PROBE_PREDECESSOR_SHA256,
        _CURRENT_PROBE_TRANSITIONS, _CURRENT_PROBE_RESULT_ROWS, _CURRENT_PROBE_RESULT_JAVA),
    'longlane_required_native_source_successor_20261113': (_LONG_NATIVE_RECEIPT, _LONG_NATIVE_RECEIPT_SHA256, _LONG_NATIVE_PREDECESSOR_SHA256,
        _LONG_NATIVE_TRANSITIONS, _LONG_NATIVE_RESULT_ROWS, _LONG_NATIVE_RESULT_JAVA),
}



_FULL_VERIFIER_SHA256 = "6bd246c84d91ab0ac6c8de02246d1a8c00db62aadc03387e3337bc4735b52947"
_FULL_PROFILE_SHA256 = {"java": "3156a2fd1b43f002062a4a1c282756d07ad687333ad54ee3b109ac5ea8bf38b9", "native": "d8188c990d2586771a9254e22c889d9e845deafb3e0c18a676f57560515a3fd1"}


def _closure_file(relative, missing_ok=False):
    """A canonical contained receipt/source/artifact path; reject aliased components."""
    if (not isinstance(relative, str) or not relative or "\\" in relative or ":" in relative
            or any(part in ("", ".", "..") for part in relative.split("/"))
            or Path(relative).is_absolute()):
        raise SystemExit("Invalid full-build path: " + repr(relative))
    path = REPO
    for part in relative.split("/"):
        path = path / part
        if path.is_symlink() or getattr(path, "is_junction", lambda: False)():
            raise SystemExit("Aliased full-build path: " + relative)
    if not path.resolve().is_relative_to(REPO.resolve()):
        raise SystemExit("Escaping full-build path: " + relative)
    if not path.is_file() and not (missing_ok and not path.exists()):
        raise SystemExit("Missing full-build file: " + relative)
    return path


def _full_product_resources():
    return {'META-INF/LICENSE': HERE / 'LICENSE', 'META-INF/provenance.json': HERE / 'provenance.json', 'META-INF/m3-donation.json': HERE / 'm3-donation.json', 'META-INF/name-mapping.json': HERE / 'name-mapping.json', 'META-INF/M3-DONATION-NOTICE.txt': HERE / 'M3-DONATION-NOTICE.txt', 'META-INF/NETBEANS-NOTICE.txt': HERE / 'NETBEANS-NOTICE.txt', 'META-INF/m3collections.md': REPO / 'src/jdk.m3.collections/share/legal/m3collections.md', 'jdk/m3/collections/M3_SDK_FAMILY_CATALOG.tsv': REPO / 'src/jdk.m3.collections/share/classes/jdk/m3/collections/M3_SDK_FAMILY_CATALOG.tsv'}


def _qualified_full_build(mains, tests, runner, java_home, admitted_sources):
    """Execute the existing owner once; admit fresh source/artifact/command evidence."""
    script_name = "m3/collections/qualification/full-java-jni-20261018/verify.py"
    script = _closure_file(script_name)
    if digest(script) != _FULL_VERIFIER_SHA256:
        raise SystemExit("Full verifier source drift; qualify the owning recipe")
    tree = ast.parse(script.read_text(encoding="utf-8"))
    registry_node = next(n for n in tree.body if isinstance(n, ast.Assign)
                         and any(isinstance(t, ast.Name) and t.id == "LIBRARIES" for t in n.targets))
    registry = ast.literal_eval(registry_node.value)
    native = REPO / "src/jdk.m3.collections/share/native/libm3collections"
    fixtures = [REPO / "test/jdk/jdk/m3/collections/native" / name for name in
                ("m3_mask_window_spans.c", "m3_mask_window_regions.c",
                 "m3_bitlane_jump_jni.c", "m3_gather_spans.c")]
    admitted = {native / name for entry in registry.values() for name in entry["sources"]}
    sources = set(mains + tests) | admitted | set(fixtures)
    sources |= set(native.rglob("*.h")) | set((REPO / "m3/native/include").rglob("*.h"))
    sources |= {HERE / "pom.xml", REPO / "m3/arrays/pom.xml",
                REPO / "make/m3/collections/CMakeLists.txt"}
    expected_sources = {p.relative_to(REPO).as_posix():
                        digest(_closure_file(p.relative_to(REPO).as_posix())) for p in sorted(sources)}
    extra_snapshot = {script_name: _FULL_VERIFIER_SHA256}
    for source in _full_product_resources().values():
        relative = source.relative_to(REPO).as_posix()
        extra_snapshot[relative] = digest(_closure_file(relative))
    if not isinstance(admitted_sources, dict) or not admitted_sources:
        raise SystemExit("Installed admitted source inventory is missing")
    for relative, expected in admitted_sources.items():
        if digest(_closure_file(relative)) != expected:
            raise SystemExit("Installed admitted source changed before full build: " + relative)
        extra_snapshot[relative] = expected
    head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=REPO, text=True).strip()
    if os.environ.get("EXPECTED_HEAD") not in (None, head):
        raise SystemExit("Installed checked-out head differs from expected head")
    receipt_name = "m3/collections/target/full-java-jni/PROOF.json"
    receipt_path = _closure_file(receipt_name, missing_ok=True)
    def signature():
        if not receipt_path.exists():
            return None
        s = receipt_path.stat()
        return (s.st_ino, s.st_size, s.st_mtime_ns, s.st_ctime_ns)
    previous = signature()
    env = dict(os.environ, EXPECTED_HEAD=head, JAVA_HOME=str(java_home))
    runner("02-full-java-jni", [sys.executable, "-I", "-S", "-B", script], env=env)
    receipt_path = _closure_file(receipt_name)
    if signature() == previous:
        raise SystemExit("Full-build receipt is stale")
    raw = receipt_path.read_bytes()
    proof = json.loads(raw.decode("utf-8"), object_pairs_hook=unique_json_object)
    if (proof.get("status") != "PASS_OWNING_COLLECTION_JAVA_AND_JNI"
            or proof.get("head_sha") != head or proof.get("source_sha256") != expected_sources
            or proof.get("native") != "generated headers; C17 strict; fatal UBSan; actual -Xcheck:jni"
            or proof.get("jvm_profile_evidence") != "configured -DargLine vectors bound to successful Maven argv; Surefire child argv not observed"
            or proof.get("java_sources") != len(mains) or proof.get("test_sources") != len(tests)):
        raise SystemExit("Full-build head/source/count receipt mismatch")
    snapshot = dict(expected_sources, **extra_snapshot)
    for name, expected in snapshot.items():
        if digest(_closure_file(name)) != expected:
            raise SystemExit("Full-build input changed: " + name)
    if subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=REPO, text=True).strip() != head:
        raise SystemExit("Full-build head changed during execution")
    artifacts = proof.get("native_library_artifacts")
    if (proof.get("native_libraries") != sorted(registry)
            or not isinstance(artifacts, dict) or set(artifacts) != set(registry)):
        raise SystemExit("Full-build native registry mismatch")
    for name, record in artifacts.items():
        expected_path = "m3/collections/target/full-java-jni/native/lib" + name + ".so"
        if (not isinstance(record, dict) or set(record) != {"path", "sha256"}
                or record["path"] != expected_path
                or digest(_closure_file(record["path"])) != record["sha256"]):
            raise SystemExit("Full-build native artifact mismatch: " + name)
        snapshot[expected_path] = record["sha256"]
    array_name = "m3/collections/target/full-java-jni/array-native/libm3_arrays.so"
    array_record = proof.get("arrays_native_artifact", {})
    arrays = proof.get("source_dependencies", {}).get("jdk.m3.arrays", {})
    if (set(array_record) != {"path", "sha256"} or array_record["path"] != array_name
            or digest(_closure_file(array_name)) != array_record["sha256"]
            or arrays.get("native_sha256") != array_record["sha256"]):
        raise SystemExit("Full-build arrays provider mismatch")
    jar_name = "m3/collections/target/m3-collections-0.1.0-SNAPSHOT.jar"
    arrays_name = "m3/arrays/target/m3-arrays-0.1.0-SNAPSHOT.jar"
    jar, array_jar = _closure_file(jar_name), _closure_file(arrays_name)
    for name, expected in [(jar_name, proof.get("jar_sha256")),
                           (arrays_name, arrays.get("jar_sha256")),
                           (array_name, array_record["sha256"])]:
        if digest(_closure_file(name)) != expected:
            raise SystemExit("Full-build JAR/provider drift: " + name)
        snapshot[name] = expected
    commands, modes = proof.get("completed_commands"), proof.get("configured_jvm_profiles")
    if (not isinstance(commands, list) or not commands or not isinstance(modes, dict)
            or set(modes) != {"java", "native"}):
        raise SystemExit("Full-build command/profile evidence missing")
    for command in commands:
        if (not isinstance(command, dict) or set(command) != {"argv", "cwd", "returncode"}
                or not isinstance(command["argv"], list) or not command["argv"]
                or any(not isinstance(arg, str) for arg in command["argv"])
                or command["cwd"] != str(REPO) or type(command["returncode"]) is not int
                or command["returncode"] != 0):
            raise SystemExit("Invalid completed full-build command")
    prefix = [os.environ.get("MVN", "mvn"), "-B", "-ntp"]
    if os.environ.get("M3_MAVEN_OFFLINE") == "1":
        prefix.append("-o")
    if os.environ.get("M2_REPO"):
        prefix.append("-Dmaven.repo.local=" + os.environ["M2_REPO"])
    for mode, profile in modes.items():
        if not isinstance(profile, dict) or set(profile) != {"vm_args", "completed_command_index"}:
            raise SystemExit("Invalid configured full-build profile")
        vm, index = profile["vm_args"], profile["completed_command_index"]
        if (not isinstance(vm, list) or any(not isinstance(arg, str) for arg in vm)
                or type(index) is not int or index < 0 or index >= len(commands)):
            raise SystemExit("Invalid full-build JVM vector/index")
        normalized = [arg.replace(str(REPO), "${REPO}").replace(os.sep, "/") for arg in vm]
        fingerprint = hashlib.sha256(json.dumps(normalized, separators=(",", ":"), ensure_ascii=True).encode()).hexdigest()
        if fingerprint != _FULL_PROFILE_SHA256[mode]:
            raise SystemExit("Full-build configured JVM vector drift: " + mode)
        expected_command = prefix + ["-f", str(HERE / "pom.xml"), "-Dtest=*",
                                   "-DargLine=" + " ".join(vm), "-DfailIfNoTests=true", "verify"]
        if commands[index]["argv"] != expected_command:
            raise SystemExit("Full-build JVM/Maven command link drift: " + mode)
    if modes["java"]["completed_command_index"] == modes["native"]["completed_command_index"]:
        raise SystemExit("Full-build modes share a command record")
    for mode in ("java", "native"):
        counts = proof.get("junit", {}).get(mode, {})
        if (set(counts) != {"tests", "failures", "errors", "skipped"}
                or any(type(v) is not int or v < 0 for v in counts.values())
                or not counts["tests"] or counts["failures"] or counts["errors"]
                or mode == "native" and counts["skipped"]):
            raise SystemExit("Full-build owning suite failed: " + mode)
    snapshot[receipt_name] = hashlib.sha256(raw).hexdigest()
    for relative, expected in snapshot.items():
        if digest(_closure_file(relative)) != expected:
            raise SystemExit("Full-build evidence drift before installed commands: " + relative)
    return proof, jar, array_jar, list(modes["native"]["vm_args"]), snapshot, raw


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--cost-probe', action='store_true', help='record paired diagnostic allocation/time samples')
    args = parser.parse_args()
    java_home = Path(os.environ['JAVA_HOME']).resolve()
    junit = Path(os.environ['JUNIT_CONSOLE_JAR']).resolve()
    if not junit.is_file():
        raise SystemExit('JUNIT_CONSOLE_JAR must name a real JUnit platform console standalone JAR')
    if digest(junit) != '329bd10288875a74d04c9ca6b7c9889c265ddf87b21a9ea42a7f7392f391472a':
        raise SystemExit('Expected exact JUnit console 1.12.2 artifact')
    if not sys.platform.startswith('linux'):
        raise SystemExit('This JNI acceptance host requires Linux; no partial overall PASS')
    target = HERE / 'target/donation-verification'
    shutil.rmtree(target, ignore_errors=True)
    evidence = target / 'evidence'
    evidence.mkdir(parents=True)

    def run(name, command, *, env=None):
        command = [str(x) for x in command]
        result = subprocess.run(command, cwd=REPO, capture_output=True, text=True, env=env)
        output = result.stdout + result.stderr
        (evidence / (name + '.log')).write_text(
            '$ ' + repr(command) + '\n' + output + '\nEXIT=' + str(result.returncode) + '\n')
        if result.returncode:
            print(output[-16000:])
            raise SystemExit(result.returncode)
        print(name + ': PASS', flush=True)
        return output

    version = run('01-java-version', [java_home / 'bin/java', '-version'])
    if 'version "21.' not in version and 'version "21"' not in version:
        raise SystemExit('The qualified baseline requires Java 21')
    packet = read_donation_packet()
    manifest_name = packet.get('installed_source_manifest', 'qualification/installed-source-enum-set-20261008.tsv')
    manifest_relative = Path(manifest_name)
    if manifest_relative.is_absolute() or '..' in manifest_relative.parts:
        raise SystemExit('Invalid installed source manifest path')
    manifest = HERE / manifest_relative
    if digest(manifest) != packet['installed_source_manifest_sha256']:
        raise SystemExit('Installed source manifest drift: actual=' + digest(manifest) + ' expected=' + packet['installed_source_manifest_sha256'] + '; reconcile with canonical recipe before verifying')
    rows = [line.split('\t') for line in manifest.read_text().splitlines()
            if line and not line.startswith('#')]
    rows = include_path_mask_successor(include_stored_representative_successor(include_filtered_view_successor(rows)))
    rows = include_object_state_offset_successor(rows)
    rows = include_tools4j_refinement_successor(rows)
    rows = include_superset_source_successor(rows)
    rows = include_master_source_successor(rows)
    rows = include_module_source_successor(rows)
    rows = include_current_context_source_successor(rows)
    rows = include_current_receiver_source_successor(rows)
    rows = include_current_probe_source_successor(rows)
    rows = include_longlane_required_native_source_successor(rows)
    if len({row[0] for row in rows}) != len(rows):
        raise SystemExit('Duplicate installed source path')
    for path, expected in rows:
        relative = Path(path)
        if relative.is_absolute() or '..' in relative.parts or not path.startswith('m3/collections/'):
            raise SystemExit('Invalid receiver path: ' + path)
        source = located(path)
        if (not source.is_file()
                or (path.endswith("/M3ObjectStateSlots28.java")
                    or path.endswith("/M3ObjectStateSlots28SelfTest.java")) and source.is_symlink()
                or digest(source) != expected):
            raise SystemExit('Installed source drift: ' + path)
    declared_java = {path for path, _ in rows if path.endswith('.java') and '/src/' in path}
    actual_java = {recorded(path) for path in (HERE / 'src').rglob('*.java')}
    actual_java |= {recorded(path) for root in MOVED_JAVA_ROOTS for path in (REPO / root).rglob('*.java')}
    if declared_java != actual_java:
        raise SystemExit('Java source set changed; qualify a successor packet')
    mains = sorted(located(path) for path in declared_java if '/src/main/' in path)
    tests = sorted(located(path) for path in declared_java if '/src/test/' in path)
    probes = [HERE / 'verification' / (name + '.java') for name in
              ('SegmentedLaneNativeProbe', 'PreparedLaneAllocationProbe')]
    if read_donation_packet() != packet or digest(manifest) != packet["installed_source_manifest_sha256"]:
        raise SystemExit("Installed metadata/manifest changed before full build")
    admitted_sources = {located(path).relative_to(REPO).as_posix(): expected for path, expected in rows}
    full, jar, array_jar, native_vm, full_snapshot, full_raw = _qualified_full_build(mains, tests, run, java_home, admitted_sources)
    (evidence / 'full-build-proof.json').write_bytes(full_raw)
    classes, test_classes = target / 'classes', target / 'test-classes'
    classes.mkdir()
    test_classes.mkdir()
    common = [java_home / 'bin/javac', '--release', '21', '-Xlint:all,-module', '-Werror', '-Xpkginfo:always']
    run('02-module-compile', common + ['--module-path', array_jar, '-d', classes] + mains)
    run('03-test-compile', common + ['-cp', os.pathsep.join(map(str, (classes, junit, array_jar))),
                                   '-d', test_classes] + tests + probes)
    test_resource_root = REPO / 'test/jdk/jdk/m3/collections/resources'
    for resource in test_resource_root.rglob('*'):
        if resource.is_file():
            destination = test_classes / resource.relative_to(test_resource_root)
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(resource, destination)
    runtime_classpath = os.pathsep.join(map(str, (jar, test_classes, array_jar)))
    reports = target / 'junit-reports'
    run('04-all-contract-tests', [java_home / 'bin/java', *native_vm, '-jar', junit, 'execute',
                                '--disable-banner', '--class-path', runtime_classpath,
                                '--scan-class-path', '--fail-if-no-tests', '--reports-dir', reports])
    totals = {'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
    for report in reports.glob('TEST-*.xml'):
        suite = ET.parse(report).getroot()
        for key in totals:
            totals[key] += int(suite.attrib.get(key, '0'))
    if not totals['tests'] or any(totals[key] for key in ('failures', 'errors', 'skipped')):
        raise SystemExit('JUnit acceptance requires nonzero tests and no failures/errors/skips: ' + repr(totals))
    for name in ('LazyTreeMapSelfTest', 'LazyValueStateSelfTest'):
        run('04-' + name, [java_home / 'bin/java', *native_vm, '-ea', '-cp', runtime_classpath,
                           'jdk.m3.collections.' + name])
    descriptor = run('06-module-descriptor', [java_home / 'bin/jar', '--describe-module', '--file', jar])
    requirements = [line.strip() for line in descriptor.splitlines() if line.strip().startswith('requires ')]
    if sorted(requirements) != ['requires java.base mandated', 'requires jdk.m3.arrays transitive']:
        raise SystemExit('Unexpected runtime dependency: ' + repr(requirements))
    with zipfile.ZipFile(jar) as archive:
        expected_classes = {p.relative_to(classes).as_posix() for p in classes.rglob('*.class')}
        packaged_names = archive.namelist()
        actual_classes = {name for name in packaged_names if name.endswith('.class')}
        if actual_classes != expected_classes or any(packaged_names.count(name) != 1 for name in actual_classes):
            raise SystemExit('Full-build JAR differs from fresh installed class inventory')
        packaged_resources = _full_product_resources()
        for name, source in packaged_resources.items():
            if packaged_names.count(name) != 1 or archive.read(name) != source.read_bytes():
                raise SystemExit('Packaged module resource missing, duplicate or drifted: ' + name)
        test_resources = {p.relative_to(test_resource_root).as_posix() for p in test_resource_root.rglob('*') if p.is_file()}
        if test_resources.intersection(packaged_names):
            raise SystemExit('Test resources leaked into product JAR')
    library = _closure_file(full['native_library_artifacts']['m3bitlane']['path'])
    run('09-checked-jni-parity', [java_home / 'bin/java', *native_vm, '-cp', runtime_classpath,
                                 'SegmentedLaneNativeProbe', library])
    run('10-prepared-allocation', [java_home / 'bin/java', *native_vm, '-Xbatch', '-cp', runtime_classpath,
                                  'PreparedLaneAllocationProbe'])
    if args.cost_probe:
        cost = run('11-collection-cost-probe', [java_home / 'bin/java', *native_vm, '-Xbatch', '-cp', runtime_classpath,
                                               'jdk.m3.collections.CollectionCostProbe'])
        (evidence / 'collection-cost.csv').write_text(cost)
    for relative, expected in full_snapshot.items():
        if digest(_closure_file(relative)) != expected:
            raise SystemExit('Full-build evidence changed during installed acceptance: ' + relative)
    if read_donation_packet() != packet or digest(manifest) != packet["installed_source_manifest_sha256"]:
        raise SystemExit("Installed metadata/manifest changed during acceptance")
    final_head = run('12-final-head', ['git', 'rev-parse', 'HEAD']).strip()
    if final_head != full['head_sha']:
        raise SystemExit('Head changed during installed acceptance')
    receipt = {'copyright': 'Copyright 2026 Hitesh Soliwal and contributors', 'license': 'Apache-2.0', 'schema': 'M3_COLLECTION_RECEIVING_QUALIFICATION_V1',
               'scope': 'SEPARATELY_BUILT_COLLECTION_MODULE',
               'installed_source_manifest_sha256': digest(manifest),
               'java_sources': len(mains), 'test_sources': len(tests), 'junit': totals,
               'main_self_tests': ['LazyTreeMapSelfTest:PASS', 'LazyValueStateSelfTest:PASS'],
               'java_compile': '--release 21 -Xlint:all,-module -Werror -Xpkginfo:always; arrays module path',
               'full_build_head_sha': full['head_sha'],
               'full_build_receipt_sha256': hashlib.sha256(full_raw).hexdigest(),
               'full_build_verifier_sha256': _FULL_VERIFIER_SHA256,
               'full_build_launches': 1,
               'jvm_profile_evidence': full['jvm_profile_evidence'],
               'runtime_requires': ['java.base', 'jdk.m3.arrays'], 'native_standard': 'c17',
               'undefined_behavior_sanitizer': 'PASS', 'checked_jni': 'PASS',
               'prepared_lane_allocation': 'PASS', 'packaged_license_provenance': 'PASS',
               'cost_probe': 'DIAGNOSTIC_RECORDED' if args.cost_probe else 'NOT_RUN',
               'full_reactor': 'NOT_RUN', 'openjdk_jtreg_hotspot': 'NOT_RUN',
               'universal_performance_claim': False, 'phase_advance': False,
               'active_phase': 'STRING', 'target_baseline': 'eda1ae0733952394851c3580f8efb7624563b98c'}
    (evidence / 'qualification.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps(receipt, indent=2))


if __name__ == '__main__':
    main()
