<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# Central AST naming-registry integration

This crate updates only the existing `m3/docs/name-mapping.json` authority. The
existing `M3HashPinnedTextSnapshotRecipe` emits the exact registry postimage; no
second registry or transformation engine is introduced.

The snapshot appends twelve exact mappings: six compiler frontend classes, the two
source-facts/cache adapters, the compiler-owned JavaCompiler-to-M3ASTPC observer
hook, the optional OpenRewrite adapter, and the two hierarchy/facts classes. Every mapping names its actual
canonical and target source paths, SHA-256 template identities, recipe custody,
proof, and final commit handoff. The plugin mapping also references the qualified
build-support adapters. The external plugin is named `M3ASTIndex`, preserving the
existing compiler-owned `M3AST` plugin and exact public `com.sun.source.util.M3AST`
API. Supporting nested atoms seal the additive existing AST snapshot accessor and
the existing partial cache's overflow correction without reserving new owners.

Only `reason` and `observation` change in the historical
`synexia.counterpart.MIndexCompilerASTPrecompute` migration record. Its full
counterpart remains pending. All prior mappings, global migration gates, java.base
reservations, family mappings, and the complete receiving sequence remain equal
to the pinned baseline.

The source-facts mapping records the user-confirmed hierarchy: L0 per-file facts
and atoms, L1 file summary, L2 package, L3 module, L4 project, L5 repository. This
source-facts atom supplies its L0 contribution. The hierarchy mapping points to
actual qualified lazy L0–L5 provider evaluation, precise dependency invalidation,
and the existing partial/cache owners. The related new upstream detached physical
atom/state leaf index is recorded separately as a read-only owner assessment.

Two exact recipes reach the same final postimage. `m3-ast-registry-20261017`
accepts the original registry; `m3-ast-registry-hierarchy-20261017` accepts the
previously qualified ten-mapping intermediate. Both are no-ops at the final
postimage, in either replay order. Each refuses the other route's preimage. The
original ten-mapping source, resources, and qualification remain in
`evidence/history/initial-ten-mappings/`.

## Qualification

The three actual Maven SDK tests passed before materialization and again during
application: both exact emission routes and composed fixed-point replay, preservation of an
unrelated source, missing/duplicate/preimage refusal, and template-drift refusal.
The emitted candidate and materialized target both passed `jsonschema` 4.23.0
validation against the repository's unmodified Draft 2020-12 schema.

Independent scope guards also reject six explicit mutations: receiving phase,
global gate, java.base reservation, full counterpart promotion, the L2 package
label, and an exact target source hash. Thirty-eight input manifest/template
hashes are checked during validation.

Evidence is retained in `evidence/QUALIFICATION.json`, with the original Maven
logs, Surefire summary, and separate candidate/target schema validation receipts.
The final source commit is deliberately resolved through
`m3/compiler/ast-sharing-20261017/handoff.json` after the combined work is sealed.

## Reproduce

Use Java 21 and Maven with your approved settings. Install the pinned proof-only
Python dependencies from `requirements-schema.txt` in your validation environment.
From this directory:

```sh
python3 registry.py prepare --target /absolute/path/to/M3jdk21
mvn -B -ntp test
python3 registry.py verify \
  --mapping target/candidate/m3/docs/name-mapping.json \
  --output evidence/candidate-validation.json
mvn -B -ntp -Dm3.registry.materialize=true \
  -Dm3.registry.target=/absolute/path/to/M3jdk21 test
python3 registry.py verify \
  --mapping /absolute/path/to/M3jdk21/m3/docs/name-mapping.json \
  --output evidence/target-validation.json
```

The Python preparation step writes fixture and recipe resources only. The
materialization step selects the route matching the exact admitted preimage (or
the shared fixed-point postimage), and writes only the actual SDK recipe result.
The final map has 68 entries: all 56 original mappings plus these 12 task-qualified
entries. No global status is promoted by this finite registry packet.
