#!/bin/bash
# gate_compression.sh <mode> <out> : runs the compression tests for one layout and prints one summary row.
#   mode=master : m3/compression as on master (package jdk.m3.compression, classpath, as its Maven module ran them)
#   mode=moved  : the moved layout (package jdk.internal.m3.compression inside java.base): main sources compiled into a
#                 java.base patch with -Xlint:all,-serial -Werror; the tests (same package, they use package-private
#                 types) patched in too; JUnit on the boot class path so patched java.base can read it.
# Both modes run every JUnit class and every *SelfTest main, without and with the opt-in JNI library (MinGW gcc, C11,
# the CMake warning set), and print totals so the two layouts can be compared.
set -u
export MSYS_NO_PATHCONV=1 PATH="/c/ProgramData/mingw64/mingw64/bin:$PATH"
MODE=$1; O=$2; rm -rf $O; mkdir -p $O/main $O/test $O/native; O=$(cd $O && pwd -W)
J="/c/Program Files/Java/jdk-21/bin"
JU=S:/m3-scratch/eb-conv/m2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar
INC='-IC:/Program Files/Java/jdk-21/include'; INC2='-IC:/Program Files/Java/jdk-21/include/win32'
W="-Wall -Wextra -Wconversion -Wsign-conversion -Wshadow -Wpedantic -Werror"
if [ "$MODE" = master ]; then
  R=m/m3/compression; PKG=jdk.m3.compression; MSRC=$R/src/main/java/jdk/m3/compression; TSRC=$R/src/test/java/jdk/m3/compression
  CSRC=$R/src/main/native/m3_lz_jni.c
  "$J/javac" --release 21 -Xlint:all,-serial -Werror -d $O/main $(ls $MSRC/*.java) > $O/javac_main.log 2>&1; echo "javac main rc=$?"
  "$J/javac" --release 21 -cp "$O/main;$JU" -d $O/test $(ls $TSRC/*.java) > $O/javac_test.log 2>&1; echo "javac test rc=$?"
  RUN=("$J/java" -cp "$O/test;$O/main;$JU")
else
  PKG=jdk.internal.m3.compression; MSRC=out/src/java.base/share/classes/jdk/internal/m3/compression
  TSRC=out/test/jdk/jdk/internal/m3/compression; CSRC=out/src/java.base/share/native/libm3compression/m3_lz_jni.c
  "$J/javac" -Xlint:all,-serial -Werror --patch-module java.base=out/src/java.base/share/classes -d $O/main \
    $(ls $MSRC/*.java) > $O/javac_main.log 2>&1; echo "javac main (java.base patch) rc=$?"
  "$J/javac" --patch-module "java.base=$O/main;out/test/jdk" --add-reads java.base=ALL-UNNAMED,java.management,jdk.management --add-modules java.management,jdk.management -cp "$JU" -d $O/test \
    $(ls $TSRC/*.java) > $O/javac_test.log 2>&1; echo "javac test (java.base patch) rc=$?"
  RUN=("$J/java" -Xbootclasspath/a:$JU --patch-module "java.base=$O/main;$O/test" --add-reads java.base=ALL-UNNAMED,java.management,jdk.management --add-modules java.management,jdk.management
       --add-opens java.base/$PKG=ALL-UNNAMED -cp "$JU")
fi
gcc -std=c11 -O2 -shared $W "$INC" "$INC2" -o $O/native/m3lz.dll $CSRC > $O/native/gcc.log 2>&1; echo "native rc=$?"
JUNIT=$(cd $TSRC && grep -l "@Test" *.java | sed "s/\.java$//; s/^/--select-class=$PKG./" | tr '\n' ' ')
MAINS=$(cd $TSRC && grep -l "static void main" *SelfTest.java | sed 's/\.java$//' | tr '\n' ' ')
for lib in none native; do
  P=(); [ $lib = native ] && P=(-Dm3.lz.library=$O/native/m3lz.dll)
  "${RUN[@]}" "${P[@]}" org.junit.platform.console.ConsoleLauncher execute $JUNIT --disable-banner --details=summary > $O/junit_$lib.log 2>&1
  printf 'JUNIT\t%s\t%s\n' $lib "$(grep -E 'tests (successful|failed|skipped|aborted)' $O/junit_$lib.log | tr -s ' []' ' ' | tr '\n' ' ')"
  ok=0; bad=0
  for m in $MAINS; do "${RUN[@]}" "${P[@]}" $PKG.$m > $O/main_${lib}_$m.log 2>&1 && ok=$((ok+1)) || { bad=$((bad+1)); echo "  FAIL $lib $m: $(tail -1 $O/main_${lib}_$m.log | cut -c1-120)"; }; done
  printf 'MAINS\t%s\tpass=%s fail=%s\n' $lib $ok $bad
done
