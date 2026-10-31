# M3ChangeJournal Java 21 receiving qualification — hold

Canonical reusable recipe: [Synexia PR #12823](https://github.com/hsoliwal/com.synexia/pull/12823), head commit `f103e6f4336861f5f1684e5a4baa0e73b590154c`. Its reusable engine is **the existing** `M3HashPinnedJavaSnapshotRecipe`; exact Java postimages are mirrored from its manifest. `SOURCE_SEALS.tsv` pins complete source identities. Source and tests are original first-party Apache-2.0 expression.

## Already executed
- Java 21 strict `javac --release 21 -Xlint:all -Werror`, isolated implementation/test with minimal stand-in classes exposing the already-inspected `M3LongArrayList` and `M3IntArrayList` methods: **PASS**.
- Java 21 main oracle: **M3_CHANGE_JOURNAL_PASS** for wraparound, byte states, full-width flags, overrun/pagination, invalid cursors, detached pages and 800 synchronized concurrent append operations. This is not an original M3 collection-owner or JUnit run.
- Remote canonical source templates and target source files are read back and compared directly against exact source templates. Those are content custody gates, not runtime proofs.

## Required gates (not run)
1. Actual Maven/OpenRewrite SDK `Recipe.run` with Java LST, generated code equality, second-run fixed point, drift refusal; document exact canonical head.
2. Compile complete original M3 collection owners under Java 21 and run JUnit `M3ChangeJournalTest`; the pre-existing `.github/workflows/m3-collections.yml` automatically covers `m3/collections/**` on PRs. No green CI result is assumed.
3. Test snapshot cursor saturation/rollover, large-cursor arithmetic, oversized capacity/OOME preflight and allocation costs before treating as production retention.
4. Source-built M3JDK21 installed module/JMOD and public API descriptor/admission checks. The current global receiving phase remains STRING, regardless of this optional collection candidate.
5. Actual Eclipse `IResourceDelta`, EMF notification and SWT/Workbench integrations must each have their own canonical serial per-file recipe, original-owner tests and separate PR.
6. Compare CPU/heap/native RSS vs original Eclipse event handling and an unjournaled M3 parent, on representative cold/warm workloads. No speedup claimed.

This journal does **not** own source payloads, storage locks, callbacks, EMF notifier policy, OSGi services or native JNI symbols. Its optional primitive sidecar can record owner-issued generation-bearing handles, but readers must re-observe the authoritative source when history is lost. Use category-specific legal donor decisions from canonical `CATALOGUE.tsv`.
