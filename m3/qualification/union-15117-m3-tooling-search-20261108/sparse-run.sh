#!/bin/bash
cd "$(dirname "$0")/.."; export PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH
P=src/main/java/jdk/m3/tooling/search
for v in stage stage_u; do
  o=/tmp/sp-$v; rm -rf $o; mkdir -p $o
  javac -d $o $v/m3/tooling/search/$P/M3SortedIntBounds.java $v/m3/tooling/search/$P/M3NativeSortedIntBounds.java sparse/SparseProbe.java
  printf '%s\t' $v; java -Xmx1g -cp $o SparseProbe
done
