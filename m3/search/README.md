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
