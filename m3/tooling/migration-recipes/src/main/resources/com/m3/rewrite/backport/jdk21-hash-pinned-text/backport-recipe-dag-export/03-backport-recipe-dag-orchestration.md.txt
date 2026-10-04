# M3JDK21 backport recipe-DAG orchestration

Status: candidate-only interchange contract between the M3JDK21 backport programme and the
canonical Synexia M3 recipe-DAG runtime.

## Why this exists

M3JDK21 already owns the complete released-change denominator, compatibility queue, canonical
seven-pass backport lifecycle and source-bound OpenRewrite recipes. Synexia already owns the
backend-neutral atomic recipe DAG and its Camel, Airflow and Drools/KIE projections.

This bridge does **not** create another workflow engine.

It exports the backport queue into deterministic DAG evidence so the existing M3 orchestrators can
reason about the same pass topology without gaining mutation or promotion authority.

## Canonical backport lifecycle

Every queue item has the same ordered dependency chain:

```text
inventory
  -> compatibility-classify
  -> dependency-closure
  -> materialize
  -> apply
  -> verify
  -> promote
```

Only `apply` is a source-changing semantic pass.

The queue export deliberately records:

```text
declared_source_changing=true
manifest_mutation_authority=false
promotion_authority=false
```

for the apply node.

The distinction is intentional. The queue tells an orchestrator **where** mutation belongs. It does
not authorize the mutation.

## Binding and proof

`m3/backports/export_recipe_dag.py` joins queue identities to
`m3/tooling/recipe-catalogue.tsv` only when the identity is unambiguous.

An apply node is:

- `UNBOUND` when no recipe catalogue entry exists;
- `RECIPE_DECLARED` when an exact recipe id/class is known;
- `execution_ready=true` only when both recipe status and recipe verification are explicitly
  `VERIFIED` (or the admitted PASS/GREEN synonyms).

A draft PR, materialized source change, catalogue row, or `PENDING_CI` receipt is therefore never
converted into execution authority.

At the current programme frontier, adapted backports remain verification work. The exporter is
expected to report zero execution-ready apply nodes until the corresponding proof records are
actually green.

## Manifest identity

The exporter emits TSV with one row per `queue item × pass`.

Every queue item has a content-addressed `item_root`. Every DAG node has a content-addressed
`node_root`. The full ordered node-root sequence has one `dag_root`.

The exporter sorts items deterministically and uses a length-prefixed SHA-256 field encoding, so
queue row ordering cannot change semantic identity.

The CI root receipt records:

- schema;
- DAG id;
- item count;
- node count;
- execution-ready node count;
- DAG root.

## State projection

For one queue item's current pass:

- earlier passes are `REACHED`;
- the selected pass is `CURRENT`;
- later candidate work is `FUTURE`;
- rejected items expose later work as `BLOCKED`;
- superseded items expose later work as `REDIRECTED`;
- held items expose later work as `HELD`.

Nothing disappears from the denominator.

## Synexia consumption

The canonical Synexia transformation model is:

```text
OpenRewriteRecipeAtomSpec
        |
OpenRewriteRecipeDagPlan
        |
      RecipeDag
   /      |       \
Camel   Airflow   Drools/KIE
        |
proof receipts
        |
serial promotion outside orchestrator
```

M3JDK21's TSV is an **interchange candidate**, not an executable `OpenRewriteRecipeAtomSpec`.

A downstream importer must fail closed for a source-changing node unless it has, at minimum:

1. exact recipe class/id;
2. exact recipe/manifest root;
3. Maven POM/profile/goal binding;
4. exact target/documentation roots required by the atom model;
5. required proof gates;
6. compatible edit scope;
7. verified proof status.

Unbound or unverified rows remain review/scheduling evidence.

## Camel / Airflow / Drools roles

- **Camel** may execute an already-admitted exact Maven/OpenRewrite binding.
- **Airflow** may schedule the same DAG dependency graph and exact argument vectors.
- **Drools/KIE** may deny or hold candidate nodes according to immutable policy facts.
- None may infer semantic equivalence.
- None may invent a recipe binding.
- None may edit source directly.
- None may promote canonical truth.

Maven/OpenRewrite/JUnit/OpenJDK build+jtreg remain the source-change and proof substrate.

## Horizontal scale

Independent queue items can be reviewed/materialized in parallel. Inside one item, the seven passes
are ordered. Source-changing file-local recipe leaves may fan out only under their own M3 scope
contract.

This preserves the core M3 property:

```text
large denominator != broad mutation authority
```

Fourteen thousand independent upstream changes do not become one repository-wide refactor merely
because they share one queue.

## Commands

Generate ephemeral orchestration evidence:

```bash
python3 m3/backports/export_recipe_dag.py \
  --output /tmp/BACKPORT_RECIPE_DAG.tsv \
  --root-output /tmp/BACKPORT_RECIPE_DAG_ROOT.txt
```

The main backport workflow publishes those two files as a review artifact. They are generated from
the canonical queue/pass/catalogue sources rather than maintained as a second source of truth.

## Completion rule

This bridge does not change the M3JDK21 completion denominator.

Backport convergence still requires every released upstream change to reach an explicit
compatibility decision and every accepted change to reach exact source provenance, recipe replay,
build/compiler/jtreg/runtime proof where applicable, fixed point and serial promotion.

The DAG export simply makes that unfinished work mechanically schedulable without weakening its
proof contract.
