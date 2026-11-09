#!/bin/bash
# branch b gates: m3/collections package + test (compare with the master baseline), hash/topK/case-liang reruns
set -u
export JAVA_HOME="C:/Program Files/Java/jdk-21"
WT=/s/m3-scratch/eb-conv/wt-restore; G=/s/m3-scratch/eb-conv/restore/gateWT
log() { echo "$(date +%H:%M:%S) $*" >> $G/branch.log; }
: > $G/branch.log; log start
cd $WT/m3/collections && mvn -q -B -ntp -Dmaven.repo.local=S:/m3-scratch/eb-conv/m2 -DskipTests package > $G/branch_collections_package.log 2>&1; log "collections package -DskipTests rc=$? errors=$(grep -c 'ERROR' $G/branch_collections_package.log)"
mvn -B -ntp -Dmaven.repo.local=S:/m3-scratch/eb-conv/m2 test > $G/branch_collections_test.log 2>&1; log "collections test rc=$? $(grep -E 'Tests run:.*Fail' $G/branch_collections_test.log | tail -1)"
ls $WT/m3/collections/target/surefire-reports/*.txt 2>/dev/null | sed 's#.*/##; s#\.txt$##' | sort > $G/branch_tests.txt; log "test classes: $(wc -l < $G/branch_tests.txt)"
grep -hE '^Tests run:' $WT/m3/collections/target/surefire-reports/*.txt 2>/dev/null | awk -F'[:,]' '{r+=$2; f+=$4; e+=$6; s+=$8} END {print "branch totals run="r" failures="f" errors="e" skipped="s}' >> $G/branch.log
# hash / topK / case-liang: sections A-C of gates_wt.sh on the branch tree
sed -n '1,/^# D\. collections/p' /s/m3-scratch/eb-conv/restore/gates_wt.sh | sed 's#\$G/gate.log#$G/gate_b.log#' > $G/gates_abc.sh
bash $G/gates_abc.sh; log "gates A-C: $(grep -cE 'rc=0' $G/gate_b.log) rc=0 lines, $(grep -E 'rc=[1-9]' $G/gate_b.log | wc -l) failures"
cd $WT && log "worktree changes after gates: $(git status --short | grep -vE '^(R|M|A|\?\?) ' | wc -l)"
log end
