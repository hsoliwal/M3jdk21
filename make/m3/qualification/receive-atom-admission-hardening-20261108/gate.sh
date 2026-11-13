#!/bin/bash
cd "$(dirname "$0")"
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 LANG=C.UTF-8 LC_ALL=C.UTF-8 PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH
for t in acm acm_u acm_m1 acm_m2; do
  for s in verify-contract verify-repository-census verify-jni-projection verify-jni-export-join; do
    timeout 250 bash $t/make/m3/tooling/atom-census/$s.sh > /tmp/$t-$s.log 2>&1; printf '%s\t%s\trc=%s\t%s\n' $t $s $? "$(tail -1 /tmp/$t-$s.log | cut -c1-140)"
    [ $t = acm_m1 -o $t = acm_m2 ] && break
  done
done
