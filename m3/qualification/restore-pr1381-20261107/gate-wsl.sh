#!/bin/bash
cd "$(dirname "$0")"; JI=/usr/lib/jvm/java-21-openjdk-amd64/include; H=/mnt/s/m3-scratch/converge-20261106/rel/rx7w/restored/h
for s in M3RX7.master.c M3RX7.c; do gcc -std=c11 -D_GNU_SOURCE -O2 -Wall -Wextra -Werror -fPIC -shared -I$JI -I$JI/linux -I$H -I. $s -o /tmp/rx7_$s.so && echo "wsl $s rc=0 exports=$(nm -D --defined-only /tmp/rx7_$s.so | grep -c ' T Java_')"; done
