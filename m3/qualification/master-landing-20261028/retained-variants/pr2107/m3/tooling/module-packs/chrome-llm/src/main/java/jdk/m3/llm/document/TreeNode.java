/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * Adapted for M3JDK21 from m3-flldcim/tools/m3-text-pipeline/src/main/java/jdk/internal/m3/flldcim/tools/TreeNode.java
 * Synexia revision: 2ab439480ecb0643615e85b42def954dbf3b5627.
 * Namespace mapping preserves algorithms and original schema/hash identities.
 */
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.document;

/** Addressable source/tree node. Candidate description labels are not authoritative facts. */
public record TreeNode(String id, String parent, String kind, int start, int end,
                       String label, String payloadHash) { }
