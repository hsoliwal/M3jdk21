# Native index windows and stable-order reuse

Product owner: `hsoliwal/M3jdk21`. This refines existing `M3LongIndexProjection` and
`M3IndexMaskCompactor`; it does not introduce another collection, viewport, row store or
object-lifetime model. Canonical recipe/test packet is under
`m3/collections/recipes/native-index-range-20261017` in this same public receiver.
The original Apache-2.0 Synexia recipe engine is reused as build tooling only.

## Arrays, indexes, offsets and state masks

A projection retains one private immutable `long[]` payload and a private `int[]` physical
row directory. A slice retains half-open offsets into that directory. Flag bytes supplied
at filtering time describe **logical positions in the current view**; they are not indexed
by physical row ID. Required bits use unsigned-byte semantics. Stable ordering and exact
duplicate occurrences survive filtering, slicing and sorting.

Previously the JNI route materialized `physicalIndices()` for the entire logical window
before invoking the native selector. The new package-private range path gives the existing
native core `(indices, from, size, states, mask)`. It returns the same exact detached
selection index without the preliminary `int[n]`. Predicates/comparators/object references
never cross JNI. Neither payload nor states are retained by native code. Fixed Region-API
chunks remain at 256 elements; in-place prefix compaction of the local row-ID chunk removes
one additional 256-jint scratch array. A zero required mask avoids state reads because all
states necessarily match. Input state bytes must remain stable during a call, as before.

Existing exported `selectNative` and public Java signatures remain. Capability discovery
is lazy and private. A binary without the range capability uses its **original native**
whole-window route with the original copy; it does not silently switch to Java. No library
still raises `UnsatisfiedLinkError`, including an empty native-required selection.
An advertised range ABI with a missing/broken range symbol is not concealed.

Repeated sorting in the already requested direction returns a distinct immutable descriptor
sharing its existing index window. Empty/singleton windows do the same while reporting the
requested direction. Opposite-direction requests still use the incumbent stable signed-long
merge sort; simply reversing indexes would reverse equal-key occurrences and is rejected.
Public defensive array-returning methods still copy. This sharing is valid only because
payload and indexes are already privately owned and immutable.

## Costs and boundaries

New-binary native filtering remains O(n) work and O(k) result storage, with fixed native
scratch, but removes the extra O(n) Java window copy. Legacy binaries keep their former
cost. Same-direction sorting becomes O(1) extra space/time for descriptor construction;
other sorting retains O(n log n) time and O(n) index scratch. Measured allocation observations
and environment are in the recipe's `evidence/RESULTS.md`; they are not JMH, application
latency, or a general JNI speedup claim.

This is a detached prepared index snapshot. It is deliberately not substituted for the
live filtered `Collection`, `Set`, `SortedSet`, `NavigableSet`, map or multiset wrappers.
Their predicate reevaluation, mutation, comparator/range, concurrency and null contracts
remain with their existing owners. RWT/GWT/Nebula scaffolding is not moved into `java.base`.
All-widget conversion, full JDK integration, jtreg/platform/GC qualification remain separate.

## Example and reproduction

`examples/IndexedMaskPipelineExample.java` runs the same sorted/sliced/high-bit mask pipeline
with `JAVA` or explicitly requested `NATIVE_REQUIRED`. The proof runner compiles and runs
both. The new owning-module JUnit adapter invokes the same generated self-test.

From repository root:

```sh
python3 m3/collections/recipes/native-index-range-20261017/verify.py
mvn -B -f m3/collections/recipes/native-index-range-20261017/pom.xml clean verify
```

To update generated sources, edit templates/recipe and reviewed hashes, qualify, then use
`verify.py --apply` in an isolated checkout. The recipe rejects source drift, missing pinned
owners and corrupt templates; a repeat is a fixed point. Promotion is serial, not a
multi-file filesystem transaction. Check the evidence before treating any gate as passed.

Catalogue search uses the accumulated TSV files, for example:

```sh
grep -inE 'native-range|stable-order' m3/collections/algorithm-catalog/*.tsv
```
