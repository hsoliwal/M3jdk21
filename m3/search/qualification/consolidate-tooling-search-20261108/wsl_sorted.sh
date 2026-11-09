#!/bin/bash
cd "$(dirname "$0")"
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 LANG=C.UTF-8 LC_ALL=C.UTF-8 PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH
for s in verify verify-long-bounds; do bash tree/search/qualification/sorted-bounds/$s.sh > /tmp/$s.log 2>&1; printf '%s\trc=%s\t%s\n' $s $? "$(tail -1 /tmp/$s.log | cut -c1-120)"; grep -m1 -h "BOUNDS_TEST" /tmp/$s.log | cut -c1-120; done
