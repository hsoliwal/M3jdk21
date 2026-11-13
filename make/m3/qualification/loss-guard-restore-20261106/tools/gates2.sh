#!/bin/bash
# second gate pass: m3/collections main with m3/arrays on the sourcepath; array-map-fill lane via its run.sh
set -u
export MSYS_NO_PATHCONV=1
WT=/s/m3-scratch/eb-conv/wt-restore; WTW=S:/m3-scratch/eb-conv/wt-restore
G=/s/m3-scratch/eb-conv/restore/gateWT; GW=S:/m3-scratch/eb-conv/restore/gateWT
J="C:/Program Files/Java/jdk-21/bin"
CS=S:/m3-scratch/eb-conv/m2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar
API=S:/m3-scratch/eb-conv/m2/org/junit/jupiter/junit-jupiter-api/5.11.4/junit-jupiter-api-5.11.4.jar
log() { echo "$(date +%H:%M:%S) $*" >> $G/gate2.log; }
: > $G/gate2.log; log start
cd $WT && { git sparse-checkout list; echo /m3/collections/implementations/; echo /m3/arrays/src/main/java/; } | sort -u | git sparse-checkout set --no-cone --stdin && git sparse-checkout reapply; log "sparse reapplied: implementations=$(find m3/collections/implementations -type f 2>/dev/null | wc -l) arrays=$(find m3/arrays/src/main/java -type f 2>/dev/null | wc -l)"
rm -rf $G/coll2; mkdir -p $G/coll2/main $G/coll2/test
TS=$WT/m3/collections/src/test/java/jdk/m3/collections
"$J/javac" --release 21 -nowarn -d $G/coll2/main -sourcepath "$WTW/m3/arrays/src/main/java;$WTW/m3/collections/src/main/java" $(find $WT/m3/collections/src/main/java -name '*.java') > $G/coll2/javac_main.log 2>&1; log "coll2_main rc=$? classes=$(find $G/coll2/main -name '*.class' | wc -l) errors=$(grep -c 'error:' $G/coll2/javac_main.log)"
grep -E 'error:' $G/coll2/javac_main.log | sed 's#.*wt-restore[\\/]##' | cut -c1-160 > $G/coll2/main_errors.txt
"$J/javac" --release 21 -nowarn -d $G/coll2/main -sourcepath "$WTW/m3/arrays/src/main/java;$WTW/m3/collections/src/main/java" $(find $WT/m3/collections/src/main/java -name '*.java' | grep -v M3ApiEntryFacades.java) > $G/coll2/javac_main_noFacades.log 2>&1; log "coll2_main_without_M3ApiEntryFacades rc=$? classes=$(find $G/coll2/main -name '*.class' | wc -l) errors=$(grep -c 'error:' $G/coll2/javac_main_noFacades.log)"
"$J/javac" --release 21 -nowarn -d $G/coll2/test -cp "$GW/coll2/main;$API" $TS/M3DeepReadOnlyCollectionSelfTest.java $TS/M3DeepReadOnlyCollectionTest.java $TS/M3FilteredNavigationBoundarySelfTest.java $TS/M3FilteredNavigationBoundaryTest.java $TS/M3IndexPathMasksLongSelfTest.java $TS/M3IndexPathMasksLongTest.java $TS/M3ReadOnlyViewBoundarySelfTest.java $TS/M3ReadOnlyViewBoundaryTest.java $TS/M3LongHashSetAddAtomTest.java > $G/coll2/javac_test.log 2>&1; log "coll2_test rc=$? errors=$(grep -c 'error:' $G/coll2/javac_test.log)"
grep -E 'error:' $G/coll2/javac_test.log | sed 's#.*wt-restore[\\/]##' | cut -c1-160 | sort | uniq -c | sort -rn | head -20 > $G/coll2/test_errors.txt
for t in M3DeepReadOnlyCollectionSelfTest M3DeepReadOnlyCollectionTest M3FilteredNavigationBoundarySelfTest M3FilteredNavigationBoundaryTest M3IndexPathMasksLongSelfTest M3IndexPathMasksLongTest M3ReadOnlyViewBoundarySelfTest M3ReadOnlyViewBoundaryTest M3LongHashSetAddAtomTest; do "$J/javac" --release 21 -nowarn -d $G/coll2/test -cp "$GW/coll2/main;$API" $TS/$t.java > $G/coll2/javac_$t.log 2>&1; log "coll2_compile $t rc=$? errors=$(grep -c 'error:' $G/coll2/javac_$t.log)"; done
"$J/java" -ea -jar "$CS" execute -cp "$GW/coll2/main;$GW/coll2/test" --select-package jdk.m3.collections --include-classname ".*(DeepReadOnlyCollection|FilteredNavigationBoundary|IndexPathMasksLong|ReadOnlyViewBoundary|LongHashSetAddAtom).*Test" --details=summary --disable-banner > $G/coll2/junit.txt 2>&1; log "coll2_junit rc=$? $(grep -E 'tests (successful|failed)' $G/coll2/junit.txt | tr -s ' ' | tr '\n' ' ')"
for c in M3DeepReadOnlyCollectionSelfTest M3FilteredNavigationBoundarySelfTest M3IndexPathMasksLongSelfTest M3ReadOnlyViewBoundarySelfTest; do "$J/java" -ea -cp "$GW/coll2/main;$GW/coll2/test" jdk.m3.collections.$c > $G/coll2/$c.txt 2>&1; log "coll2_self $c rc=$? $(tail -1 $G/coll2/$c.txt | cut -c1-100)"; done
export JAVA_HOME="C:/Program Files/Java/jdk-21"; export MAVEN_OPTS="-Dmaven.repo.local=S:/m3-scratch/eb-conv/m2"
cd $WT/make/m3/collections/qualification/array-map-fill-20261031 && PATH="$J:$PATH" bash run.sh > $G/mvn_array_map_fill_run.log 2>&1; log "array_map_fill run.sh rc=$? $(grep -E 'Tests run:' $G/mvn_array_map_fill_run.log | tail -1 | cut -c1-120)"
cd $WT && log "worktree changes after run.sh: $(git status --short | wc -l)"
log end
