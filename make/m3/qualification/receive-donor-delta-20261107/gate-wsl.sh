#!/bin/bash
# donor-delta gate: javac -Werror over the assembled latest postimages, then every test main (JDK 21, Linux).
cd "$(dirname "$0")/stage/m3/tooling/donor-delta"
export PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH LANG=C.UTF-8 LC_ALL=C.UTF-8
C=/tmp/donordelta_c; rm -rf $C; mkdir -p $C
javac --release 21 -Xlint:all -Werror -d $C $(find src -name '*.java') && echo "javac rc=0"
javadoc --release 21 -quiet -Werror -Xdoclint:all -d /tmp/donordelta_doc src/main/java/jdk/m3/tooling/donors/*.java > /tmp/dd_jd.log 2>&1; echo "javadoc rc=$? $(head -c 200 /tmp/dd_jd.log)"
for t in M3DonorDeltaPlannerSelfTest M3DonorTreeLanesSelfTest M3DonorTreeLanesGrowthTest M3DonorSortedFastPathProof; do
  echo "$t: $(java -cp $C jdk.m3.tooling.donors.$t 2>&1 | tail -1 | cut -c1-160) rc=${PIPESTATUS[0]}"
done
bash verify.sh 2>&1 | tail -1
