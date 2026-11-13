#!/bin/bash
# P2-084 gate: atom-census contract (master) + repository-file-census + java-semantic postimages, JDK 21 Linux.
set -u
cd "$(dirname "$0")"
export PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH LANG=C.UTF-8 LC_ALL=C.UTF-8
R=m3jdk-repository-file-census-20261101/postimages/m3/tooling/atom-census/src
S=m3jdk-java-semantic-20261101/postimages/m3/tooling/atom-census/src
C=/tmp/p2084c; D=/tmp/p2084d; rm -rf $C $D; mkdir -p $C $D
javac --release 21 -Xlint:all -Werror -d $C $(find ac/m3/tooling/atom-census/src/main/java -name '*.java') \
  $R/main/java/jdk/m3/tooling/atoms/repository/*.java $S/main/java/jdk/m3/tooling/atoms/repository/*.java \
  $R/test/java/jdk/m3/tooling/atoms/repository/M3RepositoryFileCensusSelfTest.java $S/test/java/jdk/m3/tooling/atoms/repository/*.java && echo "javac rc=0"
javadoc --release 21 -quiet -Werror -Xdoclint:all -classpath $C -d $D $R/main/java/jdk/m3/tooling/atoms/repository/*.java $S/main/java/jdk/m3/tooling/atoms/repository/*.java > /tmp/p2084_jd.log 2>&1; echo "javadoc doclint rc=$? $(head -c 300 /tmp/p2084_jd.log)"
java -cp $C jdk.m3.tooling.atoms.repository.M3RepositoryFileCensusSelfTest 2>&1 | tail -2
java -cp $C jdk.m3.tooling.atoms.repository.M3JavaSemanticCensusSelfTest 2>&1 | tail -2
