<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Filtered collection contracts: qualified per-file refinement

This package records repairs in the existing `m3/collections` owners: legal navigation across bounded sorted views, strict read-only mutation refusal, deep filtered List predicate composition, and deep known read-only Collection/Set traversal. Two explicit constructor declarations restore the unchanged strict module compilation gate. Four independently established test-oracle mistakes are corrected with stronger identity, JDK, and layout assertions.

The source baseline is `hsoliwal/M3JDK21@e913ab89056657eca4009678cfe9a657c020e79b`. Canonical donor and transformation ownership remains in `hsoliwal/com.synexia`. `SOURCE.json` records exact baseline and final bytes for every changed Java file and the canonical recipe dependency. These are separately licensed proving-module changes; the global ordered JDK receiving family remains STRING.

## Behavior and owner decisions

| Existing owner | Refined behavior | Retained contract |
| --- | --- | --- |
| `M3FilteredViews` and mutable Set navigation | A query may fall outside the owner's bounded range. Use a stored neighbor for the fallback endpoint, then traverse the original visible range. | Comparator order, nullable stored representatives, live bounds, predicate order, detached immutable map navigation entries. |
| `M3FilteredViews` read-only maps and sorted sets | Refuse no-op/default mutations and empty sequenced endpoint removals before callbacks or lookup. Guard the original JDK sorted reversed views. | Existing sorted reversed read traversal and callback demand; strict documented read-only behavior. |
| `M3FilteredNavigableSet` | Reuse the existing helper and explicitly override already-inherited removal endpoints. | Existing public callable descriptors and compatibility facade. |
| `M3FilteredLists` | Replace the recursive conjunction using the pinned current Synexia balanced predicate atom in `M3FilteredViews`. | Original backing List, stable logical indexes, predicate occurrences and exceptions, write admission, cursor/range semantics. |
| `M3FilteredViews.CollectionView` | Open known read-only iterator decorators into temporary predicate-reference arrays. Replay membership continuations with the original representative scans. | No backing-element snapshot; repeated membership predicate demand, identity/equality behavior, lazy iteration and strict iterator removal refusal. |
| `M3ObjectSlotStore28`, `M3SpanIndex` | Make the existing public empty constructors explicit. | The same `()V` constructors and field initialization order. |

For example, an even-number view over `[2,8)` now answers `lower(100)` with the retained member `6`, rather than rejecting `100` as an invalid range endpoint. Direct creation of an invalid range still rejects that range. If `null` is a stored member, it must remain distinguishable from the navigation API's absent-result sentinel. The fallback compares an actual stored extreme; it never invents a null lookup against a comparator that rejects null.

The original legal range path is retained. The fallback opens only known M3 owners for boundary lookup and keeps their complete wrapper chain on traversal. This avoids the duplicate inner predicate callback found in the intermediate unconditional-anchor candidate. The exact original/intermediate/final witness is retained under `evidence/`.

Read-only corrections deliberately change previously broken behavior: documented read-only no-op mutations now throw `UnsupportedOperationException`, and empty removal endpoints refuse mutation before an emptiness check. The private reversed guards forward reads to the same JDK view, because rebuilding a filter over reversed backing would change existing callback demand.

## Reuse, catalogue and per-file transformation

`DONORS.json` pins the selected historical bounded-set and map-entry atoms, and the current `DeepFilteredListComposition20261017` resource blobs. The M3 receiver reuses appropriate atoms in its existing owners; it does not import a parallel collection hierarchy. Current JDK unmodifiable APIs supply ordinary permission wrappers. No Guava runtime dependency or external challenge/editorial source is copied.

The existing Synexia problem/category and donor-review owners supply discovery. The `dfs` category supplies the recursion-elimination reference review, including the established LeetCode, HackerRank, GeeksforGeeks and pinned GitHub ordering. The explicit continuation arrays reuse the existing M3 filtered cursor and representative scan. `ALGORITHM_DECISIONS.json` records selected and rejected alternatives, cost, compatibility, licensing and the exact per-file decisions.

The qualified task recipe is `com.synexia.m3jdk.CollectionsContractRefinement20261017`, in canonical crate `synexia-openrewrite-recipes/recipe-crates/m3-collections-contract-refinement-20261017`. Its four Java/text snapshot engine sources are reused unchanged. Each source epoch executes actual OpenRewrite SDK tests, input sealing tests and receiver cases before receiving SDK Results. The final consolidated `m3jdk21-collections-final` dataset applies sixteen baseline-to-final Java files; intermediate epochs and their counterexamples remain available for review. The earlier `m3jdk21-collections` dataset contains fourteen files and is retained as history. An unknown preimage is refused, and replay at the final fixed point writes nothing.

Catalogue structural and logic fingerprints identify candidates. They are not proofs that implementations are equivalent. Actual SDK, callback, behavior, public-descriptor and native-selection receipts have distinct claims in `EVIDENCE.json`.

## Verification records

The focused receiving proofs compile the exact SDK output with the source-built M3 Java 21 image and `--release 21 -proc:none -Xlint:all -Werror`. They retain these finite observations:

| Proof | Recorded scope |
| --- | --- |
| Bounded/null/read-only composition | 32,267 checks, including all 27 three-level read-only/mutable/facade Set compositions and all eight read-only/mutable Map compositions. |
| Deep List donor proof | 55,469 checks, depth 65,536, 192 seeds, `-Xss256k`. |
| Deep read-only Collection/Set proof | 84 independent depth/demand/failure assertions, depth 65,536, `-Xss256k`; original predecessor overflow retained. |
| Exact effect tapes | 18,779 bytes of generic owner/iterator/equality/predicate events, and 2,537 bytes of sorted/reversed/mixed-map events, match the predecessor byte for byte. |
| Existing wrapper contracts | 123,500 read-only, 288,223 mutable, 117,773 facade and 69 stored-representative assertions. |
| Owning module, Java selection | 214 tests across 61 suites, zero failures, errors or skips, on the source-built M3 image with an empty library path. |
| Owning module, native selection | 214 tests across 61 suites, zero failures, errors or skips, with the eight existing native libraries, `-Xcheck:jni` and undefined-behavior failure enabled. |
| Full module declarations and build parity | 116 production source units comprise 114 ordinary top-level types, the module descriptor and package documentation. The Java/native builds emitted the same 441 class files byte for byte. |
| Existing JNI boundary integration | Eight existing libraries and nine Java/native owners; required native, interpreted native and absent-library cases, recorded separately from wrapper Java changes. |

The large navigation corpus, per-stage recipe gates and final owning-module executions have their own retained receipts. Counts from an intermediate source epoch are not relabeled as final-source counts. No production JNI implementation or native symbol changes in this collection wrapper/constructor pass. User predicates and comparators stay Java callbacks.

The final module/API gate uses the unchanged module POM and compares all 114 ordinary type declarations, the module descriptor and `package-info.java` bytes. `M3FilteredNavigableSet` now declares `removeFirst()` and `removeLast()`, which were already inherited. Callable signatures remain available with the same descriptors; reflection on declaration ownership changes. `PUBLIC_API.json` records that distinction explicitly.

A separate incremental native execution exposed stale compiled owner files, including missing new private helpers, while the receiving source bytes remained final. That execution and its class-artifact witness are retained under `evidence/observed-incremental-*`. The accepted Java and native runs transported the qualified module into fresh unique build roots, sealed all 249 owning inputs, checked all sixteen qualified postimages, and recorded class hashes at test entry and after completion. `evidence/accepted-module-gates.json` selects those accepted receipts. Their complete collection class-origin records are retained alongside the exact Maven logs; historical shared-output attempts are excluded from acceptance.

## Original four failing oracles

The first whole-module run executed 205 tests and reported four assertion failures. Investigation established four fixture errors; production `M3LongTreeSet` did not require a change.

The signed-survivor fixture never inserted zero, so its central gap is `-21` to `+21`, or 42 units. Its corrected assertion also compares the complete sequence with an independently populated JDK `TreeSet`. The capacity-four page fixture splits full pages in half; ten ordered values occupy four pages before its existing exception/partial-commit assertions. The step-three seed already contains `7`, so both JDK and M3 views correctly reject duplicate insertion; the corrected fixture also inserts the fresh value `8`. Finally, `List.remove(Object)` removes the first equals-equal reference, which was the hidden member. The corrected stored-representative proof asserts that the visible reference survives, then removes that exact reference and tests absence.

The original failing log and the independent JDK witness are retained. This separation prevents a bad expected value from becoming a production change and preserves the stronger regression coverage.

## Cost and admission limits

Balanced List conjunctions bound M3 composition height logarithmically while preserving all predicate occurrences. A known read-only iterator uses O(depth) temporary predicate references and no backing-element copy. Membership deliberately retains the original repeated scans: for one always-visible member, depth `d` still executes `d(d+1)/2` predicate calls. Stack safety does not imply a new linear-time membership contract.

The diagnostic probe under this package's `src/test/java` records twelve finite resource cases against fresh original and final class roots. The paired depths 1 and 64 preserve the same results and callback demand. Both original depth-65,536 cases overflow with `-Xss256k`; both final cases complete. Thread CPU and allocation, retained owner/cursor field graphs, post-GC process heap, Linux process RSS and HotSpot Native Memory Tracking observations are retained in `evidence/resources/RECEIPT.json` and its raw logs.

The measurements expose a concrete tradeoff. At depth 64, the List composition retains 127 balanced `FilterNode` objects instead of 63 recursive conjunction lambdas, and measured wrapper-construction allocation grows from 3,072 to 10,760 bytes. The generic Collection owner graph stays the same; measured traversal allocation changes from 2,112 to 1,056 bytes in that case, while depth-one traversal stays at 96 bytes. These are observations from one finite run, including the shared diagnostic result-record overhead. The resource gate does not select a throughput winner or impose timing thresholds. Small process-heap deltas include VM initialization and garbage collection noise, so even negative shallow deltas remain in the evidence and are not interpreted as an individual owner's size.

This pass does not claim elimination of every recursive owner family. Sorted/navigable representative membership, deeply nested sorted range construction, deeply nested strict read-only List wrappers, mutable/opaque cursor internals, and caller recursion remain separate admission boundaries. Opaque third-party callback identity after a rejected range construction is not asserted. Existing stable-predicate, owner concurrency, fail-fast and synchronization limits remain in force.

The native receipts are for the recorded Linux x86_64 M3 image and bounded inputs. They do not establish other-platform portability, whole-JDK acceptance, or a universal throughput improvement.

## Reproduce

The unchanged owning POM uses the source-derived `org.junit.jupiter:junit-jupiter:5.10.2-m3-source-1` coordinate, and the accepted tests load `junit-platform-launcher:1.10.2-m3-source-1`. A fresh Maven cache needs the existing checked-in dependency reactor. From the repository root, bootstrap its relevant dependency closure, then run the owning module with Java 21:

```sh
mvn -B -ntp -f m3/vendor/third_party/pom.xml \
  -pl junit-jupiter-5.10.2,junit-platform-launcher-1.10.2 -am -DskipTests install
mvn -B -ntp -DfailIfNoTests=true -f m3/collections/pom.xml clean verify
```

Both commands must use the same Maven repository; pass the same `-Dmaven.repo.local` value to both if overriding the default. This prerequisite follows the existing [vendored-build documentation](../../../docs/a3-vendored-build-prerequisite-20261015.md) and source reactor. JUnit keeps its upstream EPL-2.0 rights and existing [provenance](../../../vendor/third_party/junit-jupiter-5.10.2/PROVENANCE.json), [license](../../../vendor/third_party/junit-jupiter-5.10.2/LICENSE) and third-party notices. The Apache-2.0 build integration does not relicense JUnit. This document does not assert a new execution of the dependency bootstrap.

For native selection, first build the eight existing libraries from the recorded Java/header/native sources using [the native build commands](evidence/native-build-commands.json) and [boundary source/library receipts](evidence/native-boundary-receipt.json). Point `M3_NATIVE_LIBDIR` at their absolute output directory, using the qualified platform and M3 runtime:

```sh
M3_NATIVE_LIBDIR=/absolute/path/to/qualified-libraries
UBSAN_OPTIONS=halt_on_error=1:print_stacktrace=1 \
  mvn -B -ntp -DfailIfNoTests=true -f m3/collections/pom.xml \
  -DargLine="-Xmx768m -Xcheck:jni -Djava.library.path=$M3_NATIVE_LIBDIR -Dm3.collections.signal.native=true -Dm3.collections.rank.native=true" \
  clean verify
```

The exact accepted Java/native launch properties, source-built M3 runtime identity and selected library hashes are in the final execution receipts. The focused self-test main classes also remain directly executable. Their `--read-trace` or `--trace` modes produce the retained effect tapes. The canonical crate README documents SDK qualification and `receive.py --dataset m3jdk21-collections-final --target ... --check`, reception and fixed-point replay. The standalone resource observer is separate from the 214-test owning-module count; its compile and launch arrays are retained under `evidence/resources/`.
