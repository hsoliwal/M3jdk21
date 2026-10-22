<!-- SPDX-License-Identifier: Apache-2.0 -->
# JDK21 collection receiving → DOOP array/offset/mask runtime owners

**Review branch:** stacked on [source-custody PR #1325](https://github.com/hsoliwal/M3jdk21/pull/1325).  
**Source custody:** 59 selected JDK 21 implementation files, original license and binding records in
`m3/collections/implementations/jdk21-package-renamed/` and
`m3/collections/receiving/jdk21-package-renamed-20261018/`.
Those files remain **unchanged** by this follow-on; custody claims are not runtime claims.

## Immutable design invariant

1. Physical collection state resides in parallel `Object[]`, primitive `int[]/long[]`,
   optional `byte[]` state-bit lanes. Hash buckets, sorted order, links, free slots,
   offsets, and masks are integers, **never** stored per-mapping `Entry`, `Node`, or
   wrapper objects.
2. Logical keys and values are references to user objects; the invariant does **not**
   prohibit user-owned object values. Ordinary `Map.Entry` projections are created only
   if and when a public API explicitly requests one (`entrySet().iterator().next()`,
   navigation entry-return APIs, etc.). They are not physically stored in the map.
   Views and iterators may be lightweight API objects; their traversal state is
   integer offsets, not copied per-element entries.
3. Empty owners share zero-length arrays; new arrays are allocated only on first
   insertion or growth. Removal nulls stale reference slots. Slot reuse is by integer
   free-list. No back-end delegation to standard `HashMap`, `HashSet`, `ArrayDeque`
   or `PriorityQueue`.
4. Internal state bits are not JVM reachability. GC remains authoritative for heap
   objects, native/JNI handles have independent lifetime rules, and `M3LFU` may reclaim
   **only** proven recreatable/cached *auxiliary* state (unborrowed and unpinned).
   It must not evict live map entries, queue items, keys, or values and silently
   change JDK semantics. Weak-key collections alone use weak reachability as contracted.
   Existing `M3ObjectResidency` and `M3GcPressureGate` can gate suitable
   parent-owned deferred projections; do not create a second lifetime authority.
5. Slot-state bit masks are local owner metadata and are cleared on removal/reuse.
   No object address is cached across JNI calls or moving-GC safepoints.

## Concrete direct runtime owners in this stacked PR

| JDK source family | Independent M3 owner | Structure | Contract status |
| --- | --- | --- | --- |
| `HashMap` | `M3HashMap` | parallel key/value, bucket-offset, free, state-bit arrays | Differential tests added; tree bins, serial form, JDK replacement pending |
| `HashSet` | `M3HashSet` | `M3HashMap` key-only physical lanes | Differential tests added; JDK replacement pending |
| `ArrayDeque` | `M3ArrayDeque` | circular offset `Object[]` | Differential tests added; JDK replacement pending |
| `PriorityQueue` | `M3PriorityQueue` | implicit binary heap `Object[]` | Differential tests added; JDK replacement pending |

Other existing array owners include `M3ArrayList`, `M3TreeMap`, `M3TreeSet`,
`M3LinkedHashMap`, `M3LinkedHashSet`, `M3LinkedList`, `M3WeakHashMap`,
and `M3EnumSet`. Their existence **does not** prove JDK bytecode or API
compatibility. All 59 source types and dispositions are recorded in
`SOURCE_TO_OWNER.tsv`; no unimplemented source is marked admitted.

## Recipe-first path and acceptance gates

Use the canonical M3Scale pipeline: exact chronological source inventory → donor
algorithm catalogue by category → validated per-file **Inline → Atomize → Patternize
→ Comment** recipe → compiled/generated diff → repeat to fixed point → contract
oracles → PR. The four direct owners above are initial independently written
runtime code, **not** claims that the full JDK donor bodies were transformed or
that a qualified OpenRewrite/HotSpot recipe already ran. Those parts remain open
until a real validated recipe and its fixed-point/rollback evidence exist.

Run `mvn -B -ntp -f m3/collections/pom.xml test` on a Maven-capable JDK 21
environment, including `M3HashMapSetTest` and `M3QueueContractTest`.
The existing `M3 collection contracts` GitHub workflow additionally executes
repository-specific proof. Record actual runner evidence before advancing the
disposition; no test pass or speedup is inferred from the presence of test sources.

Before promoting to `java.base`, validate all JDK public surface operations,
serialization, iterators/views, exception parity, null/identity/comparator behavior,
Java Memory Model for concurrent types, rare collisions, memory pressure, GC,
JNI/native boundaries and jtreg. Preserve public `java.util` names, ABI and licenses.
