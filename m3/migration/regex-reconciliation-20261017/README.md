# Canonical regex mapping reconciliation

The existing naming authority now describes four previously unmapped regex capabilities. The observed ordered-relation adapter remains implemented but unverified; current dispatch, pressure and hierarchical acceptance candidates remain pending. Source fixtures are identified as candidates and never substituted for actual receiving owners.

Two existing metadata defects are repaired separately. The base TQ record now pins the already-present sparse implementation while retaining its former target in lineage. The sparse refinement's recipe path belonged to historical private source commit f1e9ff1f, not the receiving checkout; its exact manifest is retained as a pinned source artifact, while receiving recipe qualification remains pending. Original record status, contracts, test references and source-kernel custody remain intact. This transfers no runtime evidence.

The canonical file remains `m3/docs/name-mapping.json`, with the existing `name-mapping.schema.json` and `migration.py`. No additional semantic registry is introduced. All 55 top-level mappings and 20 gates remain exact; 54 old records are entirely unchanged, two retain their contracts/status/tests with explicit custody changes, and four records are appended.

## Mechanical recipe and rollback

The SDK directly reuses target-owned `M3Jdk21HashPinnedTextSnapshotRecipe` without modifying its engine. The repeatable entry is `com.m3.CanonicalRegexMappingReconciliation`: exact original A to final D, with fixed point at D. Independent stage recipes preserve A-to-B append, B-to-C TQ lineage and C-to-D source custody for review. Intermediate B/C are refused by the full entry and use their explicit next stage. These leaves are not advertised as an automatically idempotent nested composition.

`com.m3.CanonicalRegexMappingRollback` restores exact D to exact A and refuses foreign edits. [source-binding.json](source-binding.json) records all hashes, eligibility and inverse boundaries. [SOURCE_AUDIT.json](SOURCE_AUDIT.json), [CUSTODY_ROWS.json](CUSTODY_ROWS.json) and [ARTIFACT_BINDINGS.json](ARTIFACT_BINDINGS.json) contain source/hash evidence, not executable acceptance receipts.

## Evidence and remaining gates

Independent static/byte review passed for the 24-file SDK: three authored tests, 73 closure refusals, 21 report refusals, 15 operation refusals and two intermediate-state refusals. The tests retain five exact outputs and hash the actual 395-byte historical source manifest. Maven and Python global validation are NOT_RUN; dedicated SDK execution is NOT_SCHEDULED.

After an admitted bounded worker and dependency cache are available:

```text
python3 -I -S -B m3/migration/regex-reconciliation-20261017/sdk/verify.py --self-test
mvn -o -B -ntp -f m3/migration/regex-reconciliation-20261017/sdk/pom.xml test
python3 -I -S -B m3/migration/regex-reconciliation-20261017/sdk/verify.py --reports --repository <receiving-checkout>
```

The verifier always checks inputs; it has no `--inputs` option. Optional repository verification pins final D plus nine unchanged owners/schema/validator. The unchanged complete Python validator, physical symlink/root checks and runtime gates remain separate.

[COVERAGE.md](COVERAGE.md) is a RAM rendition of the existing pure renderer rules on D. It remains **INCOMPLETE**: 60 observed records, including 1 historically tested, 25 unverified, 32 pending and 2 blocked. It is not an exhaustive inventory or a claim that the Python command ran. The complete migration, all three integration routes, matched-JDK/JNI behavior, retention and performance remain open. Storage protection and continual optimization stay ACTIVE.

The [project recipe skill](https://github.com/hsoliwal/com.synexia/blob/515f2f2153aa6afbc758da34c495c7223ee7997a/.claude/skills/m3-recipe-mastery/SKILL.md) requires: “Keep the PR draft until the required Maven/OpenRewrite dry run, reactor compile/lint/JUnit gates, native/runtime gates and custody receipts actually execute.” This candidate remains draft.
