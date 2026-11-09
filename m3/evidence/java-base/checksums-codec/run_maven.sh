#!/bin/bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# run_maven.sh <master tree> <branch tree> <work dir> : CHECKSUMS_TO_JDK v1 Maven gate (JDK 21 and mvn on PATH).
# Builds each commons-codec donor pom from copies of both trees, runs verify_source.py with the branch jar, and
# compares the branch jar entry by entry with the published jar (Maven Central) and with the master jar.
set -u
A=$1; B=$2; W=$3; V=m3/vendor/third_party; rm -rf "$W"; mkdir -p "$W/master/$V" "$W/branch" "$W/published"
cp -r "$B/." "$W/branch/"
compare() { python3 - "$@" <<'PY'
import sys, zipfile
a, b = (zipfile.ZipFile(p) for p in sys.argv[2:4])
na, nb = set(a.namelist()), set(b.namelist())
diff = sorted(n for n in na & nb if not n.endswith("/") and a.read(n) != b.read(n))
print(f"  vs {sys.argv[1]}: {len(na)} entries, {len(nb)} there; only here: {' '.join(sorted(na - nb)) or '-'};"
      f" only there: {' '.join(sorted(nb - na)) or '-'}; differing: {' '.join(diff)}")
if sys.argv[1] == "published":
    print("  CRC tables left in any class:", any(b"\xf2\x6b\x83\x03" in a.read(n) or b"\x77\x07\x30\x96" in a.read(n)
                                                 for n in na if n.endswith(".class")))
PY
}
for v in 1.17.1 1.22.1; do
  cp -r "$A/$V/commons-codec-$v" "$W/master/$V/"
  for side in master branch; do
    mvn -q -B -f "$W/$side/$V/commons-codec-$v/pom.xml" clean package > "$W/mvn_${side}_$v.log" 2>&1
    echo "mvn $side commons-codec $v rc=$?"
  done
  J=$W/branch/$V/commons-codec-$v/target/commons-codec-$v-m3-source-1.jar
  python3 -B "$W/branch/$V/commons-codec-$v/verify_source.py" "$J" | sed 's/"artifact_sha256": "[0-9a-f]*", //'
  P=$W/published/commons-codec-$v.jar
  curl -sSfo "$P" "https://repo.maven.apache.org/maven2/commons-codec/commons-codec/$v/commons-codec-$v.jar"
  compare published "$J" "$P"
  compare master "$J" "$W/master/$V/commons-codec-$v/target/commons-codec-$v-m3-source-1.jar"
done
