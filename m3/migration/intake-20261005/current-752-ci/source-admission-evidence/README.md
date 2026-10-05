# Current M3 CI recipes and Java/JNI qualification

The reusable Cij/Cit producer has passed real OpenRewrite execution, strict Java compilation, all nine JUnit methods and standalone generation of five exact adaptations. The separately sealed current native lane has passed all 32 commands against Synexia revision `9963cc08ff13922b92a0e3db7c30f56fddacba7d`.

This increment follows the existing recipe-first process: M3's actual CI failures are imported as evidence, Synexia owns the reusable task and qualification, and M3 receives the generated files through its existing sealed installer. All prior source, intake, reconciliation and native receipts retain their original revisions and scope.

## What the recipes change

| Family | M3 target | Exact adaptation |
| --- | --- | --- |
| Cij | `m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/M3A3RegexMemoryLabDeliveryRecipeTest.java` | Insert one missing backslash in the path-separator literal. |
| Cij | `m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/M3A3RegexMemoryWorkflowRecipeTest.java` | Insert one missing backslash in the path-separator literal. |
| Cit | `m3/backports/compatibility_policy.py` | Express the one physical source NUL as a Python escape, preserving the intended runtime rejection. |
| Cit | `m3/backports/test_compatibility_policy.py` | Retain six methods; add 18 NUL cases and two literal-text positive controls in a seventh method. |
| Cit | `.github/workflows/m3-foundation.yml` | Select the existing checksum-pinned JDK for Maven and print tool versions before the unchanged verification command. |

The two Java fixtures retain all four existing test methods and every byte beyond the two one-byte repairs. No public API, generic recipe implementation, parser or compiler owner changes.

## Reused owners

The [isolated Maven task](../../tasks/m3-ci-752/README.md) contains exact, attributed source dependencies. Cij uses M3's existing `M3Jdk21HashPinnedTextSnapshotRecipe` to handle malformed Java as text; Java parsing and real receiving-project compilation remain separate requirements. Cit uses Synexia's existing `M3HashPinnedTextSnapshotRecipe` unchanged. Cij's M3-specific recipe is scoped to the task classpath.

The [final owner resolution](../../docs/20261005-m3-ci-owner-resolution.md) explains the route. The earlier PlainText-to-Java-owner assessment was incorrect, and the contemplated owner extension was never implemented. Both static-review records remain explicit; neither is presented as an executed compiler or recipe failure.

## Actual producer evidence

[Producer result and reconstruction bundle](producer-current/README.md):

- RESULT SHA-256: `d6d265a959e378fce47d68b5aa4a3a3bbee14bebf21355a54bd8077cb51e8630`.
- Five-file output seal: `3895ac2617d07cefd481a299aa18092d49c5bfb9733177740038b35e1c161442`.
- Nine JUnit methods; zero failures, errors or skips.
- Four Cij and eight Cit before/after states; 32 union states in each of two recipe orders.
- Twelve Cij refusals, fourteen Cit refusals and ten changes-after-scan refusals.
- Exact Java reparsing, reproducible patches, fixed points and preservation of unrelated input.

The original v1 preparation stopped before any gate because the runner serialized path ordering inconsistently. The corrected v2 changes only that runner ordering. Both complete project snapshots and the original refusal are retained. The successful source and target images were not changed to make the oracle pass.

## Actual current Java/JNI evidence

[Native result and reconstruction bundle](native-current-v1/README.md) records nine lint, ten compile, six test and seven runtime commands. The strict Java/C builds, both sanitizer configurations, Java/native comparisons, missing-library/ABI cases and deliberately incorrect semantic variants all produced their required outcomes.

The observed checks include 2,895,062 Java short-flag assertions; 2,895,084 JNI flag assertions in each sanitizer configuration; 94,299 actual-tree assertions per backend over 5,021 nodes with zero query-triggered provider calls; 44,148 lazy-list assertions; and 293,132 superset assertions across 400 forests. These are functional checks, not timing or memory measurements.

Native RESULT SHA-256: `1e82bec642c8ea8ece15e8caeeed283416c1b612e22c357b20178524b6ab1605`.

## M3 receiving boundary

The receiving revision is `752191c9291f6467110fb8a7badfbdc4c2d41af2`, with the retained PR 148 intake applied additively. M3's task lives at `m3/migration/intake-20261005/current-752-ci/`. It records the five-path mapping and installer plan, preserves the 81-record authority and original 35-output receipts, and carries the native bundle byte for byte.

The receiving project's full Maven builds, coverage requirements, 108 historical intake tests, installer checks and current-context receipt replay have their own execution receipts in [M3 PR 148](https://github.com/hsoliwal/M3jdk21/pull/148). Producer acceptance alone grants no receiving-project or JDK runtime acceptance.

The bundles retain source and executed evidence with exact identities. They omit tool installations, dependency-cache bodies and compiled binaries while retaining the recorded hashes and omission boundaries. They do not establish a hermetic full repository build, other-platform execution, a complete JDK/HotSpot port, a speedup or a protected-branch merge.
