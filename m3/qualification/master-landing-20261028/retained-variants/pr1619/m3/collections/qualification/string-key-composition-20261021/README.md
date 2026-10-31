<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
     SPDX-License-Identifier: Apache-2.0 -->
# String-key composition: receiving and installed evidence

The selected String comparison, collection-view and native-selection changes pass their requested runtime and consumer gates in a fresh source-built JDK and a fresh linked collection SDK. The expanded String selection has one historical fixture failure and three inherited source/receipt failures; promotion remains blocked. This record keeps those boundaries explicit.

## What changed

`M3String` and its existing precompute bridge reuse exact immutable shared ranges before existing logical fallbacks. Ordinary String equality, natural UTF-16 order and prefix operations keep their APIs, allowing existing string-key consumers to benefit without a wrapper or extra spelling store. Filtered sorted views evaluate the stored comparator-equivalent representative. Native row compaction reuses existing bounded scratch as unsigned admitted ordinals, avoids payload reads for rejected chunks and fetches only an admitted chunk's enclosing span.

No tree implementation is replaced. New tests exercise the current array AVL owners and the unchanged relocated Commons trie. The [opportunity catalogue](../../algorithm-catalog/string-key-composition-20261021/README.md) records all 39 decisions, 312 exact-source inventory rows, 104 existing family dispositions and 14 primary references. Inventoried owners and pending/scaffold rows are distinct from qualified behavior.

## Executed gates

| Gate | Result and scope |
| --- | --- |
| Fresh HotSpot and complete JDK image | PASS. Only a generated configure helper was reused after all 44 autoconf inputs matched byte-for-byte; product binaries were built from the received source. [Source/build seals](SOURCE_BUILD.json). |
| Owning collection module, Java path | 494 tests, zero failures/errors, two native-only skips. |
| Owning collection module, required JNI | 494 tests, zero failures/errors/skips; ten fresh native libraries. [Module results](COLLECTIONS.json), [actual runs](COLLECTION_RUNS.json). |
| Fresh-image requested String contracts | Keyed collections, exact comparison, prefix, mixed equality, leaf cursor and UTF-16 differential fixtures pass. Stock hash/equals/compare/prefix fixtures pass with M3 disabled. |
| Expanded fresh String selection | 14/15 manifest records and 26/27 actions pass across 11 unique source fixtures; overall runner exits 1. Compact-on FactsComposition fails its historical Immediate-versus-Atom oracle assumption; compact-off passes 196,246 checks. The exact baseline reproduces that split. [Detailed String result](FRESH_STRING.md), [machine-readable actions](FRESH_STRING.json). |
| Fresh linked SDK | 29 modules, 512 production class files, 494 tests with zero failures/errors/skips. All 370 observed production collection classes originate from JRT; all ten libraries load from the installed image, with 21 JNI exports. [Installed result](LINKED_SDK.json), [class origins](CLASS_ORIGINS.json), [native origins](NATIVE_ORIGINS.json). |
| Focused installed consumers | View donor/new proofs pass 533/830 checks; unchanged cursor behavior retains its 1,008-case trace; composed tree tests pass 268,781 checks in each CompactStrings mode; native scalar selection passes 8,192 cases plus five invalid-input checks. |

The full 494-test installed JUnit run uses ordinary compiled execution with String M3 storage disabled by default (`String.M3_JOINED_STRINGS=false`, `UseCompiler=true`, `CompactStrings=true`) and the required collection JNI flags enabled. The dedicated composed tree runs explicitly use `-Xint`, `-XX:+UnlockExperimentalVMOptions` and `-XX:+UseM3StringStorage`, with CompactStrings enabled and disabled; both pass 268,781 checks. These are separate configurations. No full-suite result with composed String storage is claimed. [The execution-mode receipt](EXECUTION_MODES.json) and [actual flag/field probes](VM_CONFIGURATION.json) seal that distinction.

The native C fixture compares the same four export families against a scalar oracle. Both baseline and candidate pass 4,148 correctness/fault cases. For one survivor among 1,025 input elements, the baseline transfers 1,025 payload elements in five regions; the candidate transfers one element in one region. State passes, count-drift detection, all nine compactor exports, stable order and offset semantics remain checked. The full mask library has eleven exports including its two existing mask-window exports; all ten SDK libraries together have twenty-one. These are transfer counts, not a throughput or memory benchmark. ASan/UBSan pass with leak detection disabled; LeakSanitizer is blocked by the environment's ptrace restriction and that failed attempt is retained.

## Failures and holds retained

The full-borrow receiver check passes. The closure checker honestly reports all 12 executable gates OPEN and promotion BLOCKED. Three unchanged source checks fail on both current receiving sources and exact pinned baseline inputs: a historical pool-marker assertion, a precompute-bridge receipt hash and a STRING successor-chain length. No checker, receipt or historical chain was rewritten to hide those failures.

The first required-native module run omitted rank/signal opt-in flags; its one failing test and exact invocation are retained in [the initial run record](COLLECTION_RUNS_INITIAL.json). Correcting the runner flags produced the passing required-native run without a production change. A first tree recipe attempt refused a donor fixture outside supported Java source layouts; moving the same body under `qualification/.../src/test/java` qualified a fresh dataset without widening the parser or text lane. Canonical evidence also retains the initial overly broad catalogue-test expectation and rejected timing setup.

## Reproduce the relevant checks

The canonical task is [com.synexia](https://github.com/hsoliwal/com.synexia)'s `synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017/tasks/m3-string-key-collections-20261021`. Its SDK_EXECUTION, DATASET_TARGETS, TARGET_RECEIVING and provenance records bind actual recipe inputs/results, fixed-point reception and per-file rights. The four snapshot engines remain unchanged. Use the task's QUALIFICATION_COMMANDS.md for canonical Maven/receiver replay; do not copy templates or edit emitted output.

With a configured receiving checkout and JDK21/Maven available, the executed source-build and Java module commands are:

```sh
make CONF=m3-string-keys-release JOBS=6 hotspot
make CONF=m3-string-keys-release JOBS=6 images
mvn -B -ntp -f m3/collections/pom.xml \
  '-DargLine=-Xmx512m -XX:ActiveProcessorCount=2' package
```

The required-native run additionally supplies the freshly built libraries and all opt-ins:

```sh
mvn -B -ntp -f m3/collections/pom.xml \
  '-DargLine=-Xmx512m -XX:ActiveProcessorCount=2 -Xcheck:jni -Dm3.collections.all.native.required=true -Dm3.mask.spans.native.required=true -Dm3.collections.rank.native=true -Dm3.collections.signal.native=true -Djava.library.path=/absolute/fresh/native-release' test
```

`COLLECTION_RUNS.json` includes the exact native compilation/header commands and argv used here. `STRING_ACTIONS.json`, `STRING_IMAGE.json` and `STRING_SOURCE_SEALS.json` bind the jtreg 7.3.1 harness and the freshly built image's own compiler and runtime. `LINKED_RUNS.json` records the unchanged existing jmod-pack tool, linked-image command line and named-module verification. Local absolute paths in those receipts are historical evidence and must be replaced with corresponding isolated paths when reproducing; binaries and credential-bearing settings are not distributed.

## Observed performance and remaining scope

[The preliminary paired observations](PRELIMINARY_OBSERVATIONS.md) retain three baseline/candidate pairs on a prior M3 interpreter VM with exact pinned owners. For the 4,096-unit shared-equal fixture, median warm equals elapsed time was 2,088.282 microseconds versus 28.101 microseconds. The corpus verifies actual shared ranges and independent-owner controls. This was a noisy run alongside a source build, not a fresh-image performance qualification; allocation, retained memory, CPU time and GC were not measured. The initially contaminated same-candidate-arm setup was rejected, preserved and excluded from the table. [Raw measurements](PRELIMINARY_MEASUREMENTS.json) and [source seals](PRELIMINARY_SOURCE_SEALS.json) make that distinction reviewable.

No whole-jtreg/JCK, compiled-M3, all-provider, all-platform, global interning, allocation improvement or phase-promotion claim follows from these finite results. The M3 Patricia scaffolds still lack implementations; compressed/double-array/adaptive radix storage remains explicitly pending. Existing source and donor notices remain attached to their owners, and challenge-site code was not imported.
