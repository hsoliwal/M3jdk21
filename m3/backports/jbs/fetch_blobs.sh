#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
# Atom BLOB_PREFETCH: fetch missing blobs from a promisor remote in chunks, the way promisor lazy fetch does (noop
# negotiation, --stdin oids), instead of one lazy round trip per blob. Stops when the object store drive is below MIN_MB.
# Usage: fetch_blobs.sh <repo> <remote> <oid list> [chunk]
set -uo pipefail
R="$1"; REM="$2"; LIST="$3"; N="${4:-4000}"; MIN_MB=2048; T=$(mktemp -d)
GIT_NO_LAZY_FETCH=1 git -C "$R" cat-file --batch-check < "$LIST" | awk '$2=="missing"{print $1}' > "$T/miss"
echo "missing $(wc -l < "$T/miss") $(date -u +%T)"; split -l "$N" -d -a 3 "$T/miss" "$T/c."
for c in "$T"/c.*; do
	free=$(powershell -NoProfile -Command "[math]::Floor((Get-PSDrive ${R:1:1}).Free/1MB)" | tr -d '\r')
	[ "$free" -ge "$MIN_MB" ] || { echo "STOP free=${free}MB below $MIN_MB"; exit 3; }
	git -C "$R" -c fetch.negotiationAlgorithm=noop -c gc.auto=0 fetch "$REM" --no-tags --no-write-fetch-head \
		--recurse-submodules=no --filter=blob:none --stdin < "$c" 2>&1 | tail -1
	echo "HEARTBEAT chunk $(basename "$c") rc=${PIPESTATUS[0]} free=${free}MB pack=$(du -sm "$R/objects/pack" | cut -f1)MB $(date -u +%T)"
done
GIT_NO_LAZY_FETCH=1 git -C "$R" cat-file --batch-check < "$LIST" | awk '{print $2}' | sort | uniq -c
rm -rf "${T:?}"
