# SAN recipe packet

This packet extends the existing migration inventory with `--scope repository`.
The old `scan(repo, commit)` and CLI default retain family selection. The new mode
keeps every tracked item as an unresolved obligation; it never sets semantic,
dependency or production admission flags true.

Run `mvn -o -B -ntp -f m3/tooling/migration-recipes/tasks/san/pom.xml package`
with Java 21 and the recorded Maven cache. The existing OpenRewrite text recipe
checks exact preimages, generates the three postimages under `target/generated`,
proves a fixed point and rejects drift, omission and duplication. The third postimage wires the same proof into the existing A3 workflow and preserves
all its earlier gates. It adds no rewrite engine. Python is only the existing Git accounting adapter; semantic analysis stays
with the existing Synexia Java inventory.

Run `python3 -m unittest -v test_inventory.py` from `m3/migration` after applying the
reviewed recipe. All original tests remain; new cases cover non-MIndex obligations,
binary/native/template/dormant inputs, exact commit isolation, symlinks, submodules,
content budgets and absence of semantic/admission claims.

The original inventory survives unchanged from commit
`584932a5f631bf8eb40b9525d18184085be1a404` through target base
`db2017c1aa4aace99543417fba79e3f7a307c26b` (Git blob
`f51ce78b6a2fa786aae248b058a90cb4cbf05b32`). Its legacy family behavior is preserved.
The task's dependency and toolchain receipts distinguish online cache preparation
from the final offline package. This does not close full-reactor, product-image or
source-estate semantic admission.

See [whole-Synexia scope](../../../../docs/whole-synexia-sanitization.md).
