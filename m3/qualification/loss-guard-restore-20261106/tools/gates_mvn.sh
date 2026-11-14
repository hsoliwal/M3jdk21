#!/bin/bash
set -u
WT=/s/m3-scratch/eb-conv/wt-restore; G=/s/m3-scratch/eb-conv/restore/gateWT
export JAVA_HOME="C:/Program Files/Java/jdk-21"
log() { echo "$(date +%H:%M:%S) $*" >> $G/mvn.log; }
: > $G/mvn.log; log start
cd $WT/m3/collections/qualification/array-map-fill-20261031 && mvn -q -B -ntp -Dmaven.repo.local=S:/m3-scratch/eb-conv/m2 test > $G/mvn_array_map_fill.log 2>&1; log "array_map_fill mvn test rc=$? $(grep -E 'Tests run:' $G/mvn_array_map_fill.log | tail -1)"
if cd $WT/m3/collections/qualification/jdk21-array-only-entry-facades-20261018/recipe 2>/dev/null; then mvn -q -B -ntp -Dmaven.repo.local=S:/m3-scratch/eb-conv/m2 test-compile > $G/mvn_entry_facades.log 2>&1; log "entry_facades mvn test-compile rc=$? $(grep -cE 'ERROR' $G/mvn_entry_facades.log) error lines"; else log "entry_facades recipe dir not checked out"; fi
log end
