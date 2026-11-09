# M3JDK21 parser and filtered-view continuation

Canonical recipe owner: hsoliwal/com.synexia. Product owner: hsoliwal/M3jdk21.
Pinned target: 5cb893e1ab224236b33d41132a31f604d89a4e12.
This is a refinement of current product sources, not a second runtime layer.

## Frozen contracts and passes

1. Existing M3ASTIngress.eclipseJdt stays syntax-only. Add an explicit resolved
   projection using real JDT public binding/problem contracts. Store detached
   metadata in M3ASTIndex arrays; no retained JDT objects or per-node records.
   Existing methods, source identity, child order and eight structural lanes stay.
   Binding keys are meaningful only in the supplied JDT compilation environment;
   no cross-project cache, inferred binding or fabricated source correspondence.
2. Reject completed AST ranges that exclude already indexed descendants under
   unknown intermediate ranges; reject newly appended nodes outside known ancestors.
   This closes an invalid-input acceptance hole while preserving accepted valid trees.
3. Refine read-only filtered-view terminal atoms with direct cursor loops. Preserve
   iterator calls, predicate order, short circuiting, nulls, exceptions, live backing,
   sorting metadata and all public interfaces. Map.Entry remains a fresh API projection.
4. Restore the previously qualified TreeMap lookup/admission/entry-slot atoms in
   the newer current owners. Preserve compatibility facade callbacks, always create
   fresh caller-owned entries, retain lazy/key-only semantics and AVL arrays. Current
   master starts at 128/640; PR1442's different lineage starts at 630/640.
5. Reconnect existing strict SortedSet endpoint guards and read-only reversed view.
   Existing regression contracts identify three current baseline failures. This is
   a contract repair, not preservation of those erroneous observations.
6. Refine the existing JNI mask compactor to avoid fetching rejected row chunks.
   Keep every exported symbol, required-mask semantics, region bounds, state-drift
   guard, detached results, zero-mask behavior and Java reference implementation.
   Caller must keep arrays stable during selection as in the original contract.

## Qualification

Use actual Java21/JDT parse and binding resolution, diagnostics, recovery, ASTRewrite
and reparsing; immutable snapshots after source edits; Java21 grammar shapes and
code-looking literals; unchanged original cross-frontend tests; bounded random tree
range invariants; baseline/candidate callback traces for filtered sorted sets/maps;
full collection Maven tests; original 640 TreeMap observations; actual JNI parity
under -Xcheck:jni, C17 warnings-as-errors and exported symbol comparison; real
OpenRewrite SDK exact postimages, reverse input order, fixed point and drift refusals.

Full Eclipse/PDE workspace reconciliation, full Groovy/Kotlin semantic models,
Maven effective models, Jackson whole-module integration, source-built HotSpot/jtreg
and platform/performance claims require their own executed receipts. Preserve these
as open work, not as implied completion from this bounded qualification.
