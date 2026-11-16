#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# COMPILE_GATE v1.1 (2026-11-16; v1.1: -implicit:none and GATE_CLASSES, because v1 wrote 8,142 class files). usage: compile_gate.sh <leaf-dir> <pin-overlay-classes> <full-tree-classes>
# Compiles M3StringFacts.java and String.java of <leaf-dir>/base and <leaf-dir>/head with a stock JDK 21 javac under
# --patch-module java.base=<side>;<pin overlay>;<full tree>, then compares the diagnostics of the two sides with the
# line numbers removed (BLLSE records: file, severity, message). The stock javac cannot build all of java.base, so
# both sides may report errors; the gate is that head adds none. Exit 0 = no new diagnostics, 1 = new diagnostics.
set -u
leaf=$1; pin=$2; full=$3
java_home=${PROOF_JAVA_HOME:-C:/Program Files/Java/jdk-21}
rel=src/java.base/share/classes
compile() {
  local side=$1
  classes=${GATE_CLASSES:-$leaf/proof/work}/javac-$side; rm -rf "$classes"; mkdir -p "$classes"
  MSYS_NO_PATHCONV=1 "$java_home/bin/javac" -proc:none -implicit:none -nowarn -XDignore.symbol.file -Xmaxerrs 5000 \
    -d "$classes" --patch-module "java.base=$leaf/$side/$rel;$pin;$full" \
    "$leaf/$side/$rel/java/lang/M3StringFacts.java" "$leaf/$side/$rel/java/lang/String.java" \
    > "$leaf/proof/work/javac-$side.log" 2>&1
  echo "javac_$side exit=$? errors=$(grep -cE ': error: ' "$leaf/proof/work/javac-$side.log")"
  grep -E '\.java:[0-9]+: (error|warning): ' "$leaf/proof/work/javac-$side.log" \
    | sed -E 's#\\#/#g; s#^.*/(base|head)/src/#src/#; s#^.*/share/classes/#share/classes/#; s#\.java:[0-9]+: #.java\t#' \
    | sort > "$leaf/proof/work/diag-$side.tsv"
}
compile base
compile head
comm -13 "$leaf/proof/work/diag-base.tsv" "$leaf/proof/work/diag-head.tsv" > "$leaf/proof/work/diag-new.tsv"
comm -23 "$leaf/proof/work/diag-base.tsv" "$leaf/proof/work/diag-head.tsv" > "$leaf/proof/work/diag-resolved.tsv"
echo "new=$(wc -l < "$leaf/proof/work/diag-new.tsv") resolved=$(wc -l < "$leaf/proof/work/diag-resolved.tsv")"
head -5 "$leaf/proof/work/diag-new.tsv"
[ ! -s "$leaf/proof/work/diag-new.tsv" ]
