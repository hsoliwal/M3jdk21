// SPDX-License-Identifier: Apache-2.0

package jdk.internal.m3.paradigm.context;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;


/** Contract regressions for the independently admitted COP literal repairs. */
class ContextBoundedSyntaxContractTest {
    @Test
    void quotedJsonValuesAndKeysRetainOnlyShape() {
        assertEquals("{\"*\":\"*\",\"*\":#}",
                ToStringShape.shape("{\"name\":\"Alice\",\"count\":12}"));
        assertEquals(ToStringShape.shape("{\"name\":\"Alice\",\"count\":12}"),
                ToStringShape.shape("{\"other\":\"Bob\",\"count\":39}"));
    }

    @Test
    void escapedQuotesAndBackslashesStayInsideQuotedValues() {
        assertEquals("\"*\"", ToStringShape.shape("\"a\\\"b\\\\c\""));
        assertEquals("{\"*\":\"*\"}", ToStringShape.shape("{\"a\\\"b\":\"c\\\\d\"}"));
    }

    @Test
    void numbersIdentifiersAndWhitespaceAreNormalized() {
        assertEquals("@[@=#, @=#]", ToStringShape.shape("Thing[alpha=-1.25e+3, beta=0]"));
        assertEquals("@ @", ToStringShape.shape("  hello\t\nworld  "));
        assertEquals("", ToStringShape.shape(""));
    }

    @Test
    void truncationAndFailureBoundariesArePreserved() {
        assertEquals("", ToStringShape.sample("private", 0));
        assertEquals("@", ToStringShape.sample("private", 3));
        Object broken = new Object() {
            @Override
            public String toString() {
                throw new IllegalStateException("private details");
            }
        };
        assertEquals("<toString-failed:java.lang.IllegalStateException>", ToStringShape.sample(broken, 1));
        assertThrows(IllegalArgumentException.class, () -> ToStringShape.sample("x", -1));
        assertThrows(NullPointerException.class, () -> ToStringShape.sample(null, 1));
        assertThrows(NullPointerException.class, () -> ToStringShape.shape(null));
    }

    @Test
    void structuralPunctuationIsNotCollapsedIntoIdentity() {
        assertNotEquals(ToStringShape.shape("[1,2]"), ToStringShape.shape("{1:2}"));
        assertEquals("[#,#]", ToStringShape.shape("[1,2]"));
    }

    @Test
    void allEightPersistedMetadataKeysAreDistinctAndUnchanged() {
        List<String> keys = List.of(ISelfContainedDb.META_PARENT_ID, ISelfContainedDb.META_COMPONENT,
                ISelfContainedDb.META_CREATED_AT, ISelfContainedDb.META_SCHEMA_VERSION,
                ISelfContainedDb.META_CONTEXT_UID, ISelfContainedDb.META_CONTEXT_ID,
                ISelfContainedDb.META_CONTEXT_CLASS, ISelfContainedDb.META_UPDATED_AT);
        assertEquals(List.of("parent_id", "component", "created_at", "schema_version", "context_uid",
                "context_id", "context_class", "updated_at"), keys);
        assertEquals(8L, keys.stream().distinct().count());
    }

    @Test
    void persistenceSchemaKeepsItsTablesAndRevisionIndexes() {
        List.of("events", "snapshots", "components", "metadata").forEach(table -> {
            assertTrue(ISelfContainedDb.SCHEMA.contains("CREATE TABLE IF NOT EXISTS " + table + " ("));
        });
        List.of("idx_events_created_seq", "idx_events_type_created_seq", "idx_events_revision",
                "idx_snapshots_revision_version", "idx_snapshots_hash", "idx_components_uid").forEach(index -> {
                    assertTrue(ISelfContainedDb.SCHEMA.contains("CREATE INDEX IF NOT EXISTS " + index));
                });
    }
}
