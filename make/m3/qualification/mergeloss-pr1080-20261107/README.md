# Merge-loss restore: PR #1080 (prepared regex batch backend, rope affix effects)

PR #1080 "Refine M3 rope regex affixes and prepared Java/JNI composite matches" (head `6adadadf64ae`, base
`e8320c066974`) was merged by the 6-parent ancestry-only superset merge `0890555a47c2`, whose tree equals its
first parent. None of its 300 changed paths reached master. Fold-audit leaves L0006, L0007, L0052 and their
dependents (L0034, L0040, L0041, L0071, L0075, L0078, L0085, L0209) point here.

This commit restores it onto master `b51c5a94192f` (3-way: ours = master `aa2372843bff`, base = `e8320c066974`,
theirs = `6adadadf64ae`; none of the 44 paths master changed since `aa2372843bff` overlaps these 300).

## What is restored

| Kind | Paths | Method |
|------|-------|--------|
| Added by #1080, absent on master | 292 (278 under `make/m3/runtime-integration/regex-rope-affix-effects`, regex-plan-amalgamation 5, regex-mandatory-affixes 3, regex-required-ngrams 1, 2 docs, 2 workflows, `M3StringRegexPreparedBatchTest.java`) | historical blob ids reused byte-exact (`restore_retained4.py` BLOB rows) |
| Clean 3-way merges | `M3StringFacts.java`, `jdk/internal/mindex/M3RegexDpPlan.java`, regex-rolling-cursor `run_source_extracted_java21.py`, `verify_exact_source.py` | `git merge-file` |
| Resolved conflicts | `M3StringRegexDp.java`, `libjava/M3RegexDpPlan.c`, `M3StringRegexDpTest.java`, `doc/m3/name-mapping.json` | both sides kept, see below |

## Conflict resolutions

- `M3StringRegexDp.tryMatch`: master's ordered relation-algebra lane runs first, unchanged. #1080's warm
  whole-range effect lookup (`closure != null`) follows, then #1080's `rejectsByPrecompute` gate (NO_MATCH on a
  failed necessary condition: minimum length, fixed first/last unit, unit/bigram/trigram signals, literal affixes),
  then master's rolling and evaluate lanes with the `tryRepeatedUtf16` fallback. The gate sits after the relation
  lane because master's `M3StringHierarchicalRegexTest` route inventory expects the atoms-33/atoms-63 mismatch cases
  to reach the relation Access entry. #1080's 8 Plan facts and their retained-byte accounting are added.
- `M3RegexDpPlan.c`: `first_bit` and `fail()` move above the `M3_REGEX_CUDA_PROVIDER` guard, as #1080 needs them
  for the batch code. They keep master's bodies: the binary-search `first_bit` fallback and `fail()` through
  `m3_jni_throw_by_name` from `m3_jni_util.h`. #1080's batch backend is added. Master's forward declaration of
  `first_bit` is gone because the definition now comes before every use.
- `M3StringRegexDpTest.java`: union of both sides. Master's strict `decision()` guard ("expected canonical M3
  storage") stays; #1080's `canonicalOwner()` admission serves only its random amalgamation loop.
- `name-mapping.json`: #1080's three mappings are appended (73 -> 76). The prepared-batch entry gains a `target`
  key because master's `test_m3_string_concat_dag_successor.py` indexes every mapping by `target`.

Line accounting against master: 0 lines removed in five of the files. In `M3RegexDpPlan.c`, 19 lines moved and
1 forward declaration was dropped.

## Behaviour change (intended by #1080)

For dot/star plans, a failed mandatory literal, affix or length condition now returns NO_MATCH before the rolling,
evaluate and `tryRepeatedUtf16` lanes; previously those lanes decided or returned UNKNOWN. The gate checks only
necessary conditions, so a definite answer cannot change from match to non-match.

## Gates

See `GATES.tsv`. Scripts: `gate-java.sh` (patch-module javac), `qualify-merged.sh` (wrapper over #1080's unchanged
`qualify.sh`), `gate-exports.sh` (JNI export superset), `gate-atoms.sh` (master atom tests). The JNI export check,
#1080's own qualification (Java fallback, JNI, portable `-Xint`, UBSan, allocation failure injection) and master's
37 atom tests pass on the merged owners.

Not run: jtreg on an M3JDK image (java.lang owners cannot be patched into a stock VM), and #1080's source-extracted
A/B fixtures. Their fixture anchor `M3String.CodeUnitCursor` no longer exists in master's M3String. The restored
receipts (`verify_receiver.py`, `verify.py`, the extracted-source scripts and their two workflows) are byte-exact
history. They pin #1080's product hashes and do not match the merged owners, just as master's rolling-cursor
receipt already pinned a superseded hash.

Known on master, not caused here: `M3CommonRegexFactsHostTest` reflects the field `tables`, which master renamed to
the SoftReference `caches`.
