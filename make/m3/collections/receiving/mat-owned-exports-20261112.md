<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Immutable MAT primitive exports

This receiving batch refines two existing internal owners in `jdk.m3.collections`.
Its admission is bounded component review. It does not advance the ordered
String → arrays → collections receiving stages or admit a replacement for a
standard Java collection. The final source identities and executed checks are in
the adjacent [passive receipt](mat-owned-exports-20261112.json).

## Source and target ownership

The implementation candidates originate in Synexia PR
[#17163](https://github.com/hsoliwal/com.synexia/pull/17163), head
`11d030de097f4db167610fe642f3835dcfb8cc4c`, in the existing
`m3-collections-contract-refinement-20261017` recipe host. This batch reuses
`com.synexia.m3jdk.CollectionsContractRefinement20261017`, with exact current
M3JDK21 inputs from `b98bf8c065818aac3447be165909e2a013325d9a`. Recipe sources,
templates, mastery tests and execution machinery remain in Synexia. The target
receives generated implementations, product tests, CI configuration and receipts.

| Current owner in `jdk.internal.m3.collections.mat` | Refined operation | Preserved contract |
| --- | --- | --- |
| `MatAdaptiveIntIntMap` | `valuesBySortedKey()` | Ascending signed-key alignment, duplicate overwrite behavior, detached mutable result array, all three existing encodings |
| `MatSparseIntVector` | `fromDense()` | Caller input remains independent; private freshly allocated position/value arrays become the immutable owner's arrays; validation remains active |
| `MatSparseIntVector` | `toArray()` | Exact size/default/explicit values and a fresh mutable array on every call |

The sources remain at
`src/jdk.m3.collections/share/classes/jdk/internal/m3/collections/mat/`.
Class/package names, public and protected descriptors, the explicit map builder
constructor, historical exception text, license banners and previous lineage
documentation are retained. No module export, retained entry object, native
symbol or runtime dependency is added.

## Why these atoms change

For SORTED maps, the stored key and value arrays already share an ordering, so
export clones the value array directly instead of allocating a key copy and
performing a binary search for each value. DIRECT maps scan existing occupancy
bits in ascending physical-slot order and copy only present values to one output
array; signed key order follows the existing minimum-key/offset mapping. HASH
export retains its current algorithm.

The sparse factory already creates private position/value arrays. A private
ownership-transfer constructor avoids cloning those arrays again. The public
constructor still defensively copies caller arrays and retains the same
validation sequence. Dense export fills nonzero defaults; a fresh zero-filled
Java array needs no additional fill for the zero default.

These changes reduce explicit copying and lookups. Any allocation measurements
in the receipt describe their exact sampled workload, not a universal CPU or
whole-JDK performance guarantee.

## Receiving tests and Maven integration

`MatPrimitiveExportContract` retains the donor's 211,200 deterministic checks
and adds constructor, bounds, empty-map, signed-extreme, detached-array and
DIRECT bitmap-hole cases. `MatPrimitiveExportContractTest` runs the same
oracle through the existing Jupiter suite. Both live under
`test/jdk/jdk/internal/m3/collections/mat/`; the existing `m3/collections/pom.xml`
already includes that root. No new Maven module or dependency is necessary.

The shared `m3jdk-modules` workflow runs the exact test after building the owning
arrays/collections Maven modules. The shared collections workflow also watches
the moved JDK source, test and native-build roots. Existing jobs, permissions and
required gates remain in place.

The historical `full-java-jni-20261018/verify.py` still contains old source-root,
class and native-library assumptions. Its reconciliation and the complete
source-built JDK, jtreg, JNI/platform and release gates remain separately open;
the scoped oracle does not certify them.

## Naming and lineage disposition

Disposition: `EXISTING_OWNER_REFINEMENT_NO_RENAME`.
The canonical per-type map has no exact record for these two existing owners.
That map gap is explicitly deferred under naming law 9 of
`doc/m3/m3jdk21-porting-invariant.md`; this record does not invent a previously
accepted capability ID or grant backend/phase promotion. The existing canonical
map remains the authority. No source is relocated and no type becomes public.

This is routine maintenance of already-public array/bitmap owners. Existing
Apache-2.0 OR EPL-2.0 source notices and upstream attribution remain unchanged;
the new target regression and receiving documentation carry their own notices.
