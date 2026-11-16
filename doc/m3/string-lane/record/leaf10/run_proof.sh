#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# STREAM_ENCODER_PROOF v1 (2026-11-16). usage: run_proof.sh <leaf-dir> <classes-root>
# Compiles the base and the head sun/nio/cs/StreamEncoder.java of <leaf-dir> against a stock JDK 21 with
# --patch-module, then runs EncoderProof three times on that JVM: unpatched (stock StreamEncoder), base patched in,
# head patched in. Gates: the bytes of every case are equal in all three runs; the write-length sequence of head is
# compared with stock and with base and the differing case counts are printed. Then a rough timing of each.
# Exit 0 = bytes equal everywhere, 1 = a byte difference, 3 = a compile or run failure.
set -u
leaf=$1; classes=$2
here=$(cd "$(dirname "$0")" && { pwd -W 2>/dev/null || pwd; })
java_home=${PROOF_JAVA_HOME:-C:/Program Files/Java/jdk-21}
rel=src/java.base/share/classes
work=$leaf/proof/work; mkdir -p "$work" "$classes/harness"
for side in base head; do
  rm -rf "$classes/$side"; mkdir -p "$classes/$side"
  MSYS_NO_PATHCONV=1 "$java_home/bin/javac" -nowarn -XDignore.symbol.file -implicit:none -d "$classes/$side" \
    --patch-module "java.base=$leaf/$side/$rel" "$leaf/$side/$rel/sun/nio/cs/StreamEncoder.java" \
    > "$work/javac-$side.log" 2>&1 || { echo "COMPILE_FAIL $side"; head -5 "$work/javac-$side.log"; exit 3; }
done
"$java_home/bin/javac" -Xlint:all -Werror -d "$classes/harness" "$here/EncoderProof.java" || { echo "COMPILE_FAIL harness"; exit 3; }
run() {
  local side=$1 mode=$2; shift 2
  if [ "$side" = stock ]; then "$java_home/bin/java" -cp "$classes/harness" EncoderProof "$mode" "$@"
  else MSYS_NO_PATHCONV=1 "$java_home/bin/java" --patch-module "java.base=$classes/$side" -cp "$classes/harness" EncoderProof "$mode" "$@"; fi
}
for side in stock base head; do run "$side" cases "$work/cases-$side.tsv" | sed "s/^/$side\t/" || exit 3; done
bytes() { cut -f1,2 "$work/cases-$1.tsv"; }
shape() { cut -f1,3 "$work/cases-$1.tsv"; }
bs=$(diff <(bytes stock) <(bytes head) | grep -c '^>'); bb=$(diff <(bytes base) <(bytes head) | grep -c '^>')
ss=$(diff <(shape stock) <(shape head) | grep -c '^>'); sb=$(diff <(shape base) <(shape head) | grep -c '^>')
sbs=$(diff <(shape stock) <(shape base) | grep -c '^>')
printf 'bytes_differ_head_vs_stock\t%s\nbytes_differ_head_vs_base\t%s\n' "$bs" "$bb"
printf 'write_lengths_differ_head_vs_stock\t%s\nwrite_lengths_differ_head_vs_base\t%s\nwrite_lengths_differ_base_vs_stock\t%s\n' "$ss" "$sb" "$sbs"
for side in stock base head; do run "$side" time | sed "s/^/$side\t/"; done
[ "$bs" -eq 0 ] && [ "$bb" -eq 0 ] && echo "RESULT	PASS" || { echo "RESULT	FAIL"; exit 1; }
