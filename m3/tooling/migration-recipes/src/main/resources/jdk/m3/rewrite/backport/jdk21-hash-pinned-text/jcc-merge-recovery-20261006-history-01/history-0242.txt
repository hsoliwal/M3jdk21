#!/usr/bin/env python3
"""Read-only Git-tree and exact-restoration audit for the pinned merge recovery."""
import hashlib
import json
from pathlib import Path, PurePosixPath

TASK = Path('/workspace/scratch/1c68df1bae79/javac-convergence-20261005')
REVIEW = TASK / 'work/source-merge-review'
OUT = TASK / 'work/publication-current/source-recovery-publish'
BASE_COMMIT = 'be92c62ece9023b5c33676716a1076d00e26120a'
BASE_TREE = '3c4f32b66633a251ba2c117090830252a7e2da03'
DELIVERED_TREE = '6f9b6a48f8cefa7a643f222c7aa1f621497725e7'


def digest(raw):
    return hashlib.sha256(raw).hexdigest()


def git_blob(raw):
    return hashlib.sha1(b'blob ' + str(len(raw)).encode() + b'\0' + raw).hexdigest()


def git_tree(rows):
    data = b''
    for row in sorted(rows, key=lambda e: (e['path'] + ('/' if e['type'] == 'tree' else '')).encode()):
        mode = '40000' if row['type'] == 'tree' else row['mode']
        data += mode.encode() + b' ' + row['path'].encode() + b'\0' + bytes.fromhex(row['sha'])
    return hashlib.sha1(b'tree ' + str(len(data)).encode() + b'\0' + data).hexdigest()


def exact_write(path, raw):
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists():
        assert path.read_bytes() == raw, f'Refuse to replace a different frozen output: {path.name}'
    else:
        path.write_bytes(raw)


def json_bytes(value):
    return (json.dumps(value, indent=2, sort_keys=True) + '\n').encode()


def safe_path(path):
    p = PurePosixPath(path)
    assert path and str(p) == path and not p.is_absolute()
    assert '..' not in p.parts and '\\' not in path and '\0' not in path
    return p.parts


def load_inputs():
    metadata_raw = (REVIEW / 'MERGE_METADATA.json').read_bytes()
    additions_raw = (REVIEW / 'SAFE_ADDITIONS_STAGED.json').read_bytes()
    docs_raw = (REVIEW / 'MERGED_DOC_ABSENCE.json').read_bytes()
    assert digest(additions_raw) == 'a3c4741cad08018ff12f21288fa1483ffd55b2c878ab65f5c3c0ce82a10e3e91'
    assert digest(docs_raw) == '349defbc45ca766238e50530b0e669a3b970cc7f268bb66f51f14d73b49ced54'
    metadata, additions, docs = map(json.loads, (metadata_raw, additions_raw, docs_raw))
    assert metadata['merge_commit']['sha'] == BASE_COMMIT
    assert metadata['merge_commit']['tree']['sha'] == BASE_TREE
    assert additions['merged_revision'] == BASE_COMMIT and additions['merged_root'] == BASE_TREE
    assert docs['merge_commit'] == BASE_COMMIT and docs['merge_root'] == BASE_TREE
    return metadata, additions, docs, {
        'merge_metadata_sha256': digest(metadata_raw),
        'safe_additions_sha256': digest(additions_raw),
        'doc_absence_sha256': digest(docs_raw),
    }


def normalize_trees(metadata, docs):
    trees = {}
    observations = []

    def admit(sha, entries):
        rows = []
        names = set()
        for e in entries:
            assert '/' not in e['path'] and e['path'] not in names
            assert e['type'] in ('tree', 'blob', 'commit')
            names.add(e['path'])
            rows.append({k: e[k] for k in ('path', 'mode', 'type', 'sha', 'size') if k in e})
        assert git_tree(rows) == sha, f'Tree object identity mismatch: {sha}'
        rows.sort(key=lambda e: e['path'])
        if sha in trees:
            assert trees[sha] == rows, f'Inconsistent observations of tree: {sha}'
        else:
            trees[sha] = rows

    def expand(tree, origin):
        assert tree.get('truncated') is False
        entries = tree['tree']
        assert len({e['path'] for e in entries}) == len(entries)
        nested = any('/' in e['path'] for e in entries)
        if not nested:
            admit(tree['sha'], entries)
        else:
            children = {}
            for e in entries:
                safe_path(e['path'])
                parent, _, name = e['path'].rpartition('/')
                children.setdefault(parent, []).append({**e, 'path': name})
            admit(tree['sha'], children.get('', []))
            for e in entries:
                if e['type'] == 'tree':
                    admit(e['sha'], children.get(e['path'], []))
        observations.append({'origin': origin, 'sha': tree['sha'], 'entries': len(entries), 'recursive': nested})

    for sha, tree in metadata['trees'].items():
        assert tree['sha'] == sha
        expand(tree, 'cached-content-addressed-object')
    for scope in metadata['leaf_reads']:
        prefix = scope['path'] + '/'
        rows = [{**e, 'path': path[len(prefix):]} for path, e in metadata['leaf_entries'].items()
                if path.startswith(prefix)]
        assert len(rows) == scope['entries']
        expand({'sha': scope['tree'], 'tree': rows, 'truncated': scope['truncated']}, scope['path'])
    expand(docs['subtree'], 'independent-document-absence-readback')
    return trees, observations


def resolve(trees, root, path):
    parts = safe_path(path)
    current = root
    trail = []
    for i, name in enumerate(parts):
        assert current in trees, f'Incomplete tree observation at {"/".join(parts[:i])}'
        entries = {e['path']: e for e in trees[current]}
        entry = entries.get(name)
        trail.append({'parent': current, 'name': name, 'entry': entry})
        if entry is None:
            return {'state': 'ABSENT', 'first_missing_path': '/'.join(parts[:i + 1]), 'trail': trail}
        if i == len(parts) - 1:
            return {'state': 'PRESENT', 'entry': entry, 'trail': trail}
        assert entry['type'] == 'tree', f'Path ancestor is not a tree: {"/".join(parts[:i + 1])}'
        current = entry['sha']
    raise AssertionError('Unreachable')


def main():
    if not __debug__:
        raise RuntimeError('Optimized Python disables required base-tree admission assertions')
    metadata, additions, docs, input_seals = load_inputs()
    trees, observations = normalize_trees(metadata, docs)
    checked = []
    seen = set()
    for row in additions['rows']:
        path = row['path']
        assert path not in seen
        seen.add(path)
        before = resolve(trees, BASE_TREE, path)
        original = resolve(trees, DELIVERED_TREE, path)
        assert before['state'] == 'ABSENT' and row['current_preimage'] == 'ABSENT'
        assert original['state'] == 'PRESENT'
        old = original['entry']
        assert old['type'] == 'blob' and old['mode'] == row['mode']
        assert old['sha'] == row['git_blob'] and old['size'] == row['bytes']
        staged = Path(row['staged_path'])
        source = Path(row['local_path'])
        for p in (staged, source):
            assert p.is_file() and not p.is_symlink() and p.resolve().is_relative_to(TASK)
        raw = staged.read_bytes()
        assert raw == source.read_bytes(), f'Restored bytes differ from delivered artifact: {path}'
        assert len(raw) == row['bytes'] and digest(raw) == row['sha256'] and git_blob(raw) == row['git_blob']
        assert ('100755' if staged.stat().st_mode & 0o111 else '100644') == row['mode']
        text = raw.decode('utf-8')
        assert text.encode('utf-8') == raw and '\0' not in text
        checked.append({'path': path, 'mode': row['mode'], 'bytes': len(raw),
                        'sha256': row['sha256'], 'git_blob': row['git_blob'],
                        'first_missing_path': before['first_missing_path'],
                        'delivered_blob': old['sha']})
    assert len(checked) == 1039 and sum(r['bytes'] for r in checked) == 24321144
    model = {'schema': 'jcc-merge-publication-base-tree-model/1',
             'commit': BASE_COMMIT, 'root': BASE_TREE, 'delivered_root': DELIVERED_TREE,
             'inputs': input_seals, 'trees': trees}
    model_raw = json_bytes(model)
    exact_write(OUT / 'BASE_TREE_MODEL.json', model_raw)
    receipt = {'schema': 'jcc-merge-restoration-publication-audit/1', 'status': 'PASS',
               'base_commit': BASE_COMMIT, 'base_tree': BASE_TREE,
               'delivered_commit': 'd1cf2d81ee74a4a837f78e0cd2e4ccae2b3d4906',
               'delivered_tree': DELIVERED_TREE, 'inputs': input_seals,
               'tree_model_sha256': digest(model_raw), 'rehash_tree_objects': len(trees),
               'tree_observations': observations, 'files': len(checked),
               'bytes': sum(r['bytes'] for r in checked), 'rows': checked,
               'scope': 'Exact restored bytes and modes, delivered object membership, and current absence. No source edit, recipe execution, compiler/test acceptance, branch or PR mutation.'}
    receipt_raw = json_bytes(receipt)
    exact_write(OUT / 'RESTORATION_BASE_AUDIT.json', receipt_raw)
    print(json.dumps({'status': 'PASS', 'files': len(checked), 'bytes': receipt['bytes'],
                      'rehash_tree_objects': len(trees),
                      'receipt_sha256': digest(receipt_raw),
                      'model_sha256': digest(model_raw)}))


if __name__ == '__main__':
    main()
