#!/bin/bash
cd "$(dirname "$0")"
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 LANG=C.UTF-8 LC_ALL=C.UTF-8
export PATH=$JAVA_HOME/bin:$PATH
mkdir -p logs_l
for s in verify-long-bounds verify; do bash stage_l/m3/tooling/search/$s.sh > logs_l/$s.log 2>&1; printf 'GATE\t%s\trc=%s\t%s\n' $s $? "$(tail -1 logs_l/$s.log | cut -c1-150)"; done
grep -h "M3_SORTED_LONG_BOUNDS_TEST\|PRECONDITIONS" logs_l/verify-long-bounds.log | head -2
grep -h "OBSERVATION" logs_l/verify-long-bounds.log | cut -c1-260
