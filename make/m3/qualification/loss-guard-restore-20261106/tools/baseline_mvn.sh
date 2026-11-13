#!/bin/bash
set -u
export JAVA_HOME="C:/Program Files/Java/jdk-21"
B=/s/m3-scratch/eb-conv/wt-base; G=/s/m3-scratch/eb-conv/restore/gateWT
log() { echo "$(date +%H:%M:%S) $*" >> $G/baseline.log; }
: > $G/baseline.log; log start
cd $B/m3/collections && mvn -B -ntp -Dmaven.repo.local=S:/m3-scratch/eb-conv/m2 test > $G/base_collections_test.log 2>&1; log "collections test rc=$? $(grep -E 'Tests run:.*Fail' $G/base_collections_test.log | tail -1)"
ls $B/m3/collections/target/surefire-reports/*.txt 2>/dev/null | sed 's#.*/##; s#\.txt$##' | sort > $G/base_tests.txt; log "test classes: $(wc -l < $G/base_tests.txt)"
grep -hE '^Tests run:' $B/m3/collections/target/surefire-reports/*.txt 2>/dev/null | awk -F'[:,]' '{r+=$2; f+=$4; e+=$6; s+=$8} END {print "baseline totals run="r" failures="f" errors="e" skipped="s}' >> $G/baseline.log
log end
