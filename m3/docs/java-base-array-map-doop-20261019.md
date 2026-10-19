<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 java.base direct-array map behavioral atoms (2026-10-19)

The product is **M3JDK21**. Changes in this PR edit existing
`src/java.base/share/classes/java/util` classes directly; there is
no separate M3 + JDK compatibility layer, adapter, second map owner
or Synexia runtime dependency. The existing OpenJDK licenses and
original notices remain on modified JDK implementation sources.

## Serial owner-first changes

- `java.util.EnumMap` already retains canonical enum-ordinal
  `Object[] vals` and `K[] keyUniverse`. An array slot is absent
  iff `vals[ordinal] == null`; the existing `NULL` sentinel is a
  **present mapping to null**. Reuse this single authoritative mask,
  not an independent bitmap. `forEach`, `replaceAll` and
  `toString` now traverse offsets directly without per-mapping
  `EntryIterator.Entry` materialization. Public `entrySet()`
  still creates requested entries when called, as specified.
- `java.util.IdentityHashMap` already retains a single alternating
  `Object[]` mapping key at even offset and value at the next
  odd offset. `toString` now walks these exact coordinates,
  unmasking the existing null-key sentinel and preserving
  `(this Map)` rendering. It checks `modCount` before visiting
  a subsequent occupied slot after reentrant structural changes,
  matching the existing entry iterator's best-effort fail-fast
  boundary. Existing unusual reference-equality Map contracts and
  serialization are unchanged.

Neither operation copies or stores one `Map.Entry` per mapping.
No primitive JNI call is appropriate for arbitrary Java object
formatting or callback invocation; object references stay GC-traced
in the original Java arrays. Optional JNI mask compaction belongs
to the previously established M3 collections native owner.

## Verification surfaces

- New OpenJDK jtreg-style tests `DirectOrdinalTraversal` and
  `DirectOffsetString` compare the new operations against the
  original entrySet-based behavior under randomized mutation,
  nulls, order, exception timing, identity aliases, self references
  and structural interference.
- A focused GitHub Actions workflow copies the exact two JDK
  source files into an isolated patch source tree, compiles using
  `javac --patch-module java.base=...`, and executes both oracles
  with `java --patch-module java.base=...`.
- The exact SHA256 source-sealed Java transformations live in
  Synexia recipes
  `com.synexia.m3.Jdk21EnumMapDoopOrdinal20261019` and
  `com.synexia.m3.Jdk21IdentityMapDoopOffset20261019`.
  They are tooling/provenance assets, not a product runtime link.
- A source-to-category catalogue remains inside this product at
  `m3/collections/algorithm-catalog/java-base-array-map-atoms-20261019.tsv`.

The patch-module test is **not** a substitute for executing the
full OpenJDK build, jtreg suite, serialization compatibility checks,
HotSpot/GC validation, JFR allocation benchmarks and installed
M3JDK21 runtime image proof. `HashMap`, `TreeMap`,
`WeakHashMap` and `LinkedList` node-bearing implementations
still require separate, much larger contract-preserving array
conversions. Do not describe those as complete.

Source rights: `EnumMap.java` and `IdentityHashMap.java` retain
their GPLv2 with Classpath Exception terms; jtreg tests use
GPL-2.0-only. Metadata in this document is Apache-2.0.
