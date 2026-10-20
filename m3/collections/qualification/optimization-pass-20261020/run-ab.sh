#!/bin/bash
# usage: ab.sh <tag> <owner> <repo-relative-file>...
# Compiles $BASE (default HEAD) version of the files (before) and the working-tree version (after) into two class dirs, then
# alternates before/after JVM runs of the owned class (REPEATS pairs per size, default 5) so machine-load drift
# hits both alike. Writes bench/<tag>-before.tsv and -after.tsv (agg.sh: min time, median rest) plus .raw files.
set -e
W=K:/m3-work/agents/collections-superset/wt-dlb
S=S:/m3-scratch/collections
B=$S/bench
J="C:/Program Files/Java/jdk-21/bin"
CP="$(tr -d '\r\n' < $S/javac-cp.txt)"
REPEATS=${REPEATS:-5}
BASE=${BASE:-HEAD}
TAG=$1; OWNER=$2; shift 2
mkdir -p $S/ab-save/$TAG
compile() {
  rm -rf $B/classes-$1 && mkdir -p $B/classes-$1
  find $W/m3/collections/implementations/guava-array-list-multimap/src/main $W/m3/collections/implementations/guava-hash-multiset/src/main \
    $W/m3/collections/implementations/apache-hash-bag/src/main $W/m3/collections/implementations/guava-enum-hash-bi-map/src/main \
    $W/m3/collections/implementations/guava-enum-bi-map/src/main $W/m3/collections/implementations/apache-dual-linked-hash-bidi-map/src/main \
    $W/m3/collections/implementations/spring-multi-value-map-adapter/src/main $W/m3/collections/implementations/guava-enum-multiset/src/main \
    $W/m3/collections/implementations/guava-linked-hash-multiset/src/main -name '*.java' > $B/main-$1.txt
  echo $W/m3/collections/qualification/optimization-pass-20261020/OwnedVsDonorBench.java >> $B/main-$1.txt
  "$J/javac" -J-Xmx512m --release 21 -Xlint:all -Werror -proc:none -cp "$CP" -d $B/classes-$1 @$B/main-$1.txt > $B/javac-$1.log 2>&1 || { echo "COMPILE_FAIL $1"; head -20 $B/javac-$1.log; exit 2; }
}
for f in "$@"; do cp $W/$f $S/ab-save/$TAG/$(basename $f); git -C $W show $BASE:$f > $W/$f; done
compile before
for f in "$@"; do cp $S/ab-save/$TAG/$(basename $f) $W/$f; done
compile after
JVM="-Xms3g -Xmx3g -XX:+UseSerialGC -XX:ErrorFile=$B/hs_err_%p.log"
rm -f $B/$TAG-before.tsv.raw $B/$TAG-after.tsv.raw
for n in 100000 1000000; do for r in $(seq 1 $REPEATS); do for v in before after; do
  "$J/java" $JVM -cp "$B/classes-$v;$CP" OwnedVsDonorBench $B/$TAG-$v.tsv.raw $n $OWNER owned >> $B/ab-run.log 2>&1 || echo "FAIL $v $n" >> $B/ab-run.log
done; done; done
for v in before after; do
bash $S/agg.sh $B/$TAG-$v.tsv.raw > $B/$TAG-$v.tsv
done
paste <(cut -f4-10,12 $B/$TAG-before.tsv) <(cut -f6-10 $B/$TAG-after.tsv)
