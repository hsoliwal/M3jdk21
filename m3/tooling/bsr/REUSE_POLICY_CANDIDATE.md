<!-- SPDX-License-Identifier: Apache-2.0 -->
# Full-contribution reuse policy candidate

Status: **DRAFT / NOT REPLAYED / NOT PUBLISHED**. This is recipe input, not a
second product policy or naming registry. The canonical document is unchanged.

This additive successor uses the existing `com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe` in the existing
`m3/tooling/bsr` Maven crate. No new framework, engine, runtime owner or dependency
is introduced. Historical manifests, templates and receipts remain untouched.

Pinned base: `hsoliwal/M3jdk21@05daa15c75524cb2ed76cfdb0b44d62bacaa47fb`.
Canonical output, after qualification: `m3/docs/M3JDK21_PORTING_INVARIANT.md`.
Before-image Git blob: `83b223703880997bfbec8083d6c36f187a6063c3`.
The manifest pins exact UTF-8 before/after SHA-256 values. Seven new JUnit tests
cover source/tool pins, replay and fixed point, source drift, missing/duplicate
inputs, unrelated-source preservation, template tampering and policy scope.

## Replay

From the repository root, on Java 21 with Maven and the declared dependencies:

```sh
mvn -B -f m3/tooling/bsr/pom.xml -Dtest=ReusePolicyTest test
```

The test emits only `m3/tooling/bsr/target/generated-reuse/m3/docs/M3JDK21_PORTING_INVARIANT.md`.
It never writes the canonical document. Run the existing crate's complete suite
and required custody checks as well. Review the exact output, reconcile concurrent
changes, and compose the executed candidate into the existing sealed successor
before publication. Do not manually overwrite an admitted policy or refresh old
proof hashes to hide drift. Existing naming maps, statuses and gates are unchanged.

## Current evidence and limitation

Only UTF-8/blob/manifest validation and Java test syntax parsing were performed
for this candidate in this session. Maven is not installed in the local runtime,
and direct dependency retrieval is unavailable. **JUnit execution, OpenRewrite
replay, full custody verification and hosted CI qualification are NOT established.**
No runtime code was changed, and no prior proof is claimed for this successor.

The policy makes all product targets owners of polished public code and explicitly
includes source, recipes, templates, tests, benchmarks, documentation and receipts
in eligible reuse. It preserves Apache-2.0 for authorized Synexia contributions
without relicensing third-party/OpenJDK material or presuming GPLv2 compatibility.
