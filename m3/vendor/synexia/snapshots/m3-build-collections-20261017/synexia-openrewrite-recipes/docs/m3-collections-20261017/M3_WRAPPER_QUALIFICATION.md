# Filtered and sorted collection qualification

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

## Delivered source changes

Two existing Synexia owners received bounded candidates from exact preimages. Public signatures, source generation checks, callback order, exception precedence, reference identity, range behavior and defensive exports remain the existing contracts. No legacy/admission/live/snapshot/native owner was treated as an alias of a different contract.

| Owner and source atom | Preserved boundary | Qualified final SHA-256 |
| --- | --- | --- |
| `M3FilteredViews.NavigableSetView.descendingIterator` removes intermediate private view construction | Retains exactly `nav.descendingSet().iterator()` followed by the existing filtering cursor; does not substitute the backing `descendingIterator()` method. | `48bcb4d3dd3d7ebcfda5ba502e1ae59322254009b622b229d96a8ea349426f68` |
| `M3FilteredViews.SortedMapView.isEmpty` removes private entry-set/export-iterator construction | Retains source `entrySet`, iterator, key/value access and predicate order; no exported entry or mutation semantics change. | Same final file above. |
| `PrimitiveIndexView.filterLane` and `filterStateMask` reuse an exact-fit result array | Only the array freshly allocated within that call is reused, and only when every projected row passes. Source rows and exported arrays never alias the result. Partial selections still copy. | `c0380232909fe1f3056b2a1438d544c4a22e4290c35a603d6851e8eff68ffe2d` |

The baseline is `hsoliwal/com.synexia@11c94feb18155a53cc213708c353791132edc4a7`. Exact originals, including original CRLFs, were materialized before execution and verified by both Git blob SHA-1 and SHA-256. `M3_WRAPPER_BASELINE_CUSTODY.tsv` records 38 exact sources, tests, focused POMs and native inputs.

## Recipe application and fixed point

Every method atom was qualified independently using the existing `M3HashPinnedJavaSnapshotRecipe` on the real OpenRewrite 8.17.1 SDK, Maven 3.9.9 and Java 21. The parent-controlled SDK harness executed the intermediate first pass, second pass and composed final baseline-to-final pass. Its gates include exact output identity, exact fixed point, source drift refusal, duplicate target refusal, missing required source refusal and input immutability. These are actual SDK results, separate from candidate-template generation.

The final named recipes are `com.synexia.m3.M3FilteredWrapperPrivateConstruction20261017` and `com.synexia.m3.M3PrimitiveIndexFilterExactFit20261017`. Each uses a composed baseline-to-final whole-file crate. Intermediate baseline-to-A and A-to-B resources remain isolated qualification evidence: a forward hash recipe for baseline-to-A would refuse B on rerun. Their historic preconditions were retained. The delivered recipe itself is unchanged on its final postimage.

## Independent behavioral evidence

The new oracles were frozen before candidate bodies were authored. The same exact source fixtures were compiled against the baseline and against each SDK-generated pass, with `javac --release 21 -proc:none -Xlint:all -Werror`. Public `javap -public` output was byte-identical to the baseline after each pass.

| Oracle or gate | Baseline | First atom | Second atom | Final delivery |
| --- | --- | --- | --- | --- |
| Existing `M3FilteredViewsContract` | 123,500 checks, 750 randomized fixtures PASS | PASS | PASS | PASS |
| Existing `M3MutableFilteredViewsContract` | 288,223 assertions PASS | PASS | PASS | PASS |
| Frozen independent `M3WrapperSemanticContract` | 3,757 assertions PASS | PASS | PASS | PASS |
| Existing Java long-snapshot contract | 3,004 fixtures PASS | PASS | PASS | PASS |
| Existing actual JNI long-snapshot contract | 3,004 fixtures PASS | PASS | PASS | PASS |
| Existing actual JNI mutable-wrapper contract | 1,000 fixtures PASS | PASS | PASS | PASS |
| Frozen independent primitive-row filter contract | 798,846 assertions; eight primitive kinds and 64 fixtures PASS | PASS | PASS | PASS |
| Public signatures | Baseline receipt | Exact parity | Exact parity | Exact parity |

The wrapper oracle covers collection/set/sorted/navigable set and map shapes, ascending and reverse order, live backing mutation, original object identity, nullable values, repeated lookahead, exhaustion and exception identity. It also covers legacy predicate `ClassCastException` differences, null-argument versus unsupported-operation precedence, mutable removal after prefetch, bulk admission, map key-set addition refusal, fresh entries and stale live entries. For both construction atoms a proxy records every delegate/iterator/getter/predicate event. Injecting the same exception object at every boundary retains the exact event prefix and exception identity.

The primitive oracle uses independent ordered integer-row lists and all eight primitive lane kinds, including signed and floating-point special carriers. It tests zero/full/sparse admission, reversed/sliced/sorted projections, signed masks, defensive exported arrays, independent materialized stores, absent metadata and invalid lanes. Version-sensitive probes test the order of null validation and staleness checks, no callbacks from a stale view, a callback mutating a later row, later-row deletion, and callback failure after mutation. These preserve the existing precedence between source access failure, callback failure and the final generation check.

The tests do not claim exhaustive behavioral equivalence. Coverage of unrelated lazy/top-K, admission-only, DOOP or all physical sorted-container families was an owner inventory, not a newly established proof for each owner. Existing `LongFilteredView.toArray` already avoids its exact-fit copy. No duplicate implementation was introduced.

## Retained negative counterexamples

`M3WrapperNavigationCounterexample` deliberately exits unsuccessfully for four fixed inputs on the baseline and on every wrapper pass. Both orders of mutable bounded-range navigation retain their original mismatch with the independently projected JDK set. The M3 and legacy null-ceiling probes retain their null-sentinel mismatch. These expected failures are logged separately and never counted as passing qualification tests.

The existing read-only wrapper documentation already describes null-navigation ambiguity. The [Java 21 NavigableSet contract](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/NavigableSet.html) explains the null-return ambiguity. These performance atoms do not silently redefine the existing range/null policy; those counterexamples remain concrete inputs for a separately approved semantic decision.

## Genuine native execution

The existing `PackedSelectionNative.java`, `packed_selection_jni.h` and `packed_stable_filter.c` were compiled without implementation changes. `javac -h` generated the ABI header from the actual Java owner. GCC 13.3.0 compiled the real C source using C17, optimization, `-Wall -Wextra -Werror`, shared/PIC output and the actual JDK 21 JNI headers. The exported set contains the eight existing primitive compaction entry points.

The real `libM3WrapperStableFilter.so` has SHA-256 `f42ca37b2ae3bd77fdac6b35f2dec9ae03bf5db4b88741198cf5ca6b7f3bb034`. Both existing native-required oracles executed it under `-Xcheck:jni`, on the baseline and every common wrapper pass. This proves the tested long-compaction path; it does not claim runtime exercise of all eight primitive exports or a changed native hot path. No new ABI or speculative JNI optimization was added.

## Measured allocation

`M3WrapperAllocationProbe` was frozen and run in separate baseline/final JVMs using the same Java 21.0.2+13 toolchain, `-Xms256m -Xmx256m -Xbatch`, fixture, warmup and five measurement rounds. It uses the JVM thread-allocation counter and a volatile result sink. Medians below are allocated bytes per operation; they are not timing results.

| Scenario | Default JVM baseline | Default JVM final | Saved bytes per operation |
| --- | ---: | ---: | ---: |
| Descending wrapper iterator | 64 | 64 | 0 |
| Sorted-map emptiness | 0 | 0 | 0 |
| Primitive empty filter, lane or mask | 64 | 48 | 16 |
| Primitive full-match filter, 1,024 rows, lane or mask | 8,256 | 4,144 | 4,112 |
| Primitive half-match filter, 1,024 rows, lane or mask | 6,208 | 6,208 | 0 |

In the diagnostic run with `-XX:-DoEscapeAnalysis`, descending iterator allocation was 136 to 80 bytes/op, and sorted-map emptiness was 112 to 80. The default JVM had already removed those private wrapper allocations; the diagnostic result supports the construction reduction without implying a normal-configuration allocation or speedup gain. The primitive full-match saving appeared in both configurations. No latency, throughput, energy, cross-platform or broad workload performance claim is made. `evidence/allocation-paired-summary.json` contains all 20 scenario/configuration rows and each original run log is retained.

## Donor and category review

The existing searchable owners are `AlgorithmCatalogue`, `ChallengeSearchIndex`, `ChallengeSearchCli` and `ChallengeCategoryCatalog` in the canonical `synexia-algo` corpus package. Their typed entries separate category/provenance metadata from substitution authority. A documentation TSV is not automatically loaded into this index; the existing compiled catalogue/recipe seam must be used for admission.

| Reviewed source | Exact identity or source | Decision |
| --- | --- | --- |
| Guava filtered navigable sets | [`google/guava@5e82f2f528fbbd970e00748025b48e3eb42ed702`](https://github.com/google/guava/tree/5e82f2f528fbbd970e00748025b48e3eb42ed702), `Sets.java` blob `ae2628cb5d7d7c556fe00149123eb1b08709c6de`, Apache-2.0 license blob `d645695673349e3947e8e5ae42332d0ac3164cd7` | Useful live-filter/range comparison. Mutation and exact backing-call traces differ from the read-only M3 owner. No copied implementation and no substitution. |
| Commons predicated sorted set | [`apache/commons-collections@0b545e2f21ee8f2e8269201b5754465d76db70c7`](https://github.com/apache/commons-collections/tree/0b545e2f21ee8f2e8269201b5754465d76db70c7), source blob `fe75d069f11e9e36946a68fce0c59b6d5c5bc25d`, Apache-2.0 license blob `ff9ad4530f5716304e042cbac72cecd7c40f0e0e` | Validates initial contents and admissions; distinct from hiding rejected existing members. No alias or copied implementation. |
| LeetCode [Peeking Iterator](https://leetcode.com/problems/peeking-iterator/) | Primary problem page | Lookahead/exhaustion shape for independent adversarial tests. It does not specify the full live, mutable, null or callback contract. No problem/editorial/submission code copied. |
| HackerRank [Java Iterator](https://www.hackerrank.com/challenges/java-iterator/problem) | Primary problem page | Iterator boundary category, not substitution authority. No source copied. |
| GeeksforGeeks [sorted-array search, insert and delete](https://www.geeksforgeeks.org/dsa/search-insert-and-delete-in-a-sorted-array/) | Primary article, actual retrieved content | Sorted boundary/search category. No snippets copied. The earlier floor-problem URL exposed only its page shell and is not claimed as a reviewed statement. |

The GitHub donor sources and license texts were read before any source-admission decision. This delivery uses original first-party Synexia/M3JDK implementations and first-party independent probes; upstream annotation contexts used only by recipe parsing retain their exact headers and complete EPL/Apache license texts.

## M3JDK target boundary

The target packet for `hsoliwal/M3jdk21@970a34350ebdf6a3f1c5ff5acc8445d0f3515f55` uses `jdk.m3.collections.M3FilteredViews`, `M3MutableFilteredViews` and `com.m3.util.M3PrimitiveIndexView`/`M3PrimitiveSoaStore` with the required M3 names. It reuses the existing `M3PrimitiveKind` and `M3DenseHash` source owners after verifying token-level correspondence under the approved name mapping. The complete existing `com.m3.collections` module remains separate and unchanged.

The target native peer remains `M3IndexMaskCompactor.selectNative`. Its detached aligned-array, unsigned-mask contract is not substituted for the versioned row-view filter. The target packet has its own exact source-projection and recipe manifests. The parent executed all 11 per-file Java crates and the additive asset crate through the actual SDK, then applied only verified generated bytes to the receiver. Drift, duplicate and exact fixed-point gates passed.

The real receiver Maven build passed with the repository's original source-built JUnit `5.10.2-m3-source-1` coordinate: nine production compilation inputs, four test inputs, three tests and zero failures, errors or skips. The source-build receipt records ten original upstream owners with 826 independently checked source/provenance hashes. The retained receipt SHA-256 is `f7966de84efbf7b1f560dc48dda623aefce98a633e483407c49e16375306f40d`.

The first image run passed compilation and host oracles, then exposed an overly strict raw-native-hash check in the newly authored builder. The existing JMOD packager invokes `jlink --strip-debug`, so a valid linked native file differs from the unstripped input. A bounded two-file forward asset recipe corrected only the builder check and its README. An independent diagnostic confirmed exact equality between the linked bytes and `objcopy --strip-debug` of the original library. Native source and ABI were unchanged. The historical asset crate remains intact, and final delivery uses a composed ABSENT-to-final asset crate to retain convergence.

After that actual SDK follow-up application, **offline Maven `verify -Pm3-collections-image` passed**. Its fresh builder invocation compiled all 83 existing `com.m3.collections` sources and all nine inputs for the optional `jdk.m3.collections` module with Java 21 lint/Werror. The compiled JNI export set is exactly `Java_com_m3_collections_M3IndexMaskCompactor_selectNative`.

| Actual receiver oracle | Host named modules | Linked development image |
| --- | --- | --- |
| Read-only filtered/sorted contract | 123,500 checks PASS | 123,500 checks PASS |
| Mutable filtered/sorted contract | 288,223 assertions PASS | 288,223 assertions PASS |
| Independent versioned primitive-row contract | 798,846 assertions PASS | 798,846 assertions PASS |
| Unchanged original mask compactor `--native` oracle | 16,925 checks, JNI=true PASS | 16,925 checks, JNI=true PASS |

Both runs use `-Xcheck:jni`; native-required mode fails when the peer is absent. The existing packager produced two genuine JMODs and a validated 29-module linked image. The builder verified `java`, `javac`, `jar`, `jmod`, `jlink` and `jdeps`, compiled the actual module descriptors, and reran the independent mains from the image. Its native comparison matches the exact independent strip-debug reference, including SHA-256 `1bf2a2f057fe1ced09c10bdd9052387c723c39477ac59226d09b4d9ae2b31dde`. `evidence/target-final-qualification-receipt.json` records original/linked native hashes, modular artifact hashes, source inputs and retained execution logs.

Full custom HotSpot compilation, jtreg, interpreter/C1/C2, CDS, module-pack coverage thresholds and broader platform/resource evidence remain distinct from the executed optional-module and source-JUnit results above. This optional Apache module does not advance the ordered String/array/collection/AST receiving sequence or promote a new java.util backend.
