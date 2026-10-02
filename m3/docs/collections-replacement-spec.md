# Collections replacement specification

Status: documentation-only design. No JDK collection implementation is changed by this document.

This specification treats collections as a first-class M3 replacement programme. Interfaces define compatibility obligations; concrete implementations own representation decisions.

## 1. Design rule

Do not replace “Collection” with one universal M3 container.

Each concrete owner has different observable semantics:

- order;
- mutability;
- null handling;
- identity/equality;
- iterator behavior;
- view coupling;
- concurrency;
- serialization;
- subclass hooks.

Shared storage atoms are allowed only below those semantic membranes.

## 2. Observed current JDK representation

The pinned M3jdk21 master retains ordinary OpenJDK collection implementations.

Representative observed owners:

- `ArrayList`: `Object[] elementData`, `size`, inherited `modCount`; resizable-array semantics and fail-fast iterators.
- `HashMap`: bucket table with node/tree-node bins, load-factor resize, optional treeification, fail-fast collection views and unspecified map iteration order.
- `ConcurrentHashMap`: concurrent bin/table structure with non-blocking retrieval, per-key happens-before guarantees, weakly consistent traversal and no null keys/values.
- `CopyOnWriteArrayList`: volatile `Object[]` generation plus mutation lock; snapshot iterators.
- Other owners must be inspected at the exact implementation commit before replacement.

These are contract-bearing implementations, not merely inefficient layouts.

## 3. Existing M3/Synexia primitives to evaluate

Observed source-side owners already provide reusable storage ideas:

- `PackedFlatArrays`: growth geometry and primitive ring copying;
- `PackedLongDeque`: one `long[]` circular lane;
- `PrimitiveDeques`: primitive ring families;
- `PackedLongLongHashMap`: open-addressed `long[] keys`, `long[] values`, `byte[] states`;
- `PackedLongSortedSet`: one sorted primitive lane and batch merge.

Reuse is by contract. None of these classes is a drop-in replacement for a generic public JDK collection.

## 4. Storage toolkit for JDK collections

Candidate internal atoms:

### 4.1 Reference lane

A compact `Object[]` or segmented reference lane with:

- explicit size;
- growth policy;
- mod-count publication;
- copy/compaction helpers;
- no per-element wrapper.

Useful for `ArrayList`, queues and heaps.

### 4.2 Primitive lane

Primitive arrays for internal metadata:

- hashes;
- next/previous indexes;
- tree parent/child/color;
- states;
- order links;
- generations;
- counts.

Primitive metadata may remove node objects without changing generic key/value references.

### 4.3 Entry store

For maps, separate:

- key reference lane;
- value reference lane;
- hash lane;
- occupancy/state lane;
- bucket/chain/index lane;
- optional encounter-order lane.

A compact entry store is preferable to one Java Node per mapping where semantics permit it.

### 4.4 Immutable generation

For immutable and copy-on-write collections:

- frozen payload generation;
- structural sharing;
- explicit new generation on mutation;
- readers retain old generation.

This matches snapshot-style contracts better than mutating globally shared atoms.

### 4.5 Persistent structural nodes

Use only when persistent/path-copy semantics provide a measured win and do not create more objects than the baseline. A persistent tree is not automatically less bloated than a flat array.

## 5. Generic versus primitive APIs

A primitive backend can eliminate boxing only while values remain in a primitive domain.

Example:

- internal `long -> long` table: no boxing;
- public `Map<Long,Long>`: callers still pass/receive `Long` references unless compiler/runtime specialization proves otherwise.

Therefore record separately:

- retained internal boxing;
- boundary boxing;
- temporary boxing;
- autobox cache reuse;
- compiler-specialized path.

Do not report “boxing eliminated” for a generic surface when only the internal hash metadata became primitive.

## 6. List family

### 6.1 ArrayList

Current semantic obligations:

- random access;
- permits null;
- specified list encounter order;
- structural modification bookkeeping;
- fail-fast best-effort iterators/list iterators;
- backed `subList`;
- clone;
- serialization;
- `ensureCapacity`/`trimToSize`;
- exact bounds/exception behavior.

Candidate backend:

- one reference lane;
- explicit logical size;
- shared empty immutable arrays;
- growth policy compatible with the unspecified public guarantee;
- optional segmented lane only if it does not change random-access complexity or retention badly.

Do not use a shared immutable atom for mutable element slots.

Acceptance:

- differential public API;
- nested subList mutation;
- iterator/listIterator add/set/remove;
- modCount interference;
- array conversion;
- serialization round trip;
- self-add and aliased source cases;
- OOME/overflow boundaries.

### 6.2 LinkedList

Do not assume a packed array is automatically equivalent.

Contracts include:

- List + Deque;
- bidirectional list iterator;
- stable positional semantics;
- null support;
- insertion/removal at both ends;
- serialization.

Candidate choices:

1. compact index-linked entry store with element lane + next/prev int lanes;
2. segmented gap/deque structure;
3. retain baseline if packing harms mutation locality or iterator semantics.

A single flat array that shifts on every middle insertion is not a semantic-performance replacement for linked behavior.

### 6.3 Vector / Stack

Preserve synchronization behavior and legacy method surface. An ArrayList backend cannot be reused without the monitor contract.

## 7. Deque and queue family

### 7.1 ArrayDeque

Candidate:

- one circular reference lane using power-of-two or carefully proven modulo geometry;
- head/size or head/tail indexes;
- no node allocation.

Must preserve:

- null prohibition;
- first/last behavior;
- occurrence removal;
- iterator/descending iterator behavior;
- growth and overflow;
- clone/serialization.

Existing primitive ring atoms are useful algorithmic donors, not direct generic replacements.

### 7.2 PriorityQueue

Candidate:

- one reference heap lane;
- comparator retained separately;
- primitive size/index arithmetic;
- specialized sift helpers.

Must preserve:

- heap ordering by comparator/natural order;
- null rejection;
- iterator's unspecified order;
- remove semantics;
- serialization;
- comparator exposure;
- malformed comparator exceptions.

### 7.3 Blocking queues/deques

Default disposition: retain until exact lock/condition/visibility design exists.

For:

- ArrayBlockingQueue;
- LinkedBlockingQueue;
- LinkedBlockingDeque;
- PriorityBlockingQueue;
- DelayQueue;
- SynchronousQueue;
- LinkedTransferQueue.

A compact storage change must preserve:

- blocking/wakeup protocol;
- interruption;
- fairness where specified/configured;
- capacity semantics;
- weakly consistent traversal where applicable;
- happens-before;
- progress properties.

No “primitive ring” substitution is accepted without concurrency proof.

## 8. Hash map/set family

### 8.1 HashMap

Candidate backend options:

- open addressing with reference key/value lanes and primitive hash/state lanes;
- compact bucket-index chains with int next indexes;
- hybrid compact bins with tree fallback.

Do not choose open addressing merely because it has fewer objects. It must preserve:

- null key/value;
- equality via `equals`;
- user hashCode behavior;
- collision correctness;
- unspecified iteration order without inventing a new promise;
- fail-fast iterators;
- backed views;
- entry `setValue`;
- clone;
- serialization;
- compute/merge callback semantics and reentrancy behavior;
- tree-bin protection or equivalent adversarial guarantee.

Mutation of a key's hash/equality remains a user hazard; M3 must not “fix” it by interning the key.

### 8.2 HashSet

Shares map storage but retains Set contract and serialization representation. Acceptance must cover map-backed implementation details that are externally visible through serialization or reflection assumptions where supported.

### 8.3 LinkedHashMap / LinkedHashSet

Add an explicit encounter-order index:

- insertion order or access order;
- head/tail;
- next/prev lanes.

Must preserve:

- access-order mutation on reads where documented;
- eldest-entry hook;
- fail-fast behavior;
- view order;
- subclass protected behavior.

A plain HashMap compact backend is insufficient.

### 8.4 WeakHashMap

Default disposition: retain pending evidence.

Weak-reference processing, ReferenceQueue interaction and GC semantics dominate object-count optimization. Do not replace weak keys with numeric IDs that strengthen lifetime.

### 8.5 IdentityHashMap

Mandatory rule: key equality is reference identity.

Candidate compact backend may use:

- reference lanes;
- identity hash;
- open addressing.

Never:

- content-intern keys;
- replace `==` with `equals`;
- collapse two equal but distinct references.

Test with equal Strings that are distinct objects, mutable equal objects, self keys, null and resize.

### 8.6 EnumMap / EnumSet

These are already specialization-oriented. Measure before changing.

Potential M3 work:

- verify compact ordinal lanes/bitsets;
- reuse shared immutable empty/small shapes if contract-safe.

Do not replace a good specialized representation simply for architectural uniformity.

## 9. Sorted and navigable collections

### 9.1 TreeMap / TreeSet

Candidate backends:

- packed red-black tree: key/value reference lanes plus int parent/left/right and byte color;
- paged sorted reference lanes with explicit navigation;
- immutable persistent tree for immutable variants only.

Comparator semantics are authoritative.

Must preserve:

- `compare`-based key identity;
- comparator null rules;
- first/last/lower/floor/ceiling/higher;
- subMap/headMap/tailMap backed views;
- descending views;
- entry mutation rules;
- fail-fast behavior;
- serialization.

A sorted-array backend changes insertion complexity; accept only if workload/contract goals explicitly allow that internal complexity tradeoff and no documented complexity promise is violated.

### 9.2 ConcurrentSkipListMap/Set

Default: retain/adapt only after lock-free/progress proof. Compacting nodes cannot weaken sorted concurrent navigation or weak consistency.

## 10. Immutable/unmodifiable collections

Separate:

- immutable value owner;
- unmodifiable view over mutable owner.

They are not equivalent.

### Immutable owner

May safely use:

- shared empty/singleton shapes;
- immutable flat lanes;
- structural sharing;
- content-derived precomputed hash where API permits and lifetime is bounded.

### Unmodifiable view

Must retain the backing collection relationship. If the backing collection changes, the view may reflect those changes according to the wrapper contract.

Never replace an unmodifiable wrapper with an immutable copied atom unless the existing operation already promises a copy.

## 11. Copy-on-write collections

`CopyOnWriteArrayList` and `CopyOnWriteArraySet` are natural generation-based candidates.

Candidate M3 representation:

- immutable generation object containing one reference lane;
- volatile current-generation reference;
- mutation lock;
- writers allocate new generation;
- iterators retain prior generation.

Possible optimization:

- share unchanged chunks/segments for large lists, if traversal and array snapshots remain exact.

Must preserve:

- snapshot iterator;
- unsupported iterator mutation;
- memory consistency;
- mutation atomicity;
- `toArray` independence where required.

Avoid retaining a huge old generation through a tiny snapshot without measurement; compaction policy must be explicit.

## 12. ConcurrentHashMap

Treat as its own programme.

Required documented semantics include:

- no null keys/values;
- retrievals generally non-blocking;
- per-key happens-before from completed update to reporting retrieval;
- atomic map operations such as putIfAbsent/replace/compute families as specified;
- concurrent resizing;
- weakly consistent iterators/spliterators;
- bulk operations;
- approximate aggregate counts during mutation.

A proposed compact backend needs:

- publication protocol;
- resize/help protocol;
- collision defense;
- atomic state transitions;
- counter design;
- memory-order proof;
- stress tests.

No reuse of a single-threaded packed hash table without a new concurrency design.

## 13. Views

Backed views are first-class acceptance surfaces.

Inventory and test:

- `List.subList`;
- `Map.keySet`;
- `Map.values`;
- `Map.entrySet`;
- sorted/navigable subviews;
- descending views;
- synchronized/unmodifiable wrappers.

For each view record:

- owner retained;
- allowed mutation;
- mutation propagation direction;
- iterator semantics;
- equality/hashCode;
- spliterator flags;
- serialization if applicable.

M3 view descriptors must never outlive a mutable owner without the same failure/observation behavior as the baseline.

## 14. Iterators and spliterators

For each concrete collection record:

- fail-fast best effort;
- weakly consistent;
- snapshot;
- immutable.

Preserve:

- remove/add/set support;
- exception class;
- late binding;
- exact `Spliterator` characteristics;
- estimated/exact size behavior;
- encounter order;
- parallel split behavior.

A primitive internal cursor can be used beneath the Java iterator object; the public iterator object cannot be removed if the API returns one.

## 15. Callback-bearing methods

Methods such as:

- removeIf;
- replaceAll;
- compute;
- computeIfAbsent;
- computeIfPresent;
- merge;
- forEach;

have callback ordering and exception semantics.

Rules:

- do not invoke user callbacks under a new lock unless baseline does;
- do not invoke a callback twice because of optimistic retry unless allowed;
- preserve partial mutation behavior on callback failure;
- preserve reentrancy detection where implemented;
- preserve null-result semantics.

## 16. Serialization and reflection

Every replacement must inventory:

- `serialVersionUID`;
- custom `writeObject/readObject`;
- serialized ordering;
- capacity fields that are or are not serialized;
- clone;
- protected/package-private hooks used by related classes;
- reflection-sensitive fields only where compatibility policy requires them.

Do not assume private layout is irrelevant to JDK compatibility tests.

## 17. Memory model for measurements

For each baseline and candidate report:

- collection object bytes;
- backing arrays;
- per-entry node bytes;
- reference lanes;
- primitive metadata lanes;
- empty-capacity bytes;
- resize peak live bytes;
- temporary iteration/view objects;
- retained old generations;
- boxed key/value bytes where applicable.

Report both primitive payload and actual retained heap.

## 18. Performance matrix

Benchmark separately:

- construction;
- get/contains;
- add/put;
- remove;
- iteration;
- view creation;
- bulk operations;
- resize;
- clear/reuse;
- serialization;
- collision/adversarial behavior;
- multi-threaded contention for concurrent owners.

Sizes:

- 0, 1, tiny;
- cache-resident medium;
- large;
- long-lived sparse capacity;
- mutation-heavy.

No universal “faster” claim from one lane.

## 19. Worked example: compact generic map

Illustrative candidate only:

```
Object[] keys;
Object[] values;
int[] hashes;
byte[] states;
int size;
```

Possible advantages:

- removes one Node object per entry;
- keeps metadata contiguous;
- separates generic references from primitive metadata.

Still required:

- call user `hashCode/equals`;
- permit HashMap null rules;
- return ordinary `Map.Entry` views;
- preserve fail-fast;
- preserve callback semantics;
- preserve serialization;
- manage adversarial collisions.

This design does **not** eliminate boxing for `Map<Long,Long>`.

## 20. Worked example: identity-sensitive keys

Given:

```
String a = new String("x");
String b = new String("x");
```

For `HashMap`, `a` and `b` are equal keys.

For `IdentityHashMap`, `a` and `b` are distinct keys.

Any M3 canonical text interner used as the key identity source would break `IdentityHashMap`. A compact identity map must retain original references.

## 21. Worked example: immutable versus mutable

An immutable list may point to one frozen lane shared by many readers.

A mutable ArrayList cannot share that lane with unrelated mutable lists unless mutation performs a private generation/copy before any observable change.

An unmodifiable view cannot silently become an immutable copy.

## 22. Acceptance packet per implementation

Every collection work packet includes:

1. exact baseline class and blob hash;
2. documented current representation;
3. candidate representation;
4. public/internal contract table;
5. source-bound transformation recipe;
6. differential test suite;
7. serialization/view/iterator tests;
8. memory measurements;
9. complexity comparison;
10. rollback;
11. unresolved risks.

Until all applicable items are complete, status is proposed/implemented/verified-scoped, never “collection replacement complete”.
