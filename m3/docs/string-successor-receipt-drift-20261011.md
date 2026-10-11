# STRING successor receipt drift — 2026-10-11

## Evidence

The `M3JDK String owner-coordinate runtime` CI job failed in
`test_actual_owner_sources_match_exact_receipts`. The failure is exact and
reproducible from Git object identities:

| Target | Recorded successor Git blob | Current master Git blob | Result |
|---|---|---|---|
| `src/java.base/share/classes/java/lang/M3String.java` | `d99a5080f153c24d3efaf37c30c26f9528563a38` | `70d1d03c109126fdda36c54da21b484bbe368fd9` | drift |
| `src/java.base/share/classes/java/lang/M3StringFacts.java` | `763a76fb55a52576ca1a3edc48fcc214663f799e` | `fcd108ab6181fec11d16eda811b29d18298f3fb1` | drift |
| `src/java.base/share/classes/java/lang/M3StringPositionPrecompute.java` | `c0074f4b144da23040e63e0ba8866d56d0d304ac` | `c0074f4b144da23040e63e0ba8866d56d0d304ac` | exact |

The two changed paths still contain the existing source-contract markers for
flat-needle facts and the M3 string owner. That is not enough to assert byte
identity or semantic equivalence. In particular, the current `M3String.join(String,
String)` has explicit null-return/back-pressure handling before concatenation;
the recorded successor expects the older direct
`M3StringPool.concat(canonicalize(first), canonicalize(second))` expression.
That change must be reviewed as a separate later state, not erased by changing a
hash in place.

## Historical chain

The recorded M3String postimage `d99a5080...` is the exact blob at PR #464's
merge commit `eaa59a6d2ea75d0eae2c9c28c7dfb01f127242fb`. PR #452 is the prior
designated `String.join` receiver; PR #441 is the split-literal receiver.
Later history includes commits `aec42c5a02c331914f50a6ac2783890a259c3211`
and `3c3d66694aaa1b345bbee1c3fe905f1a07ad3906`. Their current source blobs are
not the recorded PR #464 postimage. The source history therefore proves both
the original accepted implementation and a later evolution; it does not justify
restoring the older file wholesale.

## Required repair

- Preserve the original PR #441/#452/#464 postimage receipts.
- Add explicit, ordered later-state receipts for the current M3String and
  M3StringFacts Git blobs, each tied to the actual commit and reviewed
  semantic obligations.
- Validate that each successor state is unique, ordered, and distinct from its
  predecessor; retain exact live-tree byte checks.
- Keep the M3String position-precompute receipt unchanged because its live blob
  still matches exactly.
- Update source markers only after review of the new state; keep negative tests
  for responsibility loss, duplicate receipts, unqualified states, and stale
  live blobs.
- Rerun the entire String owner-coordinate runtime job and the foundation and
  release gates. No runtime or release pass is inferred from this inventory.

## Scope boundary

This is separate from the runtime integration recipe PR #693. It is a distinct
successor-lineage defect in the Phase-1 String donor/receiver catalogue.
