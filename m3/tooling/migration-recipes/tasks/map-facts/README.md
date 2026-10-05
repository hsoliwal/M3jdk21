# MapFacts: mapped String fact validation

Continuation of M3JDK21 PR99 at 3a433416ae7981301844682896e5aefc6f0f8901.
The selected source is the existing layout-independent java.base mapped-backing
kernel, not String.value or HotSpot. No new public runtime owner is introduced.

## Inventory and defect hypothesis

A text scalar currently checks its UTF16 payload CRC but publishes six header facts
without recomputation: code-point count, unpaired-surrogate count, String hash,
power of 31, first UTF16 unit and last UTF16 unit. Aliases check only some facts.
Header tampering need not change payload CRC. The first test pass must reproduce
acceptance of inconsistent metadata before claiming this is an implemented repair.

## Allowed correction

Reuse the existing scalar CRC traversal to calculate and validate all six facts.
Use ordinary Java int wraparound for String hash and powers. Empty text has hash 0,
power 1, zero counts and first/last -1. Count supplementary pairs and unpaired
surrogates with UTF16 code-unit semantics. No flattened payload, retained per-row
fact wrapper or second scalar payload traversal is allowed. Aliases reuse earlier
validated facts without rescanning payload; backwards alias chains remain valid.

Valid successful format and public/protected signatures stay unchanged. Invalid
fact headers gain explicit fail-closed rejection before publication. This is a
parser-safety strengthening, not an assertion that previously accepted corrupt
metadata was behaviorally equivalent. Retain PR99 count and failure-cleanup gates.

## Recipe ownership and serial history

Reuse M3Jdk21HashPinnedSnapshotRecipe with the compact alias MapFacts and legacy
crate directory jdk22-map-facts (name-fence compatibility, not a JDK22 donor claim).
Bind PR99 exact preimage and absent-before new test to final source images. Reconcile
MapGuard's final image with the same owner so the previous gate stays executable;
retain its original preimage, old tests and historical PR99 blob. Reconcile the
existing workflow image; add the new test without changing permissions or gates.
Documentation -> recipe/images/tests -> product/test/workflow materialization.
No master/develop write, squash, rebase, or history rewrite.

## Proof envelope

Use the existing original fixture writer, real backing class and Java21 String /
Character operations as oracles. Exercise all six scalar/alias header corruptions,
CRC failure, empty data, surrogate seams, deterministic Unicode data, alias chains,
full/sliced views, immutable input files and resource cleanup. Rerun the original
and MapGuard tests. Compare public/protected javap surfaces before/after. Compile
with -Xlint:all -Werror, then run isolated layout-independent package patches in
normal and interpreter modes; do not patch String.class or change installed JDKs.

Actual Maven/OpenRewrite/JUnit, measured 99% coverage, full configure/make/jtreg,
matched String/HotSpot/JNI/GC/CDS/JIT, platforms and programme-wide admission remain
separate required gates. A local subset run never supplies those results.

## Not claimed

This does not authenticate mutable mapped files, prevent external rewrite/truncate,
validate every header field or implement UTF8-to-UTF16 semantic parity. Trust and
immutability of committed files remain prerequisites. No measured speedup claim:
only one-pass work structure and retained-storage shape are reviewable here.
