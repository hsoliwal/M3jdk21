<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# com.m3.text source coverage and reading routes

[Guide](README.md) · [Projection contracts](text-projection-reference.md) · [Coverage ledger](coverage.md)

This is a complete **physical source-file routing map** for the public
repository directory `m3/core/src/com/m3/text` at
`8758991645037126985d56f16a208fec8a1eef38`. Its unfiltered [directory listing][DIR]
contains exactly 17 Java files and no `package-info.java`. The count excludes
the sibling [JPMS descriptor][MODULE] and `core/test`; nested records/enums
belong to their enclosing file and are not extra rows. All sources are publicly
readable, but this is not a list of 17 public Java types:
`JoinedM3StringPiece` is package-private.

The earlier [storage review](m3-packages.md#textstorage-api-contracts) read six
full bodies at `278b221e8365ebeb6095a3a5643b38a0564258b5`. The
[catalog review](lexicon-catalog-reference.md) read six more at
`c038343521587d96c9fa190715ce2f4b9f8c490a`. Directory/blob comparison against
both historical pins confirms that **all 17 current blobs are unchanged**.
The five remaining owners now have complete body and declared-member treatment
in the [projection/tool/receipt reference](text-projection-reference.md).

This adds a route for every physical owner; it does not rewrite earlier scope
statements or claim new execution of those earlier reviews. In particular,
the catalog reference's exclusion of number/instance/grammar/LangDex owners
describes that six-owner pass and remains true for that pass. The new reference
is its additive continuation. The [indexed-string bridge](indexed-string-reference.md)
remains a separate cross-package consumer/reference route, not another source
file to add to this directory's denominator.

## Seventeen-source routing map

| Physical source at the current pin | Exact Git blob SHA | Contract reference and review origin |
| --- | --- | --- |
| [JoinedM3StringPiece.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/jdk/m3/text/JoinedM3StringPiece.java) | `5022e9811ce9391671c794446c7772569cfd369f` | [Existing six-owner storage review](m3-packages.md#textstorage-api-contracts) |
| [LexiconTool.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/jdk/m3/text/LexiconTool.java) | `daee29e96b6a6b5906b4603358a1ebd03a802337` | [New complete body/member review](text-projection-reference.md#lexicontool-explicit-file-effects-and-diagnostic-lifetime) |
| [LocalM3Arena.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/jdk/m3/text/LocalM3Arena.java) | `025ce8014cba8a36a9e496bb0a56faef57cf0968` | [Existing six-owner storage review](m3-packages.md#textstorage-api-contracts) |
| [LocalM3StringPiece.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/jdk/m3/text/LocalM3StringPiece.java) | `387ae5e0c656177f1df663fd5029d4b31ba2fa00` | [Existing six-owner storage review](m3-packages.md#textstorage-api-contracts) |
| [M3InstanceIndexPrecompute.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/jdk/m3/text/M3InstanceIndexPrecompute.java) | `c4a499d6ef46e38ed5f12b0789683cae53309964` | [New complete body/member review](text-projection-reference.md#m3instanceindexprecompute-operator-records-and-exact-scope) |
| [M3LanguageGrammarSupport.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/jdk/m3/text/M3LanguageGrammarSupport.java) | `0ac0ecb7d4abe99f5c3b7a21e0099e05a92bb168` | [New complete body/member review](text-projection-reference.md#m3languagegrammarsupport-one-immutable-signal) |
| [M3LexiconPrecompute.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/jdk/m3/text/M3LexiconPrecompute.java) | `4ea2db8d3b2bfec5f720ef499b8b697186718cb9` | [Existing six-owner catalog review](lexicon-catalog-reference.md#m3lexiconprecompute-typed-values) |
| [M3NumberSpace.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/jdk/m3/text/M3NumberSpace.java) | `69f517474f58e756f770fc59dc2ea1ab982fbaef` | [New complete body/member review](text-projection-reference.md#m3numberspace-generated-range-and-explicit-materialization) |
| [M3StringPiece.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/jdk/m3/text/M3StringPiece.java) | `62506fb9b3cff0e6f24e856648528db58c45b92b` | [Existing six-owner storage review](m3-packages.md#textstorage-api-contracts) |
| [SharedLexiconCatalog.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/jdk/m3/text/SharedLexiconCatalog.java) | `df137d5b2a52e9ad58f276132099b47692720ed2` | [Existing six-owner catalog review](lexicon-catalog-reference.md#sharedlexiconcatalog-startup-first-touch-and-close) |
| [SharedLexiconFamilySidecarCatalog.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/jdk/m3/text/SharedLexiconFamilySidecarCatalog.java) | `8dd76c1d5911dbf4b4b786098c1223fcdbb57b63` | [Existing six-owner catalog review](lexicon-catalog-reference.md#five-family-file-sidecars) |
| [SharedLexiconImage.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/jdk/m3/text/SharedLexiconImage.java) | `364eecefcb3822918c849606ddb177c0ceaa30d5` | [Existing six-owner storage review](m3-packages.md#sharedlexiconimage-file-and-lifetime-contract) |
| [SharedLexiconPrecomputeCatalog.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/jdk/m3/text/SharedLexiconPrecomputeCatalog.java) | `ce34ac405c41b3980e558efc688a5ebc1f37b47b` | [Existing six-owner catalog review](lexicon-catalog-reference.md#in-memory-sharedlexiconprecomputecatalog) |
| [SharedRelatedLexemeCatalog.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/jdk/m3/text/SharedRelatedLexemeCatalog.java) | `bb8931eeb17563238c92f6c1ee200fb1905595c3` | [Existing six-owner catalog review](lexicon-catalog-reference.md#related-lexemes-direction-and-multiplicity) |
| [StorageIdentity.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/jdk/m3/text/StorageIdentity.java) | `8cd2ae86bee8f04f0d69709eed12897ff5c6a71c` | [Existing six-owner storage review](m3-packages.md#textstorage-api-contracts) |
| [SynexiaLangDexSourceEnvelope.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/jdk/m3/text/M3LangDexSourceEnvelope.java) | `b3f2291429429c62825a292bd5b7f618046bed27` | [New complete body/member review](text-projection-reference.md#synexialangdexsourceenvelope-verification-without-admission) |
| [SynexiaPrecomputePayload.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/jdk/m3/text/M3PrecomputePayload.java) | `5896894dec303b94847de9755033189e2112010e` | [Existing six-owner catalog review](lexicon-catalog-reference.md#typed-per-record-payload) |

The original source-sampled package inventory stays intact. The accounting here
advances bounded body-reference coverage from six storage plus six catalog
owners to a routed set of seventeen; it does not relabel the whole package,
project, repository, Java SE API or runtime as complete.

## Five-owner declaration audit

The new [reference](text-projection-reference.md) covers all explicitly declared
fields, constructors and methods in the five bodies, plus record components and
enum constants. Compiler-generated record/enum methods are described by their
ordinary Java semantics where relevant; they are not represented as handwritten
implementations.

| Owner | Declared surface and implementation boundaries covered |
| --- | --- |
| LexiconTool | No fields; private constructor; main build/map dispatch, delegated image operations, printed diagnostics, delay/fence and propagated errors |
| M3NumberSpace | Seven public constants/INSTANCE fields; private values/backing; private constructor; number, spelling, sourceRecordId, parse, precomputedValueCount, storageIdentity; private describe/checkValue |
| M3InstanceIndexPrecompute | UTF8_ORDER and seven retained fields; public constructor; seven label/collection accessors; size, referencedConceptCount, appliesTo, findByName, instancesOf, normalizedInstanceName; InstanceType's eight values; InstanceRecord's four components/compact constructor; immutableConcepts, immutableMetadata, text, validateName, asciiWhitespace, validateUtf8, compareUtf8 |
| M3LanguageGrammarSupport | SOURCE_ID, TARGET_FIELD, SUPPORTED; private constructor; supports and supportedCodes |
| SynexiaLangDexSourceEnvelope | Eight private constants/Patterns; private constructor; Receipt's four components/compact constructor; verify; readAsciiLine, requireCanonicalLong, decodeNullable, requireUnused, increment, sha256 |

### Exact bytes for the new owners

These SHA-256 values describe the full UTF-8 source bytes fetched at the pin,
distinct from the Git blob identifiers in the routing table.

| Source file | SHA-256 |
| --- | --- |
| LexiconTool.java | `fb3b3a4db8bb699a326d9e2b1114ed04089400294349f97bde0ce6b73e1aa51c` |
| M3NumberSpace.java | `6c70f843dca2bc099c99de24fb21987986e6f0ed03a79677ad88f65d27f0273e` |
| M3InstanceIndexPrecompute.java | `2be8634db1d545bce03ba34cab2569b2adbb869b19544acd2f42bb4e02a7b5f1` |
| M3LanguageGrammarSupport.java | `f2f6f8dcb6b0a0c91569a9eee0ea199c0e77a24594033534351408b2c8ccfff3` |
| SynexiaLangDexSourceEnvelope.java | `7093a00bd467cd4a477512eaae58546588e5881a6cf3dd081fdc843c88b7120d` |

## Build, tests and supporting-source scope

[The module descriptor][MODULE] exports `com.m3.text`. The complete
[build script][BUILD] compiles core sources and its explicitly listed main-based
tests with `--release 21` using the tools in the nonempty `M3_JDK` directory,
runs four JVM modes, then invokes LexiconTool to build the demo image. Its
environment error requests an isolated JDK 21, but the script does not verify
version, vendor or isolation. `m3/core` is absent from the
[Maven control-reactor module lists][POM]. The source directory is a package in
the exported independent module, not a java.base package and not an inferred
Maven reactor child.

The following test/fixture bodies were read in the new five-owner review.
Assertion-level scope and unsupported cases are in the
[test-attribution matrix](text-projection-reference.md#test-attribution-and-open-qualification).
Every entry is **NOT_RUN** in this documentation pass.

| Source under `m3/core/test/` unless noted | Exact Git blob SHA | Review role |
| --- | --- | --- |
| [M3NumberSpaceTest.java][NT] | `7c6dbeb96a7495cecf95058356a97ce090f6202a` | Direct number-view/parser assertions |
| [M3NumberSpaceContractTest.java][NC] | `f23f5f62d1b5aa6f66ad1a43ac935af07b3f82d2` | Full range and source/target/field-map assertions |
| [M3NumberSpaceExportFixture.java][NF] | `4c1981d060e56ec0fc448652cf50b9a50ab0e1ff` | Java-owner TSV producer helper, not in core build test list |
| [M3InstanceIndexPrecomputeTest.java][IT] | `875cc39d069dbb5095fa8fc1ef9892352200e676` | Operator-proposal construction/lookup/selected rejection assertions |
| [M3LanguageGrammarSupportTest.java][GT] | `cfbeac6d55cb6866ff98833f971c1f1d4ff016f1` | Sixteen support signals and manifest mapping |
| [LangDexSourceEnvelopeTest.java][ET] | `7e7a80a03c671886a8b49afd1fc9e97fcfa151b6` | Wire/receipt fixture and focused rejection cases |
| [LangDexProjectionReceiverTest.java][PT] | `8f56e44a5927309d54d9a82b651b383271218a1f` | Separate synthetic catalog/payload route; not envelope admission |
| [FoundationTest.java][FT] | `822bb5d6b77ebb166a5cefd8f8507a2b073f17d2` | Existing image/storage tests and optional exported catalog path |
| [m3/runtime-integration/tests/test_m3_number_space_export.py][NP] | `401f4b281d5c66d8082e1a965fc94c301a5862f7` | Fixture-driven exporter/verifier orchestration and deterministic output assertions |

The complete core listing contains no dedicated LexiconTool test. Its concrete
callers inspected here are [build.sh][BUILD] and
[verify-shared-mapping.py][MAP]. The latter is a Linux two-process
same-read-only-inode diagnostic and explicitly disclaims PSS/memory-saving
measurement. Reading its body is not executing it.

The [number target map][NM], [instance contract][IM],
[source manifest][SM], [field map][FM] and relevant
[export-policy sections][EX] were inspected for target ownership, source
identity, generated numeric envelope metadata and the distinction between
LangDex intake and admission. Their source links retain the same pin.
The exporter/verifier implementations, producer repository, operator datasets
and full repository-wide inbound call graph are not newly audited by this page.
No empty search result is used as proof that additional callers cannot exist.

## Remaining boundaries

- No `package-info.java` exists in this directory; external references do not
  supply inline package prose
- All builds, tests, examples, scripts, policy/recipe verifiers, benchmarks and
  matched-runtime/platform checks are **NOT_RUN by this authoring/review pass**
- Number-space identity is generated for an M3-defined range; a family key,
  decimal source ID, local storage identity and image coordinate remain distinct
- The instance schema is a target-owned proposal over operator data; receipt
  labels and fingerprints are not authenticated, and forward/reverse name order
  differs in the current implementation
- Grammar support is a Boolean signal; grammar data and execution remain with
  their source provider
- A LangDex receipt validates exact bytes against supplied expectations; it
  does not authenticate provenance, resolve duplicate identities, join concept
  links or admit rows to a catalog
- Existing storage/catalog test gaps remain at their original references.
  Resource-failure, hostile/concurrent-input, platform/lifetime and source-license
  qualification remain open
- Hosted CI must be reported separately for its exact commit and scope. No
  source, configuration, workflow, license, inventory denominator, canonical
  owner or ordered receiving phase is changed by this documentation increment

[DIR]: https://github.com/hsoliwal/M3jdk21/tree/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text
[MODULE]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/module-info.java
[BUILD]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/build.sh
[POM]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/pom.xml
[MAP]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/verify-shared-mapping.py
[NT]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/test/M3NumberSpaceTest.java
[NC]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/test/M3NumberSpaceContractTest.java
[NF]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/test/M3NumberSpaceExportFixture.java
[IT]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/test/M3InstanceIndexPrecomputeTest.java
[GT]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/test/M3LanguageGrammarSupportTest.java
[ET]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/test/LangDexSourceEnvelopeTest.java
[PT]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/test/LangDexProjectionReceiverTest.java
[FT]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/test/FoundationTest.java
[NP]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/runtime-integration/tests/test_m3_number_space_export.py
[NM]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/lexicon/m3-number-target-map.tsv
[IM]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/lexicon/m3-instance-index-contract.tsv
[SM]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/lexicon/m3-source-manifest.tsv
[FM]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/lexicon/m3-precompute-field-map.tsv
[EX]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/lexicon/M3_EXPORT.md


## Six-owner member-depth continuation, 15 October 2026

The historical seventeen-source table above remains unchanged. The following
additive routes extend its same six storage rows from the earlier selected
contract overview to complete declared-member/state/helper/accessor coverage at
`510dfa27b0679ce33348140afc57b558b8853802`. Each current blob was freshly checked
and still equals its historical table identity. This changes reference depth,
not the package's seventeen-file denominator or the prior review's dated scope.

| Existing storage row | Additive member route |
| --- | --- |
| JoinedM3StringPiece.java | [Non-public flat directory](core-storage-reference.md#joinedm3stringpiece-non-public-flat-directory) |
| LocalM3Arena.java | [Allocation and trusted admission](core-storage-reference.md#localm3arena-allocation-and-trusted-admission) |
| LocalM3StringPiece.java | [Backing, encoding and range members](core-storage-reference.md#localm3stringpiece-immutable-public-view-over-trusted-owned-bytes) |
| M3StringPiece.java | [Declared and inherited surface](core-storage-reference.md#m3stringpiece-declared-and-inherited-surface) |
| SharedLexiconImage.java | [State, validation and lookup/copy boundaries](core-storage-reference.md#sharedlexiconimage-state-and-validation) |
| StorageIdentity.java | [Record components and generated value semantics](core-storage-reference.md#storageidentity-record-value-not-admission-authority) |

The [exact source/member/test map](core-storage-source-coverage.md) accounts for
all six bodies (427 lines), generated/inherited members separately, selected
catalog/PrefixZ callers and finite FoundationTest assertions. Tests and repository
execution are **NOT_RUN in this documentation pass**. Hosted CI remains separate
exact-commit/workflow evidence. The six catalog owners and five projection/tool/receipt
owners keep their existing routes; bootstrap String, retained MIndexString,
joined-array and mapped/native backing owners remain physically separate.
