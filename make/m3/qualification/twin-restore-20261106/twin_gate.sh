#!/bin/bash
# twin_gate.sh : compile gates for the RETAINED_SUPERSET twin restores in m3/tooling/module-packs (live vs restored).
# parser pack: javac every src/main/java file of the pack (sourcepath = pack main); collections pack: javac the pack's
# test sources against pack main + m3/collections + m3/arrays + m3/ports/collections main sources and JUnit 5.
# Prints error counts for LIVE (master as is) and RESTORED (twins applied); RESTORED must not exceed LIVE.
cd "$(dirname "$0")"
J="C:/Program Files/Java/jdk-21/bin"; M=$(cygpath -m ~/.m2/repository)
CON="$M/org/junit/platform/junit-platform-console-standalone/1.10.1/junit-platform-console-standalone-1.10.1.jar"
for side in live rest; do
  R=tw/$side; P=$R/m3/tooling/module-packs
  find $P/parser/src/main/java -name '*.java' ! -name module-info.java > tw/$side-parser.lst
  rm -rf tw/$side-pcls; "$J/javac" --release 21 -nowarn -encoding UTF-8 -proc:none -d tw/$side-pcls @tw/$side-parser.lst > tw/$side-parser.log 2>&1
  echo "$side parser javac rc=$? errors=$(grep -c ': error:' tw/$side-parser.log) files=$(wc -l < tw/$side-parser.lst)"
  SP="$P/collections/src/main/java;$R/m3/collections/src/main/java;$R/m3/arrays/src/main/java;$R/m3/ports/collections/src/main/java"
  find $P/collections/src/test/java -name '*.java' > tw/$side-ctest.lst
  rm -rf tw/$side-ccls; "$J/javac" --release 21 -nowarn -encoding UTF-8 -proc:none -cp "$CON" -sourcepath "$SP" -d tw/$side-ccls @tw/$side-ctest.lst > tw/$side-ctest.log 2>&1
  echo "$side collections-pack tests javac rc=$? errors=$(grep -c ': error:' tw/$side-ctest.log) files=$(wc -l < tw/$side-ctest.lst)"
done
