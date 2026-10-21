"""Adapt the owned exact-file recipe after frozen original and baseline evidence."""
import difflib
import gzip
import hashlib
import json
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
sys.path.insert(0, str(ROOT))
from storage_gate import check
UNIT = ROOT / 'm3-tree-empty-lookup-admission-20261019'
check(65536)

def sha(raw): return hashlib.sha256(raw).hexdigest()

def write(path, raw):
    assert sum(p.stat().st_size for p in UNIT.rglob('*') if p.is_file()) + len(raw) < 262144
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('xb') as stream:
        stream.write(raw)
        stream.flush()
        os.fsync(stream.fileno())

seal_raw = (UNIT / 'ORIGINAL_SEAL.json').read_bytes()
assert sha(seal_raw) == 'ad05e01d7ec2d165aa4ac0cea20c0bbe2a5172fc93b039cfe0e72b06b21d850a'
seal = json.loads(seal_raw)
assert seal['status'] == 'ORIGINAL_FROZEN_640' and not seal['candidateExecuted']
original_raw = gzip.decompress((UNIT/'original.stdout.jsonl.gz').read_bytes())
assert sha(original_raw) == seal['rawStdoutSha256']
baseline_path = Path(sys.argv[1]).resolve(strict=True)
baseline_path.relative_to(ROOT)
baseline_gzip = baseline_path.read_bytes()
assert sha(baseline_gzip) == sys.argv[2]
baseline_raw = gzip.decompress(baseline_gzip)
original = [json.loads(line) for line in original_raw.splitlines()]
baseline = [json.loads(line) for line in baseline_raw.splitlines()]
assert len(original) == len(baseline) == 642
assert original[-1] == baseline[-1] == {'kind':'complete','caseCount':640}
assert baseline[0]['provider'] == 'com.m3.collections.M3TreeMap'
baseline_class = ROOT/'m3-tree-put-validation-20261019/qualification-compile-v1/classes/com/m3/collections/M3TreeMap.class'
assert sha(baseline_class.read_bytes()) == baseline[0]['classSha256']
witness_id = 'NATURAL/FRESH/ROOT/ALWAYS_THROW/get'
a = next(row for row in original[1:-1] if row['id'] == witness_id)
b = next(row for row in baseline[1:-1] if row['id'] == witness_id)
assert a['outcome'] == {'kind':'return','value':None} and a['events'] == []
assert b['outcome']['kind'] == 'throw' and b['outcome']['markerIdentity'] and len(b['events']) == 1
admission = json.loads((UNIT/'BEFORE_IMPLEMENTATION.json').read_bytes())
before = Path(admission['source']).read_bytes()
assert sha(before) == admission['sourceSha256']
old = b'            // No bounds or visited key can validate a natural-order empty lookup.\n            if (tree.comparator == null) { checked(key); }\n'
new = b'            // Empty natural lookup checks null/type without comparing a key.\n            if (tree.comparator == null) { Objects.requireNonNull((Comparable<?>) key); }\n'
assert before.count(old) == 1
after = before.replace(old, new)
assert after.replace(new, old) == before
recipe = {'schema':'m3-exact-file-behavior-atom-recipe/v1',
    'sourceRepository':'hsoliwal/M3jdk21','sourceCommit':admission['baseCommit'],
    'file':'m3/collections/src/main/java/com/m3/collections/M3TreeMap.java',
    'preimageSha256':sha(before),'postimageSha256':sha(after),
    'preimageGitBlob':hashlib.sha1(b'blob '+str(len(before)).encode()+b'\0'+before).hexdigest(),
    'postimageGitBlob':hashlib.sha1(b'blob '+str(len(after)).encode()+b'\0'+after).hexdigest(),
    'before':old.decode(),'after':new.decode(),'occurrences':1,
    'atom':'empty natural unbounded lookup null/type admission without comparison',
    'originalSealSha256':sha(seal_raw),'baselinePath':str(baseline_path),
    'baselineGzipSha256':sha(baseline_gzip),'baselineRawSha256':sha(baseline_raw),
    'baselineWitness':b,'originalWitness':a,'publicInterfacesUnchanged':True,
    'allOtherSourceBytesUnchanged':True,'noOpenJdkSourceImported':True}
write(UNIT/'TRANSFORMATION_RECIPE.json',(json.dumps(recipe,indent=2)+'\n').encode())
write(UNIT/'src/main/java/com/m3/collections/M3TreeMap.java',after)
write(UNIT/'M3TreeMap.empty-lookup.diff',''.join(difflib.unified_diff(before.decode().splitlines(keepends=True),
      after.decode().splitlines(keepends=True),fromfile='published/M3TreeMap.java',tofile='candidate/M3TreeMap.java')).encode())
def apply(raw):
    if sha(raw) == sha(after): return raw,'ALREADY_APPLIED'
    if sha(raw) != sha(before): raise ValueError('unrecognized preimage')
    assert raw.count(old) == 1
    result = raw.replace(old,new)
    assert sha(result) == sha(after)
    return result,'APPLIED'
assert apply(before) == (after,'APPLIED') and apply(after) == (after,'ALREADY_APPLIED')
refused = []
for mutated in [before+b' ', after+b' ', b'']:
    try: apply(mutated)
    except ValueError: refused.append(sha(mutated))
    else: raise AssertionError('mutated input admitted')
proof = {'schema':'m3-exact-file-recipe-proof/v1','status':'PASS',
         'preimageSha256':sha(before),'postimageSha256':sha(after),'applyExact':True,
         'fixedPoint':True,'inversion':True,'refusedMutatedPreimages':refused,
         'sourceFileCount':1,'originalAndBaselineExecutedBeforeSourceWrite':True,
         'controllerSha256':sha(Path(__file__).read_bytes()),
         'donorControllers':['apply_m3_tree_put_validation_v1.py','prove_tree_put_recipe_v1.py']}
write(UNIT/'RECIPE_EXECUTION_V1.json',(json.dumps(proof,indent=2)+'\n').encode())
print(json.dumps({'status':'APPLIED_EXACT_ATOM','postimageSha256':sha(after),
                  'postimageGitBlob':recipe['postimageGitBlob'],'bytes':len(after),
                  'recipeProofSha256':sha((UNIT/'RECIPE_EXECUTION_V1.json').read_bytes())}))
