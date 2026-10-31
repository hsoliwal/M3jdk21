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
| [JoinedM3StringPiece.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/JoinedM3StringPiece.java) | `cc3c84f5ea0230e2f57e3d3ae0f5d29b9241e597` | [Existing six-owner storage review](m3-packages.md#textstorage-api-contracts) |
| [LexiconTool.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/LexiconTool.java) | `32ee9e317d9c2a464958b0cfe256b457edb2f295` | [New complete body/member review](text-projection-reference.md#lexicontool-explicit-file-effects-and-diagnostic-lifetime) |
| [LocalM3Arena.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/LocalM3Arena.java) | `d82710cd1a49b54d92afcd74cdd903cd009766ba` | [Existing six-owner storage review](m3-packages.md#textstorage-api-contracts) |
| [LocalM3StringPiece.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/LocalM3StringPiece.java) | `6b745c9472144ad6de01de4d06a533974e558fb1` | [Existing six-owner storage review](m3-packages.md#textstorage-api-contracts) |
| [M3InstanceIndexPrecompute.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/M3InstanceIndexPrecompute.java) | `e6fd8e4fa71c93d62874886135c7bb703141894b` | [New complete body/member review](text-projection-reference.md#m3instanceindexprecompute-operator-records-and-exact-scope) |
| [M3LanguageGrammarSupport.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/M3LanguageGrammarSupport.java) | `135a6a0c080603da0f4c095519b7d47315e2b281` | [New complete body/member review](text-projection-reference.md#m3languagegrammarsupport-one-immutable-signal) |
| [M3LexiconPrecompute.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/M3LexiconPrecompute.java) | `edec2ae3a82bde34159abd9a4d1c670ad59f031f` | [Existing six-owner catalog review](lexicon-catalog-reference.md#m3lexiconprecompute-typed-values) |
| [M3NumberSpace.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/M3NumberSpace.java) | `ccd577c1b1ec62709882d3ef54db8c5d8a19557b` | [New complete body/member review](text-projection-reference.md#m3numberspace-generated-range-and-explicit-materialization) |
| [M3StringPiece.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/M3StringPiece.java) | `99cb72623fa6bc2d5426553e04aaf0e5bfa993cc` | [Existing six-owner storage review](m3-packages.md#textstorage-api-contracts) |
| [SharedLexiconCatalog.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/SharedLexiconCatalog.java) | `aa8cf83bbc8349a2ed5a0a7dad818c2d7240f577` | [Existing six-owner catalog review](lexicon-catalog-reference.md#sharedlexiconcatalog-startup-first-touch-and-close) |
| [SharedLexiconFamilySidecarCatalog.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/SharedLexiconFamilySidecarCatalog.java) | `e991a6ba84d7074e9bd260cce331d5092e7ad9f5` | [Existing six-owner catalog review](lexicon-catalog-reference.md#five-family-file-sidecars) |
| [SharedLexiconImage.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/SharedLexiconImage.java) | `7d0dd69b3f92bc6e589eed4024d22686020b421c` | [Existing six-owner storage review](m3-packages.md#sharedlexiconimage-file-and-lifetime-contract) |
| [SharedLexiconPrecomputeCatalog.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/SharedLexiconPrecomputeCatalog.java) | `b952a245b2724140904bb666669c2880ce32cd64` | [Existing six-owner catalog review](lexicon-catalog-reference.md#in-memory-sharedlexiconprecomputecatalog) |
| [SharedRelatedLexemeCatalog.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/SharedRelatedLexemeCatalog.java) | `182042dfc5115a683449622dcdc6055e916f4e4c` | [Existing six-owner catalog review](lexicon-catalog-reference.md#related-lexemes-direction-and-multiplicity) |
| [StorageIdentity.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/StorageIdentity.java) | `7f0a0bb63d38a0a6efbe08f37249dd3e258eb35a` | [Existing six-owner storage review](m3-packages.md#textstorage-api-contracts) |
| [SynexiaLangDexSourceEnvelope.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/SynexiaLangDexSourceEnvelope.java) | `7f9ac5ebfc452aadb869103471e6818339746083` | [New complete body/member review](text-projection-reference.md#synexialangdexsourceenvelope-verification-without-admission) |
| [SynexiaPrecomputePayload.java](https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/core/src/com/m3/text/SynexiaPrecomputePayload.java) | `682e7f2abd59efd2adf30fb039616c61a16aa029` | [Existing six-owner catalog review](lexicon-catalog-reference.md#typed-per-record-payload) |

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
| LexiconTool.java | `8f86470619da33cf79ce3f8c6b99e7b6a4815f1c0053c7a07985a6ea4010563a` |
| M3NumberSpace.java | `99bc5c5be96dfb4e0db2f5dbc837c568333ffc6853922cee6ba6566130513443` |
| M3InstanceIndexPrecompute.java | `43af63196141d180c1d256b8420c243d99804ee480b430f31d677e57a87a041c` |
| M3LanguageGrammarSupport.java | `32e55e0ddc412cbbdb4c3fe22b659fe9660bed914215c92662ccaf00b189b1c7` |
| SynexiaLangDexSourceEnvelope.java | `90b81d145a795dae62e57afe850ad7d38704fe4fcfad69614ff4cd598eb0a084` |

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
| [M3NumberSpaceTest.java][NT] | `11ebbe9c9e96508fb4a5520268d49fe5117c2cf3` | Direct number-view/parser assertions |
| [M3NumberSpaceContractTest.java][NC] | `72fc41be172bfe9bb88c28485e1952e6f6325a43` | Full range and source/target/field-map assertions |
| [M3NumberSpaceExportFixture.java][NF] | `3390bdce6286788395cf92c2a141afc25ac74c39` | Java-owner TSV producer helper, not in core build test list |
| [M3InstanceIndexPrecomputeTest.java][IT] | `738e1025367b4aa7ad647691bfd17dfc8d3be5c7` | Operator-proposal construction/lookup/selected rejection assertions |
| [M3LanguageGrammarSupportTest.java][GT] | `316b8822120b42850dde74921a4b34ba2bb0d6cd` | Sixteen support signals and manifest mapping |
| [LangDexSourceEnvelopeTest.java][ET] | `e23a36f4fb86d93f98ff4e1571bd3f227413563b` | Wire/receipt fixture and focused rejection cases |
| [LangDexProjectionReceiverTest.java][PT] | `00cb70ca8b2aa93c903b972269ec810f47b34f18` | Separate synthetic catalog/payload route; not envelope admission |
| [FoundationTest.java][FT] | `c722aa61a9bc7953d183e1fac20c0155a19dd2dd` | Existing image/storage tests and optional exported catalog path |
| [m3/runtime-integration/tests/test_m3_number_space_export.py][NP] | `1d20650067803940820354696446ddcfd7cd3ade` | Fixture-driven exporter/verifier orchestration and deterministic output assertions |

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
[NM]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/lexicon/synexia-number-target-map.tsv
[IM]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/lexicon/synexia-instance-index-contract.tsv
[SM]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/lexicon/synexia-source-manifest.tsv
[FM]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/lexicon/synexia-precompute-field-map.tsv
[EX]: https://github.com/hsoliwal/M3jdk21/blob/8758991645037126985d56f16a208fec8a1eef38/m3/lexicon/SYNEXIA_EXPORT.md


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
