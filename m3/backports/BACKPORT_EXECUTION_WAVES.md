# M3JDK21 backport execution waves

Status: deterministic scheduling projection over the existing M3 backport recipe DAG.

## Purpose

The backport programme already owns compatibility authorities, recipe packets and a canonical
dependency DAG. This layer answers the remaining operational question: **which admitted recipe atoms
may execute together now, and which must remain held?**

It never decides compatibility and never edits OpenJDK source.

## Canonical flow

```text
JEP / upstream / community authority
  -> backport_recipe_dag.py
  -> atomization / patternization / documentation / recipe / proof atoms
  -> optional exact scope-approval ledger
  -> backport_execution_waves.py
  -> deterministic FILE-parallel waves
  -> Maven / CI execution
  -> compile + jtreg/JUnit + runtime/JNI/VM proof
  -> fixed point
  -> serial promotion
```

## Scope

FILE atoms require no additional scope approval and may fan out when their dependencies are ready.

MODULE and MULTI_MODULE atoms are **held by default**. A scheduling run can unlock only an exact
atom listed in an external approval TSV:

```text
atom_id    approved    proof_ref
jep_485.recipe    true    review/JEP-485/module-approval
```

The planner validates that approved atom IDs exist in the canonical DAG. Approval is scheduling
evidence only; it is not Java-21 compatibility or promotion authority.

If a parent atom is held, a child becomes `DEPENDENCY_HOLD` even when the child has its own scope
approval. No dependent atom can bypass a held parent.

## Waves

Ready roots are wave 0. A ready child is assigned exactly one wave after its parent. Independent
work items therefore share waves and may execute horizontally.

The DAG authority currently forbids implicit cross-work-item dependencies. A future cross-JEP
dependency must first be recorded in a canonical dependency authority; this scheduler does not infer
one.

## Camel, Airflow and Drools

Camel/Airflow/Drools are projections of the same canonical wave rows.

Each row receives deterministic identities:

- `camel_route_id`;
- `airflow_task_id`;
- `drools_fact_id`.

These fields allow an external scheduler adapter to route tasks, build task dependencies or apply
eligibility rules without inventing another DAG.

No Camel route, Airflow DAG or Drools rule produced from these IDs receives source-mutation,
compatibility or promotion authority. The M3JDK21 packet DAG, Maven/OpenRewrite recipes, compiler,
jtreg/runtime proof and serial promotion remain authoritative.

## Maven evidence

`mvn -f m3/backports/pom.xml verify` emits:

- `target/backport-recipe-dag.tsv`;
- `target/backport-recipe-dag.json`;
- `target/backport-packets/**`;
- `target/backport-execution-waves.tsv`;
- `target/backport-execution-waves.json`;
- programme-status evidence.

With no approval ledger, the output is deliberately conservative: FILE work can become READY;
broader work stays visible and held.

## M3 invariant

Repository size does not widen edit authority. Small recipe atoms are preferred. Independent FILE
atoms may execute in parallel, while work inside one dependency chain remains deterministic.

Atomization, patternization, IOP role admission and derivable documentation occur before the source
recipe/proof tail in the existing backport DAG. This scheduler therefore accelerates the proven
recipe DAG; it does not bypass it.
