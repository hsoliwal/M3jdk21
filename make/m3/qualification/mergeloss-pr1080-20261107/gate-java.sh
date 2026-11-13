#!/bin/bash
# Patch-module javac gate: master java.base tree + #1080 merged owners.
cd "$(dirname "$0")"
R=..; W=$R/m3w/src/java.base/share/classes; S=$R/resolved/src/java.base/share/classes
rm -rf merged out_merged && cp -r base merged
cp $S/java/lang/M3StringRegexDp.java merged/java/lang/M3StringRegexDp.java
cp $W/java/lang/M3StringFacts.java.merged merged/java/lang/M3StringFacts.java
cp $W/jdk/internal/mindex/M3RegexDpPlan.java.merged merged/jdk/internal/mindex/M3RegexDpPlan.java
J="C:/Program Files/Java/jdk-21/bin"; mkdir -p out_merged
"$J/javac" --patch-module java.base=merged -XDignore.symbol.file -Xlint:all,-serial,-deprecation,-removal,-this-escape -proc:none -implicit:class -d out_merged \
  merged/java/lang/M3StringRegexDp.java merged/java/lang/M3StringFacts.java merged/jdk/internal/mindex/M3RegexDpPlan.java > merged_javac.log 2>&1
echo "javac rc=$? errors=$(grep -c ': error:' merged_javac.log) warnings_owner=$(grep -E 'M3StringRegexDp|M3StringFacts|M3RegexDpPlan' merged_javac.log | grep -c warning)"
