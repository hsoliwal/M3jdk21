#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Component names of kits missing from the 69-kit harvest, from tree-only clones (no blobs read).
# Usage: harvest_addendum.sh <kits dir> > HARVEST_ADDENDUM.tsv   (columns as UNIVERSE_COMPONENTS.tsv: repo, component, path)
set -euo pipefail
cd "$1"
echo -e "repo\tcomponent\tpath"
for d in */; do
  d=${d%/}; repo=${d/_//}
  git -C "$d" ls-tree -r --name-only HEAD | awk -v repo="$repo" '
    match($0, /(^|\/)components\/[A-Z][A-Za-z0-9]+\//) { s=substr($0, RSTART, RLENGTH); n=split(s, a, "/"); name=a[n-1]; if (!(name in seen)) { seen[name]=1; print repo "\t" name "\t" $0 } next }
    match($0, /elements\/ons-[a-z-]+\//) { s=substr($0, RSTART+9, RLENGTH-10); if (!(s in seen)) { seen[s]=1; print repo "\t" s "\t" $0 } next }
    match($0, /^src\/kendo\.[a-z]+\.js$/) { s=substr($0, 11, RLENGTH-13); if (!(s in seen)) { seen[s]=1; print repo "\t" s "\t" $0 } next }
    match($0, /^packages\/ag-grid-community\/src\/[a-zA-Z]+\//) { s=substr($0, 32, RLENGTH-32); if (!(s in seen)) { seen[s]=1; print repo "\t" s "\t" $0 } }'
done
