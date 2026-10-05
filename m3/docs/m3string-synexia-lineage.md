# M3String Synexia donor lineage

Status: runtime-port authority supplement. The operational mapping remains `m3/docs/name-mapping.json`.

Synexia is the donor/reference repository. M3JDK deliberately uses M3 target names. This file records
the source history used to reconstruct semantics so a future sync does not copy one accidental
snapshot or reuse donor identifiers as target ABI.

| Synexia commit | Donor transition | M3JDK consequence |
| --- | --- | --- |
| `8b202d3c10617d67f45247ccd81a21b9c5332a08` | Inline scalar handles and share immutable token spans; `MIndexString` converges to owner + packed long coordinate | `java.lang.M3String` has exactly `owner` + `value` instance state |
| `c3c67a433e1689d42bc68720290b7ca5f54c3313` | Materialize canonical tuple reference DAG | `M3StringTuple` owns persistent child coordinates; no per-value segment/offset/end arrays |
| `40d66f6edd0a9cf3e6b4545058c1ad54f6cd03bc` | Route concat/compose/repeat through canonical tuple DAG | M3 concat/repeat create/reuse tuple owners; substring is a coordinate range |
| `d08ee60be27b60a951a55a2b472d515bf142f8bf` | Compose/reuse canonical text precomputation without retaining a second spelling | M3 precompute is owner/range keyed derived metadata only |
| `b9d35ce3c3c266fb3641352a07903563a3a86b81` | Retain canonical fact bundles in canonical tuple body | `M3StringOwner` lazily retains internal fixed-size `M3StringFacts` |
| `f2f15f1aaa147cf1af556b0c148954178f971de6` | Introduce native UTF-16/UTF-8 shadows explicitly outside canonical MIndexString | M3JDK byte/char arrays are compatibility shadows; `M3String` never retains them |

Current donor pin reviewed for this port: `hsoliwal/com.synexia@9963cc08ff13922b92a0e3db7c30f56fddacba7d`.

## Name map

| Synexia donor/reference | M3JDK target |
| --- | --- |
| `MIndexString` | `java.lang.M3String` |
| `IndexStringTuple` | `java.lang.M3StringTuple` |
| native scalar resolver/interner responsibility | `java.lang.M3StringAtom` + `java.lang.M3StringPool` |
| canonical String facts/precomputation | `java.lang.M3StringFacts` + internal M3 kernels such as `jdk.internal.mindex.M3TQ` |
| UTF-16 native shadow | `M3String.nativeCharShadow` + libjava JNI implementation |
| mapped String backing | `jdk.internal.mindex.M3MappedStringBacking` |
| backing contract | `jdk.internal.mindex.M3StringBacking` |

This mapping is semantic. It does not imply source-level binary compatibility or a blanket prefix
rename. A donor responsibility may split across several target owners when the JDK bootstrap/VM
boundary requires it.

## Invariants carried forward

1. Canonical text identity is owner + coordinate.
2. Scalar payload is native/mapped canonical storage; Java text arrays are not canonical payload.
3. Composition is a persistent canonical reference DAG.
4. Substrings are owner/range coordinates.
5. Precompute is entirely implementation-internal and never a public Java String contract.
6. Precompute cannot become a second spelling store.
7. JNI byte/char/UTF arrays and buffers are shadows/materializations only.
8. Absence or eviction of precompute cannot alter Java String semantics.
