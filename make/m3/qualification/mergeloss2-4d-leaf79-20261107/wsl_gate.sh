#!/bin/bash
# ML79 WSL gate: gcc -Werror (Linux branches incl. the mmap exact-address path) for master vs restored libjava files.
cd "$(dirname "$0")"
JI=/usr/lib/jvm/java-21-openjdk-amd64/include
for side in master restored; do
  for f in M3BitLane28 M3RX7 M3RegexDpPlan M3StateRelation M3StringPool M3StringRegexDp; do
    gcc -std=c11 -D_GNU_SOURCE -O2 -Wall -Wextra -Werror -fPIC -I$JI -I$JI/linux -Iinc -I$side -c $side/$f.c -o /tmp/ml79_${side}_$f.o > /tmp/ml79_${side}_$f.log 2>&1
    echo "$side $f rc=$? diag=$(grep -cE 'warning|error' /tmp/ml79_${side}_$f.log)"
  done
done
gcc -std=c11 -O2 -Wall -Wextra -Werror -fPIC -shared -I$JI -I$JI/linux -Iinc -Irestored restored/*.c -o /tmp/ml79_restored.so && echo "restored link .so rc=0 exports=$(nm -D --defined-only /tmp/ml79_restored.so | grep -c ' T Java_')"
gcc -std=c11 -O2 -Wall -Wextra -Werror -fPIC -shared -I$JI -I$JI/linux -Iinc -Imaster master/*.c -o /tmp/ml79_master.so && echo "master link .so rc=0 exports=$(nm -D --defined-only /tmp/ml79_master.so | grep -c ' T Java_')"
diff <(nm -D --defined-only /tmp/ml79_master.so | awk '{print $3}' | sort) <(nm -D --defined-only /tmp/ml79_restored.so | awk '{print $3}' | sort) && echo "EXPORTS_IDENTICAL"
