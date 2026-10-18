# M3JDK21 Java/JNI array, ownership and sizing slice

## Target and limitations

This is an **M3JDK21 in-JVM change set**, not a "M3 + JDK" application layer.
Its Java classes are in `src/java.base/share/classes/jdk/internal/m3/`, and its JNI
method is intended to compile into the platform's `libjava` native library.
A standalone JDK 21 `--patch-module` workflow qualifies the new classes against
OpenJDK 21 without claiming that the target M3JDK21 fork builds successfully.

**Status:** Local Java 21 and C/JNI validation passed. The full fork was not
cloned or built. This experimental slice is submitted on a review branch;
the pull request remains draft pending full-fork build, jtreg/HotSpot gates
and reconciliation with active collection work.

## Data invariants

1. No per-element `Entry` object in storage. Values live in `Object[]` or
   immutable `byte[][]` pages; metadata lives in parallel primitive arrays.
2. Every mutable slot descriptor uses generation-qualified handles. Reused
   slots cannot make prior handles valid; wrap retires a slot.
3. M3 lease counters are **not** arbitrary Java reference counts. Only M3
   protocol acquisition/release is represented.
4. Filtered and stable-sorted views use lazily refreshed `int[]` indexes; views
   are read-only, live as backing updates happen, and fail fast for iterators.
   They do not claim Guava's mutable filtered-collection contract.
5. Interned atoms are stored once per pool for equal byte content. Hash lookup
   is collision-checked against all bytes; `slice` and composite concatenation
   never copy the original payload. Existing `java.lang.String` has **not**
   been replaced; preserving its full semantics is separate work.
6. JNI/JVMTI sizing returns **shallow bytes only** (an implementation-specific
   estimate); it is neither an ownership count nor retained size.
7. Source-inventory recipe is syntax-aware, serial per file and SHA-256 based;
   it emits `UNPROVEN` candidates. It makes no source edits or unjustified
   equivalence claims.

## Source mapping

- `M3IndexedArray.java`: dynamic parallel arrays, masks, stable handles,
  explicit leases, logical position arrays, removal and free-slot reuse.
- `M3IndexedView.java`: versioned filtered and sorted index wrappers.
- `M3IntAlgorithms.java`: problem-category-qualified XOR singleton and adaptive
  lonely-neighbor filters with offset-aware primitive-array kernels and mask storage.
- `M3ByteSlices.java`: immutable page-backed atoms, open-addressed `int[]`
  intern dictionary, offsets, lengths, read-only ByteBuffer API facades.
- `M3CompositeBytes.java`: immutable sequence of primitive descriptors,
  64-bit positions, sharing across concatenation and slicing, lease lifecycle.
- `M3ObjectMetrics.java` + `M3ObjectMetrics.c`: JNI/JVMTI shallow object sizing.
- `make/m3/recipes/`: deterministic Java 21 source-inventory recipe with
  Maven descriptor and a local fixed-point test.
- `donor-catalogue.tsv`: searchable per-problem donor comparison and status.

## Complexity / expected costs

| Mechanic | Complexity | Remarks |
| --- | --- | --- |
| XOR singleton (paired inputs only) | O(n) time / O(1) extra space | Not a substitute for true frequency counting |
| Lonely-neighbor dense | O(n + U/64) time / O(n + U/64) space | long[] presence/duplicate masks for bounded integer domain U |
| Lonely-neighbor sparse | O(n) expected time / O(n) space | int[] hash keys + byte[] masks; worst-case O(n²) probing |
| Indexed append | amortized O(1) | occasional copies of primitive indexes and reference array |
| Indexed remove | O(n) | shifts logical `int[]` positions, not stored values |
| Acquire/release lease | O(1) | per-handle synchronized updates |
| Filtered view rebuild | O(n) | cached until store version changes |
| Stable sorted index rebuild | O(n log n) | O(n) primitive scratch; no entry copies |
| Atom interning | O(len) average | open addressing, checks byte equality on hash matches |
| Atom slice | O(1) | one primitive-array descriptor, shared page |
| Composite concat | O(pieces) | descriptor copy only, no byte copies |
| Composite slice | O(pieces) | descriptor arithmetic only |
| Composite byteAt | O(log pieces) | long index with prefix search |
| JNI shallow size | VM-specific | diagnostic query; never used for liveness |

## Known gaps / explicit compatibility gates

- No HotSpot C1/C2 compiler patch, write-barrier instrumentation, complete
  heap reference-edge counters, or automatic `javac` ownership proofs.
- No `java.lang.String` VM layout rewrite, GC/CDS/JNI String integration,
  native shared lexicon, pool eviction, or native direct-buffer recycling.
- No mutable Guava-like filtered view, SortedSet/NavigableSet wrapper,
  Eclipse or JDK API replacement, or LFU entry-facade cache yet.
- No performance benchmark or full JDK test suite / `jtreg` on the target fork.
- No proof that new paths are conflict-free with active fork branches or PRs.
- Composite concat is caller-lifecycle-confined: concurrent `close()` must be
  coordinated by callers; the byte store itself synchronizes its operations.
- Off-heap ownership requires stronger memory reclamation than the current
  Java-heap-backed immutable pages. Pages live as long as the pool object.

## Deterministic local proof

Run from the package root on Linux with JDK 21 plus a C compiler:

```sh
./make/m3/test-local.sh
./make/m3/recipes/test-local.sh
```

The first script patches the JDK 21 `java.base` module locally, builds a JNI
shared library solely for local tests, and executes four Java test mains.
The second compiles/runs the Maven-compatible AST recipe and verifies its
file-local, fixed-point manifest by byte comparison.

In a complete fork build, the C source is expected to be included in libjava
through the JDK native source discovery. That full native build integration
still requires verification against the actual fork build recipes.

### Replacement/recipe invariant

Keep the baseline hashes; perform inventory before changing any existing file.
For each requested replacement: classify, compare donors, record license and
expected contracts, run the candidate on generated fixtures, compile/test with
HotSpot as the oracle, compare observable behavior, reject on disagreement,
then rerun to fixed point. Independent candidate trials can be parallelized,
but per-file source changes must remain serial and reviewable.

## LeetCode / HackerRank qualification: two different lonely contracts

LeetCode 136 and HackerRank Lonely Integer require **exactly one singleton** and
that **every other integer occurs exactly twice**. The XOR atom therefore
requires no counts or entry objects; an odd input length is necessary, not a
proof of the full data contract. Never replace general uniqueness or ownership
counting with XOR.

LeetCode 2150 additionally requires the singleton's `x - 1` and `x + 1`
values to be absent. M3JDK21 uses a deterministic data-shape selector:

- For a compact span at most 1,048,576 integers and at most eight times the
  input count (with a 64-value floor), use `long[]` bit masks for presence
  and duplicate status; each frequency saturates at two.
- Otherwise use open-addressed `int[]` keys and `byte[]` state masks, with
  explicit equality checks on hash collisions. This is expected O(n),
  but adversarial collisions have a worse bound.

Both paths preserve encounter order, accept negative/full-range `int`
values without neighbor overflow, do not mutate the input, expose
`offset,length` bounds, and store no per-value objects. Randomized test
comparisons against a straightforward boxed-HashMap oracle cover both
paths; separate randomized paired-input tests qualify XOR. Tests are
algorithmic contract checks, not full JDK or performance qualifications.

These examples are algorithm categories and candidate inspirations, not
licenses to copy third-party solutions. Catalogue entries explicitly track
constraint matching, selected/rejected alternatives, and verification.
