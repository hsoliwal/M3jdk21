#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
# Pass 2 of the JBS scaffold: mark the enhancements that jdk21u already carries. Every fixed Backport issue with a
# 21.0.x fix version links to its main issue ("Backport" link, inward); those main keys become ALREADY_IN_JDK21U.
# Usage: jbs_mark_21u.sh JBS_ENHANCEMENTS_22_27.tsv > out.tsv   (side file: JDK21U_BACKPORTED_KEYS.txt)
set -euo pipefail
IN="$1"; API=https://bugs.openjdk.org/rest/api/2/search; K=$(dirname "$IN")/JDK21U_BACKPORTED_KEYS.txt
q='project = JDK AND issuetype = Backport AND resolution = Fixed AND fixVersion >= "21.0.1" AND fixVersion < "22" ORDER BY key'
start=0; : > "$K.tmp"
while :; do
	page=$(curl -s -G "$API" --data-urlencode "jql=$q" --data-urlencode "startAt=$start" --data-urlencode 'maxResults=500' --data-urlencode 'fields=issuelinks')
	n=$(printf '%s' "$page" | jq '.issues | length'); [ "$n" -gt 0 ] || break
	printf '%s' "$page" | jq -r '.issues[].fields.issuelinks[]? | select(.type.name=="Backport") | .inwardIssue.key // empty' >> "$K.tmp"
	start=$((start + n))
done
sort -u "$K.tmp" > "$K"; rm -f "$K.tmp"
awk -F'\t' -v OFS='\t' 'NR==FNR {b[$1]=1; next} FNR==1 {print; next} { if ($2 in b) $8="ALREADY_IN_JDK21U"; print }' "$K" "$IN"
