# Current JDK continuation status — 2026-10-06

This file is an overlay, not a rewrite of historical run receipts such as
`tasks/recipe-closure/TODO.tsv`.

Closed recipe gates are backed by later executed evidence. Product/runtime gates remain separate.
In particular, PlainText source custody closes the isolated OpenRewrite parser/type failure for exact
snapshot replay; it does not prove String/JNI product semantics.

Native image work remains environment-blocked by real Linux development headers. No configure
bypass or fake header is admitted.

Active backport work:
- PR #177: J485 Stream Gatherers candidate, recipe-first, Java21 proof pending.
- PR #179: J423 G1 Region Pinning corrected-lineage inventory, native materialization pending.
