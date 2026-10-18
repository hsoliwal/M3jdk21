<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 array and collection contract refinement

This canonical Synexia task crate reuses the existing Java and text snapshot recipes, with unchanged engine sources. It contains finite, source-sealed array and collection refinements, their ordered intermediate history, a final independent delivery dataset, and searchable catalogue evidence. The first checkpoint was published at `11ac8a842ef832ceaed9365b7d70c71df7368432`; its portable array custody remains pinned to that exact source. Later passes extend this same crate.

The array dataset repairs scalar indexing across empty joined segments in `M3Arrays`, restores its two required snapshot segment-count methods, and keeps the existing native comparator test executable in Release builds. Four generated files include the existing Java owner, the native test, and two focused Java regression tests. Public interfaces and production JNI symbols remain unchanged.

The final collection dataset contains sixteen files: six existing production owners, two corrected existing test oracles, and eight added regression files. It restores legal navigation outside bounded filtered views, preserves stored representatives and callback order, enforces the existing read-only contract on default and sequenced mutators, and removes proven deep-filter stack overflows by reusing existing ordered conjunction and explicit-continuation patterns. Two existing implicit public constructors are made explicit for strict owning-module compilation. These changes add no runtime dependency or production native symbol.

## Synexia M3 invariant and finite review scope

Retain reusable, validated transformation recipes; apply their actual SDK Results through contract-preserving, serial per-file transformations; and refine algorithm choices through the existing searchable category and donor catalogues. A behavioral atom may be replaced independently when the enclosing public contract and composed behavior remain established. Source fingerprints select comparison candidates; they do not authorize a mutation or establish semantic equivalence.

The pinned input inventory contains 248 files: 116 collection production Java files, 106 collection Java tests, nine collection native owners, two collection native tests, and fifteen files in the array closure. Every file has an exact source seal and existing-owner catalogue entry. Ten existing files have explicitly named operations evaluated in this pass, and ten new regression files have final postimage fingerprints. All other operation reviews remain `PENDING`, including unreviewed operations inside selected files. This is a finite proving-module pass, not a claim that every collection or JDK family has been exhaustively refined.

## Reused owners and source epochs

`ENGINE_SOURCES.tsv` pins all four exact current engine sources to Synexia `10e8628b2479e56f6f841f160233d3a8bf04a6db`. `HARNESS_LINEAGE.json` identifies the previously qualified AST/build harness at `1d890f4c332bf2b548ddd4a1ef14dd599b4238bc`. This crate adapts its task identities, display strings, Maven coordinate and receipt label; it preserves the Java SDK qualification class, sealing/receiving algorithms, and refusal gates.

The M3 receiving baseline is `hsoliwal/M3JDK21@e913ab89056657eca4009678cfe9a657c020e79b`. Dataset resources retain exact preimages and postimages. Unknown source bytes are refused. The receiver applies actual SDK Results serially after validating every bound input and target; replay at the fixed point changes no files or timestamps.

| Dataset | Targets | Role and source epoch |
| --- | ---: | --- |
| `fixture` | 6 | Machinery qualification only; Java and native/text lanes |
| `m3jdk21-arrays` | 4 | Final array delivery from the pinned M3 baseline |
| `m3jdk21-collection-build` | 2 | Explicit existing constructors |
| `m3jdk21-navigation` | 4 | Historical first navigation pass; two later counterexamples retained |
| `m3jdk21-collection-oracles` | 1 | Independent sorted-tree test oracle repair |
| `m3jdk21-view-successor` | 9 | Source-sealed successor of navigation; bounded/null/callback fixes, List composition and read-only guards |
| `m3jdk21-collections` | 14 | Historical consolidation before the final generic-view pass |
| `m3jdk21-generic-views` | 3 | Source-sealed successor for generic decorator iteration and membership |
| `m3jdk21-collections-final` | 16 | Final independent collection delivery from the original pinned M3 baseline |
| `synexia-collection-shapes` | 1 | Data-only append of four shapes in the existing canonical Java catalogue |
| `synexia-collection-ledger` | 2 | Append 23 decision/donor rows to the existing two canonical TSV owners |
| `m3jdk21-array-proof-v2` | 122 | Qualified public custody and review metadata, pinned to checkpoint 1 |

All rows above passed actual SDK 5/8/9 gates. `m3jdk21-array-proof` is a separate retained **refusal**, not a qualified row: two runnable Java evidence files initially sat outside the existing Java owner's admitted source layouts. The corrected version moves those exact bytes under `src/test/java`, records the delivery aliases, and retains the unchanged admission policy. No receiving writes occurred for the refused version.

`datasets/m3jdk21-collections-final/COMPOSITION.json` checks every intermediate before/after transition and selects only the last actual SDK Result for each path. The earlier navigation stage is retained as history, with its discovered callback and null-probe counterexamples. Use the final sixteen-file dataset when replaying a complete collection delivery. Applying a superseded stage to final source correctly refuses its unexpected source epoch.

## Reproduce the finite recipes

Use Java 21, Maven 3.9 or newer, and Python 3. From this crate in a canonical Synexia checkout:

```sh
mvn -B -ntp -Dm3.dataset=fixture -Pcontract verify
mvn -B -ntp -Dm3.dataset=m3jdk21-arrays -Pcontract verify
mvn -B -ntp -Dm3.dataset=m3jdk21-collections-final -Pcontract verify
```

The POM pins OpenRewrite 8.17.1. Supply `-Drecipe.owner.root=/absolute/path/to/synexia-openrewrite-recipes` when the existing owner is outside its canonical location. Add `-o` when dependencies are cached. Each dataset writes its own `target/<dataset>` output. For a fresh repeated contract execution, use a new `-Dm3.contract.output=/absolute/new/evidence` directory.

Each qualified dataset passed five actual SDK tests, eight input-sealing tests, and nine isolated receiver cases. These exercise Java LST and native/text lanes, exact replay, source order, mixed pre/postimages, fixed point, missing/duplicate/drift inputs, wrong representation, post-scan changes, recipe/result tampering, symlink refusal, and file modes/timestamps. `evidence/<dataset>/SDK_QUALIFICATION.json`, source/result bindings and raw reports retain the executed scope. The exact machinery fixture remains distinct from product runtime evidence.

After qualification, receive into an isolated M3 checkout:

```sh
python3 receive.py --dataset m3jdk21-arrays --target /absolute/m3-checkout --check
python3 receive.py --dataset m3jdk21-arrays --target /absolute/m3-checkout
python3 receive.py --dataset m3jdk21-collections-final --target /absolute/m3-checkout --check
python3 receive.py --dataset m3jdk21-collections-final --target /absolute/m3-checkout
python3 receive.py --dataset m3jdk21-arrays --target /absolute/m3-checkout
```

A source-sealed successor is required for a changed preimage. Correct reviewed recipe inputs, execute the SDK gates, then receive their retained results. Per-file atomic replacement does not imply a transaction across multiple concurrently edited files.

## Algorithm comparison and receiving evidence

`CATALOGUE.tsv` uses the established `M3_COLLECTION_VIEW_ALGORITHM_DECISIONS_20261017.tsv` vocabulary. `catalogue/arrays-evaluation.json` records the authoring comparison: an existing first-party upper-bound atom was selected because it finds the first cumulative end strictly greater than the ordinal in O(log s) time and O(1) scratch while retaining the existing descriptors. An arbitrary duplicate match is incorrect; linear rewind loses the logarithmic bound. Existing Synexia and M3 owners are pinned. Guava and the three challenge platforms provide contract and adversarial-shape evidence; no challenge/editorial/submission source is copied and no Guava runtime dependency is introduced.

`catalogue/views-successor-evaluation.json` and `catalogue/generic-views-evaluation.json` retain each file's candidate comparison, donor pins, rejected alternatives, costs, counterexamples, and admission limits. Frozen authoring receipts retain their original statuses; `catalogue/FINAL_RECEIVING_STATUS.json` resolves completed recipe and standalone receiving gates and identifies later whole-module gates separately. Existing central decision and donor TSV owners receive append-only rows through their own qualified dataset.

The existing `M3ProblemShapeCatalogue` gains four data entries: segmented upper bounds, live filtered navigation, ordered filter composition, and read-only collection guards. Generic recursion elimination reuses its existing `dfs` shape. The unchanged original probe and an additional observer confirmed that all 36 prior serialized entries/reviews and public callable descriptors remain equal; 565 search queries retain the previous members' ordering. New entries are searchable and use the existing donor-review owner. None grants source-copy or promotion authority.

`catalogue-proof/README.md` explains the source inventory and executable search/reproduction path. The unchanged `AtomCatalogCli` fingerprints all 248 baseline files in 28 independent bounded shards, and twenty final changed/new postimages in five more shards. It retains exact, structural, logic, SimHash, LSEM, and DESM candidate signals and existing indexed SQL semantics. The original whole-inventory 50,000-node refusal is preserved; bounded serial partitioning keeps that cap unchanged. Public custody retains compact per-file/per-method indexes, exact roots, and the existing-owner reproducer; bulky SQL artifacts are regenerated when needed. No merged catalogue-root authority is invented.

The frozen authoring evaluation retains its original pending reception markers. `catalogue/arrays-receiving-status.json` resolves those gates against the actual SDK-generated receiver and links the separate runtime receipts. On that receiver, Java, `-Xcheck:jni`, and interpreter JNI runs each passed 417,595 checks across 912 cases. The bounded resource probe used nine fresh JVMs: the unique-boundary comparison retained identical cold allocation and owner geometry, and two million warm scalar reads allocated zero bytes. These observations establish the measured scope; no throughput improvement is claimed.

Final standalone collection proofs use actual received sources and a source-built M3 Java image. They retain the 65,536-depth, `-Xss256k` witnesses, the 18,779-byte generic callback/source/identity event tape, the 2,537-byte sorted/map tape, compact boundary contracts, and public callable comparisons. The facade now explicitly declares two inherited sequenced removal methods; declaration/reflection ownership changes are disclosed even though the callable descriptor closure is unchanged. Generic membership deliberately preserves its original repeated scans: one always-visible element may demand `d(d+1)/2` callbacks. The change establishes stack safety in the admitted known-owner chain; it makes no linear-time membership claim.

Opaque third-party recursion, arbitrary callback traces after foreign range rejection, deeply nested sorted range construction, concurrent owner mutation, and whole-JDK acceptance remain separate admission boundaries. The final original owning Maven module passed 214 JUnit tests across 61 suites in each required Java and native mode, with zero failures, errors or skips. Unique run roots sealed all 249 owning inputs, checked all sixteen qualified postimages, and retained 441 identical compiled class files across modes. The API gate accounted for all 116 source units. Exact accepted receipts are under `evidence/runtime/collections-owning-module/`; earlier shared archives with stale generated classes/reports remain excluded from acceptance.

The fixture qualifies transformation machinery. Native ABI, product behavior, allocation, hosted CI, non-Linux platforms, and full JDK acceptance have their own gates. String remains the active ordered receiving family; these separately licensed proving-owner repairs do not promote arrays or collections into the global JDK family sequence.

`NOTICE.md`, `LICENSE.txt`, `FOSS_REUSE_DECISION.tsv` and retained per-file headers preserve first-party Apache-2.0 attribution and donor boundaries. M3 runtime classes gain no Synexia runtime dependency.

## Filtered navigation successor from the current M3JDK21 source

The `filtered-navigation-20261018` dataset is a nine-file continuation from `hsoliwal/M3JDK21@2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`. It uses the same unchanged four engine sources, input sealer, actual SDK tests and receiver. Five named behavioral atoms in `M3FilteredViews` and `M3IndexPathMasks` repair comparator-middle null navigation, stored-key entry membership, strict sorted-set sequenced mutation refusal, known generic decorator stack overflow and output aliasing with state-mask vectors. Three added regression Java files and four text qualification/workflow inputs complete this finite delivery. Native sources and the public callable API closure remain unchanged.

`datasets/filtered-navigation-20261018/` retains the selected category comparisons, exact donor pins, source inventory and failed baseline witnesses. The inventory contains 203 exact locally present baseline support files; only the five named production atoms have been reviewed in this pass. Every other operation remains pending. The corrected entry-membership callback path is disclosed as a contract repair, while the ordinary unaffected-read tape is retained separately. Repeated tiered/interpreter runs are the same corpus and are not summed as independent coverage.

The separate `filtered-navigation-ci-20261018` dataset has two text targets in Synexia at `140c263a90f9803b9c46103c63355a286e0515f0`: the already active focused workflow and its one missing row in the existing workflow custody catalogue. It creates no workflow, provider or harness. All other catalogue bytes remain unchanged.

Both datasets use the existing five SDK, eight preparation and nine receiver gates. Reproduce with `mvn -B -ntp -Dm3.dataset=filtered-navigation-20261018 -Pcontract verify` or the CI dataset name, then run `receive.py --check` against the named baseline/postimage repository. Keep dataset inputs immutable after qualification and use a fresh contract output directory for another run. Exact execution outcomes are recorded outside those inputs in `evidence/filtered-navigation-20261018/` and `evidence/filtered-navigation-ci-20261018/`; hosted CI and product runtime acceptance remain separate receipts. Historical datasets, catalogue decisions and evidence are preserved.

The successor ordinary-read tape excludes both repaired entry membership and null-sentinel comparator demand; the latter can add one actual stored-boundary comparison. Historical strict empty sequenced endpoints on Map-derived views remain unreconciled in the unchanged outer JDK wrappers. The Set-focused guard repair does not establish universal endpoint closure; a Map factory replacement requires separate concrete-wrapper, marker and delegation admission.
