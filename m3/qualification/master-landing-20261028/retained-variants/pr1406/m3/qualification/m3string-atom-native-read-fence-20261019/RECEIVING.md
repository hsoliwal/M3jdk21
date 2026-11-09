# M3JDK21 — original native String Atom lifetime hardening

**Runtime ownership:** `src/java.base/share/classes/java/lang/M3StringAtom.java`
in **M3JDK21 itself**. `M3String` remains the canonical
owner+packed-coordinate value. Neither `String`'s public API,
interning policy, native address, coder, payload ownership, GC layout
nor JNI symbols are changed.

## Source-first convergence and exact identity

Product base: [M3JDK21 #1414](https://github.com/hsoliwal/M3jdk21/pull/1414)
at `672e29f20bd039acaaeeb97ea17eee2939fcc8e4`.
It retains [#1406](https://github.com/hsoliwal/M3jdk21/pull/1406)
`M3StringPool` and [#1407](https://github.com/hsoliwal/M3jdk21/pull/1407)
fixed-address GVAS implementation without rewriting them.

Existing original *Synexia* source recipe: [#12410](https://github.com/hsoliwal/com.synexia/pull/12410).
Execution successor: [#12453](https://github.com/hsoliwal/com.synexia/pull/12453).
Named forward `com.synexia.m3.StringAtomNativeFence20261018`
and inverse reuse `M3HashPinnedJavaSnapshotRecipe`, with source
manifests, full Java AST candidates and original runtime test.
No new implementation/recipe engine or runtime Synexia dependency.

The two baseline product Git blobs and all source SHA-256 values
match the canonical recipe before any edit. The receiver's two
changed sources match the exact canonical template Git blobs
after the serial source operations; see `SOURCE_SEALS.tsv`.
The original 9,382-byte jtreg source is retained verbatim within the
expanded test, including its original cases and two launch modes.

## Bounded native liveness atom

The existing scalar `M3StringAtom` reads native/mapped memory through
`Unsafe.getByte`. A local atom is weak-pool tracked and native bytes may
be retired after the atom becomes unreachable; `payloadOwner` is null for
local atoms, so it does not establish the last-use lifetime.

Reuse OpenJDK's `java.lang.ref.Reference.reachabilityFence(this)` in a
`finally` after the last primitive read in `charAt`, `getChars`,
`getBytes`. Bounds/null checks, copied code units, UTF-16 endianness,
all return/exception paths and early LATIN1 branches remain unchanged.
This protects last-use receiver reachability but is not a general
solution for concurrent explicit unmap/free or arbitrary foreign memory.

## Regression and acceptance

Expanded existing jtreg `M3StringOwnerRangeLifetimeTest`
retains owner/coordinate and tuple/bounds/lifetime checks and adds
finite local-native admission, warmup and synchronized pool admission/
GC-expunge pressure:
- 68 cold eligibility configurations;
- 896 warmups;
- 3,584 last-use read/copy cases;
- 56 reader/pressure rendezvous;
- eight normal, interpreted, C1/C2-requested, CompactStrings on/off
  runtime configurations.

Stress is evidence of operational regressions, *not* deterministic proof
of a baseline use-after-free. JIT compilation tiers and actual GC
reclamation schedule must be verified separately.

The existing
`.github/workflows/mindex-string-backing.yml`
triggers on `M3String*.java` and `test/jdk/java/lang/String/M3*.java`
and **already includes the original lifetime jtreg file** in its exact
selected target list. This branch deliberately leaves the workflow
unchanged, preserving its full matching-JDK, source-invariant and String
tests. Its historical predecessor source-custody/build failures are
separate blockers and must not be represented as an Atom behavior failure.

**DRAFT / NOT MERGE-QUALIFIED:** the canonical Maven/OpenRewrite
forward+inverse execution and actual matched M3JDK21 HotSpot image/jtreg
are pending; memory pressure, all-tier JIT, JNI, GC/CDS and
multi-platform gates remain mandatory. No default branch updated.
