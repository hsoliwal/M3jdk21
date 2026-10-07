# M3 immutable array substrate

Status: first bounded array receiving slice for M3JDK21.

This separately built Apache-2.0 module adapts qualified Synexia MIndex/MatIndex
array mechanics into canonical M3 naming. It does **not** alter Java array
language, verifier, reflection, JNI, identity, covariance or mutable element
semantics. Ordinary `byte[]`, `char[]`, `int[]`, `long[]` and `T[]`
remain authoritative public/JVM values.

## First slice

- `M3ByteArrayView`: immutable byte snapshot/slice/join with read-only
  scatter/gather descriptors.
- `M3Utf16ArrayView`: immutable UTF-16 snapshot/slice/join with exact
  JDK-shaped coordinates, search, comparison, code-point traversal and explicit
  `char[]` materialization.
- `M3Utf16Facts`: composable Java String hash, power, code-point count and
  ASCII/LATIN1 facts, including a surrogate pair split across a segment seam.
- `M3IntArrayView` / `M3LongArrayView`: immutable snapshot/slice/join value
  planes for future collection backends.
- `M3ArrayNative`: optional JNI UTF-16 compare/concatenate boundary. JNI keeps
  no Java heap pointer after a call and Java remains the exact fallback.
- strict C11 native checks plus Java/native differential tests.

Snapshot admission copies mutable Java arrays once. Slices and joins retain
immutable owners/descriptors without copying payload. `copy()`,
`copyTo(...)` and JNI concatenation are explicit ordinary mutable-array
materialization boundaries.

## Why this is not a Java-array replacement

M3String demonstrated that target-internal ownership can change while the public
JDK contract remains stable. Arrays need an even stricter boundary because Java
arrays are inherently mutable, covariant reference values with VM-visible
layout and identity. This module therefore provides immutable internal values
only. A future JDK owner may use these behind a proven contract, but this module
must never make two ordinary arrays share mutable payload or change array store,
clone, reflection, serialization, Unsafe/VarHandle, JNI or GC semantics.

## Relationship to collections

The existing `m3/collections` module owns primitive lanes, stable slots,
concurrent lanes/maps/sets and optional JNI. The array substrate is the next
lower immutable value layer. Collection owners may adopt it selectively only
after differential contract and allocation/performance proof; no collection is
rewritten merely because an M3 array view exists.

## Build

Java proof:

```sh
mvn -f m3/arrays/pom.xml test
```

Linux native/JNI proof:

```sh
mvn -f m3/arrays/pom.xml -Pnative-linux test
```

The native profile requires CMake, a C11 compiler and JNI headers. Absence of
native acceleration is not a correctness failure for ordinary module use; the
native proof itself must execute before claiming JNI parity.

## Port order

1. qualify immutable byte/UTF-16/int/long snapshots, slices and joins;
2. receive mapped/shared immutable array owners with namespace/generation and
   lifetime proof;
3. connect selected M3 collection backends to the qualified value layer;
4. test individual `java.util` implementation backends without renaming public
   classes;
5. only then consider java.base/internal VM integration where a concrete JDK
   consumer justifies it.

No full JDK array replacement, zero-copy-at-all-boundaries, universal O(1)
operation, or performance win is claimed by this first slice.
