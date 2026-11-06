#!/bin/bash
# gate2.sh : M3_SEARCH leaf-2 gates over out2 (leaf 1 + leaf 2). G2 main -Werror, G3 tests -Werror, G4 JUnit with
# every native library loaded (m3search engine + the four window/concat sidecars), G5 the same JUnit run without
# natives (Java-only fallback must stay green).
cd "$(dirname "$0")"
J="C:/Program Files/Java/jdk-21/bin"; M=$(cygpath -m ~/.m2/repository); W=$(cygpath -m "$PWD")
SP="m3deps/m3/collections/src/main/java;m3deps/m3/arrays/src/main/java;m3deps/m3/ports/file/src/main/java"
CON="$M/org/junit/platform/junit-platform-console-standalone/1.10.1/junit-platform-console-standalone-1.10.1.jar"
rm -rf c2m c2t; mkdir -p c2m c2t
find out2/m3/search/src/main/java -name '*.java' > m2.lst
"$J/javac" --release 21 -Xlint:all,-module -Werror -encoding UTF-8 -sourcepath "$SP" -d c2m @m2.lst > j2m.log 2>&1
echo "G2 main rc=$? diagnostics=$(grep -cE ': (error|warning):' j2m.log)"
find out2/m3/search/src/test/java -name '*.java' > t2.lst
"$J/javac" --release 21 -Xlint:all,-module -Werror -encoding UTF-8 -cp "c2m;$CON" -sourcepath "$SP" -d c2t @t2.lst > j2t.log 2>&1
echo "G3 test rc=$? diagnostics=$(grep -cE ': (error|warning):' j2t.log)"
NAT="-Dm3.native.enabled=true -Dm3.native.library.path=$W/nat/m3search.dll -Dm3.minimum.cover.native.junit=true"
NAT="$NAT -Dm3.minimum.cover.library=$W/nat/m3coverwindow.dll -Dm3.window.native.library=$W/nat/m3coverwindow.dll"
NAT="$NAT -Dm3.batch.subsequence.library=$W/nat/m3batchsubseq.dll -Dm3.concat.native=$W/nat/m3wordconcat.dll"
NAT="$NAT -Dm3.subsequence.native=$W/nat/m3subseqwindow.dll"
"$J/java" $NAT -jar "$CON" --class-path "c2m;c2t" --select-package jdk.m3.search --details=summary --disable-banner > junit2_native.log 2>&1
echo "G4 junit+native rc=$? $(grep -E 'tests (successful|failed|skipped|found)' junit2_native.log | tr -s ' ' | tr '\n' ' ')"
"$J/java" -jar "$CON" --class-path "c2m;c2t" --select-package jdk.m3.search --details=summary --disable-banner > junit2_java.log 2>&1
echo "G5 junit java-only rc=$? $(grep -E 'tests (successful|failed|skipped|found)' junit2_java.log | tr -s ' ' | tr '\n' ' ')"
