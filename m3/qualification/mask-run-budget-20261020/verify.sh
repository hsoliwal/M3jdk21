#!/usr/bin/env bash
# Source-sealed standalone Java 21 proof; not a substitute for the product release gates.
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
product="$root/src/java.base/share/classes/jdk/internal/mindex/M3CollectionMaskCompactor.java"
test_source="$root/m3/qualification/mask-run-budget-20261020/src/test/java/M3CollectionMaskCompactorBudgetTest.java"
crate="$root/m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-mask-run-budget-20261020"
python3 - "$root" <<'PY'
from hashlib import sha256
from pathlib import Path
import sys

root = Path(sys.argv[1])
crate = root / "m3/tooling/migration-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-mask-run-budget-20261020"
manifest = crate / "manifest.tsv"
rows = [line.split("\t") for line in manifest.read_text(encoding="utf-8").splitlines()
        if line and not line.startswith("#")]
assert len(rows) == 2, "unexpected recipe target count"
for path, before, after, template in rows:
    source = root / path
    tpl = crate / template
    assert source.is_file() and tpl.is_file(), f"missing target/template: {path}"
    actual = sha256(source.read_bytes()).hexdigest()
    sealed = sha256(tpl.read_bytes()).hexdigest()
    assert actual == after, f"installed source drift: {path}"
    assert sealed == after, f"recipe template drift: {path}"
    assert before == "ABSENT" or len(before) == 64, f"bad before guard: {path}"
print("Sealed source + template postimages: 2/2")
PY
mkdir -p "$work/src/jdk/internal/mindex" "$work/base" "$work/test"
cp "$product" "$work/src/jdk/internal/mindex/M3CollectionMaskCompactor.java"
javac -Xlint:all -Werror --patch-module java.base="$work/src" \
    -d "$work/base" "$work/src/jdk/internal/mindex/M3CollectionMaskCompactor.java"
javac -Xlint:all -Werror --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED \
    --patch-module java.base="$work/base" -d "$work/test" "$test_source"
java --patch-module java.base="$work/base" \
    --add-exports java.base/jdk.internal.mindex=ALL-UNNAMED \
    -cp "$work/test" M3CollectionMaskCompactorBudgetTest
