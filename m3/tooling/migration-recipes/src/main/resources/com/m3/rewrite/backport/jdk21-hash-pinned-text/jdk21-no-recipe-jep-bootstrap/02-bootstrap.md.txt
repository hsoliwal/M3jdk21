# High-risk JEP recipe bootstrap

Status: implementation-authority and FILE-recipe evidence generation only.

## Scope

The current residue queue contains multiple kinds of `NO_RECIPE_EVIDENCE` work. This lane covers
only the rows whose next action is:

`CLOSE_DEPENDENCY_AND_HIGH_RISK_PROOF`

JEP 404 and JEP 475 already have dedicated exact-path packets, so this batch covers the remaining
14 high-risk JEPs:

`483, 506, 509, 514, 515, 516, 517, 518, 520, 521, 522, 527, 534, 536`.

Policy/preview/vector residue stays in its own compatibility/dependency lanes.

## Authority

`HIGH_RISK_IMPLEMENTATION_AUTHORITY.tsv` records the exact OpenJDK implementation commit for each
JEP, along with JBS identity, final GA donor tag and the exact implementation-commit path count.

This ledger is **not** Java-21 compatibility authority.

Special lineage is explicit. For JEP 509 the admitted implementation commit is the redo
`8358666`, after implementation `8342818` was backed out by `8358628`.

Known dependency edges already present in M3 evidence are retained. JEP 521 additionally records
that its two-file non-experimental delta sits on top of JEP 404's Generational Shenandoah
implementation.

## Batch algorithm

For each matrix row:

```text
pinned implementation commit
 -> prove commit exists
 -> prove commit is ancestor of final GA tag
 -> derive exact changed paths from git
 -> prove path count equals ledger
 -> compare Java21 against implementation commit
 -> compare Java21 against final GA
 -> classify exact implementation paths against GA
 -> select MATERIALIZE_GA_POSTIMAGE paths
 -> generate one-file Java/native/text recipe crates
 -> account for generated targets + typed exclusions
 -> upload evidence
```

Final-GA postimages retain follow-up fixes without expanding the implementation path denominator.

## Parallelism

Each JEP matrix job is independent evidence work and may run in parallel. Within one JEP, path
inventory and crate generation are deterministic. One generated crate owns one physical FILE target.

Feature composition remains a later dependency/scope step. A 300-file HTTP/3 implementation is
still 300 independently reviewable FILE recipe atoms before any multi-module composition decision.

## Camel / Airflow / Drools

The batch artifacts feed the canonical backport recipe DAG and execution-wave planner. Camel,
Airflow and Drools remain projections of that DAG. They do not become compatibility, source-mutation
or promotion authorities.

## Completion boundary

A green bootstrap job means only:

- implementation authority is pinned;
- the exact implementation denominator is proved;
- final-GA selected paths are classified;
- source-pinned FILE recipe evidence can be generated.

It does not mean the JEP is compatible with Java 21, builds successfully, passes jtreg/runtime proof,
or is promotable.
