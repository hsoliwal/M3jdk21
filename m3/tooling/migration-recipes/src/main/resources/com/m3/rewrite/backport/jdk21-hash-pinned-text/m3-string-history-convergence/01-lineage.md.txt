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
| `33ebec8fa64c215bf8bfa0dc4ec66085f5500a52` | Cache immutable per-atom metrics, bit signal and character facts together | M3 fixed-size facts prepare once on canonical owner/range identity |
| `46d92ec424862459e767b1d6df5b329e3241cdf3` | Compile reusable search-plan prefix/skip/hash/signal metadata | M3 length-proportional pattern metadata lives in separately bounded `M3StringSearchPrecompute` |
| `e649343956a2ea21609713f7a62954fefe757468` | Fail fast rope search from precomputed bit facts | M3 candidate facts may prove absence but never prove equality |
| `bb4033de627c36e371430095a5db1122d30cc5a0` | Route String-facade search through cached facts | `java.lang.String` M3 routes apply canonical filters before exact search |
| `0cd5b787de63fb4c579a0a5d449087ee88503ae0` | Reuse shared-reference range facts without materializing text | M3 owner-local exact-coordinate range facts remain bounded and payload-free |
| `6b21032aaee902122d3dfeb027b270acc23de67b` | Retain canonical patterns and reuse operation facts | repeated M3 pattern search may reuse weakly keyed prepared metadata |
| `86d729240243314bce305d1c158a30ecf2024a28` | Extend prefix-Z facts with borders/periods | O(n) analyses remain separate internal budgeted lanes, never M3String fields |
| `10b4894505a6f6306f340c5ff3ef17270706b2c6` | Add exact UTF-16 Manacher palindrome facts | length-proportional palindrome lanes remain caller/cache owned |
| `d6308bff7e3a55e77c4dc45c4f47187acb2e523b` | Exact suffix-decision precompute with Java/JNI validation | an early decision is authoritative only when mathematically complete and independently validated |
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
9. VM-local native scalar owners are weakly canonicalized: live M3 coordinates/DAGs own lifetime; dead native blocks are reclaimed through the local reference queue.
10. Every Java byte[]/char[] compatibility shadow, including the shared empty VM sentinel, is created through the JNI shadow boundary.

## Search/precompute convergence absorbed

The M3JDK target now follows the donor history rather than a single donor snapshot:

- `M3StringFacts` retains fixed-size character, Java-hash, prefix/suffix, bigram/trigram,
  code-point and whitespace facts on canonical owner/range identity.
- `M3StringSearchPrecompute` is a separate bounded 256-slot weak-owner cache for at most
  8,192-unit pattern KMP metadata. It retains no String, char[], byte[] or M3String payload.
- `String.indexOf`, bounded `indexOf`, `lastIndexOf`, starts/suffix filters and character
  searches consume these facts only as safe pruning/preparation; exact UTF-16 comparison remains
  final truth.
- Length-proportional donor analyses such as Z, Manacher, position masks, LCP and regex plans remain
  separate internal budgeted lanes and are not fields of `M3String`, `M3StringOwner` or
  `M3StringFacts`.

## Array projection boundary

M3JDK now treats the ordinary Java arrays exactly as compatibility/output projections:

- `String.toCharArray()` obtains its caller-owned `char[]` directly from `M3String`;
- public `String.getBytes(...)` dispatches to `M3String.encode` when the String is M3-backed;
- UTF-8 uses the canonical `utf8Length` fact to allocate the final byte array exactly and streams
  UTF-16 units directly, including split pairs and JDK replacement semantics for unpaired surrogates;
- US-ASCII and ISO-8859-1 allocate from canonical code-point geometry and emit one replacement byte
  per unmappable code point;
- other Charsets receive one caller-owned UTF-16 projection and the stock `CharsetEncoder`
  replacement contract;
- no produced `byte[]` or `char[]` becomes canonical M3 String storage.

### No-replacement byte paths

The JDK-internal no-replacement helpers also stay on M3 storage:

- `String.getBytesUTF8NoRepl` uses `M3String.encodeUtf8NoRepl`;
- `String.getBytesNoRepl` uses `M3String.encodeNoRepl`;
- UTF-8 preserves the existing unpaired-surrogate `IllegalArgumentException` with
  `UnmappableCharacterException` cause at the internal helper;
- the checked `getBytesNoRepl` wrapper continues to expose the original
  `CharacterCodingException` family;
- these paths do not manufacture a Compact-String shadow before producing the caller-owned bytes.
