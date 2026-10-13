# M3JDK21 backport programme status — 2026-10-13

This is current-tree evidence, not a claim that the complete backport programme is finished.

## Recipe/DAG custody

The previously pending Synexia canonical-DAG handoff is superseded by a proof-bound canonical
Apache-2.0 handoff.

Current receiver state:

- canonical reusable recipe owner: `hsoliwal/com.synexia`;
- target: `hsoliwal/M3jdk21`;
- application policy: `PROVEN_RECIPE_DAG_ONLY`;
- Nebula proving ground: `hsoliwal/nebula`;
- reusable DAG/orchestration dependency: admitted;
- source mutation authority from handoff: false;
- semantic-equivalence authority from handoff: false;
- Java-21 compatibility authority from handoff: false;
- promotion authority from handoff: false.

Exact machine receipts:

- `m3/backports/SYNEXIA_NEBULA_RECIPE_DAG_HANDOFF.tsv`;
- `m3/tooling/backport-dag/src/main/resources/com/m3/tooling/dag/m3-synexia-canonical-dag-binding.tsv`;
- `m3/synexia-import/nebula-mastered-dag-receiver-v2.tsv`.

Historical draft/blocked receipts remain in-tree as evidence and are not rewritten.

## Nebula disposition

The recipe-first proving model is established:

```text
Maven/Tycho baseline
 -> Java inventory
 -> small OpenRewrite atom recipes
 -> atomization
 -> patternization / IOP
 -> derivable documentation
 -> JUnit / semantic coverage
 -> second-pass fixed point
 -> original Tycho build on candidate
 -> proof-bound transfer
```

The same canonical DAG may be projected to Maven/OpenRewrite, Camel, Airflow and Drools/KIE.
Those projections have no mutation or promotion authority.

The latest Nebula closure keeps fresh native GTK/Win32/Cocoa current-head execution
`PENDING_EXTERNAL_CI`. That does not reopen recipe/DAG custody, but it does prevent using that
receipt as native-runtime promotion proof.

## Backport denominator

Backport execution remains active against the existing released denominator:

- 85 released JEP authority rows;
- JDK 22 through JDK 27 GA intervals;
- 14,948 upstream commits in the released interval inventory;
- non-JEP/JBS enhancement intake;
- community/FOSS candidate intake.

The active residue remains authoritative in `JEP_RESIDUE_QUEUE.tsv`.

## Target execution

For every compatible work item, the target path remains:

```text
authority inventory
 -> compatibility decision
 -> dependency closure
 -> exact file delta
 -> A3 FILE atomize/patternize/document preparation
 -> source-pinned reusable recipe
 -> recipe JUnit
 -> diff
 -> lint/static
 -> Java-21 compile
 -> jtreg/JUnit
 -> runtime/VM/JNI parity when required
 -> fixed point
 -> serial promotion
```

A canonical recipe-DAG handoff cannot bypass any of these target-specific gates.

## Completion boundary

The entire backport programme is not complete while compatible residue remains. Current work should
continue by consuming `JEP_RESIDUE_QUEUE.tsv` in deterministic priority order and improving one
bounded recipe/proof packet at a time.
