# Exact collection source inventory and catalogue links

`collection-file-review-20261017.tsv` contains **80 source inventory rows in 24 curated categories**. The source set is exactly the local Java files bound by the passing `core80-02.json` Java 21 compile receipt. It includes implementation owners, interfaces, support types and the two package/module descriptors. It is not a claim that these are all Java files at current M3JDK21 master.

Every complete source was read serially, SHA256 checked against that receipt, then re-read and checked before output. Role/category/operation/storage text is a declaration and documentation review. It is a selected storage summary, not an exhaustive AST field extraction. Nested/multiline/inherited declarations, generated methods and arbitrary method bodies are not exhaustively analyzed by the lexical receipt. Names and categories establish retrieval candidates only. The 80 rows explicitly retain `REVIEW_REQUIRED` for interface and behavior proof, and admit no replacement or performance winner.

The companion `collection-candidate-comparison-20261017.tsv` preserves the existing M3 category/candidate/source/revision/rights/contract/evidence/decision format and adds stable `donor_link_id` values. Join each file's semicolon-separated links to those IDs. Exact donor bodies that retain nodes or HeapElement objects are refused as direct target storage donors. Problem sites provide taxonomy only; no site editorial, submission or article implementation was copied. Licensed problem repositories are separately pinned corpus-discovery links, with individual-file review still pending. Guava candidate 33.7.2 remains distinct from the target SDK's 33.4.8-jre. Eclipse 11.1.0 ListAdapter carries EPL-1.0 and EDL-1.0 source-header notices.

## Reuse the existing searchable/indexed components

These TSV files add data links, with no new Java search engine, CLI, runtime dependency or production edit. Existing APIs at `hsoliwal/com.synexia@0a5d36185342e6f59d5cd8e4321ea0ab0d9aeecd` remain the retrieval and review surfaces:

- `AlgorithmCatalogue.search(term)` returns existing selected algorithm entries. Its fixed list does not automatically ingest this TSV.
- `PinnedAlgorithmDonorCatalog.byKind(...)`, `revisions(repository)` and `require(repository, revision)` use the packaged authoritative `synexia-algo/corpus/donors.tsv`, blob `4439f6251f7ca5ebe06d6e4c131990552e60c9d9`. New candidate pins in this comparison table do not silently alter that manifest. A repository at another revision is not an exact pin match.
- `ProblemCategoryCrosswalk.index(occurrences, exactKey, originKey, topics, monitor)` delegates postings/grouping to the existing `AdditiveTemplateIndex`. Supply exact content identities and source-provided platform topic labels. A caller may use `source_path + SHA256` as the exact occurrence identity; curated local category strings must not be misrepresented as externally observed native labels. Unsupported native labels remain unresolved.
- `SerialProblemReview` requires real interface hashes, an exact candidate atom, independent current whole-file observation and an independently executed differential proof verifier for eligibility. This source TSV is not a fabricated `SourceFile` admission packet: it deliberately contains no invented interface or differential proof digest.
- Existing `M3SerialFileAtomConvergenceRecipe`, blob `d9204d36131fd080e1bfb8b120f646beefb0a5e6` in `com.synexia`, supplies the serial inventory/compare/proof lifecycle. Its inventory plan has no copy, mutation, replacement or promotion authority. Follow the target PORTING invariant and qualify one real file before moving to the next.

These are documented caller-side integration points; this unit has not executed their Java methods. Apache adapter and JNI oracle results from other validation units apply only to their exact qualified sources and contracts, not transitively to these 80 files.

## Exact source/proof binding

Source-set SHA256: `fe4a87b11ba635110b41f3168196b1e3fb5a30b9814e2fda05d525984b688acd`. Definition: concatenate UTF-8 `source_path<TAB>sha256<LF>` in ordinal order, then SHA256. Shared compile receipt SHA256: `7fe7626117be708f6b357756bb94325a20417dbb442a3d7d5315d4e36ad7bad5`.

The compile proves that this exact set compiles together with Java 21 and warnings treated as errors. It does not prove per-type behavior, all interfaces against another implementation, JNI parity, allocation/performance budgets, GC, serialization, concurrent linearizability or complete master coverage.

The local-only generator commands are `python -I -S -B <authorized-root>\catalog-unit\source_inventory.py` followed by `python -I -S -B <authorized-root>\catalog-unit\build_catalog_data.py`. Each reuses the root native-temp/reserve gate; the latter rechecks source hashes and TSV cardinality/link integrity. They launch no JVM, build or tests. The publishable target is static TSV/Markdown data; inventory helpers and raw local receipts remain in authorized scratch.
