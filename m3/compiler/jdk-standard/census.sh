#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Writes a deterministic parse-only warning census for M3-owned Java source files.
set -euo pipefail
if [[ $# -gt 2 ]]; then
  echo "usage: census.sh [report.tsv] [--scheme=m3]" >&2
  exit 2
fi
here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
root="$(git -C "$here" rev-parse --show-toplevel)"
report="${1:-${TMPDIR:-/tmp}/m3jdk-standard-census.tsv}"
scheme="${2:-}"
if [[ -n "$scheme" && "$scheme" != "--scheme=m3" ]]; then
  echo "unknown option: $scheme" >&2
  exit 2
fi
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mapfile -t main_sources < <(find "$here/src/main/java" -type f -name '*.java' | sort)
javac --release 21 -proc:none -Xlint:all -Werror -d "$work/classes" \
  "${main_sources[@]}"
args=(--root "$root" --report "$report")
if [[ -n "$scheme" ]]; then
  args+=("$scheme")
fi
java -cp "$work/classes" jdk.m3.compiler.standard.M3JdkStandardCensus "${args[@]}"
