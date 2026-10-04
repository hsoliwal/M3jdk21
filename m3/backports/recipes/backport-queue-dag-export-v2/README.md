# Complete backport queue DAG export v2

This packet makes the complete M3JDK21 compatibility queue mechanically schedulable without
granting source-mutation or promotion authority.

## Source atoms

Five source-sealed FILE atoms own the exporter, unit proof, documentation, dedicated CI workflow and
recipe catalogue registration. `M3BackportRecipeDagExportRecipe` is the only MULTI_MODULE join.

## Queue semantics

Every queue item becomes the canonical seven-pass chain:

```text
inventory
 -> compatibility-classify
 -> dependency-closure
 -> materialize
 -> apply
 -> verify
 -> promote
```

Only `apply` is declared source-changing. Exported rows always have
`manifest_mutation_authority=false` and `promotion_authority=false`.

An apply node can be `execution_ready=true` only when the exact bound recipe has both status and
verification in VERIFIED/PASS/GREEN state.

## Orchestration

The queue TSV is candidate scheduling evidence. The packet describing this exporter itself is a
canonical `M3RecipeDag` input and is projected by the existing control plane into Apache Camel,
Apache Airflow and Drools artifacts.

Workflow engines may schedule admitted recipe bindings. They may not infer equivalence, widen scope,
write JDK source directly or promote canonical truth.

## Fixed point

The source-sealed recipe generates four absent targets plus one exact recipe-catalogue replacement.
Second replay must be unchanged. Queue export is deterministic under input-row reordering and emits
a content-addressed DAG root receipt.
