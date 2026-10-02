# Whole-JDK M3 worked examples and semantic counterexamples

Status: **illustrative documentation only**. Pseudocode and proposed names in this file are not implemented APIs unless explicitly linked to an existing owner. Tests described as “required” were not executed by this documentation change.

The examples make the architecture concrete without converting a proposed representation into an implementation claim.

## 1. Shared lexicon hit + VM-local miss + reference-only join and slice

Assume an immutable published lexicon contains UTF-16 content for `"Who "`, while `"am I"` is not present.

Conceptual owner state:

```text
shared lexicon generation L7
  atom 41 -> "Who "

VM-local immutable interner generation V12
  atom 9  -> "am I"

joined value J
  segment 0 -> (L7, atom 41, start 0, length 4)
  segment 1 -> (V12, atom 9,  start 0, length 4)
  utf16Length = 8
```

`J` does not require a third eight-code-unit payload merely to represent the concatenation. A slice `J[2,7)` can retain ranges into those owners.

Required invariants:
- logical UTF-16 is exactly `"o am "`;
- owners/generations remain alive while the value or slice is live;
- equality/hash/compare inspect or reuse exact proven facts for logical content, not composition identity;
- a split surrogate at a segment or slice boundary remains two Java UTF-16 code units exactly as stock String requires;
- UTF-8/other encoding scans across segment seams correctly;
- `String`/JNI/serialization APIs that require contiguous storage cross an explicit materialization/projection boundary.

Counterexample: using the mapped virtual address of the lexicon payload as identity is invalid because another JVM may map the same immutable file at a different address.

## 2. Compact generic map with explicit boxing boundaries

Illustration: an internal workload maps primitive long keys to primitive int values. A candidate storage owner may use:

```text
long[] keys
int[] values
byte[]/bitset state
primitive hash/probe metadata
```

An explicit M3 primitive API can return `int` directly. A public `Map<Long,Integer>` view cannot.

Boundary:

```text
primitive lookup: 42L -> 7        // no boxing required in the primitive API
Map.get(Long.valueOf(42L)) -> Integer.valueOf(7)
```

Required:
- null policy is defined at the generic adapter;
- `containsKey` cannot be inferred from a sentinel value unless presence is represented separately;
- `Long.equals/hashCode` and `Integer.equals/hashCode` semantics remain the generic contract;
- `entrySet()` materializes/provides Map.Entry objects with correct mutation semantics if the map is mutable;
- wrapper allocation/caching at the generic boundary is measured rather than claimed eliminated.

Counterexample: saying “the map is primitive-backed, therefore Map<Long,Integer> has zero boxing” is false at generic object-returning boundaries.

## 3. Immutable shared collection versus mutable collection

An immutable set of canonical string IDs may normalize:

```text
domain = text-owner-generation T4
kind   = SET
lane   = sorted unique [11, 19, 42]
```

If exact domain + kind + lane equality is confirmed, multiple immutable wrappers may safely refer to one canonical collection body, subject to the documented identity contract.

A mutable `HashSet<String>` with the same elements is different:

```text
A = new HashSet<>(["a","b"])
B = new HashSet<>(["a","b"])
```

`A.equals(B)` can be true while `A != B`. Mutating `A` must not mutate `B`. Therefore the two mutable collection objects cannot be collapsed into one mutable canonical object.

Safe design options:
- independent mutable owner state;
- copy-on-write/persistent immutable body with a distinct mutable compatibility object and exact mutation policy;
- freeze/snapshot to a separately canonical immutable value.

Counterexample: “equal collection contents imply a shared mutable atom” violates Java object independence.

## 4. Backed collection view and mutation

Consider:

```java
var list = new ArrayList<>(List.of("a", "b", "c", "d"));
var view = list.subList(1, 3);      // ["b", "c"]
view.set(0, "B");
```

Required observable result: the root list reflects the supported backed-view mutation (`["a","B","c","d"]`).

An M3 slice descriptor for the view therefore needs more than `offset=1,length=2`: for a mutable backed view it must retain the root owner, mutation route, structural-version/modification rules and lifetime. A detached immutable slice would be a semantic change.

The same principle applies to map `keySet/values/entrySet`, navigable range/descending views and Java 21 reversed/sequenced views.

Counterexample: replacing every view with a compact snapshot because snapshots are cheaper to reason about changes aliasing.

## 5. Identity-sensitive keys and hash collisions

`IdentityHashMap` compares keys by reference identity. Suppose:

```java
String a = new String("x");
String b = new String("x");
```

Even though `a.equals(b)`, they are distinct identity keys. An M3 text interner may prove equal character content, but that content ID must **not** replace the key references in `IdentityHashMap`.

Separately, for an ordinary hash map, two unequal keys can have the same hash. A packed hash/signature lane may select a candidate but exact `equals` confirmation remains required.

Required identity taxonomy:
- Java reference identity;
- value equality;
- comparator equivalence;
- canonical immutable payload identity;
- collection-composition identity;
- owner-local numeric ID.

Counterexample: numeric ID equality from two independent arenas does not prove any of the above.

## 6. Concurrent update: linearization and visibility

Illustration for a `ConcurrentMap.compute`-style update on key K:

```text
T1 reads current mapping for K
T1 invokes remapping callback under the implementation's permitted protocol
T2 concurrently reads/updates other keys
T1 atomically publishes K -> V2
```

A candidate slot-table implementation must identify the operation's linearization point (for example, the successful CAS/locked publication that makes K->V2 current) and the happens-before/visibility path for a subsequent successful read.

Required proof covers:
- callback invocation count/recursive-update restrictions;
- null-return removal semantics;
- resize/migration of K while the operation is in progress;
- safe publication of the new value reference to GC and readers;
- no stale slot reuse/ABA if slot IDs are recycled;
- weakly consistent iterators are not misrepresented as an atomic snapshot;
- progress/fairness claims are no stronger than the actual implementation and JDK contract.

Counterexample: “uses CAS, therefore the collection is linearizable and lock-free” is not a proof of each operation or of whole-collection progress.

## 7. AST/DAG projection preserving semantic identity

Suppose an existing AST owner represents a call node with ordered children:

```text
CALL
  child[0] = receiver
  child[1] = argument0
  child[2] = argument1
```

A primitive M3 projection may store node kind and child IDs in lanes/CSR. It must preserve:
- child order;
- node kind and language/parser domain;
- source/attribution/type metadata required by consumers;
- owner namespace/generation;
- DAG sharing where present;
- edge meaning (child, control-flow, data-flow, relation type) rather than flattening every edge into an untyped adjacency list.

A hash or “object signal” may reduce candidates but exact structural/semantic confirmation is required before canonical reuse.

Counterexample: sorting child IDs to get a canonical lane would corrupt ordered AST semantics even if the same set of children remains.

## 8. Source enhancement port: rename + split + target conflict

Historical mapping:

```text
capability C-17
source S@100: com.synexia.OldIndex
target T@500: jdk.internal.m3.TargetIndex
relationship: adapted-port
last synchronized source: S@100
```

Upstream source S@140:
- renames `OldIndex` to `LookupIndex`;
- splits encoding policy into `IndexEncoding`;
- fixes overflow in `lookupRange`.

Target T@550 meanwhile:
- retains `TargetIndex`;
- has a JDK-specific bootstrap-safe allocator and different constructor;
- already fixed a separate GC-lifetime bug.

Correct port procedure:
1. diff S@100..S@140;
2. attribute rename, split and behavior fix to C-17 and any new capability IDs;
3. compare T@500..T@550;
4. preserve target bootstrap/GC adaptations;
5. port the overflow behavior through a deterministic recipe/patch;
6. map `IndexEncoding` as split/adapter/retained dependency rather than overwriting `TargetIndex`;
7. run exact target differential/build/runtime gates;
8. advance `last synchronized source` only after every source change has an explicit disposition.

Counterexample: copying the newest source file over the target because filenames or hashes look related can discard target-only correctness.

## 9. Failed acceptance gate blocks promotion

Candidate claims a smaller joined String representation and passes a microbenchmark, but an enabled differential test shows:

```text
expected: OutOfMemoryError at contractually exercised allocation boundary
actual: operation returns normally
```

or a collection candidate passes throughput tests but a `subList` mutation no longer reaches the root.

Required state transition:

```text
implemented -> verification FAIL -> remain OPEN/BLOCKED
```

Do not:
- weaken/delete the oracle;
- relabel the failure as a performance win;
- transfer an older passing receipt;
- mark the capability accepted because the PR merged.

The failed gate may trigger a contract review if the oracle itself is demonstrably wrong, but changing the expected contract is a separate reviewed decision.

## 10. Route comparison on one operation

Example: summing `List<Integer>` values.

### Route A — explicit primitive API

A caller chooses a primitive M3 list and sums `int` lanes directly. This can avoid boxing inside that API. Converting to `List<Integer>` is an explicit boundary.

### Route B — compiler lowering

The compiler may lower an internal loop over a proven nonescaping primitive-specialized value only if attribution, dispatch, exceptions, nulls, iterator semantics and identity are known. A generic externally visible `List<Integer>` cannot be rewritten merely from spelling.

### Route C — matched JDK

An ordinary JDK list implementation may use a specialized internal lane only if its runtime representation can still satisfy arbitrary `Integer`/null/reference behavior for that concrete object and all VM/library consumers. This is much stronger than Route A evidence.

The three routes can share algorithms and storage concepts; acceptance evidence remains route-scoped.
