<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# One-file stable-order restoration review — 2026-10-20

The selected one-file stable-order restoration has passed canonical SDK replay, strict Java 21 behavior/API checks and the current JNI/native checks. The separate retained-wrapper review preserves its known failures. This additive review is pinned to M3JDK21 `95d0266ff15ae1f11876cce7360b155ef33a1862`; it leaves prior decisions and qualification history intact. `CANDIDATES.tsv` remains ordinary searchable catalogue data, with no new algorithm registry or wrapper framework.

The selected atom is the already-published Synexia `stable-order-groups` recipe. The current projection exactly matches its preimage. Its known-opposite-direction branch reverses equal-value groups and copies occurrence IDs forward within each group, preserving stable ties in sliced and filtered index windows. It retains current address-kernel calls and the encounter-order merge unchanged. The exact source and canonical publication pins are in `SOURCE_PINS.json`.

Current mask compaction is retained: its first-to-last admitted-span row transfer is stronger than the historical suffix-only candidate. Fresh native checks confirm that its bounded ABI and output batching remain intact. The current strict read-only iterator guard also already matches its qualified postimage. Faster ordinary iterator routing remains refused under the frozen callback, failure and retry effects; no predicate cache or universal provider wrapper policy is added.

The review reuses the existing source/licensing decisions and the dated discovery record in `DISCOVERY.json`. Challenge statements and articles supply categories and counterexamples only. No editorial, submitted solution or external algorithm body is copied. JDK, Guava, Eclipse Collections, Apache Commons Collections and Spring Framework keep their existing names and distinct contracts; Apache predicated admission is not filtering, sorted-set uniqueness is not duplicate occurrence identity, and Spring/Guava mutation policies are not interchangeable.

## Generated-source qualification

The [qualification summary](../../qualification/stable-order-restoration-20261020/RESULTS.json) separates source generation, behavior, native execution and retained-wrapper findings. Four serial canonical Java SDK passes executed 20 recipe-proof tests and emitted one production source plus three verification sources. Each receiving pass then compiled strictly.

The [generated Java receipt](../../qualification/stable-order-restoration-20261020/verification/generated/RESULTS.json) binds those exact receiver files. Actual Temurin Java 21.0.12.1+1 compiled production and tests with `--release 21 -Xlint:all -Werror`. Five existing JUnit entrypoints, the projection self-test and explicit missing-native oracle pass; compiled public/protected descriptors equal baseline. Production source/class hashes stay unchanged throughout verification, and test classes do not shadow production classes.

The [native receipt](../../qualification/stable-order-restoration-20261020/verification/native-final/RESULTS.json) records 12 actual JVM runs and two C instrumentation fixtures, with all 12 current JNI symbols matching generated headers. Strict C17 with UBSan and `-Xcheck:jni` produced zero diagnostics. Three current C owners remain byte-identical. The native contract checks all 65,536 mask pairs and 67,914 windows with 3,267,362 assertions. Its observation digest matches baseline and generated Java: `e15c399b7be3d8c0fbd38bc6e2cd4c63b93bb0b95d1272bce9acdcc7bec5445b`.

The initial native proof compilation lacked the original compactor test's primitive-container dependencies. That failure is retained; ten exact current support sources completed the proof closure in a class directory disjoint from generated production. No assertion or production/native source was changed to obtain the final pass.

## Resource observations from generated sources

The unchanged resource probe sorts a 32,750-row slice in the opposite known direction after 50 warmups, with three samples of 100 conversions each.

| Metric per 100 conversions | Baseline median | Generated receiver median |
| --- | ---: | ---: |
| Allocated bytes | 26,207,984 | 13,106,384 |
| Wall time, ns | 56,981,777 | 4,671,171 |
| Thread CPU time, ns | 57,242,035 | 4,834,429 |
| Heap delta after requested GC, bytes | 32 | 32 |

The [generated comparison](../../qualification/stable-order-restoration-20261020/verification/GENERATED_COMPARISON.json) binds all samples and source receipts. The observed allocation saving is 131,016 bytes per conversion, the size of one temporary index array for this workload. Timing is a finite warm observation; requested GC and whole-process RSS do not establish retained-heap or native-memory savings. Cold setup is measured separately and is not claimed to improve. The earlier diagnostic timings remain separately preserved as historical evidence, not substituted for this generated-source run.

## Fresh retained-wrapper findings

The [separate wrapper receipt](../../qualification/stable-order-restoration-20261020/verification/retained-wrappers/RESULTS.json) compiles four unchanged current owners and executes the original historical fixtures without weakening them. The read-only policy JUnit gate passes 1/1. Five of nine standalone modes pass: policy (25 checks), preserved read effects (8), bounded-map navigation (64), bounded-set navigation (64), and interior-null navigation (6).

Four standalone invocations remain nonzero: `readonly-linear` observes 787,456 predicate calls; `readonly-failfast` observes missing backing-modification detection; `comparator-failure` observes a swallowed comparator-owned exception; and the unchanged default `readonly` mode stops at that same comparator failure. The default mode is not a fourth distinct defect, and assertions after each first failure are not claimed executed. These findings belong to unchanged wrapper owners and are not caused by the stable-order restoration.

The current interior-null and ordinary bounded-navigation fixtures now pass. This new scoped evidence supersedes the historical pending-null reproduction for this fixture and revision; the older catalogue record remains intact. It does not prove all comparators or all wrapper behavior. Faster traversal remains a separate contract task because changing callback demand, failure timing or retry behavior cannot be hidden inside this sorting restoration.

These are bounded proving-module results, not full collection/five-provider compatibility or a source-built integrated JDK. The integrated receiving sequence is unchanged: String remains active before arrays and collections.
