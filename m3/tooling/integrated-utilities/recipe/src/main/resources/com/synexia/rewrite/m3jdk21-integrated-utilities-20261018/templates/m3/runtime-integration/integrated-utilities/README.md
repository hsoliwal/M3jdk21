<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Integrated java.base utility owners

This receiver increment places utility operations directly in M3JDK21's existing
`java.base` packages. `java.io.IOUtils`, `java.util.stream.Streams`, and
`java.lang.reflect.Reflection` are public JDK-owned utility families. There is no
companion SDK, additional runtime module, or Synexia runtime dependency. Existing
public JDK type names and method contracts remain unchanged. The existing
`java.util.Strings.isNullOrBlank` documentation is corrected to describe its
already-implemented all-whitespace behavior.

## Public operations and ownership

### java.io.IOUtils — 16 operations

| API | Contract |
| --- | --- |
| `copy(InputStream, OutputStream)` | Delegate to the input's `transferTo`, including specialized implementations. |
| `copy(Reader, Writer)` | Delegate to the reader's `transferTo`. |
| `copy(InputStream, OutputStream, long)` | Copy at most the limit; never read a byte beyond it. |
| `copy(Reader, Writer, long)` | Copy at most the limit measured in UTF-16 code units. |
| `copyRange(InputStream, OutputStream, long, long)` | Skip an exact offset from the current position and copy a bounded range. |
| `copyRange(Reader, Writer, long, long)` | The corresponding UTF-16 range operation. |
| `readFully(InputStream, byte[])` | Fill the array or throw EOFException after the available prefix. |
| `readFully(InputStream, byte[], int, int)` | Fill only the validated destination range. |
| `readFully(Reader, char[])` | Fill the character array or throw EOFException. |
| `readFully(Reader, char[], int, int)` | Fill only the validated UTF-16 destination range. |
| `skipFully(InputStream, long)` | Use the existing InputStream.skipNBytes owner after nonnegative validation. |
| `skipFully(Reader, long)` | Exact skip with scalar-read fallback for zero progress; reject invalid skip counts. |
| `contentEquals(InputStream, InputStream)` | Compare remaining bytes with bounded buffers and Arrays.mismatch. |
| `contentEquals(Reader, Reader)` | Compare remaining UTF-16 units independently of short-read boundaries. |
| `toString(InputStream, Charset)` | Read and decode through existing input and String owners with explicit charset. |
| `write(CharSequence, OutputStream, Charset)` | Encode through String.getBytes using the explicit charset. |

Resources remain caller-owned. No utility method flushes or closes a resource.
Negative counts fail before I/O; empty bounded copies and empty copy ranges perform
no I/O. A nonempty copy range whose offset extends beyond EOF throws EOFException.
Its copied length may be shorter at EOF. Array ranges are validated without integer
overflow. A failure preserves the original exception and may leave a consumed or
written prefix, as documented. Comparisons can read ahead inside their two fixed
buffers when unequal. Identical non-null input references compare equal without
being consumed.

Bounded copying uses at most 8192 scratch elements; comparison uses two arrays of
8192 elements. Full reads reuse caller-owned destination arrays. Character counts
refer to UTF-16 units, including unpaired surrogates. Conversion explicitly has
String's malformed/unmappable replacement behavior and materializes its input;
it does not pretend to be a bounded streaming conversion.

### java.util.stream.Streams — 6 operations

The existing Streams class becomes public in place. Its original range
spliterators, primitive/reference builders, concatenation helpers, and exception
composition bodies remain unchanged.

| API | Contract |
| --- | --- |
| `stream(Iterable)` | Reuse the iterable's spliterator and its order/size/binding rules. |
| `stream(Iterable, boolean)` | The same source with an explicit initial parallel mode. |
| `stream(Iterator)` | Lazy sequential traversal in iterator order, with unknown size. |
| `stream(Iterator, boolean)` | Reuse Spliterators' existing batching for parallel traversal. |
| `stream(Enumeration)` | Reuse Enumeration.asIterator and the iterator factory. |
| `zip(Stream, Stream, BiFunction)` | Pair corresponding elements until either input ends. |

Zip is initially sequential and its source is unsplittable. It keeps only common
ORDERED/SIZED/SUBSIZED characteristics, with the minimum of exact source sizes.
SORTED, DISTINCT, and NONNULL are not inferred for mapped results. Exact
Long.MAX_VALUE size decreases correctly after traversal. An input iterator may
prefetch one unpaired stream element while checking exhaustion. Stream factories
do not close caller-owned iterables or iterators. Closing zip closes its two input
streams in order through the pre-existing `Streams.composedClose` owner, including
suppressed exceptions. Exhaustion alone does not close a stream.

### java.lang.reflect.Reflection — 12 operations

| API | Contract |
| --- | --- |
| `hierarchy(Class)` | Lazy class-to-superclass chain, retaining Class identity. |
| `interfaces(Class)` | Deterministic, distinct interface closure with the documented discovery order. |
| `rawType(Type)` | Resolve an existing concrete class, parameterized raw class, or resolvable generic array. |
| `componentType(Type)` | Preserve an immediate class or generic component as Type. |
| `typeArguments(Type)` | Preserve ParameterizedType's actual argument order. |
| `upperBounds(Type)` | Preserve TypeVariable/Wildcard upper bounds and implicit Object. |
| `lowerBounds(Type)` | Preserve Wildcard lower bounds. |
| `annotations(AnnotatedElement)` | Preserve getAnnotations presence and inheritance rules. |
| `annotations(AnnotatedElement, Class)` | Preserve repeatable/inherited getAnnotationsByType rules. |
| `parameters(Executable)` | Preserve parameter metadata and order. |
| `parameterTypes(Executable)` | Preserve generic parameter metadata. |
| `exceptionTypes(Executable)` | Preserve generic exception metadata. |

Type variables and wildcards do not become invented exact runtime classes.
Generic-array traversal is iterative, detects identity cycles, and refuses more
than 255 dimensions without unbounded recursion. Class.arrayType remains the
array-class owner. Metadata access does not enable access, invoke members, or
initialize target classes; user-supplied metadata interfaces can execute their own
accessors. Private and closed-module members keep their access state.

Invocation, lookup by class name, access enabling, and public-member discovery
are outside this increment. Wrapping caller-sensitive APIs from a bootstrap
utility can change access checks, so those operations need their own caller-context
proof before addition. No global Class/ClassLoader cache is introduced.

## Recipe ownership, provenance, and gates

The postimages must be produced through the canonical Synexia Maven/OpenRewrite
recipe with exact preimage checks, fixed point, and drift refusal. The receiver
catalogue records target-owned implementation and direct reuse of existing
OpenJDK owners. Apache/Guava/Spring and programming-challenge APIs are contract or
problem-category references only; their implementation text is not copied into
these GPLv2-with-Classpath-exception target sources. Per-file upstream rights and
first-party target copyright notices are retained.

`contract-catalogue.json` contains searchable categories, source hashes, API
ownership, complexity/selection reasons, and explicit pending qualification.
`verify.py` is proof tooling, not the transformation recipe. Run it after recipe
materialization with a Java 21 toolchain:

```sh
python3 m3/runtime-integration/integrated-utilities/verify.py \
  --java-home /path/to/jdk-21 \
  --output /path/to/utility-contract-receipt.json
```

The standalone jtreg-style mains pass under stock Java 21 patched with these
exact four source files: IOUtilsTest 2,196 checks, StreamsTest 181 checks, and
ReflectionTest 74 checks in each of mixed and interpreter modes (4,902 checks
total). Coverage includes zero and short reads, exact bounds/EOF/prefix writes,
UTF-16/charset behavior, close/failure propagation, stream metadata and range
boundaries, generic-array cycles/dimension limits, repeatable annotations, and
private/closed-module access preservation. Existing String behavior is unchanged
by its documentation correction.

This evidence establishes the stated isolated candidate boundary. Full fork-image
builds, the wider java.io/java.util.stream/java.lang.reflect jtreg suites, other
VM compilation modes and platforms, resource/throughput benchmarks, and the
repository's ordered receiving gates remain pending. No performance improvement,
whole-family completion, or full-JDK acceptance is claimed by these finite tests.
