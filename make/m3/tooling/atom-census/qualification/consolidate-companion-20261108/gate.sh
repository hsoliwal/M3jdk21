#!/bin/bash
cd "$(dirname "$0")"
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 LANG=C.UTF-8 LC_ALL=C.UTF-8 PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH
A=am/atom-census
bash $A/verify-companion.sh > /tmp/vc.log 2>&1; echo "verify-companion rc=$?"; cut -c1-150 /tmp/vc.log
for s in verify-contract verify-jni-projection; do bash $A/$s.sh > /tmp/$s.log 2>&1; echo "$s rc=$? $(tail -1 /tmp/$s.log | cut -c1-100)"; done
c=$(mktemp -d); javac --release 21 -Xlint:all -Werror -d $c $(find $A/src/main/java -name '*.java') > /tmp/allmain.log 2>&1; echo "all atom-census main sources javac -Werror rc=$?"; head -3 /tmp/allmain.log
