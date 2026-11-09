#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Before/after differential of recipe PRECOMPUTE_JOINED_LANES v1 (apply.py). Every stage image of
# M3FsPrecomputeBundle.java (apply.py --replay --emit, rebuilt from the pinned master blob) is compiled on its own
# against the module classes; JoinedLanesDifferential runs on each stage build. Stages 02-04 are performance atoms,
# so their presence and raw digests must equal stage 01's (stage 00, master, has no joined API: stage 01 adds it).
# The key/value and hash-lane templates of the Synexia crates run on the M3KeyValueSlots class extracted from the
# stage-03 and stage-04 images. Every JVM run is bounded by RUN_TIMEOUT seconds (default 120); RAW_STORE_ROOT puts the
# raw-mode content stores on a faster local file system (default: the scratch dir; the store fsyncs every object).
# usage: differential.sh <module-classes-dir> <scratch-dir> [normal-trials=15000] [xint-trials=2000] [raw-scenarios=60]
# env:   JAVA_HOME (a JDK 21), PYTHON (default python3), RUN_TIMEOUT, RAW_STORE_ROOT
set -euo pipefail
abs() { (cd "$1" && { pwd -W 2>/dev/null || pwd; }); }  # Windows-form path under Git Bash
Q=$(abs "$(dirname "$0")")
R=$(dirname "$Q")
ROOT=$(abs "$R/../../../..")
CLASSES=$(abs "$1")
W=$2
NORMAL=${3:-15000}
XINT=${4:-2000}
RAW=${5:-60}
J=${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin
T=${RUN_TIMEOUT:-120}
SEP=:
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; esac
rm -rf "$W" && mkdir -p "$W"
[[ -z "${RAW_STORE_ROOT:-}" ]] || { rm -rf "$RAW_STORE_ROOT" && mkdir -p "$RAW_STORE_ROOT"; }
"${PYTHON:-python3}" -I "$R/apply.py" --root "$ROOT" --replay --emit "$W/images" > "$W/replay.log"
stages=$(cd "$W/images" && ls -d [0-9][0-9]-* | grep -v '^00-' | sort)
for s in $stages; do
  mkdir -p "$W/cls-$s"
  "$J/javac" --release 21 -Xlint:all -Werror -implicit:none -encoding UTF-8 -cp "$CLASSES" -d "$W/cls-$s" \
      "$W/images/$s/M3FsPrecomputeBundle.java"
done
first=$(echo "$stages" | head -n 1)
mkdir -p "$W/driver"
"$J/javac" --release 21 -Xlint:all -Werror -encoding UTF-8 -cp "$W/cls-$first$SEP$CLASSES" -d "$W/driver" \
    "$Q/JoinedLanesDifferential.java"
fail=0
for s in $stages; do
  cp="$W/driver$SEP$W/cls-$s$SEP$CLASSES"
  timeout "$T" "$J/java" -cp "$cp" JoinedLanesDifferential presence "$NORMAL" | sed "s/^/$s normal /" | tee -a "$W/digests.log"
  timeout "$T" "$J/java" -Xint -cp "$cp" JoinedLanesDifferential presence "$XINT" | sed "s/^/$s xint /" | tee -a "$W/digests.log"
  timeout "$T" "$J/java" -cp "$cp" JoinedLanesDifferential raw "$RAW" "${RAW_STORE_ROOT:-$W}/store-$s" | sed "s/^/$s normal /" | tee -a "$W/digests.log"
done
for kind in "normal PRESENCE" "xint PRESENCE" "normal RAW"; do
  n=$(grep " $kind " "$W/digests.log" | sed 's/ model_old_reads=.*//; s/^[^ ]* //' | sort -u | wc -l)
  if [[ "$n" != 1 ]]; then echo "DIFFERENTIAL FAIL $kind: stage digests differ"; fail=1; fi
done
grep " PRESENCE " "$W/digests.log" | grep -v -E "cases=([0-9]+) .* model_equals_real=\1$" && { echo "DIFFERENTIAL FAIL model/real"; fail=1; }
extract() { # <image> <new-name>: the private M3KeyValueSlots class of a stage image, renamed
  awk '/^  private static final class M3KeyValueSlots<K, V> \{/{f=1} f{print} f&&/^  \}$/{exit}' "$1" | sed "s/M3KeyValueSlots/$2/g"
}
inject() { # <template> <anchor> <file> <out>
  "${PYTHON:-python3}" -I - "$1" "$2" "$3" "$4" <<'PY'
import sys
from pathlib import Path
template, anchor, body, out = Path(sys.argv[1]).read_text(), sys.argv[2], Path(sys.argv[3]).read_text(), sys.argv[4]
if template.count(anchor) != 1:
    raise SystemExit("template anchor refused: " + anchor)
Path(out).write_text(template.replace(anchor, body))
PY
}
if [[ -d "$W/images/03-key-value-lanes" ]]; then
  mkdir -p "$W/slots"
  extract "$W/images/03-key-value-lanes/M3FsPrecomputeBundle.java" M3KeyValueSlots > "$W/slots/nested.txt"
  inject "$Q/M3SlotsSmoke.template.java.txt" "    // M3_SLOTS_NESTED_IMPLEMENTATION" "$W/slots/nested.txt" "$W/slots/M3SlotsSmoke.java"
  "$J/javac" --release 21 -Xlint:all -Werror -d "$W/slots" "$W/slots/M3SlotsSmoke.java"
  timeout "$T" "$J/java" -cp "$W/slots" M3SlotsSmoke | tee -a "$W/digests.log"
  timeout "$T" "$J/java" -Xint -Dm3.slots.smoke.steps=10000 -cp "$W/slots" M3SlotsSmoke | tee -a "$W/digests.log"
fi
if [[ -d "$W/images/04-hash-lane-cache" ]]; then
  mkdir -p "$W/hash"
  { extract "$W/images/03-key-value-lanes/M3FsPrecomputeBundle.java" LegacySlots
    extract "$W/images/04-hash-lane-cache/M3FsPrecomputeBundle.java" CachedSlots; } > "$W/hash/nested.txt"
  inject "$Q/HashLaneDifferential.template.java.txt" "  // OLD_NESTED_MAP" "$W/hash/nested.txt" "$W/hash/step.java"
  inject "$W/hash/step.java" "  // NEW_NESTED_MAP" /dev/null "$W/hash/HashLaneDifferential.java"
  "$J/javac" --release 21 -Xlint:all -Werror -d "$W/hash" "$W/hash/HashLaneDifferential.java"
  timeout "$T" "$J/java" -cp "$W/hash" HashLaneDifferential | tee -a "$W/digests.log"
  timeout "$T" "$J/java" -Xint -Dm3.hash.operations=12000 -Dm3.hash.transactions=2500 -cp "$W/hash" HashLaneDifferential \
      | tee -a "$W/digests.log"
fi
if [[ "$fail" == 0 ]]; then echo "DIFFERENTIAL PASS stages=$(echo $stages | tr ' ' ',')"; else exit 1; fi
