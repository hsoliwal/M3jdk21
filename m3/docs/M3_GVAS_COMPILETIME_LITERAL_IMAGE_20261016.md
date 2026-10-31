<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 — compile-time literal image with strict fixed-address receiving

Copyright 2026 Hitesh Soliwal and contributors.

**Invariant:** compilation/preparation owns immutable *content* and *offsets*; runtime JNI
owns admission of the backing inode at the identical requested numerical virtual address in
every participating JVM. No address relocation or heap-copy fallback is permitted. Neither a
hash collision nor a different virtual pointer may count as equality of canonical locations.

## Canonical owners: no second String or compiler implementation

- The existing `m3/ports/compiletime` ASM/`SourceInterpreter` scanner is the **only**
  class-file inventory. `M3GvasLiteralHarvester` calls that scanner and uses the resulting
  `M3InventoryState.ldc` strings; it does not rewrite bytecode or run application code.
- `M3GvasLiteralImageWriter` encodes sorted, unique compile-time literal values once in a
  deterministic, bounded little-endian image. A row carries exact byte offset, UTF-16 length,
  Java hash, modular `31^length`, code point count, ASCII/Latin1/surrogate flags, first and
  last code unit. The string payload is encoded once, **not** copied into each record.
- `M3GvasLiteralImage` is a read-only `com.m3.text` proof reader in M3JDK21. It validates
  the whole image before admitting a row, then `addressOf(String)` computes
  `strictBase + payloadByteOffset`, returning zero on miss. Search uses binary search over the
  UTF-16 byte owner; no map-entry or per-string payload objects.
- The existing `M3FixedAddressMapping` owner supplies native/JNI exact-base admission,
  read-only shared backing and SHA-256. No new native symbol is introduced by this change.
  This PR is stacked on M3JDK21 #966 and Synexia #11849.
- The **canonical Maven/OpenRewrite recipe and donor catalogue live in Synexia**. M3JDK21
  includes its generated target classes/tests/receiving proof only, never a Synexia runtime dep.

## Byte layout v1

Header: ASCII `M3GVAS01` at 0, version=1 at 8, header size=64 at 12, count at 16,
row size=32 at 20, row offset=64 at 24, aligned UTF-16LE payload offset at 28 and
full file bytes at 32. All other header bytes are zero. Rows store eight little-endian
int32 values: `payloadOffset, utf16Length, javaHash, pow31, codePoints, flags, first, last`.
Padding is zero; payload starts with a two-byte empty-string NUL sentinel. All other
literal bytes occur **once**, in Java `String.compareTo` UTF-16 order.

Memory/content safety: refusing invalid geometry, order, duplicate strings, out-of-range
rows, overlapping payloads, bad hashes, code-point counts or flags happens before
publication. Image size is bounded (default 64 MiB, caller may lower it); overflow or
exhausted budget refuses whole emission rather than silently dropping literals. Scanner
failures refuse the build. Publication requires an absent destination and is a no-clobber
staging-directory move. Publisher must keep the file bytes and length immutable while
processes map it. Verification at start does not protect against later external writers.

## Build-time compiler emission

```
# Existing Maven module with canonical classfile scanner, not a new project:
mvn -B -f m3/ports/precompute/pom.xml install -DskipTests
mvn -B -f m3/ports/compiletime/pom.xml test
# Run over selected JARs; list has one absolute JAR filename per line:
java -cp ... com.m3.compiletime.M3GvasLiteralHarvester \
  --out ./prepared-literals --jars ./jars.txt --max-bytes 67108864
```

This produces `literals.m3gvas`, `COMPILETIME_LITERAL_OFFSETS.tsv` (compiler-reference
coordinate evidence, key hashes are candidate indexes only), `INPUT_SET_MANIFEST.tsv` and
`PRECOMPUTE_RECEIPT.tsv` (raw image SHA-256). It also supports `--jdk` to scan `jrt:/`.
The existing `M3ContentKey` is reused for the candidate digest; **content-key equality alone
is never canonical equality**. The complete UTF-16 bytes are the authority.

## Local JNI acceptance

```
M3_JDK=/path/to/jdk-21 python3 m3/verify-gvas-literal-precompute.py
```

The script compiles real Java 21 sources with `javac -h -Xlint:all -Werror`, builds the
existing native mapper as C11 `-Wall -Wextra -Werror`, emits a reproducible image of
2048+ adversarial literals, checks exact Unicode/Java facts, refuses occupied addresses,
wrong digests and internally corrupt images (even when the corrupted file's digest is
supplied), and coordinates **three live JVM processes** (`-Xint`, `-Xbatch`,
`-XX:-CompactStrings`). All three must have a `r--s` mapping at `0x600000000000`, the
same inode and **the same numeric native pointer for `"hello"`**.

## What is deliberately NOT claimed

This is a bounded compile-time preparation and strict native receiving slice, not
java.lang.String replacement, a globally writable shared object graph, guaranteed
startup in arbitrary processes, compiled constant replacement in javac/C2, Windows
verification, or end-to-end HotSpot/JTreg integration. Existing Java String contracts,
Java heap object identities, reflection/classloading, codepoint/UCS-2 edge semantics,
GC and JMM authority stay unchanged. A dynamic miss is delegated to the existing
String/runtime owner when an actual lowering recipe is admitted in a later PR.

**Do not advance the String -> arrays -> collections -> compiler runtime promotion
sequence based on this isolated proof.**
