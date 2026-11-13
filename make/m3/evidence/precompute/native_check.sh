#!/bin/bash
cd "$(dirname "$0")"
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 PATH=/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH
for v in t_before t; do
  H=/tmp/nat-$v; rm -rf $H; mkdir -p $H/h $H/c $H/o
  for m in precompute regex-cursor; do javac -nowarn -h $H/h -d $H/c $(find $v/m3/ports/$m/src/main/java $v/m3/ports/$m/src/test/java -name '*.java' ! -name module-info.java) -cp /mnt/s/m3-scratch/eb-conv/m2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar 2>/dev/null; done
  ok=0; bad=0; miss=0
  for f in $(find $v/m3/ports/precompute/src/main/native $v/m3/ports/regex-cursor/src/test/native -name '*.c'); do
    if gcc -std=c11 -fPIC -shared -Wall -Wextra -Werror -I$JAVA_HOME/include -I$JAVA_HOME/include/linux -I$H/h -I$(dirname $f) -I$(dirname $f)/../include -o $H/o/$(basename $f .c).so $f 2>$H/o/$(basename $f .c).log; then ok=$((ok+1)); else bad=$((bad+1)); fi
    for s in $(grep -oE "Java_[A-Za-z0-9_]+" $f | sort -u); do grep -rqw "$s" $H/h || miss=$((miss+1)); done
  done
  echo "$v native compile ok=$ok fail=$bad symbols_not_in_generated_headers=$miss"
done
