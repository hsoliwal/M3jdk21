# A3 serial FILE work planner

Status: M3JDK21 absorption-control extension.

## Purpose

A3 already inventories the current JDK tree, preserves every JEP/JBS/community disposition, and
can atomize/patternize explicitly selected Java files without writing product source.

The complete upstream compatibility queue already contains the missing physical information:

- exact upstream commit;
- touched paths;
- risk;
- scope floor;
- proof lane;
- recipe strategy;
- priority;
- next action.

The missing join is deterministic expansion from one feature/commit row into the smallest honest
FILE work units while retaining the parent feature's recomposition scope.

## A3Work

`A3Work` joins:

```text
A3 inventory of current src/test tree
        +
complete compatibility queue
        ↓
serial FILE work list
```

Each touched path becomes one ordered work row.

A FILE row records:

- upstream feature order and per-feature file order;
- release, commit and JBS identities;
- subject/domain/risk;
- parent join scope;
- proof lane and recipe strategy;
- priority and compatibility state;
- exact path;
- whether that target currently exists;
- current-tree kind when present;
- preparation lane;
- fixed FILE atom scope.

The feature's original `MODULE` or `MULTI_MODULE` scope is never weakened merely because its
physical files can be reviewed independently.

## Preparation lanes

Current Java target:

```text
A3_JAVA_ATOMIZE_PATTERNIZE
```

This lane uses the retained Java-21 OpenRewrite convergence recipe through `A3Apply`. It prepares
the current target implementation before donor absorption and never writes the source root.

Added Java target:

```text
ADD_JAVA_REVIEW
```

There is no target body to atomize. A later exact donor packet may use the existing hash-pinned
OpenRewrite add-file recipe.

Current native target:

```text
NATIVE_ATOM_REVIEW
```

OpenRewrite is not treated as a C/C++ parser. Native/JNI/HotSpot source uses the existing native
review/source-sealed lane and Java remains the semantic oracle where applicable.

Added native target:

```text
NATIVE_ADD_REVIEW
```

Current resource/build/test text:

```text
HASH_PINNED_TEXT_REVIEW
```

Absent non-Java target:

```text
HASH_PINNED_ADD_REVIEW
```

Unknown/no-path feature residue stays explicit as `FEATURE_REVIEW`.

## Serial per-file law

Within one compatibility-queue row, paths are normalized, deduplicated, sorted, and assigned stable
`file_order` values. The complete output is ordered by upstream queue order then file order.

This gives the requested near-serial review path without pretending coupled changes have FILE
promotion authority:

```text
feature
  -> file 0 inventory/atomize/patternize/review
  -> file 1 inventory/atomize/patternize/review
  -> ...
  -> parent scope recomposition
  -> compile/jtreg/runtime/native gates
```

Independent FILE preparation may execute in parallel physically, but canonical promotion remains
serial and proof-gated.

## Donor policy

OpenJDK release/commit lineage remains the source and contract donor for JDK absorption.

LeetCode, HackerRank and GeeksforGeeks may enrich algorithm taxonomy, edge cases, complexity classes
and benchmark ideas only when an upstream/JDK atom is algorithmic. Their solution bodies do not
become JDK source. Any selected mechanic is independently implemented or reused from an admitted
JDK/Synexia owner and must pass JDK contract/equivalence gates.

## Recipe-first custody

The A3Work source change itself is installed by a source-sealed OpenRewrite recipe before runtime
materialization.

The recipe owns:

- the exact current `A3.java` preimage to add the `work` command;
- additive `A3Work.java`;
- additive `A3WorkTest.java`.

Second application must be a fixed point.

## Completion boundary

A3Work is preparation evidence only. It does not admit compatibility, copy donor code, mutate JDK
product source, or promote a backport.

The product oracle remains:

```text
configure -> make -> jtreg -> runtime/JNI/HotSpot proof -> benchmark when claimed
```

## Compatibility-queue TSV codec

`COMPATIBILITY_QUEUE.tsv` is produced by Python `csv.writer(..., delimiter="\t")`. A3Work must
therefore consume the producer's quoting rules rather than assuming every physical tab separates a
column.

The A3 codec supports the bounded subset used by the queue:

- tab delimiter;
- double-quoted fields;
- doubled quotes inside quoted fields;
- strict refusal of unterminated quotes or characters after a closing quote;
- one physical line per queue record.

Display/evidence text is canonicalized to a single TSV cell by replacing embedded tab/CR/LF
characters with spaces. Paths, hashes, scopes and identifiers remain strict structural values and
are never repaired by textual substitution.

This keeps the complete upstream denominator readable without weakening path or identity checks.
