import argparse,hashlib,json
from pathlib import Path
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[3]
spec=json.loads((HERE/'source-provenance.json').read_text())
parser=argparse.ArgumentParser();parser.add_argument('--jar');parser.add_argument('--args')
args=parser.parse_args()
for row in spec['sources']:
    p=ROOT/row['path'];data=p.read_bytes()
    assert len(data)==row['bytes'] and hashlib.sha256(data).hexdigest()==row['sha256'],str(p)
if args.jar:
    p=Path(args.jar);data=p.read_bytes();expected=spec['provider']
    assert p.name==expected['basename'] and len(data)==expected['bytes'] and hashlib.sha256(data).hexdigest()==expected['sha256'],'provider custody'
if args.args:
    paths=[str((ROOT/row['path']).resolve()).replace('\\','/') for row in spec['sources'] if '/implementations/' in row['path']]
    Path(args.args).write_text('\n'.join('"'+p+'"' for p in paths)+'\n',encoding='utf-8')
print(json.dumps({'sources':len(spec['sources']),'source_custody':'PASS','provider_checked':bool(args.jar)}))
