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
import jdk.internal.m3.collections.TokenInterner;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AbstractSelfContainedDb - Base implementation for self-contained databases.
 *
 * <p>Architecture: One UUID = One Indexed DB File.
 * <pre>
 * ctx/
 * ├─ 9f3a1b2c.db    ← Self-contained, indexed, portable
 * ├─ b821e3f4.db    ← Self-contained, indexed, portable
 * └─ c012d4e5.db    ← Self-contained, indexed, portable
 * </pre>
 *
 * <p>Benefits:
 * <ul>
 *   <li><b>Isolation</b> - No cross-context corruption</li>
 *   <li><b>Fast GC</b> - Delete file = delete context</li>
 *   <li><b>Portable</b> - Copy file = copy context</li>
 *   <li><b>No locks</b> - Each context owns its DB</li>
 *   <li><b>Fast queries</b> - Indexes per context</li>
 * </ul>
 *
 * <h2>Template Method Pattern</h2>
 * <p>Subclasses must implement:
 * <ul>
 *   <li>{@link #doAppend(String, String)} - Append event to storage</li>
 *   <li>{@link #doQueryRecent(int)} - Query recent events</li>
 *   <li>{@link #doLatestSnapshot()} - Get latest snapshot</li>
 *   <li>{@link #doWriteSnapshot(String)} - Write snapshot</li>
 *   <li>{@link #doEventCount()} - Count events</li>
 * </ul>
 *
 * @since ContextOS 1.1
 */
/**
 * AbstractSelfContainedDb.
 * @author Synexia
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/memory/AbstractSelfContainedDb.java}
 * blob {@code 0a341664acc305465f61d5e3d49d32c0263a374c} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractSelfContainedDb implements ISelfContainedDb {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AbstractSelfContainedDb.class);


    private final UUID uuid;
    private final Path dbPath;

    /**
     * Create a self-contained database.
     *
     * @param uuid the context UUID
     * @param dbPath the path to the .db file
     */
    protected AbstractSelfContainedDb(UUID uuid, Path dbPath) {
        this.uuid = uuid;
        this.dbPath = dbPath;
    }

    @Override
    public UUID uuid() {
        return uuid;
    }

    @Override
    public Path dbPath() {
        return dbPath;
    }

    @Override
    public boolean exists() {
        return java.nio.file.Files.exists(dbPath);
    }

    @Override
    public void append(String eventType, String payload) {
        doAppend(eventType, payload);
    }

    @Override
    public List<String> queryRecent(int limit) {
        return doQueryRecent(limit);
    }

    @Override
    public List<String> queryByType(String eventType, int limit) {
        return doQueryByType(eventType, limit);
    }

    @Override
    public Optional<String> latestSnapshot() {
        return doLatestSnapshot();
    }

    @Override
    public void writeSnapshot(String snapshotJson) {
        doWriteSnapshot(snapshotJson);
    }

    @Override
    public int eventCount() {
        return doEventCount();
    }

    @Override
    public void exportTo(Path targetPath) {
        doExportTo(targetPath);
    }

    @Override
    public void compact() {
        doCompact();
    }

    @Override
    public boolean delete() {
        return doDelete();
    }

    // --- Abstract Template Methods (Must Implement) ---

    /**
     * Append event to storage.
     *
     * @param eventType the event type
     * @param payload the event payload
     */
    protected abstract void doAppend(String eventType, String payload);

    /**
     * Query recent events.
     *
     * @param limit max events to return
     * @return events in chronological order
     */
    protected abstract List<String> doQueryRecent(int limit);

    /**
     * Get latest snapshot.
     *
     * @return latest snapshot JSON if exists
     */
    protected abstract Optional<String> doLatestSnapshot();

    /**
     * Write snapshot.
     *
     * @param snapshotJson the snapshot JSON
     */
    protected abstract void doWriteSnapshot(String snapshotJson);

    /**
     * Count events.
     *
     * @return event count
     */
    protected abstract int doEventCount();

    // --- Optional Template Methods (Override for Custom Behavior) ---

    /**
     * Query events by type.
     *
     * @param eventType the event type to filter
     * @param limit max events to return
     * @return matching events
     */
    protected List<String> doQueryByType(String eventType, int limit) {
        // Default: filter from all events
        return queryRecent(Integer.MAX_VALUE).stream()
                .filter(e -> e.contains(TokenInterner.getInstance().intern("\"event_type\":\"") + eventType + TokenInterner.getInstance().intern("\"")))
                .limit(limit)
                .toList();
    }

    /**
     * Export database to target path.
     *
     * @param targetPath the target path
     */
    protected void doExportTo(Path targetPath) {
        try {
            java.nio.file.Files.copy(dbPath, targetPath);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to export database", e);
        }
    }

    /** Compact the database (VACUUM). */
    protected void doCompact() {
        // Default: no-op, subclasses implement SQLite VACUUM
    }

    /**
     * Delete the database file.
     *
     * @return true if deleted
     */
    protected boolean doDelete() {
        try {
            return java.nio.file.Files.deleteIfExists(dbPath);
        } catch (java.io.IOException e) {
            return false;
        }
    }
}
