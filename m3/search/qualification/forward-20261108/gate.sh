#!/bin/bash
# gate.sh <tree> <out> : javac -Xlint:all -Werror main + tests of m3/search against the com.m3 dependency jars, then the
# JUnit console over every test class; prints the console summary.
set -u
export MSYS_NO_PATHCONV=1
T=$1; O=$2; rm -rf $O; mkdir -p $O/main $O/test; O=$(cd $O && pwd -W)
J="/c/Program Files/Java/jdk-21/bin"
M2=S:/m3-scratch/eb-conv/m2; R=S:/m3-scratch/eb-conv/reactor/m2
CP="$R/com/m3/m3-collections/0.1.0-SNAPSHOT/m3-collections-0.1.0-SNAPSHOT.jar;$M2/com/m3/m3-algorithms/0.1.0-SNAPSHOT/m3-algorithms-0.1.0-SNAPSHOT.jar;$M2/com/m3/m3-file/0.1.0-SNAPSHOT/m3-file-0.1.0-SNAPSHOT.jar"
JU="$M2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar"
bash "$(dirname "$0")/gate_native.sh" $T $O
find $T/search/src/main/java -name '*.java' > $O/main.txt
"$J/javac" --release 21 -Xlint:all -Xlint:-processing -Werror -encoding UTF-8 -cp "$CP" -d $O/main @$O/main.txt > $O/javac_main.log 2>&1; echo "javac main rc=$? $(grep -c 'error\|warning' $O/javac_main.log)"
find $T/search/src/test/java -name '*.java' > $O/test.txt
"$J/javac" --release 21 -encoding UTF-8 -cp "$O/main;$CP;$JU" -d $O/test @$O/test.txt > $O/javac_test.log 2>&1; echo "javac test rc=$? $(grep -c 'error' $O/javac_test.log)"
D=$O/native; "$J/java" -Dm3.native.enabled=true -Dm3.native.library.path=$D/m3search.dll -Dm3.minimum.cover.native.junit=true -Dm3.minimum.cover.library=$D/m3coverwindow.dll -Dm3.window.native.library=$D/m3coverwindow.dll -Dm3.batch.subsequence.library=$D/m3batchsubseq.dll -Dm3.concat.native=$D/m3wordconcat.dll -Dm3.pair.jni.library=$D/m3pairsum.dll -Dm3.subsequence.native=$D/m3subseqwindow.dll -Djava.io.tmpdir=S:/m3-scratch/prfix2/tmp -jar "$JU" execute --class-path "$O/test;$O/main;$CP" --scan-class-path --disable-banner --details=summary > $O/junit.log 2>&1; echo "junit rc=$?"
grep -E "tests (found|successful|failed|aborted|skipped)" $O/junit.log
