# Current merged-source inventory and recovery inputs

## Finding

All **503 original source-packet paths are present** at commit
`be92c62ece9023b5c33676716a1076d00e26120a`, tree `3c4f32b66633a251ba2c117090830252a7e2da03`.
Every selected current body is available locally and matches the current tree's exact Git blob and byte length.
The complete file-byte comparisons are:

| Relationship between original 0b, published d1 and current be92 | Files |
|---|---:|
| All three complete bodies identical | 480 |
| Published postimage retained, differing from original | 2 |
| Current body differs from both earlier complete bodies | 21 |
| Exact reversion to original among the published changes | 0 |
| Missing original packet path | 0 |

Thus current matches published d1 for **482 of 503** files and original 0b for **480 of 503**.
The two retained production postimages are `CompetitiveCapabilityPlanIndex.java` and
`DonorMechanicalShapeCatalog.java`. The original nine changed-production-file denominator remains historical;
the current 21-file difference set is a distinct baseline and is not assigned the old proof results.

`CURRENT_SOURCE_MANIFEST.json` contains all 503 exact repository paths, current Git entries,
complete tree trails, local body locations, byte lengths, SHA-256/Git identities, original/published
body identities, actual byte-comparison results and POM-declared roles.
Its SHA-256 is `20fd857cb00711e02c34cd27445bd2c057db8f57a0cafb8ed9a455b453f3b37c`.
`SOURCE503_INPUT_SEALS.json` binds **1,239** files, all stable at final inspection.
The exact current path trails use 173 complete content-verified Git tree objects for the 503 rows.

Current bodies were obtained through 482 reused existing bodies whose Git blobs match the current tree,
8 exact current acquisitions shared by the source owner, and 13 current acquisitions made by this inventory.
The current contents are compared as complete bytes. Equality of a hash, a commit ancestor or a filename
is not promoted to semantic equivalence or test qualification.

## Current build roles

| Scope | Exact declaration/evidence | Meaning for recovery |
|---|---|---|
| Published JCC main configuration | 443 explicit source includes; 17 selected bodies differ at be92 | This is the historical proof design to reconcile with current owners. |
| Current JCC POM | Path exists, 16,616 bytes, 91 explicit main includes | The older POM form is present; the published 443-include form was not retained. |
| Current parent main configuration | 88 explicit main source paths | Includes ten additional MIndex regex/search owners outside original503. |
| Current parent RE2/J execution | 19 explicit vendor source paths, release 8 | Preserve the separate current execution and exact vendor source identities. |
| Current parent Surefire configuration | Mastery convergence test and RXMTest | Both selectors are explicit obligations of the current parent task. |

`PARENT_BUILD_CONTEXT.json` maps **109 distinct paths**: 88 main sources + 19 vendor sources +
both test classes. Of those, 98 are in original503 and 11 are additional. Its SHA-256 is
`4f8fbf09070e4d45e42c7ac29c94401d70058fea04d98e44b3526a24c77912fc`. The earlier helper list `PARENT_DECLARED_CONTEXT.json`
contains 107 compiler paths plus the new RXMTest; the full map also includes the already-selected
Mastery test and should be used when assembling the parent context.

The current parent POM also disables incremental compilation. Its RE2/J execution uses the existing
repository vendor sources under their release-8 compilation policy. The newer RXM suite and the
parent POM were read and captured; they were not pruned, rewritten, compiled or executed here.
RXMTest declares three test methods. This is a declaration count, not a passing test count.

### Additional main dependencies

The current Mastery owner directly uses `M3MasteryClassLoader` and `M3MasteryContractSurface`.
Their exact current bodies are captured as additional canonical owner inputs. The source owner's
independent helper/API review assesses their behavior and contract implications.

The current Graph owner also directly constructs `PSource`. Graph is included by the published
443-source configuration, so PSource is a compilation dependency even though the new scan option
defaults to off. PSource's full body was read. Its only direct repository owner dependencies found
are Graph, `M3RecipeFirstSourceCoverageTable` and `sealed.SealHash`, all already among the 443 includes.
The remaining types are JDK/OpenRewrite or nested types within PSource.

Accordingly **443 + 2 Mastery helpers + PSource = 446** is the bounded current main-source preparation
arithmetic from these changes. It is not compiler-resolved closure or runtime acceptance.
Provider/resource obligations and the other current owner reviews remain part of the source owner's
frozen recovery design. It is also distinct from any designated-source count used by the receiving handoff.

The ten new parent MIndex sources and RXMTest were fully read for direct dependency availability.
No additional direct canonical owner beyond original503 plus those eleven additions was found.
RE2/J Pattern is a real compile dependency of MIndexRegexSearch, and RXMTest also uses the vendor
Matcher and PatternSyntaxException; those owners are within the captured nineteen vendor sources.
The parent-closure review records the precise owner memberships and external provider limits.

### Inventory-only context

`M3RecipeLaboratoryFixtures` is in original503 for source inventory but is excluded from the published
JCC main includes. Its new direct references to `Lfs`, `PJSeal` and `M3RecipeProjectLab` are captured
as bounded optional context. `M3RecipeLaboratory` was already referenced before this merge and is
separately labelled as a pre-existing owner outside the selected packet. These four bodies were not
silently added to the active JCC compiler graph, and their transitive laboratory graph was not expanded.

## All current selected changes

The following 21 rows are complete-file differences from published d1. Each also differs from original0b.
`CURRENT_SOURCE_CHANGED.tsv` gives the full paths and exact original/published/current SHA-256 values.
The manifest and complete diffs supply the full identity and source evidence.

| File | Relevant reviewed role | Current bytes |
|---|---|---:|
| CompetitiveProblemReview.java | Published JCC main | 48141 |
| DevelopHistoryAuditor.java | Published JCC main | 50772 |
| DevelopHistoryWriter.java | Published JCC main | 15181 |
| ChallengeDonorPassLedger.java | Published JCC main | 33102 |
| synexia_mindex.h | Native header | 13313 |
| NativeLibrarySpec.java | Published JCC main | 4151 |
| pom.xml | Current parent POM | 16380 |
| M3AtomPatternMasteryConvergenceTest.java | Current parent test | 61816 |
| M3ApplyVerifiedPrivatePrimitiveAtomsRecipe.java | Published JCC main | 9016 |
| M3AtomizeRecipe.java | Published JCC main | 4777 |
| M3HashPinnedJavaSnapshotRecipe.java | Published JCC main | 21621 |
| M3OpenRewriteRuntimeRecipeInventoryRecipe.java | Published JCC main | 6188 |
| M3OpenRewriteTranspiler.java | Published JCC main | 27278 |
| M3PreparePrivatePrimitiveAtomsRecipe.java | Published JCC main | 4354 |
| M3PrivatePrimitiveAtomSupport.java | Published JCC main | 40181 |
| M3RecipeCrateSerialAtomReview.java | Published JCC main | 23936 |
| M3RecipeLaboratoryFixtures.java | Inventory-only fixture owner | 16573 |
| M3RecipeMasteryLab.java | Published JCC main | 42415 |
| M3RepositorySemanticGraphRecipe.java | Published JCC main | 35641 |
| M3SemanticCodeInventoryRecipe.java | Published JCC main | 36683 |
| CodeIdentity.java | Published JCC main | 1364 |

The six-owner report fully reviews the current atom preparation/application/support, fixtures and Graph
changes. It identifies source-pattern-aware scanning and visiting, structural helper reuse,
bounded local/branch candidates, additional fixture-spectrum structures and the opt-in PSource wiring.
Those are present current changes requiring current proof; they are not automatically superseding
implementations or authority to restore older whole owners.

The additional selected history auditor/writer changes add per-parent merge-materialization records,
bounded read-only Git inspection, exact entry-mode/type/blob comparison and recovery/summary output.
Their new references remain within their existing selected owners and JDK/progress contracts.
The current native-library specification adds bounded UTF-8 path-file resolution after an explicit path
and before logical-name selection; invalid configured path files fail instead of selecting a different
library. CodeIdentity's delta removes an unused import. These observations describe source changes;
no current API or behavioral equivalence gate ran.

The current JNI header adds an opaque persistent CUDA-session type and create/filter/destroy declarations.
Their implementation, ABI and platform execution are not qualified by a header inventory. The prior
native/JNI gates remain unrun and this review creates no fresh native result.

## Protected fixture and instructions

The canonical upstream `M3DocumentationAttributionExecutionTest.java` is present at be92 with the exact
original 5,637-byte body, Git blob `3354b97e964b170b88df1ff8964e863d728ce9af`, SHA-256
`dbc50c7a436b4bee61fffbfec3104b981850d15f8d052f7517b3b0945476e8d2`.
It was compared with the retained original protected fixture bytes.

The copied JCC upstream fixture is absent: the complete current JCC `src` tree lacks the `upstream`
component. Its preserved original and published 6,070-byte postimage are separately identified in
`CURRENT_CONTEXT_MANIFEST.json`. No fixture was edited, recreated or used to claim a current pass here.

Root AGENTS is present and byte-identical across original, published and current: 15,464 bytes,
Git blob `7038981e6d86a5e3bae25803fa0b6d77b557d538`, SHA-256
`086b03459e4f441d0ecf1c90773dd63877c03956957ec0f41ac7ceb90069c0f9`.
It was read in full. All 191 additional ancestor AGENTS paths in this bounded target scope are absent
in complete current trees. The instruction inventory records each exact absence trail.
The recipe-first, preserved-contract, NRD/history and exact-evidence rules continue to apply.

## Verification and limitations

Final inspection revalidated **715** selected/supplemental/instruction paths through complete,
content-verified current Git trees: 523 present and 192 absent. The absent total is one copied fixture
plus 191 ancestor-instruction paths. All 523 present target bodies are available, with exact current
Git blob and byte-length matches. All 1,239 sealed original/source comparison inputs remained stable.
The original 503 denominator is not inflated by additional helpers, tests, POM context or instructions.

Only metadata retrieval, exact body acquisition/reuse, source/XML reading, byte comparison and review
artifact generation ran. There were no builds, JUnit runs, recipe runs, source/POM/fixture/gate edits,
branch mutations or publication operations in this task. No historical proof epoch was overwritten.
The source owner's earlier exact original-source proof remains 91 core tests, 8 parent tests and
67 upstream tests with 66 passing and one error; the later JNI gates were unrun. Those results do not
qualify current be92 bodies or the new parent suite.

This inventory is not whole merged-repository acquisition, a fully resolved Maven model, complete
dependency review of every module, current compiler/API equivalence, or current JNI/full-module
acceptance. The canonical module POM and module mirror review belongs to a separate runtime-admission
audit. New current-baseline recipes, exact protected fixtures, providers, gate commands and receipts
must be frozen and executed by the source owner before stronger current claims are made.

## Correction record

The initial source handoff wrongly described the JCC POM path as absent. That statement is corrected:
the path exists with the older 91-include body, while its published 443-include form was not retained.
Both exact POM bodies and their roles are now bound. The initial manifest and seals remain under
`amendments/initial-503-handoff`; `AMENDMENT.json` records the supersession and verifies that every
original/published/current body identity and exact path proof for all 503 rows is unchanged.

## Artifact map

- `CURRENT_SOURCE_MANIFEST.json`: exact current/original/published identities for the original 503 paths.
- `SOURCE503_INPUT_SEALS.json` and `SOURCE503_SEAL.json`: the selected-source comparison seals.
- `CURRENT_CONTEXT_MANIFEST.json`: separate fixtures, current JCC POM, required helpers, parent additions and optional context.
- `PARENT_BUILD_CONTEXT.json` and `BUILD_ROLE_MAP.json`: exact current parent and historical/current JCC declarations.
- `CURRENT_INSTRUCTIONS.json`: current root AGENTS and complete ancestor-path absence evidence.
- `CURRENT_SOURCE_CHANGED.tsv` and `selected-diffs/`: all 21 differing selected files and their source deltas.
- `owner-delta-review/`: independent six-owner/PSource review and separate ten-source/RXM direct-closure review.
- `INVENTORY_REVIEW.json`: machine-readable final observations and explicit scope limits.
- `FILE_SEALS.json`: final artifact identities; the seal file excludes itself.
