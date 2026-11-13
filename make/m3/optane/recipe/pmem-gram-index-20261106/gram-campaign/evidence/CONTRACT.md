<!-- SPDX-License-Identifier: Apache-2.0 -->
# Contract: reviewed source gram search before durable Chrome work

This is one new private Java21 FILE-local composition, not an additional native
search, parser, source store, JNI interface, donor catalogue or source writer.
The following incumbent owners stay byte-identical:

- `PmemCorpusGramIndex` from Synexia PR #16531: source SHA-256
  `85d83f286e49e69e3ada9e8f4e0f772e66f214964c7d332ef2107e20dcfbd5fd`.
- `PmemCorpusSelectedSearch`: `a88eb92d8cf00411171d20648f90ce51c7b472f6604d29073bcd82d976f9628f`.
- `PmemCatalogueWorkOrders` (PR #15318), `QualityIndex`, `PmemFileCampaign`,
  original JNI/POSIX/C17 KMP and Maven/OpenRewrite engines.

## Atomic pre-dispatch algorithm

1. Freeze and copy the selected primitive file indices and binary search pattern.
   Validate the selection is sorted, unique and complete against exactly one
   externally reviewed `PmemCatalogueWorkOrders.Review` per selected source path.
2. `PmemCatalogueWorkOrders.prepare` checks **every** selected original file's
   source SHA, complete quality reports, contract/atom/recipe, donor category,
   non-deferred decision and independently trusted review receipt. A missing
   review refuses the entire work order; a gram's absence never implies clean
   source or skips mandatory prior review.
3. `PmemCorpusGramIndex.firstHits` uses the original 2-distinct-gram filter and
   its existing Java FILE or POSIX/C17 JNI KMP to verify exact first byte offsets.
   Incomplete hit-budget results, invalid bounds, cancellation, source generation
   drift, and index/selection mismatch refuse before any tasks can be dispatched.
4. For every surviving exact matching file, preserve the already authenticated
   original file task and append source-index, pattern, selection and result-offset
   receipts to the existing context. The incumbent durable `PmemFileCampaign`
   hashes that context into its packet. New context/recipe/source identity does
   not mint a second dispatch permit for an existing task ID.
5. Return an immutable list of exactly matching tasks, in original source order.
   Empty complete searches produce **zero** tasks. No file mutation/model call
   occurs in this helper. Candidate source application remains with canonical
   Maven/OpenRewrite recipes, compiler/API/behavior and native/JNI test gates.

## Safety/resource boundaries

- The entire original source snapshot and its path/atom/quality identities are
  owned by M3File/PMEMSDK. An immutable mapping lease must span index build and
  search; manifest root equality alone cannot detect external malicious writes.
- `MAX_SELECTED_FILES=20_000`, positive pattern/result budgets from original
  owners, exact UTF-8 context byte checks and a 16KiB maximum review context.
- The hash-bearing context includes all four source/search identities plus the
  file-relative byte offset. This may cause an otherwise admissible LLD to
  exceed its budget, which must refuse rather than truncate context.
- Quality/completeness and donor review are mandatory even for files later
  pruned by the gram signal. This intentionally separates one-time per-file
  review from potentially cheaper subsequent searches/Chrome dispatch.
- `complete=true` means **every potentially matching file in the selected set**
  was searched; it does not assert whole-repository absence if the selection was
  a subset. No hash or LLM output establishes semantic equivalence.

## Local verification and release HOLD

- Compiled exact Java21 new source with strict `-Xlint:all -Werror` against
  original GramIndex and SelectedSearch source plus test-only SDK API stand-ins.
- Independent 20,000-entry deterministic campaign: 846 assertions PASS under
  `-Xmx128m`, JIT and `-Xint` identical; 274 exact work orders.
- Four authored Jupiter-shaped test methods compiled and executed with an
  API-compatible fake FILE/image/ledger, including 20,000 physical tiny files;
  JNI-required method returns early without the native opt-in. **Not actual
  JUnit/JNI execution.**
- Actual OpenRewrite SDK, Maven/JUnit engine, full original JNI/JMOD/JLink/C17
  parity/sanitizers, 1GiB resident RSS, live Chrome 20k calls, provenance and
  M3JDK21 receiving product gates are NOT_RUN / HOLD.
