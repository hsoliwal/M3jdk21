# PMEMSDK sparse first-hit review — private source atom

**Scope:** One additive, package-private Java21 class inside the existing PMEMSDK campaign. The original public `PmemCorpusSearch` file, all `PmemSourceImage`/`PmemRegion` interfaces and all JNI C17 exports remain unchanged. This is a selected-file search *signal*, not category/donor adoption or source-modification authority.

## Contracts

- `firstHits(image, sortedUniqueFileIndices, pattern, maxHits, cancellation)` accepts a *reviewed* list of canonical corpus indices. All indices are copied/validated before the first callback/JNI search; negative, duplicate, descending and out-of-range indices fail closed.
- Selected count ≤20,000; pattern ≤ incumbent 1 MiB; results ≤20,000. Selected empty set returns complete with zero hits. Empty pattern matches every selected file (including zero-byte files) at byte offset 0.
- Skip a file shorter than the binary pattern without entering the incumbent JNI search; otherwise delegate to `PmemSourceImage.indexOf`, preserving its mapping leases and first-hit 64-bit **file-relative** positions. No match may cross adjacent packed file extents.
- The result contains only private primitive index/offset arrays, canonical root, exact pattern digest and a **selectionSha256** framed over source generation and ordered indices. No mutable caller array escapes. `scannedFiles` counts considered *selected* files, and `complete=false` warns that the hit budget stopped evaluation; do not infer anything about excluded or unscanned files.
- Cancellation throws and returns no partial result. Caller must retain the source snapshot's lifetime/immutability lease. Manifest-root recheck alone cannot detect hostile writes to unchanged-size backing files.
- Downstream use requires existing `PmemCatalogueWorkOrders` source/quality/category/recipe/reviewer and `PmemFileCampaign` durable one-model-call and compiler/JNI/test validation; never convert search hits into model or code-copy authority.

## Complexity and performance posture

Time O(k * patternLength + sum(eligible selected file lengths)) through the **incumbent per-file** KMP (k ≤20k); extra memory O(k + maxHits + patternLength), excluding underlying file-cache/system memory. A 1GiB mapped image is not proof of 1GiB RAM residency; no speedup claim without measured cold/warm and native/FILE benchmarks.

## Acceptance

- Executed locally with Java21 strict compiler, original legacy full-search differential 2,775 assertions and selected-search brute-force fixture 71 assertions on a 20,000-entry source corpus in JIT and `-Xint`; test-only API-compatible stubs, not native PMEMSDK.
- Authored actual Maven/JUnit FILE and optional POSIX/JNI comparison, 20,000 physical Java files, exact OpenRewrite Recipe.run first-pass/fixed-point/occupied path refusals and source/hash verification.
- **HOLD:** full Maven/OpenRewrite engine, original Java/JNI JMOD, native C17 parity, real Checkstyle quality reports, complete canonical provenance, true 1GiB residency, 20,000 live Chrome AI calls and result-only M3JDK21 receiver tests. An authored or queued CI step is not an execution receipt.
