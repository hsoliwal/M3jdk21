#!/bin/bash
# SPDX-License-Identifier: Apache-2.0
# Copyright (c) 2026 Hitesh Soliwal and contributors
#
# Generation step for the frequency-ranked catalog exports of m3/lexicon/m3-lexicon-registry.tsv.
# The exports are generated output, not checked in; every input, count file and export is
# pinned by SHA-256 in lexicon-inputs.tsv, lexicon-corpora.tsv and the registry.
#
# usage: build-exports.sh INPUT_DIR OUTPUT_DIR [LEXICON_NUM ...]
#   INPUT_DIR   the measured word lists named in lexicon-inputs.tsv (<name>.tsv.gz)
#   OUTPUT_DIR  receives counts/<num>.tsv and exports/<num>-<source_id>/ (must not hold them)
#   LEXICON_NUM restricts the run to these registry rows (default: every row, and then
#               WriteCorpusManifest.java writes OUTPUT_DIR/corpus-<root>.m3corpus)
# Environment:
#   M3_JAVA          java launcher of a JDK whose java.base has jdk.m3.text (default: java)
#   M3_JAVA_OPTIONS  extra launcher options, for example --patch-module java.base=CLASSES
#   JDK_SRC_ZIP      lib/src.zip of JDK 21.0.9, for the java.base/java/util corpus
#   PYTHON_LIB       the Lib directory of CPython 3.12.6, for the cpython/Lib corpus
#   PYTHON           python launcher (default: python3)
# A pin of "-" is reported as RECORD with the actual value instead of being checked.
# Exit status 0 only when every selected row matched every pin.
set -u
here=$(cd "$(dirname "$0")" && pwd)
registry="$here/../../../m3/lexicon/m3-lexicon-registry.tsv"
inputs="$here/lexicon-inputs.tsv"
corpora="$here/lexicon-corpora.tsv"
in=${1:?INPUT_DIR}; out=${2:?OUTPUT_DIR}; shift 2
java=${M3_JAVA:-java}; python=${PYTHON:-python3}
mkdir -p "$out/counts" "$out/exports"
failures=0

sha() { sha256sum "$1" | cut -c1-64; }
cell() { # file key-column key value-column
  awk -F'\t' -v k="$3" -v kc="$2" -v vc="$4" 'NR > 1 && $kc == k { print $vc; exit }' "$1"
}
fail() { echo "$1"; failures=$((failures + 1)); }
pin() { # what expected actual
  if [ "$2" = "-" ]; then echo "RECORD $1 $3"
  elif [ "$2" = "$3" ]; then echo "MATCH $1 $3"
  else echo "MISMATCH $1 expected=$2 actual=$3"; failures=$((failures + 1)); fi
}

rows=("$@")
[ ${#rows[@]} -eq 0 ] && mapfile -t rows < <(awk -F'\t' 'NR > 1 { print $1 }' "$registry")
for num in "${rows[@]}"; do
  source_id=$(cell "$registry" 1 "$num" 3); tag=$(cell "$registry" 1 "$num" 5)
  rule=$(cell "$registry" 1 "$num" 7); export_pin=$(cell "$registry" 1 "$num" 8)
  input=$(cell "$inputs" 1 "$num" 2); input_pin=$(cell "$inputs" 1 "$num" 3)
  oracles=$(cell "$inputs" 1 "$num" 4); counts_pin=$(cell "$inputs" 1 "$num" 5)
  [ -n "$source_id" ] && [ -n "$input" ] || { fail "MISSING row $num"; continue; }
  pin "$num input $input" "$input_pin" "$(sha "$in/$input")"
  oracle_files=()
  [ "$oracles" != "-" ] && for o in ${oracles//,/ }; do oracle_files+=("$in/$o"); done
  extra=()
  case "$rule" in
    codebase-token:java.base/java/util@*) extra=(--corpus "${JDK_SRC_ZIP:?JDK_SRC_ZIP}") ;;
    codebase-token:cpython/Lib@*) extra=(--corpus "${PYTHON_LIB:?PYTHON_LIB}") ;;
  esac
  counts="$out/counts/$num.tsv"
  summary=$("$python" -I "$here/frequency_counts.py" "$rule" "$in/$input" "$counts" \
      "${oracle_files[@]}" "${extra[@]}") || { fail "FAILED counts $num"; continue; }
  echo "COUNTS $num $source_id $summary"
  case "$rule" in codebase-token:*)
    corpus=${rule#codebase-token:}
    pin "$num corpus $corpus" "$(cell "$corpora" 1 "$corpus" 3)" \
        "$(echo "$summary" | sed -n 's/.*corpus_sha256=\([0-9a-f]*\).*/\1/p')" ;;
  esac
  pin "$num counts" "$counts_pin" "$(sha "$counts")"
  dir="$out/exports/$num-$source_id"
  "$java" ${M3_JAVA_OPTIONS:-} --add-exports java.base/jdk.m3.text=ALL-UNNAMED \
      jdk.m3.text.LexiconTool build --frequency "$source_id" "$tag" "$counts" "$dir" \
      || { fail "FAILED export $num"; continue; }
  pin "$num export $source_id" "$export_pin" "$(sha "$dir/m3.export.json")"
done
if [ $# -eq 0 ] && [ $failures -eq 0 ]; then
  "$java" ${M3_JAVA_OPTIONS:-} --add-exports java.base/jdk.m3.text=ALL-UNNAMED \
      "$here/WriteCorpusManifest.java" "$registry" "$inputs" "$here/lexicon-receipts.tsv" \
      "$out" || failures=$((failures + 1))
fi
echo "BUILD_EXPORTS rows=${#rows[@]} failures=$failures"
[ $failures -eq 0 ]
