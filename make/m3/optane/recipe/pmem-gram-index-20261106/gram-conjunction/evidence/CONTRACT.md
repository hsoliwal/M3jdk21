<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 invariant — two necessary byte-pair signals, one original exact search owner

Parent branch PR #15431 `m3/pmem-adaptive-2gram-recipefirst-20261102` at SHA
`87acd7a0fb85fe47ad082de97270747b52603024`.

**Contract frozen:** existing `PmemCorpusSearch`, `PmemCorpusSelectedSearch`, `PmemSourceImage`, original `PmemSearch` and native C/JNI KMP owners remain byte-for-byte unchanged. Only the private `PmemCorpusGramIndex.firstHits` search atom and its JUnit regression change; public/protected Java signatures and native symbols are untouched.

The original search selected the globally rarest two-byte gram required by a query as a necessary membership filter, then invoked exact KMP for surviving files. This refinement chooses the two globally rarest **distinct** query grams (stable ties by first occurrence). A whole-pattern match necessarily contains *both*, so their intersection cannot eliminate a true match while the original immutable source snapshot lease remains valid. Every surviving file still receives the original exact Java/FILE or POSIX/JNI KMP first-hit check. No independent parser, engine, JNI export or persisted index is added.

Empty, one-byte or all-repeated-pair patterns retain original semantics. Pair hits need not be adjacent, so false positives remain possible; matching positions always come from original KMP. Candidate counts may shrink and `Result.complete` may become soundly true earlier when previously unknown files are proved absent. Sorted file identity, file-relative byte offsets, cancellations, budget bounds, source seals, immutable byte arrays and source-file boundaries remain intact. Worst-case candidate filtering adds at most one per-file exact gram probe; no throughput speedup claim is made.

**Frozen postimages:** Java SHA-256 `85d83f286e49e69e3ada9e8f4e0f772e66f214964c7d332ef2107e20dcfbd5fd`; JUnit SHA-256 `ac7afd7cad94cc2c7c7a3fb63ac67216a879705518e370707ac7ff2a995adac9`. Original hashes and complete preimages are retained in `manifest.tsv` and `pre/` assets. The existing `M3HashPinnedJavaSnapshotRecipe` owns the only source mutation, with exact input/output checks, actual SDK `Recipe.run` first pass, fixed point, stale/missing refusal. The result-only public product receiver must run its own JNI, compiler, jtreg and legal gates; no private recipe copies go to M3JDK21.

**Executed locally:** OpenJDK21 strict javac, original 3,887-assertion regression, 20,000-entry scalar oracle 85,005 assertions before and after, identical exact-hit SHA-256 `d81690f7723fa478d04cca40d51ea20c6023a9531c43ed1ab7b0463ff8ccff18`, unchanged `javap -p` signatures and six restricted JUnit-shaped tests. On a synthetic 20k corpus, 10,103 candidates became 207 with the same 207 exact hits. **Not run:** real Maven/OpenRewrite, real JUnit, original native POSIX JNI/JMOD, 1GB RSS/performance, Chrome 20k inference, full provenance or any public-fork receiving acceptance. Keep DRAFT/HOLD.
