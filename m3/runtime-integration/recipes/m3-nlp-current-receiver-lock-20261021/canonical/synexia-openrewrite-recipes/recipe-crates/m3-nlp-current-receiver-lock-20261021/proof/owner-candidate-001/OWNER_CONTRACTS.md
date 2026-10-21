<!-- SPDX-License-Identifier: Apache-2.0 -->
# Exact current NLP receiver source closure

This successor qualifies the existing Java 21 companion lane at receiving source
`hsoliwal/M3jdk21@2779aba3f98f31c324be43f731be7914e1a05935`. It changes one
existing constructor declaration and the existing closed source manifest. The
original Lucene and OpenNLP build owners, source staging recipes, dependency
locks, module aggregation, legal-notice rules, probes, and image checks remain
unchanged. Canonical reusable recipe ownership is
`hsoliwal/com.synexia`, namespace `m3-nlp-current-receiver-lock-20261021`.

## Existing source owners admitted by the manifest

| Owner | Exact receiving source and contract | Evidence and limit |
| --- | --- | --- |
| `M3GvasLiteralImage` | Receiving commit `c09e67319378740dcad570d506463594dbfad3ee`, source SHA-256 `2c1a8b9cc9870fc5fdf8e6f8ca46af907c503346aaf6d783b74a6498fd3601cd`. Its bounded optional primitive hash directory refers to existing native UTF-16 rows, preserves exact spelling comparison and the binary-search fallback, and retains the existing image geometry and JNI address ownership. | The actual received source and test match canonical PR #12387 head `c28bd8e83d0b88a9dba0c0a9fdbd35a25e6ecd07` by Git blob and manifest SHA. The unchanged strict Java/C11 gate passed collision/digest/corruption refusal and three cooperating JVMs with one fixed address/inode; 17,530 checks per child. The first sandbox attempt failed when child `/proc` mappings were inaccessible; it is retained. This is finite Linux Java/JNI proof, without a whole String/HotSpot or cross-platform claim. |
| `M3BigString` | Receiving commit `632a9eefb68798d86acd83e886fd267938638e01`, source SHA-256 `e09815cea88c44e4da89ca8979ba247cddeee6c97b496d1aa2309a48916a90de`. Immutable long-coordinate composition reuses existing immutable chunks and primitive summary/index lanes. UTF-16, summary, literal-search and bounded int-window Java regex contracts remain unchanged. | The exact receiving source/test match their canonical manifest and existing owning workflow. That workflow's literal hash/strict compilation/test steps passed 3,213 checks. Canonical PR #12347's immutable head has different templates; see the separate discrepancy below. No source or new streaming API is imported from those differing templates. |
| `M3RopeTextIndex` | Receiving commit `b2f989a0fc77bc95a5fddfee7b378085d742fe8c`, source SHA-256 `ff2495c01b8df68a01167e29daa4ea4a6af25650e59c5cc705cc2fdc7cfa0141`. A bounded 16-way primitive directory indexes existing immutable M3StringPiece owners without copying characters. Coordinates are long UTF-16 units; Java regex remains confined to a requested int-sized window. | Source/test Git blobs match canonical PR #12344 head `d6f1b7a3b9b3eda4a29877ff65b6c2d2901dd1be` and its manifest. The original whole-module proof initially failed an unrelated exported-constructor lint warning. After the explicit constructor atom below, the unchanged original script passed 83,941 checks in each of normal JIT, interpreter, noncompact-string and C2 modes. A prior finite dependency-closure diagnostic also passed but is not substituted for that whole-module gate. |

The first-party sources retain their Apache-2.0 headers and authorship. The
BigString/Rope donor catalogues identify Swift Collections algorithmic ideas;
they expressly do not copy Swift source or add a Swift runtime. This correction
does not introduce third-party algorithm bodies or reinterpret their licenses.
Exact historical PR/manifest/source observations are retained in
`LINEAGE_REVIEW.json` and `RECEIVING_COMMIT_READBACK.json`.

## Constructor atom

`SharedLexiconPrecomputeCatalog.Builder` previously received an implicit public
no-argument constructor. Its seven field initializers create the same seven
maps before either constructor body. The successor adds only:

```java
public Builder() { }
```

This satisfies the existing strict module compiler's `missing-explicit-ctor`
check. It does not suppress lint or change the module descriptor. Exact
`javap -public -s` output is equal for all nine catalogue classes, and the
entire `javap -c -p -s` Builder output is equal, including constructor and field
initialization instructions. The existing `M3LexiconPrecomputeTest` passes
50 checks on each version. Debug source-line tables may differ; complete
class-file byte identity is not claimed.

The source moves from SHA-256
`8e6156376ea9548971b78bb876dc3968195266f04c17c5b061d7f5e4b52f367d`
to `92fd76c3f0d3c10c90ea4cb52485d2dbceda513fa3d794d22ec67fd153fd62ed`.
This first-party declaration preserves the existing source copyright/license.

## Closed manifest successor

`current-source-lock.json` changes from
`054029431fbd8063c1e0397c43f3538f319984e252c92f08538881dc0a2eaff5`
to `7f591c98c603ba8128c97747fe38fd1ccfe8adb5f2cd712b98b2697fff935acb`.
Its only logical changes are the GVAS and constructor-owner hashes and two
explicit entries for the already received BigString and RopeTextIndex sources.
All other values, schema, source roots, module descriptors and guards are equal.
The manifest has 145 exact file entries; `com.m3.text` has an exact 27-file Java
source closure. Its older `receiver.base_commit` field is retained as original
provenance; the new canonical handoff separately records the current receiving
base and two-file successor.

The unchanged production verifier accepts the exact candidate and refuses a
known owner's byte drift, a missing owner, and an extra unlisted Java source.
The original combined-receiver refusal is retained unchanged. Neither the
source coverage check nor its failure behavior was widened or removed.

## Historical BigString template discrepancy

At immutable canonical PR #12347 head
`87219f9b837e78cb317e9ebee91334269a5f1668`, its source template hash is
`eb62c188e6b39ccc79d9cf372f15abdba12fd5ecd21f35e96eb22dd7ab08261e`,
while its manifest records the received `e09815ce...` source. The test template
hash is `ad5737365bcacdf839fa62ead8369e35e78efbc7f7afd55739d708f0512d358f`,
while the manifest and receiving workflow record
`5f47db4885a99b1ec7dc95957276d0130f2eab7cba16614907995c722fa6f9a6`.
The differing templates add public `codeUnits()`/`codePoints()` stream methods
and a corresponding streaming/interoperability test. Full diffs are retained.
No normalization or historical recipe replay success is inferred. This
successor binds and tests the exact existing received source; it does not repair
that older canonical packet or claim to regenerate it from the differing head.

## Scope and pending gates

The combined proof uses the exact existing Lucene companion
`873b6167603c69101d626ddffd8d733340300fea` and OpenNLP companion
`8b647d1d389736952e43fb46865c874747f7acde`, actual source-built modules and
their locked external archives. Original source-lock donor-basis commits remain
distinct from those companion implementation pins. Java 21 preview use remains
explicit. Original Java 25 fork/Gradle qualification is a separate lane.

This work does not advance the String → arrays → collections → AST/compiler
promotion sequence, establish global family completion, or replace the JDK's
String/HotSpot/GC/CDS/JNI product acceptance. Full upstream suites, optional
Faiss native runtime, cross-platform gates, externally supplied model coverage,
performance admission and exact published-head CI remain separate. The
canonical recipe, emitted target readback and actual target runtime receipts
must be linked separately from isolated candidate proof.
