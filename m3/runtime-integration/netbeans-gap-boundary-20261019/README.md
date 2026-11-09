<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# NetBeans gap-boundary convergence

Canonical recipe: com.synexia.m3.NetBeansGapBoundary20261019, owned in
hsoliwal/com.synexia at synexia-openrewrite-recipes/recipe-crates/netbeans-gap-boundary-20261019.
See SYNEXIA_HANDOFF.json for the exact source revision and output hashes.

This refines the existing javax.swing.text.GapContent.findMarkAdjustIndex owner
inside java.desktop. A lower-bound search replaces the backward duplicate scan;
findSortIndex and its Position identity selection remain unchanged. No new API,
retained field, storage owner, Synexia runtime dependency or JNI symbol is added.

Apache NetBeans GapList/OffsetGapList are used unchanged as offline proof sources.
They already implement the same gap/offset concepts, and their first-equal lookup
has the same linear duplicate scan. The target retains its OpenJDK implementation
and license; no Apache donor body is copied into GPL source. Full donor choices,
rejected fits and deferred lexer/flyweight consumers are in the catalogue and
canonical CONTRACT.md. This is a scoped implementation, not full NetBeans absorption.

Qualified scope and exact commands/logs accompany the canonical recipe handoff.
Full source-built image, full jtreg/tier/platform/JCK/reactor and global ordered-phase
promotion remain pending. No family-completion or native acceleration is claimed.
