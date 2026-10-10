# Char-array recipe superseded-owner reconciliation — 2026-10-10

Baseline: `e617717424bd0af23881fb383777ae14dc7c8bb4`
Branch: `aix/repair-char-array-superseded-owner-20261010`

## Failure and lineage

Foundation CI run 38041788650 and release run 38041788663 fail in
`m3/runtime-integration/char-array-copy/recipe/apply.py --check` with:

- Superseded path: `src/java.base/share/classes/java/lang/M3String.java`
- Manifest expected Git blob: `95dbd02121d2935d3727a20aa5e3e37f44acda3c`
- Actual Git blob computed by the checked-out recipe: `2e1239e157650dda20d4fd1d95e6fe35d1fe4c5a`

History shows the manifest owner pin was intentionally set on 2026-10-09 in
[e850ebd](https://github.com/hsoliwal/M3jdk21/commit/e850ebd027fb7f3b26bf81b6d2b0cb56176bc0ec)
and the superseded-owner rule was clarified in
[5233605](https://github.com/hsoliwal/M3jdk21/commit/5233605d66757e538968365a84b9891492c35202).
The same target path subsequently evolved through
[955ee00](https://github.com/hsoliwal/M3jdk21/commit/955ee0004a3d7740e513fc3ad64208d51f55c1fe),
[088c333](https://github.com/hsoliwal/M3jdk21/commit/088c333478f99f56b99360eb8367ea8032d6d553),
[c66aacb](https://github.com/hsoliwal/M3jdk21/commit/c66aacba5f21d5881a5b1a1162c22fb85713beeb),
[aec42c5](https://github.com/hsoliwal/M3jdk21/commit/aec42c5a02c331914f50a6ac2783890a259c3211),
and [3c3d666](https://github.com/hsoliwal/M3jdk21/commit/3c3d66694aaa1b345bbee1c3fe905f1a07ad3906).
These commits preserve the `M3String.java` successor path while extending its
implementation; the old `MIndexString.java` recipe target is no longer the
current owner.

## Repair rule

Update only `superseded_by.git_blob` for this exact successor path to the Git
blob computed from the reviewed baseline. Preserve the old recipe's before/after
hashes and the fail-closed comparison. Do not update the pin from a raw-content
digest or from an unreviewed worktree. The CI-computed actual Git blob above is
the baseline evidence for this leaf.

## Verification boundary

After the manifest update, rerun the recipe's `--check`, test, reverse/apply
fixed-point sequence, foundation workflow and release admission. A newly exposed
drift (including the superseded invariant test owner) must be handled as a new
leaf; do not broaden the manifest refresh. This repair does not claim the other
release-suite failures fixed.
