<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Core text storage: exact source, member and test map

[Guide](README.md) · [Member contracts](core-storage-reference.md) · [Seventeen-source map](text-source-coverage.md) · [Coverage ledger](coverage.md)

## Evidence boundary

Dated continuation: **15 October 2026**. Public source pin:
`510dfa27b0679ce33348140afc57b558b8853802` in `hsoliwal/M3jdk21`.
Exactly **six primary Java files / 427 source lines** are mapped below. Complete
bodies and FoundationTest were read; fresh public fetches were recomputed as Git
blob SHA-1 and raw SHA-256 and matched the packet's unchanged source identities.
This closes declared-member/reference depth for these six files. It does not add
six files to the historical [17-owner package denominator](text-source-coverage.md)
or promote package/module/project/repository or runtime completion.

The [historical overview](m3-packages.md#textstorage-api-contracts) already read
these six bodies at `278b221e8365ebeb6095a3a5643b38a0564258b5`. Its contracts,
tests-read-only statement and earlier limitations remain intact. This map adds
the systematic state/accessor/internal-helper/generated/inherited treatment;
the six catalog and five projection/tool/receipt owners keep their own references.
No repository code, build, policy verifier, test, benchmark or native library was
executed in this documentation authoring/review pass. Every test attribution
below is **read, NOT_RUN in this pass**. Hosted CI remains separate evidence for
its exact commit and workflow scope; earlier/later CI results are not erased.

## Six primary source identities

All paths in this table are under `m3/core/src/com/m3/text/`; line counts include
the entire file, including comments and blank lines. The exact six rows are the
primary denominator; nested Encoding and record-generated members are not extra files.

| Primary source | Lines / UTF-8 bytes | Git blob SHA-1 | Raw SHA-256 |
| --- | --- | --- | --- |
| [JoinedM3StringPiece.java][JOINED] | 62 / 2634 | `5022e9811ce9391671c794446c7772569cfd369f` | `1655c02a61577f8aebed59b94c92701be835c4c7ebaf80efa3c71271a4bd75d7` |
| [LocalM3Arena.java][ARENA] | 50 / 1975 | `025ce8014cba8a36a9e496bb0a56faef57cf0968` | `e9573e7f04a875eb7bda7d3554a57653ebd655f66ac67be724b366092e76afc4` |
| [LocalM3StringPiece.java][LOCAL] | 51 / 2115 | `387ae5e0c656177f1df663fd5029d4b31ba2fa00` | `77fd1cbeae4dc20a4c23497102cb7c44ec14adbdd6bbf328326957515a7858ad` |
| [M3StringPiece.java][PIECE] | 21 / 767 | `62506fb9b3cff0e6f24e856648528db58c45b92b` | `7ed45335bea7dc1107d4c28ca505ad334c845685f099af0df3e3112c411a7755` |
| [SharedLexiconImage.java][IMAGE] | 228 / 11400 | `364eecefcb3822918c849606ddb177c0ceaa30d5` | `d4fcdb58e6a33d82fa18526bd4cd380869661e03f6a2beb721030b13e1789401` |
| [StorageIdentity.java][IDENTITY] | 15 / 494 | `8cd2ae86bee8f04f0d69709eed12897ff5c6a71c` | `f6897d5ddd7f39cc0d94bdc1b4db8b695b1946bc65672fb6c0408fa110c1b354` |

## Complete declared-member routing

Line ranges refer to the pinned sources above. Every explicitly declared field,
constructor, method and nested type is listed, including non-public helpers.
Generated/inherited surfaces are separately labelled so source-level closure
does not imply inspection of generated classfiles or an ABI gate.

| Owner and reference route | Declared member inventory | Generated / inherited distinction | Finite tests |
| --- | --- | --- | --- |
| [M3StringPiece](core-storage-reference.md#m3stringpiece-declared-and-inherited-surface) | Sealed interface and two permitted implementations (7–8); covariant `subSequence` (9); default `flatten` (11–15); static `join` (18–20). No fields/constructors/nested types. | CharSequence obligations `length`, `charAt`, `toString`; inherited `isEmpty`, `chars`, `codePoints`; static CharSequence.compare is not inherited. Covariant bridge generation is not a separately authored method. | T1–T3; inherited stream/default behavior is not asserted by this fixture. |
| [LocalM3Arena](core-storage-reference.md#localm3arena-allocation-and-trusted-admission) | Public no-arg constructor (17); private final `owner`, `sequence` (19–20); private `identity` (22–28); public `copyUtf16` (31–40), `copyLatin1` (43–45); package-private `owned` (47–49). | Ordinary Object behavior; no implicit no-arg constructor is counted because one is declared. | T1–T4, T5; trusted malformed inputs, counter exhaustion and failure rollback are not directly tested. |
| [LocalM3StringPiece](core-storage-reference.md#localm3stringpiece-immutable-public-view-over-trusted-owned-bytes) | Public nested Encoding enum (14–18), constants LATIN1/UTF16_LE (15), final shift (16), constructor (17); five private final fields backing/encoding/identity/codeUnitOffset/codeUnitLength (19–23); package-private constructor (25–32); storageIdentity (34), encoding (35), codeUnitOffset (36), retainedBackingBytes (38), length (39), charAt (40–45), covariant subSequence (46–49), toString (50). | Encoding.values/valueOf and inherited Enum methods; piece inherits Object equals/hashCode and M3StringPiece.flatten/CharSequence defaults. | T1–T3 and T5; encoding/retainedBackingBytes getters and enum/generated behavior have no focused assertions here. |
| [JoinedM3StringPiece](core-storage-reference.md#joinedm3stringpiece-non-public-flat-directory) | Package-private class (9); private final leaves/ends/length (10–12); package-private constructor (14–39); length (40), charAt (41–49), subSequence (50–60), toString (61). | Object equals/hashCode; M3StringPiece.flatten and CharSequence defaults. Public methods do not make the class public. | T1–T3; overflow/allocation limits and joined object identity are not directly asserted. |
| [StorageIdentity](core-storage-reference.md#storageidentity-record-value-not-admission-authority) | Public record components owner/generation/record (10); compact constructor (11–14). No additional handwritten methods or fields. | Component backing fields/accessors, equals/hashCode/toString and canonical constructor machinery; component semantics differ from piece Object equality. | T1/T4 use equals and HashSet identity collection; direct constructor refusal, accessors and diagnostic representation are not focused tests. |
| [SharedLexiconImage](core-storage-reference.md#sharedlexiconimage-state-and-validation) | Constants MAGIC/HEADER/MAX_IMAGE_BYTES/MAX_RECORDS (22–25); retained mapping/version/offsets/lengths/recordDigests/payloadOffset/utf16Units/imageIdentity (26–32); private constructor (34–70); 19 methods detailed immediately below. No nested type. | Ordinary Object behavior; no generated record/enum, AutoCloseable, close/unmap or implicit public constructor. | T5/T6 direct image cases; T7 selected indirect catalog calls. No blanket helper API qualification. |

### SharedLexiconImage complete method ledger

| Visibility and member | Lines | Detailed route / evidence |
| --- | --- | --- |
| public static `open(Path)` | 72–78 | [Constructor/open validation](core-storage-reference.md#constructor-and-open-sequence); T5/T6 |
| public `warm()` | 80 | [Public methods](core-storage-reference.md#public-methods-and-publication-boundary); invoked by T5, no residency assertion |
| public `imageIdentity()` | 81 | Public scalar metadata; T5 equality across two opens, not cryptographic authentication |
| public `version()` | 82 | Public scalar metadata; explicit v2 assertion in T5 |
| public `size()` | 83 | Public scalar metadata; T5 record count |
| public `mappedBytes()` | 84 | Mapping limit; no direct FoundationTest assertion |
| public `utf16Units()` | 86 | Retained payload dimension; used by selected catalog openImage validation, no direct scalar assertion in T5 |
| private `recordBytes(int)` | 88–92 | [Private helper ledger](core-storage-reference.md#private-helper-ledger); index-first slice path, exercised indirectly by T5/T6 |
| private `javaHash(int)` | 94–102 | Opening helper; v2 success fixture in T5, no isolated bad-hash refusal case |
| private `compareRecords(int,int)` | 104–113 | Opening helper; sorted Unicode fixture, no dedicated duplicate/descending rejection case |
| package-private `recordText(int)` | 116–125 | [Lookup/materialization](core-storage-reference.md#package-private-lookup-and-materialization); T7 indirect catalog text path |
| package-private `findRecord(String)` | 128–132 | Lookup exact match/-1; T7 indirect selected find cases |
| package-private `lowerBound(String)` | 135–144 | Lookup insertion point `0..size`; T7 indirect catalog prefix only |
| package-private `startsWith(int,String)` | 147–158 | Null-before-index prefix test; T7 indirect nonempty prefix only |
| private `compareRecordTo(int,String)` | 160–171 | Index access before value access internally; helper caller null discipline |
| public `copyRecord(int,LocalM3Arena)` | 173–183 | [Public methods](core-storage-reference.md#public-methods-and-publication-boundary); T5/T6 successful copies and selected refusal/mutation |
| public static `create(Path,List<String>)` | 186–214 | Public file creation; T5/T6 happy v1 and existing-destination refusal |
| private static `digestImage(ByteBuffer)` | 216–223 | [Private helper ledger](core-storage-reference.md#private-helper-ledger); `[0,32)` + `[64,limit)` digest; T6 header reinterpretation and file mutation rejection |
| private static `digest(ByteBuffer)` | 224–227 | Consumes temporary buffer remainder; opening/copy digest path, T5/T6 |

## Finite test attribution

The complete [FoundationTest.java][TEST] is 198 lines / 17,914 bytes, Git blob
`822bb5d6b77ebb166a5cefd8f8507a2b073f17d2`, raw SHA-256
`9c3c1480807ccb15ed6ee81dafa91a6e2a9f4bcc8c749caf0ea09ad5bbe6485b`.
The helper `expect` tests throwable type membership; it does not establish exact
message text, all error-priority combinations or absence of partial effects.
Reading its `FOUNDATION_PASS` print statement is not an observed pass.

| ID / body lines | Assertions present | What that evidence does not establish |
| --- | --- | --- |
| T1: pieces 20–36 | 1,000 seeded arrays, lengths 0–64, units drawn from 0–65535; copied input isolation; randomized split; slice identity equality/offset; mutated input-directory isolation; each joined char and each suffix slice; separate copies have different identities. | Exhaustive Unicode/range/segmentation coverage, content-equality override, generated member/ABI coverage, atomic snapshot of concurrently mutated input. |
| T2: pieces 37–44 | All 256 Latin-1 units after input mutation; explicit unpaired surrogate/NUL/FFFF fixture; 10,000 repeated joins with empty pieces preserve text. | Memory/throughput benchmark, zero allocation, exhaustive surrogate stream behavior, maximum array/directory length or native memory. |
| T3: pieces 45–49 plus chainIndex 55 | Local charAt(-1), charAt(256), empty joined charAt(0), null join element, reversed local slice range are refused with selected throwable classes. | Null join-array/copy inputs, every bounds pair, combined-invalid ordering, malformed trusted constructor/owned calls, enum valueOf or inherited defaults. |
| T4: pieces 50–53 | Eight workers each create 1,000 UTF-16 pieces through one arena; collected identity union has 8,000 entries. | Long.MAX_VALUE exhaustion, UUID collision proof, counter rollback, concurrent external array mutation or general concurrency/linearizability certification. |
| T5: images 66–74; writeV2/javaHash 56–65 | create/open two v1 owners; warm invoked; equal imageIdentity and expected record count; copies of six ordered Unicode records (`""`, a, U+0100, unpaired high, supplementary pair, unpaired low); explicit v2 fixture/version and copies; save a local record. | Empty image/zero-record lookup, maximum file/count limits, mappedBytes direct assertion, complete format variants or warm residency. Test writeV2 does not make production create emit v2. |
| T6: images 75–89 | Header/digest reinterpretation rejected; existing destination refused; invalid negative copy index; later file mutation leaves saved local copy unchanged and causes selected copy/open failure; tiny/missing file; explicit local fallback; Integer.MAX_VALUE record count rejected. | The changedHeader fixture preserves its dimension equation and only expects IOException: it is not an isolated invalid-dimensions guard proof. No every-version/range/hash/digest/order/null/error-order corpus, no hostile concurrent truncation, interrupted-write cleanup, crash recovery, durability or deterministic unmap proof. |
| T7: catalog 100–179 and optional exportedCatalog 180–196 | Separate catalog integration assertions include exact find/missing, prefix, materialized text, v2 shard metadata/sidecars, lazy failure/retry/close; exported fixture expects 10,004 records and selected mappings/prefixes when supplied. | Catalog owners remain outside the six-primary denominator. These selected indirect image calls do not independently test every image helper, image empty-prefix/null/index behavior or catalog-wide completion. |

`main` (197) calls pieces, images and catalog, and exportedCatalog only when one
argument is supplied, then performs test-directory cleanup. None of these paths
was run here. Fixture cleanup is not a product image close/unmap API or a promise
that mapped-file deletion behaves identically on every OS.

## Caller and build support: separate from the six primary rows

| Public source at the same pin | Exact Git blob SHA-1 | Read scope and use |
| --- | --- | --- |
| [m3/core/src/module-info.java][MODULE] | `bdd278210a498e2665620c02413b1e658c79370e` | Complete 5-line descriptor; exports independent com.m3.text. |
| [m3/build.sh][BUILD] | `70b50228bba3775b6feacd500eca3f3c7529e0f2` | Complete script read: required M3_JDK tool-directory variable (no version probe), --release 21 compilation/JAR/test construction, jit/int/nocompact/c2 invocations, separate demo-image publication flow. No execution in this pass. Its temp/chmod/mv sequence is a build caller's behavior, not SharedLexiconImage.create's guarantee. |
| [SharedLexiconCatalog.java][CATALOG] | `df137d5b2a52e9ad58f276132099b47692720ed2` | Selected call bodies: find 247–265, prefix 272–288, textAt 291–299, validateShardSidecars 355–375, openImage 455–466, imageText 679–683. Uses findRecord/lowerBound/startsWith/recordText and image metadata; reuse complete catalog reference. |
| [M3PrefixZ.java][PREFIXZ] | `317108234d7692410517731b325f7d28de99354e` | Complete body read, selected consumer at 42–85: sealed piece, length and charAt; reuse existing algorithm reference rather than add a seventh primary owner. |
| [java/lang/CharSequence.java][CHARSEQ] | `87bc4a5525a850a7323a784a1927e6221d706ed6` | Selected inherited declarations/default bodies and static compare distinction, not a new whole-Java-SE API claim. |

The four generated/inherited categories tracked above are Java language/library
semantics: record components/accessors/value methods, enum values/valueOf/Enum
behavior, CharSequence defaults and covariant bridge machinery. There is no
classfile generation or enumeration result in this pass.

## Policy and public-evidence limits

The current public AGENTS instructions, porting invariant, naming-map
authority/relevant storage rows, historical full-family pin/catalogue and current
full-borrow README/pin/receiver-status/promotion-phase snapshot were checked as
target-side policy evidence. The policy's 4,770-path historical catalogue and
32-family successor inventory are not storage acceptance or completion counts.
String qualification and ordered promotion remain unchanged. The policy verifier
was deliberately **NOT_RUN in this pass** under the documentation-only/no-repository-execution
scope; policy files were not edited and no private donor source was accessed.

The unchanged overview, current body identities, complete member routing and
finite tests-read map establish a bounded documentation-depth closure only.
Allocation-failure injection, explicit exhaustion, malformed internal admission,
generated-member behavior suites, helper edge matrices, file mutation races,
mapping reclamation, native/JNI/platform behavior, exact runtime/consumer fit and
resource/performance proof remain unqualified by this pass.

[JOINED]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/core/src/jdk/m3/text/JoinedM3StringPiece.java
[ARENA]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/core/src/jdk/m3/text/LocalM3Arena.java
[LOCAL]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/core/src/jdk/m3/text/LocalM3StringPiece.java
[PIECE]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/core/src/jdk/m3/text/M3StringPiece.java
[IMAGE]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/core/src/jdk/m3/text/SharedLexiconImage.java
[IDENTITY]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/core/src/jdk/m3/text/StorageIdentity.java
[TEST]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/core/test/FoundationTest.java
[MODULE]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/core/src/module-info.java
[BUILD]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/build.sh
[CATALOG]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/core/src/jdk/m3/text/SharedLexiconCatalog.java
[PREFIXZ]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/m3/algorithms/src/jdk/m3/algorithm/M3PrefixZ.java
[CHARSEQ]: https://github.com/hsoliwal/M3jdk21/blob/510dfa27b0679ce33348140afc57b558b8853802/src/java.base/share/classes/java/lang/CharSequence.java
