#!/bin/bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# run_gate.sh <master tree> <branch tree> <out> [count] : JGIT_BASE64_TO_JDK v1 gate (JDK 21, mvn, python3, curl on PATH).
#  1. Installs the donor's two source-built dependencies (JavaEWAH, slf4j-api) from the master tree.
#  2. retarget/tools/maven_gate.sh: the donor pom on master and on the branch, verify_source.py with the branch jar,
#     and the branch jar entry by entry against the published jar and the master jar.
#  3. javac -Xlint:all,-options --release 8 over the donor sources, master and branch: errors and warnings.
#  4. JgitBase64Differential, 3 seeds, on the master jar and the branch jar: line by line.
#  5. JgitBase64Speed on the master jar and the branch jar.
set -u
A=$1; B=$2; O=$3; N=${4:-100000}; H=$(cd "$(dirname "$0")" && pwd); rm -rf "$O"; mkdir -p "$O"
V=m3/vendor/third_party; D=org.eclipse.jgit; JV=5.13.3.202401111512-r
URL=https://repo1.maven.org/maven2/org/eclipse/jgit/$D/$JV/$D-$JV.jar
java -version 2>&1 | head -1
mkdir -p "$O/deps"
for dep in JavaEWAH slf4j-api-2.0.12; do
  cp -r "$A/$V/$dep" "$O/deps/"
  mvn -q -B -f "$O/deps/$dep/pom.xml" clean install > "$O/mvn_install_$dep.log" 2>&1; echo "mvn install $dep rc=$?"
done
bash "$B/m3/tooling/jdk-superset/retarget/tools/maven_gate.sh" "$A" "$B" "$O/maven" "$D" "$URL"
CP="$(ls ~/.m2/repository/com/googlecode/javaewah/JavaEWAH/1.1.13-m3-source-1/*.jar):$(ls ~/.m2/repository/org/slf4j/slf4j-api/2.0.12-m3-source-1/*.jar)"
for side in master branch; do
  T=$A; [ $side = branch ] && T=$B; mkdir -p "$O/javac-$side"
  javac -proc:none -Xlint:all,-options --release 8 -encoding UTF-8 -cp "$CP" -d "$O/javac-$side" \
    $(find "$T/$V/$D/src/main/java" -name '*.java') > "$O/javac_$side.log" 2>&1
  echo "javac $side rc=$? errors=$(grep -c ': error: ' "$O/javac_$side.log") warnings=$(grep -c ': warning: ' "$O/javac_$side.log")" \
       "Base64.java warnings=$(grep -c 'util/Base64.java:.*warning' "$O/javac_$side.log")"
done
jar_of() { ls "$O/maven/$1/$V/$D"/target/*-m3-source-1.jar; }
mkdir -p "$O/harness"
javac -d "$O/harness" -cp "$(jar_of master)" "$H/JgitBase64Differential.java" "$H/JgitBase64Speed.java"
echo "harness javac rc=$?"
for seed in 1 2 3; do
  for side in master branch; do
    java -cp "$O/harness:$(jar_of $side)" JgitBase64Differential $seed $N > "$O/diff_${side}_$seed.txt" 2>&1
  done
  m="$O/diff_master_$seed.txt"; b="$O/diff_branch_$seed.txt"
  echo "seed $seed: $N samples; lines $(wc -l < "$m")/$(wc -l < "$b"); differing $(diff "$m" "$b" | grep -c '^<')"
  paste -d'\t' "$m" "$b" | awk -F'\t' '$1 != $2' >> "$O/pairs.tsv"
done
echo "# lines per case kind and outcome, seed 1 branch (kind, ok/ex):"
awk '{print $1, $2 == "ok" || $3 == "ok" ? "ok" : "ex"}' "$O/diff_branch_1.txt" | sort | uniq -c
echo "# differing lines (master | branch):"
cut -c1-200 "$O/pairs.tsv" | sort | uniq -c
for side in master branch; do
  java -cp "$O/harness:$(jar_of $side)" JgitBase64Speed | sed "s/^/$side /"
done
