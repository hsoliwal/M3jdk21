<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Upstream scope supplement: runtime convergence review, 2026-10-20

## Review identity and evidence boundary

This append-only observation supplements the historical `catalogue/SCOPE_COVERAGE.tsv` and `EVALUATION_PASS_02.tsv`; it does not rewrite their earlier observations. The reviewed M3JDK21 upstream revision is **`2779aba3f98f31c324be43f731be7914e1a05935`**, compared with the previous receiving parent **`73f59218dc4ef1a3ef79aad990e77273db2a2d60`**. The original witness/preimage source revision remains **`d6d76b563050fc13c4fa79bc494edf29f1614b37`**.

`UPSTREAM_REVIEW_20261020.json` records this review's actual read-only Git-object checks, 32 source/document seals, source counts, and all 29 recipe precondition comparisons. Every precondition still matches the pinned upstream commit. The new upstream commits do not overwrite one of the 29 intended receiving targets. They do change other collection owners used by those targets, so the final receiving contract must execute against the preserved current checkout.

Source inventories, source-custody records and previously published qualification reports below are **inherited records inspected in this review**. This review did not execute the NLP, PCollections, Guava or downstream interop suites. Their status is not borrowed to make this runtime-convergence pass green. Fresh receiving/qualification results belong in separately dated executed receipts.

## Newly present scope that the earlier inventory did not include

| Area | Preserved current upstream owner and record | What the inspected record establishes | This pass's disposition |
| --- | --- | --- | --- |
| Full mapped file family | `m3/ports/file`; `m3/docs/name-mapping.d/m3-file-nlp-search.json`; `m3/tooling/module-packs/nlp-search/FILE_PORT_MAP.json` and `FILE_API_PARITY.json` | The mapping accounts for 50 Synexia file-family sources and 709 public/protected source declarations or record components. The module README identifies 80 Java sources including support/module descriptor and three C workers. Snapshots, source facts, token stores, mapped images, generation-bound dictionaries and byte facts now have concrete separate port owners. | Preserve these owners and exact canonical lineage. The old statement about the narrow stateless `m3/file` owner remains accurate for that older module, but no longer describes all file capability now present in the repository. This review does not requalify all mapped behavior. |
| M3AST, Lucene and OpenNLP source-search consumer | `m3/tooling/module-packs/nlp-search/frontend/src/main/java/org/m3jdk/tools/search/M3SourceIndex.java`; module-pack README, `INTEGRATION_CUSTODY.json`, `SOURCE_REF.json`, `FILE_PROBES.json`, `FRONTEND_PROBES.json` | Concrete source/proof machinery connects file snapshots to real compiler callbacks, OpenNLP spans and Lucene disk indexing. Custody binds canonical commit `c4ae7e361c9253aaced47e843549a06e505d924d` and 145 receiving targets; donor source provenance separately pins Synexia `50f48cbbe11f17fb8dc209bac3812f8940a1e267`. The documented pack builds ten JMODs with Lucene 9.10.0 and OpenNLP Tools 2.4.0 plus bounded fork backports. | Preserve the concrete consumer rather than propose another generic search owner. The README describes executable gates and distinguishes a stock-VM compiler-source proof from a complete M3JDK21 build. Reading that contract or custody record is not executing its `RESULT.json` run. Eclipse Search UI/workspace integration, complete upstream suites, other platforms and full modified-runtime acceptance remain separate. |
| Persistent collections and views | `m3/collections/pcollections`; `provenance.json`, README and bounded reactor | The new additive module reuses existing M3 collection wrappers and mask JNI. It includes persistent sequence/hash/sorted/ordered owners and immutable index views. Provenance pins the unchanged MIT PCollections donor `hrldcpr/pcollections@1f2212e7bf935451ea421ca3d499ef3c96c45e5c`; its source and original tests stay in `m3/vendor/third_party/pcollections`. | This is inherited implementation, not a new runtime-convergence change. `M3PListView` directly consumes the modified mask selector, and its fixtures should receive a fresh downstream check. Its tests and image proof are separate from the core collection qualifier. Persistent-module presence does not promote a `java.base` backend or complete the collection family. |
| Guava ArrayListMultimap support | `m3/collections/qualification/guava-native-array-list-multimap-20261018/FULL_QUALIFICATION_REPORT_V2.json` and README | The retained report records 6,064 unchanged Guava cases, 4/4 canonical map, 4/4 receiver map, 4/4 receiver list and 23/23 interop checks for its own pins. Upstream adds `M3ArrayList.trimToSize(boolean)` and lazy expected-key reservation to `M3LinkedHashMap`, retaining old entry-point behavior. | Preserve the new source and provenance. These counts are inherited report values, not this pass's rerun. The final core qualification must compile and exercise the new shared-owner versions. |
| Guava HashBiMap and interface qualification | `m3/collections/qualification/guava-native-hash-bimap-v7-20261018/HASH_BIMAP_FULL_QUALIFICATION_REPORT_V7.json`; `guava-bimap-interface-20261018`; `guava-class-to-instance-interface-20261018` | The HashBiMap report declares `QUALIFIED_NATIVE_HASH_BIMAP_JAVA21` with `goal_complete: false`; the receiving README records 10,601 sole-provider tests for that earlier artifact. M3JDK21 receives custody/qualification metadata while concrete Guava source lives in its separate fork. | Preserve the existing reports, native-fork records and interface distinctions. No reexecution, new Guava implementation or all-family acceptance is claimed by this review. |
| Challenge catalogue and correction memory | `m3/collections/algorithm-catalogue/challenge-patterns-20261018`; `AGENTS.md`; `m3/docs/mop-recipe-improvement-memory.md`; `m3/mop/SELF_IMPROVING_RECIPES.md` | The new catalogue preserves 14 official LeetCode/HackerRank references as taxonomy and explicitly imports no solution source. The new policy requires canonical recipe-owned append-only correction memory, including failed attempts and source/evidence seals. | Reuse these records before later algorithm work. This task's canonical memory must retain its own observed corrections/rejections and exact regression evidence; the historical catalogue is not rewritten. No new engine or global runtime memory service is authorized. |

The new NLP/file mapping explicitly preserves public `java.lang.String` payload authority and the active **String qualification** stage. None of these inherited modules or records advances the global receiving order.

## Changed qualification closure

At the pinned current upstream commit, core collection production Java files increase from **268 to 270**, and collection test source files from **254 to 259** before this recipe's additions. The current owning qualifier discovers those inventories dynamically, invokes clean builds and retains actual test totals; its source list is not limited to the older count.

The six production paths changed upstream are `M3ArrayList`, `M3CollectionSdkCatalog`, `M3LinkedHashMap`, `M3LongIndexPathBudget`, `M3ProgressMonitor` and `M3ShadowStreams`. The five new test sources cover trim epochs, map capacity, long path budgets and shadow streams. `M3ProgressMonitor` now delegates its additive shadow-stream defaults to `M3ShadowStreams`; that dependency is in the same discovered collection source root and uses `java.base` APIs.

No upstream changes in this comparison affect the Eclipse core/framework source inventories or the three current `java.base` sources used by the array-mask oracle. Applying the 29 sealed postimages gives the exact declared Eclipse seal inventories. This source check does not replace fresh compilation and the actual final 16/21 Eclipse outcomes.

The older **475-test** collection result remains an accurate historical witness for its original candidate. Final current-checkout counts must come from the new owning run, including the added upstream tests. Compilation/test execution assumes one source writer; the root qualifier must not run while source changes are being made.

## Existing downstream checks to reuse

After installing the freshly qualified core collection artifact, the current PCollections POMs support these separate clean commands with the same Java 21/Maven/local-repository environment:

```sh
mvn -B -ntp -f m3/vendor/third_party/pcollections/pom.xml clean install
mvn -B -ntp -f m3/collections/pcollections/pom.xml clean verify
```

The unchanged donor adapter explicitly requires JUnit Platform Console Standalone 1.12.2 and AssertJ 3.27.7. The M3 module uses the source-built JUnit 5.10.2-m3-source-1 and invokes four existing vector/hash/derived/view fixture groups. `M3PViewsContract` additionally has `--java`, `--absent` and `--native` modes; required-native mode checks actual loading and Java/native parity. Reuse the qualified collection library and newly compiled module/test classes for those modes. These commands are identified here, not claimed executed.

The separate `m3/collections/interop/pom.xml` also directly consumes the modified mask/index/view owners. Its 44 test source files are outside the core module's test root. `clean verify` uses its existing Guava 33.4.8-jre, Eclipse Collections 11.1.0, Spring Core 6.2.0 and Commons Collections 4.5.0 dependencies; optional native branches can use the same built JNI directory with `-Xcheck:jni`. `m3/tooling/module-packs/collections/pom.xml clean verify` supplies another existing filtered-view consumer fixture without invoking its separate image profile. Each additional execution needs its own receipt and actual results.

The optional Eclipse JMOD builder is a larger, separate six-module packaging gate. Its existing strict source/descriptor refusals remain authoritative; this supplement does not authorize bypassing them or equate a linked module image with a complete modified-JDK build.

