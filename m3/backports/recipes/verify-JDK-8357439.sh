#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  echo "usage: $0 <jcmd-completion-script>" >&2
  exit 64
fi

script="$1"
test -f "$script"
bash -n "$script"

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

cat > "$tmp/jcmd" <<'MOCK'
#!/usr/bin/env bash
set -euo pipefail
if [ "${1:-}" = "-l" ]; then
  cat <<'OUT'
111 p.Main
222 jdk.jcmd/sun.tools.jcmd.JCmd
333 p.Other
OUT
  exit 0
fi
if [ "${2:-}" = "help" ]; then
  cat <<'OUT'
The following commands are available:
help
Thread.print
VM.info
For more information about a specific command use 'help <command>'.
OUT
  exit 0
fi
exit 0
MOCK
chmod +x "$tmp/jcmd"
export PATH="$tmp:$PATH"

# shellcheck disable=SC1090
source "$script"

processes="$(_jcmd__list_processes)"
grep -qx '111 p.Main' <<<"$processes"
grep -qx '333 p.Other' <<<"$processes"
if grep -q 'jdk.jcmd/sun.tools.jcmd.JCmd' <<<"$processes"; then
  echo "jcmd must filter itself from process candidates" >&2
  exit 1
fi

commands="$(_jcmd__list_commands_for_jvm 111)"
grep -qx 'help' <<<"$commands"
grep -qx 'Thread.print' <<<"$commands"
grep -qx 'VM.info' <<<"$commands"

COMP_WORDS=(jcmd "")
COMP_CWORD=1
_jcmd_completion
printf '%s\n' "${COMPREPLY[@]}" | grep -qx -- '-l'
printf '%s\n' "${COMPREPLY[@]}" | grep -qx -- '--help'
printf '%s\n' "${COMPREPLY[@]}" | grep -qx '111'
printf '%s\n' "${COMPREPLY[@]}" | grep -qx 'p.Main'

COMP_WORDS=(jcmd 111 "")
COMP_CWORD=2
_jcmd_completion
printf '%s\n' "${COMPREPLY[@]}" | grep -qx -- '-f'
printf '%s\n' "${COMPREPLY[@]}" | grep -qx 'Thread.print'
printf '%s\n' "${COMPREPLY[@]}" | grep -qx 'VM.info'

COMP_WORDS=(jcmd 111 help "")
COMP_CWORD=3
_jcmd_completion
if printf '%s\n' "${COMPREPLY[@]}" | grep -qx 'help'; then
  echo "help completion must not suggest help recursively" >&2
  exit 1
fi
printf '%s\n' "${COMPREPLY[@]}" | grep -qx 'Thread.print'

touch "$tmp/commands.txt"
cd "$tmp"
COMP_WORDS=(jcmd 111 -f "comm")
COMP_CWORD=3
_jcmd_completion
printf '%s\n' "${COMPREPLY[@]}" | grep -qx 'commands.txt'

printf 'JDK-8357439 bash completion verification: PASS\n'
