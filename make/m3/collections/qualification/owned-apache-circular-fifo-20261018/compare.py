import json,sys
from pathlib import Path
root=Path(sys.argv[1])
provider=json.loads((root/'provider.json').read_text())
for name in ['java.json','native.json']:
    candidate=json.loads((root/name).read_text())
    if candidate!=provider:raise SystemExit('provider/reference protocol differs: '+name)
before=json.loads((root/'provider-mutation.json').read_text())
after=json.loads((root/'owned-mutation.json').read_text())
if before!=after:raise SystemExit('provider-specific mutation/exception observations differ')
print(json.dumps({'status':'PASS','queueProtocol':provider,'concreteMutationObservations':before,'native':'real GCC-built JNI -Xcheck:jni'}))
