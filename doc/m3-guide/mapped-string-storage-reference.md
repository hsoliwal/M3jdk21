<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->

# Mapped String backing, logical addresses and lifetime

[Guide](README.md) · [Runtime owners](runtime-owners.md) · [Bootstrap String ownership](bootstrap-string-ownership.md) · [String operations and facts](bootstrap-string-contracts.md)

This is a bounded implementation reference for six owners in `java.base`'s internal `jdk.internal.mindex` package: `M3StringBacking`, `M3MappedStringBacking`, `M3Boundary`, `M3Address28`, `M3Release` and package-private `M3Bits`. All six complete bodies, including their declared helpers, were read at public source revision `1dce82bfb7f1058d74f7503c4cd54fa7d431c032`. Their blobs also match the earlier package inventory at `278b221e8365ebeb6095a3a5643b38a0564258b5`.

These owners are internal implementation surfaces, not a newly exported JDK API, a separate Maven module, or an operating-system interning service. The reference does not complete the other owners in this package. Source inspection and documentation checks are distinct from target qualification: repository builds, tests, policy/recipe verifiers, native execution and resource measurements were **NOT_RUN** for this pass.

## Read the existing ownership contracts first

The [current runtime guide](../../m3/runtime-integration/README.md), [bootstrap ownership reference](bootstrap-string-ownership.md), [JNI ingress/acquire/release guide](../../m3/docs/mindex-jni-string-ingress.md) and [native build matrix](../../m3/docs/native-build.md) retain their responsibilities. The older [backing design](../mindex-string-backing.md) labels its first-stage narrative as historical; its statement that ordinary `String` is unchanged must not be applied to the later integrated runtime.

The [porting invariant](../../m3/docs/m3jdk21-porting-invariant.md), [naming map](../../m3/docs/name-mapping.json), [full-family pin](../../m3/compatibility/synexia-full-family-pin.tsv), [current receiver plan](../../m3/synexia-import/current-full-borrow/README.md) and [borrowing policy](../../SYNEXIA_APACHE_BORROWING_POLICY.md) remain authoritative. Receiving order is String → arrays → collections → AST/compiler → remaining families. This page neither advances that order nor imports private donor bodies. Public target source, per-file OpenJDK/third-party rights and existing provenance remain unchanged.

### Four different kinds of coordinate

| Value | Meaning and boundary |
| --- | --- |
| `M3StringBacking` text ID | A logical text coordinate interpreted by its backing. The mapped implementation packs a language in the high 32 bits and one-based row in the low 32 bits. The number alone carries no store namespace or generation. |
| `M3MappedStringBacking.utf8Handle(id)` | A positive one-based row in the particular sibling byte store, not a byte offset or native address. Two language rows may deliberately share it. |
| `M3Address28` slot | A 28-bit logical primitive-lane slot split into region/block/offset. It is unrelated to a text ID, mapped-file byte offset or process address. |
| `M3StringAtom.address` | A process-local native address in the separate bootstrap String owner path. A retained mapping/owner and validated range make it usable; the address is not portable identity. |

In the bootstrap model, `M3String`'s packed coordinate is an owner-relative UTF-16 start/length, again distinct from these values. Java object identity, `String.intern()` identity, exact UTF-16 equality, owner/range identity and persisted store identity must remain separate. Mapping the same immutable file can allow OS page sharing; it does not make equal text from arbitrary files, owners or processes one canonical Java object.

## `M3StringBacking`: transport-neutral surface

The [complete interface][interface] requires immutable backing semantics and says an identifier is logical rather than a process address. It permits mapped, VM-managed, FFM or native implementations, but does not supply these transports or authenticate their ownership. It declares no writer, address export, namespace/generation accessor, ID enumerator, refresh, retain/release counter or `isClosed()` operation.

| Declared method | Contract at this boundary |
| --- | --- |
| `close()` | Narrows `AutoCloseable.close()` to no checked exception. Concrete release mechanics and post-close behavior belong to the implementation. |
| `length(id)`, `utf8Length(id)` | UTF-16 unit and UTF-8 byte lengths, respectively; they are different coordinate spaces. |
| `codePointCount(id)`, `unpairedSurrogateCount(id)` | Logical Unicode/surrogate facts; isolated UTF-16 surrogate units are preserved in the text. |
| `hashCode(id)`, `charAt(id,index)` | String-compatible UTF-16 hash and indexed unit access. This is an ID-parameterized hash, not an override of `Object.hashCode()`. |
| `utf16View(id)`, `utf8View(id)`, `materialize(id)` | Representation/export boundaries. The interface does not itself guarantee direct buffers, independent lifetime or a particular allocation strategy. |
| Default `contentEquals(id,other)` | Returns false immediately for null; otherwise compares length then every UTF-16 unit. Null therefore does not force ID validation. It repeatedly calls `other.length()` and needs a stable `CharSequence` for a coherent comparison. |
| Default `compare(leftId,rightId)` | Lexicographic UTF-16-unit comparison, then length; validates/accesses each ID through implementation methods. No locale collation, Unicode normalization or code-point ordering. |

The concrete mapped override of `contentEquals` rejects null with `NullPointerException("other")`. Code written against the interface must not infer identical null behavior across implementations. Neither comparison implementation promises a transaction over a concurrently mutating `CharSequence`.

A concrete caller is [`M3TQ.precompute(backing,id,from,to,maxUtf16Units)`][tq]: it scans through a temporary nonmaterializing reader, requires the backing to remain open and immutable during preparation, and returns facts without retaining it. Reuse must be bound to the exact owner, generation, ID and range by the caller; the overload does not install a cache or change `Pattern`/`Matcher` dispatch. This is a scoped dependency read, not a new full `M3TQ` review.

## Opening a mapped text/byte snapshot

[`M3MappedStringBacking.open(Path)`][open] normalizes a nonnull path to an absolute path, opens the text file read-only, selects its commit, maps its committed extent, then opens the sibling path formed by appending `.bytes` to that normalized path's string. The byte file is mandatory even when there are no text rows. This owner uses Java NIO `FileChannel`/`MappedByteBuffer`; it declares no custom JNI entry point, loads no custom native library and owns no FFM arena. Standard JDK mapping still relies on platform/native OS machinery; the absence of a custom M3 JNI boundary does not imply otherwise.

The constructor builds private row directories and verifies all selected text/byte rows before returning the backing. This is eager admission, not lazy first-access validation. It reads the two commits independently: there is no cross-file transaction, shared lock, generation-equality check or automatic refresh. A publisher must provide a coherent pair of immutable committed images. Appended bytes beyond a selected commit are not mapped by that snapshot; later rows require a new open.

On an `IOException`, construction closes acquired resources and throws `UncheckedIOException` with an open-context message. On a `RuntimeException` or `Error`, it closes acquired resources and rethrows the original failure. Cleanup errors are attached as suppressed exceptions rather than masking the original. Cleanup closes channels; it is not explicit unmapping of any pages already mapped.

### File header and commit selection

Both headers are 64 bytes and all integers/longs read here are **big-endian**, independent of host byte order. [`readCommit` and its helpers][commits] enforce:

| Field | Text file | Sibling byte file |
| --- | --- | --- |
| Magic at byte 0 | `0x4d49584d` (`MIXM`) | `0x4d49424d` (`MIBM`) |
| Version at byte 4 | Exactly 3 | Exactly 1 |
| Header size at byte 8 | Exactly 64 | Exactly 64 |
| Commit slots | Two 24-byte slots at offsets 16 and 40 | Same |
| Minimum record header used for row-count bound | 120 bytes | 32 bytes |

Each slot contains signed-positive generation (`long`), committed byte length (`long`), row count (`int`) and CRC32 (`int`). CRC covers the preceding 20 bytes in big-endian form. Nonpositive generation, committed length below 64, negative rows or wrong CRC makes that slot invalid. If neither slot is valid, open fails. Otherwise the highest generation wins, with slot 0 winning a tie.

After selection, committed length must fit the current channel size, and row count must not exceed `(committedLength - 64) / minimumRecordHeader`. This prevents an impossible row-directory allocation from that count; it is not a user-configurable memory budget. If the selected newest slot has valid CRC but fails these later geometry checks, the reader fails rather than retrying the older slot. Text and byte generations are not compared or exposed.

Byte 12 of each file header is not inspected. CRC is an integrity check, not cryptographic authenticity, source provenance or protection against an adversary able to rewrite both payload and checksum.

### Paging and record geometry

[`Pages`][pages] maps the committed extent in 256 MiB read-only chunks. Row scanning starts at byte 64. If fewer than a full record header remain in a page, or the next record magic is zero, parsing advances to the next page. It does not inspect every skipped padding byte. Each admitted record must fit entirely in its page and within the selected committed extent; no record-spanning view is assembled.

`Pages.check(offset,bytes)` rejects negative values or `offset > length - bytes` with `IndexOutOfBoundsException`, avoiding an overflowing `offset + bytes` validation. `slice` then rejects a nonempty range crossing pages with a corruption `IllegalStateException`. `getInt`/`getLong` use ordered slices; `getByte` performs a checked absolute byte read. `nextPage` and record sizing use checked arithmetic; `align8` rounds an integer size upward with `Math.addExact`. Excessive/malicious sizes may throw `ArithmeticException`, range errors or allocation failures rather than one uniform corruption exception.

The row loops stop after the declared number of rows; they do not require their final cursor to equal committed length. This reader does not establish that every committed byte belongs to a parsed row or authenticated padding. Private arrays retain row metadata; no payload array is kept as a second canonical spelling. Page/directory objects and temporary encoding buffers still consume memory, and all selected records are scanned at open.

### Text records: facts and payload aliases

The [text parser and validators][text-parse] read this subset of the 120-byte `MIXR` header:

| Relative offset | Field and admission rule |
| --- | --- |
| 0 / 4 | Record magic / record byte size; size must fit page/commit and be at least 120 |
| 8 | Language coordinate, stored as an `int`; no locale, language registry or sign restriction is validated |
| 12 / 16 | Nonnegative UTF-16 unit length / UTF-8 byte length |
| 20 / 24 | Code-point count / unpaired-surrogate count |
| 28 / 32 | Java UTF-16 hash / power of 31 |
| 36 / 40 | First / last UTF-16 unit; scalar verification requires `-1` for empty payload |
| 44 | Payload reference: zero for a scalar, otherwise one-based earlier owner row |
| 56 | Scalar UTF-16 payload CRC32 |
| 112 | Positive sibling byte-store handle, whose length must match the text row's UTF-8 length |

A scalar (`payloadRef == 0`) must have exactly `align8(120 + 2 * utf16Length)` bytes. Its payload follows the header in big-endian UTF-16. Admission recomputes CRC32, code points, isolated-surrogate count, Java hash, power-of-31 and edge units. Hash/power arithmetic intentionally wraps as Java `int`. A high/low surrogate pair counts as one code point; isolated units remain exact text.

A nonzero payload reference must designate an already parsed row, and its record size must be exactly 120. It reuses that row's character offset and must match its UTF-16/UTF-8 lengths, code-point/surrogate facts, Java hash, power, edge units and **exact UTF-8 handle**. It can use a different language coordinate. It may refer to an earlier alias as well as an original scalar; forward/self references and cyclic aliases are refused. The alias's own CRC field is not independently checked because it has no payload.

For every new scalar, [`verifyTextUtf8`][utf8-validation] re-encodes the validated UTF-16 with the standard JDK UTF-8 encoder using replacement for malformed/unmappable input. It compares the exact output against the referenced byte payload, including final exhaustion. A reusable 4 KiB heap scratch buffer bounds encoder output staging; it does not bound total file/directory/mapping memory. Aliases reuse the previously admitted scalar facts. This is ordinary UTF-8, **not JNI modified UTF-8**. Exact unpaired UTF-16 units survive in the text while their byte projection follows encoder replacement.

### Byte records and deliberately unconsumed metadata

The [private `ByteStore`][byte-store] requires `MIBR` magic, exactly `align8(32 + length)` record bytes, nonnegative length, page/commit containment and CRC32 over payload bytes. Handle `h` selects row `h - 1`; zero, negative or out-of-range handles fail with `IllegalArgumentException`.

The byte record's hash at offset 12 is copied into the private `hashes` array but is neither recomputed nor exposed/used by this owner. A correct byte CRC alone does not bind bytes to text; the scalar UTF-8 comparison supplies that separate check. Byte header fields at relative offsets 20–31 are not consumed.

Text header bytes 48–55 and 60–111 are not consumed by this implementation. No optional sidecar, translation profile, source-ID manifest, metadata extension or semantic interpretation is loaded from them. Reserved/ignored fields are not required to be zero. The private `payloadRefs` array records admitted references but has no public accessor; `Pages.capacities` records mapped chunk sizes without being used by subsequent accessors. Neither unused storage nor its presence establishes an additional validation contract. See the separate [lexicon/catalog reference](lexicon-catalog-reference.md) for actual sidecar consumers.

## IDs, views and all mapped accessors

[`row(id)`][row-close] first requires an open backing. It interprets the low 32 bits as an **unsigned** one-based coordinate, rejecting zero or a coordinate greater than `size()`. It then requires the row's language to equal the high 32-bit `int`. Failures are `IllegalArgumentException`; a closed backing fails first with `IllegalStateException` for these ID-based accesses. For a known language/zero-based row, the fixture forms an ID as `((long) language << 32) | Integer.toUnsignedLong(row + 1)`. This is a format explanation, not a public ID factory or enumeration API.

The ID does not contain a filename, UUID, digest or selected commit generation. `path()` returns a normalized path, not resolved file identity or an authenticated namespace. Callers comparing persisted coordinates across opens/processes must establish the same immutable store namespace and compatible snapshot themselves. The reader does not reject duplicate scalar spellings or globally intern them. Cross-language alias rows can have different IDs while sharing bytes and comparing equal.

| Declared accessor/operation | Effects, checks and result |
| --- | --- |
| `path()`, `size()` | Return normalized text path and admitted text-row count. They do not check `closed` and remain readable after close. |
| `languageId(id)`, `length(id)`, `utf8Length(id)` | Validate ID/open state, then return the stored language or validated lengths. |
| `codePointCount(id)`, `unpairedSurrogateCount(id)` | Whole-row facts verified at open; not range facts or normalization. |
| `hashCode(id)`, `hash31Power(id)`, `firstUtf16Unit(id)`, `lastUtf16Unit(id)` | Validated whole-row String facts; edge accessors return `-1` for an admitted empty scalar/alias. |
| `utf8Handle(id)` | Returns the checked sibling byte-row handle; it is not a pointer or owning resource. |
| `charAt(id,index)` | Checks ID/open state and UTF-16 index, then reads the big-endian unit at `charOffset + 2L * index`. Code-point indices are not accepted. |
| `utf16View(id)` | Fresh read-only `CharBuffer` cursor over the UTF-16 payload, explicitly big-endian. No payload copy for a nonempty view. |
| `utf8View(id)` | Fresh read-only `ByteBuffer` cursor over the canonical sibling byte payload. Cursor mutation does not alter a different returned cursor or the payload. |
| `view(id,start,end)`, `view(id)` | Check ID/open state and half-open UTF-16 bounds, then return a small `View` retaining this backing. Empty ranges and split-surrogate boundaries are allowed. |
| `materialize(id)` | Converts the UTF-16 view to an ordinary String. This is an explicit materialization boundary; no String-object-identity guarantee is made. |
| Package-private `materialize(id,start,length)` | Uses the whole UTF-16 view, adjusts position/limit, then materializes its slice. Trusted `View` callers provide the valid range; it does not independently use an overflow-safe start/length precheck. |
| `compare(leftId,rightId)` | Unit-wise lexicographic comparison then length, using repeated checked reads. |
| `contentEquals(id,other)` | Rejects null before checking ID/open state; compares one captured backing length against `other.length()`, then exact units. Caller controls stability and effects of `other`. |
| `close()` | See lifetime below; no checked exception. |

**Empty-buffer exception to the direct-view description:** `Pages.slice(offset,0)` returns `ByteBuffer.allocate(0).asReadOnlyBuffer()`. Empty UTF-8 and UTF-16 views are read-only but heap-backed/non-direct. The method comments' “direct” description applies to nonempty mapped payloads, not every returned buffer. This source-observed distinction was not repaired by the documentation pass.

### `View`: retained range, not a String value object

[`View`][view] retains `backing`, original `id`, absolute UTF-16 `start` and `length`. Its private constructor trusts the enclosing validated factories. It has no close/retain/release method or independent ownership transfer.

| Declared member | Behavior |
| --- | --- |
| `id()`, `start()`, `length()` | Return stored values without rechecking the backing. The ID remains that of the whole row, not a new subrange ID. |
| `charAt(index)` | Checks range-relative index, then delegates with `start + index`; backing open state is checked by that delegated read. |
| `subSequence(from,to)` | Checks bounds relative to this view and creates a new retained view with adjusted start. It does not check whether the backing has since closed. |
| `codePointCount()` | Scans this range with `Character.codePointCount`; a pair split at a range edge is counted according to the range's units. An empty range need not read the backing. |
| `hashCode()` | Uses the backing's precomputed hash for a whole-row view, otherwise computes the ordinary wrapping UTF-16 hash. When `start == 0` it checks the backing's length; a nonzero-start partial view proceeds directly to its unit loop, and an empty such view can return zero after close. |
| `toString()` | Materializes this exact range through the backing. |

There is no `equals` override: views use object identity for equality even though their hashes are content-derived. `CharSequence` defaults are inherited, not separately specialized here. A retained view keeps the backing object reachable but cannot prevent another holder calling its `close()`.

## Lifetime, concurrency and external writers

[`close()`][row-close] sets the volatile `closed` flag before closing either channel. Repeated sequential closes are no-ops. On the normal/`IOException` paths it attempts both text and byte closes; text/byte `IOException`s are combined into `UncheckedIOException`, with a later byte failure suppressed when necessary. An unchecked exception or `Error` from the text close is not caught here and can prevent the byte-close attempt. State stays closed even if channel close fails; a later close does not retry cleanup.

There is **no explicit unmap**, cleaner invocation, reference count, buffer revocation or mapping-array clear. Previously returned buffer objects retain their mapped backing through the NIO view chain, and their operations do not consult this owner's `closed` flag. Do not describe `close()` as immediate native-memory reclamation or guarantee a post-close buffer failure. Likewise, the backing and `View` can retain mappings after close. ID-based backing methods refuse new access, while `path()`, `size()` and cached `View` metadata can still be read.

The volatile flag is not a transaction or lock across a complete operation. Reads racing close may pass an earlier open check; a multi-step comparison can observe closure during a later checked read. Concurrent `close()` calls are not serialized. This source does not promise linearizable read/close behavior or a deterministic mapping-release deadline.

Read-only mapping prevents writes through these returned views. It does **not** freeze bytes against other writers or enforce append-only publication. The owner takes no file lock, checks no live generation on each read, and does not continuously revalidate CRC/facts. Rewriting/truncating admitted bytes can invalidate cached facts, change observed content or make native mapping access unsafe. Keep the committed extents immutable for the complete lifetime of every backing, buffer and native consumer; publish replacements as new qualified snapshots rather than editing active payloads. Reopening a path is not proof that it refers to the same underlying file.

The inherited [MappedByteBuffer contract][nio-mapping] explicitly leaves visibility of external file changes operating-system dependent and warns that truncation can make a mapped region inaccessible. The source supplies no portable guarantee for rename/unlink/replacement behavior across operating systems. The separate two-process bootstrap test described below is a finite Linux-style inode/mapping test, not a guarantee for this reader or for every filesystem/platform.

## `M3Boundary`: optional weak JVM metadata

[`M3Boundary`][boundary] is a different owner in the same package: a frozen class-index boundary, not a file mapping, native allocator or String store. Its private constructor retains weak-reference arrays and fixed class/loader counts. Package-private `of(classes,loaders,retainWeakBoundary)` requires nonnull arrays. With retention disabled it stores shared empty arrays and only the input lengths, allocating no per-entry weak references and not inspecting each class element. With retention enabled it creates a weak reference per nonnull class (null elements fail) and per loader (null is allowed); input arrays themselves are not retained.

| Declared method | Contract and edge cases |
| --- | --- |
| `retained()` | Tests `classes.length != 0`, not the original option. A retained-mode construction with no classes consequently reports false. |
| `classCount()`, `loaderSpaceCount()` | Frozen inventory counts, not live-reference counts. |
| `classAt(row)` | Checks row against frozen count, then returns an optional live `Class`; empty for compact/detached or collected references. |
| `loaderAt(loaderSpace)` | Checks bounds first. Bootstrap coordinate 0 always returns empty; compact/detached and collected loaders also return empty. |
| `loaderSpaceOf(loader)` | Null means bootstrap and returns 0, even without retained references. Otherwise searches by object identity starting at slot 1; returns `-1` for absent, compact/detached or collected loader. |
| `liveClassCount()` | Counts currently nonnull weak referents, or zero without retained references. |
| `liveLoaderCount()` | Returns zero without retained references; otherwise begins at one for the logical bootstrap space and counts live loader references from slot 1. It is not simply the count of nonnull loader objects. |
| `retainedReferenceCount()` | Returns class-array plus loader-array lengths when `retained()` is true, otherwise zero. Approximate weak-reference-object accounting, not byte/heap measurement. |

Weak references do not pin classes/loaders. Returned optionals temporarily contain strong referents held by the caller; GC may clear other references between calls, so counts and separate queries are not an atomic liveness snapshot. There is no `close()`, invalidation listener, global loader registry or cross-process class identity.

Scoped [`M3CI` caller excerpts][ci-options] show standard options retain the boundary and compact options omit it, both with default maximum 16,384 classes; [freeze passes the discovered class/loader arrays][ci-freeze]. Coordinates are snapshot-local. Matching binary names, text descriptors or loader-space integers in a different snapshot does not establish the same loaded class. Full `M3CI` and member-owner contracts remain outside this six-owner review.

## `M3Release`: exact caller-declared provenance

The [record][release] stores seven existing JDK String references: `origin`, `group`, `artifact`, `rawVersion`, `extension`, `classifier`, `contentSha256`. The compact constructor requires the first five to be nonnull and nonempty; classifier must be nonnull but may be empty. The digest must be nonnull, exactly 64 characters and exclusively lowercase ASCII hexadecimal. Null yields `NullPointerException`; empty required labels, wrong digest length or wrong characters yield `IllegalArgumentException`.

There is no trimming, case normalization, URI/Maven-coordinate validation, version parsing, digest computation or artifact access. Whitespace-only labels meet the nonempty check. Generated accessors return the stored Strings; generated record equality/hash/toString use all seven exact components. Thus `rawVersion="1"` and `"1.0"` remain distinct even if a separate version comparator treats them alike. The private `require` helper implements only null/nonempty validation.

A syntactically valid digest is a caller assertion, not proof of downloaded bytes, a signature or a loaded class's possibly transformed definition. Scoped [`M3CB.declare`/`effectiveVersion`/`effective`/`isCurrent`][cb] attach this declaration to an actual class row and session epoch; conflicting declarations are refused and explicit invalidation retires the associations. This does not install a class loader, automatically detect redefinition, select an implementation or authenticate definition bytes. `M3Release` is metadata, despite the word “Release”; it does not release resources.

## `M3Address28` and `M3Bits`: logical arithmetic

The [public internal address helper][address] is static-only, with a private no-op constructor. Its layout has 8 region bits, 8 block bits and 12 in-block offset bits:

| Constant | Value |
| --- | --- |
| `REGION_BITS`, `BLOCK_BITS`, `OFFSET_BITS` | 8, 8, 12 |
| `REGION_COUNT`, `BLOCKS_PER_REGION` | 256, 256 |
| `VALUES_PER_BLOCK`, `OCCUPANCY_WORDS_PER_BLOCK` | 4,096 slots, 64 words |
| `MAX_SLOTS`, `MAX_SLOT` | 268,435,456 slots, last valid slot 268,435,455 |
| `OFFSET_MASK`, `BLOCK_MASK` | 4,095, 255 |

`region(slot)`, `block(slot)` and `offset(slot)` each call `checkSlot`, then extract the 8/8/12 pieces. `compose(region,block,offset)` rejects any out-of-range bits, including negative inputs, with `IndexOutOfBoundsException`, then combines the components. `checkSlot` accepts `0 <= slot < MAX_SLOTS`. `checkRange(from,to)` uses `Objects.checkFromToIndex` for a half-open range; `checkRangeSize(offset,length)` uses `Objects.checkFromIndexSize` without an overflowing addition. Both accept the empty range at `MAX_SLOTS`, where a single slot access is invalid.

The helper neither allocates primitive blocks nor authenticates a native pointer. Allocation, mutation, occupancy and concurrency belong to the lane owner. A 28-bit slot space is a logical capacity, not a retained-memory allocation or portable memory address.

Package-private [`M3Bits`][bits] has a private no-op constructor and four unchecked static helpers:

| Method | Exact arithmetic and caller obligation |
| --- | --- |
| `wordIndex(bitIndex)` | `bitIndex >>> 6`; caller must establish a valid nonnegative bit index and array extent |
| `bitMask(bitIndex)` | `1L << (bitIndex & 63)`; intentionally chooses a bit within a word, with no validation |
| `wordsForBits(bitCount)` | Widen to `long`, add 63, unsigned-shift by 6, cast to `int`; for nonnegative `int` counts this is ceiling division without signed-int addition overflow |
| `bitCapacity(wordCount)` | `wordCount << 6` in `int`; large/negative counts are not rejected and overflow is not checked |

Do not promote these helpers into defensive range-check APIs. Scoped [`M3BitLane28.get/set`][bit-caller] validate through `M3Address28` before applying word/mask arithmetic to an in-block offset. The separate explicit `cardinalityNative(from,to)` path invokes a JNI peer after range validation. [Its C implementation][bit-native] copies each selected page's words with `GetLongArrayRegion` into a 64-word stack buffer, checks directory/page dimensions, and releases local references. No array pointer is retained. Its native symbol must be present in the matching JDK `libjava`; there is no automatic Java fallback in that call. This does not add a custom JNI-library prerequisite to `M3Address28`, `M3Bits` or the mapped String reader themselves.

## Native String consumption is a separate path

The current bootstrap [`M3StringPool.Lexicon`][lexicon-open] opens `M3LEX001` (versions 1/2) or `SYNARR01` (version 1), rather than delegating to this text-v3/byte-v1 backing. The full format/lookup contracts already live in [bootstrap String ownership](bootstrap-string-ownership.md#lexicon-snapshot-formats-and-mapped-lifetime). Setting `jdk.mindex.lexicon` does not make a `MIXM`/`MIBM` pair one of those accepted formats.

Scoped [`Lexicon.atom`][lexicon-atom] computes an address from its retained mapping base plus validated payload/row offsets and supplies the mapping as `payloadOwner` to [`M3StringAtom.mapped`][atom]. The atom's limited constructor checks (nonzero address for nonempty content, storage width one or two) do not independently prove pointer validity, extent or lifetime. Width-two access uses explicit endian; logical compact-String coder and physical mapped width can differ. Callers must keep the owning mapped memory reachable and unchanged. There is no conversion from a raw text ID or 28-bit slot directly into this address.

HotSpot's [M3 field-layout and owner-copy excerpt][vm-copy] reads the matching owner/atom/tuple fields and copies UTF-16 units from native addresses or recursively from child owners. This is why installing a `String.class` or an atom class into an unrelated VM is unsafe. The integrated experiment needs a complete matched Java/HotSpot image; the current [runtime guide](../../m3/runtime-integration/README.md) identifies `UseM3StringStorage` as experimental, default-off and interpreter-constrained, with compiled mode/CDS/JFR/JVMCI/dedup restrictions. The plain backing test and the enabled ordinary-String experiment are different qualification lanes.

The existing [JNI guide](../../m3/docs/mindex-jni-string-ingress.md) is the authoritative caller-facing narrative. Scoped [construction and acquire/release source][jni-chars] confirms ordinary `NewString`/`NewStringUTF` construction precedes gated Java admission, and UTF-16/modified-UTF-8 acquires allocate native compatibility copies with matching releases. [Critical/region source][jni-critical] confirms `GetStringCritical` copies when M3 storage is enabled, while flag-off non-Latin1 text takes the existing pin path; critical release uses the same storage-flag rule. Native callers must pair releases, honor critical-region restrictions, size region outputs and handle failure. A shadow pointer is not canonical identity and releasing it does not release the canonical owner. Standard UTF-8 bytes from `utf8View` cannot be substituted for JNI modified UTF-8.

## Source tests and remaining proof

The following are inspected source assertions and harness requirements, **not execution results**:

| Source and read scope | What it actually checks; limits |
| --- | --- |
| [M3MappedStringBackingTest][test-backing], complete body | Writes one text-v3/byte-v1 pair containing a scalar with paired/isolated surrogates and a language alias. Checks lengths, language, hash, equality/materialization, unit access, nonempty direct/read-only views, shared UTF-8 handle, split-surrogate views, range hash/code points, equal-row comparison and a mismatched language rejection. Its jtreg declaration needs `java.base/jdk.internal.mindex` and runs without a custom JNI library or storage flag. No assertion covers empty buffers, close state, corruption, CRC/fact mismatch, commit failover, page seams, concurrent reads/close or external mutation. |
| [M3Lane28Test][test-lane], complete body | Checks 4,096 deterministic address round trips, endpoints, compose/range rejection and arithmetic bounds; broader lane tests include sparse/dense and JNI parity/refusal cases. The whole test requires the native peer even when interest is only address arithmetic. Optional `lane28.native` is a stock-JDK proof-loader route, not automatic production fallback. No separate direct test of all unchecked `M3Bits` inputs is established. |
| [CBProbe][test-cb], complete body | Exercises actual class/loader identity, live round trips, compact fallback, declared releases, conflicts/session epochs, same-name loader isolation and finite GC/unloading polling. It tests distinct raw version labels and one invalid digest string. It does not exhaust each boundary method, constructor null/empty/hex edge or all collector/timing behavior. |
| [MappedNativeProbe][test-native], complete body; [run-focused.sh][run-focused], complete harness | Checks mapped scalar/tuple/range text through five JNI modes and JVMTI without changing canonical owner/coordinate. Uses the **bootstrap shared-owner path**, not `M3MappedStringBacking`. Harness needs matched test JDK, intentional boot JDK, separately qualified shared-owner source, GCC/Linux JNI headers, built native `.so` helpers and its prepared fixture. |
| [two-process.py][two-process], complete harness | Starts two enabled bootstrap VMs against one shared arena, checks a read-only shared mapping and same inode/derived ID, unlinks only after both report ready, then requires lifetime checks after GC. It depends on prepared integration fixtures and Linux-style mapping evidence. It is not a test of text-v3/byte-v1 backing, arbitrary cross-process equality, external rewrites, every filesystem or deterministic unmapping. |

`run-api.sh` is another [source-reviewed JNI harness][run-api], with explicit checked-JNI default/interpreter/C2/enabled/noncompact cases; its C2 case does not enable M3. Its expected counters/digests are script checks, not new passing receipts. Use the [native build guide](../../m3/docs/native-build.md) and actual current workflow for reproduction rather than inferring support from a test filename.

Open work includes hostile text/byte format coverage (CRC-valid wrong facts and UTF-8, invalid aliases/IDs, unconsumed bytes, newest invalid geometry), empty buffer behavior, selected-commit coherence across the file pair, mapping/page boundaries, open/cleanup failure injection, concurrent close and mutable `CharSequence` interactions, mapped lifetime under GC and external file operations, actual native symbol/ABI execution, platform variation, resource budgets and full package/member-consumer review. CRC or digest presence, a public source body and a finite fixture do not close these gates.

## Coverage and exact source ledger

Only the six owner rows below are marked **FULL_BODY_CONTRACT_REVIEW** in this increment. The mapped owner includes `View`, `ByteStore`, `Pages`, `Commit`, private construction/cleanup, parsing, encoding/CRC checks, range/page helpers and their state. `Commit`'s three generated record accessors/equality/hash/toString are ordinary value projections; the private record is created by the validated slot reader. No implicit or inherited API is claimed to have been independently reimplemented or exhaustively tested.

For completeness, the mapped owner's nonpublic helpers have the following bounded roles:

| Helper/state | Contract |
| --- | --- |
| Constructor; `close(AutoCloseable,Throwable)` | Eager acquisition/validation and failure-preserving cleanup described above; no object is returned until both stores are admitted. |
| `parseTextRows`, `verifyTextPayload`, `verifyTextUtf8`, `matchUtf8` | Validate geometry/aliases, scalar facts/CRC and exact streamed byte projection. `matchUtf8` consumes both supplied cursors and refuses excess/mismatched expected output. |
| `row`, `requireOpen` | The owner-local ID and closed-state gates described above. |
| `ByteStore(Path)`, `parse`, `row`, `length`, `view`, `close` | Acquire and validate its own channel/commit/pages; retain offsets/lengths/hash metadata; serve checked handles; close its channel once. Its private plain-boolean closed check is part of `row`'s `IllegalArgumentException` condition, not the outer owner's `IllegalStateException` gate. Constructor failure closes its channel and suppresses cleanup errors. |
| `readCommit`, `readCommitSlot`, `commitCrc`, `byteCrc` | Selected-commit validation and CRC32 computations; byte CRC scans exactly the requested payload bytes. |
| `readFully` | Positional channel reads until the header buffer fills; premature EOF becomes `IOException`. Zero-byte reads retry with no explicit timeout, cancellation or retry limit. |
| `nextPage`, `align8`, `corrupt` | Checked page/record arithmetic; `corrupt` creates `IllegalStateException` with a mapped-backing prefix. Not every possible malformed-input failure passes through it. |
| `Pages` constructor; `map`, `getByte`, `getInt`, `getLong`, `slice`, `check` | Retain mapped chunks/capacities/extent and provide checked single-page access; zero-byte slices allocate the empty heap buffer described above. |
| Constants and directories | Text/byte magic/version/header sizes, commit positions, 256 MiB map page and 4 KiB UTF-8 scratch define this format implementation. `textPath`, channels, `byteStore`, `textPages`, two long row arrays, ten int row arrays and `closed` are per-backing state; these metadata arrays are not a global cache or second text payload. |

| Complete owner | Git blob |
| --- | --- |
| [M3StringBacking][interface] | `5daedbe16dfc9377783b52d932e548cc44f0b335` |
| [M3MappedStringBacking][mapped] | `c2dc2371c730aa758ccc45e06ba4ab95661e5593` |
| [M3Boundary][boundary] | `f10f815fc1a59a0d1ea7ed45158b89178f128a1a` |
| [M3Address28][address] | `14cdb999f5eab5be443044bff2d273fb4e4113fb` |
| [M3Release][release] | `1116f4cb0ffd361e764688640adb53af8cabe86d` |
| [M3Bits][bits] | `2ad46058a5325483647af4af16858c329ae24665` |

Dependency scope stays bounded: `M3TQ` backing adapter (230–253), `M3CI` options/boundary/freeze (25–48, 175–196, 899–907), `M3CB` declaration/effective/currentness (109–164), `M3BitLane28` checked word/mask and explicit native bridge (1–67, 208–218), `M3StringAtom` mapped address/reads (12–45, 302–355), `M3StringPool.Lexicon` mapping/admission/address construction (912–1034, 1191–1288), HotSpot field/copy code (945–1071), JNI String boundaries (2137–2274, 2767–2883) and the inherited `MappedByteBuffer` lifetime/file-mutation Javadoc (39–68). The `java.base` module descriptor was checked for the absence of a `jdk.internal.mindex` export. The small `M3BitLane28.c` peer was read completely as a dependency; that does not complete all Java lane contracts or other native readers. These are not additional fully reviewed package owners.

The original 24-file P03 package inventory remains unchanged. This increment deepens six owners; prior `M3TQ` documentation and these scoped caller reads do not imply complete sibling, package, `java.base`, repository, runtime or donor-family acceptance.

[interface]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/jdk/internal/mindex/M3StringBacking.java
[mapped]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/jdk/internal/mindex/M3MappedStringBacking.java
[open]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/jdk/internal/mindex/M3MappedStringBacking.java#L94-L153
[commits]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/jdk/internal/mindex/M3MappedStringBacking.java#L579-L646
[pages]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/jdk/internal/mindex/M3MappedStringBacking.java#L755-L832
[text-parse]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/jdk/internal/mindex/M3MappedStringBacking.java#L278-L363
[utf8-validation]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/jdk/internal/mindex/M3MappedStringBacking.java#L648-L738
[byte-store]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/jdk/internal/mindex/M3MappedStringBacking.java#L477-L573
[row-close]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/jdk/internal/mindex/M3MappedStringBacking.java#L365-L410
[view]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/jdk/internal/mindex/M3MappedStringBacking.java#L412-L475
[tq]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/jdk/internal/mindex/M3TQ.java#L230-L253
[boundary]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/jdk/internal/mindex/M3Boundary.java
[ci-options]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/jdk/internal/mindex/M3CI.java#L25-L48
[ci-freeze]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/jdk/internal/mindex/M3CI.java#L899-L907
[release]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/jdk/internal/mindex/M3Release.java
[cb]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/jdk/internal/mindex/M3CB.java#L109-L164
[address]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/jdk/internal/mindex/M3Address28.java
[bits]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/jdk/internal/mindex/M3Bits.java
[bit-caller]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/jdk/internal/mindex/M3BitLane28.java#L1-L67
[bit-native]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/native/libjava/M3BitLane28.c
[lexicon-open]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/lang/M3StringPool.java#L912-L1034
[lexicon-atom]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/lang/M3StringPool.java#L1191-L1288
[atom]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/lang/M3StringAtom.java#L302-L355
[vm-copy]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/hotspot/share/classfile/javaClasses.cpp#L945-L1071
[jni-chars]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/hotspot/share/prims/jni.cpp#L2137-L2274
[jni-critical]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/hotspot/share/prims/jni.cpp#L2767-L2883
[test-backing]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/test/jdk/java/lang/String/M3MappedStringBackingTest.java
[test-lane]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/test/jdk/jdk/internal/mindex/M3Lane28Test.java
[test-cb]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/test/jdk/jdk/internal/mindex/cb/com/m3/cb/CBProbe.java
[test-native]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/runtime-integration/tests/MappedNativeProbe.java
[run-focused]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/runtime-integration/run-focused.sh
[two-process]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/runtime-integration/two-process.py
[run-api]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/m3/runtime-integration/run-api.sh
[nio-mapping]: https://github.com/hsoliwal/M3jdk21/blob/1dce82bfb7f1058d74f7503c4cd54fa7d431c032/src/java.base/share/classes/java/nio/MappedByteBuffer.java#L39-L68
