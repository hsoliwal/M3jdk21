# MIndex-backed String — JDK integration contract

## Objective

Allow a modified JDK to expose ordinary `java.lang.String` semantics while the immutable character
and byte payload is owned by the same MIndex canonical storage used by explicit M3 values and
compiler lowering.

JNI is optional. The first supported JDK backing is the OS-mapped MIndex store.

## Current stage

This branch introduces the JDK-internal backing kernel:

- `jdk.internal.mindex.MIndexStringBacking`
- `jdk.internal.mindex.MIndexMappedStringBacking`

The mapped implementation reads the same MIndex text image version 3 and canonical byte image
version 1 used by Synexia.

It supports:

- stable language/row MIndex IDs;
- UTF-16 length and indexed character access;
- precomputed Java String hash;
- code-point and malformed-surrogate facts;
- read-only direct UTF-16 views;
- read-only direct canonical UTF-8 views;
- cross-language payload aliases;
- UTF-16 range views with String-compatible split-surrogate semantics.

No JNI library is loaded.

## Identity rule

A String backing identity is a logical MIndex coordinate plus its store namespace.

A process virtual address is never identity.

This allows the same persisted payload to be mapped at different virtual addresses in different
JVMs while retaining the same canonical ID.

## Why java.lang.String is not changed in this first patch

OpenJDK 21 String is VM-special:

- `String.value` is a VM-trusted `byte[]`;
- `String.coder` participates in compact-string intrinsics;
- HotSpot `java_lang_String` directly reads and writes the value array;
- CDS, string tables, GC string deduplication, JNI conversion and compiler intrinsics assume the
  existing layout.

Arbitrary mmap/native storage cannot legally masquerade as a JVM `byte[]`.

Therefore the safe migration order is:

1. prove a JDK-internal MIndex backing can read the canonical store exactly;
2. pin String semantic parity with jtreg;
3. introduce an explicit VM/JDK backing discriminator and stable MIndex coordinates;
4. teach Java String hot operations to dispatch through the backing abstraction;
5. teach HotSpot String helpers, StringTable/CDS/intrinsics and JNI conversion about that backing;
6. permit MIndex-backed String instances to omit a duplicate character payload;
7. retain materialization only where a genuine JVM array is contractually required.

## String contract

The MIndex route must preserve:

- UTF-16 code-unit indexing;
- `substring` ranges may split a surrogate pair;
- unpaired surrogates remain exact UTF-16 code units;
- Java String hashCode;
- comparison/equality by logical UTF-16 content;
- code-point operations;
- JDK UTF-8 replacement behavior;
- fresh mutable arrays from public copy APIs.

MIndex hashes and signals may reject candidates but cannot replace exact content verification unless
canonical identity itself proves the same immutable payload.

## Store compatibility

Text image:
- magic `MIXM`;
- version 3;
- 64-byte header;
- redundant 24-byte commit slots;
- `MIXR` records;
- UTF-16 payload stored big-endian;
- semantic rows may alias an earlier payload-owner row.

Byte image:
- sibling path `<text-path>.bytes`;
- magic `MIBM`;
- version 1;
- 64-byte header;
- redundant commit slots;
- `MIBR` records.

Both are append-only after publication. The JDK reader opens a committed read-only snapshot and
ignores bytes beyond the selected valid commit.

## Next String/HotSpot patch

The next patch should add a JDK-private backing descriptor without changing public String APIs.

It must inventory and update, at minimum:

- `java.lang.String`;
- `StringLatin1`;
- `StringUTF16`;
- `StringConcatHelper`;
- `StringConcatFactory`;
- HotSpot `java_lang_String`;
- StringTable and string deduplication;
- CDS archived strings;
- JNI String creation/access;
- JVMCI/intrinsic layout assumptions;
- serialization behavior.

The transition must be differential and pass-by-pass. Array-backed String remains the oracle until
each MIndex-backed path has parity evidence.
