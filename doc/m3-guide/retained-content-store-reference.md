<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->

# Retained content objects, mappings and precompute persistence

[Guide](README.md) · [Exact source/member/evidence map](retained-content-store-reference-coverage.md) · [Retained regex](retained-regex-reference.md#6-persisted-request-cache-identity-and-publication) · [Typed precompute](retained-precompute-reference.md)

This reference covers all eight Java source files in the public retained package
`m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs`
at M3JDK21 commit `363c02caa2e5af1df9460b927ecd78f6a9dbcefb`, including nested
and support members. It describes source behavior, not an executed acceptance
result. Compilation, repository tests, examples, scripts, policy verifiers,
recipes, JNI/native loading, benchmarks and runtime qualification were **NOT_RUN**
in this documentation authoring/review pass. Any hosted CI is separate evidence
for its exact commit.

## 1. Ownership and source boundaries

These files provide physical content identity, filesystem objects, read-only
primitive mappings and a deterministic-result pointer index. They do not supply
an application database, transaction manager, general virtual filesystem,
canonical String dictionary, recipe-admission service or JDK storage backend.

| Owner | Responsibility |
| --- | --- |
| `M3ContentId` | Immutable 32-byte identity, SHA-256 calculation and hex transport |
| `M3ObjectKind` | Nine declared reference roles, including output and descriptor roles |
| `M3ObjectRef` | Nonnull ID/kind and nonnegative physical byte count |
| `M3DirectoryContentStore` | Staged create-only object publication, reads/verification, primitive lane writers and a role-based text bundle format |
| `M3MappedObject` | Size/digest-admitted, read-only byte and primitive views |
| `M3PrecomputeKey` | Canonical binary operation-description bytes and their SHA-256 key |
| `M3PrecomputeResult` | Binary positive-reference or explicit negative result descriptor |
| `M3PrecomputeCache` | Key-to-result pointer publication, lookup and calls into a separate bundle/catalog model |

The complete retained `synexia-indexstring` subtree has no Maven `pom.xml`,
`module-info.java` or `package-info.java`. It is **not a complete compilable
module or an admitted JDK runtime dependency**. In particular, this package
references `M3NativeContent`, `M3FsPrecomputeBundle` and `M3FsPrecomputeCatalog`,
whose implementations are absent from that subtree. Their call boundaries are
visible; their algorithms, format, native fallback and lifecycle contracts are
not supplied by these eight files. A matching name elsewhere is not source
closure. [Vendor custody][vendor] and [delivery policy][delivery] keep the
retained source outside normal product source roots. The [reactor][reactor],
[importer POM][import-pom] and [foundation build][build] describe different
source/build responsibilities; none makes this slice a standalone fs module.

Keep the following authorities separate:

- [IndexDB semantics](indexdb-semantic-reference.md) and
  [IndexDB persistence](indexdb-persistence-reference.md) document the separate
  `m3/indexdb` migration/proof surface. The [borrowing contract][borrowing]
  identifies `com.synexia:synexia-m3index-db` as the canonical reusable DB
  coordinate. This filesystem package is not that database
- [Retained regex persistence](retained-regex-reference.md#6-persisted-request-cache-identity-and-publication)
  is a caller of the generic cache and adds its own codec/request checks
- The [typed-precompute package](retained-precompute-reference.md) has separate
  key/Class/schema, planning and callback contracts; its similarly named keys
  are not this filesystem key's serialization format
- The [indexed-string bridge](indexed-string-reference.md), frozen compatibility
  fixture and [canonical runtime owners](runtime-owners.md) retain their own
  namespaces, provenance and admission gates

Applicable Apache-2.0 source and documentation retain `Copyright 2026 Hitesh
Soliwal and contributors` and the preserved [LICENSE][license]/[NOTICE][notice].
OpenJDK and third-party rights remain independently scoped. The public
[reuse policy][reuse], [borrowing contract][borrowing], [porting invariant][policy]
and [current receiving plan][current-plan] separate reuse permission, exact
source custody and target runtime acceptance. This reference advances none of
those gates or the String → arrays → collections → AST/compiler → remaining
families receiving order.

## 2. IDs, references and levels of verification

[`M3ContentId`][content-id] stores exactly 32 bytes. `of` clones its input and
rejects other lengths; `bytes()` returns another clone. `parse` requires exactly
64 hexadecimal characters, accepts upper- or lowercase hex and rejects malformed
text. `hex()`/`toString()` emit lowercase. `digest` hashes the supplied byte array;
it does not snapshot caller memory before hashing, so the caller must keep that
array stable during the call. `compareTo` is unsigned lexicographic byte order,
`equals` delegates byte equality to `MessageDigest.isEqual`, and `hashCode` is
cached from `Arrays.hashCode`. The equality call is not an authenticity guarantee
or a documented constant-time protocol. Absence of the SHA-256 provider becomes
`ExceptionInInitializerError` in the internal digest factory.

[`M3ObjectRef`][object-ref] is a record of `(id, size, kind)`. Construction checks
only nonnull ID/kind and nonnegative size. Generated equality includes all three
components; it does not touch storage. The kind is carried in references and
descriptors, not a separately verified object-file header. Identical bytes may
therefore have the same physical ID while being referenced with different kinds.
`ref(id, kind)` observes the existing file's size and accepts the caller's kind.

The nine [`M3ObjectKind`][object-kind] values, in declaration order, are
`RAW_BYTES`, `MINDEX_FILE_IMAGE`, `PRECOMPUTE_OUTPUT`, `PRECOMPUTE_DESCRIPTOR`,
`FILE_MANIFEST`, `GENERATION_ROOT`, `MINDEX_SOURCE_FACTS_IMAGE`,
`PRECOMPUTE_BUNDLE_DESCRIPTOR` and `PRECOMPUTE_PIPELINE_DESCRIPTOR`.
Their names identify roles, not implementations of manifest/generation/pipeline
services. Result descriptors encode their ordinals, making that order part of
this persisted format.

| Operation | What it establishes | What it does not establish |
| --- | --- | --- |
| ID/ref construction | Syntax and in-memory geometry | Existence, payload hash, kind semantics or provenance |
| `contains(id)` | A non-symlink regular file at the derived object path at that observation | Correct size, content or later existence |
| `ref(id, kind)` | Regular-file check and observed size | Digest or role validation |
| `open(ref)` | Regular-file and current size check, then an input stream | Digest check or an immutable snapshot |
| `readSmall(ref, limit)` | Bounded copied bytes, exact length/EOF and SHA-256 equality | Authenticity or application-format validity |
| `verify(ref)` | Count and SHA-256 comparison for bytes actually streamed | Continuous integrity, signatures or semantic equivalence |
| `openMapped(ref, limit)` | Mapping budget, non-symlink final file, size checks and full mapped-byte digest | Future immutability, whole-process memory cap or codec validity |
| Existing-object publication hit | Size and delegated exact-byte comparison with the staged candidate | A new proof of the absent comparison helper's implementation |
| `contentEquals` | Exact equality of two checked mapped byte spans using `mismatch` | Interpretation of those bytes as the same logical value |

SHA-256 IDs route physical objects and operation descriptions. They do not
establish source-recipe permission, behavior-bearing dependency identity,
correct execution, semantic equivalence, caller authority or cryptographic
origin authentication. The repository's source-intake/recipe receipts are
separate records with separate gates.

## 3. Store layout, writes and reads

[`M3DirectoryContentStore(Path)`][store-construction] converts the root to an
absolute normalized path, derives `objects` and `staging`, rejects direct
symlinks at those three paths before and after `createDirectories`, and retains
the paths. Normalization is lexical; it is not `toRealPath` or a recursive
ancestor-security check. `root()` returns the normalized Path. Construction has
filesystem effects even before any object is put.

For a 64-character content hex ID `hh…`, the layout is:

```text
<root>/objects/<first 2 hex characters>/<remaining 62 characters>
<root>/staging/object-<UUID>.tmp
<root>/precompute-index/<first 2 key hex characters>/<remaining 62>.ref
<root>/precompute-index/<first 2 key hex characters>/<remaining 62>.ref.lock
<root>/staging/precompute-<UUID>.tmp
```

Only the cache constructor creates `precompute-index`. The object path uses
normalized hex derived from an ID, never an arbitrary filename supplied by a
key's type, algorithm or configuration.

### Object ingestion and create-only publication

The [`put(InputStream, kind)` overload][store-put] performs this staging
sequence; the array and Path overloads delegate after their own preliminary steps:

1. Validate nonnull source/kind; create a UUID staging name
2. Open the staging output with `CREATE_NEW` and `WRITE`
3. Read through a 128 KiB buffer, write those buffered bytes, hash the same
   bytes and accumulate the length with `Math.addExact`; zero-length reads
   are retried and EOF ends ingestion
4. Close the output and derive the content ID
5. Create the object's shard directory, reject a direct symlink shard, then
   perform [publication][store-publication]
6. Attempt to delete the staging pathname in `finally`, on success or failure

`put(byte[], kind)` wraps the array without cloning it. The ID describes the
buffered bytes actually written, not a promised single-time snapshot of a
concurrently mutable input. `put(Path, kind)` checks an absolute normalized
input for non-symlink regular-file status, opens it and closes that owned stream
with try-with-resources. It normalizes/checks/opens the source before the delegated
put validates kind, so a path or I/O failure can precede a null-kind failure.
The check and subsequent open are separate operations.
`put(InputStream, kind)` leaves the supplied stream open for the caller; it owns
only its output. It does not bound input length, elapsed time or disk use and
has no cancellation API. A stream returning zero indefinitely can prevent
progress. The encompassing `catch (ArithmeticException)` translates any such
exception from reading, callbacks, writing or publication to an `IOException`
labelled "M3 object size overflow", not only an overflow from its own byte counter.
The final staging deletion can itself throw and mask an earlier failure.

If the destination already exists, `requireExactExisting` requires a regular
non-symlink file, the same size and `M3NativeContent.filesEqual(candidate,
existing)`. A mismatch raises `IOException` rather than accepting a digest
collision/corrupt object. The same check follows a lost `FileAlreadyExistsException`
publication race. The missing `M3NativeContent` body prevents asserting how its
byte comparison, optional native provider or fallback works.

For a new destination, the helper opens the staged file for writing,
`force(true)`s it, closes that channel and calls `Files.createLink(target,
source)`. A hard link cannot replace an already existing target. Unsupported
hard-link operations are translated to `IOException`; other filesystem failures
propagate. There is no copy or non-atomic-move fallback for object publication.
The temp path is kept until the enclosing `finally`, including after a lost
race. A successful object and a failed temp cleanup can coexist; cleanup failure
can itself propagate. No operation marks published files read-only.

This is per-object create-only publication, not a transaction across object,
result descriptor and pointer. There is no parent-directory fsync, crash journal,
startup staging recovery, orphan collection, deletion/eviction API, rollback or
backup implementation here. Reopening a store recreates missing directories; it
does not scan or repair objects. A crash/failure may leave a staging file or an
unreferenced immutable object. File forcing does not by itself prove durable
directory publication across every filesystem/power-loss scenario.

### Read, verify and copied versus mapped access

[`open`][store-read] returns a caller-owned stream after regular-file/size checks;
it must be closed by the caller. It uses an ordinary read open after those checks,
so it does not itself hold a no-follow/path-identity guarantee across an external
replacement race. `readSmall` rejects a negative limit, rejects declared size
above the limit before opening, reads that many bytes, checks there is no extra
byte, closes its stream, checks the observed length and hashes the returned copy.
Corruption/truncation/extra bytes fail with `IOException`.

`verify` owns/closes its stream, hashes chunks, accumulates a long byte count and
returns false for an observed count or digest mismatch. Missing/nonregular files,
a pre-open size mismatch and I/O failures can throw instead. Its `Math.addExact`
is not wrapped like the put path. Neither `contains` nor `verify` repairs data.
`openMapped` delegates admission to `M3MappedObject`; it is not a content copy.

## 4. Primitive lane production and role bundles

The [lane writers][lane-writers] produce `PRECOMPUTE_OUTPUT` objects through the
same `put(InputStream, kind)` machinery. Counts are nonnegative `int`s. The
encoding must match the writer, and the array or callback must be nonnull.

| Writer | Accepted encodings | Value handling |
| --- | --- | --- |
| `putIntLane(int[], encoding)` and `(count, encoding, IntUnaryOperator)` | `INT32_BE`, `INT32_LE` | Preserve all 32 bits via `Integer.toUnsignedLong` |
| `putUtf16Lane(count, encoding, IntUnaryOperator)` | `UTF16_BE`, `UTF16_LE` | Each supplied value must be in 0…65535; surrogate code units are permitted |
| `putShortLane(short[], encoding)` and `(count, encoding, IntUnaryOperator)` | `INT16_BE`, `INT16_LE` | Array elements become unsigned shorts; callback values are masked to low 16 bits, not range-rejected |
| `putLongLane(long[], encoding)` and `(count, encoding, IntToLongFunction)` | `INT64_BE`, `INT64_LE` | Preserve all 64 bits |

`LaneEncoding` also declares `BYTE` with width 1; UTF16/INT16 widths are 2,
INT32 width is 4 and INT64 width is 8. There is no dedicated byte-array lane
writer beyond ordinary `put`. The enum names carry declared byte order; they do
not convert arbitrary payloads during bundle construction.

The private [`PrimitiveLaneInputStream`][primitive-stream] computes each encoded
byte from `valueAt(element)`, using an unsigned shift and byte mask. A callback
is invoked for **each byte**, not once per element; the array overloads also
reread their array element for each byte. Stable inputs/callbacks are therefore
required to preserve a coherent primitive value. No cloned primitive array is
retained. Non-arithmetic callback exceptions propagate through staged put
cleanup; an `ArithmeticException` is translated by the broad put catch described
above, and cleanup I/O failure can mask either. Both
single-byte and bulk reads advance a long byte cursor; bulk reads validate the
target span. Internally, an EOF bulk read returns -1 even for zero requested
length, while a zero-length read before EOF returns 0. This helper is private,
not an exported stream API.

### Lane and bundle geometry

[`PrecomputeLane(role, encoding, elementCount, object)`][lane-bundle] requires
nonnull components, nonnegative count, an object kind of `PRECOMPUTE_OUTPUT`,
`MINDEX_FILE_IMAGE` or `MINDEX_SOURCE_FACTS_IMAGE`, and exact equality of
`Math.multiplyExact(elementCount, encoding.width())` with `object.size()`.
Overflow is `ArithmeticException`; geometry/kind failures are
`IllegalArgumentException`. Construction does not open the object or validate
that its role has the desired semantic contents.

`PrecomputeBundle(sourceRoot, configRoot, lanes)` requires two parseable
64-character SHA-256 strings and a nonempty lane list with unique roles. It
sorts by role ordinal and freezes the list; null lanes fail during sorting or
inspection. Root strings are retained as supplied, including accepted uppercase
hex. They are declared identities, not recomputed source/configuration hashes.
Generated record equality includes the root strings and ordered lanes.

`supports(profile)` tests role-set inclusion; extra lanes are allowed.
`missing(profile)` returns absent roles in enum order; `requireProfile` throws
when that list is nonempty. `require(role)` returns the lane or throws. These
operations do not validate role-specific encodings, cross-lane dimensions,
sortedness, posting offsets, automaton correctness or actual store availability.

### Profiles and retention metadata

[Profiles][profiles] have immutable required-role sets. All profiles in the first
15 rows below use `SOURCE` / `KEEP`; the last three use `QUERY` /
`KEEP_IF_REUSED`. `fileManifestEligible()` returns whether the scope is `SOURCE`.
These are descriptive metadata: these files implement no file-manifest publisher,
reuse counter, retention enforcement, eviction, execution plan or GPU allocation.
The GPU-named profile selects backend-neutral persisted lanes only.

| Profile | Required roles |
| --- | --- |
| `TOKEN_POSTINGS` | `SORTED_KEYS`, `POSTING_OFFSETS`, `POSTING_ROWS`, `TOKEN_ORDINALS` |
| `NGRAM_CANDIDATE` | `NGRAM_KEYS`, `NGRAM_POSTING_OFFSETS`, `NGRAM_POSTING_ROWS`, `NGRAM_LOOKUP_DISPLACEMENTS`, `NGRAM_LOOKUP_SLOTS` |
| `UTF16_SCAN` | `UTF16_CORPUS`, `RECORD_OFFSETS`, `RECORD_LENGTHS` |
| `GPU_GREP_CANDIDATE_SCAN` | All `NGRAM_CANDIDATE` and `UTF16_SCAN` roles |
| `GRAPH_FORWARD` | `CSR_OFFSETS`, `CSR_EDGES` |
| `GRAPH_BIDIRECTIONAL` | `CSR_OFFSETS`, `CSR_EDGES`, `REVERSE_CSR_OFFSETS`, `REVERSE_CSR_EDGES` |
| `TUPLE_FORWARD` | `TUPLE_ROWS` |
| `TUPLE_BIDIRECTIONAL` | `TUPLE_ROWS`, `REVERSE_TUPLE_ROWS` |
| `BITMAP` | `BITMAP_WORDS` |
| `BITMAP_RANK_SELECT` | `BITMAP_WORDS`, `RANK_SELECT_BITS`, `RANK_SELECT_DIRECTORY` |
| `INTERVAL` | `INTERVAL_STARTS`, `INTERVAL_ENDS` |
| `WINDOW_SUM` | `WINDOW_STARTS`, `WINDOW_ENDS`, `PREFIX_SUM_I64` |
| `HASH_BATCH` | `HASH64` |
| `SIMILARITY` | `SIMHASH64`, `MINHASH64` |
| `CHUNKED_SOURCE` | `CHUNK_OFFSETS`, `CHUNK_LENGTHS`, `RECORD_OFFSETS`, `RECORD_LENGTHS` |
| `TRIGRAM_QUERY` | `QUERY_TRIGRAM_IDS`, `QUERY_PROGRAM` |
| `DFA_QUERY` | `AUTOMATON_START_STATE`, `AUTOMATON_TRANSITIONS`, `AUTOMATON_ACCEPTS` |
| `DFA_COMPACT_QUERY` | `AUTOMATON_START_STATE`, `AUTOMATON_ALPHABET_CLASS_COUNT`, `AUTOMATON_ASCII_CLASSES`, `AUTOMATON_TRANSITIONS`, `AUTOMATON_ACCEPTS` |

There are 39 `PrecomputeRole` constants. `TOKEN_IDS` is the additional declared
role not required by any of these profiles. Role availability, hash/fingerprint
lanes and candidate indexes do not authorize an exact-match or semantic-result
shortcut.

### Role-based bundle serialization

[`putPrecomputeBundle`][bundle-io] encodes a UTF-8 text descriptor and puts it as
`PRECOMPUTE_BUNDLE_DESCRIPTOR`. It does not verify referenced lane objects.
The first line has `M3PRECOMPUTE1`, sourceRoot and configRoot separated by tabs;
each following line has role name, encoding name, decimal element count,
lowercase object ID, decimal byte size and object-kind name. Every line ends in
LF. Ordering is the bundle's role-ordinal order.

`readPrecomputeBundle(ref, maxBytes)` requires the descriptor kind, uses
`readSmall` for size/digest-verified copied bytes, parses the text and requires
that re-encoding exactly equals the decoded text. Framing requires a final LF,
at least one lane, exactly three header fields and six fields per lane. It
rejects bad enums/numbers/ID syntax, duplicate roles, geometry and noncanonical
ordering/number spellings. Most parse value failures are wrapped as `IOException`;
`Math.multiplyExact` overflow is `ArithmeticException` and is not caught by the
`IllegalArgumentException` handler. A negative byte limit or wrong descriptor
kind is also a caller-contract failure, not a cache miss. Parsing authenticates
neither root declaration nor the lane payloads. `openPrecomputeLane` requires
the role and then separately admits that lane's full mapping with its byte limit.

This nested role-based `PrecomputeBundle` format is **not interchangeable** with
`M3FsPrecomputeBundle`, which the cache calls through an absent separate owner.
Sharing an object-kind name does not establish compatible bytes.

## 5. Mapped bytes, primitive views and lifetime

[`M3MappedObject.open`][mapping] is package-private and reached through the store.
It rejects null arguments, a negative `int` budget and a reference larger than
that budget. The final path must be a non-symlink regular file; the channel is
opened with `READ` and `NOFOLLOW_LINKS`. Its size must equal the reference size.
The size fits an `int` because of the prior budget. A zero-byte object gets an
empty read-only heap buffer; nonempty objects use `FileChannel.map(READ_ONLY)`.

Admission hashes a read-only duplicate of the entire mapping and checks the
channel size again. It rejects a changed size or mismatching digest. The channel
is closed after admission, but the returned read-only buffer and derived views
keep the mapping alive. **There is no `close`, `AutoCloseable`, deterministic
unmap or explicit release operation on this class.** No native address is exposed.
Failure after mapping can also leave a mapping awaiting normal JVM reclamation.

[Accessors and views][mapped-views] preserve the original reference and offer:

- `reference()` and `size()` for metadata and admitted capacity
- `byteAt(offset)` for an absolute checked byte read
- `bytes(offset, length)` for a checked, independent read-only slice with
  position zero and capacity equal to length
- `chars`, `shorts`, `ints`, `longs` for a specified count at a byte offset,
  using an explicit nonnull `ByteOrder` and widths 2, 2, 4 and 8
- `contentEquals(offset, other, otherOffset, length)` for two checked byte spans,
  compared by `ByteBuffer.mismatch`, rather than by ID equality

`lane` multiplies count by width in long arithmetic, checks the entire span
before narrowing the byte length and sets byte order before making the typed
view. Negative counts/offsets, overrun and invalid `byteAt` indices fail through
bounds checks. Natural alignment is not required. `chars` exposes UTF-16 code
units; it does not validate surrogate pairing, normalize text or assign string
identity. No float/double view or schema/dictionary inference is provided.

Read-only buffers prohibit mutation through those handles; they do not prevent
other processes, paths or writable mappings from changing/truncating the file.
The store and its ancestors must stay under trusted control and the backing
bytes must remain immutable for the entire life of all views. Digest admission
is an observation, not a lease. Independent views have independent cursors;
sharing one buffer's mutable cursor between threads requires caller coordination.
The per-mapping budget limits encoded capacity, not page residency, heap,
all mappings, or whole-process memory. Admission reads/hashes every byte and is
not a constant-time open or zero-I/O shortcut.

## 6. Deterministic keys and persisted result descriptors

### Key construction and binary format

[`M3PrecomputeKey`][key] validates nonempty, NUL-free type/algorithm names,
nonnegative algorithm version, an immutable nonnull list of nonnull content IDs,
and a map with nonempty NUL-free keys and nonnull values. It preserves input-ID
order and duplicates. It sorts configuration keys lexicographically for encoding;
`configuration()` itself returns an immutable map with no iteration-order
promise. Values may be empty or contain NUL. Names are not stripped or normalized.
No constructor limit is imposed on string bytes or input/configuration counts.

Format 1 uses `DataOutputStream` big-endian integers: format, length-prefixed
UTF-8 type, length-prefixed UTF-8 algorithm, algorithm version, input count and
32 bytes per input, configuration count, then each sorted key/value as a
length-prefixed UTF-8 string. `canonicalBytes()` returns a clone; `id()` is
SHA-256 of those bytes. The string/list/map accessors expose immutable values.
The class does not override `equals`/`hashCode`; use the explicit ID or canonical
bytes for the intended encoded-key comparison rather than object equality.

The constructor's `String.getBytes(UTF_8)` uses the charset's replacement behavior
for malformed surrogate sequences ([Java 21 encoding contract][jdk-encoding]).
It is not an exact arbitrary-Java-UTF-16
identity function. Different malformed strings can lose their distinction during
encoding. Callers needing exact code-unit identity must bind an appropriate
representation, as the retained regex caller does with its separate UTF-16 hash.
A correctly computed key identifies the encoded declarations; it does not hash
implementation classes, validate configuration claims or prove the supplied
input IDs describe the operation's actual inputs.

[`decode`][key-decode] requires format 1, a nonnegative version, at most 65,536
inputs and at most 65,536 configuration pairs, and at most 1 MiB of UTF-8 bytes
per string. It requires each full 32-byte ID/string, strictly decodes UTF-8,
rejects duplicate configuration keys and trailing bytes, reconstructs a key and
requires byte-for-byte equality with its canonical re-encoding. Unsorted map
entries therefore fail. Constructor `IllegalArgumentException`s are wrapped as
`IOException`; truncation and strict-decoder failures are I/O errors. Null input
is a caller error. These are per-field/count limits, not a total decode-memory
budget. Because construction does not impose the decoder limits, not every
constructible oversized key can be decoded by this method.

### Positive and explicit negative results

[`M3PrecomputeResult`][result] retains key ID, `State.VALUE` or `State.EMPTY`, and
an optional output reference. Its private invariant is that VALUE has an output
and EMPTY does not. `value(key, output)`/`empty(key)` capture `key.id()` only.
`keyId`, `state` and `isEmpty` are accessors; `output()` throws
`IllegalStateException` for EMPTY. This class does not define value equality.

Result format 1 is: big-endian format int, 32 key-ID bytes, one unsigned state
ordinal, and for VALUE only 32 output-ID bytes, a signed long byte size and an
int object-kind ordinal. Canonical EMPTY is 37 bytes and VALUE is 81 bytes.
`decode` checks format, complete IDs, state/kind ordinal range, nonnegative size
and exact EOF. It checks structure, not referenced-file existence or contents.
A zero-byte output is still VALUE, distinct from EMPTY. No error/exception state,
expiration, completion timestamp, implementation revision or retry policy is
stored here; those require caller behavior and key configuration.

## 7. Cache lookup, publication, bundle delegation and recovery

[`M3PrecomputeCache(store)`][cache-lookup] retains a nonnull store, derives
`precompute-index` and `staging` below its root and creates the index directory.
Neither store nor cache is `AutoCloseable`; they retain paths rather than a
long-lived open file/channel. Both can perform I/O and create files/directories.

### Base lookup and publication

`lookup(key)` derives the pointer path from `key.id()`. If
`Files.isRegularFile(pointer)` is false it returns `Optional.empty()`; this can
also represent a nonregular/inaccessible observation, not just a never-published
key. That check follows symlinks. Otherwise it reads the whole pointer as ASCII,
trims it, parses a descriptor ID, obtains a `PRECOMPUTE_DESCRIPTOR` ref, reads at
most 256 verified descriptor bytes, decodes a result and requires a matching key
ID. There is no explicit byte cap on the pointer file itself. Invalid pointer hex
can throw unchecked `IllegalArgumentException`; corrupt/missing descriptors and
key mismatch fail rather than becoming misses. A present EMPTY is a durable
negative result distinct from an absent pointer. A positive lookup does **not**
open/verify its output object or validate the canonical key's contents.

[`publish(result)`][cache-publish] first stores a canonical result descriptor,
then writes `<descriptor lowercase hex>\n` to a new UUID staging file. It creates
the pointer shard and opens a sibling `.ref.lock` with `CREATE`/`WRITE`, taking a
blocking file lock. Under that lock it either:

- Reads/trims an existing pointer and accepts an identical descriptor hex
- Throws `IOException` when the existing pointer differs, including a
  positive-versus-negative conflict for the same key
- Moves the temp pointer to the previously absent pointer path

The lock is checked for validity and released/closed with its channel; the temp
pointer is deleted in `finally`. Lock files remain. An identical-pointer hit
compares pointer text, not decoded output bytes. Publication accepts a result's
output reference without proving it exists or that it is appropriate for the
computation. It does not persist the key's canonical bytes.

The pointer helper first requests `ATOMIC_MOVE`, falling back to ordinary
`Files.move` only for `AtomicMoveNotSupportedException`. It is different from the
object hard-link helper: the source itself notes that atomic moves may replace
a target, while the pointer path relies on cooperating writers honoring the
same lock and absent-target check. There is no strong create-only guarantee
against a noncooperating external writer. The fallback has no atomicity promise,
and pointer writes/directory metadata are not explicitly forced. No JVM mutex,
timeout, fairness, single-flight computation or retry-on-overlapping-lock logic
is implemented; same-JVM overlapping file-lock attempts can fail instead of
queueing like an ordinary Java monitor ([Java 21 file-lock contract][jdk-lock]).
Lookups do not acquire that lock.

The result descriptor is stored before pointer publication. A conflict or later
I/O/cleanup failure can therefore leave an unreferenced descriptor; a caller may
already have stored the output too. There is no rollback, corrupt-pointer
quarantine, overwrite/replace API, automatic recomputation, stale-lock-file cleanup
or startup recovery scan. An existing corrupt pointer/result is not silently
replaced. Computing two results concurrently can duplicate work and reach the
same-key conflict. Exactly-once computation is not a contract here.

### Separate bundle/catalog boundary

[Bundle cache methods][cache-bundles] use absent `M3FsPrecomputeBundle` and
`M3FsPrecomputeCatalog`, not the store's nested role bundle:

- `lookupBundle(key)` calls ordinary lookup; both a miss and explicit EMPTY
  become `Optional.empty()`. A positive output must have bundle-descriptor kind.
  It reads at most 256 KiB of verified bytes, calls the external bundle decoder,
  checks bundle key ID, calls catalog `byId(profile).requireCompatible(bundle)`,
  then `bundle.requireStore(store)`
- `publishBundle(key, bundle)` checks equal key IDs, invokes the same catalog
  compatibility/store checks, stores the bundle's canonical bytes and publishes
  a VALUE pointing to that descriptor
- `lookupMappedBundle(key, maxLaneBytes, maxTotalBytes)` delegates to the decoded
  bundle's `openMapped` only after a positive lookup. This wrapper does not check
  the budgets on a miss, and the absent implementation determines that delegated
  method's additional budget and ownership semantics

No guarantee about those absent decoder/catalog/store checks can be inferred
beyond the calls themselves. In particular, this reference does not supply their
format, complete profile set, error taxonomy or source closure.

### Multi-bundle mapping and accounting

[`lookupMappedBundles`][mapped-bundles] implements more logic locally. It copies
a nonempty list of keys, rejects negative lane/total budgets, checks distinct
key IDs as it iterates and calls `lookupBundle` in input order. It returns absent
as soon as a bundle is absent, so later duplicate IDs or failures are not
necessarily inspected. No lane mappings are opened until all lookups succeed.

It then collects distinct lane content IDs in first-seen order. Each reference
must fit `maxLaneBytes` and `Integer.MAX_VALUE`; repeated IDs must have the same
size and kind. The unique byte sum must fit `maxTotalBytes`, checked by subtraction
before addition. It maps each distinct reference once with `store.openMapped`,
then rebuilds the bundle/lane lists in their original order using the shared
`M3MappedObject` per ID. A later mapping or construction failure does not
explicitly unmap earlier successes.

`MappedBundle` freezes its lane list and validates nonnull key/bundle, equal
bundle key ID, lane-count equality and positional equality of each mapped lane's
lane descriptor with the bundle lane. `requireLane(name)` strips a nonnull,
nonblank name and returns the matching lane or throws. Validation of the separate
`MappedLane` implementation remains outside the retained slice.

`MappedBundleSet` freezes a nonempty bundle list and rejects a negative
`uniqueMappedBytes`. Its public constructor does not recompute that number;
the lookup method above computes it. `bundles(profile)` strips a nonnull,
nonblank profile and returns matching bundle records in list order. Generated
record accessors/equality are structural and do not reverify files. Neither
record supplies a close/unmap API. The unique-byte accounting is local to one
lookup and concerns encoded lane capacities, not all live mappings, descriptor
heap, virtual-memory overhead or resident process memory.

## 8. Caller responsibilities, security and evidence

The two direct non-fs callers in the complete retained production subtree are
[`MIndexRegexPrecompute`][regex-caller] and [`MIndexRegexPrecomputeKey`][regex-key].
Their [published contract](retained-regex-reference.md#6-persisted-request-cache-identity-and-publication)
adds exact request and codec validation to these generic primitives. On a
positive regex hit it requires `PRECOMPUTE_OUTPUT`, admits a bounded mapping,
decodes the image and checks expression, semantics and state limit. Corrupt or
mismatched hits fail without calling the compiler. A compiler's null result is
persisted as EMPTY; compiler exceptions are not stored by that caller.

Its key binds an exact-UTF-16 expression hash, explicit compiler/codec revisions,
semantics, state limit, runtime identity hash and fixed historical owner literals.
The original owner literals are not the later Program/codec source hashes; see
the [regex identity discussion](retained-regex-reference.md#historical-owner-literals-versus-current-implementation).
The generic cache does not discover or correct inaccurate revision declarations.
Mapped codec checks and request checks are separate from filesystem digest checks.

The trust model requires a trusted root, ancestors and cooperating writers:

- The object constructor/publication reject selected direct symlinks, but do not
  walk every ancestor or hold a directory-handle sandbox. Most file checks and
  uses are separate, and the cache does not apply the store's symlink policy
  to its pointer/index/lock hierarchy
- `openMapped` has a final-component no-follow channel open; that does not secure
  mutable ancestor paths or prevent later writes to the inode
- There is no user/tenant ACL, encryption, signature, authentication, chmod-based
  immutability, path-root authorization or content-origin verification here
- Public `root()` exposes the location. Caller-supplied root/source paths may be
  arbitrary Paths subject to OS access; lexical normalization is not a security
  boundary. UUID staging and hex shards avoid key-text path traversal but do not
  defend an already hostile directory tree
- Error paths may leave published/unreferenced objects and ordinary locks/files;
  callers need an external operational policy before using the store for durable
  multi-process or untrusted workloads

The [coverage map](retained-content-store-reference-coverage.md) records exact
source hashes, every member group, inspected callers and public evidence.
There are no fs test bodies under the retained `src/test` tree; its five Java
test/proof files belong to AST. The public v7 [qualification record][qualification]
reports bounded importer/AST/replay work and explicitly excludes source-complete
receiving build, actual consumer wiring, native/JNI, matched JDK/jtreg, performance
and full-family completion. It is not an executed current test of these eight
owners. Source analysis here does not turn those exclusions into success.

Before runtime promotion, unresolved gates include complete dependency/licensing
closure, exact provider byte-comparison behavior, valid/corrupt/malformed artifact
cases, concurrent/cooperating and hostile-path behavior, disk/permission/cleanup
failures, crash durability/recovery, mapped replacement/lifetime/platform cases,
caller format/request compatibility and actual cold/warm resource measurements.
Source and documentation review can specify those boundaries; it does not execute
them or authorize source changes, merge, deployment or phase advancement.

[content-id]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3ContentId.java
[object-ref]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3ObjectRef.java
[object-kind]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3ObjectKind.java
[store-construction]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L310-L330
[store-put]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L485-L527
[store-publication]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L727-L778
[store-read]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L529-L612
[lane-writers]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L332-L452
[primitive-stream]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L679-L725
[lane-bundle]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L211-L308
[profiles]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L35-L209
[bundle-io]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L454-L483
[mapping]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3MappedObject.java#L19-L62
[mapped-views]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3MappedObject.java#L64-L148
[key]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeKey.java
[key-decode]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeKey.java#L62-L129
[result]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeResult.java
[cache-lookup]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeCache.java#L16-L46
[cache-publish]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeCache.java#L220-L280
[cache-bundles]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeCache.java#L172-L218
[mapped-bundles]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeCache.java#L48-L170
[regex-caller]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexPrecompute.java
[regex-key]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexPrecomputeKey.java
[vendor]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/README.md
[delivery]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-delivery.md
[reactor]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/pom.xml
[import-pom]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/pom.xml
[build]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/build.sh
[license]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/LICENSE
[notice]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/NOTICE
[reuse]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-apache-reuse.md
[borrowing]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/synexia-apache2-borrowing-contract.md
[policy]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/docs/m3jdk21-porting-invariant.md
[current-plan]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/current-full-borrow/README.md
[qualification]: https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/evidence/QUALIFICATION.json

[jdk-encoding]: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html#getBytes(java.nio.charset.Charset)
[jdk-lock]: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/channels/FileChannel.html#lock()
