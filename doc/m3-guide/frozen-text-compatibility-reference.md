<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Frozen-text compatibility fixture contracts

[Guide](README.md) · [Four-source and member map](frozen-text-compatibility-source-coverage.md) · [Package navigation](m3-packages.md#package-4)

This reference covers the complete four-file physical package
`m3/compatibility/synexia-reference/src/com/synexia/indexstring` at public target
revision `c842fab8cf5b799257e3c9ea07f8baddde80db09`: two pinned storage snapshots,
one throwing compile-only seam and one executable test. It documents every
explicit public callable and the supporting private/package-private members.
The [source map](frozen-text-compatibility-source-coverage.md) records hashes,
declaration routes, test evidence and the exact scope of the review.

**This is test-only reference code.** It is separate from the retained-view
[indexed-string bridge](indexed-string-reference.md), internal
[mapped String storage](mapped-string-storage-reference.md), and canonical
[bootstrap String owners](bootstrap-string-ownership.md). A shared Java package
name does not make those physical roots interchangeable. No fixture Maven
artifact, JPMS descriptor, native backend or public JDK API is introduced here.
All repository compilation, tests, compatibility scripts, workflows, examples,
native execution, runtime gates and benchmarks were **NOT_RUN** for this
documentation pass. Reading assertions does not establish a passing result.

## Placement, provenance and authority

The public [provenance record][provenance] identifies `FrozenBytes.java` and
`FrozenChars.java` as byte-for-byte snapshots from donor revision
`64a2ea61c73b548413fed6686a9daeeb0b9b0564`. Their Apache-2.0 headers, the exact
[license][license] and applicable [attribution][notice] remain in place. The
public provenance is the evidence used here; this review does not inspect or
revalidate private donor content. The throwing `MIndexAddressedView` seam is
identified as first-party fixture support, not a copy of a working addressed
implementation. The package-local test is not a third frozen-storage owner.

The [target porting invariant][policy], [name mapping][mapping],
[full-family pin][family-pin], [catalogue][catalogue] and
[current receiver plan][receiver] remain authoritative. Reusable Synexia
mechanics and target Java/HotSpot/JNI acceptance have distinct owners. Receiving
order remains String → arrays → collections → AST/compiler → remaining
families; this source-documentation increment advances no phase or receipt.
It changes no source, provenance, license, policy or generated custody inventory.

### Identical names, different physical sources

The fixture [FrozenBytes][bytes] declares `addressed(Kind, ByteOrder)`;
the [retained-port FrozenBytes][port-bytes] at the same target revision omits it.
The fixture [FrozenChars][chars] declares package-private
`copyOf(char[], int, int)`; the [retained-port FrozenChars][port-chars] omits that
overload. Their different blobs are recorded in the source map. Neither the
retained-port guide's seven-owner scope nor the mapped-runtime guide's coverage
is counted as fixture coverage. Keep these definitions isolated rather than
putting duplicate qualified names on the same loader path. The fixture does not
contain joined views, an interner, a file-opening API or a canonical String pool.

## Ownership, ranges and lifetime

`FrozenBytes` stores either a `byte[]` or a read-only direct `ByteBuffer`, plus
an offset, logical length, retained-byte count and cached hash. `FrozenChars`
stores either a `char[]` or a `FrozenBytes` owner, plus a UTF-16-unit offset,
logical length and cached hash. Their private constructors assign these fields;
admission and validation belong to their factory/helper paths.

Public `copyOf` methods clone caller arrays. The input must be stable while it
is being copied; these methods provide no transaction over concurrent mutation.
Package-private `owned` factories retain the actual array. Their ownership
transfer requires that no writable alias be used afterward. Likewise, a
read-only direct descriptor restricts writes through that descriptor; it does
not revoke a separate writable buffer or external writer to the same memory.
The direct admission path relies on already validated, published immutable
storage. Final fields and read-only views do not enforce that external invariant.

Ranges are half-open `[start, end)`, with empty ranges permitted. Byte operations
count bytes; character operations count UTF-16 code units, including isolated
surrogates. Subviews share backing and retain its lifetime. A full-range slice
returns the same object; a different range creates a descriptor and rescans the
selected payload to compute its own hash. Thus slicing is not generally O(1),
despite avoiding a payload copy. Even a short or empty subview may retain a large
backing. No close/unmap, reference-count, explicit cleanup or cross-process
lifetime protocol is declared by these classes.

The implementations avoid changing their private buffer cursors during reads;
exports create fresh cursors. Callers may change an exported buffer's position
or limit independently, and may set byte order on an exported `ByteBuffer`.
Read-only payload access is not a guarantee
that sharing one mutable cursor among threads is safe, nor proof of concurrent
publication or external-alias safety. There is no fixture synchronization or
concurrency test. Stable immutable backing and normal safe publication remain
caller obligations.

## FrozenBytes: all twelve public callables

Source: [complete FrozenBytes body][bytes]. Hashing treats each byte as unsigned
and applies 32-bit FNV-1a, starting at `0x811c9dc5` and multiplying by
`0x01000193` after each XOR, with Java `int` wraparound. `hash32()` is a cached
integer, not an override of `Object.hashCode()`; the class declares no value
`equals` or `hashCode`. A supplied direct-storage hash is trusted.

| Public member | Exact behavior and boundary |
| --- | --- |
| `copyOf(byte[])` | Rejects null with `NullPointerException("bytes")`, clones all bytes, then admits the clone through `owned`. Subsequent source-array mutation cannot change the admitted payload. |
| `addressed(Kind, ByteOrder)` | Delegates to this fixture's `MIndexAddressedView.of`. Always throws `UnsupportedOperationException`; neither null checks nor a usable addressed view are supplied by the shim. |
| `length()` | Returns logical byte length. |
| `retainedBytes()` | Returns the backing accounting value retained by this view. Heap admission uses full array length; direct admission accepts a caller-supplied count. Shared views must not be summed as independent backing allocations. This is not measured total process/heap/native memory. |
| `isDirect()` | Tests whether the direct-buffer field is nonnull. This does not prove file mapping, file identity, sealing or native execution. |
| `compact()` | Returns `this` when logical length equals `retainedBytes`; otherwise performs `owned(copy())`, producing independent heap storage and a recomputed hash. A full-size direct owner can therefore remain direct. |
| `hash32()` | Returns the stored hash. It does not inspect or verify payload at access time. |
| `byteAt(int)` | Validates the logical index with `Objects.checkIndex`, adds the view offset and reads the heap array or direct buffer. An invalid index throws `IndexOutOfBoundsException`. |
| `copy()` | Allocates an independent heap `byte[]` for the logical range. Heap uses `Arrays.copyOfRange`; direct storage uses an absolute buffer bulk read. |
| `slice(int, int)` | Uses `Objects.checkFromToIndex`; invalid ranges throw `IndexOutOfBoundsException`. Full range returns `this`; otherwise shares backing, adds the offset, preserves the original retained count and recomputes the selected-range FNV-1a hash. |
| `copyTo(int, byte[], int, int)` | First rejects null target, then checks source range and target range with `Objects.checkFromIndexSize`, then copies. Source/target starts and count are bytes. No destination write occurs before those checks. |
| `asReadOnlyBuffer()` | Returns a fresh read-only descriptor of exactly the logical range with position zero. It shares payload; direct backing stays direct and heap backing stays heap. No encoding or byte-order conversion is performed. |

### Internal admission and hashing

- `owned(byte[])` explicitly rejects null, retains the array without cloning,
  sets offset zero, uses array length for both logical and retained lengths, and
  computes its hash. Keeping a writable alias violates the intended contract.
- `mapped(ByteBuffer, int retainedBytes, int verifiedHash)` checks directness,
  read-only status and `retainedBytes >= bytes.remaining()`. Failure throws
  `IllegalArgumentException`; null fails on buffer access. It takes a fresh
  read-only slice of the caller's current remaining region and retains that
  descriptor. Changing the caller's cursor later does not change this region.
- `mapped` does not map a file, require `MappedByteBuffer`, authenticate an
  immutable publisher, verify `verifiedHash`, measure actual backing size or
  remove writable aliases. The provided hash can be inconsistent with payload.
  Full-range slicing/compaction preserves that hash when it returns `this`;
  a new non-full slice or copying compaction recomputes from observed bytes.
- `hash(byte[])` scans all array elements. `hash(ByteBuffer)` scans the current
  remaining region using absolute reads without advancing the supplied position.
  These helpers do not validate content or impose a text encoding, and null
  arguments fail through ordinary access.

The `compact()` implementation is narrower than its broad detachment comment:
equal logical and retained sizes return the existing object regardless of
backing kind. If an independent heap value is required unconditionally,
`copy()` provides an independent array and `FrozenBytes.copyOf(value.copy())`
constructs a heap owner, at the cost of another defensive copy. This is a
source-derived expression, not an executed example or an allocation benchmark.

## FrozenChars: all eleven public callables

Source: [complete FrozenChars body][chars]. Heap input is a sequence of Java
`char` units. Direct storage interprets every two bytes as one **UTF-16BE** unit:
high byte first, independently of host byte order. Odd byte counts are refused
at direct admission. Surrogate pairing is not required; no charset decoder,
replacement of malformed sequences, Unicode normalization or code-point indexing
occurs. The hash recurrence is `hash = 31 * hash + unit` from zero, with Java
`int` wraparound, matching String's UTF-16-unit content hash when payload/hash
admission is valid. It is not a value-equality implementation.

| Public member | Exact behavior and boundary |
| --- | --- |
| `copyOf(char[])` | Rejects null with `NullPointerException("chars")`, clones the full array and admits the clone through `owned`. |
| `hash32()` | Returns the cached UTF-16-unit hash; direct admission's supplied value is not verified here. |
| `isDirect()` | Tests whether a `FrozenBytes` owner is retained rather than a heap character array. |
| `length()` | Returns UTF-16-unit length, not byte length or Unicode code-point count. |
| `charAt(int)` | Checks the unit index with `Objects.checkIndex`; reads one heap char or combines two unsigned owner bytes in big-endian order. Invalid index throws `IndexOutOfBoundsException`. |
| `subSequence(int, int)` | Validates a half-open unit range; full range returns `this`. Otherwise retains the same heap array or byte owner, adjusts unit offset/length and rescans selected units for the hash. No surrogate-boundary repair occurs. |
| `copy()` | Returns a fresh independent `char[]`: heap uses `Arrays.copyOfRange`; direct storage creates a char-buffer view and reads all logical units into the new array. |
| `copyTo(int, char[], int, int)` | Rejects null target, validates source and target unit ranges before writes, then uses array copy or an absolute char-buffer bulk read. Zero count is accepted at valid boundary positions. |
| `asReadOnlyBuffer()` | Creates a fresh read-only `CharBuffer` for the logical range, position zero. Heap shares the array; direct storage calls `directUtf16Bytes()`, explicitly sets `BIG_ENDIAN`, then creates the char view. |
| `directUtf16Bytes()` | Requires a direct byte owner; heap-backed values throw `IllegalStateException("heap chars need explicit encoding/copy")`. Returns a fresh direct read-only byte descriptor, position zero, covering `2 * length()` bytes at the logical unit offset. It allocates descriptors, not a copied payload. Consumers must preserve UTF-16BE interpretation. |
| `toString()` | Materializes a String from the logical heap range or from `copy()` for direct storage. The direct branch first allocates a character array; neither branch is a retained String view over this fixture owner. |

### Internal ranges and direct admission

- Package-private `copyOf(char[], int offset, int count)` is a separate overload.
  It first rejects null, validates an offset/count range, allocates exactly
  `count` chars and copies that range before `owned` admission. It is not a
  public convenience API and is not exercised by the two inspected tests.
- `owned(char[])` scans and retains the supplied array with offset zero and
  its full length. It does not clone or explicitly guard null; null fails during
  the scan. Safe ownership transfer is a precondition.
- `mapped(FrozenBytes, int verifiedHash)` requires a direct owner and even byte
  length, otherwise throwing `IllegalArgumentException`. Null fails on owner
  access. It retains the supplied owner, uses half the byte length as unit
  length and trusts the supplied hash. It does not establish file provenance,
  verify the hash or perform a UTF validation/decoding pass.
- `hashUtf16(ByteBuffer)` rejects odd remaining byte counts with
  `IllegalArgumentException`, then combines each adjacent big-endian byte pair
  and applies the 31-times hash. Absolute reads preserve the original cursor;
  the buffer need not be direct. Its current remaining region must be stable.

The class implements `CharSequence` but declares no separate `chars`,
`codePoints` or other inherited-default implementation. Its explicit declarations
are the eleven rows above, not a new independent stream API. It declares no
`equals`, `hashCode`, `retainedBytes`, `compact` or `close` method. An independent
heap `FrozenChars` can be constructed through `copyOf(value.copy())`, with an
additional defensive clone; this is not a zero-allocation detachment operation.

## Compile-only addressed seam

The [complete shim][shim] has a private constructor and a public nested
`Kind` enum containing exactly `BYTE`, `CHAR`, `SHORT`, `INT` and `LONG`.
Its only explicitly declared public callable is
`of(FrozenBytes owner, Kind kind, ByteOrder order)`, which immediately throws
`UnsupportedOperationException("addressed-view API is outside this fixture")`.
It validates none of the arguments, including nulls. The enum's compiler-generated
methods are not additional explicitly declared operations in the count.

Consequently, `FrozenBytes.addressed` cannot construct a usable view here.
The comment on that snapshot method does not establish working indexing,
alignment, byte-order selection, primitive lanes, kernels or native address
export in this fixture. Neither inspected test calls either addressed entry.
The [provenance][provenance] explicitly excludes addressed operations, native
kernels, corpus data and production APIs. Substituting an implementation from
another source root would change this test boundary and is outside this guide.

## Exact build and classpath route

The [compatibility script][harness] is the concrete caller/build authority:

1. It reads `M3_JDK`, identifies the fixture root and uses the fixed donor
   revision as the reference-output subdirectory name. The environment variable
   is mandatory; this script does not select or download a toolchain.
2. It recomputes Git blob SHA-1 identities for the two snapshot sources and
   requires their Apache-2.0 SPDX prefix. It also checks the exact license blob.
   A mismatch raises `ValueError` before compilation. Although the provenance
   describes pin checking broadly, this implementation does **not** hash-pin
   the shim or mapped test, or verify the NOTICE file's contents.
3. It creates
   `m3/build/reference-test-classes/64a2ea61c73b548413fed6686a9daeeb0b9b0564`
   and invokes `M3_JDK/bin/javac --release 21` on the two snapshots, shim,
   package-local mapped test and unnamed-package `SynexiaContractTest`.
4. Compilation and both Java invocations use
   `--module-path m3/build/com.m3.text.jar --add-modules com.m3.text` for the
   independently built product module. The Java invocations use the separate
   reference-output directory as `-cp`, and `-ea`, then run
   `SynexiaContractTest` followed by
   `com.synexia.indexstring.SynexiaMappedStorageTest`.

The script does not build `com.m3.text.jar` itself; it must already exist.
`subprocess.run(..., check=True)` stops the sequence on a failing subprocess,
so later tests are not necessarily reached. It reuses the output directory
without clearing it. Existing classes, an old JAR or a printed PASS string in a
source file are not evidence of this invocation's success.

The [foundation build][build] compiles `m3/core/src` into
`m3/build/classes`, packages `com.m3.text.jar`, and separately compiles product
tests into `m3/build/tests`. It does not select the compatibility fixture source
root for that JAR. The [foundation workflow][workflow] declares foundation build
before the compatibility comparison in its `foundation` job. The later
two-process mapping verification is a different step, not behavior of the
fixture's mapped test. The workflow's pull-request paths select `m3/**`,
`src/java.base/share/classes/java/lang/String.java` and the workflow itself; a documentation-only edit under
`doc/m3-guide` alone is not included in that path filter. No workflow execution
is inferred here.

The public provenance states that the fixture is not shipped in product modules
or runtime images. The inspected script/build route shows its separate output
and classpath; this is source-level placement evidence, not a fresh audit of a
packaged binary or runtime image.

## What the inspected tests assert

### Differential content and ranges

[`SynexiaContractTest.main`][contract-test] creates one `LocalM3Arena`, then loops
over every unit `0..65535` in the three-unit sample `{'a', unit, 'z'}`. For each
sample it constructs fixture `FrozenChars.copyOf(source)` and product
`arena.copyUtf16(source)`, changes the source's middle unit to `!`, and compares:

- the fixture String materialization with the product's `flatten()` result;
- the fixture hash with the flattened product String's hash;
- content for every half-open subrange of the three-unit sample.

This is a differential content/range test with post-construction input mutation.
It does not independently assert the original expected text against each owner,
and its subrange loop compares content, not each subrange's cached hash. Its
comment explicitly disclaims API/owner equivalence. It does not exercise the
package-private range-copy overload, byte-owner contracts, actual file mapping,
JNI, or the addressed seam. Checks throw `AssertionError` explicitly. The
`SYNEXIA_CONTENT_RANGE_PASS` text is the intended output after completion, not
an observed result from this review.

### Direct-memory ownership and materialization

[`SynexiaMappedStorageTest.main`][mapped-test] creates the six-unit sequence
`0000, 0041, D800, DC00, DFFF, FFFF`. It allocates a direct writable buffer,
writes units in `BIG_ENDIAN` order, flips it, publishes a read-only descriptor,
hashes bytes, and calls the two package-private direct factories. Its assertions
then check:

- direct byte/character ownership, read-only byte export, UTF-16 length, content,
  String-compatible character hash and each `charAt` unit;
- content, cached character hash and directness of every character subrange;
- exact char-array copy and independence when the returned copy is mutated;
- a selected source/target-offset `copyTo` range;
- direct/read-only byte export, byte-counted length and every unit via `getChar`;
- one partial byte slice's copied content, compacted length and heap backing.

Its private `check(boolean, String)` increments a static `checks` counter and
throws `AssertionError(message)` on false. The public main ignores its arguments
and does not reset that counter; repeated main invocation in the same class
loader accumulates it. A failure prevents the final
`SYNEXIA_MAPPED_STORAGE_PASS` print. The harness normally supplies a separate
Java invocation, but neither that print nor the expected assertion paths were
executed for this documentation pass.

Despite the test's name and messages, it uses `allocateDirect`, not a mapped
file. The writable buffer variable remains in scope and is not mutated after
publication. The test therefore does not prove writable-alias exclusion,
immutable image admission, actual file mapping/unmapping, JNI execution,
concurrency, or operating-system memory sharing. Its compact assertion is for a
partial byte slice; it does not check full-size direct `compact()` returning
the original direct owner.

## Coverage limits and continuation

The four complete bodies and their declared members are documented, with the
two actual test bodies, build route and provenance kept separate in the
[source/test/hash map](frozen-text-compatibility-source-coverage.md). This is
bounded source-contract coverage. The historical package-inventory sample is
not relabeled as a runtime pass or a review of every same-named package.

Remaining execution/error coverage includes null and invalid ranges, direct
admission refusals, supplied-hash mismatch, caller cursor changes, writable
aliases, heap/direct parity across all byte methods, full-range identity and
compaction, ranged character copying, empty/large retained backing, allocation
failure and concurrent access. No addressed operation is covered. No resource
measurement establishes a memory/performance improvement. Package Javadoc is
absent at this physical root, and broader consumer/custody, API compatibility,
canonical String integration and target qualification are separate work.

[bytes]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/synexia-reference/src/com/synexia/indexstring/FrozenBytes.java
[chars]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/synexia-reference/src/com/synexia/indexstring/FrozenChars.java
[shim]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/synexia-reference/src/com/synexia/indexstring/MIndexAddressedView.java
[mapped-test]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/synexia-reference/src/com/synexia/indexstring/SynexiaMappedStorageTest.java
[contract-test]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/SynexiaContractTest.java
[harness]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/check-synexia.py
[build]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/build.sh
[workflow]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/.github/workflows/m3-foundation.yml
[provenance]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/synexia-reference/PROVENANCE.md
[notice]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/synexia-reference/NOTICE.md
[license]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/synexia-reference/LICENSE-2.0.txt
[port-bytes]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/indexstring/src/main/java/com/synexia/indexstring/FrozenBytes.java
[port-chars]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/ports/indexstring/src/main/java/com/synexia/indexstring/FrozenChars.java
[policy]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/docs/m3jdk21-porting-invariant.md
[mapping]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/docs/name-mapping.json
[family-pin]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/compatibility/synexia-full-family-pin.tsv
[catalogue]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/docs/synexia-full-family-catalogue.json
[receiver]: https://github.com/hsoliwal/M3jdk21/blob/c842fab8cf5b799257e3c9ea07f8baddde80db09/m3/synexia-import/current-full-borrow/README.md
