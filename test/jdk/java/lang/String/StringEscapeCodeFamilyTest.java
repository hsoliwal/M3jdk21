/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * GPLv2 with the Classpath exception.
 *
 * @test
 * @summary JUnit coverage of the String escape-code family (escapeJava ... unescapeProperties)
 * @run junit StringEscapeCodeFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      StringEscapeCodeFamilyTest
 * @run junit/othervm -XX:+UnlockExperimentalVMOptions -XX:+UseM3StringStorage
 *      -XX:-CompactStrings StringEscapeCodeFamilyTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Every escape-code method gives the same answer on every storage form of a text (the whole
 * String, a range of a longer String, a concatenation of two halves) as on the flat String; fixed
 * cases pin the documented rules. The flat answers equal those of commons-text 1.15.0
 * StringEscapeUtils and unbescape 1.1.8 CssEscape/PropertiesEscape on every code point and on
 * every text of up to four units over escape alphabets (donor-oracle proof in the string-lane
 * record).
 */
public class StringEscapeCodeFamilyTest {
    private static final char HIGH = (char) 0xD835;
    private static final char LOW = (char) 0xDD1E;
    private static final char[] UNITS = {'\\', 'u', '0', 'F', '"', '\n', '\r', ' ', '-', ',',
        (char) 0x00E9, HIGH, LOW};

    static Stream<String> texts() {
        List<String> texts = new ArrayList<>();
        for (int length = 0; length <= 4; length++) {
            int total = (int) Math.pow(UNITS.length, length);
            for (int code = 0; code < total; code++) {
                char[] units = new char[length];
                int rest = code;
                for (int i = 0; i < length; i++) {
                    units[i] = UNITS[rest % UNITS.length];
                    rest /= UNITS.length;
                }
                texts.add(new String(units));
            }
        }
        return texts.stream();
    }

    private static List<Object> answers(String s) {
        List<Function<String, String>> methods = List.of(String::escapeJava, String::unescapeJava,
                String::escapeEcmaScript, String::unescapeEcmaScript, String::escapeJson,
                String::unescapeJson, String::escapeCsv, String::unescapeCsv, String::escapeXSI,
                String::unescapeXSI, String::escapeCssString, String::escapeCssIdentifier,
                String::unescapeCss, String::escapePropertiesValue, String::escapePropertiesKey,
                String::unescapeProperties,
                text -> text.translateEscapes(String.EscapeSyntax.FTL),
                text -> text.translateEscapes(String.EscapeSyntax.ST));
        List<Object> out = new ArrayList<>();
        for (Function<String, String> method : methods) {
            try {
                out.add(method.apply(s));
            } catch (IllegalArgumentException e) {
                out.add(e.getClass());
            }
        }
        return out;
    }

    private static List<Function<String, String>> forms() {
        return List.of(
            text -> new String(("<<" + text + ">>").toCharArray()).substring(2, 2 + text.length()),
            text -> new String(text.toCharArray(), 0, text.length() / 2)
                    .concat(new String(text.toCharArray(), text.length() / 2,
                            text.length() - text.length() / 2)));
    }

    @ParameterizedTest(name = "{index}")
    @MethodSource("texts")
    void everyFormAnswersAsTheFlatString(String text) {
        List<Object> expected = answers(new String(text.toCharArray()));
        for (Function<String, String> form : forms()) {
            assertEquals(expected, answers(form.apply(text)));
        }
    }

    @Test
    void javaRules() {
        assertEquals("a\\\"b\\\\c\\b\\t\\n\\f\\r", "a\"b\\c\b\t\n\f\r".escapeJava());
        assertEquals("'/\u007F", "'/\u007F".escapeJava(), "apostrophe, slash, DEL kept");
        assertEquals("\\u0000\\u001F\\u0080\\u00E9", "\u0000\u001F\u0080\u00E9".escapeJava());
        assertEquals("\\uD835\\uDD1E", ("" + HIGH + LOW).escapeJava(), "a pair, two escapes");
        assertEquals("\\uD835", ("" + HIGH).escapeJava(), "a lone surrogate");
        assertEquals("\\'\\/", "'/".escapeEcmaScript());
        assertEquals("'\\/\\u007F", "'/\u007F".escapeJson());
        String plain = "plain text 1+1=2";
        assertSame(plain, plain.escapeJava());
        assertSame(plain, plain.escapeEcmaScript());
        assertSame(plain, plain.escapeJson());
    }

    @Test
    void unescapeJavaRules() {
        assertEquals("\u0000\u0007\u00FF\u00FF7@8", "\\0\\7\\377\\3777\\1008".unescapeJava());
        assertEquals("'7", "\\477".unescapeJava(), "three octal digits only after 0 to 3");
        assertEquals("A\u00E9\u00E9", "\\u0041\\uu00e9\\u+00E9".unescapeJava());
        assertEquals("\b\t\n\f\r\\\"'", "\\b\\t\\n\\f\\r\\\\\\\"\\'".unescapeJava());
        assertEquals("ax/", "a\\x\\/".unescapeJava(), "an unknown escape loses its backslash");
        assertEquals("a", "a\\".unescapeJava(), "a trailing backslash is removed");
        assertEquals("\uf123", "\\u-EDD".unescapeJava(),
                "a signed value is read as Integer.parseInt reads it");
        assertThrows(IllegalArgumentException.class, () -> "\\u12".unescapeJava());
        assertThrows(IllegalArgumentException.class, () -> "\\u12g4".unescapeJava());
        assertEquals("\"x\"", "\\\"x\\\"".unescapeEcmaScript());
        assertEquals("/", "\\/".unescapeJson());
        String plain = "no escapes";
        assertSame(plain, plain.unescapeJava());
    }

    @Test
    void csvRules() {
        assertEquals("\"a,b\"", "a,b".escapeCsv());
        assertEquals("\"say \"\"hi\"\"\"", "say \"hi\"".escapeCsv());
        assertEquals("\"\r\"", "\r".escapeCsv());
        String plain = "a b;c";
        assertSame(plain, plain.escapeCsv());
        assertEquals("a,b", "\"a,b\"".unescapeCsv());
        assertEquals("say \"hi\"", "\"say \"\"hi\"\"\"".unescapeCsv());
        assertEquals("a\"b", "\"a\"\"b\"".unescapeCsv(), "doubled quotes collapse");
        assertEquals("ab", "\"ab\"".unescapeCsv(), "quotes go even without specials");
        String lone = "\"";
        assertSame(lone, lone.unescapeCsv(), "a lone quotation mark is not a field");
        String open = "\"ab";
        assertSame(open, open.unescapeCsv());
    }

    @Test
    void xsiRules() {
        assertEquals("a\\ b\\|c\\$\\\t", "a b|c$\t".escapeXSI());
        assertEquals("ab", "a\r\nb".escapeXSI(), "CR LF removed");
        assertEquals("a\rb", "a\rb".escapeXSI(), "a lone CR kept");
        assertEquals("ab", "a\nb".escapeXSI());
        assertEquals("a b\\", "a\\ b\\\\".unescapeXSI());
        assertEquals("", "\\".unescapeXSI());
        String plain = "a!{}";
        assertSame(plain, plain.escapeXSI());
    }

    @Test
    void cssRules() {
        assertEquals("\\\"\\'\\\\\\/\\;\\26x\\3C\\3E", "\"'\\/;&x<>".escapeCssString());
        assertEquals("\\E9 0\\E9  \\E9\\E9", "\u00E90\u00E9 \u00E9\u00E9".escapeCssString());
        assertEquals("\\1D51E", ("" + HIGH + LOW).escapeCssString(), "a pair, one code point");
        assertEquals("\\D835", ("" + HIGH).escapeCssString(), "a lone surrogate");
        assertEquals("\\0", "\u0000".escapeCssString());
        assertEquals("a b!#", "a b!#".escapeCssString(), "level 2 keeps the space and !#");
        assertEquals("\\31 a", "1a".escapeCssIdentifier(), "a leading digit in hex");
        assertEquals("\\-1", "-1".escapeCssIdentifier());
        assertEquals("-a", "-a".escapeCssIdentifier());
        assertEquals("\\-", "-".escapeCssIdentifier());
        assertEquals("\\_a_b-c", "_a_b-c".escapeCssIdentifier());
        assertEquals("a\\ b\\3A c\\.d", "a b:c.d".escapeCssIdentifier());
        assertEquals("\\E9 a", "\u00E9a".escapeCssIdentifier());
        assertEquals("\\E9\\ ", "\u00E9 ".escapeCssIdentifier(), "no hex space before a space");
        String plain = "abc_1";
        assertSame(plain, plain.escapeCssIdentifier());
        assertSame(plain, plain.escapeCssString());
    }

    @Test
    void unescapeCssRules() {
        assertEquals("\u00E90", "\\E9 0".unescapeCss());
        assertEquals("" + HIGH + LOW, "\\1D51E".unescapeCss());
        assertEquals("" + HIGH + LOW + "x", "\\D835 \\DD1E x".unescapeCss(), "a pair of escapes");
        assertEquals("\uFFFD\uFFFD", "\\0\\110000".unescapeCss());
        assertEquals("\uFFFD", "\\D835".unescapeCss(), "a lone surrogate value");
        assertEquals("ab", "a\\\r\nb".unescapeCss(), "a line continuation");
        assertEquals("g;", "\\g\\;".unescapeCss());
        assertEquals("a\u000B", "a\\\u000B".unescapeCss());
        String trailing = "a\\";
        assertSame(trailing, trailing.unescapeCss(), "a trailing backslash is kept");
    }

    @Test
    void propertiesRules() {
        assertEquals("\\ a b", " a b".escapePropertiesValue(), "only a leading space");
        assertEquals("\\:a:=b=", ":a:=b=".escapePropertiesValue(), "a leading colon only");
        assertEquals("\\\\\\t\\n\\f\\r\\u0008\\u007F\\u00E9", "\\\t\n\f\r\b\u007F\u00E9"
                .escapePropertiesValue());
        assertEquals("a\\ b\\!\\#\\:\\=", "a b!#:=".escapePropertiesKey());
        assertEquals("\\uD835\\uDD1E", ("" + HIGH + LOW).escapePropertiesKey());
        assertEquals("\t\n\f\rA\\u12x", "\\t\\n\\f\\r\\u0041\\u12x".unescapeProperties());
        assertEquals("bx", "\\b\\x".unescapeProperties(), "other escapes lose the backslash");
        String kept = "\\u12";
        assertSame(kept, kept.unescapeProperties(), "a short unicode escape is kept");
        String plain = "key.name";
        assertSame(plain, plain.escapePropertiesKey());
        assertSame(plain, plain.escapePropertiesValue());
    }

    @Test
    void templateSyntaxRules() {
        String.EscapeSyntax ftl = String.EscapeSyntax.FTL;
        assertEquals("a\"'\\{=\n\r\t\f\b><&b",
                "a\\\"\\'\\\\\\{\\=\\n\\r\\t\\f\\b\\g\\l\\ab".translateEscapes(ftl));
        assertEquals("A\u4142Az", "\\x41\\x4142\\x0041z".translateEscapes(ftl),
                "one to four digits");
        assertThrows(IllegalArgumentException.class, () -> "\\q".translateEscapes(ftl));
        assertThrows(IllegalArgumentException.class, () -> "a\\".translateEscapes(ftl));
        assertThrows(IllegalArgumentException.class, () -> "\\x".translateEscapes(ftl));
        assertThrows(IllegalArgumentException.class, () -> "\\xg".translateEscapes(ftl));
        String.EscapeSyntax st = String.EscapeSyntax.ST;
        assertEquals("\n\r\tb\"\\x", "\\n\\r\\t\\b\\\"\\\\x".translateEscapes(st));
        assertThrows(IllegalArgumentException.class, () -> "a\\".translateEscapes(st));
        assertThrows(NullPointerException.class,
                () -> "a".translateEscapes((String.EscapeSyntax) null));
        String plain = "no escapes";
        assertSame(plain, plain.translateEscapes(ftl));
        assertSame(plain, plain.translateEscapes(st));
        assertEquals("a\\tb", "a\\\\tb".translateEscapes(), "Java's own escapes unchanged");
    }

    @Test
    void longTextsAcrossWindows() {
        String pair = "" + HIGH + LOW;
        for (int k = 505; k <= 1030; k += (k == 520 ? 498 : 1)) {
            String x = "x".repeat(k);
            assertEquals(x + "\\uD835\\uDD1E", (x + pair).escapeJava());
            assertEquals(x + "\\1D51E 0", (x + pair + "0").escapeCssString());
            assertEquals(x + "\\E9  ", (x + "\u00E9 ").escapeCssString());
            assertEquals(x + "y", (x + "\r\ny").escapeXSI());
            assertEquals(x + "A", (x + "\\u0041").unescapeJava());
            assertEquals(x + pair, (x + "\\D835 \\DD1E").unescapeCss());
            String text = x + pair + "a";
            assertSame(text, text.escapeCsv());
            assertSame(text, text.escapeXSI());
            assertEquals(x + "A\n", (x + "\\x41\\n").translateEscapes(String.EscapeSyntax.FTL));
            assertEquals(x + pair, (x + "\\" + pair).translateEscapes(String.EscapeSyntax.ST));
        }
    }
}
