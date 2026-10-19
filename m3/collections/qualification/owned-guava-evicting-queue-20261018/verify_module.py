import hashlib,json,sys,zipfile
from pathlib import Path
spec=json.loads((Path(__file__).resolve().parent/'source-bindings.json').read_text())
with zipfile.ZipFile(sys.argv[1]) as jar:
    code=jar.read('com/m3/collections/guava/M3EvictingQueue.class')
    if code[:4]!=bytes.fromhex('cafebabe') or int.from_bytes(code[6:8],'big')!=65:raise SystemExit('owned Java21 class missing')
    for name in ['LICENSE.txt','NOTICE.txt','THIRD_PARTY_NOTICES.md']:
        expected=next(row for row in spec['sources'] if row['path'].endswith('guava-evicting-queue/'+name))
        data=jar.read('META-INF/'+name)
        if hashlib.sha256(data).hexdigest()!=expected['sha256']:raise SystemExit('license custody differs: '+name)
print(json.dumps({'module':'PASS','ownedJava21Class':True,'retainedLicenses':True}))
