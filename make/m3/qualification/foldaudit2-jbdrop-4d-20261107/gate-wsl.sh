#!/bin/bash
cd "$(dirname "$0")"; JI=/usr/lib/jvm/java-21-openjdk-amd64/include
for s in master fixed; do gcc -std=c11 -D_GNU_SOURCE -O2 -Wall -Wextra -Werror -fPIC -shared -I$JI -I$JI/linux $s.c -o /tmp/om_$s.so && echo "wsl $s rc=0 exports=$(nm -D --defined-only /tmp/om_$s.so | grep -c ' T Java_')"; done
