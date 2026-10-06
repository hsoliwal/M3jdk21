# A3 Synexia delivery work bridge

## Purpose

A sealed Synexia delivery manifest identifies **what** Apache-2.0 assets M3JDK21 may receive.
`SynexiaImportPlan` identifies how those assets differ from the currently vendored snapshot.

A3 still needs a deterministic answer to the next question:

> Which target-side proof/review lane should consume each delivered asset?

This bridge converts the full delivery manifest plus the current vendor snapshot into an ordered,
read-only A3 work table. It does not stage files, apply recipes, modify OpenJDK source, or promote
anything.

## Input chain

```text
Synexia clean pinned revision
  -> M3_DELIVERY_EXPORT_V1 manifest
  -> M3JDK21 SynexiaImportPlan
  -> A3Synexia work rows
  -> target-specific review/proof
```

The bridge reuses the existing `SynexiaImportManifest` and `SynexiaImportPlan` Java models.
There is no second TSV parser or duplicate delivery-state implementation.

## Review lanes

The import plan's asset lane maps to one A3 review lane:

| Delivery lane | A3 review lane |
| --- | --- |
| OPENREWRITE_RECIPE | RECIPE_EXECUTION_REVIEW |
| OPENREWRITE_TEST | RECIPE_PROOF_REVIEW |
| OPENREWRITE_RESOURCE | RECIPE_RESOURCE_REVIEW |
| CONVERGENCE_JAVA | JAVA_ATOM_PATTERN_REVIEW |
| CONVERGENCE_TEST | JAVA_CONTRACT_PROOF_REVIEW |
| CONVERGENCE_NATIVE | JNI_NATIVE_PARITY_REVIEW |
| M3_CLONER | DELIVERY_TOOL_REVIEW |
| M3INDEX | INDEX_PRECOMPUTE_REVIEW |
| M3_RECIPE | RECIPE_DAG_REVIEW |
| OTHER_APACHE | APACHE_MANUAL_REVIEW |

These are review/proof destinations only.

They do not state that a donor/convergence class belongs in `java.base`, HotSpot, or any other JDK
runtime module.

## Delta action

Each row also preserves the import action:

- KEEP -> `REUSE_VERIFIED_VENDOR`
- ADD -> `STAGE_THEN_REVIEW`
- REPLACE -> `STAGE_THEN_REVIEW`
- STALE -> `REVIEW_STALE_NO_DELETE`

Only ADD/REPLACE require a staged candidate. STALE never authorizes deletion.

## CLI integration

The A3 CLI gains:

```text
A3 synexia --root <M3JDK21> --manifest <manifest-relative-to-root> --out <m3/build/...>
```

The manifest must be inside the M3JDK21 working tree/build evidence plane. A3 does not follow an
arbitrary external path.

Output is deterministic TSV sorted by target path.

## Relationship to atomize/patternize

The bridge does not atomize arbitrary imported recipe implementation source automatically.

Instead:

- reusable OpenRewrite recipes remain canonical Synexia-owned tooling;
- convergence Java intended for target adaptation enters Java atom/pattern review;
- native convergence code enters Java-oracle/JNI parity review;
- M3Index/precompute material enters index/precompute review;
- actual JDK source changes remain A3 FILE candidates with their own source-bound recipes.

This preserves the user's invariant:

```text
inventory -> recipe -> atomize -> patternize -> compiler/JUnit -> fixed point -> absorb
```

while preventing an Apache vendor asset from being mistaken for an already-admitted OpenJDK product
change.

## Problem/challenge evidence

LeetCode, HackerRank and GeeksforGeeks remain reference-only problem/category evidence.
GitHub donors retain their separate repository/revision/license custody.

A3 consumes only Synexia-owned output that has already crossed the Apache delivery gate.

## Completion boundary

A row reaching `REUSE_VERIFIED_VENDOR` means only that the target-side vendor snapshot exactly
matches the sealed Synexia export.

A row reaching `STAGE_THEN_REVIEW` means only that a candidate should be staged under `m3/build`.

Neither state is JEP/backport/runtime acceptance.
