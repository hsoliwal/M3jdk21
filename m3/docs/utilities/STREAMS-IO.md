<!--
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
-->
# Streams and I/O utilities

`java.util.Streams` and `java.io.IOUtils` provide one public entry point for each
family. They use the existing JDK stream, spliterator, array-comparison and I/O
owners. The existing `java.util.Strings` and internal `java.util.stream.Streams`
remain their existing owners; these additions do not create another sorting or
native runtime.

## Streams

All eleven `Streams.stream` overloads return initially sequential streams.

| Source | Result and source authority |
| --- | --- |
| `Collection<T>` | `Stream<T>` from the collection's own spliterator |
| `Iterable<T>` | `Stream<T>` from the iterable's own spliterator |
| `Iterator<? extends T>` | Ordered `Stream<T>` of the remaining elements |
| `Enumeration<? extends T>` | Ordered `Stream<T>` through its iterator adapter |
| `Optional<T>` | The optional's standard empty or single-element `Stream<T>` |
| `OptionalInt`, `OptionalLong`, `OptionalDouble` | The matching primitive optional stream |
| `PrimitiveIterator.OfInt`, `OfLong`, `OfDouble` | Ordered primitive stream without boxed traversal |

The helper requests an owner's spliterator or enumeration adapter at stream
creation. Stream traversal is deferred until pipeline evaluation. Collection and
iterable encounter order, binding time, comparator, characteristics and
interference rules follow the supplied spliterator. Iterator and enumeration
order follows the source's remaining traversal. The JDK exposes three primitive
iterator specializations; no synthetic boolean, byte, short, char or float
iterator hierarchy is introduced.

Every source is non-null and borrowed. Closing the resulting stream does not
close its source; no close handler is installed. The caller manages any backing
resource and must not independently consume the same source while traversing its
stream. Helper construction adds no per-element wrapper.

The authorities are the Java 21
[StreamSupport](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/StreamSupport.html),
[Spliterators](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Spliterators.html)
and [Collection](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collection.html)
APIs.

## I/O

`IOUtils` has fifteen public static methods across seven operation families.
Byte counts are bytes; character counts are UTF-16 `char` units.

| Operation | Behavior |
| --- | --- |
| `copy(InputStream, OutputStream)` and `copy(Reader, Writer)` | Delegate whole-input transfer to the input's `transferTo`; retain its `long` result |
| The two `copy` overloads with a `long limit` | Reject a negative limit, copy at most that many units, stop normally at EOF, and leave later input unread |
| `readFully` for `byte[]` and `char[]`, with whole-buffer and offset/length forms | Fill the requested range or throw `EOFException`; retain any successfully read prefix and preserve elements outside the range |
| `skipFully(InputStream, long)` | Validate a nonnegative count, then use the input's `skipNBytes` implementation |
| `skipFully(Reader, long)` | Require the exact count; a zero-progress skip falls back to one character read, and early EOF throws |
| `contentEquals` for two streams or two readers | Compare the remaining sequences with bounded buffers and `Arrays.mismatch`, independent of bulk-read fragment sizes |
| `readAllBytes(InputStream)` | Delegate to the input's standard implementation |
| `toString(InputStream, Charset)` and `toString(Reader)` | Decode bytes with the required non-null charset, or collect already decoded reader characters |

Bounded copies and exact reads fall back to a single-unit read if a bulk read
returns zero. A zero copy limit performs no I/O. Offset and length validation is
overflow-safe and occurs before reading. Copy limit arithmetic stays within the
remaining nonnegative `long` budget. Character skipping rejects invalid skip
results rather than subtracting them from its remaining budget.

Every supplied source and destination is non-null and borrowed. Helpers do not
close or flush them, suppress exceptions, install cancellation callbacks or start
background work. An I/O failure can leave a copied or read prefix; these methods
do not promise transactional I/O. Blocking, interruption and concurrent access
follow the underlying object. Comparison may read ahead into its private buffers;
identical source references compare equal without reading. Byte decoding follows
the replacement behavior of `String(byte[], Charset)`.

Whole-input operations intentionally use the standard Java 21
[InputStream](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/InputStream.html)
and [Reader](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/io/Reader.html)
authorities. Bounded comparisons use the existing
[Arrays](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html)
range-mismatch operations.

## Source and API-reference custody

These are original target-owned implementations with the per-file notice
`GPL-2.0-only WITH Classpath-exception-2.0` and copyright 2026 Hitesh Soliwal and
contributors. No Guava or Apache Commons implementation was copied, transformed
or included. Their API catalogues were reviewed to identify familiar entry-point
families and to compare contracts:

- [Guava 33.4.2-jre Streams API](https://guava.dev/releases/33.4.2-jre/api/docs/com/google/common/collect/Streams.html)
  lists collection, iterable, iterator and optional adapters. This implementation
  uses the JDK authorities directly and adds the three JDK primitive-iterator
  overloads and enumeration entry point. The
  [official Guava repository](https://github.com/google/guava) declares Apache 2.0.
- [Apache Commons IO IOUtils API](https://commons.apache.org/proper/commons-io/apidocs/org/apache/commons/io/IOUtils.html)
  supplies API-reference context for copying, exact reads, comparison and text
  conversion. This API requires an explicit non-null charset for byte-to-text
  conversion and provides no quiet close or exception-suppression methods. The
  [official project dependencies page](https://commons.apache.org/proper/commons-io/dependencies.html)
  declares Apache 2.0 for Commons IO.

The API-reference evaluation was performed on 2026-10-18. External API reference
licenses are not presented as the license for these newly authored target files.

## Contract verification

The standalone `StreamsIOUtilitiesContract` executes against the actual public
classes patched into Java 21. It checks all overloads, source identity and
encounter metadata, lazy and short-circuit traversal, primitive paths that fail
if boxed `next()` is used, resource borrowing, negative and overflow admission,
short and zero bulk reads, exact prefixes after EOF or exceptions, finite
`Long.MAX_VALUE` copy/skip behavior, asymmetric comparison fragments and explicit
charset decoding. Deterministic randomized cases use independent array and
string oracles.

Initial candidate qualification passed strict Java 21 compilation, 4,072 checks
in default execution and 4,072 under `-Xint`, plus Javadoc with
`-Xdoclint:all -Werror`. Those candidate receipts do not establish product receiver
replay. The utility receiver verifier separately checks the actual applied source
manifest, catalogue, public APIs and behavioral contracts. Neither gate is a full
M3JDK21 image build, jtreg/JCK run or throughput claim.
