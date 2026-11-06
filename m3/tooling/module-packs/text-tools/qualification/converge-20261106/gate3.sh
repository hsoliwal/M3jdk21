#!/bin/bash
# gate3.sh <text-tools-root> <label> : M3_TEXT_TOOLS_CONVERGE gates.
# G1 javac --release-free module build of jdk.m3.text/io/structured with java.base patched by M3jdk21's own
#    java.util.concurrent.Progress{Job,Monitor,Monitors} sources (the M3 java.base API M3File needs), -Xlint:all,-module -Werror.
# G2 gcc -Werror for every native/*.c (shared, JDK 21 headers).
cd "$(dirname "$0")"
TT=$1; L=$2; J="C:/Program Files/Java/jdk-21/bin"; JI="C:/Program Files/Java/jdk-21/include"
W=g3-$L; rm -rf $W; mkdir -p $W/jb/java/util/concurrent $W/cls $W/nat
for f in $(cd H:/m3-scratch/repos/M3jdk21-release.git && git ls-tree --name-only origin/master src/java.base/share/classes/java/util/concurrent/ | grep -E "/(Progress|Monitored)[A-Za-z]*.java$" | xargs -n1 basename | sed "s/.java$//"); do
  (cd H:/m3-scratch/repos/M3jdk21-release.git && git cat-file -p origin/master:src/java.base/share/classes/java/util/concurrent/$f.java) > $W/jb/java/util/concurrent/$f.java
done
find "$TT/src" -name '*.java' > $W/src.lst
find $W/jb -name '*.java' >> $W/src.lst
"$J/javac" -source 21 -target 21 -Xlint:all,-module -Werror -encoding UTF-8 --module-source-path "$TT/src" \
  --patch-module java.base=$W/jb -d $W/cls -h $W/hdr @$W/src.lst > $W/javac.log 2>&1
echo "G1 $L javac rc=$? diagnostics=$(grep -cE ': (error|warning):' $W/javac.log)"
for c in "$TT"/native/*.c; do
  n=$(basename $c .c)
  gcc -shared -O2 -std=c11 -Wall -Wextra -Werror -I"$JI" -I"$JI/win32" -I"$(find $W/hdr -type d | tr '\n' ' ' | cut -d' ' -f1)" $(find $W/hdr -mindepth 1 -type d -printf '-I%p ') -o $W/nat/$n.dll "$c" > $W/nat/$n.log 2>&1
  echo "G2 $L gcc $n rc=$? diagnostics=$(grep -cE 'error|warning' $W/nat/$n.log)"
done
