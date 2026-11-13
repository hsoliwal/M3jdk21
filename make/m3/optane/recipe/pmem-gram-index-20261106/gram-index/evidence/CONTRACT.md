<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 PMEM exact 2-gram candidate filter: frozen Java/JNI contract

## Existing owners (NO API/ABI change)

- PMEMSDK `PmemSourceImage` owns exact source bytes, immutable manifest root, sealed reads and original native POSIX/JNI or Java FILE search.
- `PmemCorpusSelectedSearch` owns validated sorted file selection and its exact JNI/FILE KMP dispatch and durable result identity. This original Java source is unchanged.
- `PmemCorpusSearch` is the original public whole-corpus API, byte-for-byte unchanged.
- `PmemCorpusGramIndex` is NEW, **package-private** and retains no third-party runtime dependency or JNI pointer.
- M3File/M3AST, QualityIndex, ProblemCatalogue, Maven/OpenRewrite and Chrome campaign retain their existing admission and authority. A gram search result is only a candidate signal, **never** proof of behavioral equivalence or licence permission.

## Algorithm and guarantees

- Build in two deterministic **serial** passes over `PmemSourceImage.readSealed(file)` so each original source is SHA-256 verified twice before the index becomes visible. Retain one flat sorted `char[]` 16-bit *unsigned byte-pair* lane and one primitive `int[]` per-file offset lane, plus `int[65536]` document-frequency counters. No Map.Entry or per-file bitmap objects.
- Preflight retained memory = `char.length * 2 + offsets.length * 4 + frequency.length * 4`, bounded by caller budget and hard 512 MiB ceiling. Temporary 2×`int[65536]` scratch, sort workspace, one owning byte buffer from incumbent `readSealed` and GC overhead are additional memory. Worst-case 20,000 sources with many distinct pairs may exceed budget: fail closed, do not truncate index or silently change the filter.
- Query preflights **every** sorted unique selected canonical index before callbacks or JNI. Clone source-selection and byte-pattern inputs. For pattern length 0 or 1, use existing selected-search unchanged. For pattern length >=2, pick the required byte pair with minimum document frequency, then binary-search only that pair in each source's sorted gram lane.
- Every exact full-pattern match contains every adjacent byte pair. Therefore a missing *required* pair establishes definite absence and cannot yield a false negative over the original immutable source image. Presence may be a false positive; all retained files still go through the unchanged exact JNI/FILE KMP search.
- Return original full-selection SHA-256, index SHA-256, source/pattern SHA-256, total selection, pruned count, JNI/FILE candidate count and scanned candidate count, plus original file indices and `long` offsets. `complete` means all possible candidates were checked; if hit budget stopped the underlying selected search early, `complete=false`. Eliminated files do not need JNI calls.
- Cancellation raises without returning partial results. Malformed/changed owner root, stale/closed image, out-of-range/duplicate selection, oversized pattern, index budget violation and failed sealed reads all refuse.
- **External source immutability is mandatory** throughout indexing and querying. An unchanged manifest root does not independently detect hostile writes to an underlying mmap. M3File owns the actual immutable snapshot lease; without that lease, absence claims are not admitted.
- Precompute complexity approximately `O(2 * corpusBytes + sum(uniquePairsPerFile * log(uniquePairsPerFile)))`. Query filter `O(selectedFiles * log(maxUniquePairsPerFile))`, followed by exact KMP on the remaining candidates. These are asymptotic bounds; claimed runtime speedup requires native/FILE cold/warm benchmarks.

## Required gates

1. Inventory/freeze original public Java and JNI symbols; record source SHA-256.
2. Reuse canonical `M3HashPinnedJavaSnapshotRecipe`, two `ABSENT` source/test templates, actual `Recipe.run` emission/fixed point/unreviewed-occupancy refusal. Improve the recipe before touching product sources.
3. Strict Java 21 compile, original first-hit and selected-search oracle, random/permutation and binary/UTF-8/empty cases, repeated-prefix adversaries.
4. Original PMEMSDK Maven/JUnit with `PmemBackend.FILE` and real `PmemBackend.POSIX`, C17 JNI checked and sanitizer, exact JMOD/jlink parity.
5. Qualify 20,000 actual physical source files, real 1 GB `source.bin` mapping with RSS/cold/warm measurements, model input context and complete source-bound quality reports.
6. Source-built receiving M3JDK21/other `hsoliwal/*` product targets only AFTER Synexia executed recipe and product-native acceptance receipts; products receive results, not Synexia-private recipe code.

Local independent Java21/C17 JNI oracle is NOT proof of production PMEMSDK JNI/JMOD; full Maven/OpenRewrite and genuine hardware remain HOLD until executed.

## Actual 1 GB mapped workload (additional independent fixture)

`verify-mmap-1gb.sh` uses Java NIO's *test-only* `MappedByteBuffer` image wrapper,
not the original production PMEMSDK JNI owner. It creates a real sparse
1,000,000,000-byte file with 20,000 logical 50,000-byte slices, injects a
known 6-byte signature in every eleventh file, and verifies the entire source
image twice through sealed reads. On a 128 MiB Java-heap cap, the expected
1,819 matches are found at their original byte offsets. Only surviving files
reach the exact first-hit delegate. The file is unlinked after verification.
Sparse zeros are intentionally not a realistic software code distribution;
RSS, page cache, allocation and elapsed time from this fixture must not be
reported as production 1 GB code-index performance or native/Optane evidence.
