#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Exact manifest/template/receiver byte custody; catalogue contracts stay in the recipe tests."""
import hashlib
from pathlib import Path

SCRIPT = Path(__file__).absolute()
for component in (SCRIPT, *SCRIPT.parents):
    if component.is_symlink() or getattr(component, "is_junction", lambda: False)():
        raise RuntimeError("aliased qualification path")
ROOT = Path(__file__).resolve().parents[4]
RECIPE = ROOT / "m3/collections/recipe/collection-file-review-batch80-20261017"
OWNER = RECIPE / "src/main/resources/com/synexia/rewrite/hash-pinned-text/collection-file-review-batch80"
EXPECTED = {
    "m3/collections/algorithm-catalog/README-collection-file-review-20261017.md": "f598ec356cd140e71a7d2c085c61ac857b8a7d8e8228723a85df7cfff60db4d9",
    "m3/collections/algorithm-catalog/SOURCE_INVARIANT_CONTRACT-20261017.md": "e80c95e72cdff72e8f14be43e70a768a503eaeea0b168a1ce6393696d51c1fe5",
    "m3/collections/algorithm-catalog/TASKS-collection-batch80-20261017.md": "86062da35ac684503c468118cf925b286be92cfd470e9bc10748f8e712659cae",
    "m3/collections/algorithm-catalog/collection-candidate-comparison-20261017.tsv": "e9d721fd062fe291f695a9dc39de2f4d90a725a49432cde5017ccac644c910cd",
    "m3/collections/algorithm-catalog/collection-catalogue-input-provenance-20261017.tsv": "e50ed945f9d64519744945895af73b0df57039d3079d603d59f4929c963997cf",
    "m3/collections/algorithm-catalog/collection-file-review-20261017.tsv": "fba70d976e0521bbcaffb7474499430454cffef323b0c2b6d606b856069f2be1",
}


def read_owned(path):
    relative = path.relative_to(ROOT)
    current = ROOT
    for part in relative.parts:
        current = current / part
        if current.is_symlink() or getattr(current, "is_junction", lambda: False)():
            raise RuntimeError("aliased input: " + relative.as_posix())
    if not path.is_file():
        raise RuntimeError("missing input: " + relative.as_posix())
    return path.read_bytes()


rows = [
    line.split("\t")
    for line in read_owned(OWNER / "manifest.tsv").decode("utf-8").splitlines()
    if line and not line.startswith("#")
]
if len(rows) != 6 or any(len(row) != 4 for row in rows):
    raise RuntimeError("expected six exact manifest rows")
if [row[0] for row in rows] != sorted(EXPECTED):
    raise RuntimeError("manifest owner/order/uniqueness drift")
for target, before, after, template in rows:
    if before != "ABSENT" or after != EXPECTED[target] or template != Path(target).name:
        raise RuntimeError("manifest lineage drift: " + target)
    payload = read_owned(OWNER / template)
    received = read_owned(ROOT / target)
    if hashlib.sha256(payload).hexdigest() != after or received != payload:
        raise RuntimeError("receiver/template drift: " + target)
print("PASS COLLECTION_REVIEW_BATCH80_RECEIVER: six exact catalogue recipe outputs")
