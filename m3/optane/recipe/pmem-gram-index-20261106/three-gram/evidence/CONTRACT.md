<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 private GramIndex: three distinct necessary byte-pairs

This is a one-file private Java21 refactor under original `PmemCorpusGramIndex`.
The 65,536-symbol primitive byte-pair index, sparse sorted postings, dense
8KiB bitmaps, sourceRoot/index/selection/pattern SHA-256, FILE and POSIX C17/JNI
KMP, parent leases, cancellation, output result identities, exceptions, and
all existing public/protected signatures remain unchanged.

For a pattern with at least three **distinct** adjacent byte-pairs, select
the three least frequent pairs by exact immutable indexed document-frequency,
breaking ties in original pattern order. A complete pattern requires all
three distinct pairs in the same file. Check third only if first and second
are present, then invoke existing exact KMP for remaining candidates.
For short/repeated patterns, require only as many distinct pairs as exist.
No false negatives under the original immutable source-generation lease.

The filtered search still supplies only candidate evidence. Trusted M3File,
quality-plugin completeness, M3AST atom/interface/behavior contracts,
donor-category review, Maven/OpenRewrite recipe and actual JUnit/native
acceptance control source changes. No LLM/cache/donor may promote matches.

## Local evidence (independent scalar reference, Java21)
- Original and candidate strict `javac --release 21 -Xlint:all -Werror`: PASS.
- 20,000 synthetic source files: **11,000** original KMP candidate files and
  **2,000** candidate files with the third required gram; **2,000** exact
  original/candidate matches. 166,887 scalar checks per version (both JIT
  and -Xint); exact output SHA-256 equal
  `d404938cc6c0e8c860b468276fe8dba4b84d8fbd75008e887da583b18152fce7`.
- The published independent proof contains an additional deterministic
  20k reference workload with 115,937 scalar checks and equal original/
  candidate receipt `e0fd91745ffe6c4aae09374070c9ebcba3e0478b5d0ffb41714f18496e5a8d7f`.
- `javap -p -s` descriptor parity: PASS.
- Actual Maven/OpenRewrite `Recipe.run`, JUnit engine, JNI/JMOD C17,
  source residency and full provenance: **NOT_RUN / HOLD**, not inferred from
  test stubs or queued GitHub Actions.

## Exact source seals
PRE Java `85d83f286e49e69e3ada9e8f4e0f772e66f214964c7d332ef2107e20dcfbd5fd`
POST Java `86c79682e7c373034a5f086c7241602b2203459ca188b32a02f60e42321780b5`
PRE JUnit `ac7afd7cad94cc2c7c7a3fb63ac67216a879705518e370707ac7ff2a995adac9`
POST JUnit `6ef88217cba20945cdab10fac70d60e55abc398587b786c1af2b288a24e3cb62`
