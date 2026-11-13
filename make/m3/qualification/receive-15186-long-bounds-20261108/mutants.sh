#!/bin/bash
cd "$(dirname "$0")"
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 LANG=C.UTF-8 LC_ALL=C.UTF-8 PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH
for m in m1 m2; do timeout 280 bash mut_l/$m/make/m3/tooling/search/verify-long-bounds.sh > logs_l/mut-$m.log 2>&1; rc=$?; printf 'MUTANT\t%s\t%s\t%s\n' $m "$([ $rc = 0 ] && echo SURVIVED || echo rejected rc=$rc)" "$(grep -m1 -iE 'fail|mismatch|expected|exception' logs_l/mut-$m.log | cut -c1-150)"; done
