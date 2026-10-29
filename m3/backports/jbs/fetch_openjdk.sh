#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
# Atom UPSTREAM_FETCH: blobless fetch of openjdk/jdk master + jdk-2x tags into a work repo whose objects/info/alternates
# points at the M3jdk21 clone (so the shared JDK 21 history is not fetched twice). Usage: fetch_openjdk.sh [repo] [log]
set -uo pipefail
R="${1:-/k/m3-work/tmp/m3jdk-jbs/work.git}"; L="${2:-/s/m3-scratch/m3jdk-jbs/logs/fetch_openjdk.log}"
date -u > "$L"
( while sleep 240; do echo "HEARTBEAT $(date -u +%T) $(du -sm $R/objects/pack 2>/dev/null | cut -f1)MB free=$(df -m /s | awk 'NR==2{print $4}')MB"; done ) >> "$L" &
HB=$!
git -C "$R" -c fetch.negotiationAlgorithm=skipping -c gc.auto=0 fetch --no-tags --filter=blob:none --no-write-fetch-head openjdk \
  '+refs/heads/master:refs/remotes/openjdk/master' '+refs/tags/jdk-2*:refs/tags/jdk-2*' >> "$L" 2>&1
echo "rc=$? $(date -u)" >> "$L"; kill $HB
git -C "$R" rev-parse refs/remotes/openjdk/master >> "$L"; du -sm "$R/objects/pack" >> "$L"
