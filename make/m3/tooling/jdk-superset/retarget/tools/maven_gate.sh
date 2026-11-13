#!/bin/bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# maven_gate.sh <master tree> <branch tree> <work dir> <donor> <published jar URL> : the Maven gate of a DONOR_OVERLAY
# recipe (JDK 21, mvn and python3 on PATH). Builds m3/vendor/third_party/<donor>/pom.xml from copies of both trees,
# runs the branch's verify_source.py with the branch jar, and compares the branch jar entry by entry with the published
# jar and with the master jar. Prints one block per comparison; the caller's evidence names what may differ.
set -u
A=$1; B=$2; W=$3; DONOR=$4; URL=$5; V=m3/vendor/third_party
rm -rf "$W"; mkdir -p "$W/master/$V" "$W/branch/$V"
cp -r "$A/$V/$DONOR" "$W/master/$V/"; cp -r "$B/$V/$DONOR" "$W/branch/$V/"
mkdir -p "$W/branch/m3/tooling"; cp -r "$B/m3/tooling/." "$W/branch/m3/tooling/"  # the recipes verify_source.py loads
for side in master branch; do
  mvn -q -B -f "$W/$side/$V/$DONOR/pom.xml" clean package > "$W/mvn_$side.log" 2>&1
  echo "mvn $side $DONOR rc=$?"
done
jar_of() { ls "$W/$1/$V/$DONOR"/target/*-m3-source-1.jar; }
python3 -B "$W/branch/$V/$DONOR/verify_source.py" "$(jar_of branch)" | sed 's/"artifact_sha256": "[0-9a-f]*", //'
curl -sSfo "$W/published.jar" "$URL"
python3 - "$(jar_of branch)" "$W/published.jar" "$(jar_of master)" <<'PY'
import sys, zipfile
branch, published, master = (zipfile.ZipFile(p) for p in sys.argv[1:4])
for label, other in (("published", published), ("master", master)):
    na, nb = set(branch.namelist()), set(other.namelist())
    diff = sorted(n for n in na & nb if not n.endswith("/") and branch.read(n) != other.read(n))
    print(f"  vs {label}: {len(na)} entries, {len(nb)} there; only here: {' '.join(sorted(na - nb)) or '-'};"
          f" only there: {' '.join(sorted(nb - na)) or '-'}; differing: {' '.join(diff) or '-'}")
PY
