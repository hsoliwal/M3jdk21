#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Refuses meaningful-name findings introduced on added M3-owned Java source lines.
set -euo pipefail
if [[ $# -lt 2 || $# -gt 4 ]]; then
  echo "usage: check-changed.sh <base-sha> <head-sha> [report.tsv] [--scheme=m3]" >&2
  exit 2
fi
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root="$(git -C "$here" rev-parse --show-toplevel)"
base="$1"
head="$2"
report="${3:-${TMPDIR:-/tmp}/m3-jdk-standard-changed-findings.tsv}"
scheme="${4:-}"
if [[ -n "$scheme" && "$scheme" != "--scheme=m3" ]]; then
  echo "unknown option: $scheme" >&2
  exit 2
fi
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mapfile -t main_sources < <(find "$here/src/main/java" -type f -name '*.java' | sort)
javac --release 21 -proc:none -Xlint:all -Werror -d "$work/classes" \
  "${main_sources[@]}"
args=(--root "$root" --base "$base" --head "$head" --report "$report")
if [[ -n "$scheme" ]]; then
  args+=("$scheme")
fi
java -cp "$work/classes" com.m3.compiler.standard.M3ChangedNameGate "${args[@]}"
