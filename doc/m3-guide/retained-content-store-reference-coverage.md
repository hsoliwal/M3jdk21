<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors. -->

# Retained content-store reference: source and evidence coverage

[Contract reference](retained-content-store-reference.md) · [Guide](README.md) · [Coverage](coverage.md)

## 1. Exact eight-source primary inventory

Primary coverage is exactly the eight physical Java files in
`m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs`
at public commit `363c02caa2e5af1df9460b927ecd78f6a9dbcefb`.
All complete bodies, including nested types and private/package helpers, were
read. Fresh unfiltered directory listings at that pin and documentation stack
head `1de629d26ea82d2729a5993bfc88099f83221923` return the same eight paths and
Git blobs. The complete non-truncated retained subtree corroborates that list.
Its fs tree identity is `7c0db4bda946804e1f32d7c125888385d12cb7d4`.

The [comparison from the source pin to that documentation head](https://github.com/hsoliwal/M3jdk21/compare/363c02caa2e5af1df9460b927ecd78f6a9dbcefb...1de629d26ea82d2729a5993bfc88099f83221923)
contains two documentation commits and only Markdown paths under `doc/m3-guide`.
Source and policy bytes are unchanged across that comparison. The current
source bytes were checked against their Git blob identities and the SHA-256
fields of the public v7 export and source-custody receipt.

The common path prefix above applies to every filename in this table. These
are eight source owners, not eight runtime admissions. Nested records/enums and
all support members are included below without inflating that physical count.
This map is distinct from the 37-source retained AST map and the seven-source
retained root-regex map.

| Primary source | Lines | Bytes | Git blob SHA-1 | SHA-256 |
| --- | ---: | ---: | --- | --- |
| [M3ContentId.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3ContentId.java) | 71 | 2243 | `b8dffdfaf6809e130b7c95ae38c79cb2beb937c6` | `c1412090abf49c04fa5cc9b5d457d3809ad4bbac5bb41c45085b6ccaf075d74e` |
| [M3DirectoryContentStore.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java) | 779 | 27555 | `0fd9aec324ca0e1f608b0a72c22945ded63bd486` | `882a34ba5e4956c38c3c6ebd37840c76bc2ea7e480d2d86a3ef2cee5c7ea3f39` |
| [M3MappedObject.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3MappedObject.java) | 148 | 5840 | `103921e3c8efcf102c09c684faf46e2ac2c81838` | `4238d100d53cea1a7bbe3b4fd4105bf3f0b3a3acb30b0aee67e4fbe3797708a4` |
| [M3ObjectKind.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3ObjectKind.java) | 15 | 379 | `661a00395debb0d216d36cb028bcd72bb8651aa0` | `4ab6ba55814edd9501803e26e03c0dee8c73b033c105f1b3756ce7bf5c15c4ea` |
| [M3ObjectRef.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3ObjectRef.java) | 13 | 452 | `41cfec7999b4e07689069b584ce825d76a0b9e9d` | `e69debebeae92ecae49c7047f293ae256045243caec5e9e8b17fe81560499c77` |
| [M3PrecomputeCache.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeCache.java) | 280 | 11298 | `3603832eefe138bea46f22a7c1bf548153d2d391` | `14ecc79d5d298539085aa922090eb70665c05e913ef99acc74faeb040f00ea87` |
| [M3PrecomputeKey.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeKey.java) | 168 | 6630 | `25d34816903b02a5af53a131191d72f71d09dc09` | `720cff4115f6d30a90d228ae4a98a3fef76763c1ab0b08d909218c221f7d4cde` |
| [M3PrecomputeResult.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeResult.java) | 102 | 3747 | `727c38c2bd9c91cc643feff2fbb1c4290ccfda5d` | `e50355b571f0ebb61319ddf338003481c26e177f8346e1920f6702a457269fe4` |

All eight files have an Apache-2.0 SPDX header. Git SHA-1 is object identity,
not a security/authenticity claim. SHA-256 body matching establishes exact
public retained bytes, not behavioral correctness or permission to promote a
runtime dependency.

## 2. Complete member-to-contract map

Every declared field, constructor, public/package/private method and nested
owner is routed below. Source ranges include each implementation's complete
body. Enum constants and compiler-generated enum/record APIs are accounted for
separately; inherited Java platform methods are not additional source owners.

### M3ContentId

| Source range | Members | Contract route |
| --- | --- | --- |
| [11–21](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3ContentId.java#L11-L21) | Class; `BYTES`, `HEX`, `sha256`, cached `hashCode`; private constructor | Immutable stored ID and cached Java hash; §2 |
| [23–53](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3ContentId.java#L23-L53) | `of`, `parse`, `digest`, package `newDigest` | Cloning, hex/length checks, supplied-byte hashing and provider failure; §2 |
| [55–70](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3ContentId.java#L55-L70) | `bytes`, `hex`, `compareTo`, `equals`, `hashCode`, `toString` | Defensive export, unsigned order, byte equality and rendering; §2 |

### M3ObjectKind

| Source range | Members | Contract route |
| --- | --- | --- |
| [4–15](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3ObjectKind.java#L4-L15) | All nine constants; generated `values()`/`valueOf(String)` | Declared reference roles and persisted ordinal compatibility; §2 |

### M3ObjectRef

| Source range | Members | Contract route |
| --- | --- | --- |
| [6–13](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3ObjectRef.java#L6-L13) | Record components `id`, `size`, `kind`; compact constructor; generated accessors/equality/hash/string | Nonnull/nonnegative geometry, structural equality and no I/O; §2 |

### M3DirectoryContentStore

| Source range | Members | Contract route |
| --- | --- | --- |
| [31–33](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L31-L33) | Class; `COPY_BUFFER_BYTES`, `PRECOMPUTE_BUNDLE_MAGIC` | 128 KiB copy buffer and versioned descriptor magic; §§3–4 |
| [35–92](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L35-L92) | All 39 `PrecomputeRole` values; `PrecomputeScope.SOURCE/QUERY`; `PrecomputeRetention.KEEP/KEEP_IF_REUSED` | Role declarations and descriptive metadata; §4 profile table |
| [94–209](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L94-L209) | All 18 `PrecomputeProfile` values; fields `scope`, `retention`, `roles`; both constructors; `scope`, `retention`, `fileManifestEligible`, `roles` | Complete required-role table, default SOURCE/KEEP and query exceptions; §4 |
| [211–231](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L211-L231) | All nine `LaneEncoding` values; `width` field, constructor and accessor | Byte widths and explicit endian labels; §4 |
| [233–255](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L233-L255) | `PrecomputeLane` components/compact constructor and generated record APIs | Allowed kinds, nonnegative count, checked multiplication and exact byte geometry; §4 |
| [257–308](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L257-L308) | `PrecomputeBundle` components/compact constructor; `supports`, `missing`, `requireProfile`, `require`; generated record APIs | Root syntax, role sorting/uniqueness, immutable list, inclusion and lookup; §4 |
| [310–330](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L310-L330) | `root`, `objects`, `staging`; constructor and `root()` | Normalized paths, directory creation, selected direct-symlink rejection; §3 |
| [332–360](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L332-L360) | Both `putIntLane` overloads | Array/callback ownership, int encoding and output role; §4 |
| [362–390](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L362-L390) | `putUtf16Lane` | Code-unit range, surrogate acceptance and per-byte callback observations; §4 |
| [392–424](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L392-L424) | Both `putShortLane` overloads | Unsigned array conversion and low-16-bit callback masking; §4 |
| [426–452](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L426-L452) | Both `putLongLane` overloads | Long callback/array observation and explicit order; §4 |
| [454–483](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L454-L483) | `putPrecomputeBundle`, `readPrecomputeBundle`, `openPrecomputeLane` | Canonical text, bounded verified descriptor read, separate lane mapping; §4 |
| [485–527](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L485-L527) | `put(byte[], kind)`, `put(Path, kind)`, `put(InputStream, kind)` | Streamed staging, input ownership, broad arithmetic-failure translation and cleanup; §3 |
| [529–574](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L529-L574) | `contains`, `ref`, `open`, `readSmall` | Existence/size versus full digest validation and caller stream ownership; §§2–3 |
| [576–612](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L576-L612) | `openMapped`, `verify`, package `objectPath` | Mapping delegation, count/digest boolean and hex shards; §§2–3,5 |
| [614–677](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L614-L677) | `encodePrecomputeBundle`, `parsePrecomputeBundle`, `requireSha256` | Text framing/value parsing, canonical re-encoding, overflow boundary and root casing; §4 |
| [679–725](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L679-L725) | Private `PrimitiveLaneInputStream`; fields `elements`, `width`, `bigEndian`, `valueAt`, `byteOffset`; constructor; both `read` overloads; `encodedByte` | Per-byte callback, shift/mask, bulk bounds and cursor/EOF behavior; §4 |
| [727–755](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L727-L755) | `publishTemp`, `requireExactExisting` | Existing-hit/lost-race check; absent native-content dependency; §3 |
| [757–778](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3DirectoryContentStore.java#L757-L778) | `moveCreateOnly`, `newTemp`, `rejectSymlink` | Force then hard link, UUID staging and direct-path policy; §3 |

### M3MappedObject

| Source range | Members | Contract route |
| --- | --- | --- |
| [19–38](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3MappedObject.java#L19-L38) | Class; `reference`, `data`; private constructor | Read-only duplicate, immutable backing obligation and mapping lifetime; §5 |
| [40–62](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3MappedObject.java#L40-L62) | Package `open` | Budget, final-component no-follow, channel size/digest admission, empty buffer; §5 |
| [64–93](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3MappedObject.java#L64-L93) | `reference`, `size`, `byteAt`, `bytes` | Metadata, checked absolute byte reads and independent slices; §5 |
| [95–127](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3MappedObject.java#L95-L127) | `chars`, `shorts`, `ints`, `longs` | Explicit-order primitive views and no normalization/alignment requirement; §5 |
| [129–148](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3MappedObject.java#L129-L148) | `contentEquals`, private `lane` | Exact span mismatch test, long-width bounds and no deterministic close; §5 |

### M3PrecomputeKey

| Source range | Members | Contract route |
| --- | --- | --- |
| [19–52](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeKey.java#L19-L52) | Class; `FORMAT`, `type`, `algorithm`, `algorithmVersion`, `inputs`, `configuration`, `canonical`, `id`; constructor | Operation declarations, defensive collections, canonical bytes and ID; §6 |
| [54–60](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeKey.java#L54-L60) | `type`, `algorithm`, `algorithmVersion`, `inputs`, `configuration`, `id`, `canonicalBytes` | Immutable values and cloned byte export; §6 |
| [62–131](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeKey.java#L62-L131) | `decode`, private `readString` | Strict UTF-8, count/length caps, duplicate/trailing/noncanonical refusal; §6 |
| [133–168](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeKey.java#L133-L168) | Private `encode`, `requireName`; package `writeString` | Ordered binary serialization, NUL rules and UTF-8 replacement caveat; §6 |

### M3PrecomputeResult

| Source range | Members | Contract route |
| --- | --- | --- |
| [12–31](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeResult.java#L12-L31) | Class; `FORMAT`; `State.VALUE/EMPTY`; fields `keyId`, `state`, `output`; private constructor | Positive/negative invariant and ordinal format; §6 |
| [33–53](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeResult.java#L33-L53) | `value`, `empty`, `keyId`, `state`, `isEmpty`, `output` | Factory requirements and EMPTY output failure; §6 |
| [55–72](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeResult.java#L55-L72) | `canonicalBytes` | 37-byte EMPTY and 81-byte VALUE encodings; §6 |
| [74–102](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeResult.java#L74-L102) | `decode` | Exact framing/ordinal/size validation without output admission; §6 |

### M3PrecomputeCache

| Source range | Members | Contract route |
| --- | --- | --- |
| [16–29](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeCache.java#L16-L29) | Class; descriptor limits; `store`, `indexRoot`, `staging`; constructor | Index directory effect, 256-byte/256-KiB caps and path ownership; §7 |
| [31–46](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeCache.java#L31-L46) | `lookup` | Pointer parsing, verified descriptor, matching key ID, absent versus EMPTY; §7 |
| [48–76](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeCache.java#L48-L76) | `MappedBundle` components/compact constructor; `requireLane`; generated record APIs | Frozen lane list, positional descriptor/key geometry and stripped name; §7 |
| [78–95](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeCache.java#L78-L95) | `MappedBundleSet` components/compact constructor; `bundles(String)`; generated record APIs | Nonempty list, nonnegative declared accounting and profile filtering; §7 |
| [97–170](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeCache.java#L97-L170) | `lookupMappedBundles` | Ordered early absence, duplicate IDs, unique-byte accounting and mapping reuse; §7 |
| [172–218](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeCache.java#L172-L218) | `lookupMappedBundle`, `lookupBundle`, `publishBundle` | Separate missing bundle/catalog owners and explicit delegated checks; §7 |
| [220–263](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeCache.java#L220-L263) | `publish` | Descriptor-first write, file lock, pointer equality/conflict and cleanup; §7 |
| [265–280](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/fs/M3PrecomputeCache.java#L265-L280) | Private `pointerPath`, `moveCreateOnly` | Hex pointer shard, atomic move request and ordinary-move fallback; §7 |

All contract routes in the table refer to sections of the
[companion reference](retained-content-store-reference.md). Enum `values` and
`valueOf` are generated for `PrecomputeRole`, `PrecomputeScope`,
`PrecomputeRetention`, `PrecomputeProfile`, `LaneEncoding`, `M3ObjectKind` and
`M3PrecomputeResult.State`; their enumeration/ordinal meaning follows the exact
source declarations. Record accessors, equality, hashCode and toString follow
record components after compact-constructor validation; they do not read files
or prove external payload equality. The three ordinary immutable key/result/
mapping holders do not acquire generated record equality or close semantics.

## 3. Caller and unresolved dependency map

The complete retained production subtree was searched for references to the
eight fs owners. Outside fs itself, the two source callers are the following.
This is a bounded retained-tree scan, not a whole-repository symbol resolution
or a proof about future/manual consumers.

| Public caller | Calls and added obligations | Separate coverage |
| --- | --- | --- |
| [MIndexRegexPrecompute.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexPrecompute.java) | `key`, new cache, lookup, negative-result return, output-kind check, mapped admission, codec decode, request compatibility, output put and result publication; full body read | [Regex persistence contract](retained-regex-reference.md#6-persisted-request-cache-identity-and-publication) |
| [MIndexRegexPrecomputeKey.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexRegexPrecomputeKey.java) | Explicit generic-key construction from UTF-16 expression/runtime hashes, revisions, semantics, ceiling and historical owner literals; full body read | [Owner-literal distinction](retained-regex-reference.md#historical-owner-literals-versus-current-implementation) |

The fs owners otherwise use Java I/O, NIO/path/channel/buffer APIs, SHA-256,
collection/value utilities and callback interfaces, plus each other and these
three unresolved same-package source dependencies:

| Missing retained owner | Visible use | Limit on this reference |
| --- | --- | --- |
| `M3NativeContent` | `M3DirectoryContentStore.requireExactExisting` calls `filesEqual(candidate, existing)` after size/regular-file checks | Exact comparison is delegated; no comparator algorithm, native load or fallback proof |
| `M3FsPrecomputeBundle` | Cache decode/canonical bytes, key/profile/lanes, `requireStore`, `openMapped`, `Lane`, `MappedLane`, `MappedView` | No inferred wire format, full validation, standalone build or external mapped-lane lifetime contract |
| `M3FsPrecomputeCatalog` | Cache `byId(profile).requireCompatible(bundle)` | No inferred profile catalogue, dispatch/failure semantics or validation completeness |

The exact [retained subtree](https://github.com/hsoliwal/M3jdk21/tree/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring)
contains none of those source bodies. It contains the store's *different*
`M3DirectoryContentStore.PrecomputeBundle`; sharing the word bundle and an
object-kind enum does not close the dependency or establish format compatibility.
The AST/shared/typed-precompute packages, canonical IndexDB and source importer
remain separate primary coverage rather than extra owners in this map.

## 4. Test, failure and execution evidence map

No fs test source is physically present under the retained `src/test` tree.
The complete tree listing contains exactly the five AST Java test/proof paths
below. A content-reference scan finds no mention of the eight fs owners in
these bodies. Their presence is not filesystem-contract coverage or evidence
that they compile or ran in this pass.

| Retained test/proof path under `src/test/java/com/synexia/indexstring/ast/` | Relevance to this fs packet |
| --- | --- |
| [MIndexASTCodecBoundedReadProof.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/test/java/com/synexia/indexstring/ast/MIndexASTCodecBoundedReadProof.java) | Different AST codec boundary; no direct fs-owner reference |
| [MIndexASTLcaCodecTest.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/test/java/com/synexia/indexstring/ast/MIndexASTLcaCodecTest.java) | Different AST LCA codec boundary; no direct fs-owner reference |
| [MIndexASTPartialCacheErrorTest.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/test/java/com/synexia/indexstring/ast/MIndexASTPartialCacheErrorTest.java) | Different AST cache/error lifecycle; no direct fs-owner reference |
| [MIndexASTPartialCacheTest.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/test/java/com/synexia/indexstring/ast/MIndexASTPartialCacheTest.java) | Different AST partial cache; no direct fs-owner reference |
| [MIndexASTPrecomputeTest.java](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/vendor/synexia/synexia-indexstring/src/test/java/com/synexia/indexstring/ast/MIndexASTPrecomputeTest.java) | Different AST geometry/query oracles; no direct fs-owner reference |

The [published regex source/evidence map](retained-regex-reference-coverage.md#5-historical-public-oracle-bodies-read-not_run)
separately identifies historical Program and TrigramQuery oracle bodies. It does
not claim direct current v7 persistence/cache/mapping tests for these eight fs
owners. The public [v7 qualification record](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/evidence/QUALIFICATION.json)
reports four bounded AST tests, 21 importer tests, exact byte-intake cases and
three-target replay. It explicitly excludes source-complete receiving build,
actual consumer wiring, cold-image wrong-geometry refusal, AST lifecycle, full
seed/Maven/coverage gates, JNI/native providers, matched JDK/jtreg, performance
and full-family completion. Those are historical reported outcomes, not newly
executed filesystem tests. The intake README's six compiled source owners are
importer owners, not a claim that these eight retained Java files were compiled.

| Contract under review | Evidence available here | Unclosed behavioral gate |
| --- | --- | --- |
| ID/reference admission | Complete bodies and exact hashes | Clone/hex/size/null/ordering oracles and intended identity use |
| Streamed object creation and hard-link reuse | Complete put/publication bodies; comparator call boundary | Comparator/provider closure, equal-byte and forced-conflict races, failed/unsupported links, disk/permissions/cleanup failure |
| Read/verify/mapping | Complete bound/hash/view code | Truncation, mutation, corrupt bytes, symlink/ancestor races, empty files, view geometry and provider/platform lifetime |
| Lane writers and text bundles | Complete callbacks, geometry, profiles and parser | Per-byte callback effects, malformed framing/count/overflow, noncanonical descriptors and cross-lane/application semantics |
| Key/result codecs | Complete encoders/decoders and format accounting | Malformed UTF-8/surrogates, count/size limits, invalid/trailing data and end-to-end producer/consumer compatibility |
| Pointer cache and negative results | Complete lookup/publish/lock/move code | Concurrent publishers, overlapping JVM locks, corrupt/oversized pointers, valid versus conflicting result, crash/recovery durability |
| Bundle-set mapping | Locally visible deduplication/accounting plus delegated calls | Missing bundle/catalog implementations, same-ID inconsistencies, partial failure lifetime and total-memory evidence |
| Regex consumer | Full two caller bodies and published reference | Current complete source/runtime image and actual persistence/codec/request differential tests |

This table lists evidence still required; it does not report those cases as
failures or invent tests that exist. No standalone build/test command is given
for an incomplete source slice. All repository code/test/script/policy execution
was NOT_RUN here; static source, hash, link and editorial checks are different.

## 5. Public custody and provenance

All eight current target bodies exactly match their rows in the public
[v7 export](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/export.tsv).
That manifest has 22 nonlicense Java payloads (six original root regex owners,
eight fs and eight typed-precompute owners), plus LICENSE/NOTICE. It records
source revision `80d288951148548ef6aa4c95df9cb31a438aac16`, exact original paths,
`Apache-2.0` / `APACHE_SOURCE`, and manifest root
`d1889319f72f5e3ab9fb4804e27e8ec58d2bfc75e112539f5322f5f95e3d658b`.
The target path is `m3/vendor/synexia/` plus the original path. No private donor
implementation was read or imported for this documentation.

The public [SOURCE_CUSTODY.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/SOURCE_CUSTODY.json)
contains matching byte counts, Git blobs and SHA-256s for each of the eight rows.
Its status is bounded production-packet custody, with `sourceCompleteClosure=false`,
`javaBaseRuntimeQualified=false` and per-source `runtimeAdmission=false`.
Its statements about historical source observations remain attributed receipt
claims; they are not a fresh verification of the donor repository.

[PROVENANCE.json](https://github.com/hsoliwal/M3jdk21/blob/363c02caa2e5af1df9460b927ecd78f6a9dbcefb/m3/synexia-import/intakes/regex-precompute-v7-20261007/PROVENANCE.json)
binds the export and custody receipt, preserves source attribution and separate
OpenJDK/third-party notices, and describes source custody/publication planning.
Its README input identifies the historical packet input, not the later edited
intake README. Later regex Program/codec successors are documented in the
[regex lineage map](retained-regex-reference-coverage.md#6-provenance-map-and-intentional-successor-evolution);
no successor replacement of these eight fs payloads is needed to reconcile
their current bodies with the original v7 export.

| Public evidence file at source pin | Bytes | Git blob SHA-1 | SHA-256 |
| --- | ---: | --- | --- |
| `export.tsv` | 8547 | `7bc78e7c6f65ccd1f56cf6f06907512a5a50fa36` | `ae72faedec7913ee25e74f4592b06bc9b6e31e80800587df1629619f91b20c9e` |
| `SOURCE_CUSTODY.json` | 29478 | `dc3b25ff91f9f32c66e9a9989992acf0d6bc9721` | `97df0a295c9bb3c32ff71a3df6380c5beceb21e4528c218baa12cd4c39df0fb4` |
| `PROVENANCE.json` | 1518 | `1af65edd0f61261d6bcce7200ee1b3860c623fe9` | `dfa3e4023a370da8a0b6f406a2391b2d1c24f86915e785f9287cab10d111e014` |
| `evidence/QUALIFICATION.json` | 2048 | `4d54c1449f4189264d84f1eda27a9bacfd591370` | `597e119d7fd3375a0c6fd8ddf88c1f9a6c32932c01ed76f32e00ea14023a1dab` |

The manifest-root field is a recorded protocol identity, separate from the raw
export-file SHA-256 above. Matching retained bytes to these records is an exact
identity check. It does not rerun the importer or establish the correctness or
authenticity of historical execution receipts.

## 6. Policy/build reconciliation and final disposition

Read-only policy sources include root AGENTS instructions, vendor custody,
delivery/reuse/borrowing, both porting-invariant documents, relevant name-map
policy/receiving-sequence fields, the public full-family pin/catalogue and
current-full-borrow README/String-phase/pin/phase-order/status/gates. They keep
String qualification active, later phases waiting, runtime source ownership in
the target and reusable mechanics in the canonical Synexia lineage. No source,
policy, recipe, license or provenance file is changed by this reference.

The complete retained-tree listing and inspected `m3/pom.xml`,
`m3/synexia-import/pom.xml`, `m3/ports/indexstring/pom.xml` and `m3/build.sh`
support the bounded source-set conclusion: the retained fs package has no
standalone descriptor; the reactor/importer, compatibility bridge and core shell
build are separate. These are static file observations, not an executed
effective-POM graph or a claim about every possible external/manual harness.

The previous fs row in `packages/custody-01.md` was PATH_ONLY, and the retained
regex reference intentionally treated fs as supporting dependencies. This packet
adds primary contracts for these eight files only. It does not overwrite earlier
inventory history, absorb AST/shared/typed-precompute ownership, or substitute
for IndexDB, source-recipe admission or a canonical runtime storage reference.

| Check | Disposition |
| --- | --- |
| Complete physical fs listing and source identity | Eight paths checked at source pin and docs stack head; source bytes/hash match |
| Primary source/member coverage | All eight full bodies and nested/support groups read and routed |
| Callers and missing dependency boundary | Two direct retained non-fs callers read; three absent fs owners explicitly delimited |
| Tests/evidence | No fs tests in retained test tree; bounded public historical records identified |
| Compilation, repository unit tests, policy/provenance verification or recipe execution | NOT_RUN |
| JNI/native/providers, JDK build/jtreg and installed-image/platform qualification | NOT_RUN |
| Crash/durability/concurrency/security qualification and resource benchmarks | NOT_RUN |
| Source-complete module, semantic equivalence, runtime admission or family completion | Not established |

Publication or successful Markdown checks do not alter this disposition. Any
hosted CI outcome belongs to its exact commit and must be reported separately.
