#!/bin/bash
# order_gate.sh <master-sha> : NATIVE_ORDER_MONITOR_GATE v1 for m3/tooling/module-packs/text-tools.
# live = master's text-tools; fix = live with m3unixtext.c looking up "checkCancelled" (the java.base
# java.util.concurrent.ProgressMonitor method NativeLineBatch passes) instead of the donor's "checkCanceled".
# Per side: java.base patched with master's java.util.concurrent Progress*/Monitored* sources; main sources +
# the relocated NativeLineBatchOrderSelfTest compiled together (-h for JNI headers); every native/*.c built with
# gcc -std=c11 -Wall -Wextra -Werror; then the self-test runs with the side's natives. Expected: live FAIL
# (NoSuchMethodError from GetMethodID), fix PASS (M3_NATIVE_ORDER_PASS).
cd "$(dirname "$0")"
J="C:/Program Files/Java/jdk-21/bin"; JI="C:/Program Files/Java/jdk-21/include"; G=/h/m3-scratch/repos/M3jdk21-release.git
R=${1:?master sha}; T=m3/tooling/module-packs/text-tools
rm -rf ord; mkdir -p ord/jb/java/util/concurrent ord/jbcls
GIT_DIR=$G git ls-tree --name-only $R src/java.base/share/classes/java/util/concurrent/ | grep -E '/(Progress|Monitored)[A-Za-z]*\.java$' |
  while read -r f; do GIT_DIR=$G git show "$R:$f" > ord/jb/java/util/concurrent/$(basename $f); done
"$J/javac" -nowarn --patch-module java.base=ord/jb -d ord/jbcls $(find ord/jb -name '*.java') > ord/jb.log 2>&1 || { echo JB_FAIL; exit 1; }
for side in live fix; do
  W=ord/$side; mkdir -p $W/cls $W/nat $W/hdr
  GIT_DIR=$G git ls-tree -r --name-only $R -- $T/src $T/native | while read -r f; do
    d=$W/tt/${f#$T/}; mkdir -p "$(dirname "$d")"; GIT_DIR=$G git show "$R:$f" > "$d"; done
  [ $side = fix ] && sed -i 's/GetMethodID(env, monitorClass, "checkCanceled", "()V")/GetMethodID(env, monitorClass, "checkCancelled", "()V")/' $W/tt/native/m3unixtext.c
  find $W/tt/src -name '*.java' ! -name module-info.java > $W/src.lst; echo NativeLineBatchOrderSelfTest.java >> $W/src.lst
  "$J/javac" -source 21 -target 21 -nowarn -encoding UTF-8 --patch-module java.base=ord/jbcls -d $W/cls -h $W/hdr @$W/src.lst > $W/javac.log 2>&1
  echo "$side javac rc=$? errors=$(grep -c ': error:' $W/javac.log)"
  for c in $W/tt/native/*.c; do n=$(basename $c .c)
    gcc -shared -O2 -std=c11 -Wall -Wextra -Werror -I"$JI" -I"$JI/win32" -I$W/hdr -I$W/tt/native -o $W/nat/$n.dll "$c" > $W/nat/$n.log 2>&1
    echo "$side gcc $n rc=$?"; done
  (cd $W && timeout 300 "$J/java" --patch-module java.base=../jbcls -Djava.library.path=nat -cp cls jdk.m3.text.NativeLineBatchOrderSelfTest > run.log 2>&1)
  echo "$side run rc=$? $(grep -m1 -E 'M3_NATIVE_ORDER_PASS|Exception|Error' $W/run.log | cut -c1-160)"
done
