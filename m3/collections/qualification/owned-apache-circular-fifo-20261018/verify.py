import argparse,hashlib,json
from pathlib import Path
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--root',type=Path,default=Path('.'));parser.add_argument('--jar',type=Path)
    args=parser.parse_args();root=args.root.resolve()
    spec=json.loads((Path(__file__).resolve().parent/'source-bindings.json').read_text())
    for row in spec['sources']:
        data=(root/row['path']).read_bytes()
        if len(data)!=row['bytes'] or hashlib.sha256(data).hexdigest()!=row['sha256']:raise SystemExit('source custody differs: '+row['path'])
    if args.jar:
        data=args.jar.read_bytes();expected=spec['provider']
        if len(data)!=expected['bytes'] or hashlib.sha256(data).hexdigest()!=expected['sha256']:raise SystemExit('provider custody differs')
    print(json.dumps({'source_custody':'PASS','sources':len(spec['sources']),'providerVerified':bool(args.jar)}))
if __name__=='__main__':main()
