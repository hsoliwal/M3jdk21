#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
# Scaffold for M3JDK21 backports from JBS (bugs.openjdk.org, public JIRA REST API): every fixed Enhancement of
# JDK 22..27, one row per issue. Classification (backportable to 21? already in jdk21u? owner lane) is the fill.
# Usage: jbs_enhancements.sh > JBS_ENHANCEMENTS_22_27.tsv
set -euo pipefail
API=https://bugs.openjdk.org/rest/api/2/search
printf 'release\tjbs\tcomponent\tsubcomponent\tpriority\tlabels\tsummary\tdisposition\n'
for v in 22 23 24 25 26 27; do
	q="project = JDK AND issuetype = Enhancement AND resolution = Fixed AND fixVersion = $v ORDER BY key"
	start=0
	while :; do
		page=$(curl -s -G "$API" --data-urlencode "jql=$q" --data-urlencode "startAt=$start" --data-urlencode 'maxResults=500' \
			--data-urlencode 'fields=summary,components,labels,priority,customfield_10008')
		n=$(printf '%s' "$page" | jq '.issues | length')
		[ "$n" -gt 0 ] || break
		printf '%s' "$page" | jq -r --arg v "$v" '.issues[] | [$v, .key, ((.fields.components // [])[0].name // "-"),
			(.fields.customfield_10008.name // "-"), (.fields.priority.name // "-"), ((.fields.labels // []) | join(",")),
			(.fields.summary | gsub("[\t\r\n]"; " ")), "UNCLASSIFIED"] | @tsv'
		start=$((start + n))
	done
done
