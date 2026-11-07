#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# unhold.sh <module> <reason>: records the delete-block edit that takes a HELD module out of profile m3-offreactor-held
# (its exact recorded lines, from EDITS.tsv) so gate.sh can re-gate it; one commit. Used when the first gate was RED for
# an environment reason (here: a path outside the m3/ sparse checkout), never to force a result.
set -u
M=$1; REASON=$2
W=${W:-/s/m3-scratch/eb-conv/wt-reactor}; Q=$W/m3/qualification/reactor-wiring-20261107
cd "$W" || exit 9
git diff --quiet -- m3/pom.xml || { echo "UNHOLD_ABORT $M dirty pom"; exit 8; }
PAYLOAD=$(python -I -c "import csv,json,sys; rows=[r for r in csv.DictReader(open(sys.argv[1],encoding='utf-8'),delimiter='\t') if r['note'].startswith(sys.argv[2]+' HELD')]; print(json.loads(rows[-1]['payload']))" "$Q/EDITS.tsv" "$M")
[ -n "$PAYLOAD" ] || { echo "UNHOLD_ABORT $M no HELD row"; exit 8; }
python -I "$Q/apply.py" record --root . --file m3/pom.xml --op delete-block --text "$PAYLOAD" --note "$M unheld for re-gate: $REASON" || exit 7
git add m3/pom.xml "$Q" && git commit -q -m "reactor-wiring: $M unheld for re-gate ($REASON)" && echo "UNHELD $M"
