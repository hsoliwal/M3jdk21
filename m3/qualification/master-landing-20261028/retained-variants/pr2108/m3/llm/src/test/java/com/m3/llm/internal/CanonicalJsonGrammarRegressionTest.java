// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// Derivation: AUTHORED_TWO_DEFECT_REGRESSION; see SOURCE_PROVENANCE.tsv.
package com.m3.llm.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CanonicalJsonGrammarRegressionTest {
    @Test
    void rejectsDuplicateKeysWhoseFirstValueIsNull() {
        List.of("{\"a\":null,\"a\":1}", "{\"a\":null,\"a\":null}",
                "{\"outer\":{\"a\":null,\"a\":false}}", "[{\"a\":null,\"a\":0}]",
                "{\"a\":null,\"" + "\\" + "u0061\":0}")
                .forEach(input -> assertThrows(M3ContractException.class, () -> CanonicalJson.parse(input)));
    }

    @Test
    void rejectsEveryRawControlInBothStringValuesAndObjectKeys() {
        IntStream.range(0, 32).forEach(code -> {
            char control = (char) code;
            assertThrows(M3ContractException.class,
                    () -> CanonicalJson.parse("{\"a\":\"before" + control + "after\"}"));
            assertThrows(M3ContractException.class,
                    () -> CanonicalJson.parse("{\"before" + control + "after\":0}"));
        });
    }

    @Test
    void preservesEscapedControlsAndDistinctNullMembers() {
        String escaped = "{\"first\":null,\"second\":\"a\\nb\\tc\\r\\bd\\f\"}";
        Object parsed = CanonicalJson.parse(escaped);
        assertEquals(escaped, CanonicalJson.string(parsed));
        assertTrue(((Map<?, ?>) parsed).containsKey("first"));
        assertEquals("a\nb\tc\r\bd\f", ((Map<?, ?>) parsed).get("second"));
        assertEquals(String.valueOf((char) 0), CanonicalJson.parse("\"" + "\\" + "u0000\""));
    }

    @Test
    void retainsDuplicateRefusalForNonNullFirstValues() {
        List.of("{\"a\":0,\"a\":1}", "{\"a\":true,\"a\":false}",
                "{\"a\":\"first\",\"a\":null}", "{\"a\":[],\"a\":{}}")
                .forEach(input -> assertThrows(M3ContractException.class, () -> CanonicalJson.parse(input)));
    }
}
