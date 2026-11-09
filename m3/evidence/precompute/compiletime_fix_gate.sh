#!/bin/bash
cd "$(dirname "$0")"
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH LANG=C.UTF-8 LC_ALL=C.UTF-8
M2=/mnt/s/m3-scratch/eb-conv/m2; V=9.6
ASM=$M2/org/ow2/asm/asm/$V/asm-$V.jar:$M2/org/ow2/asm/asm-tree/$V/asm-tree-$V.jar:$M2/org/ow2/asm/asm-analysis/$V/asm-analysis-$V.jar
F="M3ConstantRegexDifferential M3RegexRequirement M3SiteRecorder"
for cfg in "master:w_before:../rp/t" "fixed:w:../rp/t" "pre2828:w_before:w_before"; do
  IFS=: read n C P <<< "$cfg"; O=/tmp/ct2-$n; rm -rf $O; mkdir -p $O/p $O/c
  javac --release 21 -nowarn -d $O/p $(find $P/m3/ports/precompute/src/main/java -name '*.java' ! -name module-info.java) 2>/dev/null
  files=$(for f in $F; do echo $C/m3/ports/compiletime/src/main/java/jdk/m3/compiletime/$f.java; done)
  javac --release 21 -nowarn -implicit:none -sourcepath $C/m3/ports/compiletime/src/main/java -cp $O/p:$ASM -d $O/c $files > $O/log 2>&1
  echo "$n rc=$? errors=$(grep -c 'error:' $O/log) precompute-symbol-errors=$(grep -A2 'error:' $O/log | grep -cE 'jdk\.m3\.precompute|M3RegexShape|M3RegexOracle|M3Progress|RegexShape|RegexOracle|Progress\b')"
done
