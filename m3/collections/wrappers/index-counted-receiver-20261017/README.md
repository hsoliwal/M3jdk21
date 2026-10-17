<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 sorted counted collections: canonical source-first receiving crate

**Date:** 17 October 2026. **Source of reusable algorithms and recipes:**
`hsoliwal/com.synexia`. **JDK product/runtime owner:**
`hsoliwal/M3jdk21`, stacked on [M3JDK21 #1031](https://github.com/hsoliwal/M3jdk21/pull/1031).
Do not introduce any Synexia runtime dependency into the product.

## Historical, file-scoped inventory and donor selection

M3JDK21 already owns generic Collection/Set/NavigableSet/List, filtered
Map, sorted comparator snapshots, signed-int JNI selection, and transient
AVL batch staging on the #1025/#1031 stack. It does **not** yet own the
ranked, primitive-ID sorted multiset or live counted wrapper.

Synexia [#11971](https://github.com/hsoliwal/com.synexia/pull/11971)
already contains these owners and their contract proofs. Borrow its
complete dependency closure rather than implementing another TreeMap:
`MIndexSpace`, `MIndexHeapAdmission`, `M3IndexOrderedStore`,
`M3IndexRange`, `M3IndexCountKernel`, `M3IndexNavigableSet`,
`M3IndexSortedMultiset`, and `M3FilteredSortedMultisets`.
All are first-party Apache-2.0. Import them as 8 M3JDK21 product files
with package adaptation and an explicit renamed
`M3IndexHeapAdmission` product owner.

The primitive count kernel retains Java by default and optionally calls
the native C17 `m3_count_kernel.c` implementation when
`-Dm3.collections.counts.native=true`. Only package/header JNI symbols
and the library name are modified; numeric algorithm and JNI region
ownership remain unchanged. No persistent native Java addresses,
global references, callbacks or new collector barriers.

Guava TreeMultiset and Eclipse TreeBag are **alternatives to evaluate**,
not copied source. The current selected primitive-lane design favors
cache locality and avoiding retained entry objects, but has O(d)
structural insertion/rank/select; it is not claimed superior to
tree-based O(log d) insertion. Challenge-site algorithms from LeetCode,
HackerRank and GeeksforGeeks contribute only adversarial taxonomy.
See `CATEGORY_CATALOGUE.tsv` and `PROVENANCE.tsv`.

## Recipe FIRST: Inline → Atomize → Patternize → Comment

Canonical named Maven/OpenRewrite recipe:
`com.synexia.m3.M3Jdk21FilteredSortedCountedReceiver20261017`.

The existing `M3HashPinnedJavaSnapshotRecipe` parses and produces
**12 exact ABSENT-before Java21 files**, including 8 implementation
types, the two original Synexia sorted-multiset oracles, the JUnit
bridge and a new 768-round Java/JNI parity oracle.
`M3HashPinnedTextSnapshotRecipe` receives the one C17 native atom.

For text-parser admission without widening the product native-source
trust boundary, the canonical C file is qualified at its existing
`src/main/native/m3_count_kernel.c` recipe path. The product receiver
installs the **same exact Git blob** at
`m3/collections/src/main/native/collections/m3_count_kernel.c`.
The independently verifiable receiving script requires byte-identity
and SHA-256 at both paths, plus native compilation and Java-JNI
differential behavior.

Each target has its exact SHA-256 manifest, original donor revision,
rights and expected interfaces. No original M3JDK21 source file
is overwritten or removed. Source templates and actual Maven
`Recipe.run` tests are committed before the target receives generated
code. A wrong output means fix and rerun the recipe.

## Contracts and failure boundaries

The ordered-store ID lanes own canonical references only through the
`MIndexSpace` domain, not one object per occurrence. Each group holds
an `int` positive occurrence count. `longSize` is exact and
`Collection.size()` saturates to `Integer.MAX_VALUE`.
Null is a separate ID sentinel and is accepted only with a compatible
comparator. The first admitted representative retains identity.
Counters validate entire ranges, including after a found ordinal.
Iterator fail-fast tests include hidden-group count modifications.
Filtered read-only variants refuse even no-op mutations.

Proof gates:
1. Real Maven/OpenRewrite Java LST and native text replay, SHA-256
   preimage/ABSENT admission, fixed-point, occupied-source refusal.
2. Strict Java21 `javac`, original JDK TreeMap/differential
   sorted-multiset tests, count-overflow/large-group edge cases.
3. Java and optional C17/JNI 768-round `sum`/`locate`/`selectedRows`
   parity, invalid input and stable order; run under
   Serial, Parallel, G1, ZGC and Shenandoah, with `-Xcheck:jni`.
4. Reuse existing M3JDK21 whole collection/JNI regressions and
   exact source ledger. A separately qualified successor of #1002's
   `M3InstalledSourceInventoryRecipe` is mandatory before
   installed-JDK promotion; historical manifests remain untouched.
5. Fastdebug source-built JDK/jtreg, JCK, native Windows/ARM
   and JMH before claiming universal JDK GC/performance improvement.

The focused receiver does **not** modify `java.util`, `java.base`,
HotSpot, a GC collector, reference counting or JNI shadow objects.
Passing focused tests is not whole-project implementation.
