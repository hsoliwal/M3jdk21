<!--
Copyright 2026 Hitesh Soliwal and contributors
SPDX-License-Identifier: Apache-2.0
-->
# Selection spliterator callback-state refinement

## Scope and defect

This small follow-up refines the existing `M3CollectionViews.SelectionSpliterator` in the M3JDK21 collection module. It applies after the separately qualified sorting/view convergence packet. That packet leaves this owner unchanged. The runtime change saves and restores the existing `action` and `delivered` fields around each nonnull `tryAdvance` invocation.

The permanent baseline counterexample uses the real filtered view over an `ArrayList` containing one element. Its outer consumer receives that element and calls `tryAdvance` again on the exhausted same cursor. The old nested call overwrites the shared delivery flag with `false`, so the outer call also returns `false` despite delivering its element. A nested predicate traversal can likewise replace the enclosing callback or leave the enclosing operation with another invocation's delivery flag.

After the refinement, each invocation evaluates its own return value and then restores the enclosing invocation's state in `finally`. The outermost call restores the original null callback, including when a predicate or consumer throws. Null validation still happens before either field changes. Exceptions retain their identity; elements already consumed from the backing spliterator are not rolled back.

## Source and rights

The first-party owner is pinned to:

- Repository: [hsoliwal/M3jdk21](https://github.com/hsoliwal/M3jdk21).
- Revision: `2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.
- Path: `m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java`.
- Git blob: `af9a82214a62eff0f9194d6a95179829b6f57b4e`.
- Before SHA-256: `c1ae229f00911e0881de9cae102f2c65f0f7f9f8eb4da1abd364a816dd84ee0d`.
- After SHA-256: `facd79d33a6db897f4e4128530f3e320250a75bd3b32dd43119c19f9437a8298`.

Existing Apache-2.0 and copyright notices remain intact. A modification notice identifies the new runtime refinement. The regression sources and these records are original 2026 work by Hitesh Soliwal and contributors, under Apache-2.0.

A bounded search of published pull requests, code and commit records did not find an existing save/restore repair to receive. The published [Synexia owner at revision 971d3f34dba39d51de30da2c5be3b6ea297b053d](https://github.com/hsoliwal/com.synexia/blob/971d3f34dba39d51de30da2c5be3b6ea297b053d/synexia-primitives/src/main/java/com/synexia/primitives/M3CollectionViews.java), Git blob `1b710d217b63ce0753fd21e8a832bc86a1b2497b`, still had the old clear-only implementation. This is a local refinement of the pinned M3JDK21 owner, not an assertion that an unpublished repair was received. The bounded search is not a claim that no other repair exists anywhere.

The [Java 21 Spliterator API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Spliterator.html) supplies the normal callback, boolean-result and null-action contract reference. No third-party implementation or challenge-site solution was copied.

## Per-file decisions

The accompanying catalogue, `algorithm-catalog/selection-reentrant-20261018.tsv`, records all five target files. There is one runtime edit, one standalone regression source, one owning-module JUnit adapter and two documentation records. The source-sealed recipe must admit the exact runtime preimage and the absence of all four new paths.

The runtime adds two operation-local variables and uses the existing `finally` block to restore state. All six retained fields, all method signatures, the source cursor, predicate, owner comparator and split construction remain unchanged. No new per-element objects, retained index lanes, payload copies, wrapper hierarchy, native symbols or module dependency are introduced. There is constant additional local state per active invocation; nested traversal still uses the ordinary Java call stack. No throughput or allocation benchmark claim is made for this repair.

## Candidate verification

The complete post-convergence collection module was used as the input fixture: 345 source/configuration inputs before this refinement and 347 after adding the two Java test files. All 134 main Java files and the unchanged module POM were compiled with Java 21, the existing lint checks and `-Werror`. The real cached JUnit dependency and owning Surefire configuration were retained.

| Check | Observed candidate result |
| --- | --- |
| Permanent one-element witness compiled against the exact unchanged owner | Expected failure: outer delivery must remain true after nested exhaustion |
| Complete collection module, Java mode | 271 tests, 0 failures, 0 errors, 2 explicit native-only skips |
| Complete collection module, required native mode | 271 tests, 0 failures, 0 errors, 0 skips |
| Standalone real-owner regression | 6,117 checks passed |
| `javap -p -s` for the public owner and private selection spliterator | Identical declarations, fields and JVM descriptors |
| Independent source and regression review | No concrete defect reported |

The two Java-mode skips belong to `M3FilteredSplitsMaskSpansTest`; both execute in the required-native gate. Native mode used the previously qualified nine real collection libraries, with `-Xcheck:jni`, fatal UBSan settings, the all-native and span-native requirements, and the existing rank and signal opt-ins. This follow-up changes no Java native signature or C source.

The eight new JUnit methods cover nested consumers, nested predicates, exhaustion, consumer and predicate exceptions (including caught and uncaught cases), null admission before effects, recursive and cross-child splits, bulk traversal, and callback lifetime. They use real `ArrayList`, `M3ArrayList` and `TreeSet` owners. Where the upstream owner's bulk traversal reserves its remaining range before callbacks, the oracle follows that real owner behavior. The tests neither replace the backing collection with a stand-in nor mutate its contents during traversal.

Callback retention is checked deterministically by reading the existing private callback field: the enclosing callback must be restored while active, and the field must be null after the outer operation ends. This is not a garbage-collector timing or reachability benchmark.

## Qualification boundary and replay

These results qualify the frozen candidate source images. They do not themselves claim that the canonical Maven/OpenRewrite recipe has received the five targets. The recipe's actual admission, fixed-point, missing-input and drift controls, followed by a replay into the receiving tree, require separate execution receipts. The preceding 49-target packet remains unchanged.

The existing public restrictions on predicates, backing-owner behavior and concurrent access remain in force. This repair preserves the selection adapter's operation-local callback state during synchronous nesting; it does not promise universal reentrant behavior from arbitrary upstream spliterators, transactionally undo callback effects, make the cursor thread-safe or guarantee arbitrary nesting depth.

The ordinary owning-module gate is:

```sh
mvn -B -ntp -f m3/collections/pom.xml verify
```

The required-native gate additionally uses the existing module properties `m3.mask.spans.native.required=true`, `m3.collections.all.native.required=true`, `m3.collections.rank.native=true` and `m3.collections.signal.native=true`. Its test JVM needs the rank/signal opt-ins, `-Xcheck:jni` and the library path built by the existing native verification workflow. Frozen source inventories, logs, the expected-failure receipt and descriptor comparison accompany the canonical candidate provenance; actual receiving-tree receipts are recorded separately.
