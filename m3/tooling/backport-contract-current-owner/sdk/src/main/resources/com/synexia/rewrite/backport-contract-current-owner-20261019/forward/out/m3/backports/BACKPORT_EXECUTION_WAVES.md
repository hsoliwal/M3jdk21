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

Admission reasons have explicit precedence. Non-executable evidence remains `EVIDENCE_HOLD`.
An executable atom without its own required scope approval remains `SCOPE_APPROVAL_HOLD`,
including when its parent is also held. Only an otherwise-ready child of a held parent becomes
`DEPENDENCY_HOLD`; its own scope approval cannot bypass that dependency.

Consequently, two unapproved MODULE or MULTI_MODULE atoms retain their individual scope holds.
Approving only the child leaves the parent scope-held and makes the child dependency-held.
Approving both permits execution only when their remaining admission checks pass. A held atom
always has wave -1; scope approval is not dependency readiness or promotion authority.

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

## Machine-readable wave serialization

The TSV is a real tab-delimited record stream: one LF-terminated header and one LF-terminated
physical row per atom. The JSON is a valid UTF-8 JSON document with exactly one terminal LF.
Writing the two literal characters backslash and `n` is not equivalent to writing a line
terminator: it merges TSV rows and invalidates the JSON suffix.

The `test_emitted_waves_are_real_tsv_rows_and_single_terminated_json` regression parses the
actual output through `csv.DictReader` and `json.loads`, checks exact physical row counts,
wave assignments, root identity and terminal newlines. A serialization failure holds the output;
Camel/Airflow/Drools scheduling cannot consume a malformed or unverified wave artifact.

## M3 invariant

Repository size does not widen edit authority. Small recipe atoms are preferred. Independent FILE
atoms may execute in parallel, while work inside one dependency chain remains deterministic.

Atomization, patternization, IOP role admission and derivable documentation occur before the source
recipe/proof tail in the existing backport DAG. This scheduler therefore accelerates the proven
recipe DAG; it does not bypass it.

## Source-sealed admission safety (2026-10-14)

A scheduler projection is not proof of compatibility. Before producing any Camel, Airflow or
Drools task identities, the wave planner now checks:

- approval TSV headers and row widths, including zero-row ledgers;
- duplicate atom IDs **across all approval rows**, including denied then approved entries;
- exact approved atom membership in the canonical DAG;
- contiguous canonical atom order and known semantic edit scopes;
- absence of unapproved cross-work-item dependencies;
- one-to-one atom-to-scheduler identity after normalization (no `a-b` / `a_b` alias);
- explicit non-executable evidence never carries `scope_approved=true`, including FILE atoms.

The existing `FILE -> VISIBILITY -> PACKAGE -> MODULE -> MULTI_MODULE -> LIBRARY_API`
authority ladder is unchanged. Scope approvals remain declarations for scheduling, not
independently verified recipe/JUnit, Java-21 compatibility or release promotion receipts.

JEP 475 (G1 Late Barrier Expansion) is an intentional negative proof fixture:
its MULTI_MODULE recipe/compile atoms remain held without explicit scoped approval, and no
scheduler layer may turn its 58-path inventory into a compatibility claim.

The source-sealed successor recipe is canonically authored in `hsoliwal/com.synexia`;
M3JDK21 receives only the reviewed postimages and an immutable source/proof reference.
The previous `M3BackportExecutionWaveReceiverRecipe` and its preimages are retained for
history. The successor is an additive refinement, not a silent rewrite of that evidence.
