<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# RENAME_DATA_FIX v1: m3/m3-import data and Java agree after the JDK naming rename (2026-11-07)

Follows NAMING_VAR_FIX v1 (`../naming-var-fix-20261107`, #2635), which made the tests compile. On that base
(`dc44fdbc244`) `mvn -f m3/m3-import/pom.xml test` ran 50 tests: 4 failures and 3 errors. All 7 come from the rename
`fe35ea98841` (recipe JDK_NAMING_RENAME). It renamed the Java but not the TSV data the Java reads (TSV is lineage,
outside its scope). It also missed two literal shapes: a path segment that ends the literal (`"m3/vendor/<word>"`, rule
`SEGMENT` needs a following `/`) and a dotted segment before a wildcard or the end (`rewrite.<word>.*`, rule
`SEGMENT_DOT` needs a letter after the dot).

## Rule for choosing the canonical side

- M3JDK names follow the naming law. This covers schema ids, header names, M3JDK paths, M3JDK owners and dispositions
  read by the renamed Java. Data is rewritten by the naming law itself: `jdk_naming.rewrite_line`, loaded from
  `make/m3/tooling/jdk-naming`, so data and Java come from the same rules.
- Donor facts stay as they are: the donor repository, the revision, donor paths and donor git blobs at that revision.
  com.synexia HEAD `3d5c6b08cd2` still has `synexia-*` modules, so these values are still true.
- Literal shapes that the rules do not cover are LITERAL rows in [STEPS.tsv](STEPS.tsv), with a count and a reason.

## Per-test decisions

| test | evidence | canonical | fix |
|---|---|---|---|
| `M3ImportPlanTest.plansFullDeltaAndStagesOnlyChangedCandidates` | `git ls-tree` master: `m3/vendor/m3` has 1366 files and `m3/vendor/<old word>` does not exist; every target must start with `m3/vendor/m3/` (`M3ImportManifest.Entry`) | data | `M3ImportPlan` stale walk and javadoc, plus the `M3ImportManifest` refusal message, now say `m3/vendor/m3` |
| `M3ImporterTest.producerAndReceiverShareKnownDeliveryRootVector` | producer com.synexia `M3DeliveryExportManifest` (blob `d62e95389ff2`, HEAD `3d5c6b08cd2`), compiled and run: target `m3/vendor/<old word>/A.java` gives `01b4e1f8…fc55` (the old pin), target `m3/vendor/m3/A.java` gives `a03b12ba…9d5`. The receiver gives the same value | fixture path (M3JDK) | vector `01b4e1f8804e2699c9133c3ca7dbf3f2841f2ff3991096778a6131cbc9c3fc55` -> `a03b12ba031692aa982fdd8edce0f015a369d03463c01d917377c0aa871e09d5` |
| `M3CanonicalFamilyPolicyTest.checkedInReceiverTableMatchesJavaClassifier` | `m3index-family-receiver.tsv` is the declarative twin of `M3CanonicalFamilyPolicy.Family`: authored by the receiver, not a donor blob, pinned nowhere (grep of its blob and sha256) | Java | whole table through the naming law (header `canonical_m3_owner`, prefixes `m3-…/`, owners `com.synexia:m3-…`, equal to the enum by construction) |
| `M3CanonicalRecipeHandoffTest.checkedInHandoffPinsCurrentCanonicalM3RecipeAssets` | `M3CanonicalRecipeHandoff` checks the schema id and that recipe owners start with `jdk.internal.m3.rewrite.`; it checks only the format of source path and blob | Java for columns 0 and 6, donor for the rest | naming law on schema and canonical_owner; repository, revision, donor source paths and donor blobs keep their bytes |
| `M3RecipeOwnershipPolicyTest.checkedInIntakeIsBoundToExactM3RevisionAndBlobs` | `SOURCE.tsv` is the receiver's custody receipt; its schema id is an M3JDK name | Java for column 0 | naming law on the schema id; donor repository, revision `d479e021`, paths and blobs stay |
| `M3RecipeOwnershipPolicyTest.checkedInM3LedgerAuditsEveryCurrentRecipeSource` | the renamed policy reads every column of the ledger by its M3 name; the recipe tree is `jdk/m3/rewrite/{a3,backport,m3,scope,semantic}` | Java for columns 0, 2, 3, 4 and the header | naming law on those columns; LITERAL `jdk.m3.rewrite.<word>.*` -> `jdk.m3.rewrite.m3.*`; test literal `"jdk.m3.rewrite.<word>."` -> `"jdk.m3.rewrite.m3."`. The ledger was a byte copy of donor blob `ca2962ae` (SOURCE.tsv still pins that blob, a donor fact); the local copy is now naming-law(ca2962ae), and `apply.py --check` recomputes it from that blob |
| `M3SeedSnapshotTest.checkedInSeedManifestMatchesEveryVendoredM3File` | reseal `d64d47f5e06` carried 12 target pins, but not the source paths or the `# root` line. The root left on master (`ca570e87`) is the root of the pre-rename rows; the current rows give `08d95a73`. The naming law over each donor source path equals its resealed target suffix in 15 of 15 rows. `M3EditScope` was left AMBIGUOUS by the reseal; naming law(pinned pre-rename bytes) = the current file exactly | resealed targets | naming law on source_path; M3EditScope sha `962d37d6…34ab` -> `d2156c4c…9bd5`; root recomputed: `ca570e874586c327f98e3982bf9e62a404e87e266b3195df69be06a0b5111ef6` -> `aca867c7bc1f7beda95f99641c68559cf03df5b0b60d6b9ac010214c4d3eaba8` (Python frames = receiver = test) |

## Residual (1 error, not from the rename)

`M3SeedSnapshotTest` still reports target hash drift on two rows: `M3HashPinnedJavaSnapshotRecipe.java` and
`M3ScopeInference.java`. Before the rename (`06495fea415`) both vendored files already differed from the seed pins
(`c4bbf313` and `17228a69` pinned, `d108e1de` and `8b0d400a` on disk). Commit `46ee4ad33e9` (2026-10-18) updated them
to newer donor bytes ("canonical runtime recipes 663e2cd") without updating the seed export pinned at donor
`62aea466`. The current files are exactly naming law(those 10-18 bytes). Re-pinning them would hide a content change
from the snapshot check and change the seed's donor revision, so that is an owner decision and this recipe leaves it.

## Recipe

```text
python -I m3/m3-import/qualification/rename-data-fix-20261107/apply.py          # CHANGED x10, then changed=0
python -I m3/m3-import/qualification/rename-data-fix-20261107/apply.py --check  # CHECK files=10 failed=0
```

[EDITS.tsv](EDITS.tsv) pins each file's preimage blob and sha256 at `dc44fdbc244` and its postimage sha256. The naming law
is read from the same commit (`jdk_naming.py` and `NAMING_RULES.tsv` at `dc44fdbc244`). A later rule therefore cannot
change what the recipe writes. The first `--check` after adding rule SEGMENT_DOT_END refused because the LITERAL row
for the wildcard owner found nothing left to replace. Gates are in [GATES.tsv](GATES.tsv).
