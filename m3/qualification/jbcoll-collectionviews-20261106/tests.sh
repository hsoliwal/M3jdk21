#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# tests.sh <patch-classes> <scratch> <test-file>... : the jtreg tests of the touched java.base classes without jtreg
# (no jtreg on the qualification host). All files compile together (javac --patch-module java.base=<patch-classes>,
# helpers like StoredRepresentativeOracle are shared); each file with "@test" runs once per "@run main[/othervm]
# [vm-options] Class [args]" line as java --patch-module java.base=<patch-classes> [--add-opens M/P=ALL-UNNAMED for
# every "@modules M/P:open"] [vm-options] -cp <classes> Class [args], each bounded to 120 s. Prints TEST <name>
# PASS|FAIL rc=<n> and TESTS pass=<n> fail=<n>; exit 1 on any failure. JAVA_HOME = a JDK 21.
set -uo pipefail
P=${1:?patch classes}; S=${2:?scratch}; shift 2
J="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
rm -rf "$S/classes" "$S/logs"; mkdir -p "$S/classes" "$S/logs"
"$J/javac" -g -nowarn -encoding UTF-8 --patch-module "java.base=$P" -d "$S/classes" "$@" > "$S/javac.log" 2>&1
crc=$?
echo "TESTS_JAVAC rc=$crc files=$# errors=$(grep -c ': error:' "$S/javac.log")"
[ $crc = 0 ] || exit 1
pass=0; fail=0
for f in "$@"; do
    grep -q '@test' "$f" || continue
    opens=$(grep -oE '@modules .*' "$f" | tr ' ' '\n' | grep ':open$' | sed 's/:open$//; s#^#--add-opens=#; s#$#=ALL-UNNAMED#' | tr '\n' ' ')
    while read -r run; do
        set -- $run
        shift   # main or main/othervm
        vmopts=(); while [ $# -gt 0 ] && [ "${1#-}" != "$1" ]; do vmopts+=("$1"); shift; done
        cls=$1; shift
        log="$S/logs/$cls.log"
        timeout 120 "$J/java" --patch-module "java.base=$P" $opens "${vmopts[@]}" -cp "$S/classes" "$cls" "$@" > "$log" 2>&1
        rc=$?
        if [ $rc = 0 ]; then pass=$((pass + 1)); v=PASS; else fail=$((fail + 1)); v=FAIL; fi
        echo "TEST $cls $v rc=$rc $(tail -1 "$log" | cut -c1-160)"
    done < <(grep -oE '@run main(/othervm)? .*' "$f" | sed 's/^@run //')
done
echo "TESTS pass=$pass fail=$fail"
[ $fail = 0 ]
