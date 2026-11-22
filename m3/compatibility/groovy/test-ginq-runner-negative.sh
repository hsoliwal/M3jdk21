#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
set -euo pipefail
runner="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/run-ginq-join-installed.sh"
if [[ ! -f "$runner" ]]; then
  echo 'missing installed-image runner' >&2
  exit 1
fi
scratch="$(mktemp -d)"
trap 'rm -rf "$scratch"' EXIT
fake="$scratch/faux-jdk"
mkdir -p "$fake/bin" "$fake/lib" "$fake/jmods"
printf 'not-a-real-runtime\n' > "$fake/lib/modules"
printf 'not-a-real-jmod\n' > "$fake/jmods/jdk.m3.collections.jmod"
cat > "$fake/bin/java" <<'JAVA'
#!/usr/bin/env bash
case "${1:-}" in
  --list-modules)
    printf '%s\n' 'java.base@21'
    if [[ "${M3_FAKE_MODE:-}" == prefix_collision ]]; then
      printf '%s\n' 'jdk.m3.collections.fake@21'
    elif [[ "${M3_FAKE_MODE:-}" != missing_module ]]; then
      printf '%s\n' 'jdk.m3.collections@21'
    fi
    ;;
  -XshowSettings:properties)
    printf '%s\n' '    java.home = /' >&2
    ;;
  *) exit 99 ;;
esac
JAVA
cat > "$fake/bin/javap" <<'JAVAP'
#!/usr/bin/env bash
printf '%s\n' 'public java.lang.String capitalize();' \
              'public java.lang.String uncapitalize();'
if [[ "${M3_FAKE_MODE:-}" != missing_api ]]; then
  printf '%s\n' 'public java.lang.String center(int, java.lang.String);'
fi
JAVAP
cat > "$fake/bin/javac" <<'JAVAC'
#!/usr/bin/env bash
exit 99
JAVAC
chmod +x "$fake/bin/java" "$fake/bin/javap" "$fake/bin/javac"
checks=0
expect_refusal() {
  local mode="$1" expected="$2" code
  local out="$scratch/$mode-output"
  set +e
  M3_FAKE_MODE="$mode" bash "$runner" "$fake" "$out" \
    > "$scratch/$mode.stdout" 2> "$scratch/$mode.stderr"
  code=$?
  set -e
  if [[ "$code" -ne "$expected" || -e "$out" ]]; then
    echo "bad $mode refusal: actual=$code expected=$expected" >&2
    cat "$scratch/$mode.stderr" >&2
    exit 1
  fi
  checks=$((checks+1))
}
mv "$fake/bin/java" "$scratch/temporarily-withheld-java"
expect_refusal missing_java 65
mv "$scratch/temporarily-withheld-java" "$fake/bin/java"
mkdir -p "$scratch/stale-output"
set +e
M3_FAKE_MODE=bad_home bash "$runner" "$fake" "$scratch/stale-output" \
  > "$scratch/stale.stdout" 2> "$scratch/stale.stderr"
code=$?
set -e
if [[ "$code" -ne 66 ]]; then echo "bad stale output refusal: $code" >&2; exit 1; fi
checks=$((checks+1))
expect_refusal missing_module 67
expect_refusal prefix_collision 67
mv "$fake/jmods/jdk.m3.collections.jmod" "$scratch/temporarily-withheld-jmod"
expect_refusal missing_jmod 71
mv "$scratch/temporarily-withheld-jmod" "$fake/jmods/jdk.m3.collections.jmod"
expect_refusal missing_api 72
expect_refusal bad_home 73
printf 'M3_GINQ_RUNNER_NEGATIVE_PASS checks=%d\n' "$checks"
