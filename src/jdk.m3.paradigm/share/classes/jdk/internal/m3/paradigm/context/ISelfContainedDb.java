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

import jdk.internal.m3.paradigm.annotation.ISpec;
import jdk.internal.m3.paradigm.annotation.Model;
import jdk.internal.m3.paradigm.annotation.Stability;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Self-Contained Database - One UUID = One Indexed DB File.
 *
 * <h2>Architecture</h2>
 * <pre>
 * ctx/
 * ├─ 9f3a1b2c.db    ← Self-contained, indexed, portable
 * ├─ b821e3f4.db    ← Self-contained, indexed, portable
 * └─ c012d4e5.db    ← Self-contained, indexed, portable
 * </pre>
 *
 * <h2>Each UUID.db Contains</h2>
 * <pre>
 * ┌─────────────────────────────────────────┐
 * │  UUID.db (Self-Contained)               │
 * ├─────────────────────────────────────────┤
 * │  TABLE events      + INDEX (created_at) │
 * │  TABLE snapshots   + INDEX (version)    │
 * │  TABLE components  + INDEX (type)       │
 * │  TABLE metadata    (parent_id, etc.)    │
 * └─────────────────────────────────────────┘
 * </pre>
 *
 * <h2>Benefits</h2>
 * <ul>
 *   <li><b>Isolation</b> - No cross-context corruption</li>
 *   <li><b>Fast GC</b> - Delete file = delete context</li>
 *   <li><b>Portable</b> - Copy file = copy context</li>
 *   <li><b>No locks</b> - Each context owns its DB</li>
 *   <li><b>Fast queries</b> - Indexes per context</li>
 *   <li><b>Backup</b> - Just copy .db files</li>
 * </ul>
 *
 * @since 1.1.0 - ContextOS Integration
 */
@ISpec(
    value = "Self-Contained Database - one UUID = one indexed DB file",
    constraints = {
        "One context = one SQLite file (UUID.db)",
        "Contains tables: events, snapshots, components, metadata",
        "Each table has appropriate indexes",
        "Benefits: isolation, fast GC, portable, no locks"
    },
    invariants = {
        "uuid() returns context UUID (filename without .db)",
        "path() returns full path to .db file",
        "Delete file = delete context (natural GC)"
    },
    examples = {
        "ISelfContainedDb.open(uuid)",
        "db.uuid()",
        "Files.delete(db.path())"
    },
    model = Model.ModelType.SONNET,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/memory/ISelfContainedDb.java}
 * blob {@code d3545f461bf0648268f734127daaa845428f43df} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface ISelfContainedDb {

    // --- IDENTITY ---

    /** Get context UUID (filename without .db). */
    UUID uuid();

    /** Get DB file path. */
    Path dbPath();

    /** Check if DB file exists. */
    boolean exists();

    // --- INDEXED OPERATIONS (FAST) ---

    /** Append event - O(1) amortized. */
    void append(String eventType, String payload);

    /**
     * Query recent events - O(log n) with index.
     *
     * @param limit max events (bloom window)
     * @return events in chronological order
     */
    List<String> queryRecent(int limit);

    /** Query by event type - O(log n) with index. */
    List<String> queryByType(String eventType, int limit);

    /** Get latest snapshot - O(1) with index. */
    java.util.Optional<String> latestSnapshot();

    /** Write snapshot - O(1). */
    void writeSnapshot(String snapshotJson);

    /** Count events - O(1) with SQLite optimization. */
    int eventCount();

    // --- SELF-CONTAINED OPERATIONS ---

    /**
     * Export context (copy DB file).
     *
     * @param targetPath destination path
     */
    void exportTo(Path targetPath);

    /**
     * Import context (copy DB file).
     *
     * @param sourcePath source .db file
     * @return new self-contained DB
     */
    static ISelfContainedDb importFrom(Path sourcePath, Path targetDir) {
        // Copy file, return wrapper
        throw new UnsupportedOperationException("Implement in concrete class");
    }

    /** Compact database (VACUUM). */
    void compact();

    /**
     * Delete database file.
     *
     * @return true if deleted
     */
    boolean delete();

    // --- SCHEMA ---

    /** Standard schema for self-contained DB. */
    String SCHEMA = """
        -- EVENTS (append-only log)
        CREATE TABLE IF NOT EXISTS events (
            seq        INTEGER PRIMARY KEY AUTOINCREMENT,
            event_type TEXT NOT NULL,
            revision   INTEGER NOT NULL DEFAULT 0,
            payload    TEXT NOT NULL,
            created_at INTEGER NOT NULL
        );

        -- SNAPSHOTS (content-deduplicated mementos)
        CREATE TABLE IF NOT EXISTS snapshots (
            version      INTEGER PRIMARY KEY AUTOINCREMENT,
            revision     INTEGER NOT NULL DEFAULT 0,
            payload      TEXT NOT NULL,
            payload_hash TEXT NOT NULL DEFAULT '',
            created_at   INTEGER NOT NULL
        );

        -- COMPONENT POINTERS (UID, never eager object graphs)
        CREATE TABLE IF NOT EXISTS components (
            component_key TEXT PRIMARY KEY,
            context_uid   TEXT NOT NULL,
            context_id    TEXT NOT NULL,
            updated_at    INTEGER NOT NULL
        );

        -- METADATA (self-describing)
        CREATE TABLE IF NOT EXISTS metadata (
            key   TEXT PRIMARY KEY,
            value TEXT
        );

        -- INDEXES (fast UID-local queries)
        CREATE INDEX IF NOT EXISTS idx_events_created_seq
            ON events(created_at DESC, seq DESC);
        CREATE INDEX IF NOT EXISTS idx_events_type_created_seq
            ON events(event_type, created_at DESC, seq DESC);
        CREATE INDEX IF NOT EXISTS idx_events_revision
            ON events(revision DESC, seq DESC);
        CREATE INDEX IF NOT EXISTS idx_snapshots_revision_version
            ON snapshots(revision DESC, version DESC);
        CREATE INDEX IF NOT EXISTS idx_snapshots_hash
            ON snapshots(payload_hash);
        CREATE INDEX IF NOT EXISTS idx_components_uid
            ON components(context_uid);
        """;

    /** Metadata keys. */
    String META_PARENT_ID = "parent_id";
    String META_COMPONENT = "component";
    String META_CREATED_AT = "created_at";
    String META_SCHEMA_VERSION = "schema_version";
    String META_CONTEXT_UID = "context_uid";
    String META_CONTEXT_ID = "context_id";
    String META_CONTEXT_CLASS = "context_class";
    String META_UPDATED_AT = "updated_at";
}
