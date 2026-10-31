<!-- SPDX-License-Identifier: Apache-2.0 -->
# Eclipse reveals missing Java capabilities: catalogue-driven M3JDK21 gap pass

**M3JDK21 is the integrated JDK21/HotSpot/Java/JNI SDK, not an M3+JDK overlay.** Synexia remains the convergence, recipe, comparison and provenance workspace. This census creates no Eclipse runtime dependency, proxy hierarchy, second model or duplicate Java standard library. Public Java APIs keep Java names; `M3*` implementations may be internal where the JDK contract can be sealed.

This is a **dated worklist, not a full source census or an acceptance declaration**. The 109 rows in `M3_ECLIPSE_CAPABILITY_MATRIX.tsv` include:
- the **12 inspected Claude-marked forks** from the 2026-10-20 site inventory (3,733 source locations; 3,235 UI-thread candidates);
- additional Eclipse/JDK-consumer families (RAP/RWT, m2e, WTP, LSP4E, ECF, NatTable, Mylyn, Swing/JavaFX), explicitly `CENSUS_PENDING`;
- existing M3JDK21 owners from a bounded current-master directory/source inspection;
- distinct `REUSE_PRESENT`, `PARTIAL_OWNER`, `OWNER_UNVERIFIED` and `RECEIVER_DRAFT` states. **None of these statuses means full implementation or compatibility acceptance.**

## Key unresolved capability families

1. **Core Runtime:** adapters, dynamic extension registries, safe runners, content types, status/error handling, preferences, resource rules, monitor composition, scheduled and keyed work.
2. **EFS/Resources:** provider identities, logical filesystem watchers, workspace transactions, linked files, external modifications, marker/resource deltas, file generation fences, class/build impact, snapshots and retention.
3. **EMF:** actual original-owner list observation, notification ordering, typed feature storage, containment/opposites, proxy resolution, cross-resource links, transactions, rollback, serialization and dynamic Ecore authority.
4. **JDT/Java tools:** working copies, exact compilation units and JDT bindings, AST relations, source/bytecode origin, annotation processing, incremental diagnostics, refactoring/text edits, symbol search/navigation, content assist and launched debug views.
5. **SWT/JFace/Workbench:** native handle generations, resident trees/tables/lists, styled text, IME/accessibility/DPI, no UI-thread work, frame coalescing, viewport-only paint/damage, provider-side filtering/sorting, lazy facades, one Problems View-derived model, all other views.
6. **PDE/Equinox/p2/GEF/EGit:** model-edit deltas instead of form rebuilds, OSGi dynamic resolver/registry/activation, p2 SAT/version constraints and signed artifact plans, graph spatial clipping and git DAG/diff/staging composition.
7. **Beyond marked forks:** additional candidate platforms require exact repository commit and source-root census. Do not report an unscanned Eclipse project as fully mapped.

## Reference pattern: Problems View becomes a JVM capability, not an Eclipse-only library

```
source model/resource/AST/marker event (unchanged public contracts)
  → original M3JDK21 authoritative String/FS/AST/indexed collection owner
  → one generation-fenced primitive delta: IDs + offsets + masks + ranges
  → existing M3ListDiff/M3Patch where key-diff is semantically valid
  → M3WindowPlan changed range / changed-row projection (this leaf)
  → UI-thread O(visible change) native commit, invisible rows remain cold
  → CPU layout/control; qualified GPU only for batch raster/geometry
```

**One model** means each domain has one authoritative owner, not that all distinct systems share one physical store. Do not turn file identity, Java symbol identity, native handle identity or semantic AST identity into interchangeable integer IDs. Generations, owners, resource lifetimes, external providers, callback behavior and public contracts remain sealed.

### This concrete Java 21 leaf

Existing `com.m3.collections.M3WindowPlan` gains three additive public methods while preserving every incumbent method:
- `changedCount(row,count)`: O(1) length of changed logical range intersecting the current committed window.
- `changedEntry(row,count)`: O(1) first resident native entry, or -1 if offscreen.
- `projectChangedRows(changedRows,from,length,nativeEntries,targetOffset)`: preflighted O(changed input rows) projection into native row indexes, stable duplicates, no output mutation on invalid geometry; no per-row object; scratch only if arrays alias.

These methods do **not** implement a viewer/OS paint callback or add JNI symbols. The original `M3WindowPlan` source preserves all original public methods; `javap -public` compared baseline and candidate (all original declarations retained). The exact candidate/test Java files passed local Java21 warnings-as-errors and **349,502 differential assertions** over 40,000 randomized scenarios, plus alias/overflow/fence/refusal cases. The original owner's other tests, Maven/OpenRewrite SDK, native JDK image, UI pixel/event differentials and full provider compatibility remain independently required.

### Mandatory recipe/promotion sequence

`PRESERVE → SOURCE-PIN → CATALOGUE → ATOM/CONTRACT → SOURCE-SEALED MAVEN/OPENREWRITE RECIPE → ACTUAL RECIPE.RUN → GENERATED POSTIMAGE → FIXED POINT → ORIGINAL AND DIFFERENTIAL TESTS → JNI/ABI/GUI/PERF → PR REVIEW → PROMOTION`.

Use the existing canonical `m3-ast-build-reverse-20261017` recipe crate and **unchanged** `M3HashPinnedJavaSnapshotRecipe`; do not add a second executor, mapper, catalogue engine or production-time Synexia dependency. Current candidate source files are precommitted reviewed templates; local Java compilation does **not** constitute actual Maven/OpenRewrite execution.

The tagged per-fork markers are historical per-line source candidates and can be stale after intervening PRs: reconcile every target against its **current original owner** before emission. Record `NOT_RUN` for all unavailable tests, retain source and license notices, never rebase Synexia develop and never merge unqualified product PRs.

Primary evidence: [Claude site inventory](https://github.com/hsoliwal/eclipse.platform.swt/blob/m3/viewport-integration-20261016/docs/m3/correction-sites/README.md), [model-listener review](https://github.com/hsoliwal/eclipse.platform.swt/blob/m3/viewport-integration-20261016/docs/m3/correction-sites/FINAL_PASS_MODEL_LISTENER.tsv), [UI-thread review](https://github.com/hsoliwal/eclipse.platform.swt/blob/m3/viewport-integration-20261016/docs/m3/ui-thread-sites/README.md), [Problems View handover](https://github.com/hsoliwal/eclipse.platform.ui/blob/master/docs/m3/markers-view/HANDOVER.md), [M3JDK21 naming law](https://github.com/hsoliwal/M3jdk21/blob/master/m3/docs/M3JDK21_PORTING_INVARIANT.md).
