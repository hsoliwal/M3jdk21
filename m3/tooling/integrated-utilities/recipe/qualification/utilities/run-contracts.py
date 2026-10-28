from pathlib import Path
import hashlib
import json
import os
import subprocess
import sys

root = Path(__file__).resolve().parent
jdk = Path(os.environ.get('M3_UTILITY_JAVA_HOME', '/tmp/m3-toolchain/jdk-21.0.12.1+1'))
source = root / 'postimages/src/java.base/share/classes'
classes = root / 'build/classes'
tests = root / 'build/tests'
classes.mkdir(parents=True, exist_ok=True)
tests.mkdir(parents=True, exist_ok=True)
commands = []
def run(arguments):
    command = [str(x) for x in arguments]
    result = subprocess.run(command, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    commands.append({'command': command, 'exit_code': result.returncode, 'output': result.stdout})
    print(result.stdout, end='', flush=True)
    if result.returncode:
        (root/'contract-results.json').write_text(json.dumps({'status': 'FAILED', 'commands': commands}, indent=2)+'\n')
        raise SystemExit(result.returncode)
    return result.stdout

run([jdk/'bin/java', '-version'])
run([jdk/'bin/javac', '-J-Xmx256m', '--patch-module', 'java.base='+str(source), '-d', classes,
     *sorted(source.rglob('*.java'))])
run([jdk/'bin/javac', '-J-Xmx256m', '--patch-module', 'java.base='+str(classes), '-d', tests,
     *sorted((root/'postimages/test/jdk').rglob('*.java'))])
for mode in [[], ['-Xint']]:
    for name in ['IOUtilsTest', 'StreamsTest', 'ReflectionTest']:
        run([jdk/'bin/java', '-Xmx256m', *mode, '--patch-module', 'java.base='+str(classes), '-cp', tests, name])
files = {str(p.relative_to(root/'postimages')): hashlib.sha256(p.read_bytes()).hexdigest()
         for p in sorted((root/'postimages').rglob('*')) if p.is_file()}
result = {'status': 'PASSED', 'scope': 'ISOLATED_STOCK_JAVA_21_PATCH_MODULE_CONTRACTS',
          'full_fork_image': 'PENDING', 'commands': commands, 'postimage_sha256': files}
(root/'contract-results.json').write_text(json.dumps(result, indent=2)+'\n')
print('Utility contracts passed in mixed and interpreter modes.', flush=True)
