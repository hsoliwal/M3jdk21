<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 object slot/index snapshot receiver

This is a **proving-ground receiver**, not collection promotion. The canonical
transformation owner is Synexia PR #12080 and the exact source/head is pinned in
`RECEIPT.tsv`.

## Why this atom exists

The current tree already has the important pieces separately:

* generation-safe 28-bit slots and compact object state bytes;
* dynamic primitive lanes and slot maps;
* live filtered wrappers;
* cached primitive filtered snapshots;
* primitive signal ordering;
* required/excluded mask JNI windows.

The missing reusable composition was an object payload owner plus detached
filtered/sorted snapshots expressed entirely as **payload arrays + physical
slots + state bytes + row permutations + offsets**.

The new classes fill that gap without adding Entry/Node/Cell objects or a new
state/JNI subsystem.

## Contract

`M3ObjectSlotStore28<E>` owns object references in sparse 4,096-reference
pages. `M3ObjectStateSlots28` remains the only lifecycle/state authority and
`M3LongLane28` carries the live generation-bearing handle for each physical
slot.

`M3ObjectIndexSnapshot<E>` is detached/read-only:

```text
Object[] payload snapshot
int[]    physical slot ids
byte[]   state flags
int[]    logical row permutation
[from,to) slice
```

Filtering by state uses required/excluded masks. Predicate filtering evaluates
once and retains row ids. Comparator sorting is stable indirect sorting. Signal
sorting projects once and reuses M3SignalOrder.

JNI receives only primitive rows and state bytes via the existing bounded
M3IndexMaskWindow kernel. Object references, predicates and comparators remain
in Java.

## Receiving boundary

The repository invariant is String -> arrays -> collections -> AST/compiler ->
remaining families. This receiver is intentionally marked
`WAITING_FOR_STRING`. It can compile and prove its local contracts, but it
cannot advance the Collections phase or become a first-release claim until the
predecessor gates are sealed.

## Verification

```bash
python3 m3/collections/qualification/m3-object-slot-index-snapshot-20261017/verify.py
mvn -B -ntp -f m3/collections/pom.xml -Dtest=M3ObjectSlotIndexTest test
mvn -B -ntp -f m3/tooling/migration-recipes/pom.xml \
  -Dtest=M3ObjectSlotIndexSnapshot20261017RecipeTest test
```

The focused GitHub workflow additionally builds the existing
`m3_collections_mask` JNI library with strict C warnings and reruns the
dependency-free proof with `-Xcheck:jni`.

The catalogue is mirrored from the Synexia convergence crate for review
continuity. It records why first-party M3 owners were selected and why external
collection implementations remain reference/evidence donors rather than copied
runtime code.
