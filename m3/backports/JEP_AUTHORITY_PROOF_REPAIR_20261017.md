# Released-JEP source recipe and scope proof repair, 2026-10-17

State: **target metadata/tests repaired; product code untouched; CI pending**.

## Exact preceding failure

Hosted action `39007794302`, job `117323709151`:
pinned source-built Maven bootstrap and 14 canonical Synexia JUnit tests
passed. The actual target `M3ReleaseJepAuthorityRepairRecipeTest`
then failed because its eleven-row manifest was **not sorted according
to Java String.compareTo**, as strictly required by the existing
`M3Jdk21HashPinnedTextSnapshotRecipe.targets()` validator.

The same job's target scope tests had out-of-date expectations:
- target-only overlay has **38**, not **19**, entries after the prior
  19 approved bulk/parallel/Tornado receiver registrations;
- canonical Synexia `M3MIndexJoinedCharsViewRecipe` has **MODULE**
  minimum scope, not `FILE`. The target adapter correctly reported MODULE
  and rejected file-local promotion.

These are concrete proof-tool metadata regressions, **not permission to
silently expand any source-changing recipe scope**.

## Source-bound remedy

- Reorder **existing** eleven manifest lines with Java UTF-16 code-unit
  lexicographic ordering. Preserve each four-column path, preimage SHA-256,
  after SHA-256 and resource filename unchanged; no template/source
  bytes rewritten, no input/test removed.
- Add an independent JUnit guard checking all 11 exact names are strictly
  ordered and each referenced after-resource matches its declared SHA-256.
- Match the 38 exact current target registrations in the scope count,
  assert all 19 later names remain at least MODULE and behavior preserving,
  and explicitly assert canonical JoinedChars MODULE minimum/file-local
  prohibition. **No scope owner, inference implementation, policy, or
  admission code is changed.**

This PR stacks on the separately reviewable pinned-dependency bootstrap
PR #1011. The pre-existing OpenRewrite replay, fixed-point test and JEP
authority controls are unchanged and must actually pass CI.

## Boundary / admission

- No OpenJDK `src/**`, JNI/HotSpot, JEP catalogue content, runtime-image
  source, language/classfile contract, licence or build gate is changed.
- Not a full-JEP promotion or a duplicate generic recipe engine.
- A new actual full target build/test and exact source replay are required
  before merging or declaring `ADMITTED`.
