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

## A3 gate repair discovered by CI

Exact-head GitHub A3 run `37254813807` failed compilation before SAN could run.
The retained A3 algorithm and serial-work recipes lost `SourceFile` target typing
through generic `Tree.withId`/`withMarkers` calls. The `san-a3-compat` packet reuses
`com.synexia.rewrite.M3HashPinnedJavaSnapshotRecipe` with **module-relative** paths
(root: `m3/tooling/migration-recipes`). It preserves each metadata field through
explicit typed assignments, with no public descriptor or authority change.

Running the six actual retained A3 test methods then exposed absent-before outputs
being rejected by their own visitor. The repair records generated paths and
accepts them only when generation occurred, the manifest explicitly says ABSENT,
and exact postimage bytes still match. Changed-after-scan existing files still
refuse. The serial-work metadata test now separately requires OpenRewrite's
`RecipesThatMadeChanges` marker and exact preservation of all original markers,
matching the scheduler contract. No assertion is removed.

History: algorithm owner introduced at `be0ff2db6d7a199f8abdb43c71a1776c8dfe4c16`;
serial owner introduced at `25480599b306bdb23dcfbda5150f3f5101009df6`, API correction
`24a7eba6cec5c630a0c8c91089415ab2fc69d194`, then typed-ID correction
`f92246a99004b1a9cacb7f71c436331bb2d18a08`. This extends that correction and retains
the source seals and all previous gates. Initial refusal/test failures are retained.

The combined SAN task now has five JUnit tests and additionally runs all six
existing A3 test methods against the generated/compiled owners. The CI step also
compares all three generated A3 postimages with the actual installed files.
