import json,sys
from pathlib import Path
root=Path(sys.argv[1])
sealed=json.loads((Path(__file__).resolve().parent/'proof-summary.json').read_text())
expected=sealed['rootExecuted']['protocol']
provider=json.loads((root/'provider-protocol.json').read_text())
owned=json.loads((root/'owned-protocol.json').read_text())
if provider!=expected:raise SystemExit('current provider differs from frozen entire protocol result')
if owned!=provider:raise SystemExit('owned/provider entire protocol result differs')
surface=json.loads((root/'surface.json').read_text())
if surface!=sealed['rootExecuted']['publicSurface']:raise SystemExit('mapped public surface differs')
projection=sealed['rootExecuted']['projection']
for name in ['provider-projection.json','owned-projection-java.json','owned-projection-native.json']:
    actual=json.loads((root/name).read_text())
    if actual!=projection:raise SystemExit('entire primitive projection differs: '+name)
print(json.dumps({'status':'PASS','protocol':provider,'publicSurface':surface,'projection':projection,
                  'scope':'same finite corpora; mapped owner identity; existing real JNI primitive projection; no native queue ABI'}))
