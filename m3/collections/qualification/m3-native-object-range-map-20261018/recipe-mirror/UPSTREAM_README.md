<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 native generic object-keyed RangeMap<K,V> — 18 October 2026

**Actual Java collection, not a provider adapter.** The source generator
creates `com.m3.collections.M3ObjectRangeMap<K,V>` in M3JDK21. This is
a generic reference-bearing Java owner for object key and value types,
**not another primitive long map** and not a relabeled Guava/Eclipse/
Spring/Apache implementation.

## Physical representation and behavior

One authoritative set of parallel growable arrays holds `Object[] starts`,
`Object[] ends`, `Object[] values`, and aligned `int[] states` (full
32-bit including signed/high bits). No retained per-span Entry, Node or
Range object. The existing M3SlotArrays owner controls geometric growth.
All arrays preallocate candidate replacements before publishing the new
lane shapes. Updates use binary-search bounded endpoints, one aligned
`System.arraycopy` tail shift, and O(1) metadata per edited span.
Range keys may be arbitrary comparator-ordered references, including
nullable endpoints when the comparator admits them. Null values are valid
and distinguished from absence with `containsPoint`.

Semantics: mutable sorted nonoverlapping half-open ranges `[from,to)`,
point get and getOrDefault O(log ranges), comparator-sorted insertion,
replacement and removal O(ranges) worst case; left/right clipping
preserves old state/value references. Neighboring ranges are deliberately
not coalesced: no Guava TreeRangeMap contract spoofing. Endpoint
comparator must be stable; this is single-threaded and not reentrant.

Each `IndexView` owns only detached `int[]` physical ordinals,
half-open offsets and a revision. Slices are O(1) shared metadata;
reverse and sorted-by-value transform ordinal arrays only using a
stable bottom-up merge sort. Java predicates are evaluated once per span,
never via native callbacks. All derived views are fail-fast under owner
mutation. An explicit `selected(required, forbidden, backend)`
uses existing M3IndexPathMasks with full 32-bit stages and Java or
real-required JNI backend. It passes only detached `int[]` row IDs
and physical offsets, with the caller-owned `int[]` state plane;
no Java reference, comparator, or Java heap address enters C17.
No new JNI symbol or duplicate algorithm engine.

## Canonical recipe FIRST

Named class-backed OpenRewrite `com.synexia.rewrite.M3NativeObjectRangeMap20261018`
reuses existing Maven `M3HashPinnedJavaSnapshotRecipe`.
Its sorted four-row manifest seals exactly:
1. new owner source (ABSENT-before);
2. independent Java21/C17 checked-JNI differential proof;
3. standalone reference-oracle randomized Java21 selftest;
4. owning Maven/JUnit adapter.

The `Recipe.run` JUnit exercises actual AST candidate generation,
SHA-256 exact source postimages, 32 source-order/presence combinations,
fixed point, duplicate owner and source drift refusals. Fix templates
and rerun this canonical recipe; don't patch emitted Java directly.

Run when the full source Maven/OpenRewrite SDK is available:
```sh
mvn -B -ntp -f synexia-openrewrite-recipes/pom.xml \
  -Dtest=M3NativeObjectRangeMap20261018RecipeTest test
```

## Executed evidence and remaining gates

The actual new source was compiled with OpenJDK21
`javac --release 21 -proc:none -Xlint:all,-serial -Werror`,
and **706,260 checks passed** in normal Java and again under `-Xint`
across 12,000 seeded native-generic range operations (natural/reverse
comparator), state high-bit tests, absent/null-key/value handling,
stable sorted/filtered spans, range editing and stale generation checks.
The local compile used two API-shaped test-only stand-ins for the
existing M3SlotArrays and M3IndexPathMasks owners, explicitly **not**
the real target module/native selector. An initial negative test
exposed missing contradictory-mask rejection and a reverse-domain
oracle error; both were corrected in the recipe source and retested.

The new independent real-JNI proof executes 600 randomized source
instances and compares Java-vs-native physical indices and GC-traced
value-reference identities with `-Xcheck:jni`; it is **authored,
not yet executed**. The target PR must separately establish real M3
source closure, owning Maven/JUnit, C17/UBSan JNI, installed JDK
jtreg/GC, concurrent/memory pressure behavior where applicable,
and API surface parity with vendor collection families.
This family is not an assertion that *every* JDK/Guava/Eclipse/Spring/
Apache class has been rewritten.

Existing M3 owners and copyright retained. LeetCode/HackerRank/
GeeksforGeeks are reviewed as *algorithm categories*, no challenge
solutions/editorials copied. First-party Apache-2.0 rights preserved.
