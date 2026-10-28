#!/usr/bin/env python3
"""Hydrate unchanged pinned public donors and a separate receiving source snapshot."""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import hashlib
import json
import os
import subprocess
import time

WORK = Path(__file__).resolve().parent
REPO = WORK.parent / 'M3jdk21'
EVIDENCE = WORK / 'evidence'
EVIDENCE.mkdir(exist_ok=True)
DONORS = {
    'lucene': ('https://github.com/hsoliwal/lucene.git', '873b6167603c69101d626ddffd8d733340300fea'),
    'opennlp': ('https://github.com/hsoliwal/opennlp.git', '8b647d1d389736952e43fb46865c874747f7acde'),
}
PATHS = [
    'm3/tooling/module-packs/nlp-search',
    'm3/tooling/jmod-recipes',
    'm3/core/src',
    'm3/ports/compiletime/src',
    'm3/vendor/synexia/synexia-mindex/compiler',
    'm3/vendor/synexia/synexia-mindex/runtime',
    'm3/vendor/synexia/synexia-native-interop/src',
    'm3/vendor/synexia/synexia-openrewrite-recipes',
]

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def donor(name, url, revision):
    path = WORK / 'forks' / name
    if path.exists():
        raise FileExistsError(path)
    path.parent.mkdir(exist_ok=True)
    log = EVIDENCE / (name + '-fetch.log')
    commands = [['git', 'init', '-q', str(path)],
        ['git', '-C', str(path), 'remote', 'add', 'origin', url],
        ['git', '-C', str(path), 'fetch', '--depth=1', 'origin', revision],
        ['git', '-C', str(path), 'checkout', '--detach', 'FETCH_HEAD']]
    started = time.monotonic()
    with log.open('x') as stream:
        for cmd in commands:
            stream.write(json.dumps(cmd) + '\n'); stream.flush()
            result = subprocess.run(cmd, stdout=stream, stderr=subprocess.STDOUT,
                env=dict(os.environ, GIT_TERMINAL_PROMPT='0'), timeout=240)
            if result.returncode:
                raise RuntimeError(str(log) + ' exit=' + str(result.returncode))
    actual = subprocess.check_output(['git', '-C', str(path), 'rev-parse', 'HEAD'], text=True).strip()
    assert actual == revision, (name, actual)
    clean = subprocess.check_output(['git', '-C', str(path), 'status', '--porcelain'], text=True)
    assert clean == '', (name, clean)
    return {'repository': url, 'revision': revision, 'path': str(path),
        'tracked_clean': True, 'log_sha256': sha(log), 'seconds': round(time.monotonic()-started,3)}

def receiver():
    head = subprocess.check_output(['git', '-C', str(REPO), 'rev-parse', 'HEAD'], text=True).strip()
    dest = WORK / 'receiver-source'
    if dest.exists():
        raise FileExistsError(dest)
    log = EVIDENCE / 'receiver-hydration.log'
    commands = [['git', 'init', '-q', str(dest)],
        ['git','-C',str(dest),'remote','add','origin','https://github.com/hsoliwal/M3jdk21.git'],
        ['git','-C',str(dest),'sparse-checkout','init','--cone'],
        ['git','-C',str(dest),'sparse-checkout','set',*PATHS],
        ['git','-C',str(dest),'fetch','--depth=1','--filter=blob:none','origin',head],
        ['git','-C',str(dest),'checkout','--detach','FETCH_HEAD']]
    with log.open('x') as stream:
        for command in commands:
            stream.write(json.dumps(command)+'\n');stream.flush()
            result = subprocess.run(command, stdout=stream, stderr=subprocess.STDOUT,
                env=dict(os.environ, GIT_TERMINAL_PROMPT='0'), timeout=240)
            if result.returncode:
                raise RuntimeError(str(log) + ' exit=' + str(result.returncode))
    actual = subprocess.check_output(['git','-C',str(dest),'rev-parse','HEAD'],text=True).strip()
    assert actual == head, actual
    assert subprocess.check_output(['git','-C',str(dest),'status','--porcelain'],text=True) == ''
    files = sorted(dest / p for p in subprocess.check_output(
        ['git','-C',str(dest),'ls-files','-z']).decode().split('\0') if p and (dest / p).is_file())
    rows = [{'path': str(p.relative_to(dest)), 'sha256':sha(p), 'bytes':p.stat().st_size} for p in files]
    manifest = EVIDENCE / 'RECEIVER_SOURCE_FILES.json'
    manifest.write_text(json.dumps(rows,indent=2)+'\n')
    return {'repository':'hsoliwal/M3jdk21','revision':head,'snapshot':str(dest),
        'selected_paths':PATHS,'files':len(rows),'log_sha256':sha(log),
        'source_manifest_sha256':sha(manifest),'product_tree_modified':False}

with ThreadPoolExecutor(max_workers=3) as pool:
    tasks = {name:pool.submit(donor,name,*values) for name,values in DONORS.items()}
    tasks['receiver'] = pool.submit(receiver)
    result = {}
    for name,future in tasks.items():
        try:
            result[name] = {'status':'PASS',**future.result()}
        except Exception as exc:
            result[name] = {'status':'FAIL','error':repr(exc)}
receipt = EVIDENCE / 'SOURCE_HYDRATION.json'
receipt.write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result,indent=2),flush=True)
raise SystemExit(0 if all(v['status']=='PASS' for v in result.values()) else 1)
