<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: GPL-2.0-only -->
# Concurrent Object.toAdapter identity and JNI gate

## Existing contract (locked)

`java.lang.Object.toAdapter(Class<T>)` returns the receiver for assignable
requests, the existing lazy `M3Object` capability for the exact `M3Object.class`
request, or `null` for unsupported requests; null `Class` throws
`NullPointerException`. Ordinary virtual dispatch and the erased
`(Ljava/lang/Class;)Ljava/lang/Object;` JNI descriptor stay unchanged.
Subclasses may override the method. This gate adds tests only: it must not
introduce a second adapter framework, fields on `Object`, a new JNI product
symbol, implicit mutation observation or new adaptation semantics.

## New concurrency qualification

`ObjectAdapterConcurrentIdentityTest.java` and its matching
`libObjectAdapterConcurrentIdentityTest.c` are an additive jtreg fixture.
Following the cold-creation race, six workers start together, each traversing the same deterministic set of
nineteen strongly retained receivers for 128 rounds. Sixteen different
receivers deliberately report equal via `equals` and the same `hashCode`;
the other three are primitive/reference arrays. At every iteration the
worker checks that Java and JNI adaptation return the original receiver,
share the exact retained `M3Object`, preserve its `.source()` identity,
and return `null` for a nonassignable request. One aggregate count proves
all scheduled iterations completed; a post-run pairwise identity check
confirms that equal-but-distinct receivers never alias capabilities.

### Additional cold-creation race

Before the retained-owner read workload, six workers use per-source barriers to
concurrently request the first `M3Object` for each of 64 newly allocated,
equal-but-distinct receivers. Half the workers enter through Java and the others
through JNI. All 384 returned capabilities remain strongly reachable until
every worker completes; one canonical facade per receiver is required. This
exercises first-allocation serialization, not merely hot lookup with a
previously cached facade. It does not force identity-hash collisions or prove
weak-collection behavior, which remain separate qualification requirements.

The run must complete without deadlock under both observation modes, in JIT
and interpreted execution. A timeout is a failure, not a performance result.

The native fixture resolves the inherited `Object.toAdapter` method with
`GetMethodID` and invokes it with virtual `CallObjectMethod`. It uses no
C-side cache or source-specific adapter and deletes its temporary class
reference. The existing `ObjectAdapterNativeTest` separately covers
`CallObjectMethodA`/`V`, exceptions, class-loader identities and bounded
local frames; this fixture must not duplicate those cases.

Run the new fixture on a **matched M3JDK21 image** with `-Xcheck:jni`, both
`jdk.m3.objectEvents.enabled=false` and `true`, followed by normal jtreg,
regression and native build gates. Compiling against a standalone model or
patched declaration stubs is only a syntax/fixture check, not an accepted
M3JDK runtime or JNI qualification. No performance improvement is claimed.

## Category-first donor evaluation

The authoritative, machine-readable decisions are recorded in
`DONOR_CATEGORY_EVALUATION.tsv`; any later source reuse must reverify
repository license, donor commit, semantic contracts and observable behavior.


| Category | Evaluated reference | Decision | Contract reason |
| --- | --- | --- | --- |
| Type dispatch | JDK `Class.isInstance` / `Class.cast` | REUSE EXISTING | Dynamic assignability precedes special capability |
| Identity ownership | JDK `WeakReference` / identity hash | REUSE EXISTING | Retained capability is canonical, unretained is collectible |
| Bounded cache eviction | LeetCode 146 LRU Cache | REJECT | LRU eviction/strong-key semantics do not prove retained facade identity |
| General hash lookup | LeetCode 706 Design HashMap | METADATA_ONLY | Requires identity and weak-lifetime adaptation |
| Frequency indexing | HackerRank Frequency Queries | METADATA_ONLY | Counts/frequencies are not object identities |
| Open addressing | GeeksforGeeks linear probing | CANDIDATE_ONLY | Tombstones and rehash are relevant to a separate source-sealed weak-index candidate, not this test |
| Open addressing (Java) | TheAlgorithms/Java LinearProbingHashMap (`761a5fe83d18`) | REJECT_DIRECT_PORT | Uses `equals` for keys; `contains` infers presence from non-null value; no weak values |
| Cuckoo hashing (Java) | TheAlgorithms/Java HashMapCuckooHashing (`8bcf00730acb`) | REJECT_DIRECT_PORT | Integer keys and relocating entries do not preserve identity-bound weak capabilities |

No third-party solution source is copied. Donor rights and downstream Java/JNI
compatibility remain independent gates. The proposed weak-index performance
change in Synexia PR #14948 is not promoted by this test-only work.

Sources:
- https://leetcode.com/problems/lru-cache/
- https://leetcode.com/problems/design-hashmap/
- https://www.hackerrank.com/challenges/frequency-queries/problem
- https://www.geeksforgeeks.org/dsa/implementing-hash-table-open-addressing-linear-probing-cpp/
- https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ref/WeakReference.html
- https://github.com/hsoliwal/M3jdk21/pull/2317
- https://github.com/TheAlgorithms/Java/blob/master/src/main/java/com/thealgorithms/datastructures/hashmap/hashing/LinearProbingHashMap.java
- https://github.com/TheAlgorithms/Java/blob/master/src/main/java/com/thealgorithms/datastructures/hashmap/hashing/HashMapCuckooHashing.java
- https://github.com/TheAlgorithms/Java/blob/master/LICENSE

## Qualification status

**LOCAL PASS (not product acceptance):** JDK 21 compile of the new Java test
against temporary `java.lang.Object` and `M3Object` API declaration stubs, with
`-proc:none -Xlint:all -Werror`; generated JNI header checked; C17 `-fsyntax-only`
and `-fPIC -shared` with `-Wall -Wextra -Werror -Wpedantic`. An independent
**model-only** test substituted a Java static adapter oracle and matching static
JNI entrypoint; under stock JDK 21 it completed 14,592 hot-lookup iterations and 384 cold-create results with six
workers in each of default and `-Xint` modes with `-Xcheck:jni`. This neither
exercises the installed `Object.toAdapter` method nor validates the product
native virtual dispatch. The checked-in C source is the virtual-call fixture.

**PENDING**: source-built M3JDK21 jtreg and whole-image tests, Windows/macOS,
full repo CI and performance benchmark. Record actual runtime evidence before
claiming the gate passes.
