#!/bin/bash
# gate4.sh : M3_TEXT_TOOLS_CONVERGE tests leaf. Relocate the donor's src/test harnesses (merge_unix4j.relocate +
# package map), compile them with the converged text-tools main sources on the class path (java.base patched with
# this repository's own java.util.concurrent Progress*/Monitored* sources), build the main and test natives with
# gcc -Werror, then run every harness main() in its own JVM. PASS = exit 0. Writes g4/RESULTS.tsv.
cd "$(dirname "$0")"
J="C:/Program Files/Java/jdk-21/bin"; JI="C:/Program Files/Java/jdk-21/include"; TT=final/m3/tooling/module-packs/text-tools
W=g4; rm -rf $W; mkdir -p $W/jb/java/util/concurrent $W/jbcls $W/cls $W/nat $W/tsrc $W/hdr
cp g3-converged/jb/java/util/concurrent/*.java $W/jb/java/util/concurrent/
"$J/javac" -nowarn --patch-module java.base=$W/jb -d $W/jbcls $(find $W/jb -name '*.java') > $W/jb.log 2>&1 || { echo "JB_FAIL"; exit 1; }
python -I relocate_tests.py syntest $W/tsrc > $W/tests.tsv
find $TT/src -name '*.java' ! -name module-info.java > $W/src.lst
find $W/tsrc -name '*.java' >> $W/src.lst
M=$(cygpath -m ~/.m2/repository)
JU="$M/org/junit/platform/junit-platform-console-standalone/1.10.1/junit-platform-console-standalone-1.10.1.jar"
"$J/javac" -source 21 -target 21 -nowarn -encoding UTF-8 --patch-module java.base=$W/jbcls -cp "$JU" -d $W/cls -h $W/hdr @$W/src.lst > $W/javac.log 2>&1
echo "G1 tests javac rc=$? errors=$(grep -c ': error:' $W/javac.log)"
for c in $TT/native/*.c $W/tsrc/native/*.c; do
  [ -f "$c" ] || continue
  n=$(basename $c .c)
  gcc -shared -O2 -std=c11 -Wall -Wextra -Werror -I"$JI" -I"$JI/win32" -I$W/hdr -I$TT/native -o $W/nat/$n.dll "$c" > $W/nat/$n.log 2>&1
  echo "G2 gcc $n rc=$?"
done
gcc -O2 -std=c11 -Wall -Wextra -Werror -I$TT/native -o $W/nat/m3_utf8_kmp_selftest.exe $W/tsrc/c/m3_utf8_kmp_selftest.c > $W/nat/kmp.log 2>&1 \
  && { $W/nat/m3_utf8_kmp_selftest.exe > $W/kmp-run.log 2>&1; echo "G3 c-selftest m3_utf8_kmp rc=$?"; } || echo "G3 c-selftest build FAIL"
mkdir -p $W/nonative $W/wrongabi
cp $W/nat/m3textio.dll $W/nat/m3textops.dll $W/nat/m3structured.dll $W/wrongabi/ 2>/dev/null
cp $W/nat/m3unixaho_wrong_abi.dll $W/wrongabi/m3unixaho.dll; cp $W/nat/m3unixtext_wrong_abi.dll $W/wrongabi/m3unixtext.dll
printf 'class\targs\tenv\trc\tlast\n' > $W/RESULTS.tsv
while IFS=$'\t' read -r cls args env; do
  [ -n "$cls" ] || continue; [ "$args" = "-" ] && args=""
  lib=nat; [ "$env" = nonative ] && lib=nonative; [ "$env" = wrongabi ] && lib=wrongabi
  (cd $W && timeout 300 "$J/java" --patch-module java.base=jbcls -Djava.library.path=$lib -Dm3.structured.native=nat/m3structured.dll \
     -cp cls $cls $args > "run-${cls##*.}${args:+-$args}.log" 2>&1); rc=$?
  last=$(tail -1 "$W/run-${cls##*.}${args:+-$args}.log" | tr '\t' ' ' | cut -c1-120)
  printf '%s\t%s\t%s\t%s\t%s\n' "$cls" "$args" "$env" "$rc" "$last" >> $W/RESULTS.tsv
done < <(grep -v '^#' run_list.tsv)
echo "G4 harness runs=$(($(wc -l < $W/RESULTS.tsv)-1)) pass=$(awk -F'\t' 'NR>1 && $4==0' $W/RESULTS.tsv | wc -l)"
