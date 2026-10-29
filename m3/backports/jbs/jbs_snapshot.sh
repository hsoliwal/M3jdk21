#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
# Atom JBS_SNAPSHOT (pattern of m3jdk-superset jbs_pull.sh, plus subcomponent): page one JQL out of the public JBS REST API
# (bugs.openjdk.org, no login) into immutable pages <out>/<name>/pNNNN.json, 1000 issues each. Re-runnable: kept pages stay.
# Usage: jbs_snapshot.sh <out> <name> <jql>
set -uo pipefail
OUT="$1"; NAME="$2"; JQL="$3"; D="$OUT/$NAME"; mkdir -p "$D"
F=summary,issuetype,status,resolution,fixVersions,components,customfield_10008,labels,priority,resolutiondate,issuelinks
API=https://bugs.openjdk.org/rest/api/2/search
total=$(curl -s -m 120 -G $API --data-urlencode "jql=$JQL" --data-urlencode maxResults=0 | jq '.total') || { echo "$NAME COUNT_FAILED"; exit 2; }
echo "$NAME total=$total $(date -u +%T)"
for ((at = 0; at < total; at += 1000)); do
	p=$(printf '%s/p%04d.json' "$D" $((at / 1000))); [ -s "$p" ] && continue
	for try in 1 2 3; do
		curl -s -m 600 -G $API --data-urlencode "jql=$JQL ORDER BY key" --data-urlencode startAt=$at \
			--data-urlencode maxResults=1000 --data-urlencode fields=$F -o "$p.part" &&
			jq -e '.issues | length > 0' "$p.part" > /dev/null 2>&1 && { mv "$p.part" "$p"; break; }
		sleep 20
	done
	[ -s "$p" ] || { echo "$NAME PAGE_FAILED at=$at"; exit 2; }
	sleep 5; echo "HEARTBEAT $NAME page $((at / 1000)) $(date -u +%T)"
done
echo "$NAME pages=$(ls "$D" | grep -c '^p.*json$') done $(date -u +%T)"
