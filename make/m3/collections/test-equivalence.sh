#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 || ( $# -eq 2 && $2 != --stop-after-compile ) ]]; then
  echo "Usage: $0 <fresh-output-directory> [--stop-after-compile]" >&2
  exit 2
fi
root="$(cd "$(dirname "$0")/../../.." && pwd)"
if [[ -e $1 || -L $1 ]]; then
  echo "Refusing existing output path: $1" >&2
  exit 2
fi
mkdir -p "$1"/{tools,module,test,generated,logs}
work="$(cd "$1" && pwd)"
java_cmd="${M3_EQUALITY_JAVA:-java}"
compiler=("$java_cmd" "-Xmx${M3_EQUALITY_JAVAC_HEAP:-3g}" -m jdk.compiler/com.sun.tools.javac.Main)
flags=(-source 21 -target 21 -proc:none -g -encoding UTF-8 -Xlint:all,-module -Werror
       -implicit:none -Xprefer:source -XDignore.symbol.file=true)
printf 'stage\texit_code\n' > "$work/stages.tsv"
run_stage() {
  local stage="$1" rc log
  shift
  log="$work/logs/$stage.log"
  printf '%q ' "$@" > "$log"
  printf '\n' >> "$log"
  if "$@" >> "$log" 2>&1; then rc=0; else rc=$?; fi
  printf '%s\t%s\n' "$stage" "$rc" >> "$work/stages.tsv"
  cat "$log"
  if (( rc != 0 )); then exit "$rc"; fi
}

run_stage LINT-shell bash -n "$0"
run_stage LINT-inventory python3 - "$root" "$work" <<'PY'
import pathlib, subprocess, sys
root, work = map(pathlib.Path, sys.argv[1:])
roots = [root / 'src/jdk.m3.collections' / part / 'classes' for part in ('share', 'providers')]
tracked = subprocess.check_output(['git', '-C', str(root), 'ls-files', '--',
                                  *[str(p.relative_to(root)) for p in roots]], text=True).splitlines()
expected = {root / p for p in tracked if p.endswith('.java') and not p.endswith('/module-info.java')}
if not expected or any(not p.is_file() for p in expected):
    raise SystemExit('Complete tracked owning-module sources are required; sparse omissions are refused')
sources = sorted(expected | {p for r in roots for p in r.rglob('*.java') if p.name != 'module-info.java'})
quote = lambda p: '"' + str(p).replace('\\', '\\\\').replace('"', '\\"') + '"\n'
(work / 'module-sources.txt').write_text(quote(work / 'generated/module-info.java') + ''.join(map(quote, sources)))
descriptors = subprocess.check_output(['git', '-C', str(root), 'ls-files', '--',
                                      'src/*/share/classes/module-info.java'], text=True).splitlines()
modules = sorted({p.split('/')[1] for p in descriptors if len(p.split('/')) == 5})
if not modules:
    raise SystemExit('Actual JDK module inventory is required')
(work / 'modules.txt').write_text(','.join(modules))
print('OWNING_MODULE_SOURCE_COUNT=' + str(len(sources) + 1))
PY
run_stage COMPILE-toolchain "$java_cmd" -version
run_stage COMPILE-generator "${compiler[@]}" "${flags[@]}" -d "$work/tools" \
  "$root/make/jdk/src/classes/build/tools/module/GenModuleInfoSource.java"
run_stage COMPILE-descriptor "$java_cmd" -cp "$work/tools" build.tools.module.GenModuleInfoSource \
  -o "$work/generated/module-info.java" --modules "$(< "$work/modules.txt")" \
  --source-file "$root/src/jdk.m3.collections/share/classes/module-info.java" \
  "$root/src/jdk.m3.collections/providers/classes/module-info.java.extra"
run_stage COMPILE-module "${compiler[@]}" "${flags[@]}" -Xdoclint:all/protected,-missing \
  -d "$work/module" "@$work/module-sources.txt"
run_stage COMPILE-test "${compiler[@]}" "${flags[@]}" \
  --module-path "$work/module" --add-modules jdk.m3.collections -d "$work/test" \
  "$root/test/jdk/jdk/m3/collections/providers/EquivalenceFromFunctionsTest.java"
if [[ ${2:-} == --stop-after-compile ]]; then
  echo "M3_EQUIVALENCE_COMPILE_PASS output=$work"
  exit 0
fi
runtime=(--module-path "$work/module" --add-modules jdk.m3.collections -cp "$work/test" EquivalenceFromFunctionsTest)
run_stage TEST-default "$java_cmd" "${runtime[@]}"
run_stage TEST-receipt grep -Ex 'EQUIVALENCE_FROM_FUNCTIONS_PASS checks=[1-9][0-9]*' "$work/logs/TEST-default.log"
run_stage RUNTIME-interpreter "$java_cmd" -Xint "${runtime[@]}"
run_stage RUNTIME-tier1 "$java_cmd" -XX:TieredStopAtLevel=1 "${runtime[@]}"
echo 'M3_EQUIVALENCE_PASS configurations=3'
