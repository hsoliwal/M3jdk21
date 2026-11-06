#!/bin/bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Regenerate the ARRAY_MAP and ID_MAP members from their templates, then prove them (differential against Commons
# Collections 4.5.0 and java.util.LinkedHashMap, atoms, contracts, literals and the fixed point). Needs JDK 21 and
# Maven; run from anywhere.
set -eu
here=$(cd "$(dirname "$0")" && pwd)
out=$(mktemp -d)
trap 'rm -rf "$out"' EXIT
javac -d "$out" "$here/src/main/java/jdk/m3/collections/fill/M3ArrayMapFill.java"
for family in ARRAY_MAP:MEMBERS ID_MAP:ID_MEMBERS; do
  java -cp "$out" jdk.m3.collections.fill.M3ArrayMapFill "$here/templates/${family%%:*}.java.tmpl" \
    "$here/${family##*:}.tsv" "$here/../../implementations"
done
mvn -B -ntp -f "$here/pom.xml" test
