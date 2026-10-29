#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
# Atom JEP_VETO_REASON: for each vetoed JEP key, the JBS issue comments (public REST API), paced; the reason text is the
# last comment body that mentions withdraw/reject/close, else the last comment (first 400 chars, one line).
# Usage: jep_reasons.sh <VETOED_JEPS.tsv> <raw dir> <out reasons.tsv>
set -uo pipefail
IN="$1"; RAW="$2"; OUT="$3"; mkdir -p "$RAW"
tail -n +2 "$IN" | cut -f1 | while read -r k; do
	[ -s "$RAW/$k.json" ] || { curl -s -m 60 "https://bugs.openjdk.org/rest/api/2/issue/$k?fields=comment,resolution,status" -o "$RAW/$k.json"; sleep 3; }
done
for f in "$RAW"/*.json; do
	jq -r '[.key, ((.fields.comment.comments // []) | (map(select(.body | test("withdr|reject|clos|no longer|abandon"; "i"))) | last // null) as $m
		| ($m // last // {body: "no JBS comment"}) | (.body | gsub("[\t\r\n]+"; " ") | .[0:400]))] | @tsv' "$f" 2>/dev/null
done > "$OUT"; echo "reasons $(wc -l < "$OUT")"
