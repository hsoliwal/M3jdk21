#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/mock" "$tmp/pkgs/jtreg8/bin" "$tmp/pkgs/jtreg7/bin"
cat > "$tmp/mock/dpkg-query" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
case "$1" in
  -W) printf '%s' "${MOCK_VERSION:?}" ;;
  -L) printf '%s\n' "${MOCK_JTREG8_FILE:?}" ;;
  *) exit 12 ;;
esac
EOF
cat > "$tmp/mock/dpkg" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
[[ "$1" == '--compare-versions' && "$3" == ge ]]
[[ "${MOCK_VERSION%%+*}" == 8.3 || "${MOCK_VERSION%%+*}" == 8.2.1 ]]
EOF
cat > "$tmp/pkgs/jtreg8/bin/jtreg" <<'EOF'
#!/usr/bin/env bash
[[ "${1:-}" == '-version' ]] && { echo 'jtreg 8.3'; exit 0; }
exit 23
EOF
cat > "$tmp/pkgs/jtreg7/bin/jtreg" <<'EOF'
#!/usr/bin/env bash
[[ "${1:-}" == '-version' ]] && { echo 'jtreg 7.5'; exit 0; }
exit 23
EOF
chmod +x "$tmp/mock/dpkg-query" "$tmp/mock/dpkg" "$tmp/pkgs/jtreg8/bin/jtreg" "$tmp/pkgs/jtreg7/bin/jtreg"
export MOCK_VERSION='8.3+1+ds1-2~26.04.1'
export MOCK_JTREG8_FILE="$tmp/pkgs/jtreg8/bin/jtreg"
# Preserve system tools for sort/grep but select our package-owner simulators.
export PATH="$tmp/mock:$PATH"
actual="$(bash "$here/resolve-jtreg8.sh")"
[[ "$actual" == "$MOCK_JTREG8_FILE" ]] || exit 31
printf 'PASS package-owned jtreg8 path %s\n' "$actual"
# A preinstalled unrelated 'jtreg' must never be selected.
export MOCK_JTREG8_FILE="$tmp/pkgs/jtreg7/bin/unknown"
if bash "$here/resolve-jtreg8.sh" >"$tmp/unqualified.out" 2>"$tmp/unqualified.err"; then
  echo 'FAIL: selected an unqualified different package' >&2; exit 32
fi
[[ ! -s "$tmp/unqualified.out" ]] || exit 33
printf 'PASS unqualified JTReg7-or-missing launcher refused\n'
# Even a functional launcher cannot override a version too old for TEST.ROOT.
export MOCK_VERSION='8.1+1+ds1-1'
export MOCK_JTREG8_FILE="$tmp/pkgs/jtreg8/bin/jtreg"
if bash "$here/resolve-jtreg8.sh" >"$tmp/old.out" 2>"$tmp/old.err"; then
  echo 'FAIL: accepted JTReg below minimum' >&2; exit 34
fi
[[ ! -s "$tmp/old.out" ]] || exit 35
printf 'PASS old JTReg version refused\n'
# Nonexecutable package entry must fail closed.
export MOCK_VERSION='8.3+1+ds1-2~26.04.1'
chmod -x "$tmp/pkgs/jtreg8/bin/jtreg"
if bash "$here/resolve-jtreg8.sh" >"$tmp/nonexec.out" 2>"$tmp/nonexec.err"; then
  echo 'FAIL: selected nonexecutable launcher' >&2; exit 36
fi
[[ ! -s "$tmp/nonexec.out" ]] || exit 37
printf 'PASS nonexecutable package launcher refused\n'
echo 'M3LEX_JTREG8_LAUNCHER_SOURCE_TEST_PASS cases=4'
