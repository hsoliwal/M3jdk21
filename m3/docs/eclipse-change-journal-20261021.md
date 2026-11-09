<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Eclipse capability gap: versioned Java change journal (2026-10-21)

Status: additive M3JDK21 java.base candidate, not an Eclipse/EMF replacement or installed-image qualification.

## Inventory and ownership

| Eclipse source / capability | Existing Java/M3 owner | Qualification gate |
| --- | --- | --- |
| EMF BasicEList (EPL-2.0, blob 56b2ffe918dab4e47b31b8dcaa275333f72dc438) | java.util.List, M3ArrayList, existing M3ObservableList in M3JDK21 PR #1725 | Unique/inverse/containment, serialization and collection behavior |
| EMF Notification (EPL-2.0, blob 5187221e44dac36a4abb9cbf1f19d1c627539306) | NEW java.util.ChangeJournal versioned replay | Notifier, feature ID, notifications and adapters remain separate |
| EMF EcoreEList (EPL-2.0, blob 6655d9b282add7466785ef694e8d84baefba4e43) | Existing M3 array/mask/index owners | Proxy resolution, containment, inverse pairs |
| EFS and Eclipse Resources | Existing m3/fs, m3/file, NIO and delta PRs #1573, #1581, #1608, #1611 and #1637 | Provider identity, file refresh, snapshots, workspace contracts |
| Jobs and SubMonitor | Existing java.util.concurrent ProgressMonitor, ProgressJob and ScheduledExecutorService | Rule conflicts, cancellation, progress ownership |
| SWT/JFace/Nebula | Existing M3ListModel and indexed viewport owners | UI thread, disposal, selection/focus, accessibility and native JNI |
| JDT | Existing M3 AST/compiler and Synexia OpenRewrite recipes | AST bindings, parsing, refactoring and build parity |

No EPL implementation source is copied. Source code under java.base remains GPL-2.0-only WITH Classpath-exception-2.0; recipe and documentation have first-party Apache-2.0 custody. This is NOT M3 plus a separate JDK: M3JDK21 is the Java product.

## Java ChangeJournal contract

A bounded ring stores kind in byte[], coordinates/counts in int[] and optional old/new values in Object[]. No retained per-event Entry/Node/Notification objects. The append method returns a monotonic version; snapshot(cursor) projects a stable Batch and rejects negative, future and overwritten cursors. RESET indicates that a consumer needs a new full model snapshot; it is not a fine-grained EMF notification. Batch callbacks run without holding the journal monitor. The two reference lanes retain history values until their slots are overwritten or the journal is released: configure capacity accordingly.

The existing M3ObservableList in PR #1725 owns synchronous callbacks. This change only adds a complementary replay mechanism and must not duplicate that list. A follow-on recipe may integrate it as an opt-in after preserving the current list behavior. The owner must coordinate its state change and the journal append atomically; ChangeJournal synchronization alone cannot guarantee atomicity for another owner's state.

## Recipe and proof gates

Canonical Synexia recipe: com.synexia.rewrite.M3EclipseChangeJournal20261021, reusing M3HashPinnedJavaSnapshotRecipe with crate eclipse-change-journal-20261021. Capture ABSENT-before paths, exact SHA-256 postimage seals, fixed-point/deterministic repeat, refusal of occupied/conflicting targets, JUnit recipe replay, and Java 21 compilation. The first-party target sources and jtreg test form one bounded reviewer packet. No new rewrite engine or hidden runtime layer.

Executed locally on a stock JDK 21 patch-module: strict -Xlint:all -Werror javac and ChangeJournalTest main. Still pending on the exact pull-request head: canonical Maven/OpenRewrite recipe tests, complete M3JDK21 build, installed-image jtreg/JCK, EMF behavioral differentials, OSGi/SWT lifecycle qualification, GC/native memory/throughput measurements and cross-platform ABI.

## Next receiving order

1. Qualify this canonical recipe, direct java.base source and target jtreg.
2. Reuse the existing M3ObservableList owner; add opt-in replay only after tests.
3. Adapt actual EMF EList/NotifyingList semantics in the hsoliwal/org.eclipse.emf fork without replacing its public API.
4. Apply source-sealed, file-local, recipe-driven transformations to EFS/Resources, Jobs, SWT/JFace, JDT and all other forks, respecting each contract.
