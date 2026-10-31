#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
set -euo pipefail
base="$(realpath "$1")"
evidence="$(realpath -m "$2")"
crate="$(cd "$(dirname "$0")" && pwd)"
mkdir -p "$evidence"
cd "$crate"
java -version 2> "$evidence/java-version.txt"
mvn -version > "$evidence/maven-version.txt"
python3 prepare.py "$base" "$evidence/candidate" | tee "$evidence/prepare.log"
mvn -B -ntp -Dm3.base="$base" verify | tee "$evidence/maven-verify.log"
mvn -B -ntp -Dm3.base="$base" exec:java -Dexec.mainClass=com.synexia.rewrite.RegionFoundationReplay "-Dexec.args=$base $evidence/delivery" | tee "$evidence/replay.log"
python3 qualify.py "$base" "$evidence/delivery" "$evidence/runtime" | tee "$evidence/qualification.log"
cp postimages.tsv "$evidence/postimages.tsv"
cp -r target/surefire-reports "$evidence/surefire-reports"
find "$evidence" -type f ! -name SHA256SUMS -print0 | sort -z | xargs -0 sha256sum > "$evidence/SHA256SUMS"
