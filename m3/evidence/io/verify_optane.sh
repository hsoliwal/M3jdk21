#!/bin/bash
# vo.sh <tree> <out> : run m3/optane/verify.sh of a tree into <out>; print its exit code and outcome lines.
T=$1; O=$2; rm -rf "$O" "$O.log"
(cd "$T" && M3_OPTANE_BUILD="$O" timeout 1200 bash m3/optane/verify.sh > "$O.log" 2>&1)
echo "verify rc=$?"
grep -E 'PASS|FAIL|Exception|error:' "$O.log" | tail -5 | cut -c1-150
