#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
# Deterministic, network-free JDK-8357439 Bash-completion contract smoke test.
set -euo pipefail

if (( $# > 1 )); then
    printf 'usage: %s [path-to-jcmd-bash-completion]\n' "$0" >&2
    exit 2
fi

script=${1:-src/jdk.jcmd/share/conf/bash-completion/jcmd}
if [[ ! -f "$script" ]]; then
    printf 'missing jcmd Bash completion: %s\n' "$script" >&2
    exit 2
fi
bash -n "$script"
script=$(cd "$(dirname "$script")" && pwd -P)/$(basename "$script")

tmp=$(mktemp -d)
trap 'rm -rf -- "$tmp"' EXIT

# The stub is deliberately a Bash function: no native JDK, Java process, or attach permission
# is required, and the source script must never consult a network or package manager.
M3_EMPTY_PROCESSES=0
M3_EMPTY_COMMANDS=0
jcmd() {
    if [[ ${1:-} == -l ]]; then
        if (( M3_EMPTY_PROCESSES == 0 )); then
            printf '%s\n' \
                '123 app.Main' \
                '456 demo.Main' \
                '123 app.Main' \
                '789 jdk.jcmd/sun.tools.jcmd.JCmd'
        fi
        return 0
    fi
    if [[ ${2:-} == help ]]; then
        if (( M3_EMPTY_COMMANDS == 0 )); then
            printf '%s\n' \
                "${1}:" \
                'The following commands are available:' \
                'VM.info' \
                'VM.version' \
                'GC.heap_info' \
                'help' \
                'For more information about a specific command use help <command>.'
        fi
        return 0
    fi
    printf 'unexpected stub jcmd call: %q %q\n' "${1:-}" "${2:-}" >&2
    return 63
}

source "$script"
complete -p jcmd | grep -Eq '(^| )-F _jcmd_completion( |$)'

fail() {
    printf 'JDK-8357439 completion smoke FAIL: %s\n' "$1" >&2
    exit 1
}
contains() {
    local candidate=$1
    local value
    for value in "${COMPREPLY[@]}"; do
        if [[ $value == "$candidate" ]]; then return 0; fi
    done
    return 1
}
expect_has() { contains "$1" || fail "missing suggestion: $1"; }
expect_lacks() { if contains "$1"; then fail "unexpected suggestion: $1"; fi; }
expect_once() {
    local needle=$1 value count=0
    for value in "${COMPREPLY[@]}"; do
        if [[ $value == "$needle" ]]; then ((count+=1)); fi
    done
    (( count == 1 )) || fail "expected exactly one $needle suggestion; got $count"
}
complete_at() {
    COMP_CWORD=$(($#-1))
    COMP_WORDS=("$@")
    if ! _jcmd_completion; then
        fail "jcmd completion callback failed"
    fi
}

# The PID, main-class and generic-option alternatives are all supplied exactly once.
complete_at jcmd 12
expect_once 123
expect_lacks 456
expect_lacks 789
complete_at jcmd app.
expect_once app.Main
expect_lacks demo.Main
complete_at jcmd ''
expect_once --help
expect_once -l
expect_once 123
expect_once 456
expect_lacks 789

# A real target JVM's diagnostic command list is filtered by the current token.
complete_at jcmd 123 VM.
expect_once VM.info
expect_once VM.version
expect_lacks GC.heap_info
expect_lacks -f
complete_at jcmd 123 GC.
expect_once GC.heap_info
expect_lacks VM.info
complete_at jcmd 123 ''
expect_once -f
expect_once VM.info
expect_once help

# `jcmd <pid> help <command>` excludes recursive help and suggests diagnostic commands.
complete_at jcmd 123 help VM.
expect_once VM.info
expect_once VM.version
expect_lacks GC.heap_info
complete_at jcmd 123 help ''
expect_has VM.info
expect_lacks help

# `-f` completes local files without running target JVM diagnostic commands.
printf 'VM.info\n' > "$tmp/m3-jcmd-script.commands"
pushd "$tmp" >/dev/null
complete_at jcmd 123 -f m3-jcmd-script.
expect_once m3-jcmd-script.commands
popd >/dev/null

# Unhandled argument positions must never generate invented suggestions.
complete_at jcmd 123 VM.info anything
(( ${#COMPREPLY[@]} == 0 )) || fail 'invented suggestions for unsupported arguments'

# Empty JVM enumeration and empty JVM diagnostic-command lists are valid.
M3_EMPTY_PROCESSES=1
complete_at jcmd ''
expect_once -l
expect_once --help
expect_lacks 123
M3_EMPTY_PROCESSES=0
M3_EMPTY_COMMANDS=1
complete_at jcmd 123 ''
expect_once -f
expect_lacks VM.info
M3_EMPTY_COMMANDS=0

printf 'PASS: JDK-8357439 jcmd completion (PID, command, help, file, dedupe, empty, negative)\n'
