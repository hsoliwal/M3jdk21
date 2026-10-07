# JCC history recovery and retention, 2026-10-06

This task restores 443 previously published M3 recipe, test and evidence files from immutable
receiving commit `0483f79ae51dd578c2e4fec1b8431bd51b59b469`. The six-parent merge
`f5a28ac841958313d0c287f87c263a84b7ef6c39` preserved that commit as history while dropping its
JCC additions. The targets are also absent at the reviewed current input
`87590cb96fb0e2dc0f88fae8e01957f7179cd255`, root
`6672d8a05a2324f29e38e0ab5ed242039cdf6d86`.

The existing `M3Jdk21HashPinnedTextSnapshotRecipe` owns all three replay crates. Two crates stay
within its unchanged 256-target budget (256 plus 187 historical additions). A third crate changes
six exact current files: the existing retention owner, tests, Maven entry, workflow, README and
retention catalogue. The existing sealed `m3/migration/recipe.py` owns installation and rollback.
No new transformation engine, retention engine, runtime owner or Maven dependency version is added.

## Historical custody is explicit

The two new `jcc-merge-recovery-20261006-history-*` catalogue rows retain historical recipe/test/
evidence custody. They do not declare the JCC capabilities promoted or accepted. Both existing
JEP 458 product rows remain byte-for-byte at the start of the catalogue. The product-promotion
rule in `m3/history/README.md` remains applicable to new product capability acceptance; these
two historical custody rows preserve already published artifacts and their unresolved results.

Historical source 0b/d1/0c53 results, donor 4990 evidence, failed checks and raw-byte readback limits
keep their original epochs. Restoring those files does not rerun their tests. Current mapping
reconciliation and current source execution have separate inputs and receipts. M3JDK21 remains the
Java/HotSpot/JNI/runtime owner; Synexia remains the donor.

The retention extension reads obligations from immutable baseline commits and every direct parent.
It cannot silently discard obligations by deleting a catalogue row or manifest, removing a whole
history namespace, or rewriting an expected hash. An unchanged declaration whose target body has
evolved remains `PRESENT_DRIFTED_REVIEW`. The workflow checks the PR head with its pinned base and
parent snapshots; push verification can inspect the actual resulting merge commit. It does not
claim a prospective merge result or branch-protection guarantee.

## Execution

The focused Maven verifier uses the existing Rewrite 8.17.1, JUnit 5.10.2, compiler plugin 3.13.0
and Surefire 3.2.5 pins. Run it with the pinned JDK21 and admitted offline dependency closure:

```sh
mvn -o -B -f m3/tooling/migration-recipes/tasks/jcc-merge-recovery-20261006/verification/pom.xml verify
```

`JccMergeRecoveryTest` checks all 443 historical Git blob and SHA-256 identities, all six current
pre/postimages, the named recipe, serializer, output identity, fresh fixed point and conflicts.
When supplied `-Dm3.recovery.materialized=/new/empty/directory`, it writes actual OpenRewrite
Result bytes with `CREATE_NEW` after verifying the complete 449-output set.

The Python fixture verifier separately exercises the existing sealed installer, exact rollback,
non-mutating drift/guard/mixed-state refusals, the actual catalogue's 443 historical obligations,
and immutable-baseline deletion/reseal detection. Its 26 JEP targets are exact existing recipe
afterimages in a synthetic fixture; that test does not assert current JDK target acceptance.

```sh
python3 -B m3/tooling/migration-recipes/tasks/jcc-merge-recovery-20261006/verification/verify_installer.py \
  --repository /absolute/receiving/checkout --output /new/isolated/proof-directory
```

The broader owning-module coverage, source export, actual merge/CI, JDK, JNI/native, platform and
performance gates retain their existing authorities. Finite metadata and history custody proof
does not supply those gates.
