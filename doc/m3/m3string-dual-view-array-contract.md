<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3JDK21 String: one text owner, ordinary String object, two projections

Status: behavioral contract for the draft [M3JDK21 receiver PR #1536](https://github.com/hsoliwal/M3jdk21/pull/1536). Not full installed-image qualification.

**M3JDK21 is one integrated Java/HotSpot/JNI SDK, not an adapter on top of the JDK.** Public `java.lang.String` retains its standard Java 21 fields, API, identity, hash, coder, construction, serialization and array-return semantics. `java.lang.String` also has the internal `M3String m3` canonical owner/range reference.

The actual content of an admitted M3-backed String exists only in canonical interned payloads: fixed-size short-string `long[]` pages, native atoms, and read-only OS-mapped lexicon/shared-array atoms. Concatenation and slicing compose owners, indexes, offsets and lengths without creating a joined character payload. Native JNI/HotSpot readers traverse these owners and their stable lifetime guards.

For nonempty M3-backed Strings, `String.value` is the shared, zero-length `byte[]` compatibility sentinel. This preserves the actual JDK `byte[]` field and prevents one duplicate character payload per String. `String.coder` and `m3` represent the real text. String operations must read `m3` when it is present. Bootstrap/non-M3 Strings continue using their ordinary contiguous `value` bytes.

## Byte and char array *views* are not arbitrary Java primitive arrays

The logical byte view and logical UTF-16 char view are backed by the **same** M3 canonical owner/range graph. JNI can consume their regions directly into caller-supplied buffers or produce a native traversal over the graph without storing a second full spelling.

A Java 21 `String` has no `char[]` backing field. `String.toCharArray()` is specified to return a new, mutable `char[]`; an internal `String.value()` call or JNI `GetString*Chars` requiring a contiguous array/pointer is a **materialization boundary**. These results cannot transparently alias immutable read-only OS mappings or multiple discontiguous arrays while honoring Java array length, identity, writes, reflection, `Unsafe`, VarHandles, `arraycopy`, covariance and JNI acquire/release rules. Fresh result arrays may consume space; they are never stored in `M3String` or permanently cached as String text.

An optional future HotSpot virtual-array implementation would need explicit and complete proofs for Java array representation, GC/JMM/barriers, JNI and public contracts. This draft does **not** claim that ordinary `byte[]` or `char[]` are zero-heap aliases of an OS mapping.

## Required regression contracts

* A wrapped `String(M3String)` has the standard `byte[] value` field, `coder`, and `m3`, with a VM-wide shared empty `value` sentinel.
* Two String wrappers of one M3 descriptor do not duplicate text; their `m3` references and sentinel array are the same, while the wrappers remain distinct objects.
* `length`, `equals`, `hashCode`, `charAt`, `substring`, `codePoints` and UTF-8 encoding match ordinary JDK String behavior across OS-mapped atom boundaries, embedded NUL and surrogate pairs.
* Two `toCharArray()` calls return independent, mutable snapshots; explicit compact-byte projections return independent arrays; mutating either snapshot cannot mutate interned text.
* Projecting an array cannot replace or cache a full backing array inside the String, and the M3 owner continues retaining its read-only mapping.
* Native ABI and full source-built JDK21, JNI/JTReg, GC/CDS and comparative CPU/heap/native process memory gates remain pending until executed.

Canonical source-sealed authoring recipes live in Synexia. The prior paired PRs are [Synexia #12541](https://github.com/hsoliwal/com.synexia/pull/12541) and [Synexia #12550](https://github.com/hsoliwal/com.synexia/pull/12550).
