#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# usage: TMPDIR=<tmp> M2_REPO=<repo> JAVA_HOME=<jdk21> [MVN=<mvn>] wsl-profile.sh <out-dir> <archive.tar> [extra mvn args...]
#   archive.tar = git archive <commit> m3/collections/pom.xml m3/collections/src m3/arrays/pom.xml m3/arrays/src m3/native/include
# Unpacks the archive into <out-dir>/src; libm3_arrays.so by cmake
# (as #2577); mvn -o verify -Pm3-collections-native; testcase table <out-dir>/testcases.tsv. All writes under <out-dir>, $TMPDIR and $M2_REPO.
set -uo pipefail
OUT=$1; TAR=$2; shift 2
export JAVA_HOME=${JAVA_HOME:?JDK 21} PATH=$JAVA_HOME/bin:$PATH
: "${TMPDIR:?scratch tmp}"
export MAVEN_OPTS="-Djava.io.tmpdir=$TMPDIR"
MVN=${MVN:-mvn}
rm -rf "$OUT"; mkdir -p "$OUT/src" "$OUT/arrays-build"
tar -x -f "$TAR" -C "$OUT/src"
cmake -S "$OUT/src/m3/arrays/src/main/native" -B "$OUT/arrays-build" > "$OUT/cmake.log" 2>&1 && cmake --build "$OUT/arrays-build" >> "$OUT/cmake.log" 2>&1 || { echo "cmake failed"; exit 2; }
LIB=$(dirname "$(find "$OUT/arrays-build" -name 'libm3_arrays.so' | head -1)")
echo "libm3_arrays.so: $LIB"
cd "$OUT/src/m3/collections"
$MVN -B -ntp -o -Dmaven.repo.local="${M2_REPO:?maven repository}" \
  -Djava.io.tmpdir="$TMPDIR" "-DargLine=-Djava.io.tmpdir=$TMPDIR" verify -Pm3-collections-native -Dm3.collections.native.dir="$LIB" \
  -Dmaven.test.failure.ignore=true "$@" > "$OUT/mvn.log" 2>&1
echo "mvn rc=$? $(grep -E 'BUILD (SUCCESS|FAILURE)' "$OUT/mvn.log") $(grep -E 'Tests run:.*Skipped' "$OUT/mvn.log" | tail -1)"
python3 - "$OUT/src/m3/collections/target/surefire-reports" > "$OUT/testcases.tsv" <<'PY'
import glob, sys, xml.etree.ElementTree as ET
for f in sorted(glob.glob(sys.argv[1] + '/TEST-*.xml')):
    for tc in ET.parse(f).getroot().iter('testcase'):
        st = 'PASS'
        for k in ('failure', 'error', 'skipped'):
            if tc.find(k) is not None: st = k.upper()
        print('%s\t%s\t%s' % (tc.get('classname'), tc.get('name'), st))
PY
echo "testcases: $(wc -l < "$OUT/testcases.tsv") $(cut -f3 "$OUT/testcases.tsv" | sort | uniq -c | tr '\n' ' ')"
