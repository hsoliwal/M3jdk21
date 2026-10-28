<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21 runtime convergence, 2026-10-20

Canonical destination: `synexia-openrewrite-recipes/recipe-crates/m3-runtime-convergence-20261020/` in `hsoliwal/com.synexia`.
Named recipe: `com.synexia.m3jdk.RuntimeConvergence20261020`.
Original M3JDK21 witness/preimage revision: `d6d76b563050fc13c4fa79bc494edf29f1614b37`.
Preserved receiving parent: `2779aba3f98f31c324be43f731be7914e1a05935`; all 29 preconditions still match. See `provenance/RECEIVING_PARENT_UPDATE_20261020.json` and the dated upstream review under `evidence/final-review/`.

## Status and scope

This additive Synexia task recipe refines existing M3JDK21 Java/JNI collection and Eclipse runtime owners. M3JDK21 remains the integrated Java/HotSpot/JNI product and runtime authority. The manifests bind 29 reviewed targets: 19 Java files and 10 native/text files. `provenance/RECEIVING_FILES.json` records each exact preimage and postimage. Actual SDK execution, receiving and target acceptance have separate retained receipts.

Progress callback forwarding, OSGi service lifetime, filtered/sorted views and checked JNI masks reuse their current owners. Preserve the String -> arrays -> collections -> AST/compiler ordered receiving gates. This task does not advance them.

The Maven host/test/receiver comes from canonical Synexia develop pinned once at `f04fbac91a7891e64b77c7b24cef1ab75ec88144`. `SOURCE_PROVENANCE.json` lists exact host blobs; `upstream-host/` retains those original files. The POM binds this task's identity, exact engine custody copy and received-owner contract profile. Existing Java and native/text engines remain byte-identical to the canonical source identities in `ENGINE_SOURCES.tsv`.

## Reviewed implementation changes

| Owner or gate | Refinement |
| --- | --- |
| `M3FilteredLists` | Preserve underlying iterator invalidation at nonempty, exhausted, empty and nested provider boundaries; retain writable copy-on-write backing policy. |
| `M3FilteredViews`, `M3FilteredNavigableSet` | Restore strict read-only endpoint refusal and live backed sorted reversal. |
| `M3LongIndexProjection` | Reuse stable equal-value run reversal for known opposite sorted order, sharing payload and allocating the final index array. |
| Java/C `M3IndexMaskCompactor` | Share private checked offset/window emitters; omit payload-region reads for wholly rejected chunks; preserve existing JNI signatures. |
| `M3EclipseProgressMonitor` | Guard synchronous callback reentry; preserve fractional credit, exception identity and retries including nested failure. |
| `M3EclipseRuntimeActivator` | Preserve service context during real registration/unregistration callbacks and defer stop during registration; invoke framework callbacks outside the activator monitor. |
| Regression and qualification | Add provider, endpoint, order, mask, SubMonitor, Equinox and JDK array-mask oracles; repair source-JUnit launcher provenance and require fresh owning Java/JNI/Eclipse compilation. |

Public callable interfaces remain unchanged. `M3FilteredNavigableSet` explicitly overrides inherited `removeFirst`/`removeLast`; reflection over declared methods can observe those added declarations. Endpoint/invalidation fixes intentionally correct demonstrated violations of the existing provider contracts. Original failing witnesses are retained. The proposed early threshold exit in the JDK's internal array compactor is rejected by its cost comparison; only its stronger independent regression fixture is received. Current `java.util.ArrayList`, `java.util.Vector` and their internal compactor remain unchanged.

## Populate and execute

For each sorted, unique target append a manifest row containing target path, actual before SHA-256 (or explicit ABSENT), reviewed after SHA-256 and a simple resource filename. Put exact preimages under `pre/<resource>` and exact postimages beside the manifest. Java belongs in the Java LST lane; native C and supported text belong in the text lane. Do not copy payloads into the product manually.

The four existing engine/support sources are pinned at `10e8628b2479e56f6f841f160233d3a8bf04a6db`. Current Synexia develop has evolved three of those sources. Both canonical and receiving builds default to the exact `sdk-owner/` copy, preserving the tested identity while the shared owners continue to evolve. `provenance/PUBLIC_ENGINE_PREEXISTENCE.json` confirms all four exact sources already exist in the public receiving base. With Java 21, Maven 3.9.9, Python 3 and the Linux C17 toolchain:

```sh
mvn -B -ntp -f pom.xml clean test
python3 receive.py --target /absolute/path/to/M3JDK21 --check
python3 receive.py --target /absolute/path/to/M3JDK21
python3 receive.py --target /absolute/path/to/M3JDK21
mvn -B -ntp -f pom.xml -Pcontract -Dm3.contract.target=/absolute/path/to/M3JDK21 verify
```

The last receive must report zero changes. `--check` admits both preimages and postimages; CI additionally requires `changes=0` for an already-received checkout. The unchanged five-test SDK suite runs the named descriptor, lossless LST replay, per-file mixed states, reversed input order, a complete fixed point, and drift/missing/duplicate/wrong-representation/post-scan refusal. Retain actual generated `target/candidate/` output, SDK_INPUTS.tsv, SDK_RESULTS.tsv, both lane patches and executed Surefire reports.

The contract profile runs the existing owning collection qualifier, source-builds its ten dependency modules, builds all ten existing collection JNI libraries, exercises Java fallback and required-native execution, installs the tested collection artifact, and runs the real Eclipse modules. It compiles an exact current-source java.base patch for the independent array-mask oracle. Fresh compiler outputs and source identity checks are required. These executions retain their declared module/source-patch scope; a full installed M3JDK21 image, jtreg, whole-platform and comparative resource gates remain distinct.

The unchanged receiver verifies every actual execution input and every target state before serial per-file atomic replacement. This is not a multi-file filesystem transaction if I/O fails midway. Keep exclusive target write access.

## Search and donor reuse

Before applying or correcting this recipe, read `RECIPE_MEMORY.tsv` and `memory/LESSONS.md`, including rejected attempts and their linked successors. The immutable `memory/events/` records retain atom identities, applicability, exact source/diff/fixture pins and observed outcomes under `SYNEXIA-MOP-RECIPE-MEMORY-1`. The capture records deliberately retain unknown historical commit/timing information and pre-receiving statuses; subsequent receiving and publication observations are appended separately.

`catalogue/INHERITED_DONOR_CATALOGUE.tsv` is the complete unchanged historical source from M3JDK21's `m3/collections/algorithm-catalog/wrapper-convergence-20261018/index-candidates/CATALOGUE.tsv`; dated new decisions are separate in CATEGORY_REVIEW_20261020.tsv, PROBLEMS_20261020.tsv and EVALUATION_PASS_02.tsv. SCOPE_COVERAGE.tsv preserves the full discussed scope and its unimplemented or unqualified consumers. No inherited decisions are overwritten.

Use existing searchable owners, without creating a new global catalogue framework:

- Canonical `AlgorithmCatalogue.search`, `PinnedAlgorithmDonorCatalog.require/revisions/byKind`, `ProblemCategoryCrosswalk.index` and `SerialProblemReview`; exact source API pins are in INHERITED_LOOKUP_PROVENANCE.tsv.
- The existing target inspection CLI `make/m3/recipes/src/main/java/org/synexia/m3/recipe/M3ProblemCatalogue.java` directly accepts PROBLEMS_20261020.tsv. Run its `validate`, `stats`, or `find area filtered_views` commands. All rows are metadata links only; separate donor provenance governs any source reuse.
- `rg -n 'FILTERED_LIVE_VIEW|monitor_callbacks|service_lifetime|JNI_ORDERED_MASK' catalogue` provides bounded TSV lookup with no added runtime.

Example using the unchanged target inspection CLI:

```sh
javac --release 21 -encoding UTF-8 -d /tmp/runtime-catalogue-classes /path/to/M3JDK21/make/m3/recipes/src/main/java/org/synexia/m3/recipe/M3ProblemCatalogue.java
java -cp /tmp/runtime-catalogue-classes org.synexia.m3.recipe.M3ProblemCatalogue catalogue/PROBLEMS_20261020.tsv validate
java -cp /tmp/runtime-catalogue-classes org.synexia.m3.recipe.M3ProblemCatalogue catalogue/PROBLEMS_20261020.tsv find area filtered_views
```

WORK_ORDERS_20261020.tsv has inventory-only pending fields, never placeholder digest strings. WORK_ORDER.schema.json describes inputs to the existing `M3SerialFileAtomConvergenceRecipe.plan` boundary. Populate real compiler/API and executable behavior identities before constructing qualified work orders. That planner always denies mutation/replacement/promotion authority; the sealed implementation recipes and independent gates remain required. Curated local category names are not claimed to be native challenge-platform labels.

LeetCode, HackerRank and GeeksforGeeks provide categories and adversarial shapes only. No challenge body/editorial/submission source is included. Existing first-party owners and pinned Guava/Eclipse/Boost references take precedence over newly discovered donors. Apache predication and Spring filtering mutation semantics remain separate.

## Canonical publication mapping and rights

Publish this crate's POM, receiver, test, task resources, dated catalogue, contracts, source provenance, evidence and notices under the canonical directory above. `sdk-owner/` is the exact pinned custody copy used in both canonical and target builds; `upstream-host/` retains original-source history. The existing evolving engines remain at `synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/`. The receiver mirror is `m3/recipes/runtime-convergence-20261020/`.

Append this new family to `CANONICAL_RECIPE_HOME.tsv` using actual resulting Git blob identities. Do not invent blobs before a commit is formed. Preserve develop ancestry and publish only the exact qualified task packet; do not add a dedicated workflow merely to obtain a green label.

Copyright 2026 Hitesh Soliwal and contributors. First-party recipe/test/configuration expression is Apache-2.0. Preserve original payload/donor/OpenJDK headers, NOTICE and per-file licenses; recipe licensing does not relicense generated target sources. The JDK oracle retains its authored GPLv2/Classpath terms, with exact licence text in licenses/. Public disclosure scope is routine maintenance of already-public owners and existing engine copies, recorded in provenance/PUBLICATION_SCOPE.md. No new runtime dependency on Synexia is introduced.
