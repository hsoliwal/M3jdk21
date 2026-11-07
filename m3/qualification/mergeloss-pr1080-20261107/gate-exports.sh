#!/bin/bash
# Export superset gate: master's libjava M3RegexDpPlan JNI symbols must all survive in the merged owner.
cd "$(dirname "$0")"; JI=/usr/lib/jvm/java-21-openjdk-amd64/include; H=/tmp/q1080_merged/headers
for s in master merged; do
  gcc -std=c17 -D_GNU_SOURCE -O2 -Wall -Wextra -Werror -fPIC -shared -I$JI -I$JI/linux -I$H -I. $s/M3RegexDpPlan.c -o /tmp/c1080_$s.so && echo "$s rc=0 exports=$(nm -D --defined-only /tmp/c1080_$s.so | grep -c ' T Java_')"
done
comm -23 <(nm -D --defined-only /tmp/c1080_master.so | awk '/ T Java_/{print $3}' | sort) <(nm -D --defined-only /tmp/c1080_merged.so | awk '/ T Java_/{print $3}' | sort) > /tmp/c1080_lost.txt
echo "lost_exports=$(wc -l < /tmp/c1080_lost.txt)"; cat /tmp/c1080_lost.txt
comm -13 <(nm -D --defined-only /tmp/c1080_master.so | awk '/ T Java_/{print $3}' | sort) <(nm -D --defined-only /tmp/c1080_merged.so | awk '/ T Java_/{print $3}' | sort) | sed 's/^/added /'
