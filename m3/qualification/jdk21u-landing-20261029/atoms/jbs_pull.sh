#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
# Atom JBS_SNAPSHOT: pages one JQL query out of the public JDK Bug System REST API (bugs.openjdk.org, no login) into
# immutable JSON pages <out>/<name>/p<NNNN>.json (1000 issues each) plus <out>/<name>.tsv:
# key, issuetype, status, resolution, fixVersions, affectsVersions, components, labels, priority, resolved,
# backport_of (the "backport of" link for Backport issues), summary. Re-runnable: pages already on disk are kept.
# Usage: jbs_pull.sh <out dir> <name> <jql>
set -uo pipefail
OUT="$1"; NAME="$2"; JQL="$3"; D="$OUT/$NAME"; mkdir -p "$D"
F=key,summary,issuetype,status,resolution,fixVersions,versions,components,labels,priority,resolutiondate,issuelinks
total=$(curl -s -m 120 -G https://bugs.openjdk.org/rest/api/2/search --data-urlencode "jql=$JQL" --data-urlencode maxResults=0 |
	python -I -c 'import json,sys; print(json.load(sys.stdin)["total"])') || { echo "$NAME COUNT_FAILED"; exit 2; }
for ((at = 0; at < total; at += 1000)); do
	p=$(printf '%s/p%04d.json' "$D" $((at / 1000))); [ -s "$p" ] && continue
	for try in 1 2 3; do
		curl -s -m 600 -G https://bugs.openjdk.org/rest/api/2/search --data-urlencode "jql=$JQL ORDER BY key" \
			--data-urlencode startAt=$at --data-urlencode maxResults=1000 --data-urlencode fields=$F -o "$p.part" &&
			python -I -c 'import json,sys; json.load(open(sys.argv[1], encoding="utf-8"))["issues"]' "$p.part" 2>/dev/null && { mv "$p.part" "$p"; break; }
		sleep 20
	done
	[ -s "$p" ] || { echo "$NAME PAGE_FAILED at=$at"; exit 2; }
done
python -I - "$D" "$OUT/$NAME.tsv" <<'EOF'
import glob, json, sys
d, out = sys.argv[1:3]; rows = {}
j = lambda xs, k="name": ",".join(sorted(x.get(k, "") for x in (xs or [])))
for p in sorted(glob.glob(d + "/p*.json")):
    for i in json.load(open(p, encoding="utf-8"))["issues"]:
        f = i["fields"]; bp = ""
        for l in f.get("issuelinks") or []:
            if l.get("type", {}).get("name") == "Backport" and "outwardIssue" not in l and "inwardIssue" in l: bp = l["inwardIssue"]["key"]
            elif l.get("type", {}).get("name") == "Backport" and "outwardIssue" in l and f["issuetype"]["name"] == "Backport": bp = bp or l["outwardIssue"]["key"]
        rows[i["key"]] = [i["key"], f["issuetype"]["name"], f["status"]["name"], (f.get("resolution") or {}).get("name", ""),
                          j(f.get("fixVersions")), j(f.get("versions")), j(f.get("components")), ",".join(sorted(f.get("labels") or [])),
                          (f.get("priority") or {}).get("name", ""), (f.get("resolutiondate") or "")[:10], bp,
                          " ".join((f.get("summary") or "").split())]
with open(out, "w", encoding="utf-8", newline="\n") as o:
    o.write("key\tissuetype\tstatus\tresolution\tfixVersions\taffectsVersions\tcomponents\tlabels\tpriority\tresolved\tbackport_of\tsummary\n")
    for k in sorted(rows, key=lambda k: int(k.split("-")[1])): o.write("\t".join(rows[k]) + "\n")
print(len(rows))
EOF
echo "$NAME total=$total rows=$(($(wc -l < "$OUT/$NAME.tsv") - 1))"
