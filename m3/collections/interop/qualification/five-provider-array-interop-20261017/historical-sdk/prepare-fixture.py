from pathlib import Path
import hashlib, json, os, shutil, subprocess
workspace = Path(os.environ['GITHUB_WORKSPACE'])
historical = Path(os.environ['RUNNER_TEMP']) / 'm3-sdk-family-historical-20261017'
historical.mkdir(parents=True, exist_ok=False)
task = Path('m3/collections/recipes/sdk-family-names-20261017')
shutil.copytree(workspace / task, historical / task,
                ignore=shutil.ignore_patterns('target', 'evidence'))
resources = historical / task / 'src/main/resources/com/synexia/rewrite/sdk-family-names-20261017'
manifest = resources / 'manifest.tsv'
sha = lambda path: hashlib.sha256(path.read_bytes()).hexdigest()
assert sha(manifest) == '3c992a514044453bc5784abf862701743f784ef67f5cfc4ee076ac073179efd7'
commit = '4e29dc9d4141261e8763fbe9ffd6a20e017b1aff'
external = {
    'm3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java',
    'm3/collections/src/main/java/com/m3/collections/M3LongIndexProjection.java',
    'm3/collections/src/main/native/collections/m3_index_mask_compactor.c',
}
restored = set()
admitted = {}
for line in manifest.read_text().splitlines():
    name, before, after, template = line.split('\t')
    target = historical / name
    target.parent.mkdir(parents=True, exist_ok=True)
    if template != 'KEEP':
        source = resources / template
        assert sha(source) == after, ('historical template drift', name)
        shutil.copyfile(source, target)
    elif name in external:
        url = f'https://raw.githubusercontent.com/hsoliwal/M3jdk21/{commit}/{name}'
        subprocess.run(['curl', '--fail', '--location', '--retry', '3',
                        '--output', str(target), url], check=True)
        restored.add(name)
    else:
        assert name.startswith(str(task) + '/'), ('unexpected historical owner', name)
    assert sha(target) == after, ('historical source drift', name)
    if template == 'KEEP':
        assert before == after, ('changed historical KEEP row', name)
    admitted[name] = after
assert restored == external and len(admitted) == 19
proof = workspace / 'proof'
(proof / 'historical-sdk-fixture.json').write_text(json.dumps({
    'manifest_sha256': sha(manifest), 'external_source_commit': commit,
    'historical_postimages': admitted,
    'original_recipe_copied_without_source_edits': True,
}, indent=2) + '\n')
with open(os.environ['GITHUB_ENV'], 'a') as environment:
    environment.write(f'HISTORICAL_SDK_ROOT={historical}\n')
print('ADMITTED_HISTORICAL_POSTIMAGES', len(admitted))
