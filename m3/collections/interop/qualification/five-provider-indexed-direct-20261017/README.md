<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Five-provider M3 array/flag implementations — direct Spring + indexed selection

Canonical source/recipe owner: hsoliwal/com.synexia; implementation target:
hsoliwal/M3jdk21 (independent Apache-2.0 M3 collections/optional interop).
Parent target branch `m3/five-provider-array-interop-20261017`, exact commit
`6c00eef6fd67ed6c34fd86560a4ffe8343ec17d4` (M3JDK21 PR #1174).
This keeps its **33 existing public factories and 20 existing collectors**
unchanged by signature, and all provider-family implementations already present.
No java.base, HotSpot or native source files are modified.

## Actual implementation

- The existing `springMultiValueMap()` factory stops constructing
  `M3SpringMultiValueMap` (Spring's adapter subclass) and constructs
  `M3SpringArrayMultiValueMap`, the direct implementation from canonical
  Synexia #12181 / M3JDK21 #1190, identical source Git blob
  `47c6517b81a2091c48c88a03fc53533a9eb9ee9f`.
  Public return type is the real Spring 6.2 `MultiValueMap`. It owns an
  incumbent M3 array/hash key directory and M3 linked value lists.
  Original `M3SpringMultiValueMap` is retained for existing users.
- New `M3FiveProviderFlagSelection<E>` builds ONE detached M3 count/flag
  snapshot by asking the existing `M3ObjectCounts.flaggedElements`
  to select full 32-bit required/excluded state bits. That delegates the
  actual coordinate selection to `M3IndexPathMasks` Java/C17 JNI and never
  sends references, callbacks or comparators to native. The detached
  snapshot preserves original insertion order, counted occurrences, nulls,
  flags and captured cardinality. Does not mutate the source.
- Real Guava `Multiset`, Eclipse Collections `MutableBag` and Apache
  Commons `MultiSet` facade instances share the SAME captured
  `M3ObjectCounts` owner; their distinct count/change semantics are
  NOT conflated. JDK Map and NavigableMap and Spring MultiValueMap
  outputs are *independent M3-backed materializations* because their
  mutations and cardinalities are different. Spring count projection
  represents one `List<Integer>` containing the captured count per key,
  NOT a Spring bag implementation. JDK sorted map refuses comparator
  collisions instead of losing distinct selected keys.
- Count columns `int[]`, flag columns `int[]` (all 32 bits), and
  JDK M3 object key arrays / primitive slot links remain the ONLY
  retained element storage. New API facade allocations are allowed;
  there is no new per-element Entry/Node backing or JNI symbol.

## Pinned source and exact candidate status

Original factory `M3LibraryCollections.java` SHA256
`48732bcf25f5fca5c9f684506c53608125cc14b27bed4981999cc54432da957d`
=> `b35e46bc758ae7118185d3513aacb9d7565ccab0bbe8a941a450e36f31de4413`.
Original SDK bindings `sdk-family-bindings.tsv` SHA256
`c9de7a7f2cd7d5f8820d425ee2a944e41869e988d280c295064e558442efabb1`
=> `19c42a55d5d7e470767a62d9f549b7990fe6ae4bebc4728b588e5be857169a89`.
Three additional ABSENT-before Java outputs and two ABSENT-before
real-SDK JUnit outputs in canonical hash-pinned Java LST manifest. This
is a distinct successor recipe; historical M3 #1174 manifests remain
unaltered, and its independently proven revision is preserved.
Class-backed `M3HashPinnedJavaSnapshotRecipe` and
`M3HashPinnedTextSnapshotRecipe` are reused. No replacement parser
or transformation framework.

Category ledger `CATALOGUE.tsv` compares actual provider return shapes,
complexity, Java GC/JNI ownership, rights and donor oracles. The original
Spring/Guava/Commons Java API dependencies are Apache-2.0, Eclipse
bag may carry EPL/EDL donor rights, OpenJDK GPLv2/CPE remains under its
own source contract. LeetCode/HackerRank/GFG are problem references only,
not borrowed editorials or transformed challenge source.

## Proof and required HOLD

- Strongest parent PR #1174 reports 211 core + 126 interop real
  provider JUnit executions and six checked native count/flag tests.
  These are historical parent receipts, NOT proof of this successor.
- Reused Spring direct class and its 4,500-step provider differential JUnit
  originate from #1190, but that PR's source corpus does not independently
  prove integration on #1174.
- New seeded JUnit directly checks all five provider types, nulls,
  bit31 flags, value counts, distinct-versus-occurrence cardinality,
  shared mutable provider snapshot behavior, detached JDK/Spring copies,
  comparator collision refusal and JNI-required parity.
- Actual canonical Maven/OpenRewrite fixed-point/refusal, owning core
  Maven/JUnit, optional real provider dependency Maven/JUnit, C17 JNI
  -Xcheck:jni, public method-tree retention and full JDK/jtreg/GC/heap
  still require exact-head receipts. Source has not been claimed green
  solely because it was generated.
- This is a substantial five-provider *collection-family* successor,
  **NOT complete replacement of every class and API in all five
  upstream libraries**. Immutable/synchronized/weak/reference/primitive,
  serializable and all allocating-result families remain individually gated.

Never rebase Synexia develop or modify product master directly.
