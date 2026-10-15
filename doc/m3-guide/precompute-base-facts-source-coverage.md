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
| [m3/ports/precompute/src/main/java/com/m3/precompute/M3TextMetrics.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextMetrics.java) | 134 | `e67ad2157aacb8b31b293e522294d4c7f657adff` | `89d82f896a2d2db189fd7d50d9ae0c04c490408252be594cad8e15caf432f0a0` |
| [m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java) | 417 | `631145b68358970e6a7d9a792580e7a100eebb20` | `4ad21ad1b55a5b17388a0e5e77b0ea39e4accec7eb0437218d3f9c82734cff2a` |
| [m3/ports/precompute/src/main/java/com/m3/precompute/M3Histogram.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3Histogram.java) | 83 | `e8abc6db52f3032f415f5316167a169dec043438` | `3611e342693aa46127c359b472c66103344b382257a6ffcc8c206936c180009d` |
| [m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java) | 314 | `84bc435ddfa6a84d5c41dc5ae3ddc2711d609938` | `52a5e25c521089f20d98c5420ca90e7369a6cc7109fe388447e3dedeadbe42a2` |
| [m3/ports/precompute/src/main/java/com/m3/precompute/M3Progress.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3Progress.java) | 17 | `028db488d17a3597d060c1d7381f6722c9b202a3` | `c52bc47f5c8f85aea86a19bc2db40c7325db0aceecbfae4c83572135f83e30b3` |

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
| record components / generated accessors | `utf16Length(); utf8Length(); codePointCount(); unpairedSurrogateCount(); javaHashCode(); hash31Power(); firstUtf16Unit(); lastUtf16Unit()` | [13–21](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextMetrics.java#L13-L21) | Metrics component table; compiler-generated methods are separate from declarations |
| private static final field | `UTF8_REPLACEMENT_BYTES` | [23–24](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextMetrics.java#L23-L24) | Encoder replacement width |
| public static final field | `EMPTY` | [26–27](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextMetrics.java#L26-L27) | Canonical zero record |
| public compact constructor | `M3TextMetrics` | [29–43](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextMetrics.java#L29-L43) | Local validation, not semantic consistency |
| public static method | `of(CharSequence)` | [45–91](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextMetrics.java#L45-L91) | Live scan and byte/hash arithmetic |
| public static method | `combine(M3TextMetrics,M3TextMetrics)` | [93–119](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextMetrics.java#L93-L119) | Operand identity, stored-fact seam correction |
| public method | `isEmpty()` | [121–123](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextMetrics.java#L121-L123) | Stored length only |
| public method | `isWellFormedUtf16()` | [125–127](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextMetrics.java#L125-L127) | Stored unpaired count only |
| private static helper | `utf8Bytes(char)` | [129–133](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextMetrics.java#L129-L133) | Non-surrogate widths |

Generated surface: eight component fields/accessors, canonical record machinery, and component-based `equals(Object)`, `hashCode()` and `toString()`. The compact constructor is explicitly declared above. The generated record hash differs from the stored String-style `javaHashCode` component. Generated methods have no handwritten method bodies here.

### M3TextSignals

[Full contract](precompute-base-facts-reference.md#signals-stable-facts-and-candidate-boundaries).

| Kind | Complete declared surface | Source lines | Contract route |
| --- | --- | --- | --- |
| public static final constants | `ASCII; LATIN1; BLANK; ALL_LETTERS; ALL_DIGITS; HAS_LETTER; HAS_DIGIT; HAS_UPPER; HAS_LOWER; WELL_FORMED_UTF16` | [16–25](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L16-L25) | Ten flags and exact values/predicates |
| private static final constants | `HASH_BASE; MIN_HASH_LANES` | [27–28](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L27-L28) | 1,000,003 polynomial base; 16 lanes |
| private final fields | `metrics; characters; bigrams; trigrams` | [30–33](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L30-L33) | Geometry and three histograms |
| private final fields | `hash64; power64; presence64; prefix2; suffix2` | [34–38](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L34-L38) | Hash/composition/filter state |
| private final fields | `letters; digits; whitespace; uppercase; lowercase` | [39–43](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L39-L43) | Code-point classification counts |
| private final fields | `flags; simHash64; minHash` | [44–46](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L44-L46) | Eagerly computed outputs |
| private constructor | `M3TextSignals(metrics,characters,bigrams,trigrams,hash64,power64,presence64,prefix2,suffix2,letters,digits,whitespace,uppercase,lowercase)` | [48–91](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L48-L91) | Stores state, derives flags and both sketches |
| public static method | `compile(CharSequence)` | [93–146](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L93-L146) | String/toString preparation |
| public static method | `combine(M3TextSignals,M3TextSignals)` | [149–205](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L149-L205) | Histograms and seam composition |
| public method | `metrics()` | [207–209](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L207-L209) | Stored record reference |
| public method | `flags()` | [211–213](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L211-L213) | Stored bit mask |
| public method | `hasAll(int)` | [215–217](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L215-L217) | Mask containment including zero/unknown bits |
| public method | `hasAny(int)` | [219–221](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L219-L221) | Mask intersection including zero/unknown bits |
| public method | `contentHash64()` | [223–225](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L223-L225) | Stored polynomial hash |
| public method | `presence64()` | [227–229](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L227-L229) | Stored lossy presence mask |
| public method | `characterCount(char)` | [231–233](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L231-L233) | Exact UTF-16-unit multiplicity |
| public method | `bigramCount(char,char)` | [235–237](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L235-L237) | Exact contiguous unit-pair multiplicity |
| public method | `trigramCount(char,char,char)` | [239–241](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L239-L241) | Exact contiguous unit-triple multiplicity |
| public method | `mayContain(M3TextSignals)` | [243–247](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L243-L247) | Length/presence candidate test |
| public method | `mayEqual(M3TextSignals)` | [249–254](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L249-L254) | Length/two-hash candidate test |
| public method | `isAnagramOf(M3TextSignals)` | [256–258](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L256-L258) | Exact unit multiset equality |
| public method | `editLowerBound(M3TextSignals)` | [266–273](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L266-L273) | Maximum of length and rounded q-gram bounds |
| public method | `simHash64()` | [275–277](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L275-L277) | Stored sketch |
| public method | `simHashDistance(M3TextSignals)` | [279–281](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L279-L281) | 64-bit Hamming distance |
| public method | `minHash()` | [283–285](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L283-L285) | Defensive 16-lane clone |
| public method | `estimatedJaccard(M3TextSignals)` | [288–298](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L288-L298) | Equal-lane fraction and explicit empty cases |
| public method | `histogramPayloadBytes()` | [300–302](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L300-L302) | Only histogram primitive element storage |
| public method | `labels()` | [304–318](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L304-L318) | Ordered unmodifiable flag-name list |
| private helper | `computeSimHash()` | [320–338](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L320-L338) | Highest-q features; frequency-weighted votes; zero ties |
| private helper | `computeMinHash()` | [340–355](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L340-L355) | Highest-q distinct features; unsigned minima |
| private static helper | `packedPrefix2(CharSequence)` | [357–361](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L357-L361) | Zero/one/two leading units |
| private static helper | `packedSuffix2(CharSequence)` | [363–367](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L363-L367) | Zero/one/two trailing units |
| private static helper | `composePrefix2(M3TextSignals,M3TextSignals)` | [369–375](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L369-L375) | Reuse left two or make seam pair |
| private static helper | `composeSuffix2(M3TextSignals,M3TextSignals)` | [377–383](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L377-L383) | Reuse right two or make seam pair |
| private static helper | `bigram(int,int)` | [385–387](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L385-L387) | 32-bit packed unit key |
| private static helper | `trigram(int,int,int)` | [389–391](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L389-L391) | 48-bit packed unit key |
| private static helper | `addSignal(long,char)` | [393–398](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L393-L398) | ASCII-only folding and two presence bits |
| private static helper | `mix64(long)` | [400–407](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L400-L407) | Wrapping xor/shift/multiply mixer |
| private static helper | `mix32(int)` | [409–416](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L409-L416) | Wrapping xor/shift/multiply mixer |

Totals: ten public constants, two private constants, seventeen private instance fields, one private constructor, **21 explicit public methods**, eleven private helpers. Object identity methods are inherited, not signal value comparisons.

### M3Histogram

[Full contract](precompute-base-facts-reference.md#histogram-internal-ownership-and-arithmetic).

| Kind | Complete declared surface | Source lines | Contract route |
| --- | --- | --- | --- |
| package-private final class | `M3Histogram` | [7–83](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3Histogram.java#L7-L83) | Internal dependency only |
| package-visible final fields | `keys; counts` | [8–9](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3Histogram.java#L8-L9) | References final, elements mutable |
| private constructor | `M3Histogram(long[],int[])` | [11–14](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3Histogram.java#L11-L14) | Stores arrays directly |
| package-private static method | `of(long[])` | [16–30](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3Histogram.java#L16-L30) | Sorts input in place; retains fresh arrays |
| package-private method | `plus(M3Histogram)` | [32–52](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3Histogram.java#L32-L52) | Sorted checked merge and trim copies |
| package-private method | `l1(M3Histogram)` | [54–69](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3Histogram.java#L54-L69) | Long multiset L1 |
| package-private method | `count(long)` | [71–74](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3Histogram.java#L71-L74) | Binary search; absent is zero |
| package-private method | `same(M3Histogram)` | [76–78](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3Histogram.java#L76-L78) | Key/count array equality |
| package-private method | `payloadBytes()` | [80–82](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3Histogram.java#L80-L82) | 12 bytes per distinct key |

### M3EditDistancePlan

[Full contract](precompute-base-facts-reference.md#edit-distance-plan-and-workspace).

| Kind | Complete declared surface | Source lines | Contract route |
| --- | --- | --- | --- |
| private static final constants | `MAX_PATTERN_UNITS; MAX_SOURCE_UNITS; MAX_DP_CELLS; MYERS_MAX_UNITS` | [15–18](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L15-L18) | 1,048,576; 1,048,576; 64,000,000; 63 |
| private final fields | `pattern; myersUnits; myersMasks; myersHighBit; myersDomainMask` | [20–24](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L20-L24) | Private copied pattern / optional bit-vector state |
| private constructor | `M3EditDistancePlan(char[],char[],long[],long,long)` | [26–37](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L26-L37) | Stores prepared private arrays |
| public static method | `compile(CharSequence)` | [39–68](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L39-L68) | Copy and optional 1–63-unit masks |
| public method | `patternLength()` | [70–72](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L70-L72) | Stored unit length |
| public method | `usesMyers()` | [74–76](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L74-L76) | Non-null unit table |
| public method | `primitivePayloadBytes()` | [78–83](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L78-L83) | Plan primitive arrays only |
| public method | `workspace()` | [85–87](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L85-L87) | Allocate independent identity-bound rows and pattern facts |
| public method | `distance(CharSequence)` | [89–91](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L89-L91) | New workspace convenience |
| public method | `distance(CharSequence,Workspace,M3Progress)` | [93–102](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L93-L102) | Validation and empty/Myers/full-DP dispatch |
| public method | `within(CharSequence,int)` | [104–106](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L104-L106) | New workspace convenience |
| public method | `within(CharSequence,Workspace,int,M3Progress)` | [108–130](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L108-L130) | Validation, negative filters, Myers/band dispatch |
| private helper | `myersDistance(CharSequence,M3Progress)` | [132–160](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L132-L160) | Single-word recurrence and callbacks |
| private helper | `mask(char)` | [162–165](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L162-L165) | Binary-search unit mask |
| private helper | `dynamicDistance(CharSequence,Workspace,M3Progress)` | [167–202](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L167-L202) | Two rows; normalizes stored previous row on result path |
| private helper | `bandedWithin(CharSequence,Workspace,int,M3Progress)` | [204–245](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L204-L245) | Full-row sentinel fill; narrow recurrence; early rejection |
| private helper | `ensureWorkload(int)` | [247–251](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L247-L251) | Explicit source length range |
| private helper | `requireFullDpBudget(int)` | [253–258](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L253-L258) | Checked full recurrence estimate |
| private helper | `requireBandedBudget(int,int)` | [260–268](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L260-L268) | Checked band recurrence estimate |
| public override | `length()` | [271–273](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L271-L273) | CharSequence unit length |
| public override | `charAt(int)` | [276–278](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L276-L278) | Checked unit access |
| public override | `subSequence(int,int)` | [281–284](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L281-L284) | Checked range then String materialization |
| public override | `toString()` | [287–289](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L287-L289) | String materialization |
| public static final nested class | `Workspace` | [291–313](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L291-L313) | One workspace owner; no public constructor |
| private final workspace fields | `owner; previous; current; patternSignals` | [292–295](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L292-L295) | Identity and mutable rows / cached pattern facts |
| private workspace constructor | `Workspace(M3EditDistancePlan)` | [297–302](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L297-L302) | Two arrays and compile(owner), even for Myers |
| public workspace method | `previousRowCopy()` | [304–306](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L304-L306) | Defensive diagnostic clone, not universal last-row guarantee |
| private workspace helper | `require(M3EditDistancePlan)` | [308–312](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L308-L312) | Owner identity refusal |

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
| public interface / anonymous implementation | `M3Progress; anonymous body of NONE` | [5–17](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3Progress.java#L5-L17) | Anonymous body declares no additional member |
| implicit public static final field | `NONE` | [6–6](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3Progress.java#L6-L6) | One no-op anonymous instance |
| public default method | `begin(String,long)` | [8–8](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3Progress.java#L8-L8) | No-op; no validation or state |
| public default method | `subTask(String)` | [9–9](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3Progress.java#L9-L9) | No-op; no selected client invokes it |
| public default method | `worked(long)` | [10–10](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3Progress.java#L10-L10) | No-op; no implicit accounting |
| public default method | `checkCanceled()` | [11–11](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3Progress.java#L11-L11) | No-op; caller defines cancellation |
| public default method | `done()` | [12–12](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3Progress.java#L12-L12) | No-op; not automatic release |
| public static method | `none()` | [14–16](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3Progress.java#L14-L16) | Returns NONE identity |

## Read test methods and limits

These two complete test bodies contain **seven `@Test` methods**. READ is the
evidence status; none was executed here. Supporting test helpers were also read.
Finite agreement with another implementation is not exhaustive semantic proof.

| Test method | Pinned lines | What its assertions actually inspect | Limits |
| --- | --- | --- | --- |
| `M3TextSignalsTest.compositionMatchesFlatCompilationIncludingSurrogateSeams` | [12–36](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/test/java/com/m3/precompute/M3TextSignalsTest.java#L12-L36) | 11-string × 11-string composition matrix including empty and surrogate fragments; uses same helper | Does not directly compare all unit/q-gram counts or independently establish classifications |
| `M3TextSignalsTest.randomizedCompositionRetainsExactHistogramsAndSketches` | [39–70](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/test/java/com/m3/precompute/M3TextSignalsTest.java#L39-L70) | 500 seeded strings with lengths below 48; one random split; same helper plus all character and bigram counts over its 12-unit alphabet | Not exhaustive; no direct all-trigramCount assertion or hostile/mutable inputs |
| `M3TextSignalsTest.editLowerBoundNeverExceedsIndependentExactDistance` | [73–89](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/test/java/com/m3/precompute/M3TextSignalsTest.java#L73-L89) | 1,500 seeded pairs below 14 units over seven units; lower bound ≤ independent recurrence and ≥ length gap | Finite lower-bound comparison, not all edit/encoding domains |
| `M3TextSignalsTest.similaritySketchesAreSelfConsistentButNotSemanticAuthority` | [92–109](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/test/java/com/m3/precompute/M3TextSignalsTest.java#L92-L109) | Self SimHash/Jaccard; one near-result range; two mayEqual negatives; three pair-facade fields | No arbitrary collision, mayContain, isAnagramOf or statistical quality proof |
| `M3EditDistancePlanTest.deterministicExamplesAndThresholds` | [11–23](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/test/java/com/m3/precompute/M3EditDistancePlanTest.java#L11-L23) | kitten/sitting, sitten threshold and empty pattern examples | Selected deterministic values, no callback assertions |
| `M3EditDistancePlanTest.randomizedMyersAndDynamicPathsMatchIndependentDp` | [26–46](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/test/java/com/m3/precompute/M3EditDistancePlanTest.java#L26-L46) | 1,000 seeded pairs of lengths below 90, six-unit alphabet; distance and eight thresholds {0,1,2,3,5,8,16,expected} | No exhaustive 63/64, ceiling, large-band or arbitrary CharSequence matrix |
| `M3EditDistancePlanTest.workspaceOwnershipAndBudgetsFailClosed` | [49–66](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/test/java/com/m3/precompute/M3EditDistancePlanTest.java#L49-L66) | Foreign workspace; 1,048,577-unit pattern/source; 20,000×20,000 full-DP refusal | No negative-threshold or banded-work refusal assertion; no callback/concurrency/resource measurement |

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
| `src/main/java/com/m3/precompute/M3TextSignals.java` | [93–145](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3TextSignals.java#L93-L145) | Direct Metrics.of/Histogram.of chain; composition at 149–205 and count/same/L1 at 231–273 are primary-body evidence |
| `src/main/java/com/m3/precompute/M3EditDistancePlan.java` | [108–130](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3EditDistancePlan.java#L108-L130) | Fresh value signals / workspace pattern signals / lower-bound rejection; workspace preparation at 297–302 |
| `src/main/java/com/m3/precompute/M3CodeTextSignals.java` | [107–179](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3CodeTextSignals.java#L107-L179) | Lexical preparation then finish(String/toString) and base signals; [batch provenance](precompute-batch-reference.md#code-text-compilation-and-snapshot-provenance) |
| `src/main/java/com/m3/precompute/M3SimilarityPrecompute.java` | [25–41](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3SimilarityPrecompute.java#L25-L41) | Derives pair fields from signals; [pairwise record](precompute-batch-reference.md#pairwise-similarity-record) |
| `src/main/java/com/m3/precompute/M3SimilarityBatchJava.java` | [22–50](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3SimilarityBatchJava.java#L22-L50) | Progress client with 1,024-row cadence; [score contract](precompute-batch-reference.md#approximate-similarity-batch-contract); no exact edit query |
| `src/main/java/com/m3/precompute/M3RegexStringSignals.java` | [153–180](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3RegexStringSignals.java#L153-L180) | Progress client with 256-row cadence; [regex snapshot](regex-precompute-reference.md#ordered-snapshots-parity-and-hashing) |
| `src/main/java/com/m3/precompute/M3CpuToolRuntime.java` | [1–274](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3CpuToolRuntime.java#L1-L274) | Negative direct-client finding: no lexical reference to any of these five owners; keep [CPU runtime reference](cpu-tool-precompute-reference.md) |
| `pom.xml` | [1–283](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/pom.xml#L1-L283) | Standalone coordinates/release/JUnit/Surefire; optional native and recipe profiles, no effective-model execution |
| `README.md` | [1–157](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/README.md#L1-L157) | Isolated non-bootstrap scope, retained runtime owners, build instructions and promotion gates; historical test/parity prose is not a new execution receipt |

## Exact supporting-source and guide identities

| Source path / body | Lines | Git blob SHA-1 | Raw SHA-256 |
| --- | ---: | --- | --- |
| [m3/ports/precompute/src/test/java/com/m3/precompute/M3TextSignalsTest.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/test/java/com/m3/precompute/M3TextSignalsTest.java) | 153 | `2defb5e9a66749dec38384dccd6999107c94fb32` | `c3ef8cb7199a21bea060829fa416342a11f26b2147d9ff5688eb146dfd3a8b9d` |
| [m3/ports/precompute/src/test/java/com/m3/precompute/M3EditDistancePlanTest.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/test/java/com/m3/precompute/M3EditDistancePlanTest.java) | 95 | `47c363ad6ae3cb1d2d24ce7c5549c1cfe0609117` | `62d7d2b794d8ae5d8e23317c964a434200032e384b7d293a9f07f2ebb546cfd3` |
| [m3/ports/precompute/pom.xml](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/pom.xml) | 283 | `8be0e2a08677712873f8c1e3cfea8aec85e2de62` | `2e5dec5159d051824000aa2641dca8fd4abc62397413f347402577d113c835a9` |
| [m3/ports/precompute/README.md](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/README.md) | 157 | `8826e8d83f785f12646017c538585b0d6ec6d34a` | `abffc8a9c4b199887ec8da724fcd64fa6fea8f88fe800f43d9ebf0be07eb821e` |
| [m3/ports/precompute/src/main/java/com/m3/precompute/M3CodeTextSignals.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3CodeTextSignals.java) | 216 | `06cc4d51ddbe305652c164b0002dd85eafec9116` | `f17ad2501d023d1fa9e9e5597ff729c59d1634ade0c4fe4d7a7af7adc40a9700` |
| [m3/ports/precompute/src/main/java/com/m3/precompute/M3SimilarityPrecompute.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3SimilarityPrecompute.java) | 41 | `e1a9564234eb389878a29f1cc73ad8670ec891d7` | `6bacf1f763f5231a1b0a5aa8b518f8011fa38ec34ea05a0d91e60755d42141d8` |
| [m3/ports/precompute/src/main/java/com/m3/precompute/M3SimilarityBatchJava.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3SimilarityBatchJava.java) | 91 | `42f3d27f56a669bb0d251489d5df15eb52f8118a` | `6b2fc1bd11d07ed522194666fb42ce9906c70f237dadc6aaefa412e135ade7d8` |
| [m3/ports/precompute/src/main/java/com/m3/precompute/M3CpuToolRuntime.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3CpuToolRuntime.java) | 274 | `238c688fd519236b87f0597af1f4ec7555e902e7` | `149e0e9e2a17a6bea89d19d675c20d0c973de118a916138890ee0031670e539a` |
| [m3/ports/precompute/src/main/java/com/m3/precompute/M3RegexStringSignals.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/ports/precompute/src/main/java/com/m3/precompute/M3RegexStringSignals.java) | 316 | `e2b62034d7ba9de17fefb8c156b185a9c2011f13` | `475729a1d7e48f65882febfaf9186e269d243975a87ef2aa772dd854bbbbe81c` |
| [src/java.base/share/classes/java/lang/CharSequence.java](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/src/java.base/share/classes/java/lang/CharSequence.java) | 305 | `87bc4a5525a850a7323a784a1927e6221d706ed6` | `975334d4fe56d0cfced46335c53894215ede36b23dd904c3b786c61f854a8a7a` |
| [doc/m3-guide/precompute-source-coverage.md](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/doc/m3-guide/precompute-source-coverage.md) | 66 | `7daee104ae299ab7f78a7ab210e823b10c973cb0` | `141940b6c6df3410d511a2e2086fc75ec1a3773a3021fe3410c98b651d6daba3` |
| [doc/m3-guide/precompute-batch-reference.md](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/doc/m3-guide/precompute-batch-reference.md) | 551 | `dc2636c21671409a522e1ccd6842ef8fde040709` | `b8d405095d13b06d5c3ba4815233b49ea37b5a35c9e26d69e4f5a71259117612` |
| [doc/m3-guide/regex-precompute-reference.md](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/doc/m3-guide/regex-precompute-reference.md) | 541 | `59723cafb048ddde034898a331d0eae8b626b455` | `2f1d70b2520c107e346ad61293cfb78437e19ec954590a590277b9bab668d19c` |
| [doc/m3-guide/cpu-tool-precompute-reference.md](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/doc/m3-guide/cpu-tool-precompute-reference.md) | 454 | `03a674ce67cea2a64f729674e7ad3febda337ae3` | `5b4199a5416b027f3011c9f8d045886c28257bae6b06d8893729cf0391424245` |

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
| [AGENTS.md](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/AGENTS.md) | 50 | `2253be71930e69f34420a72df8e8ba278ae61fb5` | `7de3689f6941871be9af41171851efa87cea662b29d831ecf87eeea4a5a2919b` |
| [m3/docs/m3jdk21-porting-invariant.md](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/docs/m3jdk21-porting-invariant.md) | 499 | `f65cc98e9088377755c51a447ce974356b28f15c` | `fa84270f09a42fe4dbeb245812e7b710297825e2bfd461bce99bb64056e1399d` |
| [m3/docs/name-mapping.json](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/docs/name-mapping.json) | 8309 | `288f20575d3f9d106571990f21944e7f46b0cf71` | `f99b80ededfb898804547ad3baf87cdc86aaccda4331c076591e2459d4f09ecb` |
| [m3/compatibility/synexia-full-family-pin.tsv](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/compatibility/synexia-full-family-pin.tsv) | 23 | `3d24fc38677435453787ca67f3d5458dbfaf5b5f` | `5d2a89e133164bfc276ed834be47ad3d1bd2583e4bed08a7f94a7e1eb2b50bcc` |
| [m3/docs/synexia-full-family-catalogue.json](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/docs/synexia-full-family-catalogue.json) | 247 | `8145c667c941a9358e9a5149489e455d950afc34` | `2e6fa85aeaaec5a6c37aa429b66d102ee9497735fdd5476ca4085d34667b5eef` |
| [m3/synexia-import/current-full-borrow/README.md](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/synexia-import/current-full-borrow/README.md) | 112 | `92347a83839d85d5e9b07b829a545f5c8a4c431a` | `9b6960f76ed3dfaf2cb97ad497b59d0c76294502d6d0047bed70a1dd7d5e26c6` |
| [m3/synexia-import/current-full-borrow/pin.tsv](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/synexia-import/current-full-borrow/pin.tsv) | 27 | `7b3c0b7d0a6b8daab40ef0062a9e9293d9b7abd2` | `d84e185891f08d76da765e43d1e138e33fbef99f50de9cc83a99c5284ab46ab5` |
| [m3/synexia-import/current-full-borrow/receiver-status.tsv](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/synexia-import/current-full-borrow/receiver-status.tsv) | 33 | `be8a15064e9b50f2c5c2a581159ee9e6b3e60cbc` | `7c1326d024e111e953c139e30be1cfe3026bab0458d648963efb0247684a0715` |
| [m3/synexia-import/current-full-borrow/promotion-phases.tsv](https://github.com/hsoliwal/M3jdk21/blob/db49925d43d4854707911868e603426db7055664/m3/synexia-import/current-full-borrow/promotion-phases.tsv) | 6 | `c4e8582f99caba32d718b2c1e86dae0854cdce3d` | `e4730068f321d551913a4cf7d40832f43b315c3973312c73b744ca5081758eb5` |

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
