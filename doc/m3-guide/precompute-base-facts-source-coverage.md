<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Precompute base-fact source and member coverage

[Guide](README.md) · [Contract reference](precompute-base-facts-reference.md) · [Existing 23-file map](precompute-source-coverage.md)

## Evidence boundary and history

Date: **2026-10-15 UTC**. Repository: public `hsoliwal/M3jdk21` only.
Primary source pin: `db49925d43d4854707911868e603426db7055664` (draft #905 head
at inspection). All five primary bodies, two tests, POM/README and five selected
support bodies match the earlier `1de629d26ea82d2729a5993bfc88099f83221923` packet
in both Git blob and raw-byte hashes. A later comparison to draft #908 head
`510dfa27b0679ce33348140afc57b558b8853802` shows one documentation-only commit,
six changed guide paths and no source/policy change. It does not alter this source
pin or grant publication/merge authority.

The earlier [23-file map](precompute-source-coverage.md) correctly recorded
**23 body-read routes, five selected-method overviews and eighteen dedicated
scoped references**. This five-owner reference adds systematic declared-member
routing; it discovers no new production source and recounts no batch/regex/CPU
owner. Its dated additive navigation supersedes only the five overview-level
member gaps once the reference is reviewed and published. Earlier source dates,
limits and NOT_RUN statements remain historical evidence. No package, module,
project, repository or receiving-phase completion follows.

All **965 lines** in the five primary bodies were inspected: 134 + 417 + 83 +
314 + 17. The complete source subtree receipt has 54 entries and no
`module-info.java`, `package-info.java`, `M3TextMetricsTest.java`,
`M3HistogramTest.java` or `M3ProgressTest.java`. This is a scoped filename/listing
statement, not proof that no test elsewhere exercises these types.

The accompanying reference owns five files, including one package-private
implementation class and the nested workspace/anonymous implementation. A source
file count is not a count of public APIs, packages or independently compiled
modules. All builds, tests, examples, scripts, recipes, policy/provenance
verifiers, workflows, native loads, benchmarks and runtime qualification are
**NOT_RUN in this documentation authoring/review pass**. Hosted CI is separate
exact-commit evidence, not established by this static review. Local text/hash/link checks are editorial
checks and execute no repository code.

## Exact primary source identities

`git_blob_sha1` identifies Git's blob object (header plus content); `sha256` below
hashes only the raw UTF-8 file bytes. They are not interchangeable. Source links
are pinned; all listed primary files are below
`m3/ports/precompute/src/main/java/com/m3/precompute/`.

| Source path / body | Lines | Git blob SHA-1 | Raw SHA-256 |
| --- | ---: | --- | --- |
| [m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextMetrics.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextMetrics.java) | 134 | `c64044be24be39dcb2f98b73c78f47d0e1bc37b0` | `5f42c0983f206118c5493cd9ecc2e9b582ca3e339de83972dabe8d775a89ee8a` |
| [m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java) | 417 | `9dae8c89566faf46fe93eb93abce2dbf75af21f9` | `22ef43339c76cdb80e7972ebbcff823a34454624f50b2cd4059f4c0d751f976f` |
| [m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Histogram.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Histogram.java) | 83 | `ad0c7d46aa933e553a96c4576e9ff6d85a112f93` | `eddd09b577fc0cba2dc35126e9bef59bf2d376dc539d4fc24f41674094dd03b5` |
| [m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java) | 314 | `0ce6b26740c698480fbfbe81323c1babc0517e6e` | `5386fb982ab12e566c51f374f115e550f7dc9bc2332763a306568616982abcbd` |
| [m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Progress.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Progress.java) | 17 | `0f2b071e55f6a092ff3841e13db8b86520339126` | `3995af4d1447b3d5f1df9fafbae068c278c115e8f5800d6dbe6fec45dc01dee8` |

## Every declared member and generated surface

Rows enumerate declared constants/state, constructors, methods/helpers and nested
owners. Grouped field rows list every field name; line links identify each
declaration. Signatures omit parameter names except the long private Signals
constructor for readability. No generated/inherited method is counted as an
explicit source declaration.

### M3TextMetrics

[Full contract](precompute-base-facts-reference.md#metrics-components-construction-and-composition).

| Kind | Complete declared surface | Source lines | Contract route |
| --- | --- | --- | --- |
| record components / generated accessors | `utf16Length(); utf8Length(); codePointCount(); unpairedSurrogateCount(); javaHashCode(); hash31Power(); firstUtf16Unit(); lastUtf16Unit()` | [13–21](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextMetrics.java#L13-L21) | Metrics component table; compiler-generated methods are separate from declarations |
| private static final field | `UTF8_REPLACEMENT_BYTES` | [23–24](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextMetrics.java#L23-L24) | Encoder replacement width |
| public static final field | `EMPTY` | [26–27](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextMetrics.java#L26-L27) | Canonical zero record |
| public compact constructor | `M3TextMetrics` | [29–43](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextMetrics.java#L29-L43) | Local validation, not semantic consistency |
| public static method | `of(CharSequence)` | [45–91](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextMetrics.java#L45-L91) | Live scan and byte/hash arithmetic |
| public static method | `combine(M3TextMetrics,M3TextMetrics)` | [93–119](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextMetrics.java#L93-L119) | Operand identity, stored-fact seam correction |
| public method | `isEmpty()` | [121–123](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextMetrics.java#L121-L123) | Stored length only |
| public method | `isWellFormedUtf16()` | [125–127](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextMetrics.java#L125-L127) | Stored unpaired count only |
| private static helper | `utf8Bytes(char)` | [129–133](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextMetrics.java#L129-L133) | Non-surrogate widths |

Generated surface: eight component fields/accessors, canonical record machinery, and component-based `equals(Object)`, `hashCode()` and `toString()`. The compact constructor is explicitly declared above. The generated record hash differs from the stored String-style `javaHashCode` component. Generated methods have no handwritten method bodies here.

### M3TextSignals

[Full contract](precompute-base-facts-reference.md#signals-stable-facts-and-candidate-boundaries).

| Kind | Complete declared surface | Source lines | Contract route |
| --- | --- | --- | --- |
| public static final constants | `ASCII; LATIN1; BLANK; ALL_LETTERS; ALL_DIGITS; HAS_LETTER; HAS_DIGIT; HAS_UPPER; HAS_LOWER; WELL_FORMED_UTF16` | [16–25](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L16-L25) | Ten flags and exact values/predicates |
| private static final constants | `HASH_BASE; MIN_HASH_LANES` | [27–28](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L27-L28) | 1,000,003 polynomial base; 16 lanes |
| private final fields | `metrics; characters; bigrams; trigrams` | [30–33](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L30-L33) | Geometry and three histograms |
| private final fields | `hash64; power64; presence64; prefix2; suffix2` | [34–38](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L34-L38) | Hash/composition/filter state |
| private final fields | `letters; digits; whitespace; uppercase; lowercase` | [39–43](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L39-L43) | Code-point classification counts |
| private final fields | `flags; simHash64; minHash` | [44–46](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L44-L46) | Eagerly computed outputs |
| private constructor | `M3TextSignals(metrics,characters,bigrams,trigrams,hash64,power64,presence64,prefix2,suffix2,letters,digits,whitespace,uppercase,lowercase)` | [48–91](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L48-L91) | Stores state, derives flags and both sketches |
| public static method | `compile(CharSequence)` | [93–146](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L93-L146) | String/toString preparation |
| public static method | `combine(M3TextSignals,M3TextSignals)` | [149–205](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L149-L205) | Histograms and seam composition |
| public method | `metrics()` | [207–209](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L207-L209) | Stored record reference |
| public method | `flags()` | [211–213](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L211-L213) | Stored bit mask |
| public method | `hasAll(int)` | [215–217](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L215-L217) | Mask containment including zero/unknown bits |
| public method | `hasAny(int)` | [219–221](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L219-L221) | Mask intersection including zero/unknown bits |
| public method | `contentHash64()` | [223–225](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L223-L225) | Stored polynomial hash |
| public method | `presence64()` | [227–229](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L227-L229) | Stored lossy presence mask |
| public method | `characterCount(char)` | [231–233](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L231-L233) | Exact UTF-16-unit multiplicity |
| public method | `bigramCount(char,char)` | [235–237](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L235-L237) | Exact contiguous unit-pair multiplicity |
| public method | `trigramCount(char,char,char)` | [239–241](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L239-L241) | Exact contiguous unit-triple multiplicity |
| public method | `mayContain(M3TextSignals)` | [243–247](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L243-L247) | Length/presence candidate test |
| public method | `mayEqual(M3TextSignals)` | [249–254](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L249-L254) | Length/two-hash candidate test |
| public method | `isAnagramOf(M3TextSignals)` | [256–258](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L256-L258) | Exact unit multiset equality |
| public method | `editLowerBound(M3TextSignals)` | [266–273](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L266-L273) | Maximum of length and rounded q-gram bounds |
| public method | `simHash64()` | [275–277](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L275-L277) | Stored sketch |
| public method | `simHashDistance(M3TextSignals)` | [279–281](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L279-L281) | 64-bit Hamming distance |
| public method | `minHash()` | [283–285](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L283-L285) | Defensive 16-lane clone |
| public method | `estimatedJaccard(M3TextSignals)` | [288–298](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L288-L298) | Equal-lane fraction and explicit empty cases |
| public method | `histogramPayloadBytes()` | [300–302](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L300-L302) | Only histogram primitive element storage |
| public method | `labels()` | [304–318](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L304-L318) | Ordered unmodifiable flag-name list |
| private helper | `computeSimHash()` | [320–338](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L320-L338) | Highest-q features; frequency-weighted votes; zero ties |
| private helper | `computeMinHash()` | [340–355](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L340-L355) | Highest-q distinct features; unsigned minima |
| private static helper | `packedPrefix2(CharSequence)` | [357–361](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L357-L361) | Zero/one/two leading units |
| private static helper | `packedSuffix2(CharSequence)` | [363–367](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L363-L367) | Zero/one/two trailing units |
| private static helper | `composePrefix2(M3TextSignals,M3TextSignals)` | [369–375](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L369-L375) | Reuse left two or make seam pair |
| private static helper | `composeSuffix2(M3TextSignals,M3TextSignals)` | [377–383](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L377-L383) | Reuse right two or make seam pair |
| private static helper | `bigram(int,int)` | [385–387](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L385-L387) | 32-bit packed unit key |
| private static helper | `trigram(int,int,int)` | [389–391](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L389-L391) | 48-bit packed unit key |
| private static helper | `addSignal(long,char)` | [393–398](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L393-L398) | ASCII-only folding and two presence bits |
| private static helper | `mix64(long)` | [400–407](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L400-L407) | Wrapping xor/shift/multiply mixer |
| private static helper | `mix32(int)` | [409–416](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L409-L416) | Wrapping xor/shift/multiply mixer |

Totals: ten public constants, two private constants, seventeen private instance fields, one private constructor, **21 explicit public methods**, eleven private helpers. Object identity methods are inherited, not signal value comparisons.

### M3Histogram

[Full contract](precompute-base-facts-reference.md#histogram-internal-ownership-and-arithmetic).

| Kind | Complete declared surface | Source lines | Contract route |
| --- | --- | --- | --- |
| package-private final class | `M3Histogram` | [7–83](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Histogram.java#L7-L83) | Internal dependency only |
| package-visible final fields | `keys; counts` | [8–9](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Histogram.java#L8-L9) | References final, elements mutable |
| private constructor | `M3Histogram(long[],int[])` | [11–14](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Histogram.java#L11-L14) | Stores arrays directly |
| package-private static method | `of(long[])` | [16–30](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Histogram.java#L16-L30) | Sorts input in place; retains fresh arrays |
| package-private method | `plus(M3Histogram)` | [32–52](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Histogram.java#L32-L52) | Sorted checked merge and trim copies |
| package-private method | `l1(M3Histogram)` | [54–69](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Histogram.java#L54-L69) | Long multiset L1 |
| package-private method | `count(long)` | [71–74](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Histogram.java#L71-L74) | Binary search; absent is zero |
| package-private method | `same(M3Histogram)` | [76–78](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Histogram.java#L76-L78) | Key/count array equality |
| package-private method | `payloadBytes()` | [80–82](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Histogram.java#L80-L82) | 12 bytes per distinct key |

### M3EditDistancePlan

[Full contract](precompute-base-facts-reference.md#edit-distance-plan-and-workspace).

| Kind | Complete declared surface | Source lines | Contract route |
| --- | --- | --- | --- |
| private static final constants | `MAX_PATTERN_UNITS; MAX_SOURCE_UNITS; MAX_DP_CELLS; MYERS_MAX_UNITS` | [15–18](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L15-L18) | 1,048,576; 1,048,576; 64,000,000; 63 |
| private final fields | `pattern; myersUnits; myersMasks; myersHighBit; myersDomainMask` | [20–24](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L20-L24) | Private copied pattern / optional bit-vector state |
| private constructor | `M3EditDistancePlan(char[],char[],long[],long,long)` | [26–37](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L26-L37) | Stores prepared private arrays |
| public static method | `compile(CharSequence)` | [39–68](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L39-L68) | Copy and optional 1–63-unit masks |
| public method | `patternLength()` | [70–72](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L70-L72) | Stored unit length |
| public method | `usesMyers()` | [74–76](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L74-L76) | Non-null unit table |
| public method | `primitivePayloadBytes()` | [78–83](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L78-L83) | Plan primitive arrays only |
| public method | `workspace()` | [85–87](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L85-L87) | Allocate independent identity-bound rows and pattern facts |
| public method | `distance(CharSequence)` | [89–91](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L89-L91) | New workspace convenience |
| public method | `distance(CharSequence,Workspace,M3Progress)` | [93–102](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L93-L102) | Validation and empty/Myers/full-DP dispatch |
| public method | `within(CharSequence,int)` | [104–106](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L104-L106) | New workspace convenience |
| public method | `within(CharSequence,Workspace,int,M3Progress)` | [108–130](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L108-L130) | Validation, negative filters, Myers/band dispatch |
| private helper | `myersDistance(CharSequence,M3Progress)` | [132–160](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L132-L160) | Single-word recurrence and callbacks |
| private helper | `mask(char)` | [162–165](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L162-L165) | Binary-search unit mask |
| private helper | `dynamicDistance(CharSequence,Workspace,M3Progress)` | [167–202](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L167-L202) | Two rows; normalizes stored previous row on result path |
| private helper | `bandedWithin(CharSequence,Workspace,int,M3Progress)` | [204–245](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L204-L245) | Full-row sentinel fill; narrow recurrence; early rejection |
| private helper | `ensureWorkload(int)` | [247–251](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L247-L251) | Explicit source length range |
| private helper | `requireFullDpBudget(int)` | [253–258](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L253-L258) | Checked full recurrence estimate |
| private helper | `requireBandedBudget(int,int)` | [260–268](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L260-L268) | Checked band recurrence estimate |
| public override | `length()` | [271–273](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L271-L273) | CharSequence unit length |
| public override | `charAt(int)` | [276–278](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L276-L278) | Checked unit access |
| public override | `subSequence(int,int)` | [281–284](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L281-L284) | Checked range then String materialization |
| public override | `toString()` | [287–289](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L287-L289) | String materialization |
| public static final nested class | `Workspace` | [291–313](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L291-L313) | One workspace owner; no public constructor |
| private final workspace fields | `owner; previous; current; patternSignals` | [292–295](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L292-L295) | Identity and mutable rows / cached pattern facts |
| private workspace constructor | `Workspace(M3EditDistancePlan)` | [297–302](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L297-L302) | Two arrays and compile(owner), even for Myers |
| public workspace method | `previousRowCopy()` | [304–306](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L304-L306) | Defensive diagnostic clone, not universal last-row guarantee |
| private workspace helper | `require(M3EditDistancePlan)` | [308–312](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L308-L312) | Owner identity refusal |

Totals: four constants, five outer instance fields, one private outer constructor,
**13 explicit public outer methods**, seven private outer helpers; nested Workspace
has four fields, one private constructor, one public method and one private helper.
Separately inherited CharSequence defaults are [isEmpty, lines 100–102](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/src/java.base/share/classes/java/lang/CharSequence.java#L100-L102),
[chars, lines 148–179](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/src/java.base/share/classes/java/lang/CharSequence.java#L148-L179) and
[codePoints, lines 198–254](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/src/java.base/share/classes/java/lang/CharSequence.java#L198-L254).
`CharSequence.compare` is static, not inherited. The plan does not implement
Comparable; ordinary Object equality/hash/monitor surface is not a new declared
plan method or automatic workspace lock. The supporting interface's iterators are
not additional precompute owners.

### M3Progress

[Full contract](precompute-base-facts-reference.md#progress-defaults-and-actual-caller-cadence).

| Kind | Complete declared surface | Source lines | Contract route |
| --- | --- | --- | --- |
| public interface / anonymous implementation | `M3Progress; anonymous body of NONE` | [5–17](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Progress.java#L5-L17) | Anonymous body declares no additional member |
| implicit public static final field | `NONE` | [6–6](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Progress.java#L6-L6) | One no-op anonymous instance |
| public default method | `begin(String,long)` | [8–8](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Progress.java#L8-L8) | No-op; no validation or state |
| public default method | `subTask(String)` | [9–9](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Progress.java#L9-L9) | No-op; no selected client invokes it |
| public default method | `worked(long)` | [10–10](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Progress.java#L10-L10) | No-op; no implicit accounting |
| public default method | `checkCanceled()` | [11–11](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Progress.java#L11-L11) | No-op; caller defines cancellation |
| public default method | `done()` | [12–12](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Progress.java#L12-L12) | No-op; not automatic release |
| public static method | `none()` | [14–16](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3Progress.java#L14-L16) | Returns NONE identity |

## Read test methods and limits

These two complete test bodies contain **seven `@Test` methods**. READ is the
evidence status; none was executed here. Supporting test helpers were also read.
Finite agreement with another implementation is not exhaustive semantic proof.

| Test method | Pinned lines | What its assertions actually inspect | Limits |
| --- | --- | --- | --- |
| `M3TextSignalsTest.compositionMatchesFlatCompilationIncludingSurrogateSeams` | [12–36](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/test/java/jdk/m3/precompute/M3TextSignalsTest.java#L12-L36) | 11-string × 11-string composition matrix including empty and surrogate fragments; uses same helper | Does not directly compare all unit/q-gram counts or independently establish classifications |
| `M3TextSignalsTest.randomizedCompositionRetainsExactHistogramsAndSketches` | [39–70](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/test/java/jdk/m3/precompute/M3TextSignalsTest.java#L39-L70) | 500 seeded strings with lengths below 48; one random split; same helper plus all character and bigram counts over its 12-unit alphabet | Not exhaustive; no direct all-trigramCount assertion or hostile/mutable inputs |
| `M3TextSignalsTest.editLowerBoundNeverExceedsIndependentExactDistance` | [73–89](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/test/java/jdk/m3/precompute/M3TextSignalsTest.java#L73-L89) | 1,500 seeded pairs below 14 units over seven units; lower bound ≤ independent recurrence and ≥ length gap | Finite lower-bound comparison, not all edit/encoding domains |
| `M3TextSignalsTest.similaritySketchesAreSelfConsistentButNotSemanticAuthority` | [92–109](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/test/java/jdk/m3/precompute/M3TextSignalsTest.java#L92-L109) | Self SimHash/Jaccard; one near-result range; two mayEqual negatives; three pair-facade fields | No arbitrary collision, mayContain, isAnagramOf or statistical quality proof |
| `M3EditDistancePlanTest.deterministicExamplesAndThresholds` | [11–23](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/test/java/jdk/m3/precompute/M3EditDistancePlanTest.java#L11-L23) | kitten/sitting, sitten threshold and empty pattern examples | Selected deterministic values, no callback assertions |
| `M3EditDistancePlanTest.randomizedMyersAndDynamicPathsMatchIndependentDp` | [26–46](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/test/java/jdk/m3/precompute/M3EditDistancePlanTest.java#L26-L46) | 1,000 seeded pairs of lengths below 90, six-unit alphabet; distance and eight thresholds {0,1,2,3,5,8,16,expected} | No exhaustive 63/64, ceiling, large-band or arbitrary CharSequence matrix |
| `M3EditDistancePlanTest.workspaceOwnershipAndBudgetsFailClosed` | [49–66](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/test/java/jdk/m3/precompute/M3EditDistancePlanTest.java#L49-L66) | Foreign workspace; 1,048,577-unit pattern/source; 20,000×20,000 full-DP refusal | No negative-threshold or banded-work refusal assertion; no callback/concurrency/resource measurement |

TextSignals' `same` helper (111–120) compares metrics, flags, 64-bit content hash,
presence mask, SimHash, MinHash, labels and histogram payload bytes; matching
payload size does not establish every histogram count. Its `randomString`
(122–128), independent two-row `exact` (130–148) and `printable` (150–152) helpers
support those finite loops. EditDistancePlan's `randomString` (68–74) and
independent `exact` (76–94) support its random matrix. No direct-record validation,
internal-array corruption, allocation failure, custom toString mismatch, throwing
monitor, interruption, reentrant/concurrent call, full diagnostic-row freshness
or complete inherited-CharSequence test is supplied by these bodies.

## Selected caller and build routing

This is a bounded caller sample. Supporting sources are not new primary owners
and are not being upgraded to full contract coverage by this reference.

| Body / exact path within module | Relevant lines | Observed relationship and existing route |
| --- | --- | --- |
| `src/main/java/com/m3/precompute/M3TextSignals.java` | [93–145](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3TextSignals.java#L93-L145) | Direct Metrics.of/Histogram.of chain; composition at 149–205 and count/same/L1 at 231–273 are primary-body evidence |
| `src/main/java/com/m3/precompute/M3EditDistancePlan.java` | [108–130](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3EditDistancePlan.java#L108-L130) | Fresh value signals / workspace pattern signals / lower-bound rejection; workspace preparation at 297–302 |
| `src/main/java/com/m3/precompute/M3CodeTextSignals.java` | [107–179](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3CodeTextSignals.java#L107-L179) | Lexical preparation then finish(String/toString) and base signals; [batch provenance](precompute-batch-reference.md#code-text-compilation-and-snapshot-provenance) |
| `src/main/java/com/m3/precompute/M3SimilarityPrecompute.java` | [25–41](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3SimilarityPrecompute.java#L25-L41) | Derives pair fields from signals; [pairwise record](precompute-batch-reference.md#pairwise-similarity-record) |
| `src/main/java/com/m3/precompute/M3SimilarityBatchJava.java` | [22–50](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3SimilarityBatchJava.java#L22-L50) | Progress client with 1,024-row cadence; [score contract](precompute-batch-reference.md#approximate-similarity-batch-contract); no exact edit query |
| `src/main/java/com/m3/precompute/M3RegexStringSignals.java` | [153–180](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3RegexStringSignals.java#L153-L180) | Progress client with 256-row cadence; [regex snapshot](regex-precompute-reference.md#ordered-snapshots-parity-and-hashing) |
| `src/main/java/com/m3/precompute/M3CpuToolRuntime.java` | [1–274](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3CpuToolRuntime.java#L1-L274) | Negative direct-client finding: no lexical reference to any of these five owners; keep [CPU runtime reference](cpu-tool-precompute-reference.md) |
| `pom.xml` | [1–283](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/pom.xml#L1-L283) | Standalone coordinates/release/JUnit/Surefire; optional native and recipe profiles, no effective-model execution |
| `README.md` | [1–157](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/README.md#L1-L157) | Isolated non-bootstrap scope, retained runtime owners, build instructions and promotion gates; historical test/parity prose is not a new execution receipt |

## Exact supporting-source and guide identities

| Source path / body | Lines | Git blob SHA-1 | Raw SHA-256 |
| --- | ---: | --- | --- |
| [m3/ports/precompute/src/test/java/jdk/m3/precompute/M3TextSignalsTest.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/test/java/jdk/m3/precompute/M3TextSignalsTest.java) | 153 | `2be7a13ff3802c570f0a7952101b7bb571fa3a91` | `9ab73ef900ebf19aa3638cecb975c686f3818dc8dab072e92fbcfc57bdaad9be` |
| [m3/ports/precompute/src/test/java/jdk/m3/precompute/M3EditDistancePlanTest.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/test/java/jdk/m3/precompute/M3EditDistancePlanTest.java) | 95 | `fa80b7eb4ef3f0d58bf92321856c252e76da6d43` | `dab47f3aeb3dde5f94db1c3f1ae2def201e689c8aa1037cd67a3c77e5f49b88f` |
| [m3/ports/precompute/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/pom.xml) | 283 | `8be0e2a08677712873f8c1e3cfea8aec85e2de62` | `2e5dec5159d051824000aa2641dca8fd4abc62397413f347402577d113c835a9` |
| [m3/ports/precompute/README.md](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/README.md) | 157 | `8826e8d83f785f12646017c538585b0d6ec6d34a` | `abffc8a9c4b199887ec8da724fcd64fa6fea8f88fe800f43d9ebf0be07eb821e` |
| [m3/ports/precompute/src/main/java/jdk/m3/precompute/M3CodeTextSignals.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3CodeTextSignals.java) | 216 | `4c1a7042645de54db053e78c519f667940b024f5` | `f64aac93badaec71807d2c399c4a86d8fdcd3f31db65f4aba37410efb8ef662c` |
| [m3/ports/precompute/src/main/java/jdk/m3/precompute/M3SimilarityPrecompute.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3SimilarityPrecompute.java) | 41 | `a9ff21b8b19a9433d45d76e13fb5a5d9a2b59b3a` | `d2e94f06244ea4f3892e4b22bc2b2103c62aea18702883fff2fede9f0c65c1be` |
| [m3/ports/precompute/src/main/java/jdk/m3/precompute/M3SimilarityBatchJava.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3SimilarityBatchJava.java) | 91 | `20a33545f72a36f5e373b4908c40cb56d4b7f733` | `790e8fbb87eada489982762f45548ba9d145d98da877e78169888579a3ff0220` |
| [m3/ports/precompute/src/main/java/jdk/m3/precompute/M3CpuToolRuntime.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3CpuToolRuntime.java) | 274 | `e3f4c66cbe8d7120f5a25d6f22a18d70ad3e21da` | `99b3880f110da6b55d9389ec498e866ed7f9544f9c139a3b6dc6942b36c8c266` |
| [m3/ports/precompute/src/main/java/jdk/m3/precompute/M3RegexStringSignals.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/jdk/m3/precompute/M3RegexStringSignals.java) | 316 | `06712696f9a9446f66fc1a764a24e5e723ea4bd8` | `aa93c240ad22392ab6ec155ef318175446f26eda77b8eb32b06b2600a4bb42e9` |
| [src/java.base/share/classes/java/lang/CharSequence.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/src/java.base/share/classes/java/lang/CharSequence.java) | 305 | `87bc4a5525a850a7323a784a1927e6221d706ed6` | `975334d4fe56d0cfced46335c53894215ede36b23dd904c3b786c61f854a8a7a` |
| [doc/m3-guide/precompute-source-coverage.md](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/doc/m3-guide/precompute-source-coverage.md) | 66 | `7daee104ae299ab7f78a7ab210e823b10c973cb0` | `141940b6c6df3410d511a2e2086fc75ec1a3773a3021fe3410c98b651d6daba3` |
| [doc/m3-guide/precompute-batch-reference.md](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/doc/m3-guide/precompute-batch-reference.md) | 551 | `94a9452bf9b2825f9d6dba32e948fbdfac763022` | `01820fcbe9d12e249bfa64dadc1fdd87949916a33323257d8f1ab6be4277b088` |
| [doc/m3-guide/regex-precompute-reference.md](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/doc/m3-guide/regex-precompute-reference.md) | 541 | `646f5a6b3892888845a75838afccea8ea7598f6b` | `e4d105f6060a66e44faa4787db8e3809c94c4838dfd9ce82a3dd381a0219bb8a` |
| [doc/m3-guide/cpu-tool-precompute-reference.md](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/doc/m3-guide/cpu-tool-precompute-reference.md) | 454 | `aaf6f088f182e68c32870ab0e9b0229adf7a02b6` | `803a3b16fa44f0a017f7d7e259c15a91da43bb1ddd9737fe6425e0d8c878639f` |

## Policy identity and unchanged boundaries

Public target policy context was re-read, including the current receiving
sequence and precompute scope in the name map, the older catalogue/pin and the
successor receiver summary/status. The full bodies' exact identities below do
not imply requalification of all historical mapping rows or private donor bodies.
The successor receipt leaves its source families SOURCE_PIN_ONLY and its phase
plan PLANNED; documentation does not edit those statuses. Policy/provenance
verifiers were **NOT_RUN**. No private donor source is incorporated, and per-file
OpenJDK/third-party notices remain distinct from these Apache-2.0 guide headers.
Root AGENTS applies; successful parent-directory listings and direct missing-file
responses found no `doc/AGENTS.md` or `doc/m3-guide/AGENTS.md` at this pin.

| Source path / body | Lines | Git blob SHA-1 | Raw SHA-256 |
| --- | ---: | --- | --- |
| [AGENTS.md](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/AGENTS.md) | 50 | `f78685ca6efb5191e4e11b0bcfd137ddac7430b9` | `d9cc483da6c7330b30a454f6ce65868978167e302bcdb0805bf6d8a5da018621` |
| [m3/docs/m3jdk21-porting-invariant.md](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/docs/m3jdk21-porting-invariant.md) | 499 | `d933c7f8a4ba875b318c499214857146724fc4da` | `604ef82225fdb9e6ecf00e14f7630fb0956b85ec89e5c9241537733d4054f39a` |
| [m3/docs/name-mapping.json](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/docs/name-mapping.json) | 8309 | `288f20575d3f9d106571990f21944e7f46b0cf71` | `f99b80ededfb898804547ad3baf87cdc86aaccda4331c076591e2459d4f09ecb` |
| [m3/compatibility/m3-full-family-pin.tsv](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/compatibility/m3-full-family-pin.tsv) | 23 | `3d24fc38677435453787ca67f3d5458dbfaf5b5f` | `5d2a89e133164bfc276ed834be47ad3d1bd2583e4bed08a7f94a7e1eb2b50bcc` |
| [m3/docs/m3-full-family-catalogue.json](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/docs/m3-full-family-catalogue.json) | 247 | `8145c667c941a9358e9a5149489e455d950afc34` | `2e6fa85aeaaec5a6c37aa429b66d102ee9497735fdd5476ca4085d34667b5eef` |
| [m3/m3-import/current-full-borrow/README.md](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/m3-import/current-full-borrow/README.md) | 112 | `11fbf03808fe5d1999b38766eebd7d1944ace049` | `8a010b39ee657098078ddc8e9502a545b6628c9b23c0b3f63526423ba36a4b55` |
| [m3/m3-import/current-full-borrow/pin.tsv](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/m3-import/current-full-borrow/pin.tsv) | 27 | `8bdaaaf8bacbaf190e537e82955915a720592c92` | `8cdf72c68a0075ad8b3bb0968369619d1c4c34bc675d785d19aec39717f495b4` |
| [m3/m3-import/current-full-borrow/receiver-status.tsv](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/m3-import/current-full-borrow/receiver-status.tsv) | 33 | `895d91811075652f0328a9c1cf28489c68c09158` | `390785bf33ebe9e9b3d7a60f5a2e597fb110f72ab68b7d3e45bead245f33a5e1` |
| [m3/m3-import/current-full-borrow/promotion-phases.tsv](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/m3-import/current-full-borrow/promotion-phases.tsv) | 6 | `c4e8582f99caba32d718b2c1e86dae0854cdce3d` | `e4730068f321d551913a4cf7d40832f43b315c3973312c73b744ca5081758eb5` |

## Remaining qualification

The member-route gap is bounded to the five bodies above. API usage still needs
stable matching observations of mutable/custom CharSequences; exact original-text
verification after positive filters; domain-correct UTF-16 Levenshtein use;
sequential identity-bound workspace reuse; and caller-specific progress/error
handling. A copied array, matching hash, cache location or finite seed matrix does
not replace these obligations.

Not established by this static review: executed build and seven-test results,
fuller constructor/mask/flag/
q-gram/filter/sketch assertions, all budget/overflow/allocation boundaries,
corrupted same-package arrays, callback exceptions/cancellation order and
latency, reentrancy/concurrency, inherited-stream and diagnostic-row behavior,
measured preparation/reuse/retained-heap/native-process costs, concrete consumer
wiring and all applicable runtime/provider/platform gates. This reference neither
claims those tests passed nor reopens implementation or authorizes publication,
merge, workflow dispatch, new runtime state or a competing canonical owner.
