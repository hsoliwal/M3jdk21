# SPDX-License-Identifier: Apache-2.0
# Adapted from the proved Guava module/license helper.
import hashlib
import json
from pathlib import Path
import sys
import zipfile

HERE = Path(__file__).resolve().parent
spec = json.loads((HERE / 'source-bindings.json').read_text(encoding='utf-8'))
prefix = 'com/m3/collections/spring/M3LinkedCaseInsensitiveMap'
with zipfile.ZipFile(sys.argv[1]) as jar:
    owned = [name for name in jar.namelist() if name.endswith('.class')]
    if prefix + '.class' not in owned or any(not name.startswith(prefix) for name in owned):
        raise SystemExit('standalone owner classes differ or provider classes packaged')
    for name in owned:
        code = jar.read(name)
        if code[:4] != bytes.fromhex('cafebabe') or int.from_bytes(code[6:8], 'big') != 65:
            raise SystemExit('owned Java21 class missing: ' + name)
    for name in ['LICENSE.txt', 'NOTICE.txt', 'THIRD_PARTY_NOTICES.md']:
        expected = next(row for row in spec['sources'] if row['path'].endswith('spring-case-insensitive-map/' + name))
        data = jar.read('META-INF/' + name)
        if len(data) != expected['bytes'] or hashlib.sha256(data).hexdigest() != expected['sha256']:
            raise SystemExit('license custody differs: ' + name)
print(json.dumps({'module': 'PASS', 'ownedJava21Classes': len(owned), 'retainedLicenses': True,
    'scope': 'JDK-only standalone owner classes and exact three retained license resources.'}))
