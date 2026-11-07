#!/bin/bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Commit-sealed, dependency-free repository FILE_CONTAINER census proof.
set -euo pipefail
here=$(cd "$(dirname "$0")" && pwd)
repository=$(git -C "$here" rev-parse --show-toplevel)
classes=$(mktemp -d)
docs=$(mktemp -d)
runs=$(mktemp -d)
trap 'rm -rf "$classes" "$docs" "$runs"' EXIT
mapfile -t sources < <({
  find "$here/src/main/java/jdk/m3/tooling/atoms/contract" -name '*.java' -print
  find "$here/src/main/java/jdk/m3/tooling/atoms/repository" -name '*.java' -print
  find "$here/src/test/java/jdk/m3/tooling/atoms/repository" -name '*SelfTest.java' -print
} | sort)
javac --release 21 -Xlint:all -Werror -d "$classes" "${sources[@]}"
mapfile -t main_sources < <(
  find "$here/src/main/java/jdk/m3/tooling/atoms/repository" -name '*.java' -print | sort
)
javadoc --release 21 -quiet -Werror -Xdoclint:all -classpath "$classes" -d "$docs" "${main_sources[@]}"
java -cp "$classes" jdk.m3.tooling.atoms.repository.M3RepositoryFileCensusSelfTest
java -cp "$classes" jdk.m3.tooling.atoms.repository.M3RepositoryFileCensus \
  "$repository" HEAD "$runs/first"
java -cp "$classes" jdk.m3.tooling.atoms.repository.M3RepositoryFileCensus \
  "$repository" HEAD "$runs/second"
for file in FILES.tsv ATOMS.tsv EVIDENCE.tsv REPOSITORY_CENSUS_RECEIPT.tsv; do
  if ! cmp -s "$runs/first/$file" "$runs/second/$file"; then
    echo "repository census fixed-point failure: $file" >&2
    diff -u "$runs/first/$file" "$runs/second/$file" | head -200 >&2 || true
    exit 1
  fi
done
target="$repository/build/m3-repository-file-census"
rm -rf "$target"
mkdir -p "$target"
cp "$runs/first/FILES.tsv" "$runs/first/ATOMS.tsv" "$runs/first/EVIDENCE.tsv" \
  "$runs/first/REPOSITORY_CENSUS_RECEIPT.tsv" "$target/"
printf 'M3_REPOSITORY_FILE_CENSUS_FIXED_POINT\tPASS\n'
cat "$target/REPOSITORY_CENSUS_RECEIPT.tsv"
