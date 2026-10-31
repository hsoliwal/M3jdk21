# Nebula mastered-DAG receiver — successor receipt (2026-10-13)

Status: **recipe/DAG custody admitted; target runtime/native promotion remains fail-closed**.

This is an additive successor to the historical
`m3/synexia-import/nebula-mastered-dag-receiver.tsv`. The older receipt remains immutable evidence
of the GitHub Actions startup failure around Nebula PR #104. This successor does not rewrite or
reinterpret that historical result.

## Current proving evidence

- Nebula repository: `hsoliwal/nebula`
- current observed master: `7b36ce9e3e0c8d1e91324589b38b9d9d5a164356`
- recipe/DAG transfer document: `m3/docs/FINAL_PROVEN_TRANSFER_20261006.md`
- current-master runtime-equivalence receipt:
  `m3/convergence/CURRENT_MASTER_RUNTIME_EQUIVALENCE_CLOSURE_20261011.md`
- latest UI-distillation closure:
  `m3/convergence/FINAL_UI_DISTILLATION_CLOSURE_20261012.md`

The later receipts establish that reusable recipe/DAG custody returned to Synexia and that the
qualified Nebula runtime had zero source drift through the recorded closure. The latest closure also
explicitly keeps fresh native GTK/Win32/Cocoa current-head execution pending external CI.

## Canonical reusable ownership

M3JDK21 consumes the reusable recipe/DAG contract only through the Synexia Apache handoff:

`m3/backports/SYNEXIA_NEBULA_RECIPE_DAG_HANDOFF.tsv`.

This successor receiver therefore records:

- recipe DAG transfer: `ADMITTED`;
- reusable custody: `CANONICAL_SYNEXIA_APACHE_HANDOFF`;
- runtime source-equivalence evidence: `GREEN_ZERO_DRIFT_FROM_QUALIFIED_RUNTIME`;
- native current-head execution: `PENDING_EXTERNAL_CI`.

## Authority boundary

This receipt grants none of:

- OpenJDK source mutation authority;
- semantic equivalence;
- Java-21 compatibility admission;
- backport promotion.

The portable DAG enters the existing M3JDK21 target control plane:

```text
Synexia canonical handoff
 -> JEP/JBS compatibility authority
 -> dependency closure
 -> exact file delta
 -> A3 atomize/patternize/document
 -> source-pinned recipe crate
 -> recipe JUnit
 -> diff/lint
 -> compile/jtreg
 -> runtime/JNI/VM proof
 -> fixed point
 -> serial target promotion
```

Camel, Airflow and Drools/KIE remain scheduler/policy projections only.
