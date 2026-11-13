#!/bin/bash
cd "$1"
for s in 1 2 3; do n=$(wc -l < stock_$s.txt); if cmp -s stock_$s.txt m3_$s.txt; then echo "seed $s: $n lines IDENTICAL sha256=$(sha256sum < m3_$s.txt | cut -c1-16)"; else echo "seed $s: $n lines, DIFFERENT $(diff stock_$s.txt m3_$s.txt | grep -c "^<")"; diff stock_$s.txt m3_$s.txt | head -6 | cut -c1-300; fi; done
