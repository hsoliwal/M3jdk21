#!/bin/bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Atom census of one repository: run.sh <repo-label> <out-dir> <source-root>... (JDK 21; javac parser only).
set -eu
here=$(cd "$(dirname "$0")" && pwd)
classes=$(mktemp -d)
trap 'rm -rf "$classes"' EXIT
javac -d "$classes" "$here/src/main/java/jdk/m3/tooling/atoms/M3AtomCensus.java"
label=$1; out=$2; shift 2
java -cp "$classes" jdk.m3.tooling.atoms.M3AtomCensus "$label" "$here/PATTERN_RULES.tsv" "$out" "$@"
