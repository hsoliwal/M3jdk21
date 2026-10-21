<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21 interop contract follow-up, 2026-10-20

Canonical home: `synexia-openrewrite-recipes/recipe-crates/m3-interop-contracts-20261020/` in `hsoliwal/com.synexia`.
Named recipe: `com.synexia.m3jdk.InteropContracts20261020`.
Canonical parent: `504f88d9783a7fab8772ba2d5f8ce4f45b25d564`.
Intended receiving parent: M3JDK21 `d1b9aa9f35cf6f8cf7af1fb581471c2551bd7e97`, the runtime-convergence change reviewed in [M3JDK21 #1616](https://github.com/hsoliwal/M3JDK21/pull/1616).

## Purpose and exact scope

The supplemental current-source interop build exposed old fixture compilation failures, incompatible oracle assumptions, and stale factory inventory metadata. This follow-up repairs those demonstrated failures while preserving the existing production Java/JNI owners. M3JDK21 remains the integrated runtime and product authority. The recipe is authored and qualified in Synexia before target receiving.

The final frontier contains **13 existing paths: 11 Java test files, one overview, and one metadata resource**. `provenance/TARGETS.json` records every exact preimage, postimage, Git blob, lane, and resource name. The original preimages are pinned to M3JDK21 `2779aba3f98f31c324be43f731be7914e1a05935`; the first read-only receiving check confirmed that all 13 still match the intended parent checkout. The final candidate-set SHA-256 is `05f397c6181992c354c6113aca7e2f9039bb8c835bef8e5c14a286ec526176eb`.

No production Java or JNI source, native ABI, collection algorithm, public factory signature, or compiler warning gate changes here. The `sdk-family-bindings.tsv` resource **does** intentionally change one observable metadata cell: the existing bindings loader will correctly report `M3ArrayList` as the Guava list-multimap values owner supplied by the unchanged factory. This correction is not a claim that all observable metadata values are unchanged.

| Finding | Contract-preserving repair |
| --- | --- |
| Java 21 compound `var` declaration, missing `Map` import, obsolete factory receiver, deprecated nominal predicate reference under `-Werror` | Correct the fixture syntax, import, actual owner reference and equivalent predicate expression. Preserve the Java 21 strict compiler flags. |
| Stable merge fixture interleaves equal-key occurrences from two runs | Expect the existing stable left-run-before-right-run order, preserving source occurrence indexes. |
| Guava mask contract assumes the native library is always absent | Exercise required-native selection when the library is available and preserve strict absence refusal when it is unavailable. Separate JVM observations establish both branches. |
| Spring factory assertions and mapped factory counts lag existing owners | Name the existing `M3SpringArrayMultiValueMap` owner and account for the already-present Guava BiMap and Apache BidiMap factories. The inventory remains bounded to 25 factory entries. |
| Spring seeded differential exits at a legitimate fixed-list growth exception | Execute both operations, compare runtime-exception classes, and retain the original 4,500-operation seed and all subsequent state assertions. |
| Commons HashBag 4.5.0 is used as an unconditional retain-all oracle despite a concrete implementation divergence | Preserve M3's documented minimum-cardinality rule. Use independent occurrence consumption for retain-all, keep the other five original real-donor operation comparisons, retain all 6,000 seeded inputs and post-state assertions, and add a direct donor-divergence fixture. |
| Guava values-owner metadata says `M3LinkedList` although the existing factory supplies `M3ArrayList::new` | Correct the same cell in the overview and existing packaged TSV. Reuse the current metadata loader and resource path. |

The additional explicit HashBag divergence fixture raises the current interop JUnit census from 158 to 159. No original test is disabled, no seeded sequence is shortened, and no mismatch is hidden by a broad exception catch. The finite interop checks do not establish complete donor-library substitutability, serialization, concurrency, resource superiority, or a receiving-phase promotion.

## Existing recipe machinery and execution

The four reusable SDK engine/support files, the five-test qualification source, and `receive.py` are copied byte-for-byte from the main runtime-convergence crate. `ENGINE_SOURCES.tsv` pins the existing Synexia engines at `10e8628b2479e56f6f841f160233d3a8bf04a6db`. `SOURCE_PROVENANCE.json` records the exact source host blobs; `upstream-host/` preserves the predecessor POM, descriptor, and notice. The POM changes only the task identity and removes the unrelated main-runtime contract profile. It retains OpenRewrite 8.17.1, Java 21, strict warnings, and the unchanged nonempty test gate. No new transformation engine is introduced.

The **actual final 13-target SDK run passed all five tests**, with zero failures, errors, or skips. It produced 13 SDK results bound to 36 verified source inputs. The Java lane parsed every one of the 11 Java preimages as a lossless `J.CompilationUnit`, including the originally uncompilable compound-`var` fixture. The original qualification suite verifies named-descriptor composition, exact replay, reversed input order, per-file mixed states, a full fixed point, and refusal for drift, missing or duplicate files, wrong representation, and post-scan changes. It also checks input-object immutability and refusal without partial result publication.

`evidence/sdk-attempt-02/` contains the actual command, process result and complete Maven log. `evidence/sdk-final-13/` retains the generated results, input seals, both lane patches, Surefire reports, and read-only receiver receipt. The earlier successful 12-target attempt remains unchanged under `evidence/sdk-attempt-01/` and `evidence/sdk-execution/`; its exact 34 qualified input files are preserved there. That historical attempt is superseded for receiving by the 13-target result.

With Java 21, Maven and Python available, run the existing machinery from this crate:

```sh
mvn -B -ntp -f pom.xml clean verify
python3 -B receive.py --target /absolute/path/to/M3JDK21 --check
python3 -B receive.py --target /absolute/path/to/M3JDK21
python3 -B receive.py --target /absolute/path/to/M3JDK21
python3 -B receive.py --target /absolute/path/to/M3JDK21 --check
```

After receiving, the repeated receive and final check must report zero changes. The unchanged receiver verifies the complete SDK input set and all target states before serial per-file atomic replacement. It is not a multi-file filesystem transaction if a later I/O operation fails; hold exclusive target write access. Never copy the sealed resources directly into product files to bypass SDK execution or receiving.

## Qualification and admission boundaries

The correction owner executed fresh isolated interop `clean verify` builds against the real pinned SDK dependencies, with 159 tests and zero failures, errors, or skips in each Java-fallback and native-present mode. Every Java class in the 13-target build matches its already-qualified 12-target counterpart; only the packaged metadata changed. Separate executions ran the existing binding inventory main (152 checks in each mode) and confirmed actual native availability in the same JVM as the Guava contract (26,421 checks and 6,400 steps in each mode). The exact commands, dependency/source/class identities, all historical failed attempts, and complete 12/13-target reports are retained under `evidence/interop-correction/`. These are peer execution receipts, distinct from this crate author's SDK execution.

Independent Apache review evidence is under `evidence/interop-correction/apache-review/`: 294 multiplicity cases, 90 explicit donor divergences, the original step-702 mismatch, and all 6,000 operations against an occurrence model with 51,079 assertions. Spring's independent probe and source pins remain in the correction evidence. Its original 4,500-step sequence produced 209 matching runtime exceptions. These observations explain the oracle changes; they do not independently admit the target receiver.

Actual root receiving and received-checkout verification have also **passed**. The unchanged receiver applied all 13 SDK results and then repeated with zero changes. Fresh Java-fallback and native-present `clean verify` executions on the received checkout each passed 159 tests, with zero failures, errors, or skips, stable source seals, and freshly generated classes. The existing binding inventory main passed 152 checks over 5 families and 25 bindings. The final receiver check reported 13 targets and zero changes. `evidence/receiving-20261020/RECEIPT.json` binds those root executions and their complete reports; this crate author independently checked both XML totals and receipt hashes without relabeling them as the author's executions. `catalogue/EVALUATION_RECEIVED_20261020.tsv` and `memory/observations/RECEIVED_13_20261020.json` append the current disposition. Historical pending capture events remain immutable.

The existing hosted workflow on the main product PR refused an already-existing source pin before running recipe or runtime tests. `evidence/hosted/HOSTED_WORKFLOW_BLOCKER.json` identifies the workflow, exact expected and actual engine blobs, unchanged parent evidence, and [failed hosted run](https://github.com/hsoliwal/M3jdk21/actions/runs/39441310580). The old six-recipe/55-test admission packet needs its own qualified current-source successor. This follow-up does not replace its pin, change its census, skip it, or claim a hosted pass. Installed-image, jtreg, whole-platform, comparative performance and ordered String/arrays/collections/AST-compiler gates remain distinct.

## Catalogue and reusable correction memory

Read `RECIPE_MEMORY.tsv` and `memory/LESSONS.md` before selecting or repeating an atom. Preserve failed candidates and use linked successors; append later execution observations instead of rewriting historical states. The per-file before/after identities make this a bounded serial recipe, and its meaningful fixture atoms remain independently reviewable without changing production ownership.

The complete correction-memory packet contains 13 immutable events and is sealed by `PACKET_SHA256SUMS`. `evidence/memory-copy-validation/RECEIPT.json` records this author's actual read-only validation of all 395 copied packet files, closed event schemas, canonical event bytes, digest links, literal trigger observations, and 73 exact referenced artifacts. The packet retains the correction owner's 26 positive/negative profile checks; the copied-packet validator does not relabel those retained checks as a new execution. Its static validator source is retained alongside the receipt. Separate SDK and received-checkout observations are outside the historical packet checksum list and remain included in the final full crate frontier.

`catalogue/PROBLEMS_20261020.tsv` adds relevant problem categories using GitHub, LeetCode, HackerRank and GeeksforGeeks. The incumbent M3 rule is compared first against the exact donor API and concrete source. Cardinality-preserving intersection is suitable for the Bag contract; duplicate-eliminating intersection is rejected. Borrowed-list exception behavior, stable merge ties, strict native absence, and exact runtime inventory form separate contract categories. Challenge and tutorial links contribute taxonomy and adversarial inputs only; no statement, editorial, or submitted solution is copied. Runtime performance is not inferred from asymptotic labels or fixture timings.

The main runtime catalogue and lookup-source provenance are retained byte-exact under `catalogue/inherited/`, with their original statuses and source pins in `provenance/INHERITED_SOURCES.json`. The correction packet adds its own dated decisions rather than rewriting that accumulated record. Reuse the existing array-backed `M3ProblemCatalogue` inspection CLI and existing canonical catalogue/search owners. For example:

```sh
rg -n 'multiset_intersection|borrowed_list|REJECTED_CONTRACT' catalogue
java -cp /path/to/catalogue-classes org.synexia.m3.recipe.M3ProblemCatalogue catalogue/PROBLEMS_20261020.tsv validate
java -cp /path/to/catalogue-classes org.synexia.m3.recipe.M3ProblemCatalogue catalogue/PROBLEMS_20261020.tsv find area multiset_intersection
```

The CLI remains an inspection tool; the existing qualified SDK engines hold transformation execution. Neither a catalogue hit nor a passing probe grants replacement or phase-promotion authority. Exact donor/source rights and inherited public engine evidence remain attached under `provenance/`, `NOTICE.md` and `licenses/`.
