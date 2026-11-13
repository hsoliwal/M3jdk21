#!/bin/bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Driver of ResidencyEvictionReentryDifferential: compiles the oracle against CLASSES and runs it, restarting it after
# each RESUME (MAX_STUCK livelocked workers per JVM). usage: vpdiff.sh <oracle dir> <classes> <out file> <episodes> <ops>
# env: JAVA_HOME = JDK 21; M3_ARRAYS_JAR = the m3-arrays jar the module was built with.
SRC=$1; C=$2; OUT=$3; EP=$4; OPS=$5
J="${JAVA_HOME:?JAVA_HOME}/bin"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
O="$OUT.oracle"; rm -rf "$O" "$OUT"; mkdir -p "$O"
"$J/javac" --release 21 -Xlint:all -Werror -encoding UTF-8 -cp "$C$SEP${M3_ARRAYS_JAR:-}" -d "$O" \
    "$SRC/jdk/m3/collections/ResidencyEvictionReentryDifferential.java" || exit 2
first=0; runs=0
while :; do runs=$((runs+1))
  "$J/java" -cp "$O$SEP$C$SEP${M3_ARRAYS_JAR:-}" jdk.m3.collections.ResidencyEvictionReentryDifferential "$OUT" $first $EP $OPS; rc=$?
  [ $rc -eq 3 ] || break
  first=$(grep '^RESUME ' "$OUT" | tail -1 | cut -d' ' -f2)
done
echo "rc=$rc runs=$runs lines=$(wc -l < "$OUT") hangs=$(grep -c ' HANG ' "$OUT") sha256=$(grep -v '^RESUME ' "$OUT" | sha256sum | cut -c1-12)"
