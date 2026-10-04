# M3IndexDB semantic atom index

Status: M3JDK21 tool-plane implementation contract.

## One owner

M3IndexDB is the sole persistence and canonical semantic-index owner.

Do not create a parallel SQLite/H2 semantic database. Do not duplicate the semantic binary format
inside OpenRewrite recipes. OpenRewrite, MIndexAST, LSEM and future analyzers are producers; they
emit typed candidates that are normalized and persisted by M3IndexDB.

M3IndexDB remains Apache-2.0 tool-plane code. It is not an OpenJDK product/runtime dependency.

## Hierarchy

The admitted semantic vocabulary is:

    REPOSITORY
      PROJECT
        LIBRARY
          MODULE
            PACKAGE
              FILE
                DOCUMENTATION
                INTERFACE
                IMPLEMENTATION
                  FIELD
                    ATOM*
                  METHOD
                    ATOM*
                  INTERFACE / IMPLEMENTATION (nested)

The vocabulary may be atomized further without changing this contract. A later FILE recipe may
split one current ATOM into child ATOMs. Parent identities are recomputed from their admitted child
composition.

## Stable identity versus current content

A semantic node has two independent concepts:

1. stable identity: SHA-256 of M3_NODE_V1 + kind + semanticKey;
2. current content/composition fingerprint.

Examples:

- type identity is qualified-name owned;
- method identity is signature owned;
- field identity is field-name owned;
- file identity is source-path owned;
- statement/expression atoms may retain ordinal identity where execution order is observable.

Declaration reordering must not accidentally rename methods, fields or types.

## Fingerprint planes

Every node carries:

- exact SHA-256: exact UTF-16 source/content identity;
- structural SHA-256: normalized AST/composition shape;
- logic SHA-256: normalized operation/composition identity;
- structural hash64: compact structural lookup signal;
- logic hash64: compact normalized logic lookup signal;
- SimHash64: locality-sensitive near-similarity signal;
- normalized composition string.

These are distinct claims.

An exact hash proves exact indexed content identity.

A structural or logic hash match is a recognition signal, not proof of behavioral equivalence.

A SimHash distance is candidate similarity only. It must never auto-merge files, atoms or methods.

## Child-first composition

Leaf fingerprints come from source analyzers.

M3IndexDB recomputes every parent that has children, leaf-first:

    ATOM
      -> FIELD / METHOD
      -> INTERFACE / IMPLEMENTATION
      -> FILE
      -> PACKAGE
      -> MODULE
      -> LIBRARY
      -> PROJECT
      -> REPOSITORY

Producer-supplied parent fingerprints therefore cannot become canonical merely because a worker
emitted them.

## Ordered versus unordered roles

Normalization must preserve observable order.

Ordered roles currently include:

- ATOM: statement/expression execution order;
- FIELD: Java field/initializer declaration order;
- ORDERED:*: explicit future ordered semantic roles.

Unordered membership includes:

- methods in a type;
- files in a package;
- types/doc nodes in a file when their role distinguishes them;
- packages/modules/libraries/projects.

For unordered roles M3IndexDB assigns canonical child order from role + child semantic identity.
For ordered roles producer ordinal is retained and conflicting ordinals fail closed.

This allows independent FILE workers to emit local ordinals without making repository fan-in
nondeterministic.

## Physical storage

M3IndexDB stores payloads once by SHA-256:

    blobs/<content-sha256>.blob
    refs/<name-sha256>.ref

Multiple logical artifact names pointing to identical semantic bytes reuse one blob.

The semantic payload itself uses:

- one sorted deduplicated UTF-8 dictionary;
- primitive node ordinals;
- primitive edge ordinals;
- binary 32-byte SHA-256 values, not 64-character duplicated strings;
- 64-bit structural/logic/SimHash lanes.

No second database copy is required.

## OpenRewrite responsibilities

M3SemanticIndexRecipe is FILE-local and non-mutating.

It extracts:

- atom candidates;
- fields;
- methods;
- interfaces/implementations;
- documentation;
- file/package/module/library/project/repository scope identities;
- exact/structural/logic/SimHash leaf signals;
- ordered participant edges.

It may fan out independently across files.

M3TypeRelationRecipe is the explicit MODULE-scope promotion. It scans attributed source types and
adds IMPLEMENTS/EXTENDS edges only when both source and target types are present in the scanned
module. External or unresolved targets are not guessed.

M3SemanticIndexM3DbBridgeRecipe is the MULTI_MODULE fan-in recipe. It runs FILE extraction plus
MODULE relationship resolution, reads their DataTables, asks M3IndexDB to normalize/recompose them,
and persists the canonical content-addressed artifact.

The resolver and bridge are not allowed to become second hash or binary-format owners.

## Scope expansion

The current FILE pass records what is safely knowable inside one file.

Future passes must promote authority only when necessary:

    FILE
      -> VISIBILITY
      -> PACKAGE
      -> MODULE
      -> MULTI_MODULE
      -> LIBRARY_API

Examples:

- private method atomization: FILE;
- accessibility change: VISIBILITY;
- package-private participant reconciliation: PACKAGE;
- implements/extends resolution across source files: MODULE (implemented by M3TypeRelationRecipe);
- reactor-wide framework/pattern graph: MULTI_MODULE;
- exported public contract change: LIBRARY_API.

Do not give a FILE recipe module-wide visibility merely to make implementation easier.

## Proof

The retained semantic-index unit requires:

    OpenRewrite recipe
    + recipe JUnit tests
    + M3IndexDB JUnit tests
    + declared scope
    + contract mode
    + exact commit
    + coverage gate
    + verification receipt

The current Maven gates require at least 99% line and 99% branch coverage for the M3 scope/atom/index
tooling kernel and for M3IndexDB.

No recipe or semantic DB capability may be called VERIFIED until the exact commit actually passes
the configured executable verification.
