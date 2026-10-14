// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class M3ItemReferenceScanTest {
    private static byte[] bytes(String name) throws IOException {
        try (InputStream in = M3ItemReferenceScanTest.class.getResourceAsStream("/" + name + ".class")) {
            return in.readAllBytes();
        }
    }

    @Test
    void classifiesEveryEscapeKind() throws IOException {
        String[] names = {"com/m3/compiletime/fixture/FakeItem", "com/m3/compiletime/fixture/FakeTable",
            "com/m3/compiletime/fixture/ItemUsers"};
        M3ItemReferenceScan scan = new M3ItemReferenceScan();
        for (String n : names) scan.hierarchy(bytes(n));
        scan.closeTransientSet();
        assertTrue(scan.transientTypes.contains("com/m3/compiletime/fixture/FakeItem"), "@M3Transient seeds the set");
        scan.sites("test", "fixture", bytes("com/m3/compiletime/fixture/ItemUsers"));
        Map<String, String> byMethod = new HashMap<>();
        for (String[] r : scan.rows) byMethod.merge(r[3], r[9] + ":" + r[10], (a, b) -> a + "|" + b);
        assertEquals("BEHAVIOUR_ONLY:-", byMethod.get("behaviour"));
        assertEquals("BEHAVIOUR_ONLY:-", byMethod.get("behaviourLoop"));
        assertEquals("RETAINS:FIELD", byMethod.get("field"));
        assertEquals("RETAINS:COLLECTION", byMethod.get("collection"));
        assertEquals("RETAINS:RETURN", byMethod.get("returned"));
        assertEquals("RETAINS:IDENTITY_COMPARE", byMethod.get("identity"));
        assertEquals("RETAINS:LAMBDA", byMethod.get("lambda"));
        assertEquals("RETAINS:RETAINS_CALLEE", byMethod.get("retainsCallee"));
        assertEquals("UNKNOWN:ARG_UNKNOWN", byMethod.get("unknownArg"));
        assertEquals("RETAINS:ARRAY", byMethod.get("arrayStore"));
        assertEquals("RETAINS:IDENTITY_HASH", byMethod.get("hashed"));
        assertEquals("BEHAVIOUR_ONLY:-", byMethod.get("constructed"));
        for (String[] r : scan.rows) assertTrue(Integer.parseInt(r[5]) >= 0, "exact bytecode offset recorded");
    }

    @Test
    void siteKeyMatchesTheSwtEngineVector() {
        // Same vector as SWT Test_ItemResidency#siteKeyIsFnv1a32OfOwnerNameDescAndBci.
        String s = "org/eclipse/swt/examples/X.paint(Lorg/eclipse/swt/events/PaintEvent;)V@17";
        int h = 0x811C9DC5;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 16777619;
        }
        assertEquals(h, M3ItemReferenceScan.siteKey("org/eclipse/swt/examples/X", "paint",
                "(Lorg/eclipse/swt/events/PaintEvent;)V", 17));
    }

    private static Map<String, String> classifications() throws IOException {
        M3ItemReferenceScan scan = new M3ItemReferenceScan();
        for (String name : new String[] {"FakeItem", "FakeTable", "ItemUsers"}) {
            scan.hierarchy(bytes("com/m3/compiletime/fixture/" + name));
        }
        scan.closeTransientSet();
        scan.sites("test", "fixture", bytes("com/m3/compiletime/fixture/ItemUsers"));
        Map<String, String> result = new HashMap<>();
        for (String[] row : scan.rows) result.put(row[3], row[9] + ":" + row[10]);
        return result;
    }

    @Test
    void requireNonNullReturnedAliasesKeepTheirFieldAndReturnEscapes() throws IOException {
        Map<String, String> rows = classifications();
        for (String name : new String[] {"requireAlias", "requireMessageAlias", "requireSupplierAlias"}) {
            assertEquals("RETAINS:FIELD", rows.get(name), name + rows);
        }
        assertEquals("RETAINS:RETURN", rows.get("requireReturnAlias"), rows.toString());
    }

    @Test
    void returnedAliasCastAndLocalCopiesKeepTheirEscape() throws IOException {
        Map<String, String> rows = classifications();
        assertEquals("RETAINS:FIELD", rows.get("requireCastAlias"), rows.toString());
        assertEquals("BEHAVIOUR_ONLY:-", rows.get("requireBehaviour"), rows.toString());
    }

    @Test
    void behaviourAnnotationDoesNotAuthorizeAnotherOverload() throws IOException {
        Map<String, String> rows = classifications();
        assertEquals("UNKNOWN:ARG_UNKNOWN", rows.get("overloadCall"), rows.toString());
    }

    @Test
    void retainingAnnotationDoesNotClassifyAnotherOverload() throws IOException {
        Map<String, String> rows = classifications();
        assertEquals("UNKNOWN:ARG_UNKNOWN", rows.get("retainingOverloadCall"), rows.toString());
    }

    @Test
    void otherNullHelpersRemainUnknown() throws IOException {
        Map<String, String> rows = classifications();
        assertEquals("UNKNOWN:ARG_UNKNOWN", rows.get("differentNullHelper"), rows.toString());
    }
}
