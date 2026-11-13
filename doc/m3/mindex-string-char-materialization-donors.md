# MIndex String char materialization — donor review

Date: 2026-10-02

## Decision

`java.lang.String` remains a facade over canonical `MIndexString` storage when the experimental
M3 runtime is enabled. Joined values remain atom/range descriptors. A real contiguous `char[]`
is allocated only when a Java/JNI compatibility contract explicitly requests one.

The reviewed donor projects reinforce the same separation:

| Donor | Reusable pattern | M3JDK decision |
|---|---|---|
| Protocol Buffers `RopeByteString` | concatenate without copying; tree/leaf representation; balance depth against length; copy only small leaves | algorithm reference for future MIndex join balancing; do not copy source |
| Abseil `Cord` | share chunks/external memory; cheap sub-ranges; explicit copy-to-string/span boundary | storage/lifetime reference; keep GC-visible MIndex ownership semantics |
| Netty `CompositeByteBuf` | indexed component composition; bulk copy only at explicit boundary | direct analogue for segment-wise MIndex materialization |
| Chronicle Bytes | mapped/off-heap access with explicit resource ownership | lifetime/view reference for mapped MIndex atoms |
| Apache Spark `UTF8String` | base-object + address/offset/length text descriptor, cached facts, explicit copy boundary | addressed-text reference; preserve MIndex UTF-16 semantics instead of Spark code-point semantics |
| OpenJDK String/StringUTF16/StringConcatHelper/JNI | exact Java UTF-16, Compact String and JNI materialization contract | semantic authority for the modified-JDK route |

Exact repository commits and licenses are recorded in
`m3/runtime-integration/FOSS_REUSE_DECISION.tsv`.

## Current implementation

The canonical MIndex representation is unchanged.

`MIndexString.getChars(srcBegin, srcEnd, dst, dstBegin)` now treats the caller-owned destination
as the single flattening boundary:

- LOCAL/LATIN1 delegates to the existing JDK bulk inflater;
- LOCAL/UTF16 delegates to the existing JDK UTF-16 bulk copier;
- mapped lexicon atoms are traversed sequentially from one computed address;
- JOINED storage finds the first segment once and then walks overlapping segment ranges
  sequentially, recursively bulk-copying each scalar atom.

Therefore `String.toCharArray()` allocates exactly the required Java `char[]` and does not first
materialize a joined String or Compact-String byte array.

HotSpot exposes the equivalent `java_lang_String::copy_chars` /
`java_lang_MIndexString::copy_chars` primitive. JNI `GetStringChars` and `GetStringRegion`
use that primitive instead of repeatedly calling `char_at` for each code unit.

## Why the JNI array-concatenation examples are a boundary, not identity

JNI can allocate a new `jcharArray` and sequentially copy any number of independent source
arrays into it. That is useful when a true Java array is required, but it necessarily creates a new
contiguous JVM array. It therefore cannot be the canonical MIndex concat representation.

MIndex keeps:

```text
String
  -> MIndexString
       -> atom/range descriptors
       -> mapped or VM-local scalar payloads
```

and performs:

```text
atom/range sequence
  -> one requested destination char[]
```

only at the compatibility boundary.

## Follow-up donor application

Protocol Buffers' rope balance rule is a strong candidate for very large repeated concat chains.
That should be evaluated as a separate representation/performance change after this boundary is
compiled and differentially verified. It must preserve the existing MIndex canonical atom/range
identity and all String/JNI contracts.
