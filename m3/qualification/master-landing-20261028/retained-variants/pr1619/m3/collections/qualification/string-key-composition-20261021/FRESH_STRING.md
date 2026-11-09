# Fresh source-built String qualification

The requested String comparison and string-key collection tests pass on the newly
source-built installed image at
`m3jdk21/build/m3-string-keys-release/images/jdk`. Tests were compiled from the
recipe-emitted receiving source by that image's own javac and executed through
jtreg 7.3.1, with a stock JDK21 used only for the harness. Every test action has
`-Xcheck:jni`. No production patch-module or prior-image test classes entered
receiving acceptance. Exact image hashes, source hashes, jtreg declarations,
commands, outputs and extracted per-action results are retained.

## Runtime results

The finite expanded selection produced **14 passing and one failing jtreg manifest
record**, across 11 unique test sources. Its 27 main/TestNG actions contain 26 passes
and one failure. The overall runner exits 1, and a wholly passing selection is not
claimed. Repeated manifest records cover additional VM configurations rather than
additional unique tests.

| Selected fixture | Fresh installed-image result |
| --- | --- |
| M3StringKeyedCollectionsTest | PASS, 6,987 checks per run, CompactStrings on/off |
| M3StringCompositeComparisonTest | PASS, 120,923 checks per run, all three declared variants |
| M3StringCompositePrefixCursorTest | PASS, 3,747 checks per run, CompactStrings on/off |
| M3StringMixedEqualityGatesTest | PASS, 46,946 checks per run, both declared M3 modes and compact-off supplement |
| M3StringLeafCursorCanonicalTest | PASS, 29,771 checks per run, compact on/off |
| M3StringUtf16DifferentialTest | PASS, 524 values / 436,112 checks per run, both declared M3 modes and compact-off supplement |
| Stock HashCode, CompactString Equals, CompareTo, StartsWith | All PASS with M3 explicitly disabled; both declared CompactStrings variants |
| Additional M3StringFactsCompositionTest | Compact-on FAIL: historical flat-oracle assertion expects M3StringAtom but receives M3StringImmediate; compact-off PASS, 196,246 checks |

The additional facts fixture's exact on/off split is independently reproduced using
its unchanged source and all 18 String owners pinned to receiving preimage
`18213fe70c66567fdfc6ba6f669909e7e9251663`, on the prior built VM. That patch run is
baseline diagnosis only; it is not receiving-image acceptance. The fixture's scalar
atom assumption is the first failing assertion, and the candidate does not change
scalar admission. No test or production source was edited to turn this failure into
a pass. The initial diagnostic driver mistakenly expected failure in both compact
modes; its expectation error and raw outputs remain preserved, and the corrected
named driver records the actual matrix.

## Source-policy results and exact baseline witnesses

Full-borrow receiver validation passes. The closure checker reports all 12 executable
gates OPEN and promotion BLOCKED; its zero exit status validates that honest open
state, not closure.

Three source checks fail: the structural invariant check requires the historical
pool marker `candidate.atom() != requested.atom()`; the precompute-bridge receiver
receipt has hash drift; and the STRING successor chain has length drift. Each
unchanged checker was rerun against its genuine Git-object inputs materialized from
exact preimage `18213fe`, and each produced the identical failure. Every pinned input
is sealed in `source-baseline-witness/pinned-inputs.json`. These are pre-existing
source/receipt failures retained as limitations. No policy, receipt, historical chain
or promotion state was changed.

## Evidence and scope

`FRESH_IMAGE_RESULTS.json` contains the exact per-action status/markers and the
classification above. `fresh-image-20261021T043259Z` holds complete local jtreg work
and reports; its `image.json`, `sources.json`, `actions.json` and `test-results.json`
are the compact source/build seals and result index. `fresh_image.py` is the runner.
It executes source checks independently and only launches the fresh image after an
explicit root build-complete handoff.

`fresh-image-essential-files.json` lists bounded text evidence for canonical retention.
Full jtreg scratch classes, binaries and XML/system-property dumps are excluded from
that list. The complete raw evidence remains locally available. No whole-jtreg/JCK,
compiled-M3, GC/CDS/JFR/JVMCI, all-family completion or phase promotion is claimed.
