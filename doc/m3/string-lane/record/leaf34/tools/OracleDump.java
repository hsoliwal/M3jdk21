// SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
// Copyright (c) 2026 Synexia, Hitesh Soliwal and others

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.text.translate.EntityArrays;
import org.unbescape.html.HtmlEscape;
import org.unbescape.html.HtmlEscapeLevel;
import org.unbescape.html.HtmlEscapeType;

/**
 * ESCAPE_MARKUP_ORACLE_DUMP v1 (2026-11-17). usage: OracleDump <out-dir>
 * Dumps, from the donor jars themselves (CPU only), the facts the table generator checks and uses:
 * oracle_html5_sorted.tsv  unbescape 1.1.8 HTML5_SYMBOLS.SORTED_NCRS order: index, name, code points
 * oracle_html5_escape.tsv  every code point unbescape escapes to a named reference (HTML5 type, level 4)
 * oracle_html4_escape.tsv  the same for the HTML4 type
 * oracle_commons_html4.tsv commons-text 1.15.0 EntityArrays BASIC/ISO8859_1/HTML40_EXTENDED/APOS escape maps
 */
public final class OracleDump {
    private OracleDump() {}

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        Class<?> symbols = Class.forName("org.unbescape.html.HtmlEscapeSymbols");
        Object html5 = field(symbols, null, "HTML5_SYMBOLS");
        char[][] ncrs = (char[][]) field(symbols, html5, "SORTED_NCRS");
        int[] codes = (int[]) field(symbols, html5, "SORTED_CODEPOINTS");
        int[][] doubles = (int[][]) field(symbols, html5, "DOUBLE_CODEPOINTS");
        StringBuilder sorted = new StringBuilder();
        for (int i = 0; i < ncrs.length; i++) {
            int code = codes[i];
            String points = code >= 0 ? Integer.toHexString(code)
                    : Integer.toHexString(doubles[-code - 1][0]) + " "
                    + Integer.toHexString(doubles[-code - 1][1]);
            sorted.append(i).append('\t').append(new String(ncrs[i])).append('\t').append(points)
                    .append('\n');
        }
        write(out.resolve("oracle_html5_sorted.tsv"), sorted);
        write(out.resolve("oracle_html5_escape.tsv"),
                escapes(HtmlEscapeType.HTML5_NAMED_REFERENCES_DEFAULT_TO_DECIMAL));
        write(out.resolve("oracle_html4_escape.tsv"),
                escapes(HtmlEscapeType.HTML4_NAMED_REFERENCES_DEFAULT_TO_DECIMAL));
        StringBuilder commons = new StringBuilder();
        dump(commons, "BASIC", EntityArrays.BASIC_ESCAPE);
        dump(commons, "ISO8859_1", EntityArrays.ISO8859_1_ESCAPE);
        dump(commons, "HTML40_EXTENDED", EntityArrays.HTML40_EXTENDED_ESCAPE);
        dump(commons, "APOS", EntityArrays.APOS_ESCAPE);
        dumpInverse(commons, "BASIC_UNESCAPE", EntityArrays.BASIC_UNESCAPE);
        dumpInverse(commons, "ISO8859_1_UNESCAPE", EntityArrays.ISO8859_1_UNESCAPE);
        dumpInverse(commons, "HTML40_EXTENDED_UNESCAPE", EntityArrays.HTML40_EXTENDED_UNESCAPE);
        dumpInverse(commons, "APOS_UNESCAPE", EntityArrays.APOS_UNESCAPE);
        write(out.resolve("oracle_commons_html4.tsv"), commons);
        System.out.println("oracle_dump\tsorted\t" + ncrs.length);
    }

    private static StringBuilder escapes(HtmlEscapeType type) {
        StringBuilder out = new StringBuilder();
        for (int point = 0; point <= Character.MAX_CODE_POINT; point++) {
            if (point >= 0xD800 && point <= 0xDFFF) continue;
            String escaped = HtmlEscape.escapeHtml(Character.toString(point), type,
                    HtmlEscapeLevel.LEVEL_4_ALL_CHARACTERS);
            if (!escaped.startsWith("&#")) {
                out.append(Integer.toHexString(point)).append('\t').append(escaped).append('\n');
            }
        }
        return out;
    }

    private static void dump(StringBuilder out, String set, Map<CharSequence, CharSequence> map) {
        Map<Integer, String> sorted = new TreeMap<>();
        for (Map.Entry<CharSequence, CharSequence> e : map.entrySet()) {
            String key = e.getKey().toString();
            if (key.codePointCount(0, key.length()) != 1) throw new IllegalStateException(key);
            sorted.put(key.codePointAt(0), e.getValue().toString());
        }
        for (Map.Entry<Integer, String> e : sorted.entrySet()) {
            out.append(set).append('\t').append(Integer.toHexString(e.getKey())).append('\t')
                    .append(e.getValue()).append('\n');
        }
    }

    private static void dumpInverse(StringBuilder out, String set,
            Map<CharSequence, CharSequence> map) {
        Map<String, String> sorted = new TreeMap<>();
        for (Map.Entry<CharSequence, CharSequence> e : map.entrySet()) {
            String value = e.getValue().toString();
            sorted.put(e.getKey().toString(), Integer.toHexString(value.codePointAt(0)));
        }
        for (Map.Entry<String, String> e : sorted.entrySet()) {
            out.append(set).append('\t').append(e.getValue()).append('\t').append(e.getKey())
                    .append('\n');
        }
    }

    private static Object field(Class<?> owner, Object target, String name) throws Exception {
        Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void write(Path path, CharSequence text) throws IOException {
        Files.writeString(path, text, StandardCharsets.UTF_8);
    }
}
