#!/bin/bash
# usage: bench.sh <out.tsv> <sizes,comma> [owner...]  -- compile owned modules + bench once, then one JVM per (owner, size, impl)
S=S:/m3-scratch/collections/bench
W=K:/m3-work/agents/collections-superset/wt-dlb/m3/collections
J="C:/Program Files/Java/jdk-21/bin"
CP="$(tr -d '\r\n' < S:/m3-scratch/collections/javac-cp.txt)"
OUT=$1; SIZES=$2; shift 2
OWNERS="${*:-M3HashMultiset M3LinkedHashMultiset M3HashBag M3EnumMultiset M3ArrayListMultimap M3DualLinkedHashBidiMap M3MultiValueMapAdapter M3EnumHashBiMap M3EnumBiMap}"
rm -rf $S/classes && mkdir -p $S/classes
find $W/implementations/guava-array-list-multimap/src/main $W/implementations/guava-hash-multiset/src/main $W/implementations/apache-hash-bag/src/main \
  $W/implementations/guava-enum-hash-bi-map/src/main $W/implementations/guava-enum-bi-map/src/main $W/implementations/apache-dual-linked-hash-bidi-map/src/main \
  $W/implementations/spring-multi-value-map-adapter/src/main $W/implementations/guava-enum-multiset/src/main \
  $W/implementations/guava-linked-hash-multiset/src/main -name '*.java' > $S/main.txt
echo $W/qualification/optimization-pass-20261020/OwnedVsDonorBench.java >> $S/main.txt
"$J/javac" -J-Xmx512m --release 21 -Xlint:all -Werror -proc:none -cp "$CP" -d $S/classes @$S/main.txt > $S/javac.log 2>&1 || { echo COMPILE_FAIL; head -20 $S/javac.log; exit 2; }
JVM="-Xms3g -Xmx3g -XX:+UseSerialGC -XX:ErrorFile=$S/hs_err_%p.log"
echo "$JVM" > $S/jvm-flags.txt
rm -f $OUT
: > $S/run.log
for o in $OWNERS; do for n in ${SIZES//,/ }; do for i in donor owned; do
  "$J/java" $JVM -cp "$S/classes;$CP" OwnedVsDonorBench $OUT $n $o $i >> $S/run.log 2>&1 || echo "FAIL $o $n $i" >> $S/run.log
done; done; done
echo rc=0
