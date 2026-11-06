<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3/search: M3 search kernels

Received from com.synexia `m3-fast-search` (develop 2026-11-06), absorbed under M3 names by recipe:

| M3 owner | Donor | Shape |
|---|---|---|
| `M3SearchEngine`, `M3JavaAdaptiveSearchEngine`, `M3CompiledSearch` | SearchEngine, JavaAdaptiveSearchEngine, CompiledSearch | precompiled exact byte search, engine selection |
| `M3KmpSearch` | KmpSearch | Knuth-Morris-Pratt prefix table |
| `M3BoyerMooreHorspoolSearch` | BoyerMooreHorspoolSearch | BMH skip table, overlap fallback |
| `M3BitapExactSearch` | BitapExactSearch | shift-and bit-parallel exact search (Java path; native entry points declared) |
| `M3MinimumCoverWindow` | MinimumCoverWindow | minimum window substring (LC 76) |
| `M3MinimumSubsequenceWindow` | MinimumSubsequenceWindow | minimum window subsequence (LC 727) |
| `M3FixedWordConcatenation` | FixedWordConcatenation | substring with concatenation of all words (LC 30) |
| `M3BatchSubsequenceMatch` | BatchSubsequenceMatch | number of matching subsequences (LC 792) |

Progress and cancellation use `jdk.m3.collections.M3ProgressMonitor` (the donor's `com.synexia.job.IProgressMonitor`).

## How it was received

`qualification/relocation-20261106/`:
- `closure.py`: LEAF_CLOSURE. Only donor classes whose non-JDK dependencies are the progress monitor joined. ByteOccurrences, M3BatchOffsetSearch, M3BatchReverseSearch, SearchEngines and JniSearchEngine reach `JniSearchEngine` / `synexia-native-interop` and form the native leaf (leaf 2, with the C kernels).
- `relocate_search.py` + `rules.tsv`: M3_SEARCH_RELOCATE v1, the text-lane relocation recipe. It sets the package `jdk.m3.search`, maps the progress monitor, prefixes the M3 type names, and adds a provenance line with the donor blob.
- `LEDGER.tsv`: one row per file, with the donor path, donor git blob, output path and output sha256.
- `diff/SearchDiffDigest.java`: M3_SEARCH_DIFF v1.

## Gates (2026-11-06, JDK 21.0.9, Windows)

- javac `--release 21 -Xlint:all,-module -Werror`: main 0 diagnostics, tests 0 diagnostics.
- JUnit 5 (27 relocated donor test classes): 59 tests found, 45 successful, 0 failed. 14 are skipped: the native minimum-cover tests, which need `-Dm3.minimum.cover.native.junit=true` and the leaf-2 library.
- Differential: the same seeded stream (200 seeds × 500 cases: indexOf/lastIndexOf over 6 offsets plus the four window kernels) ran on the com.synexia originals and on `jdk.m3.search`, giving 1,600,000 rows each. The digests are identical: `4bcb44bd72588a4a8dc5f5d9adf6f795906ce3ae5c1d40423ddf671e31db2981`.

Not run: the Maven reactor build on CI, and the native kernels (leaf 2).

## Leaf 2: the native engine (2026-11-06)

Received by M3_SEARCH_RELOCATE v2 (`qualification/relocation-20261106-native/`). v2 adds JNI symbol and class-name
rewriting for C, plus the native-loader mapping onto `jdk.m3.file.M3JniLibraryLoader` / `M3NativeLibrarySpec`
(m3/ports/file, #2492). The library's logical name `synexia_m3` becomes `m3search`.

| M3 owner | Donor |
|---|---|
| `M3JniSearchEngine` | JniSearchEngine (pattern handles, bulk count/positions, monitored bulk ABI) |
| `M3SearchEngines` | SearchEngines (JNI first, Java adaptive fallback) |
| `M3ByteOccurrences` | ByteOccurrences (single-byte memchr/count/reverse lanes) |
| `M3BatchOffsetSearch`, `M3BatchReverseSearch` | same names (batch first/last occurrence, byte[] and direct ByteBuffer) |
| `src/main/native/*.c` | jni_search.c (the m3search library) and the sidecars for minimum cover window, minimum subsequence window, fixed-word concatenation, batch subsequence and bitap |

Donor corrections made at the source before receiving:
- com.synexia #16742 removed a byte-identical duplicate of the batch JNI block from `jni_search.c`. Superset merge 73095e9b890d had introduced it, and with it the file did not compile.
- One test literal was corrected by recipe rule: `indexOfMany((byte[]) null, …)`, because the bare `null` was ambiguous between overloads.

Not received:
- `jni_z_similarity.c` and the Java `M3ZSimilarity` (the Z similarity shape, LC 2223, is owned by `jdk.m3.algorithm.M3PrefixZ`).
- `JniSearchEngineNativeTest` and the 11 donor tests that import `com.synexia.job.ProgressSnapshot` (no M3 equivalent API).
- `BitapChunkedRegionProof`, which calls methods its donor class does not have.

Gates (Windows, JDK 21.0.9, MinGW-w64 gcc):
- javac `-Xlint:all,-module -Werror`: main and tests 0 diagnostics.
- gcc `-shared -O3 -std=c11 -Wall -Wextra -Werror`: 6 libraries, 0 diagnostics.
- JUnit with every native library loaded: 97 found, 97 passed, 0 skipped.
- JUnit Java-only (the default build, which excludes `*Native*Test`): 55 found, 55 passed.

Run the native tests: build `src/main/native` with CMake, then
`mvn -f m3/search/pom.xml -Pm3-search-native -Dm3.search.native.dir=<build dir> test` (on Windows also
`-Dm3.search.native.prefix= -Dm3.search.native.suffix=.dll`).

## Leaf 4: top-K selection, rank fusion and pair-sum search (2026-11-06)

M3_SEARCH_RELOCATE v3 (`qualification/relocation-20261106-fastsearch/`) receives the pure-JDK search kernels of com.synexia
`synexia-fastsearch`. The other classes of that module are an application layer (Lucene, Jackson, HTTP, WARC) and stay
in com.synexia. `closure_ffs.py` and `seed_closure.py` record the selection.

| M3 owner | Donor | Shape |
|---|---|---|
| `M3TopKSelector`, `M3JavaTopKSelector`, `M3TopKInputs` | knowledge.index TopKSelector, JavaTopKSelector, TopKInputs | bounded top-K selection with progress and cancellation |
| `M3ReciprocalRankFusion`, `M3HybridSearchOptions` | ReciprocalRankFusion, HybridSearchOptions | weighted reciprocal rank fusion of ranked lists |
| `M3PreparationCache`, `M3QueryPreparationStats` | PreparationCache, QueryPreparationStats | bounded query-preparation cache |
| `M3PairSumSearch`, `M3PairSumJni` + `pair_sum_search.c` (m3pairsum) | problem PairSumSearch, PairSumJni | two-sum over sorted and unsorted lanes (LC 1 and LC 167), Java and C17 JNI |

The donor monitor `com.synexia.common.progress.ProgressMonitor` maps to `jdk.m3.collections.M3ProgressMonitor`. The kernels call beginTask, subTask, worked, done and checkCanceled, which have the same meaning on both.

Gates (Windows, JDK 21.0.9, MinGW-w64):
- javac `-Xlint:all,-module -Werror`: the whole m3/search module, main and tests, 0 diagnostics.
- gcc `-Werror` on `pair_sum_search.c`: 0 diagnostics.
- JUnit default build: 69 found, 63 passed, 0 failed, 6 skipped. The skipped ones are pair-sum JNI tests that need the library.
- JUnit leaf-4 classes with `-Dm3.pair.jni.library`: 14/14 passed.
- `HybridSearchKernelChecks`: 25,000 randomized top-K cases, 10,000 budget comparisons and 3,000 weighted-fusion oracles, PASS.
- `PairSumParityDriver`: Java/C17 parity over 15,000 sorted singles, 24,576 batch queries and 8,000 unsorted singles, PASS.
- Differential: the com.synexia originals (the stub monitor is in `diff/stub`) print byte-identical output for both checks, with sha256 `2e771122b347…` (hybrid) and `85c6bd1dd98a…` (parity).

## Bitap update (2026-11-06)

Landing #16732 had regressed com.synexia develop's `BitapExactSearch` and its JNI kernel to an older copy. The newer copy survived only as a retained variant, and com.synexia #16744 and #16745 restored it. Leaf 1 received the older copy, so this update re-applies M3_SEARCH_RELOCATE v2 to the restored donor (`LEDGER_v2b_bitap.tsv`). The M3 side had no edits beyond relocation, so the new relocation is the merged result.
- `M3BitapExactSearch` gains the chunked-region and overlap-count API: `countOverlapping` and `*Native` region entries. `jni_bitap_exact.c` follows it.
- `BitapChunkedRegionProof` was held out of leaf 1 because it called methods the old donor lacked. It is now received.

Gates (Windows, JDK 21.0.9, MinGW-w64):
- javac `-Werror`, module main and tests: 0 diagnostics.
- gcc `-Werror`: 0 diagnostics.
- JUnit default build: 70 found, 64 passed, 0 failed.
- `BitapChunkedRegionProof` against the m3bitap library: `M3_BITAP_CHUNK_REGION_PASS checks=8118`.
- `BitapExactNativeParityMain`: `BITAP_JAVA_JNI_PARITY PASS assertions=910008`.
- Both proofs give output byte-identical to the com.synexia originals.
