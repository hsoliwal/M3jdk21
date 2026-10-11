# Restore exact STRING successor receipt gate — 2026-10-11

## Finding

The live master tree at `50a78c3e604981e8178580ad12c5d6d4ab7c8d77` has reverted a previously verified source-receipt correction. The exact repair already exists in history at commit `30661cba808b8fb927953a5815369ff811147489` (subject: `fix: repin current String successor receipts`), whose parent is `b41c68cc4c11f02181d91514a3d7a4b5ddd4f2ba`.

At that historical commit:
- `M3String.java` receipt pins the exact live blob `70d1d03c109126fdda36c54da21b484bbe368fd9`.
- `M3StringFacts.java` receipt pins the exact live blob `fcd108ab6181fec11d16eda811b29d18298f3fb1`.
- The checker explicitly requires both canonicalization results to be tested for pool refusal before concatenation.
- `M3StringPositionPrecompute.java` remains pinned to its unchanged exact blob `c0074f4b144da23040e63e0ba8866d56d0d304ac`.

The live master currently has the same two source blobs as that historical repair, but the successor table points at older blobs `d99a5080f153c24d3efaf37c30c26f9528563a38` and `763a76fb55a52576ca1a3edc48fcc214663f799e`, while the checker expects the old binary-concat expression. CI therefore fails on an already-reconciled source state.

## Minimal repair

Restore only the two exact known-good receipt/checker postimages from commit `30661cba808b8fb927953a5815369ff811147489`. Do not edit `M3String.java`, `M3StringFacts.java`, or `M3StringPositionPrecompute.java`. Do not rewrite or delete any history. The original historical donor pins remain unchanged; only the successor pins and the semantic checker are restored to the previously reviewed state.

## Verification required

Run the String successor test suite, exact current-source receipt checks, the String owner-coordinate runtime job, foundation contracts, retained-capability audit, and release admission. A CI pass is required before treating the receipt repair as complete.
