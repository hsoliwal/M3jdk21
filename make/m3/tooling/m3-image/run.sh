#!/bin/bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT
mapfile -d '' sources < <(find "$here/src/main/java" -name '*.java' -type f -print0 | sort -z)
javac --release 21 -Xlint:all -Werror -d "$classes" "${sources[@]}"
exec java -cp "$classes" jdk.m3.tooling.image.M3ImageTool "$@"
