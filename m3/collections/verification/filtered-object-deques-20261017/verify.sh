#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
cd "$(dirname "$0")/../../../.."
classdir="$(mktemp -d)"
trap 'rm -rf "$classdir"' EXIT
javac --release 21 -proc:none -Xlint:all -Werror \
  --class-path "$classdir" --source-path "$classdir" -d "$classdir" \
  src/jdk.m3.collections/share/classes/jdk/m3/collections/SlotSpliterators.java \
  src/jdk.m3.collections/share/classes/jdk/m3/collections/FilteredDeques.java \
  m3/collections/verification/filtered-object-deques-20261017/M3FilteredDequeContract.java
java -ea -cp "$classdir" jdk.m3.collections.M3FilteredDequeContract
