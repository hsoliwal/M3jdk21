# Final source root and retained contract accounting

The final source snapshot is bound to `hsoliwal/com.synexia` commit `d1cf2d81ee74a4a837f78e0cd2e4ccae2b3d4906`, root tree `6f9b6a48f8cefa7a643f222c7aa1f621497725e7`, and `refs/heads/aix/jcc-canonical-integration-20261005`. This review independently rehashed the complete root and rewrite package tree, reconstructed and rehashed every intervening tree, and rehashed each retained source body. No source binding is inferred merely from an old commit hash.

`ROOT_ACCOUNTING.json` contains all input receipt paths and SHA-256 seals, complete 343 root entries, all 201 root-POM declarations, the six complete reconstructed intermediate entry sets, nine exact body bindings, and the four unchanged old mapped artifact objects to preserve in `previous_sources`.

## Root and Maven accounting

- Final and old root trees each independently match their expected Git tree SHA. Both have 343 immediate entries: 255 trees, 87 blobs, one gitlink. Names, modes and types are identical.
- Exactly four root subtree object IDs changed: `m3-fast-search`, `synexia-algo`, `synexia-donor-inventory`, `synexia-openrewrite-recipes`. Every other root entry is exactly equal.
- Root `pom.xml` is final blob `98889a040aad5cc3c82f782bb4b173d118207988`, 119981 bytes, SHA-256 `f01cca4d90bd8fa1b1125474b5178cc80762d934bcb7bf5817f08f3f0dcf6aab`. The actual local body matches both final and old root entries.
- Fresh XML parsing yields 201 unique module paths: 190 direct paths and 11 nested paths across eight nested roots; 194 unique immediate-root components. There are 200 project declarations and one `m3-tornado` profile declaration. The entire ordered declaration/context list equals the prior E2 declaration artifact, and every root component exists in the final root.
- These are declarations, not proof of profile activation, acquisition/build of every module POM, complete reactor closure, destination export or delivered capability.

## Independently reconstructed final tree chain

The actual final root and package receipts are complete and nontruncated. The publication readback receipt records the child bindings of the intermediate trees. For each intermediate tree, this review started from its complete rehashed old entry set, replaced only the child SHAs recorded by the final readback, and recomputed the final Git tree SHA. Every computed SHA equals the corresponding final published SHA. A guessed unchanged sibling would cause a mismatch; no such mismatch occurred.

| Path | Verified final Git tree |
| --- | --- |
| root | `6f9b6a48f8cefa7a643f222c7aa1f621497725e7` |
| `synexia-openrewrite-recipes` | `d8106554d43b3b9fa0d25d5eb1b89b90ebc28a91` |
| `synexia-openrewrite-recipes/src` | `dc4cff8f68b4f138f333bfdb9eab938d6d277388` |
| `synexia-openrewrite-recipes/src/main` | `75636030562e4f373a64436dce092c780adcdf99` |
| `synexia-openrewrite-recipes/src/main/java` | `bee9798cb41dee193ff909a2afd3a01be438c918` |
| `synexia-openrewrite-recipes/src/main/java/com` | `14d3cc911d4028a99dd21c5357da35f779d07fb8` |
| `synexia-openrewrite-recipes/src/main/java/com/synexia` | `d629188abe30d99fee38ceade5d20a044320332e` |
| `synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite` | `284cac67103e667d93846eeb92d83cf99f1daed1` |

The rewrite package receipt has 1358 immediate entries. Commit-to-root and branch membership rely on the recorded `SOURCE_IMPLEMENTATION_PUBLICATION_VERIFIED.json` verification; this read-only task made no remote call and did not acquire/recompute the raw commit object. The discovered local final-repo Git database does not contain the published final commit/tree objects; no local HEAD was substituted for the final publication identity.

## Retained bodies and contracts

All four existing mapped owner bodies remain byte-identical to their exact current-0b preimages: `M3RecipeMasteryLab`, `M3RecipeMasterySchedulePlanner`, `M3AtomPatternSignalChainRecipe`, and `M3CodeSignalChain`. Their old mapped objects are included intact in the JSON for future lineage preservation, including old commit, ref, role, empty signatures and null fingerprint. The final active source artifacts may be repinned to the verified final branch/commit while retaining those old objects.

All four `M3TranspilePass`, `M3TranspilePassResult`, `M3TranspilePlan`, and `M3TranspileRun` bodies are also byte-identical to exact old-tree entries and source acquisition hashes. `M3OpenRewriteTranspiler` is changed: final blob `fd2134586b5369a0dcd62d207dbc6208168dd72f`, SHA-256 `cc4e0b24f50b0a6f462c8aa1ff1ddee2eea67d6043158b9e7a8f5a1e91b9e4c9`; its body/contract review belongs to the parent source-review task.

Fresh full reads in this task covered the four Transpile records and the schedule planner. Their contracts remain:

- `M3TranspilePass(String id, Recipe recipe)`: nonblank, NUL-free stripped ID and nonnull recipe.
- `M3TranspilePassResult(int ordinal, String passId, String recipeName, String sourcePath, String beforeSha256, String afterSha256, boolean changed)`: positive ordinal, required text, lowercase 64-hex hashes, changed flag consistent with before/after hash equality.
- `M3TranspilePlan(Kind kind, String sourcePath, List<M3TranspilePass> passes, String structureSha256, String instanceSha256)`: normalized path, immutable pass list and syntactically valid hashes; `compile(Kind, String)` derives structure and instance fingerprints, `passCount()`, and `run(String, List<Path>)` delegates to the shared transpiler. Kinds remain IDENTITY, IOP_MECHANICAL, JAVA_MECHANICAL_CANDIDATE, JAVA_MECHANICAL_STATIC_ANALYSIS_CANDIDATE.
- `M3TranspileRun(String sourcePath, String inputSha256, String outputSha256, String output, List<M3TranspilePassResult> passes)`: immutable receipt list, contiguous ordinals, common path, unbroken hash chain, final pass/output-hash equality; `changed()` compares hashes and `toTsv()` emits safe receipt fields. Its constructor does not itself recompute the output string hash; direct record construction is not a new correctness oracle.
- `M3RecipeMasterySchedulePlanner.plan(List<String>, int)`: at most 16 recipe IDs and 10000 schedules; deterministic exhaustive nonempty subsets/permutations when budget permits, otherwise complete ordered single/pair coverage with optional longer schedules. It refuses budgets smaller than complete ordered-pair coverage. The Plan record returns false from mutationAuthority() and promotionAuthority().

The larger three unchanged bodies carry forward the prior current-0b full-body review through exact byte equality; this task does not claim another full read of those three. Existing stored compiled API excerpts for AtomPatternSignalChainRecipe (14 class blocks) and MasteryLab (10 class blocks) match exactly between prior candidate and final replay evidence. No fresh javap, compilation or test was performed. No separate compiled API evidence is claimed here for the other retained owners.

## Scope and unresolved qualification

All requested root, POM, tree-chain and nine source-body bindings are resolved in local evidence. This does not qualify source export, destination materialization, destination gates, remote delivery, JNI parity or whole-repository correctness. The source publication receipt remains blocked at the required upstream runtime gate and later JNI gates remain unexecuted. These blocked acceptance flags must not be promoted by source rebinding.

Only files in this review directory were written. No source edits, recipe changes, builds, tests, remote requests or Git writes occurred.
