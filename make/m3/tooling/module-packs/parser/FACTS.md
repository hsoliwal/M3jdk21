<!-- Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 -->
# Shared syntax facts in M3JDK21

`M3FactsWriter` projects an existing detached `M3ASTIndex` into
`<module>/target/m3/facts.sqlite`. It does not read source, invoke a parser, print
an AST, or retain every index. The existing compiler/tree owner remains the
authority; SQLite is a derived, transactional query surface.

The schema identity is `M3_AST_FACTS_SQLITE_V1`. The inspected E48 references did
not supply a compatible table schema, so this writer does not claim E48 schema
equivalence. It owns four names in SQLite's main schema: `m3_ast_meta`,
`m3_ast_generations`, `m3_ast_files` and `m3_ast_nodes`. Other tables remain
independent. Modified or partial owned schemas, foreign triggers, temporary
shadows and temporary triggers targeting those names are refused before writes.

## Ownership and publication

The Path overload opens and closes its connection. A borrowed Connection must be
used exclusively for the duration of the call. An autocommit connection receives
one owned transaction. A caller transaction receives one savepoint and remains
uncommitted; `Result.committed()` is false. A failure before successful publication
attempts to roll back this generation, preserving earlier generations and
unrelated caller work. A rollback failure remains attached to the original
failure; the writer does not turn autocommit back on and thereby risk an implicit
commit. After a successful commit, restoring autocommit or closing an owned
connection can still fail. That failure is reported but cannot retract the
committed generation. Callers must distinguish a failed operation from proof
that no generation was published.

The Iterable overload borrows the input and does not close it. The Stream
overload takes ownership and closes it exactly once, including setup failure.
Successful close happens inside the transaction, before commit. Use this overload
for parallel M3FS traversal: a callback result can precede its traversal region's
final provider close, while an atomic generation must wait for both complete
exhaustion and successful close. A failed close is never retried.

## Identities and bounds

Source UTF-16 identity, optional raw-byte identity, optional exact-span identity
and structural identity are different columns with different framing. Original
name, literal and kind UTF-16 units have lossless BLOB lanes; ordinary SQL display
text is not the authority for unpaired surrogate units. Modifier bits and owner
indices remain detached primitive lanes. This is syntax evidence, not proof of
semantic equivalence or a semantic cache hit. The caller's optional configuration
seal must account for compiler/toolchain options, dependencies, processors and
rule versions if it is used as part of a broader semantic identity.

Limits bound admitted files and nodes, JDBC row count, node-batch text units,
per-file structural text work and operation-wide exact-span work. One index and
its optional flat structural hashes are retained at a time. Exact-span hashing
is disabled by default because nested ranges overlap. A single oversized row or
exceeded budget fails the transaction. These bounds exclude the caller's current
source/index, JDBC/SQLite caches, URI metadata, and provider-owned traversal or
archive state. They are not total-heap or throughput guarantees.

## Build and runtime

The existing `jdk.m3.parser` module has a static transitive `java.sql` edge. The
SQLite driver is supplied by the SDK/tooling runtime that chooses this feature;
there is no Synexia runtime dependency and no replacement native SQL engine.
The receiving tests use the real pinned Xerial SQLite JDBC 3.45.1.0 JNI driver,
including transaction/savepoint rollback, exact lane round trips and native
execution under `-Xcheck:jni`. Driver/module image qualification and a full
source-built M3JDK21 remain separate evidence gates.

The optional OpenRewrite provider visits the supplied real `J.CompilationUnit`.
It neither prints nor reparses it. Original OpenRewrite Maven artifacts remain
under the existing tool pack's classpath arrangement until its aggregate modular
closure is separately qualified. Unknown source ranges remain unknown.

Canonical per-file recipe resources, donor choices, failed candidates, receiving
hashes, and correction memory live in the owning Synexia one-pass recipe crate.
Each validated correction retains a regression guard and a lesson through the
existing `M3RecipeMemory` owner. Copyright and donor notices describe their actual
source expressions; upstream licenses remain intact.
