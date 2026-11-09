"""Validate the exact counted-bag correction inputs without importing repository code."""
import argparse
import hashlib
import json
from pathlib import Path

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('--root',type=Path,default=Path('.'))
    options=parser.parse_args()
    root=options.root.resolve()
    packet=Path(__file__).resolve().parent
    bindings=json.loads((packet/'source-bindings.json').read_text(encoding='utf-8'))
    for row in bindings:
        source=root/row['path']
        data=source.read_bytes()
        if len(data)!=row['bytes'] or hashlib.sha256(data).hexdigest()!=row['sha256']:
            raise SystemExit('source custody mismatch: '+row['path'])
    print(json.dumps({'source_custody':'PASS','files':len(bindings),'scope':'one counted owner iterator correction and its proof/receiving inputs'}))

if __name__=='__main__':
    main()
