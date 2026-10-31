#!/bin/bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Regenerate the ARRAY_MAP members from the template, then prove them (differential against Commons Collections
# 4.5.0 and the fixed point). Needs JDK 21 and Maven; run from anywhere.
set -eu
here=$(cd "$(dirname "$0")" && pwd)
out=$(mktemp -d)
trap 'rm -rf "$out"' EXIT
javac -d "$out" "$here/src/main/java/jdk/m3/collections/fill/M3ArrayMapFill.java"
java -cp "$out" jdk.m3.collections.fill.M3ArrayMapFill "$here/templates/ARRAY_MAP.java.tmpl" "$here/MEMBERS.tsv" \
  "$here/../../implementations"
mvn -B -ntp -f "$here/pom.xml" test
