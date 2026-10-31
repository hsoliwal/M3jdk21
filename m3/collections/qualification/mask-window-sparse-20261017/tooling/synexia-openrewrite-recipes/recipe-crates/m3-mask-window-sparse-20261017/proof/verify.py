# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0
"""Repeat the selected Java/JNI proof. Toolchains/dependencies must already be available."""
from pathlib import Path
import argparse, hashlib, json, os, shutil, subprocess

parser = argparse.ArgumentParser()
parser.add_argument('--output', required=True)
parser.add_argument('--maven', default='mvn')
parser.add_argument('--m2', required=True)
args = parser.parse_args()
crate = Path(__file__).resolve().parents[1]
out = Path(args.output).resolve()
out.mkdir(parents=True, exist_ok=False)
jdk = Path(os.environ['JAVA_HOME'])
env = os.environ.copy()
env['PATH'] = str(jdk/'bin') + os.pathsep + env['PATH']
env['UBSAN_OPTIONS'] = 'halt_on_error=1:print_stacktrace=1'
events = []
def run(stage, command, expected=0):
    command = list(map(str, command))
    log = out/(stage+'.log')
    with log.open('w') as stream:
        process = subprocess.run(command, env=env, stdout=stream, stderr=subprocess.STDOUT)
    events.append(dict(stage=stage, command=command, exit=process.returncode, expected=expected,
                       logSha256=hashlib.sha256(log.read_bytes()).hexdigest()))
    (out/'events.json').write_text(json.dumps(events, indent=2)+'\n')
    if process.returncode != expected:
        raise SystemExit('HOLD: '+stage+'; see '+str(log))
    print('PASS', stage, flush=True)

def lint(paths):
    for p in paths:
        data=p.read_bytes()
        if not data.endswith(b'\n') or any(line.rstrip() != line for line in data.splitlines()):
            raise SystemExit('text lint: '+str(p))

resources=crate/'resources/com/synexia/rewrite'
before=out/'before'
before.mkdir()
for kind in ['java','text']:
    res=resources/('hash-pinned-'+kind)/'m3-mask-window-sparse-20261017'
    for line in (res/'manifest.tsv').read_text().splitlines():
        path, pre, post, name=line.split('\t')
        assert hashlib.sha256((res/name).read_bytes()).hexdigest()==post
        if pre != 'ABSENT':
            data=(res/('before-'+name)).read_bytes()
            assert hashlib.sha256(data).hexdigest()==pre
            dest=before/Path(path).name
            dest.write_bytes(data)
for line in (crate/'proof/FIXTURES.tsv').read_text().splitlines()[1:]:
    name, sha=line.split('\t')
    data=(crate/'proof/fixtures'/name).read_bytes()
    assert hashlib.sha256(data).hexdigest()==sha
    (before/name).write_bytes(data)
lint(list(before.iterdir())+list(crate.glob('src/test/**/*.java')))
baseline=out/'baseline';baseline.mkdir()
run('01-baseline-compile',['javac','--release','21','-proc:none','-Xlint:all','-Werror','-d',baseline,*before.glob('*.java')])
base=[args.maven,'-o','-B','-ntp','-f',crate/'pom.xml','-Dmaven.repo.local='+str(Path(args.m2).resolve()),'-Dcanonical.classpath='+str(baseline)]
run('02-recipe-compile',base+['test-compile'])
run('03-recipe-test',base+['surefire:test','-Dmaterialize.dir='+str(out/'generated')])
generated=out/'generated/m3/collections'
lint(list(generated.rglob('*.java'))+list(generated.rglob('*.c')))
classes=out/'classes';classes.mkdir()
headers=out/'headers';headers.mkdir()
run('04-product-compile',['javac','--release','21','-proc:none','-Xlint:all','-Werror','-h',headers,'-d',classes,
    *generated.rglob('*.java'),before/'M3IndexMaskCompactor.java',before/'M3IndexMaskWindowSelfTest.java',before/'M3MaskNativeParityProof.java'])
c=generated/'src/main/native/collections/m3_index_mask_window.c'
common=['gcc','-std=c17','-O2','-Wall','-Wextra','-Werror','-pedantic','-fsanitize=undefined','-fno-sanitize-recover=all',
        '-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),'-include',headers/'com_m3_collections_M3IndexMaskWindow.h']
run('05-native-compile',common+['-fPIC','-shared',c,before/'m3_index_mask_compactor.c','-o',out/'libm3_collections_mask.so'])
run('06-region-compile',common+[c,generated/'src/test/native/m3_mask_window_regions.c','-o',out/'regions'])
run('07-region-test',[out/'regions'])
for name,arguments in [('M3MaskWindowSparseContract',['java']),('M3MaskWindowSparseContract',['native']),
                       ('M3IndexMaskWindowSelfTest',['--native']),('M3MaskNativeParityProof',[])]:
    run('08-'+name+'-'+''.join(arguments),['java','-Xcheck:jni','-Djava.library.path='+str(out),'-cp',classes,'com.m3.collections.'+name,*arguments])
run('09-missing-native',['java','-Xcheck:jni','-Djava.library.path='+str(out/'missing'),'-cp',classes,
                       'com.m3.collections.M3MaskWindowSparseContract','missing'])
print('Selected proof complete; full reactor/JDK/image/platform gates remain separate.')
