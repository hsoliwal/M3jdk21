#!/bin/bash
# RX7 restore gate (#1381 LexicalPlan): the six #1381 jtreg contracts on a stock JDK 21, java.base patched with the
# jdk.internal.mindex classes compiled from master (side "master") or from the restored M3RX7 (side "restored").
# Native: master's libjava M3RX7.c (unchanged by the restore) built as a bootstrap-loaded DLL with MinGW -Werror.
cd "$(dirname "$0")"
J="C:/Program Files/Java/jdk-21/bin"; JI="C:/Program Files/Java/jdk-21/include"
G=/s/m3-scratch/converge-20261106/r1080/gate
export PATH="/c/ProgramData/mingw64/mingw64/bin:$PATH"
for side in master restored; do
  src=$([ $side = master ] && echo $G/merged || echo $G/rx)
  W=rx7w/$side; rm -rf $W; mkdir -p $W/all $W/p/jdk/internal $W/h $W/t
  "$J/javac" --patch-module java.base=$src -XDignore.symbol.file -nowarn -proc:none -implicit:class -h $W/h -d $W/all \
    $src/jdk/internal/mindex/M3RX7.java $src/jdk/internal/mindex/M3RX7Transitions.java > $W/javac.log 2>&1 || { echo "$side owner javac FAIL"; continue; }
  cp -r $W/all/jdk/internal/mindex $W/p/jdk/internal/
  mkdir -p $W/in/jdk/internal/mindex && cp rx7t/M3RX7NativeBootstrap.java rx7t/M3RX7ArrayPowerContract.java $W/in/jdk/internal/mindex/
  "$J/javac" --patch-module "java.base=$src;$W/in" -XDignore.symbol.file -nowarn -proc:none -implicit:none -d $W/p \
    $W/in/jdk/internal/mindex/M3RX7NativeBootstrap.java $W/in/jdk/internal/mindex/M3RX7ArrayPowerContract.java > $W/pjavac.log 2>&1 \
    || echo "$side in-package contracts javac FAIL ($(grep -c error: $W/pjavac.log) errors)"
  "$J/javac" --patch-module java.base=$W/p --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED -d $W/t \
    rx7t/M3RX7LexicalContract.java rx7t/M3RX7NativeBoundaryContract.java rx7t/M3RX7TransitionCompositionContract.java \
    rx7t/RX7FindConvergence.java > $W/tjavac.log 2>&1 || { echo "$side contracts javac FAIL ($(grep -c error: $W/tjavac.log) errors)"; continue; }
  gcc -std=c11 -O2 -Wall -Wextra -Werror -shared -I"$JI" -I"$JI/win32" -I$W/h -I/s/m3-scratch/converge-20261106/jbdrop/nat /s/m3-scratch/converge-20261106/jbdrop/nat/$([ $side = master ] && echo M3RX7.master.c || echo M3RX7.c) -o $W/m3rx7.dll \
    > $W/gcc.log 2>&1 || echo "$side gcc FAIL $(head -c 200 $W/gcc.log)"
  NP=$(cygpath -m $PWD/$W/m3rx7.dll)
  JA=(--patch-module java.base=$W/p --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED --add-opens java.base/jdk.internal.mindex=ALL-UNNAMED)
  for c in M3RX7LexicalContract M3RX7TransitionCompositionContract RX7FindConvergence; do
    echo "$side $c: $("$J/java" "${JA[@]}" -cp $W/t $c 2>&1 | tail -1 | cut -c1-150)"
  done
  echo "$side M3RX7NativeBoundaryContract java-default: $("$J/java" "${JA[@]}" -cp $W/t M3RX7NativeBoundaryContract java-default 2>&1 | tail -1 | cut -c1-150)"
  echo "$side M3RX7NativeBoundaryContract native: $("$J/java" -Xcheck:jni "${JA[@]}" -Dm3.rx7.native.path=$NP -cp $W/t M3RX7NativeBoundaryContract 2>&1 | tail -1 | cut -c1-150)"
  echo "$side M3RX7ArrayPowerContract java: $("$J/java" "${JA[@]}" jdk.internal.mindex.M3RX7ArrayPowerContract 2>&1 | tail -1 | cut -c1-150)"
  echo "$side M3RX7ArrayPowerContract native: $("$J/java" -Xcheck:jni "${JA[@]}" jdk.internal.mindex.M3RX7ArrayPowerContract $NP 2>&1 | tail -1 | cut -c1-150)"
done
