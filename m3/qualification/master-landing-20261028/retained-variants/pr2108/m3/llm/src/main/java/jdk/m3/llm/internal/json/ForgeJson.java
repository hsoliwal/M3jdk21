// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified for M3JDK: attributed package relocation and product module integration.
// Derivation: PINNED_JSON_HELPER_ATOM_EXTRACTION; see SOURCE_PROVENANCE.tsv.
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.llm.internal.json;

import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;

/** Public facade; the existing strict recovery codec remains the single implementation. */
public final class ForgeJson {
    private ForgeJson() { }
    public static Object decode(byte[] bytes) { return RecoveryJson.parse(text(bytes)); }
    public static byte[] encode(Object value) { return RecoveryJson.write(value).getBytes(StandardCharsets.UTF_8); }
    public static String text(byte[] bytes) { return JsonSupport.text(bytes); }
    public static String hash(byte[] bytes) { return JsonSupport.sha256(bytes); }
    public static Map<String, Object> object(Object value) { return RecoveryJson.object(value); }
    public static List<?> array(Object value) { return RecoveryJson.array(value); }
    public static String string(Map<String, Object> value, String key) { return RecoveryJson.string(value, key); }
}
