<!-- SPDX-License-Identifier: Apache-2.0 -->
# #1335 array-only API entry facades — second chronological source wave

Original PR: https://github.com/hsoliwal/M3jdk21/pull/1335
Exact donor head: `c4b5e7098ef18bdee744f194c12bd0243bf550b2`.
Exact integration receiver before wave: `776236e1445fd6b4f7d55289906051543f419186`.

This wave **adds 17 missing first-party Git blob contents under the exact donor paths**. Their original tree modes `100644` were read from the GitHub Git Data tree, including the complete 14-file Maven/OpenRewrite recipe subtree, workflow, public `M3ApiEntryFacades.java`, and original Java proof class. No existing Java class was replaced and no prior staged #1330 file was removed.

**HOLD: `M3LFUCache.java`** has existing receiver blob `dc1dc5167efb5ed859c1aa3c41a07d6a5332c4cb` versus donor `afd11c03a56cf782e55ab1bf71ed4f59a7dd3aae`. The existing LFU storage/contract owner remains untouched. Reconcile semantic atoms through the original `M3PhysicalHunkMergeRecipe`, `M3HunkResolver` and source-sealed Maven/OpenRewrite recipe with unchanged public/observable behavior. Test the facade against that resolved class before promotion.

**PARTIAL CONTENT STAGING; NOT AN ORIGINAL DONOR PR MERGE.** This commit has only the previous integration commit as parent; never attach donor head ancestry until all 18 path changes are present and behavior/ABI gates pass. This candidate is not an M3 facade layered over the JDK; M3JDK21 is the integrated Java/HotSpot/JNI SDK. Open PRs #1330 and #1335 remain unmodified and unclosed. Full original Maven/JUnit/Java21/HotSpot/JNI/ABI/CI qualification is **HOLD/NOT_RUN**.
