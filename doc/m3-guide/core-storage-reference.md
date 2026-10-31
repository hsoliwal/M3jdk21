<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Core text storage: member and trust-boundary reference

[Guide](README.md) · [Exact source/member/test map](core-storage-source-coverage.md) · [Seventeen-owner routing map](text-source-coverage.md)

## Scope and historical continuity

This **15 October 2026** continuation reads the complete six storage bodies
(427 source lines) at public M3JDK21 commit
`510dfa27b0679ce33348140afc57b558b8853802`. Their unchanged blob identities
are recorded in the [source map](core-storage-source-coverage.md). It adds a
systematic declared-member, generated/inherited-surface and internal admission
contract to the existing [text/storage overview](m3-packages.md#textstorage-api-contracts).
That earlier overview and its dated scope remain valid historical evidence;
these are six previously explained implementations, not six newly discovered files.
All tests and build bodies cited here were **read, NOT_RUN in this documentation
authoring/review pass**. Hosted CI is separate evidence tied to its exact commit
and workflow scope; this statement does not erase earlier or later CI results.

The [descriptor][MODULE] exports `com.m3.text` as an independent experimental
module. [m3/build.sh][BUILD] requires `M3_JDK` as its tool-directory variable,
compiles with `--release 21`, builds
`com.m3.text.jar`, and defines four FoundationTest launch modes. Those are source
instructions, not executed receipts. The script does not probe or certify the
selected compiler/runtime version; `--release 21` alone does not establish a
stock JDK 21 toolchain. This is neither `java.base` nor an implicit
Maven control-reactor child. The [target policy][POLICY] classifies `m3/core` as
receiver/proof residue while preserving canonical reusable ownership and target
runtime acceptance separately. No source, naming map, policy or phase changes are
made by this reference; no donor repository was read.

Use the existing [catalog contracts](lexicon-catalog-reference.md),
[projection/tool/receipt contracts](text-projection-reference.md),
[indexed-string bridge](indexed-string-reference.md),
[PrefixZ contracts](indexed-algorithm-reference.md), and
[bootstrap String ownership](bootstrap-string-ownership.md) for their distinct
owners. `M3StringPiece` is not `java.lang.M3String`, retained `MIndexString`, a
native String backing or a Lucene/OpenNLP adapter. Their
[Lucene](lucene-string-opportunity-reference.md) and
[OpenNLP](opennlp-string-opportunity-reference.md) opportunity maps remain
separate hypotheses and consumer evidence, not performance or admission proof
for these six bodies.

## M3StringPiece: declared and inherited surface

Source: [M3StringPiece.java, lines 7–20][PIECE]. All range coordinates below are
UTF-16 code units and half-open intervals. The interface is public and sealed;
only public final `LocalM3StringPiece` and package-private final
`JoinedM3StringPiece` are permitted. There are no declared fields or constructors.

| Explicit declaration | Contract |
| --- | --- |
| `M3StringPiece subSequence(int start, int end)` | Covariant redeclaration of the CharSequence operation; the concrete classes check bounds and return new views/directories. No default body is declared here. |
| `default String flatten()` | Allocates `char[length()]`, fills it through `charAt(i)`, then constructs a new String. Each call performs this projection, including empty input; no flat String cache is retained. Allocation failures propagate. It neither transfers ownership of the piece backing nor promises a particular internal String encoding/backing layout. |
| `static M3StringPiece join(M3StringPiece... pieces)` | Delegates to the non-public joined constructor. Zero arguments produce an empty joined directory; a null array or null element is refused. Nested directories are flattened, not character payloads. |

`length`, `charAt` and `toString` are inherited CharSequence obligations, implemented
by the two concrete owners, rather than additional handwritten interface methods.
The inherited [CharSequence defaults][CHARSEQ] are `isEmpty` (`length() == 0`),
`chars` (zero-extended UTF-16 units) and `codePoints` (paired surrogates combined;
other units passed through). Stream traversal delegates to the view's reads and
can combine a surrogate pair across joined leaves; slicing may expose an
unpaired half. These are inherited behavior observations, not newly tested M3
implementations. `CharSequence.compare` is static, not an inherited instance
method. The concrete pieces inherit Object equality/hash semantics; neither
implements content equality or Comparable. Compiler bridge methods, where
needed for covariant returns, are generated artifacts, not extra authored bodies
or a bytecode/API-ABI qualification in this pass.

## LocalM3Arena: allocation and trusted admission

Source: [LocalM3Arena.java, lines 15–49][ARENA]. The public final class has one
public no-argument constructor, two private final fields and four declared methods.

| Declaration | State, effects and refusal order |
| --- | --- |
| `LocalM3Arena()` | Explicit construction initializes `owner = UUID.randomUUID()` and `sequence = new AtomicLong()` (zero). It retains no payload arrays, pool, interner or release state. |
| `owner`, `sequence` | The private UUID namespaces allocations; the atomic long supplies per-arena records. UUID use is not cryptographic provenance or an access credential. |
| private `identity()` | Atomically increments the sequence; refuses `Long.MAX_VALUE` with `IllegalStateException("identity exhausted")` instead of wrapping. Creates `StorageIdentity(owner, 1, record)` with records beginning at one. Generation is fixed at one here. Atomic identity allocation is not a lock over caller input or an allocation transaction. |
| `copyUtf16(char[] input)` | Null check precedes `Math.multiplyExact(input.length, 2)` and byte allocation. Copies low byte then high byte for each char, preserving NUL and unpaired surrogates without normalization/decoding. Calls `owned` only after the copy. Null gives NullPointerException (NPE); byte-count overflow gives ArithmeticException; allocation failures propagate. |
| `copyLatin1(byte[] input)` | Null-checks and clones input before calling `owned` with LATIN1. Bytes are unsigned 0–255 code units; there is no charset detection, UTF-8 decode or replacement policy. |
| package-private `owned(byte[] bytes, Encoding encoding)` | Trusted ownership transfer: constructs a local piece over the same array, offset zero, length `bytes.length >> encoding.shift`, and a fresh identity. It does **not** clone, validate nulls/encoding compatibility, check even UTF-16 byte length, or seal the array against a retained alias. It is not a public arbitrary-buffer admission API. |

Normal copy methods require caller input to remain stable during copying; they do
not atomically snapshot concurrent mutations. The directly inspected third caller
of `owned` is `SharedLexiconImage.copyRecord`, after copying and validating bytes.
The local piece constructor and its slicing caller are the other internal trust
boundary. Maintaining private, stable backing is a same-package invariant.

Argument evaluation in `owned` calls `identity()` before evaluating
`bytes.length >> encoding.shift`. A malformed same-package null handoff can
consume an identity and then throw NPE; there is no rollback. An odd UTF16_LE
array would have its byte count shifted down without a parity check. Exhaustion
is checked only when identity allocation is reached; public copy methods may
already have allocated/copied data before it is refused. No promise of gap-free
published identities or deterministic allocation-failure timing follows.

## LocalM3StringPiece: immutable public view over trusted owned bytes

Source: [LocalM3StringPiece.java, lines 13–50][LOCAL]. Its nested public enum
`Encoding` declares `LATIN1(0)` and `UTF16_LE(1)`, package-private final `shift`,
and a non-public constructor that stores the shift. Byte width is `1 << shift`;
offset and length remain code-unit coordinates. Generated `values()` returns
the declaration-order values in a new array; `valueOf(String)` uses exact enum
names (null gives NPE, unknown name gives IllegalArgumentException (IAE)). Enum name/ordinal/comparison,
identity equality and ordinary enum `toString` are inherited/generated language
surface, not custom encoding or decoder implementations.

| Declaration | Contract |
| --- | --- |
| private final `backing`, `encoding`, `identity`, `codeUnitOffset`, `codeUnitLength` | Retains one byte array, its encoding and allocation identity, and the view's range. Final references do not make an externally aliased byte array intrinsically immutable. No backing array is publicly exposed. |
| package-private constructor `(byte[] owned, Encoding encoding, StorageIdentity identity, int offset, int length)` | Assigns all five fields as supplied. No clone, null check, range check, encoding-width check or provenance check. Trusted callers must establish those invariants; later reads are not a substitute for validating arbitrary constructor arguments. |
| `storageIdentity()` | Returns the retained immutable StorageIdentity reference, shared by slices. It identifies the full allocation, not just this range or its content. |
| `encoding()` | Returns the retained Encoding constant, without decoding or copying. |
| `codeUnitOffset()` | Returns the UTF-16-unit origin within the backing, not a byte offset. |
| `retainedBackingBytes()` | Returns the entire `backing.length`, including bytes outside this range. It excludes object/array headers and all other metadata; an empty or tiny slice can retain the full array. |
| `length()` | Returns `codeUnitLength`. |
| `charAt(int index)` | First checks `0 <= index < codeUnitLength`; otherwise IndexOutOfBoundsException (IOOBE). Computes `(codeUnitOffset + index) << encoding.shift`, unsigned-loads the low byte, and for UTF16_LE adds the high byte. No repair/normalization or second range validation is performed. Arithmetic safety relies on admitted backing/range invariants. |
| `subSequence(int start, int end)` | Checks `0 <= start <= end <= codeUnitLength`; otherwise IOOBE. Always returns a new LocalM3StringPiece sharing backing/encoding/identity, with offset `oldOffset + start` and length `end - start`. Full-range and empty slices are new range objects too. |
| `toString()` | Calls inherited `flatten()`, with its explicit allocation boundary. |

No equals/hashCode override, close, free, arena return, native handle or interning
operation is declared. Piece equality is Object identity; comparing the record
identity alone cannot distinguish different ranges. Dropping the arena does not
invalidate a piece. A local slice keeps its complete heap backing reachable;
the ordinary GC lifetime differs from the image mapping lifetime below.

## JoinedM3StringPiece: non-public flat directory

Source: [JoinedM3StringPiece.java, lines 9–61][JOINED]. The package-private
final class is exposed only through the M3StringPiece interface. Its three private
final fields are `LocalM3StringPiece[] leaves`, cumulative exclusive `int[] ends`,
and total code-unit `int length`.

| Declaration | Contract |
| --- | --- |
| package-private constructor `(M3StringPiece[] input)` | Null-checks input, clones its reference directory, then makes two passes. Pass one null-checks each element and adds nested leaf counts or one for a nonempty local leaf, using `Math.addExact`; empty locals contribute zero. Allocates the leaf/end arrays. Pass two copies nested leaf references or nonempty local references and builds cumulative ends with `Math.addExact`. Count or length overflow is ArithmeticException; allocation failures propagate. No payload byte copy or new StorageIdentity occurs. |
| `length()` | Returns the retained total. An empty join has zero leaves/ends and length zero. |
| `charAt(int index)` | Bounds check comes first, including refusal of index zero on an empty join. Binary-searches the first end strictly greater than the index, then reads the chosen local leaf at a relative offset. Cost depends on leaf count; no recursive rope traversal. |
| `subSequence(int start, int end)` | Bounds check first; allocates a temporary local-range directory sized to existing leaf count, scans all leaves, creates local views only for nonempty intersections, then copies the populated references into a new joined constructor. O(leaf count) directory scan plus allocations; bytes remain shared. Empty slices remain joined directory objects, including full slicing of an empty join. |
| `toString()` | Calls `flatten()`. For a joined piece, each char read uses directory lookup; this is not a promised O(n) contiguous bulk copy or allocation-free export. |

Nested joins flatten directories, including repeated empty joins, without copying
character bytes or recursively retaining nested directory objects. Replacing
entries in the caller's input array after construction does not affect the
result; cloning is not an atomic snapshot against concurrent mutation during
construction. A full subsequence does not return `this`. No joined storageIdentity
accessor, custom equals/hashCode, mutable traversal cursor or close method exists.

## StorageIdentity: record value, not admission authority

Source: [StorageIdentity.java, lines 10–14][IDENTITY]. The public record has
three components: `UUID owner`, `long generation`, `long record`. Its public
compact constructor first requires nonnull owner (NPE), then rejects either
nonpositive numeric component with IAE. It does not check that a UUID belongs to
a real arena, that a record was issued, or that two identities describe identical
text. Callers can construct equal values themselves; identity grants no access.

Compiler-generated private component fields and public `owner()`, `generation()`
and `record()` accessors expose the stored values. Generated equals/hashCode use
all three components; generated toString is a diagnostic record representation,
not a storage/wire format. These are record-generated methods, not omitted
handwritten implementations. Offset/length are not components: slices of one
backing share this value while remaining different piece objects.

## SharedLexiconImage: state and validation

Source: [SharedLexiconImage.java, lines 21–228][IMAGE]. Reuse the earlier
[file/lifetime overview](m3-packages.md#sharedlexiconimage-file-and-lifetime-contract)
for the format narrative. This section makes each declaration and validation
boundary explicit.

### Constants and retained state

| Declaration | Meaning |
| --- | --- |
| private `MAGIC = 0x4d334c4558303031L`, `HEADER = 64` | M3LEX001 magic and fixed byte header length. Header/directory integers use big-endian order; payload units are explicitly assembled little-endian. |
| public `MAX_IMAGE_BYTES = 64 * 1024 * 1024`, `MAX_RECORDS = 65536` | Whole-file limit and record-count limit, not independent maximum payload allowances. Directory/header bytes consume file capacity. |
| private final `mapping`, `version` | Retained read-only MappedByteBuffer and parsed supported version. Read-only describes access through this view, not protection from another writer. |
| private final `offsets`, `lengths` | Heap snapshots of record directory positions/lengths, in UTF-16 units. No array accessor is provided. |
| private final `recordDigests` | Flattened 32-byte SHA-256 snapshot per record, computed while opening; these digests are not separately stored as an on-disk per-record digest directory. |
| private final `payloadOffset`, `utf16Units`, `imageIdentity` | Payload byte origin, payload unit count and lowercase hex of the verified embedded image digest. |

The retained primitive metadata is `40 * count` bytes (two int arrays and one
32-byte-per-record array), excluding headers, fields, String, mapping and VM/OS
costs. Validation, String materialization and copyRecord allocate additional
storage. This is not a total heap/native/process-memory budget.

### Constructor and open sequence

The private constructor accepts the buffer only from `open` in this body; it is
not a public arbitrary-buffer validator. It assigns the buffer and sets big-endian
order, then performs these checks in source order:

1. Require at least 64 bytes and matching magic; otherwise IOException for header.
2. Accept version 1 or 2 only; otherwise IOException for version.
3. Read count, payload origin and unit count. Reject negative or excessive count,
   negative units or units above `MAX_IMAGE_BYTES / 2`, wrong directory origin
   (`64 + (8 or 12) * count`), or size other than `payload + 2 * units`.
4. Read checksum bytes 32–63 and compare against `digestImage`; reject mismatch
   before directory-record checks. Retain its hex text as imageIdentity.
5. Allocate snapshots and inspect each directory entry. Offset and length must be
   nonnegative and their long sum within payload units. Compute the Java hash;
   v2 must match its stored hash. From the second row, require strictly increasing
   UTF-16 record content. Capture each record's SHA-256 into recordDigests.
6. Repeat the whole-image digest comparison after capture; reject observed change.

These checks do not require contiguous, nonoverlapping or increasing physical
record offsets, nor that records exhaust the payload. Ordering is by decoded
UTF-16 content, not directory position or code-point/collation order. Well-formed
UTF-16 is not required. This is corruption detection, not a signed provenance
check or a race-proof OS file seal; caller-managed immutable publication remains
necessary.

`open(Path)` opens a read-only FileChannel, checks `64 <= size <= MAX_IMAGE_BYTES`,
maps that whole size read-only, and calls the constructor. The try-with-resources
channel closes on success or failure. Null path handling is delegated to the
FileChannel API; this body has no separate null/path-normalization check. Missing
files, access and other I/O failures propagate. A successfully created mapping
may outlive a later validation failure until VM reclamation; there is no explicit
unmap even on failure. No native library, executable bootstrap hook, network
lookup or global mapped-image registry is introduced by these six bodies.

### Public methods and publication boundary

| Declaration | Contract |
| --- | --- |
| `warm()` | Calls `mapping.load()` and discards its return. Explicit caller-driven request; no portable pinning, residency duration or latency guarantee, and no enforced bootstrap-phase check. |
| `imageIdentity()` | Returns the retained verified embedded digest string. It is not the path, a signature, authorization, a fresh hash of the current mapping or the SHA-256 of all file bytes including the checksum slot. |
| `version()` | Returns retained version 1 or 2. |
| `size()` | Returns `offsets.length`, the record count. |
| `mappedBytes()` | Returns mapping limit, the mapped byte size; not currently resident bytes. |
| `utf16Units()` | Returns retained payload-unit count as long, without materializing text. It need not equal the sum of record lengths for every accepted external layout. |
| `copyRecord(int index, LocalM3Arena arena)` | Requires nonnull arena **before** index validation through recordBytes. Copies the selected record into a new byte array, hashes that owned array, compares all 32 captured digest bytes using an accumulated difference, and throws IOException on mismatch. Only then calls arena.owned with UTF16_LE. Success has fresh local identity/backing and does not retain mapping; repeated copies are separate allocations. It checks the selected copied record against its captured digest, not the current whole image or other records. |
| `create(Path target, List<String> words)` | Calls words.size before making `List.copyOf`, checks the count again on the snapshot, then requires strictly increasing String order. Null list/element gives NPE; excessive count or duplicate/out-of-order words gives IOException. Sums lengths with Math.addExact, computes the v1 directory and total size, and refuses files above the limit. Allocates/builds a complete heap ByteBuffer and writes the checksum before opening the target with CREATE_NEW/WRITE. Writes until exhausted and force(true), then closes. |

Creation emits **v1 only**; the v2 test writer is separate test support. Empty list
creation yields a 64-byte image with zero records/units. A sorted list may contain
one empty String (first); duplicates, including two empties, are refused. An empty
record copy still receives a new local identity. Creation preserves code units
including malformed surrogates; it does not sort, deduplicate or normalize input.

The source uses exact long addition for unit summation, but the bounded count of
ordinary Strings already bounds that sum below long overflow; this is not a claim
of a reachable overflow test. Allocation and runtime failures propagate. Target
validation/opening follows all list validation and buffer construction, so invalid
input may fail before an existing/null target is considered. There is no atomic
snapshot guarantee against a concurrently modified input List. CREATE_NEW refuses
an existing destination; an I/O failure after new-file creation can leave partial
output. No automatic deletion/rollback, rename publication, parent-directory
creation, cross-process lock or crash-recovery protocol is supplied.

### Package-private lookup and materialization

These methods are same-package services, not additional exported public methods.
All comparisons use unsigned UTF-16 units as Java String ordering does; surrogate
halves are neither rejected nor repaired. No lookup caches or mutable shared
cursors are stored.

| Declaration | Result, error order and cost |
| --- | --- |
| `String recordText(int index)` | recordBytes validates index, then a new char array is decoded and used to construct a new String. Empty record produces an empty String through the same construction. Preserves exact units. Unlike copyRecord, performs **no per-record digest recheck**. |
| `int findRecord(String value)` | Null-check first (even for an empty image), call lowerBound, then compare the candidate if within size. Exact match returns its zero-based row; absent returns -1. Empty image returns -1. No String materialization or record-digest recheck. |
| `int lowerBound(String value)` | Null-check first; binary search returns first row whose content is not less than value, in inclusive range `0..size`. `size` is an insertion point, not a valid record index. Empty image returns zero; empty value returns zero, whether or not an empty record exists. No record-digest recheck. |
| `boolean startsWith(int index, String prefix)` | Null prefix check precedes index check, then obtains a record view. A longer prefix returns false; otherwise compares every prefix unit and returns true if all match. Empty prefix is true for every valid record, but does not bypass invalid-index refusal or make an empty image have a valid row. No record-digest recheck. |

`lowerBound` makes logarithmically many record comparisons; each comparison can
scan a common prefix. `findRecord` adds a candidate comparison. `startsWith`
scans up to the prefix length. Each performed record comparison/prefix check
allocates short-lived buffer views; empty-image lowerBound/findRecord need no
record views. They avoid
copying full record text, not all allocation. A changed file can affect these
reads without a deliberate digest-related IOException. The
[catalog reference](lexicon-catalog-reference.md#sharedlexiconcatalog-startup-first-touch-and-close)
already records this distinction: its immutable-file obligation is essential.
Catalog prefix rejects empty prefixes at its own boundary; that does not change
the image helper's empty-prefix contract.

### Private helper ledger

| Declaration | Contract and trust assumption |
| --- | --- |
| `ByteBuffer recordBytes(int index)` | Checks index, computes `payloadOffset + offsets[index] * 2`, and returns an independent read-only slice of `lengths[index] * 2` bytes. Shares mapped storage with position zero and length-sized limit/capacity; does not copy payload or revalidate digest. Arithmetic relies on opening's range/size bounds. |
| `int javaHash(int index)` | Gets a record view and accumulates `31 * hash + unit` with ordinary wrapping int arithmetic. Decodes little-endian code units manually. Used in opening, including v1 calculation even though only v2 stores a comparison hash. |
| `int compareRecords(int leftIndex, int rightIndex)` | Obtains left view then right view, compares units until difference, otherwise compares byte lengths (equivalent sign for even-sized UTF-16 records). Returns comparison sign, not String.compareTo's exact numeric difference. Used for strict ordering at open. |
| `int compareRecordTo(int index, String value)` | Gets/validates the record view before accessing value.length; no independent null check. Compares unsigned units and then unit lengths using Integer.compare. Publicly reachable lookup callers check value first. |
| static `byte[] digestImage(ByteBuffer bytes)` | SHA-256 over bytes `[0,32)` followed by `[64,limit)`, deliberately excluding embedded checksum `[32,64)`. Uses independent read-only slices, leaving original position unchanged. Includes dimensions/version and the entire directory/payload, including unused payload. |
| static `byte[] digest(ByteBuffer bytes)` | SHA-256 over the supplied buffer's current remaining bytes; consumes its position to limit. Callers pass temporary views/wrappers, so this does not mutate the mapping's shared cursor. |

Both digest helpers turn unavailable SHA-256 into AssertionError rather than
IOException. The helpers do not expose mutable retained arrays or buffer cursors.
The image has no close/unmap method and does not implement AutoCloseable; channel
closure, Java reachability, VM mapping reclamation and OS page-cache sharing are
distinct events. The public local-copy path protects already-published heap
pieces against later file edits. It does not make concurrent mutation/truncation
of mapped files a supported adversarial-input or concurrency contract.

## Callers, finite tests and qualification limits

[SharedLexiconCatalog][CATALOG] is a selected caller: exact lookup calls findRecord;
prefix lookup calls lowerBound/startsWith; text and sidecar validation call
recordText. Its openImage additionally verifies its manifest's whole-file digest,
v2/version/count/units and edge text. These catalog checks do not add a per-read
image digest check. [M3PrefixZ][PREFIXZ] consumes the sealed piece length/charAt
surface without flattening the input; reuse its separate metadata-budget,
cancellation and comparison-cost reference. This is not a repository-wide call graph.

The full [FoundationTest][TEST] body was read. Relevant cases are the seeded
piece corpus, copied input/directory isolation, 256 Latin-1 units, malformed
surrogates, repeated empty joins, selected bounds/null checks, concurrent arena
identity collection, six ordered Unicode image records, v1/v2 copy, selected
corruption/refusal paths and post-copy file-mutation isolation. Exact line-level
attribution and unsupported cases appear in the [test ledger](core-storage-source-coverage.md#finite-test-attribution).
Catalog assertions in the same file remain separate integration evidence.

No repository build, Java compiler/test, policy verifier, native/library load,
benchmark, sanitizer, platform mapping test or Lucene/OpenNLP integration was
executed in this documentation authoring/review pass. Hosted CI remains separate
exact-commit/workflow evidence. No throughput, native-memory saving, universal immutability against
external writers, whole-package completion or runtime-family acceptance follows
from this six-owner source/member closure.

[PIECE]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/core/src/com/m3/text/M3StringPiece.java
[ARENA]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/core/src/com/m3/text/LocalM3Arena.java
[LOCAL]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/core/src/com/m3/text/LocalM3StringPiece.java
[JOINED]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/core/src/com/m3/text/JoinedM3StringPiece.java
[IDENTITY]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/core/src/com/m3/text/StorageIdentity.java
[IMAGE]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/core/src/com/m3/text/SharedLexiconImage.java
[MODULE]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/core/src/module-info.java
[BUILD]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/build.sh
[POLICY]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/docs/m3jdk21-porting-invariant.md
[CHARSEQ]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/src/java.base/share/classes/java/lang/CharSequence.java
[CATALOG]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/core/src/com/m3/text/SharedLexiconCatalog.java
[PREFIXZ]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/algorithms/src/com/m3/algorithm/M3PrefixZ.java
[TEST]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/core/test/FoundationTest.java
