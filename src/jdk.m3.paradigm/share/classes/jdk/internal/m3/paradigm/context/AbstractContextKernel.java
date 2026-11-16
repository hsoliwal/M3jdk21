/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.paradigm.context;

import jdk.internal.m3.collections.PlaceholderLogger;

import jdk.internal.m3.paradigm.context.IContextMemento.ContextId;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AbstractContextKernel - Base implementation for context kernel (Authority, Not Storage).
 *
 * <p>Key Invariants:
 * <ul>
 *   <li>Kernel does routing only</li>
 *   <li>Context DBs stay independent (one UUID = one .db file)</li>
 *   <li>Lazy load by UUID</li>
 *   <li><b>NO LOOPS</b> - parent chain must terminate at root</li>
 * </ul>
 *
 * <h2>Loop Prevention</h2>
 * <pre>
 * A → B → C → A  ← FORBIDDEN (cycle detected)
 * A → B → C      ← OK (terminates at root)
 *
 * wouldCreateLoop(childId, parentId):
 *   Walk up parent chain from parentId
 *   If we hit childId → loop would be created → REJECT
 * </pre>
 *
 * <h2>Template Method Pattern</h2>
 * <p>Subclasses must implement:
 * <ul>
 *   <li>{@link #doCreate(String)} - Create context node</li>
 *   <li>{@link #doFork(UUID, String)} - Fork child context</li>
 *   <li>{@link #doLoad(UUID)} - Load existing context</li>
 *   <li>{@link #doLink(UUID, UUID)} - Link parent/child</li>
 *   <li>{@link #doDelete(UUID)} - Delete context</li>
 * </ul>
 *
 * @since ContextOS 1.1
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/memory/AbstractContextKernel.java}
 * blob {@code 5b1c226085c772d8fb21f4dc5acda52851d407b6} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractContextKernel implements IContextKernel {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AbstractContextKernel.class);


    private final Path rootPath;

    /**
     * Create a context kernel with specified root path.
     *
     * @param rootPath the root storage path
     */
    protected AbstractContextKernel(Path rootPath) {
        this.rootPath = rootPath;
    }

    @Override
    public IContextNode create(String component) {
        return doCreate(component);
    }

    @Override
    public IContextNode fork(UUID parentId, String component) {
        return doFork(parentId, component);
    }

    @Override
    public Optional<IContextNode> load(UUID contextId) {
        return doLoad(contextId);
    }

    @Override
    public IContextNode getOrCreate(UUID contextId, String component) {
        return load(contextId).orElseGet(() -> {
            IContextNode node = doCreate(component);
            return node;
        });
    }

    @Override
    public void link(UUID childId, UUID newParentId) {
        if (newParentId != null && wouldCreateLoop(childId, newParentId)) {
            throw new IllegalArgumentException(
                    "Cannot link: would create cycle between " + childId + " and " + newParentId);
        }
        doLink(childId, newParentId);
    }

    @Override
    public boolean wouldCreateLoop(UUID childId, UUID parentId) {
        if (parentId == null) {
            return false; // Root cannot create loop
        }
        if (childId.equals(parentId)) {
            return true; // Direct self-reference
        }

        // Walk up parent chain from parentId
        UUID current = parentId;
        int maxDepth = 1000; // Prevent infinite loops in corrupted data
        int depth = 0;

        // NFL: parent chain traversal with depth bound — loop preserved per M3 §3
        while (current != null && depth < maxDepth) {
            Optional<IContextNode> node = load(current);
            if (node.isEmpty()) {
                break; // Reached non-existent node
            }

            ContextId id = node.get().id();
            UUID nodeParent = id.parent();

            if (childId.equals(nodeParent)) {
                return true; // Found childId in ancestor chain
            }

            current = nodeParent;
            depth++;
        }

        return false;
    }

    @Override
    public List<IContextNode> children(UUID parentId) {
        return doChildren(parentId);
    }

    @Override
    public Optional<IContextNode> parent(UUID contextId) {
        return load(contextId)
                .map(IContextNode::id)
                .map(ContextId::parent)
                .flatMap(this::load);
    }

    @Override
    public boolean delete(UUID contextId) {
        return doDelete(contextId);
    }

    @Override
    public int deleteTree(UUID contextId) {
        List<IContextNode> childNodes = children(contextId);
        int childCount = childNodes.stream()
                .mapToInt(child -> deleteTree(child.id().id()))
                .sum();
        return childCount + (doDelete(contextId) ? 1 : 0);
    }

    @Override
    public void close() {
        doClose();
    }

    @Override
    public Path root() {
        return rootPath;
    }

    // --- Abstract Template Methods (Must Implement) ---

    /**
     * Create a new root context.
     *
     * @param component the component name
     * @return new context node
     */
    protected abstract IContextNode doCreate(String component);

    /**
     * Create a child context (fork).
     *
     * @param parentId parent context UUID
     * @param component the component name
     * @return new child context node
     */
    protected abstract IContextNode doFork(UUID parentId, String component);

    /**
     * Load existing context by ID.
     *
     * @param contextId the context UUID
     * @return context node if exists
     */
    protected abstract Optional<IContextNode> doLoad(UUID contextId);

    /**
     * Link a context to a new parent.
     *
     * @param childId the context to re-parent
     * @param newParentId the new parent (null to make root)
     */
    protected abstract void doLink(UUID childId, UUID newParentId);

    /**
     * Get children of a context.
     *
     * @param parentId the parent context UUID
     * @return list of child nodes
     */
    protected abstract List<IContextNode> doChildren(UUID parentId);

    /**
     * Delete a context.
     *
     * @param contextId the context UUID
     * @return true if deleted
     */
    protected abstract boolean doDelete(UUID contextId);

    /** Close the kernel and release resources. */
    protected void doClose() {
        // Default: no-op
    }
}
