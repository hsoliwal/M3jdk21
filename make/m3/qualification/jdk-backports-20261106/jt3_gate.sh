#!/bin/bash
# jt3_gate.sh <master-sha> <paths.lst> : RETAINED_3WAY_COMPILE_GATE v1 for JDK-tree twins.
# live tree = master's src/java.base/share/classes (+ src/jdk.compiler/share/classes when a jdk.compiler path is
# listed); rest tree = live with each listed path replaced by jt3/chained/<path> (combine_twins.py output).
# java.base and jdk.compiler paths: javac --patch-module <module>=<tree> over the listed files (implicit closure),
# -proc:none -nowarn -Xmaxerrs 100000. test/jdk paths: javac the test against the side's patched java.base tree with
# --add-exports for its @modules. errdiff.py lists errors NEW in rest (must be empty) and GONE.
cd "$(dirname "$0")"
J="C:/Program Files/Java/jdk-21/bin"; G=/h/m3-scratch/repos/M3jdk21-release.git; R=${1:?master}; L=${2:?paths}
mk() { # side
  local T=jt3/$1; rm -rf $T; mkdir -p $T/jb $T/jc
  (cd $T/jb && tar -xf ../../jb.tar)
  grep -q '^src/jdk.compiler/' $L && { [ -f jt3/jc.tar ] || GIT_NO_LAZY_FETCH=1 GIT_DIR=$G git archive --format=tar $R:src/jdk.compiler/share/classes > jt3/jc.tar; (cd $T/jc && tar -xf ../../jc.tar); }
  [ $1 = rest ] && while read -r p; do
    case $p in
      src/java.base/share/classes/*) cp jt3/chained/$p $T/jb/${p#src/java.base/share/classes/};;
      src/jdk.compiler/share/classes/*) cp jt3/chained/$p $T/jc/${p#src/jdk.compiler/share/classes/};;
    esac; done < $L
}
[ -f jt3/jb.tar ] || GIT_NO_LAZY_FETCH=1 GIT_DIR=$G git archive --format=tar $R:src/java.base/share/classes > jt3/jb.tar
for side in live rest; do
  mk $side; T=jt3/$side; JB=$(cygpath -m "$PWD/$T/jb"); JC=$(cygpath -m "$PWD/$T/jc")
  grep '^src/java.base/share/classes/' $L | sed "s#^src/java.base/share/classes/#$JB/#" > $T/jb.lst
  if [ -s $T/jb.lst ]; then
    "$J/javac" -proc:none -nowarn -Xmaxerrs 100000 -encoding UTF-8 --patch-module java.base="$JB" -d $T/jbcls @$T/jb.lst > $T/jb.log 2>&1
    echo "$side java.base javac rc=$? errors=$(grep -c ': error:' $T/jb.log)"
  fi
  grep '^src/jdk.compiler/share/classes/' $L | sed "s#^src/jdk.compiler/share/classes/#$JC/#" > $T/jc.lst
  if [ -s $T/jc.lst ]; then
    "$J/javac" -proc:none -nowarn -Xmaxerrs 100000 -encoding UTF-8 --patch-module java.base="$JB" --patch-module jdk.compiler="$JC" -d $T/jccls @$T/jc.lst > $T/jc.log 2>&1
    echo "$side jdk.compiler javac rc=$? errors=$(grep -c ': error:' $T/jc.log)"
  fi
  : > $T/tests.log
  grep '^test/jdk/' $L | while read -r p; do
    mkdir -p $T/t; if [ $side = live ]; then GIT_DIR=$G git show "$R:$p"; else cat jt3/chained/$p; fi > $T/t/$(basename $p)
    X=$(grep -oE '@modules .*' $T/t/$(basename $p) | sed 's/@modules//' | tr ' ' '\n' | grep / | sed 's/:.*//' | sed 's#^#--add-exports=#; s#$#=ALL-UNNAMED#' | tr '\n' ' ')
    "$J/javac" -proc:none -nowarn -Xmaxerrs 100000 -encoding UTF-8 --patch-module java.base="$JB" $X -d $T/tcls $T/t/$(basename $p) >> $T/tests.log 2>&1
    echo "$side test $(basename $p) javac rc=$?"
  done
done
for k in jb jc tests; do [ -f jt3/live/$k.log ] && { echo "== errdiff $k"; python -I errdiff.py jt3/live/$k.log jt3/rest/$k.log; }; done
