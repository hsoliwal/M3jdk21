#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# tests.sh <patch-classes> <scratch> <test-file>... : jtreg tests of the touched java.base classes without jtreg (no
# jtreg on the qualification host). All files compile together (javac --patch-module java.base=<patch-classes>, plus
# --add-exports for every "@modules M/P" and "@modules M/P:open"). Each file with "@test" runs once per "@run main"
# line, or once as "main <Class>" when it has no @run line (the jtreg default): java --patch-module
# java.base=<patch-classes> [--add-exports / --add-opens of its @modules] [vm-options] -cp <classes> Class [args],
# 120 s bound each. Files with "@run testng|junit" are not compiled and "main/manual" runs are not run: SKIP lines
# (no framework jar on the host; manual by design).
# Prints TEST <class> PASS|FAIL|SKIP and TESTS pass=<n> fail=<n> skip=<n>; exit 1 on any failure. JAVA_HOME = JDK 21.
set -uo pipefail
P=${1:?patch classes}; S=${2:?scratch}; shift 2
J="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
rm -rf "$S/classes" "$S/logs"; mkdir -p "$S/classes" "$S/logs"
framework=(); plain=()
for f in "$@"; do if grep -qE '@run (testng|junit)' "$f"; then framework+=("$f"); else plain+=("$f"); fi; done
set -- "${plain[@]}"
mods() { grep -oE '@modules .*' "$1" | tr ' ' '\n' | grep / ; }
exports=$(for f in "$@"; do mods "$f"; done | sed 's/:.*//' | sort -u | sed 's#^#--add-exports=#; s#$#=ALL-UNNAMED#' | tr '\n' ' ')
"$J/javac" -g -nowarn -encoding UTF-8 --patch-module "java.base=$P" $exports -d "$S/classes" "$@" > "$S/javac.log" 2>&1
crc=$?
echo "TESTS_JAVAC rc=$crc files=$# errors=$(grep -c ': error:' "$S/javac.log")"
[ $crc = 0 ] || exit 1
pass=0; fail=0; skip=0
for f in "${framework[@]}"; do skip=$((skip + 1)); echo "TEST $(basename "$f" .java) SKIP $(grep -oE '@run (testng|junit)' "$f" | head -1) (not compiled: no framework jar)"; done
for f in "$@"; do
    grep -q '@test' "$f" || continue
    flags=$(mods "$f" | sed -E 's#^([^:]*):open$#--add-opens=\1=ALL-UNNAMED#; t; s#^([^:]*)$#--add-exports=\1=ALL-UNNAMED#; s#:.*##' | tr '\n' ' ')
    runs=$(grep -oE '@run [a-z/]+ .*' "$f" | sed 's/^@run //')
    [ -n "$runs" ] || runs="main $(basename "$f" .java)"
    while read -r run; do
        set -- $run
        mode=$1; shift
        case $mode in
            main|main/othervm) ;;
            *) skip=$((skip + 1)); echo "TEST $(basename "$f" .java) SKIP @run $mode"; continue ;;
        esac
        vmopts=(); while [ $# -gt 0 ] && [ "${1#-}" != "$1" ]; do vmopts+=("$1"); shift; done
        cls=$1; shift
        log="$S/logs/$cls.$((pass + fail)).log"
        timeout 120 "$J/java" --patch-module "java.base=$P" $flags "${vmopts[@]}" -cp "$S/classes" "$cls" "$@" > "$log" 2>&1
        rc=$?
        if [ $rc = 0 ]; then pass=$((pass + 1)); v=PASS; else fail=$((fail + 1)); v=FAIL; fi
        echo "TEST $cls $v rc=$rc ${vmopts[*]} $(tail -1 "$log" | cut -c1-160)"
    done <<< "$runs"
done
echo "TESTS pass=$pass fail=$fail skip=$skip"
[ $fail = 0 ]
