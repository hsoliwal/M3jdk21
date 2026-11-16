#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# COMPILE_GATE v1.2 (2026-11-16; v1.1: -implicit:none and GATE_CLASSES, because v1 wrote 8,142 class files;
# v1.2: the files to compile are arguments, so every java.base leaf can use it).
# usage: compile_gate.sh <leaf-dir> <pin-overlay-classes> <full-tree-classes> <path-under-share/classes>...
# Compiles the named files of <leaf-dir>/base and <leaf-dir>/head with a stock JDK 21 javac under
# --patch-module java.base=<side>;<pin overlay>;<full tree>, then compares the diagnostics of the two sides with the
# line numbers removed (BLLSE records: file, severity, message). The stock javac cannot build all of java.base, so
# both sides may report errors; the gate is that head adds none. A side directory must also hold any file the edited
# files need in a newer version than the overlay has. GATE_CLASSES names the class output root: use a drive with
# small clusters. Exit 0 = both sides ran and head has no new diagnostics, 1 = new diagnostics, 3 = javac wrote no log.
set -u
leaf=$1; pin=$2; full=$3; shift 3
java_home=${PROOF_JAVA_HOME:-C:/Program Files/Java/jdk-21}
rel=src/java.base/share/classes
work=$leaf/proof/work
mkdir -p "$work"
compile() {
  local side=$1; shift
  local classes=${GATE_CLASSES:-$work}/javac-$side
  rm -rf "$classes"; mkdir -p "$classes" || return 3
  local files=()
  local path
  for path in "$@"; do files+=("$leaf/$side/$rel/$path"); done
  MSYS_NO_PATHCONV=1 "$java_home/bin/javac" -proc:none -implicit:none -nowarn -XDignore.symbol.file -Xmaxerrs 5000 \
    -d "$classes" --patch-module "java.base=$leaf/$side/$rel;$pin;$full" "${files[@]}" > "$work/javac-$side.log" 2>&1
  local status=$?
  echo "javac_$side exit=$status errors=$(grep -cE ': error: ' "$work/javac-$side.log") classes=$(find "$classes" -name '*.class' | wc -l)"
  grep -E '\.java:[0-9]+: (error|warning): ' "$work/javac-$side.log" \
    | sed -E 's#^.*[/\\](base|head)[/\\]src[/\\]#src/#; s#\.java:[0-9]+: #.java\t#' | sort > "$work/diag-$side.tsv"
  [ "$status" -eq 0 ] || [ -s "$work/javac-$side.log" ] || { echo "javac_$side failed without output (disk full?)"; return 3; }
}
compile base "$@" || exit 3
compile head "$@" || exit 3
comm -13 "$work/diag-base.tsv" "$work/diag-head.tsv" > "$work/diag-new.tsv"
comm -23 "$work/diag-base.tsv" "$work/diag-head.tsv" > "$work/diag-resolved.tsv"
echo "new=$(wc -l < "$work/diag-new.tsv") resolved=$(wc -l < "$work/diag-resolved.tsv")"
head -5 "$work/diag-new.tsv"
[ ! -s "$work/diag-new.tsv" ]
