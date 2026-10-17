# Hash-pinned JDK21 text crate manifest reconciliation (2026-10-17)

Status: **exact-data/fixture repair candidate; Maven and JDK acceptance pending**.

## Inventory, not a new recipe engine

Audited every one of the **72** crate directories under
`m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/`
against its current `manifest.tsv` and file listing on `master`.

For each crate, inspected four-field TSV width, strict Java lexical target-path
ordering, original flat template filename syntax `[A-Za-z0-9_.-]+`,
and existence of each declared template file in that crate root.
This does **not** attest SHA-256 resource payloads, runtime compatibility,
OpenJDK API behavior, or recipe fixed point; those remain Maven tests.

Exactly **four** crate manifests failed those structural checks:

| Crate | Failure | Receiving correction |
| --- | --- | --- |
| `jdk21-jep-residue-proof` | All 3 template names mistakenly prefixed `templates/`, but actual sealed files exist directly in the crate root | This PR: strip the nonexistent path prefix from three template fields |
| `m3-string-mechanical-donor-map` | Two middle target paths reversed in strict lexical order | This PR: reorder four existing exact lines |
| `m3-string-stream-traversal` | Workflow target follows source checker instead of leading in strict lexical order | This PR: reorder three exact lines |
| `release-jep-authority-repair-20261006` | Eleven manifest rows in ordinal/narrative order instead of Java `String.compareTo` target-path order | Independently addressed in [JEP authority PR #1015](https://github.com/hsoliwal/M3jdk21/pull/1015); not duplicated here |

### Existing canonical parser remains unchanged

`M3Jdk21HashPinnedTextSnapshotRecipe.java` and its own validation tests
are **byte-identical to original master** after checking the actual resource
layout. No relaxed directory traversal, URL interpretation, source-path
admission, checksum, or template-lookup rule is introduced. Every original
template name remains a safe single file basename.

Each current manifest entry retains its:
- exact source target path;
- expected source preimage SHA-256 or ABSENT;
- expected output SHA-256;
- original template bytes / object provenance.

Only the path-ordering metadata or incorrect resource-name prefix changes.

### Fail-closed JEP-residue test repairs

`M3JepResidueProofGateReceiverRecipeTest` previously provided *two* preimages
to a required three-target recipe, then tried to read `before.get(2)` in its
negative case. Its positive source-bound check now includes the QUEUE, SOURCE
and TEST together, retains the original unrelated-input preservation test,
and verifies divergent/missing sources still throw runtime refusals. The
test's execution-context error listener now preserves `IllegalStateException`
rather than wrapping legitimate refusal errors in `AssertionError`.

### Required promotion gates

1. `mvn -B -ntp -f m3/pom.xml -pl tooling/migration-recipes -am test`
   (including named JEP-residue and both M3 String text regression suites).
2. Existing source-sealed recipe replay, negative drift/refusal and fixed
   point tests, exact current tree source/postimage SHA readback.
3. Existing Java21/HotSpot/JNI/JFR acceptance for specific JEPs, if any
   resulting runtime source is later materialized.

This change touches **no OpenJDK product source** and does not grant target
mutation/feature completion authority. Synexia remains the canonical donor
of reusable A3 transformations; M3JDK21 owns these exact target resource
receipts. Keep review draft until hosted tests complete.
