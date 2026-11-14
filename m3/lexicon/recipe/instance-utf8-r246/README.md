Copyright 2026 Hitesh Soliwal, Synexia. SPDX-License-Identifier: Apache-2.0

M3InstanceIndexPrecompute compares names and metadata keys by encoding both Strings into fresh UTF-8 byte arrays for every tree comparison. This recipe replaces only that private comparator with a scalar scan over the same immutable text. It keeps unsigned UTF-8 order and the existing '?' replacement for unpaired surrogates, without constructing encoded arrays. The algorithm requires constant auxiliary space; the tree, public API, source identity and owned storage stay unchanged.

The existing JDK hash-pinned AST recipe first replays its own exact instance-index path registration, then generates the index owner from its pinned preimage. Six JUnit recipe tests cover exact output, fixed points, missing/duplicate/foreign refusal and neighboring/traversal paths. No broad core-path admission is introduced.

Qualification ran the actual unchanged baseline and exact generated owner in separate JVMs. Five behavior JUnit tests passed in each stage, including 1,408,801 exact encoding-oracle comparisons per stage: every UTF-16 unit against ten pivots in both directions, all pairs from a short malformed/supplementary corpus and seeded longer inputs. The two unchanged contract mains reported 25 and 59 checks in both stages. Public views preserve name/metadata order, record identity, immutability, source scope, refusals and existing reverse-view ordering. No normalization or reverse-order contract change is bundled.

On Java21.0.9+7-LTS-338, five fixed 20000-operation rounds observed:
- Direct comparator allocated bytes before: [5000000,5000000,5000000,5000000,5000000]; after: [0,0,0,0,0].
- Public findByName allocated bytes before: [33300456,33300456,33300456,33300456,33300456]; after: [6262472,6262472,6262472,6262472,6262472].
- Timing is recorded for every round. It varies and does not establish a global speedup; public normalization still allocates.

ICU's code-point-order idea was evaluated before implementation, with pinned source/license and no copied method body. Its malformed-surrogate order differs from the existing UTF-8 encoder contract. Actual JDK encoding remains the oracle. The initial recipe-test adaptation had an incorrect neighbor suffix (a no-op rename); the test caught it. Its complete failed receipt is retained, and the corrected test passed on the final rerun.

The canonical packet is Synexia synexia-openrewrite-recipes/crates/m3-instance-utf8-r246/; the identical M3JDK mirror is m3/lexicon/recipe/instance-utf8-r246/. M3JDK additionally installs the two recipe manifests/templates and the two generated existing source owners. R245 mapping work remains in its separate PR. Current default-branch source pins are included; any later source drift requires recipe refresh and new proof.

Run the sealed qualification with:
```text
python -X utf8 -I -S -B qualification/run.py
```
The unchanged replay helper assembles hash-bound readable inputs into RAM and invokes the existing offline Maven/RAM compiler with the pinned 47-JAR classpath. Existing Windows toolchain/cache paths are prerequisites; the runner downloads/copies no cache. Full logs and packet/source identities are in evidence/qualification.json, a zlib/hex envelope bound to expanded length and SHA-256.

All four stages passed. Zero scratch files were retained; one exclusive temporary POM was removed after owned children were reaped. The authorized 545 MiB total task cap and 8 GiB K: reserve were preserved; F: inputs stayed read-only.

This is Java behavior/allocation evidence on one platform. It does not prove a new JNI build, ARM/Android/macOS/ESP/Pico execution, whole-reactor completion, complete corpora or a full LangDex admission bridge. The source-only UnicodeX JNI/identity findings are catalogued separately with their unexecuted discriminators. The broader goal remains active.

The final registration uses independent anchors from R245; deterministic source edits compose to identical bytes in either order. evidence/registration-composition.json records that source-level check without claiming a completed Git merge or union build.
