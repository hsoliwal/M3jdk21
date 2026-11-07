#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Synexia contributors
# SPDX-License-Identifier: Apache-2.0
# JDK21 host fixture; never interpreted as an M3JDK installed-image acceptance gate.
set -euo pipefail
root=$(cd "$(dirname "$0")" && pwd)
work=$(mktemp -d "${TMPDIR:-/tmp}/m3-cds-proof.XXXXXXXX")
classes="$work/classes"
mkdir -p "$classes"
trap 'rm -rf "$work"' EXIT
mapfile -t sources < <(find "$root/src/main/java" "$root/src/test/java" -name '*.java' -type f -print | sort)
javac --release 21 -Xlint:all -Werror -d "$classes" "${sources[@]}"
LANG=C.UTF-8 LC_ALL=C.UTF-8 java -cp "$classes" jdk.m3.tooling.cds.M3CdsTrainReplaySelfTest "$work"
