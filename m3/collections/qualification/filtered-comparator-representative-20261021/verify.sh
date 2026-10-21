#!/usr/bin/env bash
# Standalone Java21 source-sealed regression; compile both exact old and new owners.
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../.." && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
crate="$root/m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-filtered-representative-20261021"
python3 - "$root" <<'PY'
from hashlib import sha256
from pathlib import Path
import sys
root = Path(sys.argv[1])
crate = root / "m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-filtered-representative-20261021"
manifest = [s.split("\t") for s in (crate / "manifest.tsv").read_text(encoding="utf-8").splitlines()
            if s and not s.startswith("#")]
expected = ["m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java",
            "m3/collections/src/test/java/com/m3/collections/M3FilteredComparatorRepresentativeProof.java"]
if len(manifest) != 2 or [row[0] for row in manifest] != expected:
    raise AssertionError("unknown or unordered source-guard targets")
for path, before, after, template in manifest:
    if before != "ABSENT" and (len(before) != 64 or any(x not in "0123456789abcdef" for x in before)):
        raise AssertionError("invalid before-source guard")
    if len(after) != 64 or any(x not in "0123456789abcdef" for x in after):
        raise AssertionError("invalid output-source guard")
    for entry in (root / path, crate / template):
        if not entry.is_file() or sha256(entry.read_bytes()).hexdigest() != after:
            raise AssertionError("target/recipe drift: " + str(entry))
previous = crate / "M3FilteredViews.before.java"
if sha256(previous.read_bytes()).hexdigest() != manifest[0][1]:
    raise AssertionError("regression preimage mismatches pinned parent PR")
print("Original and successor source pins: 3/3", flush=True)
PY
test_file="$root/m3/collections/src/test/java/com/m3/collections/M3FilteredComparatorRepresentativeProof.java"
helper="$root/m3/collections/src/main/java/com/m3/collections/M3SlotSpliterators.java"
product="$root/m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java"
mkdir -p "$work/before-source/com/m3/collections" "$work/before-classes" "$work/after-classes"
cp "$crate/M3FilteredViews.before.java" "$work/before-source/com/m3/collections/M3FilteredViews.java"
javac --release 21 -Xlint:all -Werror -d "$work/before-classes" \
    "$work/before-source/com/m3/collections/M3FilteredViews.java" "$helper" "$test_file"
set +e
java -cp "$work/before-classes" com.m3.collections.M3FilteredComparatorRepresentativeProof \
    >"$work/before.stdout" 2>"$work/before.stderr"
before_status=$?
set -e
if [ "$before_status" -eq 0 ]; then
    echo "Baseline unexpectedly passed the comparator-alias regression" >&2
    exit 1
fi
grep -F "generic Set must retain comparator membership" "$work/before.stderr" >/dev/null || {
    echo "Baseline failed for an unrelated reason" >&2
    cat "$work/before.stderr" >&2
    exit 1
}
echo "Original PR #1503 regression reproduced" 
javac --release 21 -Xlint:all -Werror -d "$work/after-classes" "$product" "$helper" "$test_file"
java -cp "$work/after-classes" com.m3.collections.M3FilteredComparatorRepresentativeProof
