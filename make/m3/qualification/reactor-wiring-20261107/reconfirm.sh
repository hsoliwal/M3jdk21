#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# reconfirm.sh <module> <reason>: one DECISION.tsv row that re-confirms a HELD module without a Maven run (recipe
# REACTOR_WIRING_RECONFIRM): the last row of the module is copied, section/gate/build/message replaced; the reason must
# cite the CPU evidence (ls of the local repos, grep of the verify script). No pom change, no commit here.
set -u
M=$1; REASON=$2
W=${W:-/s/m3-scratch/eb-conv/wt-unhold}; Q=$W/make/m3/qualification/reactor-wiring-20261107
SLUG=$(echo "$M" | tr '/' '-')
LAST=$(awk -F'\t' -v m="$M" '$2==m' "$Q/DECISION.tsv" | tail -1)
[ -n "$LAST" ] || { echo "RECONFIRM_ABORT $M no row"; exit 8; }
GAV=$(echo "$LAST" | cut -f5); DEPS=$(echo "$LAST" | cut -f6); FP=$(echo "$LAST" | cut -f9)
printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "lane 37 unhold" "$M" lane37 HELD "$GAV" "$DEPS" "reconfirm-$SLUG" NOT_RUN "$FP" - - "$(echo "$REASON" | tr '\t' ' ')" n/a >> "$Q/DECISION.tsv"
echo "RECONFIRMED $M"
