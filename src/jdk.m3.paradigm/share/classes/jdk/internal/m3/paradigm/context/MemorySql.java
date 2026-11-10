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

import java.util.List;

/**
 * SQLite schema shared by Java integration tests and the production journal.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/memory/MemorySql.java}
 * blob {@code d37b41342358466c79b27e1a38a48cebbaa7f810} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
final class MemorySql {
    static final int LEGACY_VERSION = 1;
    static final int VERSION = 2;

    /**
     * V2 stores every UUID-shaped identity as two INTEGER words.
     *
     * <p>SQLite INTEGER is signed, but its 64-bit two's-complement bit pattern is
     * preserved exactly by JDBC setLong/getLong. Treat these columns as unsigned
     * words at the semantic layer. No UUID text is stored in new databases.
     */
    static final List<String> SCHEMA = List.of(
            """
            CREATE TABLE cm_meta (
              singleton INTEGER PRIMARY KEY CHECK(singleton=1),
              schema_version INTEGER NOT NULL,
              uid_hi INTEGER NOT NULL,
              uid_lo INTEGER NOT NULL,
              head INTEGER NOT NULL CHECK(head>=0),
              checkpoint_interval INTEGER NOT NULL
                CHECK(checkpoint_interval BETWEEN 1 AND 128))
            """,
            """
            CREATE TABLE cm_revision (
              revision INTEGER PRIMARY KEY CHECK(revision>0),
              operation_hi INTEGER NOT NULL,
              operation_lo INTEGER NOT NULL,
              event_type TEXT NOT NULL CHECK(length(event_type) BETWEEN 1 AND 256),
              created_seconds INTEGER NOT NULL,
              created_nanos INTEGER NOT NULL
                CHECK(created_nanos BETWEEN 0 AND 999999999),
              request_digest TEXT NOT NULL,
              state_digest TEXT NOT NULL,
              origin_hi INTEGER,
              origin_lo INTEGER,
              origin_revision INTEGER NOT NULL,
              UNIQUE(operation_hi,operation_lo),
              CHECK((origin_hi IS NULL AND origin_lo IS NULL AND origin_revision=-1)
                OR (origin_hi IS NOT NULL AND origin_lo IS NOT NULL AND origin_revision>=0)))
            """,
            """
            CREATE TABLE cm_delta (
              revision INTEGER NOT NULL REFERENCES cm_revision(revision),
              cell_key TEXT NOT NULL,
              cell_value TEXT,
              removed INTEGER NOT NULL,
              PRIMARY KEY(revision,cell_key),
              CHECK((removed=1 AND cell_value IS NULL)
                OR (removed=0 AND cell_value IS NOT NULL)))
            """,
            """
            CREATE TABLE cm_checkpoint (
              revision INTEGER PRIMARY KEY REFERENCES cm_revision(revision),
              payload BLOB NOT NULL CHECK(length(payload)<=16777216))
            """,
            """
            CREATE TABLE cm_current (
              cell_key TEXT PRIMARY KEY NOT NULL,
              cell_value TEXT NOT NULL)
            """,
            """
            CREATE TABLE cm_word_state (
              revision INTEGER PRIMARY KEY REFERENCES cm_revision(revision),
              control INTEGER NOT NULL,
              signal_count INTEGER NOT NULL CHECK(signal_count BETWEEN 0 AND 65536),
              signal_words BLOB NOT NULL,
              CHECK(length(signal_words)=((signal_count+7)/8)*8))
            """,
            "CREATE INDEX cm_revision_kind ON cm_revision(event_type,revision)",
            "CREATE INDEX cm_revision_time ON cm_revision(created_seconds,created_nanos,revision)",
            "CREATE INDEX cm_delta_key ON cm_delta(cell_key,revision)",
            "CREATE INDEX cm_current_value ON cm_current(cell_value,cell_key)");

    private MemorySql() { }
}
